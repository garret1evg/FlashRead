package com.tool.flashread.data.repository

import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.platform.ReaderTextSettingsStorage

class ReaderTextSettingsRepository {
    fun save(settings: ReaderTextSettings) {
        ReaderTextSettingsStorage.save(settings)
    }

    fun load(): ReaderTextSettings = ReaderTextSettingsStorage.load()
}
