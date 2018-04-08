package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.stubs.index

abstract class StubIndex<Psi : PsiElement> {

    abstract val key: IndexId<String, Psi>

    fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<Psi> {
        val list = mutableListOf<Psi>()
        project.process {
            if (it is LuaPsiFile) {
                index(it)

                it.getSink().process(this.key, key, Processor {
                    list.add(it)
                    true
                })
            }
            true
        }
        return list
    }

    fun processKeys(project: Project, scope: GlobalSearchScope, processor: Processor<String>) {
        project.process {
            if (it is LuaPsiFile) {
                index(it)

                it.getSink().processKeys(this.key, processor)
            }
            true
        }
    }
}