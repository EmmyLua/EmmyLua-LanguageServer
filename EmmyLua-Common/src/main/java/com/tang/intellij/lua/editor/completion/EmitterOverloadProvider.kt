package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaTypes
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.TextEdit

class EmitterOverloadProvider : LuaCompletionProvider() {
    override fun addCompletions(session: CompletionSession) {
        val completionParameters = session.parameters
        val completionResultSet = session.resultSet

        val psi = completionParameters.position
        val callExpr = psi.parent.parent.parent

        if (callExpr is LuaCallExpr) {
            var activeParameter = 0

            callExpr.args.firstChild?.let { firstChild ->
                var child: PsiElement? = firstChild
                while (child != null && child != psi) {
                    if (child.node.elementType == LuaTypes.COMMA) {
                        activeParameter++
                    }
                    child = child.nextSibling
                }
            }
            if (activeParameter == 0) {
                val searchContext = SearchContext.get(callExpr.project)
                callExpr.guessParentType(searchContext).let { parentType ->
                    parentType.each { ty ->
                        if (ty is ITyFunction) {
                            val firstParam = ty.mainSignature.params.firstOrNull()
                            if(firstParam != null) {
                                if(firstParam.ty.subTypeOf(Ty.STRING, searchContext, true)) {
                                    ty.process(Processor { sig ->
                                        sig.params.firstOrNull()?.let {
                                            val paramType = it.ty
                                            if (paramType is TyStringLiteral) {
                                                addStringOverload(psi, paramType, sig, completionResultSet)
                                            }
                                        }
                                        true
                                    })
                                }
                                else if (firstParam.ty.subTypeOf(Ty.NUMBER, searchContext, true)){
                                    ty.process(Processor { sig ->
                                        sig.params.firstOrNull()?.let {
                                            val paramType = it.ty
                                            if (paramType is TyStringLiteral) {
                                                addNumberOverload(psi, paramType, sig, completionResultSet)
                                            }
                                        }
                                        true
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addStringOverload(
        psiElement: PsiElement,
        tyStringLiteral: TyStringLiteral,
        signature: IFunSignature,
        completionResultSet: CompletionResultSet
    ) {
        val file = psiElement.containingFile.virtualFile
        if(file is ILuaFile) {
            val newText = "\"${tyStringLiteral.content}\""
            val element = LuaLookupElement(newText)
            if(psiElement.node.elementType == LuaTypes.STRING) {
                element.textEdit = TextEdit(psiElement.textRange.toRange(file), newText)
            }
            element.isEnumMember = true
            element.kind = CompletionItemKind.EnumMember
            completionResultSet.addElement(element)
        }
    }

    private fun addNumberOverload(
        psiElement: PsiElement,
        tyStringLiteral: TyStringLiteral,
        signature: IFunSignature,
        completionResultSet: CompletionResultSet
    ) {
        if(psiElement.node.elementType != LuaTypes.STRING) {
            val element = LuaLookupElement(tyStringLiteral.content)
            element.isEnumMember = true
            element.kind = CompletionItemKind.EnumMember
            completionResultSet.addElement(element)
        }
    }

}