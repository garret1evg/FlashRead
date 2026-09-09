package com.evgeniich.flashread

import android.app.Application
import android.content.Context
import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.platform.AndroidAppContext
import com.evgeniich.flashread.platform.AppThemeStorage
import com.evgeniich.flashread.platform.applyPlatformTheme

class FlashReadApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        AndroidAppContext.init(this)
        applyPlatformTheme(AppTheme.fromStorage(AppThemeStorage.load()))
    }

    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.init(this)
    }
}
