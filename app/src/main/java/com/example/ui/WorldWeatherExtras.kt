package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.SceneEngine
import com.example.scene.WeatherLayout
import kotlin.math.cos
import kotlin.math.sin

private val RainbowBands = arrayOf(
    Color(0xFFFF8A8A), Color(0xFFFFB86B), Color(0xFFFFE27A),
    Color(0xFF9EE493), Color(0xFF8EC5FF), Color(0xFFC3A6FF)
)

/** Outdoor weather keepsakes, drawn behind the couple: the rainbow after rain and the snowday snowman. */
internal fun drawWeatherKeepsakes(scope: DrawScope, cw: Float, ch: Float, p: Float, engine: SceneEngine) {
    if (engine.rainbowTimer > 0f) drawRainbow(scope, cw, ch, p, engine.rainbowTimer)
    if (engine.snowmanStage > 0) drawSnowman(scope, cw, ch, p, engine.snowmanStage, engine.snowmanWobbleTimer, engine.sceneTime)
}

/** A soft pixel rainbow that fades in, lingers and fades away. */
private fun drawRainbow(scope: DrawScope, cw: Float, ch: Float, p: Float, timer: Float) {
    val elapsed = WeatherLayout.RAINBOW_SECONDS - timer
    val fade = minOf(elapsed / 2.5f, timer / 5f, 1f).coerceIn(0f, 1f) * 0.55f
    if (fade <= 0f) return
    val center = WeatherLayout.rainbowCenter(cw, ch)
    val outer = WeatherLayout.rainbowOuterRadius(cw)
    val bandW = WeatherLayout.rainbowBandWidth(cw) / RainbowBands.size
    val block = (bandW * 0.9f).coerceAtLeast(p)
    for (band in RainbowBands.indices) {
        val r = outer - bandW * (band + 0.5f)
        val color = RainbowBands[band].copy(alpha = fade)
        // Square blocks stepped along the arc keep the rainbow in the pixel-art style.
        val steps = (Math.PI.toFloat() * r / block).toInt().coerceAtLeast(12)
        for (i in 0..steps) {
            val a = Math.PI.toFloat() * i / steps
            val x = center.x - cos(a) * r
            val y = center.y - sin(a) * r
            scope.drawRect(color, Offset(x - block / 2f, y - block / 2f), Size(block, block))
        }
    }
}

/** The snowman grows in stages as the couple catch snowflakes; it wobbles when tapped. */
private fun drawSnowman(scope: DrawScope, cw: Float, ch: Float, p: Float, stage: Int, wobbleTimer: Float, time: Float) {
    val base = WeatherLayout.snowmanBase(cw, ch)
    val wobble = if (wobbleTimer > 0f) sin(time * 30f) * 1.2f * p * (wobbleTimer / 0.8f) else 0f
    val snow = Color(0xFFF7FBFF)
    val shade = Color(0xFFD6E4F0)

    scope.drawOval(Color(0x33000000), Offset(base.x - 10f * p, base.y - 1.5f * p), Size(20f * p, 3f * p))
    // Base
    scope.drawCircle(shade, 7.5f * p, Offset(base.x, base.y - 6.5f * p))
    scope.drawCircle(snow, 7f * p, Offset(base.x - 0.6f * p, base.y - 7f * p))
    if (stage >= 2) {
        val mx = base.x + wobble * 0.5f
        scope.drawCircle(shade, 5.5f * p, Offset(mx, base.y - 16f * p))
        scope.drawCircle(snow, 5f * p, Offset(mx - 0.5f * p, base.y - 16.5f * p))
        // Button stones
        scope.drawRect(Color(0xFF4A4E57), Offset(mx - 0.6f * p, base.y - 18f * p), Size(1.2f * p, 1.2f * p))
        scope.drawRect(Color(0xFF4A4E57), Offset(mx - 0.6f * p, base.y - 15f * p), Size(1.2f * p, 1.2f * p))
    }
    if (stage >= 3) {
        val hx = base.x + wobble
        val hy = base.y - 24f * p
        scope.drawCircle(shade, 4f * p, Offset(hx, hy))
        scope.drawCircle(snow, 3.6f * p, Offset(hx - 0.4f * p, hy - 0.4f * p))
        scope.drawRect(Color(0xFF2B2B2B), Offset(hx - 1.8f * p, hy - 1.2f * p), Size(0.9f * p, 0.9f * p))
        scope.drawRect(Color(0xFF2B2B2B), Offset(hx + 0.9f * p, hy - 1.2f * p), Size(0.9f * p, 0.9f * p))
        scope.drawRect(Color(0xFF2B2B2B), Offset(hx - 1.2f * p, hy + 1.2f * p), Size(2.4f * p, 0.6f * p))
        if (stage >= 4) {
            // Carrot nose, scarf and a twig arm waving
            scope.drawRect(Color(0xFFF4A261), Offset(hx, hy - 0.2f * p), Size(3f * p, 1f * p))
            scope.drawRect(Color(0xFFE63946), Offset(hx - 4f * p, hy + 3f * p), Size(8f * p, 1.8f * p))
            scope.drawRect(Color(0xFFE63946), Offset(hx + 1.5f * p, hy + 4.5f * p), Size(1.8f * p, 4f * p))
            val wave = sin(time * 3f) * 1.5f * p
            scope.drawLine(Color(0xFF6D4C41), Offset(base.x + 4.5f * p, base.y - 17f * p), Offset(base.x + 10f * p, base.y - 22f * p + wave), strokeWidth = 0.9f * p)
            scope.drawLine(Color(0xFF6D4C41), Offset(base.x - 4.5f * p, base.y - 17f * p), Offset(base.x - 9.5f * p, base.y - 20f * p), strokeWidth = 0.9f * p)
        }
    }
}

