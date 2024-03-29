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

package com.tang.intellij.lua.stubs.index

import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaTableField
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.StubKeys
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.TyClass
import com.tang.intellij.lua.ty.TyParameter

class LuaClassMemberIndex : StubIndex<Int, LuaClassMember>() {
    override fun getKey() = StubKeys.CLASS_MEMBER

    companion object {
        val instance = LuaClassMemberIndex()

        fun process(key: String, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            if (context.isDumb)
                return false
            val all = LuaClassMemberIndex.instance.get(key.hashCode(), context.project, context.scope)
            return ContainerUtil.process(all, processor)
        }

//        fun processNonIndex(
//            className: String,
//            fieldName: String,
//            context: SearchContext,
//            processor: Processor<LuaClassMember>
//        ) {
//            val classDef = LuaShortNamesManager.getInstance(context.project).findClass(className, context)
//            if (classDef != null) {
//                val type = classDef.type
//                if (type is)
//                // from alias
//                type.lazyInit(context)
//                val member = LuaShortNamesManager.getInstance(context.project).findMember(type, fieldName, context)
//                if (member != null) {
//                    processor.process(member)
//                }
//            }
//        }

        fun process(
            className: String,
            fieldName: String,
            context: SearchContext,
            processor: Processor<LuaClassMember>,
            deep: Boolean = true
        ): Boolean {
            val key = "$className*$fieldName"
            if (!process(key, context, processor))
                return false

            if (deep) {
                val classDef = LuaClassIndex.find(className, context)
                if (classDef != null) {
                    val type = classDef.type
                    // from alias
                    type.lazyInit(context)
                    val notFound = type.processAlias(Processor {
                        process(it, fieldName, context, processor, false)
                    })
                    if (!notFound)
                        return false

                    var founded = false
                    TyClass.processSuperClass(type, context) { superType ->
                        LuaShortNamesManager.getInstance(context.project)
                            .processAllMembers(superType, fieldName, context, Processor {
                                processor.process(it)
                                founded = true
                                true
                            })
                        true
                    }
                    return founded
                }
            }
            return true
        }

        fun processOrigin(
            className: String,
            fieldName: String,
            context: SearchContext,
            processor: Processor<LuaClassMember>,
            deep: Boolean = true
        ): Boolean {
            val key = "$className*$fieldName"
            if (!process(key, context, processor))
                return false

            if (deep) {
                val classDef = LuaClassIndex.find(className, context)
                if (classDef != null) {
                    val type = classDef.type
                    // from alias
                    type.lazyInit(context)
                    val notFound = type.processAlias(Processor {
                        process(it, fieldName, context, processor, false)
                    })
                    if (!notFound)
                        return false

                    val superClassName = type.superClassName
                    if (superClassName != null) {
                        val superClass = LuaClassIndex.find(superClassName, context)
                        if (superClass is TyClass && !superClass.isInterface) {
                            return process(superClassName, fieldName, context, processor, true)
                        }
                    }
                }
            }
            return true
        }

        fun find(type: ITyClass, fieldName: String, context: SearchContext): LuaClassMember? {
            var perfect: LuaClassMember? = null
            var docField: LuaDocTagField? = null
            var tableField: LuaTableField? = null
            processAll(type, fieldName, context, Processor {
                when (it) {
                    is LuaDocTagField -> {
                        docField = it
                        false
                    }

                    is LuaTableField -> {
                        tableField = it
                        true
                    }

                    else -> {
                        if (perfect == null)
                            perfect = it
                        true
                    }
                }
            })
            if (docField != null) return docField
            if (tableField != null) return tableField
            return perfect
        }

        fun findOrigin(type: ITyClass, fieldName: String, context: SearchContext): LuaClassMember? {
            var perfect: LuaClassMember? = null
            var docField: LuaDocTagField? = null
            var tableField: LuaTableField? = null
            processOrigin(type.className, fieldName, context, {
                when (it) {
                    is LuaDocTagField -> {
                        docField = it
                        false
                    }

                    is LuaTableField -> {
                        tableField = it
                        true
                    }

                    else -> {
                        if (perfect == null)
                            perfect = it
                        true
                    }
                }
            })
            if (docField != null) return docField
            if (tableField != null) return tableField
            return perfect
        }

        fun processAll(
            type: ITyClass,
            fieldName: String,
            context: SearchContext,
            processor: Processor<LuaClassMember>
        ) {
            if (type is TyParameter)
                type.superClassName?.let { process(it, fieldName, context, processor) }
            else process(type.className, fieldName, context, processor)
        }

        fun processAll(type: ITyClass, context: SearchContext, processor: Processor<LuaClassMember>) {
            if (process(type.className, context, processor)) {
                type.lazyInit(context)
                type.processAlias(Processor {
                    process(it, context, processor)
                })
            }
        }

        fun findMethod(
            className: String,
            memberName: String,
            context: SearchContext,
            deep: Boolean = true
        ): LuaClassMethod? {
            var target: LuaClassMethod? = null
            process(className, memberName, context, Processor {
                if (it is LuaClassMethod) {
                    target = it
                    return@Processor false
                }
                true
            }, deep)
            return target
        }

        /*fun indexStub(indexSink: IndexSink, className: String, memberName: String) {
            indexSink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode())
            indexSink.occurrence(StubKeys.CLASS_MEMBER, "$className*$memberName".hashCode())
        }*/
    }
}