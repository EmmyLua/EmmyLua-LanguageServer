package com.tang.vscode.formatter

import java.lang.StringBuilder

class FormattingContext {
    // 输出所用的
    public val sb = StringBuilder()
    public var currentLineWidth = 0

    private val blockEnvQueue: MutableList<FormattingBlockEnv> = mutableListOf()
    private var firstEnv = true


    public fun enterBlockEnv(indent: Int = -1, autoRoundToIndent: Boolean = false) {
        if (firstEnv) {
            firstEnv = false
            blockEnvQueue.add(FormattingBlockEnv(this, 0))
            return
        }


        val lastEnv = blockEnvQueue.last()
        var newIndent = if (indent == -1) lastEnv.indent + FormattingOptions.indent else indent

        if (FormattingOptions.alignToVerticalAlignmentLine && autoRoundToIndent) {
            val rest = newIndent % FormattingOptions.indent
            // 如果基本缩进为1,2 则不断向前对齐
            // 还有选3缩进的吗？？
            // 4缩进的时候也尽量向前对齐
            if (rest < 4) {
                newIndent -= rest
            } else {
                newIndent += FormattingOptions.indent - rest
            }
        }

        val newEnv = FormattingBlockEnv(this, newIndent)
        blockEnvQueue.add(newEnv)
    }

    public fun exitBlockEnv() {
        blockEnvQueue.removeAt(blockEnvQueue.lastIndex);
    }

    public fun print(text: String, autoAlignment: Boolean = true): FormattingContext {
        blockEnvQueue.last().print(text, autoAlignment)
        return this
    }

    public fun getFormattingResult(): String {
        return sb.toString()
    }

    public fun getCurrentCharacter(): Int {
        val indent = blockEnvQueue.last().indent
        return if (currentLineWidth <= indent) 0 else currentLineWidth - indent
    }

    public fun getCurrentIndent(): Int {
        return blockEnvQueue.last().indent
    }

    public fun getNextIndent(): Int {
        return getCurrentIndent() + FormattingOptions.indent
    }

    var equipOperatorAlignment: Boolean
        get() {
            return blockEnvQueue.isNotEmpty() && blockEnvQueue.last().equipOperatorAlignment
        }
        set(value) {
            if (blockEnvQueue.isNotEmpty()) {
                blockEnvQueue.last().equipOperatorAlignment = value
            }
        }

    // 这是一个相对缩进
    var equipOperatorAlignmentIndent: Int
        get() {
            return if (blockEnvQueue.isNotEmpty()) blockEnvQueue.last().equipOperatorAlignmentIndent else 0
        }
        set(value) {
            if (blockEnvQueue.isNotEmpty()) {
                blockEnvQueue.last().equipOperatorAlignmentIndent = value
            }
        }

}