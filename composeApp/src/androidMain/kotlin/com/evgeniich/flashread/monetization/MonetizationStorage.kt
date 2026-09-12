@file:Suppress("DEPRECATION")

package com.evgeniich.flashread.monetization

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.evgeniich.flashread.monetization.AdLevel.Companion.fromStorage
import com.evgeniich.flashread.monetization.AdLevel.Companion.toStorage
import com.evgeniich.flashread.platform.AndroidAppContext
import timber.log.Timber

/**
 * Android EncryptedSharedPreferences-based storage for monetization state.
 *
 * Uses AES256 encryption via Android Keystore to protect stored values
 * from manual modification through root access or file editing.
 *
 * Persists the following state across app restarts:
 * - Total usage time
 * - Unique usage days
 * - Reward expiration timestamp
 * - Interstitial ad tracking (daily count, last shown, reset date)
 * - Developer mode settings
 *
 * Session-specific data (sessionStartMs, sessionInterstitialCount, etc.)
 * is intentionally NOT persisted as it resets with each session.
 */
object MonetizationStorage {
    private const val PREFS_NAME = "flashread_monetization_encrypted_prefs"

    // Usage tracking keys
    private const val KEY_TOTAL_USAGE_MS = "total_usage_ms"
    private const val KEY_UNIQUE_USAGE_DAYS = "unique_usage_days"
    private const val KEY_LAST_USAGE_DATE = "last_usage_date"
    private const val KEY_USAGE_DATES = "usage_dates" // Comma-separated YYYY-MM-DD dates

    // Session tracking keys
    private const val KEY_SESSION_ID = "session_id"

    // Ad level keys
    private const val KEY_APPLIED_LAYOUT_LEVEL = "applied_layout_level"
    private const val KEY_LEVEL2_EVER_ACTIVATED = "level2_ever_activated"

    // Reward state keys
    private const val KEY_REWARD_EXPIRES_AT_MS = "reward_expires_at_ms"
    private const val KEY_HAS_EVER_EARNED_REWARD = "has_ever_earned_reward"
    private const val KEY_REWARD_HINT_SHOWN = "reward_hint_shown"

    // Interstitial tracking keys
    private const val KEY_LAST_INTERSTITIAL_MS = "last_interstitial_ms"
    private const val KEY_DAILY_INTERSTITIAL_COUNT = "daily_interstitial_count"
    private const val KEY_LAST_INTERSTITIAL_RESET_DATE = "last_interstitial_reset_date"
    private const val KEY_INTERSTITIAL_ELIGIBILITY_ACCUMULATED_MS = "interstitial_eligibility_accumulated_ms"

    // Developer mode keys
    private const val KEY_DEVELOPER_MODE_UNLOCKED = "developer_mode_unlocked"
    private const val KEY_DEVELOPER_LEVEL_OVERRIDE = "developer_level_override"

    @Volatile
    private var encryptedPrefs: SharedPreferences? = null

