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
fun drawMochiMeter(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float) {
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
fun drawKeepsakeShelf(scope: DrawScope, shelf: List<String>, cw: Float, ch: Float, p: Float) {
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
    // Small finds take a narrow spot; dishes, catches and bouquets are wider.
    var x = left + 2f * p
    for (item in shelf) {
        val w = keepsakeSprite(item)?.width ?: 4
        if (x + w * p > left + width) break
        drawKeepsake(scope, item, x, y, p)
        x += (w + 1.4f) * p
    }
}

/** A keepsake as a tiny pixel sprite, standing on [baseY] with its left edge at [x]. */
fun drawKeepsake(scope: DrawScope, item: String, x: Float, baseY: Float, p: Float, silhouette: Color? = null) {
    keepsakeSprite(item)?.let { sprite ->
        com.example.games.CozySprites.draw(scope, sprite, x, baseY - sprite.height * p, p, com.example.games.CozySprites.BOUQUET_COLORS, silhouette = silhouette)
        return
    }
    fun px(cx: Int, cy: Int, w: Int, h: Int, c: Color) = scope.drawRect(silhouette ?: c, Offset(x + cx * p, baseY - (cy + h) * p), Size(w * p, h * p))
    when (item.substringAfter(":")) {
        "WILDFLOWER" -> { px(2, 0, 1, 3, Color(0xFF3F7A4A)); px(1, 3, 3, 1, Color(0xFFE88AA8)); px(2, 4, 1, 1, Color(0xFFF6BD60)); px(1, 4, 1, 1, Color(0xFFE88AA8)); px(3, 4, 1, 1, Color(0xFFE88AA8)) }
        "RED_LEAF" -> { px(1, 0, 1, 1, Color(0xFF7A4A1E)); px(1, 1, 3, 2, Color(0xFFD9480F)); px(2, 3, 1, 1, Color(0xFFE76F51)) }
        "LOVE_NOTE" -> { px(0, 0, 4, 3, Color(0xFFFFF4E6)); px(1, 1, 2, 1, Color(0xFFE88AA8)) }
        "SEASHELL" -> { px(0, 0, 4, 1, Color(0xFFF5C6A5)); px(1, 1, 2, 2, Color(0xFFFADBC6)); px(2, 3, 1, 1, Color(0xFFF5C6A5)) }
        "STAR_PEBBLE" -> { px(0, 0, 4, 2, Color(0xFF6C757D)); px(1, 1, 1, 1, Color(0xFFFFD166)); px(2, 2, 1, 1, Color(0xFF8D99AE)) }
        // The festivals and the jar, for the collection book (plan 09, H).
        "BLOSSOM_PICNIC" -> { px(0, 0, 4, 1, Color(0xFF3F7A4A)); px(0, 1, 1, 1, Color(0xFFFF8FAB)); px(2, 1, 1, 1, Color(0xFFFFD166)); px(3, 1, 1, 1, Color(0xFFB497E7)) }
        "LANTERN_NIGHT" -> { px(1, 0, 2, 1, Color(0xFF7A4A1E)); px(0, 1, 4, 2, Color(0xFFE76F51)); px(1, 2, 2, 1, Color(0xFFFFD166)); px(1, 3, 2, 1, Color(0xFF7A4A1E)) }
        "GIFT_EXCHANGE" -> { px(0, 0, 4, 3, Color(0xFF52B788)); px(1, 0, 1, 3, Color(0xFFE63946)); px(0, 3, 1, 1, Color(0xFFE63946)); px(2, 3, 1, 1, Color(0xFFE63946)) }
        // The Friday fox's face, and its ball (plan 10, D)
        "VISIT" -> { px(0, 3, 1, 1, Color(0xFF3D2C2E)); px(3, 3, 1, 1, Color(0xFF3D2C2E)); px(0, 0, 4, 3, Color(0xFFE8743B)); px(1, 0, 2, 1, Color(0xFFFFF4E6)); px(1, 1, 1, 1, Color(0xFF3D2C2E)); px(2, 1, 1, 1, Color(0xFF3D2C2E)) }
        "BALL" -> { px(1, 0, 1, 1, Color(0xFFE63946)); px(0, 1, 3, 1, Color(0xFFE63946)); px(1, 1, 1, 1, Color(0xFFFFD166)); px(1, 2, 1, 1, Color(0xFFE63946)) }
        "THANK_YOU" -> { px(0, 0, 4, 3, Color(0xFFBDE0FE)); px(1, 1, 1, 1, Color(0xFFFF8FAB)); px(2, 0, 1, 1, Color(0xFFFFD166)); px(0, 3, 4, 1, Color(0xFF7A4A1E)) }
        else -> px(0, 0, 3, 3, Color(0xFFFFB5C2))
    }
}

/** The pixel art for keepsakes made or caught in the cozy games (plan 07, C3-C5), or null. */
fun keepsakeSprite(item: String): com.example.games.CozySprites.Sprite? {
    val kind = item.substringAfter(":")
    return when {
        item.startsWith("dish:") -> com.example.games.CozySprites.DISHES[kind]
        item.startsWith("catch:") -> com.example.games.FishingCatch.entries.firstOrNull { it.name == kind }?.let { com.example.games.CozySprites.CATCHES[it] }
        item == com.example.progress.Gifts.BOUQUET -> com.example.games.CozySprites.BOUQUET
        // A crop from the garden, for the collection book (plan 09, H).
        item.startsWith("crop:") -> com.example.games.Ingredient.entries.firstOrNull { it.name == kind }?.let { com.example.games.CozySprites.INGREDIENTS[it] }
        else -> null
    }
}
