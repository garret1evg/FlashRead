package com.evgeniich.flashread.platform

import com.evgeniich.flashread.core.speedread.SpeedReadSettings

actual object SpeedReadSettingsStorage {
    private var inMemorySettings = SpeedReadSettings()

    actual fun save(settings: SpeedReadSettings) {
        inMemorySettings = settings.normalized()
    }

    actual fun load(): SpeedReadSettings = inMemorySettings
}
