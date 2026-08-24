package com.evgeniich.flashread.core.youtube

import com.evgeniich.flashread.core.speedread.splitBookParagraphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YouTubeCaptionXmlTest {

    @Test
    fun parsesTextNodesAndIgnoresMissingDur() {
        val xml = """
            <transcript>
              <text start="0.5">Hello</text>
              <text start="1.2" dur="2.0">world</text>
              <text start="4"></text>
              <text start="5" dur="1"/>
            </transcript>
        """.trimIndent()
        val snippets = YouTubeCaptionXml.parse(xml)
        assertEquals(2, snippets.size)
        assertEquals("Hello", snippets[0].text)
        assertEquals(0.5, snippets[0].start)
        assertEquals(0.0, snippets[0].duration)
        assertEquals("world", snippets[1].text)
        assertEquals(2.0, snippets[1].duration)
    }

    @Test
    fun decodesEntitiesAndStripsInnerTags() {
        val xml = """
            <transcript>
              <text start="0">Tom &amp; Jerry &lt;3</text>
              <text start="1">He said &quot;hi&quot; &apos;there&apos;</text>
              <text start="2">a&#160;b</text>
              <text start="3">Hello <font>nested</font> tags</text>
            </transcript>
        """.trimIndent()
        val snippets = YouTubeCaptionXml.parse(xml)
        assertEquals("Tom & Jerry <3", snippets[0].text)
        assertEquals("He said \"hi\" 'there'", snippets[1].text)
        assertEquals("a\u00A0b", snippets[2].text)
        assertEquals("Hello nested tags", snippets[3].text)
    }

    @Test
    fun joinsCaptionsIntoReaderParagraphs() {
        val xml = """
            <transcript>
              <text start="0">Hello
              world.</text>
              <text start="1">This is next!</text>
              <text start="2">And a question?</text>
            </transcript>
        """.trimIndent()
        val text = YouTubeCaptionXml.toReaderText(xml)
        assertEquals(
            listOf("Hello world.", "This is next!", "And a question?"),
            splitBookParagraphs(text),
        )
    }
}
