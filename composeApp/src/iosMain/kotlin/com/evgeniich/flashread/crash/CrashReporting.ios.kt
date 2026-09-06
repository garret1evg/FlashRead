package com.evgeniich.flashread.crash

actual object CrashReporting : CrashLogger {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
}
