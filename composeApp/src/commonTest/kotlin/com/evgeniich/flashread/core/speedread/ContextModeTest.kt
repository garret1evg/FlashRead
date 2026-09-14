package com.evgeniich.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextModeTest {

    @Test
    fun offNeverShowsOrExtracts() {
        assertFalse(ContextMode.Off.shouldShowContext(isPlaying = false))
        assertFalse(ContextMode.Off.shouldShowContext(isPlaying = true))
        assertFalse(ContextMode.Off.shouldExtractContext(isPlaying = false))
        assertFalse(ContextMode.Off.shouldExtractContext(isPlaying = true))
    }

    @Test
    fun whenPausedShowsOnlyWhileNotPlaying() {
        assertTrue(ContextMode.WhenPaused.shouldShowContext(isPlaying = false))
        assertTrue(ContextMode.WhenPaused.shouldExtractContext(isPlaying = false))
        assertFalse(ContextMode.WhenPaused.shouldShowContext(isPlaying = true))
        assertFalse(ContextMode.WhenPaused.shouldExtractContext(isPlaying = true))
    }

    @Test
    fun alwaysShowsAndExtractsRegardlessOfPlayback() {
        assertTrue(ContextMode.Always.shouldShowContext(isPlaying = false))
        assertTrue(ContextMode.Always.shouldShowContext(isPlaying = true))
        assertTrue(ContextMode.Always.shouldExtractContext(isPlaying = false))
        assertTrue(ContextMode.Always.shouldExtractContext(isPlaying = true))
    }
}
