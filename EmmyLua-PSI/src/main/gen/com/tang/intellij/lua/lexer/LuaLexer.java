package com.tang.intellij.lua.lexer;

import com.intellij.lexer.FlexAdapter;

public class LuaLexer extends FlexAdapter {
    public LuaLexer() {
        super(new _LuaLexer());
    }
}
