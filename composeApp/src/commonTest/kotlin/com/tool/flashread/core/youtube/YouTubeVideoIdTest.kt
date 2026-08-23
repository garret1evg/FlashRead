package com.tool.flashread.core.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeVideoIdTest {

    @Test
    fun extractsRawId() {
        assertEquals("dQw4w9WgXcQ", YouTubeVideoId.extract("dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeVideoId.extract("  dQw4w9WgXcQ  "))
    }

    @Test
    fun extractsWatchUrls() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=43s"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://m.youtube.com/watch?app=desktop&v=dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("youtube.com/watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun extractsShortLinks() {
        assertEquals("dQw4w9WgXcQ", YouTubeVideoId.extract("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeVideoId.extract("https://youtu.be/dQw4w9WgXcQ?t=43"))
        assertEquals("dQw4w9WgXcQ", YouTubeVideoId.extract("www.youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeVideoId.extract("youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun extractsShortsEmbedAndLivePaths() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://www.youtube.com/shorts/dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://www.youtube.com/embed/dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://www.youtube.com/live/dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeVideoId.extract("https://www.youtube.com/embed/dQw4w9WgXcQ?start=10"),
        )
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(YouTubeVideoId.extract(""))
        assertNull(YouTubeVideoId.extract("   "))
        assertNull(YouTubeVideoId.extract("not-a-video-id"))
        assertNull(YouTubeVideoId.extract("https://www.youtube.com/watch?v=short"))
        assertNull(YouTubeVideoId.extract("https://www.youtube.com/playlist?list=PLabc"))
    }
}
