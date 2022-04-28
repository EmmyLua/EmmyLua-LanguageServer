package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer
import com.tang.intellij.lua.configuration.IConfigurationManager
import com.tang.intellij.lua.editor.CaretImpl
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer.LuaLexer
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.psi.LuaPsiFile

class CompletionResultSetImpl(private val consumer: Consumer<LookupElement>) : CompletionResultSet() {
    override fun withPrefixMatcher(prefix: String): CompletionResultSet {
        val set = CompletionResultSetImpl(consumer)
        set.prefixMatcher = prefixMatcher.cloneWithPrefix(prefix)
        return set
    }

    private val set = mutableSetOf<String>()
    override fun addElement(item: LookupElement) {
        if (set.add(item.lookupString))
            consumer.consume(item)
    }
}

object CompletionService {
    private val contributors = arrayOf(
        LuaCompletionContributor(),
        SmartCompletionContributor(),
        LuaDocCompletionContributor()
    )

    fun collectCompletion(file: PsiFile, caret: Int, consumer: Consumer<LookupElement>) {
        val config = IConfigurationManager.get(file.project)
        val parameters = CompletionParameters()
        parameters.completionType = CompletionType.BASIC
        parameters.originalFile = file
        parameters.offset = caret
        val context = CompletionInitializationContext()
        context.file = file
        context.caret = CaretImpl(caret)
        context.startOffset = caret
        context.dummyIdentifier = CompletionInitializationContext.DUMMY_IDENTIFIER
        contributors.forEach { it.beforeCompletion(context) }
        parameters.position =
            if (context.dummyIdentifier.isEmpty()) file.findElementAt(caret) ?: return else insertDummyIdentifier(
                context
            )

        val text = file.text
        val result = CompletionResultSetImpl(consumer)
        val prefix = findPrefix(text, caret)
        result.prefixMatcher = CamelHumpMatcher(prefix, config.completionCaseSensitive)

        parameters.originalFile.putUserData(CompletionSession.KEY, CompletionSession(parameters, result))

        contributors.forEach { it.fillCompletionVariants(parameters, result) }
    }

    private fun insertDummyIdentifier(context: CompletionInitializationContext): PsiElement {
        val oriFile = context.file
        val pos = context.startOffset
        val text = oriFile.text.replaceRange(pos, pos, context.dummyIdentifier)
        val parser = LuaParser()
        val builder = PsiBuilderFactory.getInstance().createBuilder(LuaParserDefinition(), LuaLexer(), text)
        val node = parser.parse(LuaParserDefinition.FILE, builder)
        val copy = node.psi as LuaPsiFile
        copy.virtualFile = oriFile.virtualFile
        val position = copy.findElementAt(pos)
        context.file = copy
        return position!!
    }

    private fun findPrefix(text: String, pos: Int): String {
        var i = pos
        while (i > 0) {
            val c = text[i - 1]
            if (!c.isJavaIdentifierPart() && !c.isJavaIdentifierStart()) {
                break
            }
            i--
        }
        return text.substring(i, pos)
    }
}