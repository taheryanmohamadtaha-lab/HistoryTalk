package com.historytalk.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density

// پالت خاکی/زرین با کنتراست بالا
private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4A12), onPrimary = Color.White,
    primaryContainer = Color(0xFFF1DFB2), onPrimaryContainer = Color(0xFF2B1D03),
    secondary = Color(0xFF7D5F28), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE3C6), onSecondaryContainer = Color(0xFF2B2111),
    tertiary = Color(0xFF7A3E2B), onTertiary = Color.White,
    background = Color(0xFFFAF4E6), onBackground = Color(0xFF2A2118),
    surface = Color(0xFFFFFBF2), onSurface = Color(0xFF2A2118),
    surfaceVariant = Color(0xFFEFE6D2), onSurfaceVariant = Color(0xFF51463A),
    outline = Color(0xFF857766),
    error = Color(0xFF9F2A1F), onError = Color.White,
    errorContainer = Color(0xFFF8DAD3), onErrorContainer = Color(0xFF410E08),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE3BE6B), onPrimary = Color(0xFF2B1E05),
    primaryContainer = Color(0xFF4F3A12), onPrimaryContainer = Color(0xFFF7E3B0),
    secondary = Color(0xFFD2B77C), onSecondary = Color(0xFF2B2111),
    secondaryContainer = Color(0xFF3E3320), onSecondaryContainer = Color(0xFFEFE3C6),
    tertiary = Color(0xFFE5A58F), onTertiary = Color(0xFF3F1608),
    background = Color(0xFF1B1710), onBackground = Color(0xFFEDE3D0),
    surface = Color(0xFF241F17), onSurface = Color(0xFFEDE3D0),
    surfaceVariant = Color(0xFF3A3326), onSurfaceVariant = Color(0xFFD3C7B1),
    outline = Color(0xFF9A8D78),
    error = Color(0xFFFFB4A8), onError = Color(0xFF561E16),
    errorContainer = Color(0xFF7A2E24), onErrorContainer = Color(0xFFFFDAD3),
)

/** فونت وزیرمتن از assets/fonts (در CI دانلود می‌شود)؛ در نبود آن فونت سیستم. */
@Composable
private fun rememberAppFontFamily(): FontFamily {
    val ctx = LocalContext.current
    return remember {
        runCatching {
            val have = ctx.assets.list("fonts")?.toSet().orEmpty()
            val fonts = buildList {
                if ("Vazirmatn-Regular.ttf" in have) add(Font(path = "fonts/Vazirmatn-Regular.ttf", assetManager = ctx.assets, weight = FontWeight.Normal))
                if ("Vazirmatn-Bold.ttf" in have) add(Font(path = "fonts/Vazirmatn-Bold.ttf", assetManager = ctx.assets, weight = FontWeight.Bold))
            }
            if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
        }.getOrDefault(FontFamily.Default)
    }
}

private fun Typography.withFamily(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun HistoryTalkTheme(
    dark: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val family = rememberAppFontFamily()
    val typography = remember(family) { Typography().withFamily(family) }
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * fontScale)) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = typography,
            content = content,
        )
    }
}
