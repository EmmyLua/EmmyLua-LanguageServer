package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaTypes
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.lsp.ILuaFile
import org.eclipse.lsp4j.CompletionItemKind

class CallbackCompletionProvider : LuaCompletionProvider() {
    override fun addCompletions(session: CompletionSession) {
        val completionParameters = session.parameters
        val completionResultSet = session.resultSet

        val psi = completionParameters.position
        val callExpr = psi.parent.parent.parent

        if (callExpr is LuaCallExpr) {
            var activeParameter = 0
            var nCommas = 0

            callExpr.args.firstChild?.let { firstChild ->
                var child: PsiElement? = firstChild
                while (child != null && child != psi) {
                    if (child.node.elementType == LuaTypes.COMMA) {
                        activeParameter++
                        nCommas++
                    }
                    child = child.nextSibling
                }
            }
            val searchContext = SearchContext.get(callExpr.project)
            callExpr.guessParentType(searchContext).let { parentType ->
                parentType.each { ty ->
                    if (ty is ITyFunction) {
                        val activeSig = ty.findPerfectSignature(callExpr, nCommas + 1)
                        if (activeParameter < activeSig.params.size) {
                            activeSig.params[activeParameter].let {
                                val paramType = it.ty
                                if (paramType is TyFunction) {
                                    addCallback(paramType, searchContext, completionResultSet)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun addCallback(
        luaType: TyFunction,
        searchContext: SearchContext,
        completionResultSet: CompletionResultSet
    ) {
        val params = luaType.mainSignature.params
        val element = LuaLookupElement("function(${params.map { it.name }.joinToString(", ")}) end")
        element.kind = CompletionItemKind.Function
        completionResultSet.addElement(element)
    }
}