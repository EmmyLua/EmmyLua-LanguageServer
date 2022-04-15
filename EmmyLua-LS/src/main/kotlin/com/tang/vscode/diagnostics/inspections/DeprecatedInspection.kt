package com.tang.vscode.diagnostics.inspections

import com.intellij.psi.PsiElement
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import com.tang.vscode.diagnostics.DiagnosticsOptions
import com.tang.vscode.diagnostics.InspectionsLevel
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticTag

object DeprecatedInspection {
    fun nameExprDeprecatedInspections(o: LuaNameExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.deprecated != InspectionsLevel.None) {
            checkDeprecated(o) {
                val diagnostic = Diagnostic()
                diagnostic.message = "'${o.name}' is deprecated"
                diagnostic.severity = DiagnosticSeverity.Hint
                diagnostic.tags = listOf(DiagnosticTag.Deprecated)
                diagnostic.range = o.textRange.toRange(file)
                diagnostics.add(diagnostic)
            }
        }
    }

    fun indexDeprecatedInspections(o: LuaIndexExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.deprecated != InspectionsLevel.None) {
            val id = o.id ?: return
            checkDeprecated(o) {
                val diagnostic = Diagnostic()
                diagnostic.message = "'${o.name}' is deprecated"
                diagnostic.severity = DiagnosticSeverity.Hint
                diagnostic.tags = listOf(DiagnosticTag.Deprecated)
                diagnostic.range = id.textRange.toRange(file)
                diagnostics.add(diagnostic)
            }
        }
    }

    private fun checkDeprecated(o: PsiElement, action: () -> Unit) {
        val resolve = o.reference?.resolve() ?: o
        val isDeprecated = when (resolve) {
            is LuaClassMember -> resolve.isDeprecated
            is LuaDocTagClass -> resolve.isDeprecated
            else -> false
        }
        if (isDeprecated) action()
    }
}