package com.tang.intellij.lua.editor

import com.intellij.openapi.editor.Caret

class CaretImpl(private val caretOffset: Int) : Caret {
    override fun getOffset(): Int {
        return caretOffset
    }
}