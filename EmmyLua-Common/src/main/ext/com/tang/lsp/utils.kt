package com.tang.lsp

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun range(sLine: Int, sChar: Int, eLine: Int, eChar: Int): Range {
    return Range(Position(sLine, sChar), Position(eLine, eChar))
}

fun TextRange.toRange(file: ILuaFile): Range {
    val lineStart = file.getLine(this.startOffset)
    val lineEnd = file.getLine(this.endOffset)
    return range(lineStart.first, lineStart.second, lineEnd.first, lineEnd.second)
}

fun PsiReference.getRangeInFile(file: ILuaFile): Range {
    var textRange = rangeInElement
    val parentRange = element.textRange
    textRange = textRange.shiftRight(parentRange.startOffset)
    return textRange.toRange(file)
}

val PsiElement.nameRange: TextRange? get() {
    if (this is PsiNameIdentifierOwner) {
        val id = this.nameIdentifier
        return id?.textRange
    }
    return this.textRange
}