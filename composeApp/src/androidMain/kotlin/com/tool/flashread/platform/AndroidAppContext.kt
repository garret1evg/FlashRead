package com.tool.flashread.platform

import android.content.Context

object AndroidAppContext {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
