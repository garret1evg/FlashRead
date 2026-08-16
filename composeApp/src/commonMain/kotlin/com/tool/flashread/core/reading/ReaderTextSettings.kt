package com.tool.flashread.core.reading

import kotlin.math.roundToInt

data class ReaderTextSettings(
    val fontSizeSp: Int = ReaderTextDefaults.DEFAULT_FONT_SIZE_SP,
    val lineHeightMultiplier: Float = ReaderTextDefaults.DEFAULT_LINE_HEIGHT,
    val theme: ReaderTheme = ReaderTextDefaults.DEFAULT_THEME,
    val alignment: ReaderAlignment = ReaderTextDefaults.DEFAULT_ALIGNMENT,
) {
    fun normalized(): ReaderTextSettings = copy(
        fontSizeSp = fontSizeSp.coerceIn(
            ReaderTextDefaults.MIN_FONT_SIZE_SP,
            ReaderTextDefaults.MAX_FONT_SIZE_SP,
        ),
        lineHeightMultiplier = ReaderTextDefaults.snapLineHeight(lineHeightMultiplier),
    )
}

enum class ReaderTheme {
    Light,
    Sepia,
    Dark,
}

enum class ReaderAlignment {
    Start,
    Center,
    Justify,
}

object ReaderTextDefaults {
    const val MIN_FONT_SIZE_SP = 14
    const val MAX_FONT_SIZE_SP = 28
    const val DEFAULT_FONT_SIZE_SP = 18
    const val FONT_SIZE_SLIDER_STEPS = MAX_FONT_SIZE_SP - MIN_FONT_SIZE_SP - 1

    const val MIN_LINE_HEIGHT = 1.2f
    const val MAX_LINE_HEIGHT = 2.0f
    const val DEFAULT_LINE_HEIGHT = 1.55f
    const val LINE_HEIGHT_STEP = 0.05f
    val LINE_HEIGHT_SLIDER_STEPS = ((200 - 120) / 5) - 1

    val DEFAULT_THEME = ReaderTheme.Light
    val DEFAULT_ALIGNMENT = ReaderAlignment.Start

    fun snapLineHeight(value: Float): Float {
        val clamped = value.coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT)
        val steps = ((clamped - MIN_LINE_HEIGHT) / LINE_HEIGHT_STEP).roundToInt()
        val snapped = MIN_LINE_HEIGHT + steps * LINE_HEIGHT_STEP
        return ((snapped * 100f).roundToInt() / 100f).coerceIn(MIN_LINE_HEIGHT, MAX_LINE_HEIGHT)
    }
}
