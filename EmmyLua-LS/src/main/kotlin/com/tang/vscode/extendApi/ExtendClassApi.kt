package com.tang.vscode.extendApi

open class LuaApiBase(val name: String = "", open val comment: String = "", open val location: String = "")

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
    val attribute: String,
    val fields: List<LuaApiField>,
    val methods: List<LuaApiMethod>
) : LuaApiBase()

data class LuaReportApiParams(
    val classes: List<LuaApiClass>,
    val root: String
)
