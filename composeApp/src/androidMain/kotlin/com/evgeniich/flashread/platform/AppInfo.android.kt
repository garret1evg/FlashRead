package com.evgeniich.flashread.platform

actual object AppInfo {
    actual val versionName: String
        get() = runCatching {
            val context = AndroidAppContext.applicationContext
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "1.0"
}
