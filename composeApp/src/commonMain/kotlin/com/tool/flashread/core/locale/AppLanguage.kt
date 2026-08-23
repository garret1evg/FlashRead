package com.tool.flashread.core.locale

sealed interface AppLanguage {
    data object System : AppLanguage
    data class Language(val code: String) : AppLanguage

    fun toStorage(): String = when (this) {
        System -> STORAGE_SYSTEM
        is Language -> code
    }

    companion object {
        const val STORAGE_SYSTEM = "system"
        const val FALLBACK_CODE = "en"
        const val ARABIC_CODE = "ar"

        val SUPPORTED_CODES = setOf(
            "en",
            "es",
            "pt",
            "fr",
            "de",
            "ru",
            "uk",
            "hi",
            ARABIC_CODE,
        )

        fun fromStorage(value: String?): AppLanguage {
            if (value.isNullOrBlank() || value.equals(STORAGE_SYSTEM, ignoreCase = true)) {
                return System
            }
            return language(value) ?: System
        }

        fun language(code: String): Language? {
            val subtag = languageSubtag(code)
            return if (subtag in SUPPORTED_CODES) Language(subtag) else null
        }
    }
}

fun languageSubtag(tag: String): String {
    val normalized = tag.trim().replace('_', '-')
    if (normalized.isEmpty()) return ""
    return normalized.substringBefore('-').lowercase()
}

fun resolveLocaleOverride(
    preference: AppLanguage,
    systemLanguageTag: String,
): String? {
    return when (preference) {
        AppLanguage.System -> {
            val systemCode = languageSubtag(systemLanguageTag)
            if (systemCode in AppLanguage.SUPPORTED_CODES) null else AppLanguage.FALLBACK_CODE
        }
        is AppLanguage.Language -> preference.code
    }
}

fun isRtlLocaleOverride(localeOverride: String?): Boolean {
    return localeOverride != null && languageSubtag(localeOverride) == AppLanguage.ARABIC_CODE
}