/** Rain puddles on the ground: they fill while it rains, ripple under the drops, and dry slowly. */
internal fun drawPuddles(scope: DrawScope, cw: Float, ch: Float, p: Float, engine: SceneEngine, isNight: Boolean) {
    val unit = WeatherLayout.weatherUnit(cw, p)
    val raining = engine.weather == com.example.scene.WeatherType.RAIN
    val time = engine.sceneTime
    val water = if (isNight) Color(0xFF2C3E5C) else Color(0xFF7FA6C9)
    val shine = if (isNight) Color(0xFF8FA7D9) else Color(0xFFE3F2FF)
    for ((index, puddle) in engine.particles.puddles.withIndex()) {
        if (puddle.size <= 0.02f) continue
        val halfW = engine.particles.puddleHalfWidth(unit, puddle.size)
        val halfH = halfW * 0.38f
        val cx = cw * puddle.normX
        val cy = ch * puddle.normY
        val a = (0.35f + puddle.size * 0.4f).coerceAtMost(0.75f)
        scope.drawOval(water.copy(alpha = a), Offset(cx - halfW, cy - halfH), Size(halfW * 2f, halfH * 2f))
        // A sliver of reflected sky
        scope.drawRect(shine.copy(alpha = a * 0.6f), Offset(cx - halfW * 0.45f, cy - halfH * 0.35f), Size(halfW * 0.5f, unit * 0.5f))
        if (raining) {
            for (r in 0..1) {
                val phase = ((time * 0.8f + index * 0.37f + r * 0.5f) % 1f)
                val rx = cx + (if (r == 0) -0.3f else 0.35f) * halfW
                val ringW = unit * (1f + phase * 4f)
                scope.drawOval(
                    shine.copy(alpha = (1f - phase) * 0.55f),
                    Offset(rx - ringW, cy - ringW * 0.38f), Size(ringW * 2f, ringW * 0.76f),
                    style = PuddleRingStroke
                )
            }
        }
    }
}

private val PuddleRingStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)

/** Footprints, pawprints and finger traces in the snow, fading as fresh snow covers them. */
internal fun drawSnowPrints(scope: DrawScope, cw: Float, ch: Float, p: Float, prints: List<com.example.engine.SnowPrint>) {
    if (prints.isEmpty()) return
    val unit = WeatherLayout.weatherUnit(cw, p)
    val dent = Color(0xFF9FB3C8)
    for (i in prints.indices) {
        val print = prints[i]
        val fade = (1f - print.age / com.example.engine.ParticleSystem.SNOW_PRINT_SECONDS).coerceIn(0f, 1f)
        val color = dent.copy(alpha = 0.55f * fade)
        val x = print.normX * cw
        val y = print.normY * ch
        when (print.kind) {
            com.example.engine.SnowPrintKind.FOOT -> {
                // A left and right boot print, slightly staggered in the walking direction
                val step = if (print.facingLeft) -unit * 0.6f else unit * 0.6f
                scope.drawOval(color, Offset(x - unit * 1.4f, y - unit * 0.35f), Size(unit * 1.1f, unit * 0.6f))
                scope.drawOval(color, Offset(x + unit * 0.3f + step, y), Size(unit * 1.1f, unit * 0.6f))
            }
            com.example.engine.SnowPrintKind.PAW -> {
                scope.drawCircle(color, unit * 0.32f, Offset(x, y))
                scope.drawCircle(color, unit * 0.16f, Offset(x - unit * 0.35f, y - unit * 0.38f))
                scope.drawCircle(color, unit * 0.16f, Offset(x, y - unit * 0.5f))
                scope.drawCircle(color, unit * 0.16f, Offset(x + unit * 0.35f, y - unit * 0.38f))
            }
            com.example.engine.SnowPrintKind.TRACE -> {
                scope.drawCircle(color.copy(alpha = 0.7f * fade), unit * 0.65f, Offset(x, y))
            }
        }
    }
}
