package com.tool.flashread.navigation

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
}
