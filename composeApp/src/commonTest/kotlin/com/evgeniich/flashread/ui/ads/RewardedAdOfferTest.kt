package com.evgeniich.flashread.ui.ads

import com.evgeniich.flashread.monetization.AdLevel
import com.evgeniich.flashread.monetization.MonetizationConfig
import com.evgeniich.flashread.monetization.MonetizationState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RewardedAdOfferTest {

    @Test
    fun hiddenDuringInitialAdFreePeriod() {
        assertFalse(
            shouldShowRewardedAdOffer(
                state = MonetizationState.INITIAL,
                nowMs = 0L,
                adsAllowed = true,
            ),
        )
    }

    @Test
    fun shownAfterInitialPeriodWhenAdsAllowed() {
        val state = MonetizationState(
            totalUsageMs = MonetizationConfig.initialAdFreePeriodMs,
            calculatedLevel = AdLevel.Level0,
        )
        assertTrue(shouldShowRewardedAdOffer(state, nowMs = 0L, adsAllowed = true))
    }

    @Test
    fun hiddenAfterInitialPeriodWhenAdsNotAllowed() {
        val state = MonetizationState(
            totalUsageMs = MonetizationConfig.initialAdFreePeriodMs,
            calculatedLevel = AdLevel.Level0,
        )
        assertFalse(shouldShowRewardedAdOffer(state, nowMs = 0L, adsAllowed = false))
    }

    @Test
    fun shownWhileRewardActiveEvenIfAdsNotAllowed() {
        val state = MonetizationState(rewardExpiresAtMs = 10_000L)
        assertTrue(shouldShowRewardedAdOffer(state, nowMs = 1_000L, adsAllowed = false))
    }

    @Test
    fun shownWhenCalculatedLevelLeftInitialBeforeLayoutApply() {
        val state = MonetizationState(
            totalUsageMs = MonetizationConfig.initialAdFreePeriodMs,
            calculatedLevel = AdLevel.Level0,
            appliedLayoutLevel = AdLevel.Initial,
        )
        assertTrue(shouldShowRewardedAdOffer(state, nowMs = 0L, adsAllowed = true))
    }

    @Test
    fun hiddenOneMillisecondBeforeInitialPeriodEnds() {
        val state = MonetizationState(
            totalUsageMs = MonetizationConfig.initialAdFreePeriodMs - 1,
            calculatedLevel = AdLevel.Initial,
            appliedLayoutLevel = AdLevel.Initial,
        )
        assertFalse(shouldShowRewardedAdOffer(state, nowMs = 0L, adsAllowed = true))
    }
}
