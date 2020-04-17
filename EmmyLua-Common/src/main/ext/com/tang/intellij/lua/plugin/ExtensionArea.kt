package com.tang.intellij.lua.plugin

class ExtensionArea {
    private val points = mutableMapOf<String, ExtensionPoint<*>>()

    fun registerExtensionPoint(point: ExtensionPoint<*>) {
        points[point.qualifiedName] = point
    }

    fun <T> getExtensionPoint(extensionPointName: String): ExtensionPoint<T>? {
        return points[extensionPointName] as ExtensionPoint<T>?
    }
}