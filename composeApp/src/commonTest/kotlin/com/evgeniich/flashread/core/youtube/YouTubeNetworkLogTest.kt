package com.evgeniich.flashread.core.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class YouTubeNetworkLogTest {

    @Test
    fun redactsInnertubeApiKeyInLoggedUrls() {
        val message = "POST https://www.youtube.com/youtubei/v1/player?key=AIzaSyDummyKey_123"
        val redacted = redactYouTubeLog(message)
        assertEquals(
            "POST https://www.youtube.com/youtubei/v1/player?key=***",
            redacted,
        )
        assertFalse(redacted.contains("AIzaSyDummyKey_123"))
    }
}
