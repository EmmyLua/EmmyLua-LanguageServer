@file:Suppress("ArrayInDataClass", "MemberVisibilityCanBePrivate")

package com.tang.vscode.configuration

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.intellij.util.ThreeState
import com.tang.intellij.lua.configuration.IConfigurationStructure
import com.tang.intellij.lua.configuration.ISourceRoot
import com.tang.intellij.lua.configuration.Pattern
import com.tang.lsp.FileURI
import com.tang.vscode.EmmyConfigurationSource
import java.io.File
import java.io.FileReader
import java.io.IOException

data class SourceRoot(
        val dir: String,
        val exclude: Array<String>,
        val jsonSource: EmmyConfigurationSource
): ISourceRoot {
    override fun relative(path: String): String? {
        val f = FileURI(path, false)
        val r = absoluteDir?.relativize(f)
        if (r == f) {
            return null
        }
        return r?.toString()
    }

    companion object {
        fun readFrom(reader: JsonReader, jsonSource: EmmyConfigurationSource): SourceRoot? {
            var dir: String? = null
            val excludes = mutableListOf<String>()
            reader.readObject {
                when (it) {
                    "dir" -> dir = reader.nextString()
                    "exclude" -> {
                        reader.readArray { excludes.add(reader.nextString()) }
                    }
                    else -> reader.skipValue()
                }
            }

            val dirValue = dir ?: return null
            return SourceRoot(dirValue, excludes.toTypedArray(), jsonSource)
        }
    }

    override val absoluteDir: FileURI? by lazy {
        val absolute = File(dir).isAbsolute
        if (absolute) {
            FileURI(File(dir).toURI(), false)
        } else {
            jsonSource.fileURI.parent?.resolve(dir, false)
        }
    }

    override fun isExclude(uri: FileURI): ThreeState {
        val r = absoluteDir?.relativize(uri)
        if (r == null || r == uri) {
            return ThreeState.UNSURE
        }
        for (pattern in exclude) {
            if (Pattern.match(pattern, r.toString())) {
                return ThreeState.YES
            }
        }
        return ThreeState.NO
    }

    override fun isInclude(uri: FileURI): ThreeState {
        return when(isExclude(uri)) {
            ThreeState.YES -> ThreeState.NO
            ThreeState.NO -> ThreeState.YES
            else -> ThreeState.UNSURE
        }
    }
}

data class Editor(val completionCaseSensitive: Boolean = false) {
    companion object {
        fun readFrom(reader: JsonReader): Editor {
            var completionCaseSensitive = false
            reader.readObject {
                when (it) {
                    "completionCaseSensitive" -> {
                        completionCaseSensitive = reader.nextBoolean()
                    }
                    else -> reader.skipValue()
                }
            }
            return Editor(completionCaseSensitive)
        }
    }
}

class ConfigurationStructure(val source: EmmyConfigurationSource) : IConfigurationStructure {
    var luaVersion = "lua5.4"
    override val sourceRoots = mutableListOf<SourceRoot>()
    var editor: Editor? = null

    init { reload() }

    fun reload() {
        source.fileURI.toFile()?.let { file ->
            val jsonReader = Gson().newJsonReader(FileReader(file))
            readFrom(jsonReader)
        }
    }

    fun readFrom(reader: JsonReader) {
        reader.readObject { name ->
            when (name) {
                "lua.version" -> {
                    luaVersion = reader.nextString()
                }
                "source" -> {
                    this.sourceRoots.clear()
                    reader.readArray {
                        val source = SourceRoot.readFrom(reader, source)
                        if (source != null) this.sourceRoots.add(source)
                    }
                }
                "editor" -> {
                    this.editor = Editor.readFrom(reader)
                }
                else -> reader.skipValue()
            }
        }
    }
}

private fun JsonReader.readObject(action: (name: String) -> Unit) {
    try {
        val type = peek()
        if (type == JsonToken.BEGIN_OBJECT) {
            beginObject()
            while (hasNext()) {
                action(nextName())
            }
            endObject()
        } else skipValue()
    } catch (e: IOException) {
    }
}

private fun JsonReader.readArray(action: () -> Unit) {
    try {
        val type = peek()
        if (type == JsonToken.BEGIN_ARRAY) {
            beginArray()
            while (hasNext()) {
                action()
            }
            endArray()
        } else skipValue()
    } catch (e: IOException) {
    }
}