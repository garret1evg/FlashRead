package com.evgeniich.flashread.core.youtube

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val YouTubeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal expect fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit = {},
): HttpClient

internal fun createYouTubeHttpClient(): HttpClient = createPlatformHttpClient {
    install(ContentNegotiation) {
        json(YouTubeJson)
    }
    install(Logging) {
        logger = YouTubeHttpLogger
        level = LogLevel.BODY
        sanitizeHeader { header -> header.equals(HttpHeaders.Cookie, ignoreCase = true) }
    }
    defaultRequest {
        header(HttpHeaders.AcceptLanguage, "en-US")
    }
}
