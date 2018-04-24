package com.tang.vscode.api.impl

import com.tang.vscode.api.IFolder
import com.tang.vscode.api.IVirtualFile
import java.io.File
import java.net.URI

abstract class VirtualFileBase(override val uri: URI) : IVirtualFile {
    private var parentFolder: IFolder? = null
    private val file by lazy { File(uri.path) }

    override val isFolder: Boolean
        get() = false

    override var parent: IFolder
        get() = parentFolder!!
        set(value) { parentFolder = value }

    override fun getName(): String {
        return file.name
    }

    override fun matchUri(uri: URI): Boolean {
        return uri == this.uri
    }
}