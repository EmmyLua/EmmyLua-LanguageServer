package com.tang.intellij.lua.stubs.index

import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaPsiElement
import com.tang.intellij.lua.psi.LuaTableField
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.StubKeys
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.TyParameter

class LuaConstIndex: StubIndex<Int, LuaPsiElement>() {
    override fun getKey() = StubKeys.CONST

    fun isConst(className: String, fieldName: String, context: SearchContext): Boolean {
        val key = "$className*$fieldName"
        return get(key.hashCode(), context.project, context.scope).size == 1
    }

    fun isConstLocal(filePath: String, name: String, context: SearchContext): Boolean{
        val key = "$filePath*$name"
        return get(key.hashCode(), context.project, context.scope).size == 0
    }

    companion object {
        val instance = LuaConstIndex()

        /*fun indexStub(indexSink: IndexSink, className: String, memberName: String) {
            indexSink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode())
            indexSink.occurrence(StubKeys.CLASS_MEMBER, "$className*$memberName".hashCode())
        }*/
    }

}