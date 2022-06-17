package com.tang.vscode.inlayHint

import com.intellij.psi.PsiElement
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyFunction
import com.tang.intellij.lua.ty.TyClass
import com.tang.intellij.lua.ty.findPerfectSignature
import com.tang.intellij.lua.ty.hasVarargs
import com.tang.lsp.nameRange
import com.tang.lsp.toRange
import com.tang.vscode.RenderRange
import com.tang.vscode.api.impl.LuaFile
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintKind
import org.eclipse.lsp4j.jsonrpc.messages.Either

object InlayHintService {
    fun getInlayHint(file: LuaFile): MutableList<InlayHint> {
        val paramHints = mutableListOf<RenderRange>()
        val localHints = mutableListOf<RenderRange>()
        val overrideHints = mutableListOf<RenderRange>()

        file.psi?.acceptChildren(object : LuaRecursiveVisitor() {
            override fun visitClassMethodDef(o: LuaClassMethodDef) {
                if (LuaSettings.instance.overrideHint) {
                    val context = SearchContext.get(o.project)
                    val classType = o.guessClassType(context)
                    if (classType != null) {
                        TyClass.processSuperClass(classType, context) { sup ->
                            val id = o.classMethodName.id
                            if (id != null) {
                                val member = sup.findMember(id.text, context)
                                if (member != null) {
                                    val funcBody = o.children.find { it is LuaFuncBody }
                                    if (funcBody is LuaFuncBody) {
                                        var fchild = funcBody.firstChild
                                        while (fchild != funcBody.lastChild) {
                                            if (fchild.text == ")") {
                                                overrideHints.add(
                                                    RenderRange(
                                                        fchild.textRange.toRange(file),
                                                        "override",
                                                        "${sup.className}#${member.name}"
                                                    )
                                                )

                                                return@processSuperClass false
                                            }

                                            fchild = fchild.nextSibling
                                        }
                                    }
                                }
                            }
                            true
                        }
                    }
                }
                o.acceptChildren(this)
            }

            override fun visitLocalDef(o: LuaLocalDef) {
                if (o.parent is LuaExprStat) // non-complete stat
                    return
                if (LuaSettings.instance.localHint) {
                    val nameList = o.nameList
                    o.exprList?.exprList.let { _ ->
                        nameList?.nameDefList?.forEach {
                            it.nameRange?.let { nameRange ->
                                // 这个类型联合的名字太长对大多数情况都不是必要的，将进行必要的裁剪
                                val gussType = it.guessType(SearchContext.get(o.project))
                                val displayName = gussType.displayName
                                when {
                                    displayName.startsWith("fun") -> {
                                        localHints.add(RenderRange(nameRange.toRange(file), "function"))
                                    }
                                    displayName.startsWith('[') -> {
                                        // ignore
                                    }
                                    else -> {
                                        val unexpectedNameIndex = displayName.indexOf("|[")
                                        when (unexpectedNameIndex) {
                                            -1 -> {
                                                localHints.add(RenderRange(nameRange.toRange(file), displayName))
                                            }
                                            else -> {
                                                localHints.add(
                                                    RenderRange(
                                                        nameRange.toRange(file),
                                                        displayName.substring(0, unexpectedNameIndex)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                o.acceptChildren(this)
            }

            override fun visitCallExpr(callExpr: LuaCallExpr) {
                if (LuaSettings.instance.paramHint) {
                    var activeParameter = 0
                    var nCommas = 0
                    val literalMap = mutableMapOf<Int, Int>()
                    callExpr.args.firstChild?.let { firstChild ->
                        var child: PsiElement? = firstChild
                        while (child != null) {
                            if (child.node.elementType == LuaTypes.COMMA) {
                                activeParameter++
                                nCommas++
                            } else if (child.node.elementType == LuaTypes.LITERAL_EXPR
                                || child.node.elementType == LuaTypes.TABLE_EXPR
                                || child.node.elementType == LuaTypes.CLOSURE_EXPR
                                || child.node.elementType == LuaTypes.BINARY_EXPR
                                || child.node.elementType == LuaTypes.NAME_EXPR
                            ) {
                                paramHints.add(RenderRange(child.textRange.toRange(file), null))
                                literalMap[activeParameter] = paramHints.size - 1;
                            }

                            child = child.nextSibling
                        }
                    }

                    callExpr.guessParentType(SearchContext.get(callExpr.project)).let { parentType ->
                        parentType.each { ty ->
                            if (ty is ITyFunction) {
                                val sig = ty.findPerfectSignature(callExpr)
                                var index = 0;

                                sig.params.forEach { pi ->
                                    literalMap[index]?.let {
                                        paramHints[it].hint = pi.name
                                    }
                                    index++
                                }

                                if (sig.hasVarargs() && LuaSettings.instance.varargHint) {
                                    for (paramIndex in literalMap.keys) {
                                        if (paramIndex >= index) {
                                            literalMap[paramIndex]?.let {
                                                paramHints[it].hint = "var" + (paramIndex - index).toString()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })

        val inlayHints = mutableListOf<InlayHint>()
        if (paramHints.isNotEmpty()) {
            for (paramHint in paramHints) {
                if (paramHint.hint != null) {
                    val hint = InlayHint(paramHint.range.start, Either.forLeft("${paramHint.hint}:"))
                    hint.kind = InlayHintKind.Parameter
                    hint.paddingRight = true
                    inlayHints.add(hint)
                }
            }
        }
        if (localHints.isNotEmpty()) {
            for (localHint in localHints) {
                val hint = InlayHint(localHint.range.end, Either.forLeft(":${localHint.hint}"))
                hint.kind = InlayHintKind.Type
                hint.paddingLeft = true
                inlayHints.add(hint)
            }
        }
        if (overrideHints.isNotEmpty()) {
            for (overrideHint in overrideHints) {
                val hint = InlayHint(overrideHint.range.end, Either.forLeft(overrideHint.hint))
                hint.paddingLeft = true
                if (overrideHint.data != null) {
                    hint.data = overrideHint.data
                }

                inlayHints.add(hint)
            }
        }
        return inlayHints
    }
}