    /**
     * Saves the monetization state to EncryptedSharedPreferences.
     *
     * Only persists fields that need to survive app restarts.
     * Session-specific fields are intentionally excluded.
     *
     * @param state The monetization state to persist.
     */
    fun save(state: MonetizationState) {
        prefs().edit {
            putLong(KEY_TOTAL_USAGE_MS, state.totalUsageMs)
            putInt(KEY_UNIQUE_USAGE_DAYS, state.uniqueUsageDays)

            // Usage dates list (comma-separated)
            putString(KEY_USAGE_DATES, state.usageDates.joinToString(","))

            // Session tracking
            putLong(KEY_SESSION_ID, state.sessionId)

            // Ad level state
            putString(KEY_APPLIED_LAYOUT_LEVEL, state.appliedLayoutLevel.toStorage())
            putBoolean(KEY_LEVEL2_EVER_ACTIVATED, state.level2EverActivated)

            // Reward state
            if (state.rewardExpiresAtMs != null) {
                putLong(KEY_REWARD_EXPIRES_AT_MS, state.rewardExpiresAtMs)
            } else {
                remove(KEY_REWARD_EXPIRES_AT_MS)
            }
            putBoolean(KEY_HAS_EVER_EARNED_REWARD, state.hasEverEarnedReward)
            putBoolean(KEY_REWARD_HINT_SHOWN, state.rewardHintShown)

            // Interstitial tracking
            if (state.lastInterstitialMs != null) {
                putLong(KEY_LAST_INTERSTITIAL_MS, state.lastInterstitialMs)
            } else {
                remove(KEY_LAST_INTERSTITIAL_MS)
            }
            putInt(KEY_DAILY_INTERSTITIAL_COUNT, state.dailyInterstitialCount)
            if (state.lastInterstitialResetDate != null) {
                putString(KEY_LAST_INTERSTITIAL_RESET_DATE, state.lastInterstitialResetDate)
            } else {
                remove(KEY_LAST_INTERSTITIAL_RESET_DATE)
            }
            putLong(KEY_INTERSTITIAL_ELIGIBILITY_ACCUMULATED_MS, state.interstitialEligibilityAccumulatedMs)

            // Developer mode
            putBoolean(KEY_DEVELOPER_MODE_UNLOCKED, state.developerModeUnlocked)
            if (state.developerLevelOverride != null) {
                putString(KEY_DEVELOPER_LEVEL_OVERRIDE, state.developerLevelOverride.toStorage())
            } else {
                remove(KEY_DEVELOPER_LEVEL_OVERRIDE)
            }
        }
    }

    /**
     * Loads the monetization state from EncryptedSharedPreferences.
     *
     * Returns a [MonetizationState] with persisted values restored
     * and calculated level recomputed based on usage metrics.
     *
     * @return The restored monetization state.
     */
    fun load(): MonetizationState {
        val prefs = prefs()

        val totalUsageMs = prefs.getLong(KEY_TOTAL_USAGE_MS, 0L)
        val uniqueUsageDays = prefs.getInt(KEY_UNIQUE_USAGE_DAYS, 0)

        // Parse usage dates from comma-separated string
        val usageDatesStr = prefs.getString(KEY_USAGE_DATES, null)
        val usageDates = usageDatesStr
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        // Session tracking
        val sessionId = prefs.getLong(KEY_SESSION_ID, 0L)

        // Ad level state
        val appliedLayoutLevel = prefs.getString(KEY_APPLIED_LAYOUT_LEVEL, null)
            ?.let { fromStorage(it) }
            ?: AdLevel.Initial
        val level2EverActivated = prefs.getBoolean(KEY_LEVEL2_EVER_ACTIVATED, false)

        // Reward state
        val rewardExpiresAtMs = prefs.getLong(KEY_REWARD_EXPIRES_AT_MS, -1L)
            .takeIf { it >= 0 }
        val hasEverEarnedReward = prefs.getBoolean(KEY_HAS_EVER_EARNED_REWARD, false)
        val rewardHintShown = prefs.getBoolean(KEY_REWARD_HINT_SHOWN, false)

        // Interstitial tracking
        val lastInterstitialMs = prefs.getLong(KEY_LAST_INTERSTITIAL_MS, -1L)
            .takeIf { it >= 0 }
        val dailyInterstitialCount = prefs.getInt(KEY_DAILY_INTERSTITIAL_COUNT, 0)
        val lastInterstitialResetDate = prefs.getString(KEY_LAST_INTERSTITIAL_RESET_DATE, null)
        val interstitialEligibilityAccumulatedMs = prefs.getLong(KEY_INTERSTITIAL_ELIGIBILITY_ACCUMULATED_MS, 0L)

        // Developer mode
        val developerModeUnlocked = prefs.getBoolean(KEY_DEVELOPER_MODE_UNLOCKED, false)
        val developerLevelOverride = prefs.getString(KEY_DEVELOPER_LEVEL_OVERRIDE, null)
            ?.let { fromStorage(it) }
            ?.takeIf { it != AdLevel.Initial } // Initial is the fallback, treat as no override

        val calculatedLevel = AdLevel.calculate(totalUsageMs, uniqueUsageDays)

        return MonetizationState(
            totalUsageMs = totalUsageMs,
            uniqueUsageDays = uniqueUsageDays,
            usageDates = usageDates,
            sessionId = sessionId,
            calculatedLevel = calculatedLevel,
            appliedLayoutLevel = appliedLayoutLevel,
            level2EverActivated = level2EverActivated,
            rewardExpiresAtMs = rewardExpiresAtMs,
            hasEverEarnedReward = hasEverEarnedReward,
            rewardHintShown = rewardHintShown,
            lastInterstitialMs = lastInterstitialMs,
            dailyInterstitialCount = dailyInterstitialCount,
            lastInterstitialResetDate = lastInterstitialResetDate,
            interstitialEligibilityAccumulatedMs = interstitialEligibilityAccumulatedMs,
            developerModeUnlocked = developerModeUnlocked,
            developerLevelOverride = developerLevelOverride,
        )
    }

