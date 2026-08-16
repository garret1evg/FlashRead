package com.tool.flashread.core.model

data class Book(
    val id: String,
    val title: String,
    val content: String,
    val sourceType: MaterialSourceType = MaterialSourceType.Book,
)

enum class MaterialSourceType {
    Book,
    YouTube,
}
