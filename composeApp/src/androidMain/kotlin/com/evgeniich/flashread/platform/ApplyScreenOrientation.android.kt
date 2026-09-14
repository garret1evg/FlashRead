package com.evgeniich.flashread.platform

import android.content.pm.ActivityInfo

actual fun applyScreenOrientation(allowRotation: Boolean) {
    val activity = AndroidAppContext.currentActivity ?: return
    activity.requestedOrientation = if (allowRotation) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
