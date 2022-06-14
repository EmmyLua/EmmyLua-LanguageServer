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

package com.tang.intellij.lua.ty

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.Processor
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.LuaDocFunctionTy
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.*
import java.lang.Integer.max

interface IFunSignature {
    val colonCall: Boolean
    val returnTy: ITy
    val params: Array<LuaParamInfo>
    val displayName: String
    val paramSignature: String
    val tyParameters: Array<TyParameter>
    val varargTy: ITy?
    fun substitute(substitutor: ITySubstitutor): IFunSignature
    fun subTypeOf(other: IFunSignature, context: SearchContext, strict: Boolean): Boolean
}

fun IFunSignature.processArgs(callExpr: LuaCallExpr, processor: (index: Int, param: LuaParamInfo) -> Boolean) {
    val expr = callExpr.expr
    val thisTy = if (expr is LuaIndexExpr) {
        expr.guessType(SearchContext.get(expr.project))
    } else null
    processArgs(thisTy, callExpr.isMethodColonCall, processor)
}

fun IFunSignature.processArgs(
    thisTy: ITy?,
    colonStyle: Boolean,
    processor: (index: Int, param: LuaParamInfo) -> Boolean
) {
    var index = 0
    var pIndex = 0
    if (colonStyle && !colonCall) {
        pIndex++
    } else if (!colonStyle && colonCall) {
        val pi = LuaParamInfo.createSelf(thisTy)
        if (!processor(index++, pi)) return
    }

    for (i in pIndex until params.size) {
        if (!processor(index++, params[i])) return
    }
}

fun IFunSignature.processArgs(processor: (index: Int, param: LuaParamInfo) -> Boolean) {
    var index = 0
    if (colonCall)
        index++
    for (i in params.indices) {
        if (!processor(index++, params[i])) return
    }
}

fun IFunSignature.processParams(
    thisTy: ITy?,
    colonStyle: Boolean,
    processor: (index: Int, param: LuaParamInfo) -> Boolean
) {
    var index = 0
    if (colonCall) {
        val pi = LuaParamInfo.createSelf(thisTy)
        if (!processor(index++, pi)) return
    }

    for (element in params) {
        if (!processor(index++, element)) return
    }
}

fun IFunSignature.getFirstParam(thisTy: ITy?, colonStyle: Boolean): LuaParamInfo? {
    var pi: LuaParamInfo? = null
    processParams(thisTy, colonStyle) { _, paramInfo ->
        pi = paramInfo
        false
    }
    return pi
}

fun IFunSignature.getParamTy(index: Int): ITy {
    val info = params.getOrNull(index)
    return info?.ty ?: Ty.UNKNOWN
}

//eg. print(...)
fun IFunSignature.hasVarargs(): Boolean {
    return this.varargTy != null
}

fun IFunSignature.isGeneric() = tyParameters.isNotEmpty()

abstract class FunSignatureBase(
    override val colonCall: Boolean,
    override val params: Array<LuaParamInfo>,
    override val tyParameters: Array<TyParameter> = emptyArray()
) : IFunSignature {
    override fun equals(other: Any?): Boolean {
        if (other is IFunSignature) {
            if (params.size != other.params.size)
                return false
            return params.indices.none { params[it] != other.params.getOrNull(it) }
        }
        return false
    }

    override fun hashCode(): Int {
        var code = 0//returnTy.hashCode()
        params.forEach {
            code += it.ty.hashCode() * 31
        }
        return code
    }

    override val displayName: String by lazy {
        val paramSB = mutableListOf<String>()
        params.forEach {
            paramSB.add(it.name + (if (it.nullable) "?" else "") + ":" + it.ty.displayName)
        }
        if (hasVarargs()) {
            varargTy?.let {
                paramSB.add("...:" + it.displayName)
            }
        }
        "(${paramSB.joinToString(", ")}) -> ${returnTy.displayName}"
    }

    override val paramSignature: String
        get() {
            val list = arrayOfNulls<String>(params.size)
            for (i in params.indices) {
                val lpi = params[i]
                list[i] = lpi.name
            }
            return "(" + list.joinToString(", ") + ")"
        }

    override fun substitute(substitutor: ITySubstitutor): IFunSignature {
        val list = params.map { it.substitute(substitutor) }
        return FunSignature(
            colonCall,
            returnTy.substitute(substitutor),
            varargTy?.substitute(substitutor),
            list.toTypedArray(),
            tyParameters
        )
    }

    override fun subTypeOf(other: IFunSignature, context: SearchContext, strict: Boolean): Boolean {
        for (i in params.indices) {
            val p1 = params[i]
            val p2 = other.params.getOrNull(i) ?: return false
            if (!p1.ty.subTypeOf(p2.ty, context, strict))
                return false
        }
        return true
    }
}

