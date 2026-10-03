package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Cozy Frame" design tokens for all UI chrome (dialogs, sheets, buttons, cards).
 * Warm, quiet and minimal so the pixel-art world stays the star. Scene drawing does not use these.
 * Contrast notes are against [TinyColors.Paper] unless stated.
 */
object TinyColors {
    /** Dialog and sheet surface. */
    val Paper = Color(0xFFFFF9F5)
    /** Cards and rows sitting on Paper. */
    val Card = Color(0xFFFFFCF8)
    /** Quiet fill for inactive chips, inputs and grouped rows. */
    val Muted = Color(0xFFF6EEE8)
    /** Titles and body text (9.5:1). */
    val Ink = Color(0xFF553D36)
    /** Secondary text (4.6:1, AA). Use instead of Ink at reduced alpha. */
    val InkMuted = Color(0xFF816E62)
    /** Hairline borders and dividers. */
    val Line = Color(0xFFEBDDD2)
    /** Primary actions and links (4.95:1 with white text). */
    val Rose = Color(0xFFB74C65)
    /** Selected fills and icon badge backgrounds. */
    val RoseSoft = Color(0xFFFCEDEF)
    /** Highlight borders, decorative accents. */
    val Blush = Color(0xFFFFB5C2)
    /** Success / "saved" state (4.8:1 with white text). */
    val Sage = Color(0xFF4F7A6F)
    val SageSoft = Color(0xFFE9F1EE)
    /** Warm badge accent; always pair with Ink text. */
    val Honey = Color(0xFFF6BD60)
    val HoneySoft = Color(0xFFFDF1DC)
    /** Accent used only by the Dream Journal header badge. */
    val Plum = Color(0xFF7B4A8E)
    val PlumSoft = Color(0xFFF3ECF6)
    /** Dialog scrim colour. */
    val Scrim = Color(0xFF2E211D)
}

object TinySpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

object TinyRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val Small = RoundedCornerShape(sm)
    val Medium = RoundedCornerShape(md)
    val Large = RoundedCornerShape(lg)
    val Dialog = RoundedCornerShape(xl)
    val Pill = RoundedCornerShape(50)
}

/** Serif titles keep the storybook warmth; body text stays in the clean default sans. */
object TinyType {
    val Display = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, color = TinyColors.Ink)
    val Title = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = TinyColors.Ink)
    val Section = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, color = TinyColors.Ink)
    val Body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = TinyColors.Ink)
    val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
    val Label = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = TinyColors.Ink)
    val Caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = TinyColors.InkMuted)
    /** The floor: nothing in chrome is set smaller than this. */
    val Micro = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, color = TinyColors.InkMuted)
}
