package com.evgeniich.flashread.core.reading

import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.speedread.SpeedReadDefaults
import com.evgeniich.flashread.core.speedread.splitBookParagraphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookProgressTest {

    @Test
    fun wordCountIgnoresExtraWhitespace() {
        assertEquals(0, wordCount(""))
        assertEquals(0, wordCount("   \n\t"))
        assertEquals(2, wordCount("  hello   world  "))
        assertEquals(3, wordCount("one\ntwo\r\nthree"))
    }

    @Test
    fun bookProgressPercentUsesContentOffset() {
        assertEquals(0, bookProgressPercent(contentOffset = 0, contentLength = 0))
        assertEquals(0, bookProgressPercent(contentOffset = 0, contentLength = 100))
        assertEquals(50, bookProgressPercent(contentOffset = 50, contentLength = 100))
        assertEquals(100, bookProgressPercent(contentOffset = 100, contentLength = 100))
        assertEquals(100, bookProgressPercent(contentOffset = 150, contentLength = 100))
        assertEquals(0, bookProgressPercent(contentOffset = -10, contentLength = 100))
    }

    @Test
    fun withReadingStatsCachesCountsOnBook() {
        val content = "one two three\n\nfour five"
        val book = Book(
            id = "1",
            title = "Sample",
            content = content,
        ).withReadingStats()
        assertEquals(5, book.wordCount)
        assertEquals(content, book.content)
    }

    @Test
    fun paragraphStartOffsetsMatchSplitBookParagraphs() {
        val samples = listOf(
            "",
            "   \n\n  ",
            "one paragraph",
            "one\n\ntwo",
            "one\r\ntwo\r\n\r\nthree",
            "  leading  \n\n  trailing  \n",
            "a\n\n\nb",
            "\r\n  first\r\n\r\n  second  \r\n",
        )
        samples.forEach { content ->
            val paragraphs = splitBookParagraphs(content)
            val offsets = paragraphStartOffsets(content)
            assertEquals(
                paragraphs.size,
                offsets.size,
                "offset count mismatch for: $content",
            )
            offsets.forEachIndexed { index, offset ->
                assertTrue(offset in content.indices, "offset out of range for: $content")
                assertTrue(!content[offset].isWhitespace(), "offset should point at non-whitespace")
                assertEquals(
                    paragraphs[index],
                    content.substring(offset).lineContent(),
                    "offset content mismatch at $index for: $content",
                )
            }
        }
    }

    @Test
    fun normalizeParagraphsSplitsLongSentenceLineAndIsIdempotent() {
        val longLine = "Word. ".repeat(70).trimEnd()
        assertTrue(longLine.count { !it.isWhitespace() } > 0)
        assertTrue(
            longLine.split(Regex("\\s+")).size > MAX_PARAGRAPH_WORDS ||
                longLine.length > MAX_PARAGRAPH_CHARS,
        )

        val normalized = normalizeParagraphs(longLine)
        assertTrue(normalized.contains('\n'), "expected reflow into multiple lines")
        assertEquals(normalized, normalizeParagraphs(normalized))
    }

    @Test
    fun normalizeParagraphsLeavesShortLineUnchanged() {
        val short = "Hello world."
        assertEquals(short, normalizeParagraphs(short))
    }

    @Test
    fun normalizeParagraphsHardBreaksLongLineWithoutPunctuation() {
        val words = (1..80).joinToString(" ") { "w$it" }
        val normalized = normalizeParagraphs(words)
        assertTrue(normalized.lines().size > 1, "expected hard fallback to insert newlines")
        assertEquals(normalized, normalizeParagraphs(normalized))
    }

    @Test
    fun normalizeParagraphsLeavesEmptyStringEmpty() {
        assertEquals("", normalizeParagraphs(""))
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

    private fun String.lineContent(): String {
        var end = 0
        while (end < length) {
            val ch = this[end]
            if (ch == '\n') break
            if (ch == '\r' && end + 1 < length && this[end + 1] == '\n') break
            end++
        }
        return substring(0, end).trimEnd()
    }
}
