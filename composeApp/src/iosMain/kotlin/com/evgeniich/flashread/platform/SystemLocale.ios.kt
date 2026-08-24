package com.evgeniich.flashread.platform

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun currentSystemLanguageTag(): String = processSystemLanguageTag

private val processSystemLanguageTag: String by lazy {
    (NSLocale.preferredLanguages.firstOrNull() as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "en"
}
