package com.tang.vscode.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticTag

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
                }
                is LuaIndexExpr -> {
//                    indexDeprecatedInspections(it, file, diagnostics)
                    fieldValidationInspections(it, file, diagnostics)
                }
                is LuaCallExpr -> {
//                    callDeprecatedInspections(it, file, diagnostics)
                    callExprInspections(it, file, diagnostics)
                }
                is LuaAssignStat -> {
                    assignInspections(it, file, diagnostics)
                }
                is LuaNameExpr -> {
                    undeclaredVariableInspections(it, file, diagnostics)
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

    private fun callDeprecatedInspections(o: LuaCallExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        val expr = o.expr
        if (expr is LuaNameExpr) {
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

    private fun indexDeprecatedInspections(o: LuaIndexExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        val searchContext = SearchContext.get(o.project)
        val res = resolve(o, searchContext)
        if ((res is LuaClassMethodDef && res.isDeprecated)
            || (res is LuaClassField && res.isDeprecated)
        ) {
            o.id?.let { id ->
                val diagnostic = Diagnostic()
                diagnostic.message = "deprecated"
                diagnostic.severity = DiagnosticSeverity.Hint
                diagnostic.tags = listOf(DiagnosticTag.Deprecated)
                diagnostic.range = id.textRange.toRange(file)
                diagnostics.add(diagnostic)
            }
        }
    }

    private fun fieldValidationInspections(o: LuaIndexExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.fieldValidation != InspectionsLevel.None) {
            if (o.parent is LuaVarList) {
                return
            }
            val searchContext = SearchContext.get(o.project)
            val res = resolve(o, searchContext)
            val context = SearchContext.get(o.project)
            val prefixType = o.guessParentType(context)

            if (prefixType !is TyUnknown && res == null) {
                o.id?.let { id ->
                    val diagnostic = Diagnostic()
                    diagnostic.message = "Undefined property '${id.text}'"
                    diagnostic.severity = makeSeverity(DiagnosticsOptions.fieldValidation)
                    diagnostic.range = id.textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
            }
        }
    }

    private fun assignInspections(o: LuaAssignStat, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.assignValidation != InspectionsLevel.None) {
            val assignees = o.varExprList.exprList
            val values = o.valueExprList?.exprList ?: listOf()
            val searchContext = SearchContext.get(o.project)

            // Check right number of fields/assignments
            if (assignees.size > values.size) {
                for (i in values.size until assignees.size) {
                    val diagnostic = Diagnostic()
                    diagnostic.message = "Missing value assignment."
                    diagnostic.severity = makeSeverity(DiagnosticsOptions.assignValidation)
                    diagnostic.range = assignees[i].textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
            } else if (assignees.size < values.size) {
                for (i in assignees.size until values.size) {
                    val diagnostic = Diagnostic()
                    diagnostic.message = "Nothing to assign to."
                    diagnostic.severity = makeSeverity(DiagnosticsOptions.assignValidation)
                    diagnostic.range = values[i].textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
            } else {
                // Try to match types for each assignment
                for (i in 0 until assignees.size) {
                    val field = assignees[i]
                    val name = field.name ?: ""
                    val value = values[i]
                    val valueType = value.guessType(searchContext)

                    // Field access
                    if (field is LuaIndexExpr) {
                        // Get owner class
                        val parent = field.guessParentType(searchContext)

                        if (parent is TyClass) {
                            val fieldType = parent.findMemberType(name, searchContext) ?: Ty.NIL

                            if (!valueType.subTypeOf(fieldType, searchContext, false)) {
                                val diagnostic = Diagnostic()
                                diagnostic.message =
                                    "Type mismatch. Required: '%s' Found: '%s'".format(fieldType, valueType)
                                diagnostic.severity = makeSeverity(DiagnosticsOptions.assignValidation)
                                diagnostic.range = value.textRange.toRange(file)
                                diagnostics.add(diagnostic)
                            }
                        }
                    } else {
                        // Local/global var assignments, only check type if there is no comment defining it
                        if (o.comment == null) {
                            val fieldType = field.guessType(searchContext)
                            if (!valueType.subTypeOf(fieldType, searchContext, false)) {
                                val diagnostic = Diagnostic()
                                diagnostic.message =
                                    "Type mismatch. Required: '%s' Found: '%s'".format(fieldType, valueType)
                                diagnostic.severity = makeSeverity(DiagnosticsOptions.assignValidation)
                                diagnostic.range = value.textRange.toRange(file)
                                diagnostics.add(diagnostic)
                            }
                        }
                    }
                }
            }
        }
    }


    private fun callExprInspections(callExpr: LuaCallExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.parameterValidation != InspectionsLevel.None) {
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
                                    diagnostic.severity = makeSeverity(DiagnosticsOptions.parameterValidation)
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

    private fun undeclaredVariableInspections(o: LuaNameExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.undeclaredVariable != InspectionsLevel.None) {
            val res = resolve(o, SearchContext.get(o.project))

            if (res == null) {
                val diagnostic = Diagnostic()
                diagnostic.message = "Undeclared variable '%s'.".format(o.text)
                diagnostic.severity = makeSeverity(DiagnosticsOptions.undeclaredVariable)
                diagnostic.range = o.textRange.toRange(file)
                diagnostics.add(diagnostic)

            }
        }
    }

    private fun makeSeverity(level: InspectionsLevel): DiagnosticSeverity {
        return when (level) {
            InspectionsLevel.None -> DiagnosticSeverity.Information
            InspectionsLevel.Warning -> DiagnosticSeverity.Warning
            InspectionsLevel.Error -> DiagnosticSeverity.Error
        }
    }
}