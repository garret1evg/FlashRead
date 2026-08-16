package com.tool.flashread.data.repository

import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.platform.ReadingPositionStorage

class ReadingSessionRepository(
    private val onLoad: (String) -> Int = { ReadingPositionStorage.loadPosition(it) },
    private val onSave: (String, Int) -> Unit = { bookId, paragraphIndex ->
        ReadingPositionStorage.savePosition(bookId, paragraphIndex)
    },
) {
    fun savePosition(position: ReadingPosition) {
        onSave(position.bookId, position.paragraphIndex)
    }

    fun getPosition(bookId: String): ReadingPosition {
        return ReadingPosition(
            bookId = bookId,
            paragraphIndex = onLoad(bookId),
        )
    }
}
