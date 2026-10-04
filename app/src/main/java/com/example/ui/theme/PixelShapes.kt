package com.example.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A rectangle whose corners are cut in whole-pixel stair steps tracing a quarter circle: the
 * pixel-art version of a rounded corner (Plan 03, Phase 5). One step is [unit], rounded to whole
 * device pixels so the edges stay crisp.
 *
 * Give either a corner [radius] or a [percent] of the shorter side (50 makes a pill or circle).
 */
class PixelCornerShape private constructor(
    private val radius: Dp?,
    private val percent: Int?,
    private val unit: Dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val u = with(density) { unit.toPx() }.roundToInt().coerceAtLeast(1).toFloat()
        val wanted = radius?.let { with(density) { it.toPx() } } ?: (min(w, h) * (percent ?: 0) / 100f)
        val r = min(wanted, min(w, h) / 2f)
        val steps = (r / u).toInt()
        if (steps < 1) return Outline.Rectangle(Rect(0f, 0f, w, h))
        // Inset (in steps) of each pixel row from the corner's edge, top row first.
        val inset = IntArray(steps) { j ->
            val dy = steps - j - 0.5f
            (steps - sqrt((steps * steps - dy * dy).coerceAtLeast(0f))).roundToInt()
        }
        val path = Path().apply {
            moveTo(inset[0] * u, 0f)
            lineTo(w - inset[0] * u, 0f)
            for (j in 0 until steps) { // top right, downward
                lineTo(w - inset[j] * u, j * u)
                lineTo(w - inset[j] * u, (j + 1) * u)
            }
            lineTo(w, steps * u)
            lineTo(w, h - steps * u)
            for (j in steps - 1 downTo 0) { // bottom right, downward
                lineTo(w - inset[j] * u, h - (j + 1) * u)
                lineTo(w - inset[j] * u, h - j * u)
            }
            lineTo(inset[0] * u, h)
            for (j in 0 until steps) { // bottom left, upward
                lineTo(inset[j] * u, h - j * u)
                lineTo(inset[j] * u, h - (j + 1) * u)
            }
            lineTo(0f, h - steps * u)
            lineTo(0f, steps * u)
            for (j in steps - 1 downTo 0) { // top left, upward
                lineTo(inset[j] * u, (j + 1) * u)
                lineTo(inset[j] * u, j * u)
            }
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) =
        other is PixelCornerShape && other.radius == radius && other.percent == percent && other.unit == unit

    override fun hashCode() = (radius?.hashCode() ?: 0) * 31 + (percent ?: 0) * 7 + unit.hashCode()

    companion object {
        /** The size of one stair step. */
        val STEP = 2.dp

        fun radius(radius: Dp, unit: Dp = STEP) = PixelCornerShape(radius, null, unit)
        fun percent(percent: Int, unit: Dp = STEP) = PixelCornerShape(null, percent, unit)
    }
}

/** Drop-in for `RoundedCornerShape(radius)`. */
fun PixelCornerShape(radius: Dp): Shape = PixelCornerShape.radius(radius)

/** Drop-in for `RoundedCornerShape(percent)` (50 = pill or circle). */
fun PixelCornerShape(percent: Int): Shape = PixelCornerShape.percent(percent)

/** A circle drawn as pixel steps, for round buttons and badges. */
val PixelCircleShape: Shape = PixelCornerShape.percent(50)
