package com.evgeniich.flashread.platform

import platform.Foundation.NSUserDefaults

actual object AutoRotateStorage {
    private const val KEY_AUTO_ROTATE = "flashread_auto_rotate"
    private const val DEFAULT_ENABLED = false

    actual fun save(enabled: Boolean) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setBool(enabled, forKey = KEY_AUTO_ROTATE)
        defaults.synchronize()
    }

    actual fun load(): Boolean {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(KEY_AUTO_ROTATE) == null) return DEFAULT_ENABLED
        return defaults.boolForKey(KEY_AUTO_ROTATE)
    }
}
