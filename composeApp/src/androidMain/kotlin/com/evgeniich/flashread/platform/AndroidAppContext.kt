package com.evgeniich.flashread.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import com.evgeniich.flashread.analytics.applyAnalyticsConsent
import timber.log.Timber

object AndroidAppContext {
    lateinit var applicationContext: Context
        private set

    val isInitialized: Boolean
        get() = ::applicationContext.isInitialized

    fun init(context: Context) {
        applicationContext = context.applicationContext
        plantTimberIfNeeded(applicationContext)
        applyAnalyticsConsent(applicationContext)
    }

    private fun plantTimberIfNeeded(context: Context) {
        if (Timber.forest().isNotEmpty()) return
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
