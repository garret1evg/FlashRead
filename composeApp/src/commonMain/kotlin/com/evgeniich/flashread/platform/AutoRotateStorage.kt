package com.evgeniich.flashread.platform

expect object AutoRotateStorage {
    fun save(enabled: Boolean)
    fun load(): Boolean
}
