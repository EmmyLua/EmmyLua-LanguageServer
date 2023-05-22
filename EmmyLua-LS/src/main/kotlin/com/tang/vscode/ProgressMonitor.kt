package com.tang.vscode

interface IProgressMonitor {
    fun start()
    fun setProgress(text: String, percent: Float)
    fun reportError(text: String)
    fun done()
}