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

package com.tang.intellij.lua.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.tang.intellij.lua.comment.psi.LuaDocElementType
import com.tang.intellij.lua.comment.psi.LuaDocTypes
import com.tang.intellij.lua.comment.psi.impl.LuaCommentImpl
import com.tang.intellij.lua.lexer.LuaLexer
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.psi.LuaElementType
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.psi.LuaTokenType
import com.tang.intellij.lua.psi.LuaTypes

/**
 * Created by TangZhiXu on 2015/11/15.
 * Email:272669294@qq.com
 */
class LuaParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer {
        return LuaLexer()
    }

    override fun getWhitespaceTokens(): TokenSet {
        return WHITE_SPACES
    }

    override fun getCommentTokens(): TokenSet {
        return COMMENTS
    }

    override fun getStringLiteralElements(): TokenSet {
        return STRINGS
    }

    override fun createParser(project: Project): PsiParser {
        return LuaParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
        return ParserDefinition.SpaceRequirements.MAY
    }

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            LuaElementType.DOC_COMMENT -> LuaCommentImpl(node)
            FILE -> LuaPsiFile(node)
            is LuaDocElementType -> LuaDocTypes.Factory.createElement(node)
            else -> LuaTypes.Factory.createElement(node)
        }
    }

    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS = TokenSet.create(LuaTypes.SHORT_COMMENT, LuaTypes.BLOCK_COMMENT, LuaTypes.DOC_COMMENT, LuaTypes.REGION, LuaTypes.ENDREGION)
        val STRINGS = TokenSet.create(LuaTypes.STRING)

        val FILE = IFileElementType(LuaLanguage.INSTANCE)
    }
}

fun createType(string: String): IElementType {
    return LuaElementType(string)
}

fun createToken(string: String): IElementType {
    return if (string == "DOC_COMMENT") LuaElementType.DOC_COMMENT else LuaTokenType(string)
}

fun createDocType(string: String): IElementType {
    return LuaDocElementType(string)
}