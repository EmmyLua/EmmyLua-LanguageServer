package com.tang.vscode.api

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import java.io.File
import java.net.URI
import java.nio.file.Path

interface IWorkspace {
    fun addFile(file: File, text: String? = null): ILuaFile
    fun findFile(uri: String): IVirtualFile?
    fun removeFile(uri: String)
    fun eachRoot(processor: (f: IFolder) -> Boolean)
    companion object {
        val KEY = Key.create<IWorkspace>("emmy.workspace")

        fun get(project: Project): IWorkspace {
            return project.getUserData(KEY)!!
        }
    }
}

interface IFolder : IVirtualFile {
    fun addFile(file: IVirtualFile)
    fun addFile(name: String, text: CharSequence): ILuaFile
    fun removeFile(file: IVirtualFile)
    fun removeAll()
    fun findFile(name: String): IVirtualFile?
    fun findFile(vararg names: String): IVirtualFile?
    fun getFile(name: String, recursive: Boolean = false): IVirtualFile?
    fun walkFiles(processor: (f: ILuaFile) -> Boolean): Boolean
    fun createFolder(name: String): IFolder
}

interface IVirtualFile {
    val isFolder: Boolean
    fun getName(): String
    val path: Path
    val uri get() = path.toUri()
    val parent: IFolder
    fun matchUri(uri: URI): Boolean
}

interface ILuaFile : IVirtualFile {
    fun getText(): CharSequence
    val diagnostics: List<Diagnostic>
    val psi: PsiFile?
    fun unindex()
    fun getLine(offset: Int): Pair<Int, Int>
    fun didChange(params: DidChangeTextDocumentParams)
    fun getPosition(line:Int, char: Int): Int
}