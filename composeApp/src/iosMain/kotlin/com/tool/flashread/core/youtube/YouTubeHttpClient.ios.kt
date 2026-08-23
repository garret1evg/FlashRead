package com.tool.flashread.core.youtube

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Darwin) {
    config(this)
}
