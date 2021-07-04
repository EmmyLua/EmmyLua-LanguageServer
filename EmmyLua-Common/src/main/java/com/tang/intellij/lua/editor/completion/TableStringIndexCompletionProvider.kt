package com.tang.intellij.lua.editor.completion

import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.Ty

class TableStringIndexCompletionProvider : ClassMemberCompletionProvider(){
    override fun addCompletions(session: CompletionSession) {
        val completionParameters = session.parameters
        val completionResultSet = session.resultSet

        val table = PsiTreeUtil.getParentOfType(completionParameters.position, LuaIndexExpr::class.java)
        if (table != null) {
            val isColon = false
            val project = table.project
            val prefixMatcher = completionResultSet.prefixMatcher

            val contextTy = LuaPsiTreeUtil.findContextClass(table)
            val context = SearchContext.get(project)
            val prefixType = table.guessParentType(context)
            if (!Ty.isInvalid(prefixType)) {
                complete(isColon, project, contextTy, prefixType, completionResultSet, prefixMatcher, null)
            }
        }
        true
    }
}