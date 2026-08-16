package com.tool.flashread

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.data.repository.BookRepository
import com.tool.flashread.data.repository.ReaderTextSettingsRepository
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.SpeedReadSettingsRepository

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
): ReadingSessionRepository {
    return ReadingSessionRepository(
        onLoad = { positions[it] ?: 0 },
        onSave = { bookId, paragraphIndex -> positions[bookId] = paragraphIndex },
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
