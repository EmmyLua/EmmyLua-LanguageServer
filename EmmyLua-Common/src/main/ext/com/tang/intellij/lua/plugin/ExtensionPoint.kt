package com.tang.intellij.lua.plugin

class ExtensionPoint<T>(val qualifiedName: String, val interfaceName: String) {

    private val adapters = mutableListOf<ExtensionAdapter>()
    private val list = mutableListOf<T>()

    fun addExtension(adapter: ExtensionAdapter) {
        adapters.add(adapter)
        val instance = adapter.createInstance<T>()
        list.add(instance)
    }

    val extensions: List<T> get() {
        return list
    }
}