package com.evgeniich.flashread.crash

import android.util.Log
import com.evgeniich.flashread.platform.AndroidAppContext
import timber.log.Timber

internal class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!AndroidAppContext.isInitialized) return
        val crashlytics = firebaseCrashlyticsOrNull(AndroidAppContext.applicationContext) ?: return
        if (priority < Log.INFO) return
        val label = tag ?: "FlashRead"
        crashlytics.log("$priority/$label: $message")
        if (t != null && priority >= Log.ERROR) {
            crashlytics.recordException(t)
        }
    }
}
