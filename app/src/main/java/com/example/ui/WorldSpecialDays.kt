package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.engine.SpecialDay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Special-day decorations (plan 06, G2): a garland across the top of every scene, plus a light
 * touch for the day. Everything is drawn from the scene clock, so nothing in the engine changes.
 * World units, stage origin, like the rest of the scene.
 */

private val BUNTING = listOf(Color(0xFFF28482), Color(0xFFF6BD60), Color(0xFF84C7A5), Color(0xFF7FB3E0), Color(0xFFB79CED))
private val HEARTS = listOf(Color(0xFFE63E62), Color(0xFFF5A3B8), Color(0xFFD6336C))
private val BULBS = listOf(Color(0xFFE63946), Color(0xFF52B788), Color(0xFFFFC94D), Color(0xFF4EA8DE))
private val HOLI_COLOURS = listOf(Color(0xFFFF4FA3), Color(0xFF3DDC84), Color(0xFFFFD23F), Color(0xFF3FA7FF), Color(0xFFA463F2))
private val SPARKLE = listOf(Color(0xFFFFD700), Color(0xFFE5E4E2), Color(0xFFF28482), Color(0xFF7FB3E0))

internal fun drawSpecialDayDecor(scope: DrawScope, day: SpecialDay, cw: Float, ch: Float, p: Float, time: Float, isNight: Boolean) {
    val top = 6f * p
    val sag = 7f * p
    fun ropeY(x: Float) = top + sag * sin(PI.toFloat() * (x / cw).coerceIn(0f, 1f))

    // The rope itself.
    val rope = if (day == SpecialDay.DIWALI) Color(0xFF7A4A1E) else Color(0xFF8C6D5A)
    var x = 0f
    while (x < cw) {
        scope.drawRect(rope, Offset(x, ropeY(x)), Size(2f * p, p))
        x += 2f * p
    }

    val count = 9
    for (i in 0 until count) {
        val hx = cw * (i + 0.5f) / count
        val hy = ropeY(hx) + p
        when (day) {
            SpecialDay.BOY_BIRTHDAY, SpecialDay.GIRL_BIRTHDAY, SpecialDay.HOLI ->
                pennant(scope, hx, hy, p, (if (day == SpecialDay.HOLI) HOLI_COLOURS else BUNTING)[i % 5])
            SpecialDay.ANNIVERSARY, SpecialDay.VALENTINES -> pixelHeart(scope, hx - 2.5f * p, hy, p, HEARTS[i % HEARTS.size])
            SpecialDay.NEW_YEAR -> {
                scope.drawRect(Color(0xFF8C6D5A), Offset(hx, hy), Size(p, 2f * p))
                sparkleStar(scope, hx + 0.5f * p, hy + 4f * p, p, SPARKLE[i % 2], twinkle = sin(time * 3f + i) > 0.3f)
            }
            SpecialDay.CHRISTMAS -> {
                val lit = sin(time * 2.5f + i * 1.7f) > -0.3f
                val c = BULBS[i % BULBS.size]
                scope.drawRect(Color(0xFF2D4A3E), Offset(hx - 0.5f * p, hy), Size(2f * p, p))
                scope.drawRect(if (lit) c else c.copy(alpha = 0.45f), Offset(hx - p, hy + p), Size(3f * p, 3f * p))
                if (lit && isNight) scope.drawCircle(c.copy(alpha = 0.18f), 5f * p, Offset(hx + 0.5f * p, hy + 2.5f * p))
            }
            SpecialDay.DIWALI -> {
                // A marigold string (toran): orange and yellow flowers, with a hanging lantern every third.
                for (k in 0..2) {
                    val fx = hx - 3f * p + k * 3f * p
                    marigold(scope, fx, ropeY(fx), p, if ((i + k) % 2 == 0) Color(0xFFFF8C1A) else Color(0xFFFFC425))
                }
                if (i % 3 == 1) lantern(scope, hx, hy + 2f * p, p, time, i, isNight)
            }
        }
    }

    // The day's own touch.
    when (day) {
        SpecialDay.BOY_BIRTHDAY, SpecialDay.GIRL_BIRTHDAY -> {
            balloon(scope, cw * 0.08f, ch * 0.20f + sin(time * 1.3f) * 2f * p, p, Color(0xFFF28482))
            balloon(scope, cw * 0.14f, ch * 0.24f + sin(time * 1.1f + 1f) * 2f * p, p, Color(0xFF7FB3E0))
            balloon(scope, cw * 0.90f, ch * 0.21f + sin(time * 1.2f + 2f) * 2f * p, p, Color(0xFFF6BD60))
        }
        SpecialDay.ANNIVERSARY, SpecialDay.VALENTINES -> {
            // A few hearts rising slowly and fading out.
            for (k in 0 until 5) {
                val period = 7f + k
                val t = ((time + k * 2.3f) % period) / period
                val hx = cw * (0.15f + 0.17f * k) + sin(time * 0.8f + k) * 3f * p
                val hy = ch * (0.62f - 0.45f * t)
                val alpha = (1f - t) * 0.9f
                pixelHeart(scope, hx, hy, p, HEARTS[k % HEARTS.size].copy(alpha = alpha))
            }
        }
        SpecialDay.NEW_YEAR -> {
            if (isNight) {
                // Fireworks: a burst every few seconds, at changing spots in the sky.
                for (k in 0 until 2) {
                    val period = 3.2f
                    val n = ((time + k * 1.6f) / period).toInt()
                    val t = ((time + k * 1.6f) % period) / period
                    if (t > 0.75f) continue
                    val bx = cw * (0.25f + 0.5f * (((n * 37 + k * 11) % 10) / 10f))
                    val by = ch * (0.14f + 0.08f * (((n * 53 + k * 7) % 10) / 10f))
                    val radius = (4f + 22f * t) * p
                    val c = SPARKLE[(n + k) % SPARKLE.size].copy(alpha = 1f - t / 0.75f)
                    for (s in 0 until 12) {
                        val a = s * (2f * PI.toFloat() / 12f)
                        scope.drawRect(c, Offset(bx + cos(a) * radius, by + sin(a) * radius), Size(2f * p, 2f * p))
                    }
                }
            } else {
                confetti(scope, cw, ch, p, time, SPARKLE + BUNTING)
            }
        }
        SpecialDay.HOLI -> {
            // Bursts of colour powder (gulal): specks thrown outward that drift up and fade.
            for (k in 0 until 5) {
                val period = 4.5f + k * 0.6f
                val n = ((time + k * 1.3f) / period).toInt()
                val t = ((time + k * 1.3f) % period) / period
                val cx = cw * (0.12f + 0.19f * k)
                val cy = ch * (0.40f + 0.06f * ((n + k) % 3))
                val c = HOLI_COLOURS[(k + n) % HOLI_COLOURS.size]
                val alpha = (1f - t) * 0.9f
                for (s in 0 until 14) {
                    // Each speck has its own angle and reach, fixed for this burst.
                    val a = (s * 2.4f + n * 0.7f + k)
                    val reach = (3f + 14f * t) * p * (0.6f + 0.4f * ((s * 7 + n) % 5) / 4f)
                    val sx = cx + cos(a) * reach
                    val sy = cy + sin(a) * reach * 0.7f - t * 8f * p
                    val size = if (s % 3 == 0) 2f * p else p
                    scope.drawRect(c.copy(alpha = alpha), Offset(sx, sy), Size(size, size))
                }
            }
            confetti(scope, cw, ch, p, time, HOLI_COLOURS)
        }
        SpecialDay.CHRISTMAS -> sparkleStar(scope, cw * 0.5f, ropeY(cw * 0.5f) - p, p * 1.5f, Color(0xFFFFD700), twinkle = sin(time * 2f) > 0f)
        SpecialDay.DIWALI -> Unit
    }
}

