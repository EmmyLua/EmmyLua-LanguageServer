package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaClassField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaTypes
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import org.eclipse.lsp4j.CompletionItemKind

class EnumCompletionProvider : LuaCompletionProvider() {
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
                                if (paramType is TyClass && paramType.isEnum) {
                                    addEnum(paramType, searchContext, completionResultSet)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addEnum(
        luaType: ITyClass,
        searchContext: SearchContext,
        completionResultSet: CompletionResultSet
    ) {
        luaType.lazyInit(searchContext)
        luaType.processMembers(searchContext) { curType, member ->
            ProgressManager.checkCanceled()
            member.name?.let {
                val name = "${luaType.className}.${member.name}"
                if (completionResultSet.prefixMatcher.prefixMatches(name)) {
                    addEnumField(completionResultSet, member, name, curType)
                }
            }

        }
    }

    private fun addEnumField(
        completionResultSet: CompletionResultSet,
        member: LuaClassMember,
        name: String,
        fieldType: ITyClass
    ) {

        if (member is LuaClassField) {
            val element =
                LookupElementFactory.createFieldLookupElement(fieldType.className, name, member, fieldType, true)
            element.kind = CompletionItemKind.EnumMember
            element.isEnumMember = true
            completionResultSet.addElement(element)
        }
    }

}