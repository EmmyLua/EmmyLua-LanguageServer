package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.stubs.index.*

abstract class IndexSink {
    abstract fun <Psi : PsiElement, K> occurrence(indexKey: IndexId<K, Psi>, key: K, value: Psi)
    companion object {
        fun removeStubs(file: LuaPsiFile) {
            LuaClassIndex.instance.removeStubs(file)
            LuaClassMemberIndex.instance.removeStubs(file)
            LuaSuperClassIndex.instance.removeStubs(file)
            LuaShortNameIndex.removeStubs(file)
            LuaAliasIndex.instance.removeStubs(file)
            LuaConstIndex.instance.removeStubs(file)
        }
    }
}

class IndexSinkImpl(val file: LuaPsiFile) : IndexSink() {

    override fun <Psi : PsiElement, K> occurrence(indexKey: IndexId<K, Psi>, key: K, value: Psi) {
        when (indexKey) {
            StubKeys.CLASS -> LuaClassIndex.instance.occurrence(file, key, value)
            StubKeys.CLASS_MEMBER -> LuaClassMemberIndex.instance.occurrence(file, key, value)
            StubKeys.SUPER_CLASS -> LuaSuperClassIndex.instance.occurrence(file, key, value)
            StubKeys.SHORT_NAME -> LuaShortNameIndex.occurrence(file, key, value)
            StubKeys.ALIAS -> LuaAliasIndex.instance.occurrence(file, key, value)
            StubKeys.CONST -> LuaConstIndex.instance.occurrence(file, key, value)
        }
    }
}