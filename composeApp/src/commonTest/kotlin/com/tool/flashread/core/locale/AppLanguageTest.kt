package com.tool.flashread.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppLanguageTest {

    @Test
    fun languageSubtagUsesPrimaryLanguageOnly() {
        assertEquals("pt", languageSubtag("pt-BR"))
        assertEquals("en", languageSubtag("en-US"))
        assertEquals("zh", languageSubtag("zh-Hans"))
        assertEquals("ar", languageSubtag("ar_SA"))
        assertEquals("fr", languageSubtag(" FR "))
        assertEquals("", languageSubtag("  "))
    }

    @Test
    fun systemUsesSupportedOsLanguage() {
        AppLanguage.SUPPORTED_CODES.forEach { code ->
            assertNull(resolveLocaleOverride(AppLanguage.System, code))
            assertNull(resolveLocaleOverride(AppLanguage.System, "$code-XX"))
        }
        assertNull(resolveLocaleOverride(AppLanguage.System, "pt-BR"))
        assertNull(resolveLocaleOverride(AppLanguage.System, "en-US"))
        assertNull(resolveLocaleOverride(AppLanguage.System, "uk"))
        assertNull(resolveLocaleOverride(AppLanguage.System, "ar_EG"))
    }

    @Test
    fun systemFallsBackToEnglishWhenOsLanguageUnsupported() {
        assertEquals("en", resolveLocaleOverride(AppLanguage.System, "zh-Hans"))
        assertEquals("en", resolveLocaleOverride(AppLanguage.System, "ja-JP"))
        assertEquals("en", resolveLocaleOverride(AppLanguage.System, "it"))
        assertEquals("en", resolveLocaleOverride(AppLanguage.System, ""))
        assertEquals("en", resolveLocaleOverride(AppLanguage.System, "  "))
    }

    @Test
    fun explicitOverrideWinsOverSystemLanguage() {
        val french = AppLanguage.language("fr")!!
        assertEquals("fr", resolveLocaleOverride(french, "de-DE"))
        assertEquals("fr", resolveLocaleOverride(french, "ja"))

        val russian = AppLanguage.language("ru")!!
        assertEquals("ru", resolveLocaleOverride(russian, "en-US"))

        val portuguese = AppLanguage.language("pt-BR")!!
        assertEquals("pt", resolveLocaleOverride(portuguese, "zh-Hans"))
    }

    @Test
    fun arabicOverrideIsRtlAndSystemStaysLtr() {
        assertTrue(isRtlLocaleOverride("ar"))
        assertTrue(isRtlLocaleOverride("ar-EG"))
        assertTrue(
            isRtlLocaleOverride(resolveLocaleOverride(AppLanguage.language("ar")!!, "en-US")),
        )
        assertFalse(isRtlLocaleOverride(null))
        assertFalse(isRtlLocaleOverride("en"))
        assertFalse(isRtlLocaleOverride(resolveLocaleOverride(AppLanguage.System, "ar")))
        assertFalse(isRtlLocaleOverride(resolveLocaleOverride(AppLanguage.System, "zh")))
    }

    @Test
    fun languageRejectsUnsupportedCodesAndNormalizesSupportedOnes() {
        assertNull(AppLanguage.language("zh"))
        assertNull(AppLanguage.language("ja-JP"))
        assertNull(AppLanguage.language("  "))
        assertEquals(AppLanguage.Language("pt"), AppLanguage.language("pt-BR"))
        assertEquals(AppLanguage.Language("ar"), AppLanguage.language("AR"))
        assertEquals(AppLanguage.Language("en"), AppLanguage.language("en-US"))
    }

    @Test
    fun storageRoundTripPreservesSystemAndSupportedCodes() {
        assertEquals(AppLanguage.System, AppLanguage.fromStorage(null))
        assertEquals(AppLanguage.System, AppLanguage.fromStorage("system"))
        assertEquals(AppLanguage.System, AppLanguage.fromStorage("SYSTEM"))
        assertEquals(AppLanguage.System, AppLanguage.fromStorage("  "))
        assertEquals(AppLanguage.Language("es"), AppLanguage.fromStorage("es"))
        assertEquals(AppLanguage.Language("uk"), AppLanguage.fromStorage("uk-UA"))
        assertEquals(AppLanguage.System, AppLanguage.fromStorage("zh"))
        assertEquals(AppLanguage.STORAGE_SYSTEM, AppLanguage.System.toStorage())
        assertEquals("pt", AppLanguage.language("pt-BR")!!.toStorage())
    }
}
