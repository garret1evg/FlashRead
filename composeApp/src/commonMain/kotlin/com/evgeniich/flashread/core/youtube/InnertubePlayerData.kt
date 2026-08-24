package com.evgeniich.flashread.core.youtube

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal data class InnertubeCaptions(
    val tracks: List<CaptionTrack>,
    val videoTitle: String?,
)

internal object InnertubePlayerData {
    private val API_KEY = Regex(""""INNERTUBE_API_KEY"\s*:\s*"([a-zA-Z0-9_-]+)"""")
    private val CONSENT_VALUE = Regex("""name="v" value="(.*?)"""")

    const val CONSENT_FORM_MARKER = """action="https://consent.youtube.com/s""""
    const val RECAPTCHA_MARKER = """class="g-recaptcha""""
    const val BOT_DETECTED_REASON = "Sign in to confirm you’re not a bot"
    const val AGE_RESTRICTED_REASON = "This video may be inappropriate for some users."
    const val VIDEO_UNAVAILABLE_REASON = "This video is unavailable"

    fun needsConsent(html: String): Boolean = CONSENT_FORM_MARKER in html

    fun consentCookieFromHtml(html: String, videoId: String): String {
        val value = CONSENT_VALUE.find(html)?.groupValues?.get(1)
            ?: throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.Generic)
        return "YES+$value"
    }

    fun apiKeyFromWatchHtml(html: String, videoId: String): String {
        val match = API_KEY.find(html)
        if (match != null) return match.groupValues[1]
        if (RECAPTCHA_MARKER in html) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.BlockedOrUnavailable)
        }
        throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.Generic)
    }

    fun captionsFromPlayerJson(json: JsonElement, videoId: String): InnertubeCaptions {
        val root = json as? JsonObject
            ?: throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.Generic)
        assertPlayability(root.obj("playabilityStatus"), videoId)
        val captionsJson = root.obj("captions")?.obj("playerCaptionsTracklistRenderer")
        if (captionsJson == null || captionsJson.arr("captionTracks") == null) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.NoTranscript)
        }
        val tracks = YouTubeCaptionTracks.fromCaptionsJson(captionsJson)
        if (tracks.isEmpty()) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.NoTranscript)
        }
        return InnertubeCaptions(
            tracks = tracks,
            videoTitle = root.obj("videoDetails")?.string("title")?.takeIf { it.isNotBlank() },
        )
    }

    fun assertPlayability(playability: JsonObject?, videoId: String) {
        val status = playability?.string("status") ?: return
        if (status == PLAYABILITY_OK) return
        val reason = playability.string("reason")
        if (status == PLAYABILITY_LOGIN_REQUIRED) {
            when {
                reasonsMatch(reason, BOT_DETECTED_REASON) -> {
                    throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.BlockedOrUnavailable)
                }
                reasonsMatch(reason, AGE_RESTRICTED_REASON) -> {
                    throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.AgeRestrictedOrUnplayable)
                }
            }
        }
        if (status == PLAYABILITY_ERROR && reasonsMatch(reason, VIDEO_UNAVAILABLE_REASON)) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.BlockedOrUnavailable)
        }
        throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.AgeRestrictedOrUnplayable)
    }
}

private const val PLAYABILITY_OK = "OK"
private const val PLAYABILITY_LOGIN_REQUIRED = "LOGIN_REQUIRED"
private const val PLAYABILITY_ERROR = "ERROR"

private fun reasonsMatch(actual: String?, expected: String): Boolean {
    if (actual == null) return false
    return normalizeReason(actual) == normalizeReason(expected)
}

private fun normalizeReason(value: String): String =
    value.replace('\u2019', '\'').replace('\u2018', '\'')
