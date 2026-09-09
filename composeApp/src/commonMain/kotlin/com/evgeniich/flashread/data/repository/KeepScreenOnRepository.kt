package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.platform.KeepScreenOnStorage

class KeepScreenOnRepository(
    private val onLoad: () -> Boolean = { KeepScreenOnStorage.load() },
    private val onSave: (Boolean) -> Unit = { KeepScreenOnStorage.save(it) },
) {
    fun save(enabled: Boolean) = onSave(enabled)

    fun load(): Boolean = onLoad()
}
