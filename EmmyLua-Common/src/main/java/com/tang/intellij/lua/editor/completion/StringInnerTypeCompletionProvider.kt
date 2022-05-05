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
import org.eclipse.lsp4j.CompletionItemKind


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
                                if (paramType is TySerializedGeneric && paramType.base == STRING) {
//                                    val oriFile = session.parameters.originalFile.virtualFile as ILuaFile
                                    val oriPos =
                                        session.parameters.originalFile.findElementAt(session.parameters.offset)

                                    val innerType = paramType.params.firstOrNull()
                                    if (oriPos != null && innerType is ITyClass) {
                                        val content = LuaString.getContent(oriPos.text)

                                        guessPrefixType(
                                            innerType,
                                            searchContext,
                                            content.value
                                        ).forEach { prefixType ->
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
                        override fun process(
                            element: LuaLookupElement,
                            member: LuaClassMember,
                            memberTy: ITy?
                        ): LookupElement {
                            element.lookupString = prefix + element.lookupString
                            if (element.kind == CompletionItemKind.Method) {
                                element.insertText = prefix + element.insertText
                            }
                            return PrioritizedLookupElement.withPriority(element, 10.0)
                        }
                    }
                )
            }
        }
    }

    private fun getPrefix(content: String): String {
        if (!content.contains('.')) {
            return ""
        }

        val indexFields = content.split('.').toMutableList()
        indexFields[indexFields.size - 1] = ""
        return indexFields.joinToString(".")
    }

    private fun guessPrefixType(baseType: ITyClass, context: SearchContext, content: String): MutableList<ITyClass> {
        if (!content.contains('.')) {
            return mutableListOf(baseType)
        }

        val fields = content.split('.');
        val result = mutableListOf<ITyClass>()
        innerGuessPrefixType(baseType, context, fields, 0, result)
        return result
    }

    private fun innerGuessPrefixType(
        type: ITy,
        context: SearchContext,
        fields: List<String>,
        i: Int,
        result: MutableList<ITyClass>
    ) {
        when (type) {
            is TyClass -> {
                val member = type.findMember(fields[i], context) ?: return;
                val guessType = member.guessType(context)
                if (i == fields.size - 2) {
                    when (guessType) {
                        is ITyClass -> {
                            result.add(guessType)
                        }
                        is TyUnion -> {
                            guessType.eachTopClass(Processor {
                                result.add(it)
                            })
                        }
                    }
                } else {
                    innerGuessPrefixType(guessType, context, fields, i + 1, result)
                }
            }
            is TyUnion -> {
                type.eachTopClass(Processor {
                    innerGuessPrefixType(it, context, fields, i, result)
                    true
                })
            }
        }


    }
}