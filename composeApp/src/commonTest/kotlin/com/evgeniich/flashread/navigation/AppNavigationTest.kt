package com.evgeniich.flashread.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavigationTest {

    @Test
    fun topLevelRoutes_areHomeLibrarySettings() {
        assertTrue(AppRoute.Home.isTopLevel)
        assertTrue(AppRoute.Library.isTopLevel)
        assertTrue(AppRoute.Settings.isTopLevel)
        assertFalse(AppRoute.Reader.isTopLevel)
        assertFalse(AppRoute.SpeedRead.isTopLevel)
        assertFalse(AppRoute.SpeedReadPlayer.isTopLevel)
        assertFalse(AppRoute.PrivacyPolicy.isTopLevel)
        assertFalse(AppRoute.Terms.isTopLevel)
        assertFalse(AppRoute.BookEditor.isTopLevel)
        assertFalse(AppRoute.QuickSpeedRead.isTopLevel)
    }

    @Test
    fun fromRoute_mapsOnlyBottomNavigationDestinations() {
        assertEquals(AppScreen.Home, AppScreen.fromRoute(AppRoute.Home))
        assertEquals(AppScreen.Library, AppScreen.fromRoute(AppRoute.Library))
        assertEquals(AppScreen.Settings, AppScreen.fromRoute(AppRoute.Settings))
        assertNull(AppScreen.fromRoute(AppRoute.Reader))
        assertNull(AppScreen.fromRoute(AppRoute.SpeedRead))
        assertNull(AppScreen.fromRoute(AppRoute.SpeedReadPlayer))
        assertNull(AppScreen.fromRoute(AppRoute.PrivacyPolicy))
        assertNull(AppScreen.fromRoute(AppRoute.Terms))
        assertNull(AppScreen.fromRoute(AppRoute.BookEditor))
        assertNull(AppScreen.fromRoute(AppRoute.QuickSpeedRead))
    }

    @Test
    fun navigateToTopLevel_replacesNestedStack() {
        val backStack = mutableListOf(AppRoute.Library, AppRoute.Reader, AppRoute.SpeedRead)
        backStack.navigateToTopLevel(AppRoute.Home)
        assertEquals(listOf(AppRoute.Home), backStack.toList())
    }

    @Test
    fun navigateToTopLevel_isNoOpWhenAlreadySingleDestination() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Library)
        backStack.navigateToTopLevel(AppRoute.Library)
        assertEquals(listOf(AppRoute.Library), backStack.toList())
    }

    @Test
    fun pushIfNeeded_opensReaderThenSetupThenPlayer() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Library)
        backStack.pushIfNeeded(AppRoute.Reader)
        backStack.pushIfNeeded(AppRoute.SpeedRead)
        backStack.pushIfNeeded(AppRoute.SpeedReadPlayer)
        assertEquals(
            listOf(
                AppRoute.Library,
                AppRoute.Reader,
                AppRoute.SpeedRead,
                AppRoute.SpeedReadPlayer,
            ),
            backStack.toList(),
        )
    }

    @Test
    fun pushIfNeeded_doesNotDuplicateCurrentRoute() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Library)
        backStack.pushIfNeeded(AppRoute.Reader)
        backStack.pushIfNeeded(AppRoute.Reader)
        assertEquals(listOf(AppRoute.Library, AppRoute.Reader), backStack.toList())
    }

    @Test
    fun popBack_returnsThroughNestedScreensAndKeepsRoot() {
        val backStack = mutableListOf(
            AppRoute.Library,
            AppRoute.Reader,
            AppRoute.SpeedRead,
            AppRoute.SpeedReadPlayer,
        )

        assertTrue(backStack.popBack())
        assertEquals(
            listOf(AppRoute.Library, AppRoute.Reader, AppRoute.SpeedRead),
            backStack.toList(),
        )
        assertTrue(backStack.popBack())
        assertEquals(listOf(AppRoute.Library, AppRoute.Reader), backStack.toList())
        assertTrue(backStack.popBack())
        assertEquals(listOf(AppRoute.Library), backStack.toList())
        assertFalse(backStack.popBack())
        assertEquals(listOf(AppRoute.Library), backStack.toList())
    }

    @Test
    fun openReaderFromLibrary_startsFromHome() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Home)
        backStack.openReaderFromLibrary()
        assertEquals(listOf(AppRoute.Library, AppRoute.Reader), backStack.toList())
    }

    @Test
    fun openReaderFromLibrary_keepsLibraryUnderReader() {
        val backStack = mutableListOf(AppRoute.Library, AppRoute.Reader, AppRoute.SpeedRead)
        backStack.openReaderFromLibrary()
        assertEquals(listOf(AppRoute.Library, AppRoute.Reader), backStack.toList())
    }

    @Test
    fun externalOpen_showsLibraryThenReader() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Home)
        backStack.navigateToTopLevel(AppRoute.Library)
        assertEquals(listOf(AppRoute.Library), backStack.toList())
        backStack.openReaderFromLibrary()
        assertEquals(listOf(AppRoute.Library, AppRoute.Reader), backStack.toList())
    }

    @Test
    fun settingsLegalScreens_useScaffoldTopBarAndReturnToSettings() {
        assertTrue(AppRoute.PrivacyPolicy.showsScaffoldTopBar)
        assertTrue(AppRoute.Terms.showsScaffoldTopBar)
        assertFalse(AppRoute.Settings.showsScaffoldTopBar)

        val backStack = mutableListOf<AppRoute>(AppRoute.Settings)
        backStack.pushIfNeeded(AppRoute.PrivacyPolicy)
        assertEquals(listOf(AppRoute.Settings, AppRoute.PrivacyPolicy), backStack.toList())
        assertTrue(backStack.popBack())
        backStack.pushIfNeeded(AppRoute.Terms)
        assertEquals(listOf(AppRoute.Settings, AppRoute.Terms), backStack.toList())
        assertTrue(backStack.popBack())
        assertEquals(listOf(AppRoute.Settings), backStack.toList())
    }

    @Test
    fun bookEditor_isNestedWithoutScaffoldTopBar() {
        assertFalse(AppRoute.BookEditor.isTopLevel)
        assertFalse(AppRoute.BookEditor.showsScaffoldTopBar)
        assertNull(AppScreen.fromRoute(AppRoute.BookEditor))

        val backStack = mutableListOf<AppRoute>(AppRoute.Library)
        backStack.pushIfNeeded(AppRoute.BookEditor)
        assertEquals(listOf(AppRoute.Library, AppRoute.BookEditor), backStack.toList())
        assertTrue(backStack.popBack())
        assertEquals(listOf(AppRoute.Library), backStack.toList())
    }

    @Test
    fun bannerAd_isEligibleOnlyOnHomeAndLibrary() {
        assertTrue(AppRoute.Home.showsBannerAd)
        assertTrue(AppRoute.Library.showsBannerAd)
        assertFalse(AppRoute.Settings.showsBannerAd)
        assertFalse(AppRoute.Reader.showsBannerAd)
        assertFalse(AppRoute.SpeedRead.showsBannerAd)
        assertFalse(AppRoute.SpeedReadPlayer.showsBannerAd)
        assertFalse(AppRoute.PrivacyPolicy.showsBannerAd)
        assertFalse(AppRoute.Terms.showsBannerAd)
        assertFalse(AppRoute.BookEditor.showsBannerAd)
        assertFalse(AppRoute.QuickSpeedRead.showsBannerAd)
    }

    @Test
    fun bannerAd_staysEligibleWhenSwitchingHomeAndLibrary() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Home)
        assertTrue(backStack.last().showsBannerAd)
        assertTrue(backStack.last().isTopLevel)

        backStack.navigateToTopLevel(AppRoute.Library)
        assertEquals(listOf(AppRoute.Library), backStack.toList())
        assertTrue(backStack.last().showsBannerAd)
        assertTrue(backStack.last().isTopLevel)

        backStack.navigateToTopLevel(AppRoute.Home)
        assertEquals(listOf(AppRoute.Home), backStack.toList())
        assertTrue(backStack.last().showsBannerAd)
    }

    @Test
    fun bannerAd_hidesOnSettingsButBottomBarStays() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Home)
        backStack.navigateToTopLevel(AppRoute.Settings)
        assertEquals(listOf(AppRoute.Settings), backStack.toList())
        assertTrue(backStack.last().isTopLevel)
        assertFalse(backStack.last().showsBannerAd)
    }

    @Test
    fun bannerAd_hidesOnNestedScreensAndReturnsWithLibrary() {
        val backStack = mutableListOf<AppRoute>(AppRoute.Library)
        assertTrue(backStack.last().showsBannerAd)

        backStack.openReaderFromLibrary()
        assertEquals(listOf(AppRoute.Library, AppRoute.Reader), backStack.toList())
        assertFalse(backStack.last().showsBannerAd)
        assertFalse(backStack.last().isTopLevel)

        backStack.pushIfNeeded(AppRoute.SpeedRead)
        assertFalse(backStack.last().showsBannerAd)
        assertFalse(backStack.last().isTopLevel)

        assertTrue(backStack.popBack())
        assertTrue(backStack.popBack())
        assertEquals(listOf(AppRoute.Library), backStack.toList())
        assertTrue(backStack.last().showsBannerAd)
        assertTrue(backStack.last().isTopLevel)
    }

    @Test
    fun quickSpeedRead_isNestedWithoutScaffoldTopBar() {
        assertFalse(AppRoute.QuickSpeedRead.isTopLevel)
        assertFalse(AppRoute.QuickSpeedRead.showsScaffoldTopBar)
        assertNull(AppScreen.fromRoute(AppRoute.QuickSpeedRead))

        val backStack = mutableListOf<AppRoute>(AppRoute.Home)
        backStack.pushIfNeeded(AppRoute.QuickSpeedRead)
        backStack.pushIfNeeded(AppRoute.SpeedRead)
        backStack.pushIfNeeded(AppRoute.SpeedReadPlayer)
        assertEquals(
            listOf(
                AppRoute.Home,
                AppRoute.QuickSpeedRead,
                AppRoute.SpeedRead,
                AppRoute.SpeedReadPlayer,
            ),
            backStack.toList(),
        )
        assertTrue(backStack.popBack())
        assertTrue(backStack.popBack())
        assertTrue(backStack.popBack())
        assertEquals(listOf(AppRoute.Home), backStack.toList())
    }
}
