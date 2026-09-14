package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.platform.AutoRotateStorage

class AutoRotateRepository(
    private val onLoad: () -> Boolean = { AutoRotateStorage.load() },
    private val onSave: (Boolean) -> Unit = { AutoRotateStorage.save(it) },
) {
    fun save(enabled: Boolean) = onSave(enabled)

    fun load(): Boolean = onLoad()
}
