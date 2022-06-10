package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.lookup.LookupElement
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either

class LuaLookupElement(label: String) : LookupElement(label) {
    var kind = CompletionItemKind.Text
    var data: String? = null
    var insertText: String? = null
    var textEdit: TextEdit? = null
    var deprecated = false
    var isEnumMember = false
    var additionDetailDescription: String? = null
    var additionDetail: String? = null
}

val LookupElement.asCompletionItem: CompletionItem
    get() {
        val item = when {
            this is LuaLookupElement -> {
                val completionItem = CompletionItem(lookupString)
                completionItem.insertText = insertText
                completionItem.data = data
                completionItem.kind = kind

                if (textEdit != null) {
                    if (insertText != null) {
                        completionItem.additionalTextEdits = listOf(textEdit)
                    } else {
                        completionItem.textEdit = Either.forLeft(textEdit)
                    }
                }

                if (deprecated) {
                    completionItem.tags = listOf(CompletionItemTag.Deprecated)
                }
                if(additionDetailDescription != null || additionDetail != null){
                    val labelDetail = CompletionItemLabelDetails()
                    labelDetail.description = additionDetailDescription
                    labelDetail.detail = additionDetail
                    completionItem.labelDetails = labelDetail
                }

                completionItem
            }
            else -> CompletionItem(lookupString)
        }
        item.detail = this.itemText

        return item
    }