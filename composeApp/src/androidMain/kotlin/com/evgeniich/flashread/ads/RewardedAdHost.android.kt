package com.evgeniich.flashread.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.evgeniich.flashread.consent.ConsentManager
import com.evgeniich.flashread.monetization.AppLifecycleTracker
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.monetization.MonetizationPolicy
import com.evgeniich.flashread.monetization.TimeProvider
import com.evgeniich.flashread.monetization.UsageTracker
import com.evgeniich.flashread.platform.AndroidAppContext
import com.evgeniich.flashread.shared.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import timber.log.Timber
import java.lang.ref.WeakReference

/** Google test rewarded unit. Debug builds must not request production ads. */
private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

/**
 * Android implementation of [RewardedAdHost] using AdMob.
 *
 * This class manages the lifecycle of rewarded ads:
 * - Preloading ads for quick display
 * - Showing ads with proper callback handling
 * - Granting rewards only via AdMob's `onUserEarnedReward` callback
 * - Automatically using test ads in debug builds
 *
 * ## Thread Safety
 *
 * All state fields are volatile. AdMob callbacks run on the main thread.
 * State transitions are designed to be atomic and safe from race conditions.
 *
 * ## Reward Idempotency
 *
 * Each ad impression has a unique [showGeneration] counter. Rewards are only
 * applied once per impression, preventing duplicate rewards from multiple
 * `onUserEarnedReward` callbacks (which can theoretically happen).
 *
 * ## Usage Timer Handling
 *
 * When showing a rewarded ad, the usage timer is paused by persisting current
 * usage and resetting the foreground timer. This prevents the full-screen ad
 * overlay from counting as active app usage. On ad close, the timer is reset
 * again to restart from a clean baseline.
 */
class AndroidRewardedAdHost private constructor() : RewardedAdHost {

    @Volatile
    private var rewardedAd: RewardedAd? = null

    @Volatile
    private var isLoadingAd: Boolean = false

    @Volatile
    private var currentActivityRef: WeakReference<Activity>? = null

    /**
     * Generation counter for show operations.
     * Incremented each time [show] is called.
     * Used to detect stale callbacks and ensure idempotent reward application.
     */
    @Volatile
    private var showGeneration: Int = 0

    /**
     * Whether [cancelPendingShow] was called for the current [showGeneration].
     */
    @Volatile
    private var isCancelled: Boolean = false

    /**
     * The generation for which a reward was already applied.
     * Prevents duplicate reward application from multiple `onUserEarnedReward` callbacks.
     */
    @Volatile
    private var rewardedGeneration: Int = -1

    /**
     * Pending result callback for the current show operation.
     */
    @Volatile
    private var pendingResultCallback: ((RewardedAdResult) -> Unit)? = null

    /**
     * Whether a reward was earned for the current show operation.
     */
    @Volatile
    private var rewardEarnedForCurrentShow: Boolean = false

    override fun isAdReady(): Boolean = rewardedAd != null

    override fun isLoading(): Boolean = isLoadingAd

    override fun preload(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        if (!canShowRewardedAds()) {
            Timber.d("Cannot preload rewarded ad: ads not allowed")
            onFailed("Ads are not available right now. Please try again later.")
            return
        }

        if (isAdReady()) {
            Timber.d("Rewarded ad already loaded, skipping preload")
            onLoaded()
            return
        }

        if (isLoadingAd) {
            Timber.d("Rewarded ad already loading, skipping preload")
            return
        }

        loadAd(onLoaded, onFailed)
    }