class FunSignature(
    colonCall: Boolean,
    override val returnTy: ITy,
    override val varargTy: ITy?,
    params: Array<LuaParamInfo>,
    tyParameters: Array<TyParameter> = emptyArray()
) : FunSignatureBase(colonCall, params, tyParameters) {

    companion object {
        private fun initParams(func: LuaDocFunctionTy): Array<LuaParamInfo> {
            val list = mutableListOf<LuaParamInfo>()
            func.functionParamList.forEach {
                val p = LuaParamInfo()
                p.name = it.id.text
                p.ty = it.ty?.getType() ?: Ty.UNKNOWN
                list.add(p)
            }
            return list.toTypedArray()
        }

        fun create(colonCall: Boolean, functionTy: LuaDocFunctionTy): IFunSignature {
            val list = mutableListOf<TyParameter>()
            functionTy.genericDefList.forEach {
                it.name?.let { name ->
                    list.add(
                        TyParameter(
                            name,
                            it.classNameRef?.text
                        )
                    )
                }
            }
            return FunSignature(
                colonCall,
                functionTy.returnType,
                functionTy.varargParam?.type,
                initParams(functionTy),
                list.toTypedArray()
            )
        }

        fun serialize(sig: IFunSignature, stream: StubOutputStream) {
            stream.writeBoolean(sig.colonCall)
            Ty.serialize(sig.returnTy, stream)
            stream.writeTyNullable(sig.varargTy)
            stream.writeParamInfoArray(sig.params)
        }

        fun deserialize(stream: StubInputStream): IFunSignature {
            val colonCall = stream.readBoolean()
            val ret = Ty.deserialize(stream)
            val varargTy = stream.readTyNullable()
            val params = stream.readParamInfoArray()
            return FunSignature(colonCall, ret, varargTy, params)
        }
    }
}

interface ITyFunction : ITy {
    val mainSignature: IFunSignature
    val signatures: Array<IFunSignature>
}

val ITyFunction.isColonCall get() = hasFlag(TyFlags.SELF_FUNCTION)

fun ITyFunction.process(processor: Processor<IFunSignature>) {
    if (processor.process(mainSignature)) {
        for (signature in signatures) {
            if (!processor.process(signature))
                break
        }
    }
}

fun ITyFunction.findPerfectSignature(nArgs: Int): IFunSignature {
    var sgi: IFunSignature? = null
    var perfectN = Int.MAX_VALUE
    process(Processor {
        val offset = Math.abs(it.params.size - nArgs)
        if (offset < perfectN) {
            perfectN = offset
            sgi = it
            if (perfectN == 0) return@Processor false
        }
        true
    })
    return sgi ?: mainSignature
}

fun ITyFunction.findPerfectSignature(call: LuaCallExpr, paramSize: Int = 0): IFunSignature {
    var sig: IFunSignature? = null
    val callArgs = call.argList
    val nArgs = max(callArgs.size, paramSize)
    val searchContext = SearchContext.get(call.project)

    if (mainSignature.params.isNotEmpty()
        && nArgs > 1
        && call.isMethodColonCall == isColonCall
    ) {
        val firstParamTy = mainSignature.params.first().ty
        if (firstParamTy.subTypeOf(Ty.STRING, searchContext, true)) {
            process(Processor {
                val params = it.params
                if (params.isNotEmpty()) {
                    val sigFirstParamTy = params.first().ty
                    if (sigFirstParamTy is TyStringLiteral
                        && (sigFirstParamTy.content.trim('\"', '\'')
                                == callArgs.first().text.trim('\"', '\''))
                    ) {
                        sig = it
                        return@Processor false
                    }
                }
                true
            })
        } else if (firstParamTy.subTypeOf(Ty.NUMBER, searchContext, true)) {
            process(Processor {
                val params = it.params
                if (params.isNotEmpty()) {
                    val sigFirstParamTy = params.first().ty
                    if (sigFirstParamTy is TyStringLiteral
                        && (sigFirstParamTy.content == callArgs.first().text)
                    ) {
                        sig = it
                        return@Processor false
                    }
                }
                true
            })
        } else if (firstParamTy is TyClass && firstParamTy.isEnum) {
            process(Processor {
                val params = it.params
                if (params.isNotEmpty()) {
                    val sigFirstParamTy = params.first().ty
                    if (sigFirstParamTy is TyStringLiteral
                        && (sigFirstParamTy.content == callArgs.first().text)
                    ) {
                        sig = it
                        return@Processor false
                    }
                }
                true
            })
        }
    }


    if (sig == null) {
        // 是否是 inst:method() 被用为 inst.method(self) 形式
        // isInstanceMethodUsedAsStaticMethod
        if (isColonCall && call.isMethodDotCall) {
            val originSig = findPerfectSignature(nArgs - 1)

            val luaParamInfoList = mutableListOf(LuaParamInfo(Constants.WORD_SELF, Ty.UNKNOWN))
            luaParamInfoList.addAll(originSig.params)
            val luaParamInfoArray = luaParamInfoList.toTypedArray()

            sig = FunSignature(
                false,
                originSig.returnTy,
                originSig.varargTy,
                luaParamInfoArray
            )
        }
        // isStaticMethodUsedAsInstanceMethod
        else if (!isColonCall && call.isMethodColonCall) {
            val originSig = findPerfectSignature(nArgs + 1)

            if (originSig.params.isNotEmpty()) {
                val luaParamInfoList = originSig.params.toMutableList()
                luaParamInfoList.removeAt(0)
                val luaParamInfoArray = luaParamInfoList.toTypedArray()
                sig = FunSignature(
                    true,
                    originSig.returnTy,
                    originSig.varargTy,
                    luaParamInfoArray
                )
            } else {
                sig = FunSignature(
                    true,
                    originSig.returnTy,
                    originSig.varargTy,
                    emptyArray()
                )
            }
        } else {
            sig = findPerfectSignature(nArgs)
        }
    }

    return sig ?: mainSignature
}

