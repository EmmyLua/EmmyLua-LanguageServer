package com.tang.intellij.lua.comment.lexer;

import com.intellij.lexer.FlexAdapter;

public class LuaDocLexer extends FlexAdapter {
    public LuaDocLexer() {
        super(new _LuaDocLexer());
    }
}