/** A triangular bunting flag hanging from the rope at ([cx], [y]). */
private fun pennant(scope: DrawScope, cx: Float, y: Float, p: Float, color: Color) {
    for (row in 0 until 6) {
        val w = (6 - row).toFloat()
        scope.drawRect(color, Offset(cx - w / 2f * p, y + row * p), Size(w * p, p))
    }
}

/** A 5 x 5 pixel heart with its top-left at ([x], [y]). */
private fun pixelHeart(scope: DrawScope, x: Float, y: Float, p: Float, color: Color) {
    scope.drawRect(color, Offset(x + p, y), Size(p, p))
    scope.drawRect(color, Offset(x + 3f * p, y), Size(p, p))
    scope.drawRect(color, Offset(x, y + p), Size(5f * p, 2f * p))
    scope.drawRect(color, Offset(x + p, y + 3f * p), Size(3f * p, p))
    scope.drawRect(color, Offset(x + 2f * p, y + 4f * p), Size(p, p))
}

/** A four-pointed star centred on ([cx], [cy]); brighter when [twinkle]. */
private fun sparkleStar(scope: DrawScope, cx: Float, cy: Float, p: Float, color: Color, twinkle: Boolean) {
    val c = if (twinkle) color else color.copy(alpha = 0.6f)
    scope.drawRect(c, Offset(cx - p, cy - p), Size(2f * p, 2f * p))
    if (twinkle) {
        scope.drawRect(c, Offset(cx - 0.5f * p, cy - 3f * p), Size(p, 2f * p))
        scope.drawRect(c, Offset(cx - 0.5f * p, cy + p), Size(p, 2f * p))
        scope.drawRect(c, Offset(cx - 3f * p, cy - 0.5f * p), Size(2f * p, p))
        scope.drawRect(c, Offset(cx + p, cy - 0.5f * p), Size(2f * p, p))
    }
}

