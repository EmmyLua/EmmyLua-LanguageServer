package com.tang.vscode.api.impl

import com.tang.vscode.api.IFolder
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.IVirtualFile
import java.net.URI
import java.net.URLDecoder

open class Folder(uri: URI)
    : VirtualFileBase(uri), IFolder {

    private val children = mutableListOf<IVirtualFile>()

    override fun addFile(file: IVirtualFile) {
        val fb = file as VirtualFileBase
        fb.parent = this
        children.add(file)
    }

    override fun removeFile(file: IVirtualFile) {
        TODO()
    }

    override fun findFile(uri: String): IVirtualFile? {
        val formattedUri = URI(URLDecoder.decode(uri, "UTF-8"))
        var f: IVirtualFile? = null
        walkFiles {
            if (it.matchUri(formattedUri)) {
                f = it
                return@walkFiles false
            }
            true
        }
        return f
    }

    override fun getFile(name: String, recursive: Boolean): IVirtualFile? {
        var f: IVirtualFile? = null
        walkFiles {
            if (it.uri.path.indexOf(name) != -1) {
                f = it
                return@walkFiles false
            }
            true
        }
        return f
    }

    override val isFolder: Boolean
        get() = true

    override fun addFile(uri: String, text: String): ILuaFile {
        val luaFile = LuaFile(URI(uri))
        luaFile.text = text
        addFile(luaFile)
        return luaFile
    }

    override fun walkFiles(processor: (f: ILuaFile) -> Boolean): Boolean {
        for (file in children) {
            if (file is ILuaFile && !processor(file)) {
                return false
            } else if (file is IFolder && !file.walkFiles(processor)) {
                return false
            }
        }
        return true
    }
}