abstract class TyFunction : Ty(TyKind.Function), ITyFunction {

    override fun equals(other: Any?): Boolean {
        if (other is ITyFunction) {
            if (mainSignature != other.mainSignature)
                return false
            return signatures.indices.none { signatures[it] != other.signatures.getOrNull(it) }
        }
        return false
    }

    override fun hashCode(): Int {
        var code = mainSignature.hashCode()
        signatures.forEach {
            code += it.hashCode() * 31
        }
        return code
    }

    override fun subTypeOf(other: ITy, context: SearchContext, strict: Boolean): Boolean {
        if (super.subTypeOf(other, context, strict) || other == FUNCTION)
            return true // Subtype of function primitive.

        var matched = false
        if (other is ITyFunction) {
            process(Processor { sig1 ->
                other.process(Processor { sig2 ->
                    matched = sig2.subTypeOf(sig1, context, strict)
                    !matched
                })
                !matched
            })
        }
        return matched
    }

    override fun substitute(substitutor: ITySubstitutor): ITy {
        return substitutor.substitute(this)
    }

    override fun accept(visitor: ITyVisitor) {
        visitor.visitFun(this)
    }
}

class TyPsiFunction(private val colonCall: Boolean, val psi: LuaFuncBodyOwner, flags: Int = 0) : TyFunction() {
    init {
        this.flags = flags
        if (colonCall) {
            this.flags = this.flags or TyFlags.SELF_FUNCTION
        }
    }

    override val mainSignature: IFunSignature by lazy {

        object : FunSignatureBase(colonCall, psi.params, psi.tyParams) {
            override val returnTy: ITy by lazy {
                var returnTy = psi.guessReturnType(SearchContext.get(psi.project))
                /**
                 * todo optimize this bug solution
                 * local function test()
                 *      return test
                 * end
                 * -- will crash after type `test`
                 */
                if (returnTy is TyPsiFunction && returnTy.psi == psi) {
                    returnTy = UNKNOWN
                }

                returnTy
            }

            override val varargTy: ITy?
                get() = psi.varargType
        }
    }

    override val signatures: Array<IFunSignature> by lazy {
        psi.overloads
    }
}

class TyDocPsiFunction(func: LuaDocFunctionTy) : TyFunction() {
    private val main = FunSignature.create(false, func)
    override val mainSignature: IFunSignature = main
    override val signatures: Array<IFunSignature> = emptyArray()
}

class TySerializedFunction(
    override val mainSignature: IFunSignature,
    override val signatures: Array<IFunSignature>,
    flags: Int = 0
) : TyFunction() {
    init {
        this.flags = flags
    }
}

object TyFunctionSerializer : TySerializer<ITyFunction>() {
    override fun deserializeTy(flags: Int, stream: StubInputStream): ITyFunction {
        val mainSig = FunSignature.deserialize(stream)
        val arr = stream.readSignatures()
        return TySerializedFunction(mainSig, arr, flags)
    }

    override fun serializeTy(ty: ITyFunction, stream: StubOutputStream) {
        FunSignature.serialize(ty.mainSignature, stream)
        stream.writeSignatures(ty.signatures)
    }
}