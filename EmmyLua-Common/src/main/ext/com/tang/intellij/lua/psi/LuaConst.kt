package com.tang.intellij.lua.psi

import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaConstIndex

object LuaConst {

    fun isConstField(className: String, fieldName: String, context: SearchContext): Boolean{
        return LuaConstIndex.instance.isConst(className, fieldName, context)
    }

    fun isConstGlobal(name: String, context: SearchContext): Boolean{
        return LuaConstIndex.instance.isConst(Constants.WORD_G, name, context)
    }

    fun isConstLocal(filePath: String, name: String, context: SearchContext): Boolean{
        return LuaConstIndex.instance.isConstLocal(filePath, name, context)
    }

}