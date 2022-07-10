package com.tang.vscode.extendApi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.light.LightElement
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.lsp.ExtendApiBase


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

class TyExtendClass(val clazz: ExtendClass) : TyClass(
    clazz.fullName,
    clazz.name,
    clazz.baseClassName,
    listOf(),
    clazz.isInterface,
    clazz.isEnum
) {
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
    private val attribute: String,
    mg: PsiManager
) : NsMember(className, parent, mg) {

    private val ty: ITyClass by lazy { TyExtendClass(this) }
    private val methods = mutableMapOf<String, MutableList<IFunSignature>>()

    override val type: ITyClass
        get() = ty

    override fun toString(): String {
        return fullName
    }

    fun addMember(name: String, type: ITy, comment: String, location: String) {
        val member = ExtendClassMember(name, type, this, comment, location, manager)
        members.add(member)
    }

    fun addMethod(name: String, signature: FunSignature) {
        if (!methods.containsKey(name)) {
            val ty = TyExtendFunction(this, name)
            methods[name] = mutableListOf(signature)
            addMember(name, ty, comment, location)
        } else {
            methods[name]?.add(signature)
        }
    }

    fun getMethods(name: String): MutableList<IFunSignature>? {
        return methods[name]
    }

    val isEnum: Boolean
        get() = attribute == "enum"

    val isInterface: Boolean
        get() = attribute == "interface"

    val isDelegate: Boolean
        get() = attribute == "delegate"

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

class TyExtendFunction(
    private val clazz: ExtendClass,
    val name: String
) : TyFunction() {
    override val mainSignature: IFunSignature
        get() {
            val methods = clazz.getMethods(name)
            if (methods != null && methods.size >= 1) {
                return methods.first()
            }
            return FunSignature(true, Ty.create("void"), null, emptyArray())
        }

    override val signatures: Array<IFunSignature>
        get() {
            val methods = clazz.getMethods(name)
            if (methods != null) {
                return methods.toTypedArray()
            }
            return emptyArray()
        }
}