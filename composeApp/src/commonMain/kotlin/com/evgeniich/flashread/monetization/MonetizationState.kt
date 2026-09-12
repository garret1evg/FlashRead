package com.evgeniich.flashread.monetization

import kotlin.time.Duration

/**
 * Immutable state holder for the monetization system.
 *
 * This class represents the complete state of monetization at any given moment,
 * including usage tracking, ad levels, reward status, and interstitial caps.
 */
data class MonetizationState(
    // ==================== Usage Tracking ====================

    /**
     * Total cumulative foreground usage time in milliseconds.
     */
    val totalUsageMs: Long = 0L,

    /**
     * Number of unique days the app has been used.
     */
    val uniqueUsageDays: Int = 0,

    /**
     * List of unique usage dates in YYYY-MM-DD format.
     * Used for diagnostics to show actual recorded dates.
     */
    val usageDates: List<String> = emptyList(),

    /**
     * Timestamp (epoch ms) when the current session started.
     * Null if no active session.
     */
    val sessionStartMs: Long? = null,

    /**
     * Incrementing session ID for tracking across restarts.
     * Incremented on each new session start.
     */
    val sessionId: Long = 0L,

    /**
     * Timestamp (epoch ms) of the last foreground activity.
     * Used for session timeout detection.
     */
    val lastActivityMs: Long = 0L,

    // ==================== Ad Level ====================

    /**
     * The calculated ad level based on usage metrics.
     * This is the "natural" level before any rewards are applied.
     */
    val calculatedLevel: AdLevel = AdLevel.Initial,

    /**
     * The ad level currently applied to layout (banners).
     * May differ from [calculatedLevel] if a safe apply boundary hasn't been crossed.
     * Settings navigation is a safe apply boundary.
     */
    val appliedLayoutLevel: AdLevel = AdLevel.Initial,

    /**
     * Whether Level 2 has ever been activated (applied to layout).
     * Once true, timezone/date changes should not clear it.
     * Cleared only if time override results in no longer qualifying for Level 2.
     */
    val level2EverActivated: Boolean = false,

    // ==================== Reward State ====================

    /**
     * Timestamp (epoch ms) when the current reward expires.
     * Null if no active reward.
     */
    val rewardExpiresAtMs: Long? = null,

    /**
     * Whether the user has ever earned a reward by watching a rewarded ad.
     * Persists across time overrides.
     */
    val hasEverEarnedReward: Boolean = false,

    /**
     * Whether the reward hint has been shown to the user.
     * Persists across time overrides.
     */
    val rewardHintShown: Boolean = false,

    // ==================== Interstitial Tracking ====================

    /**
     * Timestamp (epoch ms) when the last interstitial was shown.
     * Null if no interstitial has been shown yet.
     */
    val lastInterstitialMs: Long? = null,

    /**
     * Number of interstitials shown in the current session.
     */
    val sessionInterstitialCount: Int = 0,

    /**
     * Number of interstitials shown today (resets at midnight).
     */
    val dailyInterstitialCount: Int = 0,

    /**
     * Date string (YYYY-MM-DD) of the last interstitial count reset.
     */
    val lastInterstitialResetDate: String? = null,

    /**
     * Accumulated active usage time in milliseconds for interstitial eligibility.
     * Spec: 20 minutes of ACTIVE usage (foreground, unlocked, not during full-screen ad).
     * Resets to 0 when: reward granted, interstitial shown, Level2 first applied.
     * Paused during reward. Continues if Level2 remains applied after time override.
     */
    val interstitialEligibilityAccumulatedMs: Long = 0L,

    // ==================== Reading Session ====================

    /**
     * Whether the current reading session has had user activity
     * (scroll/tap in reader, play in speed read).
     * Required before showing interstitials.
     */
    val readingSessionHadActivity: Boolean = false,

    // ==================== Developer Mode ====================

    /**
     * Whether developer mode is unlocked.
     */
    val developerModeUnlocked: Boolean = false,

    /**
     * Developer override for ad level.
     * When non-null, this level is used instead of [calculatedLevel].
     * Note: Not exposed in UI per spec; exists for testing.
     */
    val developerLevelOverride: AdLevel? = null,
) {
    // ==================== Computed Properties ====================

    /**
     * The effective ad level for banner display after applying rewards and developer overrides.
     *
     * Priority:
     * 1. Developer override (if set) - for testing only, not exposed in UI
     * 2. Initial level (if reward is active) - no ads during reward
     * 3. Applied layout level - the level actually applied at safe boundaries
     */
    val appliedLevel: AdLevel
        get() = developerLevelOverride
            ?: if (isRewardActive) AdLevel.Initial else appliedLayoutLevel

    /**
     * Whether there is a pending level transition (calculated differs from applied layout).
     */
    val hasPendingLevelTransition: Boolean
        get() = calculatedLevel != appliedLayoutLevel

    /**
     * Whether a reward is currently active (not expired).
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun isRewardActive(currentTimeMs: Long): Boolean {
        val expiresAt = rewardExpiresAtMs ?: return false
        return currentTimeMs < expiresAt
    }

    /**
     * Whether a reward is currently active using the stored [lastActivityMs] as reference.
     * Use [isRewardActive] with explicit timestamp for more precise checks.
     */
    val isRewardActive: Boolean
        get() = isRewardActive(lastActivityMs)

    /**
     * Remaining reward duration, or null if no reward is active.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun remainingRewardDuration(currentTimeMs: Long): Duration? {
        val expiresAt = rewardExpiresAtMs ?: return null
        val remaining = expiresAt - currentTimeMs
        return if (remaining > 0) {
            Duration.parse("PT${remaining / 1000}S")
        } else {
            null
        }
    }

    /**
     * Whether the interstitial cooldown period has elapsed.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun isInterstitialCooldownComplete(currentTimeMs: Long): Boolean {
        val lastShown = lastInterstitialMs ?: return true
        return (currentTimeMs - lastShown) >= MonetizationConfig.interstitialIntervalMs
    }

    /**
     * Whether another interstitial can be shown based on caps and cooldown.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun canShowInterstitial(currentTimeMs: Long): Boolean {
        return appliedLevel.canShowInterstitial &&
            !isRewardActive(currentTimeMs) &&
            readingSessionHadActivity &&
            isInterstitialCooldownComplete(currentTimeMs) &&
            sessionInterstitialCount < MonetizationConfig.interstitialSessionCap &&
            dailyInterstitialCount < MonetizationConfig.interstitialDailyCap
    }

    /**
     * Whether this is a new session based on timeout from last activity.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun isNewSession(currentTimeMs: Long): Boolean {
        if (sessionStartMs == null) return true
        return (currentTimeMs - lastActivityMs) >= MonetizationConfig.sessionTimeoutMs
    }

    // ==================== State Transformations ====================

    /**
     * Creates a copy with updated usage time and recalculated level.
     *
     * @param additionalMs Milliseconds to add to total usage.
     */
    fun withAddedUsage(additionalMs: Long): MonetizationState {
        val newTotal = totalUsageMs + additionalMs
        return copy(
            totalUsageMs = newTotal,
            calculatedLevel = AdLevel.calculate(newTotal, uniqueUsageDays),
        )
    }

    /**
     * Creates a copy with a new unique usage day recorded.
     */
    fun withNewUsageDay(): MonetizationState {
        val newDays = uniqueUsageDays + 1
        return copy(
            uniqueUsageDays = newDays,
            calculatedLevel = AdLevel.calculate(totalUsageMs, newDays),
        )
    }

    /**
     * Creates a copy with a new reward applied.
     * Resets interstitial eligibility counter and marks reward as earned.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     * @param durationMs Duration of the reward in milliseconds.
     */
    fun withReward(currentTimeMs: Long, durationMs: Long = MonetizationConfig.rewardDurationMs): MonetizationState {
        return copy(
            rewardExpiresAtMs = currentTimeMs + durationMs,
            hasEverEarnedReward = true,
            interstitialEligibilityAccumulatedMs = 0L, // Reset on reward
        )
    }

    /**
     * Creates a copy with the current reward expiry removed.
     * Does not change [hasEverEarnedReward], [rewardHintShown], or ad caps.
     */
    fun withClearedReward(): MonetizationState {
        return copy(rewardExpiresAtMs = null)
    }

    /**
     * Creates a copy marking a new session start.
     * Increments session ID and resets session-specific counters.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun withSessionStart(currentTimeMs: Long): MonetizationState {
        return copy(
            sessionStartMs = currentTimeMs,
            sessionId = sessionId + 1,
            lastActivityMs = currentTimeMs,
            sessionInterstitialCount = 0,
            readingSessionHadActivity = false,
        )
    }

    /**
     * Creates a copy updating the last activity timestamp.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     */
    fun withActivity(currentTimeMs: Long): MonetizationState {
        return copy(lastActivityMs = currentTimeMs)
    }

    /**
     * Creates a copy marking that reading activity occurred.
     */
    fun withReadingActivity(): MonetizationState {
        return copy(readingSessionHadActivity = true)
    }

    /**
     * Creates a copy clearing the reading activity flag.
     * Called after an interstitial opportunity is consumed or skipped.
     */
    fun withClearedReadingActivity(): MonetizationState {
        return copy(readingSessionHadActivity = false)
    }

    /**
     * Creates a copy recording an interstitial was shown.
     * Resets interstitial eligibility counter.
     *
     * @param currentTimeMs Current timestamp in epoch milliseconds.
     * @param currentDate Current date string (YYYY-MM-DD).
     */
    fun withInterstitialShown(currentTimeMs: Long, currentDate: String): MonetizationState {
        val resetDaily = lastInterstitialResetDate != currentDate
        return copy(
            lastInterstitialMs = currentTimeMs,
            sessionInterstitialCount = sessionInterstitialCount + 1,
            dailyInterstitialCount = if (resetDaily) 1 else dailyInterstitialCount + 1,
            lastInterstitialResetDate = currentDate,
            interstitialEligibilityAccumulatedMs = 0L, // Reset after showing
        )
    }

    /**
     * Creates a copy with developer mode enabled.
     */
    fun withDeveloperModeUnlocked(): MonetizationState {
        return copy(developerModeUnlocked = true)
    }

    /**
     * Creates a copy with developer level override.
     *
     * @param level The level to override with, or null to clear override.
     */
    fun withDeveloperOverride(level: AdLevel?): MonetizationState {
        return copy(developerLevelOverride = level)
    }

    /**
     * Creates a copy with developer mode locked.
     */
    fun withDeveloperModeLocked(): MonetizationState {
        return copy(developerModeUnlocked = false)
    }

    /**
     * Creates a copy with the applied layout level updated.
     * Also handles Level 2 activation latch.
     *
     * @param level The level to apply to layout.
     */
    fun withAppliedLayout(level: AdLevel): MonetizationState {
        val newLevel2Activated = if (level == AdLevel.Level2) true else level2EverActivated
        // Reset interstitial eligibility counter when Level2 is first applied
        val newEligibilityMs = if (level == AdLevel.Level2 && !level2EverActivated) {
            0L
        } else {
            interstitialEligibilityAccumulatedMs
        }
        return copy(
            appliedLayoutLevel = level,
            level2EverActivated = newLevel2Activated,
            interstitialEligibilityAccumulatedMs = newEligibilityMs,
        )
    }

    /**
     * Creates a copy with reward hint shown flag set.
     */
    fun withRewardHintShown(): MonetizationState {
        return copy(rewardHintShown = true)
    }

    /**
     * Creates a copy with a new usage date added (if not already present).
     *
     * @param date The date string in YYYY-MM-DD format.
     */
    fun withUsageDateAdded(date: String): MonetizationState {
        if (usageDates.contains(date)) return this
        return copy(usageDates = usageDates + date)
    }

    /**
     * Creates a copy with updated interstitial eligibility accumulated time.
     *
     * @param accumulatedMs The new accumulated time in milliseconds.
     */
    fun withInterstitialEligibilityTime(accumulatedMs: Long): MonetizationState {
        return copy(interstitialEligibilityAccumulatedMs = accumulatedMs)
    }

    companion object {
        /**
         * Default initial state for a new user.
         */
        val INITIAL = MonetizationState()
    }
}
