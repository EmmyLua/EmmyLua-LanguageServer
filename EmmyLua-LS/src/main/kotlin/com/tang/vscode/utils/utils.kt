package com.tang.vscode.utils

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.tang.intellij.lua.comment.psi.LuaDocClassDef
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyFunction
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.impl.LuaFile
import org.eclipse.lsp4j.*
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
    return textRange.toRange(file)
}

val PsiElement.nameRange: TextRange? get() {
    if (this is PsiNameIdentifierOwner) {
        val id = this.nameIdentifier
        return id?.textRange
    }
    return this.textRange
}

fun PsiNamedElement.getSymbol(): SymbolInformation {
    val file = containingFile.virtualFile as LuaFile
    val range = nameRange ?: textRange
    val loc = Location(file.uri.toString(), range.toRange(file))
    var text = name
    if (this is LuaFuncBodyOwner) {
        text = "$text${this.paramSignature}"
    }
    val kind = when (this) {
        is LuaClassMethod -> SymbolKind.Method
        is LuaClassField -> SymbolKind.Field
        is LuaDocClassDef -> SymbolKind.Class
        else -> SymbolKind.Variable
    }
    return SymbolInformation(text, kind, loc)
}

fun PsiNamedElement.getSymbolDetail(file: ILuaFile): SymbolInformation? {
    val range = nameRange ?: textRange
    return when (this) {
        is LuaClassMethodDef -> {
            val fTy = guessType(SearchContext(project))
            if (fTy is ITyFunction) {
                val info = SymbolInformation(
                        "${this.classMethodName.text}${fTy.mainSignature.paramSignature}",
                        SymbolKind.Method,
                        Location(file.uri.toString(), range.toRange(file))
                )
                info
            } else null
        }
        is LuaClassField -> {
            SymbolInformation(this.text, SymbolKind.Field, Location(file.uri.toString(), range.toRange(file)))
        }
        is LuaNameDef -> {
            val local = Location(file.uri.toString(), range.toRange(file))
            val info = SymbolInformation("local $name", SymbolKind.Variable, local)
            info
        }
        is LuaLocalFuncDef -> {
            val local = Location(file.uri.toString(), range.toRange(file))
            val information = SymbolInformation("local function $name", SymbolKind.Function, local)
            information
        }
        is LuaFuncDef -> {
            val local = Location(file.uri.toString(), range.toRange(file))
            val information = SymbolInformation("function $name", SymbolKind.Function, local)
            information
        }
        else -> null
    }
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

private val encodeMap = mapOf(
        " " to "%20",
        "[" to "%5b",
        "]" to "%5d",
        "!" to "%21",
        "#" to "%23",
        "$" to "%24",
        "%" to "%25",
        "+" to "%2B",
        "@" to "%40",
        ":" to "%3A",
        "=" to "%3D",
        "?" to "%3F"
)

fun safeURIName(name: String): String {
    var ret = name
    encodeMap.map { ret = ret.replace(it.key, it.value) }
    return ret
}