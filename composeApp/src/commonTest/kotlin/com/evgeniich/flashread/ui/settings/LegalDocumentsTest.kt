package com.evgeniich.flashread.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegalDocumentsTest {

    @Test
    fun privacyPolicy_hasTitleAndSections() {
        val document = LegalDocuments.privacyPolicy
        assertEquals("Privacy Policy", document.title)
        assertEquals("August 31, 2026", document.lastUpdated)
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
    fun privacyPolicy_describesFirebaseAnalyticsAndAdvertisingId() {
        val document = LegalDocuments.privacyPolicy
        val analytics = document.sections.single { it.heading == "Analytics" }
        assertTrue(analytics.body.contains("Firebase Analytics"))
        assertTrue(analytics.body.contains("usage events", ignoreCase = true))
        assertTrue(analytics.body.contains("do not include the titles or text of your books"))
        assertTrue(analytics.body.contains("Advertising ID"))
        val text = document.sections.joinToString(" ") { it.body }
        assertFalse(text.contains("does not collect analytics", ignoreCase = true))
        assertFalse(text.contains("does not collect advertising identifiers", ignoreCase = true))
    }

    @Test
    fun privacyPolicy_doesNotCollectAccountCrashReportsOrLocation() {
        val text = LegalDocuments.privacyPolicy.sections.joinToString(" ") { it.body }
        assertTrue(text.contains("does not create an account", ignoreCase = true))
        assertTrue(text.contains("does not collect crash reports", ignoreCase = true))
        assertTrue(text.contains("location data", ignoreCase = true))
        assertTrue(text.contains("does not upload your imported books", ignoreCase = true))
    }

    @Test
    fun privacyPolicy_doesNotMentionYouTube() {
        val text = LegalDocuments.privacyPolicy.sections.joinToString(" ") { it.body }
        assertFalse(text.contains("YouTube", ignoreCase = true))
        assertFalse(text.contains("transcript", ignoreCase = true))
        assertTrue(text.contains("on your device", ignoreCase = true))
    }

    @Test
    fun termsAndConditions_doesNotMentionYouTube() {
        val text = LegalDocuments.termsAndConditions.sections.joinToString(" ") { it.body }
        assertFalse(text.contains("YouTube", ignoreCase = true))
        assertFalse(text.contains("transcript", ignoreCase = true))
        assertFalse(text.contains("captions", ignoreCase = true))
    }
}
