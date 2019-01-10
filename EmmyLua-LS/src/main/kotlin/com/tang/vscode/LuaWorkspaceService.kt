package com.tang.vscode

import com.google.gson.JsonObject
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.tang.intellij.lua.stubs.index.LuaShortNameIndex
import com.tang.vscode.api.IFolder
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.IVirtualFile
import com.tang.vscode.api.IWorkspace
import com.tang.vscode.api.impl.Folder
import com.tang.vscode.api.impl.LuaFile
import com.tang.vscode.utils.computeAsync
import com.tang.vscode.utils.getSymbol
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.WorkspaceService
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaWorkspaceService : WorkspaceService, IWorkspace {
    private val _rootList = mutableListOf<IFolder>()
    private val _rootWSFolders = mutableListOf<URI>()
    private val _baseFolders = mutableListOf<IFolder>()
    private var client: LuaLanguageClient? = null

    inner class WProject : UserDataHolderBase(), Project {
        override fun process(processor: Processor<PsiFile>) {
            for (ws in _rootList) {
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

    val project: Project

    init {
        project = WProject()
        project.putUserData(IWorkspace.KEY, this)
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        for (change in params.changes) {
            when (change.type) {
                FileChangeType.Created -> addFile(change.uri)
                FileChangeType.Deleted -> removeFile(change.uri)
                FileChangeType.Changed -> {
                    removeFile(change.uri)
                    addFile(change.uri)
                }
                else -> { }
            }
        }
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        val settings = params.settings as? JsonObject ?: return
        Configuration.update(settings)
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        if (params.query.isBlank())
            return CompletableFuture.completedFuture(mutableListOf())
        val matcher = CamelHumpMatcher(params.query, false)
        return computeAsync { cancel->
            val list = mutableListOf<SymbolInformation>()
            LuaShortNameIndex.processValues(project, GlobalSearchScope.projectScope(project), Processor {
                cancel.checkCanceled()
                val name = it.name
                if (it is PsiNamedElement && name != null && matcher.prefixMatches(name)) {
                    list.add(it.getSymbol())
                }
                true
            })
            list
        }
    }

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        params.event.added.forEach {
            addRoot(it.uri)
        }
        params.event.removed.forEach {
            removeRoot(it.uri)
        }
        if (params.event.added.isNotEmpty())
            loadWorkspace()
    }

    override fun eachRoot(processor: (ws: IFolder) -> Boolean) {
        for (root in _rootList) {
            if (!processor(root))
                break
        }
    }

    private fun getWSRoot(uri: URI): IFolder {
        var ws: IFolder? = null
        eachRoot {
            if (it.matchUri(uri)) {
                ws = it
                return@eachRoot false
            }
            true
        }
        return ws ?: addWSRoot(uri)
    }

    private fun findOrCreate(path: Path, autoCreate: Boolean): Pair<IFolder?, Boolean> {
        var isCreated = false

        val driver = path.root
        val base = _baseFolders.find { it.path == driver }
        var folder = base ?: if (autoCreate) {
            val newBase = Folder(driver, driver.toFile().name)
            _baseFolders.add(newBase)
            isCreated = true
            newBase
        } else null

        if (folder == null)
            return Pair(folder, false)

        for (i in 0 until path.nameCount) {
            val name = path.getName(i).toFile().name
            val find = folder?.findFile(name) as? IFolder
            folder = if (find != null) find else {
                val create = folder?.createFolder(name)
                isCreated = true
                create
            }
        }

        return Pair(folder, isCreated)
    }

    private fun addWSRoot(uri: URI): IFolder {
        val path = Paths.get(uri)
        val exist = _rootList.find { it.path == path }
        if (exist != null) return exist

        val pair = findOrCreate(path, true)
        val folder = pair.first!!
        if (pair.second)
            _rootList.add(folder)
        return folder
    }

    private fun removeRoot(uri: String) {
        val path = Paths.get(URI(uri))
        _rootList.removeIf { folder ->
            if (folder.path == path) {
                folder.walkFiles {
                    it.unindex()
                    true
                }
                folder.parent.removeFile(folder)
                return@removeIf true
            }
            false
        }
    }

    fun addRoot(uri: String) {
        addRoot(URI(uri))
    }

    private fun addRoot(uri: URI) {
        if (_rootWSFolders.contains(uri))
            return
        getWSRoot(uri)
        _rootWSFolders.add(uri)
    }

    private fun collectFiles(file: File, list: MutableList<File>) {
        if (file.isFile && file.extension == "lua") {
            list.add(file)
        } else if (file.isDirectory) {
            file.listFiles().forEach { collectFiles(it, list) }
        }
    }

    fun loadWorkspace() {
        loadWorkspace(object : IProgressMonitor {
            override fun done() {
                client?.progressReport(ProgressReport("Finished!", 1f))
            }

            override fun setProgress(text: String, percent: Float) {
                client?.progressReport(ProgressReport(text, percent))
            }
        })
    }

    private fun loadWorkspace(monitor: IProgressMonitor) {
        monitor.setProgress("load workspace folders", 0f)
        client?.workspaceFolders()?.whenCompleteAsync { wsFolders, _ ->
            wsFolders?.forEach { addRoot(it.uri) }
            loadWorkspaceImpl(monitor)
        }
    }

    private fun loadWorkspaceImpl(monitor: IProgressMonitor) {
        val allFiles = mutableListOf<File>()
        val arr = _rootWSFolders.toTypedArray()
        _rootWSFolders.clear()
        arr.forEach { uri ->
            val folder = File(uri.path)
            collectFiles(folder, allFiles)
        }

        allFiles.forEachIndexed { index, file ->
            val findFile = findFile(file.toURI().toString())
            if (findFile == null)
                addFile(file)
            monitor.setProgress("Emmy load file: ${file.canonicalPath}", (index + 1) / allFiles.size.toFloat())
        }
        monitor.done()
        sendAllDiagnostics()
    }

    /**
     * send all diagnostics of the workspace
     */
    private fun sendAllDiagnostics() {
        project.process {
            val file = it.virtualFile
            if (file is LuaFile && file.diagnostics.isNotEmpty()) {
                client?.publishDiagnostics(PublishDiagnosticsParams(file.path.toString(), file.diagnostics))
            }
            true
        }
    }

    override fun findFile(uri: String): IVirtualFile? {
        val u = Paths.get(URI(uri))
        val pair = findOrCreate(u.parent, false)
        val root = pair.first
        return root?.findFile(u.toFile().name)
    }

    override fun addFile(file: File, text: String?): ILuaFile {
        val path = file.toPath()
        val pair = findOrCreate(path.parent, true)
        val root = pair.first!!
        return root.addFile(file.name, text ?: LoadTextUtil.getTextByBinaryPresentation(file.readBytes()))
    }

    private fun addFile(uri: String) {
        val u = URI(uri)
        addFile(File(u.path))
    }

    override fun removeFile(uri: String) {
        val file = findFile(uri)
        file?.let { it.parent.removeFile(it) }
    }

    fun connect(client: LuaLanguageClient) {
        this.client = client
    }

    fun dispose() {
        _rootWSFolders.clear()
        _rootList.forEach { it.removeAll() }
        _rootList.clear()
    }
}