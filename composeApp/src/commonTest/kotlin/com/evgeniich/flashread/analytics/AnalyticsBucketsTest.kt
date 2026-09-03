package com.evgeniich.flashread.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnalyticsBucketsTest {

    @Test
    fun progressReturnsHighestReachedBucket() {
        assertNull(AnalyticsBuckets.progress(0))
        assertNull(AnalyticsBuckets.progress(24))
        assertEquals(ProgressBucket.P25, AnalyticsBuckets.progress(25))
        assertEquals(ProgressBucket.P25, AnalyticsBuckets.progress(49))
        assertEquals(ProgressBucket.P50, AnalyticsBuckets.progress(50))
        assertEquals(ProgressBucket.P50, AnalyticsBuckets.progress(74))
        assertEquals(ProgressBucket.P75, AnalyticsBuckets.progress(75))
        assertEquals(ProgressBucket.P75, AnalyticsBuckets.progress(99))
        assertEquals(ProgressBucket.P100, AnalyticsBuckets.progress(100))
        assertEquals(ProgressBucket.P100, AnalyticsBuckets.progress(101))
    }

    @Test
    fun progressCrossedEmitsOnlyNewlyPassedThresholds() {
        assertEquals(emptyList(), AnalyticsBuckets.progressCrossed(0, 0))
        assertEquals(emptyList(), AnalyticsBuckets.progressCrossed(0, 24))
        assertEquals(listOf(ProgressBucket.P25), AnalyticsBuckets.progressCrossed(0, 25))
        assertEquals(
            listOf(ProgressBucket.P25, ProgressBucket.P50, ProgressBucket.P75),
            AnalyticsBuckets.progressCrossed(0, 75),
        )
        assertEquals(
            listOf(
                ProgressBucket.P25,
                ProgressBucket.P50,
                ProgressBucket.P75,
                ProgressBucket.P100,
            ),
            AnalyticsBuckets.progressCrossed(0, 100),
        )
        assertEquals(listOf(ProgressBucket.P75), AnalyticsBuckets.progressCrossed(50, 75))
        assertEquals(listOf(ProgressBucket.P100), AnalyticsBuckets.progressCrossed(75, 100))
        assertEquals(emptyList(), AnalyticsBuckets.progressCrossed(50, 50))
        assertEquals(emptyList(), AnalyticsBuckets.progressCrossed(75, 50))
        assertEquals(emptyList(), AnalyticsBuckets.progressCrossed(100, 0))
    }

    @Test
    fun wpmMatchesPresetBands() {
        assertEquals(WpmBucket.UpTo250, AnalyticsBuckets.wpm(100))
        assertEquals(WpmBucket.UpTo250, AnalyticsBuckets.wpm(250))
        assertEquals(WpmBucket.From251To400, AnalyticsBuckets.wpm(251))
        assertEquals(WpmBucket.From251To400, AnalyticsBuckets.wpm(400))
        assertEquals(WpmBucket.From401To600, AnalyticsBuckets.wpm(401))
        assertEquals(WpmBucket.From401To600, AnalyticsBuckets.wpm(600))
        assertEquals(WpmBucket.From601To800, AnalyticsBuckets.wpm(601))
        assertEquals(WpmBucket.From601To800, AnalyticsBuckets.wpm(800))
        assertEquals(WpmBucket.From801To1000, AnalyticsBuckets.wpm(801))
        assertEquals(WpmBucket.From801To1000, AnalyticsBuckets.wpm(1000))
        assertEquals("up_to_250", WpmBucket.UpTo250.value)
        assertEquals("251_400", WpmBucket.From251To400.value)
        assertEquals("401_600", WpmBucket.From401To600.value)
        assertEquals("601_800", WpmBucket.From601To800.value)
        assertEquals("801_1000", WpmBucket.From801To1000.value)
    }

    @Test
    fun durationUsesInclusiveLowerBounds() {
        assertEquals(DurationBucket.UpTo30s, AnalyticsBuckets.duration(-1))
        assertEquals(DurationBucket.UpTo30s, AnalyticsBuckets.duration(0))
        assertEquals(DurationBucket.UpTo30s, AnalyticsBuckets.duration(29_999))
        assertEquals(DurationBucket.From30sTo2m, AnalyticsBuckets.duration(30_000))
        assertEquals(DurationBucket.From30sTo2m, AnalyticsBuckets.duration(119_999))
        assertEquals(DurationBucket.From2To5m, AnalyticsBuckets.duration(120_000))
        assertEquals(DurationBucket.From2To5m, AnalyticsBuckets.duration(299_999))
        assertEquals(DurationBucket.From5To15m, AnalyticsBuckets.duration(300_000))
        assertEquals(DurationBucket.From5To15m, AnalyticsBuckets.duration(899_999))
        assertEquals(DurationBucket.From15mPlus, AnalyticsBuckets.duration(900_000))
        assertEquals(DurationBucket.From15mPlus, AnalyticsBuckets.duration(3_600_000))
        assertEquals("0_30s", DurationBucket.UpTo30s.value)
        assertEquals("30s_2m", DurationBucket.From30sTo2m.value)
        assertEquals("2_5m", DurationBucket.From2To5m.value)
        assertEquals("5_15m", DurationBucket.From5To15m.value)
        assertEquals("15m_plus", DurationBucket.From15mPlus.value)
    }
}
