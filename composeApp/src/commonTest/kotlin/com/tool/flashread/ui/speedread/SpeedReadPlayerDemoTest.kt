package com.tool.flashread.ui.speedread

import com.tool.flashread.core.speedread.SpeedReadPlayerStatus
import com.tool.flashread.core.speedread.orpParts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedReadPlayerDemoTest {

    @Test
    fun longWordDemoKeepsClassicOrpLetter() {
        val parts = orpParts(SpeedReadPlayerDemo.longWord.text)
        assertEquals("supercalifragilistic", SpeedReadPlayerDemo.longWord.text)
        assertEquals("r", parts.pivot)
        assertEquals(SpeedReadPlayerStatus.Playing, SpeedReadPlayerDemo.longWord.status)
    }

    @Test
    fun multiWordDemoHighlightsALetterInsideTheGroup() {
        val parts = orpParts(SpeedReadPlayerDemo.multiWord.text)
        assertEquals("one two three", SpeedReadPlayerDemo.multiWord.text)
        assertEquals(3, SpeedReadPlayerDemo.multiWord.settings.chunkSize)
        assertTrue(parts.pivot.single().isLetter())
        assertEquals(SpeedReadPlayerStatus.Playing, SpeedReadPlayerDemo.multiWord.status)
    }

    @Test
    fun pausedDemoKeepsTheCurrentWord() {
        assertEquals("wait,", SpeedReadPlayerDemo.paused.text)
        assertEquals(SpeedReadPlayerStatus.Paused, SpeedReadPlayerDemo.paused.status)
        assertTrue(SpeedReadPlayerDemo.paused.progress in 0f..1f)
    }

    @Test
    fun finishedDemoIsComplete() {
        assertEquals("Done.", SpeedReadPlayerDemo.finished.text)
        assertEquals(SpeedReadPlayerStatus.Finished, SpeedReadPlayerDemo.finished.status)
        assertEquals(1f, SpeedReadPlayerDemo.finished.progress)
        assertEquals(0L, SpeedReadPlayerDemo.finished.remainingMs)
    }
}
