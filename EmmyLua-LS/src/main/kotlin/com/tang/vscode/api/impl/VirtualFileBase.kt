package com.tang.vscode.api.impl

import com.tang.lsp.FileURI
import com.tang.lsp.IFolder
import com.tang.lsp.IVirtualFile
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
        return uri == this.uri.raw
    }
}