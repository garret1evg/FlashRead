package com.tool.flashread.core.importdoc

import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/**
 * Turns an EPUB archive into UTF-8 paragraph-per-line text.
 * Reading order follows the OPF spine; chapter XHTML is flattened by [HtmlToPlainText].
 */
object EpubTextExtractor {
    fun extract(input: InputStream): ExtractedBook {
        val entries = readZipEntries(input)
        val containerBytes = findEntry(entries, CONTAINER_PATH)
            ?: error("EPUB is missing META-INF/container.xml.")
        val opfPath = parseContainer(containerBytes)
        val opfBytes = findEntry(entries, opfPath)
            ?: error("EPUB package document was not found: $opfPath")
        val opf = parseOpf(opfBytes, opfPath)

        val chapters = ArrayList<String>()
        for (chapterPath in opf.chapterPaths) {
            val bytes = findEntry(entries, chapterPath) ?: continue
            val text = HtmlToPlainText.convert(decodeText(bytes))
            if (text.isNotEmpty()) chapters.add(text)
        }
        return ExtractedBook(
            title = opf.title,
            content = chapters.joinToString("\n"),
        )
    }
}

private const val CONTAINER_PATH = "META-INF/container.xml"
private const val OPF_PACKAGE_MEDIA = "application/oebps-package+xml"

private val HTML_MEDIA_TYPES = setOf(
    "application/xhtml+xml",
    "text/html",
)
private val HTML_EXTENSIONS = setOf("xhtml", "html", "htm")
private val SKIP_ENTRY_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "tif", "tiff", "svg",
    "css", "otf", "ttf", "woff", "woff2", "eot",
    "mp3", "mp4", "m4a", "ogg", "opus", "wav",
    "ncx", "smil", "xpgt",
)

private data class OpfPackage(
    val title: String?,
    val chapterPaths: List<String>,
)

private data class ManifestItem(
    val href: String,
    val mediaType: String,
    val properties: String,
)

private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
    val entries = LinkedHashMap<String, ByteArray>()
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) {
                zip.closeEntry()
                continue
            }
            val name = normalizeZipPath(entry.name)
            if (name.isEmpty() || shouldSkipEntry(name)) {
                zip.closeEntry()
                continue
            }
            entries[name] = zip.readBytes()
            zip.closeEntry()
        }
    }
    if (entries.isEmpty()) error("EPUB archive is empty or unreadable.")
    return entries
}

private fun shouldSkipEntry(name: String): Boolean {
    val lower = name.lowercase()
    if (lower.contains("__macosx/") || lower.endsWith(".ds_store")) return true
    val fileName = lower.substringAfterLast('/')
    if (fileName.startsWith(".")) return true
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
    return extension in SKIP_ENTRY_EXTENSIONS
}

private fun findEntry(entries: Map<String, ByteArray>, path: String): ByteArray? {
    val normalized = normalizeZipPath(path)
    entries[normalized]?.let { return it }
    val lower = normalized.lowercase()
    return entries.entries.firstOrNull { it.key.lowercase() == lower }?.value
}

private fun parseContainer(bytes: ByteArray): String {
    val parser = newXmlParser(bytes)
    var firstPath: String? = null
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.localName() == "rootfile") {
            val path = parser.attr("full-path")?.trim().orEmpty()
            if (path.isNotEmpty()) {
                val media = parser.attr("media-type")?.trim().orEmpty()
                if (media.isEmpty() || media.equals(OPF_PACKAGE_MEDIA, ignoreCase = true)) {
                    return normalizeZipPath(percentDecode(path))
                }
                if (firstPath == null) firstPath = path
            }
        }
        event = parser.next()
    }
    val fallback = firstPath?.trim().orEmpty()
    if (fallback.isEmpty()) {
        error("EPUB container.xml does not specify a package document.")
    }
    return normalizeZipPath(percentDecode(fallback))
}

private fun parseOpf(bytes: ByteArray, opfPath: String): OpfPackage {
    val parser = newXmlParser(bytes)
    var title: String? = null
    var inMetadata = false
    var inManifest = false
    var inSpine = false
    val manifest = LinkedHashMap<String, ManifestItem>()
    val spine = ArrayList<Pair<String, Boolean>>()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.localName()) {
                "metadata" -> inMetadata = true
                "manifest" -> inManifest = true
                "spine" -> inSpine = true
                "title" -> {
                    if (inMetadata && title == null) {
                        title = parser.readInnerText().ifBlank { null }
                    }
                }
                "item" -> {
                    if (inManifest) {
                        val id = parser.attr("id")?.trim().orEmpty()
                        val href = parser.attr("href")?.trim().orEmpty()
                        if (id.isNotEmpty() && href.isNotEmpty()) {
                            manifest[id] = ManifestItem(
                                href = href,
                                mediaType = parser.attr("media-type").orEmpty(),
                                properties = parser.attr("properties").orEmpty(),
                            )
                        }
                    }
                }
                "itemref" -> {
                    if (inSpine) {
                        val idref = parser.attr("idref")?.trim().orEmpty()
                        if (idref.isNotEmpty()) {
                            val linear = !parser.attr("linear").equals("no", ignoreCase = true)
                            spine.add(idref to linear)
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> when (parser.localName()) {
                "metadata" -> inMetadata = false
                "manifest" -> inManifest = false
                "spine" -> inSpine = false
            }
        }
        event = parser.next()
    }

    val byId = HashMap<String, ManifestItem>(manifest.size)
    val byIdLower = HashMap<String, ManifestItem>(manifest.size)
    for ((id, item) in manifest) {
        byId[id] = item
        byIdLower.putIfAbsent(id.lowercase(), item)
    }

    val chapterPaths = ArrayList<String>(spine.size)
    for ((idref, linear) in spine) {
        if (!linear) continue
        val item = byId[idref] ?: byIdLower[idref.lowercase()] ?: continue
        if (!isHtmlChapter(item)) continue
        chapterPaths.add(resolveHref(opfPath, item.href))
    }
    return OpfPackage(title = title, chapterPaths = chapterPaths)
}

private fun isHtmlChapter(item: ManifestItem): Boolean {
    val properties = item.properties.splitToSequence(' ', '\t', '\n', '\r')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
    if ("nav" in properties || "cover-image" in properties) return false

    val media = item.mediaType.substringBefore(';').trim().lowercase()
    if (media in HTML_MEDIA_TYPES) return true
    if (media.isNotEmpty()) return false

    val path = item.href.substringBefore('#').substringBefore('?')
    val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in HTML_EXTENSIONS
}

private fun resolveHref(opfPath: String, href: String): String {
    val decoded = percentDecode(href.substringBefore('#').substringBefore('?').trim())
    if (decoded.startsWith("/")) return normalizeZipPath(decoded)
    val opfDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "")
    val combined = if (opfDir.isEmpty()) decoded else "$opfDir/$decoded"
    return normalizeZipPath(combined)
}

