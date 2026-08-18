package com.tool.flashread.core.model

data class ReadingPosition(
    val bookId: String,
    val paragraphIndex: Int,
    /** Character index in `book.content`; [UNSET] means start at the first word of [paragraphIndex]. */
    val wordOffset: Int = UNSET,
) {
    companion object {
        const val UNSET: Int = -1
    }
}
