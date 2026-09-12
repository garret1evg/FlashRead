package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MonetizationConfigTest {

    @Test
    fun specThresholdsMatchConfig() {
        assertEquals(30.minutes, MonetizationConfig.initialAdFreePeriod)
        assertEquals(2.hours, MonetizationConfig.rewardDuration)
        assertEquals(2.hours, MonetizationConfig.level1Threshold)
        assertEquals(5.hours, MonetizationConfig.level2TimeThreshold)
        assertEquals(3, MonetizationConfig.level2DaysThreshold)
        assertEquals(30.minutes, MonetizationConfig.sessionTimeout)
        assertEquals(20.minutes, MonetizationConfig.interstitialInterval)
        assertEquals(1, MonetizationConfig.interstitialSessionCap)
        assertEquals(2, MonetizationConfig.interstitialDailyCap)
    }

    @Test
    fun millisecondHelpersMatchDurations() {
        assertEquals(
            MonetizationConfig.initialAdFreePeriod.inWholeMilliseconds,
            MonetizationConfig.initialAdFreePeriodMs,
        )
        assertEquals(
            MonetizationConfig.level1Threshold.inWholeMilliseconds,
            MonetizationConfig.level1ThresholdMs,
        )
        assertEquals(
            MonetizationConfig.level2TimeThreshold.inWholeMilliseconds,
            MonetizationConfig.level2TimeThresholdMs,
        )
        assertEquals(
            MonetizationConfig.rewardDuration.inWholeMilliseconds,
            MonetizationConfig.rewardDurationMs,
        )
        assertEquals(
            MonetizationConfig.sessionTimeout.inWholeMilliseconds,
            MonetizationConfig.sessionTimeoutMs,
        )
        assertEquals(
            MonetizationConfig.interstitialInterval.inWholeMilliseconds,
            MonetizationConfig.interstitialIntervalMs,
        )
    }
}
