package com.tang.intellij.lua.psi.search

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.stubs.index.LuaSuperClassIndex

class LuaClassInheritorsSearch {
    companion object {
        fun isClassInheritFrom(searchScope: GlobalSearchScope, project: Project, typeName: String, sup: String): Boolean {
            return !processInheritors(typeName, project, true, searchScope, mutableSetOf(), Processor {
                it.name != sup
            })
        }

        private fun processInheritors(typeName: String,
                                      project: Project,
                                      deep: Boolean,
                                      searchScope: GlobalSearchScope,
                                      processedNames: MutableSet<String>,
                                      processor: Processor<in LuaDocTagClass>): Boolean {
            var ret = true
            // recursion guard!!
            if (!processedNames.add(typeName))
                return ret

            val processed = mutableListOf<LuaDocTagClass>()
            LuaSuperClassIndex.process(typeName, project, searchScope, Processor {
                processed.add(it)
                ret = processor.process(it)
                ret
            })
            if (ret && deep) {
                for (def in processed) {
                    ret = processInheritors(def.name, project, deep, searchScope, processedNames, processor)
                    if (!ret) break
                }
            }
            return ret
        }

    }
}