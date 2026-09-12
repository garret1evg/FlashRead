package com.evgeniich.flashread.monetization

import com.evgeniich.flashread.navigation.AppRoute

/**
 * Policy object that determines ad placement eligibility based on the current
 * monetization state and screen context.
 *
 * This is the central decision-making point for all ad-related logic.
 * It uses [MonetizationState] for user data and [AdLevel] properties for rules.
 */
object MonetizationPolicy {

    // ==================== Banner Placement ====================

    /**
     * Determines whether a banner ad should be shown on the given screen.
     *
     * Banner placement rules by level:
     * - Initial: No banners on any screen
     * - Level0: Banners on Home and Library only
     * - Level1: Banners on Home, Library, Settings, Reader, SpeedRead (paused)
     * - Level2: Banners on all screens including SpeedRead (playing)
     *
     * @param state Current monetization state.
     * @param route The screen route being displayed.
     * @param isSpeedReadPlaying Whether speed read is currently playing (only for SpeedRead screens).
     * @return True if a banner should be shown.
     */
    fun shouldShowBanner(
        state: MonetizationState,
        route: AppRoute,
        isSpeedReadPlaying: Boolean = false,
    ): Boolean {
        val level = state.appliedLevel

        return when (route) {
            is AppRoute.Home,
            is AppRoute.Library,
            -> level.showsHomeBanner

            is AppRoute.Settings,
            is AppRoute.Reader,
            -> level.showsReaderBanner

            is AppRoute.SpeedReadPlayer,
            -> {
                if (isSpeedReadPlaying) {
                    level.showsSpeedReadPlayingBanner
                } else {
                    level.showsSpeedReadPausedBanner
                }
            }

            // No banners on these screens (including SpeedRead setup)
            is AppRoute.SpeedRead,
            is AppRoute.PrivacyPolicy,
            is AppRoute.Terms,
            is AppRoute.BookEditor,
            is AppRoute.QuickSpeedRead,
            -> false
        }
    }

    /**
     * Determines whether a banner ad should be shown on the given screen.
     * Simplified version using applied level directly.
     *
     * @param level The applied ad level.
     * @param route The screen route being displayed.
     * @param isSpeedReadPlaying Whether speed read is currently playing.
     * @return True if a banner should be shown.
     */
    fun shouldShowBanner(
        level: AdLevel,
        route: AppRoute,
        isSpeedReadPlaying: Boolean = false,
    ): Boolean {
        return when (route) {
            is AppRoute.Home,
            is AppRoute.Library,
            -> level.showsHomeBanner

            is AppRoute.Settings,
            is AppRoute.Reader,
            -> level.showsReaderBanner

            is AppRoute.SpeedReadPlayer,
            -> {
                if (isSpeedReadPlaying) {
                    level.showsSpeedReadPlayingBanner
                } else {
                    level.showsSpeedReadPausedBanner
                }
            }

            // No banners on these screens (including SpeedRead setup)
            is AppRoute.SpeedRead,
            is AppRoute.PrivacyPolicy,
            is AppRoute.Terms,
            is AppRoute.BookEditor,
            is AppRoute.QuickSpeedRead,
            -> false
        }
    }

    // ==================== Interstitial Eligibility ====================

    /**
     * Runtime context for interstitial eligibility check.
     * Contains conditions that can only be determined at runtime.
     */
    data class InterstitialContext(
        /** Whether a hint or tutorial overlay is currently being shown. */
        val isHintBeingShown: Boolean = false,

        /** Whether the interstitial ad is preloaded and ready to show. */
        val isAdReady: Boolean = false,

        /** Whether the app is currently in the foreground. */
        val isAppInForeground: Boolean = true,
    )

