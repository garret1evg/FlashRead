package com.evgeniich.flashread.core.theme

enum class AppTheme {
    System,
    Light,
    Sepia,
    Dark,
    ;

    fun toStorage(): String = name.lowercase()

    fun resolve(systemDark: Boolean): AppTheme = when (this) {
        System -> if (systemDark) Dark else Light
        Light, Sepia, Dark -> this
    }

    companion object {
        val DEFAULT = System

        fun fromStorage(value: String?): AppTheme {
            if (value.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: DEFAULT
        }
    }
}
