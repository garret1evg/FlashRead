package com.evgeniich.flashread.monetization

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Tracks application foreground/background state using ProcessLifecycleOwner.
 *
 * ProcessLifecycleOwner provides lifecycle events for the whole application process:
 * - ON_START is called when the app comes to foreground (first activity is started)
 * - ON_STOP is called when the app goes to background (last activity is stopped)
 *
 * This tracker provides:
 * - Observable [isInForeground] state flow for reactive UI/logic
 * - Foreground duration tracking for usage time measurement
 * - Callbacks for foreground/background transitions
 *
 * Must be initialized by calling [init] before use, typically from [AndroidAppContext.init].
 */
object AppLifecycleTracker : DefaultLifecycleObserver {

    private val _isInForeground = MutableStateFlow(false)

    /**
     * Observable state indicating whether the app is currently in foreground.
     *
     * - `true` when at least one Activity is started (visible)
     * - `false` when all Activities are stopped (app is in background or killed)
     */
    val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

    /**
     * Current foreground state as a simple boolean.
     * Use [isInForeground] flow for reactive observation.
     */
    val foreground: Boolean
        get() = _isInForeground.value

    /**
     * Timestamp (epoch ms) when the app entered foreground.
     * Null if app is in background or not yet started.
     */
    @Volatile
    private var foregroundStartMs: Long? = null

    /**
     * Callback invoked when app enters foreground.
     * Set this to receive notifications about foreground transitions.
     */
    var onForeground: (() -> Unit)? = null

    /**
     * Callback invoked when app enters background.
     * The parameter is the duration in milliseconds the app was in foreground.
     */
    var onBackground: ((foregroundDurationMs: Long) -> Unit)? = null

    @Volatile
    private var initialized = false

    /**
     * Initializes the lifecycle tracker by registering with ProcessLifecycleOwner.
     *
     * Should be called once during app startup, typically from [AndroidAppContext.init].
     * Multiple calls are safe and will be ignored.
     */
    fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true

            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            Timber.d("AppLifecycleTracker initialized")
        }
    }

    /**
     * Called when the app process enters foreground (first activity started).
     */
    override fun onStart(owner: LifecycleOwner) {
        foregroundStartMs = System.currentTimeMillis()
        _isInForeground.value = true
        Timber.d("App entered foreground")
        onForeground?.invoke()
    }

    /**
     * Called when the app process enters background (last activity stopped).
     */
    override fun onStop(owner: LifecycleOwner) {
        val startMs = foregroundStartMs
        val durationMs = if (startMs != null) {
            System.currentTimeMillis() - startMs
        } else {
            0L
        }

        foregroundStartMs = null
        _isInForeground.value = false
        Timber.d("App entered background after ${durationMs}ms in foreground")
        onBackground?.invoke(durationMs)
    }

    /**
     * Returns the duration in milliseconds since the app entered foreground.
     *
     * @return Duration in foreground, or 0 if currently in background.
     */
    fun getForegroundDurationMs(): Long {
        val startMs = foregroundStartMs ?: return 0L
        return System.currentTimeMillis() - startMs
    }

    /**
     * Resets the foreground timer to the current time.
     * Useful after persisting accumulated usage time.
     */
    fun resetForegroundTimer() {
        if (foreground) {
            foregroundStartMs = System.currentTimeMillis()
        }
    }
}
