package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FallenParticle
import com.example.engine.LoftSprites
import com.example.engine.ParticleType
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelParticle
import com.example.engine.CharacterMotionTween
import com.example.engine.WorldSprites
import com.example.engine.RoomTheme
import com.example.scene.EnvironmentType
import com.example.scene.WeatherType
import com.example.scene.SceneEngine
import com.example.scene.CouchPhase
import com.example.scene.CatState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.datetime.toLocalDateTime

data class DynamicNightStar(
    val fx: Float,
    val fy: Float,
    val sizeP: Float,
    val baseAlpha: Float,
    val color: Color,
    val twinkleSpeed: Float,
    val twinklePhase: Float,
    val hasCrossFlare: Boolean = false
)

fun generateDynamicNightStars(seed: Long = 1337L): Array<DynamicNightStar> {
    val random = kotlin.random.Random(seed)
    val list = ArrayList<DynamicNightStar>(96)

    // 1. Lore Constellations (Preserved for interactive touch mechanics & easter eggs)
    // Constellation: The Two Hearts (Companion binary stars in fx: 0.08..0.32, fy: 0.05..0.28)
    list.add(DynamicNightStar(0.14f, 0.11f, 2.0f, 0.95f, Color(0xFFFFD6E0), 1.9f, 0.0f, hasCrossFlare = true))
    list.add(DynamicNightStar(0.20f, 0.16f, 2.0f, 0.95f, Color(0xFFFFF0F3), 1.9f, 1.2f, hasCrossFlare = true))
    list.add(DynamicNightStar(0.17f, 0.08f, 1.2f, 0.75f, Color(0xFFFFFFFF), 2.5f, 2.4f))
    list.add(DynamicNightStar(0.11f, 0.18f, 1.1f, 0.70f, Color(0xFFFFE5EC), 3.0f, 3.1f))
    list.add(DynamicNightStar(0.23f, 0.22f, 1.1f, 0.70f, Color(0xFFFFD6E0), 2.4f, 0.8f))

    // Constellation: The Celestial Teapot (Center sky warmth in fx: 0.33..0.52, fy: 0.05..0.34)
    list.add(DynamicNightStar(0.36f, 0.09f, 1.8f, 0.90f, Color(0xFFFFE5AA), 2.1f, 0.9f, hasCrossFlare = true))
    list.add(DynamicNightStar(0.34f, 0.17f, 1.4f, 0.80f, Color(0xFFFFFFFF), 2.6f, 2.1f))
    list.add(DynamicNightStar(0.40f, 0.20f, 1.5f, 0.80f, Color(0xFFFFECD1), 2.3f, 3.5f))
    list.add(DynamicNightStar(0.47f, 0.19f, 1.6f, 0.85f, Color(0xFFFFFFFF), 2.0f, 1.4f, hasCrossFlare = true))
    list.add(DynamicNightStar(0.49f, 0.12f, 1.3f, 0.75f, Color(0xFFE2F0FD), 2.7f, 4.2f))

    // Constellation: Starlight Trail (Guiding starry road in fx: 0.58..0.92, fy: 0.06..0.28)
    list.add(DynamicNightStar(0.62f, 0.10f, 1.8f, 0.90f, Color(0xFFCAF0F8), 2.2f, 0.7f, hasCrossFlare = true))
    list.add(DynamicNightStar(0.68f, 0.08f, 1.4f, 0.80f, Color(0xFFFFFFFF), 2.8f, 2.8f))
    list.add(DynamicNightStar(0.73f, 0.12f, 1.5f, 0.80f, Color(0xFFE0E7FF), 2.4f, 4.0f))
    list.add(DynamicNightStar(0.82f, 0.11f, 1.9f, 0.90f, Color(0xFFFFECC4), 1.8f, 1.6f, hasCrossFlare = true))
    list.add(DynamicNightStar(0.89f, 0.14f, 1.6f, 0.85f, Color(0xFFFFFFFF), 2.2f, 3.2f, hasCrossFlare = true))

    // 2. Dynamic Stratified Star Field across the entire night sky
    // Stratified grid: 12 horizontal columns x 7 vertical tiers across the celestial dome
    val starPalettes = arrayOf(
        Color(0xFFFFFFFF), // Crisp pure white
        Color(0xFFE8EEF5), // Diamond silver-white
        Color(0xFFFFF6E5), // Soft warm ivory
        Color(0xFFFFECD1), // Pale starlight amber
        Color(0xFFD6E4FF)  // Soft starlight blue
    )

    val cols = 12
    val rows = 7
    for (r in 0 until rows) {
        val yMin = 0.02f + (r.toFloat() / rows) * 0.60f
        val yMax = 0.02f + ((r + 1).toFloat() / rows) * 0.60f
        for (c in 0 until cols) {
            // Natural occupancy with subtle jitter
            if (random.nextFloat() < 0.60f) {
                val fx = (c.toFloat() + 0.12f + random.nextFloat() * 0.76f) / cols
                val fy = yMin + random.nextFloat() * (yMax - yMin)

                // Avoid collision with moon zone (moon center is at approx 0.78, 0.15)
                val dxMoon = fx - 0.78f
                val dyMoon = fy - 0.15f
                if (dxMoon * dxMoon + dyMoon * dyMoon < 0.006f) continue

                // Avoid duplicate stars in primary constellation focus points
                if (fx in 0.12f..0.22f && fy in 0.09f..0.18f) continue

                val sizeRoll = random.nextFloat()
                val (sSize, flare, baseA) = when {
                    sizeRoll < 0.65f -> Triple(1.0f, false, 0.35f + random.nextFloat() * 0.35f)
                    sizeRoll < 0.90f -> Triple(1.3f, false, 0.55f + random.nextFloat() * 0.30f)
                    else -> Triple(1.8f, random.nextFloat() < 0.4f, 0.75f + random.nextFloat() * 0.25f)
                }
                val col = starPalettes[random.nextInt(starPalettes.size)]
                val speed = 1.3f + random.nextFloat() * 2.2f
                val phase = random.nextFloat() * 6.28f

                list.add(DynamicNightStar(fx, fy, sSize, baseA, col, speed, phase, flare))
            }
        }
    }
    return list.toTypedArray()
}

