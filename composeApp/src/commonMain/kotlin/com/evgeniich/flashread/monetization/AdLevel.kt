package com.evgeniich.flashread.monetization

/**
 * Advertising intensity level based on user engagement.
 *
 * Levels progress based on cumulative usage time and unique usage days:
 * - [Initial]: New user grace period (first 30 minutes) - no ads
 * - [Level0]: Light ads (30 min - 2 hours) - banners on Home/Library only
 * - [Level1]: Moderate ads (2 - 5 hours) - banners on more screens
 * - [Level2]: Full ads (5+ hours AND 3+ days) - all ad placements including interstitials
 */
enum class AdLevel {
    /**
     * Initial grace period for new users.
     * No ads are shown during this time.
     */
    Initial,

    /**
     * Light advertising level.
     * Only banner ads on Home and Library screens.
     */
    Level0,

    /**
     * Moderate advertising level.
     * Banner ads on Settings, Reader, and SpeedRead (paused) screens added.
     */
    Level1,

    /**
     * Full advertising level.
     * All ad placements active, including interstitials and banners during playback.
     */
    Level2,
    ;

    /**
     * Returns true if this level shows banner ads on Home and Library screens.
     */
    val showsHomeBanner: Boolean
        get() = this >= Level0

    /**
     * Returns true if this level shows banner ads on Settings and Reader screens.
     */
    val showsReaderBanner: Boolean
        get() = this >= Level1

    /**
     * Returns true if this level shows banner ads on SpeedRead screen when paused.
     */
    val showsSpeedReadPausedBanner: Boolean
        get() = this >= Level1

    /**
     * Returns true if this level shows banner ads on SpeedRead screen during playback.
     */
    val showsSpeedReadPlayingBanner: Boolean
        get() = this == Level2

    /**
     * Returns true if this level can show interstitial ads.
     */
    val canShowInterstitial: Boolean
        get() = this == Level2

    companion object {
        /**
         * Calculates the appropriate ad level based on usage metrics.
         *
         * @param usageMs Cumulative foreground usage time in milliseconds.
         * @param usageDays Number of unique days the app was used.
         * @return The calculated [AdLevel].
         */
        fun calculate(usageMs: Long, usageDays: Int): AdLevel = when {
            usageMs < MonetizationConfig.initialAdFreePeriodMs -> Initial
            usageMs < MonetizationConfig.level1ThresholdMs -> Level0
            usageMs < MonetizationConfig.level2TimeThresholdMs ||
                usageDays < MonetizationConfig.level2DaysThreshold -> Level1
            else -> Level2
        }

        /**
         * Serializes [AdLevel] to storage string.
         */
        fun AdLevel.toStorage(): String = name.lowercase()

        /**
         * Deserializes [AdLevel] from storage string.
         */
        fun fromStorage(value: String?): AdLevel {
            if (value.isNullOrBlank()) return Initial
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: Initial
        }
    }
}
