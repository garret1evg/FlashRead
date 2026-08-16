package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeedReadSettingsTest {

    @Test
    fun defaultsMatchSupportedRanges() {
        val settings = SpeedReadSettings()
        assertEquals(SpeedReadDefaults.DEFAULT_WPM, settings.wpm)
        assertEquals(SpeedReadDefaults.DEFAULT_CHUNK_SIZE, settings.chunkSize)
        assertTrue(settings.spritzEnabled)
        assertFalse(settings.loopEnabled)
        assertEquals(settings, settings.normalized())
    }

    @Test
    fun normalizedClampsWpmAndChunkSize() {
        val tooLow = SpeedReadSettings(wpm = 50, chunkSize = 0).normalized()
        assertEquals(SpeedReadDefaults.MIN_WPM, tooLow.wpm)
        assertEquals(SpeedReadDefaults.MIN_CHUNK_SIZE, tooLow.chunkSize)

        val tooHigh = SpeedReadSettings(wpm = 1200, chunkSize = 9).normalized()
        assertEquals(SpeedReadDefaults.MAX_WPM, tooHigh.wpm)
        assertEquals(SpeedReadDefaults.MAX_CHUNK_SIZE, tooHigh.chunkSize)
    }

    @Test
    fun normalizedSnapsWpmToTwentyFiveStep() {
        assertEquals(100, SpeedReadSettings(wpm = 100).normalized().wpm)
        assertEquals(1000, SpeedReadSettings(wpm = 1000).normalized().wpm)
        assertEquals(325, SpeedReadSettings(wpm = 317).normalized().wpm)
        assertEquals(300, SpeedReadSettings(wpm = 312).normalized().wpm)
        assertEquals(250, SpeedReadSettings(wpm = 240).normalized().wpm)
    }

    @Test
    fun snapWpmAndPresetsStayInsideSupportedRange() {
        SpeedReadDefaults.WPM_PRESETS.forEach { preset ->
            assertEquals(preset, SpeedReadDefaults.snapWpm(preset))
        }
        assertEquals(4, SpeedReadDefaults.MAX_CHUNK_SIZE)
        assertEquals(1000, SpeedReadDefaults.MAX_WPM)
        assertEquals(25, SpeedReadDefaults.WPM_STEP)
    }

    @Test
    fun normalizedPreservesFlags() {
        val settings = SpeedReadSettings(
            spritzEnabled = false,
            loopEnabled = true,
        ).normalized()
        assertFalse(settings.spritzEnabled)
        assertTrue(settings.loopEnabled)
    }
}
