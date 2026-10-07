package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The one shadow colour: a cool near-black at a fixed low alpha, so the same shadow reads as a
 * shadow on grass, wood, tile and snow, by day, sunset and night, without tinting any of them.
 */
val ContactShadowColor: Color = Color(0x40101828)

/** Row half-widths per (width, height), worked out once and reused every frame. */
private val contactShadowSpans = HashMap<Int, IntArray>()

private fun spansFor(w: Int, h: Int): IntArray {
    val key = w * 8 + h
    contactShadowSpans[key]?.let { return it }
    val rx = w / 2f
    val ry = h / 2f
    val spans = IntArray(h) { r ->
        val dy = (r - (h - 1) / 2f) / ry
        val t = 1f - dy * dy
        if (t <= 0f) 0 else (rx * sqrt(t)).roundToInt()
    }
    contactShadowSpans[key] = spans
    return spans
}

/**
 * A small flat pixel ellipse where something meets the ground (Plan: depth, part 1). It replaces the
 * old smooth [DrawScope.drawOval] shadow so the shadow lands on the same pixel grid as the thing it
 * sits under: [p] is that thing's pixel size and [widthPx] the shadow's width in those pixels, a
 * touch wider than the footprint. The middle is solid; the outermost pixel of each row is dropped on
 * a checkerboard, so the edge dithers instead of looking smooth. The height stays 2-4 pixels.
 *
 * Draw this UNDER the object (before its sprite), never over it.
 */
fun drawContactShadow(
    scope: DrawScope,
    centerX: Float,
    groundY: Float,
    widthPx: Int,
    p: Float,
    color: Color = ContactShadowColor
) {
    if (widthPx < 2 || p <= 0f) return
    val h = (widthPx / 4).coerceIn(2, 4)
    val spans = spansFor(widthPx, h)
    val cx = (centerX / p).roundToInt()
    val cy = (groundY / p).roundToInt()
    val topRow = cy - h / 2
    for (r in 0 until h) {
        val half = spans[r]
        val row = topRow + r
        val rowY = row * p
        if (half <= 0) {
            if (((cx + row) and 1) == 0) scope.drawRect(color, Offset(cx * p, rowY), Size(p, p))
            continue
        }
        val coreLeft = cx - half + 1
        val coreRight = cx + half - 1
        if (coreRight >= coreLeft) {
            scope.drawRect(color, Offset(coreLeft * p, rowY), Size((coreRight - coreLeft + 1) * p, p))
        }
        val leftCol = cx - half
        val rightCol = cx + half
        if (((leftCol + row) and 1) == 0) scope.drawRect(color, Offset(leftCol * p, rowY), Size(p, p))
        if (((rightCol + row) and 1) == 0) scope.drawRect(color, Offset(rightCol * p, rowY), Size(p, p))
    }
}
