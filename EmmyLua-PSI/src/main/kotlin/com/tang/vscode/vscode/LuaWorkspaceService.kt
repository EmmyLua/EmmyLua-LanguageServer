package com.tang.vscode.vscode

import com.tang.vscode.api.IWorkspace
import com.tang.vscode.api.impl.Workspace
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.services.WorkspaceService
import java.net.URI
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaWorkspaceService : WorkspaceService {
    private var _root: String = ""
    private val _wsList = mutableListOf<IWorkspace>()

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

    fun eachWorkspace(processor: (ws: IWorkspace) -> Boolean) {
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
}