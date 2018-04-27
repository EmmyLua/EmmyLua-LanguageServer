package com.tang.vscode

data class DidChangeWatchedFilesRegistrationOptions(val list: List<FileSystemWatcher>)

data class FileSystemWatcher(val patter: String)