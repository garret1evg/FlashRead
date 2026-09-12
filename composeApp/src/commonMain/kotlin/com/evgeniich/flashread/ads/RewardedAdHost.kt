package com.evgeniich.flashread.ads

/**
 * Result of a rewarded ad operation.
 */
sealed class RewardedAdResult {
    /** User watched the ad and earned the reward. */
    data object RewardEarned : RewardedAdResult()

    /** User closed the ad without earning the reward. */
    data object ClosedWithoutReward : RewardedAdResult()

    /** Ad failed to load or show. Contains an English error message suitable for UI. */
    data class Failed(val reason: String) : RewardedAdResult()

    /** Ad was cancelled (e.g., app went to background or cancelPendingShow() was called). */
    data object Cancelled : RewardedAdResult()

    /** Ad is not ready (not preloaded). */
    data object NotReady : RewardedAdResult()
}

/**
 * Interface for managing rewarded advertisements.
 * On Android, this uses AdMob rewarded ads.
 * On iOS, this is a no-op stub.
 *
 * ## Usage
 *
 * ```kotlin
 * // Get the singleton instance
 * val host = RewardedAdHost.getInstance()
 *
 * // Preload an ad (optional, for faster display)
 * host.preload()
 *
 * // Show the ad (user action required)
 * host.show { result ->
 *     when (result) {
 *         RewardedAdResult.RewardEarned -> {
 *             // Reward already applied via MonetizationManager.applyReward()
 *         }
 *         RewardedAdResult.ClosedWithoutReward -> {
 *             // User dismissed without watching fully
 *         }
 *         is RewardedAdResult.Failed -> {
 *             // Show error to user: result.reason
 *         }
 *         RewardedAdResult.Cancelled -> {
 *             // Ad was cancelled (backgrounded or cancelPendingShow())
 *         }
 *         RewardedAdResult.NotReady -> {
 *             // Ad wasn't ready, show loading or retry option
 *         }
 *     }
 * }
 * ```
 *
 * ## Important Notes
 *
 * - Rewarded ads are **never auto-shown**; they require explicit user action via [show].
 * - Rewards are granted **only** when AdMob confirms via `onUserEarnedReward`.
 * - Call [cancelPendingShow] when the user leaves the initiating screen to prevent
 *   showing the ad after navigation.
 * - Rewarded ads do NOT count against interstitial session/daily caps.
 */
interface RewardedAdHost {
    /**
     * Whether a rewarded ad is preloaded and ready to show.
     */
    fun isAdReady(): Boolean

    /**
     * Whether an ad is currently being loaded.
     */
    fun isLoading(): Boolean

    /**
     * Preloads a rewarded ad for faster display.
     *
     * Should be called after the user indicates interest in watching a rewarded ad
     * (e.g., when the "Watch Ad" button becomes visible).
     *
     * @param onLoaded Callback when ad is successfully loaded.
     * @param onFailed Callback when ad fails to load with an English error message.
     */
    fun preload(
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {},
    )

    /**
     * Shows the rewarded ad. This is an explicit user action.
     *
     * If no ad is preloaded, this will attempt to load one first, then show it.
     * The ad will only be shown if all conditions are met:
     * - Consent allows ad requests
     * - AdMob is initialized
     * - [cancelPendingShow] was not called
     * - App is in foreground with a valid activity
     *
     * After a successful reward, [MonetizationManager.applyReward] is called automatically.
     *
     * @param onResult Callback with the result of the operation.
     */
    fun show(onResult: (RewardedAdResult) -> Unit)

    /**
     * Cancels any pending show operation.
     *
     * Call this when the user leaves the screen that initiated the rewarded ad
     * to prevent showing the ad after navigation.
     *
     * If called while an ad is loading, the result callback will receive [RewardedAdResult.Cancelled].
     */
    fun cancelPendingShow()

    companion object {
        /**
         * Returns the platform-specific singleton instance.
         */
        fun getInstance(): RewardedAdHost = getRewardedAdHostInstance()
    }
}

/**
 * Returns the platform-specific RewardedAdHost singleton.
 */
expect fun getRewardedAdHostInstance(): RewardedAdHost

/**
 * Returns true if rewarded ads can be shown on this platform.
 * On Android, this checks if consent has been given and AdMob is initialized.
 * On iOS, this always returns false.
 */
expect fun canShowRewardedAds(): Boolean
