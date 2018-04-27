package com.tang.vscode.vscode

import com.intellij.openapi.project.ProjectCoreUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.util.Consumer
import com.tang.intellij.lua.editor.completion.CompletionService
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.reference.ReferencesSearch
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.impl.LuaFile
import com.tang.vscode.documentation.LuaDocumentationProvider
import com.tang.vscode.utils.TargetElementUtil
import com.tang.vscode.utils.computeAsync
import com.tang.vscode.utils.toRange
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import java.io.File
import java.net.URI
import java.util.concurrent.CompletableFuture

/**
 * tangzx
 * Created by Client on 2018/3/20.
 */
class LuaTextDocumentService(private val workspace: LuaWorkspaceService) : TextDocumentService {
    private var client: LuaLanguageClient? = null
    private val documentProvider = LuaDocumentationProvider()

    fun connect(client: LuaLanguageClient) {
        this.client = client
    }

    override fun resolveCompletionItem(item: CompletionItem): CompletableFuture<CompletionItem> {
        return computeAsync {
            item.documentation = "doc for ${item.label}"
            item.detail = "detail for ${item.label}"
            item
        }
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<MutableList<out Command>> {
        throw NotImplementedException()
    }

    override fun hover(position: TextDocumentPositionParams): CompletableFuture<Hover?> {
        return computeAsync {
            var hover: Hover? = null

            val file = workspace.findFile(position.textDocument.uri)
            if (file is ILuaFile) {
                val pos = file.getPosition(position.position.line, position.position.character)
                val element = TargetElementUtil.findTarget(file.psi, pos)
                if (element != null) {
                    val ref = element.reference?.resolve() ?: element
                    val doc = documentProvider.generateDoc(ref, element)
                    if (doc != null)
                        hover = Hover(listOf(Either.forLeft(doc)))
                }
            }

            hover
        }
    }

    override fun documentHighlight(position: TextDocumentPositionParams): CompletableFuture<MutableList<out DocumentHighlight>> {
        throw NotImplementedException()
    }

    override fun onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        throw NotImplementedException()
    }

    override fun definition(position: TextDocumentPositionParams): CompletableFuture<MutableList<out Location>> {
        return computeAsync {
            val list = mutableListOf<Location>()

            val file = workspace.findFile(position.textDocument.uri)
            if (file is ILuaFile) {
                val pos = file.getPosition(position.position.line, position.position.character)
                var element = file.psi?.findElementAt(pos)
                while (element != null) {
                    val reference = element.reference
                    if (reference != null) {
                        val result = reference.resolve()
                        if (result != null) {
                            val sourceFile = (result.containingFile as? LuaPsiFile)?.virtualFile as? LuaFile
                            if (sourceFile != null) {
                                var textRange = result.textRange
                                if (result is PsiNameIdentifierOwner)
                                    textRange = result.nameIdentifier?.textRange
                                list.add(Location(sourceFile.uri.toString(), textRange.toRange(sourceFile)))
                            }
                            break
                        }
                    }
                    element = element.parent
                }
            }

            list
        }
    }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        throw NotImplementedException()
    }

    override fun codeLens(params: CodeLensParams): CompletableFuture<MutableList<out CodeLens>> {
        throw NotImplementedException()
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> {
        throw NotImplementedException()
    }

    override fun completion(position: TextDocumentPositionParams): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        return computeAsync { checker ->
            val list = CompletionList()
            list.items = mutableListOf()
            val file = workspace.findFile(position.textDocument.uri)
            if (file is ILuaFile) {
                val pos = file.getPosition(position.position.line, position.position.character)
                val psi = file.psi
                if (psi != null) {
                    CompletionService.collectCompletion(psi, pos, Consumer {
                        checker.checkCanceled()
                        list.items.add(it)
                    })
                }
            }
            Either.forRight<MutableList<CompletionItem>, CompletionList>(list)
        }
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        throw NotImplementedException()
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val uri = params.textDocument.uri
        var file = workspace.findFile(uri)
        if (file == null) {
            val u = URI(uri)
            file = workspace.addFile(File(u.path), params.textDocument.text)
        }
        if (file is LuaFile) {
            val diagnosticsParams = PublishDiagnosticsParams(params.textDocument.uri, file.diagnostics)
            client?.publishDiagnostics(diagnosticsParams)
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {

    }

    override fun signatureHelp(position: TextDocumentPositionParams?): CompletableFuture<SignatureHelp> {
        throw NotImplementedException()
    }

    override fun didClose(params: DidCloseTextDocumentParams) {

    }

    override fun formatting(params: DocumentFormattingParams?): CompletableFuture<MutableList<out TextEdit>> {
        throw NotImplementedException()
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val file = workspace.findFile(params.textDocument.uri)
        if (file is ILuaFile) {
            file.didChange(params)
            val diagnosticsParams = PublishDiagnosticsParams(params.textDocument.uri, file.diagnostics)
            client?.publishDiagnostics(diagnosticsParams)
        }
    }

    override fun references(params: ReferenceParams): CompletableFuture<MutableList<out Location>> {
        val list = mutableListOf<Location>()
        withPsiFile(params) { file, pos ->
            var element = file.findElementAt(pos)
            while (element != null && element !is PsiFile) {
                val reference = element.reference
                var found = false
                var target: PsiElement? = null
                if (reference != null) {
                    target = reference.resolve()
                    found = true
                } else if (element is PsiNamedElement) {
                    target = element
                    found = true
                }
                if (found) {
                    if (target != null) {
                        val query = ReferencesSearch.search(target)
                        query.forEach { ref ->
                            if (ref.isReferenceTo(target)) {
                                var textRange = ref.rangeInElement
                                val parentRange = ref.element.textRange
                                textRange = textRange.shiftRight(parentRange.startOffset)
                                val luaFile = ref.element.containingFile.virtualFile as LuaFile
                                list.add(Location(luaFile.uri.toString(), textRange.toRange(luaFile)))
                            }
                        }
                    }
                    break
                }
                element = element.parent
            }
        }
        return CompletableFuture.completedFuture(list)
    }

    override fun resolveCodeLens(unresolved: CodeLens?): CompletableFuture<CodeLens> {
        throw NotImplementedException()
    }

    private fun withPsiFile(position: TextDocumentPositionParams, code: (LuaPsiFile, Int) -> Unit) {
        val file = workspace.findFile(position.textDocument.uri)
        if (file is ILuaFile) {
            val psi = file.psi
            if (psi is LuaPsiFile) {
                val pos = file.getPosition(position.position.line, position.position.character)
                code(psi, pos)
            }
        }
    }

    fun initIntellijEnv() {
        ProjectCoreUtil.theProject = workspace.project
    }
}