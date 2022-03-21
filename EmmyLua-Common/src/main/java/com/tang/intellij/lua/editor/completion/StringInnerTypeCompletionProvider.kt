package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.tang.intellij.lua.lang.type.LuaString
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaTypes
import com.tang.intellij.lua.psi.guessClassType
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.intellij.lua.ty.Ty.Companion.STRING
import com.tang.lsp.ILuaFile


class StringInnerTypeCompletionProvider : ClassMemberCompletionProvider() {
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
                while (child != null) {
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
                        val active = ty.findPerfectSignature(nCommas + 1)
                        ty.process(Processor { sig ->
                            if (sig == active) {
                                if (activeParameter < sig.params.size) {
                                    sig.params[activeParameter].let {
                                        val paramType = it.ty
                                        if (paramType is TySerializedGeneric && paramType.base == STRING) {
                                            val oriFile = session.parameters.originalFile.virtualFile as ILuaFile
                                            val oriPos =
                                                session.parameters.originalFile.findElementAt(session.parameters.offset)

                                            val innerType = paramType.params.firstOrNull()
                                            if (oriPos != null && innerType is ITyClass) {
                                                val content = LuaString.getContent(oriPos.text)

                                                guessPrefixType(
                                                    innerType,
                                                    searchContext,
                                                    content.value
                                                )?.let { prefixType ->
                                                    addInnerClass(
                                                        prefixType,
                                                        getPrefix(content.value),
                                                        searchContext,
                                                        psi.project,
                                                        completionResultSet
                                                    )
                                                }


                                            }
                                        }
                                    }
                                }

                            }

                            true
                        })
                    }
                }
            }
        }
    }

    private fun addInnerClass(
        ty: ITyClass,
        prefix: String,
        searchContext: SearchContext,
        project: Project,
        completionResultSet: CompletionResultSet
    ) {

        ty.lazyInit(searchContext)
        ty.processVisibleMembers(searchContext, ty) { curType, member ->
            ProgressManager.checkCanceled()
            member.name?.let {
                addMember(
                    completionResultSet,
                    member,
                    curType,
                    ty,
                    MemberCompletionMode.Dot,
                    project,
                    object : HandlerProcessor() {
                        override fun process(element: LuaLookupElement, member: LuaClassMember, memberTy: ITy?): LookupElement {
                            element.lookupString = prefix + element.lookupString
                            return PrioritizedLookupElement.withPriority(element, 10.0)
                        }
                    }
                )
            }
        }
    }

    private fun getPrefix(content: String): String{
        if (!content.contains('.')) {
            return ""
        }

        val indexFields = content.split('.').toMutableList()
        indexFields[indexFields.size - 1] = ""
        return indexFields.joinToString(".")
    }

    private fun guessPrefixType(baseType: ITyClass, context: SearchContext, content: String): ITyClass? {
        if (!content.contains('.')) {
            return baseType
        }

        var type = baseType
        val indexFields = content.split('.')
        for (i in 0 until indexFields.size - 1) {
            val indexField = indexFields[i]
            val member = type.findMember(indexField, context) ?: return null
            val guessType = member.guessType(context)
            if (guessType !is ITyClass) {
                return null
            }

            type = guessType
        }
        return type
    }
}