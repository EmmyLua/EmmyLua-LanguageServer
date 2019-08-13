package com.tang.intellij.lua.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.ThreeState
import com.tang.lsp.FileURI
import java.io.File

interface IConfigurationStructure {
    val sourceRoots: List<ISourceRoot>
}

interface ISourceRoot {
    val absoluteDir: FileURI?

    fun relative(path: String): String?

    fun isInclude(uri: FileURI): ThreeState

    fun isExclude(uri: FileURI): ThreeState
}

interface IConfigurationManager {
    companion object {
        val KEY = Key.create<IConfigurationManager>(IConfigurationManager::class.java.name)

        fun get(project: Project): IConfigurationManager {
            return KEY.get(project)
        }
    }

    val sourceRoots: Array<ISourceRoot>

    val completionCaseSensitive: Boolean

    fun isInclude(file: File): ThreeState

    fun isInclude(uri: FileURI): ThreeState

    fun isExclude(uri: FileURI): ThreeState

    fun getConfigurationFor(root: FileURI): IConfigurationStructure?
}