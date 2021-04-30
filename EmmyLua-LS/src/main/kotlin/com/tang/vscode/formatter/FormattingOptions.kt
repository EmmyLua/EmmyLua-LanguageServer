package com.tang.vscode.formatter

/**
 * 实际上选择越多并不是越好
 */
object FormattingOptions {
    //换行字符串
    val lineSeparator = System.lineSeparator()
    val emptyWhite = " "

    // 缩进设定，不会有人选择1缩进吧
    var indent = 4
    // 函数间距设定
    var functionSpacing = 1
    // 循环语句与上文间距设定
    var loopSpacing = 1
    // 表的行宽，超过该值,表会全部换行
    var tableLineWidth = 80
    
}