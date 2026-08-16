package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrpTest {

    @Test
    fun indexIsZeroForSingleCharacter() {
        assertEquals(0, orpIndex("I"))
        assertEquals(0, orpIndex("a"))
    }

    @Test
    fun indexIsOneForLengthTwoToFive() {
        assertEquals(1, orpIndex("ab"))
        assertEquals(1, orpIndex("cat"))
        assertEquals(1, orpIndex("hello"))
    }

    @Test
    fun indexIsTwoForLengthSixToNine() {
        assertEquals(2, orpIndex("planet"))
        assertEquals(2, orpIndex("something"))
    }

    @Test
    fun indexIsThreeForLengthTenToThirteen() {
        assertEquals(3, orpIndex("abstracter"))
        assertEquals(3, orpIndex("international"))
    }

    @Test
    fun indexIsFourForLengthFourteenAndUp() {
        assertEquals(4, orpIndex("abstractionism"))
        assertEquals(4, orpIndex("supercalifragilistic"))
    }

    @Test
    fun emptyTextReturnsZero() {
        assertEquals(0, orpIndex(""))
    }

    @Test
    fun spritzDisabledReturnsNull() {
        assertNull(orpIndex("hello", spritzEnabled = false))
        assertNull(orpIndex("a", spritzEnabled = false))
    }
}
