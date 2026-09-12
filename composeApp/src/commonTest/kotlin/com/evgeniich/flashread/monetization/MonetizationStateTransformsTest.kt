package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonetizationStateTransformsTest {

    @Test
    fun withClearedRewardClearsExpiryAndKeepsEarnedFlag() {
        val state = MonetizationState(
            rewardExpiresAtMs = 50_000L,
            hasEverEarnedReward = true,
            rewardHintShown = true,
            interstitialEligibilityAccumulatedMs = 12_000L,
            dailyInterstitialCount = 1,
            sessionInterstitialCount = 1,
        )
        val cleared = state.withClearedReward()

        assertNull(cleared.rewardExpiresAtMs)
        assertTrue(cleared.hasEverEarnedReward)
        assertTrue(cleared.rewardHintShown)
        assertEquals(12_000L, cleared.interstitialEligibilityAccumulatedMs)
        assertEquals(1, cleared.dailyInterstitialCount)
        assertEquals(1, cleared.sessionInterstitialCount)
    }

    @Test
    fun withRewardReplacesExpiryInsteadOfStacking() {
        val t1 = 1_000L
        val first = MonetizationState.INITIAL.withReward(t1)
        assertEquals(t1 + MonetizationConfig.rewardDurationMs, first.rewardExpiresAtMs)
        assertTrue(first.hasEverEarnedReward)
        assertEquals(0L, first.interstitialEligibilityAccumulatedMs)

        val t2 = t1 + 10_000L
        val second = first.withReward(t2)
        assertEquals(t2 + MonetizationConfig.rewardDurationMs, second.rewardExpiresAtMs)
        assertNotEquals(t1 + 2 * MonetizationConfig.rewardDurationMs, second.rewardExpiresAtMs)
        assertTrue(second.hasEverEarnedReward)
    }

    @Test
    fun isRewardActiveIsTrueBeforeExpiryAndFalseAtOrAfter() {
        val expiry = 50_000L
        val state = MonetizationState(rewardExpiresAtMs = expiry)

        assertTrue(state.isRewardActive(expiry - 1))
        assertFalse(state.isRewardActive(expiry))
        assertFalse(state.isRewardActive(expiry + 1))
        assertFalse(MonetizationState.INITIAL.isRewardActive(0L))
    }

    @Test
    fun appliedLevelIsInitialWhileRewardIsActive() {
        val now = 10_000L
        val state = MonetizationState(
            calculatedLevel = AdLevel.Level2,
            appliedLayoutLevel = AdLevel.Level2,
            rewardExpiresAtMs = now + 1,
            lastActivityMs = now,
        )
        assertTrue(state.isRewardActive)
        assertEquals(AdLevel.Initial, state.appliedLevel)
        assertEquals(AdLevel.Level2, state.appliedLayoutLevel)
    }

    @Test
    fun isNewSessionDependsOnInactivityGapNotMidnight() {
        val sessionStart = 1_000L
        val lastActivity = 2_000L
        val state = MonetizationState(
            sessionStartMs = sessionStart,
            lastActivityMs = lastActivity,
        )

        assertFalse(state.isNewSession(lastActivity + MonetizationConfig.sessionTimeoutMs - 1))
        assertTrue(state.isNewSession(lastActivity + MonetizationConfig.sessionTimeoutMs))

        val midnightMs = 1_704_067_200_000L
        val aroundMidnight = MonetizationState(
            sessionStartMs = midnightMs - MonetizationConfig.sessionTimeoutMs,
            lastActivityMs = midnightMs - 10 * 60_000L,
        )
        val tenMinutesAfterMidnight = midnightMs + 10 * 60_000L
        assertFalse(aroundMidnight.isNewSession(tenMinutesAfterMidnight))
        assertTrue(MonetizationState.INITIAL.isNewSession(0L))
    }

    @Test
    fun withSessionStartResetsSessionCounters() {
        val now = 40_000L
        val started = MonetizationState(
            sessionId = 3L,
            sessionInterstitialCount = 1,
            dailyInterstitialCount = 1,
            readingSessionHadActivity = true,
        ).withSessionStart(now)

        assertEquals(now, started.sessionStartMs)
        assertEquals(now, started.lastActivityMs)
        assertEquals(4L, started.sessionId)
        assertEquals(0, started.sessionInterstitialCount)
        assertEquals(1, started.dailyInterstitialCount)
        assertFalse(started.readingSessionHadActivity)
    }

    @Test
    fun withInterstitialShownIncrementsCapsAndResetsEligibility() {
        val shownAt = 80_000L
        val date = "2026-01-15"
        val shown = MonetizationState(
            sessionInterstitialCount = 0,
            dailyInterstitialCount = 0,
            interstitialEligibilityAccumulatedMs = 12_000L,
        ).withInterstitialShown(shownAt, date)

        assertEquals(shownAt, shown.lastInterstitialMs)
        assertEquals(1, shown.sessionInterstitialCount)
        assertEquals(1, shown.dailyInterstitialCount)
        assertEquals(date, shown.lastInterstitialResetDate)
        assertEquals(0L, shown.interstitialEligibilityAccumulatedMs)

        val sameDay = shown.withInterstitialShown(shownAt + 1, date)
        assertEquals(2, sameDay.sessionInterstitialCount)
        assertEquals(2, sameDay.dailyInterstitialCount)

        val nextDay = sameDay.withInterstitialShown(shownAt + 2, "2026-01-16")
        assertEquals(3, nextDay.sessionInterstitialCount)
        assertEquals(1, nextDay.dailyInterstitialCount)
        assertEquals("2026-01-16", nextDay.lastInterstitialResetDate)
    }

    @Test
    fun addedUsageRecalculatesLevelAndLeavesDatesUnchanged() {
        val dates = listOf("2026-01-14", "2026-01-15", "2026-01-16")
        val start = MonetizationState(
            totalUsageMs = MonetizationConfig.level1ThresholdMs - 1,
            uniqueUsageDays = dates.size,
            usageDates = dates,
            calculatedLevel = AdLevel.Level0,
        )
        val updated = start.withAddedUsage(1)
        assertEquals(MonetizationConfig.level1ThresholdMs, updated.totalUsageMs)
        assertEquals(AdLevel.Level1, updated.calculatedLevel)
        assertEquals(dates, updated.usageDates)
        assertEquals(dates.size, updated.uniqueUsageDays)

        // No state helper lowers usage; Manager.setActiveUsageTime is untested in commonTest.
        val oneHourMs = MonetizationConfig.level1ThresholdMs / 2
        val lowered = updated.copy(
            totalUsageMs = oneHourMs,
            calculatedLevel = AdLevel.calculate(oneHourMs, updated.uniqueUsageDays),
        )
        assertEquals(AdLevel.Level0, lowered.calculatedLevel)
        assertEquals(dates, lowered.usageDates)
        assertEquals(dates.size, lowered.uniqueUsageDays)
    }

    @Test
    fun developerLevelOverrideWinsOverReward() {
        val now = 5_000L
        val state = MonetizationState(
            appliedLayoutLevel = AdLevel.Level2,
            rewardExpiresAtMs = now + MonetizationConfig.rewardDurationMs,
            lastActivityMs = now,
        ).withDeveloperOverride(AdLevel.Level0)

        assertEquals(AdLevel.Level0, state.appliedLevel)
        assertNull(state.withDeveloperOverride(null).developerLevelOverride)
    }
}
