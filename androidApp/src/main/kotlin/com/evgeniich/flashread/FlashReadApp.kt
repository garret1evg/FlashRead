package com.evgeniich.flashread

import android.app.Application
import com.evgeniich.flashread.platform.AndroidAppContext

class FlashReadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.init(applicationContext)
    }
}
