package com.tool.flashread.platform

import com.tool.flashread.core.speedread.SpeedReadSettings

expect object SpeedReadSettingsStorage {
    fun save(settings: SpeedReadSettings)
    fun load(): SpeedReadSettings
}