var dynamicNightStars: Array<DynamicNightStar> = generateDynamicNightStars()

fun reallocateNightStars(seed: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()) {
    dynamicNightStars = generateDynamicNightStars(seed)
}

/**
 * Drawn just after the sky's colour bands, before the clouds and everything else in the scene
 * (set by drawWorldFrame around drawEnvironment; drawing is single-threaded). The rainbow uses it,
 * so the clouds, hills and trees stand in front of it.
 */
var afterSkyBands: ((DrawScope) -> Unit)? = null

/** Pins the moon's phase (0 new, 0.5 full) instead of tonight's; for previews and tests only. */
var moonPhaseOverride: Float?
    get() = com.example.engine.MoonPhase.override
    set(value) { com.example.engine.MoonPhase.override = value }

/** Tonight's moon phase, 0 new to 0.5 full and back to 1 (shared with the loft, which lives in :shared). */
fun currentMoonFraction(): Float = com.example.engine.MoonPhase.current()

/** Seconds for the night sky to turn once across the screen: the stars drift slowly west and wrap round. */
const val SKY_TURN_SECONDS = 2700f

/** How far the night sky has turned at scene time [time], as a share of the screen width. */
fun skyDrift(time: Float): Float = (time / SKY_TURN_SECONDS) % 1f

/** A star's (or constellation's) share across the screen after the sky has turned by [drift]. */
fun driftedX(fx: Float, drift: Float): Float {
    val x = fx - drift
    return if (x < 0f) x + 1f else x
}

/** The current hour of the day with minutes and seconds, from the real clock. */
private fun clockHours(): Float {
    val c = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return c.hour + c.minute / 60f + c.second / 3600f
}

