package com.tang.vscode

import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

class FileURI {
    constructor(uriString: String): this(URI(uriString))

    constructor(uri: URI) {
        var u = uri
        _scheme = u.scheme
        if (_scheme != "file" && u.path == null)
            u = URI("file:/${u.rawSchemeSpecificPart}")
        _path = Paths.get(u)
    }

    constructor(scheme: String, path: Path) {
        _scheme = scheme
        _path = path
    }

    fun resolve(other: String): FileURI {
        return FileURI(scheme, path.resolve(other))
    }

    private val _path: Path

    private val _scheme: String

    val path: Path get() = _path

    val scheme: String get() = _scheme

    val name: String get() = _path.fileName.toString()

    val parent: FileURI get() = FileURI(scheme, path.parent)

    override fun toString(): String {
        return _path.toUri().toString()
    }
}

fun main(args: Array<String>) {
    val u = FileURI("a:b")
    println(u)
}