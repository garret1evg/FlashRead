package com.tool.flashread.core.youtube

import com.tool.flashread.core.locale.languageSubtag
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal data class CaptionTrack(
    val baseUrl: String,
    val languageCode: String,
    val isGenerated: Boolean,
    val name: String = "",
) {
    val timedTextUrl: String = baseUrl.replace("&fmt=srv3", "")

    val requiresPoToken: Boolean = "&exp=xpe" in timedTextUrl
}

internal object YouTubeCaptionTracks {
    fun fromCaptionsJson(captionsJson: JsonObject): List<CaptionTrack> {
        val tracksJson = captionsJson.arr("captionTracks") ?: return emptyList()
        val tracks = ArrayList<CaptionTrack>(tracksJson.size)
        for (element in tracksJson) {
            val track = element as? JsonObject ?: continue
            val baseUrl = track.string("baseUrl") ?: continue
            val languageCode = track.string("languageCode").orEmpty()
            tracks.add(
                CaptionTrack(
                    baseUrl = baseUrl,
                    languageCode = languageCode,
                    isGenerated = track.string("kind") == "asr",
                    name = captionName(track),
                ),
            )
        }
        return tracks
    }

    /**
     * Prefers a manually created track over ASR for each requested language,
     * then English, then the first remaining manual track (else first ASR).
     */
    fun pick(tracks: List<CaptionTrack>, languages: List<String>): CaptionTrack? {
        if (tracks.isEmpty()) return null
        val ordered = buildList {
            addAll(languages)
            if (languages.none { languageCodesMatch("en", it) }) add("en")
        }
        for (language in ordered) {
            tracks.firstOrNull { !it.isGenerated && languageCodesMatch(it.languageCode, language) }
                ?.let { return it }
            tracks.firstOrNull { it.isGenerated && languageCodesMatch(it.languageCode, language) }
                ?.let { return it }
        }
        tracks.firstOrNull { !it.isGenerated }?.let { return it }
        return tracks.first()
    }

    fun languagePriority(preferredLanguage: String?): List<String> {
        val primary = preferredLanguage?.let(::languageSubtag).orEmpty()
        return buildList {
            if (primary.isNotEmpty()) add(primary)
            if (primary != "en") add("en")
        }
    }
}

internal fun languageCodesMatch(trackCode: String, preferred: String): Boolean {
    val track = languageSubtag(trackCode)
    val want = languageSubtag(preferred)
    return track.isNotEmpty() && track == want
}

private fun captionName(track: JsonObject): String {
    val name = track.obj("name") ?: return ""
    name.string("simpleText")?.let { return it }
    val run = name.arr("runs")?.firstOrNull() as? JsonObject
    return run?.string("text").orEmpty()
}

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull
