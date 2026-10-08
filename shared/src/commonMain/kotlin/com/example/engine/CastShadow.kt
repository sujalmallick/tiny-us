package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.WeatherType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The cast shadow colour: the same cool near-black as [ContactShadowColor], lighter, so where the
 * two overlap under a foot the ground darkens a step and the cast shadow reads as the paler part.
 */
val CastShadowColor: Color = Color(0x3A101828)

/**
 * The light a scene's cast shadows fall away from (depth, part 2).
 *
 * [sunProgress] is how far the sun has come across the sky, 0 rising on the left, 0.5 overhead and
 * 1 setting on the right (see the sky's celestialProgress). [lampX], when set, is a fire or lamp in
 * screen pixels: shadows fall away from it instead, by night or indoors.
 */
data class CastLight(
    val sunProgress: Float = 0.5f,
    val outdoor: Boolean = true,
    val night: Boolean = false,
    val weather: WeatherType = WeatherType.SUNNY,
    val lampX: Float? = null
)

/**
 * The light of the frame being drawn. The world sets it once per frame, before drawing anything,
 * so the couple, Mochi, the visitors and every prop throw their shadows the same way.
 */
object SceneLight {
    var current: CastLight = CastLight()
}

/**
 * A cast shadow's shape on the ground: its far end lands [tipDx] pixels to the side (+ right) and
 * [rows] pixel rows toward the viewer. The ground is seen at a low angle, so even a long evening
 * shadow is only a few rows deep and stretches mostly sideways.
 */
data class CastShadowShape(val tipDx: Int, val rows: Int) {
    val visible: Boolean get() = rows > 0

    companion object {
        val NONE = CastShadowShape(0, 0)
    }
}

/**
 * Where the shadow of something [widthPx] wide and [heightPx] tall (in its own pixels) standing at
 * [centerX] falls under [light]:
 * - Outdoors by day it points away from the sun: right in the morning, left at sunset, short and
 *   underfoot at noon, and longest when the sun is low.
 * - Overcast (rain, snow) and moonlit nights: none. The contact shadow alone grounds things then.
 * - A fire or lamp ([CastLight.lampX]): short, falling away from it.
 * - Indoors with no lamp: a short soft shadow, a little to the right of the room's window light.
 */
fun castShadowShape(light: CastLight, widthPx: Int, heightPx: Int, centerX: Float): CastShadowShape {
    if (widthPx < 2 || heightPx < 1) return CastShadowShape.NONE
    light.lampX?.let { lampX ->
        val away = if (centerX >= lampX) 1 else -1
        return CastShadowShape(away * (heightPx * 0.35f).roundToInt().coerceIn(2, 10), 2)
    }
    if (!light.outdoor) return CastShadowShape((heightPx * 0.12f).roundToInt().coerceIn(1, 4), 2)
    if (light.night || light.weather == WeatherType.RAIN || light.weather == WeatherType.SNOW) {
        return CastShadowShape.NONE
    }
    // +1 with the sun rising on the left (the shadow points right) to -1 setting on the right
    val side = (0.5f - light.sunProgress.coerceIn(0f, 1f)) * 2f
    val low = abs(side)
    return CastShadowShape((side * heightPx * 0.7f).roundToInt(), 2 + (low * 3f).roundToInt())
}

/**
 * The shadow something casts across the ground (depth, part 2), on the same pixel grid as the thing
 * itself: [p] is its pixel size, [widthPx] and [heightPx] its size in those pixels, and ([centerX],
 * [groundY]) the middle of where it meets the ground.
 *
 * The footprint is swept along [castShadowShape]'s direction, narrowing toward the far end like the
 * top of the silhouette would. Each row is one solid run; its last pixels toward the far end fade
 * out on a checkerboard, so the end dithers instead of stopping hard.
 *
 * Draw it first, then the contact shadow, then the sprite. Nothing is drawn above [groundY], so the
 * shadow never climbs a wall or spills onto water behind a deck.
 */
fun drawCastShadow(
    scope: DrawScope,
    centerX: Float,
    groundY: Float,
    widthPx: Int,
    p: Float,
    heightPx: Int = widthPx,
    light: CastLight = SceneLight.current,
    color: Color = CastShadowColor
) {
    if (p <= 0f) return
    val shape = castShadowShape(light, widthPx, heightPx, centerX)
    if (!shape.visible) return
    val cx = (centerX / p).roundToInt()
    val cy = (groundY / p).roundToInt()
    val reach = abs(shape.tipDx)
    // The far part of a sideways shadow fades: past this many pixels from the middle it dithers
    val fadeFrom = ((reach + widthPx / 2f) * 0.7f).roundToInt()
    for (r in 0 until shape.rows) {
        val t0 = r / shape.rows.toFloat()
        val t1 = (r + 1) / shape.rows.toFloat()
        val half0 = widthPx * (1f - 0.45f * t0) / 2f
        val half1 = widthPx * (1f - 0.45f * t1) / 2f
        val c0 = cx + shape.tipDx * t0
        val c1 = cx + shape.tipDx * t1
        val left = min(c0 - half0, c1 - half1).roundToInt()
        val right = max(c0 + half0, c1 + half1).roundToInt() - 1
        if (right < left) continue
        val row = cy + r
        val rowY = row * p
        fun dither(from: Int, to: Int) {
            for (col in from..to) {
                if (((col + row) and 1) == 0) scope.drawRect(color, Offset(col * p, rowY), Size(p, p))
            }
        }
        fun solid(from: Int, to: Int) {
            if (to >= from) scope.drawRect(color, Offset(from * p, rowY), Size((to - from + 1) * p, p))
        }
        when {
            // The far row is all checkerboard
            r == shape.rows - 1 -> dither(left, right)
            // Short shadows (noon, indoors) soften at both ends of each row
            reach < 2 -> {
                solid(left + 1, right - 1)
                dither(left, left)
                if (right > left) dither(right, right)
            }
            shape.tipDx > 0 -> {
                val fade = min(cx + fadeFrom, right)
                solid(left, fade - 1)
                dither(fade, right)
            }
            else -> {
                val fade = max(cx - fadeFrom, left)
                dither(left, fade)
                solid(fade + 1, right)
            }
        }
    }
}
