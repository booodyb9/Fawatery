package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FawratyDarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = DeepObsidian,
    primaryContainer = RoyalGoldDark,
    onPrimaryContainer = RoyalGoldLight,
    secondary = GlassBlue,
    onSecondary = Color.White,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = GlassBlueLight,
    tertiary = GlassBlueLight,
    background = DeepObsidian,
    onBackground = TextPrimary,
    surface = CardBackgroundGlass,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassGoldBorder
)

@Composable
fun FawratyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FawratyDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FawratyTheme(content = content)
}
