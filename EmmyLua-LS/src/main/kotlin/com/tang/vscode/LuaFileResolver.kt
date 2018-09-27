package com.tang.vscode

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.tang.intellij.lua.ext.ILuaFileResolver
import com.tang.vscode.api.impl.LuaFile

class LuaFileResolver : ILuaFileResolver {
    override fun find(project: Project, shortUrl: String, extNames: Array<String>): VirtualFile? {
        val arr = shortUrl.split("/")
        for (extName in extNames) {
            val fileName = arr.last() + extName

            val file = findFile(project, fileName, arr)

            if (file != null)
                return file
        }
        return null
    }

    private fun findFile(project: Project, fileName: String, arr: List<String>): VirtualFile? {
        var result: LuaFile? = null
        project.process {
            val virtualFile = it.virtualFile
            if (virtualFile is LuaFile) {
                if (virtualFile.name == fileName) {
                    result = virtualFile

                    var cur = result?.parent
                    for (i in arr.size - 2 downTo 0) {
                        val name = arr[i]
                        if (cur?.getName() != name) {
                            result = null
                            break
                        }
                        cur = cur.parent
                    }

                    if (result != null)
                        return@process false
                }
            }
            true
        }
        return result
    }
}