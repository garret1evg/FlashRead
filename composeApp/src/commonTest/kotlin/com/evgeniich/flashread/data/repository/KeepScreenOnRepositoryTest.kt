package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.memoryKeepScreenOnRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeepScreenOnRepositoryTest {

    @Test
    fun loadDefaultsToEnabled() {
        assertTrue(memoryKeepScreenOnRepository().load())
    }

    @Test
    fun saveAndLoadPersistsDisabled() {
        val stored = booleanArrayOf(true)
        val repository = memoryKeepScreenOnRepository(stored)

        repository.save(false)

        assertFalse(stored[0])
        assertFalse(repository.load())
    }

    @Test
    fun saveAndLoadPersistsEnabled() {
        val stored = booleanArrayOf(false)
        val repository = memoryKeepScreenOnRepository(stored)

        repository.save(true)

        assertTrue(stored[0])
        assertTrue(repository.load())
    }
}
