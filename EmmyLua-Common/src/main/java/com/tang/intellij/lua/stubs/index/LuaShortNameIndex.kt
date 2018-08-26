package com.tang.intellij.lua.stubs.index

import com.tang.intellij.lua.psi.LuaPsiElement
import com.tang.intellij.lua.stubs.StubKeys

object LuaShortNameIndex : StubIndex<String, LuaPsiElement>() {
    override fun getKey() = StubKeys.SHORT_NAME
}