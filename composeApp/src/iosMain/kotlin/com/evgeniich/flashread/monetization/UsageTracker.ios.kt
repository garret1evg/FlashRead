package com.evgeniich.flashread.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS no-op stub implementation of [UsageTracker].
 *
 * Usage tracking is not implemented on iOS in this version.
 * All methods are no-ops and return default values.
 */
actual object UsageTracker {

    private val _isInForeground = MutableStateFlow(true)

    /**
     * Always returns 0 on iOS (usage tracking not implemented).
     */
    actual val totalUsageMs: Long
        get() = 0L

    /**
     * Always returns 0 on iOS (usage tracking not implemented).
     */
    actual val uniqueUsageDays: Int
        get() = 0

    /**
     * Always returns empty list on iOS (usage tracking not implemented).
     */
    actual val usageDates: List<String>
        get() = emptyList()

    /**
     * Always returns true on iOS (no foreground tracking).
     */
    actual val isInForeground: StateFlow<Boolean>
        get() = _isInForeground

    /**
     * No-op on iOS.
     */
    actual fun init() {
        // No-op
    }

    /**
     * Always returns 0 on iOS.
     */
    actual fun getCurrentSessionDurationMs(): Long = 0L

    /**
     * No-op on iOS.
     */
    actual fun persistCurrentUsage() {
        // No-op
    }

    /**
     * No-op on iOS.
     */
    actual fun setTotalUsageMs(totalMs: Long) {
        // No-op
    }

    /**
     * No-op on iOS.
     */
    actual fun setUsageDates(dates: List<String>) {
        // No-op
    }

    /**
     * No-op on iOS.
     */
    actual fun reset() {
        // No-op
    }
}
