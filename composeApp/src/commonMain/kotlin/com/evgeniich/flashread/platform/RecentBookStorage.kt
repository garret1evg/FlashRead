package com.evgeniich.flashread.platform

expect object RecentBookStorage {
    fun save(bookId: String?)
    fun load(): String?
}
