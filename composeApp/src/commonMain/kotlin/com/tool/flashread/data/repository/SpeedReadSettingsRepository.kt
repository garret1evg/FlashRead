package com.tool.flashread.data.repository

import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.platform.SpeedReadSettingsStorage

class SpeedReadSettingsRepository {
    fun save(settings: SpeedReadSettings) {
        SpeedReadSettingsStorage.save(settings)
    }

    fun load(): SpeedReadSettings = SpeedReadSettingsStorage.load()
}
