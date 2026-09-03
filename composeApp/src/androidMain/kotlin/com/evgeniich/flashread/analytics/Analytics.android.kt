package com.evgeniich.flashread.analytics

import android.content.Context
import android.os.Bundle
import com.evgeniich.flashread.platform.AndroidAppContext
import com.google.firebase.analytics.FirebaseAnalytics

actual object Analytics : AnalyticsLogger {
    override fun log(event: AnalyticsEvent) {
        if (!AndroidAppContext.isInitialized) return
        FirebaseAnalytics.getInstance(AndroidAppContext.applicationContext)
            .logEvent(event.name, event.toBundle())
    }
}

internal fun applyAnalyticsConsent(context: Context) {
    FirebaseAnalytics.getInstance(context).setConsent(
        mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
        ),
    )
}

internal fun AnalyticsEvent.toBundle(): Bundle {
    val bundle = Bundle(params.size)
    for ((key, value) in params) {
        bundle.putString(key, value)
    }
    return bundle
}
