package com.tool.flashread.core.youtube

import timber.log.Timber

/** Logcat drops a single line past ~4KB; split bodies so they stay visible. */
private const val MaxLogChunk = 3500

internal actual fun logYouTubeHttpPlatform(message: String) {
    if (message.length <= MaxLogChunk) {
        Timber.tag(YouTubeHttpLogTag).d("%s", message)
        return
    }
    var index = 0
    var part = 1
    val parts = (message.length + MaxLogChunk - 1) / MaxLogChunk
    while (index < message.length) {
        val end = minOf(index + MaxLogChunk, message.length)
        Timber.tag(YouTubeHttpLogTag).d("[%d/%d] %s", part, parts, message.substring(index, end))
        index = end
        part++
    }
}
