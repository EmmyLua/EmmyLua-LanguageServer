/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.comment.psi.LuaDocOverloadDef
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.lang.type.LuaString
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

/**
 * 获取所在的位置
 */
fun LuaLocalDef.getIndexFor(psi: LuaNameDef): Int {
    var idx = 0
    val nameList = nameList
    LuaPsiTreeUtilEx.processChildren(nameList, Processor{
        if (it is LuaNameDef) {
            if (it == psi)
                return@Processor false
            idx++
        }
        return@Processor true
    })
    return idx
}

val LuaNameDef.docTy: ITy? get() {
    val localDef = PsiTreeUtil.getParentOfType(this, LuaLocalDef::class.java)
    return localDef?.comment?.ty
}

fun LuaAssignStat.getIndexFor(psi: LuaExpr): Int {
    var idx = 0
    LuaPsiTreeUtilEx.processChildren(this.varExprList, Processor{
        if (it is LuaExpr) {
            if (it == psi)
                return@Processor false
            idx++
        }
        return@Processor true
    })
    return idx
}

fun LuaAssignStat.getExprAt(index:Int) : LuaExpr? {
    val list = this.varExprList.exprList
    return list.getOrNull(index)
}

fun LuaListArgs.getIndexFor(psi: LuaExpr): Int {
    var idx = 0
    LuaPsiTreeUtilEx.processChildren(this, Processor {
        if (it is LuaExpr) {
            if (it == psi)
                return@Processor false
            idx++
        }
        return@Processor true
    })
    return idx
}

val LuaExprList.exprStubList: List<LuaExpr> get() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, LuaExpr::class.java)
}

fun LuaExprList.getExprAt(idx: Int): LuaExpr? {
    return exprStubList.getOrNull(idx)
}

fun LuaExprList.guessType(context: SearchContext):ITy {
    val exprList = exprStubList
    return if (exprList.size == 1)
        exprList.first().guessType(context)
    else {
        val list = mutableListOf<ITy>()
        exprList.forEach { list.add(it.guessType(context)) }
        TyTuple(list)
    }
}

fun LuaParametersOwner.getIndexFor(paramNameDef: LuaParamNameDef): Int {
    val list = this.paramNameDefList
    if (list != null) {
        list.indices
                .filter { list[it].name == paramNameDef.name }
                .forEach { return it }
    }
    return 0
}

fun LuaLocalDef.getExprFor(nameDef: LuaNameDef): LuaExpr? {
    val nameList = this.nameList
    nameList ?: return null
    val exprList = this.exprList
    exprList ?: return null

    var next = nameList.firstChild
    var idx = 0
    var found = false
    while (next != null) {
        if (next is LuaNameDef) {
            if (next == nameDef) {
                found = true
                break
            }
            idx++
        }
        next = next.nextSibling
    }
    if (!found) return null
    return exprList.getExprAt(idx)
}

val LuaParamNameDef.owner: LuaParametersOwner
    get() = PsiTreeUtil.getParentOfType(this, LuaParametersOwner::class.java)!!

val LuaFuncBodyOwner.overloads: Array<IFunSignature> get() {
    val list = mutableListOf<IFunSignature>()
    if (this is LuaCommentOwner) {
        val comment = comment
        if (comment != null) {
            val children = PsiTreeUtil.findChildrenOfAnyType(comment, LuaDocOverloadDef::class.java)
            children.forEach {
                val fty = it.functionTy
                if (fty != null)
                    list.add(FunSignature.create(false, fty))
            }
        }
    }
    return list.toTypedArray()
}

enum class LuaLiteralKind {
    String,
    Bool,
    Number,
    Nil,
    Unknown;

    companion object {
        fun toEnum(ID: Byte): LuaLiteralKind {
            return LuaLiteralKind.values().find { it.ordinal == ID.toInt() } ?: Unknown
        }
    }
}

val LuaLiteralExpr.kind: LuaLiteralKind get() {
    return when(node.firstChildNode.elementType) {
        LuaTypes.STRING -> LuaLiteralKind.String
        LuaTypes.TRUE -> LuaLiteralKind.Bool
        LuaTypes.FALSE -> LuaLiteralKind.Bool
        LuaTypes.NIL -> LuaLiteralKind.Nil
        LuaTypes.NUMBER -> LuaLiteralKind.Number
        else -> LuaLiteralKind.Unknown
    }
}

val LuaLiteralExpr.stringValue: String get() {
    val content = LuaString.getContent(text)
    return content.value
}

val LuaLiteralExpr.boolValue: Boolean get() = text == "true"

val LuaLiteralExpr.numberValue: Float get() = text.toFloat()

val LuaComment.docTy: ITy? get() {
    return this.typeDef?.type
}

val LuaComment.ty: ITy? get() {
    val cls = classDef?.type
    return cls ?: typeDef?.type
}

val LuaDocClassDef.aliasName: String? get() {
    val owner = LuaCommentUtil.findOwner(this)
    when (owner) {
        is LuaAssignStat -> {
            val expr = owner.getExprAt(0)
            if (expr is LuaNameExpr)
                return getGlobalTypeName(expr)
        }

        is LuaLocalDef -> {
            val expr = owner.exprList?.getExprAt(0)
            if (expr is LuaTableExpr)
                return getTableTypeName(expr)
        }
    }
    return null
}

val LuaIndexExpr.brack: Boolean get() {
    return lbrack != null
}

val LuaIndexExpr.docTy: ITy? get() {
    return assignStat?.comment?.docTy
}

val LuaIndexExpr.prefixExpr: LuaExpr get() {
    return firstChild as LuaExpr
}

val LuaExpr.assignStat: LuaAssignStat? get() {
    val p1 = PsiTreeUtil.getStubOrPsiParent(this)
    if (p1 is LuaVarList) {
        val p2 = PsiTreeUtil.getStubOrPsiParent(p1)
        if (p2 is LuaAssignStat)
            return p2
    }
    return null
}

val LuaNameExpr.docTy: ITy? get() {
    return assignStat?.comment?.ty
}

val LuaFuncDef.forwardDeclaration: PsiElement? get() {
    /*return CachedValuesManager.getCachedValue(this, KEY_FORWARD, {
        val refName = name
        val ret = if (refName == null) null else resolveLocal(refName, this)
        CachedValueProvider.Result.create(ret, this)
    })*/
    TODO()
}

val LuaCallExpr.argList: List<LuaExpr> get() {
    val args = this.args
    return when (args) {
        is LuaSingleArg -> listOf(args.expr)
        is LuaListArgs -> args.exprList
        else -> emptyList()
    }
}

val LuaBinaryExpr.left: LuaExpr? get() {
    return PsiTreeUtil.getStubChildOfType(this, LuaExpr::class.java)
}

val LuaBinaryExpr.right: LuaExpr? get() {
    val list = PsiTreeUtil.getChildrenOfTypeAsList(this, LuaExpr::class.java)
    return list.getOrNull(1)
}