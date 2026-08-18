package com.tool.flashread.platform

expect object ReadingPositionStorage {
    fun savePosition(bookId: String, paragraphIndex: Int, wordOffset: Int)
    fun loadPosition(bookId: String): Int
    fun loadWordOffset(bookId: String): Int
}
