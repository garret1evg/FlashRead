package com.evgeniich.flashread.platform

expect object KeepScreenOnStorage {
    fun save(enabled: Boolean)
    fun load(): Boolean
}
