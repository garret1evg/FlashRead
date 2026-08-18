package com.tool.flashread.platform

import com.tool.flashread.core.model.ReadingPosition

actual object ReadingPositionStorage {
    private val inMemoryPositions = mutableMapOf<String, Int>()
    private val inMemoryWordOffsets = mutableMapOf<String, Int>()

    actual fun savePosition(bookId: String, paragraphIndex: Int, wordOffset: Int) {
        inMemoryPositions[bookId] = paragraphIndex.coerceAtLeast(0)
        inMemoryWordOffsets[bookId] = wordOffset
    }

    actual fun loadPosition(bookId: String): Int {
        return inMemoryPositions[bookId] ?: 0
    }

    actual fun loadWordOffset(bookId: String): Int {
        return inMemoryWordOffsets[bookId] ?: ReadingPosition.UNSET
    }
}
