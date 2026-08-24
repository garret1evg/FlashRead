package com.evgeniich.flashread.platform

import androidx.core.content.edit

actual object RecentBookStorage {
    private const val PREFS_NAME = "flashread_recent_book_prefs"
    private const val KEY_BOOK_ID = "recent_book_id"

    actual fun save(bookId: String?) {
        prefs().edit {
            if (bookId.isNullOrBlank()) {
                remove(KEY_BOOK_ID)
            } else {
                putString(KEY_BOOK_ID, bookId)
            }
        }
    }

    actual fun load(): String? {
        return prefs().getString(KEY_BOOK_ID, null)?.takeIf { it.isNotBlank() }
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
