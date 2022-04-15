package com.tang.vscode.diagnostics.inspections

import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.resolve
import com.tang.intellij.lua.search.SearchContext
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import com.tang.vscode.diagnostics.DiagnosticsOptions
import com.tang.vscode.diagnostics.InspectionsLevel
import org.eclipse.lsp4j.Diagnostic

object UndeclaredVariableInspection {
    fun undeclaredVariableInspections(o: LuaNameExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.undeclaredVariable != InspectionsLevel.None) {
            val res = resolve(o, SearchContext.get(o.project))

            if (res == null) {
                val diagnostic = Diagnostic()
                diagnostic.message = "Undeclared variable '%s'.".format(o.text)
                diagnostic.severity = Severity.makeSeverity(DiagnosticsOptions.undeclaredVariable)
                diagnostic.range = o.textRange.toRange(file)
                diagnostics.add(diagnostic)
            }
        }
    }
}