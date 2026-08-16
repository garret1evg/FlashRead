package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedReadTokenizerTest {

    @Test
    fun splitsParagraphsLikeTheReader() {
        val content = "First paragraph.\r\n\n  Second paragraph.  \n\n\nThird."
        val paragraphs = splitBookParagraphs(content)
        assertEquals(listOf("First paragraph.", "Second paragraph.", "Third."), paragraphs)
    }

    @Test
    fun tokenizesWordsWithParagraphIndexes() {
        val content = "Hello world.\n\nNext line here"
        val tokens = tokenizeBook(content)
        assertEquals(listOf("Hello", "world.", "Next", "line", "here"), tokens.map { it.text })
        assertEquals(listOf(0, 0, 1, 1, 1), tokens.map { it.paragraphIndex })
        assertEquals(listOf(false, true, false, false, false), tokens.map { it.isSentenceEnd })
        assertEquals(1.0, tokens[0].pauseMultiplier)
        assertEquals(2.2, tokens[1].pauseMultiplier)
    }

    @Test
    fun skipsBlankLinesAndCollapsesWhitespace() {
        val tokens = tokenizeBook("  One   two  \n\n\n  \nThree")
        assertEquals(listOf("One", "two", "Three"), tokens.map { it.text })
        assertEquals(listOf(0, 0, 1), tokens.map { it.paragraphIndex })
    }

    @Test
    fun emptyContentYieldsNoTokens() {
        assertTrue(tokenizeBook("").isEmpty())
        assertTrue(tokenizeBook(" \n\n ").isEmpty())
    }

    @Test
    fun scannerNextAndPreviousWalkTheSameTokens() {
        val content = "  One   two  \r\n\n  \nThree four.\n\nYes!"
        val expected = tokenizeBook(content)
        val source = SpeedReadSource(content)
        val forward = ArrayList<String>()
        var current = source.first()
        while (current != null) {
            forward.add(source.text.substring(current.start, current.end))
            current = source.next(current)
        }
        assertEquals(expected.map { it.text }, forward)

        val backward = ArrayList<String>()
        current = source.first()
        while (current != null) {
            val next = source.next(current)
            if (next == null) break
            current = next
        }
        while (current != null) {
            backward.add(source.text.substring(current.start, current.end))
            current = source.previous(current)
        }
        assertEquals(expected.map { it.text }.asReversed(), backward)
    }

    @Test
    fun groupsTokensByChunkSize() {
        val tokens = tokenizeBook("one two three four five six seven")
        val chunks = chunkTokens(tokens, chunkSize = 3)
        assertEquals(3, chunks.size)
        assertEquals(listOf("one", "two", "three"), chunks[0].tokens.map { it.text })
        assertEquals(listOf("four", "five", "six"), chunks[1].tokens.map { it.text })
        assertEquals(listOf("seven"), chunks[2].tokens.map { it.text })
        assertEquals(0, chunks[0].startTokenIndex)
        assertEquals(3, chunks[1].startTokenIndex)
        assertEquals(6, chunks[2].startTokenIndex)
        assertEquals("one two three", chunks[0].displayText)
    }

    @Test
    fun chunkSizeIsClampedToSupportedRange() {
        val tokens = tokenizeBook("a b c d e f")
        assertEquals(6, chunkTokens(tokens, chunkSize = 1).size)
        assertEquals(2, chunkTokens(tokens, chunkSize = 5).size)
        assertEquals(6, chunkTokens(tokens, chunkSize = 0).size)
        assertEquals(2, chunkTokens(tokens, chunkSize = 99).size)
    }
}