/**
 * How far the sun (5:00 to 20:00) or the moon (20:00 to 5:00) has come along its path across the
 * sky, 0 at rising on the left to 1 at setting on the right. It follows the real clock, so it moves
 * a little every few minutes; when the time of day is set by hand and doesn't match the clock, a
 * fitting fixed spot is used instead.
 */
fun celestialProgress(isNight: Boolean, isSunset: Boolean, isMorning: Boolean): Float {
    val h = clockHours()
    val phase = com.example.engine.TimeOfDayPhase.fromHour(h.toInt())
    return if (isNight) {
        if (phase.isNight) (((h - 20f + 24f) % 24f) / 9f).coerceIn(0f, 1f) else 0.62f
    } else {
        val matches = when {
            isSunset -> phase.isSunset
            isMorning -> phase.isMorning
            else -> phase == com.example.engine.TimeOfDayPhase.AFTERNOON
        }
        when {
            matches -> ((h - 5f) / 15f).coerceIn(0f, 1f)
            isSunset -> 0.9f
            isMorning -> 0.12f
            else -> 0.45f
        }
    }
}

/** A round pixel disc [d] scene pixels across with its top-left at ([x], [y]). */
private fun drawPixelDisc(scope: DrawScope, x: Float, y: Float, d: Float, p: Float, color: Color) {
    val inset = 2f * p
    scope.drawRect(color, Offset(x, y), Size(d * p, d * p))
    scope.drawRect(color, Offset(x - inset, y + inset), Size(inset, (d - 4f) * p))
    scope.drawRect(color, Offset(x + d * p, y + inset), Size(inset, (d - 4f) * p))
    scope.drawRect(color, Offset(x + inset, y - inset), Size((d - 4f) * p, inset))
    scope.drawRect(color, Offset(x + inset, y + d * p), Size((d - 4f) * p, inset))
}

/**
 * The sun on its way across the day sky: low and orange at morning and sunset, high and pale at
 * noon. Drawn before the clouds and the ground, so it sets behind the hills. Hidden in the rain.
 */
private fun drawDaySun(
    scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float,
    isSunset: Boolean, isMorning: Boolean, weather: com.example.scene.WeatherType
) {
    if (weather == com.example.scene.WeatherType.RAIN) return
    val prog = celestialProgress(false, isSunset, isMorning)
    val arc = sin(prog * kotlin.math.PI.toFloat())
    val d = 12f
    val x = cw * (0.06f + 0.80f * prog)
    val y = ch * (0.46f - 0.38f * arc)
    val low = isSunset || isMorning
    val body = when {
        isSunset -> Color(0xFFFF9F43)
        isMorning -> Color(0xFFFFC46B)
        weather == com.example.scene.WeatherType.SNOW -> Color(0xFFFFF4D6)
        else -> Color(0xFFFFE07A)
    }
    val core = if (low) Color(0xFFFFD9A0) else Color(0xFFFFF6CF)
    val center = Offset(x + d * p / 2f, y + d * p / 2f)
    val pulse = 0.9f + sin(time * 0.8f) * 0.1f
    scope.drawCircle(body.copy(alpha = 0.12f * pulse), radius = 11f * p, center = center)
    drawPixelDisc(scope, x, y, d, p, body)
    scope.drawRect(core, Offset(x + 2f * p, y + 2f * p), Size((d - 4f) * p, (d - 4f) * p))
}

// Precomputed Sky Bands and Splits for zero allocation per frame
val SPLITS_4_BAND = listOf(0.35f, 0.65f, 0.85f)
val SPLITS_5_BAND_PINK = listOf(0.25f, 0.50f, 0.72f, 0.88f)
val SPLITS_5_BAND_AUTUMN = listOf(0.28f, 0.50f, 0.72f, 0.88f)
val SPLITS_MORNING = listOf(0.45f, 0.75f)

