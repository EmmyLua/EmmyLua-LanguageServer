package com.tang.vscode

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.util.Processor
import com.tang.vscode.api.IFolder
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.IVirtualFile
import com.tang.vscode.api.IWorkspace
import com.tang.vscode.api.impl.Folder
import com.tang.vscode.utils.safeURIName
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
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
                else -> { }
            }
        }
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        TODO()
    }

    override fun didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams) {
        didChangeWorkspaceFolders2(params)
    }

    @JsonNotification("emmy/didChangeWorkspaceFolders")
    private fun didChangeWorkspaceFolders2(params: DidChangeWorkspaceFoldersParams) {
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
        _rootList.removeIf {
            if (it.matchUri(u)) {
                it.walkFiles {
                    it.unindex()
                    true
                }
                it.parent.removeFile(it)
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
        val allFiles = mutableListOf<File>()
        val arr = _rootWSFolders.toTypedArray()
        _rootWSFolders.clear()
        arr.forEach { uri->
            val folder = File(uri.path)
            collectFiles(folder, allFiles)
        }

        allFiles.forEachIndexed { index, file ->
            addFile(file)
            monitor.setProgress("Emmy load file: ${file.canonicalPath}", (index  + 1) / allFiles.size.toFloat())
        }
        monitor.done()
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
        val path = safeURIName(file.invariantSeparatorsPath)
        val uri = URI("file:///$path")
        val pair = findOrCreate(uri.resolve(""), true)
        val root = pair.first!!
        return root.addFile(file.name, text ?: LoadTextUtil.getTextByBinaryPresentation(file.readBytes()))
    }

    private fun addFile(uri: String) {
        val u = URI(uri)
        addFile(File(u.path))
    }

    override fun removeFile(uri: String) {
        val file = findFile(uri) as? ILuaFile
        file?.let {
            it.parent.removeFile(it)
            it.unindex()
        }
    }

    fun connect(client: LuaLanguageClient) {
        this.client = client
    }
}