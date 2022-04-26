package com.tang.vscode.diagnostics.inspections

import com.intellij.openapi.util.TextRange
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.TyPsiDocClass
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Range

object InheritInspection {
    fun inheritInspections(docClass: LuaDocTagClass, file: ILuaFile, diagnostics: MutableList<Diagnostic>) {
        val superRef = docClass.superClassNameRef
        if (superRef != null) {
            val classList = superRef.classNameRefList

            val context = SearchContext.get(docClass.project)
            val originType = docClass.type
            if (originType.isInterface && classList.isNotEmpty()) {
                val diagnostic = Diagnostic()
                diagnostic.message =
                    "Interface ‘${docClass.text}’ do not support inheritance"
                diagnostic.severity = DiagnosticSeverity.Warning
                diagnostic.range = docClass.textRange.toRange(file)
                diagnostics.add(diagnostic)
                return
            }

            for (i in 0 until classList.size) {
                val clazz = LuaShortNamesManager.getInstance(docClass.project).findClass(classList[i].text, context)

                if (clazz != null) {
                    val clazzType = clazz.type
                    if (i != 0 && !clazzType.isInterface) {
                        val diagnostic = Diagnostic()
                        diagnostic.message =
                            "Multiple inheritance cannot inherit non-interface classes '${classList[i].text}'"
                        diagnostic.severity = DiagnosticSeverity.Warning
                        diagnostic.range = classList[i].textRange.toRange(file)
                        diagnostics.add(diagnostic)
                    }

                    if (clazzType.isInterface) {
                        interfaceCheck(originType, clazzType, context, docClass.textRange.toRange(file), diagnostics)
                    }
                } else {
                    val diagnostic = Diagnostic()
                    diagnostic.message = "Inherit an undefined class or interface '${classList[i].text}'"
                    diagnostic.severity = DiagnosticSeverity.Warning
                    diagnostic.range = classList[i].textRange.toRange(file)
                    diagnostics.add(diagnostic)
                }
            }
        }
    }

    private fun interfaceCheck(
        originType: ITyClass,
        interfaceType: ITyClass,
        context: SearchContext,
        range: Range,
        diagnostics: MutableList<Diagnostic>
    ): Boolean {
        var isSubType = true
        interfaceType.processMembers(context, { _, member ->
            if (member.name == null) {
                isSubType = false
                return@processMembers
            }
            val originMember = originType.findOriginMember(member.name!!, context)
            if (originMember == null) {
                val diagnostic = Diagnostic()
                diagnostic.message =
                    "Does not implement member ${member.name}:${member.guessType(context).displayName} of interface '${interfaceType.displayName}'"
                diagnostic.severity = DiagnosticSeverity.Warning
                diagnostic.range = range
                diagnostics.add(diagnostic)
                isSubType = false
                return@processMembers
            }

            val thisMemberType = originMember.guessType(context)
            val interfaceMemberType = member.guessType(context)
            if (!thisMemberType.subTypeOf(interfaceMemberType, context, true)) {
                val diagnostic = Diagnostic()
                diagnostic.message =
                    "Member '${member.name}' is not compatible with the type '${interfaceMemberType.displayName}'" +
                            " of interface '${interfaceType.displayName}' 's member '${member.name}'"
                diagnostic.severity = DiagnosticSeverity.Warning
                diagnostic.range = range
                diagnostics.add(diagnostic)
                isSubType = false
                return@processMembers
            }

        }, false)

        return isSubType
    }

}