package com.evgeniich.flashread.platform

import platform.Foundation.NSUserDefaults

actual object AppThemeStorage {
    private const val KEY_THEME = "flashread_app_theme"

    actual fun save(value: String?) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value.isNullOrBlank()) {
            defaults.removeObjectForKey(KEY_THEME)
        } else {
            defaults.setObject(value, forKey = KEY_THEME)
        }
        defaults.synchronize()
    }

    actual fun load(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_THEME)?.takeIf { it.isNotBlank() }
    }
}
