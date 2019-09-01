package com.tang.intellij.lua.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.tang.intellij.lua.IVSCodeSettings
import com.tang.intellij.lua.ext.ILuaFileResolver

object LuaFileUtil {

    fun findFile(project: Project, shortUrl: String?): VirtualFile? {
        val settings = IVSCodeSettings.get(project)
        val extensions = settings.fileExtensions.toTypedArray()
        return if (shortUrl == null) null else ILuaFileResolver.findLuaFile(project, shortUrl, extensions)
    }
}