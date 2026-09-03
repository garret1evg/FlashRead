package com.evgeniich.flashread.analytics

interface AnalyticsLogger {
    fun log(event: AnalyticsEvent)
}

expect object Analytics : AnalyticsLogger
