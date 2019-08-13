package com.tang.vscode.api.impl

import com.tang.lsp.FileURI
import com.tang.lsp.IFolder
import com.tang.lsp.ILuaFile
import com.tang.lsp.IVirtualFile

open class Folder(fileURI: FileURI, private val myName: String? = null)
    : VirtualFileBase(fileURI), IFolder {

    private val children = mutableListOf<IVirtualFile>()

    override fun getName(): String {
        return myName ?: super.getName()
    }

    override fun addFile(file: IVirtualFile) {
        val old = findFile(file.getName())
        if (old == file)
            return
        if (old != null)
            removeFile(old)

        val fb = file as VirtualFileBase
        fb.parent = this
        children.add(file)
    }

    override fun removeFile(file: IVirtualFile) {
        children.remove(file)
        if (file is ILuaFile) {
            file.unindex()
        } else if (file is IFolder) {
            file.removeAll()
        }
    }

    override fun removeAll() {
        walkFiles {
            removeFile(it)
            true
        }
        children.clear()
    }

    override fun findFile(name: String): IVirtualFile? {
        return children.find { it.getName().toLowerCase() == name.toLowerCase() }
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
            if (it.uri.endsWith(name)) {
                f = it
                return@walkFiles false
            }
            true
        }
        return f
    }

    override val isFolder: Boolean
        get() = true

    override fun addFile(name: String, text: CharSequence): ILuaFile {
        val luaFile = LuaFile(uri.resolve(name, false))
        luaFile.text = text
        addFile(luaFile)
        return luaFile
    }

    override fun walkFiles(processor: (f: ILuaFile) -> Boolean): Boolean {
        for (i in children.size - 1 downTo 0) {
            val file = children[i]

            if (file is ILuaFile && !processor(file)) {
                return false
            } else if (file is IFolder && !file.walkFiles(processor)) {
                return false
            }
        }
        return true
    }

    override fun createFolder(name: String): IFolder {
        val folder = Folder(uri.resolve(name, true))
        addFile(folder)
        return folder
    }
}