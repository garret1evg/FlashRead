package com.evgeniich.flashread.ui.speedread

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.speedread.SpeedReadDefaults
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.library.MaterialArtwork
import com.evgeniich.flashread.ui.library.MaterialTitleFormatter
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadSetupScreen(
    book: Book,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeedReadSetupViewModel = viewModel(key = book.id) {
        SpeedReadSetupViewModel(book)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val remainingMinutes = uiState.remainingMinutes
    val remainingTimeLabel = remainingTimeLabel(remainingMinutes)
    val wpmValueCd = stringResource(Res.string.wpm_value_cd, settings.wpm)
    val wpmSliderCd = stringResource(Res.string.wpm_slider_cd)
    val startLabel = stringResource(Res.string.start_speed_read)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
        ) {
            Spacer(Modifier.height(FlashReadDimens.space8))
            MaterialSummaryCard(book = book)
            Spacer(Modifier.height(FlashReadDimens.space24))

            Text(
                text = settings.wpm.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 56.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = wpmValueCd
                },
            )
            Text(
                text = stringResource(Res.string.wpm_label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            Text(
                text = remainingTimeLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics {
                    contentDescription = remainingTimeLabel
                },
            )

            Spacer(Modifier.height(FlashReadDimens.space16))
            Slider(
                value = settings.wpm.toFloat(),
                onValueChange = { value ->
                    viewModel.updateSettings(settings.copy(wpm = SpeedReadDefaults.snapWpm(value.roundToInt())))
                },
                valueRange = SpeedReadDefaults.MIN_WPM.toFloat()..SpeedReadDefaults.MAX_WPM.toFloat(),
                steps = SpeedReadDefaults.WPM_SLIDER_STEPS,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FlashReadDimens.minTouchTarget)
                    .semantics { contentDescription = wpmSliderCd },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${SpeedReadDefaults.MIN_WPM}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${SpeedReadDefaults.MAX_WPM}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(FlashReadDimens.space12))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
                verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
            ) {
                SpeedReadDefaults.WPM_PRESETS.forEach { preset ->
                    val presetCd = stringResource(Res.string.wpm_preset_cd, preset)
                    FilterChip(
                        selected = settings.wpm == preset,
                        onClick = { viewModel.updateSettings(settings.copy(wpm = preset)) },
                        label = { Text("$preset") },
                        modifier = Modifier
                            .heightIn(min = FlashReadDimens.minTouchTarget)
                            .semantics { contentDescription = presetCd },
                    )
                }
            }

            Spacer(Modifier.height(FlashReadDimens.space24))
            Text(
                text = stringResource(Res.string.words_per_flash),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SpeedReadDefaults.CHUNK_SIZES.forEachIndexed { index, size ->
                    val chunkCd = pluralStringResource(
                        Res.plurals.words_per_flash_cd,
                        size,
                        size,
                    )
                    SegmentedButton(
                        selected = settings.chunkSize == size,
                        onClick = { viewModel.updateSettings(settings.copy(chunkSize = size)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SpeedReadDefaults.CHUNK_SIZES.size,
                        ),
                        modifier = Modifier
                            .heightIn(min = FlashReadDimens.minTouchTarget)
                            .semantics {
                                contentDescription = chunkCd
                            },
                    ) {
                        Text(text = size.toString())
                    }
                }
            }

            Spacer(Modifier.height(FlashReadDimens.space16))
            SettingsSwitchRow(
                title = stringResource(Res.string.spritz),
                subtitle = stringResource(Res.string.spritz_subtitle_setup),
                checked = settings.spritzEnabled,
                onCheckedChange = { viewModel.updateSettings(settings.copy(spritzEnabled = it)) },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.loop),
                subtitle = stringResource(Res.string.loop_subtitle_setup),
                checked = settings.loopEnabled,
                onCheckedChange = { viewModel.updateSettings(settings.copy(loopEnabled = it)) },
            )
            Spacer(Modifier.height(FlashReadDimens.space16))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Button(
            onClick = {
                viewModel.persistSettings()
                onContinue()
            },
            enabled = uiState.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(top = FlashReadDimens.space12, bottom = FlashReadDimens.space16)
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = startLabel },
            shape = FlashReadShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = startLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MaterialSummaryCard(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val displayTitle = remember(book.title) { MaterialTitleFormatter.displayTitle(book.title) }
    val preview = remember(book.content) { previewBookContent(book.content) }
    val secondary = materialSecondaryLabel(book)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FlashReadShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlashReadDimens.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MaterialArtwork(coverFileName = book.coverFileName)
                Spacer(Modifier.width(FlashReadDimens.space12))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space4))
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(FlashReadDimens.space12))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val switchCd = "$title. $subtitle"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .padding(vertical = FlashReadDimens.space12)
            .semantics { contentDescription = switchCd },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(FlashReadDimens.space4))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(FlashReadDimens.space12))
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

private const val BOOK_PREVIEW_MAX_CHARS = 280

private fun previewBookContent(content: String): String {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.length <= BOOK_PREVIEW_MAX_CHARS) return trimmed
    return trimmed.take(BOOK_PREVIEW_MAX_CHARS).trimEnd() + "…"
}

@Composable
private fun materialSecondaryLabel(book: Book): String {
    return pluralStringResource(
        Res.plurals.word_count,
        book.wordCount,
        book.wordCount,
    )
}

@Composable
internal fun remainingTimeLabel(minutes: Int?): String {
    if (minutes == null) return stringResource(Res.string.remaining_time_unknown)
    return when {
        minutes <= 0 -> stringResource(Res.string.remaining_time_less_than_minute)
        minutes < 60 -> pluralStringResource(Res.plurals.remaining_time_minutes, minutes, minutes)
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0) {
                pluralStringResource(Res.plurals.remaining_time_hours, hours, hours)
            } else {
                stringResource(Res.string.remaining_time_hours_minutes, hours, mins)
            }
        }
    }
}

@Preview(name = "Setup 320", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Setup 390", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SpeedReadSetupScreenPreview() {
    val book = Book(
        id = "preview",
        title = "Sample book about subvocalization and speed reading.txt",
        content = "subvocalization (pronouncing words in your head) is one of the things " +
            "that can keep your reading speed down. Speed reading trains you to take in " +
            "words visually without sounding them out.",
    )
    FlashReadTheme {
        SpeedReadSetupScreen(
            book = book,
            onContinue = {},
            viewModel = remember { SpeedReadSetupViewModel(book) },
        )
    }
}
