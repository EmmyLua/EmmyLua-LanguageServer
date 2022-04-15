package com.tang.vscode.diagnostics.inspections

import com.tang.intellij.lua.psi.LuaIndexExpr
import com.tang.intellij.lua.psi.LuaVarList
import com.tang.intellij.lua.psi.resolve
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.TyUnknown
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import com.tang.vscode.diagnostics.DiagnosticsOptions
import com.tang.vscode.diagnostics.InspectionsLevel
import org.eclipse.lsp4j.Diagnostic

object FieldValidInspection {
    fun fieldValidationInspections(o: LuaIndexExpr, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
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
                    diagnostic.severity = Severity.makeSeverity(DiagnosticsOptions.fieldValidation)
                    diagnostic.range = id.textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
            }
        }
    }

}