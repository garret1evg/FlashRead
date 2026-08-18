package com.tool.flashread.ui.reader

/**
 * Represents the highlighted start word in the reader.
 *
 * @param paragraphIndex Index of the paragraph containing the word.
 * @param localStart Start index within the trimmed paragraph text.
 * @param localEnd End index (exclusive) within the trimmed paragraph text.
 * @param contentOffset Character offset in book.content where the word starts.
 * @param pinned True if the word was explicitly selected by tap (not auto-following scroll).
 */
data class ReaderStartWord(
    val paragraphIndex: Int,
    val localStart: Int,
    val localEnd: Int,
    val contentOffset: Int,
    val pinned: Boolean,
)
