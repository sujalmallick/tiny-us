package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Pixel-art light (depth, part 3). Glows used to be smooth circles with soft anti-aliased edges
 * at any sub-pixel spot; here every glow is a disc of whole pixels on the scene's grid, with its
 * outermost ring on a checkerboard so the light fades in a step instead of a blur. Stacking two or
 * three of them (as the lamps do) gives the classic stepped pool of light.
 */

/** Half-widths of each row of a pixel disc of radius [r] (rows -r..r), squashed vertically by [squash]. */
private val discSpans = HashMap<Long, IntArray>()

private fun spans(r: Int, squash: Float): IntArray {
    val rows = (r * squash).roundToInt().coerceAtLeast(1)
    val key = r.toLong() * 100_000L + rows
    discSpans[key]?.let { return it }
    val out = IntArray(rows * 2 + 1) { i ->
        val dy = (i - rows) / rows.toFloat()
        val t = 1f - dy * dy
        if (t < 0f) -1 else (r * sqrt(t)).roundToInt()
    }
    discSpans[key] = out
    return out
}

/**
 * A glow of [color] filling a pixel disc of [radius] (screen px) around [center], on the grid of
 * pixel size [p]. Its rim (the last pixel of each row, and the top and bottom rows) dithers. With
 * [squash] below 1 it lies flat, as a pool of light on the ground.
 */
fun drawPixelGlow(scope: DrawScope, color: Color, radius: Float, center: Offset, p: Float, squash: Float = 1f) {
    if (p <= 0f || color.alpha <= 0f) return
    val r = (radius / p).roundToInt()
    val cx = (center.x / p).roundToInt()
    val cy = (center.y / p).roundToInt()
    if (r < 1) {
        scope.drawRect(color, Offset(cx * p, cy * p), Size(p, p))
        return
    }
    val rowHalf = spans(r, squash)
    val rows = rowHalf.size / 2
    for (i in rowHalf.indices) {
        val half = rowHalf[i]
        if (half < 0) continue
        val row = cy - rows + i
        val y = row * p
        val edgeRow = i == 0 || i == rowHalf.size - 1
        if (edgeRow || half == 0) {
            for (col in cx - half..cx + half) {
                if (((col + row) and 1) == 0) scope.drawRect(color, Offset(col * p, y), Size(p, p))
            }
            continue
        }
        if (half >= 2) scope.drawRect(color, Offset((cx - half + 1) * p, y), Size((2 * half - 1) * p, p))
        else scope.drawRect(color, Offset(cx * p, y), Size(p, p))
        if (((cx - half + row) and 1) == 0) scope.drawRect(color, Offset((cx - half) * p, y), Size(p, p))
        if (((cx + half + row) and 1) == 0) scope.drawRect(color, Offset((cx + half) * p, y), Size(p, p))
    }
}

/**
 * A pool of [color] light on the ground around ([cx], [groundY]): three stepped flat rings, the
 * brightest in the middle, [radius] screen px across at the outside. For lamps and fires at night.
 */
fun drawGroundLightPool(scope: DrawScope, cx: Float, groundY: Float, radius: Float, p: Float, color: Color, alpha: Float) {
    val c = Offset(cx, groundY)
    drawPixelGlow(scope, color.copy(alpha = alpha * 0.40f), radius, c, p, squash = 0.28f)
    drawPixelGlow(scope, color.copy(alpha = alpha * 0.45f), radius * 0.66f, c, p, squash = 0.28f)
    drawPixelGlow(scope, color.copy(alpha = alpha * 0.55f), radius * 0.36f, c, p, squash = 0.28f)
}
