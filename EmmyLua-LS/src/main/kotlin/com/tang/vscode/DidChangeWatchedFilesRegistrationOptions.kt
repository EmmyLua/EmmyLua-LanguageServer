package com.tang.vscode

data class DidChangeWatchedFilesRegistrationOptions(val watchers: List<FileSystemWatcher>)

data class FileSystemWatcher(val globPattern: String)
