package com.tang.vscode.diagnostics.inspections

import com.tang.intellij.lua.psi.LuaAssignStat
import com.tang.intellij.lua.psi.LuaIndexExpr
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TyClass
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import com.tang.vscode.diagnostics.DiagnosticsOptions
import com.tang.vscode.diagnostics.InspectionsLevel
import org.eclipse.lsp4j.Diagnostic

object AssignInspection {

    fun assignInspections(o: LuaAssignStat, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        if (DiagnosticsOptions.assignValidation != InspectionsLevel.None) {
            val assignees = o.varExprList.exprList
            val values = o.valueExprList?.exprList ?: listOf()
            val searchContext = SearchContext.get(o.project)

            // Check right number of fields/assignments
            if (assignees.size > values.size) {
                for (i in values.size until assignees.size) {
                    val diagnostic = Diagnostic()
                    diagnostic.message = "Missing value assignment."
                    diagnostic.severity = Severity.makeSeverity(DiagnosticsOptions.assignValidation)
                    diagnostic.range = assignees[i].textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
            } else if (assignees.size < values.size) {
                for (i in assignees.size until values.size) {
                    val diagnostic = Diagnostic()
                    diagnostic.message = "Nothing to assign to."
                    diagnostic.severity = Severity.makeSeverity(DiagnosticsOptions.assignValidation)
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
                                diagnostic.severity = Severity.makeSeverity(DiagnosticsOptions.assignValidation)
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
                                diagnostic.severity = Severity.makeSeverity(DiagnosticsOptions.assignValidation)
                                diagnostic.range = value.textRange.toRange(file)
                                diagnostics.add(diagnostic)
                            }
                        }
                    }
                }
            }
        }
    }
}