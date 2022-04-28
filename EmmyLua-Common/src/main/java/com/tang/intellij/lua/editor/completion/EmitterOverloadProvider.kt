package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
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
                while (child != null) {
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
                            ty.process(Processor { sig ->
                                sig.params.firstOrNull()?.let {
                                    val paramType = it.ty
                                    if (paramType is TyStringLiteral) {
                                        addOverload(psi, paramType, sig, completionResultSet)
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

    private fun addOverload(
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
            element.isOverloadConst = true
            element.kind = CompletionItemKind.Constant
            completionResultSet.addElement(element)
        }
    }
//    private fun addEnum(
//        luaType: ITyClass,
//        searchContext: SearchContext,
//        completionResultSet: CompletionResultSet
//    ) {
//        luaType.lazyInit(searchContext)
//        luaType.processMembers(searchContext) { curType, member ->
//            ProgressManager.checkCanceled()
//            member.name?.let {
//                val name = "${luaType.className}.${member.name}"
//                if (completionResultSet.prefixMatcher.prefixMatches(name)) {
//                    addEnumField(completionResultSet, member, name, curType)
//                }
//            }
//
//        }
//    }
//
//    private fun addEnumField(
//        completionResultSet: CompletionResultSet,
//        member: LuaClassMember,
//        name: String,
//        fieldType: ITyClass
//    ) {
//
//        if (member is LuaClassField) {
//            val element =
//                LookupElementFactory.createFieldLookupElement(fieldType.className, name, member, fieldType, true)
//            element.kind = CompletionItemKind.Enum
//            completionResultSet.addElement(element)
//        }
//    }

}