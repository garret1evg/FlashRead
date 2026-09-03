package com.evgeniich.flashread.analytics

import com.evgeniich.flashread.core.importdoc.BookFormat

sealed class AnalyticsEvent {
    abstract val name: String
    abstract val params: Map<String, String>

    data class BookImport(
        val format: Format,
        val result: Result,
        val source: Source,
    ) : AnalyticsEvent() {
        override val name = NAME
        override val params = mapOf(
            PARAM_FORMAT to format.value,
            PARAM_RESULT to result.value,
            PARAM_SOURCE to source.value,
        )

        enum class Format(val value: String) {
            Text("text"),
            Fb2("fb2"),
            Epub("epub"),
            Unknown("unknown"),
            ;

            companion object {
                fun from(format: BookFormat): Format = when (format) {
                    BookFormat.Text -> Text
                    BookFormat.Fb2 -> Fb2
                    BookFormat.Epub -> Epub
                    BookFormat.Unknown -> Unknown
                }
            }
        }

        enum class Result(val value: String) {
            Success("success"),
            Empty("empty"),
            Unsupported("unsupported"),
            Damaged("damaged"),
            UnableRead("unable_read"),
            Failed("failed"),
        }

        enum class Source(val value: String) {
            Picker("picker"),
            Share("share"),
        }

        companion object {
            const val NAME = "book_import"
            const val PARAM_FORMAT = "format"
            const val PARAM_RESULT = "result"
            const val PARAM_SOURCE = "source"
        }
    }

    data object BookCreate : AnalyticsEvent() {
        override val name = NAME
        override val params: Map<String, String> = emptyMap()

        const val NAME = "book_create"
    }

    data object QuickSpeedRead : AnalyticsEvent() {
        override val name = NAME
        override val params: Map<String, String> = emptyMap()

        const val NAME = "quick_speed_read"
    }

    data class ReaderStart(
        val source: Source,
        val material: Material,
    ) : AnalyticsEvent() {
        override val name = NAME
        override val params = mapOf(
            PARAM_SOURCE to source.value,
            PARAM_MATERIAL to material.value,
        )

        enum class Source(val value: String) {
            Home("home"),
            Library("library"),
            Share("share"),
        }

        enum class Material(val value: String) {
            Imported("imported"),
            Created("created"),
        }

        companion object {
            const val NAME = "reader_start"
            const val PARAM_SOURCE = "source"
            const val PARAM_MATERIAL = "material"
        }
    }

    data class ReaderProgress(
        val bucket: ProgressBucket,
    ) : AnalyticsEvent() {
        override val name = NAME
        override val params = mapOf(PARAM_PROGRESS_BUCKET to bucket.value)

        companion object {
            const val NAME = "reader_progress"
            const val PARAM_PROGRESS_BUCKET = "progress_bucket"
        }
    }

    data class SpeedReadStart(
        val wpmBucket: WpmBucket,
        val spritzEnabled: Boolean,
        val source: Source,
        val chunkSize: Int,
    ) : AnalyticsEvent() {
        override val name = NAME
        override val params = mapOf(
            PARAM_WPM_BUCKET to wpmBucket.value,
            PARAM_SPRITZ_ENABLED to spritzEnabled.toString(),
            PARAM_SOURCE to source.value,
            PARAM_CHUNK_SIZE to chunkSize.toString(),
        )

        enum class Source(val value: String) {
            Book("book"),
            Paste("paste"),
        }

        companion object {
            const val NAME = "speed_read_start"
            const val PARAM_WPM_BUCKET = "wpm_bucket"
            const val PARAM_SPRITZ_ENABLED = "spritz_enabled"
            const val PARAM_SOURCE = "source"
            const val PARAM_CHUNK_SIZE = "chunk_size"
        }
    }

    data class SpeedReadComplete(
        val durationBucket: DurationBucket,
        val result: Result,
    ) : AnalyticsEvent() {
        override val name = NAME
        override val params = mapOf(
            PARAM_DURATION_BUCKET to durationBucket.value,
            PARAM_RESULT to result.value,
        )

        enum class Result(val value: String) {
            Finished("finished"),
            Closed("closed"),
        }

        companion object {
            const val NAME = "speed_read_complete"
            const val PARAM_DURATION_BUCKET = "duration_bucket"
            const val PARAM_RESULT = "result"
        }
    }

    data class SettingsChange(
        val settingName: SettingName,
        val settingValue: String,
    ) : AnalyticsEvent() {
        override val name = NAME
        override val params = mapOf(
            PARAM_SETTING_NAME to settingName.value,
            PARAM_SETTING_VALUE to settingValue,
        )

        enum class SettingName(val value: String) {
            Language("language"),
            Theme("theme"),
            FontSize("font_size"),
            LineHeight("line_height"),
            Alignment("alignment"),
            Wpm("wpm"),
            ChunkSize("chunk_size"),
            SpritzEnabled("spritz_enabled"),
            LoopEnabled("loop_enabled"),
        }

        companion object {
            const val NAME = "settings_change"
            const val PARAM_SETTING_NAME = "setting_name"
            const val PARAM_SETTING_VALUE = "setting_value"
        }
    }
}
