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

package com.tang.intellij.lua.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.util.Processor

object LuaPsiTreeUtilEx {

    fun walkUpNameDef(psi: PsiElement?, processor: Processor<PsiNamedElement>, nameExprProcessor: Processor<LuaAssignStat>? = null) {
        if (psi == null) return
        walkUpPsiLocalName(psi, processor, nameExprProcessor)
    }

    /**
     * 向上寻找 local 定义
     * @param element 当前搜索起点
     * @param processor 处理器
     */
    private fun walkUpPsiLocalName(element: PsiElement, processor: Processor<PsiNamedElement>, nameExprProcessor: Processor<LuaAssignStat>?) {
        var curr: PsiElement = element
        do {
            var continueSearch = true
            val prev = curr.prevSibling
            if (prev == null) {
                curr = curr.parent
                if (curr is LuaLocalDef) {
                    continue
                }
            } else curr = prev

            continueSearch = when (curr) {
                is LuaLocalDef -> resolveInNameList(curr.nameList, processor)
                is LuaParamNameDef -> processor.process(curr)
                is LuaLocalFuncDef -> processor.process(curr)
                is LuaAssignStat -> nameExprProcessor?.process(curr) ?: true
                else -> true
            }
        } while (continueSearch && curr !is PsiFile)
    }

    private fun resolveInNameList(nameList: LuaNameList?, processor: Processor<PsiNamedElement>): Boolean {
        if (nameList != null) {
            for (nameDef in nameList.nameDefList) {
                if (!processor.process(nameDef)) return false
            }
        }
        return true
    }

    fun walkUpLocalFuncDef(psi: PsiElement, processor: Processor<LuaLocalFuncDef>) {
        walkUpPsiLocalFunc(psi, processor)
    }

    /**
     * 向上寻找 local function 定义
     * @param current 当前搜导起点
     * @param processor 处理器
     */
    private fun walkUpPsiLocalFunc(current: PsiElement, processor: Processor<LuaLocalFuncDef>) {
        var continueSearch = true
        var curr = current
        do {
            if (curr is LuaLocalFuncDef)
                continueSearch = processor.process(curr)

            curr = curr.prevSibling ?: curr.parent
        } while (continueSearch && curr !is PsiFile)
    }

    fun processChildren(element: PsiElement?, processor: Processor<PsiElement>) {
        var child = element?.firstChild
        while (child != null) {
            if (!processor.process(child)) {
                break
            }
            child = child.nextSibling
        }
    }
}