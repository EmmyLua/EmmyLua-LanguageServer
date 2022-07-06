package com.tang.vscode.documentation

import com.tang.vscode.formatter.FormattingOptions

class FormatDoc(val sb: StringBuilder, var indent: Int) {
    fun write(text: String) {
        val lastLineSeparatorIndex = text.lastIndexOf('\n')
        val empty = " "
        // 表示没找到换行符
        if (lastLineSeparatorIndex == -1) {
            sb.append(text)
        } else {
            val texts = text.split('\n').map { it -> it.trimStart() }
            for (index in texts.indices) {
                if (index != texts.size - 1) {
                    sb.append(texts[index]).append("\n")
                    sb.append(empty.repeat(indent))
                } else {
                    sb.append(texts[index])
                }
            }
        }
    }

    fun writeLine(text: String){
        write(text)
        sb.append("\n")
        sb.append(" ".repeat(indent))
    }
}