private fun normalizeZipPath(path: String): String {
    val cleaned = path.replace('\\', '/').trim()
    val parts = ArrayList<String>()
    for (part in cleaned.split('/')) {
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts.add(part)
        }
    }
    return parts.joinToString("/")
}

private fun percentDecode(value: String): String {
    if (!value.contains('%')) return value
    val out = ByteArrayOutputStream(value.length)
    var i = 0
    val bytes = value.toByteArray(Charsets.ISO_8859_1)
    while (i < bytes.size) {
        val b = bytes[i]
        if (b == '%'.code.toByte() && i + 2 < bytes.size) {
            val hi = (bytes[i + 1].toInt() and 0xFF).toChar()
            val lo = (bytes[i + 2].toInt() and 0xFF).toChar()
            val parsed = "$hi$lo".toIntOrNull(16)
            if (parsed != null) {
                out.write(parsed)
                i += 3
                continue
            }
        }
        out.write(b.toInt() and 0xFF)
        i++
    }
    return out.toString(Charsets.UTF_8.name())
}

private fun decodeText(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val bom = detectBom(bytes)
    if (bom != null) {
        return String(bytes, bom.skip, bytes.size - bom.skip, bom.charset)
    }
    val encodingName = sniffDeclaredEncoding(bytes) ?: "UTF-8"
    val charset = try {
        Charset.forName(encodingName)
    } catch (_: Exception) {
        Charsets.UTF_8
    }
    return String(bytes, charset)
}

private data class Bom(val charset: Charset, val skip: Int)

private fun detectBom(bytes: ByteArray): Bom? {
    if (bytes.size >= 3 &&
        (bytes[0].toInt() and 0xFF) == 0xEF &&
        (bytes[1].toInt() and 0xFF) == 0xBB &&
        (bytes[2].toInt() and 0xFF) == 0xBF
    ) {
        return Bom(Charsets.UTF_8, 3)
    }
    if (bytes.size >= 2) {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        if (b0 == 0xFF && b1 == 0xFE) return Bom(Charsets.UTF_16LE, 2)
        if (b0 == 0xFE && b1 == 0xFF) return Bom(Charsets.UTF_16BE, 2)
    }
    return null
}

private val XML_ENCODING = Regex(
    """<\?xml\s+[^>]*encoding\s*=\s*['"]\s*([^'"]+)['"]""",
    setOf(RegexOption.IGNORE_CASE),
)
private val META_CHARSET = Regex(
    """<meta\b[^>]*\bcharset\s*=\s*['"]?\s*([A-Za-z0-9._-]+)""",
    setOf(RegexOption.IGNORE_CASE),
)
private val CONTENT_CHARSET = Regex(
    """charset\s*=\s*['"]?\s*([A-Za-z0-9._-]+)""",
    setOf(RegexOption.IGNORE_CASE),
)

private fun sniffDeclaredEncoding(bytes: ByteArray): String? {
    val headerLength = minOf(bytes.size, 1024)
    val header = String(bytes, 0, headerLength, Charsets.ISO_8859_1)
    XML_ENCODING.find(header)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
    META_CHARSET.find(header)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
    return CONTENT_CHARSET.find(header)?.groupValues?.getOrNull(1)?.trim()
}

private fun newXmlParser(bytes: ByteArray): XmlPullParser {
    val parser = KXmlParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    parser.setInput(bytes.inputStream(), null)
    return parser
}

private fun XmlPullParser.localName(): String =
    name.orEmpty().substringAfter(':').lowercase()

private fun XmlPullParser.attr(name: String): String? {
    val wanted = name.lowercase()
    for (index in 0 until attributeCount) {
        val raw = getAttributeName(index) ?: continue
        if (raw.substringAfter(':').lowercase() == wanted) {
            return getAttributeValue(index)
        }
    }
    return null
}

private fun XmlPullParser.readInnerText(): String {
    val text = StringBuilder()
    var depth = 1
    while (depth > 0) {
        when (next()) {
            XmlPullParser.START_TAG -> depth++
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> text.append(this.text)
            XmlPullParser.END_DOCUMENT -> break
        }
    }
    return collapseXmlWhitespace(text).trim()
}

private fun collapseXmlWhitespace(source: CharSequence): String {
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
