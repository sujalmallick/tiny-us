package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SoftRose,
    onPrimary = Color.White,
    primaryContainer = DeepRose,
    secondary = SageGreen,
    tertiary = PeachMuted,
    background = SoftMidnight,
    surface = SurfaceCardDark,
    onBackground = CozyCream,
    onSurface = CozyCream
)

private val LightColorScheme = lightColorScheme(
    primary = DeepRose,
    onPrimary = Color.White,
    primaryContainer = BlushPink,
    secondary = SageGreen,
    tertiary = PeachMuted,
    background = CozyCream,
    surface = SurfaceCardLight,
    onBackground = DarkSlate,
    onSurface = DarkSlate
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep custom cozy theme palette consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
