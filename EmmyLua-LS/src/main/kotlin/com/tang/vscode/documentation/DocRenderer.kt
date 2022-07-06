/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.vscode.documentation

import com.intellij.psi.PsiElement
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.ty.*

inline fun StringBuilder.wrap(prefix: String, postfix: String, crossinline body: () -> Unit) {
    this.append(prefix)
    body()
    this.append(postfix)
}

fun StringBuilder.appendLine(context: String) {
    this.append("\n\n\n$context")
}

inline fun StringBuilder.wrapTag(tag: String, crossinline body: () -> Unit) {
    body()
    //wrap("<$tag>", "</$tag>", body)
}

inline fun StringBuilder.wrapLanguage(language: String, crossinline body: () -> Unit) {
    wrap("```${language}\n", "\n```\n\n", body)
}

internal fun StringBuilder.appendClassLink(clazz: String) {
    append(clazz)
}

internal fun StringBuilder.withIndent(indent: Int){

}

internal fun renderTy(sb: StringBuilder, ty: ITy) {
    when (ty) {
        is ITyClass -> {
            sb.appendClassLink(ty.displayName)
        }
        is ITyFunction -> {
            sb.append("fun")
            renderSignature(sb, ty.mainSignature, false)
        }
        is ITyArray -> {
            renderTy(sb, ty.base)
            sb.append("[]")
        }
        is TyUnknown -> {
            sb.append("any")
        }
        is TyUnion -> {
            // 可是这也太难看了
            // 一个普通的定义 fff = {}
            // hover 出来是 fff: table|[global fff]|[global fff]
            // var idx = 0
            // TyUnion.eachPerfect(ty) {
            //    if (idx++ != 0) sb.append("|")
            //       renderTy(sb, it)
            //       true
            //     }

            // Union 的hover并不是大而全就好,得清晰
            sb.append(ty.displayName)
        }
        is TyPrimitive -> {
            sb.appendClassLink(ty.displayName)
        }
        else -> {
            sb.append(ty.displayName)
        }
    }
}

internal fun renderSignature(sb: StringBuilder, sig: IFunSignature, inComplete: Boolean) {
    if (inComplete && sig.params.size > 1) {
        sb.wrap("(\n", "\n)\n-> ") {
            var idx = 0
            sig.params.forEach {
                if (idx++ != 0) sb.append(",\n")
                sb.append("\t${it.name}${if (it.nullable) "?" else ""}: ")
                renderTy(sb, it.ty)
            }
            if (sig.hasVarargs()) {
                sig.varargTy?.let {
                    if (idx++ != 0) sb.append(",\n")
                    sb.append("\t...: ")
                    renderTy(sb, it)
                }
            }
        }
        renderTy(sb, sig.returnTy)
    } else {
        sb.wrap("(", ") -> ") {
            var idx = 0
            sig.params.forEach {
                if (idx++ != 0) sb.append(", ")
                sb.append("${it.name}${if (it.nullable) "?" else ""}: ")
                renderTy(sb, it.ty)
            }
            if (sig.hasVarargs()) {
                sig.varargTy?.let {
                    if (idx++ != 0) sb.append(", ")
                    sb.append("...: ")
                    renderTy(sb, it)
                }
            }
        }
        renderTy(sb, sig.returnTy)
    }
}

