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
        return item
    }

    fun createFieldLookupElement(clazzName: String,
                                 name: String,
                                 field: LuaClassField,
                                 ty: ITy?,
                                 bold: Boolean): LuaLookupElement {
        return LuaLookupElement(name)
    }

    private fun buildSignatureCompletionItem(name: String, signature: IFunSignature): LuaLookupElement {
        val item = LuaLookupElement("$name${signature.paramSignature}")
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
        return item
    }

    fun createKeyWordLookupElement(keyWordToken: IElementType): LookupElement {
        val element = LookupElement(keyWordToken.toString())
        element.kind = CompletionItemKind.Keyword
        return element
    }
}