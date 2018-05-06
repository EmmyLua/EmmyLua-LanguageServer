package com.tang.vscode

import org.eclipse.lsp4j.Range

enum class AnnotatorType {
    Param
}

data class AnnotatorParams(val uri: String)

data class Annotator(val uri: String, val ranges: List<Range>, val type: AnnotatorType)

data class ProgressReport(val text: String, val percent: Float)