val BANDS_SUNSET_PINK = listOf(Color(0xFF881B4C), Color(0xFFC7366E), Color(0xFFF05D8E), Color(0xFFFF94B8), Color(0xFFFFD4E2))
val BANDS_SUNSET_SAKURA = listOf(Color(0xFF38184C), Color(0xFF8B2662), Color(0xFFE05780), Color(0xFFFFB5C2))
val BANDS_SUNSET_AUTUMN = listOf(Color(0xFF2B0E1E), Color(0xFF7A1C16), Color(0xFFC4451C), Color(0xFFE77F24), Color(0xFFF7BA3E))
val BANDS_SUNSET_SNOW = listOf(Color(0xFF1C1A3A), Color(0xFF53416E), Color(0xFF9E6589), Color(0xFFDF9FB8))
val BANDS_SUNSET_RAIN = listOf(Color(0xFF1E172B), Color(0xFF48334E), Color(0xFF7A4A58), Color(0xFFA66D60))
val BANDS_SUNSET_SUMMER = listOf(Color(0xFF1F1035), Color(0xFF9E2A4B), Color(0xFFE3592B), Color(0xFFFFAA44), Color(0xFFFFD166))

val BANDS_MORNING_SAKURA = listOf(Color(0xFF343B68), Color(0xFFFF9EAA), Color(0xFFFFE0E9))
val BANDS_MORNING_AUTUMN = listOf(Color(0xFF2E334D), Color(0xFFE29578), Color(0xFFFFDDD2))
val BANDS_MORNING_SNOW = listOf(Color(0xFF24324E), Color(0xFFA5B4D0), Color(0xFFE2EAFC))
val BANDS_MORNING_RAIN = listOf(Color(0xFF202A3D), Color(0xFF64748B), Color(0xFF94A3B8))
val BANDS_MORNING_SUMMER = listOf(Color(0xFF2C3E7A), Color(0xFFFF8C69), Color(0xFFFFD1A0))

val BANDS_DAY_SAKURA = listOf(Color(0xFF6BA8D6), Color(0xFF98C5E8), Color(0xFFD2E6F5), Color(0xFFFFD6E7))
val BANDS_DAY_AUTUMN = listOf(Color(0xFF4A85B8), Color(0xFF7CA6CD), Color(0xFFB5CDE1), Color(0xFFEAD5A8))
val BANDS_DAY_SNOW = listOf(Color(0xFF5D99C6), Color(0xFF8EBCDE), Color(0xFFC8E0F2), Color(0xFFEBF4FA))
val BANDS_DAY_RAIN = listOf(Color(0xFF334B68), Color(0xFF526E88), Color(0xFF7892A8), Color(0xFF9BB1C2))
val BANDS_DAY_SUMMER = listOf(Color(0xFF3B9FE2), Color(0xFF68BCE8), Color(0xFFA5DCF4), Color(0xFFD4EFFC))

// Night Space Band Colors (5 fixed space bands)
val NIGHT_SPACE_BASE_BANDS = arrayOf(
    Color(0xFF04060E),
    Color(0xFF070B1E),
    Color(0xFF0C132C),
    Color(0xFF121B3C),
    Color(0xFF18244D)
)

