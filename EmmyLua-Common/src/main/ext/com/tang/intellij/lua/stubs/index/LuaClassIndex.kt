package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.StubKeys

class LuaClassIndex : StubIndex<String, LuaDocTagClass>() {

    override fun getKey() = StubKeys.CLASS

    companion object {
        val instance = LuaClassIndex()

        fun find(name: String, context: SearchContext): LuaDocTagClass? {
            return find(name, context.project, context.scope)
        }

        fun find(name: String, project: Project, scope: GlobalSearchScope): LuaDocTagClass? {
            var def: LuaDocTagClass? = null
            process(name, project, scope, Processor {
                def = it
                false
            })
            return def
        }

        fun process(key: String, project: Project, scope: GlobalSearchScope, processor: Processor<LuaDocTagClass>): Boolean {
            val collection = instance.get(key, project, scope)
            return ContainerUtil.process(collection, processor)
        }

        fun processKeys(project: Project, processor: Processor<String>): Boolean {
            val scope = GlobalSearchScope.allScope(project)
            return instance.processKeys(project, scope, processor)
        }
    }
}