package com.tool.flashread.core.reading

import com.tool.flashread.core.speedread.SpeedReadDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class BookProgressTest {

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
