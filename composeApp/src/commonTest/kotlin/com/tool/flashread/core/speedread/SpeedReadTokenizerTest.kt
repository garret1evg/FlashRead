package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun firstWordInParagraphReturnsCorrectWord() {
        val content = "Hello world.\n\nNext line here"
        val first = firstWordInParagraph(content, 0)
        assertNotNull(first)
        assertEquals(0, first.paragraphIndex)
        assertEquals(0, first.localStart)
        assertEquals(5, first.localEnd)
        assertEquals("Hello", content.substring(first.contentOffset, first.contentOffset + 5))

        val second = firstWordInParagraph(content, 1)
        assertNotNull(second)
        assertEquals(1, second.paragraphIndex)
        assertEquals(0, second.localStart)
        assertEquals(4, second.localEnd)
        assertEquals("Next", content.substring(second.contentOffset, second.contentOffset + 4))
    }

    @Test
    fun firstWordInParagraphWithLeadingWhitespace() {
        val content = "  First word\n\n  Second word"
        val paragraphs = splitBookParagraphs(content)
        assertEquals("First word", paragraphs[0])
        assertEquals("Second word", paragraphs[1])

        val first = firstWordInParagraph(content, 0)
        assertNotNull(first)
        assertEquals(0, first.localStart)
        assertEquals(5, first.localEnd)

        val second = firstWordInParagraph(content, 1)
        assertNotNull(second)
        assertEquals(0, second.localStart)
        assertEquals(6, second.localEnd)
    }

    @Test
    fun firstWordInParagraphReturnsNullForMissingParagraph() {
        val content = "Only one paragraph"
        assertNull(firstWordInParagraph(content, 1))
        assertNull(firstWordInParagraph("", 0))
    }

    @Test
    fun wordAtParagraphOffsetClickInMiddleOfWord() {
        val content = "Hello world"
        val location = wordAtParagraphOffset(content, 0, 2)
        assertNotNull(location)
        assertEquals(0, location.localStart)
        assertEquals(5, location.localEnd)
        assertEquals("Hello", content.substring(location.contentOffset, location.contentOffset + 5))
    }

    @Test
    fun wordAtParagraphOffsetClickInWhitespace() {
        val content = "Hello world"
        val location = wordAtParagraphOffset(content, 0, 5)
        assertNotNull(location)
        assertEquals(6, location.localStart)
        assertEquals(11, location.localEnd)
        assertEquals("world", content.substring(location.contentOffset, location.contentOffset + 5))
    }

    @Test
    fun wordAtParagraphOffsetClickAtEndReturnsLastWord() {
        val content = "Hello world"
        val location = wordAtParagraphOffset(content, 0, 20)
        assertNotNull(location)
        assertEquals(6, location.localStart)
        assertEquals(11, location.localEnd)
    }

    @Test
    fun wordHighlightAtContentOffsetFindsWord() {
        val content = "Hello world.\n\nNext line"
        val tokens = tokenizeBook(content)
        assertEquals(listOf("Hello", "world.", "Next", "line"), tokens.map { it.text })

        val helloOffset = 0
        val hello = wordHighlightAtContentOffset(content, helloOffset)
        assertNotNull(hello)
        assertEquals(0, hello.paragraphIndex)
        assertEquals(0, hello.localStart)
        assertEquals(5, hello.localEnd)

        val nextOffset = content.indexOf("Next")
        val next = wordHighlightAtContentOffset(content, nextOffset)
        assertNotNull(next)
        assertEquals(1, next.paragraphIndex)
        assertEquals(0, next.localStart)
        assertEquals(4, next.localEnd)
    }

    @Test
    fun wordHighlightAtContentOffsetInWhitespaceReturnsNextWord() {
        val content = "Hello world"
        val location = wordHighlightAtContentOffset(content, 5)
        assertNotNull(location)
        assertEquals(6, location.localStart)
        assertEquals(11, location.localEnd)
    }

    @Test
    fun wordHighlightAtContentOffsetHandlesEdgeCases() {
        assertNull(wordHighlightAtContentOffset("Hello", -1))
        assertNull(wordHighlightAtContentOffset("Hello", 100))
        assertNull(wordHighlightAtContentOffset("", 0))
    }

    @Test
    fun localRangesMatchDisplayedTrimmedText() {
        val content = "  Hello   world  \n\n  Next  "
        val paragraphs = splitBookParagraphs(content)
        assertEquals("Hello   world", paragraphs[0])
        assertEquals("Next", paragraphs[1])

        val hello = firstWordInParagraph(content, 0)
        assertNotNull(hello)
        assertEquals("Hello", paragraphs[0].substring(hello.localStart, hello.localEnd))

        val next = firstWordInParagraph(content, 1)
        assertNotNull(next)
        assertEquals("Next", paragraphs[1].substring(next.localStart, next.localEnd))
    }
}
