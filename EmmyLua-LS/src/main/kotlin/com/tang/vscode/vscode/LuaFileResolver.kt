package com.tang.vscode.vscode

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.tang.intellij.lua.ext.ILuaFileResolver
import com.tang.vscode.api.IFolder
import com.tang.vscode.api.impl.LuaFile
import com.tang.vscode.api.impl.Workspace

class LuaFileResolver : ILuaFileResolver {
    override fun find(project: Project, shortUrl: String, extNames: Array<String>): VirtualFile? {
        val ws = Workspace.get(project)
        val list = shortUrl.split('/')
        var folder: IFolder = ws
        var result: VirtualFile? = null
        result = ws.getFile(shortUrl) as? LuaFile
        /*for (index in 0 until list.size) {
            val s = list[index]
            if (index == list.lastIndex) {
                for (extName in extNames) {
                    val file = folder.getFile("$s$extName")
                    if (file is LuaFile) {
                        result = file
                    }
                }
            } else {
                val child = folder.getFile(s)
                if (child is IFolder)
                    folder = child
                else break
            }
        }*/

        return result
    }
}