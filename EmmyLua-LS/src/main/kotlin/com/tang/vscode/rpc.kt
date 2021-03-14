package com.tang.vscode

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.tang.lsp.FileURI
import org.eclipse.lsp4j.Range

enum class AnnotatorType {
    Param,
    Global,
    DocName,
    Upvalue,
    ParamHint,
    LocalHint
}

data class AnnotatorParams(val uri: String)

data class RenderRange(val range: Range, var hint: String?)

data class Annotator(val uri: String, val ranges: List<RenderRange>, val type: AnnotatorType)

data class ProgressReport(val text: String, val percent: Float)

enum class UpdateType {
    Created,
    Changed,
    Deleted
}

data class EmmyConfigurationSource(val uri: String, val workspace: String) {
    companion object {
        fun parse(arr: JsonArray): Array<EmmyConfigurationSource> {
            val list = mutableListOf<EmmyConfigurationSource>()
            for (element in arr) {
                if (element is JsonObject) {
                    val cfg = parse(element)
                    list.add(cfg)
                }
            }
            return list.toTypedArray()
        }

        private fun parse(json: JsonObject): EmmyConfigurationSource {
            val uri = json["uri"].asString
            val workspace = json["workspace"].asString
            return EmmyConfigurationSource(uri, workspace)
        }
    }

    val fileURI: FileURI get() {
        return FileURI.uri(uri, false)
    }

    override fun equals(other: Any?): Boolean {
        return other is EmmyConfigurationSource && other.uri == uri
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + workspace.hashCode()
        return result
    }
}

data class UpdateConfigParams(val type: UpdateType, val source: EmmyConfigurationSource)