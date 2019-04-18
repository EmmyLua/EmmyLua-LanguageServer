package com.tang.vscode

import com.google.gson.*
import com.tang.intellij.lua.IConfiguration
import com.yevdo.jwildcard.JWildcard

object Configuration : IConfiguration {

    private var settings: JsonObject? = null

    private var mySourceRoots = mutableListOf<String>()

    val sourceRoots get() = mySourceRoots

    private var myCompletionCaseSensitive = false

    override val completionCaseSensitive get() = myCompletionCaseSensitive

    private var myShowCodeLens = false

    private val associations = mutableListOf<String>()

    val showCodeLens get() = myShowCodeLens

    var clientType = "unknown"

    val isVSCode get() = clientType == "vsc"

    fun matchFile(name: String): Boolean {
        return associations.any { JWildcard.matches(it, name) }
    }

    fun update(settings: JsonObject) {
        this.settings = settings

        //files.associations
        val ass = path("files.associations")
        associations.clear()
        associations.add("*.lua")
        if (ass is JsonObject) {
            for (entry in ass.entrySet()) {
                val lan = entry.value.asString
                if (lan.toLowerCase() == "lua") {
                    val wildcard = entry.key
                    associations.add(wildcard)
                }
            }
        }

        //case sensitive
        val caseSensitive = path("emmylua.completion.caseSensitive")
        if (caseSensitive != null) {
            myCompletionCaseSensitive = caseSensitive.asBoolean
        }

        // source roots
        mySourceRoots.clear()
        val sourceRoots = path("emmylua.source.roots")
        if (sourceRoots is JsonArray) {
            sourceRoots.forEach {
                if (it is JsonPrimitive)
                    mySourceRoots.add(it.asString)
            }
        }

        // show codeLens
        myShowCodeLens = path("emmylua.codeLens")?.asBoolean == true
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