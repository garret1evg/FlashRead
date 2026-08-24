package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.platform.RecentBookStorage

class RecentBookRepository(
    private val onLoad: () -> String? = { RecentBookStorage.load() },
    private val onSave: (String?) -> Unit = { RecentBookStorage.save(it) },
) {
    fun save(bookId: String?) = onSave(bookId)

    fun load(): String? = onLoad()
}
