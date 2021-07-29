package com.tang.vscode.formatter

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.tang.lsp.ILuaFile
import kotlin.math.max
import kotlin.math.min

class FormattingFormatter(val file: ILuaFile, val psi: PsiFile) {
    private var fileElement: FormattingElement =
        FormattingElement(psi, FormattingType.Block, psi.textRange, mutableListOf());
    private val lineSeparator = FormattingOptions.lineSeparator
    private val emptyWhite = FormattingOptions.emptyWhite

    // 上下文变量
    private var ctx: FormattingContext = FormattingContext();

    fun add(element: PsiElement, elementType: FormattingType = FormattingType.General): FormattingElement {
        val formattingElement = FormattingElement(element, elementType, element.textRange, mutableListOf())
        return add(formattingElement)
    }

    fun add(formattingElement: FormattingElement): FormattingElement {
        add(fileElement.children, formattingElement)
        return formattingElement
    }

    fun add(list: MutableList<FormattingElement>, element: FormattingElement) {
        if (list.isEmpty()) {
            list.add(element)
        } else {
            for (index in list.lastIndex downTo 0) {
                if (element.textRange.startOffset >= list[index].textRange.endOffset) {
                    list.add(index + 1, element)
                    return
                } else if (element.textRange.endOffset <= list[index].textRange.endOffset
                    && element.textRange.startOffset >= list[index].textRange.startOffset
                ) {

                    add(list[index].children, element)
                    return
                }
            }
            list.add(0, element)
        }
    }

    fun findElement(list: MutableList<FormattingElement>, target: PsiElement): FormattingElement? {
        if (list.isNotEmpty()) {
            for (index in list.lastIndex downTo 0) {
                if (target.textRange.endOffset <= list[index].textRange.endOffset
                    && target.textRange.startOffset >= list[index].textRange.startOffset
                ) {
                    return if (target == list[index].psi) {
                        list[index]
                    } else {
                        findElement(list[index].children, target)
                    }
                }
            }
        }
        return null;
    }

    fun attachTo(target: PsiElement, comment: PsiElement): Boolean {
        val targetFormatElement = findElement(fileElement.children, target)
        if (targetFormatElement != null) {
            targetFormatElement.textRange =
                TextRange.create(targetFormatElement.textRange.startOffset, comment.textRange.endOffset)
            add(comment, FormattingType.Comment)
            return true
        }
        return false
    }

    fun getFormattingResult(): String {
        ctx = FormattingContext()
        printBlock(fileElement)
        return ctx.getFormattingResult()
    }

    private fun printElement(element: FormattingElement) {
        when (element.type) {
            FormattingType.Comment -> {
                printComment(element)
            }
            FormattingType.Function -> {
                printFunction(element)
            }
            FormattingType.LocalFunction -> {
                printLocalFunction(element)
            }
            FormattingType.Closure -> {
                printClosure(element)
            }
            FormattingType.DoBlock -> {
                printDoBlock(element)
            }
            FormattingType.IfStatement -> {
                printIfStatement(element)
            }
            FormattingType.Statement -> {
                printStatement(element)
            }
            FormattingType.RepeatStatement -> {
                printRepeatStatement(element)
            }
            FormattingType.WhileStatement -> {
                printWhileStatement(element)
            }
            FormattingType.ForAStatement -> {
                printForAStatement(element)
            }
            FormattingType.ForBStatement -> {
                printForBStatement(element)
            }
            FormattingType.LocalStatement -> {
                printLocalStatement(element)
            }
            FormattingType.BreakStatement -> {
                printBreakStatement(element)
            }
            FormattingType.ExprStatement -> {
                printExprStatement(element)
            }
            FormattingType.CallExpr -> {
                printCallExpr(element)
            }
            FormattingType.LiteralExpr -> {
                printLiteralExpr(element)
            }
            FormattingType.BinaryExpr -> {
                printBinaryExpr(element)
            }
            FormattingType.UnaryExpr -> {
                printUnaryExpr(element)
            }
            FormattingType.NamedExpr -> {
                printNameExpr(element)
            }
            FormattingType.IndexExpr -> {
                printIndexExpr(element)
            }
            FormattingType.AssignStatement -> {
                printAssignStatement(element)
            }
            FormattingType.ReturnStatement -> {
                printReturnStatement(element)
            }
            FormattingType.GotoStatement -> {
                printGotoStatement(element)
            }
            FormattingType.LabelStatement -> {
                printLabelStatement(element)
            }
            FormattingType.TableExpr -> {
                printTableExpr(element)
            }
            FormattingType.TableField -> {
                printTableField(element)
            }
            FormattingType.TableFieldSep -> {
                printTableFieldSep(element)
            }
            FormattingType.CallArgs -> {
                printCallArgs(element)
            }
            FormattingType.Expr -> {
                printExpr(element)
            }
            FormattingType.ParentExpr -> {
                printParentExpr(element)
            }
            FormattingType.ExprList -> {
                printExprList(element)
            }
            FormattingType.Operator -> {
                printOperator(element)
            }
            FormattingType.FunctionBody -> {
                printFunctionBody(element)
            }
            FormattingType.Block -> {
                printBlock(element)
            }
            FormattingType.Arg -> {
                printArg(element)
            }
            FormattingType.NameDefList -> {
                printNameDefList(element)
            }
            FormattingType.NameDef -> {
                printNameDef(element)
            }
            FormattingType.BinaryOperator -> {
                printBinaryOperator(element)
            }
            FormattingType.UnaryOperator -> {
                printUnaryOperator(element)
            }
            FormattingType.Id -> {
                printId(element)
            }
            FormattingType.SingleArg -> {
                printSingleArg(element)
            }
            FormattingType.Attribute -> {
                printAttribute(element)
            }
            FormattingType.KeyWorld -> {
                printKeyWorld(element)
            }
            else -> {
                ctx.print(element.psi.text)
            }
        }
    }