fun drawConstellationOverlay(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    p: Float,
    timer: Float,
    constellationIdx: Int,
    time: Float = 0f
) {
    if (timer <= 0f || constellationIdx !in 1..3) return
    val drift = skyDrift(time)
    fun at(fx: Float, fy: Float) = androidx.compose.ui.geometry.Offset(cw * driftedX(fx, drift), ch * fy)
    val dur = 2.4f
    val t = ((dur - timer) / dur).coerceIn(0f, 1f)
    val alpha = sin(t * kotlin.math.PI.toFloat()).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return

    val stars = when (constellationIdx) {
        1 -> listOf( // The Two Hearts
            at(0.14f, 0.11f),
            at(0.17f, 0.08f),
            at(0.20f, 0.16f),
            at(0.23f, 0.22f),
            at(0.11f, 0.18f),
            at(0.14f, 0.11f)
        )
        2 -> listOf( // The Celestial Teapot
            at(0.36f, 0.09f),
            at(0.49f, 0.12f),
            at(0.47f, 0.19f),
            at(0.40f, 0.20f),
            at(0.34f, 0.17f),
            at(0.36f, 0.09f)
        )
        3 -> listOf( // Starlight Trail
            at(0.62f, 0.10f),
            at(0.68f, 0.08f),
            at(0.73f, 0.12f),
            at(0.82f, 0.11f),
            at(0.89f, 0.14f)
        )
        else -> emptyList()
    }

    for (i in 0 until stars.size - 1) {
        val s1 = stars[i]
        val s2 = stars[i + 1]
        scope.drawLine(
            color = Color(0xFF64DFDF).copy(alpha = alpha * 0.35f),
            start = s1,
            end = s2,
            strokeWidth = 3.5f * p,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        scope.drawLine(
            color = Color.White.copy(alpha = alpha * 0.85f),
            start = s1,
            end = s2,
            strokeWidth = 1.2f * p,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    for (pt in stars) {
        scope.drawCircle(
            color = Color(0xFFFFD166).copy(alpha = alpha * 0.5f),
            radius = 3.5f * p,
            center = pt
        )
        scope.drawCircle(
            color = Color.White.copy(alpha = alpha * 0.9f),
            radius = 1.5f * p,
            center = pt
        )
    }
}

/** The colour at the very top of the night sky (the sky continued above a tall screen's stage). */
val NIGHT_ZENITH: Color get() = NIGHT_SPACE_BASE_BANDS[0]

/**
 * One star as pixel art: solid blocks (one, or two for the big ones), twinkling in steps between
 * bright, softer and dim shades of its colour against [sky], with a small cross glint on the
 * brightest anchor stars at their peak.
 */
fun drawNightStar(scope: DrawScope, x: Float, y: Float, star: DynamicNightStar, time: Float, p: Float, sky: Color) {
    val twinkle = sin(time * star.twinkleSpeed + star.twinklePhase) * 0.35f + 0.65f
    val level = star.baseAlpha * twinkle
    fun toward(f: Float) = Color(
        red = star.color.red + (sky.red - star.color.red) * f,
        green = star.color.green + (sky.green - star.color.green) * f,
        blue = star.color.blue + (sky.blue - star.color.blue) * f,
        alpha = 1f
    )
    val col = when {
        level > 0.70f -> star.color.copy(alpha = 1f)
        level > 0.45f -> toward(0.35f)
        else -> toward(0.62f)
    }
    val blocks = if (star.sizeP >= 1.7f) 2 else 1
    val s = blocks * p
    scope.drawRect(col, Offset(x, y), Size(s, s))
    if (star.hasCrossFlare && twinkle > 0.86f) {
        val arm = toward(0.45f)
        scope.drawRect(arm, Offset(x - p, y), Size(p, s))
        scope.drawRect(arm, Offset(x + s, y), Size(p, s))
        scope.drawRect(arm, Offset(x, y - p), Size(s, p))
        scope.drawRect(arm, Offset(x, y + s), Size(s, p))
    }
}

/** The star field again for the sky above the stage (mirrored, so it doesn't look copied). */
fun drawNightStarsAbove(scope: DrawScope, cw: Float, skyH: Float, time: Float, p: Float) {
    for (i in dynamicNightStars.indices) {
        val star = dynamicNightStars[i]
        val y = -skyH + ((star.fy - 0.02f) / 0.60f).coerceIn(0f, 1f) * (skyH - 2f * p)
        drawNightStar(scope, cw * driftedX(1f - star.fx, skyDrift(time)), y, star, time + 7.3f, p, NIGHT_ZENITH)
    }
}

fun drawMilkyWayNightSky(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    time: Float,
    p: Float,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY
) {
    val skyH = ch * 0.72f

    // 1. Deep Space Atmospheric Gradient (zenith to horizon)
    val horizonAirglow = when (weather) {
        com.example.scene.WeatherType.SNOW -> Color(0xFF101E42)   // Freezing crystal cobalt
        com.example.scene.WeatherType.SAKURA -> Color(0xFF261D42) // Soft plum-indigo airglow
        com.example.scene.WeatherType.AUTUMN -> Color(0xFF1E1E38) // Cedar amber-indigo
        com.example.scene.WeatherType.RAIN -> Color(0xFF101626)   // Storm charcoal-navy void
        com.example.scene.WeatherType.SUNNY -> Color(0xFF202C5E)  // Deep summer celestial airglow
    }
    val bandH = skyH / 6f
    for (i in 0 until 6) {
        val col = if (i < 5) NIGHT_SPACE_BASE_BANDS[i] else horizonAirglow
        val yStart = i * bandH
        scope.drawRect(
            color = col,
            topLeft = Offset(0f, yStart),
            size = Size(cw, bandH + 1.5f)
        )
        // 2px dither line between bands for seamless optical blending
        if (i < 5) {
            val nextCol = if (i + 1 < 5) NIGHT_SPACE_BASE_BANDS[i + 1] else horizonAirglow
            val ditherY = yStart + bandH - 2f * p
            val blendCol = nextCol.copy(alpha = 0.45f)
            val ditherStep = 6f * p
            val count = (cw / ditherStep).toInt() + 1
            for (dx in 0..count) {
                scope.drawRect(
                    color = blendCol,
                    topLeft = Offset(dx * ditherStep + (if (i % 2 == 0) 0f else 3f * p), ditherY),
                    size = Size(3f * p, 2f * p)
                )
            }
        }
    }

    // 2. Dynamically Allocated Star Field (organic distribution across full night sky), turning
    // slowly westward so the stars never sit still in one place.
    val drift = skyDrift(time)
    for (i in dynamicNightStars.indices) {
        val star = dynamicNightStars[i]
        val sy = ch * star.fy
        val band = (sy / bandH).toInt().coerceIn(0, 5)
        val sky = if (band < 5) NIGHT_SPACE_BASE_BANDS[band] else horizonAirglow
        drawNightStar(scope, cw * driftedX(star.fx, drift), sy, star, time, p, sky)
    }

    // 3. Glowing Pixel Moon with Minimal Soft Corona, crossing the sky through the night
    val moonProg = celestialProgress(isNight = true, isSunset = false, isMorning = false)
    val moonX = cw * (0.06f + 0.80f * moonProg)
    val moonY = ch * (0.30f - 0.20f * sin(moonProg * kotlin.math.PI.toFloat()))
    val moonCenter = Offset(moonX + 7f * p, moonY + 7f * p)
    val moonFraction = currentMoonFraction()
    // A thin crescent glows less than a full moon.
    val glow = 0.25f + 0.75f * com.example.engine.MoonPhase.illumination(moonFraction)
    scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.04f * glow), radius = 20f * p, center = moonCenter)
    scope.drawCircle(Color(0xFFFFF8D6).copy(alpha = 0.08f * glow), radius = 12f * p, center = moonCenter)

    // Crisp Pixel Moon Body
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX, moonY), Size(14 * p, 14 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX - 2 * p, moonY + 2 * p), Size(2 * p, 10 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX + 14 * p, moonY + 2 * p), Size(2 * p, 10 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX + 2 * p, moonY - 2 * p), Size(10 * p, 2 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX + 2 * p, moonY + 14 * p), Size(10 * p, 2 * p))
    // Craters
    scope.drawRect(Color(0xFFE9D8A6), Offset(moonX + 3 * p, moonY + 4 * p), Size(3 * p, 3 * p))
    scope.drawRect(Color(0xFFE9D8A6), Offset(moonX + 8 * p, moonY + 7 * p), Size(4 * p, 3 * p))
    // Tonight's phase: the unlit part in faint earthshine, row by row across the pixel disc
    // (18 rows; the top and bottom two are 10 pixels wide, the rest 18).
    val earthshine = Color(0xFF161D36)
    for (k in 0 until 18) {
        val half = if (k < 2 || k >= 16) 5f else 9f
        val span = com.example.engine.MoonPhase.shadowSpan(moonFraction, half) ?: continue
        val left = kotlin.math.round(span.first)
        val right = kotlin.math.round(span.second)
        if (right > left) scope.drawRect(earthshine, Offset(moonCenter.x + left * p, moonY - 2f * p + k * p), Size((right - left) * p, p))
    }

    // 4. Rare shooting star; most quiet nights remain still.
    val meteorCycle = (time + 19.7f) % 78.0f
    if (meteorCycle < 0.75f) {
        val prog = meteorCycle / 0.75f
        val startMx = cw * 0.20f
        val startMy = ch * 0.06f
        val endMx = cw * 0.44f
        val endMy = ch * 0.16f
        val headX = startMx + (endMx - startMx) * prog
        val headY = startMy + (endMy - startMy) * prog
        val tailDx = (endMx - startMx) / 0.75f * 0.025f * p
        val tailDy = (endMy - startMy) / 0.75f * 0.025f * p

        // Meteor Head (delicate pixel point)
        scope.drawRect(Color(0xFFFFFFFF), Offset(headX - p, headY - p), Size(2f * p, 2f * p))
        // Fading Stardust Trail
        scope.drawRect(Color(0xFFFFFFFF).copy(alpha = 0.70f), Offset(headX - tailDx, headY - tailDy), Size(1.5f * p, 1.5f * p))
        scope.drawRect(Color(0xFFBDE0FE).copy(alpha = 0.40f), Offset(headX - tailDx * 2f, headY - tailDy * 2f), Size(p, p))
        scope.drawRect(Color(0xFF90E0EF).copy(alpha = 0.20f), Offset(headX - tailDx * 3f, headY - tailDy * 3f), Size(p, p))
    }
}

