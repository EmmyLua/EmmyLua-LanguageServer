package com.tang.vscode.vscode

data class DidChangeWatchedFilesRegistrationOptions(val list: List<FileSystemWatcher>)

data class FileSystemWatcher(val patter: String)