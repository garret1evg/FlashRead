package com.evgeniich.flashread.analytics

enum class ProgressBucket(val value: String, val percent: Int) {
    P25("25", 25),
    P50("50", 50),
    P75("75", 75),
    P100("100", 100),
}

enum class WpmBucket(val value: String) {
    UpTo250("up_to_250"),
    From251To400("251_400"),
    From401To600("401_600"),
    From601To800("601_800"),
    From801To1000("801_1000"),
}

enum class DurationBucket(val value: String) {
    UpTo30s("0_30s"),
    From30sTo2m("30s_2m"),
    From2To5m("2_5m"),
    From5To15m("5_15m"),
    From15mPlus("15m_plus"),
}

object AnalyticsBuckets {
    private const val THIRTY_SECONDS_MS = 30_000L
    private const val TWO_MINUTES_MS = 120_000L
    private const val FIVE_MINUTES_MS = 300_000L
    private const val FIFTEEN_MINUTES_MS = 900_000L

    fun progress(percent: Int): ProgressBucket? =
        ProgressBucket.entries.lastOrNull { percent >= it.percent }

    fun progressCrossed(fromPercent: Int, toPercent: Int): List<ProgressBucket> {
        if (toPercent <= fromPercent) return emptyList()
        return ProgressBucket.entries.filter { bucket ->
            fromPercent < bucket.percent && toPercent >= bucket.percent
        }
    }

    fun wpm(wpm: Int): WpmBucket = when {
        wpm <= 250 -> WpmBucket.UpTo250
        wpm <= 400 -> WpmBucket.From251To400
        wpm <= 600 -> WpmBucket.From401To600
        wpm <= 800 -> WpmBucket.From601To800
        else -> WpmBucket.From801To1000
    }

    fun duration(durationMs: Long): DurationBucket {
        val ms = durationMs.coerceAtLeast(0L)
        return when {
            ms < THIRTY_SECONDS_MS -> DurationBucket.UpTo30s
            ms < TWO_MINUTES_MS -> DurationBucket.From30sTo2m
            ms < FIVE_MINUTES_MS -> DurationBucket.From2To5m
            ms < FIFTEEN_MINUTES_MS -> DurationBucket.From5To15m
            else -> DurationBucket.From15mPlus
        }
    }
}
