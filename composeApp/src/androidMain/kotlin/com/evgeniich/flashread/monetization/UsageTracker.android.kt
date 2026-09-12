package com.evgeniich.flashread.monetization

import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Android implementation of [UsageTracker] using [AppLifecycleTracker]
 * and [MonetizationStorage] for persistence.
 *
 * ## Implementation Details
 *
 * **Time Tracking:**
 * - Uses [AppLifecycleTracker.onBackground] callback to accumulate foreground time
 * - [totalUsageMs] returns persisted time + current session time for accurate reads
 * - Persists to [MonetizationStorage] on every background transition
 *
 * **Days Tracking:**
 * - Compares current date (YYYY-MM-DD) with last usage date on foreground
 * - Increments unique days counter only on first foreground of a new calendar day
 * - Stores actual list of dates for diagnostics
 * - Uses [LocalDate] for date operations
 *
 * ## Thread Safety
 * - All operations are thread-safe through synchronized access to mutable state
 * - [AppLifecycleTracker] callbacks may come on main thread
 */
actual object UsageTracker {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD

    @Volatile
    private var persistedUsageMs: Long = 0L

    @Volatile
    private var persistedUsageDays: Int = 0

    @Volatile
    private var persistedUsageDates: List<String> = emptyList()

    @Volatile
    private var initialized = false

    /**
     * Total cumulative usage time including current session.
     * This provides a real-time view of total usage even during an active session.
     */
    actual val totalUsageMs: Long
        get() = persistedUsageMs + AppLifecycleTracker.getForegroundDurationMs()

    /**
     * Number of unique days the app has been used.
     */
    actual val uniqueUsageDays: Int
        get() = persistedUsageDays

    /**
     * List of unique usage dates in YYYY-MM-DD format.
     */
    actual val usageDates: List<String>
        get() = persistedUsageDates

    /**
     * Delegate to [AppLifecycleTracker.isInForeground] for foreground state observation.
     */
    actual val isInForeground: StateFlow<Boolean>
        get() = AppLifecycleTracker.isInForeground

    /**
     * Initializes the usage tracker by loading persisted state and setting up
     * lifecycle callbacks.
     *
     * Must be called after [AppLifecycleTracker.init] has been called.
     */
    actual fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true

            // Load persisted state
            val state = MonetizationStorage.load()
            persistedUsageMs = state.totalUsageMs
            persistedUsageDays = state.uniqueUsageDays
            persistedUsageDates = state.usageDates

            Timber.d(
                "UsageTracker initialized: %dms total, %d days, dates: %s",
                persistedUsageMs,
                persistedUsageDays,
                persistedUsageDates,
            )

            // Set up lifecycle callbacks
            AppLifecycleTracker.onForeground = ::onAppForeground
            AppLifecycleTracker.onBackground = ::onAppBackground
        }
    }

    /**
     * Returns the duration of the current foreground session.
     */
    actual fun getCurrentSessionDurationMs(): Long {
        return AppLifecycleTracker.getForegroundDurationMs()
    }

    /**
     * Persists the current accumulated usage time immediately.
     * This includes both the persisted time and current session duration.
     */
    actual fun persistCurrentUsage() {
        val sessionDuration = AppLifecycleTracker.getForegroundDurationMs()
        if (sessionDuration > 0) {
            val newTotal = persistedUsageMs + sessionDuration
            MonetizationStorage.saveTotalUsage(newTotal)
            persistedUsageMs = newTotal
            AppLifecycleTracker.resetForegroundTimer()
            Timber.d("Persisted usage: %dms total", newTotal)
        }
    }

    /**
     * Sets the total usage time directly (for developer override).
     * Persists the value immediately and resets foreground timer baseline.
     * Does NOT add usage days.
     */
    actual fun setTotalUsageMs(totalMs: Long) {
        synchronized(this) {
            MonetizationStorage.saveTotalUsage(totalMs)
            persistedUsageMs = totalMs
            AppLifecycleTracker.resetForegroundTimer()
            Timber.d("Developer override: set total usage to %dms", totalMs)
        }
    }

    /**
     * Replaces the persisted unique usage-date list (for developer override).
     * Updates in-memory date list and unique-day count, and sets last usage date
     * to the newest date when the list is non-empty.
     */
    actual fun setUsageDates(dates: List<String>) {
        synchronized(this) {
            MonetizationStorage.saveUsageDates(dates)
            MonetizationStorage.saveUniqueDays(dates.size)
            persistedUsageDates = dates
            persistedUsageDays = dates.size
            if (dates.isNotEmpty()) {
                MonetizationStorage.saveLastUsageDate(dates.maxOrNull().orEmpty())
            } else {
                MonetizationStorage.saveLastUsageDate("")
            }
            Timber.d("Developer override: set usage dates to %s (%d days)", dates, dates.size)
        }
    }

    /**
     * Resets all usage tracking data.
     * Clears both in-memory state and persisted storage.
     */
    actual fun reset() {
        synchronized(this) {
            persistedUsageMs = 0L
            persistedUsageDays = 0
            persistedUsageDates = emptyList()
            MonetizationStorage.saveTotalUsage(0L)
            MonetizationStorage.saveLastUsageDate("")
            MonetizationStorage.saveUsageDates(emptyList())
            MonetizationStorage.saveUniqueDays(0)
            Timber.d("UsageTracker reset")
        }
    }

    /**
     * Called when app enters foreground.
     * Checks if this is a new calendar day and increments the day counter if so.
     * Also adds the date to the dates list.
     */
    private fun onAppForeground() {
        val today = LocalDate.now().format(dateFormatter)
        val lastDate = MonetizationStorage.loadLastUsageDate()

        if (lastDate != today) {
            // New day - increment unique days counter AND add to dates list
            MonetizationStorage.incrementUniqueDays()
            MonetizationStorage.saveLastUsageDate(today)
            MonetizationStorage.addUsageDate(today)
            persistedUsageDays++
            persistedUsageDates = MonetizationStorage.loadUsageDates()
            Timber.d("New usage day recorded: %s (total: %d days)", today, persistedUsageDays)
        }
    }

    /**
     * Called when app enters background.
     * Persists the accumulated foreground time.
     *
     * @param foregroundDurationMs Time spent in foreground since last onStart
     */
    private fun onAppBackground(foregroundDurationMs: Long) {
        if (foregroundDurationMs > 0) {
            val newTotal = persistedUsageMs + foregroundDurationMs
            MonetizationStorage.saveTotalUsage(newTotal)
            persistedUsageMs = newTotal
            Timber.d(
                "Session ended: +%dms, total: %dms",
                foregroundDurationMs,
                newTotal,
            )
        }
    }
}
