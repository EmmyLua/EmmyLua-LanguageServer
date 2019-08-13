package com.tang.intellij.lua.fs

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.ThreeState
import com.tang.intellij.lua.configuration.IConfigurationManager
import com.tang.intellij.lua.configuration.ISourceRoot
import com.tang.lsp.FileURI
import java.io.File

interface IFileCollection {
    val root: FileURI
    val files: List<FileURI>
}

interface IFileScopeProvider {
    fun isInclude(uri: FileURI): ThreeState
    fun isExclude(uri: FileURI): ThreeState
    fun findAllFiles(manager: FileManager): List<IFileCollection>
    fun getSourceRoots(project: Project): Array<ISourceRoot>
}

class FileCollection(
        override val root: FileURI,
        override val files: List<FileURI>
) : IFileCollection

interface IFileManager {
    fun addProvider(provider: IFileScopeProvider)
    fun isInclude(file: File): Boolean
    fun isInclude(uri: FileURI): Boolean
    fun isExclude(uri: FileURI): Boolean
    fun findAllFiles(): List<IFileCollection>
    fun getSourceRoots(project: Project): Array<ISourceRoot>
    companion object {
        val KEY = Key.create<IFileManager>(IFileManager::class.java.name)
        fun get(project: Project): IFileManager {
            return KEY.get(project)
        }
    }
}

class FileManager(project: Project) : IFileManager {
    private val providers = mutableListOf<IFileScopeProvider>()

    init {
        addProvider(ConfigurationFileScopeProvider(project))
    }

    override fun addProvider(provider: IFileScopeProvider) {
        providers.add(provider)
    }

    override fun isInclude(file: File): Boolean {
        return isInclude(FileURI.file(file))
    }

    override fun isInclude(uri: FileURI): Boolean {
        return providers.any { it.isInclude(uri) == ThreeState.YES } && !isExclude(uri)
    }

    override fun isExclude(uri: FileURI): Boolean {
        return providers.any { it.isExclude(uri) == ThreeState.YES }
    }

    override fun findAllFiles(): List<IFileCollection> {
        val list = mutableListOf<IFileCollection>()
        for (provider in providers) {
            val collections = provider.findAllFiles(this)
            list.addAll(collections)
        }
        return list
    }

    override fun getSourceRoots(project: Project): Array<ISourceRoot> {
        val list = mutableListOf<ISourceRoot>()
        providers.forEach { list.addAll(it.getSourceRoots(project)) }
        return list.toTypedArray()
    }

    fun collectFiles(file: File, list: MutableList<FileURI>) {
        if (file.isFile && isInclude(file)) {
            list.add(FileURI.file(file))
        } else if (file.isDirectory) {
            file.listFiles()?.forEach { collectFiles(it, list) }
        }
    }
}

class ConfigurationFileScopeProvider(private val project: Project) : IFileScopeProvider {
    override fun isInclude(uri: FileURI): ThreeState {
        val configurationManager = IConfigurationManager.get(project)
        return configurationManager.isInclude(uri)
    }

    override fun isExclude(uri: FileURI): ThreeState {
        val configurationManager = IConfigurationManager.get(project)
        return configurationManager.isExclude(uri)
    }

    override fun findAllFiles(manager: FileManager): List<IFileCollection> {
        val configurationManager = IConfigurationManager.get(project)
        val collections = mutableListOf<IFileCollection>()
        for (source in configurationManager.sourceRoots) {
            val dir = source.absoluteDir ?: continue
            dir.toFile()?.let { file ->
                val files = mutableListOf<FileURI>()
                manager.collectFiles(file, files)
                collections.add(FileCollection(dir, files))
            }
        }
        return collections
    }

    override fun getSourceRoots(project: Project): Array<ISourceRoot> {
        return IConfigurationManager.get(project).sourceRoots
    }
}
