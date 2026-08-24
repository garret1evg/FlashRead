package com.evgeniich.flashread

import com.evgeniich.flashread.core.locale.AppLanguage
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.model.ReadingPosition
import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import com.evgeniich.flashread.core.youtube.YouTubeTranscript
import com.evgeniich.flashread.core.youtube.YouTubeTranscriptFetcher
import com.evgeniich.flashread.data.repository.AppLanguageRepository
import com.evgeniich.flashread.data.repository.BookRepository
import com.evgeniich.flashread.data.repository.CoverRepository
import com.evgeniich.flashread.data.repository.ReaderTextSettingsRepository
import com.evgeniich.flashread.data.repository.ReadingSessionRepository
import com.evgeniich.flashread.data.repository.RecentBookRepository
import com.evgeniich.flashread.data.repository.SpeedReadSettingsRepository
import com.evgeniich.flashread.platform.coverFileNameFor

internal fun memoryBookRepository(
    books: MutableList<Book> = mutableListOf(),
): BookRepository {
    return BookRepository(
        onLoad = { books.toList() },
        onSave = {
            books.clear()
            books.addAll(it)
        },
    )
}

internal fun memoryReadingSessionRepository(
    positions: MutableMap<String, Int> = mutableMapOf(),
    wordOffsets: MutableMap<String, Int> = mutableMapOf(),
): ReadingSessionRepository {
    return ReadingSessionRepository(
        onLoad = { bookId ->
            ReadingPosition(
                bookId = bookId,
                paragraphIndex = positions[bookId] ?: 0,
                wordOffset = wordOffsets[bookId] ?: ReadingPosition.UNSET,
            )
        },
        onSave = { position ->
            positions[position.bookId] = position.paragraphIndex
            wordOffsets[position.bookId] = position.wordOffset
        },
    )
}

internal fun memorySpeedReadSettingsRepository(
    settings: Array<SpeedReadSettings> = arrayOf(SpeedReadSettings()),
): SpeedReadSettingsRepository {
    return SpeedReadSettingsRepository(
        onLoad = { settings[0] },
        onSave = { settings[0] = it },
    )
}

internal fun memoryReaderTextSettingsRepository(
    settings: Array<ReaderTextSettings> = arrayOf(ReaderTextSettings()),
): ReaderTextSettingsRepository {
    return ReaderTextSettingsRepository(
        onLoad = { settings[0] },
        onSave = { settings[0] = it },
    )
}

internal fun memoryRecentBookRepository(
    recentBookId: Array<String?> = arrayOf(null),
): RecentBookRepository {
    return RecentBookRepository(
        onLoad = { recentBookId[0] },
        onSave = { recentBookId[0] = it },
    )
}

internal fun memoryAppLanguageRepository(
    language: Array<AppLanguage> = arrayOf(AppLanguage.System),
): AppLanguageRepository {
    return AppLanguageRepository(
        onLoad = { language[0] },
        onSave = { language[0] = it },
    )
}

internal fun memoryCoverRepository(
    files: MutableMap<String, ByteArray> = mutableMapOf(),
): CoverRepository {
    return CoverRepository(
        onSave = { bookId, bytes, mimeType ->
            val name = coverFileNameFor(bookId, mimeType)
            files[name] = bytes
            name
        },
        onDelete = { files.remove(it) },
    )
}

internal class FakeYouTubeTranscriptFetcher(
    private val onFetch: suspend (videoId: String, languages: List<String>) -> YouTubeTranscript = { videoId, _ ->
        YouTubeTranscript(
            videoId = videoId,
            text = "Never gonna give you up",
            title = "Rick Astley",
        )
    },
) : YouTubeTranscriptFetcher {
    val recordedVideoIds = mutableListOf<String>()
    val recordedLanguages = mutableListOf<List<String>>()

    override suspend fun fetch(videoId: String, languages: List<String>): YouTubeTranscript {
        recordedVideoIds += videoId
        recordedLanguages += languages
        return onFetch(videoId, languages)
    }
}
