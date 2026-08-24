package com.evgeniich.flashread.core.importdoc

/**
 * Turns HTML/XHTML into the same paragraph-per-line text that
 * [com.evgeniich.flashread.core.speedread.splitBookParagraphs] expects.
 */
object HtmlToPlainText {
    fun convert(html: String): String {
        val paragraphs = ArrayList<String>()
        val current = StringBuilder()

        fun flush() {
            val text = collapseHtmlWhitespace(current).trim()
            current.setLength(0)
            if (text.isNotEmpty()) paragraphs.add(text)
        }

        var i = 0
        while (i < html.length) {
            val c = html[i]
            if (c == '<' && isMarkupStart(html, i)) {
                val markup = readMarkup(html, i)
                i = markup.end
                when (markup) {
                    is Markup.Comment, is Markup.Declaration -> Unit
                    is Markup.CData -> current.append(markup.content)
                    is Markup.Tag -> {
                        if (markup.name in SKIP_TAGS) {
                            if (!markup.isClose && !markup.isSelfClosing) {
                                i = skipUntilClosingTag(html, i, markup.name)
                            }
                        } else if (markup.name in BLOCK_TAGS) {
                            flush()
                        }
                    }
                }
            } else if (c == '&') {
                val decoded = readEntity(html, i)
                current.append(decoded.value)
                i = decoded.end
            } else {
                current.append(c)
                i++
            }
        }
        flush()
        return paragraphs.joinToString("\n")
    }
}

private val BLOCK_TAGS = setOf("p", "div", "br", "h1", "h2", "h3", "h4", "h5", "h6")
private val SKIP_TAGS = setOf("script", "style", "head")

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to "\u00A0",
    "ensp" to "\u2002",
    "emsp" to "\u2003",
    "thinsp" to "\u2009",
    "ndash" to "\u2013",
    "mdash" to "\u2014",
    "hellip" to "\u2026",
    "laquo" to "\u00AB",
    "raquo" to "\u00BB",
    "ldquo" to "\u201C",
    "rdquo" to "\u201D",
    "lsquo" to "\u2018",
    "rsquo" to "\u2019",
    "copy" to "\u00A9",
    "reg" to "\u00AE",
    "trade" to "\u2122",
    "shy" to "\u00AD",
    "deg" to "\u00B0",
    "times" to "\u00D7",
    "divide" to "\u00F7",
)

private sealed class Markup {
    abstract val end: Int

    data class Tag(
        val name: String,
        val isClose: Boolean,
        val isSelfClosing: Boolean,
        override val end: Int,
    ) : Markup()

    data class Comment(override val end: Int) : Markup()
    data class Declaration(override val end: Int) : Markup()
    data class CData(val content: String, override val end: Int) : Markup()
}

private data class DecodedEntity(val value: String, val end: Int)

private fun isMarkupStart(html: String, index: Int): Boolean {
    val next = html.getOrNull(index + 1) ?: return false
    return next == '/' || next == '!' || next == '?' || next.isLetter()
}

private fun readMarkup(html: String, start: Int): Markup {
    if (html.startsWith("<!--", start)) {
        val close = html.indexOf("-->", start + 4)
        return Markup.Comment(if (close < 0) html.length else close + 3)
    }
    if (html.startsWith("<![CDATA[", start, ignoreCase = true)) {
        val contentStart = start + 9
        val close = html.indexOf("]]>", contentStart)
        return if (close < 0) {
            Markup.CData(html.substring(contentStart), html.length)
        } else {
            Markup.CData(html.substring(contentStart, close), close + 3)
        }
    }
    if (html.startsWith("<!", start) || html.startsWith("<?", start)) {
        val close = html.indexOf('>', start + 2)
        return Markup.Declaration(if (close < 0) html.length else close + 1)
    }

    var i = start + 1
    val isClose = i < html.length && html[i] == '/'
    if (isClose) i++
    val nameStart = i
    while (i < html.length && isNameChar(html[i])) i++
    val rawName = html.substring(nameStart, i)
    val name = rawName.substringAfter(':').lowercase()

    var inQuote: Char? = null
    var isSelfClosing = false
    while (i < html.length) {
        val c = html[i]
        if (inQuote != null) {
            if (c == inQuote) inQuote = null
        } else {
            when (c) {
                '\'', '"' -> inQuote = c
                '/' -> {
                    if (html.getOrNull(i + 1) == '>') {
                        isSelfClosing = true
                        i += 2
                        break
                    }
                }
                '>' -> {
                    i++
                    break
                }
            }
        }
        i++
    }
    return Markup.Tag(
        name = name,
        isClose = isClose,
        isSelfClosing = isSelfClosing || name == "br",
        end = i.coerceAtMost(html.length),
    )
}

private fun isNameChar(c: Char): Boolean =
    c.isLetterOrDigit() || c == ':' || c == '-' || c == '_'

private fun skipUntilClosingTag(html: String, from: Int, tagName: String): Int {
    val close = "</$tagName"
    var i = from
    while (i < html.length) {
        val found = html.indexOf(close, i, ignoreCase = true)
        if (found < 0) return html.length
        val afterName = found + close.length
        val next = html.getOrNull(afterName)
        if (next == null || next == '>' || next.isWhitespace()) {
            val gt = html.indexOf('>', afterName)
            return if (gt < 0) html.length else gt + 1
        }
        i = afterName
    }
    return html.length
}

private fun readEntity(html: String, start: Int): DecodedEntity {
    val limit = minOf(html.length, start + 32)
    var i = start + 1
    if (i >= html.length) return DecodedEntity("&", start + 1)

    if (html[i] == '#') {
        i++
        val hex = i < html.length && (html[i] == 'x' || html[i] == 'X')
        if (hex) i++
        val digitsStart = i
        while (i < limit && html[i] != ';') {
            val c = html[i]
            val ok = if (hex) c.isHexDigit() else c.isDigit()
            if (!ok) return DecodedEntity("&", start + 1)
            i++
        }
        if (i >= html.length || html[i] != ';' || i == digitsStart) {
            return DecodedEntity("&", start + 1)
        }
        val digits = html.substring(digitsStart, i)
        val code = digits.toIntOrNull(if (hex) 16 else 10) ?: return DecodedEntity("&", start + 1)
        return DecodedEntity(codePointToString(code), i + 1)
    }

    val nameStart = i
    while (i < limit && html[i] != ';') {
        if (!html[i].isLetterOrDigit()) return DecodedEntity("&", start + 1)
        i++
    }
    if (i >= html.length || html[i] != ';' || i == nameStart) {
        return DecodedEntity("&", start + 1)
    }
    val name = html.substring(nameStart, i).lowercase()
    val replacement = NAMED_ENTITIES[name]
    return if (replacement != null) {
        DecodedEntity(replacement, i + 1)
    } else {
        DecodedEntity("&", start + 1)
    }
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun codePointToString(code: Int): String {
    if (code <= 0 || code > 0x10FFFF || code in 0xD800..0xDFFF) return "\uFFFD"
    if (code <= 0xFFFF) return code.toChar().toString()
    val cp = code - 0x10000
    val high = (0xD800 + (cp shr 10)).toChar()
    val low = (0xDC00 + (cp and 0x3FF)).toChar()
    return high.toString() + low
}

private fun collapseHtmlWhitespace(source: CharSequence): String {
    val out = StringBuilder(source.length)
    var previousSpace = false
    for (index in source.indices) {
        val c = source[index]
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000c') {
            if (!previousSpace) {
                out.append(' ')
                previousSpace = true
            }
        } else {
            out.append(c)
            previousSpace = false
        }
    }
    return out.toString()
}
