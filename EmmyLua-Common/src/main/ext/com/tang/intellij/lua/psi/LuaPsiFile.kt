package com.tang.intellij.lua.psi

import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.tang.intellij.lua.stubs.LuaFileStub

class LuaPsiFile(private val myNode: ASTNode) : ASTDelegatePsiElement(), PsiFile, LuaTypeGuessable, LuaDeclarationScope {

    private var virtualFile: VirtualFile? = null

    var indexed = false
    var indexing = false

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

    val fileElement: ASTNode? = node

    override fun getModificationStamp(): Long {
        return 0
    }

    val isContentsLoaded: Boolean get() = true

    companion object {
        private var idCount = 0
    }
}