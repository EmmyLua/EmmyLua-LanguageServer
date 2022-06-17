package com.tang.vscode.api.impl

import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import com.tang.intellij.lua.lang.LuaLanguageLevel
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer.LuaLexer
import com.tang.intellij.lua.lexer._LuaLexer
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.IndexSink
import com.tang.intellij.lua.ty.ITyFunction
import com.tang.intellij.lua.ty.TyClass
import com.tang.intellij.lua.ty.findPerfectSignature
import com.tang.intellij.lua.ty.hasVarargs
import com.tang.lsp.*
import com.tang.vscode.RenderRange
import com.tang.vscode.diagnostics.DiagnosticsService
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal data class Line(val line: Int, val startOffset: Int, val stopOffset: Int)

class LuaFile(override val uri: FileURI) : VirtualFileBase(uri), ILuaFile, VirtualFile {
    private var _text: CharSequence = ""
    private var _lines = mutableListOf<Line>()
    private var _myPsi: LuaPsiFile? = null
    private var _words: List<Word>? = null
    private var _version: Int = 0
    private var _rwl = ReentrantReadWriteLock()

    var workspaceDiagnosticResultId: String? = null

    override fun didChange(params: DidChangeTextDocumentParams) {
        _rwl.write {
            if (params.contentChanges.isEmpty())
                return

            var sb = _text.toString()
            var offset = 0
            params.contentChanges.forEach {
                when {
                    // for TextDocumentSyncKind.Full
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
    }

    override fun getText(): CharSequence {
        return _text
    }

    override fun getPath(): String {
        return uri.toString()
    }

    fun setText(str: CharSequence) {
        _rwl.write {
            _text = str
            onChanged()
        }
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
        ++_version
        updateLines()
        doParser()
    }

    private fun doParser() {
        _words = null
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

        index()
    }


    /*private fun getLineStart(line: Int): Int {
        return _lines.firstOrNull { it.line == line } ?.startOffset ?: 0
    }*/

    override fun getLine(offset: Int): Pair<Int, Int> {
        if (_lines.size <= 1) {
            return Pair(0, offset)
        }
        var lowIndex = 0
        var highIndex = _lines.size - 1
        var currentLine: Line? = null
        while (lowIndex <= highIndex) {
            val index = (lowIndex + highIndex) / 2
            val line = _lines[index]
            if (line.startOffset <= offset && line.stopOffset >= offset) {
                currentLine = line
                break
            }
            if (offset < line.startOffset) {
                highIndex = index - 1
            } else {
                lowIndex = index + 1
            }
        }

        if (currentLine != null) {
            //如果找到了
            return Pair(currentLine.line, offset - currentLine.startOffset)
        } else {
            // 没找到那么就认为是lowIndex所在行第0个字符,也可以是currentLine所在行最后一个+1字符
            return Pair(_lines[lowIndex].line, 0)
        }
    }

    override fun getPosition(line: Int, char: Int): Int {
        val lineData = _lines.firstOrNull { it.line == line }
        var pos = if (lineData != null) lineData.startOffset + char else char
        if (pos >= _text.length) {
            pos = _text.length
        }
        return pos
    }

    override fun getVersion(): Int {
        return _version
    }

    override fun lock(code: () -> Unit) {
        _rwl.read {
            code()
        }
    }

    fun diagnostic(diagnostics: MutableList<Diagnostic>, checker: CancelChecker?) {
        DiagnosticsService.diagnosticFile(this, diagnostics, checker)
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

    override fun processWords(processor: (w: Word) -> Boolean) {
        if (_words == null) {
            val scanner = DefaultWordsScanner(
                LuaLexer(),
                TokenSet.EMPTY,
                TokenSet.EMPTY,
                TokenSet.EMPTY
            )
            val list = mutableListOf<Word>()
            scanner.processWords(this._text) {
                val hash = StringUtil.hashCode(it.baseText.subSequence(it.start, it.end))
                list.add(Word(hash, it.start, it.end))
                true
            }
            _words = list
        }
        _words?.let { words ->
            for (word in words) {
                if (!processor(word)) break
            }
        }
    }
}