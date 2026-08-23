package com.tool.flashread.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tool.flashread.core.locale.AppLanguage
import com.tool.flashread.platform.AppInfo
import com.tool.flashread.resources.Res
import com.tool.flashread.resources.*
import com.tool.flashread.ui.components.ScreenTitle
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadShapes
import com.tool.flashread.ui.theme.FlashReadTheme
import org.jetbrains.compose.resources.stringResource

private val languagePickerOptions: List<AppLanguage> = listOf(AppLanguage.System) +
    AppLanguage.SUPPORTED_CODES.map { AppLanguage.Language(it) }

@Composable
fun SettingsScreen(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = AppInfo.versionName,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val languageLabel = stringResource(Res.string.settings_language)
    val selectedLanguageLabel = selectedLanguage.label()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8),
    ) {
        ScreenTitle(title = stringResource(Res.string.screen_settings))
        Spacer(Modifier.height(FlashReadDimens.space16))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FlashReadShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            SettingsLinkRow(
                icon = Icons.Outlined.Language,
                label = languageLabel,
                value = selectedLanguageLabel,
                onClick = { showLanguageDialog = true },
            )
        }
        Spacer(Modifier.height(FlashReadDimens.space16))
        Text(
            text = stringResource(Res.string.settings_legal),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FlashReadShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            SettingsLinkRow(
                icon = Icons.Outlined.Policy,
                label = stringResource(Res.string.settings_privacy_policy),
                onClick = onOpenPrivacyPolicy,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = FlashReadDimens.space16),
                color = MaterialTheme.colorScheme.outline,
            )
            SettingsLinkRow(
                icon = Icons.AutoMirrored.Outlined.Article,
                label = stringResource(Res.string.settings_terms),
                onClick = onOpenTerms,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(FlashReadDimens.space4))
        val versionLabel = stringResource(Res.string.settings_version, versionName)
        val versionCd = stringResource(Res.string.settings_version_cd, versionName)
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FlashReadDimens.space16)
                .semantics { contentDescription = versionCd },
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            selectedLanguage = selectedLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                showLanguageDialog = false
                onLanguageSelected(language)
            },
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    selectedLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.settings_language),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                languagePickerOptions.forEach { option ->
                    val selected = option == selectedLanguage
                    val label = option.label()
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = FlashReadDimens.minTouchTarget)
                            .selectable(
                                selected = selected,
                                onClick = { onLanguageSelected(option) },
                                role = Role.RadioButton,
                            ),
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.card,
    )
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    val rowDescription = if (value == null) label else "$label, $value"
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
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(FlashReadDimens.space4))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = rowDescription
            },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun AppLanguage.label(): String = when (this) {
    AppLanguage.System -> stringResource(Res.string.settings_language_system)
    is AppLanguage.Language -> when (code) {
        "en" -> stringResource(Res.string.language_en)
        "es" -> stringResource(Res.string.language_es)
        "pt" -> stringResource(Res.string.language_pt)
        "fr" -> stringResource(Res.string.language_fr)
        "de" -> stringResource(Res.string.language_de)
        "ru" -> stringResource(Res.string.language_ru)
        "uk" -> stringResource(Res.string.language_uk)
        "hi" -> stringResource(Res.string.language_hi)
        "ar" -> stringResource(Res.string.language_ar)
        else -> code
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    FlashReadTheme {
        SettingsScreen(
            selectedLanguage = AppLanguage.System,
            onLanguageSelected = {},
            onOpenPrivacyPolicy = {},
            onOpenTerms = {},
            versionName = "1.0",
        )
    }
}
