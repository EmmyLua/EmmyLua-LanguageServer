package com.tang.vscode.vscode

interface IProgressMonitor {
    fun setProgress(text: String, percent: Float)
    fun done()
}