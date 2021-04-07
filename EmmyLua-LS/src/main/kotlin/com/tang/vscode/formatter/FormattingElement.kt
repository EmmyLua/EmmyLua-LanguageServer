package com.tang.vscode.formatter

import com.intellij.openapi.util.TextRange

data class FormattingElement(var text:String, val type:FormattingType, val textRange: TextRange, val children: MutableList<FormattingElement>);