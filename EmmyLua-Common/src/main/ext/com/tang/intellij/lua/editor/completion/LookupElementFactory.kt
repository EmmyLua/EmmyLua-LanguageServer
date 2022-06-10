@file:Suppress("UNUSED_PARAMETER", "unused")

package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.LuaClassField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaParamInfo
import com.tang.intellij.lua.psi.LuaPsiElement
import com.tang.intellij.lua.ty.IFunSignature
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.ITyFunction
import com.tang.intellij.lua.ty.hasVarargs
import com.tang.lsp.ILuaFile
import org.eclipse.lsp4j.CompletionItemKind
import javax.swing.Icon

object LookupElementFactory {

    fun createGuessableLookupElement(name: String, psi: LuaPsiElement, ty: ITy, icon: Icon?): LookupElement {
        val element = LuaLookupElement(name)
        element.kind = when (icon) {
            LuaIcons.LOCAL_VAR -> {
                CompletionItemKind.Variable
            }
            LuaIcons.PARAMETER -> {
                CompletionItemKind.TypeParameter
            }
            else -> {
                CompletionItemKind.Value
            }
        }
        element.additionDetailDescription = ty.displayName
        return element
    }

    fun createFunctionLookupElement(
        name: String,
        psi: LuaPsiElement,
        signature: IFunSignature,
        bold: Boolean,
        ty: ITyFunction,
        icon: Icon?
    ): LookupElement {
        val item = buildSignatureCompletionItem(name, signature, false)
        item.kind = CompletionItemKind.Function

        return item
    }

    fun createMethodLookupElement(
        clazzName: String,
        lookupString: String,
        classMember: LuaClassMember,
        signature: IFunSignature,
        bold: Boolean,
        isColonStyle: Boolean,
        fnTy: ITyFunction,
        icon: Icon?
    ): LuaLookupElement {
        val item = buildSignatureCompletionItem(lookupString, signature, isColonStyle)
        item.kind = CompletionItemKind.Method
        item.itemText = "[$clazzName]"
        val file = classMember.containingFile?.virtualFile as? ILuaFile
        if (file != null) {
            item.data = "${file.uri}|${classMember.textOffset}"
        }
        if (classMember.isDeprecated) {
            item.deprecated = true
        }

        return item
    }

    fun createShouldBeMethodLookupElement(
        clazzName: String,
        lookupString: String,
        classMember: LuaClassMember,
        signature: IFunSignature,
        bold: Boolean,
        fnTy: ITyFunction,
        icon: Icon?
    ): LuaLookupElement {
        val item = buildSignatureCompletionItem(lookupString, signature, true)
        item.lookupString = ":${item.lookupString}"
        item.kind = CompletionItemKind.Method
        item.itemText = "[$clazzName]"
        val file = classMember.containingFile?.virtualFile as? ILuaFile
        if (file != null) {
            item.data = "${file.uri}|${classMember.textOffset}"
        }
        if (classMember.isDeprecated) {
            item.deprecated = true
        }

        return item
    }

    fun createFieldLookupElement(
        clazzName: String,
        name: String,
        field: LuaClassField,
        ty: ITy?,
        bold: Boolean
    ): LuaLookupElement {
        val element = LuaLookupElement(name)
        element.kind = CompletionItemKind.Field
        element.additionDetailDescription = ty?.displayName
        val file = field.containingFile?.virtualFile as? ILuaFile
        if (file != null) {
            element.data = "${file.uri}|${field.textOffset}"
        }
        if (field.isDeprecated) {
            element.deprecated = true
        }

        return element
    }

    private fun buildSignatureCompletionItem(
        name: String,
        signature: IFunSignature,
        isColonStyle: Boolean
    ): LuaLookupElement {
        var pIndex = 0
        val params = mutableListOf<LuaParamInfo>()
        if (isColonStyle) { //a:b()
            if (!signature.colonCall) { // function a.b() end
                pIndex++
            }
        } else { //a.b()
            if (signature.colonCall) { // function a:b() end
                params.add(LuaParamInfo.createSelf())
            }
        }
        params.addAll(signature.params)

        val item = LuaLookupElement(name)
        item.additionDetail = buildString {
            append("(")
            val strings = mutableListOf<String>()
            for (i in pIndex until params.size) {
                val p = params[i]
                strings.add(p.name)
            }
            if (signature.hasVarargs()) {
                strings.add("...")
            }
            append(strings.joinToString(","))
            append(")")
        }
        item.additionDetailDescription = signature.returnTy.displayName

        if (pIndex >= params.size) {
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
        val element = LuaLookupElement(keyWordToken.toString())
        element.kind = CompletionItemKind.Keyword
        return element
    }
}