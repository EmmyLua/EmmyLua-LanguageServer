package com.tang.intellij.lua.psi

import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.tang.intellij.lua.stubs.LuaFileStub

//import com.tang.vscode.api.impl.LuaFile

class LuaPsiFile(private val myNode: ASTNode) : ASTDelegatePsiElement(), PsiFile, LuaTypeGuessable {

    private var virtualFile: VirtualFile? = null

    override fun getVirtualFile(): VirtualFile {
        return virtualFile!!
    }

    fun setVirtualFile(file: VirtualFile) {
        virtualFile = file
    }

    val id = idCount++

    override fun getNode(): ASTNode = myNode

    override fun getParent(): PsiElement? {
        return null
    }

    override fun setName(name: String): PsiElement {
        TODO()
    }

    override fun getName() = virtualFile?.name!!

    override fun getOriginalFile(): PsiFile = this

    override fun isValid(): Boolean {
        return true
    }

    override fun isPhysical(): Boolean {
        return true
    }

    val stub: LuaFileStub? = null

    companion object {
        private var idCount = 0
    }
}