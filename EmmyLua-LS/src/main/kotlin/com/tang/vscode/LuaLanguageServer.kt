package com.tang.vscode

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.core.LanguageParserDefinitions
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.tang.intellij.lua.ext.ILuaFileResolver
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.reference.LuaReferenceContributor
import com.tang.vscode.utils.computeAsync
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.WorkspaceService
import java.net.URI
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaLanguageServer : LanguageServer, LanguageClientAware {
    private val workspaceService: LuaWorkspaceService = LuaWorkspaceService()
    private val documentService: LuaTextDocumentService = LuaTextDocumentService(workspaceService)
    private var client: LuaLanguageClient? = null

    companion object {
        private val WORKSPACE_FOLDERS_CAPABILITY_ID = UUID.randomUUID().toString()
        private const val WORKSPACE_FOLDERS_CAPABILITY_NAME = "workspace/didChangeWorkspaceFolders"
    }

    override fun shutdown(): CompletableFuture<Any> {
        return computeAsync {
            workspaceService.dispose()
        }
    }

    override fun getTextDocumentService() = documentService

    override fun exit() {

    }

    private fun initIntellijEnv() {
        LanguageParserDefinitions.INSTANCE.register(LuaLanguage.INSTANCE, LuaParserDefinition())
        documentService.initIntellijEnv()
        ReferenceProvidersRegistry.register(LuaReferenceContributor())
        ILuaFileResolver.EP_NAME.add(LuaFileResolver())
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        if (params.rootUri != null)
            workspaceService.root = URI(params.rootUri)

        initIntellijEnv()

        val json = params.initializationOptions as? JsonObject
        if (json != null) {
            val stdFolder = json["stdFolder"] as? JsonPrimitive
            if (stdFolder != null && stdFolder.isString)
                workspaceService.addRoot(stdFolder.asString)
            val workspaceFolders = json["workspaceFolders"] as? JsonArray
            workspaceFolders?.forEach { workspaceService.addRoot(it.asString) }
        }

        val res = InitializeResult()
        val capabilities = ServerCapabilities()

        val completionOptions = CompletionOptions()
        completionOptions.triggerCharacters = listOf(".", ":", "@")
        completionOptions.resolveProvider = true
        capabilities.completionProvider = completionOptions

        capabilities.definitionProvider = true
        capabilities.hoverProvider = true
        capabilities.referencesProvider = true
        //capabilities.codeLensProvider = CodeLensOptions(true)
        capabilities.documentHighlightProvider = true
        capabilities.renameProvider = true
        capabilities.signatureHelpProvider = SignatureHelpOptions(listOf(",", "("))
        capabilities.documentSymbolProvider = true
        capabilities.workspaceSymbolProvider = true

        capabilities.workspace = WorkspaceServerCapabilities()
        capabilities.workspace.workspaceFolders = WorkspaceFoldersOptions()
        capabilities.workspace.workspaceFolders.supported = true
        capabilities.workspace.workspaceFolders.changeNotifications = Either.forLeft(WORKSPACE_FOLDERS_CAPABILITY_ID)

        capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)

        res.capabilities = capabilities
        return CompletableFuture.completedFuture(res)
    }

    override fun initialized(params: InitializedParams) {
        val options = DidChangeWatchedFilesRegistrationOptions(listOf(FileSystemWatcher("**/*")))
        val didChangeWatchedFiles = Registration(UUID.randomUUID().toString(), "workspace/didChangeWatchedFiles", options)
        client?.registerCapability(RegistrationParams(listOf(didChangeWatchedFiles)))
        val didChangeWorkspaceFolders = Registration(WORKSPACE_FOLDERS_CAPABILITY_ID, WORKSPACE_FOLDERS_CAPABILITY_NAME)
        client?.registerCapability(RegistrationParams(listOf(didChangeWorkspaceFolders)))

        workspaceService.loadWorkspace()
    }

    override fun getWorkspaceService(): WorkspaceService {
        return workspaceService
    }

    override fun connect(client: LanguageClient) {
        val luaClient = client as LuaLanguageClient
        textDocumentService.connect(luaClient)
        workspaceService.connect(luaClient)

        this.client = luaClient
    }
}