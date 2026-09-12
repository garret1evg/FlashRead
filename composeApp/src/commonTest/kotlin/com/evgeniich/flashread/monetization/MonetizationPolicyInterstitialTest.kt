package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonetizationPolicyInterstitialTest {

    @Test
    fun eligibleWhenEveryConditionIsMet() {
        val now = 100_000L
        val state = eligibleState(now)
        assertTrue(MonetizationPolicy.isInterstitialEligible(state, now, readyContext))
        assertNull(MonetizationPolicy.getInterstitialBlockReason(state, now, readyContext))
    }

    @Test
    fun firstInterstitialUsesWallCooldownNotAccumulatedEligibility() {
        val now = 50_000L
        val state = eligibleState(now).copy(
            lastInterstitialMs = null,
            interstitialEligibilityAccumulatedMs = 0L,
        )
        assertTrue(state.isInterstitialCooldownComplete(now))
        assertTrue(MonetizationPolicy.isInterstitialEligible(state, now, readyContext))
        assertNull(MonetizationPolicy.getInterstitialBlockReason(state, now, readyContext))
    }

    @Test
    fun missingAnyConditionBlocks() {
        val now = 200_000L
        val lastShown = now - MonetizationConfig.interstitialIntervalMs
        val eligible = eligibleState(now, lastInterstitialMs = lastShown)

        assertIs<InterstitialBlockReason.LevelTooLow>(
            MonetizationPolicy.getInterstitialBlockReason(
                eligible.copy(appliedLayoutLevel = AdLevel.Level1, calculatedLevel = AdLevel.Level1),
                now,
                readyContext,
            ),
        )
        val rewarded = eligible.copy(rewardExpiresAtMs = now + 1, lastActivityMs = now)
        assertEquals(AdLevel.Initial, rewarded.appliedLevel)
        assertIs<InterstitialBlockReason.LevelTooLow>(
            MonetizationPolicy.getInterstitialBlockReason(rewarded, now, readyContext),
        )
        assertEquals(
            InterstitialBlockReason.RewardActive,
            MonetizationPolicy.getInterstitialBlockReason(
                rewarded.copy(developerLevelOverride = AdLevel.Level2),
                now,
                readyContext,
            ),
        )
        assertEquals(
            InterstitialBlockReason.NoReadingActivity,
            MonetizationPolicy.getInterstitialBlockReason(
                eligible.copy(readingSessionHadActivity = false),
                now,
                readyContext,
            ),
        )
        assertIs<InterstitialBlockReason.CooldownActive>(
            MonetizationPolicy.getInterstitialBlockReason(
                eligible.copy(lastInterstitialMs = now - MonetizationConfig.interstitialIntervalMs + 1),
                now,
                readyContext,
            ),
        )
        assertEquals(
            InterstitialBlockReason.SessionCapReached,
            MonetizationPolicy.getInterstitialBlockReason(
                eligible.copy(sessionInterstitialCount = MonetizationConfig.interstitialSessionCap),
                now,
                readyContext,
            ),
        )
        assertEquals(
            InterstitialBlockReason.DailyCapReached,
            MonetizationPolicy.getInterstitialBlockReason(
                eligible.copy(dailyInterstitialCount = MonetizationConfig.interstitialDailyCap),
                now,
                readyContext,
            ),
        )
        assertEquals(
            InterstitialBlockReason.HintShowing,
            MonetizationPolicy.getInterstitialBlockReason(
                eligible,
                now,
                readyContext.copy(isHintBeingShown = true),
            ),
        )
        assertEquals(
            InterstitialBlockReason.AdNotReady,
            MonetizationPolicy.getInterstitialBlockReason(
                eligible,
                now,
                readyContext.copy(isAdReady = false),
            ),
        )
        assertEquals(
            InterstitialBlockReason.AppInBackground,
            MonetizationPolicy.getInterstitialBlockReason(
                eligible,
                now,
                readyContext.copy(isAppInForeground = false),
            ),
        )

        assertFalse(
            MonetizationPolicy.isInterstitialEligible(
                eligible.copy(appliedLayoutLevel = AdLevel.Level1),
                now,
                readyContext,
            ),
        )
        assertFalse(MonetizationPolicy.isInterstitialEligible(eligible, now, MonetizationPolicy.InterstitialContext()))
    }

    @Test
    fun wallCooldownCompletesExactlyAtInterval() {
        val lastShown = 10_000L
        val state = eligibleState(lastShown).copy(lastInterstitialMs = lastShown)
        val justBefore = lastShown + MonetizationConfig.interstitialIntervalMs - 1
        val exactlyAt = lastShown + MonetizationConfig.interstitialIntervalMs

        assertFalse(state.isInterstitialCooldownComplete(justBefore))
        assertTrue(state.isInterstitialCooldownComplete(exactlyAt))
        assertIs<InterstitialBlockReason.CooldownActive>(
            MonetizationPolicy.getInterstitialBlockReason(state, justBefore, readyContext),
        )
        assertNull(MonetizationPolicy.getInterstitialBlockReason(state, exactlyAt, readyContext))
    }

    @Test
    fun sessionCapOneAndDailyCapTwoViaWithInterstitialShown() {
        val date = "2026-03-01"
        val t0 = 1_000_000L
        val afterFirst = eligibleState(t0).withInterstitialShown(t0, date)
        assertEquals(1, afterFirst.sessionInterstitialCount)
        assertEquals(1, afterFirst.dailyInterstitialCount)

        val afterCooldown = t0 + MonetizationConfig.interstitialIntervalMs
        assertEquals(
            InterstitialBlockReason.SessionCapReached,
            MonetizationPolicy.getInterstitialBlockReason(afterFirst, afterCooldown, readyContext),
        )

        val session2 = afterFirst.withSessionStart(afterCooldown).withReadingActivity()
        assertEquals(0, session2.sessionInterstitialCount)
        assertTrue(MonetizationPolicy.isInterstitialEligible(session2, afterCooldown, readyContext))

        val afterSecond = session2.withInterstitialShown(afterCooldown, date)
        assertEquals(MonetizationConfig.interstitialDailyCap, afterSecond.dailyInterstitialCount)

        val t2 = afterCooldown + MonetizationConfig.interstitialIntervalMs
        val session3 = afterSecond.withSessionStart(t2).withReadingActivity()
        assertEquals(
            InterstitialBlockReason.DailyCapReached,
            MonetizationPolicy.getInterstitialBlockReason(session3, t2, readyContext),
        )
        assertFalse(MonetizationPolicy.isInterstitialEligible(session3, t2, readyContext))
    }

    private val readyContext = MonetizationPolicy.InterstitialContext(
        isHintBeingShown = false,
        isAdReady = true,
        isAppInForeground = true,
    )

    private fun eligibleState(
        nowMs: Long,
        lastInterstitialMs: Long? = null,
    ) = MonetizationState(
        calculatedLevel = AdLevel.Level2,
        appliedLayoutLevel = AdLevel.Level2,
        readingSessionHadActivity = true,
        lastInterstitialMs = lastInterstitialMs,
        sessionInterstitialCount = 0,
        dailyInterstitialCount = 0,
        rewardExpiresAtMs = null,
        lastActivityMs = nowMs,
        interstitialEligibilityAccumulatedMs = 0L,
    )
}
