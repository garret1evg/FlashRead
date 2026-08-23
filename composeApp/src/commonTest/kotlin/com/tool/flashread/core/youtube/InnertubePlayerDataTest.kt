package com.tool.flashread.core.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InnertubePlayerDataTest {

    private val videoId = "dQw4w9WgXcQ"

    @Test
    fun extractsApiKeyAndConsentCookie() {
        val html = """
            <html>
            <form action="https://consent.youtube.com/s">
              <input name="v" value="CONSENT_TOKEN"/>
            </form>
            ytcfg.set({"INNERTUBE_API_KEY":"AIzaSyDummyKey_123"});
            </html>
        """.trimIndent()
        assertTrue(InnertubePlayerData.needsConsent(html))
        assertEquals("YES+CONSENT_TOKEN", InnertubePlayerData.consentCookieFromHtml(html, videoId))
        assertEquals("AIzaSyDummyKey_123", InnertubePlayerData.apiKeyFromWatchHtml(html, videoId))
        assertFalse(InnertubePlayerData.needsConsent("<html></html>"))
    }

    @Test
    fun recaptchaWithoutApiKeyIsBlocked() {
        val error = assertFailsWith<YouTubeTranscriptException> {
            InnertubePlayerData.apiKeyFromWatchHtml("""<div class="g-recaptcha"></div>""", videoId)
        }
        assertEquals(YouTubeTranscriptFailureKind.BlockedOrUnavailable, error.kind)
    }

    @Test
    fun extractsCaptionTracksAndTitleFromPlayerJson() {
        val json = YouTubeJson.parseToJsonElement(
            """
            {
              "playabilityStatus": { "status": "OK" },
              "videoDetails": { "title": "Never Gonna Give You Up", "videoId": "dQw4w9WgXcQ" },
              "captions": {
                "playerCaptionsTracklistRenderer": {
                  "captionTracks": [
                    {
                      "baseUrl": "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ&lang=en&kind=asr&fmt=srv3",
                      "languageCode": "en",
                      "kind": "asr",
                      "name": { "runs": [{ "text": "English (auto-generated)" }] }
                    },
                    {
                      "baseUrl": "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ&lang=es",
                      "languageCode": "es",
                      "name": { "simpleText": "Spanish" }
                    }
                  ]
                }
              }
            }
            """.trimIndent(),
        )
        val captions = InnertubePlayerData.captionsFromPlayerJson(json, videoId)
        assertEquals("Never Gonna Give You Up", captions.videoTitle)
        assertEquals(2, captions.tracks.size)
        assertTrue(captions.tracks[0].isGenerated)
        assertEquals("en", captions.tracks[0].languageCode)
        assertEquals(
            "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ&lang=en&kind=asr",
            captions.tracks[0].timedTextUrl,
        )
        assertFalse(captions.tracks[1].isGenerated)
        assertEquals("es", captions.tracks[1].languageCode)
        assertEquals("Spanish", captions.tracks[1].name)
        assertEquals("es", YouTubeCaptionTracks.pick(captions.tracks, listOf("es"))?.languageCode)
    }

    @Test
    fun missingCaptionsIsNoTranscript() {
        val json = YouTubeJson.parseToJsonElement(
            """{"playabilityStatus":{"status":"OK"},"videoDetails":{"title":"x"}}""",
        )
        val error = assertFailsWith<YouTubeTranscriptException> {
            InnertubePlayerData.captionsFromPlayerJson(json, videoId)
        }
        assertEquals(YouTubeTranscriptFailureKind.NoTranscript, error.kind)
    }

    @Test
    fun mapsPlayabilityFailures() {
        assertEquals(
            YouTubeTranscriptFailureKind.BlockedOrUnavailable,
            playabilityKind("LOGIN_REQUIRED", "Sign in to confirm you’re not a bot"),
        )
        assertEquals(
            YouTubeTranscriptFailureKind.AgeRestrictedOrUnplayable,
            playabilityKind("LOGIN_REQUIRED", "This video may be inappropriate for some users."),
        )
        assertEquals(
            YouTubeTranscriptFailureKind.BlockedOrUnavailable,
            playabilityKind("ERROR", "This video is unavailable"),
        )
        assertEquals(
            YouTubeTranscriptFailureKind.AgeRestrictedOrUnplayable,
            playabilityKind("UNPLAYABLE", "This video is private"),
        )
    }

    private fun playabilityKind(status: String, reason: String): YouTubeTranscriptFailureKind {
        val json = YouTubeJson.parseToJsonElement(
            """{"playabilityStatus":{"status":"$status","reason":"$reason"}}""",
        )
        val error = assertFailsWith<YouTubeTranscriptException> {
            InnertubePlayerData.captionsFromPlayerJson(json, videoId)
        }
        return error.kind
    }
}
