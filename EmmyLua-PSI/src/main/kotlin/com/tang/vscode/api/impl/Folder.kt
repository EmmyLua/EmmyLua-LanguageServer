package com.tang.vscode.api.impl

import com.tang.vscode.api.IFolder
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.IVirtualFile
import java.net.URI

open class Folder(uri: URI)
    : VirtualFileBase(uri), IFolder {

    private val _children = mutableListOf<IVirtualFile>()

    override fun addFile(file: IVirtualFile) {
        val fb = file as VirtualFileBase
        fb.parent = this
        fb.workspace = this.workspace
        _children.add(file)
    }

    override fun removeFile(file: IVirtualFile) {

    }

    override fun findFile(uri: String): IVirtualFile? {
        var f: IVirtualFile? = null
        walkFiles {
            if (it.matchUri(uri)) {
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
        for (file in _children) {
            if (file is ILuaFile && !processor(file)) {
                return false
            } else if (file is IFolder && !file.walkFiles(processor)) {
                return false
            }
        }
        return true
    }
}