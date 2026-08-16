package com.tool.flashread.platform

import com.tool.flashread.core.speedread.SpeedReadSettings

actual object SpeedReadSettingsStorage {
    private var inMemorySettings = SpeedReadSettings()

    actual fun save(settings: SpeedReadSettings) {
        inMemorySettings = settings.normalized()
    }

    actual fun load(): SpeedReadSettings = inMemorySettings
}
