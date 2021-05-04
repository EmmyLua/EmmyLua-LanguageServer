package com.tang.vscode.formatter

class FormattingBlockEnv(val ctx: FormattingContext, val indent: Int) {
    // 当前行已经使用的字符数
    var equipOperatorAlignment = false
    var equipOperatorAlignmentIndent = 0

    public fun print(text: String, autoAlignment: Boolean = true) {
        val alignmentCharacter = ctx.currentLineWidth
        if (alignmentCharacter < indent) {
            ctx.sb.append(FormattingOptions.emptyWhite.repeat(indent - alignmentCharacter))
            ctx.currentLineWidth += indent - alignmentCharacter
        }

        val lastLineSeparatorIndex = text.lastIndexOf(FormattingOptions.lineSeparator)

        // 表示没找到换行符
        if (lastLineSeparatorIndex == -1) {
            ctx.currentLineWidth += text.length
            ctx.sb.append(text)
        } else {
            if (autoAlignment) {
                if (text == FormattingOptions.lineSeparator) {
                    ctx.sb.append(FormattingOptions.lineSeparator)
                    ctx.currentLineWidth = 0
                    return
                }


                val texts = text.split(FormattingOptions.lineSeparator).map { it -> it.trimStart() }
                for (index in texts.indices) {
                    if (index != texts.size - 1) {
                        ctx.sb.append(texts[index]).append(FormattingOptions.lineSeparator)
                        if (index == texts.size - 2 && texts[texts.size - 1].isEmpty()) {
                            ctx.currentLineWidth = 0
                            return
                        }
                        ctx.sb.append(FormattingOptions.emptyWhite.repeat(indent))
                    } else {
                        ctx.sb.append(texts[index])
                        ctx.currentLineWidth = texts[index].length
                    }
                }
            } else {
                ctx.sb.append(text)
                ctx.currentLineWidth = text.length - lastLineSeparatorIndex - FormattingOptions.lineSeparator.length
            }
        }
    }
}