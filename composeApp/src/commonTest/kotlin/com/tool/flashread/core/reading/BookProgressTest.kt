package com.tool.flashread.core.reading

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.splitBookParagraphs
import kotlin.test.Test
import kotlin.test.assertEquals

class BookProgressTest {

    @Test
    fun wordCountIgnoresExtraWhitespace() {
        assertEquals(0, wordCount(""))
        assertEquals(0, wordCount("   \n\t"))
        assertEquals(2, wordCount("  hello   world  "))
        assertEquals(3, wordCount("one\ntwo\r\nthree"))
    }

    @Test
    fun paragraphCountMatchesSplitBookParagraphs() {
        val samples = listOf(
            "",
            "   \n\n  ",
            "one paragraph",
            "one\n\ntwo",
            "one\r\ntwo\r\n\r\nthree",
            "  leading  \n\n  trailing  \n",
        )
        samples.forEach { content ->
            assertEquals(
                splitBookParagraphs(content).size,
                paragraphCount(content),
                "paragraphCount mismatch for: $content",
            )
        }
    }

    @Test
    fun bookProgressPercentUsesParagraphCount() {
        assertEquals(0, bookProgressPercent(paragraphIndex = 0, paragraphCount = 0))
        assertEquals(0, bookProgressPercent(paragraphIndex = 0, paragraphCount = 4))
        assertEquals(50, bookProgressPercent(paragraphIndex = 2, paragraphCount = 4))
        assertEquals(100, bookProgressPercent(paragraphIndex = 4, paragraphCount = 4))
    }

    @Test
    fun withReadingStatsCachesCountsOnBook() {
        val book = Book(
            id = "1",
            title = "Sample",
            content = "one two three\n\nfour five",
        ).withReadingStats()
        assertEquals(5, book.wordCount)
        assertEquals(2, book.paragraphCount)
    }

    @Test
    fun remainingWordCountStartsAtCurrentParagraph() {
        val content = "one two three\n\nfour five\n\nsix"
        assertEquals(6, remainingWordCount(content, 0))
        assertEquals(3, remainingWordCount(content, 1))
        assertEquals(1, remainingWordCount(content, 2))
        assertEquals(0, remainingWordCount(content, 99))
        assertEquals(0, remainingWordCount("", 0))
    }

    @Test
    fun estimatedRemainingMinutesRoundsUp() {
        assertEquals(0, estimatedRemainingMinutes(0, 250))
        assertEquals(1, estimatedRemainingMinutes(10, 400))
        assertEquals(10, estimatedRemainingMinutes(2500, 250))
        assertEquals(1, estimatedRemainingMinutes(400, 400))
        assertEquals(2, estimatedRemainingMinutes(401, 400))
    }

    @Test
    fun estimatedRemainingMinutesUsesSnappedWpm() {
        val minutes = estimatedRemainingMinutes(1000, 1012)
        val snapped = SpeedReadDefaults.snapWpm(1012)
        assertEquals(1000, snapped)
        assertEquals(1, minutes)
    }
}
