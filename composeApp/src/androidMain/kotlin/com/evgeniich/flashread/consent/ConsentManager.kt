package com.evgeniich.flashread.consent

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.evgeniich.flashread.ads.AdMobManager
import com.evgeniich.flashread.analytics.applyAnalyticsConsent
import com.evgeniich.flashread.platform.AndroidAppContext
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import timber.log.Timber

object ConsentManager {
    private val consentInformation: ConsentInformation
        get() = UserMessagingPlatform.getConsentInformation(appContext)

    private val appContext: Context
        get() = AndroidAppContext.applicationContext

    fun canRequestAds(): Boolean {
        if (!AndroidAppContext.isInitialized) return false
        return consentInformation.canRequestAds()
    }

    val isPrivacyOptionsRequired: Boolean
        get() {
            if (!AndroidAppContext.isInitialized) return false
            return consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }

    fun requestConsentInfoUpdate(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {},
    ) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParameters(activity),
            onSuccess,
            { error ->
                Timber.w("UMP consent info update failed: ${error.errorCode} ${error.message}")
                onError()
            },
        )
    }

    fun showConsentFormIfRequired(
        activity: Activity,
        onComplete: () -> Unit = {},
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            formError?.let {
                Timber.w("UMP consent form error: ${it.errorCode} ${it.message}")
            }
            applyConsentToAnalytics()
            // Initialize AdMob if consent allows it
            AdMobManager.initializeIfAllowed(appContext)
            onComplete()
        }
    }

    fun gatherConsent(
        activity: Activity,
        onComplete: () -> Unit = {},
    ) {
        requestConsentInfoUpdate(
            activity,
            onSuccess = { showConsentFormIfRequired(activity, onComplete) },
            onError = {
                applyConsentToAnalytics()
                // Even on error, check if we can show ads (may have cached consent)
                AdMobManager.initializeIfAllowed(appContext)
                onComplete()
            },
        )
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onComplete: () -> Unit = {},
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let {
                Timber.w("UMP privacy options form error: ${it.errorCode} ${it.message}")
            }
            applyConsentToAnalytics()
            // Re-check AdMob initialization after consent change
            AdMobManager.initializeIfAllowed(appContext)
            onComplete()
        }
    }

    fun applyConsentToAnalytics() {
        if (!AndroidAppContext.isInitialized) return
        applyAnalyticsConsent(appContext)
    }

    private fun consentRequestParameters(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (isDebuggable(context)) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build(),
            )
        }
        return builder.build()
    }

    private fun isDebuggable(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
