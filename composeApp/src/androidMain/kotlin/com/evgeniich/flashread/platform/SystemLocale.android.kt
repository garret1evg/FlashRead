package com.evgeniich.flashread.platform

import android.content.res.Resources

actual fun currentSystemLanguageTag(): String {
    val locale = Resources.getSystem().configuration.locales[0]
    return locale.toLanguageTag()
}