/** One drifting pixel cloud; it wraps around a world [cw] wide. */
fun drawSkyCloud(
    scope: DrawScope,
    cw: Float,
    baseX: Float,
    y: Float,
    scaleFactor: Float,
    time: Float,
    p: Float,
    isSunset: Boolean,
    isMorning: Boolean
) {
    val cx = (baseX + (time * 10f * scaleFactor)) % (cw + 140f) - 70f
    val cloudColor = when {
        isSunset -> Color(0xFFFFDDD2)
        isMorning -> Color(0xFFFFF0F5)
        else -> Color(0xF2FFFFFF)
    }
    val cloudShadow = when {
        isSunset -> Color(0xFFE29578)
        isMorning -> Color(0xFFF7CAD0)
        else -> Color(0xFFD6E2E9)
    }

    scope.drawRect(cloudShadow, Offset(cx, y + 2 * p), Size(28 * p, 10 * p))
    scope.drawRect(cloudColor, Offset(cx, y), Size(28 * p, 10 * p))
    scope.drawRect(cloudColor, Offset(cx + 6 * p, y - 6 * p), Size(18 * p, 6 * p))
    scope.drawRect(cloudColor, Offset(cx - 5 * p, y + 3 * p), Size(6 * p, 6 * p))
    scope.drawRect(cloudColor, Offset(cx + 27 * p, y + 3 * p), Size(6 * p, 6 * p))
}

