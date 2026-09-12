package com.evgeniich.flashread.monetization

import com.evgeniich.flashread.navigation.AppRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationPolicyBannerTest {

    @Test
    fun initialShowsNoBanners() {
        val state = stateAt(AdLevel.Initial)
        assertNoBanners(state, AppRoute.Home, AppRoute.Library, AppRoute.Settings, AppRoute.Reader)
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = false))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = true))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedRead))
    }

    @Test
    fun level0ShowsHomeAndLibraryOnly() {
        val state = stateAt(AdLevel.Level0)
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Home))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Library))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.Settings))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.Reader))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = false))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = true))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedRead))
    }

    @Test
    fun level1AddsSettingsReaderAndPausedPlayer() {
        val state = stateAt(AdLevel.Level1)
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Home))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Library))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Settings))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Reader))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = false))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = true))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedRead))
    }

    @Test
    fun level2AddsPlayingPlayerBanner() {
        val state = stateAt(AdLevel.Level2)
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Home))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Library))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.Settings))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = false))
        assertTrue(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = true))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedRead))
    }

    @Test
    fun speedReadSetupNeverShowsBanner() {
        AdLevel.entries.forEach { level ->
            assertFalse(MonetizationPolicy.shouldShowBanner(stateAt(level), AppRoute.SpeedRead))
            assertFalse(MonetizationPolicy.shouldShowBanner(level, AppRoute.SpeedRead))
        }
    }

    @Test
    fun activeRewardAppliesInitialLevelAndHidesBanners() {
        val now = 20_000L
        val state = MonetizationState(
            calculatedLevel = AdLevel.Level2,
            appliedLayoutLevel = AdLevel.Level2,
            rewardExpiresAtMs = now + MonetizationConfig.rewardDurationMs,
            lastActivityMs = now,
        )
        assertEquals(AdLevel.Initial, state.appliedLevel)
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.Home))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.Library))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.Settings))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.Reader))
        assertFalse(MonetizationPolicy.shouldShowBanner(state, AppRoute.SpeedReadPlayer, isSpeedReadPlaying = true))
    }

    private fun assertNoBanners(state: MonetizationState, vararg routes: AppRoute) {
        routes.forEach { route ->
            assertFalse(MonetizationPolicy.shouldShowBanner(state, route))
        }
    }

    private fun stateAt(level: AdLevel) = MonetizationState(
        calculatedLevel = level,
        appliedLayoutLevel = level,
    )
}
