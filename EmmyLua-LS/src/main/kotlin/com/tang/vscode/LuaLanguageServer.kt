package com.tang.vscode

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.core.LanguageParserDefinitions
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.tang.intellij.lua.plugin.PluginManager
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
        PluginManager.init()
        LanguageParserDefinitions.INSTANCE.register(LuaLanguage.INSTANCE, LuaParserDefinition())
        workspaceService.initIntellijEnv()
        ReferenceProvidersRegistry.register(LuaReferenceContributor())
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        if (params.workspaceFolders != null) {
            for (workspace in params.workspaceFolders) {
                workspaceService.addRoot(workspace.uri)
            }
        }
        initIntellijEnv()

        val json = params.initializationOptions as? JsonObject
        if (json != null) {
            val stdFolder = json["stdFolder"] as? JsonPrimitive
            if (stdFolder != null && stdFolder.isString) {
                workspaceService.addRoot(stdFolder.asString)
            }
            val clientType = json["client"] as? JsonPrimitive
            if (clientType != null)
                VSCodeSettings.clientType = clientType.asString
            // lua config files
            val configFileArray = json["configFiles"] as? JsonArray
            if (configFileArray != null) {
                val configFiles = EmmyConfigurationSource.parse(configFileArray)
                workspaceService.initConfigFiles(configFiles)
            }
        }

        val res = InitializeResult()
        val capabilities = ServerCapabilities()

        val completionOptions = CompletionOptions()
        completionOptions.triggerCharacters = listOf(".", ":", "@", "(")
        completionOptions.resolveProvider = true
        completionOptions.completionItem = CompletionItemOptions(true)
        capabilities.completionProvider = completionOptions

        capabilities.definitionProvider = Either.forLeft(true)
        capabilities.hoverProvider = Either.forLeft(true)
        capabilities.referencesProvider = Either.forLeft(true)
        capabilities.codeLensProvider = CodeLensOptions(true)
        capabilities.documentHighlightProvider = Either.forLeft(true)
        capabilities.renameProvider = Either.forLeft(true)

        capabilities.signatureHelpProvider = SignatureHelpOptions(listOf(",", "("))
        capabilities.documentSymbolProvider = Either.forLeft(true)
        capabilities.workspaceSymbolProvider = Either.forLeft(true)

        capabilities.workspace = WorkspaceServerCapabilities()
        capabilities.workspace.workspaceFolders = WorkspaceFoldersOptions()
        capabilities.workspace.workspaceFolders.supported = true
        capabilities.workspace.workspaceFolders.changeNotifications = Either.forLeft(WORKSPACE_FOLDERS_CAPABILITY_ID)

        capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)

        val inlayHintOptions = InlayHintRegistrationOptions()
        inlayHintOptions.resolveProvider = true
        capabilities.inlayHintProvider = Either.forRight(inlayHintOptions)

        capabilities.diagnosticProvider = DiagnosticRegistrationOptions(false, false)

//        capabilities.semanticTokensProvider = SemanticTokensWithRegistrationOptions(
//            SemanticTokensLegend(
//                listOf(),
//                listOf()
//            ),
//            true
//        )
//        capabilities.colorProvider = Either.forLeft(true)

        res.capabilities = capabilities
        return CompletableFuture.completedFuture(res)
    }

    override fun initialized(params: InitializedParams) {
        val options = DidChangeWatchedFilesRegistrationOptions(listOf(FileSystemWatcher("**/*")))
        val didChangeWatchedFiles =
            Registration(UUID.randomUUID().toString(), "workspace/didChangeWatchedFiles", options)
        client?.registerCapability(RegistrationParams(listOf(didChangeWatchedFiles)))
        val didChangeWorkspaceFolders = Registration(WORKSPACE_FOLDERS_CAPABILITY_ID, WORKSPACE_FOLDERS_CAPABILITY_NAME)
        client?.registerCapability(RegistrationParams(listOf(didChangeWorkspaceFolders)))
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

    override fun setTrace(params: SetTraceParams) {
    }
}