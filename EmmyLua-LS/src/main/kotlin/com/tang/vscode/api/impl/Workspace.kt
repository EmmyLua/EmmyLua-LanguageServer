package com.tang.vscode.api.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
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

    override val project: Project

    init {
        project = WProject()
        project.putUserData(KEY, this)
    }

    override var parent: IFolder
        get() = this
        set(value) {}

    override var workspace: IWorkspace
        get() = this
        set(value) {}

    companion object {
        private val KEY = Key.create<IWorkspace>("emmy.workspace")

        fun get(project: Project): IWorkspace {
            return project.getUserData(KEY)!!
        }
    }

    /*override fun addFile(file: IVirtualFile) {
        val path = uri.path
        val filePath = file.uri.path
        val relative = filePath.substring(path.length)
        val nameList = relative.split('/')
        var folder: IFolder = this
        nameList.forEachIndexed { index, s ->
            if (index == nameList.lastIndex) {
                if (folder == this)
                    super.addFile(file)
                else
                    folder.addFile(file)
            } else {
                val f = Folder(URI.create(folder.toString() + "/$s"))
                folder.addFile(f)
                folder = f
            }
        }
    }*/
}