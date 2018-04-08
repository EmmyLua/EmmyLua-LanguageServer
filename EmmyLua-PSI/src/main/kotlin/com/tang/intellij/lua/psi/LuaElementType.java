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

package com.tang.intellij.lua.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.ICustomParsingType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.CharTable;
import com.tang.intellij.lua.comment.lexer.LuaDocLexer;
import com.tang.intellij.lua.comment.parser.LuaDocParser;
import com.tang.intellij.lua.lang.LuaLanguage;
import com.tang.intellij.lua.lang.LuaParserDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Created by TangZhiXu on 2015/11/15.
 * Email:272669294@qq.com
 */
public class LuaElementType extends IElementType {
    public LuaElementType(String debugName) {
        super(debugName, LuaLanguage.INSTANCE);
    }

    static class DocCommentType extends IElementType implements ICustomParsingType {
        DocCommentType() {
            super("DOC_COMMENT", LuaLanguage.INSTANCE);
        }

        @NotNull
        @Override
        public ASTNode parse(@NotNull CharSequence text, @NotNull CharTable table) {
            PsiParser parser = new LuaDocParser();
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(new LuaParserDefinition(), new LuaDocLexer(), text);
            return parser.parse(this, builder);
        }
    }

    public static IElementType DOC_COMMENT = new DocCommentType();
}
