package com.tang.vscode.formatter

/**
 * 实际上选择越多并不是越好
 */
object FormattingOptions {
    // 缩进设定，不会有人选择1缩进吧
    var indent = 4
    // 函数间距设定
    var functionSpacing = 1
    // 循环语句与上文间距设定
    var loopSpacing = 1
    // 行宽，该设定并非针对全部的语句，也不是严格按照这个标准
    var lineWidth = 80

    fun getIndentString(level: Int): String {
        return " ".repeat(indent * level)
    }
}