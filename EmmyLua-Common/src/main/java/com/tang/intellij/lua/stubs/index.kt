package com.tang.intellij.lua.stubs

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.comment.psi.LuaDocFieldDef
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.TyUnion
import com.tang.intellij.lua.ty.getTableTypeName

private val ALREADY_INDEXED = Key.create<Boolean>("lua.indexing.already_indexed")
private val IN_PROGRESS = Key.create<Boolean>("lua.indexing.in_progress")

fun index(file: LuaPsiFile) {
    if (ALREADY_INDEXED.get(file) == true)
        return
    if (IN_PROGRESS.get(file) == true)
        return
    file.putUserData(ALREADY_INDEXED, true)
    file.putUserData(IN_PROGRESS, true)
    index(file, file.getSink())
    file.putUserData(IN_PROGRESS, false)
}

private fun index(file: LuaPsiFile, sink: IndexSink) {
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
        is LuaDocClassDef -> index(psi, sink)
        is LuaDocFieldDef -> index(psi, sink)
        is LuaClassMethodDef -> index(psi, sink)
        is LuaIndexExpr -> index(psi, sink)
        is LuaTableExpr -> index(psi, sink)
        is LuaTableField -> index(psi, sink)
        is LuaNameExpr -> index(psi, sink)
        is LuaFuncDef -> index(psi, sink)
    }
}

private fun index(doc: LuaDocClassDef, sink: IndexSink) {
    sink.occurrence(StubKeys.CLASS, doc.type.className, doc)
}

private fun index(field: LuaDocFieldDef, sink: IndexSink) {
    val name = field.name
    if (name != null) {
        var className: String? = null

        val classRef = field.classNameRef
        if (classRef != null) {
            className = classRef.id.text
        } else {
            val comment = LuaCommentUtil.findContainer(field)
            val classDef = comment.classDef
            if (classDef != null) {
                className = classDef.name
            }
        }

        if (className != null) {
            sink.occurrence(StubKeys.CLASS_MEMBER, className, field)
            sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name", field)
        }
    }
}

private fun index(methodDef: LuaClassMethodDef, sink: IndexSink) {
    val methodName = methodDef.classMethodName
    val id = methodDef.nameIdentifier ?: return
    val expr = methodName.expr
    val classNameSet = mutableSetOf<String>()

    val searchContext = SearchContext(methodDef.project, methodDef.containingFile, true)
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
        sink.occurrence(StubKeys.CLASS_MEMBER, className, methodDef)
        sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name", methodDef)
    }
}

private fun index(indexExpr: LuaIndexExpr, sink: IndexSink) {
    indexExpr.assignStat ?: return
    val name = indexExpr.name ?: return

    val context = SearchContext(indexExpr.project, indexExpr.containingFile, true)
    val ty = indexExpr.guessParentType(context)
    val classNameSet = mutableSetOf<String>()
    TyUnion.each(ty) {
        if (it is ITyClass)
            classNameSet.add(it.className)
    }
    context.forStore = false

    classNameSet.forEach { className ->
        sink.occurrence(StubKeys.CLASS_MEMBER, className, indexExpr)
        sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name", indexExpr)
    }
}

private fun index(tableExpr: LuaTableExpr, sink: IndexSink) {
    /*val name = getTableTypeName(tableExpr)
    sink.occurrence(StubKeys.CLASS, name, tableExpr)*/
}

private fun index(tableField: LuaTableField, sink: IndexSink) {
    val name = tableField.name ?: return
    val className = findTableExprTypeName(tableField) ?: return

    sink.occurrence(StubKeys.CLASS_MEMBER, className, tableField)
    sink.occurrence(StubKeys.CLASS_MEMBER, "$className*$name", tableField)
}

private fun findTableExprTypeName(field: LuaTableField): String? {
    val table = PsiTreeUtil.getParentOfType(field, LuaTableExpr::class.java)
    val p1 = table?.parent as? LuaExprList
    val p2 = p1?.parent as? LuaAssignStat
    var ty: String? = null
    if (p2 != null) {
        val type = p2.getExprAt(0)?.guessType(SearchContext(p2.project, field.containingFile, true))
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
    val psiFile = luaNameExpr.containingFile
    val name = luaNameExpr.name
    //val module = if (psiFile is LuaPsiFile) psiFile.moduleName ?: Constants.WORD_G else Constants.WORD_G
    val isGlobal = resolveLocal(luaNameExpr, SearchContext(luaNameExpr.project, psiFile)) == null
    if (isGlobal) {
        sink.occurrence(StubKeys.CLASS_MEMBER, Constants.WORD_G, luaNameExpr)
        sink.occurrence(StubKeys.CLASS_MEMBER, "${Constants.WORD_G}*$name", luaNameExpr)
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

    sink.occurrence(StubKeys.CLASS_MEMBER, moduleName, funcDef)
    sink.occurrence(StubKeys.CLASS_MEMBER, "$moduleName*${nameRef.text}", funcDef)
}