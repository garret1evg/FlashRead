package com.tool.flashread.core.speedread

/**
 * Classic Spritz Optimal Recognition Point: the character the eye should land on.
 * When [spritzEnabled] is false the word is centered and no letter is highlighted.
 */
fun orpIndex(text: String, spritzEnabled: Boolean = true): Int? {
    if (!spritzEnabled) return null
    if (text.isEmpty()) return 0
    val index = when {
        text.length <= 1 -> 0
        text.length <= 5 -> 1
        text.length <= 9 -> 2
        text.length <= 13 -> 3
        else -> 4
    }
    return index.coerceAtMost(text.lastIndex)
}
