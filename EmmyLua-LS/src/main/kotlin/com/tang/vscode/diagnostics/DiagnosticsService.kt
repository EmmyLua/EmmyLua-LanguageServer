package com.tang.vscode.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticTag
import kotlin.math.exp

object DiagnosticsService {
    fun diagnosticFile(file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        PsiTreeUtil.processElements(file.psi) {
            when (it) {
                is PsiErrorElement -> {
                    val diagnostic = Diagnostic()
                    diagnostic.message = it.errorDescription
                    diagnostic.severity =
                        if (it.parent is LuaDocPsiElement) DiagnosticSeverity.Warning else DiagnosticSeverity.Error
                    diagnostic.range = it.textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
                is LuaExprStat -> {
                    val expr = it.expr
                    if (expr !is LuaCallExpr && PsiTreeUtil.findChildOfType(it, PsiErrorElement::class.java) == null) {
                        val diagnostic = Diagnostic()
                        diagnostic.message = "non-complete statement"
                        diagnostic.severity = DiagnosticSeverity.Error
                        diagnostic.range = it.textRange.toRange(file)
                        diagnostics.add(diagnostic)
                    }

                    if (DiagnosticsOptions.parameterValidation && expr is LuaCallExpr) {
                        val callExpr = expr
                        var nCommas = 0
                        val paramMap = mutableMapOf<Int, LuaTypeGuessable>()
                        callExpr.args.firstChild?.let { firstChild ->
                            var child: PsiElement? = firstChild
                            while (child != null) {
                                if (child.node.elementType == LuaTypes.COMMA) {
                                    nCommas++
                                } else {
                                    if (child is LuaTypeGuessable) {
                                        paramMap[nCommas] = child
                                    }
                                }

                                child = child.nextSibling
                            }
                        }
                        val context = SearchContext.get(callExpr.project)
                        callExpr.guessParentType(context).let { parentType ->
                            parentType.each { ty ->
                                if (ty is ITyFunction) {
                                    val sig = ty.findPerfectSignature(nCommas + 1)

                                    var index = 0;

                                    var skipFirstParam = false

                                    if (sig.colonCall && callExpr.isMethodDotCall) {
                                        index++;
                                    } else if (!sig.colonCall && callExpr.isMethodColonCall) {
                                        skipFirstParam = true
                                    }

                                    sig.params.forEach { pi ->
                                        if (skipFirstParam) {
                                            skipFirstParam = false
                                            return@forEach
                                        }

                                        val param = paramMap[index]
                                        if (param != null) {
                                            val paramType = param.guessType(context)
                                            if (!typeCheck(pi.ty, param, context)) {
                                                val diagnostic = Diagnostic()
                                                diagnostic.message =
                                                    "Type mismatch '${paramType.displayName}' not match type '${pi.ty.displayName}'"
                                                diagnostic.severity = DiagnosticSeverity.Warning
                                                diagnostic.range = param.textRange.toRange(file)
                                                diagnostics.add(diagnostic)
                                            }
                                        }
                                        ++index;
                                    }
                                    //可变参数暂时不做验证
                                }
                            }
                        }
                    }
                }
                is LuaIndexExpr -> {
                    val resolve = it.reference?.resolve()
                    if ((resolve is LuaClassMethodDef && resolve.isDeprecated)
                        || (resolve is LuaClassField && resolve.isDeprecated)
                    ) {
                        it.id?.let { id ->
                            val diagnostic = Diagnostic()
                            diagnostic.message = "deprecated"
                            diagnostic.severity = DiagnosticSeverity.Hint
                            diagnostic.tags = listOf(DiagnosticTag.Deprecated)
                            diagnostic.range = id.textRange.toRange(file)
                            diagnostics.add(diagnostic)
                        }
                    }

                    if(DiagnosticsOptions.fieldValidation){
                        if(it.parent is LuaVarList){
                            return@processElements true
                        }

                        if(resolve == null) {
                            it.id?.let { id ->
                                val diagnostic = Diagnostic()
                                diagnostic.message = "undefined property '${id.text}'"
                                diagnostic.severity = DiagnosticSeverity.Warning
                                diagnostic.range = id.textRange.toRange(file)
                                diagnostics.add(diagnostic)
                            }
                        }
                    }
                }
                is LuaCallExpr -> {
                    val expr = it.expr
                    if(expr is LuaNameExpr) {
                        val resolve = expr.reference?.resolve()
                        if (resolve is LuaFuncDef && resolve.isDeprecated) {
                            val diagnostic = Diagnostic()
                            diagnostic.message = "deprecated"
                            diagnostic.severity = DiagnosticSeverity.Hint
                            diagnostic.tags = listOf(DiagnosticTag.Deprecated)
                            diagnostic.range = expr.textRange.toRange(file)
                            diagnostics.add(diagnostic)
                        }
                    }
                }
            }

            true
        }
    }

    private fun typeCheck(defineType: ITy, variable: LuaTypeGuessable, context: SearchContext): Boolean {
        val variableType = variable.guessType(context)

        if (DiagnosticsOptions.anyTypeCanAssignToAnyDefineType && variableType is TyUnknown) {
            return true
        }

        if (DiagnosticsOptions.defineAnyTypeCanBeAssignedByAnyVariable && defineType is TyUnknown) {
            return true
        }

        // 由于没有接口 interface
        // 那么将匿名表传递给具有特定类型的定义类型也都被认为是合理的
        // 暂时不做field检查
        if (variable is LuaTableExpr &&
            (defineType.kind == TyKind.Class || defineType.kind == TyKind.Array || defineType.kind == TyKind.Tuple)
        ) {
            return true
        }

        // 类似于回调函数的写法，不写传参是非常普遍的，所以只需要认为定义类型是个函数就通过
        if (variable is LuaClosureExpr && defineType.kind == TyKind.Function) {
            return true
        }

        if (DiagnosticsOptions.defineTypeCanReceiveNilType && variableType.kind == TyKind.Nil) {
            return true
        }

        if (defineType is TyUnion) {
            var isUnionCheckPass = false
            defineType.each {
                if (typeCheck(it, variable, context)) {
                    isUnionCheckPass = true
                    return@each
                }
            }

            if (isUnionCheckPass) {
                return true
            }
        }

        return variableType.subTypeOf(defineType, context, true)
    }

}