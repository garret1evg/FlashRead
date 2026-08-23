package com.tool.flashread.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun privacyPolicy_mentionsBooksYouWrite() {
        val text = LegalDocuments.privacyPolicy.sections.joinToString(" ") { it.body }
        assertTrue(text.contains("books you write", ignoreCase = true))
    }

    @Test
    fun termsAndConditions_mentionsBooksYouWrite() {
        val text = LegalDocuments.termsAndConditions.sections.joinToString(" ") { it.body }
        assertTrue(text.contains("write your own books", ignoreCase = true))
        assertTrue(text.contains("books you write", ignoreCase = true))
    }

    @Test
    fun privacyPolicy_mentionsYouTubeCaptionDownload() {
        val text = LegalDocuments.privacyPolicy.sections.joinToString(" ") { it.body }
        assertTrue(text.contains("YouTube", ignoreCase = true))
        assertTrue(text.contains("captions", ignoreCase = true))
        assertTrue(text.contains("transcript", ignoreCase = true))
        assertTrue(text.contains("contacts YouTube", ignoreCase = true))
        assertFalse(text.contains("stores the title and URL you provide", ignoreCase = true))
    }

    @Test
    fun termsAndConditions_mentionsYouTubeCaptionDownload() {
        val text = LegalDocuments.termsAndConditions.sections.joinToString(" ") { it.body }
        assertTrue(text.contains("YouTube", ignoreCase = true))
        assertTrue(text.contains("captions", ignoreCase = true))
        assertTrue(text.contains("transcript", ignoreCase = true))
    }
}
