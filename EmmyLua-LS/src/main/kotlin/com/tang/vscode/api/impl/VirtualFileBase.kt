package com.tang.vscode.api.impl

import com.tang.vscode.FileURI
import com.tang.vscode.api.IFolder
import com.tang.vscode.api.IVirtualFile
import java.net.URI

abstract class VirtualFileBase(override val uri: FileURI) : IVirtualFile {
    private var parentFolder: IFolder? = null

    override val isFolder: Boolean
        get() = false

    override var parent: IFolder
        get() = parentFolder!!
        set(value) { parentFolder = value }

    override fun getName(): String {
        return uri.name
    }

    override fun matchUri(uri: URI): Boolean {
        return uri == this.path.toUri()
    }
}