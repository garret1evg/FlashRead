package com.evgeniich.flashread.analytics

actual object Analytics : AnalyticsLogger {
    override fun log(event: AnalyticsEvent) = Unit
}
