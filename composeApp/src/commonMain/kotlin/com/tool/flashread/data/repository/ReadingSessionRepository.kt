package com.tool.flashread.data.repository

import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.platform.ReadingPositionStorage

class ReadingSessionRepository(
    private val onLoad: (String) -> ReadingPosition = { bookId ->
        ReadingPosition(
            bookId = bookId,
            paragraphIndex = ReadingPositionStorage.loadPosition(bookId),
            wordOffset = ReadingPositionStorage.loadWordOffset(bookId),
        )
    },
    private val onSave: (ReadingPosition) -> Unit = { position ->
        ReadingPositionStorage.savePosition(
            bookId = position.bookId,
            paragraphIndex = position.paragraphIndex,
            wordOffset = position.wordOffset,
        )
    },
) {
    fun savePosition(position: ReadingPosition) {
        onSave(position)
    }

    fun getPosition(bookId: String): ReadingPosition = onLoad(bookId)
}
