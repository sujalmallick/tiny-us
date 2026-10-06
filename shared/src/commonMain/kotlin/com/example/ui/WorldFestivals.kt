package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.Festival
import com.example.data.FestivalPhase
import com.example.scene.SceneEngine
import kotlin.math.PI
import kotlin.math.sin

/*
 * Plan 09, D in the world: a poster before each festival, then its own decorations in its scene
 * (a picnic blanket, paper lanterns, a little tree with gifts), and the lanterns rising.
 */

fun drawFestival(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float) {
    val day = engine.festivalDay ?: return
    if (engine.currentScene != day.festival.scene) return
    val t = engine.sceneTime
    if (day.phase == FestivalPhase.POSTER) {
        drawFestivalPoster(scope, day.festival, cw * 0.06f, ch * 0.42f, p)
        return
    }
    when (day.festival) {
        Festival.BLOSSOM_PICNIC -> drawPicnic(scope, cw * 0.80f, ch * 0.80f, p)
        Festival.LANTERN_NIGHT -> {
            drawLanternString(scope, cw, ch * 0.40f, p, t)
            if (engine.lanternRise > 0f) {
                for (c in listOf(engine.boy, engine.girl)) {
                    val rise = engine.lanternRise
                    val x = cw * c.worldX + sin(rise * 0.9f + c.worldX * 7f) * 4f * p
                    val y = ch * c.worldY - 70f * p - rise * 22f * p
                    val fade = (1f - rise / SceneEngine.LANTERN_SECONDS).coerceIn(0f, 1f)
                    drawPaperLantern(scope, x, y, p, fade, glow = true)
                }
            }
        }
        Festival.GIFT_EXCHANGE -> drawGiftTree(scope, cw * SceneEngine.GiftTree.X, ch * SceneEngine.GiftTree.FLOOR_Y, p, t, engine.giftBoxesWaiting)
    }
}

/** A little paper poster pinned up two days before. */
private fun drawFestivalPoster(scope: DrawScope, festival: Festival, x: Float, y: Float, p: Float) {
    val paper = Color(0xFFFFF4DC)
    val accent = when (festival) {
        Festival.BLOSSOM_PICNIC -> Color(0xFFFF8FAB)
        Festival.LANTERN_NIGHT -> Color(0xFFF4A261)
        Festival.GIFT_EXCHANGE -> Color(0xFF6BBF59)
    }
    scope.drawRect(Color(0x33000000), Offset(x + p, y + p), Size(14f * p, 18f * p))
    scope.drawRect(paper, Offset(x, y), Size(14f * p, 18f * p))
    scope.drawRect(accent, Offset(x, y), Size(14f * p, 4f * p))
    scope.drawRect(Color(0xFFE63946), Offset(x + 6f * p, y - p), Size(2f * p, 2f * p)) // pin
    // A tiny picture of the festival and some "text" lines
    when (festival) {
        Festival.BLOSSOM_PICNIC -> { scope.drawRect(accent, Offset(x + 4f * p, y + 6f * p), Size(6f * p, 4f * p)); scope.drawRect(Color.White, Offset(x + 5f * p, y + 7f * p), Size(2f * p, 2f * p)) }
        Festival.LANTERN_NIGHT -> drawPaperLantern(scope, x + 7f * p, y + 7f * p, p * 0.6f, 1f, glow = false)
        Festival.GIFT_EXCHANGE -> { scope.drawRect(Color(0xFF7FB3E0), Offset(x + 4f * p, y + 6f * p), Size(6f * p, 5f * p)); scope.drawRect(Color(0xFFE63946), Offset(x + 6.5f * p, y + 6f * p), Size(p, 5f * p)) }
    }
    for (i in 0 until 3) scope.drawRect(Color(0xFFB8A99A), Offset(x + 3f * p, y + (12f + i * 2f) * p), Size((8f - i * 2f) * p, p))
}

/** A checked blanket with a basket and two cups, for the Blossom Picnic. */
private fun drawPicnic(scope: DrawScope, cx: Float, base: Float, p: Float) {
    val w = 30f * p
    val h = 9f * p
    val left = cx - w / 2f
    scope.drawRect(Color(0x33000000), Offset(left + p, base - h + p), Size(w, h))
    for (row in 0 until 3) for (col in 0 until 10) {
        val red = (row + col) % 2 == 0
        scope.drawRect(if (red) Color(0xFFE5677F) else Color(0xFFFFF4EC), Offset(left + col * 3f * p, base - h + row * 3f * p), Size(3f * p, 3f * p))
    }
    // Basket
    scope.drawRect(Color(0xFFC9A36B), Offset(cx + 4f * p, base - h - 5f * p), Size(9f * p, 6f * p))
    scope.drawRect(Color(0xFFA9824C), Offset(cx + 4f * p, base - h - 3f * p), Size(9f * p, p))
    scope.drawRect(Color(0xFF8C6A3E), Offset(cx + 6f * p, base - h - 8f * p), Size(5f * p, p))
    scope.drawRect(Color(0xFF8C6A3E), Offset(cx + 6f * p, base - h - 8f * p), Size(p, 3f * p))
    scope.drawRect(Color(0xFF8C6A3E), Offset(cx + 10f * p, base - h - 8f * p), Size(p, 3f * p))
    // Two cups and a little cake
    scope.drawRect(Color(0xFFF7F0E3), Offset(cx - 10f * p, base - h - 3f * p), Size(3f * p, 3f * p))
    scope.drawRect(Color(0xFFF7F0E3), Offset(cx - 5f * p, base - h - 3f * p), Size(3f * p, 3f * p))
    scope.drawRect(Color(0xFFF4C27A), Offset(cx - 1f * p, base - h - 3f * p), Size(4f * p, 3f * p))
    scope.drawRect(Color(0xFFFFF1F5), Offset(cx - 1f * p, base - h - 3f * p), Size(4f * p, p))
}

