package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The chrome is a warm paper frame around the pixel world, so it always uses this light scheme.
// (Following the system dark setting used to mix dark Material parts into light dialogs.)
private val CozyColorScheme = lightColorScheme(
    primary = TinyColors.Rose,
    onPrimary = Color.White,
    primaryContainer = TinyColors.RoseSoft,
    onPrimaryContainer = TinyColors.Ink,
    secondary = TinyColors.Sage,
    onSecondary = Color.White,
    secondaryContainer = TinyColors.SageSoft,
    onSecondaryContainer = TinyColors.Ink,
    tertiary = TinyColors.Honey,
    onTertiary = TinyColors.Ink,
    background = TinyColors.Paper,
    onBackground = TinyColors.Ink,
    surface = TinyColors.Paper,
    onSurface = TinyColors.Ink,
    surfaceVariant = TinyColors.Muted,
    onSurfaceVariant = TinyColors.InkMuted,
    surfaceContainerLowest = TinyColors.Card,
    surfaceContainerLow = TinyColors.Card,
    surfaceContainer = TinyColors.Paper,
    surfaceContainerHigh = TinyColors.Paper,
    surfaceContainerHighest = TinyColors.Muted,
    outline = TinyColors.Line,
    outlineVariant = TinyColors.Line,
    scrim = TinyColors.Scrim
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CozyColorScheme,
        typography = Typography,
        content = content
    )
}
