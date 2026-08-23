package com.tool.flashread.platform

actual object RecentBookStorage {
    private var inMemoryBookId: String? = null

    actual fun save(bookId: String?) {
        inMemoryBookId = bookId?.takeIf { it.isNotBlank() }
    }

    actual fun load(): String? = inMemoryBookId
}
