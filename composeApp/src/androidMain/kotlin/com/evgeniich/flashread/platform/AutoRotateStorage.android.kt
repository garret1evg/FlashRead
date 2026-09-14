package com.evgeniich.flashread.platform

import androidx.core.content.edit

actual object AutoRotateStorage {
    private const val PREFS_NAME = "flashread_app_prefs"
    private const val KEY_AUTO_ROTATE = "auto_rotate"
    private const val DEFAULT_ENABLED = false

    actual fun save(enabled: Boolean) {
        prefs().edit { putBoolean(KEY_AUTO_ROTATE, enabled) }
    }

    actual fun load(): Boolean = prefs().getBoolean(KEY_AUTO_ROTATE, DEFAULT_ENABLED)

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
