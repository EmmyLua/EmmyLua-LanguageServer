package com.tang.intellij.lua.psi

import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Processor
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.stubs.IndexSink
import com.tang.vscode.api.impl.LuaFile

class LuaPsiFile(private val myNode: ASTNode) : ASTDelegatePsiElement(), PsiFile {

    var virtualFile: LuaFile? = null

    override fun getNode(): ASTNode = myNode

    override fun getParent(): PsiElement? {
        return null
    }

    override fun setName(name: String): PsiElement {
        TODO()
    }

    override fun getName() = virtualFile?.name

    override fun getOriginalFile(): PsiFile = this

    override fun isValid(): Boolean {
        return true
    }

    override fun isPhysical(): Boolean {
        return true
    }

    private val indexMap = mutableMapOf<String, MutableMap<String, MutableList<PsiElement>>>()
    private val sink = IndexSinkImpl()

    inner class IndexSinkImpl : IndexSink {
        override fun <Psi : PsiElement> processKeys(indexKey: IndexId<String, Psi>,processor: Processor<String>) {
            indexMap[indexKey.name]?.let {
                val e = it.iterator()
                while (e.hasNext() && processor.process(e.next().key));
            }
        }

        override fun <Psi : PsiElement> process(indexKey: IndexId<String, Psi>, key: String, processor: Processor<Psi>) {
            indexMap[indexKey.name]?.let {
                it[key]?.let { list ->
                    val e = list.iterator()
                    while (e.hasNext() && processor.process(e.next() as Psi));
                }
            }
        }

        override fun <Psi : PsiElement> occurrence(indexKey: IndexId<String, Psi>, key: String, value: Psi) {
            val map = indexMap.getOrPut(indexKey.name) { mutableMapOf() }
            val list = map.getOrPut(key) { mutableListOf() }
            list.add(value)
        }
    }

    fun getSink(): IndexSink {
        return sink
    }
}