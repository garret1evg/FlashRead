package com.tool.flashread.core.youtube

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class InnertubeYouTubeTranscriptFetcher(
    private val httpClient: HttpClient = createYouTubeHttpClient(),
) : YouTubeTranscriptFetcher {

    override suspend fun fetch(videoId: String, languages: List<String>): YouTubeTranscript {
        val canonicalId = YouTubeVideoId.extract(videoId)
            ?: throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.InvalidLink)
        try {
            return fetchTranscript(canonicalId, languages)
        } catch (error: CancellationException) {
            throw error
        } catch (error: YouTubeTranscriptException) {
            throw error
        } catch (error: Exception) {
            throw YouTubeTranscriptException(
                videoId = canonicalId,
                kind = YouTubeTranscriptFailureKind.Generic,
                cause = error,
            )
        }
    }

    private suspend fun fetchTranscript(videoId: String, languages: List<String>): YouTubeTranscript {
        val watchPage = fetchWatchHtml(videoId)
        val apiKey = InnertubePlayerData.apiKeyFromWatchHtml(watchPage.html, videoId)
        val playerJson = fetchInnertubePlayer(videoId, apiKey, watchPage.cookie)
        val captions = InnertubePlayerData.captionsFromPlayerJson(playerJson, videoId)
        val track = YouTubeCaptionTracks.pick(captions.tracks, languages)
            ?: throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.NoTranscript)
        if (track.requiresPoToken) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.Generic)
        }
        val xml = requestText(track.timedTextUrl, videoId, watchPage.cookie)
        val text = YouTubeCaptionXml.toReaderText(xml)
        if (text.isBlank()) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.NoTranscript)
        }
        return YouTubeTranscript(
            videoId = videoId,
            text = text,
            title = captions.videoTitle,
            languageCode = track.languageCode.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun fetchWatchHtml(videoId: String): WatchPage {
        var html = unescapeHtml(requestText(watchUrl(videoId), videoId, consentCookie = null))
        if (!InnertubePlayerData.needsConsent(html)) {
            return WatchPage(html = html, cookie = null)
        }
        val cookie = InnertubePlayerData.consentCookieFromHtml(html, videoId)
        html = unescapeHtml(requestText(watchUrl(videoId), videoId, cookie))
        if (InnertubePlayerData.needsConsent(html)) {
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.Generic)
        }
        return WatchPage(html = html, cookie = cookie)
    }

    private suspend fun fetchInnertubePlayer(
        videoId: String,
        apiKey: String,
        consentCookie: String?,
    ): JsonElement {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", INNERTUBE_CLIENT_NAME)
                    put("clientVersion", INNERTUBE_CLIENT_VERSION)
                }
            }
            put("videoId", videoId)
        }
        val text = requestText(
            url = innertubePlayerUrl(apiKey),
            videoId = videoId,
            consentCookie = consentCookie,
            jsonBody = body,
        )
        return YouTubeJson.parseToJsonElement(text)
    }

    private suspend fun requestText(
        url: String,
        videoId: String,
        consentCookie: String?,
        jsonBody: JsonObject? = null,
    ): String {
        val method = if (jsonBody == null) "GET" else "POST"
        logYouTubeHttp("$method $url")
        val response = if (jsonBody == null) {
            httpClient.get(url) {
                consentCookie?.let { header(HttpHeaders.Cookie, "CONSENT=$it") }
            }
        } else {
            httpClient.post(url) {
                consentCookie?.let { header(HttpHeaders.Cookie, "CONSENT=$it") }
                contentType(ContentType.Application.Json)
                setBody(YouTubeJson.encodeToString(JsonElement.serializer(), jsonBody))
            }
        }
        if (response.status == HttpStatusCode.TooManyRequests) {
            logYouTubeHttp("HTTP ${response.status.value} Too Many Requests for $url")
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.BlockedOrUnavailable)
        }
        if (!response.status.isSuccess()) {
            logYouTubeHttp("HTTP ${response.status.value} ${response.status.description} for $url")
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.Generic)
        }
        val body = response.bodyAsText()
        logYouTubeHttp(
            "HTTP ${response.status.value} ${response.status.description} ${body.length} bytes for $url",
        )
        return body
    }
}

private data class WatchPage(
    val html: String,
    val cookie: String?,
)

private const val INNERTUBE_CLIENT_NAME = "ANDROID"
private const val INNERTUBE_CLIENT_VERSION = "20.10.38"

private fun watchUrl(videoId: String): String =
    "https://www.youtube.com/watch?v=$videoId"

private fun innertubePlayerUrl(apiKey: String): String =
    "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
