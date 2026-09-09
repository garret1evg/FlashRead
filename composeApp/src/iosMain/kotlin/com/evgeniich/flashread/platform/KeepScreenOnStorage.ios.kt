package com.evgeniich.flashread.platform

import platform.Foundation.NSUserDefaults

actual object KeepScreenOnStorage {
    private const val KEY_KEEP_SCREEN_ON = "flashread_keep_screen_on_speed_read"
    private const val DEFAULT_ENABLED = true

    actual fun save(enabled: Boolean) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setBool(enabled, forKey = KEY_KEEP_SCREEN_ON)
        defaults.synchronize()
    }

    actual fun load(): Boolean {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(KEY_KEEP_SCREEN_ON) == null) return DEFAULT_ENABLED
        return defaults.boolForKey(KEY_KEEP_SCREEN_ON)
    }
}
