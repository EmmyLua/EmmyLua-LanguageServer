package com.tang.vscode.api.impl

import com.tang.vscode.api.IFolder
import com.tang.vscode.api.IVirtualFile
import com.tang.vscode.api.IWorkspace
import java.io.File
import java.net.URI

abstract class VirtualFileBase(override val uri: URI) : IVirtualFile {
    private var ws: IWorkspace? = null
    private var parentFolder: IFolder? = null
    private val file by lazy { File(uri.path) }

    override val isFolder: Boolean
        get() = false

    override var workspace: IWorkspace
        get() = ws!!
        set(value) { ws = value }

    override var parent: IFolder
        get() = parentFolder!!
        set(value) { parentFolder = value }

    override val name: String
        get() = file.name

    override fun matchUri(uri: String): Boolean {
        return uri == this.uri.toString()
    }
}