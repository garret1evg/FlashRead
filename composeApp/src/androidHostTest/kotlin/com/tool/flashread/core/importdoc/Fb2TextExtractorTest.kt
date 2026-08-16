package com.tool.flashread.core.importdoc

import com.tool.flashread.core.speedread.splitBookParagraphs
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class Fb2TextExtractorTest {

    @Test
    fun extractsTitleAndSingleParagraph() {
        val fb2 = fictionBook(
            title = "War and Peace",
            body = "<p>Hello world.</p>",
        )
        val book = Fb2TextExtractor.extract(fb2.byteInputStream())
        assertEquals("War and Peace", book.title)
        assertEquals("Hello world.", book.content)
        assertNull(book.coverBytes)
    }

    @Test
    fun flattensInlineMarkupInsideParagraphs() {
        val fb2 = fictionBook(
            title = "Marked",
            body = "<p>Hello <emphasis>world</emphasis> and <strong>more</strong>!</p>",
        )
        val book = Fb2TextExtractor.extract(fb2.byteInputStream())
        assertEquals("Hello world and more!", book.content)
    }

    @Test
    fun extractsTitleSubtitleVerseAndSkipsEmptyLines() {
        val fb2 = fictionBook(
            title = "Poems",
            body = """
                <title>
                    <p>Part One</p>
                    <p>1805</p>
                </title>
                <subtitle>A note</subtitle>
                <empty-line/>
                <poem>
                    <stanza>
                        <v>First verse</v>
                        <v>Second verse</v>
                    </stanza>
                </poem>
                <p>Closing.</p>
            """.trimIndent(),
        )
        val book = Fb2TextExtractor.extract(fb2.byteInputStream())
        assertEquals(
            "Part One\n1805\nA note\nFirst verse\nSecond verse\nClosing.",
            book.content,
        )
        assertEquals(
            listOf("Part One", "1805", "A note", "First verse", "Second verse", "Closing."),
            splitBookParagraphs(book.content),
        )
    }

    @Test
    fun usesDirectTitleTextWhenThereAreNoInnerParagraphs() {
        val fb2 = fictionBook(
            title = "Direct",
            body = "<title>Chapter <emphasis>One</emphasis></title><p>Body</p>",
        )
        val book = Fb2TextExtractor.extract(fb2.byteInputStream())
        assertEquals("Chapter One\nBody", book.content)
    }

    @Test
    fun ignoresAnnotationAndBinaryPayload() {
        val fb2 = """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="http://www.w3.org/1999/xlink">
              <description>
                <title-info>
                  <book-title>Visible title</book-title>
                  <annotation>
                    <p>Do not import this.</p>
                  </annotation>
                </title-info>
              </description>
              <body>
                <p>Only body text.</p>
                <p>Image <image l:href="#cover.jpg"/> here.</p>
              </body>
              <binary id="cover.jpg" content-type="image/jpeg">YmFzZTY0c2hvdWxkbm90YXBwZWFy</binary>
            </FictionBook>
        """.trimIndent()
        val book = Fb2TextExtractor.extract(fb2.byteInputStream())
        assertEquals("Visible title", book.title)
        assertEquals("Only body text.\nImage here.", book.content)
        assertContentEquals(java.util.Base64.getDecoder().decode("YmFzZTY0c2hvdWxkbm90YXBwZWFy"), book.coverBytes)
        assertEquals("image/jpeg", book.coverMimeType)
    }

    @Test
    fun prefersFirstBookTitleAndAllowsMissingTitle() {
        val withSrcTitle = """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook>
              <description>
                <title-info><book-title>Translated</book-title></title-info>
                <src-title-info><book-title>Original</book-title></src-title-info>
              </description>
              <body><p>Text</p></body>
            </FictionBook>
        """.trimIndent()
        assertEquals("Translated", Fb2TextExtractor.extract(withSrcTitle.byteInputStream()).title)

        val untitled = fictionBook(title = null, body = "<p>Just text</p>")
        val book = Fb2TextExtractor.extract(untitled.byteInputStream())
        assertNull(book.title)
        assertEquals("Just text", book.content)
    }

    @Test
    fun decodesWindows1251FromXmlDeclaration() {
        val charset = Charset.forName("windows-1251")
        val xml = """
            <?xml version="1.0" encoding="windows-1251"?>
            <FictionBook>
              <description>
                <title-info><book-title>Заголовок</book-title></title-info>
              </description>
              <body>
                <p>Привет мир</p>
              </body>
            </FictionBook>
        """.trimIndent()
        val garbledIfUtf8 = String(xml.toByteArray(charset), Charsets.UTF_8)
        assert(garbledIfUtf8 != xml) { "Fixture must not be valid UTF-8 Russian text." }

        val book = Fb2TextExtractor.extract(xml.toByteArray(charset).inputStream())
        assertEquals("Заголовок", book.title)
        assertEquals("Привет мир", book.content)
    }

    @Test
    fun extractsFb2FromZipArchive() {
        val xml = fictionBook(title = "Zipped", body = "<p>From archive.</p>")
        val zip = zipBytes("story.fb2" to xml.toByteArray())
        val book = Fb2TextExtractor.extract(zip.inputStream())
        assertEquals("Zipped", book.title)
        assertEquals("From archive.", book.content)
    }

    @Test
    fun findsFb2InsideNestedZipFolderAndSkipsMacJunk() {
        val xml = fictionBook(title = "Nested", body = "<p>Inside.</p>")
        val zip = zipBytes(
            "__MACOSX/._book.fb2" to byteArrayOf(0x00, 0x01),
            "books/tolstoy.fb2" to xml.toByteArray(),
        )
        val book = Fb2TextExtractor.extract(zip.inputStream())
        assertEquals("Nested", book.title)
        assertEquals("Inside.", book.content)
    }

    @Test
    fun extractsWindows1251Fb2FromZip() {
        val charset = Charset.forName("windows-1251")
        val xml = """
            <?xml version="1.0" encoding="windows-1251"?>
            <FictionBook>
              <description>
                <title-info><book-title>Архив</book-title></title-info>
              </description>
              <body><p>Текст</p></body>
            </FictionBook>
        """.trimIndent()
        val zip = zipBytes("book.fb2" to xml.toByteArray(charset))
        val book = Fb2TextExtractor.extract(zip.inputStream())
        assertEquals("Архив", book.title)
        assertEquals("Текст", book.content)
    }

    @Test
    fun rejectsZipWithoutFb2Entry() {
        val zip = zipBytes("readme.txt" to "not a book".toByteArray())
        val error = assertFailsWith<IllegalStateException> {
            Fb2TextExtractor.extract(zip.inputStream())
        }
        assertEquals("ZIP archive does not contain an FB2 file.", error.message)
    }

    @Test
    fun collapsesParagraphWhitespace() {
        val fb2 = fictionBook(
            title = "Space",
            body = """
                <p>
                    Hello
                    world
                </p>
            """.trimIndent(),
        )
        assertEquals("Hello world", Fb2TextExtractor.extract(fb2.byteInputStream()).content)
    }

    @Test
    fun extractsCoverFromCoverpageBinary() {
        val cover = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val encoded = java.util.Base64.getEncoder().encodeToString(cover)
        val fb2 = """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="http://www.w3.org/1999/xlink">
              <description>
                <title-info>
                  <book-title>Covered</book-title>
                  <coverpage>
                    <image l:href="#front.png"/>
                  </coverpage>
                </title-info>
              </description>
              <body><p>Story.</p></body>
              <binary id="other.jpg" content-type="image/jpeg">aaaa</binary>
              <binary id="front.png" content-type="image/png">$encoded</binary>
            </FictionBook>
        """.trimIndent()
        val book = Fb2TextExtractor.extract(fb2.byteInputStream())
        assertEquals("Covered", book.title)
        assertEquals("Story.", book.content)
        assertContentEquals(cover, book.coverBytes)
        assertEquals("image/png", book.coverMimeType)
    }
}

private fun fictionBook(title: String?, body: String): String {
    val description = if (title == null) {
        ""
    } else {
        """
        <description>
          <title-info>
            <book-title>$title</book-title>
          </title-info>
        </description>
        """.trimIndent()
    }
    return buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="http://www.w3.org/1999/xlink">""")
        if (description.isNotEmpty()) {
            appendLine(description)
        }
        appendLine("<body>")
        appendLine(body.trimIndent())
        appendLine("</body>")
        append("</FictionBook>")
    }
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
