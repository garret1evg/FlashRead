package com.evgeniich.flashread.crash

import android.content.Context
import com.evgeniich.flashread.platform.AndroidAppContext
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

actual object CrashReporting : CrashLogger {
    override fun recordException(throwable: Throwable) {
        if (!AndroidAppContext.isInitialized) return
        firebaseCrashlyticsOrNull(AndroidAppContext.applicationContext)
            ?.recordException(throwable)
    }

    override fun log(message: String) {
        if (!AndroidAppContext.isInitialized) return
        firebaseCrashlyticsOrNull(AndroidAppContext.applicationContext)
            ?.log(message)
    }
}

internal fun applyCrashlyticsConsent(context: Context) {
    val crashlytics = firebaseCrashlyticsOrNull(context) ?: return
    val allowed = UserMessagingPlatform.getConsentInformation(context).canRequestAds()
    crashlytics.isCrashlyticsCollectionEnabled = allowed
}

internal fun firebaseCrashlyticsOrNull(context: Context): FirebaseCrashlytics? {
    if (FirebaseApp.getApps(context).isEmpty()) return null
    return FirebaseCrashlytics.getInstance()
}
