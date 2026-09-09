package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.memoryAppThemeRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeRepositoryTest {

    @Test
    fun loadDefaultsToSystem() {
        assertEquals(AppTheme.System, memoryAppThemeRepository().load())
    }

    @Test
    fun saveAndLoadPersistsExplicitTheme() {
        val stored = arrayOf(AppTheme.System)
        val repository = memoryAppThemeRepository(stored)

        repository.save(AppTheme.Sepia)

        assertEquals(AppTheme.Sepia, stored[0])
        assertEquals(AppTheme.Sepia, repository.load())
    }

    @Test
    fun saveSystemClearsExplicitOverride() {
        val stored = arrayOf(AppTheme.Dark)
        val repository = memoryAppThemeRepository(stored)

        repository.save(AppTheme.System)

        assertEquals(AppTheme.System, stored[0])
        assertEquals(AppTheme.System, repository.load())
    }
}
