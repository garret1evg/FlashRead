package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.core.locale.AppLanguage
import com.evgeniich.flashread.memoryAppLanguageRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageRepositoryTest {

    @Test
    fun loadDefaultsToSystem() {
        assertEquals(AppLanguage.System, memoryAppLanguageRepository().load())
    }

    @Test
    fun saveAndLoadPersistsExplicitLanguage() {
        val stored = arrayOf<AppLanguage>(AppLanguage.System)
        val repository = memoryAppLanguageRepository(stored)

        repository.save(AppLanguage.Language("uk"))

        assertEquals(AppLanguage.Language("uk"), stored[0])
        assertEquals(AppLanguage.Language("uk"), repository.load())
    }

    @Test
    fun saveSystemClearsExplicitOverride() {
        val stored = arrayOf<AppLanguage>(AppLanguage.Language("de"))
        val repository = memoryAppLanguageRepository(stored)

        repository.save(AppLanguage.System)

        assertEquals(AppLanguage.System, stored[0])
        assertEquals(AppLanguage.System, repository.load())
    }
}
