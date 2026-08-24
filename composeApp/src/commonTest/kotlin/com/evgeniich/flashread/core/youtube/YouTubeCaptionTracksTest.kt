package com.evgeniich.flashread.core.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YouTubeCaptionTracksTest {

    private val asrEnglish = CaptionTrack(
        baseUrl = "https://www.youtube.com/api/timedtext?v=abc&lang=en&kind=asr&fmt=srv3",
        languageCode = "en",
        isGenerated = true,
        name = "English (auto-generated)",
    )
    private val manualSpanish = CaptionTrack(
        baseUrl = "https://www.youtube.com/api/timedtext?v=abc&lang=es",
        languageCode = "es",
        isGenerated = false,
        name = "Spanish",
    )
    private val asrGerman = CaptionTrack(
        baseUrl = "https://www.youtube.com/api/timedtext?v=abc&lang=de&kind=asr",
        languageCode = "de",
        isGenerated = true,
        name = "German (auto-generated)",
    )
    private val manualEnUs = CaptionTrack(
        baseUrl = "https://www.youtube.com/api/timedtext?v=abc&lang=en-US",
        languageCode = "en-US",
        isGenerated = false,
        name = "English (United States)",
    )

    private val tracks = listOf(asrEnglish, manualSpanish, asrGerman, manualEnUs)

    @Test
    fun prefersManualOverAsrForTheSameLanguage() {
        val picked = YouTubeCaptionTracks.pick(tracks, listOf("en"))
        assertEquals(manualEnUs, picked)
    }

    @Test
    fun usesPreferredLanguageThenEnglishThenFirstAvailable() {
        assertEquals(manualSpanish, YouTubeCaptionTracks.pick(tracks, listOf("es")))
        assertEquals(asrGerman, YouTubeCaptionTracks.pick(tracks, listOf("de")))
        assertEquals(manualEnUs, YouTubeCaptionTracks.pick(tracks, listOf("fr")))
        assertEquals(
            asrEnglish,
            YouTubeCaptionTracks.pick(listOf(asrEnglish, manualSpanish, asrGerman), listOf("ja")),
        )
        assertEquals(
            manualSpanish,
            YouTubeCaptionTracks.pick(listOf(manualSpanish, asrGerman), listOf("ja")),
        )
    }

    @Test
    fun languagePriorityPutsAppLanguageThenEnglish() {
        assertEquals(listOf("de", "en"), YouTubeCaptionTracks.languagePriority("de-DE"))
        assertEquals(listOf("en"), YouTubeCaptionTracks.languagePriority("en-US"))
        assertEquals(listOf("en"), YouTubeCaptionTracks.languagePriority(null))
        assertEquals(listOf("uk", "en"), YouTubeCaptionTracks.languagePriority("uk"))
    }

    @Test
    fun stripsSrv3AndDetectsPoToken() {
        assertEquals(
            "https://www.youtube.com/api/timedtext?v=abc&lang=en&kind=asr",
            asrEnglish.timedTextUrl,
        )
        assertFalse(asrEnglish.requiresPoToken)
        val poToken = CaptionTrack(
            baseUrl = "https://www.youtube.com/api/timedtext?v=abc&lang=en&exp=xpe",
            languageCode = "en",
            isGenerated = true,
        )
        assertTrue(poToken.requiresPoToken)
    }
}
