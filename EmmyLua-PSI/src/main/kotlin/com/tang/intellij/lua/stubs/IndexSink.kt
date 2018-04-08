package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.intellij.util.indexing.IndexId

interface IndexSink {
    fun <Psi : PsiElement> occurrence(indexKey: IndexId<String, Psi>, key: String, value: Psi)
    fun <Psi : PsiElement> processKeys(indexKey: IndexId<String, Psi>, processor: Processor<String>)
    fun <Psi : PsiElement> process(indexKey: IndexId<String, Psi>, key: String, processor: Processor<Psi>)
}