    /**
     * Saves the total usage time in milliseconds.
     *
     * @param usageMs The total usage time to save.
     */
    fun saveTotalUsage(usageMs: Long) {
        prefs().edit { putLong(KEY_TOTAL_USAGE_MS, usageMs) }
    }

    /**
     * Saves the last usage date string (YYYY-MM-DD format).
     * Used to track unique usage days.
     *
     * @param date The date string to save.
     */
    fun saveLastUsageDate(date: String) {
        prefs().edit { putString(KEY_LAST_USAGE_DATE, date) }
    }

    /**
     * Loads the last usage date string.
     *
     * @return The last usage date or null if never set.
     */
    fun loadLastUsageDate(): String? {
        return prefs().getString(KEY_LAST_USAGE_DATE, null)
    }

    /**
     * Increments the unique usage days counter.
     */
    fun incrementUniqueDays() {
        val current = prefs().getInt(KEY_UNIQUE_USAGE_DAYS, 0)
        prefs().edit { putInt(KEY_UNIQUE_USAGE_DAYS, current + 1) }
    }

    /**
     * Saves the reward expiration timestamp.
     *
     * @param expiresAtMs The expiration timestamp, or null to clear.
     */
    fun saveRewardExpiration(expiresAtMs: Long?) {
        prefs().edit {
            if (expiresAtMs != null) {
                putLong(KEY_REWARD_EXPIRES_AT_MS, expiresAtMs)
            } else {
                remove(KEY_REWARD_EXPIRES_AT_MS)
            }
        }
    }

    /**
     * Saves the developer mode unlocked state.
     *
     * @param unlocked Whether developer mode is unlocked.
     */
    fun saveDeveloperModeUnlocked(unlocked: Boolean) {
        prefs().edit { putBoolean(KEY_DEVELOPER_MODE_UNLOCKED, unlocked) }
    }

    /**
     * Saves the developer level override.
     *
     * @param level The override level, or null to clear.
     */
    fun saveDeveloperLevelOverride(level: AdLevel?) {
        prefs().edit {
            if (level != null) {
                putString(KEY_DEVELOPER_LEVEL_OVERRIDE, level.toStorage())
            } else {
                remove(KEY_DEVELOPER_LEVEL_OVERRIDE)
            }
        }
    }

    /**
     * Saves the usage dates list.
     *
     * @param dates List of date strings in YYYY-MM-DD format.
     */
    fun saveUsageDates(dates: List<String>) {
        prefs().edit { putString(KEY_USAGE_DATES, dates.joinToString(",")) }
    }

