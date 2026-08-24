package com.evgeniich.flashread.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.model.MaterialSourceType
import com.evgeniich.flashread.core.reading.withReadingStats
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.components.AppLogo
import com.evgeniich.flashread.ui.components.ScreenTitle
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<Book>,
    progressPercent: (Book) -> Int,
    onImportBook: () -> Unit,
    onCreateBook: () -> Unit,
    onSpeedReadText: () -> Unit,
    onAddYouTubeVideo: (title: String, url: String) -> Unit,
    onRenameBook: (bookId: String, newTitle: String) -> Unit,
    onDeleteBook: (String) -> Unit,
    onContinueReading: (String) -> Unit,
    onEditBook: (String) -> Unit,
    modifier: Modifier = Modifier,
    busyMessage: String? = null,
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var showYouTubeDialog by remember { mutableStateOf(false) }
    var bookPendingRename by remember { mutableStateOf<Book?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
    ) {
        Spacer(Modifier.height(FlashReadDimens.space8))
        ScreenTitle(title = stringResource(Res.string.screen_library))
        if (busyMessage != null) {
            Spacer(Modifier.height(FlashReadDimens.space12))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(FlashReadDimens.space8))
            Text(
                text = busyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(FlashReadDimens.space12))
        AddMaterialButton(
            label = stringResource(Res.string.library_add_material),
            onClick = { showAddSheet = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space16))

        when {
            books.isEmpty() && busyMessage != null -> {
                Spacer(Modifier.weight(1f))
            }
            books.isEmpty() -> {
                LibraryEmptyState(
                    onAddMaterial = { showAddSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space12),
                    contentPadding = PaddingValues(bottom = FlashReadDimens.space24),
                ) {
                    items(books, key = { it.id }) { book ->
                        LibraryMaterialCard(
                            title = book.title,
                            sourceType = book.sourceType,
                            coverFileName = book.coverFileName,
                            wordCount = book.wordCount,
                            progressPercent = progressPercent(book),
                            onContinue = { onContinueReading(book.id) },
                            onRename = { bookPendingRename = book },
                            onDelete = { onDeleteBook(book.id) },
                            onEdit = if (book.id.startsWith("created:")) {
                                { onEditBook(book.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddMaterialBottomSheet(
            onDismiss = { showAddSheet = false },
            onImportBook = {
                showAddSheet = false
                onImportBook()
            },
            onCreateBook = {
                showAddSheet = false
                onCreateBook()
            },
            onSpeedReadText = {
                showAddSheet = false
                onSpeedReadText()
            },
            onAddYouTube = {
                showAddSheet = false
                showYouTubeDialog = true
            },
        )
    }

    if (showYouTubeDialog) {
        AddYouTubeDialog(
            onDismiss = { showYouTubeDialog = false },
            onConfirm = { title, url ->
                showYouTubeDialog = false
                onAddYouTubeVideo(title, url)
            },
        )
    }

    bookPendingRename?.let { book ->
        RenameMaterialDialog(
            currentTitle = book.title,
            onDismiss = { bookPendingRename = null },
            onConfirm = { newTitle ->
                bookPendingRename = null
                onRenameBook(book.id, newTitle)
            },
        )
    }
}

@Composable
private fun AddMaterialButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = FlashReadDimens.minTouchTarget),
        shape = FlashReadShapes.button,
        contentPadding = PaddingValues(
            horizontal = FlashReadDimens.space16,
            vertical = FlashReadDimens.space12,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(FlashReadDimens.space8))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryEmptyState(
    onAddMaterial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = FlashReadDimens.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(size = 80.dp)
        Spacer(Modifier.height(FlashReadDimens.space16))
        Text(
            text = stringResource(Res.string.library_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        Text(
            text = stringResource(Res.string.library_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space24))
        AddMaterialButton(
            label = stringResource(Res.string.library_add_first_material),
            onClick = onAddMaterial,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LibraryMaterialCard(
    title: String,
    sourceType: MaterialSourceType,
    coverFileName: String?,
    wordCount: Int,
    progressPercent: Int,
    onContinue: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val displayTitle = remember(title) { MaterialTitleFormatter.displayTitle(title) }
    val secondary = materialSecondaryLabel(sourceType, wordCount)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FlashReadShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onContinue,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlashReadDimens.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                MaterialArtwork(sourceType = sourceType, coverFileName = coverFileName)
                Spacer(Modifier.width(FlashReadDimens.space12))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = FlashReadDimens.space4),
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
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
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(FlashReadDimens.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(Res.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_edit)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.action_rename)) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.action_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(FlashReadDimens.space16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space12),
            ) {
                Text(
                    text = stringResource(Res.string.percent_value, progressPercent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(FlashReadDimens.space4)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            Spacer(Modifier.height(FlashReadDimens.space12))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FlashReadDimens.minTouchTarget),
                shape = FlashReadShapes.button,
                contentPadding = PaddingValues(horizontal = FlashReadDimens.space16),
            ) {
                Text(
                    text = stringResource(Res.string.action_continue),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMaterialBottomSheet(
    onDismiss: () -> Unit,
    onImportBook: () -> Unit,
    onCreateBook: () -> Unit,
    onSpeedReadText: () -> Unit,
    onAddYouTube: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.space8)
                .padding(bottom = FlashReadDimens.space24),
        ) {
            Text(
                text = stringResource(Res.string.library_add_material),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = FlashReadDimens.space16),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            AddMaterialSheetAction(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = stringResource(Res.string.action_import_book),
                onClick = onImportBook,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            AddMaterialSheetAction(
                icon = Icons.Filled.Edit,
                label = stringResource(Res.string.action_create_book),
                onClick = onCreateBook,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            AddMaterialSheetAction(
                icon = Icons.Filled.Bolt,
                label = stringResource(Res.string.action_speed_read_text),
                onClick = onSpeedReadText,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            AddMaterialSheetAction(
                icon = Icons.Filled.PlayCircle,
                label = stringResource(Res.string.library_add_youtube),
                onClick = onAddYouTube,
            )
        }
    }
}

@Composable
private fun AddMaterialSheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun materialSecondaryLabel(
    sourceType: MaterialSourceType,
    wordCount: Int,
): String {
    return when (sourceType) {
        MaterialSourceType.YouTube -> stringResource(Res.string.source_youtube_video)
        MaterialSourceType.Book -> pluralStringResource(Res.plurals.word_count, wordCount, wordCount)
    }
}

@Composable
private fun AddYouTubeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.library_add_youtube),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space12)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.library_field_title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.library_field_url)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, url) },
                enabled = url.isNotBlank(),
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.card,
    )
}

@Composable
private fun RenameMaterialDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.library_rename_title)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.library_field_title)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.card,
    )
}

