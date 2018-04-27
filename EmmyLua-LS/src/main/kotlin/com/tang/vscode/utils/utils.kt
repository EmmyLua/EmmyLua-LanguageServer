package com.tang.vscode.utils

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.impl.LuaFile
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import java.util.concurrent.CompletableFuture

fun range(sLine: Int, sChar: Int, eLine: Int, eChar: Int): Range {
    return Range(Position(sLine, sChar), Position(eLine, eChar))
}

fun PsiReference.getRangeInFile(file: ILuaFile): Range {
    var textRange = rangeInElement
    val parentRange = element.textRange
    textRange = textRange.shiftRight(parentRange.startOffset)
    val luaFile = element.containingFile.virtualFile as LuaFile
    return textRange.toRange(luaFile)
}

val PsiElement.nameRange: TextRange? get() {
    if (this is PsiNameIdentifierOwner) {
        val id = this.nameIdentifier
        return id?.textRange
    }
    return this.textRange
}

fun TextRange.toRange(file: ILuaFile): Range {
    val lineStart = file.getLine(this.startOffset)
    val lineEnd = file.getLine(this.endOffset)
    return range(lineStart.first, lineStart.second, lineEnd.first, lineEnd.second)
}

fun <R> computeAsync(code :(CancelChecker) -> R) : CompletableFuture<R> {
    return CompletableFutures.computeAsync {
        ProgressManager.setCancelChecker(it)
        code(it)
    }
}