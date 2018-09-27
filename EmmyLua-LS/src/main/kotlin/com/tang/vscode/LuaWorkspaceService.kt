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
import com.tang.vscode.utils.safeURIName
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.WorkspaceService
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaWorkspaceService : WorkspaceService, IWorkspace {
    private var _root: URI = URI("file:///")
    private val _rootList = mutableListOf<IFolder>()
    private val _rootWSFolders = mutableListOf<URI>()
    private val _baseFolders = mutableListOf<IFolder>()
    private var client: LuaLanguageClient? = null

    val configuration = Configuration()

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
                    removeRoot(change.uri)
                    addFile(change.uri)
                }
                else -> { }
            }
        }
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        val settings = params.settings as? JsonObject ?: return
        configuration.update(settings)
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

    var root: URI
        get() = _root
        set(value) {
            _root = value
            addWSRoot(value)
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

    private fun findOrCreate(uri: URI, autoCreate: Boolean): Pair<IFolder?, Boolean> {
        val split = uri.path.split('/')
        var isCreated = false

        val driver = split[1]
        val base = _baseFolders.find { it.getName() == driver }
        var folder = base ?: if (autoCreate) {
            val u = URI("${uri.scheme}:///$driver/")
            val newBase = Folder(u, driver)
            _baseFolders.add(newBase)
            isCreated = true
            newBase
        } else null

        if (folder == null)
            return Pair(folder, false)

        for (i in 2 until split.size) {
            val name = split[i]
            if (i == split.lastIndex && name.isEmpty())
                break
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
        val exist = _rootList.find { it.uri == uri }
        if (exist != null) return exist

        val pair = findOrCreate(uri, true)
        val folder = pair.first!!
        if (pair.second)
            _rootList.add(folder)
        return folder
    }

    private fun removeRoot(uri: String) {
        val uri2 = if (uri.endsWith("/")) uri else "$uri/"
        val u = URI(URLDecoder.decode(uri2, "UTF-8"))
        _rootList.removeIf { folder ->
            if (folder.matchUri(u)) {
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
                client?.publishDiagnostics(PublishDiagnosticsParams(file.uri.toString(), file.diagnostics))
            }
            true
        }
    }

    override fun findFile(uri: String): IVirtualFile? {
        val u = URI(uri)
        if (u.scheme != "file")
            return null
        val pair = findOrCreate(u.resolve(""), false)
        val root = pair.first
        return root?.findFile(File(u.path).name)
    }

    override fun addFile(file: File, text: String?): ILuaFile {
        val uri = file.toURI()
        val pair = findOrCreate(uri.resolve(""), true)
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