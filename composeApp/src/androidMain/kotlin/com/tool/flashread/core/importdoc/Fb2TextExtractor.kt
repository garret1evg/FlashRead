package com.tool.flashread.core.importdoc

import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Turns a FictionBook (`.fb2` or `.fb2.zip`) into UTF-8 paragraph-per-line text.
 * Encoding is taken from the XML declaration so `windows-1251` files decode correctly.
 */
object Fb2TextExtractor {
    fun extract(input: InputStream): ExtractedBook {
        val stream = if (input is BufferedInputStream) input else BufferedInputStream(input)
        stream.mark(ZIP_SIGNATURE_BYTES)
        val signature = ByteArray(ZIP_SIGNATURE_BYTES)
        val read = stream.read(signature).coerceAtLeast(0)
        stream.reset()
        return if (BookFormat.isZipSignature(signature.copyOf(read))) {
            extractFromZip(stream)
        } else {
            extractFromXml(stream)
        }
    }

    private fun extractFromZip(input: InputStream): ExtractedBook {
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory || !isFb2Entry(entry.name)) continue
                return extractFromXml(zip)
            }
        }
        error("ZIP archive does not contain an FB2 file.")
    }

    private fun extractFromXml(input: InputStream): ExtractedBook {
        val stream = if (input is BufferedInputStream) input else BufferedInputStream(input)
        val parser = KXmlParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        // null encoding: detect from BOM / XML declaration (often windows-1251).
        parser.setInput(stream, null)
        return parseFictionBook(parser)
    }
}

private const val ZIP_SIGNATURE_BYTES = 4

private val BODY_PARAGRAPH_TAGS = setOf("p", "v", "subtitle")

private fun isFb2Entry(name: String): Boolean {
    val normalized = name.replace('\\', '/').trim().lowercase()
    if (normalized.isEmpty() || normalized.contains("__macosx/")) return false
    val fileName = normalized.substringAfterLast('/')
    return fileName.endsWith(".fb2") && !fileName.startsWith(".")
}

private fun parseFictionBook(parser: XmlPullParser): ExtractedBook {
    var title: String? = null
    val paragraphs = ArrayList<String>()
    var descriptionDepth = 0
    var bodyDepth = 0

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                when (parser.localName()) {
                    "description" -> descriptionDepth++
                    "body" -> bodyDepth++
                    "binary" -> parser.skipElement()
                    "book-title" -> {
                        if (descriptionDepth > 0 && title == null) {
                            title = parser.readInnerText().ifBlank { null }
                        } else {
                            parser.skipElement()
                        }
                    }
                    "title" -> {
                        if (bodyDepth > 0 && descriptionDepth == 0) {
                            parser.collectTitleParagraphs(paragraphs)
                        } else {
                            parser.skipElement()
                        }
                    }
                    in BODY_PARAGRAPH_TAGS -> {
                        if (bodyDepth > 0 && descriptionDepth == 0) {
                            val text = parser.readInnerText()
                            if (text.isNotEmpty()) paragraphs.add(text)
                        } else {
                            parser.skipElement()
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.localName()) {
                    "description" -> if (descriptionDepth > 0) descriptionDepth--
                    "body" -> if (bodyDepth > 0) bodyDepth--
                }
            }
        }
        event = parser.next()
    }
    return ExtractedBook(
        title = title,
        content = paragraphs.joinToString("\n"),
    )
}

private fun XmlPullParser.localName(): String =
    name.orEmpty().substringAfter(':').lowercase()

private fun XmlPullParser.skipElement() {
    if (eventType != XmlPullParser.START_TAG) return
    var depth = 1
    while (depth > 0) {
        when (next()) {
            XmlPullParser.START_TAG -> depth++
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.END_DOCUMENT -> return
        }
    }
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

private fun XmlPullParser.collectTitleParagraphs(out: MutableList<String>) {
    val direct = StringBuilder()
    var depth = 1
    while (depth > 0) {
        when (next()) {
            XmlPullParser.START_TAG -> {
                if (depth == 1 && localName() == "p") {
                    val leftover = collapseXmlWhitespace(direct).trim()
                    if (leftover.isNotEmpty()) out.add(leftover)
                    direct.setLength(0)
                    val nested = readInnerText()
                    if (nested.isNotEmpty()) out.add(nested)
                } else {
                    depth++
                }
            }
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> direct.append(text)
            XmlPullParser.END_DOCUMENT -> return
        }
    }
    val leftover = collapseXmlWhitespace(direct).trim()
    if (leftover.isNotEmpty()) out.add(leftover)
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
