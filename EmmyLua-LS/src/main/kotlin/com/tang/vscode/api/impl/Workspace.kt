package com.tang.vscode.api.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.util.Processor
import com.tang.vscode.api.IFolder
import com.tang.vscode.api.IWorkspace
import java.net.URI

class Workspace(uri: URI) : Folder(uri), IWorkspace {

    inner class WProject : UserDataHolderBase(), Project {
        override fun process(processor: Processor<PsiFile>) {
            walkFiles {
                val psi = it.psi
                if (psi != null)
                    return@walkFiles processor.process(psi)
                true
            }
        }
    }

    override val project: Project = WProject()

    override var parent: IFolder
        get() = this
        set(value) {}

    override var workspace: IWorkspace
        get() = this
        set(value) {}
}