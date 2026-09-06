package com.evgeniich.flashread.analytics

import android.content.Context
import android.os.Bundle
import com.evgeniich.flashread.platform.AndroidAppContext
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

actual object Analytics : AnalyticsLogger {
    override fun log(event: AnalyticsEvent) {
        if (!AndroidAppContext.isInitialized) return
        firebaseAnalyticsOrNull(AndroidAppContext.applicationContext)
            ?.logEvent(event.name, event.toBundle())
    }
}

internal fun applyAnalyticsConsent(context: Context) {
    val analytics = firebaseAnalyticsOrNull(context) ?: return
    val allowed = UserMessagingPlatform.getConsentInformation(context).canRequestAds()
    val status = if (allowed) {
        FirebaseAnalytics.ConsentStatus.GRANTED
    } else {
        FirebaseAnalytics.ConsentStatus.DENIED
    }
    analytics.setConsent(
        mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to status,
            FirebaseAnalytics.ConsentType.AD_STORAGE to status,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to status,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
        ),
    )
    analytics.setAnalyticsCollectionEnabled(allowed)
}

private fun firebaseAnalyticsOrNull(context: Context): FirebaseAnalytics? {
    if (FirebaseApp.getApps(context).isEmpty()) return null
    return FirebaseAnalytics.getInstance(context)
}

internal fun AnalyticsEvent.toBundle(): Bundle {
    val bundle = Bundle(params.size)
    for ((key, value) in params) {
        bundle.putString(key, value)
    }
    return bundle
}
