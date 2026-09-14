package com.evgeniich.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeedReadContextExtractorTest {

    @Test
    fun previousAndFollowingExcludeTheActiveToken() {
        val content = "alpha bravo charlie delta echo"
        val window = extractAt(content, "charlie")

        assertEquals("alpha bravo", window.beforeText)
        assertEquals("delta echo", window.afterText)
        assertFalse("charlie" in window.beforeText)
        assertFalse("charlie" in window.afterText)
    }

    @Test
    fun chunkSizeGreaterThanOneExcludesTheWholeActiveGroup() {
        val content = "one two three four five six seven"
        val window = extractAt(content, "three", chunkSize = 3)

        assertEquals("one two", window.beforeText)
        assertEquals("six seven", window.afterText)
        val combined = "${window.beforeText} ${window.afterText}"
        assertFalse("three" in combined)
        assertFalse("four" in combined)
        assertFalse("five" in combined)
    }

    @Test
    fun packsWholeWordsOntoTwoMeasuredLines() {
        // Each "xx" is 2 chars; "xx xx" is 5. maxWidth=5 fits two words per line.
        val content = "aa bb cc dd ee ACTIVE ff gg hh ii jj"
        val window = extractAt(content, "ACTIVE", maxWidthPx = 5)

        assertEquals("bb cc\ndd ee", window.beforeText)
        assertEquals("ff gg\nhh ii", window.afterText)
    }

    @Test
    fun trimsDistantStartOfPreviousAndDistantEndOfFollowing() {
        val content = "aa bb cc dd ee ACTIVE ff gg hh ii jj"
        val window = extractAt(content, "ACTIVE", maxWidthPx = 5)

        assertFalse(window.beforeText.startsWith("aa"))
        assertFalse(window.afterText.endsWith("jj"))
        assertTrue(window.beforeText.endsWith("ee"))
        assertTrue(window.afterText.startsWith("ff"))
    }

    @Test
    fun startOfContentHasNoPreviousContext() {
        val content = "one two three four"
        val window = extractAt(content, "one")

        assertEquals("", window.beforeText)
        assertEquals("two three four", window.afterText)
    }

    @Test
    fun endOfContentHasNoFollowingContext() {
        val content = "one two three four"
        val window = extractAt(content, "four")

        assertEquals("one two three", window.beforeText)
        assertEquals("", window.afterText)
    }

    @Test
    fun preservesPunctuationAttachedToTokens() {
        val content = "Hello world. Next, yes!"
        val window = extractAt(content, "world.")

        assertEquals("Hello", window.beforeText)
        assertEquals("Next, yes!", window.afterText)
    }

    @Test
    fun emptyOrInvalidInputYieldsEmptyStrings() {
        val emptyPosition = SpeedReadPosition.Empty
        assertEquals(
            ContextWindow("", ""),
            extractSpeedReadContext("", emptyPosition, 1, 100, ::measureByLength),
        )
        assertEquals(
            ContextWindow("", ""),
            extractSpeedReadContext(" \n\n ", emptyPosition, 1, 100, ::measureByLength),
        )

        val content = "hello world"
        val valid = SpeedReadPlayback(content).startPosition(0)
        assertEquals(
            ContextWindow("", ""),
            extractSpeedReadContext(content, valid, 1, maxWidthPx = 0, ::measureByLength),
        )
        val spaceOffset = content.indexOf(' ')
        assertEquals(
            ContextWindow("", ""),
            extractSpeedReadContext(
                content,
                SpeedReadPosition(tokenIndex = 0, offset = spaceOffset, paragraphIndex = 0),
                chunkSize = 1,
                maxWidthPx = 100,
                measureWidthPx = ::measureByLength,
            ),
        )
    }

    @Test
    fun overflowingWordOccupiesItsLineWithoutSplitting() {
        val content = "aa SUPERLONGWORD bb ACTIVE LONGAFTER cc"
        val window = extractAt(content, "ACTIVE", maxWidthPx = 5)

        assertEquals("SUPERLONGWORD\nbb", window.beforeText)
        assertEquals("LONGAFTER\ncc", window.afterText)
    }

    @Test
    fun contextWalksAcrossParagraphsAndSkipsBlankLines() {
        val content = "alpha bravo\n\ncharlie delta\n\necho foxtrot"
        val window = extractAt(content, "delta")

        assertEquals("alpha bravo charlie", window.beforeText)
        assertEquals("echo foxtrot", window.afterText)
        assertFalse("delta" in window.beforeText)
        assertFalse("delta" in window.afterText)
    }

    @Test
    fun lastChunkShorterThanChunkSizeHasNoFollowingContext() {
        val content = "one two three four five"
        val window = extractAt(content, "four", chunkSize = 3)

        assertEquals("one two three", window.beforeText)
        assertEquals("", window.afterText)
        val combined = "${window.beforeText} ${window.afterText}"
        assertFalse("four" in combined)
        assertFalse("five" in combined)
    }

    @Test
    fun packsASingleLineWhenMaxLinesIsOne() {
        val content = "aa bb cc dd ee ACTIVE ff gg hh ii jj"
        val window = extractAt(content, "ACTIVE", maxWidthPx = 5, maxLines = 1)

        assertEquals("dd ee", window.beforeText)
        assertEquals("ff gg", window.afterText)
        assertFalse('\n' in window.beforeText)
        assertFalse('\n' in window.afterText)
    }

    private fun extractAt(
        content: String,
        word: String,
        chunkSize: Int = 1,
        maxWidthPx: Int = 1000,
        maxLines: Int = 2,
    ): ContextWindow {
        val offset = content.indexOf(word)
        val position = SpeedReadPlayback(content, chunkSize).positionAtOffset(offset)
        return extractSpeedReadContext(
            content = content,
            position = position,
            chunkSize = chunkSize,
            maxWidthPx = maxWidthPx,
            measureWidthPx = ::measureByLength,
            maxLines = maxLines,
        )
    }

    private fun measureByLength(text: String): Int = text.length
}
