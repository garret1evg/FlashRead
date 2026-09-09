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
    fun doesNotSplitASingleWord() {
        assertEquals(
            "supercalifragilistic",
            wrapFlashText("supercalifragilistic", maxWidthPx = 3, measureWidthPx = { it.length }),
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
