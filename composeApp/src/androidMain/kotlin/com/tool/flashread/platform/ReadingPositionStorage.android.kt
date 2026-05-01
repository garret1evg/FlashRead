package com.tool.flashread.platform

import androidx.core.content.edit

actual object ReadingPositionStorage {
    private const val PREFS_NAME = "flashread_reader_prefs"
    private const val KEY_PREFIX = "reading_position_"

    actual fun savePosition(bookId: String, paragraphIndex: Int) {
        val safeParagraphIndex = paragraphIndex.coerceAtLeast(0)
        prefs()
            .edit {
                putInt(KEY_PREFIX + bookId.hashCode(), safeParagraphIndex)
            }
    }

    actual fun loadPosition(bookId: String): Int {
        return prefs().getInt(KEY_PREFIX + bookId.hashCode(), 0)
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
