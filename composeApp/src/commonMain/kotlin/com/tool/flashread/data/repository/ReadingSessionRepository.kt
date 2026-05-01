package com.tool.flashread.data.repository

import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.platform.ReadingPositionStorage

class ReadingSessionRepository {
    fun savePosition(position: ReadingPosition) {
        ReadingPositionStorage.savePosition(
            bookId = position.bookId,
            paragraphIndex = position.paragraphIndex,
        )
    }

    fun getPosition(bookId: String): ReadingPosition {
        return ReadingPosition(
            bookId = bookId,
            paragraphIndex = ReadingPositionStorage.loadPosition(bookId),
        )
    }
}
