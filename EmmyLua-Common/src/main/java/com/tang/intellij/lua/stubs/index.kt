@file:Suppress("UNUSED_PARAMETER", "unused")

package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.TyUnion
import com.tang.intellij.lua.ty.getDocTableTypeName
import com.tang.intellij.lua.ty.getTableTypeName

fun index(file: LuaPsiFile) {
    if (file.indexed || file.indexing)
        return
    file.indexing = true
    file.indexed = true
    val sink = IndexSinkImpl(file)
    indexImpl(file, sink)
    file.indexing = false
}

private fun indexImpl(file: LuaPsiFile, sink: IndexSink) {
    file.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            if (element is LuaPsiElement)
                index(element, sink)
        }
    })
}

private fun index(psi: LuaPsiElement, sink: IndexSink) {
    when (psi) {
        is LuaDocTagClass -> index(psi, sink)
        is LuaDocTagField -> index(psi, sink)
        is LuaDocTableField -> index(psi, sink)
        is LuaDocTagAlias -> index(psi, sink)
        is LuaClassMethodDef -> index(psi, sink)
        is LuaIndexExpr -> index(psi, sink)
        is LuaTableExpr -> index(psi, sink)
        is LuaTableField -> index(psi, sink)
        is LuaNameExpr -> index(psi, sink)
        is LuaFuncDef -> index(psi, sink)
    }
}

private fun index(doc: LuaDocTagClass, sink: IndexSink) {
    val name = doc.type.className
    sink.occurrence(StubKeys.CLASS, name, doc)
    sink.occurrence(StubKeys.SHORT_NAME, name, doc)
    val superClassName = doc.type.superClassName
    if (superClassName != null) {
        sink.occurrence(StubKeys.SUPER_CLASS, superClassName, doc)
    }
}

private fun index(field: LuaDocTagField, sink: IndexSink) {
    var name = field.name
    if(name == null && field.fieldIndex != null){
        name =  "[${field.fieldIndex?.text}]"
    }
    if (name != null) {
        var className: String? = null

        val classRef = field.classNameRef
        if (classRef != null) {
            className = classRef.id.text
        } else {
            val comment = LuaCommentUtil.findContainer(field)
            val classDef = comment.tagClass
            if (classDef != null) {
                className = classDef.name
            }
        }

        if (className != null) {
            sink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode(), field)
            sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name".hashCode(), field)
            sink.occurrence(StubKeys.SHORT_NAME, name, field)
        }
    }
}

private fun index(field: LuaDocTableField, sink: IndexSink) {
    val name = field.name
    val p = field.parent as LuaDocTableDef
    val className = getDocTableTypeName(p)

    sink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode(), field)
    sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name".hashCode(), field)
    sink.occurrence(StubKeys.SHORT_NAME, name, field)
}

private fun index(alias: LuaDocTagAlias, sink: IndexSink) {
    val name = alias.name
    if (name != null)
        sink.occurrence(StubKeys.ALIAS, name, alias)
}

private fun index(methodDef: LuaClassMethodDef, sink: IndexSink) {
    val methodName = methodDef.classMethodName
    val id = methodDef.nameIdentifier ?: return
    val expr = methodName.expr
    val classNameSet = mutableSetOf<String>()

    val searchContext = SearchContext.get(methodDef.project)
    val ty = expr.guessType(searchContext)
    TyUnion.each(ty) {
        if (it is ITyClass)
            classNameSet.add(it.className)
    }
    if (classNameSet.isEmpty())
        classNameSet.add(expr.text)

    /*val isStatic = methodName.dot != null
    val visibility = methodDef.visibility
    val retDocTy = methodDef.comment?.returnDef?.type
    val params = methodDef.params
    val overloads = methodDef.overloads*/

    val name = id.text
    classNameSet.forEach {className ->
        sink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode(), methodDef)
        sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name".hashCode(), methodDef)
        sink.occurrence(StubKeys.SHORT_NAME, className, methodDef)
    }
}

