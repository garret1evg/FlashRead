package com.evgeniich.flashread.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evgeniich.flashread.core.theme.AppTheme

object FlashReadColors {
    val background = Color(0xFFF8F7FC)
    val surface = Color(0xFFFFFFFF)
    val primary = Color(0xFF6D4DB5)
    val onPrimary = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFF1D1B20)
    val textSecondary = Color(0xFF6F6975)
    val outline = Color(0xFFE5E0EA)
    val primaryContainer = Color(0xFFEDE7F6)
    val onPrimaryContainer = Color(0xFF3D2A6E)
}

object FlashReadDarkColors {
    val background = Color(0xFF121016)
    val surface = Color(0xFF1C1B20)
    val primary = Color(0xFFC4B0F0)
    val onPrimary = Color(0xFF2D1B54)
    val textPrimary = Color(0xFFE6E1E5)
    val textSecondary = Color(0xFFCAC4D0)
    val outline = Color(0xFF49454F)
    val primaryContainer = Color(0xFF4A3A73)
    val onPrimaryContainer = Color(0xFFEDE7F6)
}

object FlashReadSepiaColors {
    val background = Color(0xFFF4ECD8)
    val surface = Color(0xFFFFF8E7)
    val primary = Color(0xFF8B5A12)
    val onPrimary = Color(0xFFFFF8E7)
    val textPrimary = Color(0xFF5C4B32)
    val textSecondary = Color(0xFF8A7A62)
    val outline = Color(0xFFE6D9BF)
    val primaryContainer = Color(0xFFE8DCC4)
    val onPrimaryContainer = Color(0xFF5C4B32)
}

object FlashReadDimens {
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val screenHorizontalPadding = 20.dp
    val cardRadius = 16.dp
    val buttonRadius = 14.dp
    val minTouchTarget = 48.dp
    val typeIconSize = 40.dp
    val coverThumbWidth = 48.dp
    val coverThumbHeight = 72.dp
    /** Approximate height of an adaptive banner ad to reserve space. */
    val bannerAdHeight = 50.dp
}

object FlashReadShapes {
    val card = RoundedCornerShape(FlashReadDimens.cardRadius)
    val button = RoundedCornerShape(FlashReadDimens.buttonRadius)
    val sheet = RoundedCornerShape(
        topStart = FlashReadDimens.space24,
        topEnd = FlashReadDimens.space24,
    )
}

private val FlashReadLightColorScheme = lightColorScheme(
    primary = FlashReadColors.primary,
    onPrimary = FlashReadColors.onPrimary,
    primaryContainer = FlashReadColors.primaryContainer,
    onPrimaryContainer = FlashReadColors.onPrimaryContainer,
    secondary = FlashReadColors.primary,
    onSecondary = FlashReadColors.onPrimary,
    secondaryContainer = FlashReadColors.primaryContainer,
    onSecondaryContainer = FlashReadColors.onPrimaryContainer,
    background = FlashReadColors.background,
    onBackground = FlashReadColors.textPrimary,
    surface = FlashReadColors.surface,
    onSurface = FlashReadColors.textPrimary,
    surfaceVariant = FlashReadColors.primaryContainer,
    onSurfaceVariant = FlashReadColors.textSecondary,
    outline = FlashReadColors.outline,
    outlineVariant = FlashReadColors.outline,
)

private val FlashReadSepiaColorScheme = lightColorScheme(
    primary = FlashReadSepiaColors.primary,
    onPrimary = FlashReadSepiaColors.onPrimary,
    primaryContainer = FlashReadSepiaColors.primaryContainer,
    onPrimaryContainer = FlashReadSepiaColors.onPrimaryContainer,
    secondary = FlashReadSepiaColors.primary,
    onSecondary = FlashReadSepiaColors.onPrimary,
    secondaryContainer = FlashReadSepiaColors.primaryContainer,
    onSecondaryContainer = FlashReadSepiaColors.onPrimaryContainer,
    background = FlashReadSepiaColors.background,
    onBackground = FlashReadSepiaColors.textPrimary,
    surface = FlashReadSepiaColors.surface,
    onSurface = FlashReadSepiaColors.textPrimary,
    surfaceVariant = FlashReadSepiaColors.primaryContainer,
    onSurfaceVariant = FlashReadSepiaColors.textSecondary,
    outline = FlashReadSepiaColors.outline,
    outlineVariant = FlashReadSepiaColors.outline,
)

private val FlashReadDarkColorScheme = darkColorScheme(
    primary = FlashReadDarkColors.primary,
    onPrimary = FlashReadDarkColors.onPrimary,
    primaryContainer = FlashReadDarkColors.primaryContainer,
    onPrimaryContainer = FlashReadDarkColors.onPrimaryContainer,
    secondary = FlashReadDarkColors.primary,
    onSecondary = FlashReadDarkColors.onPrimary,
    secondaryContainer = FlashReadDarkColors.primaryContainer,
    onSecondaryContainer = FlashReadDarkColors.onPrimaryContainer,
    background = FlashReadDarkColors.background,
    onBackground = FlashReadDarkColors.textPrimary,
    surface = FlashReadDarkColors.surface,
    onSurface = FlashReadDarkColors.textPrimary,
    surfaceVariant = FlashReadDarkColors.primaryContainer,
    onSurfaceVariant = FlashReadDarkColors.textSecondary,
    outline = FlashReadDarkColors.outline,
    outlineVariant = FlashReadDarkColors.outline,
)

private val FlashReadMaterialShapes = Shapes(
    extraSmall = FlashReadShapes.button,
    small = FlashReadShapes.button,
    medium = FlashReadShapes.card,
    large = FlashReadShapes.card,
    extraLarge = FlashReadShapes.sheet,
)

val LocalAppTheme = staticCompositionLocalOf { AppTheme.Light }

@Composable
fun flashReadSwitchColors(): SwitchColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalAppTheme.current == AppTheme.Dark
    return SwitchDefaults.colors(
        checkedThumbColor = scheme.onPrimary,
        checkedTrackColor = scheme.primary,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = if (isDark) scheme.onSurfaceVariant else scheme.surface,
        uncheckedTrackColor = scheme.onSurface.copy(alpha = if (isDark) 0.24f else 0.32f),
        uncheckedBorderColor = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.70f else 0.55f),
    )
}

fun AppTheme.splashBackground(systemDark: Boolean): Color = when (resolve(systemDark)) {
    AppTheme.Dark -> FlashReadDarkColors.background
    AppTheme.Sepia -> FlashReadSepiaColors.background
    AppTheme.Light, AppTheme.System -> FlashReadColors.background
}

@Composable
fun FlashReadTheme(
    theme: AppTheme = AppTheme.System,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val resolved = theme.resolve(systemDark = darkTheme)
    ApplyWindowLightBars(light = resolved != AppTheme.Dark)
    CompositionLocalProvider(LocalAppTheme provides resolved) {
        MaterialTheme(
            colorScheme = when (resolved) {
                AppTheme.Dark -> FlashReadDarkColorScheme
                AppTheme.Sepia -> FlashReadSepiaColorScheme
                AppTheme.Light, AppTheme.System -> FlashReadLightColorScheme
            },
            shapes = FlashReadMaterialShapes,
            content = content,
        )
    }
}