    /**
     * Determines whether an interstitial ad is eligible to be shown.
     *
     * All of the following conditions must be met:
     * 1. Applied level is Level2 (full ads)
     * 2. No active reward (user hasn't watched a rewarded ad recently)
     * 3. Reading session has had activity (scroll/tap in reader, play in speed read)
     * 4. Interstitial cooldown has elapsed (20 minutes since last one)
     * 5. Session interstitial cap not reached (max 1 per session)
     * 6. Daily interstitial cap not reached (max 2 per day)
     * 7. No hint/tutorial overlay is showing
     * 8. Ad is preloaded and ready
     * 9. App is in foreground
     *
     * @param state Current monetization state.
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     * @param context Runtime context for additional conditions.
     * @return True if an interstitial can be shown.
     */
    fun isInterstitialEligible(
        state: MonetizationState,
        currentTimeMs: Long,
        context: InterstitialContext = InterstitialContext(),
    ): Boolean {
        // Quick exit if level doesn't allow interstitials
        if (!state.appliedLevel.canShowInterstitial) return false

        // Check all state-based conditions
        if (!state.canShowInterstitial(currentTimeMs)) return false

        // Check runtime conditions
        if (context.isHintBeingShown) return false
        if (!context.isAdReady) return false
        if (!context.isAppInForeground) return false

        return true
    }

    /**
     * Determines the reason why an interstitial cannot be shown.
     * Useful for debugging and developer menu.
     *
     * @param state Current monetization state.
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     * @param context Runtime context for additional conditions.
     * @return Reason why interstitial is blocked, or null if eligible.
     */
    fun getInterstitialBlockReason(
        state: MonetizationState,
        currentTimeMs: Long,
        context: InterstitialContext = InterstitialContext(),
    ): InterstitialBlockReason? {
        if (state.appliedLevel != AdLevel.Level2) {
            return InterstitialBlockReason.LevelTooLow(state.appliedLevel)
        }
        if (state.isRewardActive(currentTimeMs)) {
            return InterstitialBlockReason.RewardActive
        }
        if (!state.readingSessionHadActivity) {
            return InterstitialBlockReason.NoReadingActivity
        }
        if (!state.isInterstitialCooldownComplete(currentTimeMs)) {
            val remaining = state.lastInterstitialMs?.let { last ->
                MonetizationConfig.interstitialIntervalMs - (currentTimeMs - last)
            } ?: 0L
            return InterstitialBlockReason.CooldownActive(remaining)
        }
        if (state.sessionInterstitialCount >= MonetizationConfig.interstitialSessionCap) {
            return InterstitialBlockReason.SessionCapReached
        }
        if (state.dailyInterstitialCount >= MonetizationConfig.interstitialDailyCap) {
            return InterstitialBlockReason.DailyCapReached
        }
        if (context.isHintBeingShown) {
            return InterstitialBlockReason.HintShowing
        }
        if (!context.isAdReady) {
            return InterstitialBlockReason.AdNotReady
        }
        if (!context.isAppInForeground) {
            return InterstitialBlockReason.AppInBackground
        }
        return null
    }

    // ==================== Reward Hint ====================

    /**
     * Whether the one-time rewarded-ad hint should be presented.
     *
     * The caller must also ensure this is a safe after-reading opportunity
     * (Library arrival from a reading screen). This function checks:
     * - Level 2 has been activated ([MonetizationState.level2EverActivated] and/or
     *   applied layout is Level 2)
     * - User has never earned a rewarded-ad reward
     * - Hint has never been shown
     * - Reading session had activity
     * - App is in the foreground
     */
    fun shouldShowRewardHint(
        state: MonetizationState,
        isAppInForeground: Boolean,
    ): Boolean {
        if (!isAppInForeground) return false
        if (state.hasEverEarnedReward) return false
        if (state.rewardHintShown) return false
        if (!state.readingSessionHadActivity) return false
        val level2Activated = state.level2EverActivated ||
            state.appliedLayoutLevel == AdLevel.Level2
        return level2Activated
    }

    // ==================== Rewarded Ad Eligibility ====================

    /**
     * Determines whether the user can watch a rewarded ad.
     *
     * Rewarded ads are available when:
     * 1. The applied level shows any ads (not Initial)
     * 2. No active reward already in effect
     *
     * @param state Current monetization state.
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     * @return True if a rewarded ad can be offered.
     */
    fun canOfferRewardedAd(
        state: MonetizationState,
        currentTimeMs: Long,
    ): Boolean {
        // Don't offer if already in grace period or reward is active
        if (state.appliedLevel == AdLevel.Initial) return false
        if (state.isRewardActive(currentTimeMs)) return false
        return true
    }

    // ==================== Level Progress ====================

