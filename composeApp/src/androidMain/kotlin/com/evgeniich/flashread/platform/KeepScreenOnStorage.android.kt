package com.evgeniich.flashread.platform

import androidx.core.content.edit

actual object KeepScreenOnStorage {
    private const val PREFS_NAME = "flashread_app_prefs"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on_speed_read"
    private const val DEFAULT_ENABLED = true

    actual fun save(enabled: Boolean) {
        prefs().edit { putBoolean(KEY_KEEP_SCREEN_ON, enabled) }
    }

    actual fun load(): Boolean = prefs().getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_ENABLED)

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
