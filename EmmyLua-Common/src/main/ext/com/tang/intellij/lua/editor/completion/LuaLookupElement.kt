package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.lookup.LookupElement
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionItemTag
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either

class LuaLookupElement(label: String) : LookupElement(label) {
    var kind = CompletionItemKind.Text
    var data:String? = null
    var insertText: String? = null
    var textEdit: TextEdit? = null
    var deprecated = false
    var isEnumMember = false
}

val LookupElement.asCompletionItem: CompletionItem get() {
    val item = when {
        this is LuaLookupElement -> {
            val completionItem = CompletionItem(lookupString)
            completionItem.insertText = insertText
            completionItem.data = data
            completionItem.kind = kind
            completionItem.textEdit = Either.forLeft(textEdit)
            if(deprecated){
                completionItem.tags = listOf(CompletionItemTag.Deprecated)
            }
            completionItem
        }
        else -> CompletionItem(lookupString)
    }
    item.detail = this.itemText
    return item
}