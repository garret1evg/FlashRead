package com.tool.flashread.core.importdoc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookFormatTest {

    @Test
    fun detectsTextFromExtension() {
        assertEquals(BookFormat.Text, BookFormat.detect(fileName = "notes.txt"))
        assertEquals(BookFormat.Text, BookFormat.detect(fileName = "Chapter One.MD"))
        assertEquals(BookFormat.Text, BookFormat.detect(fileName = "index.html"))
        assertEquals(BookFormat.Text, BookFormat.detect(fileName = "page.HTM"))
        assertEquals(BookFormat.Text, BookFormat.detect(fileName = "folder/read me.txt"))
    }

    @Test
    fun detectsFb2AndEpubFromExtension() {
        assertEquals(BookFormat.Fb2, BookFormat.detect(fileName = "war_and_peace.fb2"))
        assertEquals(BookFormat.Fb2, BookFormat.detect(fileName = "Book.FB2"))
        assertEquals(BookFormat.Epub, BookFormat.detect(fileName = "novel.epub"))
        assertEquals(BookFormat.Epub, BookFormat.detect(fileName = "Novel.EPUB"))
    }

    @Test
    fun detectsFb2FromFb2ZipFileName() {
        assertEquals(BookFormat.Fb2, BookFormat.detect(fileName = "Толстой - Война и мир.fb2.zip"))
        assertEquals(BookFormat.Fb2, BookFormat.detect(fileName = "book.FB2.ZIP"))
        assertEquals(BookFormat.Unknown, BookFormat.detect(fileName = "archive.zip"))
    }

    @Test
    fun detectsFromMimeTypes() {
        assertEquals(BookFormat.Epub, BookFormat.detect(mimeType = "application/epub+zip"))
        assertEquals(
            BookFormat.Fb2,
            BookFormat.detect(mimeType = "application/x-fictionbook+xml"),
        )
        assertEquals(BookFormat.Fb2, BookFormat.detect(mimeType = "application/x-fictionbook"))
        assertEquals(BookFormat.Text, BookFormat.detect(mimeType = "text/plain"))
        assertEquals(BookFormat.Text, BookFormat.detect(mimeType = "text/plain; charset=utf-8"))
        assertEquals(BookFormat.Text, BookFormat.detect(mimeType = "text/html"))
        assertEquals(BookFormat.Text, BookFormat.detect(mimeType = "text/markdown"))
        assertEquals(BookFormat.Fb2, BookFormat.detect(mimeType = "text/xml"))
        assertEquals(BookFormat.Fb2, BookFormat.detect(mimeType = "application/xml"))
        assertEquals(BookFormat.Unknown, BookFormat.detect(mimeType = "application/octet-stream"))
        assertEquals(BookFormat.Unknown, BookFormat.detect(mimeType = "application/zip"))
    }

    @Test
    fun octetStreamFallsBackToFileName() {
        assertEquals(
            BookFormat.Fb2,
            BookFormat.detect(fileName = "book.fb2", mimeType = "application/octet-stream"),
        )
        assertEquals(
            BookFormat.Epub,
            BookFormat.detect(fileName = "book.epub", mimeType = "application/octet-stream"),
        )
        assertEquals(
            BookFormat.Fb2,
            BookFormat.detect(fileName = "book.fb2.zip", mimeType = "application/zip"),
        )
    }

    @Test
    fun zipEntriesDistinguishEpubFromFb2Zip() {
        assertEquals(
            BookFormat.Epub,
            BookFormat.detect(
                fileName = "book.zip",
                zipEntryNames = listOf("mimetype", "META-INF/container.xml", "OEBPS/content.opf"),
            ),
        )
        assertEquals(
            BookFormat.Epub,
            BookFormat.detect(
                zipEntryNames = listOf("META-INF/container.xml"),
            ),
        )
        assertEquals(
            BookFormat.Fb2,
            BookFormat.detect(
                fileName = "download.zip",
                mimeType = "application/zip",
                zipEntryNames = listOf("Толстой - Война и мир.fb2"),
            ),
        )
        assertEquals(
            BookFormat.Unknown,
            BookFormat.detect(
                fileName = "photos.zip",
                zipEntryNames = listOf("img/cover.jpg", "readme.txt"),
            ),
        )
    }

    @Test
    fun sniffsZipAndXmlSignatures() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        assertTrue(BookFormat.isZipSignature(zipHeader))
        assertEquals(BookFormat.Unknown, BookFormat.detect(headerBytes = zipHeader))

        val epubHeader = zipHeader + "mimetypeapplication/epub+zip".encodeToByteArray()
        assertEquals(BookFormat.Epub, BookFormat.detect(headerBytes = epubHeader))

        val fb2Xml = """<?xml version="1.0" encoding="windows-1251"?><FictionBook>""".encodeToByteArray()
        assertEquals(BookFormat.Fb2, BookFormat.detect(headerBytes = fb2Xml))
        assertEquals(BookFormat.Fb2, BookFormat.detect(headerBytes = "<FictionBook xmlns=".encodeToByteArray()))
        assertEquals(BookFormat.Fb2, BookFormat.detect(headerBytes = "<p>not really html".encodeToByteArray()))

        val bomFb2 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "<?xml version=\"1.0\"?><FictionBook>".encodeToByteArray()
        assertEquals(BookFormat.Fb2, BookFormat.detect(headerBytes = bomFb2))
    }

    @Test
    fun sniffsHtmlAsTextAndUtf16Fb2() {
        assertEquals(
            BookFormat.Text,
            BookFormat.detect(headerBytes = "<!DOCTYPE html><html>".encodeToByteArray()),
        )
        assertEquals(
            BookFormat.Text,
            BookFormat.detect(headerBytes = "<html lang=\"en\">".encodeToByteArray()),
        )
        assertEquals(
            BookFormat.Text,
            BookFormat.detect(
                headerBytes = """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml">""".encodeToByteArray(),
            ),
        )

        val xml = "<?xml version=\"1.0\"?><FictionBook>"
        val utf16 = ByteArray(2 + xml.length * 2)
        utf16[0] = 0xFF.toByte()
        utf16[1] = 0xFE.toByte()
        xml.forEachIndexed { index, char ->
            utf16[2 + index * 2] = (char.code and 0xFF).toByte()
            utf16[3 + index * 2] = (char.code shr 8).toByte()
        }
        assertEquals(BookFormat.Fb2, BookFormat.detect(headerBytes = utf16))
    }

    @Test
    fun sniffingWinsWhenMimeOrExtensionLies() {
        val fb2Xml = """<?xml version="1.0"?><FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">"""
            .encodeToByteArray()
        assertEquals(
            BookFormat.Fb2,
            BookFormat.detect(
                fileName = "book.txt",
                mimeType = "text/plain",
                headerBytes = fb2Xml,
            ),
        )

        val epubHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04) +
            "mimetypeapplication/epub+zip".encodeToByteArray()
        assertEquals(
            BookFormat.Epub,
            BookFormat.detect(
                fileName = "notes.txt",
                mimeType = "text/plain",
                headerBytes = epubHeader,
            ),
        )

        assertEquals(
            BookFormat.Fb2,
            BookFormat.detect(
                fileName = "archive.zip",
                mimeType = "application/zip",
                headerBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04),
                zipEntryNames = listOf("book.fb2"),
            ),
        )
    }

    @Test
    fun unknownWhenNothingMatches() {
        assertEquals(BookFormat.Unknown, BookFormat.detect())
        assertEquals(BookFormat.Unknown, BookFormat.detect(fileName = "book"))
        assertEquals(BookFormat.Unknown, BookFormat.detect(fileName = "book.docx"))
        assertEquals(BookFormat.Unknown, BookFormat.detect(mimeType = "application/pdf"))
        assertFalse(BookFormat.isZipSignature(null))
        assertFalse(BookFormat.isZipSignature(byteArrayOf(0x3C)))
    }
}
