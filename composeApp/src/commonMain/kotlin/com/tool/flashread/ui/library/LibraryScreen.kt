package com.tool.flashread.ui.library

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.core.reading.withReadingStats
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadShapes
import com.tool.flashread.ui.theme.FlashReadTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<Book>,
    progressPercent: (Book) -> Int,
    onImportBook: () -> Unit,
    onAddYouTubeVideo: (title: String, url: String) -> Unit,
    onRenameBook: (bookId: String, newTitle: String) -> Unit,
    onDeleteBook: (String) -> Unit,
    onContinueReading: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        Text(
            text = "Библиотека",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(FlashReadDimens.space12))
        AddMaterialButton(
            label = "Добавить материал",
            onClick = { showAddSheet = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space16))

        if (books.isEmpty()) {
            LibraryEmptyState(
                onAddMaterial = { showAddSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
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
                        wordCount = book.wordCount,
                        progressPercent = progressPercent(book),
                        onContinue = { onContinueReading(book.id) },
                        onRename = { bookPendingRename = book },
                        onDelete = { onDeleteBook(book.id) },
                    )
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(FlashReadDimens.space16))
        Text(
            text = "Библиотека пуста",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        Text(
            text = "Добавьте книгу или YouTube-видео, чтобы начать читать в обычном режиме или скорочтении.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space24))
        AddMaterialButton(
            label = "Добавить первый материал",
            onClick = onAddMaterial,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LibraryMaterialCard(
    title: String,
    sourceType: MaterialSourceType,
    wordCount: Int,
    progressPercent: Int,
    onContinue: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayTitle = remember(title) { MaterialTitleFormatter.displayTitle(title) }
    val secondary = remember(sourceType, wordCount) {
        materialSecondaryLabel(sourceType, wordCount)
    }
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
                MaterialTypeIcon(sourceType = sourceType)
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
                            contentDescription = "Ещё",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
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
                    text = "$progressPercent%",
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
                    text = "Продолжить",
                    maxLines = 1,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMaterialBottomSheet(
    onDismiss: () -> Unit,
    onImportBook: () -> Unit,
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
                text = "Добавить материал",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = FlashReadDimens.space16),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            AddMaterialSheetAction(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Импортировать книгу",
                onClick = onImportBook,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            AddMaterialSheetAction(
                icon = Icons.Filled.PlayCircle,
                label = "Добавить YouTube-видео",
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

private fun materialSecondaryLabel(
    sourceType: MaterialSourceType,
    wordCount: Int,
): String {
    return when (sourceType) {
        MaterialSourceType.YouTube -> "YouTube-видео"
        MaterialSourceType.Book -> formatWordCount(wordCount)
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
                text = "Добавить YouTube-видео",
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
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ссылка") },
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
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text("Отмена")
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
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text("Cancel")
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
            onAddYouTubeVideo = { _, _ -> },
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onContinueReading = {},
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
            onAddYouTubeVideo = { _, _ -> },
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onContinueReading = {},
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
)
