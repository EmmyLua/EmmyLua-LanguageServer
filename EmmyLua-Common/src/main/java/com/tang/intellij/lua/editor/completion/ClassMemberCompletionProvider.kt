/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.Processor
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.TextEdit

enum class MemberCompletionMode {
    Dot,    // self.xxx
    Colon,  // self:xxx()
    All     // self.xxx && self:xxx()
}

/**

 * Created by tangzx on 2016/12/25.
 */
open class ClassMemberCompletionProvider : LuaCompletionProvider() {
    protected abstract class HandlerProcessor {
        open fun processLookupString(lookupString: String, member: LuaClassMember, memberTy: ITy?): String =
            lookupString
        open fun processCorrectElement(element: LuaLookupElement) : LookupElement = element

        open fun process(element: LuaLookupElement, member: LuaClassMember, memberTy: ITy?): LookupElement = element
    }

    override fun addCompletions(session: CompletionSession) {
        val completionParameters = session.parameters
        val completionResultSet = session.resultSet

        val psi = completionParameters.position
        val indexExpr = psi.parent

        if (indexExpr is LuaIndexExpr) {
            val isColon = indexExpr.colon != null
            val project = indexExpr.project
            val contextTy = LuaPsiTreeUtil.findContextClass(indexExpr)
            val context = SearchContext.get(project)
            val prefixType = indexExpr.guessParentType(context)
            val file = psi.containingFile.virtualFile
            if (!Ty.isInvalid(prefixType)) {
                complete(
                    isColon,
                    project,
                    contextTy,
                    prefixType,
                    completionResultSet,
                    completionResultSet.prefixMatcher,
                    object : HandlerProcessor() {
                        override fun processCorrectElement(element: LuaLookupElement): LookupElement {
                            if(element.lookupString.startsWith(":") && indexExpr.dot != null && file is ILuaFile) {
                                element.textEdit = TextEdit(indexExpr.dot!!.textRange.toRange(file), ":")
                            }
                            return element
                        }
                    }
                )
            }
            //smart
            /*val nameExpr = indexExpr.prefixExpr
            if (nameExpr is LuaNameExpr) {
                val colon = if (isColon) ":" else "."
                val prefixName = nameExpr.text
                val postfixName = indexExpr.name?.let { it.substring(0, it.indexOf(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)) }

                val matcher = completionResultSet.prefixMatcher.cloneWithPrefix(prefixName)
                LuaDeclarationTree.get(indexExpr.containingFile).walkUpLocal(indexExpr) { d ->
                    val it = d.firstDeclaration.psi0
                    val txt = it.name
                    if (it is LuaTypeGuessable && txt != null && prefixName != txt && matcher.prefixMatches(txt)) {
                        val type = it.guessType(context)
                        if (!Ty.isInvalid(prefixType)) {
                            val prefixMatcher = completionResultSet.prefixMatcher
                            val resultSet = completionResultSet.withPrefixMatcher("$prefixName*$postfixName")
                            complete(isColon, project, contextTy, type, resultSet, prefixMatcher, object : HandlerProcessor() {
                                override fun process(element: LuaLookupElement, member: LuaClassMember, memberTy: ITy?): LookupElement {
                                    element.itemText = txt + colon + element.itemText
                                    element.lookupString = txt + colon + element.lookupString
                                    return PrioritizedLookupElement.withPriority(element, -2.0)
                                }
                            })
                        }
                    }
                    true
                }
            }*/
        }
    }

    protected fun complete(
        isColon: Boolean,
        project: Project,
        contextTy: ITy,
        prefixType: ITy,
        completionResultSet: CompletionResultSet,
        prefixMatcher: PrefixMatcher,
        handlerProcessor: HandlerProcessor?
    ) {
        val mode = if (isColon) MemberCompletionMode.Colon else MemberCompletionMode.Dot
        prefixType.eachTopClass(Processor { luaType ->
            addClass(contextTy, luaType, project, mode, completionResultSet, prefixMatcher, handlerProcessor)
            true
        })
    }

