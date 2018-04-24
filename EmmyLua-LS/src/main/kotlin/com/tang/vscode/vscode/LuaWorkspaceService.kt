package com.tang.vscode.vscode

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.util.Processor
import com.tang.vscode.api.IFolder
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.IVirtualFile
import com.tang.vscode.api.IWorkspace
import com.tang.vscode.api.impl.WorkspaceRoot
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
class LuaWorkspaceService : WorkspaceService, IWorkspace {
    private var _root: URI = URI("file://")
    private val _rootList = mutableListOf<WorkspaceRoot>()
    private val _rootWSFolders = mutableListOf<URI>()
    private val _baseFolders = mutableListOf<IFolder>()

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

    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        TODO()
    }

    var root: URI
        get() = _root
        set(value) {
            _root = value
            addWSRoot(value)
        }

    private fun eachWorkspace(processor: (ws: WorkspaceRoot) -> Boolean) {
        for (ws in _rootList) {
            if (!processor(ws))
                break
        }
    }

    private fun getWSRoot(uri: URI): WorkspaceRoot {
        var ws: WorkspaceRoot? = null
        eachWorkspace {
            if (it.matchUri(uri)) {
                ws = it
                return@eachWorkspace false
            }
            true
        }
        return ws ?: addWSRoot(uri)
    }

    private fun addWSRoot(uri: URI): WorkspaceRoot {
        val ws = WorkspaceRoot(uri)
        _rootList.add(ws)
        return ws
    }

    fun addRoot(uri: String) {
        addRoot(URI(uri))
    }

    fun addRoot(uri: URI) {
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

    fun loadWorkspace(monitor: IProgressMonitor) {
        val wsRoot = getWSRoot(root)
        val allFiles = mutableListOf<File>()
        val arr = _rootWSFolders.toTypedArray()
        _rootWSFolders.clear()
        arr.forEach { uri->
            val folder = File(uri.path)
            collectFiles(folder, allFiles)
        }

        allFiles.forEachIndexed { index, file ->
            val uri = URI("file:///${file.invariantSeparatorsPath}")
            wsRoot.addFile(uri.toString(), file.readText())
            monitor.setProgress("Emmy load file: ${file.canonicalPath}", (index  + 1) / allFiles.size.toFloat())
        }
        monitor.done()
    }

    private fun getParentUri(uri: String): URI {
        val u = URI(uri)
        return u.resolve("..")
    }

    override fun findFile(uri: String): IVirtualFile? {
        val root = getWSRoot(getParentUri(uri))
        return root.findFile(uri)
    }

    override fun addFile(uri: String, text: String): ILuaFile {
        val root = getWSRoot(getParentUri(uri))
        return root.addFile(uri, text)
    }
}