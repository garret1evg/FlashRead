package com.evgeniich.flashread.consent

import com.evgeniich.flashread.platform.AndroidAppContext
import timber.log.Timber

actual fun showPrivacyOptionsForm() {
    val activity = AndroidAppContext.currentActivity
    if (activity == null) {
        Timber.w("Cannot show privacy options form: no current activity")
        return
    }
    ConsentManager.showPrivacyOptionsForm(activity)
}

actual fun isPrivacyOptionsRequired(): Boolean = ConsentManager.isPrivacyOptionsRequired
