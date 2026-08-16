package com.tool.flashread.platform

import androidx.core.content.edit
import com.tool.flashread.core.reading.ReaderAlignment
import com.tool.flashread.core.reading.ReaderTextDefaults
import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.core.reading.ReaderTheme

actual object ReaderTextSettingsStorage {
    private const val PREFS_NAME = "flashread_reader_text_prefs"
    private const val KEY_FONT_SIZE = "font_size_sp"
    private const val KEY_LINE_HEIGHT = "line_height"
    private const val KEY_THEME = "theme"
    private const val KEY_ALIGNMENT = "alignment"

    actual fun save(settings: ReaderTextSettings) {
        val normalized = settings.normalized()
        prefs().edit {
            putInt(KEY_FONT_SIZE, normalized.fontSizeSp)
            putFloat(KEY_LINE_HEIGHT, normalized.lineHeightMultiplier)
            putString(KEY_THEME, normalized.theme.name)
            putString(KEY_ALIGNMENT, normalized.alignment.name)
        }
    }

    actual fun load(): ReaderTextSettings {
        return ReaderTextSettings(
            fontSizeSp = prefs().getInt(KEY_FONT_SIZE, ReaderTextDefaults.DEFAULT_FONT_SIZE_SP),
            lineHeightMultiplier = prefs().getFloat(
                KEY_LINE_HEIGHT,
                ReaderTextDefaults.DEFAULT_LINE_HEIGHT,
            ),
            theme = prefs().getString(KEY_THEME, null).toReaderTheme(),
            alignment = prefs().getString(KEY_ALIGNMENT, null).toReaderAlignment(),
        ).normalized()
    }

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)

    private fun String?.toReaderTheme(): ReaderTheme {
        return ReaderTheme.entries.firstOrNull { it.name == this } ?: ReaderTextDefaults.DEFAULT_THEME
    }

    private fun String?.toReaderAlignment(): ReaderAlignment {
        return ReaderAlignment.entries.firstOrNull { it.name == this }
            ?: ReaderTextDefaults.DEFAULT_ALIGNMENT
    }
}
