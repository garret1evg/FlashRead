package com.tool.flashread.core.youtube

internal data class CaptionSnippet(
    val text: String,
    val start: Double = 0.0,
    val duration: Double = 0.0,
)

/**
 * Parses YouTube timedtext XML (`<text start dur>` nodes), matching
 * youtube-transcript-api: decode entities, drop empty nodes, ignore missing `dur`.
 */
internal object YouTubeCaptionXml {
    fun parse(xml: String): List<CaptionSnippet> {
        val snippets = ArrayList<CaptionSnippet>()
        var index = 0
        while (index < xml.length) {
            val open = findTextOpen(xml, index) ?: break
            if (open.selfClosing) {
                index = open.contentStart
                continue
            }
            val close = findTextClose(xml, open.contentStart)
            val inner = xml.substring(open.contentStart, close.start)
            index = close.end
            val text = flattenXmlText(inner)
            if (text.isEmpty()) continue
            snippets.add(
                CaptionSnippet(
                    text = text,
                    start = open.attributes["start"]?.toDoubleOrNull() ?: 0.0,
                    duration = open.attributes["dur"]?.toDoubleOrNull() ?: 0.0,
                ),
            )
        }
        return snippets
    }

    fun toReaderText(xml: String): String {
        return YouTubeTranscriptText.join(parse(xml).map { it.text })
    }
}

private data class TextOpenTag(
    val attributes: Map<String, String>,
    val contentStart: Int,
    val selfClosing: Boolean,
)

private data class TextCloseTag(
    val start: Int,
    val end: Int,
)

private fun findTextOpen(xml: String, from: Int): TextOpenTag? {
    var index = from
    while (index < xml.length) {
        val start = xml.indexOf("<text", index, ignoreCase = true)
        if (start < 0) return null
        val afterName = start + 5
        val next = xml.getOrNull(afterName)
        if (next == null || next.isWhitespace() || next == '/' || next == '>') {
            return readTextOpen(xml, afterName)
        }
        index = afterName
    }
    return null
}

private fun readTextOpen(xml: String, from: Int): TextOpenTag {
    val attributes = HashMap<String, String>()
    var index = from
    var inQuote: Char? = null
    var name: String? = null
    var valueStart = -1
    var selfClosing = false

    while (index < xml.length) {
        val char = xml[index]
        if (inQuote != null) {
            if (char == inQuote) {
                val attrName = name
                if (attrName != null && valueStart >= 0) {
                    attributes[attrName.lowercase()] = xml.substring(valueStart, index)
                }
                inQuote = null
                name = null
                valueStart = -1
            }
            index++
            continue
        }
        when {
            char == '>' -> {
                index++
                break
            }
            char == '/' && xml.getOrNull(index + 1) == '>' -> {
                selfClosing = true
                index += 2
                break
            }
            char == '=' -> {
                index++
                while (index < xml.length && xml[index].isWhitespace()) index++
                val quote = xml.getOrNull(index)
                if (quote == '"' || quote == '\'') {
                    inQuote = quote
                    valueStart = index + 1
                    index++
                }
            }
            char.isWhitespace() -> index++
            else -> {
                val nameStart = index
                while (index < xml.length) {
                    val current = xml[index]
                    if (current.isWhitespace() || current == '=' || current == '>' || current == '/') break
                    index++
                }
                name = xml.substring(nameStart, index)
            }
        }
    }
    return TextOpenTag(
        attributes = attributes,
        contentStart = index.coerceAtMost(xml.length),
        selfClosing = selfClosing,
    )
}

private fun findTextClose(xml: String, from: Int): TextCloseTag {
    var index = from
    while (index < xml.length) {
        val start = xml.indexOf("</text", index, ignoreCase = true)
        if (start < 0) return TextCloseTag(xml.length, xml.length)
        var afterName = start + 6
        while (afterName < xml.length && xml[afterName].isWhitespace()) afterName++
        if (afterName < xml.length && xml[afterName] == '>') {
            return TextCloseTag(start, afterName + 1)
        }
        index = start + 6
    }
    return TextCloseTag(xml.length, xml.length)
}

internal fun flattenXmlText(raw: String): String {
    val out = StringBuilder(raw.length)
    var index = 0
    while (index < raw.length) {
        val char = raw[index]
        when {
            char == '<' && raw.startsWith("<![CDATA[", index, ignoreCase = true) -> {
                val contentStart = index + 9
                val close = raw.indexOf("]]>", contentStart)
                if (close < 0) {
                    out.append(raw.substring(contentStart))
                    break
                }
                out.append(raw.substring(contentStart, close))
                index = close + 3
            }
            char == '<' -> {
                val close = raw.indexOf('>', index + 1)
                index = if (close < 0) raw.length else close + 1
            }
            char == '&' -> {
                val decoded = decodeXmlEntity(raw, index)
                out.append(decoded.value)
                index = decoded.end
            }
            else -> {
                out.append(char)
                index++
            }
        }
    }
    return out.toString()
}

private val XML_NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to "\u00A0",
)

private data class DecodedXmlEntity(val value: String, val end: Int)

internal fun unescapeHtml(text: String): String {
    if ('&' !in text) return text
    val out = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val amp = text.indexOf('&', index)
        if (amp < 0) {
            out.append(text, index, text.length)
            break
        }
        out.append(text, index, amp)
        val decoded = decodeXmlEntity(text, amp)
        out.append(decoded.value)
        index = decoded.end
    }
    return out.toString()
}

private fun decodeXmlEntity(text: String, start: Int): DecodedXmlEntity {
    val limit = minOf(text.length, start + 32)
    var index = start + 1
    if (index >= text.length) return DecodedXmlEntity("&", start + 1)

    if (text[index] == '#') {
        index++
        val hex = index < text.length && (text[index] == 'x' || text[index] == 'X')
        if (hex) index++
        val digitsStart = index
        while (index < limit && text[index] != ';') {
            val char = text[index]
            val ok = if (hex) char.isHexDigit() else char.isDigit()
            if (!ok) return DecodedXmlEntity("&", start + 1)
            index++
        }
        if (index >= text.length || text[index] != ';' || index == digitsStart) {
            return DecodedXmlEntity("&", start + 1)
        }
        val digits = text.substring(digitsStart, index)
        val code = digits.toIntOrNull(if (hex) 16 else 10) ?: return DecodedXmlEntity("&", start + 1)
        return DecodedXmlEntity(codePointToString(code), index + 1)
    }

    val nameStart = index
    while (index < limit && text[index] != ';') {
        if (!text[index].isLetterOrDigit()) return DecodedXmlEntity("&", start + 1)
        index++
    }
    if (index >= text.length || text[index] != ';' || index == nameStart) {
        return DecodedXmlEntity("&", start + 1)
    }
    val replacement = XML_NAMED_ENTITIES[text.substring(nameStart, index).lowercase()]
    return if (replacement != null) {
        DecodedXmlEntity(replacement, index + 1)
    } else {
        DecodedXmlEntity("&", start + 1)
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
