package com.tang.vscode.api.impl

import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer.LuaLexer
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.stubs.IndexSink
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.utils.toRange
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import java.net.URI

internal data class Line(val line: Int, val startOffset:Int, val stopOffset: Int, val str: String)

class LuaFile(override val uri: URI) : VirtualFileBase(uri), ILuaFile, VirtualFile {
    private var _text: String = ""
    private var _lines = mutableListOf<Line>()
    private var _myPsi: LuaPsiFile? = null

    override val diagnostics = mutableListOf<Diagnostic>()

    override fun didChange(params: DidChangeTextDocumentParams) {
        if (params.contentChanges.isEmpty())
            return

        var sb = _text
        var offset = 0
        params.contentChanges.forEach {
            if (it.range.start.line >= _lines.size) {
                sb += "\r\n${it.text}"
                _lines.add(Line(it.range.start.line, it.range.start.character, it.range.end.character, it.text))
            } else {
                val sline = _lines[it.range.start.line]
                val eline = _lines[it.range.end.line]
                val spos = sline.startOffset + it.range.start.character
                val epos = eline.startOffset + it.range.end.character
                sb = sb.replaceRange(spos, epos, it.text)

                val textSize = it.text.length
                offset += textSize - it.rangeLength
            }
        }
        _text = sb
        onChanged()
    }

    override fun getText(): String {
        return _text
    }

    fun setText(str: String) {
        _text = str
        onChanged()
    }

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
                val str = _text.substring(lineStart, pos)
                val line = Line(lineCount++, lineStart, pos, str)
                _lines.add(line)
                lineStart = pos + lbSize
                pos += lbSize
            } else if (pos == length - 1) {
                val str = _text.substring(lineStart, length)
                val line = Line(lineCount, lineStart, pos, str)
                _lines.add(line)
                break
            } else pos++
        }
    }

    private fun onChanged() {
        updateLines()
        doParser()
    }

    private fun doParser() {
        diagnostics.clear()
        _myPsi?.let { IndexSink.removeStubs(it) }
        val parser = LuaParser()
        val builder = PsiBuilderFactory.getInstance().createBuilder(LuaParserDefinition(), LuaLexer(), text)
        val node = parser.parse(LuaParserDefinition.FILE, builder)
        val psi = node.psi
        _myPsi = psi as LuaPsiFile
        _myPsi?.virtualFile = this
        PsiTreeUtil.processElements(psi, {
            if (it is PsiErrorElement) {
                val diagnostic = Diagnostic()
                diagnostic.message = it.errorDescription
                diagnostic.severity = DiagnosticSeverity.Error
                diagnostic.range = it.textRange.toRange(this)
                diagnostics.add(diagnostic)
            }
            true
        })
    }

    private fun getLineStart(line: Int): Int {
        return _lines.firstOrNull { it.line == line } ?.startOffset ?: 0
    }

    override fun getLine(offset: Int): Pair<Int, Int> {
        val line = _lines.firstOrNull { it.startOffset <= offset && it.stopOffset >= offset }
        if (line != null)
            return Pair(line.line, offset - line.startOffset)
        return Pair(0, 0)
    }

    override fun getPosition(line: Int, char: Int): Int {
        val lineData = _lines.firstOrNull { it.line == line }
        return if (lineData != null) lineData.startOffset + char else char
    }

    override val psi: PsiFile?
        get() = _myPsi

    /*override fun findNode(line: Int, character: Int): LuaNode? {
        val lineOffset = character + getLineStart(line)
        return chunkNode?.find(line, lineOffset)
    }*/
}