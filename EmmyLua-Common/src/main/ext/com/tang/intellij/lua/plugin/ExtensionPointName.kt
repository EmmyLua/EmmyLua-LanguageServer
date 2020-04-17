@file:Suppress("UNUSED_PARAMETER")

package com.tang.intellij.lua.plugin

class ExtensionPointName<T>(val name: String) {

    private val emptyList = emptyList<T>()

    companion object {
        fun <K> create(name: String): ExtensionPointName<K> {
            return ExtensionPointName(name)
        }
    }

    val extensions: List<T> get() {
        val point = PluginManager.getExtensionPoint<T>(name)
        return point?.extensions ?: emptyList
    }
}