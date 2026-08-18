package com.tool.flashread.platform

import androidx.core.content.edit
import com.tool.flashread.core.model.ReadingPosition

actual object ReadingPositionStorage {
    private const val PREFS_NAME = "flashread_reader_prefs"
    private const val KEY_PREFIX = "reading_position_"
    private const val WORD_OFFSET_KEY_PREFIX = "reading_word_offset_"

    actual fun savePosition(bookId: String, paragraphIndex: Int, wordOffset: Int) {
        val safeParagraphIndex = paragraphIndex.coerceAtLeast(0)
        val keyHash = bookId.hashCode()
        prefs()
            .edit {
                putInt(KEY_PREFIX + keyHash, safeParagraphIndex)
                putInt(WORD_OFFSET_KEY_PREFIX + keyHash, wordOffset)
            }
    }

    actual fun loadPosition(bookId: String): Int {
        return prefs().getInt(KEY_PREFIX + bookId.hashCode(), 0)
    }

    actual fun loadWordOffset(bookId: String): Int {
        return prefs().getInt(WORD_OFFSET_KEY_PREFIX + bookId.hashCode(), ReadingPosition.UNSET)
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
