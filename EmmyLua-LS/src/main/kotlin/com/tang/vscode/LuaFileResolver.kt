package com.tang.vscode

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.tang.intellij.lua.ext.ILuaFileResolver
import com.tang.vscode.api.IWorkspace
import com.tang.vscode.api.impl.LuaFile

class LuaFileResolver : ILuaFileResolver {
    override fun find(project: Project, shortUrl: String, extNames: Array<String>): VirtualFile? {
        val ws = IWorkspace.get(project)
        val arr = shortUrl.split("/")
        for (extName in extNames) {
            var file: VirtualFile? = null
            val varargs = arr.toTypedArray()
            varargs[varargs.lastIndex] = varargs.last() + extName

            ws.eachRoot { root ->
                file = root.findFile(*varargs) as? LuaFile

                file == null
            }
            if (file != null)
                return file
        }
        return null
    }
}