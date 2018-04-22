package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.stubs.index

abstract class StubIndex<K, Psi : PsiElement> {

    abstract fun getKey(): IndexId<K, Psi>

    private var lock = false

    private val indexMap = mutableMapOf<Int, MutableMap<K, MutableList<PsiElement>>>()

    fun get(key: K, project: Project, scope: GlobalSearchScope): MutableList<Psi> {
        val list = mutableListOf<Psi>()
        if (lock)
            return list
        project.process { file ->
            if (file is LuaPsiFile) {
                lock = true
                index(file)
                lock = false

                indexMap[file.id]?.let {
                    it[key]?.forEach {
                        list.add(it as Psi)
                    }
                }
            }
            true
        }
        return list
    }

    fun processKeys(project: Project, scope: GlobalSearchScope, processor: Processor<K>) {
        if (lock)
            return
        project.process { file ->
            var continueRun = true
            if (file is LuaPsiFile) {
                lock = true
                index(file)
                lock = false

                indexMap[file.id]?.let {
                    val iter = it.keys.iterator()
                    while (iter.hasNext() && continueRun)
                        continueRun = processor.process(iter.next())
                }
            }
            continueRun
        }
    }

    fun <Psi : PsiElement, K1> occurrence(file: LuaPsiFile, key: K1, value: Psi) {
        val map = indexMap.getOrPut(file.id) { mutableMapOf() }
        val list = map.getOrPut(key as K) { mutableListOf() }
        list.add(value)
    }

    fun removeStubs(file: LuaPsiFile) {
        indexMap.remove(file.id)
    }
}