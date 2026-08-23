package com.tool.flashread.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import timber.log.Timber

object AndroidAppContext {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
        plantTimberIfNeeded(applicationContext)
    }

    private fun plantTimberIfNeeded(context: Context) {
        if (Timber.forest().isNotEmpty()) return
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
