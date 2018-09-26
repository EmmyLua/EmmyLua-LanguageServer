package com.tang.vscode

import com.google.gson.*

class Configuration {

    private var settings: JsonObject? = null

    private var _sourceRoots = mutableListOf<String>()

    val sourceRoots get() = _sourceRoots

    fun update(settings: JsonObject) {
        this.settings = settings

        // source roots
        _sourceRoots.clear()
        val sourceRoots = path("emmylua.source.roots")
        if (sourceRoots is JsonArray) {
            sourceRoots.forEach {
                if (it is JsonPrimitive)
                    _sourceRoots.add(it.asString)
            }
        }
    }

    private fun path(path: String): JsonElement? {
        val arr = path.split(".")
        var ret: JsonElement = settings ?: return null
        for (s in arr) {
            if (ret !is JsonObject || ret is JsonNull)
                return null
            ret = ret.get(s) ?: return null
        }
        return ret
    }
}