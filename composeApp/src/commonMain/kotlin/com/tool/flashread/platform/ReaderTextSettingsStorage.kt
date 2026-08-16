package com.tool.flashread.platform

import com.tool.flashread.core.reading.ReaderTextSettings

expect object ReaderTextSettingsStorage {
    fun save(settings: ReaderTextSettings)
    fun load(): ReaderTextSettings
}
