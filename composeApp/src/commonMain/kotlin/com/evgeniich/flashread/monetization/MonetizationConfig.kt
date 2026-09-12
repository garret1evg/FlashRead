package com.evgeniich.flashread.monetization

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Central configuration for monetization system.
 * All thresholds and durations are defined here as a single source of truth.
 */
object MonetizationConfig {

    // ==================== Ad-Free Periods ====================

    /**
     * Initial ad-free period for new users.
     * During this time, no ads are shown (Initial level).
     */
    val initialAdFreePeriod: Duration = 30.minutes

    /**
     * Duration of reward (ad-free period) granted after watching a rewarded ad.
     */
    val rewardDuration: Duration = 2.hours

    // ==================== Level Thresholds ====================

    /**
     * Time threshold for transitioning from Level 0 to Level 1.
     * After this cumulative usage time, Level 1 ads are shown.
     */
    val level1Threshold: Duration = 2.hours

    /**
     * Time threshold component for Level 2.
     * User must exceed this usage time AND [level2DaysThreshold] days to reach Level 2.
     */
    val level2TimeThreshold: Duration = 5.hours

    /**
     * Days threshold component for Level 2.
     * User must exceed [level2TimeThreshold] usage time AND this many unique days to reach Level 2.
     */
    const val level2DaysThreshold: Int = 3

    // ==================== Session Management ====================

    /**
     * Duration of inactivity after which a new session begins.
     * Used for interstitial session caps.
     */
    val sessionTimeout: Duration = 30.minutes

    // ==================== Interstitial Ads ====================

    /**
     * Minimum time between interstitial ad displays.
     */
    val interstitialInterval: Duration = 20.minutes

    /**
     * Maximum number of interstitial ads per session.
     */
    const val interstitialSessionCap: Int = 1

    /**
     * Maximum number of interstitial ads per day.
     */
    const val interstitialDailyCap: Int = 2

    // ==================== Milliseconds Helpers ====================

    /**
     * [initialAdFreePeriod] in milliseconds for storage/comparison.
     */
    val initialAdFreePeriodMs: Long
        get() = initialAdFreePeriod.inWholeMilliseconds

    /**
     * [level1Threshold] in milliseconds for storage/comparison.
     */
    val level1ThresholdMs: Long
        get() = level1Threshold.inWholeMilliseconds

    /**
     * [level2TimeThreshold] in milliseconds for storage/comparison.
     */
    val level2TimeThresholdMs: Long
        get() = level2TimeThreshold.inWholeMilliseconds

    /**
     * [rewardDuration] in milliseconds for storage/comparison.
     */
    val rewardDurationMs: Long
        get() = rewardDuration.inWholeMilliseconds

    /**
     * [sessionTimeout] in milliseconds for storage/comparison.
     */
    val sessionTimeoutMs: Long
        get() = sessionTimeout.inWholeMilliseconds

    /**
     * [interstitialInterval] in milliseconds for storage/comparison.
     */
    val interstitialIntervalMs: Long
        get() = interstitialInterval.inWholeMilliseconds
}
