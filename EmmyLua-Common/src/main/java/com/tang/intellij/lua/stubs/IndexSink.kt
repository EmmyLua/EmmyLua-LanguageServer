package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.stubs.index.LuaClassIndex
import com.tang.intellij.lua.stubs.index.LuaClassMemberIndex
import com.tang.intellij.lua.stubs.index.LuaSuperClassIndex

interface IndexSink {
    fun <Psi : PsiElement, K> occurrence(indexKey: IndexId<K, Psi>, key: K, value: Psi)
}

class IndexSinkImpl(val file: LuaPsiFile) : IndexSink {

    override fun <Psi : PsiElement, K> occurrence(indexKey: IndexId<K, Psi>, key: K, value: Psi) {
        when (indexKey) {
            StubKeys.CLASS -> LuaClassIndex.instance.occurrence(file, key, value)
            StubKeys.CLASS_MEMBER -> LuaClassMemberIndex.instance.occurrence(file, key, value)
            StubKeys.SUPER_CLASS -> LuaSuperClassIndex.instance.occurrence(file, key, value)
        }
    }
}