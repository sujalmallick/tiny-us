package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.WeatherType
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Environmental depth (depth, part 4): far hills fading into the sky behind the meadow, so the
 * scene reads as near, middle and far. They sit behind everything, so nothing covers the couple.
 */

/** The far and near hills' colours: the far one leans toward the sky (it is further through the air). */
private fun hillColors(isNight: Boolean, isSunset: Boolean, isMorning: Boolean, weather: WeatherType): Pair<Color, Color> = when {
    isNight -> Color(0xFF1B2741) to Color(0xFF17282B)
    weather == WeatherType.SNOW -> Color(0xFFDCE5EF) to Color(0xFFC9D5E1)
    weather == WeatherType.RAIN -> Color(0xFF71859A) to Color(0xFF5E7684)
    isSunset -> Color(0xFF9A5D7C) to Color(0xFF6E4D5E)
    isMorning -> Color(0xFFC6B5C6) to Color(0xFF9CB39A)
    weather == WeatherType.AUTUMN -> Color(0xFFB9B48E) to Color(0xFF9C9862)
    weather == WeatherType.SAKURA -> Color(0xFFB7CDC4) to Color(0xFF93C08A)
    else -> Color(0xFFA2CDBB) to Color(0xFF86BE8C)
}

/**
 * Two rows of low rolling hills on the horizon at [groundY], drawn after the sky and before the
 * ground: a pale far row and a slightly stronger near row, each with a lit top edge by day.
 */
fun drawDistantHills(scope: DrawScope, cw: Float, groundY: Float, p: Float, isNight: Boolean, isSunset: Boolean, isMorning: Boolean, weather: WeatherType) {
    val (far, near) = hillColors(isNight, isSunset, isMorning, weather)
    val farRim = if (isNight) far else Color(
        red = far.red + (1f - far.red) * 0.25f,
        green = far.green + (1f - far.green) * 0.25f,
        blue = far.blue + (1f - far.blue) * 0.25f
    )
    val nearRim = if (isNight) near else Color(
        red = near.red + (1f - near.red) * 0.18f,
        green = near.green + (1f - near.green) * 0.18f,
        blue = near.blue + (1f - near.blue) * 0.18f
    )
    val base = (groundY / p).roundToInt()
    val cols = (cw / p).roundToInt() + 1
    var x = 0
    while (x < cols) {
        val t = x / cols.toFloat()
        // Heights in whole pixels, two pixels wide per step, so the outline steps like pixel art
        val farH = (9f + 5f * sin(t * 7.1f + 0.8f) + 3f * sin(t * 15.3f + 2.1f)).roundToInt()
        val nearH = (4f + 3f * sin(t * 9.7f + 2.6f) + 1.5f * sin(t * 21f)).roundToInt()
        val left = x * p
        scope.drawRect(farRim, Offset(left, (base - farH) * p), Size(2f * p, p))
        scope.drawRect(far, Offset(left, (base - farH + 1) * p), Size(2f * p, (farH + 1) * p))
        if (nearH > 0) {
            scope.drawRect(nearRim, Offset(left, (base - nearH) * p), Size(2f * p, p))
            scope.drawRect(near, Offset(left, (base - nearH + 1) * p), Size(2f * p, (nearH + 1) * p))
        }
        x += 2
    }
}
