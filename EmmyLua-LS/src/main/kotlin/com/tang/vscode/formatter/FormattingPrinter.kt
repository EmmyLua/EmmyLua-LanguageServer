package com.tang.vscode.formatter

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.tang.lsp.ILuaFile
import java.lang.StringBuilder

class FormattingPrinter(val file: ILuaFile, val psi: PsiFile) {
    private var fileElement: FormattingElement = FormattingElement("", FormattingType.Block, psi.textRange, mutableListOf());
    private val lineSeparator = System.lineSeparator()
    private val emptyWhite = " "

    fun add(element: PsiElement, elementType: FormattingType = FormattingType.General) {
        val formattingElement = FormattingElement(element.text, elementType, element.textRange, mutableListOf())
        add(formattingElement)
    }

    fun add(formattingElement: FormattingElement) {
        add(fileElement.children, formattingElement)
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
                        && element.textRange.startOffset >= list[index].textRange.startOffset) {

                    add(list[index].children, element)
                    return
                }
            }
            list.add(0, element)
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        printBlock(sb, fileElement, 0)
        return sb.toString()
    }

    private fun printElement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val errorElement = element.children.firstOrNull { it -> it.type == FormattingType.Error }
        if (errorElement != null) {
            printErrorElement(sb, element, level)
            return
        }

        when (element.type) {
            FormattingType.Comment -> {
                printComment(sb, element, level)
            }
            FormattingType.Function -> {
                printFunction(sb, element, level)
            }
            FormattingType.LocalFunction -> {
                printLocalFunction(sb, element, level)
            }
            FormattingType.Closure -> {
                printClosure(sb, element, level)
            }
            FormattingType.DoBlock -> {
                printDoBlock(sb, element, level)
            }
            FormattingType.IfStatement -> {
                printIfStatement(sb, element, level)
            }
            FormattingType.Statement -> {
                printStatement(sb, element, level)
            }
            FormattingType.RepeatStatement -> {
                printRepeatStatement(sb, element, level)
            }
            FormattingType.WhileStatement -> {
                printWhileStatement(sb, element, level)
            }
            FormattingType.ForAStatement -> {
                printForAStatement(sb, element, level)
            }
            FormattingType.ForBStatement -> {
                printForBStatement(sb, element, level)
            }
            FormattingType.LocalStatement -> {
                printLocalStatement(sb, element, level)
            }
            FormattingType.BreakStatement -> {
                printBreakStatement(sb, element, level)
            }
            FormattingType.ExprStatement -> {
                printExprStatement(sb, element, level)
            }
            FormattingType.CallExpr -> {
                printCallExpr(sb, element, level)
            }
            FormattingType.LiteralExpr -> {
                printLiteralExpr(sb, element, level)
            }
            FormattingType.BinaryExpr -> {
                printBinaryExpr(sb, element, level)
            }
            FormattingType.UnaryExpr -> {
                printUnaryExpr(sb, element, level)
            }
            FormattingType.NamedExpr -> {
                printNameExpr(sb, element, level)
            }
            FormattingType.IndexExpr -> {
                printIndexExpr(sb, element, level)
            }
            FormattingType.AssignStatement -> {
                printAssignStatement(sb, element, level)
            }
            FormattingType.ReturnStatement -> {
                printReturnStatement(sb, element, level)
            }
            FormattingType.TableExpr -> {
                printTableExpr(sb, element, level)
            }
            FormattingType.TableField -> {
                printTableField(sb, element, level)
            }
            FormattingType.TableFieldSep -> {
                printTableFieldSep(sb, element, level)
            }
            FormattingType.CallArgs -> {
                printCallArgs(sb, element, level)
            }
            FormattingType.Expr -> {
                printExpr(sb, element, level)
            }
            FormattingType.ParentExpr -> {
                printParentExpr(sb, element, level)
            }
            FormattingType.ExprList -> {
                printExprList(sb, element, level)
            }
            FormattingType.Operator -> {
                printOperator(sb, element, level)
            }
            FormattingType.FunctionBody -> {
                printFunctionBody(sb, element, level)
            }
            FormattingType.Block -> {
                printBlock(sb, element, level)
            }
            FormattingType.Arg -> {
                printArg(sb, element, level)
            }
            FormattingType.NameDefList -> {
                printNameDefList(sb, element, level)
            }
            FormattingType.NameDef -> {
                printNameDef(sb, element, level)
            }
            FormattingType.BinaryOperator -> {
                printBinaryOperator(sb, element, level)
            }
            FormattingType.UnaryOperator -> {
                printUnaryOperator(sb, element, level)
            }
            FormattingType.Id -> {
                printId(sb, element, level)
            }
            FormattingType.SingleArg -> {
                printSingleArg(sb, element, level)
            }
            else -> {
                sb.append(element.text)
            }
        }
    }

    private fun printComment(sb: StringBuilder, element: FormattingElement, level: Int) {
        printWithIndent(sb, element.text, level)
        sb.append(lineSeparator)
    }

    private fun printFunction(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "function" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                        "end" -> {
                            sb.append(indent).append(it.text)
                        }
                    }
                }
                FormattingType.Id -> {
                    sb.append(it.text)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printLocalFunction(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "local" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                        "function" -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        "end" -> {
                            sb.append(indent).append(it.text).append(lineSeparator)
                        }
                    }
                }
                FormattingType.Id -> {
                    sb.append(it.text)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printClosure(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "function" -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        "end" -> {
                            sb.append(indent).append(it.text)
                        }
                    }
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printDoBlock(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    sb.append(indent).append(it.text).append(lineSeparator)
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printIfStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val ifStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    if (it.text == "if") {
                        sb.append(indent).append(it.text).append(emptyWhite)
                    } else if (it.text == "else" || it.text == "end") {
                        sb.append(indent).append(it.text).append(lineSeparator)
                    } else if (it.text == "elseif") {
                        sb.append(indent).append(it.text).append(emptyWhite)
                    } else if (it.text == "then") {
                        sb.append(emptyWhite).append(it.text).append(lineSeparator)
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - ifStartLine > 0) {
                        printElement(sb, it, level + 1)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printWhileStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val whileStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "while" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                        "do" -> {
                            sb.append(emptyWhite).append(it.text).append(lineSeparator)
                        }
                        "end" -> {
                            sb.append(indent).append(it.text)
                        }
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - whileStartLine > 0) {
                        printElement(sb, it, level + 1)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printRepeatStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val repeatStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "repeat" -> {
                            sb.append(indent).append(it.text).append(lineSeparator)
                        }
                        "until" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - repeatStartLine > 0) {
                        printElement(sb, it, level + 1)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printForAStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val forStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "for" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                        "do" -> {
                            sb.append(emptyWhite).append(it.text).append(lineSeparator)
                        }
                        "end" -> {
                            sb.append(indent).append(it.text)
                        }
                    }
                }
                FormattingType.Operator -> {
                    when (it.text) {
                        "," -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        "=" -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        else -> {
                            printElement(sb, it, level)
                        }
                    }
                }
                FormattingType.Arg -> {
                    sb.append(it.text).append(emptyWhite)
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - forStartLine > 0) {
                        printElement(sb, it, level + 1)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printForBStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val forStartLine = file.getLine(element.textRange.startOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "for" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                        "in" -> {
                            sb.append(emptyWhite).append(it.text).append(emptyWhite)
                        }
                        "do" -> {
                            sb.append(emptyWhite).append(it.text).append(lineSeparator)
                        }
                        "end" -> {
                            sb.append(indent).append(it.text)
                        }
                    }
                }
                FormattingType.Operator -> {
                    when (it.text) {
                        "," -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        else -> {
                            printElement(sb, it, level)
                        }
                    }
                }
                FormattingType.Comment -> {
                    val startLine = file.getLine(it.textRange.startOffset).first
                    if (startLine - forStartLine > 0) {
                        printElement(sb, it, level + 1)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        sb.append(indent).append(element.text).append(lineSeparator)
    }

    private fun printLocalStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val endLine = file.getLine(element.textRange.endOffset).first
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "local" -> {
                            sb.append(indent).append(it.text).append(emptyWhite)
                        }
                    }
                }
                FormattingType.Operator -> {
                    when (it.text) {
                        "=" -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                    }
                }
                FormattingType.NameDefList -> {
                    printElement(sb, it, level)
                    sb.append(emptyWhite)
                }
                FormattingType.Comment -> {
                    val commentLine = file.getLine(it.textRange.endOffset).first
                    if (commentLine >= endLine) {
                        sb.append(emptyWhite).append(it.text)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printBreakStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        sb.append(indent).append(element.text).append(lineSeparator)
    }

    private fun printAssignStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        val endLine = file.getLine(element.textRange.endOffset).first
        var leftExpr = true
        sb.append(indent)
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    when (it.text) {
                        "=" -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                    }
                }
                FormattingType.ExprList -> {
                    printElement(sb, it, level)
                    if (leftExpr) {
                        sb.append(emptyWhite)
                        leftExpr = false
                    }
                }
                FormattingType.Comment -> {
                    val commentLine = file.getLine(it.textRange.endOffset).first
                    if (commentLine >= endLine) {
                        sb.append(emptyWhite).append(it.text)
                    } else {
                        printElement(sb, it, level)
                    }
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
        sb.append(lineSeparator)
    }

    private fun printExprStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        sb.append(indent)
        element.children.forEach {
            printElement(sb, it, level)
        }
        sb.append(lineSeparator)
    }

    private fun printReturnStatement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        sb.append(indent)
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    sb.append(it.text).append(emptyWhite)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }

        }
        sb.append(lineSeparator)
    }

    private fun printFunctionBody(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        element.children.forEach {
            when (it.type) {
                FormattingType.KeyWorld -> {
                    when (it.text) {
                        "end" -> {
                            sb.append(indent).append(it.text)
                        }
                    }
                }
                FormattingType.Operator -> {
                    when (it.text) {
                        "," -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        "(" -> {
                            sb.append(it.text)
                        }
                        ")" -> {
                            sb.append(it.text).append(lineSeparator)
                        }
                    }
                }
                FormattingType.Comment -> {
                    printElement(sb, it, level + 1)
                }
                FormattingType.Block -> {
                    printElement(sb, it, level + 1)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printBlock(sb: StringBuilder, element: FormattingElement, level: Int) {
        var lastLine = -1
        var lastElement: FormattingElement? = null
        for (index in element.children.indices) {
            val childElement = element.children[index]
            val startLine = file.getLine(childElement.textRange.startOffset).first
            val endLine = file.getLine(childElement.textRange.endOffset).first

            // 用于检查原始布局
            val lineDiff = startLine - lastLine

            if (index != 0) {
                val type = childElement.type
                // 函数和前文定义之间插入空行
                if (type == FormattingType.Function || type == FormattingType.LocalFunction) {
                    if (lastElement?.type == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            sb.append(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        sb.append(lineSeparator.repeat(FormattingOptions.functionSpacing))
                    }
                }
                // 在语句块和前文之间至少一个空格
                else if (type == FormattingType.ForAStatement || type == FormattingType.ForBStatement || type == FormattingType.RepeatStatement
                        || type == FormattingType.WhileStatement) {
                    if (lastElement?.type == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            sb.append(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        sb.append(lineSeparator)
                    }
                }
                // 赋值语句会根据情况空行
                else if (type == FormattingType.LocalStatement || type == FormattingType.AssignStatement || type == FormattingType.ExprStatement || type == FormattingType.Comment) {
                    val lastType = lastElement?.type
                    if (lastType == FormattingType.LocalStatement || lastType == FormattingType.AssignStatement || lastType == FormattingType.ExprStatement || lastType == FormattingType.Comment) {
                        if (lineDiff > 1) {
                            // 保持原始布局
                            sb.append(lineSeparator.repeat(lineDiff - 1))
                        }
                    } else {
                        sb.append(lineSeparator)
                    }
                }
            }
            printElement(sb, childElement, level)
            lastLine = endLine
            lastElement = childElement
        }
    }

    private fun printBinaryExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            printElement(sb, it, level)
        }
    }

    private fun printUnaryExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            printElement(sb, it, level)
        }
    }

    private fun printCallExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            printElement(sb, it, level)
        }
    }

    private fun printIndexExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            printElement(sb, it, level)
        }
    }

    private fun printNameExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printTableExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        val startLine = file.getLine(element.textRange.startOffset).first
        val endLine = file.getLine(element.textRange.endOffset).first
        if (endLine > startLine || endLine - startLine > FormattingOptions.lineWidth) {
            //执行换行对齐
            element.children.forEach {
                when (it.type) {
                    FormattingType.Operator -> {
                        when (it.text) {
                            "{" -> {
                                sb.append(it.text).append(lineSeparator)
                            }
                            "}" -> {
                                sb.append(lineSeparator)
                                printWithIndent(sb, it.text, level)
                            }
                            else -> {
                                printElement(sb, it, level)
                            }
                        }
                    }
                    FormattingType.TableField -> {
                        // 手动打印一个缩进
                        sb.append(FormattingOptions.getIndentString(level + 1))
                        // TableField认为他的缩进已经被TableExpr打印了
                        printElement(sb, it, level + 1)
                    }
                    FormattingType.Comment -> {
                        printElement(sb, it, level + 1)
                    }
                    FormattingType.TableFieldSep -> {
                        sb.append(it.text).append(lineSeparator)
                    }
                    else -> {
                        printElement(sb, it, level)
                    }
                }
            }
        } else {
            //执行换行对齐
            element.children.forEach {
                when (it.type) {
                    FormattingType.TableField -> {
                        printElement(sb, it, 0)
                    }
                    FormattingType.TableFieldSep -> {
                        sb.append(it.text).append(emptyWhite)
                    }
                    else -> {
                        printElement(sb, it, level)
                    }
                }
            }
        }
    }

    private fun printExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printParentExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            printElement(sb, it, level)
        }
    }

    private fun printLiteralExpr(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printExprList(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    when (it.text) {
                        "," -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                    }
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printArg(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printCallArgs(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    when (it.text) {
                        "," -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                        else -> {
                            printElement(sb, it, level)
                        }
                    }
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printNameDefList(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    when (it.text) {
                        "," -> {
                            sb.append(it.text).append(emptyWhite)
                        }
                    }
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printNameDef(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printBinaryOperator(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(emptyWhite)
        element.children.forEach {
            printElement(sb, it, level)
        }
        sb.append(emptyWhite)
    }

    private fun printUnaryOperator(sb: StringBuilder, element: FormattingElement, level: Int) {
        element.children.forEach {
            when (it.text) {
                "not" -> {
                    printElement(sb, it, level)
                    sb.append(emptyWhite)
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printOperator(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printId(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printTableField(sb: StringBuilder, element: FormattingElement, level: Int) {
        // 当打印TableField时，认为缩进已经被打印了
        element.children.forEach {
            when (it.type) {
                FormattingType.Operator -> {
                    when (it.text) {
                        "=" -> {
                            sb.append(emptyWhite).append(it.text).append(emptyWhite)
                        }
                        else -> {
                            printElement(sb, it, level)
                        }
                    }

                }
                FormattingType.Comment -> {
                    // 打印注释时，认为tableExpr必定是换行表达
                    sb.append(it.text).append(lineSeparator).append(FormattingOptions.getIndentString(level))
                }
                else -> {
                    printElement(sb, it, level)
                }
            }
        }
    }

    private fun printTableFieldSep(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(element.text)
    }

    private fun printSingleArg(sb: StringBuilder, element: FormattingElement, level: Int) {
        sb.append(emptyWhite)
        element.children.forEach {
            printElement(sb, it, level)
        }
    }

    private fun printErrorElement(sb: StringBuilder, element: FormattingElement, level: Int) {
        val indent = FormattingOptions.getIndentString(level)
        sb.append(indent)
        sb.append(element.text.replace(lineSeparator, "$indent$lineSeparator"))
        sb.append(lineSeparator)
    }

    private fun printWithIndent(sb: StringBuilder, text: String, level: Int) {
        sb.append(FormattingOptions.getIndentString(level)).append(text)
    }

}