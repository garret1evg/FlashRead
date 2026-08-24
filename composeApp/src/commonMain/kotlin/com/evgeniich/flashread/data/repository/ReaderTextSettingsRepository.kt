package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.platform.ReaderTextSettingsStorage

class ReaderTextSettingsRepository(
    private val onLoad: () -> ReaderTextSettings = { ReaderTextSettingsStorage.load() },
    private val onSave: (ReaderTextSettings) -> Unit = { ReaderTextSettingsStorage.save(it) },
) {
    fun save(settings: ReaderTextSettings) {
        onSave(settings)
    }

    fun load(): ReaderTextSettings = onLoad()
}
