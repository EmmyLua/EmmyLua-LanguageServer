package com.tang.vscode

interface IProgressMonitor {
    fun setProgress(text: String, percent: Float)
    fun done()
}