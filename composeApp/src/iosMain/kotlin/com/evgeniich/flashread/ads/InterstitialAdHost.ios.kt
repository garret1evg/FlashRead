package com.evgeniich.flashread.ads

/**
 * No-op implementation for iOS.
 * Interstitial ads are not supported on iOS in this implementation.
 */
class IosInterstitialAdHost private constructor() : InterstitialAdHost {

    override fun isAdReady(): Boolean = false

    override fun preload(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        // No-op on iOS - no interstitial ads
        onFailed("Interstitial ads not supported on iOS")
    }

    override fun show(onResult: (InterstitialResult) -> Unit) {
        // No-op on iOS - no interstitial ads
        onResult(InterstitialResult.NotReady)
    }

    companion object {
        private val instance = IosInterstitialAdHost()

        fun getInstance(): IosInterstitialAdHost = instance
    }
}

actual fun getInterstitialAdHostInstance(): InterstitialAdHost = IosInterstitialAdHost.getInstance()

/**
 * Always returns false on iOS as interstitial ads are not supported.
 */
actual fun canShowInterstitialAds(): Boolean = false
