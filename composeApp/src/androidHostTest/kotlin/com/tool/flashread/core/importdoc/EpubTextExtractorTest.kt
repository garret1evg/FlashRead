package com.tool.flashread.core.importdoc

import com.tool.flashread.core.speedread.splitBookParagraphs
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EpubTextExtractorTest {

    @Test
    fun extractsTitleAndSingleChapterParagraph() {
        val epub = singleChapterEpub(
            title = "War and Peace",
            body = "<p>Hello world.</p>",
        )
        val book = EpubTextExtractor.extract(epub.inputStream())
        assertEquals("War and Peace", book.title)
        assertEquals("Hello world.", book.content)
    }

    @Test
    fun followsSpineOrderNotZipOrManifestOrder() {
        val epub = zipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to containerXml("OEBPS/content.opf").toByteArray(),
            "OEBPS/chapter-b.xhtml" to xhtml("<p>Second.</p>").toByteArray(),
            "OEBPS/chapter-a.xhtml" to xhtml("<p>First.</p>").toByteArray(),
            "OEBPS/content.opf" to opf(
                title = "Ordered",
                manifest = """
                    <item id="b" href="chapter-b.xhtml" media-type="application/xhtml+xml"/>
                    <item id="a" href="chapter-a.xhtml" media-type="application/xhtml+xml"/>
                """.trimIndent(),
                spine = """
                    <itemref idref="a"/>
                    <itemref idref="b"/>
                """.trimIndent(),
            ).toByteArray(),
        )
        val book = EpubTextExtractor.extract(epub.inputStream())
        assertEquals("First.\nSecond.", book.content)
        assertEquals(listOf("First.", "Second."), splitBookParagraphs(book.content))
    }

    @Test
    fun skipsImagesCssNcxAndNavDocuments() {
        val epub = zipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to containerXml("OEBPS/content.opf").toByteArray(),
            "OEBPS/style.css" to "p { color: red }".toByteArray(),
            "OEBPS/cover.jpg" to byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
            "OEBPS/toc.ncx" to "<ncx><text>Contents</text></ncx>".toByteArray(),
            "OEBPS/nav.xhtml" to xhtml("<p>Table of contents</p>").toByteArray(),
            "OEBPS/chapter1.xhtml" to xhtml("<p>Only this.</p>").toByteArray(),
            "OEBPS/content.opf" to opf(
                title = "Clean",
                manifest = """
                    <item id="css" href="style.css" media-type="text/css"/>
                    <item id="cover" href="cover.jpg" media-type="image/jpeg"/>
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                """.trimIndent(),
                spine = """
                    <itemref idref="nav"/>
                    <itemref idref="c1"/>
                """.trimIndent(),
            ).toByteArray(),
        )
        val book = EpubTextExtractor.extract(epub.inputStream())
        assertEquals("Only this.", book.content)
    }

    @Test
    fun skipsLinearNoSpineItems() {
        val epub = zipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to containerXml("OEBPS/content.opf").toByteArray(),
            "OEBPS/notes.xhtml" to xhtml("<p>Footnotes.</p>").toByteArray(),
            "OEBPS/chapter1.xhtml" to xhtml("<p>Story.</p>").toByteArray(),
            "OEBPS/content.opf" to opf(
                title = "Linear",
                manifest = """
                    <item id="notes" href="notes.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                """.trimIndent(),
                spine = """
                    <itemref idref="notes" linear="no"/>
                    <itemref idref="c1"/>
                """.trimIndent(),
            ).toByteArray(),
        )
        assertEquals("Story.", EpubTextExtractor.extract(epub.inputStream()).content)
    }

    @Test
    fun resolvesChapterHrefsRelativeToOpf() {
        val epub = zipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to containerXml("OEBPS/content.opf").toByteArray(),
            "OEBPS/Text/chapter1.xhtml" to xhtml("<p>Nested chapter.</p>").toByteArray(),
            "OEBPS/content.opf" to opf(
                title = "Nested",
                manifest = """
                    <item id="c1" href="Text/chapter1.xhtml#start" media-type="application/xhtml+xml"/>
                """.trimIndent(),
                spine = """<itemref idref="c1"/>""",
            ).toByteArray(),
        )
        val book = EpubTextExtractor.extract(epub.inputStream())
        assertEquals("Nested", book.title)
        assertEquals("Nested chapter.", book.content)
    }

    @Test
    fun decodesPercentEncodedHrefAndSkipsHeadMarkup() {
        val epub = zipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to containerXml("OPS/package.opf").toByteArray(),
            "OPS/My Chapter.xhtml" to xhtml(
                body = "<p>Visible &amp; ready.</p>",
                extraHead = "<title>Should not appear</title><script>ignored()</script>",
            ).toByteArray(),
            "OPS/package.opf" to opf(
                title = "Encoded",
                manifest = """
                    <item id="c1" href="My%20Chapter.xhtml" media-type="application/xhtml+xml"/>
                """.trimIndent(),
                spine = """<itemref idref="c1"/>""",
            ).toByteArray(),
        )
        assertEquals("Visible & ready.", EpubTextExtractor.extract(epub.inputStream()).content)
    }

    @Test
    fun allowsMissingTitleAndJoinsChapterParagraphs() {
        val epub = singleChapterEpub(
            title = null,
            body = "<h1>Chapter</h1><p>First paragraph.</p><div>Second paragraph.</div>",
        )
        val book = EpubTextExtractor.extract(epub.inputStream())
        assertNull(book.title)
        assertEquals("Chapter\nFirst paragraph.\nSecond paragraph.", book.content)
        assertEquals(
            listOf("Chapter", "First paragraph.", "Second paragraph."),
            splitBookParagraphs(book.content),
        )
    }

    @Test
    fun rejectsArchiveWithoutContainer() {
        val zip = zipBytes("readme.txt" to "not an epub".toByteArray())
        val error = assertFailsWith<IllegalStateException> {
            EpubTextExtractor.extract(zip.inputStream())
        }
        assertEquals("EPUB is missing META-INF/container.xml.", error.message)
    }

    @Test
    fun rejectsContainerWhenPackageDocumentIsMissing() {
        val zip = zipBytes(
            "META-INF/container.xml" to containerXml("OEBPS/missing.opf").toByteArray(),
        )
        val error = assertFailsWith<IllegalStateException> {
            EpubTextExtractor.extract(zip.inputStream())
        }
        assertEquals("EPUB package document was not found: OEBPS/missing.opf", error.message)
    }
}

private fun singleChapterEpub(title: String?, body: String): ByteArray {
    return zipBytes(
        "mimetype" to "application/epub+zip".toByteArray(),
        "META-INF/container.xml" to containerXml("OEBPS/content.opf").toByteArray(),
        "OEBPS/chapter1.xhtml" to xhtml(body).toByteArray(),
        "OEBPS/content.opf" to opf(
            title = title,
            manifest = """<item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>""",
            spine = """<itemref idref="c1"/>""",
        ).toByteArray(),
    )
}

private fun containerXml(opfPath: String): String =
    """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="$opfPath" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""

private fun opf(title: String?, manifest: String, spine: String): String {
    val titleTag = if (title == null) "" else "<dc:title>$title</dc:title>"
    return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="bookid" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="bookid">id</dc:identifier>
    $titleTag
    <dc:language>en</dc:language>
  </metadata>
  <manifest>
$manifest
  </manifest>
  <spine>
$spine
  </spine>
</package>
"""
}

private fun xhtml(body: String, extraHead: String = ""): String =
    """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>$extraHead</head>
  <body>
    $body
  </body>
</html>
"""

private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        for ((name, bytes) in entries) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}
