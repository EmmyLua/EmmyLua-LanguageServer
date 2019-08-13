package com.tang.lsp

import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

private fun decodeURL(s: String): String {
    //return s.replace("+", " ").replace("%3A", ":")
    return URLDecoder.decode(s, "UTF-8")
}

private fun encodeURL(src: String): String {
    return src.split(Regex("[/\\\\]")).joinToString("/") { URLEncoder.encode(it, "UTF-8") }
}

private fun toURI(uri: String): URI {
    val u2 = decodeURL(uri)
    val u3 = encodeURL(u2).replace("%3A", ":")
    return URI(u3)
}

class FileURI {

    constructor(uriString: String, isFolder: Boolean): this(toURI(uriString), isFolder)

    constructor(uri: URI, isFolder: Boolean) {
        val normalize = uri.normalize()
        val str = normalize.toString()
        _uri = if (isFolder && !str.endsWith('/')) toURI("$str/") else toURI(str)
        _scheme = _uri.scheme ?: "file"
        _uriString = _uri.toString().toLowerCase()
        _isFolder = isFolder
    }

    companion object {
        fun file(file: File): FileURI {
            return FileURI(file.toURI(), file.isDirectory)
        }

        fun uri(src: String, isFolder: Boolean): FileURI {
            return FileURI(src, isFolder)
        }
    }

    fun resolve(other: String, isFolder: Boolean): FileURI {
        if (!_isFolder)
            throw Error("must be folder!")
        val encoded = encodeURL(other)
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
        substring.split('/').map { decodeURL(it) }
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

    fun toFile(): File? {
        if (scheme != "file") {
            return null
        }
        val path = raw.path
        return File(decodeURL(path))
    }

    fun relativize(other: FileURI): FileURI? {
        val relative = FileURI(raw.relativize(other.raw), other._isFolder)
        return if (relative == other) null else relative
    }

    fun contains(other: FileURI): Boolean {
        val r = relativize(other)
        return !(r == null || r == other)
    }

    override fun toString(): String {
        val s = _uri.toString()
        return decodeURL(s)
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