package com.tool.flashread.platform

actual object ReadingPositionStorage {
    private val inMemoryPositions = mutableMapOf<String, Int>()

    actual fun savePosition(bookId: String, paragraphIndex: Int) {
        inMemoryPositions[bookId] = paragraphIndex.coerceAtLeast(0)
    }

    actual fun loadPosition(bookId: String): Int {
        return inMemoryPositions[bookId] ?: 0
    }
}
