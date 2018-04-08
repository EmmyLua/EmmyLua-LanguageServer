package com.tang.vscode.vscode

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.services.LanguageClient

interface LuaLanguageClient : LanguageClient {
    @JsonNotification
    fun annotator(ann: Annotator)
}