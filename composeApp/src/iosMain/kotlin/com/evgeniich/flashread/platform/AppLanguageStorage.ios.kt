package com.evgeniich.flashread.platform

import platform.Foundation.NSUserDefaults

actual object AppLanguageStorage {
    private const val KEY_LANGUAGE = "flashread_app_language"

    actual fun save(value: String?) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value.isNullOrBlank()) {
            defaults.removeObjectForKey(KEY_LANGUAGE)
        } else {
            defaults.setObject(value, forKey = KEY_LANGUAGE)
        }
        defaults.synchronize()
    }

    actual fun load(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_LANGUAGE)?.takeIf { it.isNotBlank() }
    }
}
