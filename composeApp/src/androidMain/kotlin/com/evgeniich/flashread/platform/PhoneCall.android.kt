package com.evgeniich.flashread.platform

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.Executor

@Composable
actual fun rememberPhoneCallActive(): Boolean {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var active by remember { mutableStateOf(audioManager.mode.isPhoneCallMode()) }

    DisposableEffect(audioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnModeChangedListener { mode ->
                active = mode.isPhoneCallMode()
            }
            audioManager.addOnModeChangedListener(mainThreadExecutor(), listener)
            onDispose { audioManager.removeOnModeChangedListener(listener) }
        } else {
            val handler = Handler(Looper.getMainLooper())
            val poll = object : Runnable {
                override fun run() {
                    active = audioManager.mode.isPhoneCallMode()
                    handler.postDelayed(this, CALL_MODE_POLL_MS)
                }
            }
            handler.post(poll)
            onDispose { handler.removeCallbacks(poll) }
        }
    }
    return active
}

private fun Int.isPhoneCallMode(): Boolean =
    this == AudioManager.MODE_RINGTONE ||
        this == AudioManager.MODE_IN_CALL ||
        this == AudioManager.MODE_IN_COMMUNICATION

private fun mainThreadExecutor(): Executor {
    val handler = Handler(Looper.getMainLooper())
    return Executor { command -> handler.post(command) }
}

private const val CALL_MODE_POLL_MS = 1_000L
