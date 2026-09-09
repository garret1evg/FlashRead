package com.evgeniich.flashread.ui.speedread

import com.evgeniich.flashread.core.speedread.SpeedReadPlayerStatus
import com.evgeniich.flashread.core.speedread.orpParts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun multiWordDemoKeepsSpritzOffAndShowsAPhraseChunk() {
        val state = SpeedReadPlayerDemo.multiWord
        assertEquals("one two three four", state.text)
        assertEquals(4, state.settings.chunkSize)
        assertFalse(state.settings.isSpritzAvailable)
        assertFalse(state.settings.effectiveSpritzEnabled)
        assertEquals(SpeedReadPlayerStatus.Playing, state.status)
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
