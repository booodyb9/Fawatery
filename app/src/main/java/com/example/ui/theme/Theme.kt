package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FawratyIosIcyColorScheme = lightColorScheme(
    primary = IosGlassBlue,
    onPrimary = Color.White,
    primaryContainer = IosGlassBlueLight,
    onPrimaryContainer = IosGlassBlueDark,
    secondary = RoyalGold,
    onSecondary = Color.White,
    secondaryContainer = RoyalGoldLight,
    onSecondaryContainer = RoyalGoldDark,
    tertiary = GlassBlueLight,
    background = IcyBackground,
    onBackground = TextPrimary,
    surface = FrostedGlassCard,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderSubtle
)

@Composable
fun FawratyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FawratyIosIcyColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FawratyTheme(content = content)
}
