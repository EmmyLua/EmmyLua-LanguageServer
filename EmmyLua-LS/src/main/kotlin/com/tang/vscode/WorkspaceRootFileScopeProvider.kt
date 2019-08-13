package com.tang.vscode

import com.intellij.openapi.project.Project
import com.intellij.util.ThreeState
import com.tang.intellij.lua.configuration.IConfigurationManager
import com.tang.intellij.lua.configuration.ISourceRoot
import com.tang.intellij.lua.fs.FileCollection
import com.tang.intellij.lua.fs.FileManager
import com.tang.intellij.lua.fs.IFileCollection
import com.tang.intellij.lua.fs.IFileScopeProvider
import com.tang.lsp.FileURI

class WorkspaceRootFileScopeProvider : IFileScopeProvider {

    private val roots = mutableSetOf<WorkspaceRoot>()

    fun addRoot(uri: FileURI) {
        roots.add(WorkspaceRoot(uri))
    }

    fun removeRoot(uri: FileURI) {
        roots.removeIf { it.absoluteDir == uri }
    }

    override fun isExclude(uri: FileURI): ThreeState {
        if (!VSCodeSettings.matchFile(uri.name)) {
            return ThreeState.YES
        }
        return ThreeState.UNSURE
    }

    override fun isInclude(uri: FileURI): ThreeState {
        if (!VSCodeSettings.matchFile(uri.name)) {
            return ThreeState.NO
        }
        for (root in roots) {
            if (root.isInclude(uri) === ThreeState.YES) {
                return ThreeState.YES
            }
        }
        return ThreeState.UNSURE
    }

    override fun findAllFiles(manager: FileManager): List<IFileCollection> {
        val collections = mutableListOf<IFileCollection>()
        for (root in roots) {
            root.absoluteDir.toFile()?.let { rootFile ->
                val files = mutableListOf<FileURI>()
                manager.collectFiles(rootFile, files)
                collections.add(FileCollection(FileURI.file(rootFile), files))
            }
        }
        return collections
    }

    override fun getSourceRoots(project: Project): Array<ISourceRoot> {
        val cm = IConfigurationManager.get(project)
        val list = mutableListOf<ISourceRoot>()
        for (root in roots) {
            val cfg = cm.getConfigurationFor(root.absoluteDir)
            if (cfg == null || cfg.sourceRoots.isEmpty())
                list.add(root)
        }
        return list.toTypedArray()
    }
}

class WorkspaceRoot(
        override val absoluteDir: FileURI
) : ISourceRoot {
    override fun relative(path: String): String? {
        val f = FileURI(path, false)
        val r = absoluteDir.relativize(f)
        if (r == f) {
            return null
        }
        return r?.toString()
    }

    override fun isInclude(uri: FileURI): ThreeState {
        val r = absoluteDir.relativize(uri)
        return if (r == null || r == uri) ThreeState.UNSURE else ThreeState.YES
    }

    override fun isExclude(uri: FileURI): ThreeState {
        TODO("not implemented")
    }
}