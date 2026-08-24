package com.evgeniich.flashread.platform

expect object AppLanguageStorage {
    fun save(value: String?)
    fun load(): String?
}