internal fun renderComment(sb: StringBuilder, comment: LuaComment?) {
    if (comment != null) {
        sb.append("\n\n")
        var child: PsiElement? = comment.firstChild
        var seenString = false
        val paramList = mutableListOf<LuaDocTagParam>()
        val returnList = mutableListOf<LuaDocTagReturn>()
        val fieldList = mutableListOf<LuaDocTagField>()
        while (child != null) {
            val elementType = child.node.elementType
            if (elementType == LuaDocTypes.STRING) {
                seenString = true
                sb.append(child.text)
            } else if (elementType == LuaDocTypes.DASHES) {
                if (seenString) {
                    sb.append("\n")
                }
            } else if (child is LuaDocPsiElement) {
                seenString = false
                when (child) {
                    is LuaDocTagParam -> {
                        if(child.commentString != null && child.commentString!!.text.isNotEmpty()) {
                            paramList.add(child)
                        }
                    }
                    is LuaDocTagReturn -> {
                        if(child.commentString != null && child.commentString!!.text.isNotEmpty()) {
                            returnList.add(child)
                        }
                    }
                    is LuaDocTagClass -> renderClassDef(sb, child)
                    is LuaDocTagField -> fieldList.add(child)
                    is LuaDocTagOverload -> renderOverload(sb, child)
                    is LuaDocTagType -> renderTypeDef(sb, child)
                    is LuaDocTagSee -> renderSee(sb, child)
                }
            }
            child = child.nextSibling
        }

        if(paramList.isNotEmpty()){
            sb.appendLine()
            sb.wrapLanguage("plaintext"){
                val paramsDescription = "Params: "
                val doc = FormatDoc(sb, paramsDescription.length)
                doc.write(paramsDescription)
                for(param in paramList){
                    val paramNameRef = param.paramNameRef
                    val commentString = param.commentString
                    if (paramNameRef != null && commentString != null && commentString.text.isNotEmpty()) {
                        doc.writeLine("${paramNameRef.text} - ${commentString.text}")
                    }
                }
            }
        }

        if(fieldList.isNotEmpty()){
            sb.appendLine()
            sb.wrapLanguage("plaintext") {
                val fieldsDescription = "Fields: "
                val doc = FormatDoc(sb, fieldsDescription.length)
                doc.write(fieldsDescription)
                for (field in fieldList) {
                    val fieldNameRef = field.fieldName
                    val commentString = field.commentString
                    if(fieldNameRef != null) {
                        if (commentString != null && commentString.text.isNotEmpty()) {
                            doc.writeLine("${fieldNameRef} - ${commentString.text}")
                        } else {
                            doc.writeLine(fieldNameRef)
                        }
                    }
                }
            }
        }

        if(returnList.isNotEmpty()){
            sb.appendLine()
            sb.wrapLanguage("plaintext") {
                val returnsDescription = "Return: "
                val doc = FormatDoc(sb, returnsDescription.length)
                doc.write(returnsDescription)
                for (rt in returnList) {
                    val commentString = rt.commentString
                    if (commentString != null) {
                        doc.writeLine(commentString.text)
                    }
                }
            }
        }

    }
}

internal fun renderClassDef(sb: StringBuilder, def: LuaDocTagClass) {
    val cls = def.type
    sb.append("class ")
    sb.wrapTag("b") { sb.appendClassLink(cls.displayName) }
    val superClassName = cls.superClassName
    if (superClassName != null) {
        sb.append(" : ")
        sb.appendClassLink(superClassName)
    }
    renderCommentString("  ", null, sb, def.commentString)
    sb.append("\n")
}

internal fun renderFieldDef(sb: StringBuilder, def: LuaDocTagField) {
    if (def.isNullable) {
        sb.appendLine("@_field_ `${def.name}?`: ")
    } else {
        sb.appendLine("@_field_ `${def.name}`: ")
    }
    renderTypeUnion(null, null, sb, def.ty)
    renderCommentString("  ", null, sb, def.commentString)
}

internal fun renderDocParam(sb: StringBuilder, child: LuaDocTagParam) {
    val paramNameRef = child.paramNameRef
    val commentString = child.commentString
    if (paramNameRef != null && commentString != null) {
        sb.appendLine("(parameter) `${paramNameRef.text}${if (child.isNullable) "?" else ""}`: ")
        renderTypeUnion(null, null, sb, child.ty)
        if (commentString.text.isNotEmpty()) {
            renderCommentString("  ", null, sb, commentString)
        }
    }
}

internal fun renderCommentString(prefix: String?, postfix: String?, sb: StringBuilder, child: LuaDocCommentString?) {
    child?.string?.text?.let {
        sb.append("\n\n")
        if (prefix != null) sb.append(prefix)
        sb.append(it)
        if (postfix != null) sb.append(postfix)
        sb.append("\n\n")
    }
}

internal fun renderTypeUnion(prefix: String?, postfix: String?, sb: StringBuilder, type: LuaDocTy?) {
    if (type != null) {
        if (prefix != null) sb.append(prefix)

        val ty = type.getType()
        renderTy(sb, ty)

        if (postfix != null) sb.append(postfix)
    }
}

internal fun renderOverload(sb: StringBuilder, overloadDef: LuaDocTagOverload) {
    overloadDef.functionTy?.getType()?.let {
        sb.appendLine("@_overload_ ")
        renderTy(sb, it)
    }
}

internal fun renderTypeDef(sb: StringBuilder, typeDef: LuaDocTagType) {
    renderTy(sb, typeDef.type)
}

internal fun renderSee(sb: StringBuilder, see: LuaDocTagSee) {
    sb.appendLine("@_see_ ")
    see.classNameRef?.resolveType()?.let {
        renderTy(sb, it)
        see.id?.let {
            sb.append("#${it.text}")
        }
    }
}

