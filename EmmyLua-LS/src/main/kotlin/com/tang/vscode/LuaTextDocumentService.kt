package com.tang.vscode

import com.google.gson.JsonPrimitive
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import com.intellij.util.Processor
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.editor.completion.CompletionService
import com.tang.intellij.lua.editor.completion.LuaLookupElement
import com.tang.intellij.lua.editor.completion.asCompletionItem
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.reference.ReferencesSearch
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassMemberIndex
import com.tang.intellij.lua.ty.*
import com.tang.lsp.*
import com.tang.vscode.api.impl.LuaFile
import com.tang.vscode.diagnostics.DiagnosticsService
//import com.tang.vscode.color.ColorService
import com.tang.vscode.documentation.LuaDocumentationProvider
import com.tang.vscode.extendApi.ExtendApiService
import com.tang.vscode.extendApi.LuaReportApiParams
import com.tang.vscode.formatter.FormattingFormatter
import com.tang.vscode.formatter.FormattingType
import com.tang.vscode.inlayHint.InlayHintService
import com.tang.vscode.utils.TargetElementUtil
import com.tang.vscode.utils.computeAsync
import com.tang.vscode.utils.getDocumentSymbols
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.TextDocumentService
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

    @Suppress("unused")
    @JsonRequest("emmy/annotator")
    fun updateAnnotators(ann: AnnotatorParams): CompletableFuture<List<Annotator>> {
        return computeAsync {
            workspace.ready {
                val file = workspace.findFile(ann.uri) as? ILuaFile
                if (file != null)
                    findAnnotators(file)
                else
                    emptyList()
            }
        }
    }

    @Suppress("unused")
    @JsonRequest("emmy/reportAPI")
    fun reportAPI(params: LuaReportApiParams): CompletableFuture<Void> {
        workspace.canWrite {
            ExtendApiService.loadApi(workspace.getProject(), params)
        }
        return CompletableFuture()
    }

    private fun findAnnotators(file: ILuaFile): List<Annotator> {
        val params = mutableListOf<TextRange>()
        val globals = mutableListOf<TextRange>()
        val docTypeNames = mutableListOf<TextRange>()
        val upValues = mutableListOf<TextRange>()
        val notUse = mutableListOf<TextRange>()

        // 认为所有local名称定义一开始都是未使用的
        val psiNotUse = mutableSetOf<PsiElement>()
        file.psi?.acceptChildren(object : LuaRecursiveVisitor() {
            override fun visitParamNameDef(o: LuaParamNameDef) {
                psiNotUse.add(o)
            }

            override fun visitFuncDef(o: LuaFuncDef) {
                val name = o.nameIdentifier
                if (name != null && o.forwardDeclaration == null) {
                    globals.add(name.textRange)
                }
                super.visitFuncDef(o)
            }

            override fun visitNameExpr(o: LuaNameExpr) {
                if (o.parent is LuaExprStat) // non-complete stat
                    return

                val context = SearchContext.get(o.project)
                val declPsi = resolveInFile(o.name, o, context)

                if (psiNotUse.contains(declPsi)) {
                    psiNotUse.remove(declPsi)
                    // 不能和下面的合并因为不想重复渲染
                    when (declPsi) {
                        is LuaParamNameDef -> {
                            params.add(declPsi.textRange)
                        }
                    }
                }

                when (declPsi) {
                    is LuaParamNameDef -> params.add(o.textRange)
                    is LuaFuncDef -> globals.add(o.textRange)
                    is LuaNameDef -> {
                    } //local
                    is LuaLocalFuncDef -> {
                    } //local
                    else -> {
                        if (o.firstChild.textMatches(Constants.WORD_SELF)) {
                            // SELF
                        } else
                            globals.add(o.textRange)
                    }
                }

                if (isUpValue(o, context))
                    upValues.add(o.textRange)
            }

            override fun visitLocalFuncDef(o: LuaLocalFuncDef) {
                psiNotUse.add(o)
                o.acceptChildren(this)
            }

            override fun visitNameDef(o: LuaNameDef) {
                psiNotUse.add(o)
            }

            override fun visitElement(element: PsiElement) {
                if (element is LuaComment) {
                    element.acceptChildren(object : LuaDocVisitor() {

                        override fun visitTagClass(o: LuaDocTagClass) {
                            val identifier = o.nameIdentifier
                            docTypeNames.add(identifier.textRange)
                            super.visitTagClass(o)
                        }

                        override fun visitClassNameRef(o: LuaDocClassNameRef) {
                            docTypeNames.add(o.textRange)
                        }

                        override fun visitElement(element: PsiElement) {
                            element.acceptChildren(this)
                        }

                        override fun visitTagAlias(o: LuaDocTagAlias) {
                            val identifier = o.nameIdentifier
                            if (identifier != null)
                                docTypeNames.add(identifier.textRange)
                            super.visitTagAlias(o)
                        }
                    })
                } else
                    super.visitElement(element)
            }
        })

        psiNotUse.forEach {
            when (it) {
                is LuaLocalFuncDef -> {
                    it.nameRange?.let { it1 -> notUse.add(it1) };
                }

                else -> {
                    notUse.add(it.textRange)
                }
            }

        }

        val all = mutableListOf<Annotator>()
        val uri = file.uri.toString()
        if (params.isNotEmpty())
            all.add(Annotator(uri, params.map { RenderRange(it.toRange(file), null) }, AnnotatorType.Param))
        if (globals.isNotEmpty())
            all.add(Annotator(uri, globals.map { RenderRange(it.toRange(file), null) }, AnnotatorType.Global))
        if (docTypeNames.isNotEmpty())
            all.add(Annotator(uri, docTypeNames.map { RenderRange(it.toRange(file), null) }, AnnotatorType.DocName))
        if (upValues.isNotEmpty())
            all.add(Annotator(uri, upValues.map { RenderRange(it.toRange(file), null) }, AnnotatorType.Upvalue))
        if (notUse.isNotEmpty()) {
            all.add(Annotator(uri, notUse.map { RenderRange(it.toRange(file), null) }, AnnotatorType.NotUse))
        }

        return all
    }

    override fun resolveCompletionItem(item: CompletionItem): CompletableFuture<CompletionItem> {
        return computeAsync {
            workspace.ready {
                val data = item.data
                if (data is JsonPrimitive) {
                    val arr = data.asString.split("|")
                    if (arr.size == 2) {
                        val file = workspace.findLuaFile(arr[0])
                        if (file is ILuaFile) {
                            val position = arr[1].toInt()
                            file.psi?.findElementAt(position)?.let { psi ->
                                PsiTreeUtil.getParentOfType(psi, LuaClassMember::class.java)?.let { member ->
                                    val doc = documentProvider.generateDoc(member, true)
                                    val content = MarkupContent()
                                    content.kind = "markdown"
                                    content.value = doc
                                    item.documentation = Either.forRight(content)
                                }
                            }
                        }
                    } else if (arr.size == 3 && arr[0] == "extendApi") {
                        val doc = documentProvider.generateExtendDoc(arr[1], arr[2])
                        if (doc != null) {
                            val content = MarkupContent()
                            content.kind = "markdown"
                            content.value = doc
                            item.documentation = Either.forRight(content)
                        }
                    }
                }
                item
            }
        }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover?> {
        return computeAsync {
            workspace.ready {
                val file = workspace.findFile(params.textDocument.uri)
                var hover: Hover? = null
                if (file is ILuaFile) {
                    val pos = file.getPosition(params.position.line, params.position.character)
                    val element = TargetElementUtil.findTarget(file.psi, pos)
                    if (element != null) {
                        val ref = element.reference?.resolve() ?: element
                        val doc = documentProvider.generateDoc(ref, false)
                        if (doc != null)
                            hover = Hover(listOf(Either.forLeft(doc)))
                    }
                }

                hover
            }
        }
    }

    override fun documentHighlight(params: DocumentHighlightParams): CompletableFuture<MutableList<out DocumentHighlight>?> {
        return computeAsync {
            workspace.ready {
                val list = mutableListOf<DocumentHighlight>()

                withPsiFile(params.textDocument, params.position) { file, psiFile, i ->
                    val target = TargetElementUtil.findTarget(psiFile, i)
                    if (target != null) {
                        val def = target.reference?.resolve() ?: target

                        // self highlight
                        if (def.containingFile == psiFile) {
                            def.nameRange?.let { range -> list.add(DocumentHighlight(range.toRange(file))) }
                        }

                        // references highlight
                        val search = ReferencesSearch.search(def, GlobalSearchScope.fileScope(psiFile))
                        search.forEach { reference ->
                            list.add(DocumentHighlight(reference.getRangeInFile(file)))
                        }
                    }
                }

                list
            }
        }
    }

    override fun definition(params: DefinitionParams?): CompletableFuture<Either<MutableList<out Location>, MutableList<out LocationLink>>?> {
        return computeAsync {
            workspace.ready {
                val list = mutableListOf<Location>()
                if (params != null) {
                    withPsiFile(params.textDocument, params.position) { _, psiFile, i ->
                        val target = TargetElementUtil.findTarget(psiFile, i)
                        val resolve = target?.reference?.resolve()
                        if (resolve != null) {
                            if (resolve is ExtendApiBase) {
                                val locationText = resolve.getLocation();
                                val locationList = locationText.split('#')
                                if (locationList.size == 2) {
                                    val line = locationList[1].toInt()
                                    list.add(Location(locationList[0], Range(Position(line - 1, 0), Position(line, 0))))
                                }

                                return@withPsiFile
                            }
                            val sourceFile = resolve.containingFile?.virtualFile as? LuaFile
                            val range = resolve.nameRange
                            if (range != null && sourceFile != null)
                                list.add(Location(sourceFile.uri.toString(), range.toRange(sourceFile)))
                        } else if (target != null) {
                            val luaFile = psiFile.virtualFile as LuaFile
                            list.add(Location(luaFile.uri.toString(), target.textRange.toRange(luaFile)))
                        }
                    }
                }
                Either.forLeft(list)
            }
        }
    }

    override fun codeLens(params: CodeLensParams): CompletableFuture<MutableList<out CodeLens>> {
        return computeAsync { cc ->
            workspace.ready {
                val file = workspace.findFile(params.textDocument.uri)
                val list = mutableListOf<CodeLens>()
                if (VSCodeSettings.showCodeLens) {
                    if (file is ILuaFile) {
                        file.psi?.acceptChildren(object : LuaVisitor() {
                            override fun visitClassMethod(o: LuaClassMethod) {
                                cc.checkCanceled()
                                o.nameIdentifier?.let { id ->
                                    val range = id.textRange.toRange(file)
                                    list.add(CodeLens(range, null, params.textDocument.uri))
                                }
                            }

                            override fun visitLocalFuncDef(o: LuaLocalFuncDef) {
                                cc.checkCanceled()
                                o.nameIdentifier?.let { id ->
                                    val range = id.textRange.toRange(file)
                                    list.add(CodeLens(range, null, params.textDocument.uri))
                                }
                                super.visitLocalFuncDef(o)
                            }
                        })
                    }
                }
                list
            }
        }
    }

    override fun resolveCodeLens(unresolved: CodeLens): CompletableFuture<CodeLens> {
        return computeAsync {
            workspace.ready {
                val data = unresolved.data as? JsonPrimitive
                val command = Command("References:0", "emmy.showReferences")
                val uri = data?.asString
                if (uri != null) {
                    val file = workspace.findFile(uri)
                    if (file is ILuaFile) {
                        val pos = file.getPosition(unresolved.range.start.line, unresolved.range.start.character)
                        val target = TargetElementUtil.findTarget(file.psi, pos)
                        if (target != null) {
                            val search = ReferencesSearch.search(target)
                            val findAll = search.findAll()
                            command.title = "References:${findAll.size}"
                            if (findAll.isNotEmpty()) {
                                command.command = "emmy.showReferences"
                                command.arguments = listOf(uri, unresolved.range.start)
                            }
                        }
                    }

                }
                unresolved.command = command
                unresolved
            }
        }
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> {
        return computeAsync {
            workspace.ready {
                val changes = mutableListOf<TextDocumentEdit>()
                withPsiFile(params.textDocument, params.position) { _, psiFile, i ->
                    val target = TargetElementUtil.findTarget(psiFile, i) ?: return@withPsiFile

                    val map = mutableMapOf<String, MutableList<TextEdit>>()
                    val def = target.reference?.resolve() ?: target
                    var refRange: Range? = null
                    def.nameRange?.let { range ->
                        val refFile = def.containingFile.virtualFile as LuaFile
                        val uri = refFile.uri.toString()
                        val list = map.getOrPut(uri) { mutableListOf() }
                        refRange = range.toRange(refFile)
                        list.add(TextEdit(refRange, params.newName))
                    }

                    // references
                    val search = ReferencesSearch.search(def)
                    search.forEach { reference ->
                        val refFile = reference.element.containingFile.virtualFile as LuaFile
                        val uri = refFile.uri.toString()
                        val list = map.getOrPut(uri) { mutableListOf() }
                        val range = reference.getRangeInFile(refFile);
                        if (range != refRange) {
                            list.add(TextEdit(range, params.newName))
                        }
                    }

                    map.forEach { (t, u) ->
                        val documentIdentifier = VersionedTextDocumentIdentifier()
                        documentIdentifier.uri = t
                        changes.add(TextDocumentEdit(documentIdentifier, u))
                    }
                }
                val edit = WorkspaceEdit(changes.map { Either.forLeft(it) })
                edit
            }
        }
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        return computeAsync { checker ->
            workspace.ready {
                val file = workspace.findFile(params.textDocument.uri)
                val list = CompletionList()
                list.items = mutableListOf()
                if (file is ILuaFile) {
                    val psi = file.psi
                    val pos = file.getPosition(params.position.line, params.position.character)
                    val trigger = params.context.triggerCharacter
                    if (psi != null) {
                        if (trigger == "(") {
                            CompletionService.collectCompletion(psi, pos, Consumer {
                                checker.checkCanceled()
                                if (it is LuaLookupElement && it.isEnumMember) {
                                    list.items.add(it.asCompletionItem)
                                }
                            })
                        } else {
                            CompletionService.collectCompletion(psi, pos, Consumer {
                                checker.checkCanceled()
                                list.items.add(it.asCompletionItem)
                            })
                        }
                    }
                }
                Either.forRight<MutableList<CompletionItem>, CompletionList>(list)
            }
        }
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> {
        return computeAsync {
            workspace.ready {
                val file = workspace.findFile(params.textDocument.uri)
                val list = mutableListOf<Either<SymbolInformation, DocumentSymbol>>()
                if (file is ILuaFile) {
                    val psi = file.psi
                    if (psi is LuaPsiFile) {
                        val symbols = getDocumentSymbols(psi, file)
                        symbols.forEach { symbol -> list.add(Either.forRight(symbol)) }
                    }
                }
                list
            }
        }
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        workspace.canWrite {
            val uri = params.textDocument.uri
            val file = workspace.findFile(uri)
            if (file == null) {
                val u = URI(uri)
                workspace.addFile(File(u.path), params.textDocument.text, true)
            } else if (file is LuaFile) {
                file.text = params.textDocument.text
            }
            if (file is ILuaFile) {
                val diagnosticList = mutableListOf<Diagnostic>()
                DiagnosticsService.inspectFile(file, diagnosticList)
                this.client?.publishDiagnostics(PublishDiagnosticsParams(
                        uri, diagnosticList
                ))
            }
        }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        workspace.canWrite {
            val uri = params.textDocument.uri
            val file = workspace.findFile(uri)
            if (file is ILuaFile) {
                val diagnosticList = mutableListOf<Diagnostic>()
                DiagnosticsService.inspectFile(file, diagnosticList)
                this.client?.publishDiagnostics(
                        PublishDiagnosticsParams(
                                uri, diagnosticList
                        )
                )
            }
        }
    }

    override fun signatureHelp(params: SignatureHelpParams?): CompletableFuture<SignatureHelp?> {
        return computeAsync {
            workspace.ready {
                var signatureHelp: SignatureHelp? = null
                val list = mutableListOf<SignatureInformation>()
                var activeParameter = 0
                var activeSig = 0
                if (params != null) {
                    withPsiFile(params.textDocument, params.position) { _, psiFile, i ->
                        val callExpr = PsiTreeUtil.findElementOfClassAtOffset(psiFile, i, LuaCallExpr::class.java, false)
                        var nCommas = 0
                        callExpr?.args?.firstChild?.let { firstChild ->
                            var child: PsiElement? = firstChild
                            while (child != null) {
                                if (child.node.elementType == LuaTypes.COMMA) {
                                    activeParameter++
                                    nCommas++
                                }
                                child = child.nextSibling
                            }
                        }

                        callExpr?.guessParentType(SearchContext.get(psiFile.project))?.let { parentType ->
                            parentType.each { ty ->
                                var tyFunction: ITy? = ty
                                if (tyFunction is ITyClass) {
                                    val context = SearchContext.get(workspace.getProject())
                                    tyFunction = tyFunction.getClassCallType(context)
                                }

                                if (tyFunction is ITyFunction) {
                                    val active = tyFunction.findPerfectSignature(callExpr, nCommas + 1)
                                    var idx = 0
                                    tyFunction.process(Processor { sig ->
                                        val information = SignatureInformation()
                                        information.parameters = mutableListOf()
                                        sig.params.forEach { pi ->
                                            val pTy = pi.ty
                                            val pTyDisplay: String = if (pTy is TyStringLiteral) {
                                                "\"${pTy.displayName}\""
                                            } else {
                                                pTy.displayName
                                            }

                                            val paramInfo =
                                                    ParameterInformation("${pi.name}${if (pi.nullable) "?" else ""}:${pTyDisplay}")
                                            information.parameters.add(paramInfo)
                                        }

                                        if (sig.hasVarargs()) {
                                            val paramInfo =
                                                    ParameterInformation("...:${sig.varargTy?.displayName}")
                                            information.parameters.add(paramInfo)
                                        }
                                        if (sig.document != null) {
                                            information.documentation = Either.forRight(MarkupContent(MarkupKind.MARKDOWN, sig.document))
                                        }

                                        information.label = sig.displayName
                                        list.add(information)

                                        if (sig == active) {
                                            activeSig = idx
                                            if (sig.hasVarargs() && activeParameter > information.parameters.size - 1) {
                                                activeParameter = information.parameters.size - 1
                                            }
                                        }
                                        idx++
                                        true
                                    })
                                }
                            }
                        }
                    }
                    if (list.size > 0) {
                        signatureHelp = SignatureHelp(list, activeSig, activeParameter)
                    }
                }
                signatureHelp
            }
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        workspace.canWrite {
            workspace.removeFileIfNeeded(params.textDocument.uri)
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        workspace.canWrite {
            val file = workspace.findFile(params.textDocument.uri)
            if (file is ILuaFile) {
                file.didChange(params)
            }
        }
    }

    override fun references(params: ReferenceParams): CompletableFuture<MutableList<out Location>> {
        return computeAsync {
            workspace.ready {
                val list = mutableListOf<Location>()
                withPsiFile(params.textDocument, params.position) { _, psiFile, pos ->
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
                list
            }
        }
    }

    override fun inlayHint(params: InlayHintParams): CompletableFuture<MutableList<InlayHint>> {
        return computeAsync {
            workspace.ready {
                val file = workspace.findFile(params.textDocument.uri)
                var list: MutableList<InlayHint> = mutableListOf()
                if (file is LuaFile) {
                    // 认为所有local名称定义一开始都是未使用的
                    list = InlayHintService.getInlayHint(file)
                }
                list
            }
        }

    }

    override fun diagnostic(params: DocumentDiagnosticParams): CompletableFuture<DocumentDiagnosticReport> {
        return computeAsync {
            workspace.ready {
                val file = workspace.findFile(params.textDocument.uri)
                if (file is ILuaFile) {
                    workspace.diagnoseFile(file, params.previousResultId, null)
                } else {
                    DocumentDiagnosticReport(RelatedUnchangedDocumentDiagnosticReport())
                }
            }
        }
    }

    override fun resolveInlayHint(unresolved: InlayHint): CompletableFuture<InlayHint> {
        return computeAsync {
            workspace.ready {
                if (unresolved.data != null && unresolved.data is JsonPrimitive) {
                    val data = (unresolved.data as JsonPrimitive).asString
                    val texts = data.split("#")
                    val labelParts = mutableListOf<InlayHintLabelPart>()
                    if (texts.size == 2) {
                        val className = texts[0]
                        val fieldName = texts[1]
                        val context = SearchContext.get(workspace.getProject())
                        val resolveList = mutableListOf<ResolveResult>()
                        LuaClassMemberIndex.process(className, fieldName, context, Processor {
                            resolveList.add(PsiElementResolveResult(it))
                            false
                        })

                        val resolve = resolveList.firstOrNull()?.element
                        if (resolve != null) {
                            val sourceFile = resolve.containingFile?.virtualFile as? LuaFile
                            val range = resolve.nameRange
                            if (range != null && sourceFile != null) {
                                val labelPart = InlayHintLabelPart(unresolved.label.left)
                                labelPart.location = Location(sourceFile.uri.toString(), range.toRange(sourceFile))
                                labelParts.add(labelPart)
                            }
                        }

                    }

                    unresolved.label = Either.forRight(labelParts)
                }
                unresolved
            }
        }
    }

    private fun withPsiFile(
            textDocument: TextDocumentIdentifier,
            position: Position,
            code: (ILuaFile, LuaPsiFile, Int) -> Unit
    ) {
        val file = workspace.findFile(textDocument.uri)
        if (file is ILuaFile) {
            val psi = file.psi
            if (psi is LuaPsiFile) {
                val pos = file.getPosition(position.line, position.character)
                code(file, psi, pos)
            }
        }
    }
}
