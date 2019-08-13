package com.tang.vscode.configuration

import com.intellij.util.ThreeState
import com.tang.intellij.lua.configuration.IConfigurationManager
import com.tang.intellij.lua.configuration.IConfigurationStructure
import com.tang.intellij.lua.configuration.ISourceRoot
import com.tang.lsp.FileURI
import com.tang.vscode.EmmyConfigurationSource
import com.tang.vscode.UpdateConfigParams
import com.tang.vscode.UpdateType
import java.io.File

class ConfigurationManager : IConfigurationManager {
    fun updateConfiguration(params: UpdateConfigParams) {
        val configuration = findConfiguration(params.source)
        when (params.type) {
            UpdateType.Created -> {
                if (configuration != null)
                    configuration.reload()
                else
                    add(params.source)
            }
            UpdateType.Changed -> configuration?.reload()
            UpdateType.Deleted -> configuration?.let { remove(it) }
        }
    }

    private fun findConfiguration(source: EmmyConfigurationSource): ConfigurationStructure? {
        return list.find { it.source == source }
    }

    private val list = mutableListOf<ConfigurationStructure>()

    private val main: ConfigurationStructure? get() {
        return list.firstOrNull()
    }

    fun init(configurationSourceList: Array<EmmyConfigurationSource>) {
        configurationSourceList.forEach {
            list.add(ConfigurationStructure(it))
        }
    }

    private fun add(source: EmmyConfigurationSource) {
        list.add(ConfigurationStructure(source))
    }

    private fun remove(configuration: ConfigurationStructure) {
        list.remove(configuration)
    }

    override val sourceRoots: Array<ISourceRoot> get() {
        val sources = mutableListOf<ISourceRoot>()
        list.forEach { sources.addAll(it.sourceRoots) }
        return sources.toTypedArray()
    }

    override val completionCaseSensitive: Boolean
        get() = main?.editor?.completionCaseSensitive ?: false

    override fun isInclude(file: File): ThreeState {
        return isInclude(FileURI(file.toURI(), false))
    }

    override fun isInclude(uri: FileURI): ThreeState {
        for (structure in list) {
            for (root in structure.sourceRoots) {
                if (root.isInclude(uri) == ThreeState.YES) {
                    return ThreeState.YES
                }
            }
        }
        return ThreeState.UNSURE
    }

    override fun isExclude(uri: FileURI): ThreeState {
        for (structure in list) {
            for (root in structure.sourceRoots) {
                if (root.isExclude(uri) == ThreeState.YES) {
                    return ThreeState.YES
                }
            }
        }
        return ThreeState.UNSURE
    }

    override fun getConfigurationFor(root: FileURI): IConfigurationStructure? {
        for (structure in list) {
            if (root.contains(structure.source.fileURI)) {
                return structure
            }
        }
        return null
    }
}
