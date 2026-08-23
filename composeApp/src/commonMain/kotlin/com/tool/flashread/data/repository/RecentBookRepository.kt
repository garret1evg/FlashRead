package com.tool.flashread.data.repository

import com.tool.flashread.platform.RecentBookStorage

class RecentBookRepository(
    private val onLoad: () -> String? = { RecentBookStorage.load() },
    private val onSave: (String?) -> Unit = { RecentBookStorage.save(it) },
) {
    fun save(bookId: String?) = onSave(bookId)

    fun load(): String? = onLoad()
}
