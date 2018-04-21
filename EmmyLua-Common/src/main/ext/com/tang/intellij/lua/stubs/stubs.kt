package com.tang.intellij.lua.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.comment.psi.LuaDocFieldDef
import com.tang.intellij.lua.comment.psi.LuaDocTableDef
import com.tang.intellij.lua.comment.psi.LuaDocTypeDef
import com.tang.intellij.lua.psi.*

abstract class FakeStubElement<T : PsiElement> : StubElement<T> {
    override fun getPsi(): T? = null
}

interface LuaExprStubElement<out T> {
    val stub: T
}

abstract class LuaStubBase<T : PsiElement> : FakeStubElement<T>()

interface LuaExprStub<T : LuaExpr> : StubElement<T>

open class LuaExprStubImpl<T : LuaExpr> : LuaStubBase<T>(), LuaExprStub<T>

class LuaExprPlaceStub(parent: StubElement<*>?) : LuaExprStubImpl<LuaExpr>()

class LuaNameDefStub : FakeStubElement<LuaNameDef>()
class LuaTableFieldStub : FakeStubElement<LuaTableField>()

class LuaBinaryExprStub : LuaStubBase<LuaBinaryExpr>(), LuaExprStub<LuaBinaryExpr>
class LuaUnaryExprStub : LuaStubBase<LuaUnaryExpr>(), LuaExprStub<LuaUnaryExpr>
class LuaNameExprStub : LuaStubBase<LuaNameExpr>(), LuaExprStub<LuaNameExpr>
class LuaTableExprStub : LuaStubBase<LuaTableExpr>(), LuaExprStub<LuaTableExpr>
class LuaLiteralExprStub : LuaStubBase<LuaLiteralExpr>(), LuaExprStub<LuaLiteralExpr>
class LuaIndexExprStub(val visibility: Visibility) : LuaStubBase<LuaIndexExpr>(), LuaExprStub<LuaIndexExpr>
class LuaClosureExprStub : LuaStubBase<LuaClosureExpr>(), LuaExprStub<LuaClosureExpr>
class LuaFuncStub : FakeStubElement<LuaFuncDef>()
class LuaPlaceholderStub : FakeStubElement<PsiElement>()

class LuaClassMethodStub : FakeStubElement<LuaClassMethod>()
class LuaLocalFuncDefStub : FakeStubElement<LuaLocalFuncDef>()

class LuaDocClassStub : FakeStubElement<LuaDocClassDef>()
class LuaDocFieldDefStub : FakeStubElement<LuaDocFieldDef>()
class LuaDocTableDefStub : FakeStubElement<LuaDocTableDef>()
class LuaDocTableFieldDefStub : FakeStubElement<LuaDocTableDef>()
class LuaDocTypeDefStub : FakeStubElement<LuaDocTypeDef>()