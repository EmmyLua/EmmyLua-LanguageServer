package com.tang.intellij.lua.configuration

class Pattern(pattern: String, private val ignoreCase: Boolean) {
    private var value: String? = null
    private val values: Array<String>

    private var index: Int = 0

    private val isExhausted: Boolean
        get() = index >= values.size

    private val isLast: Boolean
        get() = index >= values.size - 1

    init {
        var pattern = pattern

        pattern = pattern.replace('\\', '/')
        pattern = pattern.replace("\\*\\*[^/]".toRegex(), "**/*")
        pattern = pattern.replace("[^/]\\*\\*".toRegex(), "*/**")
        if (ignoreCase) pattern = pattern.toLowerCase()

        values = pattern.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        value = values[0]
    }

    fun matches(fileName: String): Boolean {
        var fileName = fileName
        val value = this.value
        if (value == "**") return true

        if (ignoreCase) fileName = fileName.toLowerCase()

        // Shortcut if no wildcards.
        if (value!!.indexOf('*') == -1 && value.indexOf('?') == -1) return fileName == value

        var i = 0
        var j = 0
        val fileNameLength = fileName.length
        val valueLength = value.length
        while (i < fileNameLength && j < valueLength) {
            val c = value[j]
            if (c == '*') break
            if (c != '?' && c != fileName[i]) return false
            i++
            j++
        }

        // If reached end of pattern without finding a * wildcard, the match has to fail if not same length.
        if (j == valueLength) return fileNameLength == valueLength

        var cp = 0
        var mp = 0
        while (i < fileNameLength) {
            if (j < valueLength) {
                val c = value[j]
                if (c == '*') {
                    if (j++ >= valueLength) return true
                    mp = j
                    cp = i + 1
                    continue
                }
                if (c == '?' || c == fileName[i]) {
                    j++
                    i++
                    continue
                }
            }
            j = mp
            i = cp++
        }

        // Handle trailing asterisks.
        while (j < valueLength && value[j] == '*')
            j++

        return j >= valueLength
    }

    private fun nextValue(): String? {
        return if (index + 1 == values.size) null else values[index + 1]
    }

    fun incr(fileName: String): Boolean {
        if (value == "**") {
            if (index == values.size - 1) return false
            incr()
            if (matches(fileName))
                incr()
            else {
                decr()
                return false
            }
        } else
            incr()
        return true
    }

    private fun incr() {
        index++
        if (index >= values.size)
            value = null
        else
            value = values[index]
    }

    private fun decr() {
        index--
        if (index > 0 && values[index - 1] == "**") index--
        value = values[index]
    }

    private fun reset() {
        index = 0
        value = values[0]
    }

    fun wasFinalMatch(): Boolean {
        return isExhausted || isLast && value == "**"
    }

    companion object {
        fun match(pattern: String, value: String): Boolean {
            val p = Pattern(pattern, false)
            val arr = value.split("/")
            for (s in arr) {
                if (p.matches(s)) {
                    p.incr(s)
                    continue
                }
                break
            }
            return p.wasFinalMatch()
        }
    }
}