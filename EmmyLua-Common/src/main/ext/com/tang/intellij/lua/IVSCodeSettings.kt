package com.tang.intellij.lua

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

interface IVSCodeSettings {
    companion object {
        val KEY = Key.create<IVSCodeSettings>(IVSCodeSettings::class.java.name)
        
        fun get(project: Project): IVSCodeSettings {
            return KEY.get(project)
        }
    }
    val isVSCode: Boolean
    val showCodeLens: Boolean
    val completionCaseSensitive: Boolean
    val sourceRoots: List<String>
    val fileExtensions: List<String>

    fun matchFile(name: String): Boolean
}