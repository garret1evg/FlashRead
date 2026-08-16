package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedReadTimingTest {

    @Test
    fun commaAndSemicolonUseOnePointFiveMultiplier() {
        assertEquals(1.5, SpeedReadTiming.pauseMultiplier("wait,"), absoluteTolerance = 1e-9)
        assertEquals(1.5, SpeedReadTiming.pauseMultiplier("pause;"), absoluteTolerance = 1e-9)
        assertEquals(1.5, SpeedReadTiming.pauseMultiplier("quoted,\""), absoluteTolerance = 1e-9)
    }

    @Test
    fun sentenceEndPunctuationUsesTwoPointTwoMultiplier() {
        assertEquals(2.2, SpeedReadTiming.pauseMultiplier("Done."), absoluteTolerance = 1e-9)
        assertEquals(2.2, SpeedReadTiming.pauseMultiplier("Wow!"), absoluteTolerance = 1e-9)
        assertEquals(2.2, SpeedReadTiming.pauseMultiplier("Really?"), absoluteTolerance = 1e-9)
        assertEquals(2.2, SpeedReadTiming.pauseMultiplier("Wait..."), absoluteTolerance = 1e-9)
        assertTrue(SpeedReadTiming.isSentenceEnd("Done."))
        assertTrue(SpeedReadTiming.isSentenceEnd("Wow!"))
        assertTrue(SpeedReadTiming.isSentenceEnd("Really?"))
    }

    @Test
    fun plainWordHasBaseMultiplier() {
        assertEquals(1.0, SpeedReadTiming.pauseMultiplier("word"), absoluteTolerance = 1e-9)
        assertEquals(false, SpeedReadTiming.isSentenceEnd("word"))
    }

    @Test
    fun longWordsGetSlightlyLongerPause() {
        val eight = "alphabet"
        assertEquals(8, eight.count { it.isLetterOrDigit() })
        assertEquals(1.0, SpeedReadTiming.pauseMultiplier(eight), absoluteTolerance = 1e-9)

        val nine = "something"
        assertEquals(9, nine.count { it.isLetterOrDigit() })
        assertEquals(1.05, SpeedReadTiming.pauseMultiplier(nine), absoluteTolerance = 1e-9)

        val withPeriod = "extraordinary!"
        val expected = 2.2 + (13 - 8) * 0.05
        assertEquals(expected, SpeedReadTiming.pauseMultiplier(withPeriod), absoluteTolerance = 1e-9)
    }

    @Test
    fun delayMsFollowsWpmAndMultiplier() {
        assertEquals(200L, SpeedReadTiming.delayMs(wpm = 300, multiplier = 1.0))
        assertEquals(300L, SpeedReadTiming.delayMs(wpm = 300, multiplier = 1.5))
        assertEquals(440L, SpeedReadTiming.delayMs(wpm = 300, multiplier = 2.2))
        assertEquals(75L, SpeedReadTiming.delayMs(wpm = 800, multiplier = 1.0))
        assertEquals(600L, SpeedReadTiming.delayMs(wpm = 100, multiplier = 1.0))
    }

    @Test
    fun chunkDelaySumsTokenDelays() {
        val chunk = SpeedReadChunk(
            tokens = listOf(
                SpeedReadToken("The", 0, 1.0, false),
                SpeedReadToken("end.", 0, 2.2, true),
            ),
            startTokenIndex = 0,
        )
        assertEquals(200L + 440L, SpeedReadTiming.chunkDelayMs(chunk, wpm = 300))
    }
}
