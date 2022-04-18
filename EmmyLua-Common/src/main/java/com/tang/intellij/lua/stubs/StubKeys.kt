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

package com.tang.intellij.lua.stubs

import com.intellij.util.indexing.IndexId
import com.tang.intellij.lua.comment.psi.LuaDocTagAlias
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.psi.LuaClassMember
import com.tang.intellij.lua.psi.LuaPsiElement

object StubKeys {
    val CLASS_MEMBER: IndexId<Int, LuaClassMember> = IndexId.create<Int, LuaClassMember>("lua.index.class.member")
    val SHORT_NAME: IndexId<String, LuaPsiElement> = IndexId.create<String, LuaPsiElement>("lua.index.short_name")
    val CLASS: IndexId<String, LuaDocTagClass> = IndexId.create<String, LuaDocTagClass>("lua.index.class")
    val SUPER_CLASS: IndexId<String, LuaDocTagClass> = IndexId.create<String, LuaDocTagClass>("lua.index.super_class")
    val ALIAS: IndexId<String, LuaDocTagAlias> = IndexId.create<String, LuaDocTagAlias>("lua.index.alias")
    val CONST: IndexId<Int, LuaPsiElement> = IndexId.create<Int, LuaPsiElement>("lua.index.const")
}
