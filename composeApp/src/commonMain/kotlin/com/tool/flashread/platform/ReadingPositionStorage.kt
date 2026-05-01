package com.tool.flashread.platform

expect object ReadingPositionStorage {
    fun savePosition(bookId: String, paragraphIndex: Int)
    fun loadPosition(bookId: String): Int
}
