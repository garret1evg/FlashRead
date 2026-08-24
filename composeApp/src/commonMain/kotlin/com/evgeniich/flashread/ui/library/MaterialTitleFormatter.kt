package com.evgeniich.flashread.ui.library

object MaterialTitleFormatter {
    private val knownExtensions = setOf(
        "txt", "epub", "fb2", "pdf", "doc", "docx", "md", "rtf", "html", "htm",
    )
    private val trailingTechnicalSuffix = Regex("""[\s\-]+(?:\(\d{3,}\)|\d{3,})$""")

    fun displayTitle(rawTitle: String): String {
        var name = rawTitle.trim()
        name = stripKnownExtension(name)
        name = name.replace('_', ' ')
        name = name.replace(Regex("\\s+"), " ").trim()
        name = name.replace(trailingTechnicalSuffix, "").trim()
        return name.ifBlank { rawTitle.trim() }
    }

    private fun stripKnownExtension(name: String): String {
        val dot = name.lastIndexOf('.')
        if (dot <= 0) return name
        val extension = name.substring(dot + 1).lowercase()
        return if (extension in knownExtensions) name.substring(0, dot) else name
    }
}
