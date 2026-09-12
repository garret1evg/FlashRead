package com.evgeniich.flashread.monetization

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Builds a persisted usage-date list of exactly [count] distinct YYYY-MM-DD values.
 *
 * - If shrinking, keeps the most recent dates from [existing].
 * - If expanding, adds synthetic past local calendar dates going backward from [today],
 *   skipping dates already present, until the list size equals [count].
 *
 * Does not add extra dates beyond [count]. [count] is clamped to be non-negative.
 */
fun buildUsageDatesForCount(
    existing: List<String>,
    count: Int,
    today: String,
): List<String> {
    val target = count.coerceAtLeast(0)
    val sortedExisting = existing.filter { it.isNotBlank() }.distinct().sorted()

    if (target == 0) return emptyList()
    if (sortedExisting.size == target) return sortedExisting
    if (sortedExisting.size > target) return sortedExisting.takeLast(target)

    val dates = sortedExisting.toMutableList()
    val existingSet = dates.toMutableSet()
    var cursor = LocalDate.parse(today)
    while (dates.size < target) {
        val candidate = cursor.toString()
        if (candidate !in existingSet) {
            dates.add(candidate)
            existingSet.add(candidate)
        }
        cursor = cursor.minus(1, DateTimeUnit.DAY)
    }
    return dates.sorted()
}
