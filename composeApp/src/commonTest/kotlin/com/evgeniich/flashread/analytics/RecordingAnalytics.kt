package com.evgeniich.flashread.analytics

class RecordingAnalytics : AnalyticsLogger {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    inline fun <reified T : AnalyticsEvent> ofType(): List<T> = events.filterIsInstance<T>()
}
