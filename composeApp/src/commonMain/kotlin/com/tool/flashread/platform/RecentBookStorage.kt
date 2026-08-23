package com.tool.flashread.platform

expect object RecentBookStorage {
    fun save(bookId: String?)
    fun load(): String?
}
