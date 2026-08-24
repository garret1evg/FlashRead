package com.evgeniich.flashread.core.youtube

import io.ktor.client.plugins.logging.Logger

internal const val YouTubeHttpLogTag = "YouTubeHttp"

private val QUERY_KEY = Regex("""([?&]key=)[^&]*""", RegexOption.IGNORE_CASE)

internal fun redactYouTubeLog(message: String): String = QUERY_KEY.replace(message, "$1***")

internal fun logYouTubeHttp(message: String) {
    logYouTubeHttpPlatform(redactYouTubeLog(message))
}

internal expect fun logYouTubeHttpPlatform(message: String)

internal object YouTubeHttpLogger : Logger {
    override fun log(message: String) {
        logYouTubeHttp(message)
    }
}
