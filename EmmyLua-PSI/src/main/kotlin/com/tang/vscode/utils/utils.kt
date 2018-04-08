package com.tang.vscode.utils

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.CompletableFutures
import com.tang.vscode.api.ILuaFile
import java.util.concurrent.CompletableFuture

fun range(sLine: Int, sChar: Int, eLine: Int, eChar: Int): Range {
    return Range(Position(sLine, sChar), Position(eLine, eChar))
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