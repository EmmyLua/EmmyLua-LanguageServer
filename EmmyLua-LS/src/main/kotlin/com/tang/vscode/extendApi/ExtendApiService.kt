package com.tang.vscode.extendApi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaParamInfo
import com.tang.intellij.lua.ty.FunSignature
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TySerializedFunction

object ExtendApiService {
    private var rootNamespaceName = "_G"
    private var rootNamespace: Namespace? = null
    private var namespaceMap: MutableMap<String, Namespace> = mutableMapOf()
    private var classMap: MutableMap<String, ExtendClass> = mutableMapOf()

    fun loadApi(project: Project, api: LuaReportApiParams) {
        val mgr = PsiManager.getInstance(project)
        rootNamespaceName = api.root.ifEmpty {
            "_G"
        }
        rootNamespace = Namespace(api.root, null, mgr, false)
        namespaceMap.clear()
        classMap.clear()

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
                    luaClass.attribute,
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
                        paramList.add(LuaParamInfo(param.name, Ty.create(param.typeName)))
                    }

                    val retType = Ty.create(luaMethod.returnTypeName)
                    val signature = FunSignature(
                        !luaMethod.isStatic,
                        retType,
                        null,
                        paramList.toTypedArray(),
                        emptyArray(),
                        luaMethod.comment
                    )
                    extendClass.addMethod(luaMethod.name, signature, luaMethod.comment, luaMethod.location)
                }
            }
        }
    }

    fun getExtendClasses(): MutableMap<String, ExtendClass> {
        return classMap
    }

    fun getNsMember(name: String): NsMember? {
        if (name == rootNamespaceName) {
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
        // generic workaround
        val genericIndex = name.indexOf('<')
        if (genericIndex != -1) {
            val baseTypeName = name.substring(0, genericIndex)
            return classMap[baseTypeName]
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

    fun findMember(clazz: String, fieldName: String, deep: Boolean): LuaClassMember? {
        val nsMember = getNsMember(clazz)
        if (nsMember != null) {
            val member = nsMember.findMember(fieldName);
            if (member != null) {
                return member
            }
            if (nsMember is ExtendClass && deep) {
                val superClass = nsMember.baseClassName
                if (superClass != null) {
                    return findMember(superClass, fieldName, deep)
                }
            }
        }
        return null
    }

}