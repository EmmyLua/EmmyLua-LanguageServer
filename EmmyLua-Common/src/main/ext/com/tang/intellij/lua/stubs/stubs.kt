package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.comment.psi.LuaDocFieldDef
import com.tang.intellij.lua.comment.psi.LuaDocTableDef
import com.tang.intellij.lua.comment.psi.LuaDocTypeDef
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy

abstract class FakeStubElement<T : PsiElement> : StubElement<T> {
    override fun getPsi(): T? = null
}

interface LuaExprStubElement<out T> {
    val stub: T
}

abstract class LuaStubBase<T : PsiElement> : FakeStubElement<T>()

interface LuaFuncBodyOwnerStub<T : LuaFuncBodyOwner> : StubElement<T> {
    fun guessReturnTy(searchContext: SearchContext):ITy
}

interface LuaDocTyStub {
    val docTy: ITy?
}

interface LuaClassMemberStub<T : PsiElement> : StubElement<T>, LuaDocTyStub {
    val visibility: Visibility
}

interface LuaExprStub<T : LuaExpr> : StubElement<T>

open class LuaExprStubImpl<T : LuaExpr> : LuaStubBase<T>(), LuaExprStub<T>

class LuaExprPlaceStub(parent: StubElement<*>?) : LuaExprStubImpl<LuaExpr>()

interface LuaNameDefStub : StubElement<LuaNameDef>, LuaDocTyStub {
    val name: String
    val anonymousType:String
}
interface LuaTableFieldStub : StubElement<LuaTableField>, LuaDocTyStub

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
class LuaLiteralExprStub : LuaStubBase<LuaLiteralExpr>(), LuaExprStub<LuaLiteralExpr>
class LuaIndexExprStub(val visibility: Visibility) : LuaStubBase<LuaIndexExpr>(), LuaExprStub<LuaIndexExpr>
class LuaClosureExprStub : LuaStubBase<LuaClosureExpr>(), LuaExprStub<LuaClosureExpr>
class LuaFuncStub : FakeStubElement<LuaFuncDef>()
class LuaPlaceholderStub : FakeStubElement<PsiElement>()

class LuaClassMethodStub : FakeStubElement<LuaClassMethod>()
class LuaLocalFuncDefStub : FakeStubElement<LuaLocalFuncDef>()

class LuaDocClassStub : FakeStubElement<LuaDocClassDef>()
interface LuaDocFieldDefStub : LuaClassMemberStub<LuaDocFieldDef> {
    val name: String

    val type: ITy

    val className: String?
}
interface LuaDocTableDefStub : StubElement<LuaDocTableDef> {
    val className: String
}
class LuaDocTableFieldDefStub : FakeStubElement<LuaDocTableDef>()
class LuaDocTypeDefStub : FakeStubElement<LuaDocTypeDef>()