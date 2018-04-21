package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.StubKeys

class LuaClassIndex : StubIndex<LuaDocClassDef>() {

    override val key: IndexId<String, LuaDocClassDef> = StubKeys.CLASS

    companion object {
        val instance = LuaClassIndex()

        fun find(name: String, context: SearchContext): LuaDocClassDef? {
            return find(name, context.project, context.getScope())
        }

        fun find(name: String, project: Project, scope: GlobalSearchScope): LuaDocClassDef? {
            var def: LuaDocClassDef? = null
            process(name, project, scope, Processor {
                def = it
                false
            })
            return def
        }

        fun process(key: String, project: Project, scope: GlobalSearchScope, processor: Processor<LuaDocClassDef>): Boolean {
            val collection = instance.get(key, project, scope)
            return ContainerUtil.process(collection, processor)
        }

        fun processKeys(project: Project, processor: Processor<String>) {
            val scope = GlobalSearchScope.allScope(project)
            instance.processKeys(project, scope, processor)
        }
    }
}