package com.tang.vscode.extendApi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.tang.intellij.lua.psi.LuaParamInfo
import com.tang.intellij.lua.ty.FunSignature
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TySerializedFunction

object ExtendApiService {
    private var rootNamespace: Namespace? = null
    private var namespaceMap: MutableMap<String, Namespace> = mutableMapOf()
    private var classMap: MutableMap<String, ExtendClass> = mutableMapOf()

    fun loadApi(project: Project, api: LuaReportApiParams) {
        val mgr = PsiManager.getInstance(project)
        if (rootNamespace == null) {
            rootNamespace = Namespace("CS", null, mgr, false)
        }

        for (luaClass in api.classes) {
            val classNs = getNamespace(luaClass.namespace)
            if (classNs != null) {
                val classFullName = if (luaClass.namespace.isEmpty()) {
                    luaClass.name
                } else {
                    "${luaClass.namespace}.${luaClass.name}"
                }
                val extendClass = ExtendClass(
                    luaClass.name,
                    classFullName,
                    luaClass.baseClass,
                    classNs,
                    luaClass.comment,
                    luaClass.location,
                    mgr
                )
                classMap[classFullName] = extendClass
                classNs.addMember(extendClass)
                for (luaField in luaClass.fields) {
                    val ty = Ty.create(luaField.typeName)
                    extendClass.addMember(luaField.name, ty, luaField.comment, luaField.location)
                }

                for (luaMethod in luaClass.methods) {
                    val paramList = mutableListOf<LuaParamInfo>()
                    for (param in luaMethod.params) {
                        paramList.add(LuaParamInfo(param, Ty.create("any")))
                    }

                    val retType = Ty.create(luaMethod.returnTypeName)
                    val ty = TySerializedFunction(
                        FunSignature(
                            !luaMethod.isStatic,
                            retType,
                            null,
                            paramList.toTypedArray()
                        ),
                        emptyArray()
                    )
                    extendClass.addMember(luaMethod.name, ty, luaMethod.comment, luaMethod.location)
                }
            }
        }
    }

    fun getExtendClasses(): MutableMap<String, ExtendClass> {
        return classMap
    }

    fun getNsMember(name: String): NsMember? {
        if (name == "_G" || name == "CS") {
            return rootNamespace
        }

        var member: NsMember? = namespaceMap[name]
        if (member != null) {
            return member
        }

        member = classMap[name]
        if (member != null) {
            return member
        }
        return null
    }

    private fun getNamespace(nsName: String): Namespace? {
        if (nsName.isEmpty()) {
            return rootNamespace
        }
        val result = namespaceMap[nsName]
        if (result != null) {
            return result
        }

        var prevNs = rootNamespace
        val nsParts = nsName.split('.')
        for (ns in nsParts) {
            prevNs = prevNs?.getOrPut(ns)
        }
        if (prevNs != null) {
            namespaceMap[nsName] = prevNs
        }

        return prevNs
    }

}