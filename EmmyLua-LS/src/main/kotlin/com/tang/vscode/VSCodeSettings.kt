package com.tang.vscode

import com.google.gson.*
import com.tang.intellij.lua.IVSCodeSettings
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

class SettingsUpdateResult(
    val associationChanged: Boolean
)

private const val DEFAULT_ASSOCIATION = "*.lua"

object VSCodeSettings : IVSCodeSettings {

    private var settings: JsonObject? = null

    private var mySourceRoots = mutableListOf<String>()

    override val sourceRoots get() = mySourceRoots

    private var myCompletionCaseSensitive = false

    override val completionCaseSensitive get() = myCompletionCaseSensitive

    private var myShowCodeLens = false

    private val associations = mutableListOf(DEFAULT_ASSOCIATION)

    override val showCodeLens get() = myShowCodeLens

    var clientType = "unknown"

    override val isVSCode get() = clientType == "vsc"

    override fun matchFile(name: String): Boolean {
        return associations.any { JWildcard.matches(it, name) }
    }

    fun update(settings: JsonObject): SettingsUpdateResult {
        this.settings = settings

        //files.associations
        val associationChanged = updateAssociations()

        //case sensitive
        val caseSensitive = path("emmylua.completion.caseSensitive")
        if (caseSensitive != null) {
            myCompletionCaseSensitive = caseSensitive.asBoolean
        }

        // source roots
        updateSourceRoots()

        // show codeLens
        myShowCodeLens = path("emmylua.codeLens")?.asBoolean == true

        return SettingsUpdateResult(associationChanged)
    }

    private fun updateSourceRoots() {
        mySourceRoots.clear()
        val sourceRoots = path("emmylua.source.roots")
        if (sourceRoots is JsonArray) {
            sourceRoots.forEach {
                if (it is JsonPrimitive)
                    mySourceRoots.add(it.asString)
            }
        }
        mySourceRoots.sort()
    }

    private fun updateAssociations(): Boolean {
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
        return !listEquals(oriAssociations, associations)
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