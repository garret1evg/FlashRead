package com.tool.flashread.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegalDocumentsTest {

    @Test
    fun privacyPolicy_hasTitleAndSections() {
        val document = LegalDocuments.privacyPolicy
        assertEquals("Privacy Policy", document.title)
        assertTrue(document.lastUpdated.isNotBlank())
        assertTrue(document.sections.size >= 5)
        assertTrue(document.sections.all { it.heading.isNotBlank() && it.body.isNotBlank() })
    }

    @Test
    fun termsAndConditions_hasTitleAndSections() {
        val document = LegalDocuments.termsAndConditions
        assertEquals("Terms & Conditions", document.title)
        assertTrue(document.lastUpdated.isNotBlank())
        assertTrue(document.sections.size >= 5)
        assertTrue(document.sections.all { it.heading.isNotBlank() && it.body.isNotBlank() })
    }
}
