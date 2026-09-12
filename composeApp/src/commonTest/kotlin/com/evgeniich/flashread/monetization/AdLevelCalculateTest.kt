package com.evgeniich.flashread.monetization

import kotlin.test.Test
import kotlin.test.assertEquals

class AdLevelCalculateTest {

    @Test
    fun exactBoundariesUseInclusiveThresholds() {
        assertEquals(
            AdLevel.Initial,
            AdLevel.calculate(MonetizationConfig.initialAdFreePeriodMs - 1, usageDays = 1),
        )
        assertEquals(
            AdLevel.Level0,
            AdLevel.calculate(MonetizationConfig.initialAdFreePeriodMs, usageDays = 1),
        )
        assertEquals(
            AdLevel.Level0,
            AdLevel.calculate(MonetizationConfig.level1ThresholdMs - 1, usageDays = 1),
        )
        assertEquals(
            AdLevel.Level1,
            AdLevel.calculate(MonetizationConfig.level1ThresholdMs, usageDays = 1),
        )
        assertEquals(
            AdLevel.Level1,
            AdLevel.calculate(
                MonetizationConfig.level2TimeThresholdMs - 1,
                MonetizationConfig.level2DaysThreshold,
            ),
        )
        assertEquals(
            AdLevel.Level2,
            AdLevel.calculate(
                MonetizationConfig.level2TimeThresholdMs,
                MonetizationConfig.level2DaysThreshold,
            ),
        )
    }

    @Test
    fun fiveHoursOnFirstUsageDateStaysLevel1() {
        assertEquals(
            AdLevel.Level1,
            AdLevel.calculate(MonetizationConfig.level2TimeThresholdMs, usageDays = 1),
        )
    }

    @Test
    fun fiveHoursAndThreeDaysIsLevel2() {
        assertEquals(
            AdLevel.Level2,
            AdLevel.calculate(
                MonetizationConfig.level2TimeThresholdMs,
                MonetizationConfig.level2DaysThreshold,
            ),
        )
    }

    @Test
    fun threeDaysWithoutFiveHoursStaysLevel1() {
        assertEquals(
            AdLevel.Level1,
            AdLevel.calculate(
                MonetizationConfig.level2TimeThresholdMs - 1,
                MonetizationConfig.level2DaysThreshold,
            ),
        )
        assertEquals(
            AdLevel.Level1,
            AdLevel.calculate(
                MonetizationConfig.level1ThresholdMs,
                MonetizationConfig.level2DaysThreshold,
            ),
        )
    }

    @Test
    fun loweringUsageRecalculatesLevelWithoutNeedingDates() {
        val days = MonetizationConfig.level2DaysThreshold
        assertEquals(
            AdLevel.Level2,
            AdLevel.calculate(MonetizationConfig.level2TimeThresholdMs, days),
        )
        // 1h is past the 30min grace period and below the 2h Level1 threshold.
        val oneHourMs = MonetizationConfig.level1ThresholdMs / 2
        assertEquals(AdLevel.Level0, AdLevel.calculate(oneHourMs, days))
        assertEquals(
            AdLevel.Level1,
            AdLevel.calculate(MonetizationConfig.level1ThresholdMs, days),
        )
        // Manager.setActiveUsageTime is untested in commonTest (needs UsageTracker).
        // Dates are a separate MonetizationState field and are not inputs to calculate().
    }
}
