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

@file:Suppress("UNUSED_PARAMETER", "unused")

package com.tang.intellij.lua.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 *
 * Created by TangZX on 2016/11/24.
 */
object LuaElementFactory {
    fun createFile(project: Project, content: String): LuaPsiFile {
        TODO()
    }

    fun createIdentifier(project: Project, name: String): PsiElement {
        TODO()
    }

    fun createLiteral(project: Project, value: String): LuaLiteralExpr {
        TODO()
    }

    fun createName(project: Project, name: String): PsiElement {
        TODO()
    }

    fun newLine(project: Project): PsiElement {
        TODO()
    }

    fun createWith(project: Project, code: String): PsiElement {
        TODO()
    }

    fun createDocIdentifier(project: Project, name: String): PsiElement {
        TODO()
    }
}
