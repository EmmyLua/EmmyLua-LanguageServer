package com.tang.vscode.formatter

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

data class FormattingElement(var psi: PsiElement, val type:FormattingType, val textRange: TextRange, val children: MutableList<FormattingElement>);
