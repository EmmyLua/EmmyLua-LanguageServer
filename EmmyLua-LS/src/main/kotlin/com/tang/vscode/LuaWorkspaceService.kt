package com.tang.vscode

import com.google.gson.JsonObject
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCoreUtil
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.tang.intellij.lua.IVSCodeSettings
import com.tang.intellij.lua.configuration.IConfigurationManager
import com.tang.intellij.lua.fs.FileManager
import com.tang.intellij.lua.fs.IFileManager
import com.tang.intellij.lua.stubs.index.LuaShortNameIndex
import com.tang.lsp.*
import com.tang.vscode.api.impl.Folder
import com.tang.vscode.api.impl.LuaFile
import com.tang.vscode.configuration.ConfigurationManager
import com.tang.vscode.diagnostics.DiagnosticsService
import com.tang.vscode.utils.computeAsync
import com.tang.vscode.utils.getSymbol
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.WorkspaceService
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaWorkspaceService : WorkspaceService, IWorkspace {
    private val rootList = mutableListOf<IFolder>()
    private val schemeMap = mutableMapOf<String, IFolder>()
    private val configurationManager = ConfigurationManager()
    private var client: LuaLanguageClient? = null
    private val project: Project = WProject()
    private val fileManager = FileManager(project)
    private val fileScopeProvider = WorkspaceRootFileScopeProvider()
    private val rwl = ReentrantReadWriteLock()

    inner class WProject : UserDataHolderBase(), Project {
        override fun process(processor: Processor<PsiFile>) {
            for (ws in rootList) {
                val continueRun = ws.walkFiles {
                    val psi = it.psi
                    if (psi != null)
                        return@walkFiles processor.process(psi)
                    true
                }
                if (!continueRun) break
            }
        }
    }

    init {
        project.putUserData(IWorkspace.KEY, this)
        fileManager.addProvider(fileScopeProvider)
    }

    fun <R> ready(code: () -> R): R {
        return rwl.read(code)
    }

    fun canWrite(code: () -> Unit) {
        rwl.write(code)
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        for (change in params.changes) {
            when (change.type) {
                FileChangeType.Created -> addFile(change.uri)
                FileChangeType.Deleted -> removeFile(change.uri)
                FileChangeType.Changed -> {
                    if (change.uri.endsWith("globalStorage")) {
                        return
                    }
                    removeFile(change.uri)
                    addFile(change.uri)
                }

                else -> {}
            }
        }
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        val settings = params.settings as? JsonObject ?: return
        val ret = VSCodeSettings.update(settings)
        if (ret.associationChanged) {
            loadWorkspace()
            refreshWorkspace()
        }
    }

    fun initConfigFiles(files: Array<EmmyConfigurationSource>) {
        if (files.isNotEmpty()) {
            configurationManager.init(files)
            fileScopeProvider.configManager = configurationManager
            for (source in configurationManager.sourceRoots) {
                if (source.absoluteDir != null) {
                    val rootList = fileScopeProvider.getRootIncludeConfigFiles(source.absoluteDir!!)
                    for (root in rootList) {
                        fileScopeProvider.removeRoot(root)
                    }
                }
            }
            for (source in configurationManager.sourceRoots) {
                if (source.absoluteDir != null) {
                    fileScopeProvider.addRoot(source.absoluteDir!!)
                }
            }
        }
    }

    @JsonRequest("emmy/updateConfig")
    fun updateConfig(params: UpdateConfigParams): CompletableFuture<Void> {
        configurationManager.updateConfiguration(params)
        loadWorkspace()
        refreshWorkspace()
        return CompletableFuture()
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<Either<MutableList<out SymbolInformation>, MutableList<out WorkspaceSymbol>>> {
        if (params.query.isBlank())
            return CompletableFuture.completedFuture(Either.forRight(mutableListOf()))
        val matcher = CamelHumpMatcher(params.query, false)
        return computeAsync { cancel ->
            val list = mutableListOf<SymbolInformation>()
            LuaShortNameIndex.processValues(project, GlobalSearchScope.projectScope(project), Processor {
                cancel.checkCanceled()
                val name = it.name
                if (it is PsiNamedElement && name != null && matcher.prefixMatches(name)) {
                    list.add(it.getSymbol())
                }
                true
            })
            Either.forLeft(list)
        }
    }

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        params.event.added.forEach {
            addRoot(it.uri)
        }
        params.event.removed.forEach {
            removeRoot(it.uri)
        }
        if (params.event.added.isNotEmpty()) {
            loadWorkspace()
            refreshWorkspace()
        }
    }

    override fun eachRoot(processor: (ws: IFolder) -> Boolean) {
        for (root in rootList) {
            if (!processor(root))
                break
        }
    }

    private fun getSchemeFolder(path: FileURI, autoCreate: Boolean): IFolder? {
        var folder: IFolder? = schemeMap[path.scheme]
        if (folder == null && autoCreate) {
            folder = Folder(FileURI("${path.scheme}:/", true))
            schemeMap[path.scheme] = folder
        }
        return folder
    }

    private fun findOrCreate(path: FileURI, autoCreate: Boolean): Pair<IFolder?, Boolean> {
        var isCreated = false
        var folder = getSchemeFolder(path, autoCreate)
        if (folder == null)
            return Pair(folder, isCreated)
        for (i in 0 until path.nameCount) {
            val name = path.getName(i)
            val find = folder?.findFile(name) as? IFolder
            folder = if (find != null) find else {
                val create = folder?.createFolder(name)
                isCreated = true
                create
            }
        }
        return Pair(folder, isCreated)
    }

    private fun addRoot(fileURI: FileURI): IFolder {
        val exist = rootList.find { it.uri == fileURI }
        if (exist != null) return exist

        val pair = findOrCreate(fileURI, true)
        val folder = pair.first!!
        rootList.add(folder)
        return folder
    }

    private fun removeRoot(uri: String) {
        val path = FileURI(uri, true)
        rootList.removeIf { folder ->
            if (folder.uri == path) {
                fileScopeProvider.removeRoot(path)
                removeFolder(folder)
                return@removeIf true
            }
            false
        }
    }

    private fun removeFolder(folder: IFolder) {
        folder.walkFiles {
            it.unindex()
            true
        }
        folder.parent.removeFile(folder)
    }

    fun addRoot(uri: String) {
        fileScopeProvider.addRoot(FileURI(uri, true))
    }

    private fun cleanWorkspace() {
        val removeList = mutableListOf<ILuaFile>()
        project.process { psiFile ->
            val file = psiFile.virtualFile
            if (file is ILuaFile) {
                if (fileManager.isExclude(file.uri)) {
                    removeList.add(file)
                }
            }
            true
        }
        removeList.forEach {
            it.parent.removeFile(it)
        }
    }

    fun loadWorkspace() {
        cleanWorkspace()
        loadWorkspace(object : IProgressMonitor {
            override fun start() {
                if (VSCodeSettings.isVSCode)
                    client?.setServerStatus(ServerStatusParams("ok", "load workspace", true))
            }

            override fun done() {
                if (VSCodeSettings.isVSCode) {
                    client?.progressReport(ProgressReport("Finished!", 1f))
                    client?.setServerStatus(ServerStatusParams("ok", "EmmyLua Language Server", false))
                }
            }

            override fun setProgress(text: String, percent: Float) {
                if (VSCodeSettings.isVSCode)
                    client?.progressReport(ProgressReport(text, percent))
            }

            override fun reportError(text: String) {
                if (VSCodeSettings.isVSCode)
                    client?.setServerStatus(ServerStatusParams("error", "load fail", false))
            }
        })
    }

    private fun refreshWorkspace() {
        client?.refreshDiagnostics()
    }

    fun diagnoseFile(file: ILuaFile, previousId: String?, checker: CancelChecker?): DocumentDiagnosticReport {
        val diagnostics = mutableListOf<Diagnostic>()

        if (file is LuaFile) {
            file.diagnostic(diagnostics, checker)
        }

        return DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport(diagnostics))
    }

    private fun loadWorkspace(monitor: IProgressMonitor) {
        rwl.write {
            try {
                monitor.start();
                monitor.setProgress("load workspace folders", 0f)
                val collections = fileManager.findAllFiles()
                var totalFileCount = 0f
                var processedCount = 0f
                collections.forEach { totalFileCount += it.files.size }
                for (collection in collections) {
                    addRoot(collection.root)
                    for (uri in collection.files) {
                        processedCount++
                        val file = uri.toFile()
                        if (file != null) {
                            monitor.setProgress(
                                    "indexing ${processedCount.toInt()} / ${totalFileCount.toInt()} (${file.name})",
                                    processedCount / totalFileCount
                            )
                        }
                        addFile(uri, null)
                    }
                }
            } catch (e: Exception) {
                monitor.reportError("workspace parse error: $e")
            }

            monitor.done()
        }
    }

    override fun findFile(uri: String): IVirtualFile? {
        val fileURI = FileURI(uri, false)
        return findFile(fileURI)
    }

    override fun findLuaFile(uri: String): ILuaFile? {
        return findFile(uri) as? ILuaFile
    }

    private fun findFile(fileURI: FileURI): IVirtualFile? {
        val parent = fileURI.parent
        val folder: IFolder? = if (parent == null)
            getSchemeFolder(fileURI, false)
        else
            findOrCreate(parent, false).first
        return folder?.findFile(fileURI.name)
    }

    private fun addDirectory(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                addFile(it)
            }
        }
    }

    override fun addFile(file: File, text: String?, force: Boolean): ILuaFile? {
        if (file.isDirectory) {
            addDirectory(file)
            return null
        }
        val fileURI = FileURI(file.toURI(), false)
        return addFile(fileURI, text, force)
    }

    private fun addFile(fileURI: FileURI, text: String?, force: Boolean = false): ILuaFile? {
        val file = fileURI.toFile()
        if (file == null || (!force && !fileManager.isInclude(fileURI))) {
            return null
        }
        val existFile = findFile(fileURI)
        if (existFile is ILuaFile) {
            return existFile
        }

        val parent = fileURI.parent
        val folder: IFolder = (if (parent == null)
            getSchemeFolder(fileURI, true)
        else
            findOrCreate(parent, true).first) ?: return null

        return try {
            val content = text ?: LoadTextUtil.getTextByBinaryPresentation(file.readBytes())
            folder.addFile(file.name, content)
        } catch (e: Exception) {
            System.err.println("Invalidate lua file: ${file.canonicalPath}")
            null
        }
    }

    private fun addFile(uri: String) {
        val u = URI(uri)
        addFile(File(u.path))
    }

    override fun removeFile(uri: String) {
        val file = findFile(uri)
        file?.let {
            it.parent.removeFile(it)
        }
    }

    override fun removeFileIfNeeded(uri: String) {
        val file = findFile(uri)
        file?.let {
            if (!fileManager.isInclude(file.uri)) {
                it.parent.removeFile(it)
            }
        }
    }

    fun connect(client: LuaLanguageClient) {
        this.client = client
    }

    fun dispose() {
        schemeMap.clear()
        rootList.forEach { it.removeAll() }
        rootList.clear()
    }

    fun initIntellijEnv() {
        ProjectCoreUtil.theProject = project
        project.putUserData(IVSCodeSettings.KEY, VSCodeSettings)
        project.putUserData(IConfigurationManager.KEY, configurationManager)
        project.putUserData(IFileManager.KEY, fileManager)
    }

    fun getProject(): Project {
        return project
    }
}
