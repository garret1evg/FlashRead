package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertEquals

class UsageDatesTest {

    @Test
    fun shrinkingKeepsMostRecentDates() {
        val existing = listOf("2026-01-10", "2026-01-12", "2026-01-15")
        assertEquals(
            listOf("2026-01-12", "2026-01-15"),
            buildUsageDatesForCount(existing, count = 2, today = "2026-01-20"),
        )
        assertEquals(
            listOf("2026-01-15"),
            buildUsageDatesForCount(existing, count = 1, today = "2026-01-20"),
        )
    }

    @Test
    fun expandingAddsPastDatesFromTodaySkippingExisting() {
        val existing = listOf("2026-01-18")
        assertEquals(
            listOf("2026-01-18", "2026-01-19", "2026-01-20"),
            buildUsageDatesForCount(existing, count = 3, today = "2026-01-20"),
        )
    }

    @Test
    fun expandingFromEmptyGoesBackwardFromToday() {
        assertEquals(
            listOf("2026-01-18", "2026-01-19", "2026-01-20"),
            buildUsageDatesForCount(emptyList(), count = 3, today = "2026-01-20"),
        )
    }

    @Test
    fun expandingSkipsTodayWhenAlreadyPresent() {
        assertEquals(
            listOf("2026-01-18", "2026-01-19", "2026-01-20"),
            buildUsageDatesForCount(listOf("2026-01-20"), count = 3, today = "2026-01-20"),
        )
    }

    @Test
    fun zeroCountReturnsEmpty() {
        assertEquals(
            emptyList(),
            buildUsageDatesForCount(listOf("2026-01-20"), count = 0, today = "2026-01-20"),
        )
        assertEquals(
            emptyList(),
            buildUsageDatesForCount(listOf("2026-01-20"), count = -1, today = "2026-01-20"),
        )
    }

    @Test
    fun sameCountReturnsSortedExistingWithoutExtras() {
        assertEquals(
            listOf("2026-01-10", "2026-01-15"),
            buildUsageDatesForCount(listOf("2026-01-15", "2026-01-10"), count = 2, today = "2026-01-20"),
        )
    }

    @Test
    fun doesNotAddExtraDatesBeyondCount() {
        val result = buildUsageDatesForCount(emptyList(), count = 2, today = "2026-01-20")
        assertEquals(2, result.size)
        assertEquals(listOf("2026-01-19", "2026-01-20"), result)
    }
}
