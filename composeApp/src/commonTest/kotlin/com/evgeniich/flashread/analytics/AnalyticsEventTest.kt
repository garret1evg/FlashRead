package com.evgeniich.flashread.analytics

import com.evgeniich.flashread.core.importdoc.BookFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEventTest {

    @Test
    fun bookImportMapsNameAndParams() {
        val event = AnalyticsEvent.BookImport(
            format = AnalyticsEvent.BookImport.Format.Epub,
            result = AnalyticsEvent.BookImport.Result.Success,
            source = AnalyticsEvent.BookImport.Source.Picker,
        )
        assertEquals("book_import", event.name)
        assertEquals(
            mapOf(
                "format" to "epub",
                "result" to "success",
                "source" to "picker",
            ),
            event.params,
        )
    }

    @Test
    fun bookImportFormatFollowsBookFormat() {
        assertEquals(
            AnalyticsEvent.BookImport.Format.Text,
            AnalyticsEvent.BookImport.Format.from(BookFormat.Text),
        )
        assertEquals(
            AnalyticsEvent.BookImport.Format.Fb2,
            AnalyticsEvent.BookImport.Format.from(BookFormat.Fb2),
        )
        assertEquals(
            AnalyticsEvent.BookImport.Format.Epub,
            AnalyticsEvent.BookImport.Format.from(BookFormat.Epub),
        )
        assertEquals(
            AnalyticsEvent.BookImport.Format.Unknown,
            AnalyticsEvent.BookImport.Format.from(BookFormat.Unknown),
        )
        assertEquals("text", AnalyticsEvent.BookImport.Format.Text.value)
        assertEquals("fb2", AnalyticsEvent.BookImport.Format.Fb2.value)
        assertEquals("epub", AnalyticsEvent.BookImport.Format.Epub.value)
        assertEquals("unknown", AnalyticsEvent.BookImport.Format.Unknown.value)
    }

    @Test
    fun bookImportResultAndSourceValuesMatchCatalog() {
        assertEquals("success", AnalyticsEvent.BookImport.Result.Success.value)
        assertEquals("empty", AnalyticsEvent.BookImport.Result.Empty.value)
        assertEquals("unsupported", AnalyticsEvent.BookImport.Result.Unsupported.value)
        assertEquals("damaged", AnalyticsEvent.BookImport.Result.Damaged.value)
        assertEquals("unable_read", AnalyticsEvent.BookImport.Result.UnableRead.value)
        assertEquals("failed", AnalyticsEvent.BookImport.Result.Failed.value)
        assertEquals("picker", AnalyticsEvent.BookImport.Source.Picker.value)
        assertEquals("share", AnalyticsEvent.BookImport.Source.Share.value)
    }

    @Test
    fun bookCreateAndQuickSpeedReadHaveNoParams() {
        assertEquals("book_create", AnalyticsEvent.BookCreate.name)
        assertEquals(emptyMap(), AnalyticsEvent.BookCreate.params)
        assertEquals("quick_speed_read", AnalyticsEvent.QuickSpeedRead.name)
        assertEquals(emptyMap(), AnalyticsEvent.QuickSpeedRead.params)
    }

    @Test
    fun readerStartMapsSourceAndMaterial() {
        val event = AnalyticsEvent.ReaderStart(
            source = AnalyticsEvent.ReaderStart.Source.Library,
            material = AnalyticsEvent.ReaderStart.Material.Created,
        )
        assertEquals("reader_start", event.name)
        assertEquals(
            mapOf(
                "source" to "library",
                "material" to "created",
            ),
            event.params,
        )
        assertEquals("home", AnalyticsEvent.ReaderStart.Source.Home.value)
        assertEquals("share", AnalyticsEvent.ReaderStart.Source.Share.value)
        assertEquals("imported", AnalyticsEvent.ReaderStart.Material.Imported.value)
    }

    @Test
    fun readerProgressMapsBucket() {
        val event = AnalyticsEvent.ReaderProgress(ProgressBucket.P75)
        assertEquals("reader_progress", event.name)
        assertEquals(mapOf("progress_bucket" to "75"), event.params)
        assertEquals("25", ProgressBucket.P25.value)
        assertEquals("50", ProgressBucket.P50.value)
        assertEquals("100", ProgressBucket.P100.value)
    }

    @Test
    fun speedReadStartMapsPlaybackParams() {
        val event = AnalyticsEvent.SpeedReadStart(
            wpmBucket = WpmBucket.From401To600,
            spritzEnabled = true,
            source = AnalyticsEvent.SpeedReadStart.Source.Paste,
            chunkSize = 2,
        )
        assertEquals("speed_read_start", event.name)
        assertEquals(
            mapOf(
                "wpm_bucket" to "401_600",
                "spritz_enabled" to "true",
                "source" to "paste",
                "chunk_size" to "2",
            ),
            event.params,
        )
        assertEquals("book", AnalyticsEvent.SpeedReadStart.Source.Book.value)
        val pausedSpritz = AnalyticsEvent.SpeedReadStart(
            wpmBucket = WpmBucket.UpTo250,
            spritzEnabled = false,
            source = AnalyticsEvent.SpeedReadStart.Source.Book,
            chunkSize = 1,
        )
        assertEquals("false", pausedSpritz.params["spritz_enabled"])
    }

    @Test
    fun speedReadCompleteMapsDurationAndResult() {
        val event = AnalyticsEvent.SpeedReadComplete(
            durationBucket = DurationBucket.From2To5m,
            result = AnalyticsEvent.SpeedReadComplete.Result.Closed,
        )
        assertEquals("speed_read_complete", event.name)
        assertEquals(
            mapOf(
                "duration_bucket" to "2_5m",
                "result" to "closed",
            ),
            event.params,
        )
        assertEquals("finished", AnalyticsEvent.SpeedReadComplete.Result.Finished.value)
    }

    @Test
    fun settingsChangeMapsNameAndValue() {
        val event = AnalyticsEvent.SettingsChange(
            settingName = AnalyticsEvent.SettingsChange.SettingName.Wpm,
            settingValue = "400",
        )
        assertEquals("settings_change", event.name)
        assertEquals(
            mapOf(
                "setting_name" to "wpm",
                "setting_value" to "400",
            ),
            event.params,
        )
        assertEquals("language", AnalyticsEvent.SettingsChange.SettingName.Language.value)
        assertEquals("theme", AnalyticsEvent.SettingsChange.SettingName.Theme.value)
        assertEquals("font_size", AnalyticsEvent.SettingsChange.SettingName.FontSize.value)
        assertEquals("line_height", AnalyticsEvent.SettingsChange.SettingName.LineHeight.value)
        assertEquals("alignment", AnalyticsEvent.SettingsChange.SettingName.Alignment.value)
        assertEquals("chunk_size", AnalyticsEvent.SettingsChange.SettingName.ChunkSize.value)
        assertEquals("spritz_enabled", AnalyticsEvent.SettingsChange.SettingName.SpritzEnabled.value)
        assertEquals("loop_enabled", AnalyticsEvent.SettingsChange.SettingName.LoopEnabled.value)
    }

    @Test
    fun everyEventExposesNonBlankName() {
        val events = listOf(
            AnalyticsEvent.BookImport(
                AnalyticsEvent.BookImport.Format.Text,
                AnalyticsEvent.BookImport.Result.Failed,
                AnalyticsEvent.BookImport.Source.Share,
            ),
            AnalyticsEvent.BookCreate,
            AnalyticsEvent.QuickSpeedRead,
            AnalyticsEvent.ReaderStart(
                AnalyticsEvent.ReaderStart.Source.Home,
                AnalyticsEvent.ReaderStart.Material.Imported,
            ),
            AnalyticsEvent.ReaderProgress(ProgressBucket.P25),
            AnalyticsEvent.SpeedReadStart(
                WpmBucket.UpTo250,
                spritzEnabled = false,
                source = AnalyticsEvent.SpeedReadStart.Source.Book,
                chunkSize = 1,
            ),
            AnalyticsEvent.SpeedReadComplete(
                DurationBucket.UpTo30s,
                AnalyticsEvent.SpeedReadComplete.Result.Finished,
            ),
            AnalyticsEvent.SettingsChange(
                AnalyticsEvent.SettingsChange.SettingName.Language,
                "en",
            ),
        )
        events.forEach { event ->
            assertTrue(event.name.isNotBlank(), "blank name for ${event::class.simpleName}")
        }
    }
}
