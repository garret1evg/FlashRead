package com.evgeniich.flashread.monetization

import com.evgeniich.flashread.navigation.AppRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central coordinator for the monetization system.
 *
 * This singleton manages all monetization state, coordinating between:
 * - [UsageTracker] for foreground time and unique days tracking
 * - [MonetizationPolicy] for ad placement decisions
 * - Platform-specific storage for persistence
 *
 * ## Responsibilities
 *
 * - Maintains the current [MonetizationState] as a reactive [StateFlow]
 * - Updates state based on usage events from [UsageTracker]
 * - Handles reward application from rewarded ads
 * - Tracks interstitial ad displays and enforces caps
 * - Manages developer mode unlock and level overrides
 * - Coordinates session lifecycle (new session detection, activity tracking)
 *
 * ## Usage
 *
 * ```kotlin
 * // Initialize once at app startup (after UsageTracker.init())
 * MonetizationManager.init()
 *
 * // Observe state in Compose
 * val state by MonetizationManager.state.collectAsState()
 *
 * // Check if banner should be shown
 * val showBanner = MonetizationPolicy.shouldShowBanner(state, currentRoute)
 *
 * // Record reading activity
 * MonetizationManager.recordReadingActivity()
 *
 * // Apply reward after watching rewarded ad
 * MonetizationManager.applyReward()
 * ```
 *
 * ## Thread Safety
 *
 * All state mutations are performed through atomic copy-on-write operations
 * with mutex synchronization. The [state] flow is safe to observe from any thread.
 */
object MonetizationManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _state = MutableStateFlow(MonetizationState.INITIAL)

    /**
     * Observable monetization state.
     * Collect this flow to reactively update UI based on monetization changes.
     */
    val state: StateFlow<MonetizationState> = _state.asStateFlow()

    /**
     * Current state snapshot.
     * Use [state] flow for reactive observation.
     */
    val currentState: MonetizationState
        get() = _state.value

    /**
     * Current applied ad level (after rewards and developer overrides).
     * Convenience property for quick ad level checks.
     */
    val appliedLevel: AdLevel
        get() = _state.value.appliedLevel

    /**
     * Whether the app is currently in foreground.
     * Delegates to [UsageTracker.isInForeground].
     */
    val isInForeground: StateFlow<Boolean>
        get() = UsageTracker.isInForeground

    private var initialized = false

    /**
     * Platform-specific state persistence callback.
     * Set by platform implementations to save state changes.
     */
    var onStatePersist: ((MonetizationState) -> Unit)? = null

    /**
     * Initializes the MonetizationManager.
     *
     * Must be called after [UsageTracker.init()] has been called.
     * Loads persisted state and sets up observers for usage changes.
     *
     * Multiple calls are safe and will be ignored.
     */
    fun init() {
        if (initialized) return
        scope.launch {
            mutex.withLock {
                if (initialized) return@withLock
                initialized = true

                // Load initial state from usage tracker
                refreshStateFromUsageTrackerInternal()

                // Observe foreground changes to update state
                UsageTracker.isInForeground
                    .onEach { inForeground ->
                        if (inForeground) {
                            onAppForeground()
                        } else {
                            onAppBackground()
                        }
                    }
                    .launchIn(scope)
            }
        }
    }

    /**
     * Initializes the MonetizationManager with a pre-loaded state.
     *
     * Use this on platforms that have persistent storage to restore
     * the state from the previous session before calling [init].
     *
     * @param initialState The state loaded from persistent storage.
     */
    fun initWithState(initialState: MonetizationState) {
        _state.value = initialState
    }

    // ==================== State Queries ====================

    /**
     * Checks if a banner should be shown on the given route.
     *
     * @param route The screen route to check.
     * @param isSpeedReadPlaying Whether speed read is currently playing.
     * @return True if a banner should be displayed.
     */
    fun shouldShowBanner(route: AppRoute, isSpeedReadPlaying: Boolean = false): Boolean {
        return MonetizationPolicy.shouldShowBanner(_state.value, route, isSpeedReadPlaying)
    }

    /**
     * Checks if an interstitial ad is eligible to be shown.
     *
     * @param context Runtime context for additional conditions.
     * @return True if an interstitial can be shown.
     */
    fun isInterstitialEligible(
        context: MonetizationPolicy.InterstitialContext = MonetizationPolicy.InterstitialContext(),
    ): Boolean {
        return MonetizationPolicy.isInterstitialEligible(
            state = _state.value,
            currentTimeMs = currentTimeMs(),
            context = context,
        )
    }

    /**
     * Gets the reason why an interstitial cannot be shown.
     *
     * @param context Runtime context for additional conditions.
     * @return Block reason or null if eligible.
     */
    fun getInterstitialBlockReason(
        context: MonetizationPolicy.InterstitialContext = MonetizationPolicy.InterstitialContext(),
    ): InterstitialBlockReason? {
        return MonetizationPolicy.getInterstitialBlockReason(
            state = _state.value,
            currentTimeMs = currentTimeMs(),
            context = context,
        )
    }

    /**
     * Checks if a rewarded ad can be offered to the user.
     *
     * @return True if rewarded ad should be shown as an option.
     */
    fun canOfferRewardedAd(): Boolean {
        return MonetizationPolicy.canOfferRewardedAd(_state.value, currentTimeMs())
    }

    /**
     * Whether the one-time rewarded-ad hint should be presented.
     *
     * The caller must also ensure this is a safe after-reading opportunity
     * (Library arrival from a reading screen).
     */
    fun shouldShowRewardHint(): Boolean {
        return MonetizationPolicy.shouldShowRewardHint(
            state = _state.value,
            isAppInForeground = UsageTracker.isInForeground.value,
        )
    }

    /**
     * Gets progress information towards the next ad level.
     *
     * @return Progress info or null if at max level.
     */
    fun getLevelProgress(): LevelProgress? {
        return MonetizationPolicy.getLevelProgress(_state.value)
    }

    /**
     * Checks if a reward is currently active.
     */
    fun isRewardActive(): Boolean {
        return _state.value.isRewardActive(currentTimeMs())
    }

    /**
     * Gets the remaining reward duration in milliseconds.
     *
     * @return Remaining duration or null if no active reward.
     */
    fun getRemainingRewardMs(): Long? {
        val expiresAt = _state.value.rewardExpiresAtMs ?: return null
        val remaining = expiresAt - currentTimeMs()
        return if (remaining > 0) remaining else null
    }

    // ==================== State Mutations ====================

    /**
     * Records that reading activity occurred in the current session.
     * This is required before interstitials can be shown.
     *
     * Call this when:
     * - User scrolls or taps in the normal reader
     * - User starts playback in speed read
     */
    fun recordReadingActivity() {
        updateState { it.withReadingActivity() }
    }

    /**
     * Clears the reading activity flag.
     * Call this after an interstitial opportunity is consumed (shown) or skipped
     * (ad not ready, not eligible, etc.) to prevent accumulation of skipped opportunities.
     */
    fun clearReadingActivity() {
        updateState { it.withClearedReadingActivity() }
    }

    /**
     * Applies a reward after the user watches a rewarded ad.
     * This grants an ad-free period as configured in [MonetizationConfig.rewardDuration].
     */
    fun applyReward() {
        val now = currentTimeMs()
        updateState { it.withReward(now) }
        persistState()
    }

    /**
     * Removes the active reward immediately without launching an ad.
     * Does not erase [MonetizationState.hasEverEarnedReward] or hint history,
     * and does not reset daily/session interstitial caps.
     */
    fun clearReward() {
        updateState { it.withClearedReward() }
        persistState()
    }

    /**
     * Records that the rewarded-ad hint has been presented.
     * Call this when the hint dialog is actually shown, not only after dismiss.
     */
    fun markRewardHintShown() {
        updateState { it.withRewardHintShown() }
        persistState()
    }

    /**
     * Records that an interstitial ad was shown.
     * Updates cooldown timer and increments session/daily counters.
     */
    fun recordInterstitialShown() {
        val now = currentTimeMs()
        val date = currentDateString()
        updateState { it.withInterstitialShown(now, date) }
        persistState()
    }

    /**
     * Starts a new session, resetting session-specific counters.
     * Called automatically when session timeout is detected.
     */
    fun startNewSession() {
        val now = currentTimeMs()
        updateState { it.withSessionStart(now) }
    }

    // ==================== Developer Mode ====================

    /**
     * Unlocks developer mode.
     * After unlocking, developer settings become visible and level override becomes available.
     */
    fun unlockDeveloperMode() {
        updateState { it.withDeveloperModeUnlocked() }
        persistState()
    }

    /**
     * Locks developer mode.
     * Hides the developer section and clears the unlock flag.
     * Does not change ads, usage, or any other state.
     */
    fun lockDeveloperMode() {
        updateState { it.withDeveloperModeLocked() }
        persistState()
    }

    /**
     * Checks if developer mode is unlocked.
     */
    fun isDeveloperModeUnlocked(): Boolean {
        return _state.value.developerModeUnlocked
    }

    /**
     * Sets or clears the developer level override.
     *
     * @param level The level to force, or null to return to calculated level.
     */
    fun setDeveloperLevelOverride(level: AdLevel?) {
        updateState { it.withDeveloperOverride(level) }
        persistState()
    }

    /**
     * Gets the current developer level override.
     *
     * @return The override level or null if using calculated level.
     */
    fun getDeveloperLevelOverride(): AdLevel? {
        return _state.value.developerLevelOverride
    }

    /**
     * Sets the total active usage time and recalculates the ad level.
     *
     * This method is for developer diagnostics only. It:
     * - Persists the new total usage time directly
     * - Resets the foreground timer baseline (prevents double-counting)
     * - Recalculates calculatedLevel immediately
     * - Applies layout level immediately (Settings is a safe boundary)
     * - If the new value no longer qualifies for Level 2, clears [level2EverActivated]
     * - If Level 2 applied status changes, resets interstitial eligibility counter to 0
     * - Does NOT add usage days
     * - Does NOT grant/revoke reward, reset ad caps, erase reward/hint history
     *
     * @param totalMs The new total usage time in milliseconds.
     */
    fun setActiveUsageTime(totalMs: Long) {
        // First, persist to UsageTracker (also resets foreground timer)
        UsageTracker.setTotalUsageMs(totalMs)

        // Now update state
        updateState { currentState ->
            val newCalculated = AdLevel.calculate(totalMs, currentState.uniqueUsageDays)
            val wasLevel2Applied = currentState.appliedLayoutLevel == AdLevel.Level2
            val willBeLevel2 = newCalculated == AdLevel.Level2

            // Determine new level2EverActivated:
            // - If new value no longer qualifies for Level2, clear the latch
            // - Otherwise, keep the current value (will be set true when Level2 is applied)
            val newLevel2EverActivated = if (!willBeLevel2) false else currentState.level2EverActivated

            // Determine interstitial eligibility counter:
            // - If Level2 applied status changes, reset to 0
            // - If still Level2 applied, keep counter
            val newEligibilityMs = when {
                wasLevel2Applied != willBeLevel2 -> 0L // Status changed
                !willBeLevel2 -> 0L // Not Level2
                else -> currentState.interstitialEligibilityAccumulatedMs // Keep
            }

            currentState.copy(
                totalUsageMs = totalMs,
                calculatedLevel = newCalculated,
                appliedLayoutLevel = newCalculated, // Apply immediately (Settings is safe)
                level2EverActivated = if (newCalculated == AdLevel.Level2) true else newLevel2EverActivated,
                interstitialEligibilityAccumulatedMs = newEligibilityMs,
            )
        }

        persistState()
    }

    /**
     * Sets the unique usage-day count and recalculates the ad level.
     *
     * This method is for developer diagnostics only. It:
     * - Persists a real date list of size [count] (keeping recent dates when shrinking,
     *   adding synthetic past dates when expanding)
     * - Recalculates calculatedLevel immediately from unchanged [totalUsageMs]
     * - Applies layout level immediately (Settings is a safe boundary)
     * - If the new value no longer qualifies for Level 2, clears [level2EverActivated]
     * - If Level 2 applied status changes, resets interstitial eligibility counter to 0
     * - Does NOT change total usage time, reward, daily/session ad caps, or hint history
     *
     * @param count The desired number of unique usage days (clamped to >= 0).
     */
    fun setUsageDays(count: Int) {
        val clampedCount = count.coerceAtLeast(0)
        val newDates = buildUsageDatesForCount(
            existing = _state.value.usageDates,
            count = clampedCount,
            today = currentDateString(),
        )
        UsageTracker.setUsageDates(newDates)

        updateState { currentState ->
            val newCalculated = AdLevel.calculate(currentState.totalUsageMs, newDates.size)
            val wasLevel2Applied = currentState.appliedLayoutLevel == AdLevel.Level2
            val willBeLevel2 = newCalculated == AdLevel.Level2

            val newLevel2EverActivated = if (!willBeLevel2) false else currentState.level2EverActivated

            val newEligibilityMs = when {
                wasLevel2Applied != willBeLevel2 -> 0L
                !willBeLevel2 -> 0L
                else -> currentState.interstitialEligibilityAccumulatedMs
            }

            currentState.copy(
                uniqueUsageDays = newDates.size,
                usageDates = newDates,
                calculatedLevel = newCalculated,
                appliedLayoutLevel = newCalculated, // Apply immediately (Settings is safe)
                level2EverActivated = if (newCalculated == AdLevel.Level2) true else newLevel2EverActivated,
                interstitialEligibilityAccumulatedMs = newEligibilityMs,
            )
        }

        persistState()
    }

    /**
     * Applies the calculated level to layout at a safe boundary.
     * Call this when navigating to Settings, returning to Library from reading,
     * or other safe screens.
     */
    fun applyLayoutLevel() {
        updateState { currentState ->
            currentState.withAppliedLayout(currentState.calculatedLevel)
        }
        persistState()
    }

    /**
     * Resets all monetization data (usage, rewards, developer mode).
     * Used for testing or at user request.
     */
    fun reset() {
        UsageTracker.reset()
        _state.value = MonetizationState.INITIAL
        persistState()
    }

    // ==================== Internal Logic ====================

    /**
     * Called when the app enters foreground.
     * Checks for new session and refreshes state from usage tracker.
     */
    private fun onAppForeground() {
        val now = currentTimeMs()
        val currentState = _state.value

        // Check if this is a new session
        if (currentState.isNewSession(now)) {
            scope.launch {
                startNewSession()
            }
        }

        // Refresh usage data from tracker
        refreshStateFromUsageTracker()
    }

    /**
     * Called when the app enters background.
     * Persists usage data.
     */
    private fun onAppBackground() {
        UsageTracker.persistCurrentUsage()
        refreshStateFromUsageTracker()
        persistState()
    }

    /**
     * Refreshes state with current usage data from [UsageTracker].
     *
     * @param touchLastActivity When true, also updates [MonetizationState.lastActivityMs].
     * Diagnostic polling should pass false so a live usage tick does not keep
     * the interstitial session alive.
     */
    fun refreshStateFromUsageTracker(touchLastActivity: Boolean = true) {
        refreshStateFromUsageTrackerInternal(touchLastActivity)
    }

    /**
     * Internal implementation of state refresh from usage tracker.
     */
    private fun refreshStateFromUsageTrackerInternal(touchLastActivity: Boolean = true) {
        val now = currentTimeMs()
        val totalUsageMs = UsageTracker.totalUsageMs
        val uniqueDays = UsageTracker.uniqueUsageDays
        val usageDates = UsageTracker.usageDates
        val calculatedLevel = AdLevel.calculate(totalUsageMs, uniqueDays)

        updateState { currentState ->
            val nextLastActivityMs = if (touchLastActivity) now else currentState.lastActivityMs
            if (
                currentState.totalUsageMs == totalUsageMs &&
                currentState.uniqueUsageDays == uniqueDays &&
                currentState.usageDates == usageDates &&
                currentState.calculatedLevel == calculatedLevel &&
                currentState.lastActivityMs == nextLastActivityMs
            ) {
                currentState
            } else {
                currentState.copy(
                    totalUsageMs = totalUsageMs,
                    uniqueUsageDays = uniqueDays,
                    usageDates = usageDates,
                    calculatedLevel = calculatedLevel,
                    lastActivityMs = nextLastActivityMs,
                )
            }
        }
    }

    /**
     * Atomically updates the state using a transformation function.
     */
    private fun updateState(transform: (MonetizationState) -> MonetizationState) {
        _state.value = transform(_state.value)
    }

    /**
     * Persists the current state via platform-specific callback.
     */
    private fun persistState() {
        onStatePersist?.invoke(_state.value)
    }

    /**
     * Returns the current time in epoch milliseconds.
     */
    private fun currentTimeMs(): Long = TimeProvider.currentTimeMs()

    /**
     * Returns the current date as YYYY-MM-DD string.
     */
    private fun currentDateString(): String = TimeProvider.currentDateString()

    // ==================== Testing Support ====================

    /**
     * Sets the state directly for testing purposes.
     * Only use in tests.
     */
    internal fun setStateForTesting(state: MonetizationState) {
        _state.value = state
    }

    /**
     * Resets initialization flag for testing.
     * Only use in tests.
     */
    internal fun resetForTesting() {
        scope.launch {
            mutex.withLock {
                initialized = false
                _state.value = MonetizationState.INITIAL
                onStatePersist = null
            }
        }
    }
}
