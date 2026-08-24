package com.evgeniich.flashread.core.model

data class Book(
    val id: String,
    val title: String,
    val content: String,
    val sourceType: MaterialSourceType = MaterialSourceType.Book,
    val wordCount: Int = 0,
    val paragraphCount: Int = 0,
    val coverFileName: String? = null,
)

enum class MaterialSourceType {
    Book,
}
