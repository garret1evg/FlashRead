package com.evgeniich.flashread.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeTest {

    @Test
    fun storageRoundTripPreservesAllValues() {
        AppTheme.entries.forEach { theme ->
            assertEquals(theme, AppTheme.fromStorage(theme.toStorage()))
            assertEquals(theme, AppTheme.fromStorage(theme.name))
            assertEquals(theme, AppTheme.fromStorage(theme.name.uppercase()))
        }
        assertEquals(AppTheme.System, AppTheme.fromStorage(null))
        assertEquals(AppTheme.System, AppTheme.fromStorage("  "))
        assertEquals(AppTheme.System, AppTheme.fromStorage("unknown"))
        assertEquals(AppTheme.Light, AppTheme.fromStorage("Light"))
        assertEquals("system", AppTheme.System.toStorage())
        assertEquals("sepia", AppTheme.Sepia.toStorage())
    }

    @Test
    fun systemFollowsOsDarkModeAndExplicitThemesStayFixed() {
        assertEquals(AppTheme.Light, AppTheme.System.resolve(systemDark = false))
        assertEquals(AppTheme.Dark, AppTheme.System.resolve(systemDark = true))
        assertEquals(AppTheme.Light, AppTheme.Light.resolve(systemDark = true))
        assertEquals(AppTheme.Sepia, AppTheme.Sepia.resolve(systemDark = true))
        assertEquals(AppTheme.Dark, AppTheme.Dark.resolve(systemDark = false))
    }
}
