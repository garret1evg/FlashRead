package com.tool.flashread.ui.speedread

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeedReadRemainingTimeFormatTest {

    @Test
    fun formatsMinutesHoursAndLessThanAMinute() {
        assertEquals("Осталось меньше минуты", formatRemainingTime(0))
        assertEquals("Осталось около 12 мин", formatRemainingTime(12))
        assertEquals("Осталось около 1 ч", formatRemainingTime(60))
        assertEquals("Осталось около 1 ч 20 мин", formatRemainingTime(80))
        assertEquals("Осталось около 2 ч", formatRemainingTime(120))
    }
}
