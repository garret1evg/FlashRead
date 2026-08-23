package com.tool.flashread.platform

import platform.Foundation.NSBundle

actual object AppInfo {
    actual val versionName: String
        get() = NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleShortVersionString")
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "1.0"
}
