@file:Suppress("UNUSED_PARAMETER")

package com.tang.intellij.lua.ext

class ExtensionPointName<T> {

    private val list = mutableListOf<T>()

    fun add(t: T) {
        list.add(t)
    }

    val extensions = list

    companion object {
        fun <K> create(name: String): ExtensionPointName<K> {
            return ExtensionPointName()
        }
    }
}