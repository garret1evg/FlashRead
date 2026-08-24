package com.evgeniich.flashread.platform

import androidx.core.content.edit

actual object AppLanguageStorage {
    private const val PREFS_NAME = "flashread_app_prefs"
    private const val KEY_LANGUAGE = "app_language"

    actual fun save(value: String?) {
        prefs().edit {
            if (value.isNullOrBlank()) {
                remove(KEY_LANGUAGE)
            } else {
                putString(KEY_LANGUAGE, value)
            }
        }
    }

    actual fun load(): String? {
        return prefs().getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
