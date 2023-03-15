package com.tang.vscode.extendApi

import com.intellij.openapi.project.Project
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.psi.LuaClass
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass

class ExtendShortNameManager : LuaShortNamesManager() {
    private fun findClass(name: String): NsMember? {
        return ExtendApiService.getNsMember(name)
    }

    override fun findClass(name: String, context: SearchContext): LuaClass? {
        return findClass(name)
    }

    override fun findMember(type: ITyClass, fieldName: String, context: SearchContext): LuaClassMember? {
        val clazz = findClass(type.className) ?: return null
        return clazz.findMember(fieldName)
    }

    override fun processAllClassNames(project: Project, processor: Processor<String>): Boolean {
        val classes = ExtendApiService.getExtendClasses()
        for ((_, clazz) in classes) {
            if (!processor.process(clazz.fullName))
                return false
        }
        return true
    }

    override fun processClassesWithName(name: String, context: SearchContext, processor: Processor<LuaClass>): Boolean {
        return findClass(name, context)?.let { processor.process(it) } ?: true
    }

    override fun getClassMembers(clazzName: String, context: SearchContext): Collection<LuaClassMember> {
        val clazz = ExtendApiService.getNsMember(clazzName)
        if (clazz != null) {
            return clazz.members
        }
        return emptyList()
    }

    private fun processAllMembers(
        type: String,
        fieldName: String,
        context: SearchContext,
        processor: Processor<LuaClassMember>,
        deep: Boolean = true
    ): Boolean {
        val clazz = ExtendApiService.getNsMember(type) ?: return true
        val continueProcess = ContainerUtil.process(clazz.members.filter { it.name == fieldName }, processor)
        if (!continueProcess)
            return false

        if (clazz is ExtendClass) {
            val baseType = clazz.baseClassName
            if (deep && baseType != null) {
                return processAllMembers(baseType, fieldName, context, processor, deep)
            }
        }

        return true
    }

    override fun processAllMembers(
        type: ITyClass,
        fieldName: String,
        context: SearchContext,
        processor: Processor<LuaClassMember>
    ): Boolean {
        return processAllMembers(type.className, fieldName, context, processor)
    }
}