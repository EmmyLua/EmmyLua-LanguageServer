package com.tang.vscode.diagnostics.inspections

import com.tang.vscode.diagnostics.InspectionsLevel
import org.eclipse.lsp4j.DiagnosticSeverity

object Severity {
    fun makeSeverity(level: InspectionsLevel): DiagnosticSeverity {
        return when (level) {
            InspectionsLevel.None -> DiagnosticSeverity.Information
            InspectionsLevel.Warning -> DiagnosticSeverity.Warning
            InspectionsLevel.Error -> DiagnosticSeverity.Error
        }
    }
}