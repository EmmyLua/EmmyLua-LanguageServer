@file:Suppress("unused")

package com.tang.vscode

import com.tang.vscode.extendApi.LuaReportApiParams
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.services.LanguageClient

interface LuaLanguageClient : LanguageClient {
    @JsonNotification("emmy/annotator")
    fun annotator(ann: AnnotatorParams)

    @JsonNotification("emmy/updateConfig")
    fun updateConfig(params: UpdateConfigParams)

    @JsonNotification("emmy/progressReport")
    fun progressReport(report: ProgressReport)

    @JsonNotification("emmy/setServerStatus")
    fun setServerStatus(status: ServerStatusParams)

    @JsonNotification("emmy/reportAPI")
    fun reportAPI(params: LuaReportApiParams)
}