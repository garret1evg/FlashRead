package com.evgeniich.flashread.ads

/**
 * Result of an interstitial ad operation.
 */
sealed class InterstitialResult {
    /** Ad was shown and dismissed by the user. */
    data object Shown : InterstitialResult()

    /** Ad failed to load or show. */
    data class Failed(val reason: String) : InterstitialResult()

    /** Ad was cancelled (e.g., app went to background). */
    data object Cancelled : InterstitialResult()

    /** Ad is not ready (not preloaded). */
    data object NotReady : InterstitialResult()
}

/**
 * Callback interface for interstitial ad events.
 */
interface InterstitialAdCallback {
    /** Called when the ad is dismissed. */
    fun onAdDismissed()

    /** Called when the ad fails to show. */
    fun onAdShowFailed(reason: String)
}

/**
 * Interface for managing interstitial advertisements.
 * On Android, this uses AdMob interstitials.
 * On iOS, this is a no-op stub.
 *
 * ## Usage
 *
 * ```kotlin
 * // Get the singleton instance
 * val host = InterstitialAdHost.getInstance()
 *
 * // Preload an ad (call early, e.g., on app start)
 * host.preload()
 *
 * // Check if ad is ready
 * if (host.isAdReady()) {
 *     // Show the ad
 *     host.show { result ->
 *         when (result) {
 *             InterstitialResult.Shown -> { /* Ad was shown */ }
 *             is InterstitialResult.Failed -> { /* Handle failure */ }
 *             InterstitialResult.Cancelled -> { /* User cancelled */ }
 *             InterstitialResult.NotReady -> { /* Ad wasn't ready */ }
 *         }
 *     }
 * }
 * ```
 */
interface InterstitialAdHost {
    /**
     * Whether an interstitial ad is preloaded and ready to show.
     */
    fun isAdReady(): Boolean

    /**
     * Preloads an interstitial ad.
     *
     * Should be called early (e.g., on app initialization or after showing an ad)
     * to ensure an ad is ready when needed.
     *
     * @param onLoaded Callback when ad is successfully loaded.
     * @param onFailed Callback when ad fails to load.
     */
    fun preload(
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {},
    )

    /**
     * Shows the preloaded interstitial ad.
     *
     * The ad must be preloaded first via [preload].
     * After showing, a new ad should be preloaded for the next display.
     *
     * @param onResult Callback with the result of showing the ad.
     */
    fun show(onResult: (InterstitialResult) -> Unit)

    companion object {
        /**
         * Returns the platform-specific singleton instance.
         */
        fun getInstance(): InterstitialAdHost = getInterstitialAdHostInstance()
    }
}

/**
 * Returns the platform-specific InterstitialAdHost singleton.
 */
expect fun getInterstitialAdHostInstance(): InterstitialAdHost

/**
 * Returns true if interstitial ads can be shown on this platform.
 * On Android, this checks if consent has been given and AdMob is initialized.
 * On iOS, this always returns false.
 */
expect fun canShowInterstitialAds(): Boolean
