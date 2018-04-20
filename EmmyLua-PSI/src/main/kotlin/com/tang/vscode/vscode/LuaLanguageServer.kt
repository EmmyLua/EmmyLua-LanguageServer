package com.tang.vscode.vscode

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.core.LanguageParserDefinitions
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.reference.LuaReferenceContributor
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaLanguageServer : LanguageServer, LanguageClientAware {
    private val workspaceService: LuaWorkspaceService = LuaWorkspaceService()
    private val documentService: LuaTextDocumentService = LuaTextDocumentService(workspaceService)
    private var client: LuaLanguageClient? = null

    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(Object())
    }

    override fun getTextDocumentService() = documentService

    override fun exit() {

    }

    private fun initIntellijEnv() {
        LanguageParserDefinitions.INSTANCE.register(LuaLanguage.INSTANCE, LuaParserDefinition())
        documentService.initIntellijEnv()
        ReferenceProvidersRegistry.register(LuaReferenceContributor())
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        workspaceService.root = params.rootUri

        initIntellijEnv()

        val json = params.initializationOptions as JsonObject
        val stdFolder = json["stdFolder"] as? JsonPrimitive
        if (stdFolder != null && stdFolder.isString)
            workspaceService.addRoot(stdFolder.asString)
        val workspaceFolders = json["workspaceFolders"] as? JsonArray
        workspaceFolders?.forEach { workspaceService.addRoot(it.asString) }


        val res = InitializeResult()
        val capabilities = ServerCapabilities()

        val completionOptions = CompletionOptions()
        completionOptions.triggerCharacters = listOf(".", ":", "@")
        completionOptions.resolveProvider = true
        capabilities.completionProvider = completionOptions

        capabilities.definitionProvider = true
        capabilities.hoverProvider = true
        capabilities.referencesProvider = true

        capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)

        res.capabilities = capabilities
        return CompletableFuture.completedFuture(res)
    }

    override fun initialized(params: InitializedParams) {
        val watchers = listOf(FileSystemWatcher("**/*"))
        val options = DidChangeWatchedFilesRegistrationOptions(watchers)

        val id = "emmylua.${workspaceService.root}"
        val registration = Registration(id, "workspace/didChangeWatchedFiles", options)
        val registrations = listOf(registration)
        client?.registerCapability(RegistrationParams(registrations))

        workspaceService.loadWorkspace(object : IProgressMonitor {
            override fun setProgress(text: String) {
                client?.progressReport(ProgressReport(text))
            }
        })
    }

    override fun getWorkspaceService(): WorkspaceService {
        return workspaceService
    }

    override fun connect(client: LanguageClient) {
        val luaClient = client as LuaLanguageClient
        textDocumentService.connect(luaClient)

        this.client = luaClient
    }
}