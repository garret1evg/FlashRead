package com.tool.flashread.ui.speedread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
import kotlin.math.roundToInt

@Composable
fun SpeedReadSetupScreen(
    book: Book?,
    settingsRepository: SpeedReadSettingsRepository,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (book == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pick a book in Library first.")
        }
        return
    }

    var settings by remember { mutableStateOf(settingsRepository.load()) }

    fun updateSettings(updated: SpeedReadSettings) {
        val normalized = updated.normalized()
        settings = normalized
        settingsRepository.save(normalized)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        BookPreviewBox(
            title = book.title,
            content = book.content,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "${settings.wpm} words per minute",
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = settings.wpm.toFloat(),
            onValueChange = { updateSettings(settings.copy(wpm = it.roundToInt())) },
            valueRange = SpeedReadDefaults.MIN_WPM.toFloat()..SpeedReadDefaults.MAX_WPM.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = chunkSizeLabel(settings.chunkSize),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = settings.chunkSize.toFloat(),
            onValueChange = { updateSettings(settings.copy(chunkSize = it.roundToInt())) },
            valueRange = SpeedReadDefaults.MIN_CHUNK_SIZE.toFloat()..SpeedReadDefaults.MAX_CHUNK_SIZE.toFloat(),
            steps = SpeedReadDefaults.MAX_CHUNK_SIZE - SpeedReadDefaults.MIN_CHUNK_SIZE - 1,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        SettingsCheckboxRow(
            label = "Spritz",
            checked = settings.spritzEnabled,
            onCheckedChange = { updateSettings(settings.copy(spritzEnabled = it)) },
        )
        SettingsCheckboxRow(
            label = "Continuously repeat text",
            checked = settings.loopEnabled,
            onCheckedChange = { updateSettings(settings.copy(loopEnabled = it)) },
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                settingsRepository.save(settings)
                onContinue()
            },
            enabled = book.content.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text("CONTINUE TO SPEED READ")
        }
    }
}

@Composable
private fun BookPreviewBox(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp),
            ),
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 12.dp)
                .offset(y = (-8).dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun SettingsCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun chunkSizeLabel(chunkSize: Int): String {
    return if (chunkSize == 1) {
        "1 word at a time"
    } else {
        "$chunkSize words at a time"
    }
}

@Preview
@Composable
private fun SpeedReadSetupScreenPreview() {
    MaterialTheme {
        SpeedReadSetupScreen(
            book = Book(
                id = "preview",
                title = "Sample book",
                content = "subvocalization (pronouncing words in your head) is one of the things " +
                    "that can keep your reading speed down. Speed reading trains you to take in " +
                    "words visually without sounding them out.",
            ),
            settingsRepository = SpeedReadSettingsRepository(),
            onContinue = {},
        )
    }
}
