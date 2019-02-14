package com.tang.vscode.api.impl

import com.intellij.lang.PsiBuilderFactory
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.lang.LuaLanguageLevel
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer._LuaLexer
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaExprStat
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.stubs.IndexSink
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.utils.toRange
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import java.nio.file.Path

internal data class Line(val line: Int, val startOffset:Int, val stopOffset: Int)

class LuaFile(override val path: Path) : VirtualFileBase(path), ILuaFile, VirtualFile {
    private var _text: CharSequence = ""
    private var _lines = mutableListOf<Line>()
    private var _myPsi: LuaPsiFile? = null

    override val diagnostics = mutableListOf<Diagnostic>()

    @Synchronized
    override fun didChange(params: DidChangeTextDocumentParams) {
        if (params.contentChanges.isEmpty())
            return

        var sb = _text.toString()
        var offset = 0
        params.contentChanges.forEach {
            when {
                // for sublime lsp
                it.range == null -> sb = it.text
                // incremental updating
                it.range.start.line >= _lines.size -> {
                    sb += it.text
                    _lines.add(Line(it.range.start.line, it.range.start.character, it.range.end.character))
                }
                else -> {
                    val sline = _lines[it.range.start.line]
                    val eline = _lines[it.range.end.line]
                    val spos = sline.startOffset + it.range.start.character
                    val epos = eline.startOffset + it.range.end.character
                    sb = sb.replaceRange(spos, epos, it.text)

                    val textSize = it.text.length
                    offset += textSize - it.rangeLength
                }
            }
        }
        _text = sb
        onChanged()
    }

    override fun getText(): CharSequence {
        return _text
    }

    fun setText(str: CharSequence) {
        _text = str
        onChanged()
    }

    @Synchronized
    private fun updateLines() {
        _lines.clear()
        var pos = 0
        var lineCount = 0
        var lineStart = 0
        val length = _text.length
        while (pos < length) {
            val c = _text[pos]
            val rn = c == '\r' && _text.getOrNull(pos + 1) == '\n'

            if (c == '\n' || rn) {
                val lbSize = if (rn) 2 else 1
                val line = Line(lineCount++, lineStart, pos)
                _lines.add(line)
                lineStart = pos + lbSize
                pos += lbSize
            } else pos++

            if (pos >= length) {
                val line = Line(lineCount, lineStart, pos)
                _lines.add(line)
                break
            }
        }
    }

    private fun onChanged() {
        updateLines()
        doParser()
    }

    private fun doParser() {
        diagnostics.clear()
        unindex()
        val parser = LuaParser()
        val builder = PsiBuilderFactory.getInstance().createBuilder(
                LuaParserDefinition(),
                FlexAdapter(_LuaLexer(LuaLanguageLevel.LUA54)),
                text
        )
        val node = parser.parse(LuaParserDefinition.FILE, builder)
        val psi = node.psi
        _myPsi = psi as LuaPsiFile
        _myPsi?.virtualFile = this
        PsiTreeUtil.processElements(psi) {
            if (it is PsiErrorElement) {
                val diagnostic = Diagnostic()
                diagnostic.message = it.errorDescription
                diagnostic.severity = if (it.parent is LuaDocPsiElement) DiagnosticSeverity.Warning else DiagnosticSeverity.Error
                diagnostic.range = it.textRange.toRange(this)
                diagnostics.add(diagnostic)
            } else if (it is LuaExprStat) {
                if (it.expr !is LuaCallExpr && PsiTreeUtil.findChildOfType(it, PsiErrorElement::class.java) == null) {
                    val diagnostic = Diagnostic()
                    diagnostic.message = "non-complete statement"
                    diagnostic.severity = DiagnosticSeverity.Error
                    diagnostic.range = it.textRange.toRange(this)
                    diagnostics.add(diagnostic)
                }
            }
            true
        }
        index()
    }

    /*private fun getLineStart(line: Int): Int {
        return _lines.firstOrNull { it.line == line } ?.startOffset ?: 0
    }*/

    @Synchronized
    override fun getLine(offset: Int): Pair<Int, Int> {
        val line = _lines.firstOrNull { it.startOffset <= offset && it.stopOffset >= offset }
        if (line != null)
            return Pair(line.line, offset - line.startOffset)
        return Pair(0, 0)
    }

    @Synchronized
    override fun getPosition(line: Int, char: Int): Int {
        val lineData = _lines.firstOrNull { it.line == line }
        return if (lineData != null) lineData.startOffset + char else char
    }

    override val psi: PsiFile?
        get() = _myPsi

    override fun getPsiFile() = _myPsi

    override fun unindex() {
        _myPsi?.let { IndexSink.removeStubs(it) }
    }

    private fun index() {
        _myPsi?.let { com.tang.intellij.lua.stubs.index(it) }
    }
}