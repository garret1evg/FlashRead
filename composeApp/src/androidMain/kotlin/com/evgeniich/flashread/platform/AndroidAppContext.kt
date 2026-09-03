package com.evgeniich.flashread.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import com.evgeniich.flashread.analytics.applyAnalyticsConsent
import timber.log.Timber
import java.lang.ref.WeakReference

object AndroidAppContext {
    lateinit var applicationContext: Context
        private set

    val isInitialized: Boolean
        get() = ::applicationContext.isInitialized

    val currentActivity: Activity?
        get() = CurrentActivityTracker.activity

    fun init(context: Context) {
        applicationContext = context.applicationContext
        plantTimberIfNeeded(applicationContext)
        applyAnalyticsConsent(applicationContext)
        (applicationContext as? Application)?.let(CurrentActivityTracker::register)
    }

    private fun plantTimberIfNeeded(context: Context) {
        if (Timber.forest().isNotEmpty()) return
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

private object CurrentActivityTracker : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var activityRef: WeakReference<Activity>? = null

    @Volatile
    private var registered = false

    val activity: Activity?
        get() = activityRef?.get()

    fun register(application: Application) {
        if (registered) return
        registered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityRef = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }
}
