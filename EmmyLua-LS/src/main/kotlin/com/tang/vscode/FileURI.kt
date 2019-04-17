package com.tang.vscode

import java.net.URI
import java.net.URLEncoder

private fun convertURI(s: String): String {
    return s.replace("+", " ")
}

class FileURI {

    constructor(uriString: String, isFolder: Boolean): this(URI(uriString), isFolder)

    constructor(uri: URI, isFolder: Boolean) {
        val str = uri.toString()
        _uri = if (isFolder && !str.endsWith('/')) URI("$str/") else uri
        _scheme = _uri.scheme
        _uriString = _uri.toString()
        _isFolder = isFolder
    }

    fun resolve(other: String, isFolder: Boolean): FileURI {
        if (!_isFolder)
            throw Error("must be folder!")
        val encoded = URLEncoder.encode(other, "UTF-8")
        return FileURI(_uri.resolve(encoded), isFolder)
    }

    private val _isFolder: Boolean

    private val _scheme: String

    private val _uri: URI

    private val _uriString: String

    private val _nameParts: List<String> by lazy {
        val path = raw.path ?: raw.rawSchemeSpecificPart
        val start = if (path.startsWith('/')) 1 else 0
        val end = if (path.endsWith('/')) path.length - 1 else path.length
        val substring = path.substring(start, end)
        substring.split('/').map { convertURI(it) }
    }

    val scheme: String get() = _scheme

    val name: String get() = _nameParts.last()

    val parent: FileURI? get() {
        val uri = if (_isFolder) _uri.resolve("..") else _uri.resolve("")
        if (uri.scheme == null)
            return null
        return FileURI(uri, true)
    }

    val root: FileURI get() = FileURI(URI("$scheme:/${_nameParts.first()}"), true)

    val nameCount: Int get() = _nameParts.size

    val raw: URI get() = _uri

    fun getName(i: Int): String {
        return _nameParts[i]
    }

    fun endsWith(name: String): Boolean {
        return this.name.endsWith(name)
    }

    override fun toString(): String {
        val s = _uri.toString()
        return convertURI(s)
    }

    override fun equals(other: Any?): Boolean {
        if (other is FileURI) {
            return other._uriString == this._uriString
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return _uriString.hashCode()
    }
}

fun main(args: Array<String>) {
    val u = FileURI("a:/b", true)
    val resolve = u.resolve("c", false)
    println(resolve.name)
}