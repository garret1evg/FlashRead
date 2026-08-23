package com.tool.flashread

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.data.repository.BookRepository
import com.tool.flashread.data.repository.CoverRepository
import com.tool.flashread.data.repository.ReaderTextSettingsRepository
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.RecentBookRepository
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
import com.tool.flashread.platform.coverFileNameFor

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
