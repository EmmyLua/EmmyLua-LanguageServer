package com.tang.intellij.lua.plugin

abstract class ExtensionAdapter {

    abstract fun <T> createInstance(): T

}

class XmlExtensionAdapter(
        private val descriptor: PluginDescriptor,
        private val implementation: String) : ExtensionAdapter()
{
    override fun <T> createInstance(): T {
        val impl = descriptor.classLoader?.loadClass(implementation)
        val instance = impl?.newInstance()
        return instance as T
    }
}