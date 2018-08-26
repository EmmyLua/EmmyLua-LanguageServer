@file:Suppress("unused")

package com.tang.vscode

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.services.LanguageClient

interface LuaLanguageClient : LanguageClient {
    @JsonNotification("emmy/annotator")
    fun annotator(ann: AnnotatorParams)

    @JsonNotification("emmy/progressReport")
    fun progressReport(report: ProgressReport)
}