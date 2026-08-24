package com.evgeniich.flashread.core.youtube

/**
 * Extracts the canonical 11-character YouTube video id from a watch URL,
 * short link, Shorts/embed/live path, or a raw id.
 */
object YouTubeVideoId {
    const val LENGTH = 11

    private val RAW_ID = Regex("^[A-Za-z0-9_-]{$LENGTH}$")
    private val WATCH_PARAM = Regex("""[?&]v=([A-Za-z0-9_-]{$LENGTH})""")
    private val SHORT_LINK = Regex("""(?:^|//)(?:www\.)?youtu\.be/([A-Za-z0-9_-]{$LENGTH})""")
    private val PATH_ID = Regex("""/(?:shorts|embed|live)/([A-Za-z0-9_-]{$LENGTH})""")

    fun extract(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (RAW_ID.matches(trimmed)) return trimmed
        WATCH_PARAM.find(trimmed)?.let { return it.groupValues[1] }
        SHORT_LINK.find(trimmed)?.let { return it.groupValues[1] }
        PATH_ID.find(trimmed)?.let { return it.groupValues[1] }
        return null
    }
}
