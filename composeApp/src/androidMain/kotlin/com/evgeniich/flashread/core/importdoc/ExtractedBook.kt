package com.evgeniich.flashread.core.importdoc

data class ExtractedBook(
    val title: String?,
    val content: String,
    val coverBytes: ByteArray? = null,
    val coverMimeType: String? = null,
)
