package com.tool.flashread.data.repository

import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.platform.SpeedReadSettingsStorage

class SpeedReadSettingsRepository(
    private val onLoad: () -> SpeedReadSettings = { SpeedReadSettingsStorage.load() },
    private val onSave: (SpeedReadSettings) -> Unit = { SpeedReadSettingsStorage.save(it) },
) {
    fun save(settings: SpeedReadSettings) {
        onSave(settings)
    }

    fun load(): SpeedReadSettings = onLoad()
}
