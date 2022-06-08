package com.tang.intellij.lua.editor.completion

import com.intellij.openapi.util.TextRange
import com.tang.intellij.lua.fs.IFileManager
import com.tang.intellij.lua.lang.type.LuaString
import com.tang.lsp.ILuaFile
import com.tang.lsp.toRange
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.TextEdit

class RequirePathCompletionProvider : LuaCompletionProvider() {
    override fun addCompletions(session: CompletionSession) {
        val project = session.parameters.originalFile.project
        val manager = IFileManager.get(project)
        val sources = manager.getSourceRoots(project)
        val oriFile = session.parameters.originalFile.virtualFile as ILuaFile
        val oriPos = session.parameters.originalFile.findElementAt(session.parameters.offset)
        if (sources.isNotEmpty() && oriPos != null) {
            val content = LuaString.getContent(oriPos.text)
            val range = oriPos.textRange
            val contentRange = TextRange(range.startOffset + content.start, range.startOffset + content.start + content.length)
            val toRange = contentRange.toRange(oriFile)

            project.process { file->
                val path = file.virtualFile.path
                for (root in sources) {
                    val relative = root.relative(path)
                    if (relative != null) {
                        val lastDot = relative.lastIndexOf('.')
                        val requirePath = if (lastDot == -1) relative else relative.substring(0, lastDot)
                        val paths = requirePath.split(Regex("[/\\\\]"))
                        val insert = paths.joinToString(".")
                        val element = LuaLookupElement(insert)
                        element.itemText = path
                        element.kind = CompletionItemKind.File
                        element.textEdit = TextEdit(toRange, insert)
                        session.resultSet.addElement(element)
                    }
                }
                true
            }
        }
    }
}