    /**
     * Calculates progress towards the next ad level.
     *
     * @param state Current monetization state.
     * @return Progress info or null if already at max level.
     */
    fun getLevelProgress(state: MonetizationState): LevelProgress? {
        return when (state.calculatedLevel) {
            AdLevel.Initial -> {
                val progress = state.totalUsageMs.toFloat() / MonetizationConfig.initialAdFreePeriodMs
                LevelProgress(
                    currentLevel = AdLevel.Initial,
                    nextLevel = AdLevel.Level0,
                    progress = progress.coerceIn(0f, 1f),
                    remainingMs = (MonetizationConfig.initialAdFreePeriodMs - state.totalUsageMs)
                        .coerceAtLeast(0),
                    remainingDays = null,
                )
            }

            AdLevel.Level0 -> {
                val progress = state.totalUsageMs.toFloat() / MonetizationConfig.level1ThresholdMs
                LevelProgress(
                    currentLevel = AdLevel.Level0,
                    nextLevel = AdLevel.Level1,
                    progress = progress.coerceIn(0f, 1f),
                    remainingMs = (MonetizationConfig.level1ThresholdMs - state.totalUsageMs)
                        .coerceAtLeast(0),
                    remainingDays = null,
                )
            }

            AdLevel.Level1 -> {
                // Level 2 requires BOTH time AND days thresholds
                val timeProgress = state.totalUsageMs.toFloat() / MonetizationConfig.level2TimeThresholdMs
                val daysProgress = state.uniqueUsageDays.toFloat() / MonetizationConfig.level2DaysThreshold
                val overallProgress = minOf(timeProgress, daysProgress)

                LevelProgress(
                    currentLevel = AdLevel.Level1,
                    nextLevel = AdLevel.Level2,
                    progress = overallProgress.coerceIn(0f, 1f),
                    remainingMs = (MonetizationConfig.level2TimeThresholdMs - state.totalUsageMs)
                        .coerceAtLeast(0),
                    remainingDays = (MonetizationConfig.level2DaysThreshold - state.uniqueUsageDays)
                        .coerceAtLeast(0),
                )
            }

            AdLevel.Level2 -> null // Already at max level
        }
    }
}

/**
 * Reasons why an interstitial ad cannot be shown.
 */
sealed class InterstitialBlockReason {
    /** Ad level is below Level2. */
    data class LevelTooLow(val currentLevel: AdLevel) : InterstitialBlockReason()

    /** User has an active reward from watching a rewarded ad. */
    data object RewardActive : InterstitialBlockReason()

    /** No reading activity in current session. */
    data object NoReadingActivity : InterstitialBlockReason()

    /** Cooldown period not elapsed since last interstitial. */
    data class CooldownActive(val remainingMs: Long) : InterstitialBlockReason()

    /** Maximum interstitials for this session already shown. */
    data object SessionCapReached : InterstitialBlockReason()

    /** Maximum interstitials for today already shown. */
    data object DailyCapReached : InterstitialBlockReason()

    /** A hint or tutorial overlay is showing. */
    data object HintShowing : InterstitialBlockReason()

    /** Interstitial ad is not preloaded. */
    data object AdNotReady : InterstitialBlockReason()

    /** App is in background. */
    data object AppInBackground : InterstitialBlockReason()
}

/**
 * Progress information towards the next ad level.
 */
data class LevelProgress(
    /** Current calculated ad level. */
    val currentLevel: AdLevel,

    /** Next ad level to reach. */
    val nextLevel: AdLevel,

    /** Progress towards next level (0.0 to 1.0). */
    val progress: Float,

    /** Remaining usage time needed in milliseconds. */
    val remainingMs: Long,

    /** Remaining unique days needed (null if not applicable). */
    val remainingDays: Int?,
) {
    /** Whether time requirement is met for next level. */
    val timeRequirementMet: Boolean
        get() = remainingMs <= 0

    /** Whether days requirement is met for next level (always true if not applicable). */
    val daysRequirementMet: Boolean
        get() = remainingDays?.let { it <= 0 } ?: true

    /** Whether all requirements are met for next level. */
    val allRequirementsMet: Boolean
        get() = timeRequirementMet && daysRequirementMet
}
