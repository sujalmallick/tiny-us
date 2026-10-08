package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.WeatherType
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Universal cast shadow color: a cool near-black with low alpha (~0.14),
 * so it softens gently into grass, wood, stone, tiles and snow.
 */
val CastShadowColor: Color = Color(0x24101828)

/**
 * Precomputed parameters for a cast shadow:
 * - [dxPx]: Total lateral pixel displacement at the furthest row (+ is right, - is left).
 * - [lengthRows]: Number of stepped rows extending forward from the ground baseline.
 * - [active]: Whether a cast shadow should be drawn.
 */
data class CastShadowGeometry(
    val dxPx: Int,
    val lengthRows: Int,
    val active: Boolean
)

/**
 * Computes the cast shadow parameters based on lighting environment:
 * - Outdoors by day: driven by sun progress (0 = morning, 0.5 = noon, 1.0 = sunset).
 *   Morning shadows point right (+dx), sunset shadows point left (-dx).
 *   Length is shortest near 0.5 (under feet) and longest near 0 or 1.
 * - Overcast (RAIN, SNOW): sun cast shadows are suppressed (none in rain/snow).
 * - Outdoors at night: no sun or moon cast shadows; local light sources (campfire, lamps)
 *   cast short, soft shadows away from the light.
 * - Indoors: subtle, short ground-plane cast shadow.
 */
fun calculateCastShadowGeometry(
    widthPx: Int,
    sunProgress: Float? = null,
    isOutdoor: Boolean = true,
    isNight: Boolean = false,
    weather: WeatherType = WeatherType.SUNNY,
    localLightX: Float? = null,
    centerX: Float = 0f
): CastShadowGeometry {
    if (widthPx < 2) return CastShadowGeometry(0, 0, false)

    if (isOutdoor) {
        if (isNight) {
            // At night: no sun or moon shadows. If there is a local light (e.g. campfire),
            // cast a short shadow away from it.
            if (localLightX != null) {
                val diffX = centerX - localLightX
                val dir = if (diffX >= 0f) 1 else -1
                val dx = dir * (widthPx / 4).coerceIn(2, 4)
                return CastShadowGeometry(dx, 2, true)
            }
            return CastShadowGeometry(0, 0, false)
        }

        // Overcast sky: no sun cast shadows in rain or snow
        if (weather == WeatherType.RAIN || weather == WeatherType.SNOW) {
            return CastShadowGeometry(0, 0, false)
        }

        // Daytime outdoors: driven by celestial progress (0.0 morning .. 0.5 noon .. 1.0 sunset)
        val prog = (sunProgress ?: com.example.ui.celestialProgressOverride ?: 0.5f).coerceIn(0f, 1f)
        val skewFactor = (0.5f - prog).coerceIn(-0.5f, 0.5f) // +0.5 at morning, 0 at noon, -0.5 at sunset

        // Lateral skew: maximal at morning (+dx, right) and sunset (-dx, left), zero at noon
        val maxShift = (widthPx * 0.65f).coerceIn(3f, 12f)
        val dx = (skewFactor * 2f * maxShift).roundToInt()

        // Length: short (2 rows) near noon (0.5), longer (4 rows) near 0 or 1
        val distFromNoon = abs(0.5f - prog) * 2f // 0 at noon, 1 at dawn/dusk
        val rows = (2 + (distFromNoon * 2f).roundToInt()).coerceIn(2, 4)

        return CastShadowGeometry(dx, rows, true)
    } else {
        // Indoors: subtle, short ground-plane cast shadow (2 rows, slight offset)
        if (localLightX != null) {
            val diffX = centerX - localLightX
            val dir = if (diffX >= 0f) 1 else -1
            val dx = dir * (widthPx / 5).coerceIn(1, 3)
            return CastShadowGeometry(dx, 2, true)
        }
        val dx = (widthPx / 7).coerceIn(1, 2)
        return CastShadowGeometry(dx, 2, true)
    }
}

/**
 * Draws a light pixel cast-shadow onto the ground plane using stepped whole-game-pixel rows.
 *
 * Drawn BEFORE the contact shadow and sprite:
 * order: cast shadow -> contact shadow -> sprite.
 */
fun drawCastShadow(
    scope: DrawScope,
    centerX: Float,
    groundY: Float,
    widthPx: Int,
    p: Float,
    sunProgress: Float? = null,
    isOutdoor: Boolean = true,
    isNight: Boolean = false,
    weather: WeatherType = WeatherType.SUNNY,
    localLightX: Float? = null,
    minGroundY: Float? = null,
    maxGroundY: Float? = null,
    color: Color = CastShadowColor
) {
    if (widthPx < 2 || p <= 0f) return
    val geom = calculateCastShadowGeometry(
        widthPx = widthPx,
        sunProgress = sunProgress,
        isOutdoor = isOutdoor,
        isNight = isNight,
        weather = weather,
        localLightX = localLightX,
        centerX = centerX
    )
    if (!geom.active || geom.lengthRows <= 0) return

    val cx = (centerX / p).roundToInt()
    val cy = (groundY / p).roundToInt()

    for (r in 0 until geom.lengthRows) {
        val row = cy + r
        val rowY = row * p
        if (minGroundY != null && rowY < minGroundY) continue
        if (maxGroundY != null && rowY > maxGroundY) continue

        // Lateral shift increases with distance from base
        val rowShift = ((r + 1).toFloat() / geom.lengthRows * geom.dxPx).roundToInt()
        val rowCx = cx + rowShift

        // Taper width slightly with each step
        val rowW = (widthPx - r * 2).coerceAtLeast(3)
        val halfW = rowW / 2
        val leftCol = rowCx - halfW
        val rightCol = rowCx + halfW

        val isEndRow = (r == geom.lengthRows - 1)
        if (isEndRow) {
            // Far row: checkerboard dither for soft fade
            for (col in leftCol..rightCol) {
                if (((col + row) and 1) == 0) {
                    scope.drawRect(color, Offset(col * p, rowY), Size(p, p))
                }
            }
        } else {
            // Inner core is solid; outermost edge pixel dithered
            val coreLeft = leftCol + 1
            val coreRight = rightCol - 1
            if (coreRight >= coreLeft) {
                scope.drawRect(color, Offset(coreLeft * p, rowY), Size((coreRight - coreLeft + 1) * p, p))
            }
            if (((leftCol + row) and 1) == 0) {
                scope.drawRect(color, Offset(leftCol * p, rowY), Size(p, p))
            }
            if (((rightCol + row) and 1) == 0) {
                scope.drawRect(color, Offset(rightCol * p, rowY), Size(p, p))
            }
        }
    }
}
