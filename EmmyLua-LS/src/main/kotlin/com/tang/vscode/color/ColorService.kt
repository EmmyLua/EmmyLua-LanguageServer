//package com.tang.vscode.color
//
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.PsiElement
//import com.tang.intellij.lua.Constants
//import com.tang.intellij.lua.comment.psi.LuaDocClassNameRef
//import com.tang.intellij.lua.comment.psi.LuaDocTagAlias
//import com.tang.intellij.lua.comment.psi.LuaDocTagClass
//import com.tang.intellij.lua.comment.psi.LuaDocVisitor
//import com.tang.intellij.lua.comment.psi.api.LuaComment
//import com.tang.intellij.lua.psi.*
//import com.tang.intellij.lua.search.SearchContext
//import com.tang.lsp.ILuaFile
//import com.tang.lsp.nameRange
//import com.tang.lsp.toRange
//import com.tang.vscode.Annotator
//import com.tang.vscode.AnnotatorType
//import com.tang.vscode.RenderRange
//import org.eclipse.lsp4j.ColorInformation
//import org.eclipse.lsp4j.jsonrpc.CancelChecker
//
///*
// * 单例是因为设计
// */
//object ColorService {
//    fun renderColor(file: ILuaFile, checker: CancelChecker): MutableList<ColorInformation> {
//        val paramList = mutableListOf<TextRange>()
//        val globalsList = mutableListOf<TextRange>()
//        val docTypeNames = mutableListOf<TextRange>()
//        val upValues = mutableListOf<TextRange>()
//        val notUse = mutableListOf<TextRange>()
//
//        // 认为所有local名称定义一开始都是未使用的
//        val psiNotUse = mutableSetOf<PsiElement>()
//        file.psi?.acceptChildren(object : LuaRecursiveVisitor() {
//            override fun visitParamNameDef(o: LuaParamNameDef) {
//                psiNotUse.add(o)
//            }
//
//            override fun visitFuncDef(o: LuaFuncDef) {
//                val name = o.nameIdentifier
//                if (name != null && o.forwardDeclaration == null) {
//                    globalsList.add(name.textRange)
//                }
//                super.visitFuncDef(o)
//            }
//
//            override fun visitNameExpr(o: LuaNameExpr) {
//                if (o.parent is LuaExprStat) // non-complete stat
//                    return
//
//                val context = SearchContext.get(o.project)
//                val declPsi = resolveInFile(o.name, o, context)
//
//                if (psiNotUse.contains(declPsi)) {
//                    psiNotUse.remove(declPsi)
//                    // 不能和下面的合并因为不想重复渲染
//                    when (declPsi) {
//                        is LuaParamNameDef -> {
//                            paramList.add(declPsi.textRange)
//                        }
//                    }
//                }
//
//                when (declPsi) {
//                    is LuaParamNameDef -> paramList.add(o.textRange)
//                    is LuaFuncDef -> globalsList.add(o.textRange)
//                    is LuaNameDef -> {
//                    } //local
//                    is LuaLocalFuncDef -> {
//                    } //local
//                    else -> {
//                        if (o.firstChild.textMatches(Constants.WORD_SELF)) {
//                            // SELF
//                        } else
//                            globalsList.add(o.textRange)
//                    }
//                }
//
//                if (isUpValue(o, context))
//                    upValues.add(o.textRange)
//            }
//
//            override fun visitLocalFuncDef(o: LuaLocalFuncDef) {
//                psiNotUse.add(o)
//                o.acceptChildren(this)
//            }
//
//            override fun visitNameDef(o: LuaNameDef) {
//                psiNotUse.add(o)
//            }
//
//            override fun visitElement(element: PsiElement) {
//                if (element is LuaComment) {
//                    element.acceptChildren(object : LuaDocVisitor() {
//
//                        override fun visitTagClass(o: LuaDocTagClass) {
//                            val identifier = o.nameIdentifier
//                            docTypeNames.add(identifier.textRange)
//                            super.visitTagClass(o)
//                        }
//
//                        override fun visitClassNameRef(o: LuaDocClassNameRef) {
//                            docTypeNames.add(o.textRange)
//                        }
//
//                        override fun visitElement(element: PsiElement) {
//                            element.acceptChildren(this)
//                        }
//
//                        override fun visitTagAlias(o: LuaDocTagAlias) {
//                            val identifier = o.nameIdentifier
//                            if (identifier != null)
//                                docTypeNames.add(identifier.textRange)
//                            super.visitTagAlias(o)
//                        }
//                    })
//                } else
//                    super.visitElement(element)
//            }
//        })
//
//        psiNotUse.forEach {
//            when (it) {
//                is LuaLocalFuncDef -> {
//                    it.nameRange?.let { it1 -> notUse.add(it1) };
//                }
//                else -> {
//                    notUse.add(it.textRange)
//                }
//            }
//
//        }
//
//        val list = mutableListOf<ColorInformation>()
//        val uri = file.uri.toString()
//        if (paramList.isNotEmpty())
//            list.addAll(paramList.map { ColorInformation(it.toRange(file), LuaColorOptions.parameterColor) })
//        if (globalsList.isNotEmpty())
//            list.addAll(globalsList.map { ColorInformation(it.toRange(file), LuaColorOptions.globalColor)})
//        if (docTypeNames.isNotEmpty())
//            list.addAll(docTypeNames.map { ColorInformation(it.toRange(file), LuaColorOptions.docTypeColor)})
//        if (upValues.isNotEmpty())
//            list.addAll(upValues.map { ColorInformation(it.toRange(file), LuaColorOptions.upvalueColor)})
////        if (notUse.isNotEmpty()) {
////            list.addAll(docTypeNames.map { ColorInformation(it.toRange(file), LuaColorOptions.parameterColor)})
////        }
//
//        return list
//    }
//}