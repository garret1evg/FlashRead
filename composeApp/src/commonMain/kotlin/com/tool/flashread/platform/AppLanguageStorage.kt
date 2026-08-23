package com.tool.flashread.platform

expect object AppLanguageStorage {
    fun save(value: String?)
    fun load(): String?
}
