package com.tool.flashread.platform

import androidx.core.content.edit
import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.SpeedReadSettings

actual object SpeedReadSettingsStorage {
    private const val PREFS_NAME = "flashread_speedread_prefs"
    private const val KEY_WPM = "wpm"
    private const val KEY_CHUNK_SIZE = "chunk_size"
    private const val KEY_SPRITZ_ENABLED = "spritz_enabled"
    private const val KEY_LOOP_ENABLED = "loop_enabled"

    actual fun save(settings: SpeedReadSettings) {
        val normalized = settings.normalized()
        prefs().edit {
            putInt(KEY_WPM, normalized.wpm)
            putInt(KEY_CHUNK_SIZE, normalized.chunkSize)
            putBoolean(KEY_SPRITZ_ENABLED, normalized.spritzEnabled)
            putBoolean(KEY_LOOP_ENABLED, normalized.loopEnabled)
        }
    }

    actual fun load(): SpeedReadSettings {
        return SpeedReadSettings(
            wpm = prefs().getInt(KEY_WPM, SpeedReadDefaults.DEFAULT_WPM),
            chunkSize = prefs().getInt(KEY_CHUNK_SIZE, SpeedReadDefaults.DEFAULT_CHUNK_SIZE),
            spritzEnabled = prefs().getBoolean(KEY_SPRITZ_ENABLED, SpeedReadDefaults.DEFAULT_SPRITZ_ENABLED),
            loopEnabled = prefs().getBoolean(KEY_LOOP_ENABLED, SpeedReadDefaults.DEFAULT_LOOP_ENABLED),
        ).normalized()
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
