package com.tang.vscode.vscode

import com.tang.vscode.api.IWorkspace
import com.tang.vscode.api.impl.Workspace
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.services.WorkspaceService
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaWorkspaceService : WorkspaceService {
    private var _root: String = ""
    private val _wsList = mutableListOf<IWorkspace>()
    private val _rootWSFolders = mutableListOf<String>()

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {

    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        TODO()
    }

    var root: String
        get() = _root
        set(value) {
            _root = value
            addWorkspace(value)
        }

    private fun eachWorkspace(processor: (ws: IWorkspace) -> Boolean) {
        for (ws in _wsList) {
            if (!processor(ws))
                break
        }
    }

    fun getWorkspace(uri: String): IWorkspace {
        var ws: IWorkspace? = null
        eachWorkspace {
            if (it.matchUri(uri)) {
                ws = it
                return@eachWorkspace false
            }
            true
        }
        return ws ?: addWorkspace(uri)
    }

    private fun addWorkspace(uri: String): IWorkspace {
        val ws = Workspace(URI(uri))
        _wsList.add(ws)
        return ws
    }

    fun addRoot(uri: String) {
        _rootWSFolders.add(uri)
    }

    fun loadWorkspace(monitor: IProgressMonitor) {
        val workspace = getWorkspace(root)
        val allFiles = mutableListOf<File>()
        val arr = _rootWSFolders.toTypedArray()
        _rootWSFolders.clear()
        arr.forEach { uri->
            val u = URI(uri)
            val folder = File(u.path)
            allFiles.addAll(folder.listFiles().filter { it.isFile && it.extension == "lua" })
        }

        allFiles.forEachIndexed { index, file ->
            val uri = URI("file:///${file.invariantSeparatorsPath}")
            workspace.addFile(uri.toString(), file.readText())
            monitor.setProgress("Emmy load file: ${file.canonicalPath}", (index  + 1) / allFiles.size.toFloat())
        }
        monitor.done()
    }
}