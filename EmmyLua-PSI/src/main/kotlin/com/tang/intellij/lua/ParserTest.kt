package com.tang.intellij.lua

import com.intellij.core.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer.LuaLexer
import com.tang.intellij.lua.parser.LuaParser

fun main(args: Array<String>) {
    LanguageParserDefinitions.INSTANCE.register(LuaLanguage.INSTANCE, LuaParserDefinition())
    val parser = LuaParser()
    val code = """
        do
        a =
        end
    """.trimIndent()

    val builder = PsiBuilderFactory.getInstance().createBuilder(LuaParserDefinition(), LuaLexer(), code);
    val node = parser.parse(LuaParserDefinition.FILE, builder)
    val psi = node.psi
    psi.firstChild
    println(node)
}