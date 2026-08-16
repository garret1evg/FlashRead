package com.tool.flashread.data.repository

import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.platform.ReaderTextSettingsStorage

class ReaderTextSettingsRepository(
    private val onLoad: () -> ReaderTextSettings = { ReaderTextSettingsStorage.load() },
    private val onSave: (ReaderTextSettings) -> Unit = { ReaderTextSettingsStorage.save(it) },
) {
    fun save(settings: ReaderTextSettings) {
        onSave(settings)
    }

    fun load(): ReaderTextSettings = onLoad()
}
