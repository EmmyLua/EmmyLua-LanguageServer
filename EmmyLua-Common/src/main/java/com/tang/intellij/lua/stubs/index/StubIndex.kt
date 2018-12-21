@file:Suppress("UNUSED_PARAMETER")

package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.psi.LuaPsiFile

abstract class StubIndex<K, Psi : PsiElement> {
    inner class StubFile {
        val elements = mutableListOf<Psi>()
    }
    inner class StubEntry(val key: K) {
        val files = mutableMapOf<Int, StubFile>()
    }

    abstract fun getKey(): IndexId<K, Psi>

    private var lock = false

    private val indexMap = mutableMapOf<K, StubEntry>()

    @Synchronized
    fun get(key: K, project: Project, scope: GlobalSearchScope): MutableList<Psi> {
        val list = mutableListOf<Psi>()
        if (lock)
            return list
        val stubEntry = indexMap[key]
        stubEntry?.files?.forEach { _, u -> list.addAll(u.elements) }

        return list
    }

    @Synchronized
    fun processKeys(project: Project, scope: GlobalSearchScope, processor: Processor<K>): Boolean {
        if (lock)
            return true
        return ContainerUtil.process(indexMap.keys, processor)
    }

    @Synchronized
    fun processValues(project: Project, scope: GlobalSearchScope, processor: Processor<Psi>) {
        for (stubEntry in indexMap.values) {
            for (stubFile in stubEntry.files.values) {
                if (!ContainerUtil.process(stubFile.elements, processor))
                    return
            }
        }
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <Psi1 : PsiElement, K1> occurrence(file: LuaPsiFile, key: K1, value: Psi1) {
        val k = key as K
        val stubEntry = indexMap.getOrPut(k) { StubEntry(k) }
        val stubFile = stubEntry.files.getOrPut(file.id) { StubFile() }
        stubFile.elements.add(value as Psi)
    }

    @Synchronized
    fun removeStubs(file: LuaPsiFile) {
        val iterator = indexMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.files.remove(file.id)
            if (entry.value.files.isEmpty())
                iterator.remove()
        }
    }
}