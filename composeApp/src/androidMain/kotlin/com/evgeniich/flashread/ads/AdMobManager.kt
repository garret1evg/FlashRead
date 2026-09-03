package com.evgeniich.flashread.ads

import android.content.Context
import com.evgeniich.flashread.consent.ConsentManager
import com.google.android.gms.ads.MobileAds
import timber.log.Timber

object AdMobManager {
    @Volatile
    private var initialized = false

    fun initializeIfAllowed(context: Context, onComplete: () -> Unit = {}) {
        if (initialized) {
            Timber.d("AdMob already initialized, skipping")
            onComplete()
            return
        }
        if (!ConsentManager.canRequestAds()) {
            Timber.d("Cannot request ads, skipping AdMob initialization")
            return
        }
        synchronized(this) {
            if (initialized) {
                Timber.d("AdMob already initialized (double-check), skipping")
                onComplete()
                return
            }
            Timber.d("Initializing AdMob SDK")
            MobileAds.initialize(context) {
                initialized = true
                Timber.d("AdMob SDK initialized successfully")
                onComplete()
            }
        }
    }

    val isInitialized: Boolean get() = initialized
}
