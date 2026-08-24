package com.evgeniich.flashread.core.reading

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderTextSettingsTest {

    @Test
    fun defaultsMatchRequestedReadingStyle() {
        val settings = ReaderTextSettings()
        assertEquals(18, settings.fontSizeSp)
        assertEquals(1.55f, settings.lineHeightMultiplier, 0.001f)
        assertEquals(ReaderTheme.Light, settings.theme)
        assertEquals(ReaderAlignment.Start, settings.alignment)
        assertEquals(settings, settings.normalized())
    }

    @Test
    fun normalizedClampsFontSizeAndLineHeight() {
        val tooLow = ReaderTextSettings(fontSizeSp = 8, lineHeightMultiplier = 0.5f).normalized()
        assertEquals(ReaderTextDefaults.MIN_FONT_SIZE_SP, tooLow.fontSizeSp)
        assertEquals(ReaderTextDefaults.MIN_LINE_HEIGHT, tooLow.lineHeightMultiplier, 0.001f)

        val tooHigh = ReaderTextSettings(fontSizeSp = 48, lineHeightMultiplier = 3f).normalized()
        assertEquals(ReaderTextDefaults.MAX_FONT_SIZE_SP, tooHigh.fontSizeSp)
        assertEquals(ReaderTextDefaults.MAX_LINE_HEIGHT, tooHigh.lineHeightMultiplier, 0.001f)
    }

    @Test
    fun snapLineHeightUsesFiveHundredths() {
        assertEquals(1.55f, ReaderTextDefaults.snapLineHeight(1.53f), 0.001f)
        assertEquals(1.55f, ReaderTextDefaults.snapLineHeight(1.57f), 0.001f)
        assertEquals(1.6f, ReaderTextDefaults.snapLineHeight(1.58f), 0.001f)
    }
}
