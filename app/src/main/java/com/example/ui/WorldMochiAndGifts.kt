package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.progress.MochiFondness
import com.example.scene.KitchenLayout
import com.example.scene.SceneEngine
import com.example.scene.SceneType

private val HeartRows = listOf(".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X..")

/**
 * Mochi's fondness (plan 07, D2): three little hearts over Mochi for a moment after a pet. Full
 * hearts are the levels reached; the next one fills from the bottom as fondness grows.
 */
internal fun drawMochiMeter(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float) {
    if (engine.mochiMeterTimer <= 0f) return
    val alpha = (engine.mochiMeterTimer / 0.6f).coerceIn(0f, 1f)
    val (mx, my) = if (engine.currentScene == SceneType.COZY_LOFT) {
        (cw * 0.38f + 14f * p) to (ch * 0.55f - 22f * p)
    } else {
        (cw * engine.catWorldX) to (ch * engine.catWorldY - 30f * p)
    }
    val level = MochiFondness.level(engine.mochiFondness)
    val toward = MochiFondness.towardNext(engine.mochiFondness)
    val full = Color(0xFFFF5D8F).copy(alpha = alpha)
    val empty = Color(0xFF6B5A66).copy(alpha = 0.55f * alpha)
    for (h in 0 until 3) {
        val x0 = mx - 10f * p + h * 7f * p
        // Rows filled from the bottom: all for reached levels, part of the next one.
        val filledRows = when {
            h < level -> 5
            h == level -> (toward * 5f).toInt()
            else -> 0
        }
        for ((r, row) in HeartRows.withIndex()) {
            for ((c, ch2) in row.withIndex()) {
                if (ch2 != 'X') continue
                val filled = r >= 5 - filledRows
                scope.drawRect(if (filled) full else empty, Offset(x0 + c * p, my + r * p), Size(p, p))
            }
        }
    }
}

/**
 * The keepsake shelf (plan 07, D3): a little wooden shelf on the kitchen wall under the clock,
 * holding the gifts the couple have given each other.
 */
internal fun drawKeepsakeShelf(scope: DrawScope, shelf: List<String>, cw: Float, ch: Float, p: Float) {
    if (shelf.isEmpty()) return
    val clock = KitchenLayout.clockCenter(cw, ch, p)
    val y = clock.y + 16f * p
    val left = clock.x - 14f * p
    val width = 28f * p
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF6B4423)
    scope.drawRect(wood, Offset(left, y), Size(width, 2f * p))
    scope.drawRect(woodDark, Offset(left, y + 2f * p), Size(width, p))
    scope.drawRect(woodDark, Offset(left + 3f * p, y + 3f * p), Size(p, 3f * p))
    scope.drawRect(woodDark, Offset(left + width - 4f * p, y + 3f * p), Size(p, 3f * p))
    shelf.take(5).forEachIndexed { i, item -> drawKeepsake(scope, item, left + 2f * p + i * 5.4f * p, y, p) }
}

/** A keepsake as a tiny pixel sprite, standing on [baseY] with its left edge at [x]. */
internal fun drawKeepsake(scope: DrawScope, item: String, x: Float, baseY: Float, p: Float) {
    fun px(cx: Int, cy: Int, w: Int, h: Int, c: Color) = scope.drawRect(c, Offset(x + cx * p, baseY - (cy + h) * p), Size(w * p, h * p))
    when (item.substringAfter(":")) {
        "WILDFLOWER" -> { px(2, 0, 1, 3, Color(0xFF3F7A4A)); px(1, 3, 3, 1, Color(0xFFE88AA8)); px(2, 4, 1, 1, Color(0xFFF6BD60)); px(1, 4, 1, 1, Color(0xFFE88AA8)); px(3, 4, 1, 1, Color(0xFFE88AA8)) }
        "RED_LEAF" -> { px(1, 0, 1, 1, Color(0xFF7A4A1E)); px(1, 1, 3, 2, Color(0xFFD9480F)); px(2, 3, 1, 1, Color(0xFFE76F51)) }
        "LOVE_NOTE" -> { px(0, 0, 4, 3, Color(0xFFFFF4E6)); px(1, 1, 2, 1, Color(0xFFE88AA8)) }
        "SEASHELL" -> { px(0, 0, 4, 1, Color(0xFFF5C6A5)); px(1, 1, 2, 2, Color(0xFFFADBC6)); px(2, 3, 1, 1, Color(0xFFF5C6A5)) }
        "STAR_PEBBLE" -> { px(0, 0, 4, 2, Color(0xFF6C757D)); px(1, 1, 1, 1, Color(0xFFFFD166)); px(2, 2, 1, 1, Color(0xFF8D99AE)) }
        else -> px(0, 0, 3, 3, Color(0xFFFFB5C2))
    }
}
