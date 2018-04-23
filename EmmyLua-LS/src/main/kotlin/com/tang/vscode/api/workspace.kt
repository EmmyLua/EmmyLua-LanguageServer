package com.tang.vscode.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import java.net.URI

interface IWorkspace : IFolder {
    val project: Project
}

interface IFolder : IVirtualFile {
    fun addFile(file: IVirtualFile)
    fun addFile(uri: String, text: String): ILuaFile
    fun removeFile(file: IVirtualFile)
    fun findFile(uri: String): IVirtualFile?
    fun getFile(name: String, recursive: Boolean = false): IVirtualFile?
    fun walkFiles(processor: (f: ILuaFile) -> Boolean): Boolean
}

interface IVirtualFile {
    val isFolder: Boolean
    fun getName(): String
    val uri: URI
    val workspace: IWorkspace
    val parent: IFolder
    fun matchUri(uri: String): Boolean
}

interface ILuaFile : IVirtualFile {
    fun getText(): String
    val diagnostics: List<Diagnostic>
    val psi: PsiFile?
    fun getLine(offset: Int): Pair<Int, Int>
    fun didChange(params: DidChangeTextDocumentParams)
    fun getPosition(line:Int, char: Int): Int
}