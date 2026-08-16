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

    @Test
    fun partsKeepLongWordPivotAtClassicIndex() {
        val parts = orpParts("supercalifragilistic")
        assertEquals(4, parts.pivotIndex)
        assertEquals("supe", parts.before)
        assertEquals("r", parts.pivot)
        assertEquals("califragilistic", parts.after)
        assertEquals("supercalifragilistic", parts.before + parts.pivot + parts.after)
    }

    @Test
    fun partsSplitMultiWordGroupAroundPivot() {
        val text = "one two three"
        val parts = orpParts(text)
        assertEquals(4, parts.pivotIndex)
        assertEquals("one ", parts.before)
        assertEquals("t", parts.pivot)
        assertEquals("wo three", parts.after)
        assertEquals(text, parts.before + parts.pivot + parts.after)
    }

    @Test
    fun partsWithoutSpritzCenterTheWholeText() {
        val parts = orpParts("hello", spritzEnabled = false)
        assertNull(parts.pivotIndex)
        assertEquals("", parts.before)
        assertEquals("", parts.pivot)
        assertEquals("hello", parts.after)
    }
}
