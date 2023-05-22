package com.tang.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import java.io.File
import java.net.URI

interface IWorkspace {
    fun addFile(file: File, text: String? = null, force: Boolean = false): ILuaFile?
    fun findFile(uri: String): IVirtualFile?
    fun findLuaFile(uri: String): ILuaFile?
    fun removeFile(uri: String)
    fun removeFileIfNeeded(uri: String)
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
    val uri: FileURI
    val parent: IFolder
    fun matchUri(uri: URI): Boolean
}

data class Word(val hashCode: Int, val start: Int, val end: Int)

interface ILuaFile : IVirtualFile {
    fun getText(): CharSequence
    val psi: PsiFile?
    fun unindex()
    fun getLine(offset: Int): Pair<Int, Int>
    fun didChange(params: DidChangeTextDocumentParams)
    fun getPosition(line:Int, char: Int): Int
    fun processWords(processor: (w: Word) -> Boolean)
    fun lock(code: () -> Unit)
}