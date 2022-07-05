package com.tang.vscode.extendApi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.light.LightElement
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TyClass
import com.tang.lsp.ExtendApiBase

// intellij-EmmyLua-Unity 抄下来的

class ExtendClassMember(
    val fieldName: String,
    val type: ITy,
    val parent: ExtendClass,
    private val comment: String,
    private val location: String,
    mg: PsiManager
) : LightElement(mg, LuaLanguage.INSTANCE), PsiNamedElement, LuaClassField, ExtendApiBase {
    override fun getComment(): String {
        return comment
    }

    override fun getLocation(): String {
        return location
    }

    override fun toString(): String {
        return fieldName
    }

    override fun guessType(context: SearchContext): ITy {
        return type
    }

    override fun setName(name: String): PsiElement {
        return this
    }

    override fun getName() = fieldName

    override fun guessParentType(context: SearchContext): ITy {
        return parent.type
    }

    override val visibility: Visibility
        get() = Visibility.PUBLIC
    override val isDeprecated: Boolean
        get() = false
}

class TyExtendClass(val clazz: ExtendClass) : TyClass(clazz.fullName, clazz.name, clazz.baseClassName) {
    override fun findMemberType(name: String, searchContext: SearchContext): ITy? {
        return clazz.findMember(name)?.guessType(searchContext)
    }

    override fun findMember(name: String, searchContext: SearchContext): LuaClassMember? {
        return clazz.findMember(name)
    }
}

class ExtendClass(
    className: String,
    val fullName: String,
    val baseClassName: String?,
    parent: Namespace?,
    private val comment: String,
    private val location: String,
    mg: PsiManager
) : NsMember(className, parent, mg) {

    private val ty: ITyClass by lazy { TyExtendClass(this) }

    override val type: ITyClass
        get() = ty

    override fun toString(): String {
        return fullName
    }

    fun addMember(name: String, type: ITy, comment: String = "", location: String) {
        val member = ExtendClassMember(name, type, this, comment, location, manager)
        members.add(member)
    }

    override fun getComment(): String {
        return comment
    }

    override fun getLocation(): String {
        return location
    }
}

abstract class NsMember(
    val memberName: String,
    val parent: Namespace?,
    mg: PsiManager
) : LightElement(mg, LuaLanguage.INSTANCE), PsiNamedElement, LuaClass, LuaClassField, ExtendApiBase {

    val members = mutableListOf<LuaClassMember>()

    override fun setName(name: String): PsiElement {
        return this
    }

    override fun getName(): String {
        return memberName
    }

    override fun guessType(context: SearchContext?): ITy {
        return type
    }

    override fun guessParentType(context: SearchContext): ITy {
        return parent?.type ?: Ty.UNKNOWN
    }

    fun getMember(name: String): LuaClassMember? {
        return members.firstOrNull { it.name == name }
    }

    fun findMember(name: String): LuaClassMember? {
        return members.firstOrNull { it.name == name }
    }

    override val visibility: Visibility
        get() = Visibility.PUBLIC
    override val isDeprecated: Boolean
        get() = false
}

private class NamespaceType(val namespace: Namespace) : TyClass(namespace.fullName) {
    override fun findMemberType(name: String, searchContext: SearchContext): ITy? {
        return namespace.getMember(name)?.guessType(searchContext)
    }

    override fun findMember(name: String, searchContext: SearchContext): LuaClassMember? {
        return namespace.getMember(name)
    }

    override val displayName: String
        get() = namespace.toString()
}

class Namespace(
    val typeName: String,
    parent: Namespace?,
    mg: PsiManager,
    val isValidate: Boolean
) : NsMember(typeName, parent, mg), LuaClass, LuaClassField {

    private val myType by lazy { NamespaceType(this) }
    private val myMembers = mutableMapOf<String, Namespace>()
    private val myClasses = mutableListOf<ExtendClass>()

    val fullName: String
        get() {
            return if (parent == null || !parent.isValidate) typeName else "${parent.fullName}.$typeName"
        }

    fun addMember(ns: String): Namespace {
        val member = Namespace(ns, this, myManager, true)
        myMembers[ns] = member
        members.add(member)
        return member
    }

    fun addMember(clazz: ExtendClass) {
        myClasses.add(clazz)
        members.add(clazz)
    }

    fun getOrPut(ns: String): Namespace {
        val m = myMembers[ns]
        if (m != null) return m
        return addMember(ns)
    }

    override val type: ITyClass
        get() = myType

    override fun toString(): String {
        return "namespace: $fullName"
    }

    override fun getComment(): String {
        return toString()
    }

    override fun getLocation(): String {
        return ""
    }
}