package com.tool.flashread.ui.speedread

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.core.reading.estimatedRemainingMinutes
import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
import com.tool.flashread.ui.library.MaterialTitleFormatter
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadShapes
import com.tool.flashread.ui.theme.FlashReadTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadSetupScreen(
    book: Book?,
    settingsRepository: SpeedReadSettingsRepository,
    onContinue: () -> Unit,
    remainingWords: Int,
    modifier: Modifier = Modifier,
) {
    if (book == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Сначала выберите материал в библиотеке.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }

    var settings by remember { mutableStateOf(settingsRepository.load()) }

    fun updateSettings(updated: SpeedReadSettings) {
        val normalized = updated.normalized()
        settings = normalized
        settingsRepository.save(normalized)
    }

    val remainingMinutes = remember(remainingWords, settings.wpm) {
        estimatedRemainingMinutes(remainingWords, settings.wpm)
    }

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
                    contentDescription = "${settings.wpm} слов в минуту"
                },
            )
            Text(
                text = "слов в минуту",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            Text(
                text = formatRemainingTime(remainingMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics {
                    contentDescription = formatRemainingTime(remainingMinutes)
                },
            )

            Spacer(Modifier.height(FlashReadDimens.space16))
            Slider(
                value = settings.wpm.toFloat(),
                onValueChange = { value ->
                    updateSettings(settings.copy(wpm = SpeedReadDefaults.snapWpm(value.roundToInt())))
                },
                valueRange = SpeedReadDefaults.MIN_WPM.toFloat()..SpeedReadDefaults.MAX_WPM.toFloat(),
                steps = SpeedReadDefaults.WPM_SLIDER_STEPS,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FlashReadDimens.minTouchTarget)
                    .semantics { contentDescription = "Скорость чтения, слов в минуту" },
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
                    FilterChip(
                        selected = settings.wpm == preset,
                        onClick = { updateSettings(settings.copy(wpm = preset)) },
                        label = { Text("$preset") },
                        modifier = Modifier
                            .heightIn(min = FlashReadDimens.minTouchTarget)
                            .semantics { contentDescription = "Пресет $preset слов в минуту" },
                    )
                }
            }

            Spacer(Modifier.height(FlashReadDimens.space24))
            Text(
                text = "Слов за показ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SpeedReadDefaults.CHUNK_SIZES.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = settings.chunkSize == size,
                        onClick = { updateSettings(settings.copy(chunkSize = size)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SpeedReadDefaults.CHUNK_SIZES.size,
                        ),
                        modifier = Modifier
                            .heightIn(min = FlashReadDimens.minTouchTarget)
                            .semantics {
                                contentDescription = chunkSizeContentDescription(size)
                            },
                    ) {
                        Text(text = size.toString())
                    }
                }
            }

            Spacer(Modifier.height(FlashReadDimens.space16))
            SettingsSwitchRow(
                title = "Spritz",
                subtitle = "Подсвечивать оптимальную точку распознавания в слове",
                checked = settings.spritzEnabled,
                onCheckedChange = { updateSettings(settings.copy(spritzEnabled = it)) },
            )
            SettingsSwitchRow(
                title = "Повтор",
                subtitle = "Непрерывно повторять текст с начала после окончания",
                checked = settings.loopEnabled,
                onCheckedChange = { updateSettings(settings.copy(loopEnabled = it)) },
            )
            Spacer(Modifier.height(FlashReadDimens.space16))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Button(
            onClick = {
                settingsRepository.save(settings)
                onContinue()
            },
            enabled = book.content.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(top = FlashReadDimens.space12, bottom = FlashReadDimens.space16)
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = "Начать скорочтение" },
            shape = FlashReadShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = "Начать скорочтение",
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
    val secondary = remember(book) { materialSecondaryLabel(book) }

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
                MaterialTypeIcon(sourceType = book.sourceType)
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
private fun MaterialTypeIcon(
    sourceType: MaterialSourceType,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = when (sourceType) {
        MaterialSourceType.Book -> Icons.AutoMirrored.Filled.MenuBook
        MaterialSourceType.YouTube -> Icons.Filled.PlayCircle
    }
    Box(
        modifier = modifier
            .size(FlashReadDimens.typeIconSize)
            .clip(RoundedCornerShape(FlashReadDimens.space12))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (sourceType) {
                MaterialSourceType.Book -> "Книга"
                MaterialSourceType.YouTube -> "YouTube-видео"
            },
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
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
            .semantics { contentDescription = "$title. $subtitle" },
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

private fun materialSecondaryLabel(book: Book): String {
    return when (book.sourceType) {
        MaterialSourceType.YouTube -> "YouTube-видео"
        MaterialSourceType.Book -> formatWordCount(book.wordCount)
    }
}

private fun formatWordCount(count: Int): String {
    val n = count % 100
    val n1 = count % 10
    val word = when {
        n in 11..14 -> "слов"
        n1 == 1 -> "слово"
        n1 in 2..4 -> "слова"
        else -> "слов"
    }
    return "$count $word"
}

internal fun formatRemainingTime(minutes: Int): String {
    return when {
        minutes <= 0 -> "Осталось меньше минуты"
        minutes < 60 -> "Осталось около $minutes мин"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0) {
                "Осталось около $hours ч"
            } else {
                "Осталось около $hours ч $mins мин"
            }
        }
    }
}

private fun chunkSizeContentDescription(chunkSize: Int): String {
    return if (chunkSize == 1) {
        "1 слово за показ"
    } else {
        "$chunkSize слова за показ"
    }
}

@Preview(name = "Setup 320", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Setup 390", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SpeedReadSetupScreenPreview() {
    FlashReadTheme {
        SpeedReadSetupScreen(
            book = Book(
                id = "preview",
                title = "Sample book about subvocalization and speed reading.txt",
                content = "subvocalization (pronouncing words in your head) is one of the things " +
                    "that can keep your reading speed down. Speed reading trains you to take in " +
                    "words visually without sounding them out.",
            ),
            settingsRepository = SpeedReadSettingsRepository(),
            remainingWords = 2400,
            onContinue = {},
        )
    }
}
