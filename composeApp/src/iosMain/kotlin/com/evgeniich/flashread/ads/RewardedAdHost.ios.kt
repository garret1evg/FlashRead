package com.evgeniich.flashread.ads

/**
 * No-op implementation for iOS.
 * Rewarded ads are not supported on iOS in this implementation.
 */
class IosRewardedAdHost private constructor() : RewardedAdHost {

    override fun isAdReady(): Boolean = false

    override fun isLoading(): Boolean = false

    override fun preload(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        // No-op on iOS - no rewarded ads
        onFailed("Rewarded ads not supported on iOS")
    }

    override fun show(onResult: (RewardedAdResult) -> Unit) {
        // No-op on iOS - no rewarded ads
        onResult(RewardedAdResult.NotReady)
    }

    override fun cancelPendingShow() {
        // No-op on iOS
    }

    companion object {
        private val instance = IosRewardedAdHost()

        fun getInstance(): IosRewardedAdHost = instance
    }
}

actual fun getRewardedAdHostInstance(): RewardedAdHost = IosRewardedAdHost.getInstance()

/**
 * Always returns false on iOS as rewarded ads are not supported.
 */
actual fun canShowRewardedAds(): Boolean = false