    override fun show(onResult: (RewardedAdResult) -> Unit) {
        // Increment generation and reset state for this show attempt
        showGeneration++
        isCancelled = false
        rewardEarnedForCurrentShow = false
        pendingResultCallback = onResult

        val currentGen = showGeneration

        Timber.d("Rewarded ad show requested, generation: %d", currentGen)

        // Check if rewarded ads can be offered according to policy
        if (!MonetizationPolicy.canOfferRewardedAd(
                MonetizationManager.currentState,
                TimeProvider.currentTimeMs(),
            )
        ) {
            Timber.d("Rewarded ad not allowed by policy (reward already active or initial period)")
            deliverResult(currentGen, RewardedAdResult.Failed("Ads are not available right now. Please try again later."))
            return
        }

        // Check consent and initialization
        if (!canShowRewardedAds()) {
            Timber.d("Cannot show rewarded ad: ads not allowed")
            deliverResult(currentGen, RewardedAdResult.Failed("Ads are not available right now. Please try again later."))
            return
        }

        if (isAdReady()) {
            showLoadedAd(currentGen)
        } else {
            // Load first, then show
            Timber.d("No rewarded ad loaded, loading first")
            loadAd(
                onLoaded = {
                    // Check if cancelled while loading
                    if (isCancelled || currentGen != showGeneration) {
                        Timber.d("Show cancelled while loading, generation: %d vs %d", currentGen, showGeneration)
                        deliverResult(currentGen, RewardedAdResult.Cancelled)
                        return@loadAd
                    }
                    showLoadedAd(currentGen)
                },
                onFailed = { error ->
                    Timber.w("Rewarded ad failed to load: %s", error)
                    deliverResult(currentGen, RewardedAdResult.Failed("The ad failed to load. Please try again."))
                },
            )
        }
    }

    override fun cancelPendingShow() {
        Timber.d("Cancelling pending rewarded ad show, generation: %d", showGeneration)
        isCancelled = true
        // Note: if ad is currently showing, this won't dismiss it - that's intentional.
        // This only prevents showing an ad that's still loading.
    }

