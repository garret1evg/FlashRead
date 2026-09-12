package com.evgeniich.flashread.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evgeniich.flashread.ads.RewardedAdHost
import com.evgeniich.flashread.consent.isPrivacyOptionsRequired
import com.evgeniich.flashread.ui.ads.RewardedAdOffer
import com.evgeniich.flashread.core.locale.AppLanguage
import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.platform.AppInfo
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.components.ScreenTitle
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import com.evgeniich.flashread.ui.theme.flashReadSwitchColors
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.monetization.MonetizationState
import com.evgeniich.flashread.monetization.TimeProvider
import org.jetbrains.compose.resources.stringResource

private val languagePickerOptions: List<AppLanguage> = listOf(AppLanguage.System) +
    AppLanguage.SUPPORTED_CODES.map { AppLanguage.Language(it) }

/** Required consecutive taps on the logo to trigger developer unlock. */
private const val DEV_UNLOCK_TAP_COUNT = 5

/** Timeout in milliseconds after which tap count resets. */
private const val DEV_UNLOCK_TAP_TIMEOUT_MS = 3000L

@Composable
fun SettingsScreen(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onManagePrivacy: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = AppInfo.versionName,
    isDeveloperModeUnlocked: Boolean = false,
    monetizationState: MonetizationState? = null,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val languageLabel = stringResource(Res.string.settings_language)
    val selectedLanguageLabel = selectedLanguage.label()
    val themeLabel = stringResource(Res.string.settings_theme)
    val selectedThemeLabel = selectedTheme.label()

    // Developer unlock gesture state
    var devTapCount by remember { mutableIntStateOf(0) }
    var devLastTapTimeMs by remember { mutableLongStateOf(0L) }
    var showDeveloperPasswordDialog by remember { mutableStateOf(false) }

    // Reset tap count and cancel any pending rewarded ad when leaving Settings
    DisposableEffect(Unit) {
        onDispose {
            devTapCount = 0
            devLastTapTimeMs = 0L
            RewardedAdHost.getInstance().cancelPendingShow()
        }
    }

    val onLogoTap: (() -> Unit)? = if (isDeveloperModeUnlocked) {
        // Already unlocked, no gesture needed
        null
    } else {
        {
            val now = TimeProvider.currentTimeMs()
            // Reset if timeout elapsed since last tap
            if (devLastTapTimeMs > 0 && (now - devLastTapTimeMs) > DEV_UNLOCK_TAP_TIMEOUT_MS) {
                devTapCount = 0
            }
            devLastTapTimeMs = now
            devTapCount++
            if (devTapCount >= DEV_UNLOCK_TAP_COUNT) {
                // Prevent duplicate dialogs
                if (!showDeveloperPasswordDialog) {
                    showDeveloperPasswordDialog = true
                }
                // Reset count after firing
                devTapCount = 0
                devLastTapTimeMs = 0L
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8),
    ) {
        ScreenTitle(
            title = stringResource(Res.string.screen_settings),
            onLogoClick = onLogoTap,
        )
        Spacer(Modifier.height(FlashReadDimens.space16))

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
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
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = FlashReadDimens.space16),
                    color = MaterialTheme.colorScheme.outline,
                )
                SettingsLinkRow(
                    icon = Icons.Outlined.Palette,
                    label = themeLabel,
                    value = selectedThemeLabel,
                    onClick = { showThemeDialog = true },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = FlashReadDimens.space16),
                    color = MaterialTheme.colorScheme.outline,
                )
                SettingsSwitchRow(
                    icon = Icons.Outlined.LightMode,
                    label = stringResource(Res.string.settings_keep_screen_on),
                    subtitle = stringResource(Res.string.settings_keep_screen_on_subtitle),
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                )
            }
            Spacer(Modifier.height(FlashReadDimens.space16))
            if (monetizationState != null) {
                RewardedAdsSettingsSection()
            }
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
                if (isPrivacyOptionsRequired()) {
                    SettingsLinkRow(
                        icon = Icons.Outlined.PrivacyTip,
                        label = stringResource(Res.string.settings_manage_privacy),
                        onClick = onManagePrivacy,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = FlashReadDimens.space16),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
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

            // Developer section (only if unlocked)
            if (isDeveloperModeUnlocked && monetizationState != null) {
                Spacer(Modifier.height(FlashReadDimens.space16))
                DeveloperMenu(
                    state = monetizationState,
                    onLockDeveloperMode = {
                        MonetizationManager.lockDeveloperMode()
                    },
                    onSetActiveUsageTime = { totalMs ->
                        MonetizationManager.setActiveUsageTime(totalMs)
                    },
                    onGrantReward = {
                        MonetizationManager.applyReward()
                    },
                    onClearReward = {
                        MonetizationManager.clearReward()
                    },
                    onSetUsageDays = { count ->
                        MonetizationManager.setUsageDays(count)
                    },
                )
            }

            // App name and version at the bottom of scroll content
            Spacer(Modifier.height(FlashReadDimens.space24))
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

    if (showThemeDialog) {
        ThemePickerDialog(
            selectedTheme = selectedTheme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                showThemeDialog = false
                onThemeSelected(theme)
            },
        )
    }

    if (showDeveloperPasswordDialog) {
        DeveloperPasswordDialog(
            onDismiss = { showDeveloperPasswordDialog = false },
            onUnlocked = {
                MonetizationManager.unlockDeveloperMode()
                showDeveloperPasswordDialog = false
            },
        )
    }
}

@Composable
private fun RewardedAdsSettingsSection() {
    RewardedAdOffer { inner ->
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                inner()
            }
        }
        Spacer(Modifier.height(FlashReadDimens.space16))
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
private fun ThemePickerDialog(
    selectedTheme: AppTheme,
    onDismiss: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.settings_theme),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            ) {
                AppTheme.entries.forEach { option ->
                    val selected = option == selectedTheme
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
                                onClick = { onThemeSelected(option) },
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
private fun DeveloperPasswordDialog(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    fun attemptUnlock() {
        if (DEV_UNLOCK_PASSWORD.isNotEmpty() && password == DEV_UNLOCK_PASSWORD) {
            onUnlocked()
        } else {
            showError = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Developer menu",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter the password to unlock developer settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(FlashReadDimens.space16))
                OutlinedTextField(
                    value = password,
                    onValueChange = { newValue ->
                        password = newValue
                        // Clear error when user edits field
                        if (showError) {
                            showError = false
                        }
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { attemptUnlock() },
                    ),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Incorrect password.") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { attemptUnlock() },
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text("Unlock")
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
private fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                maxLines = 2,
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
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = flashReadSwitchColors(),
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            ),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun AppTheme.label(): String = when (this) {
    AppTheme.System -> stringResource(Res.string.settings_theme_system)
    AppTheme.Light -> stringResource(Res.string.reader_theme_light)
    AppTheme.Sepia -> stringResource(Res.string.reader_theme_sepia)
    AppTheme.Dark -> stringResource(Res.string.reader_theme_dark)
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
        "pl" -> stringResource(Res.string.language_pl)
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
            selectedTheme = AppTheme.System,
            onThemeSelected = {},
            keepScreenOn = true,
            onKeepScreenOnChange = {},
            onManagePrivacy = {},
            onOpenPrivacyPolicy = {},
            onOpenTerms = {},
            versionName = "1.0.0",
        )
    }
}
