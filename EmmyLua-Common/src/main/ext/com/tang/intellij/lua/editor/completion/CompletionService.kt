package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer.LuaLexer
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.psi.LuaPsiFile
import org.eclipse.lsp4j.CompletionItem

class CompletionResultSetImpl(private val consumer: Consumer<CompletionItem>) : CompletionResultSet() {
    private val set = mutableSetOf<String>()
    override fun addElement(item: CompletionItem) {
        if (set.add(item.label))
            consumer.consume(item)
    }
}

object CompletionService {
    private val contributor = LuaCompletionContributor()

    private val docContributor = LuaDocCompletionContributor()

    fun collectCompletion(psi: PsiFile, pos: Int, consumer: Consumer<CompletionItem>) {
        //val element = psi.findElementAt(pos)
        val text = psi.text.replaceRange(pos, pos, "emmy")

        val parser = LuaParser()
        val builder = PsiBuilderFactory.getInstance().createBuilder(LuaParserDefinition(), LuaLexer(), text)
        val node = parser.parse(LuaParserDefinition.FILE, builder)
        val tempPsi = node.psi as LuaPsiFile
        tempPsi.virtualFile = psi.virtualFile
        val position = tempPsi.findElementAt(pos)

        val parameters = CompletionParameters()
        parameters.completionType = CompletionType.BASIC
        parameters.position = position!!
        parameters.originalFile = psi

        val result = CompletionResultSetImpl(consumer)
        result.prefixMatcher = CamelHumpMatcher("")

        parameters.originalFile.putUserData(CompletionSession.KEY, CompletionSession(parameters, result))

        contributor.fillCompletionVariants(parameters, result)
        docContributor.fillCompletionVariants(parameters, result)
    }
}