    /**
     * Sets the current activity for showing ads.
     * Should be called when activity resumes.
     *
     * **Note:** Later UI wiring will call this from MainActivity's lifecycle.
     * For now, [show] falls back to [AndroidAppContext.currentActivity].
     */
    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = activity?.let { WeakReference(it) }
    }

    /**
     * Loads a rewarded ad.
     */
    private fun loadAd(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        val context = AndroidAppContext.applicationContext
        val adUnitId = resolveRewardedAdUnitId(context)

        isLoadingAd = true
        Timber.d("Loading rewarded ad with unit ID: %s", adUnitId)

        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.d("Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isLoadingAd = false
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Rewarded ad failed to load: %d - %s", error.code, error.message)
                    rewardedAd = null
                    isLoadingAd = false
                    onFailed("${error.code}: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows the currently loaded ad after all preconditions are met.
     */
    private fun showLoadedAd(generation: Int) {
        // Re-check cancellation
        if (isCancelled || generation != showGeneration) {
            Timber.d("Show cancelled before presenting, generation: %d vs %d", generation, showGeneration)
            deliverResult(generation, RewardedAdResult.Cancelled)
            return
        }

        // Re-check consent (user might have revoked while loading)
        if (!canShowRewardedAds()) {
            Timber.d("Consent revoked before showing rewarded ad")
            deliverResult(generation, RewardedAdResult.Failed("Ads are not available right now. Please try again later."))
            return
        }

        // Re-check policy (reward might have been applied by another means)
        if (!MonetizationPolicy.canOfferRewardedAd(
                MonetizationManager.currentState,
                TimeProvider.currentTimeMs(),
            )
        ) {
            Timber.d("Policy no longer allows rewarded ad")
            deliverResult(generation, RewardedAdResult.Failed("Ads are not available right now. Please try again later."))
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            Timber.w("Rewarded ad not ready (was consumed or never loaded)")
            deliverResult(generation, RewardedAdResult.NotReady)
            return
        }

        // Get activity - try explicit reference first, fall back to AndroidAppContext
        val activity = currentActivityRef?.get()
            ?: AndroidAppContext.currentActivity

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Timber.w("No valid activity to show rewarded ad")
            deliverResult(generation, RewardedAdResult.Failed("The ad could not be shown. Please try again."))
            return
        }

        // Check if app is in foreground
        if (!AppLifecycleTracker.foreground) {
            Timber.w("App is in background, not showing rewarded ad")
            deliverResult(generation, RewardedAdResult.Cancelled)
            return
        }

        // Pause usage timer before showing ad
        // This persists current usage and resets the foreground baseline
        Timber.d("Pausing usage timer before rewarded ad")
        UsageTracker.persistCurrentUsage()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("Rewarded ad dismissed, generation: %d, reward earned: %b", generation, rewardEarnedForCurrentShow)
                rewardedAd = null

                // Reset foreground timer so overlay time is excluded
                AppLifecycleTracker.resetForegroundTimer()

                if (rewardEarnedForCurrentShow) {
                    deliverResult(generation, RewardedAdResult.RewardEarned)
                } else {
                    deliverResult(generation, RewardedAdResult.ClosedWithoutReward)
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("Rewarded ad failed to show: %d - %s", error.code, error.message)
                rewardedAd = null

                // Reset foreground timer even on failure
                AppLifecycleTracker.resetForegroundTimer()

                deliverResult(generation, RewardedAdResult.Failed("The ad could not be shown. Please try again."))
            }

            override fun onAdShowedFullScreenContent() {
                Timber.d("Rewarded ad shown, generation: %d", generation)
                // Clear the reference since it can only be shown once
                rewardedAd = null
            }

            override fun onAdClicked() {
                Timber.d("Rewarded ad clicked")
            }

            override fun onAdImpression() {
                Timber.d("Rewarded ad impression")
            }
        }

        Timber.d("Showing rewarded ad, generation: %d", generation)
        ad.show(activity) { rewardItem ->
            // onUserEarnedReward callback - this is the ONLY place rewards are granted
            Timber.d(
                "User earned reward: %d %s, generation: %d",
                rewardItem.amount,
                rewardItem.type,
                generation,
            )

            // Idempotency check: only apply reward once per generation
            if (rewardedGeneration != generation) {
                rewardedGeneration = generation
                rewardEarnedForCurrentShow = true
                MonetizationManager.applyReward()
                Timber.d("Reward applied via MonetizationManager")
            } else {
                Timber.w("Duplicate reward callback ignored for generation: %d", generation)
            }
        }
    }

    /**
     * Delivers the result to the pending callback if the generation matches.
     */
    private fun deliverResult(generation: Int, result: RewardedAdResult) {
        if (generation == showGeneration) {
            pendingResultCallback?.invoke(result)
            pendingResultCallback = null
        } else {
            Timber.d("Ignoring stale result for generation: %d (current: %d)", generation, showGeneration)
        }
    }

    companion object {
        @Volatile
        private var instance: AndroidRewardedAdHost? = null

        fun getInstance(): AndroidRewardedAdHost {
            return instance ?: synchronized(this) {
                instance ?: AndroidRewardedAdHost().also { instance = it }
            }
        }
    }
}

actual fun getRewardedAdHostInstance(): RewardedAdHost = AndroidRewardedAdHost.getInstance()

actual fun canShowRewardedAds(): Boolean {
    return AdMobManager.isInitialized && ConsentManager.canRequestAds()
}

/**
 * Debug / debuggable builds always use the Google test rewarded unit.
 * Release builds use [R.string.admob_rewarded_unit_id] from the app module.
 */
private fun resolveRewardedAdUnitId(context: Context): String {
    if (isDebuggable(context)) {
        Timber.d("Using test rewarded ad unit ID")
        return TEST_REWARDED_AD_UNIT_ID
    }
    val configured = try {
        context.getString(R.string.admob_rewarded_unit_id)
    } catch (e: Exception) {
        Timber.w("Resource admob_rewarded_unit_id not found")
        ""
    }
    if (configured.isBlank()) {
        Timber.e("Empty @string/admob_rewarded_unit_id; falling back to test rewarded unit")
        return TEST_REWARDED_AD_UNIT_ID
    }
    return configured
}

private fun isDebuggable(context: Context): Boolean {
    return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
