package com.tang.intellij.lua.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.util.AbstractQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import com.tang.intellij.lua.psi.LuaPsiFile

object ReferencesSearch {
    /**
     * Searches for references to the specified element in the scope in which such references are expected to be found, according to
     * dependencies and access rules.
     *
     * @param element the element (declaration) the references to which are requested.
     * @return the query allowing to enumerate the references.
     */
    fun search(element: PsiElement): Query<PsiReference> {
        return ReferenceQuery(element, GlobalSearchScope.allScope(element.project))
    }

    fun search(element: PsiElement, scope: SearchScope): Query<PsiReference> {
        return ReferenceQuery(element, scope)
    }

    class ReferenceQuery(private val element: PsiElement, private val scope: SearchScope) : AbstractQuery<PsiReference>() {
        override fun processResults(consumer: Processor<PsiReference>): Boolean {
            val name = if (element is PsiNamedElement) element.name else element.text
            val project = element.project
            val pattern = "\\b$name\\b".toRegex()
            project.process { file ->
                if (scope.contains(file.virtualFile) && file is LuaPsiFile) {
                    val text = file.virtualFile.text
                    if (text != null) {
                        var matchResult = pattern.find(text)

                        while (matchResult != null) {
                            val elementAt = file.findElementAt(matchResult.range.first)
                            if (elementAt != null && !processReferences(elementAt, consumer)) {
                                break
                            }
                            matchResult = matchResult.next()
                        }

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