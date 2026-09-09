package com.evgeniich.flashread.platform

import androidx.core.content.edit
import com.evgeniich.flashread.core.theme.AppTheme

actual object AppThemeStorage {
    private const val PREFS_NAME = "flashread_app_prefs"
    private const val KEY_THEME = "app_theme"
    private const val LEGACY_READER_PREFS = "flashread_reader_text_prefs"
    private const val LEGACY_READER_THEME = "theme"

    actual fun save(value: String?) {
        prefs().edit {
            if (value.isNullOrBlank()) {
                remove(KEY_THEME)
            } else {
                putString(KEY_THEME, value)
            }
        }
    }

    actual fun load(): String? {
        val stored = prefs().getString(KEY_THEME, null)?.takeIf { it.isNotBlank() }
        if (stored != null) return stored
        val legacy = AndroidAppContext.applicationContext
            .getSharedPreferences(LEGACY_READER_PREFS, 0)
            .getString(LEGACY_READER_THEME, null)
        val migrated = AppTheme.fromStorage(legacy)
        if (legacy.isNullOrBlank() || migrated == AppTheme.DEFAULT) {
            return null
        }
        val value = migrated.toStorage()
        save(value)
        return value
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
