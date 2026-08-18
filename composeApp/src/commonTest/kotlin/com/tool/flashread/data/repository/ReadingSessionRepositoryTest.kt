package com.tool.flashread.data.repository

import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.memoryReadingSessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingSessionRepositoryTest {

    @Test
    fun missingWordOffsetLoadsAsUnset() {
        val repository = memoryReadingSessionRepository(
            positions = mutableMapOf("book-1" to 3),
        )

        assertEquals(
            ReadingPosition(bookId = "book-1", paragraphIndex = 3, wordOffset = ReadingPosition.UNSET),
            repository.getPosition("book-1"),
        )
    }

    @Test
    fun saveAndLoadPersistsWordOffset() {
        val positions = mutableMapOf<String, Int>()
        val wordOffsets = mutableMapOf<String, Int>()
        val repository = memoryReadingSessionRepository(positions, wordOffsets)

        repository.savePosition(
            ReadingPosition(bookId = "book-1", paragraphIndex = 2, wordOffset = 42),
        )

        assertEquals(2, positions["book-1"])
        assertEquals(42, wordOffsets["book-1"])
        assertEquals(
            ReadingPosition(bookId = "book-1", paragraphIndex = 2, wordOffset = 42),
            repository.getPosition("book-1"),
        )
    }

    @Test
    fun saveWithoutWordOffsetStoresUnset() {
        val wordOffsets = mutableMapOf<String, Int>()
        val repository = memoryReadingSessionRepository(wordOffsets = wordOffsets)

        repository.savePosition(ReadingPosition(bookId = "book-1", paragraphIndex = 1))

        assertEquals(ReadingPosition.UNSET, wordOffsets["book-1"])
        assertEquals(ReadingPosition.UNSET, repository.getPosition("book-1").wordOffset)
    }
}
