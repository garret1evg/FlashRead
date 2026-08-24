package com.evgeniich.flashread.platform

import com.evgeniich.flashread.core.speedread.SpeedReadSettings

expect object SpeedReadSettingsStorage {
    fun save(settings: SpeedReadSettings)
    fun load(): SpeedReadSettings
}
