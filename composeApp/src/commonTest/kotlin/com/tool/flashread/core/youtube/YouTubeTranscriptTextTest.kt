package com.tool.flashread.core.youtube

import com.tool.flashread.core.speedread.splitBookParagraphs
import kotlin.test.Test
import kotlin.test.assertEquals

class YouTubeTranscriptTextTest {

    @Test
    fun collapsesNewlinesAndSplitsOnSentences() {
        val text = YouTubeTranscriptText.join(
            listOf(
                "Hello\nworld.",
                "  Next line!  ",
                "Trailing",
            ),
        )
        assertEquals(
            listOf("Hello world.", "Next line!", "Trailing"),
            splitBookParagraphs(text),
        )
    }

    @Test
    fun keepsClosingQuotesWithTheSentence() {
        val text = YouTubeTranscriptText.join(listOf("He said \"Go.\" Then we left."))
        assertEquals(
            listOf("He said \"Go.\"", "Then we left."),
            splitBookParagraphs(text),
        )
    }

    @Test
    fun skipsEmptySnippets() {
        assertEquals("Only this.", YouTubeTranscriptText.join(listOf("  ", "\n", "Only this.")))
        assertEquals("", YouTubeTranscriptText.join(listOf("", "  ")))
    }
}
