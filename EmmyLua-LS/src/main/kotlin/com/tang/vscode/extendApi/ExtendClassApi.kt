package com.tang.vscode.extendApi

open class LuaApiBase(val name: String = "", val comment: String = "", val location: String = "")

data class LuaApiField(val typeName: String) : LuaApiBase()

data class LuaParam(val name: String, val typeName: String)

data class LuaApiMethod(
    val returnTypeName: String,
    val typeName: String,
    val isStatic: Boolean,
    val params: List<LuaParam>
) : LuaApiBase()

data class LuaApiClass(
    val namespace: String,
    val baseClass: String,
    val fields: List<LuaApiField>,
    val methods: List<LuaApiMethod>
) : LuaApiBase()

data class LuaReportApiParams(
    val classes: List<LuaApiClass>
)