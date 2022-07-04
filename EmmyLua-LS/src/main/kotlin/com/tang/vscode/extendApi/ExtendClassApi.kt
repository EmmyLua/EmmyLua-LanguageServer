package com.tang.vscode.extendApi

open class LuaApiBase(val Name: String = "", val Comment: String = "", val Location: String = "")

data class LuaApiField(val TypeName: String = "") : LuaApiBase()

data class LuaApiMethod(
    val ReturnTypeName: String = "",
    val TypeName: String = "",
    val IsStatic: Boolean = false,
    val Params: Array<String> = arrayOf()
) : LuaApiBase()

data class LuaApiClass(
    val namespace: String = "",
    val BaseClass: String = "",
    val Fields: Array<LuaApiField> = arrayOf(),
    val Methods: Array<LuaApiMethod> = arrayOf()
) : LuaApiBase()

class LuaReportApiParams{
    val classes: Array<LuaApiClass> = arrayOf()
}