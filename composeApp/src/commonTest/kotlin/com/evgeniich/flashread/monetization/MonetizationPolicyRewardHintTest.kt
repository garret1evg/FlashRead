package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationPolicyRewardHintTest {

    @Test
    fun shownWhenLevel2AppliedAndNeverEarnedReward() {
        assertTrue(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(),
                isAppInForeground = true,
            ),
        )
    }

    @Test
    fun shownWhenLevel2EverActivatedEvenIfLayoutNotLevel2() {
        assertTrue(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(
                    level2EverActivated = true,
                    appliedLayoutLevel = AdLevel.Level1,
                ),
                isAppInForeground = true,
            ),
        )
    }

    @Test
    fun hiddenWhenAppInBackground() {
        assertFalse(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(),
                isAppInForeground = false,
            ),
        )
    }

    @Test
    fun hiddenWhenRewardAlreadyEarned() {
        assertFalse(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(hasEverEarnedReward = true),
                isAppInForeground = true,
            ),
        )
    }

    @Test
    fun hiddenWhenHintAlreadyShown() {
        assertFalse(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(rewardHintShown = true),
                isAppInForeground = true,
            ),
        )
    }

    @Test
    fun hiddenWhenReadingSessionHadNoActivity() {
        assertFalse(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(readingSessionHadActivity = false),
                isAppInForeground = true,
            ),
        )
    }

    @Test
    fun shownWhenAppliedLayoutIsLevel2EvenIfLatchIsFalse() {
        assertTrue(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(
                    level2EverActivated = false,
                    appliedLayoutLevel = AdLevel.Level2,
                ),
                isAppInForeground = true,
            ),
        )
    }

    @Test
    fun hiddenBeforeLevel2Activation() {
        assertFalse(
            MonetizationPolicy.shouldShowRewardHint(
                state = hintState(
                    level2EverActivated = false,
                    appliedLayoutLevel = AdLevel.Level1,
                ),
                isAppInForeground = true,
            ),
        )
    }

    private fun hintState(
        level2EverActivated: Boolean = true,
        appliedLayoutLevel: AdLevel = AdLevel.Level2,
        hasEverEarnedReward: Boolean = false,
        rewardHintShown: Boolean = false,
        readingSessionHadActivity: Boolean = true,
    ) = MonetizationState(
        level2EverActivated = level2EverActivated,
        appliedLayoutLevel = appliedLayoutLevel,
        hasEverEarnedReward = hasEverEarnedReward,
        rewardHintShown = rewardHintShown,
        readingSessionHadActivity = readingSessionHadActivity,
    )
}
