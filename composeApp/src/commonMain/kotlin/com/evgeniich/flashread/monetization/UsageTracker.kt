package com.evgeniich.flashread.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific usage tracker for monitoring app foreground time and unique usage days.
 *
 * On Android, this integrates with ProcessLifecycleOwner to accurately track
 * time spent in foreground and persists data across app restarts.
 *
 * On iOS, this is a no-op stub (usage tracking not implemented).
 *
 * ## Usage Tracking Logic
 *
 * **Time Tracking:**
 * - Tracks cumulative foreground time in milliseconds
 * - Time is accumulated when app is visible (Activity started)
 * - Persisted to storage when app goes to background
 *
 * **Days Tracking:**
 * - Counts unique calendar days when app was used
 * - A new day is counted on first foreground event of that day
 * - Uses YYYY-MM-DD format for date comparison
 * - Stores actual list of dates for diagnostics
 */
expect object UsageTracker {

    /**
     * Total cumulative usage time in milliseconds.
     * Includes persisted time plus current session time if in foreground.
     */
    val totalUsageMs: Long

    /**
     * Number of unique days the app has been used.
     */
    val uniqueUsageDays: Int

    /**
     * List of unique usage dates in YYYY-MM-DD format.
     * Used for diagnostics to show actual recorded dates.
     */
    val usageDates: List<String>

    /**
     * Observable state flow of foreground status.
     * - `true` when app is in foreground
     * - `false` when app is in background
     */
    val isInForeground: StateFlow<Boolean>

    /**
     * Initializes the usage tracker.
     *
     * On Android, this sets up callbacks with [AppLifecycleTracker] to track
     * foreground/background transitions. Should be called after [AppLifecycleTracker.init].
     *
     * On iOS, this is a no-op.
     */
    fun init()

    /**
     * Gets the current foreground duration for the active session.
     *
     * @return Duration in milliseconds since app entered foreground,
     *         or 0 if currently in background.
     */
    fun getCurrentSessionDurationMs(): Long

    /**
     * Forces a save of the current accumulated usage time.
     * Useful when you need to persist state immediately without waiting
     * for a background transition.
     */
    fun persistCurrentUsage()

    /**
     * Sets the total usage time directly (for developer override).
     * Persists the value immediately and resets foreground timer baseline.
     * Does NOT add usage days.
     *
     * @param totalMs The new total usage time in milliseconds.
     */
    fun setTotalUsageMs(totalMs: Long)

    /**
     * Replaces the persisted unique usage-date list (for developer override).
     * Also updates the unique-day count to [dates].size.
     *
     * @param dates Distinct usage dates in YYYY-MM-DD format.
     */
    fun setUsageDates(dates: List<String>)

    /**
     * Resets all usage tracking data.
     * Used for testing or when user requests data reset.
     */
    fun reset()
}
