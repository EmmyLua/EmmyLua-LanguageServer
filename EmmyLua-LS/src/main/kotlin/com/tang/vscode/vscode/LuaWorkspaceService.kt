package com.tang.vscode.vscode

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.util.Processor
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
    private var _root: String = ""
    private val _rootList = mutableListOf<WorkspaceRoot>()
    private val _rootWSFolders = mutableListOf<String>()

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

    var root: String
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

    private fun getWSRoot(uri: String): WorkspaceRoot {
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

    private fun addWSRoot(uri: String): WorkspaceRoot {
        val ws = WorkspaceRoot(URI(uri))
        _rootList.add(ws)
        return ws
    }

    fun addRoot(uri: String) {
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
            val u = URI(uri)
            val folder = File(u.path)
            collectFiles(folder, allFiles)
        }

        allFiles.forEachIndexed { index, file ->
            val uri = URI("file:///${file.invariantSeparatorsPath}")
            wsRoot.addFile(uri.toString(), file.readText())
            monitor.setProgress("Emmy load file: ${file.canonicalPath}", (index  + 1) / allFiles.size.toFloat())
        }
        monitor.done()
    }

    private fun getParentUri(uri: String): String {
        val last = uri.lastIndexOf('/')
        return uri.substring(0, last)
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