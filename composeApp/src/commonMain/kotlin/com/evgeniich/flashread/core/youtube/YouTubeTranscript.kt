package com.evgeniich.flashread.core.youtube

data class YouTubeTranscript(
    val videoId: String,
    val text: String,
    val title: String? = null,
    val languageCode: String? = null,
)

enum class YouTubeTranscriptFailureKind {
    InvalidLink,
    NoTranscript,
    AgeRestrictedOrUnplayable,
    BlockedOrUnavailable,
    Generic,
}

class YouTubeTranscriptException(
    val videoId: String,
    val kind: YouTubeTranscriptFailureKind,
    cause: Throwable? = null,
) : Exception("YouTube transcript failed ($kind) for $videoId", cause)

interface YouTubeTranscriptFetcher {
    suspend fun fetch(videoId: String, languages: List<String>): YouTubeTranscript
}
