package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.psi.LuaClassField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaPsiElement
import com.tang.intellij.lua.ty.IFunSignature
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.ITyFunction
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.InsertTextFormat
import javax.swing.Icon

object LookupElementFactory {

    fun createGuessableLookupElement(name: String, psi: LuaPsiElement, ty: ITy, icon: Icon?): LookupElement {
        return LuaLookupElement(name)
    }

    fun createFunctionLookupElement(name: String,
                                    psi: LuaPsiElement,
                                    signature: IFunSignature,
                                    bold: Boolean,
                                    ty: ITyFunction,
                                    icon: Icon?): LookupElement {
        val item = buildSignatureCompletionItem(name, signature)
        item.kind = CompletionItemKind.Function
        return item
    }

    fun createMethodLookupElement(clazzName: String,
                                  lookupString: String,
                                  classMember: LuaClassMember,
                                  signature: IFunSignature,
                                  bold: Boolean,
                                  isColonStyle: Boolean,
                                  fnTy: ITyFunction,
                                  icon: Icon?): LuaLookupElement {
        val item = buildSignatureCompletionItem(lookupString, signature)
        item.kind = CompletionItemKind.Method
        item.detail = "[$clazzName]"
        item.data = "$clazzName|${classMember.name}"
        return item
    }

    fun createFieldLookupElement(clazzName: String,
                                 name: String,
                                 field: LuaClassField,
                                 ty: ITy?,
                                 bold: Boolean): LuaLookupElement {
        val element = LuaLookupElement(name)
        element.data = "$clazzName|$name"
        return element
    }

    private fun buildSignatureCompletionItem(name: String, signature: IFunSignature): LuaLookupElement {
        val item = LuaLookupElement("$name${signature.paramSignature}")
        if (signature.params.isEmpty()) {
            item.insertText = "$name()"
        } else {
            item.insertText = name
            /*item.insertText = buildString {
                append(name)
                append("(")
                signature.params.forEachIndexed { index, info ->
                    if (index != 0) append(", ")
                    append("\${${index + 1}:${info.name}}")
                }
                append(")")
            }
            item.insertTextFormat = InsertTextFormat.Snippet*/
        }
        return item
    }

    fun createKeyWordLookupElement(keyWordToken: IElementType): LookupElement {
        val element = LookupElement(keyWordToken.toString())
        element.kind = CompletionItemKind.Keyword
        return element
    }
}