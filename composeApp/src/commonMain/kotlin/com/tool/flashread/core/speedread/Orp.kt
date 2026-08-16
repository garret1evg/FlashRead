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

data class OrpParts(
    val before: String,
    val pivot: String,
    val after: String,
    val pivotIndex: Int?,
) {
    companion object {
        val Empty = OrpParts(before = "", pivot = "", after = "", pivotIndex = null)
    }
}

fun orpParts(text: String, spritzEnabled: Boolean = true): OrpParts {
    if (text.isEmpty()) {
        val index = orpIndex(text, spritzEnabled)
        return OrpParts(before = "", pivot = "", after = "", pivotIndex = index)
    }
    val index = orpIndex(text, spritzEnabled) ?: return OrpParts(
        before = "",
        pivot = "",
        after = text,
        pivotIndex = null,
    )
    val safeIndex = snapOrpToLetter(text, index.coerceIn(0, text.lastIndex))
    return OrpParts(
        before = text.substring(0, safeIndex),
        pivot = text.substring(safeIndex, safeIndex + 1),
        after = text.substring(safeIndex + 1),
        pivotIndex = safeIndex,
    )
}

private fun snapOrpToLetter(text: String, index: Int): Int {
    if (text[index].isLetterOrDigit()) return index
    val forward = (index..text.lastIndex).firstOrNull { text[it].isLetterOrDigit() }
    val backward = (index downTo 0).firstOrNull { text[it].isLetterOrDigit() }
    return forward ?: backward ?: index
}
