package com.tang.vscode.api.impl

import com.tang.vscode.api.IFolder
import com.tang.vscode.api.ILuaFile
import com.tang.vscode.api.IVirtualFile
import java.net.URI

open class Folder(uri: URI, private val myName: String? = null)
    : VirtualFileBase(uri), IFolder {

    private val children = mutableListOf<IVirtualFile>()

    override fun getName(): String {
        return myName ?: super.getName()
    }

    override fun addFile(file: IVirtualFile) {
        val fb = file as VirtualFileBase
        fb.parent = this
        children.add(file)
    }

    override fun removeFile(file: IVirtualFile) {
        TODO()
    }

    override fun findFile(name: String): IVirtualFile? {
        return children.find { it.getName() == name }
    }

    override fun findFile(vararg names: String): IVirtualFile? {
        var folder: IFolder = this
        for (i in 0 until names.size) {
            val name = names[i]
            val file = folder.findFile(name)
            if (file is IFolder)
                folder = file
            else {
                if (i == names.lastIndex)
                    return file
                else break
            }
        }
        return null
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

    override fun addFile(name: String, text: String): ILuaFile {
        val luaFile = LuaFile(this.uri.resolve(name))
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

    override fun createFolder(name: String): IFolder {
        val u = uri.resolve("$name/")
        val folder = Folder(u)
        addFile(folder)
        return folder
    }
}