package com.tool.flashread.core.youtube

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(OkHttp) {
    config(this)
}
