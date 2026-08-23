package com.tool.flashread.ui.speedread

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeedReadRemainingTimeFormatTest {

    @Test
    fun formatsPlayerElapsedAndRemainingClock() {
        assertEquals("0:00", formatPlayerClock(0))
        assertEquals("0:12", formatPlayerClock(12_000))
        assertEquals("1:05", formatPlayerClock(65_000))
        assertEquals("1:02:03", formatPlayerClock(3_723_000))
    }
}
