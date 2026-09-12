package com.evgeniich.flashread.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.evgeniich.flashread.consent.ConsentManager
import com.evgeniich.flashread.platform.AndroidAppContext
import com.evgeniich.flashread.shared.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber
import java.lang.ref.WeakReference

/** Google test interstitial unit. Debug builds must not request production ads. */
private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

/**
 * Android implementation of [InterstitialAdHost] using AdMob.
 *
 * This class manages the lifecycle of interstitial ads:
 * - Preloading ads for quick display
 * - Showing ads with proper callback handling
 * - Automatically using test ads in debug builds
 */
class AndroidInterstitialAdHost private constructor() : InterstitialAdHost {

    @Volatile
    private var interstitialAd: InterstitialAd? = null

    @Volatile
    private var isLoading: Boolean = false

    @Volatile
    private var currentActivityRef: WeakReference<Activity>? = null

    override fun isAdReady(): Boolean = interstitialAd != null

    override fun preload(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        if (!canShowInterstitialAds()) {
            Timber.d("Cannot preload interstitial: ads not allowed")
            onFailed("Ads not allowed")
            return
        }

        if (isAdReady()) {
            Timber.d("Interstitial already loaded, skipping preload")
            onLoaded()
            return
        }

        if (isLoading) {
            Timber.d("Interstitial already loading, skipping preload")
            return
        }

        val context = AndroidAppContext.applicationContext
        val adUnitId = resolveInterstitialAdUnitId(context)

        isLoading = true
        Timber.d("Loading interstitial ad with unit ID: %s", adUnitId)

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.d("Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoading = false
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Interstitial ad failed to load: %d - %s", error.code, error.message)
                    interstitialAd = null
                    isLoading = false
                    onFailed("${error.code}: ${error.message}")
                }
            },
        )
    }

    override fun show(onResult: (InterstitialResult) -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            Timber.w("Interstitial ad not ready")
            onResult(InterstitialResult.NotReady)
            return
        }

        // Get activity - try explicit reference first, fall back to AndroidAppContext
        val activity = currentActivityRef?.get()
            ?: AndroidAppContext.currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Timber.w("No valid activity to show interstitial")
            onResult(InterstitialResult.Failed("No valid activity"))
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("Interstitial ad dismissed")
                interstitialAd = null
                onResult(InterstitialResult.Shown)
                // Preload next ad
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("Interstitial ad failed to show: %d - %s", error.code, error.message)
                interstitialAd = null
                onResult(InterstitialResult.Failed("${error.code}: ${error.message}"))
                // Preload next ad
                preload()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.d("Interstitial ad shown")
                // Clear the reference since it can only be shown once
                interstitialAd = null
            }

            override fun onAdClicked() {
                Timber.d("Interstitial ad clicked")
            }

            override fun onAdImpression() {
                Timber.d("Interstitial ad impression")
            }
        }

        Timber.d("Showing interstitial ad")
        ad.show(activity)
    }

    /**
     * Sets the current activity for showing ads.
     * Should be called when activity resumes.
     */
    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = activity?.let { WeakReference(it) }
    }

    companion object {
        @Volatile
        private var instance: AndroidInterstitialAdHost? = null

        fun getInstance(): AndroidInterstitialAdHost {
            return instance ?: synchronized(this) {
                instance ?: AndroidInterstitialAdHost().also { instance = it }
            }
        }
    }
}

actual fun getInterstitialAdHostInstance(): InterstitialAdHost = AndroidInterstitialAdHost.getInstance()

actual fun canShowInterstitialAds(): Boolean {
    return AdMobManager.isInitialized && ConsentManager.canRequestAds()
}

/**
 * Debug / debuggable builds always use the Google test interstitial unit.
 * Release builds use [R.string.admob_interstitial_unit_id] from the app module.
 */
private fun resolveInterstitialAdUnitId(context: Context): String {
    if (isDebuggable(context)) {
        Timber.d("Using test interstitial ad unit ID")
        return TEST_INTERSTITIAL_AD_UNIT_ID
    }
    val configured = try {
        context.getString(R.string.admob_interstitial_unit_id)
    } catch (e: Exception) {
        Timber.w("Resource admob_interstitial_unit_id not found")
        ""
    }
    if (configured.isBlank()) {
        Timber.e("Empty @string/admob_interstitial_unit_id; falling back to test interstitial unit")
        return TEST_INTERSTITIAL_AD_UNIT_ID
    }
    return configured
}

private fun isDebuggable(context: Context): Boolean {
    return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
