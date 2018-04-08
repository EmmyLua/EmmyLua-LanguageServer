package com.tang.intellij.lua.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.tang.intellij.lua.psi.LuaClassField
import com.tang.intellij.lua.psi.Visibility
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy

abstract class LuaNameExprMixin(node: ASTNode) : ASTWrapperPsiElement(node), LuaClassField {
    override val visibility: Visibility = Visibility.PUBLIC

    override fun guessParentType(context: SearchContext): ITy {
        TODO("not implemented")
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

    override fun getReference(): PsiReference? {
        return references.firstOrNull()
    }
}