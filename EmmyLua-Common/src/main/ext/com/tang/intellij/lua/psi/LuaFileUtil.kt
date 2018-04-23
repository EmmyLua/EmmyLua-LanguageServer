package com.tang.intellij.lua.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.tang.intellij.lua.ext.ILuaFileResolver

object LuaFileUtil {

    //有些扩展名也许是txt
    private val extensions = arrayOf(".lua.txt", ".lua", ".txt", "")

    fun findFile(project: Project, shortUrl: String?): VirtualFile? {
        return if (shortUrl == null) null else ILuaFileResolver.findLuaFile(project, shortUrl, extensions)
    }
}