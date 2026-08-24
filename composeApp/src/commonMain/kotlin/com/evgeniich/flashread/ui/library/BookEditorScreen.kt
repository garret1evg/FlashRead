package com.evgeniich.flashread.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookEditorScreen(
    initialTitle: String,
    initialContent: String,
    onBack: () -> Unit,
    onSave: (title: String, content: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    val canSave = content.isNotBlank()
    val defaultTitle = stringResource(Res.string.default_new_book_title)
    val backLabel = stringResource(Res.string.action_back)
    val saveLabel = stringResource(Res.string.action_save)
    val barTitle = title.trim().ifBlank { defaultTitle }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = barTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(FlashReadDimens.minTouchTarget)
                        .semantics { contentDescription = backLabel },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backLabel,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { onSave(title, content) },
                    enabled = canSave,
                    modifier = Modifier
                        .heightIn(min = FlashReadDimens.minTouchTarget)
                        .semantics { contentDescription = saveLabel },
                ) {
                    Text(
                        text = saveLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(top = FlashReadDimens.space8, bottom = FlashReadDimens.space16),
            verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space16),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.editor_title_label)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text(stringResource(Res.string.editor_text_label)) },
                placeholder = { Text(stringResource(Res.string.editor_text_placeholder)) },
            )
        }
    }
}

@Preview(name = "Editor 320", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Editor 360", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Editor 390", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Editor 430", widthDp = 430, heightDp = 932, showBackground = true)
@Composable
private fun BookEditorScreenPreview() {
    FlashReadTheme {
        BookEditorScreen(
            initialTitle = "",
            initialContent = "",
            onBack = {},
            onSave = { _, _ -> },
        )
    }
}

@Preview(name = "Editor filled 320", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun BookEditorScreenFilledPreview() {
    FlashReadTheme {
        BookEditorScreen(
            initialTitle = "very_long_created_book_title_that_should_ellipsis",
            initialContent = "Subvocalization is one of the things that can keep your reading speed down.\n\n" +
                "Speed reading trains you to take in words visually without sounding them out.",
            onBack = {},
            onSave = { _, _ -> },
        )
    }
}