@Preview(name = "Library 320", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Library 360", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Library 390", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Library 430", widthDp = 430, heightDp = 932, showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    FlashReadTheme {
        LibraryScreen(
            books = previewBooks(),
            progressPercent = { book -> if (book.id == "2") 64 else 12 },
            onImportBook = {},
            onCreateBook = {},
            onSpeedReadText = {},
            onAddYouTubeVideo = { _, _ -> },
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onContinueReading = {},
            onEditBook = {},
        )
    }
}

@Preview(name = "Library empty 320", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun LibraryEmptyPreview() {
    FlashReadTheme {
        LibraryScreen(
            books = emptyList(),
            progressPercent = { 0 },
            onImportBook = {},
            onCreateBook = {},
            onSpeedReadText = {},
            onAddYouTubeVideo = { _, _ -> },
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onContinueReading = {},
            onEditBook = {},
        )
    }
}

private fun previewBooks(): List<Book> = listOf(
    Book(
        id = "1",
        title = "very_long_imported_book_title_that_should_wrap_nicely_12345.txt",
        content = "one two three four five six seven eight nine ten",
        sourceType = MaterialSourceType.Book,
    ).withReadingStats(),
    Book(
        id = "2",
        title = "Speed reading lecture",
        content = "https://youtu.be/dQw4w9wg",
        sourceType = MaterialSourceType.YouTube,
    ).withReadingStats(),
    Book(
        id = "created:preview",
        title = "Мои заметки",
        content = "one two three four five",
        sourceType = MaterialSourceType.Book,
    ).withReadingStats(),
)
