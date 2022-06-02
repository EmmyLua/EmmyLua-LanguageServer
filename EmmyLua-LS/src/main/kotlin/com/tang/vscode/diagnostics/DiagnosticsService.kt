package com.tang.vscode.diagnostics

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.psi.*
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import com.tang.vscode.diagnostics.inspections.*
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.jsonrpc.CancelChecker

object DiagnosticsService {
    fun diagnosticFile(file: ILuaFile, diagnostics: MutableList<Diagnostic>, checker: CancelChecker?) {
        PsiTreeUtil.processElements(file.psi) {
            when (it) {
                is PsiErrorElement -> {
                    checker?.checkCanceled()
                    val diagnostic = Diagnostic()
                    diagnostic.message = it.errorDescription
                    diagnostic.severity =
                        if (it.parent is LuaDocPsiElement) DiagnosticSeverity.Warning else DiagnosticSeverity.Error
                    diagnostic.range = it.textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
                is LuaExprStat -> {
                    checker?.checkCanceled()
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
                    checker?.checkCanceled()
                    DeprecatedInspection.indexDeprecatedInspections(it, file, diagnostics)
                    FieldValidInspection.fieldValidationInspections(it, file, diagnostics)
                }
                is LuaCallExpr -> {
                    checker?.checkCanceled()
                    FunctionInspection.callExprInspections(it, file, diagnostics)
                }
                is LuaAssignStat -> {
                    checker?.checkCanceled()
                    AssignInspection.assignInspections(it, file, diagnostics)
                }
                is LuaNameExpr -> {
                    checker?.checkCanceled()
                    DeprecatedInspection.nameExprDeprecatedInspections(it, file, diagnostics)
                    UndeclaredVariableInspection.undeclaredVariableInspections(it, file, diagnostics)
                }
                is LuaDocTagClass -> {
                    checker?.checkCanceled()
                    InheritInspection.inheritInspections(it, file, diagnostics)
                }
            }
            true
        }
    }


}