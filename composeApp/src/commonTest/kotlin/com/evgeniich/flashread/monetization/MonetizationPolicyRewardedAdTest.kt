package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationPolicyRewardedAdTest {

    @Test
    fun hiddenDuringInitialLevel() {
        val now = 1_000L
        val state = MonetizationState(
            calculatedLevel = AdLevel.Initial,
            appliedLayoutLevel = AdLevel.Initial,
            lastActivityMs = now,
        )
        assertFalse(MonetizationPolicy.canOfferRewardedAd(state, now))
    }

    @Test
    fun offeredAfterInitialWhenNoRewardIsActive() {
        listOf(AdLevel.Level0, AdLevel.Level1, AdLevel.Level2).forEach { level ->
            val now = 2_000L
            val state = MonetizationState(
                calculatedLevel = level,
                appliedLayoutLevel = level,
                lastActivityMs = now,
            )
            assertTrue(MonetizationPolicy.canOfferRewardedAd(state, now), "level=$level")
        }
    }

    @Test
    fun hiddenWhileRewardIsActiveAndOfferedAtExpiry() {
        val expiry = 80_000L
        val before = expiry - 1
        val active = MonetizationState(
            calculatedLevel = AdLevel.Level1,
            appliedLayoutLevel = AdLevel.Level1,
            rewardExpiresAtMs = expiry,
            lastActivityMs = before,
        )
        assertFalse(MonetizationPolicy.canOfferRewardedAd(active, before))

        val atExpiry = active.copy(lastActivityMs = expiry)
        assertFalse(atExpiry.isRewardActive(expiry))
        assertTrue(MonetizationPolicy.canOfferRewardedAd(atExpiry, expiry))
    }
}