fun drawSkyAndClouds(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    isNight: Boolean,
    isSunset: Boolean,
    isMorning: Boolean,
    time: Float,
    p: Float,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
    isPinkSunset: Boolean = false
) {
    val skyH = ch * 0.66f

    // Helper: draw a 2px wide alternating-dot dither seam between two sky bands
    fun drawSkyDitherSeam(seamY: Float, topColor: Color, botColor: Color) {
        val ditherStep = 6f * p
        val count = (cw / ditherStep).toInt() + 1
        // Alternating dots: even positions draw bottom-color dots, odd positions draw top-color dots
        for (dx in 0..count) {
            val xPos = dx * ditherStep
            scope.drawRect(
                color = botColor.copy(alpha = 0.50f),
                topLeft = Offset(xPos, seamY - 2f * p),
                size = Size(3f * p, 2f * p)
            )
            if (dx % 2 == 1) {
                scope.drawRect(
                    color = topColor.copy(alpha = 0.38f),
                    topLeft = Offset(xPos + 3f * p, seamY),
                    size = Size(3f * p, 2f * p)
                )
            }
        }
    }

    if (isNight) {
        // Night sky with seasonal horizon airglow
        drawMilkyWayNightSky(scope, cw, ch, time, p, weather = weather)
    } else {
        fun drawSkyBands(bands: List<Color>, splits: List<Float>) {
            for (i in bands.indices) {
                val yStart = if (i == 0) 0f else skyH * splits[i - 1]
                val yEnd = if (i == bands.size - 1) skyH else skyH * splits[i]
                scope.drawRect(bands[i], Offset(0f, yStart), Size(cw, yEnd - yStart))
                if (i > 0) {
                    drawSkyDitherSeam(yStart, bands[i - 1], bands[i])
                }
            }
        }

        // ── Sky background fill bands per season and time-of-day ──────────────
        when {
            // ONLY the "Looking at You" scene uses a distinct pink sky during sunset in summer
            isSunset && isPinkSunset && weather == com.example.scene.WeatherType.SUNNY -> {
                drawSkyBands(BANDS_SUNSET_PINK, SPLITS_5_BAND_PINK)
            }

            // Normal seasonal sunset palette for all other scenes (and Looking at You in non-summer)
            isSunset -> {
                when (weather) {
                    com.example.scene.WeatherType.SAKURA -> drawSkyBands(BANDS_SUNSET_SAKURA, SPLITS_4_BAND)
                    com.example.scene.WeatherType.AUTUMN -> drawSkyBands(BANDS_SUNSET_AUTUMN, SPLITS_5_BAND_AUTUMN)
                    com.example.scene.WeatherType.SNOW -> drawSkyBands(BANDS_SUNSET_SNOW, SPLITS_4_BAND)
                    com.example.scene.WeatherType.RAIN -> drawSkyBands(BANDS_SUNSET_RAIN, SPLITS_4_BAND)
                    else -> drawSkyBands(BANDS_SUNSET_SUMMER, SPLITS_5_BAND_AUTUMN)
                }
            }

            // Morning dawn palettes per season
            isMorning -> {
                val morningBands = when (weather) {
                    com.example.scene.WeatherType.SAKURA -> BANDS_MORNING_SAKURA
                    com.example.scene.WeatherType.AUTUMN -> BANDS_MORNING_AUTUMN
                    com.example.scene.WeatherType.SNOW -> BANDS_MORNING_SNOW
                    com.example.scene.WeatherType.RAIN -> BANDS_MORNING_RAIN
                    else -> BANDS_MORNING_SUMMER
                }
                drawSkyBands(morningBands, SPLITS_MORNING)
            }

            // Daytime palettes per season — clear light-blue sky for summer!
            else -> {
                when (weather) {
                    com.example.scene.WeatherType.SAKURA -> drawSkyBands(BANDS_DAY_SAKURA, SPLITS_4_BAND)
                    com.example.scene.WeatherType.AUTUMN -> drawSkyBands(BANDS_DAY_AUTUMN, SPLITS_4_BAND)
                    com.example.scene.WeatherType.SNOW -> drawSkyBands(BANDS_DAY_SNOW, SPLITS_4_BAND)
                    com.example.scene.WeatherType.RAIN -> drawSkyBands(BANDS_DAY_RAIN, SPLITS_4_BAND)
                    else -> drawSkyBands(BANDS_DAY_SUMMER, SPLITS_4_BAND)
                }
            }
        }

        drawDaySun(scope, cw, ch, p, time, isSunset, isMorning, weather)
        afterSkyBands?.invoke(scope)

        // ── Drifting fluffy pixel clouds (drawn on top of sky fill) ──────────
        fun drawCloud(baseX: Float, y: Float, scaleFactor: Float) =
            drawSkyCloud(scope, cw, baseX, y, scaleFactor, time, p, isSunset, isMorning)

        drawCloud(cw * 0.08f, ch * 0.10f, 0.7f)
        drawCloud(cw * 0.60f, ch * 0.18f, 1.0f)
        drawCloud(cw * 0.35f, ch * 0.28f, 0.5f)
    }
}
