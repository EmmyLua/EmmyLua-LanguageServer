package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.lookup.LookupElement
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.TextEdit

class LuaLookupElement(label: String) : LookupElement(label) {
    var kind = CompletionItemKind.Text
    var data:String? = null
    var insertText: String? = null
    var textEdit: TextEdit? = null

    val item: CompletionItem get() {
        val completionItem = CompletionItem(lookupString)
        completionItem.insertText = insertText
        completionItem.data = data
        completionItem.kind = kind
        completionItem.textEdit = textEdit
        return completionItem
    }
}