    /**
     * Loads the usage dates list.
     *
     * @return List of date strings in YYYY-MM-DD format.
     */
    fun loadUsageDates(): List<String> {
        val datesStr = prefs().getString(KEY_USAGE_DATES, null) ?: return emptyList()
        return datesStr.split(",").filter { it.isNotBlank() }
    }

    /**
     * Adds a usage date if not already present.
     *
     * @param date Date string in YYYY-MM-DD format.
     * @return True if date was added, false if already present.
     */
    fun addUsageDate(date: String): Boolean {
        val current = loadUsageDates()
        if (current.contains(date)) return false
        saveUsageDates(current + date)
        return true
    }

    /**
     * Saves the unique usage days count.
     *
     * @param days Number of unique usage days.
     */
    fun saveUniqueDays(days: Int) {
        prefs().edit { putInt(KEY_UNIQUE_USAGE_DAYS, days) }
    }

    /**
     * Saves the applied layout level.
     *
     * @param level The level applied to layout.
     */
    fun saveAppliedLayoutLevel(level: AdLevel) {
        prefs().edit { putString(KEY_APPLIED_LAYOUT_LEVEL, level.toStorage()) }
    }

    /**
     * Saves the Level 2 ever activated flag.
     *
     * @param activated Whether Level 2 was ever activated.
     */
    fun saveLevel2EverActivated(activated: Boolean) {
        prefs().edit { putBoolean(KEY_LEVEL2_EVER_ACTIVATED, activated) }
    }

    /**
     * Saves whether user has ever earned a reward.
     *
     * @param earned Whether reward was ever earned.
     */
    fun saveHasEverEarnedReward(earned: Boolean) {
        prefs().edit { putBoolean(KEY_HAS_EVER_EARNED_REWARD, earned) }
    }

    /**
     * Saves whether reward hint was shown.
     *
     * @param shown Whether hint was shown.
     */
    fun saveRewardHintShown(shown: Boolean) {
        prefs().edit { putBoolean(KEY_REWARD_HINT_SHOWN, shown) }
    }

    /**
     * Saves the session ID.
     *
     * @param sessionId The current session ID.
     */
    fun saveSessionId(sessionId: Long) {
        prefs().edit { putLong(KEY_SESSION_ID, sessionId) }
    }

    /**
     * Loads the session ID.
     *
     * @return The last session ID, or 0 if not set.
     */
    fun loadSessionId(): Long {
        return prefs().getLong(KEY_SESSION_ID, 0L)
    }

    /**
     * Saves the interstitial eligibility accumulated time.
     *
     * @param accumulatedMs Accumulated time in milliseconds.
     */
    fun saveInterstitialEligibilityAccumulated(accumulatedMs: Long) {
        prefs().edit { putLong(KEY_INTERSTITIAL_ELIGIBILITY_ACCUMULATED_MS, accumulatedMs) }
    }

    /**
     * Clears all stored monetization data.
     * Useful for testing or resetting the monetization state.
     */
    fun clear() {
        prefs().edit { clear() }
    }

    /**
     * Creates or returns cached EncryptedSharedPreferences instance.
     *
     * Uses AES256_SIV for key encryption and AES256_GCM for value encryption,
     * both backed by Android Keystore for hardware-level security on supported devices.
     *
     * Falls back to regular SharedPreferences if encryption fails (e.g., on some OEM devices
     * with broken Keystore implementations), logging a warning.
     */
    private fun prefs(): SharedPreferences {
        encryptedPrefs?.let { return it }

        synchronized(this) {
            encryptedPrefs?.let { return it }

            val context = AndroidAppContext.applicationContext

            val prefs = try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                // Fallback to regular SharedPreferences on devices with broken Keystore
                // This is rare but can happen on some OEM devices
                Timber.w(e, "Failed to create EncryptedSharedPreferences, falling back to regular prefs")
                context.getSharedPreferences(PREFS_NAME + "_fallback", 0)
            }

            encryptedPrefs = prefs
            return prefs
        }
    }
}
