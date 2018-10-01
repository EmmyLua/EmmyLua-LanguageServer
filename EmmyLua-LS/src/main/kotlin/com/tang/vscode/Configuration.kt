package com.tang.vscode

import com.google.gson.*
import com.tang.intellij.lua.IConfiguration

object Configuration : IConfiguration {

    private var settings: JsonObject? = null

    private var _sourceRoots = mutableListOf<String>()

    val sourceRoots get() = _sourceRoots

    private var _completionCaseSensitive = false

    override val completionCaseSensitive get() = _completionCaseSensitive

    fun update(settings: JsonObject) {
        this.settings = settings

        //case sensitive
        val caseSensitive = path("emmylua.completion.caseSensitive")
        if (caseSensitive != null) {
            _completionCaseSensitive = caseSensitive.asBoolean
        }

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