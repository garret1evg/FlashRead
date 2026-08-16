package com.tool.flashread.core.importdoc

/**
 * Importable book formats. Detection prefers byte sniffing when the header
 * is available, because Android often reports FB2/EPUB as `octet-stream` or
 * a generic XML/ZIP MIME type.
 */
enum class BookFormat {
    Text,
    Fb2,
    Epub,
    Unknown,
    ;

    companion object {
        fun detect(
            fileName: String? = null,
            mimeType: String? = null,
            headerBytes: ByteArray? = null,
            zipEntryNames: List<String>? = null,
        ): BookFormat {
            val fromZip = detectFromZipEntries(zipEntryNames)
            val sniffed = sniffHeader(headerBytes)
            val fromName = detectFromFileName(fileName)
            val fromMime = detectFromMime(mimeType)

            if (sniffed != null) return sniffed
            if (isZipSignature(headerBytes)) {
                if (fromZip != null) return fromZip
                if (fromName == BookFormat.Epub || fromName == BookFormat.Fb2) return fromName
                if (fromMime == BookFormat.Epub || fromMime == BookFormat.Fb2) return fromMime
                return BookFormat.Unknown
            }

            if (fromZip != null) return fromZip
            if (isStrongMime(mimeType) && fromMime != null) return fromMime
            if (fromName != null) return fromName
            if (fromMime != null) return fromMime
            return BookFormat.Unknown
        }

        fun isZipSignature(headerBytes: ByteArray?): Boolean {
            if (headerBytes == null || headerBytes.size < 2) return false
            return headerBytes[0] == 'P'.code.toByte() && headerBytes[1] == 'K'.code.toByte()
        }
    }
}

private val TEXT_EXTENSIONS = setOf("txt", "md", "html", "htm")
private const val EPUB_MIME = "application/epub+zip"
private val FB2_MIMES = setOf(
    "application/x-fictionbook+xml",
    "application/x-fictionbook",
    "application/fictionbook+xml",
)
private val XML_MIMES = setOf("text/xml", "application/xml")

private fun detectFromFileName(fileName: String?): BookFormat? {
    val name = fileName?.substringAfterLast('/')?.substringAfterLast('\\')?.lowercase()?.trim().orEmpty()
    if (name.isEmpty()) return null
    if (name.endsWith(".fb2.zip")) return BookFormat.Fb2
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
    if (extension.isEmpty() || extension == name) return null
    return when (extension) {
        "fb2" -> BookFormat.Fb2
        "epub" -> BookFormat.Epub
        in TEXT_EXTENSIONS -> BookFormat.Text
        else -> null
    }
}

private fun detectFromMime(mimeType: String?): BookFormat? {
    val mime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
    if (mime.isEmpty()) return null
    return when {
        mime == EPUB_MIME -> BookFormat.Epub
        mime in FB2_MIMES -> BookFormat.Fb2
        mime in XML_MIMES -> BookFormat.Fb2
        mime == "application/xhtml+xml" || mime == "text/html" -> BookFormat.Text
        mime.startsWith("text/") -> BookFormat.Text
        else -> null
    }
}

private fun isStrongMime(mimeType: String?): Boolean {
    val mime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
    return mime == EPUB_MIME || mime in FB2_MIMES
}

private fun detectFromZipEntries(zipEntryNames: List<String>?): BookFormat? {
    if (zipEntryNames.isNullOrEmpty()) return null
    var sawFb2 = false
    for (raw in zipEntryNames) {
        val name = raw.replace('\\', '/').trim().lowercase()
        if (name.isEmpty()) continue
        if (name == "mimetype" || name == "meta-inf/container.xml") return BookFormat.Epub
        if (name.endsWith(".fb2")) sawFb2 = true
    }
    return if (sawFb2) BookFormat.Fb2 else null
}

private fun sniffHeader(headerBytes: ByteArray?): BookFormat? {
    if (headerBytes == null || headerBytes.isEmpty()) return null
    if (BookFormat.isZipSignature(headerBytes)) {
        return if (asciiContains(headerBytes, EPUB_MIME)) BookFormat.Epub else null
    }
    val text = headerAsText(headerBytes).trimStart()
    if (text.isEmpty()) return null
    return sniffMarkup(text)
}

private fun sniffMarkup(text: String): BookFormat? {
    var body = text
    if (body.startsWith("<?xml", ignoreCase = true)) {
        val declEnd = body.indexOf('>')
        body = if (declEnd >= 0) body.substring(declEnd + 1).trimStart() else body
    }
    val start = body.take(80).lowercase()
    if (start.startsWith("<!doctype html") ||
        start.startsWith("<html") ||
        start.startsWith("<head") ||
        start.startsWith("<body")
    ) {
        return BookFormat.Text
    }
    if (body.startsWith("<")) return BookFormat.Fb2
    if (body.contains("FictionBook", ignoreCase = true)) return BookFormat.Fb2
    return null
}

private fun headerAsText(bytes: ByteArray): String {
    val utf8Bom = bytes.size >= 3 &&
        (bytes[0].toInt() and 0xFF) == 0xEF &&
        (bytes[1].toInt() and 0xFF) == 0xBB &&
        (bytes[2].toInt() and 0xFF) == 0xBF
    if (utf8Bom) return decodeLatin1(bytes, 3)

    if (bytes.size >= 2) {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        if (b0 == 0xFF && b1 == 0xFE) return decodeUtf16(bytes, start = 2, littleEndian = true)
        if (b0 == 0xFE && b1 == 0xFF) return decodeUtf16(bytes, start = 2, littleEndian = false)
    }
    return decodeLatin1(bytes, 0)
}

private fun decodeLatin1(bytes: ByteArray, start: Int): String {
    val end = minOf(bytes.size, start + 1024)
    return buildString(end - start) {
        for (i in start until end) {
            append((bytes[i].toInt() and 0xFF).toChar())
        }
    }
}

private fun decodeUtf16(bytes: ByteArray, start: Int, littleEndian: Boolean): String {
    val end = minOf(bytes.size, start + 2048)
    return buildString((end - start) / 2) {
        var i = start
        while (i + 1 < end) {
            val lo = bytes[i].toInt() and 0xFF
            val hi = bytes[i + 1].toInt() and 0xFF
            val code = if (littleEndian) lo or (hi shl 8) else hi or (lo shl 8)
            append(code.toChar())
            i += 2
        }
    }
}

private fun asciiContains(bytes: ByteArray, needle: String): Boolean {
    val limit = minOf(bytes.size, 256)
    val first = needle[0].code
    outer@ for (i in 0..limit - needle.length) {
        if ((bytes[i].toInt() and 0xFF) != first) continue
        for (j in 1 until needle.length) {
            if ((bytes[i + j].toInt() and 0xFF) != needle[j].code) continue@outer
        }
        return true
    }
    return false
}
