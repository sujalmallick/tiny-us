package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun TextStyle.noColor() = copy(color = Color.Unspecified)

// Material styles mapped onto the TinyType scale so stock components (text fields, chips,
// date picker) match the custom chrome.
val Typography = Typography(
    headlineSmall = TinyType.Display.noColor(),
    titleLarge = TinyType.Title.noColor(),
    titleMedium = TinyType.Section.noColor(),
    titleSmall = TinyType.Label.noColor(),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TinyType.Body.noColor(),
    bodySmall = TinyType.Caption.noColor(),
    labelLarge = TinyType.Label.copy(fontSize = 14.sp).noColor(),
    labelMedium = TinyType.Label.noColor(),
    labelSmall = TinyType.Micro.noColor()
)
