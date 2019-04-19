package com.tang.vscode

import com.google.gson.*
import com.tang.intellij.lua.IConfiguration
import com.yevdo.jwildcard.JWildcard

private fun <T> listEquals(a: List<T>, b: List<T>): Boolean {
    if (a.size != b.size)
        return false
    for (i in 0 until a.size) {
        if (a[i] != b[i])
            return false
    }
    return true
}

class ConfigurationUpdateResult(
    val associationChanged: Boolean
)

private const val DEFAULT_ASSOCIATION = "*.lua"

object Configuration : IConfiguration {

    private var settings: JsonObject? = null

    private var mySourceRoots = mutableListOf<String>()

    val sourceRoots get() = mySourceRoots

    private var myCompletionCaseSensitive = false

    override val completionCaseSensitive get() = myCompletionCaseSensitive

    private var myShowCodeLens = false

    private val associations = mutableListOf(DEFAULT_ASSOCIATION)

    val showCodeLens get() = myShowCodeLens

    var clientType = "unknown"

    val isVSCode get() = clientType == "vsc"

    fun matchFile(name: String): Boolean {
        return associations.any { JWildcard.matches(it, name) }
    }

    fun update(settings: JsonObject): ConfigurationUpdateResult {
        this.settings = settings

        //files.associations
        val ass = path("files.associations")
        val oriAssociations = ArrayList(associations)
        associations.clear()
        associations.add(DEFAULT_ASSOCIATION)
        if (ass is JsonObject) {
            for (entry in ass.entrySet()) {
                val lan = entry.value.asString
                if (lan.toLowerCase() == "lua") {
                    val wildcard = entry.key
                    associations.add(wildcard)
                }
            }
        }
        associations.sort()
        val associationChanged = !listEquals(oriAssociations, associations)

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
        mySourceRoots.sort()

        // show codeLens
        myShowCodeLens = path("emmylua.codeLens")?.asBoolean == true

        return ConfigurationUpdateResult(associationChanged)
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