package com.tang.vscode.vscode

import org.eclipse.lsp4j.Range

enum class AnnotatorType {
    Param
}

data class Annotator(val ranges: List<Range>, val type: AnnotatorType)

data class ProgressReport(val text: String, val percent: Float)