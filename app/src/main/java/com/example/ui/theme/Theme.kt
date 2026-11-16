package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaDarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = ElectricIndigoSoft,
    secondary = CyanShimmer,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanShimmerSoft,
    tertiary = VioletGlow,
    onTertiary = Color.White,
    background = ObsidianBackground,
    onBackground = TextAlabaster,
    surface = NightSlateSurface,
    onSurface = TextAlabaster,
    surfaceVariant = GlassCardSurface,
    onSurfaceVariant = TextCoolGray,
    outline = BorderSubtle,
    outlineVariant = ElevatedSurfaceVariant,
    error = CoralError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
