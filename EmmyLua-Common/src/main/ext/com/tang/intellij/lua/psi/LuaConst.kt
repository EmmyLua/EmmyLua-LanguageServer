package com.tang.intellij.lua.psi

import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaConstIndex

object LuaConst {

    fun isConst(className: String, fieldName: String, context: SearchContext): Boolean{
        return LuaConstIndex.instance.isConst(className, fieldName, context)
    }

    fun isConst(name: String, context: SearchContext): Boolean{
        return LuaConstIndex.instance.isConst(Constants.WORD_G, name, context)
    }



}