    protected fun addClass(
        contextTy: ITy,
        luaType: ITyClass,
        project: Project,
        completionMode: MemberCompletionMode,
        completionResultSet: CompletionResultSet,
        prefixMatcher: PrefixMatcher,
        handlerProcessor: HandlerProcessor?
    ) {
        val context = SearchContext.get(project)
        luaType.processVisibleMembers(context, contextTy) { curType, member ->
            ProgressManager.checkCanceled()
            member.name?.let {
                if (prefixMatcher.prefixMatches(it)) {
                    addMember(
                        completionResultSet,
                        member,
                        curType,
                        luaType,
                        completionMode,
                        project,
                        handlerProcessor
                    )
                }
            }
        }
    }

    protected fun addMember(
        completionResultSet: CompletionResultSet,
        member: LuaClassMember,
        thisType: ITyClass,
        callType: ITyClass,
        completionMode: MemberCompletionMode,
        project: Project,
        handlerProcessor: HandlerProcessor?
    ) {
        val type = member.guessType(SearchContext.get(project))
        val bold = thisType == callType
        val className = thisType.displayName
        if (type is ITyFunction) {
            val fn = type.substitute(TySelfSubstitutor(project, null, callType))
            if (fn is ITyFunction)
                addFunction(
                    completionResultSet,
                    bold,
                    completionMode != MemberCompletionMode.Dot,
                    className,
                    member,
                    fn,
                    thisType,
                    callType,
                    handlerProcessor
                )
        } else if (member is LuaClassField) {
            if (type is TyUnion) {
                val fnType = type.getChildTypes().firstOrNull { it -> it is TyFunction }
                if (fnType != null) {
                    val fn = fnType.substitute(TySelfSubstitutor(project, null, callType))
                    if (fn is ITyFunction)
                        addFunction(
                            completionResultSet,
                            bold,
                            completionMode != MemberCompletionMode.Dot,
                            className,
                            member,
                            fn,
                            thisType,
                            callType,
                            handlerProcessor
                        )
                    return
                }
            }
            if (completionMode != MemberCompletionMode.Colon) {
                addField(completionResultSet, bold, className, member, type, handlerProcessor)
            }
        }
    }

    protected fun addField(
        completionResultSet: CompletionResultSet,
        bold: Boolean,
        clazzName: String,
        field: LuaClassField,
        ty: ITy?,
        handlerProcessor: HandlerProcessor?
    ) {
        val name = field.name
        if (name != null) {
            val element = LookupElementFactory.createFieldLookupElement(clazzName, name, field, ty, bold)
            val ele = handlerProcessor?.process(element, field, ty) ?: element
            completionResultSet.addElement(ele)
            return
        }
    }

    private fun addFunction(
        completionResultSet: CompletionResultSet,
        bold: Boolean,
        isColonStyle: Boolean,
        clazzName: String,
        classMember: LuaClassMember,
        fnTy: ITyFunction,
        thisType: ITyClass,
        callType: ITyClass,
        handlerProcessor: HandlerProcessor?
    ) {
        val name = classMember.name
        if (name != null) {
            fnTy.process(Processor {
                val context = SearchContext.get(classMember.project)
                val firstParam = it.getFirstParam(thisType, isColonStyle)
                val firstParamIsSelf = if (firstParam != null) {
                    callType.subTypeOf(firstParam.ty, context, true)
                } else {
                    false
                }

                if (isColonStyle && !firstParamIsSelf) {
                    return@Processor true
                }

                val lookupString = handlerProcessor?.processLookupString(name, classMember, fnTy) ?: name
                // basic
                val element = LookupElementFactory.createMethodLookupElement(
                    clazzName,
                    lookupString,
                    classMember,
                    it,
                    bold,
                    isColonStyle,
                    fnTy,
                    LuaIcons.CLASS_METHOD
                )
                val ele = handlerProcessor?.process(element, classMember, fnTy) ?: element
                completionResultSet.addElement(ele)
                // correction completion
                if (!isColonStyle && firstParamIsSelf
                    && callType != Ty.STRING
                    // workaround now
                    && callType is TyClass
                    && callType !is TySerializedClass
                    && callType !is TyTable
                    && callType != TyClass.G
                ) {
                    val colonElement = LookupElementFactory.createShouldBeMethodLookupElement(
                        clazzName,
                        lookupString,
                        classMember,
                        it,
                        bold,
                        fnTy,
                        LuaIcons.CLASS_METHOD
                    )
                    val colonEle = handlerProcessor?.processCorrectElement(colonElement) ?: colonElement
                    completionResultSet.addElement(colonEle)
                }
                true
            })
        }
    }
}