/** A string of paper lanterns across the pier for Lantern Night. */
private fun drawLanternString(scope: DrawScope, cw: Float, y: Float, p: Float, t: Float) {
    val rope = Color(0xFF8C6D5A)
    val sag = 8f * p
    fun ropeY(x: Float) = y + sag * sin(PI.toFloat() * (x / cw).coerceIn(0f, 1f))
    var x = 0f
    while (x < cw) {
        scope.drawRect(rope, Offset(x, ropeY(x)), Size(2f * p, p))
        x += 2f * p
    }
    val count = 7
    for (i in 0 until count) {
        val lx = cw * (i + 0.5f) / count
        val flicker = 0.85f + 0.15f * sin(t * 3f + i * 1.7f)
        drawPaperLantern(scope, lx, ropeY(lx) + 4f * p, p, flicker, glow = true)
    }
}

/** One paper lantern centred on [cx], top at [top]; [alpha] fades it out as it rises. */
fun drawPaperLantern(scope: DrawScope, cx: Float, top: Float, p: Float, alpha: Float, glow: Boolean) {
    if (alpha <= 0f) return
    val body = Color(0xFFF4A261).copy(alpha = alpha)
    val bright = Color(0xFFFFD166).copy(alpha = alpha)
    val rim = Color(0xFFB5543A).copy(alpha = alpha)
    if (glow) scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.22f * alpha), 7f * p, Offset(cx, top + 4f * p))
    scope.drawRect(rim, Offset(cx - 2f * p, top), Size(4f * p, p))
    scope.drawRect(body, Offset(cx - 3f * p, top + p), Size(6f * p, 6f * p))
    scope.drawRect(bright, Offset(cx - p, top + 2f * p), Size(2f * p, 4f * p))
    scope.drawRect(rim, Offset(cx - 2f * p, top + 7f * p), Size(4f * p, p))
}

/** A little decorated tree for the Gift Exchange, with the wrapped gifts under it until opened. */
private fun drawGiftTree(scope: DrawScope, cx: Float, base: Float, p: Float, t: Float, gifts: Boolean) {
    val green = Color(0xFF2F6B3F)
    val light = Color(0xFF3F8052)
    scope.drawRect(Color(0x33000000), Offset(cx - 9f * p, base - p), Size(18f * p, 2f * p))
    scope.drawRect(Color(0xFF8B5A2B), Offset(cx - 2f * p, base - 5f * p), Size(4f * p, 5f * p)) // trunk
    scope.drawRect(Color(0xFFC0392B), Offset(cx - 4f * p, base - 3f * p), Size(8f * p, 3f * p)) // pot
    for (layer in 0 until 4) {
        val w = (18f - layer * 4f) * p
        val y = base - (9f + layer * 6f) * p
        scope.drawRect(if (layer % 2 == 0) green else light, Offset(cx - w / 2f, y), Size(w, 6f * p))
    }
    scope.drawRect(Color(0xFFFFD166), Offset(cx - p, base - 36f * p), Size(2f * p, 2f * p)) // star
    scope.drawRect(Color(0xFFFFD166), Offset(cx - 2f * p, base - 35f * p), Size(4f * p, p))
    val bulbs = listOf(Color(0xFFE63946), Color(0xFF8ECAE6), Color(0xFFFFD166), Color(0xFFFF8FAB))
    for (i in 0 until 8) {
        val on = sin(t * 2.4f + i * 1.3f) > -0.2f
        val bx = cx + ((i % 4) - 1.5f) * 3.5f * p
        val by = base - (11f + (i / 2) * 6f) * p
        scope.drawRect(if (on) bulbs[i % bulbs.size] else bulbs[i % bulbs.size].copy(alpha = 0.4f), Offset(bx, by), Size(p, p))
    }
    if (!gifts) return
    for ((i, color) in listOf(Color(0xFF7FB3E0), Color(0xFFFF8FAB)).withIndex()) {
        val gx = cx + (if (i == 0) -12f else 5f) * p
        scope.drawRect(color, Offset(gx, base - 6f * p), Size(7f * p, 6f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(gx + 3f * p, base - 6f * p), Size(p, 6f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(gx + 2f * p, base - 8f * p), Size(3f * p, 2f * p))
    }
}
