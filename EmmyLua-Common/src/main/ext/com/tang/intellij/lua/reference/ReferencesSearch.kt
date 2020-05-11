package com.tang.intellij.lua.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.util.AbstractQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import com.tang.lsp.ILuaFile

object ReferencesSearch {
    /**
     * Searches for references to the specified element in the scope in which such references are expected to be found, according to
     * dependencies and access rules.
     *
     * @param element the element (declaration) the references to which are requested.
     * @return the query allowing to enumerate the references.
     */
    fun search(element: PsiElement): Query<PsiReference> {
        return ReferenceQuery(element, element.useScope)
    }

    fun search(element: PsiElement, scope: SearchScope): Query<PsiReference> {
        return ReferenceQuery(element, scope)
    }

    class ReferenceQuery(private val element: PsiElement, private val scope: SearchScope) : AbstractQuery<PsiReference>() {
        override fun processResults(consumer: Processor<PsiReference>): Boolean {
            val name = if (element is PsiNamedElement) element.name else element.text
            val nameHash = name?.hashCode()
            val project = element.project
            project.process { file ->
                val vFile = file.virtualFile
                if (vFile is ILuaFile && scope.contains(file.virtualFile)) {
                    vFile.processWords { word ->
                        var continueRun = true
                        if (word.hashCode == nameHash) {
                            val elementAt = file.findElementAt(word.start)
                            if (elementAt != null) {
                                continueRun = processReferences(elementAt, consumer)
                            }
                        }
                        continueRun
                    }
                }
                true
            }
            return true
        }

        private fun processReferences(element: PsiElement, consumer: Processor<PsiReference>): Boolean {
            var cur = element
            while (cur !is PsiFile) {
                val r = cur.reference
                if (r != null && r.isReferenceTo(this.element)) {
                    return consumer.process(r)
                }
                cur = cur.parent
            }
            return true
        }
    }
}