/** A marigold on the toran string. */
private fun marigold(scope: DrawScope, cx: Float, cy: Float, p: Float, color: Color) {
    scope.drawRect(color, Offset(cx - 1.5f * p, cy - p), Size(3f * p, 3f * p))
    scope.drawRect(color.copy(red = color.red * 0.85f, green = color.green * 0.75f), Offset(cx - 0.5f * p, cy), Size(p, p))
}

/** A hanging paper lantern (akash kandil) with a flickering glow. */
private fun lantern(scope: DrawScope, cx: Float, y: Float, p: Float, time: Float, i: Int, isNight: Boolean) {
    val flicker = 0.85f + 0.15f * sin(time * 6f + i * 2f)
    scope.drawRect(Color(0xFF7A4A1E), Offset(cx, y), Size(p, 3f * p))
    val body = Color(0xFFFF6B35)
    scope.drawRect(body, Offset(cx - 2f * p, y + 3f * p), Size(5f * p, p))
    scope.drawRect(body, Offset(cx - 3f * p, y + 4f * p), Size(7f * p, 4f * p))
    scope.drawRect(body, Offset(cx - 2f * p, y + 8f * p), Size(5f * p, p))
    scope.drawRect(Color(0xFFFFD166).copy(alpha = flicker), Offset(cx - p, y + 5f * p), Size(3f * p, 2f * p))
    scope.drawRect(Color(0xFFFFC425), Offset(cx - 2f * p, y + 9f * p), Size(p, 3f * p)) // tassels
    scope.drawRect(Color(0xFFFFC425), Offset(cx + 2f * p, y + 9f * p), Size(p, 3f * p))
    if (isNight) scope.drawCircle(Color(0xFFFFB347).copy(alpha = 0.16f * flicker), 9f * p, Offset(cx + 0.5f * p, y + 6f * p))
}

/** A party balloon on a string, its top at ([cx], [y]). */
private fun balloon(scope: DrawScope, cx: Float, y: Float, p: Float, color: Color) {
    scope.drawRect(color, Offset(cx - 2f * p, y), Size(5f * p, p))
    scope.drawRect(color, Offset(cx - 3f * p, y + p), Size(7f * p, 5f * p))
    scope.drawRect(color, Offset(cx - 2f * p, y + 6f * p), Size(5f * p, p))
    scope.drawRect(color, Offset(cx - 0.5f * p, y + 7f * p), Size(2f * p, p))
    scope.drawRect(Color.White.copy(alpha = 0.55f), Offset(cx - 2f * p, y + 2f * p), Size(p, 2f * p)) // shine
    scope.drawRect(Color(0xFF8C6D5A), Offset(cx, y + 8f * p), Size(p, 10f * p)) // string
}

/** Confetti drifting down, in whole steps. */
private fun confetti(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, colours: List<Color>) {
    for (k in 0 until 24) {
        val speed = 14f + (k % 5) * 3f
        val startY = (k * 53f) % 100f / 100f
        val y = ((startY * ch * 0.7f + time * speed * p) % (ch * 0.7f)) + 10f * p
        val xx = cw * ((k * 37 % 100) / 100f) + sin(time * 1.5f + k) * 4f * p
        val c = colours[k % colours.size]
        if ((time * 4f + k).toInt() % 2 == 0) scope.drawRect(c, Offset(xx, y), Size(2f * p, p))
        else scope.drawRect(c, Offset(xx, y), Size(p, 2f * p))
    }
}
