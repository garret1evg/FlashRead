package com.evgeniich.flashread.platform

import com.evgeniich.flashread.core.reading.ReaderTextSettings

actual object ReaderTextSettingsStorage {
    private var inMemorySettings = ReaderTextSettings()

    actual fun save(settings: ReaderTextSettings) {
        inMemorySettings = settings.normalized()
    }

    actual fun load(): ReaderTextSettings = inMemorySettings
}
