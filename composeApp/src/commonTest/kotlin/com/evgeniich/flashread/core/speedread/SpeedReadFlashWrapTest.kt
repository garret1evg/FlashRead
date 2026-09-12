package com.evgeniich.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeedReadFlashWrapTest {

    @Test
    fun keepsSingleLineWhenTextFits() {
        assertEquals(
            "one two three",
            wrapFlashText("one two three", maxWidthPx = 100, measureWidthPx = { it.length }),
        )
    }

    @Test
    fun wrapsAtLastFittingWordBoundary() {
        assertEquals(
            "one two\nthree",
            wrapFlashText("one two three", maxWidthPx = 8, measureWidthPx = { it.length }),
        )
    }

    @Test
    fun putsRemainderOnSecondLineEvenIfItOverflows() {
        assertEquals(
            "aa bb\ncc dd",
            wrapFlashText("aa bb cc dd", maxWidthPx = 5, measureWidthPx = { it.length }),
        )
    }

    @Test
    fun splitsLongSingleWordByCharacters() {
        // "superc" with maxWidth=3 splits into "sup" (3 chars) + "erc" (3 chars)
        assertEquals(
            "sup\nerc",
            wrapFlashText("superc", maxWidthPx = 3, measureWidthPx = { it.length }),
        )
    }

    @Test
    fun splitsVeryLongWordToMultipleLines() {
        // "abcdefghij" (10 chars) with maxWidth=4, maxLines=3
        // Line 1: "abcd" (4 chars), Line 2: "efgh" (4 chars), Line 3: "ij" (2 chars)
        assertEquals(
            "abcd\nefgh\nij",
            wrapFlashText("abcdefghij", maxWidthPx = 4, measureWidthPx = { it.length }, maxLines = 3),
        )
    }

    @Test
    fun singleWordFitsNoSplit() {
        assertEquals(
            "hello",
            wrapFlashText("hello", maxWidthPx = 10, measureWidthPx = { it.length }),
        )
    }

    @Test
    fun returnsEmptyAndZeroWidthUnchanged() {
        assertEquals("", wrapFlashText("", maxWidthPx = 10, measureWidthPx = { it.length }))
        assertEquals(
            "one two",
            wrapFlashText("one two", maxWidthPx = 0, measureWidthPx = { it.length }),
        )
    }
}
