package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.IFunSignature
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.TyClass
import com.tang.intellij.lua.ty.TyParameter

// fake stubs!

abstract class FakeStubElement<T : PsiElement> : StubElement<T> {
    override fun getPsi(): T? = null
    override fun getParentStub(): StubElement<*>? = null
    override fun getChildrenStubs(): MutableList<StubElement<PsiElement>> {
        TODO()
    }
}

interface LuaExprStubElement<out T> {
    val stub: T
}

abstract class LuaStubBase<T : PsiElement> : FakeStubElement<T>()

interface LuaFuncBodyOwnerStub<T : LuaFuncBodyOwner> : StubElement<T> {
    fun guessReturnTy(searchContext: SearchContext):ITy
    val returnDocTy:ITy?
    val varargTy: ITy?
    val tyParams: Array<TyParameter>
    val params: Array<LuaParamInfo>
    val overloads: Array<IFunSignature>
}

interface LuaDocTyStub {
    val docTy: ITy?
}

interface LuaClassMemberStub<T : PsiElement> : StubElement<T>, LuaDocTyStub {
    val visibility: Visibility
    val isDeprecated: Boolean
}

interface LuaExprStub<T : LuaExpr> : StubElement<T>

open class LuaExprStubImpl<T : LuaExpr> : LuaStubBase<T>(), LuaExprStub<T>

interface LuaExprPlaceStub : LuaExprStub<LuaExpr>

interface LuaNameDefStub : StubElement<LuaNameDef>, LuaDocTyStub {
    val name: String
    val anonymousType:String
}
interface LuaTableFieldStub : LuaClassMemberStub<LuaTableField> {
    val typeName: String?
    val name: String?
}

interface LuaBinaryExprStub : StubElement<LuaBinaryExpr>, LuaExprStub<LuaBinaryExpr> {
    val opType: IElementType?
}
interface LuaUnaryExprStub : StubElement<LuaUnaryExpr>, LuaExprStub<LuaUnaryExpr> {
    val opType: IElementType?
}
interface LuaNameExprStub : StubElement<LuaNameExpr>, LuaExprStub<LuaNameExpr>, LuaDocTyStub {
    val name: String
    val module: String
    val isName: Boolean
    val isGlobal: Boolean
}
interface LuaTableExprStub : StubElement<LuaTableExpr>, LuaExprStub<LuaTableExpr> {
    val tableTypeName: String
}
interface LuaLiteralExprStub : StubElement<LuaLiteralExpr>, LuaExprStub<LuaLiteralExpr> {
    val kind: LuaLiteralKind
    val string: String?
    val tooLargerString: Boolean
}

interface LuaIndexExprStub : LuaExprStub<LuaIndexExpr>, LuaClassMemberStub<LuaIndexExpr> {
    val classNames: Array<String>
    val name: String?
    val brack: Boolean
    val isAssign: Boolean
}

interface LuaClosureExprStub : StubElement<LuaClosureExpr>, LuaExprStub<LuaClosureExpr>

interface LuaFuncStub : LuaFuncBodyOwnerStub<LuaFuncDef>, LuaClassMemberStub<LuaFuncDef> {
    val name: String
    val module: String
}

interface LuaPlaceholderStub : StubElement<PsiElement>

interface LuaClassMethodStub : LuaFuncBodyOwnerStub<LuaClassMethod> {
    val classNames: Array<String>

    val name: String

    val isStatic: Boolean
}
interface LuaLocalFuncDefStub : StubElement<LuaLocalFuncDef>, LuaFuncBodyOwnerStub<LuaLocalFuncDef> {
    val name: String
}
interface LuaDocTagClassStub : StubElement<LuaDocTagClass> {
    val className: String
    val aliasName: String?
    val superClassName: String?
    val classType: TyClass
    val isDeprecated: Boolean
}
interface LuaDocTagFieldStub : LuaClassMemberStub<LuaDocTagField> {
    val name: String

    val type: ITy

    val className: String?
}
interface LuaDocTableDefStub : StubElement<LuaDocTableDef> {
    val className: String
}
interface LuaDocTableFieldStub : LuaClassMemberStub<LuaDocTableField> {
    val name: String
    val parentTypeName: String
}
interface LuaDocTagTypeStub : StubElement<LuaDocTagType>

interface LuaFileStub : StubElement<LuaPsiFile>

interface LuaDocTagAliasStub : StubElement<LuaDocTagClass> {
    val name: String
    val type: ITy
}