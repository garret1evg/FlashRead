package com.evgeniich.flashread.crash

interface CrashLogger {
    fun recordException(throwable: Throwable)
    fun log(message: String)
}

expect object CrashReporting : CrashLogger
