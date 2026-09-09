package com.evgeniich.flashread.platform

expect object AppThemeStorage {
    fun save(value: String?)
    fun load(): String?
}
