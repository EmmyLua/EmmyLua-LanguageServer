package com.tang.vscode.api.impl

import com.tang.vscode.api.IFolder
import java.net.URI

class WorkspaceRoot(uri: URI) : Folder(uri) {

    override var parent: IFolder
        get() = this
        set(value) {}
}