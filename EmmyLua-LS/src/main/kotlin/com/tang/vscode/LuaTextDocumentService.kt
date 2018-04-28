package com.tang.vscode

import com.intellij.openapi.project.ProjectCoreUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Consumer
import com.tang.intellij.lua.editor.completion.CompletionService
import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.psi.LuaVisitor
import com.tang.intellij.lua.reference.ReferencesSearch
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.impl.LuaFile
import com.tang.vscode.documentation.LuaDocumentationProvider
import com.tang.vscode.utils.*
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
        return computeAsync {
            val list = mutableListOf<DocumentHighlight>()
            withPsiFile(position) { file, psiFile, i ->
                val target = TargetElementUtil.findTarget(psiFile, i)
                if (target != null) {
                    val def = target.reference?.resolve() ?: target

                    // self highlight
                    if (def.containingFile == psiFile) {
                        def.nameRange?.let { list.add(DocumentHighlight(it.toRange(file))) }
                    }

                    // references highlight
                    val search = ReferencesSearch.search(def, GlobalSearchScope.fileScope(psiFile))
                    search.forEach {
                        if (it.isReferenceTo(def)) {
                            list.add(DocumentHighlight(it.getRangeInFile(file)))
                        }
                    }
                }
            }
            list
        }
    }

    override fun onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        throw NotImplementedException()
    }

    override fun definition(position: TextDocumentPositionParams): CompletableFuture<MutableList<out Location>> {
        return computeAsync {
            val list = mutableListOf<Location>()

            withPsiFile(position) { _, psiFile, i ->
                val target = TargetElementUtil.findTarget(psiFile, i)
                val resolve = target?.reference?.resolve()
                if (resolve != null) {
                    val sourceFile = resolve.containingFile.virtualFile as LuaFile
                    val range = resolve.nameRange
                    if (range != null)
                        list.add(Location(sourceFile.uri.toString(), range.toRange(sourceFile)))
                }
            }

            list
        }
    }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        throw NotImplementedException()
    }

    override fun codeLens(params: CodeLensParams): CompletableFuture<MutableList<out CodeLens>> {
        return computeAsync {
            val list = mutableListOf<CodeLens>()
            workspace.findFile(params.textDocument.uri)?.let {
                val luaFile = it as? ILuaFile
                luaFile?.psi?.acceptChildren(object : LuaVisitor() {
                    override fun visitClassMethod(o: LuaClassMethod) {
                        val search = ReferencesSearch.search(o)
                        val findAll = search.findAll()
                        list.add(CodeLens(o.nameIdentifier!!.textRange.toRange(luaFile), Command("References:${findAll.size}", ""), null))
                    }
                })
            }
            list
        }
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> {
        return computeAsync {
            val changes = mutableListOf<TextDocumentEdit>()
            withPsiFile(params.textDocument, params.position) { _, psiFile, i ->
                val target = TargetElementUtil.findTarget(psiFile, i) ?: return@withPsiFile

                val map = mutableMapOf<String, MutableList<TextEdit>>()
                val def = target.reference?.resolve() ?: target

                def.nameRange?.let {
                    val refFile = def.containingFile.virtualFile as LuaFile
                    val uri = refFile.uri.toString()
                    val list = map.getOrPut(uri) { mutableListOf() }
                    list.add(TextEdit(it.toRange(refFile), params.newName))
                }

                // references
                val search = ReferencesSearch.search(def)
                search.forEach {
                    if (it.isReferenceTo(def)) {
                        val refFile = it.element.containingFile.virtualFile as LuaFile
                        val uri = refFile.uri.toString()
                        val list = map.getOrPut(uri) { mutableListOf() }
                        list.add(TextEdit(it.getRangeInFile(refFile), params.newName))
                    }
                }

                map.forEach { t, u ->
                    val documentIdentifier = VersionedTextDocumentIdentifier()
                    documentIdentifier.uri = t
                    changes.add(TextDocumentEdit(documentIdentifier, u))
                }
            }
            val edit = WorkspaceEdit(changes)
            edit
        }
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
        withPsiFile(params) { _, psiFile, pos ->
            val element = TargetElementUtil.findTarget(psiFile, pos)
            if (element != null) {
                val target = element.reference?.resolve() ?: element
                val query = ReferencesSearch.search(target)
                query.forEach { ref ->
                    val luaFile = ref.element.containingFile.virtualFile as LuaFile
                    list.add(Location(luaFile.uri.toString(), ref.getRangeInFile(luaFile)))
                }
            }
        }
        return CompletableFuture.completedFuture(list)
    }

    override fun resolveCodeLens(unresolved: CodeLens?): CompletableFuture<CodeLens> {
        throw NotImplementedException()
    }

    private fun withPsiFile(position: TextDocumentPositionParams, code: (ILuaFile, LuaPsiFile, Int) -> Unit) {
        withPsiFile(position.textDocument, position.position, code)
    }

    private fun withPsiFile(textDocument: TextDocumentIdentifier, position: Position, code: (ILuaFile, LuaPsiFile, Int) -> Unit) {
        val file = workspace.findFile(textDocument.uri)
        if (file is ILuaFile) {
            val psi = file.psi
            if (psi is LuaPsiFile) {
                val pos = file.getPosition(position.line, position.character)
                code(file, psi, pos)
            }
        }
    }

    fun initIntellijEnv() {
        ProjectCoreUtil.theProject = workspace.project
    }
}