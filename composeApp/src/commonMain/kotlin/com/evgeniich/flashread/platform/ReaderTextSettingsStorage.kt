package com.evgeniich.flashread.platform

import com.evgeniich.flashread.core.reading.ReaderTextSettings

expect object ReaderTextSettingsStorage {
    fun save(settings: ReaderTextSettings)
    fun load(): ReaderTextSettings
}