    private fun printComment(element: FormattingElement) {
        // 注释的排版最麻烦
        // 它可以穿插到很多地方注释里面还会存在多行注释
        val psi = element.psi
        if (psi.node.elementType.toString() == "BLOCK_COMMENT") {
            ctx.print(element.psi.text, false).print(lineSeparator)
        } else {
            ctx.print(psi.text).print(lineSeparator)
        }
    }

    private fun printFunction(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    val text = it.psi.text
                    when (text) {
                        "function" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "end" -> {
                            ctx.print(text)
                        }
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printLocalFunction(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    val text = it.psi.text
                    when (text) {
                        "local" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "function" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "end" -> {
                            ctx.print(text)
                        }
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printClosure(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    ctx.print(it.psi.text)
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printDoBlock(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    ctx.print(it.psi.text).print(lineSeparator)
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printIfStatement(element: FormattingElement) {

        val ifStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    val text = it.psi.text
                    when (text) {
                        "if" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "else" -> {
                            ctx.print(text).print(lineSeparator)
                        }
                        "elseif" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "then" -> {
                            ctx.print(emptyWhite).print(text).print(lineSeparator)
                        }
                        "end" -> {
                            ctx.print(text)
                        }
                    }
                }
                // 这是由于if语句块内的首行注释也被认为是if语句的注释,所以要特别处理
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - ifStartLine > 0) {
                        ctx.enterBlockEnv()
                        printElement(it)
                        ctx.exitBlockEnv()
                    } else {
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printWhileStatement(element: FormattingElement) {
        val whileStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    val text = it.psi.text
                    when (text) {
                        "while" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "do" -> {
                            ctx.print(emptyWhite).print(text).print(lineSeparator)
                        }
                        "end" -> {
                            ctx.print(text)
                        }
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - whileStartLine > 0) {
                        ctx.enterBlockEnv()
                        printElement(it)
                        ctx.exitBlockEnv()
                    } else {
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printRepeatStatement(element: FormattingElement) {
        val repeatStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    val text = it.psi.text
                    when (text) {
                        "repeat" -> {
                            ctx.print(text).print(lineSeparator)
                        }
                        "until" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - repeatStartLine > 0) {
                        ctx.enterBlockEnv()
                        printElement(it)
                        ctx.exitBlockEnv()
                    } else {
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printForAStatement(element: FormattingElement) {
        val forStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            val text = it.psi.text
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (text) {
                        "for" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "do" -> {
                            ctx.print(emptyWhite).print(text).print(lineSeparator)
                        }
                        "end" -> {
                            ctx.print(text)
                        }
                    }
                }
                FormattingType.Operator -> {
                    when (text) {
                        "," -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "=" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        else -> {
                            printElement(it)
                        }
                    }
                }
                FormattingType.Arg -> {
                    ctx.print(text).print(emptyWhite)
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - forStartLine > 0) {
                        ctx.enterBlockEnv()
                        printElement(it)
                        ctx.exitBlockEnv()
                    } else {
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printForBStatement(element: FormattingElement) {
        val forStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            val text = it.psi.text
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (text) {
                        "for" -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "in" -> {
                            ctx.print(emptyWhite).print(text).print(emptyWhite)
                        }
                        "do" -> {
                            ctx.print(emptyWhite).print(text).print(lineSeparator)
                        }
                        "end" -> {
                            ctx.print(text)
                        }
                    }
                }
                FormattingType.Operator -> {
                    when (text) {
                        "," -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        else -> {
                            printElement(it)
                        }
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - forStartLine > 0) {
                        ctx.enterBlockEnv()
                        printElement(it)
                        ctx.exitBlockEnv()
                    } else {
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printStatement(element: FormattingElement) {
        element.children.forEach {
            printElement(it)
        }
        ctx.print(lineSeparator)
    }

    private fun printLocalStatement(element: FormattingElement) {
        val endLine = file.getLine(element.textRange.endOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    ctx.print(it.psi.text).print(emptyWhite)
                }
                FormattingType.Operator -> {
                    val text = it.psi.text
                    when (text) {
                        "=" -> {
                            if (ctx.equipOperatorAlignment) {
                                ctx.enterBlockEnv(ctx.getCurrentIndent() + ctx.equipOperatorAlignmentIndent)
                                ctx.print(text).print(emptyWhite)
                                ctx.exitBlockEnv()
                            } else {
                                ctx.print(text).print(emptyWhite)
                            }
                        }
                        else -> {
                            ctx.print(text).print(emptyWhite)
                        }
                    }
                }
                FormattingType.NameDefList -> {
                    printElement(it)
                    ctx.print(emptyWhite)
                }
                FormattingType.Comment -> {
                    val commentLine = file.getLine(it.textRange.endOffset).first
                    if (commentLine >= endLine) {
                        ctx.print(emptyWhite).print(it.psi.text)
                    } else {
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printBreakStatement(element: FormattingElement) {
        ctx.print(element.psi.text).print(lineSeparator)
    }

    private fun printAssignStatement(element: FormattingElement) {
        val endLine = file.getLine(element.textRange.endOffset).first
        var leftExpr = true
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    val text = it.psi.text
                    when (text) {
                        "=" -> {
                            if (ctx.equipOperatorAlignment) {
                                ctx.enterBlockEnv(ctx.getCurrentIndent() + ctx.equipOperatorAlignmentIndent)
                                ctx.print(text).print(emptyWhite)
                                ctx.exitBlockEnv()
                            } else {
                                ctx.print(text).print(emptyWhite)
                            }
                        }
                        else -> {
                            ctx.print(text).print(emptyWhite)
                        }
                    }
                }
                FormattingType.ExprList -> {
                    if (leftExpr) {
                        printElement(it)
                        ctx.print(emptyWhite)
                        leftExpr = false
                    } else {
                        printElement(it)
                    }
                }
                FormattingType.Comment -> {
                    val commentLine = file.getLine(it.textRange.endOffset).first
                    if (commentLine >= endLine) {
                        // 那么这是赋值表达式尾部的注释 只会有一行
                        ctx.print(emptyWhite).print(it.psi.text)
                    } else {
                        // 赋值表达式上边的注释
                        printElement(it)
                    }
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printExprStatement(element: FormattingElement) {
        var inlineComment = false
        element.children.forEach {
            when (it.type) {
                FormattingType.Comment -> {
                    if (inlineComment) {
                        ctx.print(emptyWhite).print(it.psi.text)
                        return@forEach
                    }
                }
                else -> {
                    inlineComment = true
                }
            }

            printElement(it)
        }
        ctx.print(lineSeparator)
    }

    private fun printReturnStatement(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    ctx.print(it.psi.text).print(emptyWhite)
                }
                else -> {
                    printElement(it)
                }
            }

        }
        ctx.print(lineSeparator)
    }

    private fun printGotoStatement(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    ctx.print(it.psi.text).print(emptyWhite)
                }
                else -> {
                    printElement(it)
                }
            }
        }
        ctx.print(lineSeparator)
    }

    private fun printLabelStatement(element: FormattingElement) {
        element.children.forEach {
            printElement(it)
        }
        ctx.print(lineSeparator)
    }

    private fun printFunctionBody(element: FormattingElement) {
        // 试图分析出合理的对齐方式
        val firstArg = element.children.firstOrNull { it.type == FormattingType.Arg }
        val lastArg = element.children.lastOrNull { it.type == FormattingType.Arg }

        if (firstArg != null && lastArg != null) {
            val firstArgLine = file.getLine(firstArg.textRange.startOffset).first
            val lastArgLine = file.getLine(lastArg.textRange.endOffset).first
            if (firstArgLine != lastArgLine) {
                // 接下来判断是否对齐到等号
                val firstBracket =
                    element.children.firstOrNull { it.type == FormattingType.Operator && it.psi.text == "(" }
                if (firstBracket != null) {
                    val firstBracketLine = file.getLine(firstBracket.textRange.startOffset).first
                    printFunctionBodyAlignment(element, firstBracketLine == firstArgLine)
                    return
                }
            }
        }

        printFunctionBodyGeneral(element)
    }

    private fun printFunctionBodyGeneral(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    val text = it.psi.text
                    when (text) {
                        "," -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "(" -> {
                            ctx.print(text)
                        }
                        ")" -> {
                            ctx.print(text).print(lineSeparator)
                        }
                    }
                }
                FormattingType.Comment -> {
                    ctx.enterBlockEnv()
                    printElement(it)
                    ctx.exitBlockEnv()
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printFunctionBodyAlignment(element: FormattingElement, alignmentToBracket: Boolean) {
        loop@ for (index in element.children.indices) {
            val child = element.children[index]
            when (child.type) {
                FormattingType.Operator -> {
                    val text = child.psi.text
                    when (text) {
                        "," -> {
                            ctx.print(child.psi.text)
                            // 会试探逗号的下一个元素是不是和逗号在同一行如果是就不换行
                            if (index < element.children.lastIndex) {
                                val next = element.children[index + 1]
                                val commonLine = file.getLine(child.textRange.startOffset).first
                                val line = file.getLine(next.textRange.startOffset).first
                                if (line > commonLine) {
                                    ctx.print(lineSeparator)
                                    continue@loop
                                }
                            }
                            ctx.print(emptyWhite)

                        }
                        "(" -> {
                            ctx.print(text)
                            if (alignmentToBracket) {
                                ctx.enterBlockEnv(ctx.currentLineWidth)
                            } else {
                                ctx.print(lineSeparator)
                                ctx.enterBlockEnv()
                            }
                        }
                        ")" -> {
                            // 有些人喜欢右括号放参数后面，有些人喜欢括号换行后对齐
                            val lastArg = element.children.lastOrNull { it.type == FormattingType.Arg }
                            if (lastArg != null) {
                                val lastArgLine = file.getLine(lastArg.textRange.endOffset).first
                                val bracketLine = file.getLine(child.textRange.endOffset).first
                                if (lastArgLine == bracketLine) {
                                    ctx.print(text)
                                    ctx.exitBlockEnv()
                                    ctx.print(lineSeparator)
                                    continue@loop
                                } else {
                                    ctx.print(lineSeparator)
                                }
                            }

                            if (alignmentToBracket) {
                                ctx.print(text)
                                ctx.exitBlockEnv()
                            } else {
                                ctx.exitBlockEnv()
                                ctx.print(text)
                            }
                            ctx.print(lineSeparator)
                        }
                        else -> {
                            printElement(child)
                        }
                    }
                }
                FormattingType.Comment -> {
                    ctx.enterBlockEnv()
                    printElement(child)
                    ctx.exitBlockEnv()
                }
                else -> {
                    printElement(child)
                }
            }

        }
    }

    private fun printBlock(element: FormattingElement) {
        ctx.enterBlockEnv()
        var lastLine = -1
        var lastElement: FormattingElement? = null
        // 代表连续的local 或者assign语句的范围
        var localOrAssignRange: Pair<Int, Int>? = null

        for (index in element.children.indices) {
            val childElement = element.children[index]
            val startLineInfo = file.getLine(childElement.textRange.startOffset)
            val endLineInfo = file.getLine(childElement.textRange.endOffset)

            val startLine = startLineInfo.first
            val endLine = endLineInfo.first

            // 用于检查原始布局
            val lineDiff = startLine - lastLine
            val type = childElement.type

            // 行布局
            if (index != 0) {

                // 函数和前文定义之间插入空行
                if (type == FormattingType.Function || type == FormattingType.LocalFunction) {
                    if (lastElement?.type == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            ctx.print(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        ctx.print(lineSeparator.repeat(FormattingOptions.functionSpacing))
                    }
                } else if (type == FormattingType.IfStatement) {
                    if (lastElement?.type == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            ctx.print(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        // if 语句保持1行间距
                        ctx.print(lineSeparator)
                    }
                }
                // 在语句块和前文之间至少一个空格
                else if (type == FormattingType.ForAStatement || type == FormattingType.ForBStatement || type == FormattingType.RepeatStatement
                    || type == FormattingType.WhileStatement
                ) {
                    if (lastElement?.type == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            ctx.print(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        ctx.print(lineSeparator.repeat(FormattingOptions.loopSpacing))
                    }
                }
                // 赋值语句会根据情况空行
                else if (type == FormattingType.LocalStatement || type == FormattingType.AssignStatement || type == FormattingType.ExprStatement || type == FormattingType.Comment) {
                    val lastType = lastElement?.type
                    if (lastType == FormattingType.LocalStatement || lastType == FormattingType.AssignStatement || lastType == FormattingType.ExprStatement || lastType == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            ctx.print(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        ctx.print(lineSeparator)
                    }
                } else if (type == FormattingType.ReturnStatement) {
                    if (lineDiff > 1) {
                        // 保持原始布局
                        ctx.print(lineSeparator.repeat(lineDiff - 1))
                    }
                }
            }

            // 列布局
            if (type == FormattingType.LocalStatement || type == FormattingType.AssignStatement) {

                if (localOrAssignRange != null) {
                    if (startLine >= localOrAssignRange.first && endLine <= localOrAssignRange.second) {
                        //ignore
                    } else {
                        localOrAssignRange = null
                    }
                } else {
                    // 认为此时为local or assign语句的首行
                    val equipOperatorIndex =
                        childElement.children.indexOfFirst { it -> it.type == FormattingType.Operator && it.psi.text == "=" }
                    // 是否要对齐等号
                    var alignment = false
                    var equipOperatorIndent = 0

                    if (equipOperatorIndex > 0 && equipOperatorIndex < childElement.children.size) {
                        val lastNotEquipElement = childElement.children[equipOperatorIndex - 1]
                        val equipOperatorElement = childElement.children[equipOperatorIndex]
                        val lastNotEquipElementLineInfo = file.getLine(lastNotEquipElement.textRange.endOffset)
                        val equipOperatorElementLineInfo = file.getLine(equipOperatorElement.textRange.startOffset)

                        if (equipOperatorElementLineInfo.second - lastNotEquipElementLineInfo.second > 1) {
                            alignment = true
                            equipOperatorIndent =
                                equipOperatorElementLineInfo.second - startLineInfo.second
                        }
                    }

                    // 如果没有发现第2个local or assign 语句,则不予对齐
                    var secondLocalOrAssignmentFounded = false

                    for (localOrAssignIndex in (index + 1)..element.children.lastIndex) {
                        val localOrAssignElement = element.children[localOrAssignIndex]
                        val localAssignType = localOrAssignElement.type
                        if (localAssignType == FormattingType.LocalStatement || localAssignType == FormattingType.AssignStatement) {
                            secondLocalOrAssignmentFounded = true
                            val localOrAssignEquipOperatorIndex =
                                localOrAssignElement.children.indexOfFirst { it -> it.type == FormattingType.Operator && it.psi.text == "=" }

                            if (localOrAssignEquipOperatorIndex > 0 && localOrAssignEquipOperatorIndex < childElement.children.size) {
                                val equipOperatorElement =
                                    localOrAssignElement.children[localOrAssignEquipOperatorIndex]
                                val equipOperatorElementLineInfo =
                                    file.getLine(equipOperatorElement.textRange.startOffset)

                                equipOperatorIndent =
                                    max(
                                        equipOperatorIndent,
                                        equipOperatorElementLineInfo.second - startLineInfo.second
                                    )
                            }

                            localOrAssignRange =
                                Pair(startLine, file.getLine(localOrAssignElement.textRange.endOffset).first)
                        } else if (localAssignType == FormattingType.Comment) {
                            // ignore
                        } else {
                            break;
                        }


                        if (alignment && secondLocalOrAssignmentFounded) {
                            ctx.equipOperatorAlignmentIndent = equipOperatorIndent
                            ctx.equipOperatorAlignment = true
                        }
                    }
                }
            } else if (type == FormattingType.Comment) {
                // 试图对 localStatement, assignStatement, CallExprStatement 的行内注释做对齐


            } else {
                localOrAssignRange = null
                ctx.equipOperatorAlignment = false
                ctx.equipOperatorAlignmentIndent = 0
            }

            printElement(childElement)

            lastLine = endLine
            lastElement = childElement
        }
        ctx.exitBlockEnv()
    }


    private fun printBinaryExpr(element: FormattingElement) {
        var currentLine = file.getLine(element.textRange.startOffset).first
        var lastElement: FormattingElement? = null
        element.children.forEach {
            val line = file.getLine(it.textRange.startOffset).first
            if (line > currentLine) {
                currentLine = line
                if (lastElement?.type != FormattingType.Comment) {
                    //则换行
                    ctx.print(lineSeparator)
                }
            }

            when (it.type) {
                FormattingType.Comment -> {
                    printElement(it)
                }
                else -> {
                    printElement(it)
                }
            }
            lastElement = it
        }
    }

    private fun printUnaryExpr(element: FormattingElement) {
        element.children.forEach {
            printElement(it)
        }

    }

    private fun printCallExpr(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.CallArgs -> {
                    val firstLeftBracketLine = file.getLine(it.textRange.startOffset).first
                    val lastEndLine = file.getLine(it.textRange.endOffset).first

                    if (lastEndLine > firstLeftBracketLine && FormattingOptions.callExprAlignToFirstArg) {
                        val firstArgs = it.children.firstOrNull { it -> it.type != FormattingType.Operator }
                        val lastArgs = it.children.lastOrNull { it -> it.type != FormattingType.Operator }
                        if (lastArgs != null && firstArgs != null) {
                            val firstArgsLineInfo = file.getLine(firstArgs.textRange.startOffset)

                            // 第一个调用的参数开始已经换行,则换行对齐到第一个参数，或者正常缩进中最大的位置
                            if (firstArgsLineInfo.first > firstLeftBracketLine) {
                                var maxIndent = firstArgsLineInfo.second
                                maxIndent = max(maxIndent, ctx.getNextIndent())

                                printCallArgsAlignment(it, false, maxIndent)
                                return@forEach
                            }

                            // 第一个参数开始没有换行

                            val lastStartLineInfo = file.getLine(lastArgs.textRange.startOffset)
                            val lastEndLineInfo = file.getLine(lastArgs.textRange.endOffset)
                            if (lastStartLineInfo.first == firstLeftBracketLine) {
                                // 认为最后一个参数和左括号在同一行

                                if (lastEndLineInfo.first == lastStartLineInfo.first) {
                                    // 认为最后一个参数没有换行
                                    printElement(it)
                                    return@forEach
                                }

                                // 最后一个参数换了行 比如表，比如 函数
                                // 那么就对齐到该参数中缩进最小的元素
                                var minIndent = lastStartLineInfo.second
                                when (lastArgs.type) {
                                    FormattingType.Closure -> {
                                        lastArgs.children
                                            .lastOrNull { arg -> arg.type == FormattingType.FunctionBody }
                                            ?.children?.lastOrNull { child -> child.type == FormattingType.KeyWorld && child.psi.text == "end" }
                                            ?.let { endElement ->
                                                minIndent = min(
                                                    minIndent,
                                                    file.getLine(endElement.textRange.startOffset).second
                                                )
                                            }
                                    }
                                    FormattingType.TableExpr -> {
                                        lastArgs.children
                                            .lastOrNull { arg -> arg.type == FormattingType.Operator && arg.psi.text == "}" }
                                            ?.let { op ->
                                                minIndent =
                                                    min(minIndent, file.getLine(op.textRange.startOffset).second)
                                            }
                                    }
                                    else -> {
                                        lastArgs.children.forEach { child ->
                                            val ch = file.getLine(child.textRange.startOffset).second
                                            minIndent = min(ch, minIndent)
                                        }
                                    }
                                }
                                printCallArgsLastArgAlignment(it, minIndent)
                                return@forEach
                            }
                            //最后一个参数和第一个参数不在同一行则对齐到括号
                            printCallArgsAlignment(it, true)

                            return@forEach
                        }

                    }

                    printElement(it)
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printIndexExpr(element: FormattingElement) {
        var lineBreak = false
        //索引表达式的换行行为跟索引运算符有关
        for (index in element.children.indices) {
            val child = element.children[index]
            when (child.type) {
                FormattingType.Operator -> {
                    val text = child.psi.text
                    if (text == "." || text == ":" || text == "[") {
                        if (index > 0) {
                            val childLineInfo = file.getLine(child.textRange.startOffset)
                            val lastChild = element.children[index - 1]
                            val lastChildLineInfo = file.getLine(lastChild.textRange.endOffset)
                            if (childLineInfo.first > lastChildLineInfo.first) {
                                // 才重新换行
                                ctx.print(lineSeparator)
                                //换行之后如何对齐，那就跟以前一样对齐就好了
                                ctx.enterBlockEnv(
                                    if (childLineInfo.second < lastChildLineInfo.second)
                                        childLineInfo.second
                                    else
                                        lastChildLineInfo.second
                                )
                                lineBreak = true
                            }
                        }
                    }
                    printElement(child)
                }
                else -> {
                    printElement(child)
                }
            }
        }
        if (lineBreak) {
            ctx.exitBlockEnv()
        }
    }

    private fun printNameExpr(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printTableExpr(element: FormattingElement) {
        val startLine = file.getLine(element.textRange.startOffset).first
        val endLine = file.getLine(element.textRange.endOffset).first

        if (endLine > startLine) {
            // 试图分析出是 { aaa,bbb }
            // 还是{ aaa
            //      bbb
            //      }
            if (element.children.size > 1) {
                val firstElement = element.children[0]
                val secondElement = element.children[1]

                if (file.getLine(firstElement.textRange.startOffset).first < file.getLine(secondElement.textRange.startOffset).first) {
                    return printTableExprLineBreakAlignment(element)
                }
            }
        }
        printTableExprAlignment(element)
    }

    // 全换行对齐
    private fun printTableExprLineBreakAlignment(element: FormattingElement) {
        var firstTableField = true
        var lastFieldOrSepElement: FormattingElement? = null
        //执行换行对齐
        for (index in element.children.indices) {
            val child = element.children[index]
            when (child.type) {
                FormattingType.Operator -> {
                    val text = child.psi.text
                    when (text) {
                        "{" -> {
                            ctx.print(text).print(lineSeparator)
                            ctx.enterBlockEnv()
                        }
                        "}" -> {
                            if (ctx.equipOperatorAlignment) {
                                ctx.equipOperatorAlignment = false
                                ctx.equipOperatorAlignmentIndent = 0
                            }


                            val lastUseCharacter = ctx.getCurrentCharacter()
                            ctx.exitBlockEnv()

                            if (lastUseCharacter != 0) {
                                ctx.print(lineSeparator)
                            }

                            ctx.print(text)
                        }
                        else -> {
                            printElement(child)
                        }
                    }
                }
                FormattingType.TableField -> {
                    // 试图分析出table field的等号对齐
                    if (firstTableField) {
                        val eqIndex =
                            child.children.indexOfFirst { it.type == FormattingType.Operator && it.psi.text == "=" }
                        if (eqIndex != -1 && eqIndex > 0) {
                            val keyElement = child.children[eqIndex - 1]
                            val eqElement = child.children[eqIndex]

                            val keyCh = file.getLine(keyElement.textRange.endOffset).second
                            val eqCh = file.getLine(eqElement.textRange.startOffset).second
                            val childStart = file.getLine(child.textRange.startOffset).second
                            var maxIndent = eqCh - childStart
                            if (eqCh - keyCh > 1) {
                                for (fieldIndex in (index + 1)..element.children.lastIndex) {
                                    val fieldElement = element.children[fieldIndex]
                                    val fieldEqElement =
                                        fieldElement.children.firstOrNull { it.type == FormattingType.Operator && it.psi.text == "=" }
                                    if (fieldEqElement != null) {
                                        val fieldEqStart = file.getLine(fieldEqElement.textRange.startOffset).second
                                        val start = file.getLine(fieldElement.textRange.startOffset).second
                                        maxIndent = max(maxIndent, fieldEqStart - start)
                                    }
                                }
                                ctx.equipOperatorAlignment = true
                                ctx.equipOperatorAlignmentIndent = maxIndent
                            }
                        }
                        firstTableField = false
                    }
                    printElement(child)
                    lastFieldOrSepElement = child
                }
                /**
                 * 在表的表达式上有多种注释风格 可以是field = 123, --注解内容
                 * 也可以是
                 *  --注解内容
                 *  field = 123,
                 *  怎么这么多奇怪的习惯
                 */
                FormattingType.Comment -> {
                    if (lastFieldOrSepElement != null) {
                        // 还有人会用多行注释？那我只好换行了
                        val commentLine = file.getLine(child.textRange.endOffset).first
                        val lastFieldEndLine = file.getLine(lastFieldOrSepElement.textRange.endOffset).first

                        if (lastFieldEndLine != commentLine) {
                            printElement(child)
                        } else {
                            ctx.print(emptyWhite).print(child.psi.text).print(lineSeparator)
                        }
                    } else {
                        printElement(child)
                    }
                }
                FormattingType.TableFieldSep -> {
                    ctx.print(child.psi.text)
                    var isAddLineSeparator = true
                    // 考察一下 下一个child是不是comment
                    if (index + 1 < element.children.size) {
                        val nextChild = element.children[index + 1]
                        if (nextChild.type == FormattingType.Comment) {
                            // 考察一下该注释是否和自己在同一行
                            val sepLine = file.getLine(child.textRange.endOffset).first
                            val commentLine = file.getLine(nextChild.textRange.endOffset).first
                            if (sepLine == commentLine) {
                                isAddLineSeparator = false
                            }
                        }
                    }
                    if (isAddLineSeparator) {
                        ctx.print(lineSeparator)
                    }
                    lastFieldOrSepElement = child
                }
                else -> {
                    printElement(child)
                }
            }
        }

    }

    // 普通对齐
    private fun printTableExprAlignment(element: FormattingElement) {
        var lastFieldOrSepElement: FormattingElement? = null
        for (index in element.children.indices) {
            val child = element.children[index]
            when (child.type) {
                FormattingType.Operator -> {
                    val text = child.psi.text
                    when (text) {
                        "{" -> {
                            ctx.print(text)
                            if (index + 1 < element.children.size) {
                                val nextChild = element.children[index + 1]
                                val nextChildLineInfo = file.getLine(nextChild.textRange.startOffset)
                                if (nextChildLineInfo.first > file.getLine(child.textRange.startOffset).first) {
                                    ctx.print(lineSeparator)
                                } else if (nextChild.type != FormattingType.Operator) {
                                    ctx.print(emptyWhite)
                                }
                                ctx.enterBlockEnv(ctx.currentLineWidth)
                            }

                        }
                        "}" -> {
                            val lastUseCharacter = ctx.getCurrentCharacter()
                            if (lastUseCharacter != 0) {
                                if (lastFieldOrSepElement != null) {
                                    val lastLine = file.getLine(lastFieldOrSepElement.textRange.endOffset).first
                                    val line = file.getLine(child.textRange.startOffset).first
                                    if (line > lastLine) {
                                        ctx.print(lineSeparator)
                                    } else {
                                        ctx.print(emptyWhite)
                                    }
                                }

                            }
                            ctx.print(text)
                            ctx.exitBlockEnv()
                        }
                        else -> {
                            printElement(child)
                        }
                    }
                }
                FormattingType.TableField -> {
                    printElement(child)
                    lastFieldOrSepElement = child
                }
                /**
                 * 在表的表达式上有多种注释风格 可以是field = 123, --注解内容
                 * 也可以是
                 *  --注解内容
                 *  field = 123,
                 *  怎么这么多奇怪的习惯
                 */
                FormattingType.Comment -> {
                    if (lastFieldOrSepElement != null) {
                        // 还有人会用多行注释？那我只好换行了
                        val commentLine = file.getLine(child.textRange.endOffset).first
                        val lastFieldEndLine = file.getLine(lastFieldOrSepElement.textRange.endOffset).first

                        if (lastFieldEndLine != commentLine) {
                            printElement(child)
                        } else {
                            ctx.print(emptyWhite).print(child.psi.text).print(lineSeparator)
                        }
                    } else {
                        printElement(child)
                    }
                }
                FormattingType.TableFieldSep -> {
                    ctx.print(child.psi.text)
                    var isAddLineSeparator = false
                    var nextTableField = false
                    // 考察一下 下一个child是不是comment
                    if (index + 1 < element.children.size) {
                        val nextChild = element.children[index + 1]
                        val sepLine = file.getLine(child.textRange.endOffset).first
                        val nextLine = file.getLine(nextChild.textRange.endOffset).first

                        if (nextChild.type == FormattingType.Comment) {
                            // 考察一下该注释是否和自己在同一行
                            if (sepLine != nextLine) {
                                isAddLineSeparator = true
                            }
                        } else if (nextChild.type == FormattingType.TableField) {
                            if (ctx.getCurrentCharacter() > FormattingOptions.tableLineWidth) {
                                isAddLineSeparator = true
                            } else if (sepLine != nextLine) {
                                isAddLineSeparator = true
                            }
                        }
                    }

                    if (isAddLineSeparator) {
                        ctx.print(lineSeparator)
                    } else {
                        ctx.print(emptyWhite)
                    }

                    lastFieldOrSepElement = child
                }
                else -> {
                    printElement(child)
                }
            }
        }
    }

    private fun printExpr(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printParentExpr(element: FormattingElement) {
        element.children.forEach {
            printElement(it)
        }
    }

    private fun printLiteralExpr(element: FormattingElement) {
        ctx.print(element.psi.text, false)
    }

    private fun printExprList(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    ctx.print(it.psi.text).print(emptyWhite)
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printArg(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printCallArgs(element: FormattingElement) {
        var firstArg = true
        // 上一个参数
        var lastArgOrBracket: FormattingElement? = null;
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    val text = it.psi.text
                    when (text) {
                        "," -> {
                            ctx.print(it.psi.text).print(emptyWhite)
                        }
                        "(" -> {
                            ctx.print(text)
                            ctx.enterBlockEnv()
                            lastArgOrBracket = it
                        }
                        ")" -> {
                            lastArgOrBracket?.let { arg ->
                                if (file.getLine(arg.textRange.endOffset).first != file.getLine(it.textRange.startOffset).first) {
                                    ctx.print(lineSeparator)
                                }
                            }
                            ctx.exitBlockEnv()
                            ctx.print(text)
                        }
                        else -> {
                            printElement(it)
                        }
                    }
                }
                else -> {
                    if (firstArg) {
                        if (FormattingOptions.blankBeforeFirstArg) {
                            ctx.print(FormattingOptions.emptyWhite)
                        }
                        firstArg = false
                    }

                    lastArgOrBracket?.let { arg ->
                        if (file.getLine(arg.textRange.endOffset).first != file.getLine(it.textRange.startOffset).first) {
                            ctx.print(lineSeparator)
                        }
                    }


                    printElement(it)
                    lastArgOrBracket = it
                }
            }
        }
    }

    /**
     * @param alignmentIndent 如果 alignmentToBracket 为false，并且该值不为-1，则对齐到指定缩进
     */
    private fun printCallArgsAlignment(
        element: FormattingElement,
        alignmentToBracket: Boolean,
        alignmentIndent: Int = -1
    ) {
        loop@ for (index in element.children.indices) {
            val child = element.children[index]
            when (child.type) {
                FormattingType.Operator -> {
                    val text = child.psi.text
                    when (text) {
                        "," -> {
                            ctx.print(text)
                            // 会试探逗号的下一个元素是不是和逗号在同一行如果是就不换行
                            if (index < element.children.lastIndex) {
                                val next = element.children[index + 1]
                                val commonLine = file.getLine(child.textRange.startOffset).first
                                val line = file.getLine(next.textRange.startOffset).first
                                if (line > commonLine) {
                                    ctx.print(lineSeparator)
                                    continue@loop
                                }
                            }
                            ctx.print(emptyWhite)

                        }
                        "(" -> {
                            ctx.print(text)
                            if (alignmentToBracket) {
                                ctx.enterBlockEnv(ctx.currentLineWidth, true)
                            } else {
                                ctx.print(lineSeparator)
                                ctx.enterBlockEnv(alignmentIndent, true)
                            }
                        }
                        ")" -> {
                            // 有些人喜欢右括号放参数后面，有些人喜欢括号换行后对齐
                            val lastCallArg = element.children.lastOrNull { it.type != FormattingType.Operator }
                            if (lastCallArg != null) {
                                val lastCallArgLine = file.getLine(lastCallArg.textRange.endOffset).first
                                val bracketLine = file.getLine(child.textRange.endOffset).first
                                if (lastCallArgLine == bracketLine) {
                                    ctx.print(text)
                                    ctx.exitBlockEnv()
                                    continue@loop
                                } else {
                                    ctx.print(lineSeparator)
                                }
                            }


                            /** 这里考虑到一个情况
                             * 有些人喜欢
                             * print(
                             *   aaa,
                             *   bbbb,
                             * )
                             * 有的人愿意
                             * print(
                             *   aaa,
                             *   bbbb,
                             *   )
                             * 实际上更常见得是
                             * local ffff = tttt({
                             *
                             *
                             * }
                             * )
                             */
                            val bracketCharacter = file.getLine(child.textRange.endOffset).second
                            if (alignmentToBracket && bracketCharacter >= ctx.getCurrentIndent()) {
                                ctx.print(text)
                                ctx.exitBlockEnv()
                            } else {
                                ctx.exitBlockEnv()
                                ctx.print(text)
                            }
                        }
                        else -> {
                            printElement(child)
                        }
                    }
                }
                else -> {

                    printElement(child)
                }
            }

        }
    }

    private fun printCallArgsLastArgAlignment(
        element: FormattingElement,
        alignmentIndent: Int = -1
    ) {
        for (index in element.children.indices) {
            val childElement = element.children[index]
            if (index > 0 && index == element.children.lastIndex - 1) {
                ctx.enterBlockEnv(alignmentIndent)
            }
            when (childElement.type) {
                FormattingType.Operator -> {
                    val text = childElement.psi.text
                    when (text) {
                        "," -> {
                            ctx.print(text).print(emptyWhite)
                        }
                        "(" -> {
                            ctx.print(text)
                            if (FormattingOptions.blankBeforeFirstArg) {
                                ctx.print(emptyWhite)
                            }
                        }
                        ")" -> {
                            ctx.exitBlockEnv()
                            ctx.print(text)
                        }
                        else -> {
                            printElement(childElement)
                        }
                    }
                }
                else -> {
                    printElement(childElement)
                }
            }
        }
    }

    private fun printAttribute(element: FormattingElement) {
        element.children.forEach {
            printElement(it)
        }
    }

    private fun printNameDefList(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    ctx.print(it.psi.text).print(emptyWhite)
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printNameDef(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printBinaryOperator(element: FormattingElement) {

        ctx.print(emptyWhite)
        element.children.forEach {
            printElement(it)
        }
        ctx.print(emptyWhite)
    }

    private fun printUnaryOperator(element: FormattingElement) {
        element.children.forEach {
            val text = it.psi.text
            when (text) {
                "not" -> {
                    ctx.print(text).print(emptyWhite)
                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printOperator(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printId(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printTableField(element: FormattingElement) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    val text = it.psi.text
                    when (text) {
                        "=" -> {
                            if (ctx.equipOperatorAlignment) {
                                ctx.enterBlockEnv(ctx.getCurrentIndent() + ctx.equipOperatorAlignmentIndent)
                                ctx.print(text).print(emptyWhite)
                                ctx.exitBlockEnv()
                            } else {
                                ctx.print(emptyWhite).print(text).print(emptyWhite)
                            }
                        }
                        else -> {
                            printElement(it)
                        }
                    }

                }
                else -> {
                    printElement(it)
                }
            }
        }
    }

    private fun printTableFieldSep(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printSingleArg(element: FormattingElement) {
        ctx.print(emptyWhite)
        element.children.forEach {
            printElement(it)
        }
    }

    private fun printErrorElement(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

    private fun printKeyWorld(element: FormattingElement) {
        ctx.print(element.psi.text)
    }

}