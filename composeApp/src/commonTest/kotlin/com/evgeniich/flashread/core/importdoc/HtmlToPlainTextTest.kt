package com.evgeniich.flashread.core.importdoc

import com.evgeniich.flashread.core.speedread.splitBookParagraphs
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlToPlainTextTest {

    @Test
    fun stripsInlineTagsAndKeepsText() {
        assertEquals(
            "Hello world!",
            HtmlToPlainText.convert("<p>Hello <em>world</em>!</p>"),
        )
        assertEquals(
            "bold and italic",
            HtmlToPlainText.convert("bold <strong>and</strong> <span class=\"i\">italic</span>"),
        )
    }

    @Test
    fun splitsParagraphsOnBlockTags() {
        val html = """
            <h1>Title</h1>
            <p>First paragraph.</p>
            <div>Second paragraph.</div>
            <p>Line one<br/>Line two</p>
            <h2>Chapter</h2>
            <p>Body</p>
        """.trimIndent()
        assertEquals(
            "Title\nFirst paragraph.\nSecond paragraph.\nLine one\nLine two\nChapter\nBody",
            HtmlToPlainText.convert(html),
        )
    }

    @Test
    fun decodesNamedAndNumericEntities() {
        assertEquals(
            "Tom & Jerry <3",
            HtmlToPlainText.convert("<p>Tom &amp; Jerry &lt;3</p>"),
        )
        assertEquals(
            "quotes \u201Ctext\u201D",
            HtmlToPlainText.convert("<p>quotes &ldquo;text&rdquo;</p>"),
        )
        assertEquals(
            "a\u00A0b",
            HtmlToPlainText.convert("<p>a&nbsp;b</p>"),
        )
        assertEquals(
            "A",
            HtmlToPlainText.convert("<p>&#65;</p>"),
        )
        assertEquals(
            "A",
            HtmlToPlainText.convert("<p>&#x41;</p>"),
        )
        assertEquals(
            "a\u00A0b",
            HtmlToPlainText.convert("<p>a&#160;b</p>"),
        )
        assertEquals(
            "\u00AB\u00BB",
            HtmlToPlainText.convert("<p>&laquo;&raquo;</p>"),
        )
    }

    @Test
    fun skipsScriptStyleHeadAndComments() {
        val html = """
            <html>
            <head><title>Hidden</title><style>p{color:red}</style></head>
            <body>
            <!-- ignore me -->
            <p>Visible</p>
            <script>alert("no")</script>
            </body>
            </html>
        """.trimIndent()
        assertEquals("Visible", HtmlToPlainText.convert(html))
    }

    @Test
    fun collapsesSourceWhitespaceButKeepsParagraphs() {
        val html = """
            <p>
                Hello
                world
            </p>
            <p>  Next   one  </p>
        """.trimIndent()
        assertEquals("Hello world\nNext one", HtmlToPlainText.convert(html))
    }

    @Test
    fun ignoresEmptyBlocksAndSelfClosingBreaks() {
        assertEquals(
            "Only this",
            HtmlToPlainText.convert("<p></p><div>  </div><br/><p>Only this</p>"),
        )
    }

    @Test
    fun handlesNamespacedTagsAndCdata() {
        assertEquals(
            "Chapter text",
            HtmlToPlainText.convert("<xhtml:p>Chapter <![CDATA[text]]></xhtml:p>"),
        )
    }

    @Test
    fun outputIsOneParagraphPerLineForTokenizer() {
        val text = HtmlToPlainText.convert(
            "<h1>War</h1><p>Peace.</p><p>And more.</p>",
        )
        assertEquals(listOf("War", "Peace.", "And more."), splitBookParagraphs(text))
    }

    @Test
    fun leavesBareAmpersandAndLessThanAsText() {
        assertEquals("1 < 2 & 3", HtmlToPlainText.convert("1 < 2 & 3"))
        assertEquals("&unknown;", HtmlToPlainText.convert("&unknown;"))
    }
}
