package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.stubs.StubKeys

class LuaSuperClassIndex : StubIndex<String, LuaDocTagClass>() {

    override fun getKey() = StubKeys.SUPER_CLASS

    companion object {
        val instance = LuaSuperClassIndex()

        fun process(s: String, project: Project, scope: GlobalSearchScope, processor: Processor<LuaDocTagClass>): Boolean {
            val c = instance.get(s, project, scope)
            return ContainerUtil.process(c, processor)
        }
    }
}