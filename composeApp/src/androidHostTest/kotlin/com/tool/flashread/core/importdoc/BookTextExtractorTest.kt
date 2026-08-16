package com.tool.flashread.core.importdoc

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BookTextExtractorTest {

    @Test
    fun extractsPlainTextWithoutTitle() {
        val file = tempFile("notes.txt", "Hello world.\nSecond line.")
        val book = BookTextExtractor.extract(file, fileName = "notes.txt", mimeType = "text/plain")
        assertNull(book.title)
        assertEquals("Hello world.\nSecond line.", book.content)
        assertNull(book.coverBytes)
    }

    @Test
    fun convertsHtmlToParagraphs() {
        val file = tempFile(
            "page.html",
            "<html><body><h1>Chapter</h1><p>Hello &amp; welcome.</p></body></html>",
        )
        val book = BookTextExtractor.extract(file, fileName = "page.html", mimeType = "text/html")
        assertNull(book.title)
        assertEquals("Chapter\nHello & welcome.", book.content)
    }

    @Test
    fun extractsFb2TitleAndParagraph() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook>
              <description>
                <title-info><book-title>War and Peace</book-title></title-info>
              </description>
              <body><p>Hello world.</p></body>
            </FictionBook>
        """.trimIndent()
        val file = tempFile("book.fb2", xml)
        val book = BookTextExtractor.extract(
            file,
            fileName = "book.fb2",
            mimeType = "application/octet-stream",
        )
        assertEquals("War and Peace", book.title)
        assertEquals("Hello world.", book.content)
        assertNull(book.coverBytes)
    }

    @Test
    fun extractsEpubTitleAndChapter() {
        val epub = zipBytes(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent().toByteArray(),
            "OEBPS/chapter1.xhtml" to """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <body><p>Hello world.</p></body>
                </html>
            """.trimIndent().toByteArray(),
            "OEBPS/content.opf" to """
                <?xml version="1.0"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="bookid" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:identifier id="bookid">id</dc:identifier>
                    <dc:title>War and Peace</dc:title>
                    <dc:language>en</dc:language>
                  </metadata>
                  <manifest>
                    <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                  </spine>
                </package>
            """.trimIndent().toByteArray(),
        )
        val file = tempFile("novel.epub", epub)
        val book = BookTextExtractor.extract(
            file,
            fileName = "novel.epub",
            mimeType = "application/epub+zip",
        )
        assertEquals("War and Peace", book.title)
        assertEquals("Hello world.", book.content)
        assertNull(book.coverBytes)
    }

    @Test
    fun extractsFb2FromZipWhenNameIsGeneric() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook>
              <description>
                <title-info><book-title>Zipped</book-title></title-info>
              </description>
              <body><p>From archive.</p></body>
            </FictionBook>
        """.trimIndent()
        val file = tempFile("download.zip", zipBytes("story.fb2" to xml.toByteArray()))
        val book = BookTextExtractor.extract(
            file,
            fileName = "download.zip",
            mimeType = "application/zip",
        )
        assertEquals("Zipped", book.title)
        assertEquals("From archive.", book.content)
    }

    @Test
    fun rejectsUnknownFormat() {
        val file = tempFile("photo.zip", zipBytes("img/cover.jpg" to byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
        val error = assertFailsWith<IllegalStateException> {
            BookTextExtractor.extract(file, fileName = "photo.zip", mimeType = "application/zip")
        }
        assertEquals(BookTextExtractor.UNSUPPORTED_FORMAT_MESSAGE, error.message)
    }

    @Test
    fun mapsBrokenXmlToDamagedFile() {
        val xml = """<?xml version="1.0" encoding="not-a-charset"?><FictionBook><body><p>x</p></body></FictionBook>"""
        val file = tempFile("book.fb2", xml)
        val error = assertFailsWith<IllegalStateException> {
            BookTextExtractor.extract(file, fileName = "book.fb2")
        }
        assertEquals(BookTextExtractor.DAMAGED_FILE_MESSAGE, error.message)
    }

    @Test
    fun mapsBrokenZipToDamagedFile() {
        val file = tempFile("novel.epub", byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00))
        val error = assertFailsWith<IllegalStateException> {
            BookTextExtractor.extract(file, fileName = "novel.epub", mimeType = "application/epub+zip")
        }
        assertEquals(BookTextExtractor.DAMAGED_FILE_MESSAGE, error.message)
    }
}

private fun tempFile(name: String, content: String): File =
    tempFile(name, content.toByteArray())

private fun tempFile(name: String, bytes: ByteArray): File {
    val file = File.createTempFile("flashread-test-", "-$name")
    file.writeBytes(bytes)
    file.deleteOnExit()
    return file
}

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