private fun index(indexExpr: LuaIndexExpr, sink: IndexSink) {
    indexExpr.assignStat ?: return
    val name = indexExpr.name ?: return

    val context = SearchContext.get(indexExpr.project)
    val ty = indexExpr.guessParentType(context)
    val classNameSet = mutableSetOf<String>()
    TyUnion.each(ty) {
        if (it is ITyClass)
            classNameSet.add(it.className)
    }

    classNameSet.forEach { className ->
        sink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode(), indexExpr)
        sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name".hashCode(), indexExpr)
        sink.occurrence(StubKeys.CONST, "$className*$name".hashCode(), indexExpr)
        sink.occurrence(StubKeys.SHORT_NAME, name, indexExpr)
    }
}

private fun index(tableExpr: LuaTableExpr, sink: IndexSink) {
    /*val name = getTableTypeName(tableExpr)
    sink.occurrence(StubKeys.CLASS, name, tableExpr)*/
}

private fun index(tableField: LuaTableField, sink: IndexSink) {
    val name = tableField.name ?: return
    val className = findTableExprTypeName(tableField) ?: return

    sink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode(), tableField)
    sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name".hashCode(), tableField)
    sink.occurrence(StubKeys.SHORT_NAME, name, tableField)
    sink.occurrence(StubKeys.CONST, "$className*$name".hashCode(), tableField)
}

private fun findTableExprTypeName(field: LuaTableField): String? {
    val table = PsiTreeUtil.getParentOfType(field, LuaTableExpr::class.java)
    val p1 = table?.parent as? LuaExprList
    val p2 = p1?.parent as? LuaAssignStat
    var ty: String? = null
    if (p2 != null) {
        val type = p2.getExprAt(0)?.guessType(SearchContext.get(p2.project))
        if (type != null) {
            ty = TyUnion.getPerfectClass(type)?.className
        }
    }
    if (ty == null && table != null)
        ty = getTableTypeName(table)
    return ty
}

private fun index(luaNameExpr: LuaNameExpr, sink: IndexSink) {
    luaNameExpr.assignStat ?: return
    val name = luaNameExpr.name
    //val module = if (psiFile is LuaPsiFile) psiFile.moduleName ?: Constants.WORD_G else Constants.WORD_G
    val isGlobal = resolveLocal(luaNameExpr, SearchContext.get(luaNameExpr.project)) == null
    if (isGlobal) {
        sink.occurrence(StubKeys.CLASS_MEMBER, Constants.WORD_G.hashCode(), luaNameExpr)
        sink.occurrence(StubKeys.CLASS_MEMBER, "${Constants.WORD_G}*$name".hashCode(), luaNameExpr)
        sink.occurrence(StubKeys.SHORT_NAME, name, luaNameExpr)
        sink.occurrence(StubKeys.CONST, "${Constants.WORD_G}*$name".hashCode(), luaNameExpr)
    }
    else{
        sink.occurrence(StubKeys.CONST, "${luaNameExpr.containingFile.virtualFile.path}*$name".hashCode(), luaNameExpr)
    }
}

private fun index(funcDef: LuaFuncDef, sink: IndexSink) {
    val nameRef = funcDef.nameIdentifier ?: return
    val moduleName = Constants.WORD_G
    /*val file = funcDef.containingFile
    if (file is LuaPsiFile) moduleName = file.moduleName ?: Constants.WORD_G
    val retDocTy = funcDef.comment?.returnDef?.type
    val params = funcDef.params
    val overloads = funcDef.overloads*/

    sink.occurrence(StubKeys.CLASS_MEMBER, moduleName.hashCode(), funcDef)
    sink.occurrence(StubKeys.CLASS_MEMBER, "$moduleName*${nameRef.text}".hashCode(), funcDef)
    sink.occurrence(StubKeys.SHORT_NAME, nameRef.text, funcDef)
}
