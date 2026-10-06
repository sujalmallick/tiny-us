package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.ThankYouJar
import com.example.scene.KitchenLayout
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlin.math.sin

/*
 * Plan 09, C in the world: the Thank-You Jar on the kitchen wall, and the Make-Up Bench's little
 * grey cloud over each of them, which clears to a rainbow.
 */

private val PEBBLES = listOf(Color(0xFFFF8FAB), Color(0xFFFFD166), Color(0xFF8ECAE6), Color(0xFFB497E7), Color(0xFF6BBF59))

fun drawCoupleLife(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float) {
    val store = engine.coupleLifeStore
    if (store != null && engine.currentScene == SceneType.COOKING) {
        drawThankYouJar(scope, KitchenLayout.thankYouJar(cw, ch, p), store.thankYous().size, p)
    }
    if (engine.makeUpActive) drawMakeUpWeather(scope, engine, cw, ch, p)
}

/** A glass jar on a little shelf, filling with coloured pebbles, one per thank-you. */
fun drawThankYouJar(scope: DrawScope, base: Offset, count: Int, p: Float) {
    val wood = Color(0xFF8B5A2B)
    val woodDark = Color(0xFF6B4423)
    // The little shelf
    scope.drawRect(wood, Offset(base.x - 7f * p, base.y), Size(14f * p, 2f * p))
    scope.drawRect(woodDark, Offset(base.x - 7f * p, base.y + 2f * p), Size(14f * p, p))
    scope.drawRect(woodDark, Offset(base.x - 5f * p, base.y + 3f * p), Size(p, 2f * p))
    scope.drawRect(woodDark, Offset(base.x + 4f * p, base.y + 3f * p), Size(p, 2f * p))
    // The glass, 8 wide and 10 tall, with a cork lid and a ribbon
    val left = base.x - 4f * p
    val top = base.y - 10f * p
    val glass = Color(0xFFDDEFF5).copy(alpha = 0.55f)
    val rim = Color(0xFFB8D8E3)
    scope.drawRect(glass, Offset(left, top), Size(8f * p, 10f * p))
    scope.drawRect(rim, Offset(left, top), Size(p, 10f * p))
    scope.drawRect(rim, Offset(left + 7f * p, top), Size(p, 10f * p))
    scope.drawRect(rim, Offset(left, top + 9f * p), Size(8f * p, p))
    // Pebbles: 6 across, filling from the bottom up to 8 rows (FULL = 20 thank-yous)
    val shown = (count.coerceIn(0, ThankYouJar.FULL) * 24 / ThankYouJar.FULL)
    for (i in 0 until shown) {
        val col = i % 6
        val row = i / 6
        scope.drawRect(PEBBLES[(i * 7) % PEBBLES.size], Offset(left + (1 + col) * p, top + (8 - row * 2) * p), Size(p, 2f * p))
    }
    scope.drawRect(Color(0xFFC9A36B), Offset(left - p, top - 2f * p), Size(10f * p, 2f * p)) // cork
    scope.drawRect(Color(0xFFE5677F), Offset(left, top), Size(8f * p, p)) // ribbon
    scope.drawRect(Color(0xFFFFFFFF).copy(alpha = 0.6f), Offset(left + 2f * p, top + 2f * p), Size(p, 4f * p)) // shine
}

/** A small rain cloud over each of them; as it clears, a rainbow arcs between them. */
private fun drawMakeUpWeather(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, p: Float) {
    val cloud = engine.makeUpCloud
    val t = engine.sceneTime
    if (cloud > 0f) {
        for (c in listOf(engine.boy, engine.girl)) {
            val x = cw * c.worldX
            val y = ch * c.worldY - 64f * p + sin(t * 1.4f + c.worldX * 9f) * p
            val grey = Color(0xFF8D99AE).copy(alpha = 0.9f * cloud)
            val light = Color(0xFFB8C1CF).copy(alpha = 0.9f * cloud)
            scope.drawRect(grey, Offset(x - 9f * p, y), Size(18f * p, 5f * p))
            scope.drawRect(grey, Offset(x - 6f * p, y - 3f * p), Size(8f * p, 3f * p))
            scope.drawRect(light, Offset(x - 1f * p, y - 4f * p), Size(7f * p, 4f * p))
            // A few slow drops
            val drop = Color(0xFF8ECAE6).copy(alpha = 0.8f * cloud)
            for (k in 0 until 3) {
                val fall = ((t * 18f + k * 7f) % 14f) * p
                scope.drawRect(drop, Offset(x - 6f * p + k * 6f * p, y + 6f * p + fall), Size(p, 2f * p))
            }
        }
    }
    val bow = engine.makeUpRainbow
    if (bow > 0f) {
        val mid = cw * (engine.boy.worldX + engine.girl.worldX) / 2f
        val baseY = ch * engine.boy.worldY - 40f * p
        val colors = listOf(Color(0xFFE63946), Color(0xFFF4A261), Color(0xFFFFD166), Color(0xFF6BBF59), Color(0xFF8ECAE6), Color(0xFFB497E7))
        colors.forEachIndexed { i, c ->
            val r = (46f - i * 3f) * p
            scope.drawArc(
                color = c.copy(alpha = 0.75f * bow),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(mid - r, baseY - r), size = Size(r * 2f, r * 2f),
                style = Stroke(width = 3f * p)
            )
        }
    }
}
