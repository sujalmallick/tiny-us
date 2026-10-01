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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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

private data class DynamicNightStar(
    val fx: Float,
    val fy: Float,
    val sizeP: Float,
    val baseAlpha: Float,
    val color: Color,
    val twinkleSpeed: Float,
    val twinklePhase: Float,
    val hasCrossFlare: Boolean = false
)

private fun generateDynamicNightStars(seed: Long = 1337L): Array<DynamicNightStar> {
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
            if (random.nextFloat() < 0.82f) {
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

private var dynamicNightStars: Array<DynamicNightStar> = generateDynamicNightStars()

fun reallocateNightStars(seed: Long = System.currentTimeMillis()) {
    dynamicNightStars = generateDynamicNightStars(seed)
}

private val PEBBLE_FRACS = floatArrayOf(
    0.04f, 0.11f, 0.19f, 0.28f, 0.35f, 0.43f, 0.51f, 0.58f, 0.66f, 0.73f, 0.81f, 0.89f, 0.95f
)

private val TUFT_ROW_FACTORS = floatArrayOf(0.18f, 0.38f, 0.60f, 0.82f)

private val SAKURA_FLOWERS = listOf(
    Pair(Color(0xFFFFB5C2), Color(0xFFFFE66D)),
    Pair(Color(0xFFFF85A1), Color(0xFFFFF0F5)),
    Pair(Color(0xFFFFF0F5), Color(0xFFFFB5C2))
)

private val AUTUMN_FLOWERS = listOf(
    Pair(Color(0xFFF4A261), Color(0xFF8B1E1E)),
    Pair(Color(0xFFE9C46A), Color(0xFFD46A28)),
    Pair(Color(0xFFD62828), Color(0xFFFFD166))
)

private val DEFAULT_FLOWERS = listOf(
    Pair(Color(0xFFFFFFFF), Color(0xFFFFD166)),
    Pair(Color(0xFFFF4D6D), Color(0xFFFFE66D)),
    Pair(Color(0xFF48CAE4), Color(0xFFFFFFFF)),
    Pair(Color(0xFFFFB703), Color(0xFF9D4EDD)),
    Pair(Color(0xFFB5179E), Color(0xFFFFCAD4))
)

private val FLOWER_POSITIONS = listOf(
    Pair(0.06f, 4f),
    Pair(0.12f, 16f),
    Pair(0.19f, 8f),
    Pair(0.24f, 26f),
    Pair(0.31f, 12f),
    Pair(0.38f, 32f),
    Pair(0.44f, 6f),
    Pair(0.52f, 22f),
    Pair(0.59f, 14f),
    Pair(0.67f, 36f),
    Pair(0.72f, 10f),
    Pair(0.79f, 28f),
    Pair(0.85f, 8f),
    Pair(0.91f, 20f),
    Pair(0.96f, 34f)
)

// Precomputed Sky Bands and Splits for zero allocation per frame
private val SPLITS_4_BAND = listOf(0.35f, 0.65f, 0.85f)
private val SPLITS_5_BAND_PINK = listOf(0.25f, 0.50f, 0.72f, 0.88f)
private val SPLITS_5_BAND_AUTUMN = listOf(0.28f, 0.50f, 0.72f, 0.88f)
private val SPLITS_MORNING = listOf(0.45f, 0.75f)

private val BANDS_SUNSET_PINK = listOf(Color(0xFF881B4C), Color(0xFFC7366E), Color(0xFFF05D8E), Color(0xFFFF94B8), Color(0xFFFFD4E2))
private val BANDS_SUNSET_SAKURA = listOf(Color(0xFF38184C), Color(0xFF8B2662), Color(0xFFE05780), Color(0xFFFFB5C2))
private val BANDS_SUNSET_AUTUMN = listOf(Color(0xFF2B0E1E), Color(0xFF7A1C16), Color(0xFFC4451C), Color(0xFFE77F24), Color(0xFFF7BA3E))
private val BANDS_SUNSET_SNOW = listOf(Color(0xFF1C1A3A), Color(0xFF53416E), Color(0xFF9E6589), Color(0xFFDF9FB8))
private val BANDS_SUNSET_RAIN = listOf(Color(0xFF1E172B), Color(0xFF48334E), Color(0xFF7A4A58), Color(0xFFA66D60))
private val BANDS_SUNSET_SUMMER = listOf(Color(0xFF1F1035), Color(0xFF9E2A4B), Color(0xFFE3592B), Color(0xFFFFAA44), Color(0xFFFFD166))

private val BANDS_MORNING_SAKURA = listOf(Color(0xFF343B68), Color(0xFFFF9EAA), Color(0xFFFFE0E9))
private val BANDS_MORNING_AUTUMN = listOf(Color(0xFF2E334D), Color(0xFFE29578), Color(0xFFFFDDD2))
private val BANDS_MORNING_SNOW = listOf(Color(0xFF24324E), Color(0xFFA5B4D0), Color(0xFFE2EAFC))
private val BANDS_MORNING_RAIN = listOf(Color(0xFF202A3D), Color(0xFF64748B), Color(0xFF94A3B8))
private val BANDS_MORNING_SUMMER = listOf(Color(0xFF2C3E7A), Color(0xFFFF8C69), Color(0xFFFFD1A0))

private val BANDS_DAY_SAKURA = listOf(Color(0xFF6BA8D6), Color(0xFF98C5E8), Color(0xFFD2E6F5), Color(0xFFFFD6E7))
private val BANDS_DAY_AUTUMN = listOf(Color(0xFF4A85B8), Color(0xFF7CA6CD), Color(0xFFB5CDE1), Color(0xFFEAD5A8))
private val BANDS_DAY_SNOW = listOf(Color(0xFF5D99C6), Color(0xFF8EBCDE), Color(0xFFC8E0F2), Color(0xFFEBF4FA))
private val BANDS_DAY_RAIN = listOf(Color(0xFF334B68), Color(0xFF526E88), Color(0xFF7892A8), Color(0xFF9BB1C2))
private val BANDS_DAY_SUMMER = listOf(Color(0xFF3B9FE2), Color(0xFF68BCE8), Color(0xFFA5DCF4), Color(0xFFD4EFFC))

// Night Space Band Colors (5 fixed space bands)
private val NIGHT_SPACE_BASE_BANDS = arrayOf(
    Color(0xFF04060E),
    Color(0xFF070B1E),
    Color(0xFF0C132C),
    Color(0xFF121B3C),
    Color(0xFF18244D)
)

// Precomputed Grass Blade Height Table to eliminate trigonometry per blade
private val GRASS_HEIGHT_LOOKUP = FloatArray(64) { i ->
    val fakeBx = i * 8.8f
    2.5f + kotlin.math.abs(sin(fakeBx * 0.18f + 0.7f)) * 1.8f + kotlin.math.abs(sin(fakeBx * 0.08f)) * 1.4f
}
private val GRASS_SPARSE_HEIGHT_LOOKUP = FloatArray(64) { i ->
    val fakeBx = i * 19.2f + 7.68f
    1.5f + kotlin.math.abs(sin(fakeBx * 0.14f)) * 1.2f
}

// Fixed landscape arrays
private val RIVER_PEB_POSITIONS = floatArrayOf(0.08f, 0.28f, 0.42f, 0.62f, 0.76f, 0.92f)
private val RIVER_STEP_STONES_X = floatArrayOf(0.15f, 0.34f, 0.52f, 0.72f, 0.88f)
private val RIVER_STEP_STONES_Y = floatArrayOf(0.74f, 0.82f, 0.76f, 0.84f, 0.78f)
private val RIVER_CLOV_POSITIONS = floatArrayOf(0.12f, 0.35f, 0.54f, 0.70f, 0.88f)
private val RIVER_REED_OFFSETS = floatArrayOf(-9f, -6f, 7f, 9f)
private val RIVER_STALKS_X = floatArrayOf(-6f, -3f, 0f, 3f, 6f)
private val RIVER_STALKS_Y = floatArrayOf(0f, -2.5f, -4f, -1.5f, 1.5f)
private val RIVER_MUSH_OFFSETS = floatArrayOf(-8f, 0f, 8f)

private enum class TapTargetKind {
    BOY,
    GIRL,
    BOTH_CHARACTERS,
    RIDE_VEHICLE
}

@Composable
fun PixelWorldView(
    engine: SceneEngine,
    atmosphereMode: String,
    modifier: Modifier = Modifier
) {
    var frameNanos by remember { mutableLongStateOf(0L) }
    var viewportWidth by remember { mutableFloatStateOf(1080f) }
    var viewportHeight by remember { mutableFloatStateOf(2400f) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // High refresh rate adaptive frame ticker loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val delta = (nanos - lastNanos) / 1_000_000_000f
                    val dt = delta.coerceIn(0.005f, 0.05f)
                    engine.update(dt, viewportWidth, viewportHeight)
                }
                lastNanos = nanos
                frameNanos = nanos
            }
        }
    }

    // Sync atmosphereMode with engine
    LaunchedEffect(atmosphereMode) {
        engine.updateAtmosphereMode(atmosphereMode)
    }

    // Dynamically reallocate stars on scene change
    LaunchedEffect(engine.currentScene) {
        reallocateNightStars(System.currentTimeMillis())
    }

    // Determine current lighting & atmosphere from engine's unified time phase
    val timePhase = engine.timeOfDayPhase
    val isNight = timePhase.isNight
    val isSunset = timePhase.isSunset
    val isMorning = timePhase.isMorning

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    viewportWidth = size.width.toFloat()
                    viewportHeight = size.height.toFloat()
                }
            }
            .pointerInput(engine) {
                var pendingTapJob: Job? = null
                var pendingAction: (() -> Unit)? = null
                var tapCount = 0
                var lastTargetKind: TapTargetKind? = null
                var lastScene = engine.currentScene

                val flushPendingTap: () -> Unit = {
                    pendingTapJob?.cancel()
                    pendingTapJob = null
                    val action = pendingAction
                    pendingAction = null
                    tapCount = 0
                    lastTargetKind = null
                    action?.invoke()
                }

                detectTapGestures(
                    onLongPress = { tapOffset ->
                        pendingTapJob?.cancel()
                        pendingTapJob = null
                        pendingAction = null
                        tapCount = 0
                        lastTargetKind = null

                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val pixelScale = (w / 115f).coerceIn(3.0f, 5.0f)
                        val charPixelScale = pixelScale * 1.38f

                        if (engine.currentScene == com.example.scene.SceneType.EVENING_RIDE) {
                            engine.onTouchScooter(w, h)
                            return@detectTapGestures
                        }

                        val boyCenterX = w * engine.boy.worldX
                        val boyCenterY = h * engine.boy.worldY - 13f * charPixelScale
                        val girlCenterX = w * engine.girl.worldX
                        val girlCenterY = h * engine.girl.worldY - 13f * charPixelScale
                        val charRadius = 26f * charPixelScale

                        if (engine.isDreamMode) return@detectTapGestures
                        val isBoy = abs(tapOffset.x - boyCenterX) < charRadius && abs(tapOffset.y - boyCenterY) < charRadius
                        val isGirl = abs(tapOffset.x - girlCenterX) < charRadius && abs(tapOffset.y - girlCenterY) < charRadius
                        if (isBoy || isGirl) {
                            engine.onLongPressCharacter(w, h)
                        }
                    },
                    onTap = { tapOffset ->
                        if (engine.isDreamMode) {
                            engine.particles.spawnSparkles(tapOffset.x, tapOffset.y, 4)
                            return@detectTapGestures
                        }

                        if (lastScene != engine.currentScene) {
                            pendingTapJob?.cancel()
                            pendingTapJob = null
                            pendingAction = null
                            tapCount = 0
                            lastTargetKind = null
                            lastScene = engine.currentScene
                        }

                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val pixelScale = (w / 115f).coerceIn(3.0f, 5.0f)
                        val charPixelScale = pixelScale * 1.38f
                        val ny = tapOffset.y / h

                        // Dedicated touch targets for Evening Scooter Ride
                        if (engine.currentScene == com.example.scene.SceneType.EVENING_RIDE) {
                            val scootCenterX = w * 0.50f
                            val scootGroundY = h * 0.70f
                            val isGirlScoot = abs(tapOffset.x - (scootCenterX + 6f * pixelScale)) < 22f * pixelScale &&
                                abs(tapOffset.y - (scootGroundY - 26f * pixelScale)) < 26f * pixelScale
                            val isBoyScoot = abs(tapOffset.x - (scootCenterX - 18f * pixelScale)) < 22f * pixelScale &&
                                abs(tapOffset.y - (scootGroundY - 26f * pixelScale)) < 26f * pixelScale
                            val isScootBody = abs(tapOffset.x - scootCenterX) < 52f * pixelScale &&
                                abs(tapOffset.y - (scootGroundY - 14f * pixelScale)) < 30f * pixelScale

                            if (isGirlScoot || isBoyScoot || isScootBody) {
                                val currentAction: () -> Unit = when {
                                    isGirlScoot -> { { engine.onTouchGirlScooter(w, h) } }
                                    isBoyScoot -> { { engine.onTouchBoyScooter(w, h) } }
                                    else -> { { engine.onTouchScooter(w, h) } }
                                }

                                if (lastTargetKind != TapTargetKind.RIDE_VEHICLE) {
                                    flushPendingTap()
                                    lastTargetKind = TapTargetKind.RIDE_VEHICLE
                                    tapCount = 1
                                    pendingAction = currentAction
                                    pendingTapJob = coroutineScope.launch {
                                        delay(260L)
                                        val act = pendingAction
                                        pendingAction = null
                                        tapCount = 0
                                        lastTargetKind = null
                                        pendingTapJob = null
                                        act?.invoke()
                                    }
                                } else {
                                    pendingTapJob?.cancel()
                                    tapCount++
                                    if (tapCount >= 3) {
                                        pendingAction = null
                                        tapCount = 0
                                        lastTargetKind = null
                                        pendingTapJob = null
                                        engine.onTripleTapRide(w, h)
                                    } else {
                                        pendingAction = currentAction
                                        pendingTapJob = coroutineScope.launch {
                                            delay(260L)
                                            val act = pendingAction
                                            pendingAction = null
                                            tapCount = 0
                                            lastTargetKind = null
                                            pendingTapJob = null
                                            act?.invoke()
                                        }
                                    }
                                }
                                return@detectTapGestures
                            }

                            // World objects in Evening Ride scene (0ms immediate latency)
                            flushPendingTap()
                            // Kalinga Temple in sky
                            if (tapOffset.y < h * 0.65f) {
                                engine.onTouchTempleSpire(w, h)
                                return@detectTapGestures
                            }
                            engine.particles.spawnSparkles(tapOffset.x, tapOffset.y, 4)
                            return@detectTapGestures
                        }

                        val isLoftBedTap = engine.currentScene.environment != EnvironmentType.COZY_LOFT || tapOffset.y <= (h * 0.55f + 2f * pixelScale)
                        val isMomoCartProp = engine.currentScene.environment == EnvironmentType.MOMO_STALL && (
                            (abs(tapOffset.x - (w * 0.50f + 31.5f * pixelScale)) < 15f * pixelScale &&
                             abs(tapOffset.y - (h * 0.69f - 29f * pixelScale)) < 15f * pixelScale) ||
                            (abs(tapOffset.x - (w * 0.50f - 13f * pixelScale)) < 20f * pixelScale &&
                             abs(tapOffset.y - (h * 0.69f - 37f * pixelScale)) < 20f * pixelScale)
                        )

                        val boyCenterX = w * engine.boy.worldX
                        val boyCenterY = h * engine.boy.worldY - 13f * charPixelScale
                        val girlCenterX = w * engine.girl.worldX
                        val girlCenterY = h * engine.girl.worldY - 13f * charPixelScale
                        val charHitX = 28f * charPixelScale
                        val charHitY = 28f * charPixelScale

                        val isBoyHit = isLoftBedTap && !isMomoCartProp && abs(tapOffset.x - boyCenterX) < charHitX && abs(tapOffset.y - boyCenterY) < charHitY
                        val isGirlHit = isLoftBedTap && !isMomoCartProp && abs(tapOffset.x - girlCenterX) < charHitX && abs(tapOffset.y - girlCenterY) < charHitY

                        // 1. Check if tap is between characters or tapping both (Couple Hug!)
                        val midX = (boyCenterX + girlCenterX) / 2f
                        val midY = (boyCenterY + girlCenterY) / 2f
                        val charDistance = abs(girlCenterX - boyCenterX)
                        val closeTogether = abs(engine.boy.worldX - engine.girl.worldX) < 0.34f
                        val isMidHit = closeTogether && abs(tapOffset.x - midX) < (charDistance * 0.35f) && abs(tapOffset.y - midY) < charHitY

                        val charTarget: TapTargetKind? = when {
                            isMidHit -> TapTargetKind.BOTH_CHARACTERS
                            isBoyHit && isGirlHit -> if (tapOffset.x < midX) TapTargetKind.BOY else TapTargetKind.GIRL
                            isBoyHit -> TapTargetKind.BOY
                            isGirlHit -> TapTargetKind.GIRL
                            else -> null
                        }

                        if (charTarget != null) {
                            if (lastTargetKind != charTarget) {
                                flushPendingTap()
                                lastTargetKind = charTarget
                                tapCount = 1
                                pendingAction = when (charTarget) {
                                    TapTargetKind.BOY -> { { engine.onTouchBoy(w, h) } }
                                    TapTargetKind.GIRL -> { { engine.onTouchGirl(w, h) } }
                                    TapTargetKind.BOTH_CHARACTERS -> { { engine.onTouchBothCharacters(w, h) } }
                                    else -> null
                                }
                                pendingTapJob = coroutineScope.launch {
                                    delay(260L)
                                    val act = pendingAction
                                    pendingAction = null
                                    tapCount = 0
                                    lastTargetKind = null
                                    pendingTapJob = null
                                    act?.invoke()
                                }
                            } else {
                                // 2nd tap on the same character target!
                                pendingTapJob?.cancel()
                                pendingAction = null
                                tapCount = 0
                                lastTargetKind = null
                                pendingTapJob = null
                                when (charTarget) {
                                    TapTargetKind.BOY -> engine.onDoubleTapBoy(w, h)
                                    TapTargetKind.GIRL -> engine.onDoubleTapGirl(w, h)
                                    TapTargetKind.BOTH_CHARACTERS -> engine.onTouchBothCharacters(w, h)
                                    else -> {}
                                }
                            }
                            return@detectTapGestures
                        }

                        // Not a character target — World Object!
                        // Flush any pending character tap immediately so no input is lost
                        flushPendingTap()

                        // 3. Cat
                        val catX = w * engine.catWorldX
                        val catY = h * engine.catWorldY - 7f * pixelScale
                        if (abs(tapOffset.x - catX) < 22f * pixelScale && abs(tapOffset.y - catY) < 18f * pixelScale) {
                            engine.onTouchCat(w, h)
                            return@detectTapGestures
                        }

                        // 3b. Ambient Birds (perched or pecking)
                        if (engine.birdSystem.onTouchBird(tapOffset.x, tapOffset.y, pixelScale)) {
                            engine.audio.playBirdChirp()
                            return@detectTapGestures
                        }

                        // 4. In-scene objects
                        when (engine.currentScene.environment) {
                            EnvironmentType.MEADOW -> {
                                // Cottage Door
                                if (abs(tapOffset.x - w * 0.20f) < 26f * pixelScale && abs(tapOffset.y - h * 0.64f) < 26f * pixelScale) {
                                    engine.onTouchCottageDoor()
                                    return@detectTapGestures
                                }
                                // Flowers
                                if (tapOffset.y > h * 0.65f) {
                                    engine.onTouchFlower(w, h)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.TREE_HILL -> {
                                if (abs(tapOffset.x - w * 0.5f) < 45f * pixelScale && tapOffset.y < h * 0.62f) {
                                    engine.onTouchTree(w, h)
                                    return@detectTapGestures
                                }
                                if (tapOffset.y > h * 0.66f) {
                                    engine.onTouchFlower(w, h)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.KITCHEN -> {
                                // 1. Cottage doorway (cycle rooms)
                                if (tapOffset.x < w * 0.16f && tapOffset.y in (h * 0.45f)..(h * 0.72f)) {
                                    engine.onCycleCottageRoom()
                                    return@detectTapGestures
                                }
                                // 2. Stew Pot on Stovetop (left burner of counter)
                                if (abs(tapOffset.x - w * 0.60f) < 18f * pixelScale && abs(tapOffset.y - (h * 0.67f - 18f * pixelScale)) < 18f * pixelScale) {
                                    engine.onTouchPot(w, h)
                                    return@detectTapGestures
                                }
                                // 3. Cutting board & ingredients (right side of counter)
                                if (abs(tapOffset.x - w * 0.72f) < 18f * pixelScale && abs(tapOffset.y - (h * 0.67f - 14f * pixelScale)) < 16f * pixelScale) {
                                    engine.onTouchIngredients(w, h)
                                    return@detectTapGestures
                                }
                                // 4. Built-in Oven & Lower Cabinets
                                if (abs(tapOffset.x - w * 0.65f) < 22f * pixelScale && abs(tapOffset.y - (h * 0.67f - 6f * pixelScale)) < 14f * pixelScale) {
                                    engine.onTouchCabinet(w, h)
                                    return@detectTapGestures
                                }
                                // 5. Retro Refrigerator
                                if (tapOffset.x > w * 0.80f && tapOffset.y in (h * 0.67f - 56f * pixelScale)..(h * 0.67f)) {
                                    engine.onTouchFridge(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // 6. Farmhouse Apron Sink (under window)
                                if (abs(tapOffset.x - w * 0.28f) < 20f * pixelScale && abs(tapOffset.y - (h * 0.65f - 16f * pixelScale)) < 18f * pixelScale) {
                                    engine.onTouchSink(w, h)
                                    return@detectTapGestures
                                }
                                // 7. Kitchen Farmhouse Dining Table (center floor)
                                val floorH = h - h * 0.65f
                                val juteY = h * 0.65f + floorH * 0.44f
                                val tblY = juteY + 7f * pixelScale
                                if (abs(tapOffset.x - w * 0.50f) < 28f * pixelScale && abs(tapOffset.y - (tblY + 8f * pixelScale)) < 16f * pixelScale) {
                                    engine.onTouchKitchenTable(w, h)
                                    return@detectTapGestures
                                }
                                // 8. Wall Clock
                                if (abs(tapOffset.x - w * 0.49f) < 16f * pixelScale && abs(tapOffset.y - (h * 0.38f + 16f * pixelScale)) < 16f * pixelScale) {
                                    engine.onTouchKitchenClock(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // 9. Retro Pedal Dustbin
                                val dustbinX = w * 0.785f
                                val dustbinY = h * 0.67f
                                if (abs(tapOffset.x - dustbinX) < 14f * pixelScale && abs(tapOffset.y - (dustbinY - 8f * pixelScale)) < 14f * pixelScale) {
                                    engine.onTouchDustbin(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // 10. Farmer's Produce Crate (lower left pantry corner)
                                val crateX = w * 0.13f
                                val crateY = h * 0.65f + floorH * 0.74f
                                if (abs(tapOffset.x - crateX) < 20f * pixelScale && abs(tapOffset.y - crateY) < 18f * pixelScale) {
                                    engine.onTouchKitchenCrate(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // 11. Glazed Ceramic Floor Planter (lower right corner)
                                val planterX = w * 0.86f
                                val planterY = h * 0.65f + floorH * 0.74f
                                if (abs(tapOffset.x - planterX) < 20f * pixelScale && abs(tapOffset.y - planterY) < 20f * pixelScale) {
                                    engine.onTouchKitchenPlanter(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // 12. Vintage Kitchen Step Stool (tucked by dining chair)
                                val stoolX = w * 0.50f - 26f * pixelScale - 16f * pixelScale
                                val stoolY = juteY + 16f * pixelScale
                                if (abs(tapOffset.x - stoolX) < 16f * pixelScale && abs(tapOffset.y - stoolY) < 16f * pixelScale) {
                                    engine.onTouchKitchenStool(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.LIVING_ROOM -> {
                                // Feature 5: Floor lamp tap (left side of room) — checked first
                                val lampX = w * 0.20f
                                val lampGroundY = h * 0.68f
                                val lampHitRadius = 24f * pixelScale
                                if (abs(tapOffset.x - lampX) < lampHitRadius && tapOffset.y in (lampGroundY - 80f * pixelScale)..(lampGroundY + 10f * pixelScale)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    engine.onTouchLivingRoomLamp(w, h)
                                    return@detectTapGestures
                                }
                                // Connecting doorway to Kitchen / Outdoor (strictly on the far left)
                                if (tapOffset.x < w * 0.12f && tapOffset.y in (h * 0.45f)..(h * 0.72f)) {
                                    engine.onCycleCottageRoom()
                                    return@detectTapGestures
                                }
                                if (abs(tapOffset.x - w * 0.28f) < 24f * pixelScale && abs(tapOffset.y - h * 0.38f) < 24f * pixelScale) {
                                    engine.onTouchPhotoFrame()
                                    return@detectTapGestures
                                }
                                // Wall Calendar (centre wall, well away from window) → opens Special Calendar
                                if (abs(tapOffset.x - w * 0.50f) < 18f * pixelScale && abs(tapOffset.y - h * 0.30f) < 16f * pixelScale) {
                                    engine.onTouchWallCalendar()
                                    return@detectTapGestures
                                }
                                // Cottage Wardrobe (interactive outfit selector)
                                val wardrobeX = w * 0.85f
                                val wardrobeY = h * 0.65f
                                if (abs(tapOffset.x - wardrobeX) < 22f * pixelScale && abs(tapOffset.y - (wardrobeY - 34f * pixelScale)) < 36f * pixelScale) {
                                    engine.onTouchWardrobe()
                                    return@detectTapGestures
                                }
                                val floorH = h - h * 0.65f
                                // Low Wooden Coffee Table (on rug directly in front of couch)
                                val tableX = w * 0.50f
                                val tableY = h * 0.65f + 23f * pixelScale
                                if (abs(tapOffset.x - tableX) < 28f * pixelScale && abs(tapOffset.y - tableY) < 16f * pixelScale) {
                                    engine.onTouchCoffeeTable(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Knitted Pouf (right edge of rug, beside coffee table)
                                val pfX = w * 0.70f
                                val pfY = h * 0.65f + 25f * pixelScale
                                if (abs(tapOffset.x - pfX) < 16f * pixelScale && abs(tapOffset.y - pfY) < 16f * pixelScale) {
                                    engine.onTouchPouf(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Mochi's Cozy Cardboard Box (bottom-left corner)
                                val boxX = w * 0.17f
                                val boxY = h * 0.65f + floorH * 0.75f
                                if (abs(tapOffset.x - boxX) < 18f * pixelScale && abs(tapOffset.y - boxY) < 16f * pixelScale) {
                                    engine.onTouchCardboardBox(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Woven Storage Basket (beside Mochi's box in bottom-left corner)
                                val bskX = w * 0.30f
                                val bskY = h * 0.65f + floorH * 0.75f
                                if (abs(tapOffset.x - bskX) < 18f * pixelScale && abs(tapOffset.y - bskY) < 16f * pixelScale) {
                                    engine.onTouchStorageBasket(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Wooden Magazine & Record Rack / Dream Journal (bottom-right corner)
                                val rackX = w * 0.80f
                                val rackY = h * 0.65f + floorH * 0.74f
                                if (abs(tapOffset.x - rackX) < 20f * pixelScale && abs(tapOffset.y - rackY) < 18f * pixelScale) {
                                    engine.onTouchMagazineRack(tapOffset.x, tapOffset.y)
                                    engine.onTouchNightstandJournal()
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.PATH_NIGHT -> {
                                if (abs(tapOffset.x - w * 0.82f) < 24f * pixelScale && abs(tapOffset.y - h * 0.65f) < 24f * pixelScale) {
                                    engine.onTouchMailbox()
                                    return@detectTapGestures
                                }
                                if (abs(tapOffset.x - w * 0.65f) < 24f * pixelScale && tapOffset.y < h * 0.65f) {
                                    engine.onTouchStreetlamp()
                                    return@detectTapGestures
                                }
                                val curbY = h * 0.66f + 30f * pixelScale
                                val curbH = h - curbY
                                // Miniature Stone Garden Pagoda Lantern (Cluster A)
                                val pagX = w * 0.20f
                                val pagY = curbY + curbH * 0.38f
                                if (abs(tapOffset.x - pagX) < 20f * pixelScale && abs(tapOffset.y - pagY) < 18f * pixelScale) {
                                    engine.onTouchPagodaLantern(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Night Lavender & Fireflies (Cluster B)
                                val lavX = w * 0.80f
                                val lavY = curbY + curbH * 0.36f
                                if (abs(tapOffset.x - lavX) < 20f * pixelScale && abs(tapOffset.y - lavY) < 18f * pixelScale) {
                                    engine.onTouchLavenderPatch(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Stepping river stones / glowing mushrooms (Cluster C)
                                if (tapOffset.y > curbY + curbH * 0.60f) {
                                    engine.onTouchMushrooms(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.TWILIGHT -> {
                                if (tapOffset.y > h * 0.65f) {
                                    engine.onTouchFlower(w, h)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.MOMO_STALL -> {
                                val groundY = h * 0.69f
                                val signCenterY = groundY - 107f * pixelScale
                                // Momo signboard
                                if (abs(tapOffset.x - w * 0.50f) < 42f * pixelScale && abs(tapOffset.y - signCenterY) < 16f * pixelScale) {
                                    engine.onTouchMomoSign(w, h)
                                    return@detectTapGestures
                                }
                                // Momo steamer on cart
                                val steamerCenterX = w * 0.50f - 13f * pixelScale
                                val steamerCenterY = groundY - 37f * pixelScale
                                if (abs(tapOffset.x - steamerCenterX) < 22f * pixelScale && abs(tapOffset.y - steamerCenterY) < 22f * pixelScale) {
                                    engine.onTouchMomoSteamer(w, h)
                                    return@detectTapGestures
                                }
                                // Spicy chutney bowl on cart
                                val bowlCenterX = w * 0.50f + 31.5f * pixelScale
                                val bowlCenterY = groundY - 29f * pixelScale
                                if (abs(tapOffset.x - bowlCenterX) < 18f * pixelScale && abs(tapOffset.y - bowlCenterY) < 18f * pixelScale) {
                                    engine.onTouchChutney(w, h)
                                    return@detectTapGestures
                                }
                                val pathY = h * 0.66f + 4f * pixelScale
                                val curbY = pathY + 26f * pixelScale
                                val curbH = h - curbY
                                // A-Frame Chalkboard Menu (on sidewalk left of stall)
                                val chkX = w * 0.28f
                                val chkY = pathY + 12f * pixelScale
                                if (abs(tapOffset.x - chkX) < 16f * pixelScale && abs(tapOffset.y - chkY) < 16f * pixelScale) {
                                    engine.onTouchChalkboard(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Bamboo momo steamers crate (on sidewalk right of stall)
                                val crateX = w * 0.74f
                                val crateY = pathY + 12f * pixelScale
                                if (abs(tapOffset.x - crateX) < 18f * pixelScale && abs(tapOffset.y - crateY) < 16f * pixelScale) {
                                    engine.onTouchBambooCrate(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Kitty milk saucer (beside cart on sidewalk)
                                val saucerX = w * 0.38f
                                val saucerY = pathY + 16f * pixelScale
                                if (abs(tapOffset.x - saucerX) < 16f * pixelScale && abs(tapOffset.y - saucerY) < 14f * pixelScale) {
                                    engine.onTouchMilkSaucer(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Outdoor Street Dining Table & Stools (lower terrace)
                                val tblX = w * 0.50f
                                val tblY = curbY + curbH * 0.55f
                                if (abs(tapOffset.x - tblX) < 28f * pixelScale && abs(tapOffset.y - tblY) < 18f * pixelScale) {
                                    engine.onTouchDiningTable(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.EVENING_ROAD -> {}
                            EnvironmentType.COZY_LOFT -> {
                                // Door tap on left to cycle rooms
                                if (tapOffset.x < w * 0.22f && tapOffset.y in (h * 0.45f)..(h * 0.72f)) {
                                    engine.onCycleCottageRoom()
                                    return@detectTapGestures
                                }
                                val windowStartX = w * 0.32f
                                val floorY = h * 0.55f
                                val moonX = windowStartX + (w - windowStartX) * 0.74f
                                val moonY = h * 0.12f
                                val moonDist = kotlin.math.hypot(tapOffset.x - moonX, tapOffset.y - moonY)

                                // 1. Moon tap
                                if (moonDist < 26f * pixelScale) {
                                    engine.onTouchLoftMoon(w, h)
                                    return@detectTapGestures
                                }

                                // 2. Table Lamp & Nightstand beside bed (CRITICAL: MUST BE PRIORITIZED BEFORE WINDOW!)
                                val lampHitLeft = windowStartX - 4f * pixelScale
                                val lampHitRight = windowStartX + 18f * pixelScale
                                val lampHitTop = floorY - 30f * pixelScale
                                val lampHitBottom = floorY + 2f * pixelScale
                                if (tapOffset.x in lampHitLeft..lampHitRight && tapOffset.y in lampHitTop..lampHitBottom) {
                                    engine.onTouchLoftLamp(w, h)
                                    return@detectTapGestures
                                }

                                // 3. Turntable & Record Player on right wall (MUST BE PRIORITIZED BEFORE WINDOW!)
                                if (tapOffset.x > w * 0.78f && tapOffset.y in (floorY - 24f * pixelScale)..(floorY + 12f * pixelScale)) {
                                    engine.onTouchLoftRecordPlayer(w, h)
                                    return@detectTapGestures
                                }

                                // 4. Pet Mochi sleeping peacefully on left side of the daybed blanket
                                val mochiCenterX = w * 0.50f
                                val mochiCenterY = floorY - 3f * pixelScale
                                if (kotlin.math.hypot(tapOffset.x - mochiCenterX, tapOffset.y - mochiCenterY) < 18f * pixelScale) {
                                    engine.onTouchLoftMochi(w, h)
                                    return@detectTapGestures
                                }

                                // 5. Bookshelf on left wall
                                if (tapOffset.x < windowStartX - 4f * pixelScale && tapOffset.y < floorY) {
                                    engine.onTouchLoftBookshelf(w, h)
                                    return@detectTapGestures
                                }

                                // 6. Coffee Table with Tea Mugs, Cookies & Lantern
                                if (tapOffset.x in (w * 0.38f)..(w * 0.72f) && tapOffset.y in (floorY + 2f * pixelScale)..(floorY + 18f * pixelScale)) {
                                    engine.onTouchLoftTable(w, h)
                                    return@detectTapGestures
                                }

                                // 6b. Floor reading nook / Dream Journal (cushion, books, lantern & basket)
                                val loftFloorH = h * 0.72f - floorY
                                if (tapOffset.x in (w * 0.16f)..(w * 0.70f) && tapOffset.y in (floorY + loftFloorH * 0.35f)..(floorY + loftFloorH * 0.85f)) {
                                    engine.onTouchLoftReadingNook(tapOffset.x, tapOffset.y)
                                    engine.onTouchNightstandJournal()
                                    return@detectTapGestures
                                }

                                // 7. Balcony railing & hanging fairy lights / ivy
                                if (tapOffset.y >= h * 0.72f) {
                                    engine.onTouchLoftPlant(w, h)
                                    return@detectTapGestures
                                }

                                // 8. Sofa / Daybed / Cuddle area
                                if (tapOffset.x in (w * 0.38f)..(w * 0.82f) && tapOffset.y in (floorY - 20f * pixelScale)..(floorY + 4f * pixelScale)) {
                                    engine.onTouchLoftSofa(w, h)
                                    return@detectTapGestures
                                }

                                // 9. Loft Panoramic Window / Skyline (Background catch-all behind furniture)
                                if (tapOffset.x > windowStartX && tapOffset.y < floorY) {
                                    engine.onTouchLoftWindow(w, h)
                                    return@detectTapGestures
                                }
                            }
                        }

                        // 5. Turn characters gaze towards tap position if idling
                        val tapNormX = tapOffset.x / w
                        if (engine.currentScene != com.example.scene.SceneType.EVENING_RIDE && !engine.isWatchSceneActive) {
                            if (engine.boy.pose == com.example.engine.CharacterPose.IDLE) {
                                engine.boy.direction = if (tapNormX < engine.boy.worldX) com.example.engine.Direction.LEFT else com.example.engine.Direction.RIGHT
                            }
                            if (engine.girl.pose == com.example.engine.CharacterPose.IDLE) {
                                engine.girl.direction = if (tapNormX < engine.girl.worldX) com.example.engine.Direction.LEFT else com.example.engine.Direction.RIGHT
                            }
                        }

                        // 6. Sky / Stars & Constellations
                        if (ny < 0.48f) {
                            val hasStarfield = isNight && engine.currentScene.environment in listOf(
                                EnvironmentType.MEADOW,
                                EnvironmentType.TREE_HILL,
                                EnvironmentType.PATH_NIGHT,
                                EnvironmentType.MOMO_STALL,
                                EnvironmentType.TWILIGHT
                            )
                            if (hasStarfield) {
                                // Constellation 1: The Two Hearts (Binary Stars)
                                if (tapNormX in 0.08f..0.32f && ny in 0.05f..0.28f) {
                                    engine.onTouchConstellation("The Two Hearts", "Two shining stars linked across the sky", tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Constellation 2: The Celestial Teapot (Center sky warmth)
                                if (tapNormX in 0.33f..0.52f && ny in 0.05f..0.34f) {
                                    engine.onTouchConstellation("The Celestial Teapot", "Pouring warmth and sweet tea over our world", tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Constellation 3: Starlight Trail (Guiding starry road)
                                if (tapNormX in 0.58f..0.92f && ny in 0.06f..0.28f) {
                                    engine.onTouchConstellation("Starlight Trail", "Guiding our evening ride through gentle breezes", tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Other sky taps spawn shooting star & fireflies
                                engine.onTouchSky(tapOffset.x, tapOffset.y, w, isNight = true)
                            } else if (engine.isCurrentSceneOutdoor) {
                                // Daytime outdoor sky tap
                                engine.onTouchSky(tapOffset.x, tapOffset.y, w, isNight = false)
                            }
                        } else {
                            // Touch ground / grass sparkles and grass puffs
                            engine.particles.spawnSparkles(tapOffset.x, tapOffset.y, 4)
                            engine.particles.spawnGrassPuff(tapOffset.x, tapOffset.y, 6)
                            engine.particles.spawnDandelionFluff(tapOffset.x, tapOffset.y)
                            engine.audio.playLeafRustle()

                            // If outdoor and on grass in Sakura or Autumn weather, clean nearby leaves/petals
                            if (engine.isCurrentSceneOutdoor && ny >= 0.65f &&
                                (engine.weather == WeatherType.SAKURA || engine.weather == WeatherType.AUTUMN)
                            ) {
                                engine.particles.sweepGroundParticles(
                                    touchX = tapOffset.x,
                                    touchY = tapOffset.y,
                                    cw = w,
                                    ch = h,
                                    radiusPx = 38f * pixelScale,
                                    dragDeltaX = 0f,
                                    dragDeltaY = -2.5f
                                )
                            }

                            // Command Mochi to trot through the grass / across the floor to the tapped spot!
                            val targetNormX = tapOffset.x / w
                            val targetNormY = tapOffset.y / h
                            engine.commandCatWalkTo(targetNormX, targetNormY, w, h)
                        }
                    }
                )
            }
            // Feature 4: Mochi drag-and-drop & Grass Swipe Cleaning for cherry blossoms and leaves
            .pointerInput(engine) {
                var isMochiBeingDragged = false
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val pixelScale = (w / 115f).coerceIn(3.0f, 5.0f)
                        val catX = w * engine.catWorldX
                        val catY = h * engine.catWorldY - 7f * pixelScale
                        isMochiBeingDragged = abs(offset.x - catX) < 22f * pixelScale &&
                            abs(offset.y - catY) < 18f * pixelScale

                        // Swipe starting on grass cleans cherry blossoms or autumn leaves
                        if (!isMochiBeingDragged && offset.y >= h * 0.65f && engine.isCurrentSceneOutdoor &&
                            (engine.weather == WeatherType.SAKURA || engine.weather == WeatherType.AUTUMN)
                        ) {
                            val swept = engine.particles.sweepGroundParticles(
                                touchX = offset.x,
                                touchY = offset.y,
                                cw = w,
                                ch = h,
                                radiusPx = 42f * pixelScale,
                                dragDeltaX = 0f,
                                dragDeltaY = -2f
                            )
                            if (swept > 0) {
                                engine.audio.playLeafRustle()
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isMochiBeingDragged) {
                            change.consume()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val newX = (change.position.x / w).coerceIn(0.10f, 0.90f)
                            val newY = (change.position.y / h).coerceIn(0.55f, 0.85f)
                            engine.catWorldX = newX
                            engine.catWorldY = newY
                            engine.catTargetX = newX
                            engine.catTargetY = newY
                            engine.catState = CatState.WALK_FOLLOW
                        } else {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val isGrassArea = change.position.y >= h * 0.65f
                            if (engine.isCurrentSceneOutdoor && isGrassArea &&
                                (engine.weather == WeatherType.SAKURA || engine.weather == WeatherType.AUTUMN)
                            ) {
                                change.consume()
                                val pixelScale = (w / 115f).coerceIn(3.0f, 5.0f)
                                val sweepRadius = 42f * pixelScale
                                val swept = engine.particles.sweepGroundParticles(
                                    touchX = change.position.x,
                                    touchY = change.position.y,
                                    cw = w,
                                    ch = h,
                                    radiusPx = sweepRadius,
                                    dragDeltaX = dragAmount.x,
                                    dragDeltaY = dragAmount.y
                                )
                                if (swept > 0) {
                                    engine.audio.playLeafRustle()
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        if (isMochiBeingDragged) {
                            engine.catState = CatState.SITTING_PURR
                            engine.catSleeping = false
                            engine.catFacingLeft = false
                            engine.audio.playCatPurr()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            engine.particles.spawnHeart(w * engine.catWorldX, h * engine.catWorldY - 20f, Color(0xFFFF8FA3))
                            engine.showMessage("Mochi settled cozily right here.", duration = 2.0f)
                            isMochiBeingDragged = false
                        }
                    },
                    onDragCancel = { isMochiBeingDragged = false }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentFrame = frameNanos // Explicitly read frameNanos to trigger continuous 60 FPS redraws!
            val cw = size.width
            val ch = size.height
            val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)
            val charPixelScale = pixelScale * 1.38f

            // Dynamic Weather outdoor check
            val isOutdoor = engine.isCurrentSceneOutdoor

            // 1. Environmental Background
            drawEnvironment(
                scope = this,
                cw = cw,
                ch = ch,
                env = engine.currentScene.environment,
                isNight = isNight,
                isSunset = isSunset,
                isMorning = isMorning,
                timeSeconds = engine.sceneTime,
                pixelScale = pixelScale,
                engine = engine
            )

            // 1b. Ground fallen particles (leaves, sakura petals, snow on grass)
            if (isOutdoor) {
                drawGroundFallenParticles(this, engine.particles.fallenParticles, pixelScale, cw, ch)
            }

            // 1c. Background seasonal particles (drawn behind characters so they never obscure characters or objects)
            drawBackgroundSeasonalParticles(this, engine.particles.particles, pixelScale)

            // 2. Characters
            val boyX = cw * engine.boy.worldX
            val boyY = ch * engine.boy.worldY
            val girlX = cw * engine.girl.worldX
            val girlY = ch * engine.girl.worldY

            val isKissing = (engine.boy.pose == com.example.engine.CharacterPose.KISS ||
                             engine.girl.pose == com.example.engine.CharacterPose.KISS)
            val isHugging = (engine.boy.pose == com.example.engine.CharacterPose.HUG ||
                             engine.girl.pose == com.example.engine.CharacterPose.HUG ||
                             engine.boy.pose == com.example.engine.CharacterPose.SIT_SNUGGLE ||
                             engine.girl.pose == com.example.engine.CharacterPose.SIT_SNUGGLE)

            // When kissing or hugging, smoothly and intimately bring the couple close together!
            val midCharX = (boyX + girlX) / 2f
            val cuddleEased = CharacterMotionTween.easeInOutCubic(engine.cuddleProgress)
            val targetHugOffset = when {
                isKissing -> 6.2f * charPixelScale
                else -> 4.8f * charPixelScale
            }
            val effectiveBoyX = boyX + ((midCharX - targetHugOffset) - boyX) * cuddleEased
            val effectiveGirlX = girlX + ((midCharX + targetHugOffset) - girlX) * cuddleEased
            val effectiveBoyY = boyY + (maxOf(boyY, girlY) - boyY) * cuddleEased
            val effectiveGirlY = girlY + (maxOf(boyY, girlY) - girlY) * cuddleEased
            val catY = ch * engine.catWorldY
            val isCatInScene = engine.currentScene.environment != EnvironmentType.COZY_LOFT &&
                engine.currentScene != com.example.scene.SceneType.EVENING_RIDE

            if (engine.currentScene == com.example.scene.SceneType.EVENING_RIDE) {
                val hopBounce = if (engine.scooterHonkTimer > 0f) {
                    val t = ((0.8f - engine.scooterHonkTimer) / 0.8f).coerceIn(0f, 1f)
                    -sin(t * Math.PI.toFloat()) * 5f * pixelScale
                } else 0f
                WorldSprites.drawScooterWithCouple(
                    scope = this,
                    cx = cw * 0.50f,
                    groundY = ch * 0.70f + hopBounce,
                    p = pixelScale,
                    timeSeconds = engine.sceneTime,
                    boyEmotion = engine.boy.emotion,
                    girlEmotion = engine.girl.emotion,
                    boyOutfitIndex = engine.boy.outfitIndex,
                    girlOutfitIndex = engine.girl.outfitIndex,
                    boyAccessoryIndex = engine.boy.accessoryIndex,
                    girlAccessoryIndex = engine.girl.accessoryIndex,
                    boyWearsGlasses = engine.boy.wearsGlasses
                )

                if (engine.scooterHonkTimer > 0f) {
                    val dur = 0.8f
                    val t = ((dur - engine.scooterHonkTimer) / dur).coerceIn(0f, 1f)
                    val p = pixelScale
                    val cx = cw * 0.50f
                    val groundY = ch * 0.70f + hopBounce
                    val scootY = groundY + 5.5f * p + sin(engine.sceneTime * 18f) * 0.8f * p
                    val wheelCenterY = scootY - 8 * p
                    val frontApronX = cx + 14 * p
                    val apronTopY = wheelCenterY - 28 * p
                    val beamY = apronTopY + 2 * p
                    val flashAlpha = sin(t * Math.PI.toFloat()).coerceIn(0f, 1f)

                    // Brilliant LED headlight beam flash
                    drawRect(Color(0x99FFF9DB).copy(alpha = flashAlpha * 0.65f), androidx.compose.ui.geometry.Offset(frontApronX + 16 * p, beamY - 4 * p), Size(75 * p, 34 * p))
                    drawRect(Color(0xFFFFD166).copy(alpha = flashAlpha), androidx.compose.ui.geometry.Offset(frontApronX + 13 * p, apronTopY + 1 * p), Size(4 * p, 8 * p))

                    // Honk sonic waves expanding forward
                    for (waveIdx in 0..2) {
                        val wT = ((t * 1.8f - waveIdx * 0.25f)).coerceIn(0f, 1f)
                        if (wT > 0f) {
                            val wRadius = (8f + wT * 26f) * p
                            val wAlpha = (1f - wT) * 0.8f
                            drawCircle(
                                color = Color(0xFFFFD166).copy(alpha = wAlpha),
                                radius = wRadius,
                                center = androidx.compose.ui.geometry.Offset(frontApronX + 16 * p, beamY + 4 * p),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f * p)
                            )
                        }
                    }
                }
            } else if (engine.currentScene.environment == EnvironmentType.COZY_LOFT) {
                LoftSprites.drawCuddledCouple(
                    scope = this,
                    boyX = boyX,
                    girlX = girlX,
                    floorY = ch * 0.55f,
                    p = pixelScale * 1.10f,
                    timeSeconds = engine.sceneTime,
                    boyEmotion = engine.boy.emotion,
                    girlEmotion = engine.girl.emotion,
                    isKissing = isKissing,
                    isReadingBook = engine.loftBookReading,
                    boyOutfitIndex = engine.boy.outfitIndex,
                    girlOutfitIndex = engine.girl.outfitIndex,
                    boyAccessoryIndex = engine.boy.accessoryIndex,
                    girlAccessoryIndex = engine.girl.accessoryIndex,
                    boyWearsGlasses = engine.boy.wearsGlasses
                )
            } else {
                val isHoldingUmbrella = engine.weather == com.example.scene.WeatherType.RAIN && isOutdoor
                val isSnow = engine.weather == com.example.scene.WeatherType.SNOW && isOutdoor
                val isSitting = engine.boy.pose == com.example.engine.CharacterPose.SIT

                // Ensure proper facing direction when kissing or hugging
                val origBoyDir = engine.boy.direction
                val origGirlDir = engine.girl.direction
                if (isKissing || isHugging) {
                    engine.boy.direction = com.example.engine.Direction.RIGHT
                    engine.girl.direction = com.example.engine.Direction.LEFT
                }

                fun drawBoy() {
                    PixelArtRenderer.drawCharacter(
                        drawScope = this,
                        char = engine.boy,
                        centerX = effectiveBoyX,
                        bottomY = effectiveBoyY,
                        pixelSize = charPixelScale,
                        isHoldingUmbrella = isHoldingUmbrella,
                        isSnow = isSnow,
                        isSpeaking = !engine.boySpeechText.isNullOrEmpty()
                    )
                }

                fun drawGirl() {
                    PixelArtRenderer.drawCharacter(
                        drawScope = this,
                        char = engine.girl,
                        centerX = effectiveGirlX,
                        bottomY = effectiveGirlY,
                        pixelSize = charPixelScale,
                        isHoldingUmbrella = isHoldingUmbrella,
                        isSnow = isSnow,
                        isSpeaking = !engine.girlSpeechText.isNullOrEmpty()
                    )
                }

                fun drawMochi() {
                    if (isCatInScene) {
                        WorldSprites.drawCat(
                            scope = this,
                            cx = cw * engine.catWorldX,
                            groundY = catY,
                            p = pixelScale,
                            timeSeconds = engine.sceneTime,
                            catState = engine.catState,
                            isSnow = isSnow,
                            facingLeft = engine.catFacingLeft
                        )
                    }
                }

                // 2D depth sorting by vertical Y plane
                val drawBoyFirst = effectiveBoyY <= effectiveGirlY

                // Helper to draw characters in sorted order
                fun drawCharacters() {
                    if (drawBoyFirst) {
                        drawBoy()
                        drawGirl()
                    } else {
                        drawGirl()
                        drawBoy()
                    }
                }

                val minY = minOf(effectiveBoyY, effectiveGirlY)
                val maxY = maxOf(effectiveBoyY, effectiveGirlY)

                if (isCatInScene && catY < minY) {
                    drawMochi()
                    drawCharacters()
                } else if (isCatInScene && catY in minY..maxY) {
                    if (drawBoyFirst) {
                        drawBoy()
                        drawMochi()
                        drawGirl()
                    } else {
                        drawGirl()
                        drawMochi()
                        drawBoy()
                    }
                } else {
                    drawCharacters()
                    if (isCatInScene) {
                        drawMochi()
                    }
                }

                // Cozy couple umbrella in rain weather (boy holds umbrella over girl)
                if (isHoldingUmbrella) {
                    WorldSprites.drawBoyHoldingUmbrella(
                        scope = this,
                        boyX = effectiveBoyX,
                        boyY = effectiveBoyY,
                        girlX = effectiveGirlX,
                        girlY = effectiveGirlY,
                        boyFacingRight = engine.boy.direction == com.example.engine.Direction.RIGHT,
                        p = charPixelScale,
                        timeSeconds = engine.sceneTime,
                        isSitting = isSitting
                    )
                }

                if (isKissing || isHugging) {
                    engine.boy.direction = origBoyDir
                    engine.girl.direction = origGirlDir
                }
            }

            // 2b. Foreground elements for Cozy Loft (patchwork quilt blanket over laps, coffee table, mugs, lantern, footstool, balcony railing, and Mochi curled on blanket)
            if (engine.currentScene.environment == EnvironmentType.COZY_LOFT) {
                LoftSprites.drawLoftForeground(
                    scope = this,
                    cw = cw,
                    ch = ch,
                    p = pixelScale,
                    timeSeconds = engine.sceneTime,
                    lampLit = engine.lampLit,
                    isSnow = engine.weather == com.example.scene.WeatherType.SNOW
                )

                val p = pixelScale
                val floorY = ch * 0.55f
                val loftFloorH = ch * 0.72f - floorY

                // 1. Loft Table: tea mug steam puffs & crumb drop
                if (engine.loftTableTimer > 0f) {
                    val dur = 1.4f
                    val t = ((dur - engine.loftTableTimer) / dur).coerceIn(0f, 1f)
                    val tableW = 34 * p
                    val tableX = cw * 0.54f - tableW / 2f
                    val tableY = floorY + 13 * p
                    val mugX = tableX + tableW - 5.5f * p
                    val mugY = tableY - 4.5f * p
                    val plateX = tableX + tableW * 0.63f
                    val plateY = tableY - 3 * p

                    val steamRise = t * 20 * p
                    val sFade = (1f - t).coerceIn(0f, 1f)
                    drawCircle(Color.White.copy(alpha = sFade * 0.65f), 2.5f * p, androidx.compose.ui.geometry.Offset(mugX + 2 * p, mugY - steamRise))
                    drawCircle(Color.White.copy(alpha = sFade * 0.45f), 3.5f * p, androidx.compose.ui.geometry.Offset(mugX + p, mugY - steamRise * 1.4f))

                    val crumbDrop = t * 6 * p
                    drawRect(Color(0xFFD4A373).copy(alpha = sFade), androidx.compose.ui.geometry.Offset(plateX + 2 * p, plateY + 2 * p + crumbDrop), Size(1.2f * p, 1.2f * p))
                    drawRect(Color(0xFF58311B).copy(alpha = sFade), androidx.compose.ui.geometry.Offset(plateX + 5 * p, plateY + 2 * p + crumbDrop * 0.8f), Size(p, p))
                }

                // 2. Reading Nook: book flutter & golden dust motes
                if (engine.loftBookNookTimer > 0f) {
                    val dur = 1.8f
                    val t = ((dur - engine.loftBookNookTimer) / dur).coerceIn(0f, 1f)
                    val nookBaseY = floorY + loftFloorH * 0.48f
                    val stackX = cw * 0.30f
                    val stackY = nookBaseY - p
                    val lantX = cw * 0.38f
                    val lantY = nookBaseY + 2 * p
                    val pageWiggle = sin(t * Math.PI.toFloat() * 6f) * (1f - t) * 2f * p

                    val openBookX = stackX - 5 * p
                    val openBookY = stackY - 4 * p - sin(t * Math.PI.toFloat()) * 3 * p
                    drawRect(Color(0xFFFFFDF0), androidx.compose.ui.geometry.Offset(openBookX - 4 * p, openBookY), Size(8 * p, 3.5f * p))
                    drawRect(Color(0xFFE9ECEF), androidx.compose.ui.geometry.Offset(openBookX - 4 * p, openBookY + 1 * p), Size(8 * p, 0.8f * p))
                    drawRect(Color(0xFF495057), androidx.compose.ui.geometry.Offset(openBookX - 0.5f * p, openBookY), Size(p, 3.5f * p))

                    val moteRise = t * 18 * p
                    val moteFade = (1f - t).coerceIn(0f, 1f)
                    drawCircle(Color(0xFFFFD166).copy(alpha = moteFade * 0.85f), 1.8f * p, androidx.compose.ui.geometry.Offset(lantX - 4 * p + pageWiggle, lantY - moteRise))
                    drawCircle(Color(0xFFFFF3B0).copy(alpha = moteFade * 0.75f), 1.4f * p, androidx.compose.ui.geometry.Offset(lantX + 3 * p - pageWiggle, lantY - moteRise * 1.2f))
                    drawCircle(Color(0xFFFFEAA7).copy(alpha = moteFade * 0.65f), 2.0f * p, androidx.compose.ui.geometry.Offset(lantX + pageWiggle * 0.5f, lantY - 6 * p - moteRise * 0.8f))
                }

                // 3. Balcony Fairy Lights: twinkle cascade wave
                if (engine.loftFairyLightsTimer > 0f) {
                    val dur = 1.6f
                    val t = ((dur - engine.loftFairyLightsTimer) / dur).coerceIn(0f, 1f)
                    val railTopY = ch * 0.72f
                    val bulbPositions = floatArrayOf(0.05f, 0.16f, 0.28f, 0.42f, 0.56f, 0.70f, 0.85f, 0.95f)
                    val bulbColors = listOf(Color(0xFFFFD166), Color(0xFFFF5D8F), Color(0xFF70E000), Color(0xFF48CAE4), Color(0xFFFFD166), Color(0xFFFF85A1), Color(0xFF48CAE4), Color(0xFFFFD166))

                    for ((i, bxRel) in bulbPositions.withIndex()) {
                        val bx = cw * bxRel
                        val by = railTopY + 8 * p + sin(bxRel * 10f) * 2 * p
                        val bT = ((t * 2.2f - i * 0.14f)).coerceIn(0f, 1f)
                        val bGlow = sin(bT * Math.PI.toFloat()).coerceIn(0f, 1f)
                        if (bGlow > 0f) {
                            val col = bulbColors[i % bulbColors.size]
                            drawCircle(col.copy(alpha = bGlow * 0.55f), (5f + bGlow * 7f) * p, androidx.compose.ui.geometry.Offset(bx, by + 2 * p))
                            drawCircle(Color.White.copy(alpha = bGlow * 0.85f), 2.2f * p, androidx.compose.ui.geometry.Offset(bx, by + 2 * p))
                        }
                    }
                }
            }

            // Draw cute earphone wire connecting both characters!
            if (engine.earphonesActive) {
                val boyAttachment = PixelArtRenderer.getEarphoneAttachmentOffset(engine.boy, effectiveBoyX, effectiveBoyY, pixelScale)
                val girlAttachment = PixelArtRenderer.getEarphoneAttachmentOffset(engine.girl, effectiveGirlX, effectiveGirlY, pixelScale)
                PixelArtRenderer.drawEarphoneCord(
                    scope = this,
                    startOffset = boyAttachment,
                    endOffset = girlAttachment,
                    p = pixelScale,
                    timeSeconds = engine.sceneTime
                )
            }

            // 3. Foreground particles (hearts, sparkles, steam, smoke, rain drops & splashes, sleep Zs)
            drawForegroundParticles(this, engine.particles.particles, pixelScale)

            // Atmospheric Lighting & Time-of-Day Layering
            val isMorning = timePhase.isMorning
            val isTwilight = timePhase.isTwilight
            val isMidnight = timePhase.isMidnight

            if (isOutdoor) {
                // Time-of-day atmospheric layer (delicate, natural light transitions)
                when {
                    isMidnight -> {
                        // Midnight celestial coziness: deep starlight indigo vignette
                        drawRect(Color(0xFF060B1E).copy(alpha = 0.16f), Offset.Zero, size)
                    }
                    isNight -> {
                        // Night: calm, intimate moonlit atmosphere
                        drawRect(Color(0xFF0A122C).copy(alpha = 0.12f), Offset.Zero, size)
                    }
                    isTwilight -> {
                        // Twilight: deep rich lavender-indigo dusk glow
                        drawRect(Color(0xFF4A2545).copy(alpha = 0.12f), Offset.Zero, size)
                    }
                    isSunset -> {
                        // Sunset: warm coral-amber golden hour wash
                        drawRect(Color(0xFFE85D04).copy(alpha = 0.08f), Offset.Zero, size)
                    }
                    isMorning -> {
                        // Morning: soft rose-ivory dewy dawn glow
                        drawRect(Color(0xFFFFD6A5).copy(alpha = 0.06f), Offset.Zero, size)
                    }
                }

                // Weather ambient atmospheric tinting
                when (engine.weather) {
                    com.example.scene.WeatherType.RAIN -> {
                        drawRect(Color(0x221B263B), Offset.Zero, size)
                    }
                    com.example.scene.WeatherType.SNOW -> {
                        drawRect(Color(0x18CAE9FF), Offset.Zero, size)
                    }
                    com.example.scene.WeatherType.AUTUMN -> {
                        drawRect(Color(0x18D9480F), Offset.Zero, size)
                    }
                    com.example.scene.WeatherType.SAKURA -> {
                        drawRect(Color(0x14FF758F), Offset.Zero, size)
                    }
                    com.example.scene.WeatherType.SUNNY -> {
                        if (!isNight && !isSunset) {
                            drawRect(Color(0x08FFB703), Offset.Zero, size)
                        }
                    }
                }
            } else {
                // Indoor atmosphere (Kitchen, Living Room, Cozy Loft)
                // Distinctly warmer and cozier than chilly outdoors
                val indoorWarmthAlpha = when {
                    isMidnight -> 0.14f
                    isNight -> 0.10f
                    isSunset -> 0.08f
                    else -> 0.04f
                }
                drawRect(Color(0xFFFFB703).copy(alpha = indoorWarmthAlpha), Offset.Zero, size)
                if (isNight || isMidnight) {
                    // Soft cozy interior evening shading
                    drawRect(Color(0xFF1B1124).copy(alpha = if (isMidnight) 0.12f else 0.07f), Offset.Zero, size)
                }
            }

            // 4. Ambient Dimming layer
            if (engine.ambientDimming > 0f) {
                drawRect(
                    color = Color(0xFF0D1117).copy(alpha = engine.ambientDimming),
                    topLeft = Offset.Zero,
                    size = size
                )
            }

            // 5. Dynamic Lightning Flash overlay
            if (engine.lightningFlashAlpha > 0f) {
                drawRect(
                    color = Color(0xFFEEF2FF).copy(alpha = engine.lightningFlashAlpha.coerceIn(0f, 0.95f)),
                    topLeft = Offset.Zero,
                    size = size
                )
            }

            // 6. Smooth scene transition fade overlay (no line artifacts or jarring bars)
            if (engine.wipeAlpha > 0f) {
                val fadeAlpha = (engine.wipeAlpha * engine.wipeAlpha).coerceIn(0f, 1f)
                drawRect(
                    color = Color(0xFF0F1423).copy(alpha = fadeAlpha),
                    topLeft = Offset.Zero,
                    size = size
                )
            }
        }

        val pixelScale = (viewportWidth / 115f).coerceIn(3.0f, 5.0f)
        val isRideScene = engine.currentScene == com.example.scene.SceneType.EVENING_RIDE
        val isLoftScene = engine.currentScene.environment == EnvironmentType.COZY_LOFT

        // In ride scene, sequence lines: his line first, then hers, never both bubbles on screen at once
        val showBoyBubble = !engine.boySpeechText.isNullOrEmpty()
        val showGirlBubble = !engine.girlSpeechText.isNullOrEmpty() && (!isRideScene || !showBoyBubble)

        val isKissing = (engine.boy.pose == com.example.engine.CharacterPose.KISS ||
                         engine.girl.pose == com.example.engine.CharacterPose.KISS)
        val isHugging = (engine.boy.pose == com.example.engine.CharacterPose.HUG ||
                         engine.girl.pose == com.example.engine.CharacterPose.HUG ||
                         engine.boy.pose == com.example.engine.CharacterPose.SIT_SNUGGLE ||
                         engine.girl.pose == com.example.engine.CharacterPose.SIT_SNUGGLE)
        val cuddleEased = CharacterMotionTween.easeInOutCubic(engine.cuddleProgress)
        val charPixelScale = pixelScale * 1.38f
        val targetHugOffset = when {
            isKissing -> 6.2f * charPixelScale
            else -> 4.8f * charPixelScale
        }

        val rawBoyX = viewportWidth * engine.boy.worldX
        val rawGirlX = viewportWidth * engine.girl.worldX
        val midCharX = (rawBoyX + rawGirlX) / 2f

        val effectiveBoyX = rawBoyX + ((midCharX - targetHugOffset) - rawBoyX) * cuddleEased
        val effectiveGirlX = rawGirlX + ((midCharX + targetHugOffset) - rawGirlX) * cuddleEased
        val rawBoyY = viewportHeight * engine.boy.worldY
        val rawGirlY = viewportHeight * engine.girl.worldY
        val effectiveBoyY = rawBoyY + (maxOf(rawBoyY, rawGirlY) - rawBoyY) * cuddleEased
        val effectiveGirlY = rawGirlY + (maxOf(rawBoyY, rawGirlY) - rawGirlY) * cuddleEased

        val isBoySitting = engine.boy.pose == com.example.engine.CharacterPose.SIT || engine.boy.pose == com.example.engine.CharacterPose.SIT_SNUGGLE
        val isGirlSitting = engine.girl.pose == com.example.engine.CharacterPose.SIT || engine.girl.pose == com.example.engine.CharacterPose.SIT_SNUGGLE

        val boyHeadX = when {
            isRideScene -> viewportWidth * 0.50f - 14f * pixelScale
            isLoftScene -> rawBoyX
            else -> effectiveBoyX + engine.boy.idleSwayOffset
        }
        val girlHeadX = when {
            isRideScene -> viewportWidth * 0.50f + 6f * pixelScale
            isLoftScene -> rawGirlX
            else -> effectiveGirlX + engine.girl.idleSwayOffset
        }

        val boyHeadY = when {
            isRideScene -> viewportHeight * 0.70f - 52f * pixelScale
            isLoftScene -> viewportHeight * 0.55f - 24f * (pixelScale * 1.10f)
            else -> effectiveBoyY - (38.5f * pixelScale) - engine.boy.bounceOffset + (if (isBoySitting) 7.5f * pixelScale else 0f)
        }
        val girlHeadY = when {
            isRideScene -> viewportHeight * 0.70f - 52f * pixelScale
            isLoftScene -> viewportHeight * 0.55f - 24f * (pixelScale * 1.10f)
            else -> effectiveGirlY - (38.5f * pixelScale) - engine.girl.bounceOffset + (if (isGirlSitting) 7.5f * pixelScale else 0f)
        }

        if (!engine.isDreamMode) {
            PixelSpeechBubblesOverlay(
                boyText = if (showBoyBubble) engine.boySpeechText else null,
                girlText = if (showGirlBubble) engine.girlSpeechText else null,
                boyHeadX = boyHeadX,
                boyHeadY = boyHeadY,
                girlHeadX = girlHeadX,
                girlHeadY = girlHeadY,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight
            )
        }

        // Pixelated Subtitle message banner placed at bottom (never overlaps floating buttons or navigation bars!)
        if (!engine.isDreamMode && !engine.sceneMessage.isNullOrEmpty()) {
            PixelMessageBox(
                message = engine.sceneMessage!!,
                alpha = { engine.messageAlpha }
            )
        }

        // Dream Journal visual overlay — drawn on top of everything when a dream is active
        if (engine.isDreamMode && engine.dreamOverlayAlpha > 0f) {
            DreamOverlay(
                engine = engine,
                theme = engine.activeDreamTheme ?: "FALLBACK",
                dreamText = engine.activeDreamText ?: "",
                alpha = engine.dreamOverlayAlpha,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                pixelScale = (viewportWidth / 115f).coerceIn(3.0f, 5.0f),
                frameNanos = frameNanos
            )
        }
    }
}

@Composable
private fun BoxScope.DreamOverlay(
    engine: SceneEngine,
    theme: String,
    dreamText: String,
    alpha: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    pixelScale: Float,
    frameNanos: Long
) {
    // Read frameNanos so Compose tracks continuous frame updates for smooth 60 FPS animations
    val currentFrame = frameNanos
    val t = engine.sceneTime

    // Theme-specific deep atmospheric base color, glowing tint wash, and label
    val (baseColor, tintColor, themeLabel) = when (theme) {
        "JAPAN"  -> Triple(Color(0xD81B0E2B), Color(0x44FF80AB), "Japan dream")
        "NORWAY" -> Triple(Color(0xD8041B24), Color(0x4443AA8B), "Aurora dream")
        "OCEAN"  -> Triple(Color(0xD8011933), Color(0x440077B6), "Ocean dream")
        "FLYING" -> Triple(Color(0xD81C263A), Color(0x4490CAF9), "Sky dream")
        "STARS"  -> Triple(Color(0xD8090D24), Color(0x443F37C9), "Starfield dream")
        "FOREST" -> Triple(Color(0xD8051A10), Color(0x442D6A4F), "Forest dream")
        "HOME"   -> Triple(Color(0xD8230F0D), Color(0x44F77F00), "Cozy dream")
        "RAIN"   -> Triple(Color(0xD80D1520), Color(0x44415A77), "Storm dream")
        "CITY"   -> Triple(Color(0xD80C0D1D), Color(0x447209B7), "City dream")
        "SWEET"  -> Triple(Color(0xD829101F), Color(0x44F72585), "Sweet dream")
        else     -> Triple(Color(0xD8150F28), Color(0x447B2CBF), "Dream")
    }

    // Breathing pulse for ethereal aura & UI highlights
    val breathe = 0.5f + 0.5f * sin(t * 2.0f)
    val floatY = sin(t * 1.6f) * 6f // Smooth levitating float in pixels

    // Full-screen procedural pixel art canvas for iconic dream elements + atmospheric dream veil
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val frame = frameNanos // Explicitly read inside Canvas to guarantee continuous 60 FPS redraws!
        val cw = size.width
        val ch = size.height
        val dreamTime = engine.sceneTime

        // 1. Dream realm atmosphere — deep dreamy wash
        drawRect(baseColor.copy(alpha = baseColor.alpha * alpha))
        drawRect(tintColor.copy(alpha = tintColor.alpha * alpha))

        // 2. Slow ethereal drifting cosmic dream fog / undulating waves across middle
        val fogCol = tintColor.copy(alpha = (0.07f + 0.04f * sin(dreamTime * 1.2f)) * alpha)
        for (f in 0 until 3) {
            val wavePhase = dreamTime * (0.5f + f * 0.25f)
            val baseY = ch * (0.36f + f * 0.16f)
            val fogPath = Path().apply {
                moveTo(0f, baseY)
                val segments = 8
                val segW = cw / segments
                for (s in 1..segments) {
                    val sx = s * segW
                    val sy = baseY + sin(wavePhase + s * 0.9f) * (18f + f * 8f) * pixelScale
                    lineTo(sx, sy)
                }
                lineTo(cw, ch)
                lineTo(0f, ch)
                close()
            }
            drawPath(fogPath, fogCol)
        }

        // 3. Procedural stardust in sky + rising dream motes
        drawDreamStardust(this, cw, ch, dreamTime, pixelScale, alpha)

        // 4. Theme-specific iconic procedural pixel art with vivid continuous motion
        drawDreamArt(this, cw, ch, theme, dreamTime, pixelScale, alpha)
    }

    // Dream banner at top: theme label + dream text + wake up button
    // Float smoothly in 60fps with glowing dreamy halo and pulsing border
    Surface(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .graphicsLayer {
                translationY = floatY
                this.alpha = alpha
            }
            .shadow(
                elevation = (10f + 6f * breathe).dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = tintColor.copy(alpha = 0.5f),
                spotColor = tintColor.copy(alpha = 0.6f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xD90D1117),
        border = BorderStroke((1.2f + 0.6f * breathe).dp, tintColor.copy(alpha = (0.45f + 0.45f * breathe).coerceIn(0.2f, 0.95f)))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFB5C2).copy(alpha = (0.75f + 0.25f * breathe).coerceIn(0f, 1f)),
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer {
                                    rotationZ = sin(t * 2.2f) * 16f
                                    val sc = 1f + 0.15f * sin(t * 3.2f)
                                    scaleX = sc
                                    scaleY = sc
                                }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = themeLabel.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFFB5C2)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dreamText,
                        fontSize = 13.5.sp,
                        color = Color.White,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Dreamy "Wake Up" Pill Button
                Surface(
                    onClick = { engine.clearDream() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f + 0.08f * breathe),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f + 0.30f * breathe)),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wake Up",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dreamy bottom glowing accent line that shimmers across card width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .drawBehind {
                        val shimProg = (sin(t * 1.8f) + 1f) * 0.5f
                        val startCol = tintColor.copy(alpha = 0.2f * alpha)
                        val midCol = Color(0xFFFFB5C2).copy(alpha = (0.6f + 0.35f * breathe) * alpha)
                        val endCol = tintColor.copy(alpha = 0.2f * alpha)
                        val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(startCol, midCol, endCol),
                            startX = size.width * (shimProg - 0.3f),
                            endX = size.width * (shimProg + 0.3f)
                        )
                        drawRect(brush)
                    }
            )
        }
    }
}

private fun drawDreamStardust(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    t: Float,
    p: Float,
    alpha: Float
) {
    val starCol = Color(0xFFFFFFFF)
    // 1. Twinkling sky stars
    for (i in 0 until 28) {
        val seed = i * 47.3f
        val sx = (seed * 67.9f) % cw
        val sy = (seed * 91.3f) % (ch * 0.65f)
        val twinkle = 0.35f + 0.65f * abs(sin(t * 2.2f + i * 1.3f))
        scope.drawCircle(
            starCol.copy(alpha = (0.55f * twinkle * alpha).coerceIn(0f, 1f)),
            radius = (1.1f + (i % 3) * 0.4f) * p,
            center = Offset(sx, sy)
        )
    }

    // 2. Rising floating dream motes / reverse gravity stardust drifting upward
    for (i in 0 until 20) {
        val seed = i * 31.7f
        val my = (ch * 0.90f - ((t * (22f + (i % 3) * 10f) * p + seed * 30f) % (ch * 0.85f)))
        val mx = (seed * 73.1f + sin(t * 1.5f + i * 0.8f) * 16f * p) % cw
        val twinkle = 0.35f + 0.65f * abs(sin(t * 2.6f + i * 1.1f))
        scope.drawCircle(
            Color(0xFFFFF0F5).copy(alpha = (0.70f * twinkle * alpha).coerceIn(0f, 1f)),
            radius = (1.2f + (i % 3) * 0.6f) * p,
            center = Offset(mx, my)
        )
        if (i % 4 == 0) {
            val glint = (3.5f * twinkle) * p
            scope.drawLine(Color.White.copy(alpha = 0.75f * twinkle * alpha), Offset(mx - glint, my), Offset(mx + glint, my), strokeWidth = 0.8f * p)
            scope.drawLine(Color.White.copy(alpha = 0.75f * twinkle * alpha), Offset(mx, my - glint), Offset(mx, my + glint), strokeWidth = 0.8f * p)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dream Journal — Exclusive Iconic Procedural Pixel Art per Theme
// ─────────────────────────────────────────────────────────────────────────────

private fun drawDreamArt(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    theme: String,
    t: Float,
    p: Float,
    alpha: Float
) {
    when (theme) {
        "JAPAN"  -> drawJapanDream(scope, cw, ch, t, p, alpha)
        "NORWAY" -> drawNorwayDream(scope, cw, ch, t, p, alpha)
        "OCEAN"  -> drawOceanDream(scope, cw, ch, t, p, alpha)
        "FLYING" -> drawFlyingDream(scope, cw, ch, t, p, alpha)
        "STARS"  -> drawStarsDream(scope, cw, ch, t, p, alpha)
        "FOREST" -> drawForestDream(scope, cw, ch, t, p, alpha)
        "HOME"   -> drawHomeDream(scope, cw, ch, t, p, alpha)
        "RAIN"   -> drawRainDream(scope, cw, ch, t, p, alpha)
        "CITY"   -> drawCityDream(scope, cw, ch, t, p, alpha)
        "SWEET"  -> drawSweetDream(scope, cw, ch, t, p, alpha)
        else     -> drawFallbackDream(scope, cw, ch, t, p, alpha)
    }
}

private fun drawJapanDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Grand Japanese Torii Gate on the right
    val gateX = cw * 0.76f
    val gateY = ch * 0.38f
    val colW = 7f * p
    val colH = ch * 0.30f
    val span = 84f * p
    val toriiRed = Color(0xFFD90429).copy(alpha = 0.88f * alpha)
    val toriiDark = Color(0xFF2B2D42).copy(alpha = 0.92f * alpha)
    val goldGlow = Color(0xFFFFD166).copy(alpha = 0.90f * alpha)

    // Left & Right Pillars
    scope.drawRect(toriiRed, Offset(gateX - span * 0.5f, gateY), Size(colW, colH))
    scope.drawRect(toriiRed, Offset(gateX + span * 0.5f - colW, gateY), Size(colW, colH))
    // Stone bases (Kamebara)
    scope.drawRect(toriiDark, Offset(gateX - span * 0.5f - 2f * p, gateY + colH - 6f * p), Size(colW + 4f * p, 6f * p))
    scope.drawRect(toriiDark, Offset(gateX + span * 0.5f - colW - 2f * p, gateY + colH - 6f * p), Size(colW + 4f * p, 6f * p))

    // Top primary crossbeam (Kasagi - curved upturned ends)
    val topBeamW = span + 34f * p
    val topBeamH = 7f * p
    scope.drawRect(toriiDark, Offset(gateX - topBeamW * 0.5f, gateY - 5f * p), Size(topBeamW, topBeamH))
    scope.drawRect(toriiDark, Offset(gateX - topBeamW * 0.5f - 3f * p, gateY - 8f * p), Size(4f * p, 4f * p))
    scope.drawRect(toriiDark, Offset(gateX + topBeamW * 0.5f - 1f * p, gateY - 8f * p), Size(4f * p, 4f * p))

    // Lower secondary beam (Nuki)
    val lowBeamW = span + 14f * p
    val lowBeamH = 5f * p
    scope.drawRect(toriiRed, Offset(gateX - lowBeamW * 0.5f, gateY + 12f * p), Size(lowBeamW, lowBeamH))

    // Central tablet plaque (Gakuzuka)
    scope.drawRect(toriiDark, Offset(gateX - 4f * p, gateY + 2f * p), Size(8f * p, 10f * p))
    scope.drawRect(goldGlow, Offset(gateX - 2f * p, gateY + 4f * p), Size(4f * p, 6f * p))

    // Swaying Hanging Red Paper Lantern (Chochin)
    val swayX = sin(t * 2.4f) * 6f * p
    val lanX = gateX + swayX
    val lanY = gateY + 18f * p
    scope.drawLine(Color(0xFF1B1B1E).copy(alpha = alpha), Offset(gateX, gateY + 17f * p), Offset(lanX, lanY), strokeWidth = 1.5f * p)
    // Warm breathing lantern glow aura
    scope.drawCircle(Color(0xFFFFD166).copy(alpha = (0.28f + 0.15f * sin(t * 3.5f)) * alpha), radius = 22f * p, center = Offset(lanX, lanY + 9f * p))
    scope.drawRect(Color(0xFFE63946).copy(alpha = 0.92f * alpha), Offset(lanX - 7f * p, lanY), Size(14f * p, 18f * p))
    scope.drawRect(goldGlow, Offset(lanX - 4f * p, lanY + 3f * p), Size(8f * p, 12f * p))
    scope.drawRect(Color(0xFF1B1B1E).copy(alpha = alpha), Offset(lanX - 5f * p, lanY - 2f * p), Size(10f * p, 2f * p))
    scope.drawRect(Color(0xFF1B1B1E).copy(alpha = alpha), Offset(lanX - 5f * p, lanY + 18f * p), Size(10f * p, 2f * p))

    // 2. Traditional Stone Pagoda Lantern (Toro) on left
    val lampX = cw * 0.14f
    val lampY = ch * 0.62f
    val stoneCol = Color(0xFF6C757D).copy(alpha = 0.85f * alpha)
    scope.drawRect(stoneCol, Offset(lampX - 10f * p, lampY + 18f * p), Size(20f * p, 5f * p))
    scope.drawRect(stoneCol, Offset(lampX - 4f * p, lampY + 8f * p), Size(8f * p, 10f * p))
    scope.drawRect(stoneCol, Offset(lampX - 7f * p, lampY - 4f * p), Size(14f * p, 12f * p))
    val lanternFlicker = (0.75f + 0.25f * sin(t * 5f)) * alpha
    scope.drawRect(Color(0xFFFFD166).copy(alpha = lanternFlicker), Offset(lampX - 4f * p, lampY - 1f * p), Size(8f * p, 6f * p))
    // Soft glowing light pool beneath stone lamp
    scope.drawOval(Color(0x55FFD166).copy(alpha = lanternFlicker * 0.45f), topLeft = Offset(lampX - 18f * p, lampY + 20f * p), size = Size(36f * p, 9f * p))
    scope.drawRect(stoneCol, Offset(lampX - 12f * p, lampY - 8f * p), Size(24f * p, 4f * p))
    scope.drawRect(stoneCol, Offset(lampX - 5f * p, lampY - 12f * p), Size(10f * p, 4f * p))
    scope.drawRect(stoneCol, Offset(lampX - 2f * p, lampY - 15f * p), Size(4f * p, 3f * p))

    // 3. Cherry Blossom Branch silhouette in top-left
    val branchCol = Color(0xFF3D2618).copy(alpha = 0.80f * alpha)
    scope.drawRect(branchCol, Offset(0f, 0f), Size(cw * 0.28f, 5f * p))
    scope.drawRect(branchCol, Offset(cw * 0.18f, 5f * p), Size(cw * 0.12f, 4f * p))
    scope.drawRect(branchCol, Offset(cw * 0.25f, 9f * p), Size(8f * p, 14f * p))
    val petalCol1 = Color(0xFFFFB5C2).copy(alpha = 0.95f * alpha)
    val petalCol2 = Color(0xFFFF758F).copy(alpha = 0.90f * alpha)
    val petalCol3 = Color(0xFFFFFFFF).copy(alpha = 0.92f * alpha)
    for (i in 0 until 8) {
        val bx = cw * (0.08f + i * 0.032f)
        val by = (8f + (i % 4) * 5f) * p
        val swayP = sin(t * 1.8f + i) * 2f * p
        val col = when (i % 3) { 0 -> petalCol1; 1 -> petalCol2; else -> petalCol3 }
        scope.drawCircle(col, radius = (3.5f + (i % 2)) * p, center = Offset(bx + swayP, by))
        scope.drawCircle(Color(0xFFFFD166).copy(alpha = alpha), radius = 1.2f * p, center = Offset(bx + swayP, by))
    }

    // 4. Drifting Falling Sakura Petals across the screen
    for (sp in 0 until 24) {
        val seed = sp * 37.1f
        val px = (seed * 53.7f + t * 28f * p + sin(t * 1.8f + sp) * 24f * p) % cw
        val py = (seed * 89.3f + t * 50f * p) % ch
        val pRot = sin(t * 2.8f + sp) * 2.5f * p
        scope.drawOval(
            Color(0xFFFFB5C2).copy(alpha = 0.88f * alpha),
            topLeft = Offset(px, py),
            size = Size(4.5f * p + pRot, 2.8f * p)
        )
    }
}

private fun drawNorwayDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    val auroraGreen = Color(0xFF06D6A0)
    val auroraTeal = Color(0xFF48CAE4)
    val auroraPurple = Color(0xFF9D4EDD)

    // 1. Sinusoidal Aurora Borealis ribbons across upper sky
    val ribbonStep = 4f * p
    val ribbonCount = (cw / ribbonStep).toInt() + 1
    for (i in 0 until ribbonCount) {
        val rx = i * ribbonStep
        // Ribbon 1 (Emerald Green)
        val w1 = sin(t * 1.4f + rx * 0.007f) * 32f * p + sin(t * 0.7f + rx * 0.015f) * 16f * p
        val topY1 = ch * 0.18f + w1
        val h1 = (45f + sin(t * 2.0f + rx * 0.02f) * 18f) * p
        val a1 = (0.28f + 0.16f * sin(t * 1.8f + rx * 0.01f)).coerceIn(0.10f, 0.50f) * alpha
        scope.drawRect(auroraGreen.copy(alpha = a1), Offset(rx, topY1), Size(ribbonStep, h1))

        // Ribbon 2 (Ethereal Violet)
        val w2 = sin(t * 1.1f + rx * 0.009f + 1.2f) * 26f * p
        val topY2 = ch * 0.12f + w2
        val h2 = (38f + cos(t * 1.7f + rx * 0.012f) * 14f) * p
        val a2 = (0.22f + 0.14f * cos(t * 1.5f + rx * 0.014f)).coerceIn(0.08f, 0.42f) * alpha
        scope.drawRect(auroraPurple.copy(alpha = a2), Offset(rx, topY2), Size(ribbonStep, h2))

        // Ribbon 3 (Luminous Teal)
        val w3 = cos(t * 1.6f + rx * 0.008f + 2.4f) * 20f * p
        val topY3 = ch * 0.25f + w3
        val h3 = (28f + sin(t * 2.2f + rx * 0.018f) * 10f) * p
        val a3 = (0.20f + 0.12f * sin(t * 1.3f + rx * 0.02f)).coerceIn(0.06f, 0.38f) * alpha
        scope.drawRect(auroraTeal.copy(alpha = a3), Offset(rx, topY3), Size(ribbonStep, h3))
    }

    // 2. Jagged Fjord Mountains along horizon
    val mountainCol = Color(0xFF0F1E2E).copy(alpha = 0.85f * alpha)
    val snowCol = Color(0xFFE2EAFC).copy(alpha = 0.90f * alpha)
    val baseH = ch * 0.68f

    for (m in 0 until 4) {
        val lx = cw * (m * 0.26f)
        val rx = cw * ((m + 1) * 0.28f).coerceAtMost(1f)
        val midX = (lx + rx) / 2f
        val peakY = ch * (0.44f + (m % 2) * 0.04f)

        val mPath = Path().apply {
            moveTo(lx, baseH)
            lineTo(midX, peakY)
            lineTo(rx, baseH)
            close()
        }
        scope.drawPath(mPath, mountainCol)

        // Snowcap on peak
        val sPath = Path().apply {
            moveTo(midX - (midX - lx) * 0.22f, peakY + (baseH - peakY) * 0.22f)
            lineTo(midX, peakY)
            lineTo(midX + (rx - midX) * 0.22f, peakY + (baseH - peakY) * 0.22f)
            close()
        }
        scope.drawPath(sPath, snowCol)
    }

    // 3. Nordic Timber Cabin on left ridge with warm glowing window
    val cabinX = cw * 0.16f
    val cabinY = ch * 0.58f
    scope.drawRect(Color(0xFFC1121F).copy(alpha = 0.90f * alpha), Offset(cabinX, cabinY), Size(16f * p, 11f * p))
    scope.drawRect(Color(0xFF1B1B1E).copy(alpha = 0.95f * alpha), Offset(cabinX - 2f * p, cabinY - 4f * p), Size(20f * p, 4f * p))
    scope.drawRect(Color(0xFFFFD166).copy(alpha = 0.95f * alpha), Offset(cabinX + 4f * p, cabinY + 3f * p), Size(5f * p, 5f * p))

    // 4. Fluttering Gentle Snow Motes
    for (sn in 0 until 18) {
        val seed = sn * 41.3f
        val sx = (seed * 67.1f + sin(t * 1.2f + sn) * 16f * p) % cw
        val sy = (seed * 93.7f + t * 35f * p) % ch
        scope.drawCircle(
            Color(0xFFE2E8F0).copy(alpha = 0.80f * alpha),
            radius = (1.2f + (sn % 3) * 0.5f) * p,
            center = Offset(sx, sy)
        )
    }
}

private fun drawOceanDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Water Caustics / Sunbeams angling down
    val causticCol = Color(0xFFADE8F4).copy(alpha = 0.12f * alpha)
    for (i in 0 until 5) {
        val startX = cw * (0.15f + i * 0.18f) + sin(t * 0.8f + i) * 15f * p
        val beamPath = Path().apply {
            moveTo(startX, 0f)
            lineTo(startX + 18f * p, 0f)
            lineTo(startX + 45f * p, ch * 0.65f)
            lineTo(startX + 15f * p, ch * 0.65f)
            close()
        }
        scope.drawPath(beamPath, causticCol)
    }

    // 2. Bioluminescent Jellyfish
    for (jIdx in 0 until 2) {
        val jx = if (jIdx == 0) cw * 0.24f + sin(t * 0.8f) * 20f * p else cw * 0.78f - sin(t * 0.7f) * 18f * p
        val jy = if (jIdx == 0) ch * 0.42f + cos(t * 1.1f) * 14f * p else ch * 0.32f + sin(t * 0.9f) * 12f * p

        val scalePulse = 1f + 0.12f * abs(sin(t * 2.5f + jIdx * 1.5f))
        val bellR = (16f + jIdx * 6f) * p * scalePulse
        val bellCol = if (jIdx == 0) Color(0xBB48CAE4) else Color(0xBBDDA15E)
        val glowCol = if (jIdx == 0) Color(0x6690E0EF) else Color(0x66E9D8A6)

        scope.drawCircle(glowCol.copy(alpha = glowCol.alpha * alpha), radius = bellR * 1.35f, center = Offset(jx, jy))
        scope.drawCircle(bellCol.copy(alpha = bellCol.alpha * alpha), radius = bellR, center = Offset(jx, jy))
        scope.drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.75f * alpha), radius = bellR * 0.35f, center = Offset(jx, jy - 2f * p))

        // 4 Trailing undulating tentacles
        for (k in 0 until 4) {
            val txBase = jx - bellR * 0.6f + k * (bellR * 0.4f)
            val tentPath = Path().apply {
                moveTo(txBase, jy + bellR * 0.6f)
                val len = (28f + jIdx * 10f) * p
                val segments = 6
                for (s in 1..segments) {
                    val sy = jy + bellR * 0.6f + s * (len / segments)
                    val sway = sin(t * 3.2f + k * 0.8f + s * 0.6f) * (4f + s * 1.2f) * p
                    lineTo(txBase + sway, sy)
                }
            }
            scope.drawPath(tentPath, Color(0xDDCAF0F8).copy(alpha = 0.70f * alpha), style = Stroke(width = 1.8f * p))
        }
    }

    // 3. Rising Air Bubbles
    val bubbleCol = Color(0x88CAF0F8).copy(alpha = 0.65f * alpha)
    val highlightCol = Color(0xFFFFFFFF).copy(alpha = 0.85f * alpha)
    for (b in 0 until 10) {
        val seed = b * 37.1f
        val bx = (cw * (0.08f + (seed % 0.84f))) + sin(t * 2f + seed) * 8f * p
        val by = (ch - ((t * (35f + (b % 4) * 12f) * p + seed * 20f) % ch))
        val br = (2.5f + (b % 4) * 1.5f) * p
        scope.drawCircle(bubbleCol, radius = br, center = Offset(bx, by))
        scope.drawCircle(highlightCol, radius = br * 0.35f, center = Offset(bx - br * 0.3f, by - br * 0.3f))
    }
}

private fun drawFlyingDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Puffy Layered Dream Clouds floating underneath characters
    val cloudColor1 = Color(0xEEF8F9FA).copy(alpha = 0.85f * alpha)
    val cloudColor2 = Color(0xDDE2EAFC).copy(alpha = 0.75f * alpha)
    val goldenFringe = Color(0xAAFFE6A7).copy(alpha = 0.60f * alpha)

    val cloudY = ch * 0.60f
    for (c in 0 until 3) {
        val speed = 12f + c * 8f
        val cx = ((t * speed * p + c * (cw * 0.38f)) % (cw + 120f * p)) - 60f * p
        val cy = cloudY + (c * 18f - 10f) * p
        val baseR = (22f + c * 6f) * p

        scope.drawCircle(goldenFringe, radius = baseR * 1.15f, center = Offset(cx, cy))
        scope.drawCircle(cloudColor2, radius = baseR, center = Offset(cx, cy))
        scope.drawCircle(cloudColor1, radius = baseR * 0.85f, center = Offset(cx - baseR * 0.5f, cy + 3f * p))
        scope.drawCircle(cloudColor1, radius = baseR * 0.80f, center = Offset(cx + baseR * 0.5f, cy + 3f * p))
        scope.drawCircle(cloudColor1, radius = baseR * 0.65f, center = Offset(cx, cy - baseR * 0.4f))
    }

    // 2. Procedural Pixel Hot Air Balloon drifting in upper sky
    val balloonX = (cw * 0.80f + sin(t * 0.6f) * 12f * p)
    val balloonY = ch * 0.28f + cos(t * 1.2f) * 10f * p
    val bR = 18f * p

    scope.drawCircle(Color(0xFFE63946).copy(alpha = 0.90f * alpha), radius = bR, center = Offset(balloonX, balloonY))
    scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.85f * alpha), radius = bR * 0.65f, center = Offset(balloonX, balloonY))
    scope.drawCircle(Color(0xFF457B9D).copy(alpha = 0.85f * alpha), radius = bR * 0.30f, center = Offset(balloonX, balloonY))

    val basketY = balloonY + bR + 10f * p
    scope.drawLine(Color(0xFF2B2D42).copy(alpha = 0.8f * alpha), Offset(balloonX - 6f * p, balloonY + bR), Offset(balloonX - 3f * p, basketY), strokeWidth = 1f * p)
    scope.drawLine(Color(0xFF2B2D42).copy(alpha = 0.8f * alpha), Offset(balloonX + 6f * p, balloonY + bR), Offset(balloonX + 3f * p, basketY), strokeWidth = 1f * p)
    scope.drawCircle(Color(0xFFFFAA00).copy(alpha = (0.7f + 0.3f * sin(t * 8f)) * alpha), radius = 2.5f * p, center = Offset(balloonX, balloonY + bR + 4f * p))
    scope.drawRect(Color(0xFF9C6644).copy(alpha = 0.92f * alpha), Offset(balloonX - 4f * p, basketY), Size(8f * p, 6f * p))

    // 3. Graceful Dream Birds gliding across the upper sky
    val birdCol = Color(0xFFFFFFFF).copy(alpha = 0.80f * alpha)
    for (bIdx in 0 until 3) {
        val bProg = (t * 22f * p + bIdx * cw * 0.36f) % (cw + 40f * p)
        val bx = cw - bProg
        val by = ch * (0.16f + bIdx * 0.07f) + sin(t * 1.8f + bIdx) * 10f * p
        val flap = sin(t * 5f + bIdx * 1.5f) * 3f * p
        // Left wing and right wing
        scope.drawLine(birdCol, Offset(bx, by), Offset(bx - 6f * p, by - 3f * p + flap), strokeWidth = 1.4f * p)
        scope.drawLine(birdCol, Offset(bx, by), Offset(bx + 6f * p, by - 3f * p + flap), strokeWidth = 1.4f * p)
    }
}

private fun drawStarsDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Giant Glowing Crescent Moon
    val moonX = cw * 0.78f
    val moonY = ch * 0.20f
    val moonR = 32f * p
    scope.drawCircle(Color(0x33FFEAA7).copy(alpha = 0.30f * alpha), radius = moonR * 1.4f, center = Offset(moonX, moonY))
    scope.drawCircle(Color(0xFFFFEAA7).copy(alpha = 0.95f * alpha), radius = moonR, center = Offset(moonX, moonY))
    scope.drawCircle(Color(0xFF191970).copy(alpha = 0.90f * alpha), radius = moonR * 0.85f, center = Offset(moonX + 10f * p, moonY - 4f * p))

    // 2. Ringed Saturn Planet
    val planetX = cw * 0.18f
    val planetY = ch * 0.28f
    val planetR = 14f * p
    scope.drawCircle(Color(0xFFC77DFF).copy(alpha = 0.92f * alpha), radius = planetR, center = Offset(planetX, planetY))
    scope.drawOval(
        Color(0xFFFFD166).copy(alpha = 0.85f * alpha),
        topLeft = Offset(planetX - 26f * p, planetY - 6f * p),
        size = Size(52f * p, 12f * p),
        style = Stroke(width = 2.2f * p)
    )

    // 3. Two Hearts Constellation in central upper sky
    val starNodes = listOf(
        Pair(cw * 0.44f, ch * 0.20f), Pair(cw * 0.48f, ch * 0.16f), Pair(cw * 0.52f, ch * 0.20f),
        Pair(cw * 0.48f, ch * 0.26f), // Heart 1
        Pair(cw * 0.56f, ch * 0.20f), Pair(cw * 0.60f, ch * 0.16f), Pair(cw * 0.64f, ch * 0.20f),
        Pair(cw * 0.60f, ch * 0.26f)  // Heart 2
    )
    val starCol = Color(0xFFFFFFFF).copy(alpha = alpha)
    val lineCol = Color(0x6690E0EF).copy(alpha = 0.55f * alpha)
    for (i in 0 until 4) {
        val next = (i + 1) % 4
        scope.drawLine(lineCol, Offset(starNodes[i].first, starNodes[i].second), Offset(starNodes[next].first, starNodes[next].second), strokeWidth = 1.2f * p)
    }
    for (i in 4 until 8) {
        val next = 4 + ((i - 4 + 1) % 4)
        scope.drawLine(lineCol, Offset(starNodes[i].first, starNodes[i].second), Offset(starNodes[next].first, starNodes[next].second), strokeWidth = 1.2f * p)
    }
    scope.drawLine(lineCol, Offset(starNodes[2].first, starNodes[2].second), Offset(starNodes[4].first, starNodes[4].second), strokeWidth = 1.2f * p)

    starNodes.forEachIndexed { i, (sx, sy) ->
        val twinkle = (0.7f + 0.3f * sin(t * 3.5f + i * 1.1f))
        val r = (2.2f * twinkle) * p
        scope.drawCircle(starCol, radius = r, center = Offset(sx, sy))
        scope.drawLine(starCol, Offset(sx - 3.5f * p * twinkle, sy), Offset(sx + 3.5f * p * twinkle, sy), strokeWidth = 0.9f * p)
        scope.drawLine(starCol, Offset(sx, sy - 3.5f * p * twinkle), Offset(sx, sy + 3.5f * p * twinkle), strokeWidth = 0.9f * p)
    }

    // 4. Periodic Shooting Star streak
    val shootCycle = (t * 0.8f) % 5f
    if (shootCycle < 1.2f) {
        val prog = shootCycle / 1.2f
        val shootX = cw * (0.10f + prog * 0.45f)
        val shootY = ch * (0.10f + prog * 0.30f)
        scope.drawLine(Color(0xFFFFFFFF).copy(alpha = (1f - prog) * alpha), Offset(shootX - 25f * p, shootY - 18f * p), Offset(shootX, shootY), strokeWidth = 2f * p)
    }
}

private fun drawForestDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    val treeCol1 = Color(0xFF081C15).copy(alpha = 0.85f * alpha)
    val treeCol2 = Color(0xFF1B4332).copy(alpha = 0.78f * alpha)

    // Left tree cluster
    for (layer in 0 until 3) {
        val tw = (40f - layer * 8f) * p
        val ty = ch * 0.44f + layer * 16f * p
        val path = Path().apply {
            moveTo(0f, ty + 24f * p)
            lineTo(tw, ty + 24f * p)
            lineTo(tw * 0.2f, ty)
            lineTo(0f, ty)
            close()
        }
        scope.drawPath(path, if (layer % 2 == 0) treeCol1 else treeCol2)
    }

    // Right tree cluster
    for (layer in 0 until 3) {
        val tw = (45f - layer * 8f) * p
        val ty = ch * 0.42f + layer * 16f * p
        val path = Path().apply {
            moveTo(cw, ty + 24f * p)
            lineTo(cw - tw, ty + 24f * p)
            lineTo(cw - tw * 0.2f, ty)
            lineTo(cw, ty)
            close()
        }
        scope.drawPath(path, if (layer % 2 == 0) treeCol1 else treeCol2)
    }

    // 2. Enchanted Spirited Fireflies & Forest Wisps
    val wispCol1 = Color(0xFFCCFF33).copy(alpha = 0.90f * alpha)
    val wispCol2 = Color(0xFF52B788).copy(alpha = 0.85f * alpha)
    val wispHalo = Color(0x44D8F3DC).copy(alpha = 0.40f * alpha)

    for (w in 0 until 12) {
        val seed = w * 29.3f
        val fx = (0.15f + (seed % 0.70f))
        val fy = (0.28f + ((seed * 1.7f) % 0.40f))
        val wx = cw * fx + sin(t * 1.4f + w * 0.8f) * 22f * p
        val wy = ch * fy + cos(t * 1.1f + w * 1.2f) * 16f * p
        val pulse = (0.7f + 0.3f * sin(t * 3f + w))
        val wr = (2.5f * pulse) * p

        scope.drawCircle(wispHalo, radius = wr * 3.5f, center = Offset(wx, wy))
        scope.drawCircle(if (w % 2 == 0) wispCol1 else wispCol2, radius = wr, center = Offset(wx, wy))
    }

    // 3. Giant Fairy Mushrooms in bottom corner
    val mushX = cw * 0.15f
    val mushY = ch * 0.72f
    scope.drawRect(Color(0xFFE2EAFC).copy(alpha = 0.95f * alpha), Offset(mushX - 3f * p, mushY), Size(6f * p, 16f * p))
    scope.drawCircle(Color(0xFFE63946).copy(alpha = 0.95f * alpha), radius = 12f * p, center = Offset(mushX, mushY))
    scope.drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.95f * alpha), radius = 2f * p, center = Offset(mushX - 4f * p, mushY - 4f * p))
    scope.drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.95f * alpha), radius = 2.2f * p, center = Offset(mushX + 4f * p, mushY - 3f * p))
    scope.drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.95f * alpha), radius = 1.8f * p, center = Offset(mushX, mushY - 7f * p))
}

private fun drawHomeDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Draped Fairy Light Garland along top
    val garlandY = ch * 0.14f
    val bulbCount = 9
    val bulbSpacing = cw / (bulbCount + 1)
    val wireCol = Color(0xFF2B2D42).copy(alpha = 0.65f * alpha)

    for (b in 1..bulbCount) {
        val bx = b * bulbSpacing
        val sag = sin((b.toFloat() / (bulbCount + 1)) * 3.14159f) * 14f * p
        val by = garlandY + sag
        val prevX = (b - 1) * bulbSpacing
        val prevSag = sin(((b - 1).toFloat() / (bulbCount + 1)) * 3.14159f) * 14f * p
        if (b > 1) {
            scope.drawLine(wireCol, Offset(prevX, garlandY + prevSag), Offset(bx, by), strokeWidth = 1.2f * p)
        }

        val bulbColor = if (b % 2 == 0) Color(0xFFFFD166) else Color(0xFFFF9EAA)
        val pulse = (0.75f + 0.25f * sin(t * 3.5f + b * 1.3f)) * alpha
        scope.drawCircle(bulbColor.copy(alpha = 0.35f * pulse), radius = 8f * p, center = Offset(bx, by + 4f * p))
        scope.drawCircle(bulbColor.copy(alpha = 0.95f * pulse), radius = 3.5f * p, center = Offset(bx, by + 4f * p))
    }

    // 2. Rising Warm Hearth Embers from floor
    for (e in 0 until 18) {
        val seed = e * 23.7f
        val ex = cw * (0.25f + (seed % 0.50f)) + sin(t * 2.2f + e) * 8f * p
        val ey = (ch * 0.75f) - ((t * 25f * p + seed * 15f) % (ch * 0.35f))
        val emberCol = when (e % 3) {
            0 -> Color(0xFFFFB703)
            1 -> Color(0xFFFB8500)
            else -> Color(0xFFFF4800)
        }
        val er = (1.5f + (e % 3) * 0.6f) * p
        scope.drawCircle(emberCol.copy(alpha = 0.85f * alpha), radius = er, center = Offset(ex, ey))
    }

    // 3. Floating Cozy Teacup with swirling steam
    val cupX = cw * 0.80f + sin(t * 1.1f) * 6f * p
    val cupY = ch * 0.45f + cos(t * 1.3f) * 8f * p
    scope.drawRect(Color(0xFFFFF0F3).copy(alpha = 0.95f * alpha), Offset(cupX - 10f * p, cupY), Size(20f * p, 12f * p))
    scope.drawRect(Color(0xFFFFCCD5).copy(alpha = 0.95f * alpha), Offset(cupX - 14f * p, cupY + 12f * p), Size(28f * p, 3f * p))
    scope.drawCircle(Color(0xFFFF4D6D).copy(alpha = 0.90f * alpha), radius = 2.5f * p, center = Offset(cupX, cupY + 6f * p))
    for (s in 0 until 3) {
        val steamSway = sin(t * 3f + s * 1.5f) * 4f * p
        val sy = cupY - 6f * p - s * 8f * p
        scope.drawCircle(Color(0x88FFFFFF).copy(alpha = 0.60f * alpha), radius = (2f + s * 0.8f) * p, center = Offset(cupX - 4f * p + s * 4f * p + steamSway, sy))
    }
}

private fun drawRainDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Heavy Angled Wind-blown Rain Streaks
    val rainCol = Color(0xAA90E0EF).copy(alpha = 0.60f * alpha)
    for (r in 0 until 40) {
        val seed = r * 31.7f
        val rx = (seed * 43.1f + t * 90f * p) % cw
        val ry = (seed * 67.3f + t * 450f * p) % (ch * 0.70f)
        scope.drawLine(rainCol, Offset(rx, ry), Offset(rx - 8f * p, ry + 16f * p), strokeWidth = 1.2f * p)
    }

    // 2. Expanding Concentric Ground Puddle Ripples
    val rippleCol = Color(0x88ADE8F4).copy(alpha = 0.50f * alpha)
    for (k in 0 until 6) {
        val seed = k * 47.9f
        val kx = cw * (0.15f + (seed % 0.70f))
        val ky = ch * 0.68f + (seed % 40f) * p
        val rPhase = ((t * 1.8f + k * 0.7f) % 1.5f) / 1.5f
        val radiusX = rPhase * 18f * p
        val radiusY = rPhase * 6f * p
        scope.drawOval(
            rippleCol.copy(alpha = (1f - rPhase) * 0.60f * alpha),
            topLeft = Offset(kx - radiusX, ky - radiusY),
            size = Size(radiusX * 2f, radiusY * 2f),
            style = Stroke(width = 1.2f * p)
        )
    }

    // 3. Periodic Forked Lightning Flash
    val lightningCycle = (t * 0.35f) % 5f
    if (lightningCycle < 0.25f) {
        scope.drawRect(Color(0x33E2EAFC).copy(alpha = 0.35f * alpha))
        val boltCol = Color(0xFFFFFFFF).copy(alpha = 0.95f * alpha)
        val boltPath = Path().apply {
            moveTo(cw * 0.45f, 0f)
            lineTo(cw * 0.43f, ch * 0.15f)
            lineTo(cw * 0.48f, ch * 0.28f)
            lineTo(cw * 0.44f, ch * 0.42f)
            lineTo(cw * 0.50f, ch * 0.55f)
        }
        scope.drawPath(boltPath, boltCol, style = Stroke(width = 2.5f * p))
        scope.drawLine(boltCol, Offset(cw * 0.48f, ch * 0.28f), Offset(cw * 0.55f, ch * 0.38f), strokeWidth = 1.5f * p)
    }
}

private fun drawCityDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Sweeping Searchlight Beams
    for (s in 0 until 2) {
        val baseBeamX = cw * (0.35f + s * 0.30f)
        val sweepAngle = sin(t * 0.8f + s * 2f) * 25f * p
        val beamPath = Path().apply {
            moveTo(baseBeamX, ch * 0.65f)
            lineTo(baseBeamX + sweepAngle - 35f * p, 0f)
            lineTo(baseBeamX + sweepAngle + 35f * p, 0f)
            close()
        }
        scope.drawPath(beamPath, Color(0x1848CAE4).copy(alpha = 0.16f * alpha))
    }

    // 2. Layered Cyber Skyline Buildings
    val bldgDark = Color(0xFF0D1B2A).copy(alpha = 0.88f * alpha)
    val bldgMid  = Color(0xFF1B263B).copy(alpha = 0.85f * alpha)
    val baseH = ch * 0.68f

    val buildings = listOf(
        Triple(0f, 32f * p, ch * 0.42f),
        Triple(cw * 0.11f, 36f * p, ch * 0.35f),
        Triple(cw * 0.24f, 42f * p, ch * 0.45f),
        Triple(cw * 0.38f, 38f * p, ch * 0.32f),
        Triple(cw * 0.52f, 44f * p, ch * 0.38f),
        Triple(cw * 0.68f, 36f * p, ch * 0.34f),
        Triple(cw * 0.81f, 40f * p, ch * 0.44f),
        Triple(cw * 0.92f, 38f * p, ch * 0.40f)
    )

    buildings.forEachIndexed { idx, (bx, bw, topY) ->
        val col = if (idx % 2 == 0) bldgDark else bldgMid
        scope.drawRect(col, Offset(bx, topY), Size(bw, baseH - topY))

        if (idx == 3 || idx == 5) {
            val spireX = bx + bw * 0.5f
            scope.drawLine(Color(0xFFE0E1DD).copy(alpha = alpha), Offset(spireX, topY), Offset(spireX, topY - 18f * p), strokeWidth = 1.5f * p)
            val blink = if (sin(t * 4f + idx) > 0f) 0.95f else 0.2f
            scope.drawCircle(Color(0xFFFF1E56).copy(alpha = blink * alpha), radius = 2f * p, center = Offset(spireX, topY - 18f * p))
        }

        val rows = ((baseH - topY) / (8f * p)).toInt()
        val cols = (bw / (7f * p)).toInt()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val winSeed = idx * 97 + r * 13 + c * 31
                if (winSeed % 3 != 0) {
                    val winCol = when (winSeed % 5) {
                        0 -> Color(0xFFFFD166)
                        1 -> Color(0xFF48CAE4)
                        2 -> Color(0xFFFF70A6)
                        else -> Color(0xFFFFF3B0)
                    }
                    val wx = bx + 3f * p + c * (7f * p)
                    val wy = topY + 4f * p + r * (8f * p)
                    scope.drawRect(winCol.copy(alpha = 0.85f * alpha), Offset(wx, wy), Size(3f * p, 3.5f * p))
                }
            }
        }
    }

    // 3. Glowing Neon Heart Billboard atop Building #3
    val heartBldgX = cw * 0.38f + 19f * p
    val heartBldgY = ch * 0.32f - 14f * p
    val heartPulse = (0.75f + 0.25f * sin(t * 3f)) * alpha
    scope.drawCircle(Color(0xFFFF0054).copy(alpha = heartPulse), radius = 5f * p, center = Offset(heartBldgX, heartBldgY))
}

private fun drawSweetDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Giant Floating Strawberry Shortcake Slice
    val cakeX = cw * 0.24f + sin(t * 1.2f) * 8f * p
    val cakeY = ch * 0.38f + cos(t * 1.4f) * 10f * p
    scope.drawRect(Color(0xFFFFE066).copy(alpha = 0.95f * alpha), Offset(cakeX - 16f * p, cakeY), Size(32f * p, 7f * p))
    scope.drawRect(Color(0xFFFFF3B0).copy(alpha = 0.95f * alpha), Offset(cakeX - 16f * p, cakeY + 7f * p), Size(32f * p, 5f * p))
    scope.drawRect(Color(0xFFFFE066).copy(alpha = 0.95f * alpha), Offset(cakeX - 16f * p, cakeY + 12f * p), Size(32f * p, 7f * p))
    scope.drawCircle(Color(0xFFFF0054).copy(alpha = 0.95f * alpha), radius = 6f * p, center = Offset(cakeX, cakeY - 4f * p))
    scope.drawCircle(Color(0xFF52B788).copy(alpha = 0.95f * alpha), radius = 2.5f * p, center = Offset(cakeX + 4f * p, cakeY - 8f * p))

    // 2. Giant Steaming Dumpling / Momo
    val momoX = cw * 0.76f - sin(t * 1.1f) * 8f * p
    val momoY = ch * 0.40f + sin(t * 1.5f) * 8f * p
    val momoCol = Color(0xFFFDF0D5).copy(alpha = 0.95f * alpha)
    scope.drawCircle(Color(0x55FFD166).copy(alpha = 0.45f * alpha), radius = 18f * p, center = Offset(momoX, momoY))
    scope.drawCircle(momoCol, radius = 12f * p, center = Offset(momoX, momoY))
    scope.drawRect(Color(0xFFE0C9A6).copy(alpha = 0.95f * alpha), Offset(momoX - 4f * p, momoY - 14f * p), Size(8f * p, 4f * p))
    for (s in 0 until 3) {
        val sSway = sin(t * 3.5f + s * 1.2f) * 4f * p
        val sy = momoY - 16f * p - s * 7f * p
        scope.drawCircle(Color(0x99FFFFFF).copy(alpha = 0.65f * alpha), radius = (2f + s * 0.6f) * p, center = Offset(momoX + sSway, sy))
    }

    // 3. Floating Wrapped Candies & Macarons
    val treats = listOf(
        Pair(cw * 0.48f, ch * 0.28f),
        Pair(cw * 0.62f, ch * 0.32f),
        Pair(cw * 0.38f, ch * 0.46f)
    )
    treats.forEachIndexed { i, (tx, ty) ->
        val tBob = sin(t * 2.2f + i * 1.5f) * 6f * p
        val treatCol = when (i) {
            0 -> Color(0xFFFF70A6)
            1 -> Color(0xFF70D6FF)
            else -> Color(0xFFFF9770)
        }
        scope.drawCircle(treatCol.copy(alpha = 0.90f * alpha), radius = 6f * p, center = Offset(tx, ty + tBob))
        scope.drawRect(Color(0xFFFFFFFF).copy(alpha = 0.90f * alpha), Offset(tx - 6f * p, ty + tBob - 1f * p), Size(12f * p, 2f * p))
    }

    // 4. Falling Colorful Sugar Sprinkles
    for (s in 0 until 20) {
        val seed = s * 41.3f
        val sx = (seed * 29.1f) % cw
        val sy = (seed * 53.7f + t * 40f * p) % (ch * 0.70f)
        val sCol = when (s % 4) {
            0 -> Color(0xFFFF5D8F)
            1 -> Color(0xFFFFD166)
            2 -> Color(0xFF06D6A0)
            else -> Color(0xFF118AB2)
        }
        scope.drawRect(sCol.copy(alpha = 0.85f * alpha), Offset(sx, sy), Size(2.2f * p, 2.2f * p))
    }
}

private fun drawFallbackDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
    // 1. Giant Ethereal Moon Crescent
    val moonX = cw * 0.75f
    val moonY = ch * 0.22f
    val moonR = 30f * p
    scope.drawCircle(Color(0x33DDA15E).copy(alpha = 0.30f * alpha), radius = moonR * 1.35f, center = Offset(moonX, moonY))
    scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.92f * alpha), radius = moonR, center = Offset(moonX, moonY))
    scope.drawCircle(Color(0xFF3A0CA3).copy(alpha = 0.88f * alpha), radius = moonR * 0.82f, center = Offset(moonX + 9f * p, moonY - 3f * p))

    // 2. Floating Iridescent Dream Bubbles
    val bubbleColors = listOf(Color(0xBB90E0EF), Color(0xBBF72585), Color(0xBB7209B7), Color(0xBB4CC9F0))
    for (b in 0 until 8) {
        val seed = b * 33.7f
        val bx = cw * (0.12f + (seed % 0.76f)) + sin(t * 1.2f + b) * 12f * p
        val by = ch * 0.70f - ((t * 22f * p + seed * 16f) % (ch * 0.45f))
        val br = (8f + (b % 4) * 3f) * p
        val bCol = bubbleColors[b % bubbleColors.size].copy(alpha = 0.50f * alpha)
        scope.drawCircle(bCol, radius = br, center = Offset(bx, by))
        scope.drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.85f * alpha), radius = br * 0.3f, center = Offset(bx - br * 0.35f, by - br * 0.35f))
    }

    // 3. Drifting Golden Stardust Motes
    val starCol = Color(0xFFFFD166).copy(alpha = 0.90f * alpha)
    for (s in 0 until 16) {
        val seed = s * 19.1f
        val sx = (seed * 37.3f + sin(t * 1.5f + s) * 10f * p) % cw
        val sy = (seed * 51.9f + cos(t * 1.1f + s) * 8f * p) % (ch * 0.65f)
        val twinkle = (0.5f + 0.5f * sin(t * 4f + s)) * alpha
        scope.drawCircle(starCol.copy(alpha = twinkle), radius = 1.8f * p, center = Offset(sx, sy))
    }
}

private fun drawEnvironment(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    env: EnvironmentType,
    isNight: Boolean,
    isSunset: Boolean,
    isMorning: Boolean,
    timeSeconds: Float,
    pixelScale: Float,
    engine: SceneEngine
) {
    val p = pixelScale

    when (env) {
        EnvironmentType.MEADOW -> {
            drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, timeSeconds, p, weather = engine.weather)
            drawMeadowGround(scope, cw, ch, isNight, isSunset, timeSeconds, p, engine.weather)
            // Cottage house in background
            WorldSprites.drawCottage(scope, cw * 0.22f, ch * 0.67f, p, timeSeconds, isNight, engine.weather)
            // Animated chimney smoke
            if (sin(timeSeconds * 3f) > 0.7f) {
                engine.particles.spawnChimneySmoke(cw * 0.17f, ch * 0.67f - 54 * p)
            }
            // Flowers with dynamic garden growth
            drawWildFlowers(scope, cw, ch * 0.70f, p, timeSeconds, engine.gardenStage, engine.flowerWiggleTimer, engine.weather)
            // Curated picnic basket
            WorldSprites.drawPicnicBasket(scope, cw * 0.84f, ch * 0.70f, p)
            // Ambient birds
            engine.birdSystem.drawBirds(scope, p)
        }
        EnvironmentType.TWILIGHT -> {
            drawSkyAndClouds(scope, cw, ch, isNight = isNight, isSunset = isSunset, isMorning = isMorning, time = timeSeconds, p = p, weather = engine.weather, isPinkSunset = true)
            drawMeadowGround(scope, cw, ch, isNight = isNight, isSunset = isSunset, timeSeconds = timeSeconds, p = p, weather = engine.weather)
            drawWildFlowers(scope, cw, ch * 0.70f, p, timeSeconds, engine.gardenStage, engine.flowerWiggleTimer, engine.weather)
            // Twinkling fairy light jar on the grass
            WorldSprites.drawFairyJar(scope, cw * 0.76f, ch * 0.70f, p, timeSeconds)
            // Ambient birds
            engine.birdSystem.drawBirds(scope, p)
        }
        EnvironmentType.TREE_HILL -> {
            drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, timeSeconds, p, weather = engine.weather)
            drawMeadowGround(scope, cw, ch, isNight, isSunset, timeSeconds, p, engine.weather)
            // Summer daytime cool tree shade under the canopy
            if (engine.weather == com.example.scene.WeatherType.SUNNY && !isNight && !isSunset) {
                WorldSprites.drawTreeShade(scope, cw * 0.5f, ch * 0.69f, p, timeSeconds)
            }
            // Grand Pixel Tree with hanging breeze swing & bark growth
            WorldSprites.drawTree(
                scope = scope,
                baseX = cw * 0.5f,
                groundY = ch * 0.69f,
                p = p,
                timeSeconds = timeSeconds,
                weather = engine.weather,
                mossStage = engine.treeMossGrowthStage,
                gfInitial = engine.girlfriendInitial
            )
            WorldSprites.drawTreeSwing(scope, cw * 0.5f, ch * 0.69f, p, timeSeconds)
            drawWildFlowers(scope, cw, ch * 0.70f, p, timeSeconds, engine.gardenStage, engine.flowerWiggleTimer, engine.weather)
            // Ambient birds
            engine.birdSystem.drawBirds(scope, p)
        }
        EnvironmentType.KITCHEN -> {
            drawKitchenRoom(
                scope = scope,
                cw = cw,
                ch = ch,
                isNight = isNight,
                isSunset = isSunset,
                p = p,
                timeSeconds = timeSeconds,
                weather = engine.weather,
                fridgeOpen = engine.fridgeDoorOpenTimer > 0f,
                sinkRunning = engine.sinkRunningTimer > 0f
            )
            WorldSprites.drawKitchen(
                scope = scope,
                counterX = cw * 0.66f,
                groundY = ch * 0.67f,
                p = p,
                timeSeconds = timeSeconds,
                isNight = isNight,
                cabinetOpen = engine.cabinetOpenTimer > 0f
            )
            WorldSprites.drawPedalDustbin(scope, cw * 0.785f, ch * 0.67f, p)

            // --- Kitchen dynamic overlays (drawn on top of static sprites) ---
            val floorY = ch * 0.65f
            val floorH = ch - floorY

            // 1. Wall Clock hands: spin fast then settle to real device time
            if (engine.clockSpinTimer > 0f) {
                val spinDur = 2.4f
                val elapsed = spinDur - engine.clockSpinTimer
                val spinFrac = elapsed / spinDur
                // Spin phase: 0..0.55 = fast spinning, 0.55..1.0 = settle to real time
                val clockCx = cw * 0.49f
                val clockCy = ch * 0.38f + 16f * p
                val clockR = 12f * p
                val cal = java.util.Calendar.getInstance()
                val realHourAngle = ((cal.get(java.util.Calendar.HOUR) % 12 + cal.get(java.util.Calendar.MINUTE) / 60f) / 12f) * (2f * Math.PI.toFloat())
                val realMinAngle  = (cal.get(java.util.Calendar.MINUTE) / 60f) * (2f * Math.PI.toFloat())
                val spinRev = if (spinFrac < 0.55f) {
                    val t = spinFrac / 0.55f
                    (1f - t * t) * 8f * (2f * Math.PI.toFloat())  // 8 full spins decelerating
                } else {
                    0f
                }
                val hourAngle  = realHourAngle  + spinRev - Math.PI.toFloat() / 2f
                val minuteAngle = realMinAngle  + spinRev * 1.5f - Math.PI.toFloat() / 2f
                val handColor = Color(0xFF3E2413)
                scope.drawLine(
                    color = handColor,
                    start = androidx.compose.ui.geometry.Offset(clockCx, clockCy),
                    end   = androidx.compose.ui.geometry.Offset(clockCx + cos(hourAngle) * clockR * 0.60f, clockCy + sin(hourAngle) * clockR * 0.60f),
                    strokeWidth = 2.2f * p,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                scope.drawLine(
                    color = handColor.copy(alpha = 0.75f),
                    start = androidx.compose.ui.geometry.Offset(clockCx, clockCy),
                    end   = androidx.compose.ui.geometry.Offset(clockCx + cos(minuteAngle) * clockR * 0.82f, clockCy + sin(minuteAngle) * clockR * 0.82f),
                    strokeWidth = 1.4f * p,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            // 2. Dustbin lid: pop up then fall back over 1.2 s
            if (engine.binLidTimer > 0f) {
                val binX = cw * 0.785f
                val binY = floorY
                val lidDur = 1.2f
                val t = (lidDur - engine.binLidTimer) / lidDur   // 0..1
                // Rise 0..0.3, hold 0.3..0.6, fall 0.6..1.0
                val lidOff = when {
                    t < 0.3f -> -12f * p * (t / 0.3f)
                    t < 0.6f -> -12f * p
                    else -> -12f * p * (1f - (t - 0.6f) / 0.4f)
                }
                scope.drawRect(
                    color = Color(0xFFAAAAAA),
                    topLeft = androidx.compose.ui.geometry.Offset(binX - 6f * p, binY - 18f * p + lidOff),
                    size = Size(12f * p, 4f * p)
                )
            }

            // 3. Produce crate: shake left-right with decay over 0.7 s
            if (engine.crateShakeTimer > 0f) {
                val shakeDur = 0.7f
                val t = (shakeDur - engine.crateShakeTimer) / shakeDur
                val decay = 1f - t
                val shakeX = sin(t * 30f) * 4f * p * decay
                val cx = cw * 0.13f + shakeX
                val cy = floorY + floorH * 0.74f
                // Draw a simple crate outline overlay so the shake is visible
                scope.drawRect(
                    color = Color(0xFFB07B42).copy(alpha = 0.55f),
                    topLeft = androidx.compose.ui.geometry.Offset(cx - 10f * p, cy - 9f * p),
                    size = Size(20f * p, 18f * p)
                )
            }

            // 4. Floor planter: water drops arc down (first 1.0 s), leaf tips emerge (0.8..2.0 s)
            if (engine.planterAnimTimer > 0f) {
                val animDur = 2.0f
                val elapsed = animDur - engine.planterAnimTimer
                val planterCx = cw * 0.86f
                val planterCy = floorY + floorH * 0.74f - 8f * p

                // Water drops: 5 small circles arcing in a parabola toward the planter top
                if (elapsed < 1.0f) {
                    val dropT = elapsed / 1.0f
                    repeat(5) { i ->
                        val phase = (dropT - i * 0.12f).coerceIn(0f, 1f)
                        if (phase > 0f) {
                            val dx = planterCx - 18f * p + i * 4f * p + phase * (4f * p - i * 4f * p)
                            val dy = planterCy - 22f * p * (1f - phase * phase)
                            scope.drawCircle(
                                color = Color(0xFF6EC6F5).copy(alpha = (1f - phase) * 0.85f),
                                radius = 2.5f * p,
                                center = androidx.compose.ui.geometry.Offset(dx, dy)
                            )
                        }
                    }
                }

                // Leaf tips: three oval tips emerging from pot rim
                if (elapsed > 0.8f) {
                    val leafT = ((elapsed - 0.8f) / 1.2f).coerceIn(0f, 1f)
                    val leafOffsets = listOf(-6f * p to -1f, 0f to -1.3f, 5f * p to -0.9f)
                    for ((lx, ly) in leafOffsets) {
                        val tipLen = leafT * 10f * p * (-ly)
                        scope.drawOval(
                            color = Color(0xFF4CAF50).copy(alpha = leafT * 0.85f),
                            topLeft = androidx.compose.ui.geometry.Offset(planterCx + lx - 3f * p, planterCy - tipLen),
                            size = Size(6f * p, tipLen.coerceAtLeast(1f))
                        )
                    }
                }
            }

            // 5. Step stool wobble: rotated rect around base over 0.8 s
            if (engine.stoolWobbleTimer > 0f) {
                val wobDur = 0.8f
                val t = (wobDur - engine.stoolWobbleTimer) / wobDur
                val decay = 1f - t
                val stoolCx = cw * 0.50f - 26f * p - 16f * p
                val stoolCy = floorY + floorH * 0.44f + 7f * p + 16f * p
                val angle = sin(t * 25f) * 0.10f * decay   // max ~6 degrees in radians
                val pivotX = stoolCx
                val pivotY = stoolCy + 8f * p
                val cosA = cos(angle)
                val sinA = sin(angle)
                // Rotate a corner (rx,ry) around pivot
                fun rx(x: Float, y: Float) = pivotX + (x - pivotX) * cosA - (y - pivotY) * sinA
                fun ry(x: Float, y: Float) = pivotY + (x - pivotX) * sinA + (y - pivotY) * cosA
                // 4 corners of the stool rect before rotation
                val left  = stoolCx - 7f * p;  val top    = stoolCy - 14f * p
                val right = stoolCx + 7f * p;  val bottom = stoolCy + 2f * p
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(rx(left, top),    ry(left, top))
                    lineTo(rx(right, top),   ry(right, top))
                    lineTo(rx(right, bottom), ry(right, bottom))
                    lineTo(rx(left, bottom), ry(left, bottom))
                    close()
                }
                scope.drawPath(path, color = Color(0xFFB07B42).copy(alpha = 0.50f))
            }
        }
        EnvironmentType.LIVING_ROOM -> {
            val isLampOn = engine.livingRoomLampLit
            val couchPhase = engine.couchVisualPhase
            val isWindowNight = if (isLampOn) isNight else (couchPhase == CouchPhase.NIGHT)
            val isWindowSunset = if (isLampOn) isSunset else (couchPhase == CouchPhase.EVENING)
            val livingRoomIsNight = if (isLampOn) isNight else (couchPhase == CouchPhase.NIGHT)
            drawLivingRoom(scope, cw, ch, isNight = livingRoomIsNight, p = p, lampLit = isLampOn, couchPhase = couchPhase, timeSeconds = timeSeconds)
            WorldSprites.drawPhotoFrame(scope, cw * 0.28f, ch * 0.38f, p)
            WorldSprites.drawWindow(scope, cw * 0.70f, ch * 0.28f, isWindowNight, p, engine.weather, isSunset = isWindowSunset)
            WorldSprites.drawWallCalendar(scope, cw * 0.49f, ch * 0.30f, p, timeSeconds)
            WorldSprites.drawCouch(scope, cw * 0.48f, ch * 0.68f, p)
            WorldSprites.drawFloorLamp(scope, cw * 0.20f, ch * 0.68f, p, engine.livingRoomLampLit)
            WorldSprites.drawWardrobe(scope, cw * 0.85f, ch * 0.65f, p, timeSeconds)

            // --- Living Room dynamic overlays (drawn on top of static sprites) ---
            val lrFloorY = ch * 0.65f
            val lrFloorH = ch - lrFloorY

            // 1. Coffee Table Candle: warm pulsating golden glow & fluttering flame
            if (engine.tableCandleTimer > 0f) {
                val dur = 1.5f
                val t = ((dur - engine.tableCandleTimer) / dur).coerceIn(0f, 1f)
                val tblW = 48 * p
                val tblX = cw * 0.50f - tblW / 2f
                val tblY = lrFloorY + 23 * p
                val cndX = tblX + 37 * p
                val cndY = tblY - 5f * p
                val glowRadius = 14f * p * (1f + 0.4f * sin(t * 14f))
                val glowAlpha = ((1f - t) * 0.45f).coerceIn(0f, 1f)
                scope.drawCircle(
                    color = Color(0xFFFFB703).copy(alpha = glowAlpha),
                    radius = glowRadius,
                    center = androidx.compose.ui.geometry.Offset(cndX + 2.2f * p, cndY + 2f * p)
                )
                // Fluttering bright flame tip
                val flk = sin(t * 26f) * 1.2f * p
                scope.drawCircle(
                    color = Color(0xFFFFD166),
                    radius = 3.5f * p,
                    center = androidx.compose.ui.geometry.Offset(cndX + 2.2f * p + flk, cndY - 1f * p)
                )
                scope.drawCircle(
                    color = Color.White,
                    radius = 1.6f * p,
                    center = androidx.compose.ui.geometry.Offset(cndX + 2.2f * p + flk * 0.5f, cndY - 0.5f * p)
                )
            }

            // 2. Knitted Pouf: elastic squish-bounce overlay
            if (engine.poufBounceTimer > 0f) {
                val dur = 0.8f
                val t = ((dur - engine.poufBounceTimer) / dur).coerceIn(0f, 1f)
                val pfX = cw * 0.70f
                val pfY = lrFloorY + 25 * p
                val pfW = 18 * p
                val pfH = 12 * p
                val squish = sin(t * Math.PI.toFloat() * 3f) * (1f - t) * 3.8f * p
                val sqH = (pfH - squish).coerceAtLeast(4f * p)
                val sqW = pfW + squish * 1.3f
                val sqLeft = pfX - sqW / 2f
                val sqTop = pfY + pfH - sqH
                scope.drawRect(Color(0xFF526456), androidx.compose.ui.geometry.Offset(sqLeft, sqTop + 2 * p), Size(sqW, sqH - 2 * p))
                scope.drawRect(Color(0xFF6B7F6E), androidx.compose.ui.geometry.Offset(sqLeft + p, sqTop), Size(sqW - 2 * p, sqH - 2 * p))
                scope.drawRect(Color(0xFF8A9E8F), androidx.compose.ui.geometry.Offset(sqLeft + 2.5f * p, sqTop + p), Size(sqW - 5 * p, 2.5f * p))
                scope.drawRect(Color(0xFF526456), androidx.compose.ui.geometry.Offset(sqLeft + sqW * 0.25f, sqTop + 2 * p), Size(1.2f * p, sqH - 4 * p))
                scope.drawRect(Color(0xFF526456), androidx.compose.ui.geometry.Offset(sqLeft + sqW * 0.50f, sqTop + 2 * p), Size(1.2f * p, sqH - 4 * p))
                scope.drawRect(Color(0xFF526456), androidx.compose.ui.geometry.Offset(sqLeft + sqW * 0.75f, sqTop + 2 * p), Size(1.2f * p, sqH - 4 * p))
            }

            // 3. Mochi's Cozy Cardboard Box: Cat head & paws peeking out!
            if (engine.cardboardBoxTimer > 0f) {
                val dur = 1.3f
                val t = ((dur - engine.cardboardBoxTimer) / dur).coerceIn(0f, 1f)
                val cBoxX = cw * 0.17f
                val cBoxY = lrFloorY + lrFloorH * 0.75f
                val cBoxW = 20 * p
                val cBoxH = 13 * p
                val cbLeft = cBoxX - cBoxW / 2f
                val peek = sin(t * Math.PI.toFloat()).coerceIn(0f, 1f)
                val headLift = 8f * p * peek

                // Cat head emerging from the yellow cushion
                val catHeadW = 10 * p
                val catHeadH = 7 * p
                val catHeadX = cBoxX - catHeadW / 2f
                val catHeadY = cBoxY + 1.5f * p - headLift

                // Ears
                scope.drawRect(Color(0xFFE76F51), androidx.compose.ui.geometry.Offset(catHeadX + p, catHeadY - 2.5f * p), Size(2.2f * p, 3f * p))
                scope.drawRect(Color(0xFFE76F51), androidx.compose.ui.geometry.Offset(catHeadX + catHeadW - 3.2f * p, catHeadY - 2.5f * p), Size(2.2f * p, 3f * p))
                scope.drawRect(Color(0xFFFFF0F3), androidx.compose.ui.geometry.Offset(catHeadX + 1.5f * p, catHeadY - 1.5f * p), Size(1.2f * p, 1.8f * p))
                scope.drawRect(Color(0xFFFFF0F3), androidx.compose.ui.geometry.Offset(catHeadX + catHeadW - 2.7f * p, catHeadY - 1.5f * p), Size(1.2f * p, 1.8f * p))

                // Face & calico ginger patch
                scope.drawRect(Color(0xFFFFF3E0), androidx.compose.ui.geometry.Offset(catHeadX, catHeadY), Size(catHeadW, catHeadH))
                scope.drawRect(Color(0xFFE76F51), androidx.compose.ui.geometry.Offset(catHeadX, catHeadY), Size(3.5f * p, 3.5f * p))

                // Eyes: happy squint when at the peak of the peek!
                if (t in 0.35f..0.65f) {
                    scope.drawRect(Color(0xFF264653), androidx.compose.ui.geometry.Offset(catHeadX + 2f * p, catHeadY + 2.5f * p), Size(2f * p, 1f * p))
                    scope.drawRect(Color(0xFF264653), androidx.compose.ui.geometry.Offset(catHeadX + catHeadW - 4f * p, catHeadY + 2.5f * p), Size(2f * p, 1f * p))
                } else {
                    scope.drawRect(Color(0xFF264653), androidx.compose.ui.geometry.Offset(catHeadX + 2.2f * p, catHeadY + 2.2f * p), Size(1.5f * p, 1.5f * p))
                    scope.drawRect(Color(0xFF264653), androidx.compose.ui.geometry.Offset(catHeadX + catHeadW - 3.7f * p, catHeadY + 2.2f * p), Size(1.5f * p, 1.5f * p))
                }
                // Nose & cheeks
                scope.drawRect(Color(0xFFFFB5C2), androidx.compose.ui.geometry.Offset(catHeadX + catHeadW / 2f - 0.6f * p, catHeadY + 4f * p), Size(1.2f * p, 1f * p))
                scope.drawRect(Color(0xFFFFCCD5).copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(catHeadX + p, catHeadY + 3.8f * p), Size(1.8f * p, 1.2f * p))
                scope.drawRect(Color(0xFFFFCCD5).copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(catHeadX + catHeadW - 2.8f * p, catHeadY + 3.8f * p), Size(1.8f * p, 1.2f * p))

                // Box front overlay (ensures cat is cleanly tucked inside the box)
                scope.drawRect(Color(0xFFC59B76), androidx.compose.ui.geometry.Offset(cbLeft, cBoxY + 3 * p), Size(cBoxW, cBoxH - 3 * p))
                scope.drawRect(Color(0xFFA98467), androidx.compose.ui.geometry.Offset(cbLeft, cBoxY + 3 * p), Size(cBoxW, 1.2f * p))
                val pawX = cBoxX
                val pawY = cBoxY + 7.5f * p
                scope.drawRect(Color(0xFF7F5539), androidx.compose.ui.geometry.Offset(pawX - 1.5f * p, pawY), Size(3 * p, 2.5f * p))
                scope.drawRect(Color(0xFF7F5539), androidx.compose.ui.geometry.Offset(pawX - 2.5f * p, pawY - 1.5f * p), Size(1.2f * p, 1.2f * p))
                scope.drawRect(Color(0xFF7F5539), androidx.compose.ui.geometry.Offset(pawX - 0.6f * p, pawY - 2 * p), Size(1.2f * p, 1.2f * p))
                scope.drawRect(Color(0xFF7F5539), androidx.compose.ui.geometry.Offset(pawX + 1.3f * p, pawY - 1.5f * p), Size(1.2f * p, 1.2f * p))

                // Paws placed over the front rim
                if (peek > 0.35f) {
                    scope.drawRect(Color.White, androidx.compose.ui.geometry.Offset(cBoxX - 4.5f * p, cBoxY + 2.2f * p), Size(3f * p, 2.2f * p))
                    scope.drawRect(Color.White, androidx.compose.ui.geometry.Offset(cBoxX + 1.5f * p, cBoxY + 2.2f * p), Size(3f * p, 2.2f * p))
                    scope.drawRect(Color(0xFFFFCCD5), androidx.compose.ui.geometry.Offset(cBoxX - 3.8f * p, cBoxY + 3f * p), Size(1.6f * p, 1f * p))
                    scope.drawRect(Color(0xFFFFCCD5), androidx.compose.ui.geometry.Offset(cBoxX + 2.2f * p, cBoxY + 3f * p), Size(1.6f * p, 1f * p))
                }
            }

            // 4. Woven Storage Basket: basket wobble & playful yarn ball rolling out
            if (engine.basketYarnTimer > 0f) {
                val dur = 1.4f
                val t = ((dur - engine.basketYarnTimer) / dur).coerceIn(0f, 1f)
                val bskX = cw * 0.30f
                val bskY = lrFloorY + lrFloorH * 0.75f
                val rollDist = 22f * p * sin(t * Math.PI.toFloat() * 0.90f).coerceAtLeast(0f)
                val bounceY = abs(sin(t * Math.PI.toFloat() * 2f)) * 4f * p * (1f - t)
                val yarnX = bskX + 8 * p + rollDist
                val yarnY = bskY + 7 * p - bounceY

                // Trailing yarn thread from basket
                val midX = (bskX + 7f * p + yarnX) / 2f
                val midY = bskY + 8f * p
                scope.drawLine(
                    color = Color(0xFFB8B8D1).copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(bskX + 7f * p, bskY + 3f * p),
                    end = androidx.compose.ui.geometry.Offset(midX, midY),
                    strokeWidth = 1.4f * p,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                scope.drawLine(
                    color = Color(0xFFB8B8D1).copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(midX, midY),
                    end = androidx.compose.ui.geometry.Offset(yarnX - p, yarnY),
                    strokeWidth = 1.4f * p,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Rolling yarn ball
                scope.drawCircle(Color(0xFFB8B8D1), radius = 3.5f * p, center = androidx.compose.ui.geometry.Offset(yarnX, yarnY))
                scope.drawCircle(Color(0xFFD8D8E8), radius = 2f * p, center = androidx.compose.ui.geometry.Offset(yarnX - p, yarnY - p))
            }

            // 5. Wooden Magazine & Record Rack: album sleeve lifts & vinyl disc spins out!
            if (engine.magazineRackTimer > 0f) {
                val dur = 1.3f
                val t = ((dur - engine.magazineRackTimer) / dur).coerceIn(0f, 1f)
                val rackX = cw * 0.80f
                val rackY = lrFloorY + lrFloorH * 0.74f
                val rackW = 26 * p
                val rLeft = rackX - rackW / 2f
                val lift = sin(t * Math.PI.toFloat()) * 13f * p
                val albumX = rLeft + 9 * p
                val albumY = rackY - 6 * p - lift

                // Animated album jacket lifting
                scope.drawRect(Color(0xFFE76F51), androidx.compose.ui.geometry.Offset(albumX, albumY), Size(8 * p, 13 * p))
                scope.drawRect(Color(0xFFFFD166), androidx.compose.ui.geometry.Offset(albumX + 1.8f * p, albumY + 2.5f * p), Size(4.4f * p, 4.4f * p))

                // Vinyl disc peeking/spinning out to the right
                val discSlide = sin(t * Math.PI.toFloat()) * 7f * p
                val discCx = albumX + 6 * p + discSlide
                val discCy = albumY + 6.5f * p
                scope.drawCircle(Color(0xFF1D1E2C), radius = 5.5f * p, center = androidx.compose.ui.geometry.Offset(discCx, discCy))
                scope.drawCircle(Color(0xFF333333), radius = 3.5f * p, center = androidx.compose.ui.geometry.Offset(discCx, discCy))
                scope.drawCircle(Color(0xFFE63946), radius = 1.8f * p, center = androidx.compose.ui.geometry.Offset(discCx, discCy))
                scope.drawCircle(Color.White, radius = 0.6f * p, center = androidx.compose.ui.geometry.Offset(discCx, discCy))
            }
        }
        EnvironmentType.COZY_LOFT -> {
            LoftSprites.drawLoftBackground(
                scope = scope,
                cw = cw,
                ch = ch,
                p = p,
                timeSeconds = timeSeconds,
                lampLit = engine.lampLit,
                recordSpinning = engine.recordSpinning,
                weather = engine.weather,
                isNight = isNight,
                isSunset = isSunset
            )

            // Loft panoramic window breeze shimmer & leaves
            if (engine.loftWindowTimer > 0f) {
                val dur = 2.0f
                val t = ((dur - engine.loftWindowTimer) / dur).coerceIn(0f, 1f)
                val windowStartX = cw * 0.32f
                val floorY = ch * 0.55f
                val breezePulse = sin(t * Math.PI.toFloat())
                scope.drawRect(
                    Color(0x35FFFFFF).copy(alpha = breezePulse * 0.28f),
                    androidx.compose.ui.geometry.Offset(windowStartX, ch * 0.08f),
                    Size(cw - windowStartX, floorY - ch * 0.08f)
                )
                for (leafIdx in 0..4) {
                    val leafT = ((t * 1.4f + leafIdx * 0.2f) % 1f)
                    val lx = windowStartX + 20 * p + leafT * (cw * 0.55f)
                    val ly = ch * 0.15f + leafIdx * 18 * p + sin(leafT * 6f) * 8 * p
                    scope.drawRect(
                        Color(0xFF74C69D).copy(alpha = (1f - leafT) * breezePulse * 0.85f),
                        androidx.compose.ui.geometry.Offset(lx, ly),
                        Size(3 * p, 2 * p)
                    )
                }
            }
        }
        EnvironmentType.PATH_NIGHT -> {
            drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, timeSeconds, p, weather = engine.weather)
            drawPathGround(scope, cw, ch, p, engine.weather, isWalk = true, timeSeconds = timeSeconds, isNight = isNight, isSunset = isSunset)
            WorldSprites.drawStreetlamp(scope, cw * 0.65f, ch * 0.68f, engine.lampLit && (isNight || isSunset), p)
            WorldSprites.drawMailbox(scope, cw * 0.82f, ch * 0.68f, hasLetter = true, p)

            val curbY = ch * 0.66f + 30f * p
            val curbH = ch - curbY

            // 1. Pagoda Lantern Glow: expanding warm golden radial glow
            if (engine.pagodaGlowTimer > 0f) {
                val dur = 1.8f
                val t = ((dur - engine.pagodaGlowTimer) / dur).coerceIn(0f, 1f)
                val pulse = sin(t * Math.PI.toFloat())
                val pagX = cw * 0.20f
                val pagY = curbY + curbH * 0.38f + 5 * p

                scope.drawCircle(
                    color = Color(0xFFFFAA00).copy(alpha = (pulse * 0.38f).coerceIn(0f, 1f)),
                    radius = (18f + pulse * 14f) * p,
                    center = androidx.compose.ui.geometry.Offset(pagX, pagY)
                )
                scope.drawCircle(
                    color = Color(0xFFFFD166).copy(alpha = (pulse * 0.55f).coerceIn(0f, 1f)),
                    radius = (10f + pulse * 8f) * p,
                    center = androidx.compose.ui.geometry.Offset(pagX, pagY)
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = (pulse * 0.85f).coerceIn(0f, 1f)),
                    radius = (3.5f + pulse * 2f) * p,
                    center = androidx.compose.ui.geometry.Offset(pagX, pagY - 1f * p)
                )
            }

            // 2. Lavender Patch Sway & Scent Waft
            if (engine.lavenderSwayTimer > 0f) {
                val dur = 1.5f
                val t = ((dur - engine.lavenderSwayTimer) / dur).coerceIn(0f, 1f)
                val lavX = cw * 0.80f
                val lavY = curbY + curbH * 0.36f
                val sway = sin(t * Math.PI.toFloat() * 4f) * (1f - t) * 3.5f * p

                val blooms = listOf(-6f to 0f, -3f to -2.5f, 0f to -4f, 3f to -1.5f, 6f to 1.5f)
                for ((sxOff, syOff) in blooms) {
                    val lx = lavX + sxOff * p + sway
                    val ly = lavY + syOff * p
                    scope.drawRect(Color(0xFF9D4EDD), androidx.compose.ui.geometry.Offset(lx - 1.2f * p, ly + 2 * p), Size(3.5f * p, 3.5f * p))
                    scope.drawRect(Color(0xFFC77DFF), androidx.compose.ui.geometry.Offset(lx - 0.6f * p, ly), Size(2.4f * p, 3 * p))
                    scope.drawRect(Color(0xFFE0AAFF), androidx.compose.ui.geometry.Offset(lx, ly - 2 * p), Size(1.8f * p, 2.2f * p))
                }

                val moteOff = t * 24f * p
                val moteFade = (1f - t).coerceIn(0f, 1f)
                scope.drawCircle(Color(0xFFE0AAFF).copy(alpha = moteFade * 0.8f), 1.8f * p, androidx.compose.ui.geometry.Offset(lavX - 4 * p + moteOff * 0.6f, lavY - 6 * p - moteOff))
                scope.drawCircle(Color(0xFFC77DFF).copy(alpha = moteFade * 0.8f), 2.2f * p, androidx.compose.ui.geometry.Offset(lavX + 3 * p + moteOff * 0.8f, lavY - 8 * p - moteOff * 1.2f))
                scope.drawCircle(Color(0xFFD8B4E2).copy(alpha = moteFade * 0.6f), 1.5f * p, androidx.compose.ui.geometry.Offset(lavX + 8 * p + moteOff * 0.5f, lavY - 4 * p - moteOff * 0.9f))
            }

            // 3. Bioluminescent Mushrooms: staggered bounce & aqua glow rings
            if (engine.mushroomBounceTimer > 0f) {
                val dur = 1.4f
                val t = ((dur - engine.mushroomBounceTimer) / dur).coerceIn(0f, 1f)
                val mushBaseY = curbY + curbH * 0.78f
                val mushCenterX = cw * 0.50f
                val mushList = listOf(-8f, 0f, 8f)

                for ((idx, xOff) in mushList.withIndex()) {
                    val mx = mushCenterX + xOff * p
                    val my = mushBaseY + (idx % 2) * 3 * p
                    val capT = ((t - idx * 0.12f) * 2.5f).coerceIn(0f, 1f)
                    val bounce = sin(capT * Math.PI.toFloat()) * 4.5f * p
                    val glowPulse = sin(t * Math.PI.toFloat())

                    scope.drawCircle(
                        color = Color(0xFF48CAE4).copy(alpha = (glowPulse * 0.40f).coerceIn(0f, 1f)),
                        radius = (8f + bounce * 0.8f) * p,
                        center = androidx.compose.ui.geometry.Offset(mx + 2 * p, my - bounce)
                    )

                    scope.drawRect(Color(0xFFEDE0D4), androidx.compose.ui.geometry.Offset(mx, my - bounce), Size(1.8f * p, 4.5f * p))
                    scope.drawRect(Color(0xFF48CAE4), androidx.compose.ui.geometry.Offset(mx - 2 * p, my - 2 * p - bounce), Size(5.8f * p, 2.5f * p))
                    scope.drawRect(Color(0xFF90E0EF), androidx.compose.ui.geometry.Offset(mx - p, my - 2.8f * p - bounce), Size(3.8f * p, p))
                    scope.drawRect(Color.White, androidx.compose.ui.geometry.Offset(mx, my - 2.8f * p - bounce), Size(1.8f * p, 0.8f * p))
                }
            }
        }
        EnvironmentType.MOMO_STALL -> {
            drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, timeSeconds, p, weather = engine.weather)
            drawPathGround(scope, cw, ch, p, engine.weather, isWalk = false, timeSeconds = timeSeconds, isNight = isNight, isSunset = isSunset)
            WorldSprites.drawMomoStall(scope, cw * 0.50f, ch * 0.69f, p, timeSeconds)

            val pathY = ch * 0.66f + 4f * p
            val curbY = pathY + 26f * p
            val curbH = ch - curbY
            val cx = cw * 0.50f
            val groundY = ch * 0.69f
            val counterH = 26 * p
            val counterY = groundY - counterH
            val roofY = counterY - 55 * p
            val awningTop = roofY - 14 * p

            // 1. Neon signboard flicker / cycling glow pulse
            if (engine.momoSignFlickerTimer > 0f) {
                val dur = 1.4f
                val t = ((dur - engine.momoSignFlickerTimer) / dur).coerceIn(0f, 1f)
                val signW = 78 * p
                val signH = 18 * p
                val signX = cx - signW / 2f
                val signY = awningTop - signH - 3 * p
                val neonCycleColor = when (((t * 4f).toInt()) % 4) {
                    0 -> Color(0xFFFFD166)
                    1 -> Color(0xFFFF007F)
                    2 -> Color(0xFF00F5D4)
                    else -> Color(0xFFFFF0F5)
                }
                val pulse = sin(t * Math.PI.toFloat())
                scope.drawRect(neonCycleColor.copy(alpha = (sin(t * 16f) * 0.35f + 0.65f).coerceIn(0f, 1f)), androidx.compose.ui.geometry.Offset(signX - p, signY - p), Size(signW + 2 * p, signH + 2 * p))
                scope.drawRect(neonCycleColor.copy(alpha = pulse * 0.28f), androidx.compose.ui.geometry.Offset(signX - 6 * p, signY - 6 * p), Size(signW + 12 * p, signH + 12 * p))
            }

            // 2. Momo Steamer: Bamboo lid rising & white momo dumplings popping up
            if (engine.momoSteamerTimer > 0f) {
                val dur = 1.5f
                val t = ((dur - engine.momoSteamerTimer) / dur).coerceIn(0f, 1f)
                val steamerW = 18 * p
                val steamerH = 22 * p
                val steamerX = cx - 22 * p
                val steamerY = counterY - steamerH + 2 * p
                val lidLift = sin(t * Math.PI.toFloat()) * 13 * p

                // Lifted domed lid
                scope.drawRect(Color(0xFFADB5BD), androidx.compose.ui.geometry.Offset(steamerX + 2 * p, steamerY + 1 * p - lidLift), Size(steamerW - 4 * p, 3 * p))
                scope.drawRect(Color(0xFFDEE2E6), androidx.compose.ui.geometry.Offset(steamerX + 4 * p, steamerY + 1.5f * p - lidLift), Size(steamerW - 8 * p, 1.5f * p))
                scope.drawRect(Color(0xFF212529), androidx.compose.ui.geometry.Offset(steamerX + steamerW / 2f - 1.5f * p, steamerY - 2 * p - lidLift), Size(3 * p, 3 * p))

                // Fresh hot momo cluster peek
                val momoPop = (lidLift * 0.65f).coerceAtLeast(0f)
                val mxCenter = steamerX + steamerW / 2f
                val myBase = steamerY + 3.5f * p
                scope.drawRect(Color(0xFFFFFDF0), androidx.compose.ui.geometry.Offset(mxCenter - 4.5f * p, myBase - momoPop), Size(9 * p, 4.5f * p))
                scope.drawRect(Color(0xFFFAF0CA), androidx.compose.ui.geometry.Offset(mxCenter - 2 * p, myBase - momoPop - 1.5f * p), Size(4 * p, 2 * p))

                // Billowing steam clouds
                val steamFade = sin(t * Math.PI.toFloat())
                scope.drawCircle(Color.White.copy(alpha = steamFade * 0.45f), 5 * p + t * 4 * p, androidx.compose.ui.geometry.Offset(mxCenter, myBase - lidLift - 4 * p))
                scope.drawCircle(Color.White.copy(alpha = steamFade * 0.32f), 8 * p + t * 6 * p, androidx.compose.ui.geometry.Offset(mxCenter - 2 * p, myBase - lidLift - 9 * p))
            }

            // 3. Fiery Red Spicy Chutney Bowl horizontal wobble & spice sparks
            if (engine.chutneySpiceTimer > 0f) {
                val dur = 1.2f
                val t = ((dur - engine.chutneySpiceTimer) / dur).coerceIn(0f, 1f)
                val bowlX = cx + 27 * p
                val bowlY = counterY - 6 * p
                val bowlW = 9 * p
                val bowlH = 6 * p
                val wobble = sin(t * Math.PI.toFloat() * 10f) * (1f - t) * 3f * p

                scope.drawRect(Color(0xFF2B2D42), androidx.compose.ui.geometry.Offset(bowlX + wobble, bowlY), Size(bowlW, bowlH))
                scope.drawRect(Color(0xFFFFFFFF), androidx.compose.ui.geometry.Offset(bowlX + 1 * p + wobble, bowlY + 1 * p), Size(bowlW - 2 * p, bowlH - 2 * p))
                scope.drawRect(Color(0xFFD90429), androidx.compose.ui.geometry.Offset(bowlX + 1.5f * p + wobble, bowlY + 1.5f * p), Size(bowlW - 3 * p, bowlH - 3 * p))
                scope.drawRect(Color(0xFFFFD166), androidx.compose.ui.geometry.Offset(bowlX + 3 * p + wobble, bowlY + 2.5f * p), Size(1 * p, 1 * p))

                val puffRise = t * 16 * p
                val puffAlpha = (1f - t).coerceIn(0f, 1f)
                scope.drawRect(Color(0xFFE63946).copy(alpha = puffAlpha * 0.85f), androidx.compose.ui.geometry.Offset(bowlX + 3.5f * p + wobble, bowlY - puffRise), Size(2.5f * p, 2.5f * p))
                scope.drawRect(Color(0xFFFFD166).copy(alpha = puffAlpha * 0.85f), androidx.compose.ui.geometry.Offset(bowlX + 2 * p - wobble, bowlY - puffRise * 0.7f), Size(2f * p, 2f * p))
            }

            // 4. A-Frame Chalkboard Menu: board shake & glowing chalk heart doodle
            if (engine.chalkboardTimer > 0f) {
                val dur = 1.6f
                val t = ((dur - engine.chalkboardTimer) / dur).coerceIn(0f, 1f)
                val chkW = 15 * p
                val chkH = 17 * p
                val chkX = cw * 0.28f - chkW / 2f
                val chkY = pathY + 6 * p
                val bWobble = sin(t * Math.PI.toFloat() * 6f) * (1f - t) * 1.5f * p
                val heartAlpha = sin(t * Math.PI.toFloat()).coerceIn(0f, 1f)

                // Chalk heart doodle
                val hx = chkX + chkW / 2f + bWobble
                val hy = chkY + 6.5f * p
                scope.drawRect(Color(0xFFFFCAD4).copy(alpha = heartAlpha), androidx.compose.ui.geometry.Offset(hx - 2 * p, hy), Size(1.8f * p, 1.8f * p))
                scope.drawRect(Color(0xFFFFCAD4).copy(alpha = heartAlpha), androidx.compose.ui.geometry.Offset(hx + 0.2f * p, hy), Size(1.8f * p, 1.8f * p))
                scope.drawRect(Color(0xFFFFCAD4).copy(alpha = heartAlpha), androidx.compose.ui.geometry.Offset(hx - 1.2f * p, hy + 1.6f * p), Size(2.4f * p, 1.8f * p))
                scope.drawRect(Color(0xFFFFCAD4).copy(alpha = heartAlpha), androidx.compose.ui.geometry.Offset(hx - 0.4f * p, hy + 3.2f * p), Size(0.8f * p, 0.8f * p))
            }

            // 5. Bamboo Momo Steamers Crate: top crate sliding sideways
            if (engine.bambooCrateTimer > 0f) {
                val dur = 1.0f
                val t = ((dur - engine.bambooCrateTimer) / dur).coerceIn(0f, 1f)
                val crtX = cw * 0.74f
                val crtY = pathY + 6 * p
                val stmY = crtY - 11 * p
                val stmW = 16 * p
                val stmLeft = crtX - stmW / 2f
                val topSlide = sin(t * Math.PI.toFloat() * 4f) * (1f - t) * 3f * p

                scope.drawRect(Color(0xFFB08968), androidx.compose.ui.geometry.Offset(stmLeft + topSlide, stmY), Size(stmW, 5f * p))
                scope.drawRect(Color(0xFFDDB892), androidx.compose.ui.geometry.Offset(stmLeft + p + topSlide, stmY + 0.5f * p), Size(stmW - 2 * p, 4f * p))
                scope.drawRect(Color(0xFF7F5539), androidx.compose.ui.geometry.Offset(crtX - 1.5f * p + topSlide, stmY - 2 * p), Size(3 * p, 2 * p))

                val stmRise = t * 14 * p
                scope.drawCircle(Color.White.copy(alpha = (1f - t) * 0.5f), 3 * p, androidx.compose.ui.geometry.Offset(crtX + topSlide, stmY - 3 * p - stmRise))
            }

            // 6. Kitty Milk Saucer: expanding milk ripple circles
            if (engine.milkSaucerTimer > 0f) {
                val dur = 1.8f
                val t = ((dur - engine.milkSaucerTimer) / dur).coerceIn(0f, 1f)
                val sauX = cw * 0.38f
                val sauY = pathY + 16 * p

                for (rIdx in 0..2) {
                    val rT = ((t * 1.8f - rIdx * 0.3f)).coerceIn(0f, 1f)
                    if (rT > 0f) {
                        val rRadius = (2f + rT * 6f) * p
                        val rAlpha = (1f - rT) * 0.8f
                        scope.drawCircle(Color.White.copy(alpha = rAlpha), rRadius, androidx.compose.ui.geometry.Offset(sauX, sauY + 1.5f * p))
                    }
                }
            }

            // 7. Outdoor Dining Table: warm kerosene lantern pulse & fragrant steam
            if (engine.streetDiningTableTimer > 0f) {
                val dur = 1.6f
                val t = ((dur - engine.streetDiningTableTimer) / dur).coerceIn(0f, 1f)
                val tblW = 38 * p
                val tblX = cw * 0.50f - tblW / 2f
                val tblY = curbY + curbH * 0.55f
                val lanX = tblX + tblW - 6 * p
                val lanY = tblY - 6.5f * p
                val pulse = sin(t * Math.PI.toFloat())

                scope.drawCircle(Color(0xFFFFAA00).copy(alpha = pulse * 0.35f), (10f + pulse * 10f) * p, androidx.compose.ui.geometry.Offset(lanX + 2.2f * p, lanY + 3.5f * p))
                scope.drawCircle(Color(0xFFFFD166).copy(alpha = pulse * 0.55f), (6f + pulse * 5f) * p, androidx.compose.ui.geometry.Offset(lanX + 2.2f * p, lanY + 3.5f * p))

                val pltX = tblX + 5 * p
                val pltY = tblY - 4 * p
                val pRise = t * 16 * p
                scope.drawCircle(Color.White.copy(alpha = (1f - t) * 0.6f), 3.5f * p, androidx.compose.ui.geometry.Offset(pltX + 5.5f * p, pltY - pRise))
                scope.drawCircle(Color.White.copy(alpha = (1f - t) * 0.4f), 2.5f * p, androidx.compose.ui.geometry.Offset(pltX + 7.5f * p, pltY - pRise * 1.3f))
            }
        }
        EnvironmentType.EVENING_ROAD -> {
            WorldSprites.drawEveningRoadEnvironment(scope, cw, ch, p, timeSeconds, isNight = isNight, isSunset = isSunset, isMorning = isMorning, weather = engine.weather)

            // Golden temple blessing halo pulse
            if (engine.templeGlowTimer > 0f) {
                val dur = 2.0f
                val t = ((dur - engine.templeGlowTimer) / dur).coerceIn(0f, 1f)
                val pulse = sin(t * Math.PI.toFloat())
                val templeBaseY = ch * 0.70f
                val farParallaxSpeed = 22f * p
                val templeLoopW = cw + 360f * p
                fun posMod(v: Float, m: Float): Float = ((v % m) + m) % m
                val t2X = posMod(cw + 280f * p - timeSeconds * farParallaxSpeed, templeLoopW) - 180f * p
                val spireY = templeBaseY - 135 * p

                if (t2X in -50f * p..(cw + 50f * p)) {
                    scope.drawCircle(Color(0xFFFFD166).copy(alpha = pulse * 0.40f), (24f + pulse * 18f) * p, androidx.compose.ui.geometry.Offset(t2X, spireY))
                    scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = pulse * 0.65f), (12f + pulse * 8f) * p, androidx.compose.ui.geometry.Offset(t2X, spireY))
                    scope.drawCircle(Color.White.copy(alpha = pulse * 0.85f), 4 * p, androidx.compose.ui.geometry.Offset(t2X, spireY))
                    for (ray in 0..3) {
                        val rAngle = (ray * 45f) * (Math.PI.toFloat() / 180f)
                        val rLen = (18f + pulse * 16f) * p
                        scope.drawLine(
                            Color(0xFFFFEAA7).copy(alpha = pulse * 0.65f),
                            androidx.compose.ui.geometry.Offset(t2X - cos(rAngle) * rLen, spireY - sin(rAngle) * rLen),
                            androidx.compose.ui.geometry.Offset(t2X + cos(rAngle) * rLen, spireY + sin(rAngle) * rLen),
                            strokeWidth = 2.2f * p
                        )
                    }
                }
            }
        }
    }

    if (isNight && engine.constellationConnectTimer > 0f) {
        drawConstellationOverlay(scope, cw, ch, p, engine.constellationConnectTimer, engine.activeConstellationIndex)
    }
}

private fun drawConstellationOverlay(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    p: Float,
    timer: Float,
    constellationIdx: Int
) {
    if (timer <= 0f || constellationIdx !in 1..3) return
    val dur = 2.4f
    val t = ((dur - timer) / dur).coerceIn(0f, 1f)
    val alpha = sin(t * Math.PI.toFloat()).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return

    val stars = when (constellationIdx) {
        1 -> listOf( // The Two Hearts
            androidx.compose.ui.geometry.Offset(cw * 0.14f, ch * 0.11f),
            androidx.compose.ui.geometry.Offset(cw * 0.17f, ch * 0.08f),
            androidx.compose.ui.geometry.Offset(cw * 0.20f, ch * 0.16f),
            androidx.compose.ui.geometry.Offset(cw * 0.23f, ch * 0.22f),
            androidx.compose.ui.geometry.Offset(cw * 0.11f, ch * 0.18f),
            androidx.compose.ui.geometry.Offset(cw * 0.14f, ch * 0.11f)
        )
        2 -> listOf( // The Celestial Teapot
            androidx.compose.ui.geometry.Offset(cw * 0.36f, ch * 0.09f),
            androidx.compose.ui.geometry.Offset(cw * 0.49f, ch * 0.12f),
            androidx.compose.ui.geometry.Offset(cw * 0.47f, ch * 0.19f),
            androidx.compose.ui.geometry.Offset(cw * 0.40f, ch * 0.20f),
            androidx.compose.ui.geometry.Offset(cw * 0.34f, ch * 0.17f),
            androidx.compose.ui.geometry.Offset(cw * 0.36f, ch * 0.09f)
        )
        3 -> listOf( // Starlight Trail
            androidx.compose.ui.geometry.Offset(cw * 0.62f, ch * 0.10f),
            androidx.compose.ui.geometry.Offset(cw * 0.68f, ch * 0.08f),
            androidx.compose.ui.geometry.Offset(cw * 0.73f, ch * 0.12f),
            androidx.compose.ui.geometry.Offset(cw * 0.82f, ch * 0.11f),
            androidx.compose.ui.geometry.Offset(cw * 0.89f, ch * 0.14f)
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

private fun drawMilkyWayNightSky(
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

    // 2. Dynamically Allocated Star Field (organic distribution across full night sky)
    for (i in dynamicNightStars.indices) {
        val star = dynamicNightStars[i]
        val sx = cw * star.fx
        val sy = ch * star.fy
        val twinkle = sin(time * star.twinkleSpeed + star.twinklePhase) * 0.35f + 0.65f
        val starAlpha = (star.baseAlpha * twinkle).coerceIn(0.15f, 1.0f)
        val sSize = star.sizeP * p
        scope.drawRect(
            color = star.color.copy(alpha = starAlpha),
            topLeft = Offset(sx, sy),
            size = Size(sSize, sSize)
        )
        // Delicate 4-point cross glint only on bright twinkling anchor stars
        if (star.hasCrossFlare && twinkle > 0.86f) {
            val flareCol = star.color.copy(alpha = (twinkle - 0.70f) * 0.75f)
            val flareArm = sSize * 1.5f
            scope.drawRect(flareCol, Offset(sx - flareArm, sy + sSize * 0.35f), Size(flareArm * 2f + sSize, sSize * 0.3f))
            scope.drawRect(flareCol, Offset(sx + sSize * 0.35f, sy - flareArm), Size(sSize * 0.3f, flareArm * 2f + sSize))
        }
    }

    // 3. Glowing Pixel Moon with Minimal Soft Corona
    val moonX = cw * 0.78f
    val moonY = ch * 0.15f
    val moonCenter = Offset(moonX + 7f * p, moonY + 7f * p)
    scope.drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.04f), radius = 20f * p, center = moonCenter)
    scope.drawCircle(Color(0xFFFFF8D6).copy(alpha = 0.08f), radius = 12f * p, center = moonCenter)

    // Crisp Pixel Moon Body
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX, moonY), Size(14 * p, 14 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX - 2 * p, moonY + 2 * p), Size(2 * p, 10 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX + 14 * p, moonY + 2 * p), Size(2 * p, 10 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX + 2 * p, moonY - 2 * p), Size(10 * p, 2 * p))
    scope.drawRect(Color(0xFFFFF3B0), Offset(moonX + 2 * p, moonY + 14 * p), Size(10 * p, 2 * p))
    // Craters
    scope.drawRect(Color(0xFFE9D8A6), Offset(moonX + 3 * p, moonY + 4 * p), Size(3 * p, 3 * p))
    scope.drawRect(Color(0xFFE9D8A6), Offset(moonX + 8 * p, moonY + 7 * p), Size(4 * p, 3 * p))

    // 4. Subtle Occasional Shooting Star (~10.0s cycle)
    val meteorCycle = (time + 3.8f) % 10.0f
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

private fun drawSkyAndClouds(
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

        // ── Drifting fluffy pixel clouds (drawn on top of sky fill) ──────────
        fun drawCloud(baseX: Float, y: Float, scaleFactor: Float) {
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

        drawCloud(cw * 0.08f, ch * 0.10f, 0.7f)
        drawCloud(cw * 0.60f, ch * 0.18f, 1.0f)
        drawCloud(cw * 0.35f, ch * 0.28f, 0.5f)
    }
}

private fun drawMeadowGround(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    isNight: Boolean,
    isSunset: Boolean,
    timeSeconds: Float,
    p: Float,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY
) {
    val groundY = ch * 0.66f
    val totalH = ch - groundY
    val isSnow = weather == com.example.scene.WeatherType.SNOW
    val isSakura = weather == com.example.scene.WeatherType.SAKURA
    val isAutumn = weather == com.example.scene.WeatherType.AUTUMN

    // 1. Layered depth bands — 5 horizontal strips darkening with depth
    val band1 = when {
        isSnow -> Color(0xFFFFFFFF)
        isAutumn -> Color(0xFF9E8D4E)
        isSakura -> Color(0xFF86CF72)
        isNight -> Color(0xFF244D38)
        isSunset -> Color(0xFF6B9E5A)
        else -> Color(0xFF7DC26A)
    }
    val band2 = when {
        isSnow -> Color(0xFFF1F5F9)
        isAutumn -> Color(0xFF8A7A3E)
        isSakura -> Color(0xFF70BD5C)
        isNight -> Color(0xFF1E3F30)
        isSunset -> Color(0xFF588157)
        else -> Color(0xFF68AE55)
    }
    val band3 = when {
        isSnow -> Color(0xFFE2E8F0)
        isAutumn -> Color(0xFF756730)
        isSakura -> Color(0xFF5CA44A)
        isNight -> Color(0xFF172E24)
        isSunset -> Color(0xFF476A44)
        else -> Color(0xFF559245)
    }
    val band4 = when {
        isSnow -> Color(0xFFCBD5E1)
        isAutumn -> Color(0xFF5E5224)
        isSakura -> Color(0xFF49833A)
        isNight -> Color(0xFF102019)
        isSunset -> Color(0xFF395436)
        else -> Color(0xFF437535)
    }
    val soil = when {
        isSnow -> Color(0xFF94A3B8)
        isAutumn -> Color(0xFF423916)
        isSakura -> Color(0xFF325E27)
        isNight -> Color(0xFF0A1510)
        isSunset -> Color(0xFF2C3D22)
        else -> Color(0xFF2F5220)
    }

    val y0 = groundY
    val y1 = groundY + totalH * 0.12f
    val y2 = groundY + totalH * 0.30f
    val y3 = groundY + totalH * 0.50f
    val y4 = groundY + totalH * 0.72f

    scope.drawRect(band1, Offset(0f, y0), Size(cw, y1 - y0))
    scope.drawRect(band2, Offset(0f, y1), Size(cw, y2 - y1))
    scope.drawRect(band3, Offset(0f, y2), Size(cw, y3 - y2))
    scope.drawRect(band4, Offset(0f, y3), Size(cw, y4 - y3))
    scope.drawRect(soil,  Offset(0f, y4), Size(cw, ch - y4))

    // 2. Dithered stepped pixel seams between color bands (Retro 16-bit RPG ground effect)
    fun drawDitherSeam(seamY: Float, topColor: Color, bottomColor: Color) {
        val dStep = 6f * p
        val count = (cw / dStep).toInt() + 1
        for (i in 0..count) {
            val sx = i * dStep
            val stepHeight = if ((i % 3) == 0) 2.5f * p else 1.5f * p
            scope.drawRect(bottomColor, Offset(sx, seamY - stepHeight), Size(2.5f * p, stepHeight))
            if (i % 2 == 1) {
                scope.drawRect(topColor, Offset(sx + 3f * p, seamY), Size(2.5f * p, 2f * p))
            }
        }
    }
    drawDitherSeam(y1, band1, band2)
    drawDitherSeam(y2, band2, band3)
    drawDitherSeam(y3, band3, band4)
    drawDitherSeam(y4, band4, soil)

    // 3. Ground texture — pebbles and soil highlights
    val pebble = when {
        isSnow -> Color(0xFFCBD5E1)
        isAutumn -> Color(0xFF5E5224)
        isNight -> Color(0xFF1A3328)
        isSunset -> Color(0xFF4A6E3A)
        else -> Color(0xFF4A7A38)
    }
    PEBBLE_FRACS.forEachIndexed { i, fx ->
        val py = groundY + (8f + (i % 6) * 9f) * p
        scope.drawRect(pebble, Offset(cw * fx, py), Size(2f * p, 1.5f * p))
        if (i % 4 == 0) scope.drawRect(pebble.copy(alpha = 0.45f), Offset(cw * fx + 5 * p, py + 4 * p), Size(1.5f * p, 1.5f * p))
    }

    // 4. In-ground grass clumps & clover patches at 4 depth tiers across the meadow
    val tuftDark = when {
        isSnow -> Color(0xFFE2E8F0)
        isAutumn -> Color(0xFF756730)
        isNight -> Color(0xFF172E24)
        isSunset -> Color(0xFF476A44)
        else -> Color(0xFF559245)
    }
    val tuftLight = when {
        isSnow -> Color(0xFFFFFFFF)
        isAutumn -> Color(0xFFA6944D)
        isNight -> Color(0xFF2E5E45)
        isSunset -> Color(0xFF80B865)
        else -> Color(0xFF96D678)
    }
    TUFT_ROW_FACTORS.forEachIndexed { rowIdx, rowFactor ->
        val rowY = groundY + totalH * rowFactor
        val tStep = (18f + rowIdx * 6f) * p
        val tCount = (cw / tStep).toInt() + 1
        for (i in 0..tCount) {
            val tx = i * tStep + ((rowIdx * 37) % 23) * p
            val sway = sin(timeSeconds * 3.0f - tx * 0.015f + rowIdx) * p * 1.5f
            scope.drawRect(tuftDark, Offset(tx + sway, rowY - 3f * p), Size(1.2f * p, 3.5f * p))
            scope.drawRect(tuftLight, Offset(tx - 1.5f * p + sway * 0.8f, rowY - 2.5f * p), Size(1.2f * p, 2.5f * p))
            scope.drawRect(tuftLight, Offset(tx + 1.5f * p + sway * 1.2f, rowY - 2f * p), Size(1.2f * p, 2f * p))
        }
    }

    // 5. Atmospheric breeze wind streaks drifting horizontally across the meadow ("air and breeze")
    val breezeCycle = cw + 180f * p
    for (b in 0 until 5) {
        val by = groundY + (6f + b * 18f) * p
        val bSpeed = 80f + b * 25f
        val bx = ((timeSeconds * bSpeed * p + b * 135f * p) % breezeCycle) - 80f * p
        scope.drawRect(Color(0x35FFFFFF), Offset(bx, by), Size(34f * p, 1.5f * p))
        scope.drawRect(Color(0x22FFFFFF), Offset(bx + 6f * p, by - 2.5f * p), Size(22f * p, 1.2f * p))
        scope.drawRect(Color(0x18FFFFFF), Offset(bx + 12f * p, by + 2.5f * p), Size(16f * p, 1f * p))
    }

    // 6. Animated grass blades / snowdrifts along horizon with rolling wind waves
    val bladeLight = when {
        isSnow -> Color(0xFFFFFFFF)
        isAutumn -> Color(0xFFC4B066)
        isNight -> Color(0xFF2E5E45)
        isSunset -> Color(0xFF80B865)
        else -> Color(0xFF96D678)
    }
    val bladeMid = when {
        isSnow -> Color(0xFFF1F5F9)
        isAutumn -> Color(0xFFA6944D)
        isNight -> Color(0xFF244D38)
        isSunset -> Color(0xFF6B9E5A)
        else -> Color(0xFF7DC26A)
    }
    val bladeDark = when {
        isSnow -> Color(0xFFE2E8F0)
        isAutumn -> Color(0xFF8C7B38)
        isNight -> Color(0xFF1A3828)
        isSunset -> Color(0xFF547844)
        else -> Color(0xFF62A055)
    }

    val step1 = 2.2f * p
    val count1 = (cw / step1).toInt() + 2
    for (i in 0..count1) {
        val bx = i * step1
        val heightBase = GRASS_HEIGHT_LOOKUP[i and 63]
        val bladeH = (heightBase * p).coerceIn(2.5f * p, 8f * p)

        val windWave = sin(timeSeconds * 3.4f - bx * 0.015f) * p * 2.6f
        val windFlutter = sin(timeSeconds * 8f + bx * 0.06f) * p * 0.6f
        val sway = if (isSnow) 0f else windWave + windFlutter

        val bladeColor = when (i % 3) { 0 -> bladeLight; 1 -> bladeMid; else -> bladeDark }
        scope.drawRect(bladeColor, Offset(bx + sway, groundY - bladeH), Size(p, bladeH + p))

        if (i % 8 == 0) {
            scope.drawRect(bladeLight, Offset(bx + p * 0.5f + sway * 1.2f, groundY - bladeH - 3f * p), Size(p * 0.8f, 3f * p))
        }
    }

    // 7. Second sparser blade row for deep layering
    val step2 = 4.8f * p
    val count2 = (cw / step2).toInt() + 2
    for (i in 0..count2) {
        val bx = i * step2 + step2 * 0.4f
        val bladeH = GRASS_SPARSE_HEIGHT_LOOKUP[i and 63] * p
        val sway = if (isSnow) 0f else sin(timeSeconds * 2.2f - bx * 0.012f + 1.1f) * p * 1.8f
        scope.drawRect(bladeDark, Offset(bx + sway, groundY - bladeH), Size(p, bladeH))
    }

    // 8. Reflective Rain Puddles with animated ripples
    if (weather == com.example.scene.WeatherType.RAIN) {
        val puddleCoords = listOf(
            Triple(0.24f, 0.74f, 22f),
            Triple(0.53f, 0.79f, 28f),
            Triple(0.79f, 0.73f, 20f)
        )
        for ((pxRel, pyRel, pSize) in puddleCoords) {
            val px = cw * pxRel
            val py = ch * pyRel
            val pw = pSize * p
            val ph = pw * 0.36f
            // Dark puddle depression
            scope.drawOval(
                color = Color(0x35122135),
                topLeft = Offset(px - pw / 2f, py - ph / 2f),
                size = Size(pw, ph)
            )
            // Soft sky water reflection
            scope.drawOval(
                color = Color(0x3060A5FA),
                topLeft = Offset(px - pw * 0.40f, py - ph * 0.35f),
                size = Size(pw * 0.80f, ph * 0.7f)
            )
            // Occasional gentle ripple ring
            val ripT = (timeSeconds * 1.8f + pxRel * 6f) % 2.5f
            if (ripT < 1.2f) {
                val ripFrac = ripT / 1.2f
                val ripW = pw * 0.25f + pw * 0.65f * ripFrac
                val ripH = ripW * 0.36f
                val ripAlpha = (1f - ripFrac) * 0.45f
                scope.drawOval(
                    color = Color.White.copy(alpha = ripAlpha),
                    topLeft = Offset(px - ripW / 2f, py - ripH / 2f),
                    size = Size(ripW, ripH),
                    style = Stroke(width = 1.2f * p)
                )
            }
        }
    }

    // 9. Sweeping cloud shadows during sunny day
    if (weather == com.example.scene.WeatherType.SUNNY && !isNight && !isSunset) {
        val cloudCycle = (timeSeconds * 0.05f) % 1.0f
        val csX = -cw * 0.4f + cloudCycle * (cw * 1.8f)
        val csY = groundY + totalH * 0.20f
        scope.drawOval(
            color = Color(0x150B132B),
            topLeft = Offset(csX, csY),
            size = Size(cw * 0.60f, totalH * 0.60f)
        )
    }

    // 10. Winter Snow Blanket Highlights (Subtle glistening terrain)
    if (isSnow) {
        for (i in 0 until 16) {
            val sx = ((i * 59) % cw.toInt()).toFloat()
            val sy = groundY + ((i * 19) % (totalH.toInt() - 10))
            scope.drawRect(Color.White, Offset(sx, sy), Size(3.5f * p, 2f * p))
        }
    }
}

private fun drawWildFlowers(
    scope: DrawScope,
    cw: Float,
    groundY: Float,
    p: Float,
    timeSeconds: Float = 0f,
    gardenStage: Int = 3,
    wiggleTimer: Float = 0f,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY
) {
    if (weather == com.example.scene.WeatherType.SNOW) {
        // Winter snowdrops and snow tufts over flowers
        for (i in 0 until 12) {
            val fx = cw * (0.08f + i * 0.075f)
            val fy = groundY + (4f + (i % 4) * 5f) * p
            scope.drawRect(Color(0xFF81B29A), Offset(fx, fy - 4 * p), Size(p, 4 * p)) // stem
            scope.drawRect(Color.White, Offset(fx - p, fy - 6 * p), Size(3 * p, 3 * p)) // drooping snowdrop bell
        }
        return
    }

    val flowerTypes = when (weather) {
        com.example.scene.WeatherType.SAKURA -> SAKURA_FLOWERS
        com.example.scene.WeatherType.AUTUMN -> AUTUMN_FLOWERS
        else -> DEFAULT_FLOWERS
    }
    val flowerPositions = FLOWER_POSITIONS

    when (gardenStage) {
        0 -> {
            // Stage 0 (Day 1): Bare garden soil patch with tender baby sprouts
            flowerPositions.take(8).forEachIndexed { _, (xf, yOffsetP) ->
                val fx = cw * xf
                val fy = groundY + yOffsetP * p
                scope.drawRect(Color(0xFF386641), Offset(fx, fy + 2 * p), Size(2.5f * p, 2.5f * p))
                scope.drawRect(Color(0xFF6A994E), Offset(fx - p, fy), Size(1.5f * p, 2 * p))
                scope.drawRect(Color(0xFF6A994E), Offset(fx + 2 * p, fy - p), Size(1.5f * p, 2 * p))
            }
        }
        1 -> {
            // Stage 1 (Day 2): Clover patches & tiny leafy shoots
            flowerPositions.take(10).forEachIndexed { i, (xf, yOffsetP) ->
                val fx = cw * xf
                val fy = groundY + yOffsetP * p
                val sway = sin(timeSeconds * 3f + i) * p * 0.8f
                scope.drawRect(Color(0xFF2D6A4F), Offset(fx + sway, fy), Size(1.2f * p, 4 * p))
                scope.drawRect(Color(0xFF52B788), Offset(fx - 2 * p + sway, fy - 2 * p), Size(2.5f * p, 2.5f * p))
                scope.drawRect(Color(0xFF52B788), Offset(fx + p + sway, fy - 2 * p), Size(2.5f * p, 2.5f * p))
                scope.drawRect(Color(0xFF52B788), Offset(fx - 0.5f * p + sway, fy - 4 * p), Size(2.5f * p, 2.5f * p))
            }
        }
        2 -> {
            // Stage 2 (Day 3-4): Closed flower buds appearing
            flowerPositions.take(12).forEachIndexed { i, (xf, yOffsetP) ->
                val fx = cw * xf
                val fy = groundY + yOffsetP * p
                val (petalColor, _) = flowerTypes[i % flowerTypes.size]
                val windSway = sin(timeSeconds * 3f - fx * 0.015f) * p * 1.5f
                scope.drawRect(Color(0xFF2D6A4F), Offset(fx + windSway * 0.5f, fy), Size(1.5f * p, 6 * p))
                scope.drawRect(Color(0xFF40916C), Offset(fx - 1.5f * p + windSway * 0.3f, fy + 2 * p), Size(2 * p, 1.5f * p))
                // Unopened round flower bud
                scope.drawRect(petalColor, Offset(fx - p + windSway, fy - 2.5f * p), Size(3.5f * p, 3.5f * p))
                scope.drawRect(Color(0xFF2D6A4F), Offset(fx + windSway, fy - 0.5f * p), Size(1.5f * p, 2 * p))
            }
        }
        else -> {
            // Stage 3..6: Full blooming wildflowers + bushes / butterflies / sapling / ladybugs
            val wiggle = if (wiggleTimer > 0f) sin(timeSeconds * 30f) * 4f * p else 0f
            flowerPositions.forEachIndexed { i, (xf, yOffsetP) ->
                val fx = cw * xf
                val fy = groundY + yOffsetP * p
                val (petalColor, centerColor) = flowerTypes[i % flowerTypes.size]
                val windSway = sin(timeSeconds * 3.4f - fx * 0.015f) * p * 2.2f + wiggle

                // Stem
                scope.drawRect(Color(0xFF2D6A4F), Offset(fx + windSway * 0.5f, fy), Size(1.5f * p, 7 * p))
                // Leaf
                scope.drawRect(Color(0xFF40916C), Offset(fx - 2 * p + windSway * 0.3f, fy + 3 * p), Size(2 * p, 2 * p))

                // Flower Blossom
                val bx = fx - 2 * p + windSway
                val by = fy - 4 * p
                scope.drawRect(petalColor, Offset(bx, by), Size(6 * p, 4 * p))
                scope.drawRect(petalColor, Offset(bx + p, by - 2 * p), Size(4 * p, 8 * p))
                scope.drawRect(centerColor, Offset(bx + 2 * p, by + p), Size(2 * p, 2 * p))

                // Stage 6 Ladybug on clover/flower
                if (gardenStage >= 6 && i == 4) {
                    val lbX = bx + 4 * p
                    val lbY = by + 2 * p
                    scope.drawRect(Color(0xFFE63946), Offset(lbX, lbY), Size(2.5f * p, 2.5f * p))
                    scope.drawRect(Color(0xFF000000), Offset(lbX + 0.5f * p, lbY + 0.5f * p), Size(p, p))
                }
            }

            // Stage 4+: Fluttering pixel butterflies!
            if (gardenStage >= 4) {
                // Butterfly 1 (Warm Gold)
                val b1X = cw * 0.32f + sin(timeSeconds * 2.2f) * 24f * p
                val b1Y = groundY - 14f * p + kotlin.math.cos(timeSeconds * 3.5f) * 12f * p
                val flap1 = sin(timeSeconds * 16f) * 2f * p
                scope.drawRect(Color(0xFFFFD166), Offset(b1X - 3 * p, b1Y - flap1), Size(3 * p, 3.5f * p))
                scope.drawRect(Color(0xFFFFD166), Offset(b1X + p, b1Y - flap1), Size(3 * p, 3.5f * p))
                scope.drawRect(Color(0xFF212529), Offset(b1X - 0.5f * p, b1Y), Size(1.5f * p, 4 * p))

                // Butterfly 2 (Sky Azure)
                val b2X = cw * 0.74f + kotlin.math.cos(timeSeconds * 1.8f) * 28f * p
                val b2Y = groundY - 18f * p + sin(timeSeconds * 3.0f) * 14f * p
                val flap2 = kotlin.math.cos(timeSeconds * 18f) * 2f * p
                scope.drawRect(Color(0xFF90E0EF), Offset(b2X - 3 * p, b2Y - flap2), Size(3 * p, 3.5f * p))
                scope.drawRect(Color(0xFF90E0EF), Offset(b2X + p, b2Y - flap2), Size(3 * p, 3.5f * p))
                scope.drawRect(Color(0xFF212529), Offset(b2X - 0.5f * p, b2Y), Size(1.5f * p, 4 * p))
            }

            // Stage 5+: Flowering cherry blossom sapling beside the cottage
            if (gardenStage >= 5) {
                val saplingX = cw * 0.37f
                val saplingY = groundY
                scope.drawRect(Color(0xFF582F0E), Offset(saplingX - 2 * p, saplingY - 26 * p), Size(3.5f * p, 26 * p))
                scope.drawRect(Color(0xFF582F0E), Offset(saplingX - 8 * p, saplingY - 22 * p), Size(8 * p, 2.5f * p))
                scope.drawRect(Color(0xFF582F0E), Offset(saplingX + 2 * p, saplingY - 24 * p), Size(8 * p, 2.5f * p))
                scope.drawRect(Color(0xFFFFCAD4), Offset(saplingX - 14 * p, saplingY - 38 * p), Size(28 * p, 16 * p))
                scope.drawRect(Color(0xFFFF758F), Offset(saplingX - 10 * p, saplingY - 42 * p), Size(20 * p, 8 * p))
                scope.drawRect(Color(0xFFFFF0F3), Offset(saplingX - 6 * p, saplingY - 34 * p), Size(4 * p, 4 * p))
                scope.drawRect(Color(0xFFFFF0F3), Offset(saplingX + 4 * p, saplingY - 30 * p), Size(4 * p, 4 * p))
            }
        }
    }
}

private fun drawKitchenRoom(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    isNight: Boolean,
    p: Float,
    timeSeconds: Float = 0f,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
    fridgeOpen: Boolean = false,
    sinkRunning: Boolean = false,
    isSunset: Boolean = false
) {
    val floorY = ch * 0.65f
    val ceilingY = ch * 0.38f

    // 1. Upper Ceiling Plaster & Exposed Oak Rafters
    val ceilingPlaster = when {
        isNight -> Color(0xFFC7B8A8)
        isSunset -> Color(0xFFE2D4C6)
        else -> Color(0xFFEDE0D4)
    }
    scope.drawRect(ceilingPlaster, Offset(0f, 0f), Size(cw, ceilingY))
    val beamCol = Color(0xFF5C3A21)
    val beamDark = Color(0xFF3E2413)
    // Horizontal main ceiling timber beam
    scope.drawRect(beamCol, Offset(0f, ceilingY - 7 * p), Size(cw, 7 * p))
    scope.drawRect(beamDark, Offset(0f, ceilingY - 1.2f * p), Size(cw, 1.5f * p))
    // Vertical timber rafters in upper ceiling (spaced nicely at 52*p)
    var rafterX = cw * 0.12f
    while (rafterX < cw) {
        scope.drawRect(beamCol.copy(alpha = 0.35f), Offset(rafterX, 0f), Size(4 * p, ceilingY - 7 * p))
        rafterX += 52 * p
    }

    // 2. Upper Wall (warm linen / cream)
    val upperWall = when {
        isNight -> Color(0xFFD3C5B5)
        isSunset -> Color(0xFFF0DFD0)
        else -> Color(0xFFFAF2E9)
    }
    val wainscotTop = floorY - 42 * p
    scope.drawRect(upperWall, Offset(0f, ceilingY), Size(cw, wainscotTop - ceilingY))

    // 3. Lower Wall Wainscoting (calming sage green beadboard)
    val wainscotBg = when {
        isNight -> Color(0xFF5D7162)
        isSunset -> Color(0xFF758572)
        else -> Color(0xFF8A9E8F)
    }
    val wainscotStripe = when {
        isNight -> Color(0xFF526456)
        isSunset -> Color(0xFF6B7B68)
        else -> Color(0xFF7E9283)
    }
    scope.drawRect(wainscotBg, Offset(0f, wainscotTop), Size(cw, 42 * p))
    var bx = 0f
    while (bx < cw) {
        scope.drawRect(wainscotStripe, Offset(bx, wainscotTop), Size(1.2f * p, 42 * p))
        bx += 6 * p
    }

    // Chair rail molding separating upper and lower wall
    val chairRail = Color(0xFF6B4226)
    scope.drawRect(chairRail, Offset(0f, wainscotTop - 2 * p), Size(cw, 2.5f * p))
    scope.drawRect(Color(0xFF4A2810), Offset(0f, wainscotTop + 0.5f * p), Size(cw, p))

    // Baseboard moulding along floor
    scope.drawRect(Color(0xFF6B4226), Offset(0f, floorY - 3 * p), Size(cw, 3 * p))

    // 4. Checked kitchen floor tiles (warm terracotta and creamy biscuit)
    val tile1 = Color(0xFFE6CCB2)
    val tile2 = Color(0xFFC59B76)
    val tileSize = 16 * p
    var row = 0
    var y = floorY
    while (y < ch) {
        var col = 0
        var x = 0f
        while (x < cw) {
            val color = if ((row + col) % 2 == 0) tile1 else tile2
            scope.drawRect(color, Offset(x, y), Size(tileSize, tileSize))
            x += tileSize
            col++
        }
        y += tileSize
        row++
    }

    // 5. Cottage Arch Doorway on Left (leading to living room / hallway)
    val doorX = cw * 0.07f
    val doorW = 20 * p
    val doorH = 64 * p
    val doorLeft = doorX - doorW / 2f
    val doorY = floorY - doorH
    scope.drawRect(Color(0xFF3E2723), Offset(doorLeft, doorY + 7 * p), Size(doorW, doorH - 7 * p))
    scope.drawCircle(Color(0xFF3E2723), doorW / 2f, Offset(doorX, doorY + 7 * p))
    val archInnerW = doorW - 4 * p
    scope.drawRect(Color(0x77FFEAA7), Offset(doorLeft + 2 * p, doorY + 9 * p), Size(archInnerW, doorH - 9 * p))
    scope.drawCircle(Color(0x77FFEAA7), archInnerW / 2f, Offset(doorX, doorY + 9 * p))
    scope.drawRect(chairRail, Offset(doorLeft - 2 * p, doorY + 7 * p), Size(2 * p, doorH - 7 * p))
    scope.drawRect(chairRail, Offset(doorLeft + doorW, doorY + 7 * p), Size(2 * p, doorH - 7 * p))

    // 6. Cottage Kitchen Window (above left prep area, at cw * 0.28f)
    val winW = 34 * p
    val winH = 44 * p
    val winX = cw * 0.28f - winW / 2f
    val winY = floorY - 96 * p
    scope.drawRect(Color(0xFF7F5539), Offset(winX, winY), Size(winW, winH))
    val isSnowOutside = weather == com.example.scene.WeatherType.SNOW
    val isSakuraOutside = weather == com.example.scene.WeatherType.SAKURA
    val isRainOutside = weather == com.example.scene.WeatherType.RAIN
    val isAutumnOutside = weather == com.example.scene.WeatherType.AUTUMN
    val skyBg = when {
        isNight -> Color(0xFF16233B)
        isSunset -> Color(0xFFF49097)
        isSnowOutside -> Color(0xFFD6E8F5)
        isRainOutside -> Color(0xFF8AADBC)
        isAutumnOutside -> Color(0xFFE8C68A)
        else -> Color(0xFFA5D8FF)
    }
    scope.drawRect(skyBg, Offset(winX + 2.5f * p, winY + 2.5f * p), Size(winW - 5 * p, winH - 5 * p))
    if (isNight) {
        scope.drawRect(Color(0xFFFFE66D), Offset(winX + 8 * p, winY + 8 * p), Size(6 * p, 6 * p))
        scope.drawRect(skyBg, Offset(winX + 10 * p, winY + 7 * p), Size(5 * p, 5 * p))
        scope.drawRect(Color.White, Offset(winX + 20 * p, winY + 11 * p), Size(1.5f * p, 1.5f * p))
        scope.drawRect(Color.White, Offset(winX + 16 * p, winY + 21 * p), Size(1.2f * p, 1.2f * p))
    } else if (isSunset) {
        scope.drawCircle(Color(0xFFFFB703), 4 * p, Offset(winX + 10 * p, winY + 12 * p))
        scope.drawRect(Color(0xFFFFDDD2).copy(alpha = 0.8f), Offset(winX + 14 * p, winY + 16 * p), Size(12 * p, 3 * p))
    } else if (isSnowOutside) {
        // Snow: white ground line + snowflakes
        scope.drawRect(Color.White, Offset(winX + 2.5f * p, winY + winH - 7 * p), Size(winW - 5 * p, 5 * p))
        for (i in 0 until 8) {
            val sx = winX + 4 * p + (i * 3.2f) * p
            val sy = winY + 4 * p + (i % 3) * 5 * p
            scope.drawRect(Color.White, Offset(sx, sy), Size(1.2f * p, 1.2f * p))
        }
    } else if (isSakuraOutside) {
        // Sakura: pink blossom petal specks
        scope.drawRect(Color.White, Offset(winX + 7 * p, winY + 12 * p), Size(14 * p, 5 * p))
        for (i in 0 until 7) {
            val px = winX + 3 * p + (i * 4f) * p
            val py = winY + 5 * p + (i % 4) * 4 * p
            scope.drawRect(Color(0xFFFFCAD4), Offset(px, py), Size(1.8f * p, 1.5f * p))
        }
    } else if (isRainOutside) {
        // Rain: grey sky + diagonal rain streaks on glass
        for (i in 0 until 8) {
            val rx = winX + 4 * p + (i * 3.5f) * p
            val ry = winY + 4 * p + (i % 3) * 6 * p
            scope.drawRect(Color(0xAAB0C4CC), Offset(rx, ry), Size(0.8f * p, 4 * p))
        }
    } else if (isAutumnOutside) {
        // Autumn: orange/red leaf specks outside
        scope.drawRect(Color.White, Offset(winX + 7 * p, winY + 12 * p), Size(14 * p, 5 * p))
        for (i in 0 until 7) {
            val ax = winX + 3 * p + (i * 4f) * p
            val ay = winY + 6 * p + (i % 3) * 5 * p
            val ac = if (i % 2 == 0) Color(0xFFE76F51) else Color(0xFFF4A261)
            scope.drawRect(ac, Offset(ax, ay), Size(2f * p, 1.5f * p))
        }
    } else {
        scope.drawRect(Color.White, Offset(winX + 7 * p, winY + 12 * p), Size(14 * p, 5 * p))
        scope.drawRect(Color.White, Offset(winX + 10 * p, winY + 9 * p), Size(8 * p, 4 * p))
        scope.drawRect(Color(0x44FFF3B0), Offset(winX + 20 * p, winY + 5 * p), Size(6 * p, 6 * p))
    }
    // Crossbars
    scope.drawRect(Color(0xFF7F5539), Offset(winX + winW / 2f - 0.75f * p, winY + 2.5f * p), Size(1.5f * p, winH - 5 * p))
    scope.drawRect(Color(0xFF7F5539), Offset(winX + 2.5f * p, winY + winH / 2f - 0.75f * p), Size(winW - 5 * p, 1.5f * p))
    // Gingham Curtains
    val curtainCol1 = Color(0xFFB5E48C)
    val curtainCol2 = Color(0xFFD8F3DC)
    scope.drawRect(curtainCol1, Offset(winX + 2.5f * p, winY + 2.5f * p), Size(5.5f * p, winH - 5 * p))
    scope.drawRect(curtainCol2, Offset(winX + 4.5f * p, winY + 2.5f * p), Size(2.2f * p, winH - 5 * p))
    scope.drawRect(curtainCol1, Offset(winX + winW - 8f * p, winY + 2.5f * p), Size(5.5f * p, winH - 5 * p))
    scope.drawRect(curtainCol2, Offset(winX + winW - 6.7f * p, winY + 2.5f * p), Size(2.2f * p, winH - 5 * p))
    // Sill with potted herb & flower vase
    scope.drawRect(Color(0xFF6B4226), Offset(winX - 2 * p, winY + winH), Size(winW + 4 * p, 3 * p))
    val herbPotW = 5 * p
    val herbPotH = 4.5f * p
    scope.drawRect(Color(0xFFD08C5C), Offset(winX + 4 * p, winY + winH - herbPotH), Size(herbPotW, herbPotH))
    scope.drawRect(Color(0xFF2D6A4F), Offset(winX + 3.5f * p, winY + winH - herbPotH - 4 * p), Size(6 * p, 4 * p))
    scope.drawRect(Color(0xFF52B788), Offset(winX + 4.5f * p, winY + winH - herbPotH - 5 * p), Size(4 * p, 2 * p))
    scope.drawRect(Color(0x99A8DADC), Offset(winX + 19 * p, winY + winH - 5 * p), Size(3.5f * p, 5 * p))
    scope.drawRect(Color(0xFF2D6A4F), Offset(winX + 20f * p, winY + winH - 8 * p), Size(1.2f * p, 3 * p))
    scope.drawRect(Color(0xFFFFD166), Offset(winX + 19f * p, winY + winH - 10 * p), Size(3.2f * p, 2.5f * p))

    // Farmhouse Ceramic Apron Sink under window (Feature 3)
    val sinkW = 26 * p
    val sinkH = 26 * p
    val sinkX = cw * 0.28f - sinkW / 2f
    val sinkY = floorY - sinkH
    // Base cabinet
    scope.drawRect(Color(0xFF6B7F6E), Offset(sinkX, sinkY + 7 * p), Size(sinkW, sinkH - 7 * p))
    scope.drawRect(Color(0xFF556658), Offset(sinkX + 2 * p, sinkY + 9 * p), Size(sinkW - 4 * p, sinkH - 10 * p))
    // Ceramic apron sink basin
    scope.drawRect(Color(0xFFEDE0D4), Offset(sinkX - p, sinkY), Size(sinkW + 2 * p, 8 * p))
    scope.drawRect(Color(0xFFFAF7F2), Offset(sinkX, sinkY + p), Size(sinkW, 6 * p))
    scope.drawRect(Color(0xFFD4C8BC), Offset(sinkX + 2 * p, sinkY + 2 * p), Size(sinkW - 4 * p, 4 * p))
    // Clean fresh water in basin
    scope.drawRect(Color(0x8890E0EF), Offset(sinkX + 3 * p, sinkY + 2.5f * p), Size(sinkW - 6 * p, 3.5f * p))
    // Swan-neck brass faucet
    val faucetX = cw * 0.28f
    val faucetY = sinkY - 6 * p
    scope.drawRect(Color(0xFFD4A373), Offset(faucetX - 0.8f * p, faucetY), Size(1.6f * p, 7 * p))
    scope.drawRect(Color(0xFFD4A373), Offset(faucetX - 4 * p, faucetY), Size(4 * p, 1.6f * p))
    scope.drawRect(Color(0xFFB08968), Offset(faucetX - 4 * p, faucetY + 1.6f * p), Size(1.4f * p, 2 * p))
    // Brass turn handles
    scope.drawRect(Color(0xFFE9C46A), Offset(faucetX - 5 * p, sinkY - 1.5f * p), Size(2 * p, 2 * p))
    scope.drawRect(Color(0xFFE9C46A), Offset(faucetX + 3 * p, sinkY - 1.5f * p), Size(2 * p, 2 * p))

    if (sinkRunning) {
        // Stream of fresh water flowing from faucet to basin
        scope.drawRect(Color(0xCC90E0EF), Offset(faucetX - 3.8f * p, faucetY + 3.6f * p), Size(1.4f * p, sinkY + 2.5f * p - (faucetY + 3.6f * p)))
        // Active ripples in basin
        scope.drawRect(Color(0xFFCAF0F8), Offset(sinkX + 6 * p, sinkY + 3 * p), Size(sinkW - 12 * p, 1.2f * p))
    }

    // 7. Retro Wall Clock (high and proud on center wall, cw * 0.49f)
    val clockX = cw * 0.49f
    val clockY = ceilingY + 16 * p
    val clockR = 7.5f * p
    scope.drawCircle(Color(0xFF6B7F6E), clockR + 1.5f * p, Offset(clockX, clockY))
    scope.drawCircle(Color(0xFFFCF6BD), clockR, Offset(clockX, clockY))
    scope.drawCircle(Color(0xFF333333), 1.2f * p, Offset(clockX, clockY))
    val minuteAngle = (timeSeconds * 0.1f) % (2f * Math.PI.toFloat())
    val hourAngle = (timeSeconds * 0.015f + 1.2f) % (2f * Math.PI.toFloat())
    val mx = (clockX + sin(minuteAngle.toDouble()) * (clockR * 0.65f)).toFloat()
    val my = (clockY - cos(minuteAngle.toDouble()) * (clockR * 0.65f)).toFloat()
    scope.drawLine(
        color = Color(0xFF333333),
        start = Offset(clockX, clockY),
        end = Offset(mx, my),
        strokeWidth = 1.2f * p
    )
    val hx = (clockX + sin(hourAngle.toDouble()) * (clockR * 0.45f)).toFloat()
    val hy = (clockY - cos(hourAngle.toDouble()) * (clockR * 0.45f)).toFloat()
    scope.drawLine(
        color = Color(0xFF333333),
        start = Offset(clockX, clockY),
        end = Offset(hx, hy),
        strokeWidth = 1.8f * p
    )

    // 8. Hanging Dried Herbs & Braided Garlic along the Timber Ceiling Beam
    // Herb 1: Braided Garlic (cw * 0.43f)
    val gX = cw * 0.43f
    scope.drawLine(Color(0xFF8B5E3C), Offset(gX, ceilingY), Offset(gX, ceilingY + 7 * p), strokeWidth = 1.2f * p)
    scope.drawCircle(Color(0xFFEDE0D4), 2.2f * p, Offset(gX, ceilingY + 8 * p))
    scope.drawCircle(Color(0xFFEDE0D4), 2.2f * p, Offset(gX - 1.2f * p, ceilingY + 11.5f * p))
    scope.drawCircle(Color(0xFFEDE0D4), 2.2f * p, Offset(gX + 1.2f * p, ceilingY + 11.5f * p))
    scope.drawCircle(Color(0xFFF3ECE4), 2.2f * p, Offset(gX, ceilingY + 15 * p))

    // Herb 2: Dried Lavender Bunch (cw * 0.58f)
    val lavX = cw * 0.58f
    scope.drawLine(Color(0xFF8B5E3C), Offset(lavX, ceilingY), Offset(lavX, ceilingY + 6 * p), strokeWidth = 1.2f * p)
    scope.drawRect(Color(0xFF2D6A4F), Offset(lavX - 1.5f * p, ceilingY + 6 * p), Size(3 * p, 4 * p))
    scope.drawRect(Color(0xFF9D4EDD), Offset(lavX - 2.5f * p, ceilingY + 10 * p), Size(5 * p, 7 * p))
    scope.drawRect(Color(0xFFC77DFF), Offset(lavX - 1.8f * p, ceilingY + 12 * p), Size(3.6f * p, 6 * p))

    // Herb 3: Dried Rosemary / Sage (cw * 0.68f)
    val herbX = cw * 0.68f
    scope.drawLine(Color(0xFF8B5E3C), Offset(herbX, ceilingY), Offset(herbX, ceilingY + 5 * p), strokeWidth = 1.2f * p)
    scope.drawRect(Color(0xFF40916C), Offset(herbX - 2 * p, ceilingY + 5 * p), Size(4 * p, 9 * p))
    scope.drawRect(Color(0xFF52B788), Offset(herbX - 1.5f * p, ceilingY + 7 * p), Size(3 * p, 8 * p))

    // 9. Overhead Pendant Lamp hanging gracefully from Oak Ceiling Beam
    val lampX = cw * 0.38f
    val lampConeY = floorY - 54 * p
    scope.drawRect(Color(0xFF222222), Offset(lampX - 0.6f * p, ceilingY), Size(1.2f * p, lampConeY - ceilingY))
    scope.drawRect(Color(0xFF2D6A4F), Offset(lampX - 6 * p, lampConeY), Size(12 * p, 3.5f * p))
    scope.drawRect(Color(0xFF1B4332), Offset(lampX - 4 * p, lampConeY - 1.8f * p), Size(8 * p, 1.8f * p))
    scope.drawRect(Color(0xFFFFD166), Offset(lampX - 2.5f * p, lampConeY + 3.5f * p), Size(5 * p, 1.8f * p))
    val glowAlpha = if (isNight) 0.32f else 0.15f
    val glowColor = Color(0xFFFFEAA7).copy(alpha = glowAlpha)
    for (step in 0..5) {
        val gy = lampConeY + 5 * p + step * 6 * p
        val gw = 16 * p + step * 8 * p
        scope.drawRect(glowColor.copy(alpha = glowAlpha * (1f - step * 0.14f)), Offset(lampX - gw / 2f, gy), Size(gw, 6 * p))
    }
    // Warm floor light pool under pendant lamp
    scope.drawOval(
        color = Color(0xFFFFD166).copy(alpha = if (isNight) 0.22f else 0.12f),
        topLeft = Offset(lampX - 26 * p, floorY - 4 * p),
        size = Size(52 * p, 8 * p)
    )

    // 9. Retro 1950s Pastel Refrigerator (Right wall, cw * 0.88f)
    val fridgeX = cw * 0.88f
    val fridgeW = 20 * p
    val fridgeH = 64 * p
    val fridgeY = floorY - fridgeH
    val fridgeMint = Color(0xFFA7C4B5)
    val fridgeDark = Color(0xFF8BA899)
    val fridgeHighlight = Color(0xFFC3DBD0)
    if (fridgeOpen) {
        // Open illuminated fridge interior cavity
        val innerLeft = fridgeX - fridgeW / 2f + 2 * p
        val innerW = fridgeW - 4 * p
        val innerTop = fridgeY + 2 * p
        val innerH = fridgeH - 4 * p
        scope.drawRect(Color(0xFFFFFBEB), Offset(innerLeft, innerTop), Size(innerW, innerH))
        // Warm bright interior light glow
        scope.drawRect(Color(0x55FFEAA7), Offset(innerLeft - 6 * p, innerTop), Size(innerW + 12 * p, innerH + 2 * p))
        // Shelves
        scope.drawRect(Color(0xFFB0C4DE), Offset(innerLeft, innerTop + 16 * p), Size(innerW, 1.2f * p))
        scope.drawRect(Color(0xFFB0C4DE), Offset(innerLeft, innerTop + 32 * p), Size(innerW, 1.2f * p))
        scope.drawRect(Color(0xFFB0C4DE), Offset(innerLeft, innerTop + 46 * p), Size(innerW, 1.2f * p))
        // Items on shelves
        scope.drawRect(Color(0xFFE63946), Offset(innerLeft + 2 * p, innerTop + 7 * p), Size(3 * p, 8 * p))
        scope.drawRect(Color(0xFF457B9D), Offset(innerLeft + 7 * p, innerTop + 5 * p), Size(3.5f * p, 10 * p))
        scope.drawRect(Color(0xFFFF758F), Offset(innerLeft + 3 * p, innerTop + 24 * p), Size(4 * p, 7 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(innerLeft + 9 * p, innerTop + 25 * p), Size(3.5f * p, 6 * p))
        scope.drawRect(Color(0xFF52B788), Offset(innerLeft + 3 * p, innerTop + 38 * p), Size(8 * p, 7 * p))
        // Door swung open to the right
        scope.drawRect(fridgeMint, Offset(fridgeX + fridgeW / 2f, fridgeY), Size(5 * p, fridgeH))
        scope.drawRect(Color(0xFFE0E0E0), Offset(fridgeX + fridgeW / 2f + 3 * p, fridgeY + 22 * p), Size(1.5f * p, 8 * p))
    } else {
        scope.drawRect(fridgeMint, Offset(fridgeX - fridgeW / 2f, fridgeY), Size(fridgeW, fridgeH))
        scope.drawRect(fridgeHighlight, Offset(fridgeX - fridgeW / 2f + 1 * p, fridgeY + 1 * p), Size(2 * p, fridgeH - 2 * p))
        scope.drawRect(fridgeDark, Offset(fridgeX + fridgeW / 2f - 2 * p, fridgeY), Size(2 * p, fridgeH))
        scope.drawRect(fridgeDark, Offset(fridgeX - fridgeW / 2f, fridgeY + 19 * p), Size(fridgeW, 1.5f * p))
        scope.drawRect(Color(0xFFE0E0E0), Offset(fridgeX - fridgeW / 2f + 2.5f * p, fridgeY + 7 * p), Size(1.8f * p, 6 * p))
        scope.drawRect(Color(0xFFE0E0E0), Offset(fridgeX - fridgeW / 2f + 2.5f * p, fridgeY + 24 * p), Size(1.8f * p, 10 * p))
        // Fridge magnets and photo
        scope.drawRect(Color(0xFFFF4D6D), Offset(fridgeX - 2 * p, fridgeY + 28 * p), Size(3 * p, 3 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(fridgeX + 3 * p, fridgeY + 34 * p), Size(2.5f * p, 2.5f * p))
        scope.drawRect(Color.White, Offset(fridgeX - 3.5f * p, fridgeY + 40 * p), Size(7 * p, 8 * p))
        scope.drawRect(Color(0xFFFFCAD4), Offset(fridgeX - 2.5f * p, fridgeY + 41 * p), Size(5 * p, 5 * p))
    }
    // Fruit basket on top
    val basketY = fridgeY - 6 * p
    scope.drawRect(Color(0xFFB08968), Offset(fridgeX - 6 * p, basketY + 2 * p), Size(12 * p, 4 * p))
    scope.drawRect(Color(0xFFE63946), Offset(fridgeX - 4 * p, basketY), Size(3.5f * p, 3.5f * p))
    scope.drawRect(Color(0xFFF77F00), Offset(fridgeX - 0.5f * p, basketY), Size(3.5f * p, 3.5f * p))
    scope.drawRect(Color(0xFFE9C46A), Offset(fridgeX + 2.5f * p, basketY + 0.5f * p), Size(3 * p, 3 * p))

    // 10. Cozy Woven Kitchen Runner Rug on the floor (where couple stands!)
    val rugX1 = cw * 0.14f
    val rugX2 = cw * 0.52f
    val rugW = rugX2 - rugX1
    val rugY = floorY + 3 * p
    val rugH = 16 * p
    val rugBase = Color(0xFFC86D51)
    val rugPattern = Color(0xFFF5EBE0)
    val rugBorder = Color(0xFF6B7F6E)
    scope.drawRect(rugBase, Offset(rugX1, rugY), Size(rugW, rugH))
    scope.drawRect(rugBorder, Offset(rugX1, rugY), Size(rugW, 1.5f * p))
    scope.drawRect(rugBorder, Offset(rugX1, rugY + rugH - 1.5f * p), Size(rugW, 1.5f * p))
    var rx = rugX1 + 5 * p
    while (rx < rugX2 - 5 * p) {
        scope.drawRect(rugPattern, Offset(rx, rugY + 6 * p), Size(2.5f * p, 3.5f * p))
        rx += 12 * p
    }
    scope.drawRect(Color(0xFFF5EBE0), Offset(rugX1 - 1.5f * p, rugY + 1 * p), Size(1.5f * p, rugH - 2 * p))
    scope.drawRect(Color(0xFFF5EBE0), Offset(rugX2, rugY + 1 * p), Size(1.5f * p, rugH - 2 * p))

    // 11. Pet Mochi's Dining Station (near fridge)
    val bowlX = cw * 0.74f
    val bowlY = floorY + 5 * p
    scope.drawRect(Color(0xFFDDB892), Offset(bowlX - 7 * p, bowlY), Size(15 * p, 6 * p))
    scope.drawRect(Color(0xFFF28482), Offset(bowlX - 5.5f * p, bowlY + 0.8f * p), Size(5.5f * p, 4f * p))
    scope.drawRect(Color(0xFF7F5539), Offset(bowlX - 4.5f * p, bowlY + 1.5f * p), Size(3.8f * p, 1.8f * p))
    scope.drawRect(Color(0xFF84A59D), Offset(bowlX + 1.2f * p, bowlY + 0.8f * p), Size(5.5f * p, 4f * p))
    scope.drawRect(Color(0xFF48CAE4), Offset(bowlX + 2f * p, bowlY + 1.5f * p), Size(3.8f * p, 2.2f * p))
    scope.drawRect(Color.White, Offset(bowlX + 3.2f * p, bowlY + 1.8f * p), Size(1.2f * p, 1f * p))

    // 12. Cozy Couple House Slippers (neatly tucked at runner rug edge)
    val slipX = cw * 0.38f
    val slipY = floorY + 20 * p
    // Girl's soft pink slipper
    scope.drawRect(Color(0xFFB56576), Offset(slipX, slipY + 4 * p), Size(6 * p, 2 * p))
    scope.drawRect(Color(0xFFFFB5C2), Offset(slipX, slipY), Size(6 * p, 4 * p))
    scope.drawRect(Color(0xFFFFF0F3), Offset(slipX + 1.5f * p, slipY), Size(3 * p, 2 * p))
    // Boy's navy blue slipper
    scope.drawRect(Color(0xFF1D2D44), Offset(slipX + 8.5f * p, slipY + 4 * p), Size(6 * p, 2 * p))
    scope.drawRect(Color(0xFF2E4668), Offset(slipX + 8.5f * p, slipY), Size(6 * p, 4 * p))
    scope.drawRect(Color(0xFF8D99AE), Offset(slipX + 10f * p, slipY), Size(3 * p, 2 * p))

    // --- Ground-level Kitchen Zones (Organized Dining Suite & Perimeter Nooks) ---
    val floorH = ch - floorY

    // 13. Central Farmhouse Dining Ensemble (grounded on warm accent rug)
    val juteW = 88 * p
    val juteH = 34 * p
    val juteX = cw * 0.50f - juteW / 2f
    val juteY = floorY + floorH * 0.44f
    // Terracotta outer border
    scope.drawRect(Color(0xFFC86D51), Offset(juteX, juteY), Size(juteW, juteH))
    // Sage green middle ring
    scope.drawRect(Color(0xFF6B7F6E), Offset(juteX + 5 * p, juteY + 3.5f * p), Size(juteW - 10 * p, juteH - 7 * p))
    // Oatmeal cream woven interior
    scope.drawRect(Color(0xFFF5EBE0), Offset(juteX + 11 * p, juteY + 7.5f * p), Size(juteW - 22 * p, juteH - 15 * p))
    // Braided fringe on ends
    scope.drawRect(Color(0xFFD4A373), Offset(juteX - 3 * p, juteY + 3 * p), Size(3 * p, juteH - 6 * p))
    scope.drawRect(Color(0xFFD4A373), Offset(juteX + juteW, juteY + 3 * p), Size(3 * p, juteH - 6 * p))

    // Farmhouse Wooden Dining Table (centered on rug)
    val tblW = 52 * p
    val tblH = 18 * p
    val tblX = cw * 0.50f - tblW / 2f
    val tblY = juteY + 7 * p
    // Sturdy wooden legs with crossbar
    scope.drawRect(Color(0xFF43281C), Offset(tblX + 4 * p, tblY + 4 * p), Size(3 * p, tblH - 4 * p))
    scope.drawRect(Color(0xFF43281C), Offset(tblX + tblW - 7 * p, tblY + 4 * p), Size(3 * p, tblH - 4 * p))
    scope.drawRect(Color(0xFF43281C), Offset(tblX + 4 * p, tblY + tblH * 0.65f), Size(tblW - 8 * p, 2 * p))
    // Solid oak tabletop with bevel
    scope.drawRect(Color(0xFF582F0E), Offset(tblX, tblY + 3.5f * p), Size(tblW, 2 * p))
    scope.drawRect(Color(0xFF7F4F24), Offset(tblX, tblY), Size(tblW, 4 * p))
    scope.drawRect(Color(0xFF9C6644), Offset(tblX + p, tblY + 0.5f * p), Size(tblW - 2 * p, 1.2f * p))
    // Gingham table runner
    val runW = 22 * p
    val runX = cw * 0.50f - runW / 2f
    scope.drawRect(Color(0xFFC86D51), Offset(runX, tblY), Size(runW, 3.8f * p))
    scope.drawRect(Color(0xFFF5EBE0), Offset(runX + 2 * p, tblY), Size(3 * p, 3.8f * p))
    scope.drawRect(Color(0xFFF5EBE0), Offset(runX + 9 * p, tblY), Size(3 * p, 3.8f * p))
    scope.drawRect(Color(0xFFF5EBE0), Offset(runX + 16 * p, tblY), Size(3 * p, 3.8f * p))
    // White ceramic vase with blooming sunflowers in center
    val vaseX = cw * 0.50f
    val vaseY = tblY - 5.5f * p
    scope.drawRect(Color(0xFFF8F9FA), Offset(vaseX - 2.5f * p, vaseY + 2 * p), Size(5 * p, 4 * p))
    scope.drawRect(Color(0xFF2D6A4F), Offset(vaseX - 0.6f * p, vaseY - 2 * p), Size(1.2f * p, 4 * p))
    scope.drawRect(Color(0xFFFFD166), Offset(vaseX - 3.5f * p, vaseY - 4.5f * p), Size(7 * p, 3.5f * p))
    scope.drawRect(Color(0xFF7F4F24), Offset(vaseX - p, vaseY - 3.5f * p), Size(2 * p, 2 * p))
    // Fruit bowl with red apples & lemon
    val fBowlX = tblX + 8 * p
    val fBowlY = tblY - 4 * p
    scope.drawRect(Color(0xFFD4A373), Offset(fBowlX - 4 * p, fBowlY + 2 * p), Size(8 * p, 2.5f * p))
    scope.drawRect(Color(0xFFE63946), Offset(fBowlX - 3 * p, fBowlY), Size(3 * p, 3 * p))
    scope.drawRect(Color(0xFFFCBF49), Offset(fBowlX, fBowlY + 0.5f * p), Size(2.8f * p, 2.5f * p))
    // Steaming tea cups
    val cupX = tblX + tblW - 10 * p
    val cupY = tblY - 4.5f * p
    scope.drawRect(Color(0xFF457B9D), Offset(cupX, cupY), Size(3.5f * p, 4.5f * p))
    scope.drawRect(Color(0xFFFFB5C2), Offset(cupX + 4.5f * p, cupY), Size(3.5f * p, 4.5f * p))
    if (sin(timeSeconds * 3f) > 0.2f) {
        scope.drawRect(Color(0x77FFFFFF), Offset(cupX + p, cupY - 3 * p), Size(1.2f * p, 2.2f * p))
        scope.drawRect(Color(0x77FFFFFF), Offset(cupX + 5.5f * p, cupY - 3 * p), Size(1.2f * p, 2.2f * p))
    }

    // Matching Dining Chairs (tucked neatly beside table)
    fun drawDiningChair(chairX: Float, facingRight: Boolean) {
        val chY = tblY - 4 * p
        val chW = 10 * p
        val chH = 22 * p
        val cl = chairX - chW / 2f
        // Chair backrest spindles
        scope.drawRect(Color(0xFF5C3A21), Offset(cl + (if (facingRight) 0f else chW - 2.5f * p), chY), Size(2.5f * p, chH))
        scope.drawRect(Color(0xFF7F4F24), Offset(cl, chY), Size(chW, 2.5f * p))
        // Upholstered seat
        scope.drawRect(Color(0xFF8A9E8F), Offset(cl, chY + 11 * p), Size(chW, 2.5f * p))
        // Legs
        scope.drawRect(Color(0xFF5C3A21), Offset(cl + p, chY + 13.5f * p), Size(2 * p, 8.5f * p))
        scope.drawRect(Color(0xFF5C3A21), Offset(cl + chW - 3 * p, chY + 13.5f * p), Size(2 * p, 8.5f * p))
    }
    drawDiningChair(tblX - 7 * p, facingRight = true)
    drawDiningChair(tblX + tblW + 7 * p, facingRight = false)

    // Vintage Step Stool tucked neatly beside left chair on rug
    val stlX = tblX - 16 * p
    val stlY = juteY + 16 * p
    val stlW = 14 * p
    val stlH = 10 * p
    val sLeft = stlX - stlW / 2f
    scope.drawRect(Color(0xFF5C3A21), Offset(sLeft + 2 * p, stlY + 2.5f * p), Size(2 * p, stlH - 2.5f * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(sLeft + stlW - 4 * p, stlY + 2.5f * p), Size(2 * p, stlH - 2.5f * p))
    scope.drawRect(Color(0xFF7F4F24), Offset(sLeft, stlY), Size(stlW, 3 * p))
    scope.drawRect(Color(0xFF935A28), Offset(sLeft + p, stlY + 0.5f * p), Size(stlW - 2 * p, p))
    // Folded sage green tea towel on stool
    scope.drawRect(Color(0xFFB5E48C), Offset(sLeft + 3.5f * p, stlY - 1.8f * p), Size(7 * p, 2.5f * p))

    // 14. Left Pantry Corner: Produce Crate & Burlap Potato Sack (neatly by left door corner)
    val crateX = cw * 0.13f
    val crateY = floorY + floorH * 0.74f
    val crateW = 24 * p
    val crateH = 17 * p
    val cLeft = crateX - crateW / 2f
    scope.drawRect(Color(0xFF3E2413), Offset(cLeft, crateY), Size(crateW, crateH))
    // Pumpkin in crate
    scope.drawRect(Color(0xFFF77F00), Offset(cLeft + 2.5f * p, crateY - 4 * p), Size(10 * p, 8 * p))
    scope.drawRect(Color(0xFF2D6A4F), Offset(cLeft + 6.5f * p, crateY - 6.5f * p), Size(2 * p, 3 * p))
    // Carrots with green tops
    scope.drawRect(Color(0xFFE76F51), Offset(cLeft + 14 * p, crateY - 3 * p), Size(4.5f * p, 8 * p))
    scope.drawRect(Color(0xFF52B788), Offset(cLeft + 15 * p, crateY - 7 * p), Size(4 * p, 5 * p))
    // Crate slats
    scope.drawRect(Color(0xFF8B5E3C), Offset(cLeft, crateY), Size(crateW, 3.5f * p))
    scope.drawRect(Color(0xFF8B5E3C), Offset(cLeft, crateY + 5.5f * p), Size(crateW, 3.5f * p))
    scope.drawRect(Color(0xFF8B5E3C), Offset(cLeft, crateY + 11 * p), Size(crateW, 3.5f * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(cLeft - p, crateY - p), Size(2.5f * p, crateH + p))
    scope.drawRect(Color(0xFF5C3A21), Offset(cLeft + crateW - 1.5f * p, crateY - p), Size(2.5f * p, crateH + p))
    // Burlap potato sack beside crate
    val sackX = cLeft + crateW + 2 * p
    val sackY = crateY + 2 * p
    scope.drawRect(Color(0xFFD4A373), Offset(sackX, sackY), Size(12 * p, 15 * p))
    scope.drawRect(Color(0xFFB08968), Offset(sackX + 1.5f * p, sackY + 1.5f * p), Size(9 * p, 11 * p))
    scope.drawRect(Color(0xFF7F5539), Offset(sackX + 3.5f * p, sackY - 2 * p), Size(5 * p, 2.5f * p))

    // 15. Right Corner: Glazed Ceramic Floor Planter with Split Monstera (anchors right corner)
    val plantX = cw * 0.86f
    val plantY = floorY + floorH * 0.74f
    val floorPotW = 18 * p
    val floorPotH = 14 * p
    val pLeft = plantX - floorPotW / 2f
    // Tripod wooden stand
    scope.drawRect(Color(0xFF5C3A21), Offset(pLeft + 2 * p, plantY + floorPotH - 2 * p), Size(2.5f * p, 7 * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(pLeft + floorPotW - 4.5f * p, plantY + floorPotH - 2 * p), Size(2.5f * p, 7 * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(pLeft + 2 * p, plantY + floorPotH + 1.5f * p), Size(floorPotW - 4 * p, 2 * p))
    // Ceramic pot body
    scope.drawRect(Color(0xFFEDE0D4), Offset(pLeft, plantY), Size(floorPotW, floorPotH))
    scope.drawRect(Color(0xFFD4A373), Offset(pLeft - p, plantY), Size(floorPotW + 2 * p, 2.5f * p))
    scope.drawRect(Color(0xFFE6CCB2), Offset(pLeft + 2 * p, plantY + 3.5f * p), Size(floorPotW - 4 * p, floorPotH - 5 * p))
    // Lush Monstera foliage
    scope.drawRect(Color(0xFF1B4332), Offset(plantX - 11 * p, plantY - 16 * p), Size(22 * p, 16 * p))
    scope.drawRect(Color(0xFF2D6A4F), Offset(plantX - 14 * p, plantY - 13 * p), Size(12 * p, 12 * p))
    scope.drawRect(Color(0xFF2D6A4F), Offset(plantX + 2 * p, plantY - 17 * p), Size(12 * p, 13 * p))
    scope.drawRect(Color(0xFF52B788), Offset(plantX - 7 * p, plantY - 19 * p), Size(14 * p, 7 * p))
    scope.drawRect(Color(0xFF74C69D), Offset(plantX - 3 * p, plantY - 20 * p), Size(7 * p, 3.5f * p))
}

private fun drawLivingRoom(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    isNight: Boolean,
    p: Float,
    lampLit: Boolean = true,
    couchPhase: CouchPhase = CouchPhase.NIGHT,
    timeSeconds: Float = 0f
) {
    val floorY = ch * 0.65f
    val wallColor = when {
        lampLit -> Color(0xFFF6EDE2) // Warm illuminated cream
        couchPhase == CouchPhase.NIGHT -> Color(0xFF78798C) // Dim cozy midnight slate
        couchPhase == CouchPhase.DAY -> Color(0xFFF4ECE1) // Morning daylight cream
        couchPhase == CouchPhase.MIDDAY -> Color(0xFFFFF9EE) // Bright noon sunlight
        couchPhase == CouchPhase.EVENING -> Color(0xFFE5B5A2) // Warm sunset peach
        else -> if (isNight) Color(0xFFBCB1A6) else Color(0xFFF6EDE2)
    }
    scope.drawRect(wallColor, Offset.Zero, Size(cw, floorY))

    // Baseboard
    val baseboardColor = if (!lampLit && couchPhase == CouchPhase.NIGHT) Color(0xFF5A3A25) else Color(0xFF7F5539)
    scope.drawRect(baseboardColor, Offset(0f, floorY - 3 * p), Size(cw, 3 * p))

    // Wooden plank floor
    val (wood1, wood2) = when {
        lampLit -> Pair(Color(0xFFDDA15E), Color(0xFFBC6C25))
        couchPhase == CouchPhase.NIGHT -> Pair(Color(0xFF8B5E3C), Color(0xFF6F4324))
        couchPhase == CouchPhase.EVENING -> Pair(Color(0xFFC7844E), Color(0xFFA55E28))
        else -> Pair(Color(0xFFDDA15E), Color(0xFFBC6C25))
    }
    scope.drawRect(wood1, Offset(0f, floorY), Size(cw, ch - floorY))
    var plankY = floorY
    var plankIndex = 0
    while (plankY < ch) {
        scope.drawRect(wood2, Offset(0f, plankY), Size(cw, 1.2f * p))
        if (plankIndex % 2 == 0) {
            scope.drawRect(wood2.copy(alpha = 0.45f), Offset(cw * 0.22f, plankY + 2 * p), Size(1.2f * p, 1.2f * p))
            scope.drawRect(wood2.copy(alpha = 0.45f), Offset(cw * 0.68f, plankY + 2 * p), Size(1.2f * p, 1.2f * p))
        } else {
            scope.drawRect(wood2.copy(alpha = 0.45f), Offset(cw * 0.42f, plankY + 2 * p), Size(1.2f * p, 1.2f * p))
            scope.drawRect(wood2.copy(alpha = 0.45f), Offset(cw * 0.88f, plankY + 2 * p), Size(1.2f * p, 1.2f * p))
        }
        plankY += 8 * p
        plankIndex++
    }

    // 1. Large Woven Living Room Area Rug (anchors both couch and coffee table)
    val rugColor = if (!lampLit && couchPhase == CouchPhase.NIGHT) Color(0xFF9E4E3C) else Color(0xFFE07A5F)
    val rugBorder = if (!lampLit && couchPhase == CouchPhase.NIGHT) Color(0xFF5A7D6C) else Color(0xFF81B29A)
    val rugCx = cw * 0.50f
    val rugW = 88 * p
    val rugH = 46 * p
    val rugY = floorY + 4 * p
    scope.drawRect(rugBorder, Offset(rugCx - rugW / 2f, rugY), Size(rugW, rugH))
    scope.drawRect(rugColor, Offset(rugCx - rugW / 2f + 2.5f * p, rugY + 2.5f * p), Size(rugW - 5 * p, rugH - 5 * p))
    // Subtle woven diamond pattern across rug center
    val diaCol = rugBorder.copy(alpha = 0.45f)
    for (i in -2..2) {
        val dx = rugCx + i * 16 * p
        scope.drawRect(diaCol, Offset(dx - 3 * p, rugY + rugH * 0.58f), Size(6 * p, 4 * p))
    }

    // Interior connecting door to Kitchen / Outdoor
    drawCottageInteriorDoor(scope, cw * 0.08f, floorY, p)

    // --- Ground-level Living Room Zones (Unified Seating Group & Corner Nooks) ---
    val floorH = ch - floorY

    // 2. Coffee Table placed directly on the rug in front of the couch
    val tblW = 48 * p
    val tblH = 14 * p
    val tblX = cw * 0.50f - tblW / 2f
    val tblY = floorY + 23 * p
    // Sturdy wooden tapered legs
    scope.drawRect(Color(0xFF43281C), Offset(tblX + 3.5f * p, tblY + 3.5f * p), Size(2.5f * p, tblH - 3.5f * p))
    scope.drawRect(Color(0xFF43281C), Offset(tblX + tblW - 6 * p, tblY + 3.5f * p), Size(2.5f * p, tblH - 3.5f * p))
    // Table slab with beveled edge and underside shadow
    scope.drawRect(Color(0xFF582F0E), Offset(tblX, tblY + 3f * p), Size(tblW, 2 * p))
    scope.drawRect(Color(0xFF7F4F24), Offset(tblX, tblY), Size(tblW, 3.5f * p))
    scope.drawRect(Color(0xFF9C6644), Offset(tblX + p, tblY + 0.5f * p), Size(tblW - 2 * p, 1.2f * p))
    // Open novel on table
    val bookX = tblX + 4 * p
    val bookY = tblY - 4 * p
    scope.drawRect(Color(0xFF2B2D42), Offset(bookX, bookY), Size(12 * p, 4 * p))
    scope.drawRect(Color(0xFFFAF0CA), Offset(bookX + 0.8f * p, bookY + 0.5f * p), Size(10.4f * p, 3 * p))
    scope.drawRect(Color(0xFFB0A990), Offset(bookX + 5.5f * p, bookY + 0.5f * p), Size(0.8f * p, 3 * p))
    scope.drawRect(Color(0xFFE63946), Offset(bookX + 5.5f * p, bookY + 2f * p), Size(0.8f * p, 2.5f * p))
    // Two ceramic cocoa mugs with marshmallows
    val mug1X = tblX + 20 * p
    val mug2X = tblX + 27 * p
    val mugY = tblY - 4.5f * p
    // Boy's slate blue mug
    scope.drawRect(Color(0xFF457B9D), Offset(mug1X, mugY), Size(4 * p, 4.5f * p))
    scope.drawRect(Color(0xFF457B9D), Offset(mug1X - 1.2f * p, mugY + p), Size(1.2f * p, 2.5f * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(mug1X + 0.8f * p, mugY + 0.6f * p), Size(2.4f * p, 1.2f * p))
    scope.drawRect(Color.White, Offset(mug1X + p, mugY + 0.8f * p), Size(p, 0.8f * p))
    // Girl's soft blush mug
    scope.drawRect(Color(0xFFFFB5C2), Offset(mug2X, mugY), Size(4 * p, 4.5f * p))
    scope.drawRect(Color(0xFFFFB5C2), Offset(mug2X + 4 * p, mugY + p), Size(1.2f * p, 2.5f * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(mug2X + 0.8f * p, mugY + 0.6f * p), Size(2.4f * p, 1.2f * p))
    scope.drawRect(Color.White, Offset(mug2X + 1.8f * p, mugY + 0.8f * p), Size(p, 0.8f * p))
    // Steam from mugs
    if (sin(timeSeconds * 3f) > 0.2f) {
        scope.drawRect(Color(0x77FFFFFF), Offset(mug1X + 1.5f * p, mugY - 3 * p), Size(1.2f * p, 2 * p))
        scope.drawRect(Color(0x77FFFFFF), Offset(mug2X + 1.5f * p, mugY - 3 * p), Size(1.2f * p, 2 * p))
    }
    // Glowing scented candle
    val cndX = tblX + 37 * p
    val cndY = tblY - 5f * p
    scope.drawRect(Color(0xFFD4A373), Offset(cndX, cndY + 2 * p), Size(4.5f * p, 3.5f * p))
    scope.drawRect(Color(0xFFFFD166), Offset(cndX + 0.8f * p, cndY + 2.5f * p), Size(2.8f * p, 2.5f * p))
    if (lampLit || couchPhase != CouchPhase.NIGHT) {
        val flk = sin(timeSeconds * 5f) * 0.4f * p
        scope.drawRect(Color(0xFFFFB703), Offset(cndX + 1.5f * p + flk * 0.3f, cndY), Size(1.5f * p, 2.2f * p))
        scope.drawRect(Color.White, Offset(cndX + 1.8f * p, cndY + 0.6f * p), Size(0.8f * p, p))
    }

    // 3. Knitted Round Floor Pouf / Ottoman (at right edge of rug, beside table)
    val pfX = cw * 0.70f
    val pfY = floorY + 25 * p
    val pfW = 18 * p
    val pfH = 12 * p
    val pfLeft = pfX - pfW / 2f
    scope.drawRect(Color(0xFF526456), Offset(pfLeft, pfY + 2 * p), Size(pfW, pfH - 2 * p))
    scope.drawRect(Color(0xFF6B7F6E), Offset(pfLeft + p, pfY), Size(pfW - 2 * p, pfH - 2 * p))
    scope.drawRect(Color(0xFF8A9E8F), Offset(pfLeft + 2.5f * p, pfY + p), Size(pfW - 5 * p, 2.5f * p))
    scope.drawRect(Color(0xFF526456), Offset(pfLeft + 4.5f * p, pfY + 2 * p), Size(1.2f * p, pfH - 4 * p))
    scope.drawRect(Color(0xFF526456), Offset(pfLeft + 9 * p, pfY + 2 * p), Size(1.2f * p, pfH - 4 * p))
    scope.drawRect(Color(0xFF526456), Offset(pfLeft + 13.5f * p, pfY + 2 * p), Size(1.2f * p, pfH - 4 * p))

    // 4. Couple Slippers (neatly resting at left couch/rug edge)
    val slipX = cw * 0.30f
    val slipY = floorY + 12 * p
    // Girl's blush slipper
    scope.drawRect(Color(0xFFB56576), Offset(slipX, slipY + 4 * p), Size(6 * p, 2 * p))
    scope.drawRect(Color(0xFFFFB5C2), Offset(slipX, slipY), Size(6 * p, 4 * p))
    scope.drawRect(Color(0xFFFFF0F3), Offset(slipX + 1.5f * p, slipY), Size(3 * p, 2 * p))
    // Boy's navy slipper
    scope.drawRect(Color(0xFF1D2D44), Offset(slipX + 8.5f * p, slipY + 4 * p), Size(6 * p, 2 * p))
    scope.drawRect(Color(0xFF2E4668), Offset(slipX + 8.5f * p, slipY), Size(6 * p, 4 * p))
    scope.drawRect(Color(0xFF8D99AE), Offset(slipX + 10f * p, slipY), Size(3 * p, 2 * p))

    // --- FOREGROUND CORNER NOOKS (Center Walkway Remains Clean) ---

    // 5. Bottom-Left Corner: Mochi's Pet Lounge & Knitting Corner
    val cBoxX = cw * 0.17f
    val cBoxY = floorY + floorH * 0.75f
    val cBoxW = 20 * p
    val cBoxH = 13 * p
    val cbLeft = cBoxX - cBoxW / 2f
    scope.drawRect(Color(0xFF6F523B), Offset(cbLeft, cBoxY), Size(cBoxW, cBoxH))
    scope.drawRect(Color(0xFFC59B76), Offset(cbLeft, cBoxY + 3 * p), Size(cBoxW, cBoxH - 3 * p))
    scope.drawRect(Color(0xFFA98467), Offset(cbLeft, cBoxY + 3 * p), Size(cBoxW, 1.2f * p))
    // Flaps angled up
    scope.drawRect(Color(0xFFC59B76), Offset(cbLeft - p, cBoxY), Size(5.5f * p, 4 * p))
    scope.drawRect(Color(0xFFC59B76), Offset(cbLeft + cBoxW - 4.5f * p, cBoxY), Size(5.5f * p, 4 * p))
    // Cozy yellow pet cushion inside box
    scope.drawRect(Color(0xFFFFD166), Offset(cbLeft + 3 * p, cBoxY + 1.5f * p), Size(14 * p, 4.5f * p))
    scope.drawRect(Color(0xFFE9C46A), Offset(cbLeft + 4 * p, cBoxY + 3.5f * p), Size(12 * p, 2.5f * p))
    // Stamped paw print on front
    val pawX = cBoxX
    val pawY = cBoxY + 7.5f * p
    scope.drawRect(Color(0xFF7F5539), Offset(pawX - 1.5f * p, pawY), Size(3 * p, 2.5f * p))
    scope.drawRect(Color(0xFF7F5539), Offset(pawX - 2.5f * p, pawY - 1.5f * p), Size(1.2f * p, 1.2f * p))
    scope.drawRect(Color(0xFF7F5539), Offset(pawX - 0.6f * p, pawY - 2 * p), Size(1.2f * p, 1.2f * p))
    scope.drawRect(Color(0xFF7F5539), Offset(pawX + 1.3f * p, pawY - 1.5f * p), Size(1.2f * p, 1.2f * p))

    // Beside Mochi's Box: Woven Wicker Blanket Basket with knitting needles
    val bskX = cw * 0.30f
    val bskY = floorY + floorH * 0.75f
    val bskW = 18 * p
    val bskH = 15 * p
    val bLeft = bskX - bskW / 2f
    scope.drawRect(Color(0xFFB08968), Offset(bLeft, bskY), Size(bskW, bskH))
    scope.drawRect(Color(0xFFD4A373), Offset(bLeft + 2 * p, bskY + 2 * p), Size(bskW - 4 * p, bskH - 4 * p))
    scope.drawRect(Color(0xFF7F5539), Offset(bLeft + 5 * p, bskY + 3 * p), Size(2 * p, bskH - 6 * p))
    scope.drawRect(Color(0xFF7F5539), Offset(bLeft + 11 * p, bskY + 3 * p), Size(2 * p, bskH - 6 * p))
    // Soft cream fleece blanket draped over rim
    scope.drawRect(Color(0xFFF8EDEB), Offset(bLeft + p, bskY - 2 * p), Size(9 * p, 7 * p))
    scope.drawRect(Color(0xFFE8D8D8), Offset(bLeft + 2 * p, bskY + 3 * p), Size(7 * p, 3 * p))
    // Yarn balls with knitting needles
    scope.drawRect(Color(0xFFB8B8D1), Offset(bLeft + 9 * p, bskY - 3 * p), Size(5 * p, 5 * p))
    scope.drawRect(Color(0xFF95D5B2), Offset(bLeft + 13 * p, bskY - 2 * p), Size(4.5f * p, 4.5f * p))
    scope.drawRect(Color(0xFFDDB892), Offset(bLeft + 11 * p, bskY - 7 * p), Size(1.2f * p, 6 * p))
    scope.drawRect(Color(0xFFDDB892), Offset(bLeft + 14 * p, bskY - 6 * p), Size(1.2f * p, 6 * p))

    // 6. Bottom-Right Corner: Wooden Magazine & Record Rack + Small Plant
    val rackX = cw * 0.80f
    val rackY = floorY + floorH * 0.74f
    val rackW = 26 * p
    val rackH = 18 * p
    val rLeft = rackX - rackW / 2f
    // Wooden rack structure (warm teak / walnut)
    scope.drawRect(Color(0xFF45240F), Offset(rLeft, rackY), Size(rackW, rackH))
    scope.drawRect(Color(0xFF7F4F24), Offset(rLeft + p, rackY + p), Size(rackW - 2 * p, rackH - 2 * p))
    scope.drawRect(Color(0xFF582F0E), Offset(rLeft, rackY + 9 * p), Size(rackW, 2 * p))
    // Back tier: Vinyl record jackets
    scope.drawRect(Color(0xFF1D3557), Offset(rLeft + 3 * p, rackY - 5 * p), Size(6 * p, 13 * p)) // navy vinyl
    scope.drawRect(Color(0xFFE76F51), Offset(rLeft + 10 * p, rackY - 6 * p), Size(7 * p, 14 * p)) // terracotta album
    scope.drawRect(Color(0xFF2D6A4F), Offset(rLeft + 18 * p, rackY - 4 * p), Size(5.5f * p, 12 * p)) // sage album
    // Front tier: Magazines with colorful spines
    scope.drawRect(Color(0xFFFAF0CA), Offset(rLeft + 2 * p, rackY + 5 * p), Size(6 * p, 11 * p))
    scope.drawRect(Color(0xFFFFB5C2), Offset(rLeft + 9 * p, rackY + 4 * p), Size(7 * p, 12 * p))
    scope.drawRect(Color(0xFF81B29A), Offset(rLeft + 17 * p, rackY + 5 * p), Size(6 * p, 11 * p))
    // Front retaining wooden bar
    scope.drawRect(Color(0xFF582F0E), Offset(rLeft - p, rackY + 13 * p), Size(rackW + 2 * p, 3.5f * p))
    scope.drawRect(Color(0xFF9C6644), Offset(rLeft, rackY + 13.5f * p), Size(rackW, 1.2f * p))
}


private fun drawCottageInteriorDoor(
    scope: DrawScope,
    doorX: Float,
    floorY: Float,
    p: Float
) {
    val doorW = 20 * p
    val doorH = 46 * p
    val dx = doorX - doorW / 2f
    val dy = floorY - doorH

    // Wooden door frame
    scope.drawRect(Color(0xFF582F0E), Offset(dx - 2 * p, dy - 2 * p), Size(doorW + 4 * p, doorH + 2 * p))
    scope.drawRect(Color(0xFF7F4F24), Offset(dx, dy), Size(doorW, doorH))
    // Recessed panels
    scope.drawRect(Color(0xFF582F0E), Offset(dx + 3 * p, dy + 4 * p), Size(doorW - 6 * p, 16 * p))
    scope.drawRect(Color(0xFF582F0E), Offset(dx + 3 * p, dy + 24 * p), Size(doorW - 6 * p, 18 * p))
    // Brass doorknob
    scope.drawRect(Color(0xFFFFD166), Offset(dx + doorW - 5 * p, dy + 24 * p), Size(2.5f * p, 2.5f * p))
}

private fun drawPathGround(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    p: Float,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
    isWalk: Boolean = false,
    timeSeconds: Float = 0f,
    isNight: Boolean = true,
    isSunset: Boolean = false
) {
    val groundY = ch * 0.66f
    val isSnow = weather == com.example.scene.WeatherType.SNOW
    val isSakura = weather == com.example.scene.WeatherType.SAKURA
    val isAutumn = weather == com.example.scene.WeatherType.AUTUMN

    // Ground grass with depth and time-of-day
    val grassTop = when {
        isSnow -> Color(0xFFFFFFFF)
        isAutumn -> if (isNight) Color(0xFF38321B) else if (isSunset) Color(0xFF6B5C28) else Color(0xFF8A7A3E)
        isSakura -> if (isNight) Color(0xFF1E3828) else if (isSunset) Color(0xFF5A8550) else Color(0xFF70BD5C)
        isNight -> Color(0xFF142B21)
        isSunset -> Color(0xFF4A7540)
        else -> Color(0xFF5CA44A) // daytime lush green
    }
    val grassBottom = when {
        isSnow -> Color(0xFFDCE6EE)
        isAutumn -> if (isNight) Color(0xFF282312) else if (isSunset) Color(0xFF4D4018) else Color(0xFF6B5C28)
        isSakura -> if (isNight) Color(0xFF152A1E) else if (isSunset) Color(0xFF44663C) else Color(0xFF5CA44A)
        isNight -> Color(0xFF0E1E17)
        isSunset -> Color(0xFF385730)
        else -> Color(0xFF478238)
    }
    scope.drawRect(grassTop, Offset(0f, groundY), Size(cw, ch - groundY))
    scope.drawRect(grassBottom, Offset(0f, groundY + 16 * p), Size(cw, ch - groundY - 16 * p))

    // Dense grass blades / snowdrifts along the horizon edge
    val bladeCol = when {
        isSnow -> Color(0xFFFFFFFF)
        isAutumn -> if (isNight) Color(0xFF7D7242) else Color(0xFF9E8D4E)
        isNight -> Color(0xFF1E3F30)
        isSunset -> Color(0xFF588157)
        else -> Color(0xFF68AE55)
    }
    val bladeColLight = when {
        isSnow -> Color(0xFFE8F1F5)
        isAutumn -> if (isNight) Color(0xFF9E9055) else Color(0xFFB5A768)
        isNight -> Color(0xFF284F3A)
        isSunset -> Color(0xFF6B9E5A)
        else -> Color(0xFF7DC26A)
    }
    val nStep = 2.5f * p
    val nCount = (cw / nStep).toInt() + 2
    for (i in 0..nCount) {
        val bx = i * nStep
        val bladeH = (2f + abs(sin(bx * 0.15f + 0.5f)) * 2.2f) * p
        scope.drawRect(
            if (i % 2 == 0) bladeCol else bladeColLight,
            Offset(bx, groundY - bladeH),
            Size(p, bladeH + p)
        )
    }

    // Cobblestone walking path
    val pathY = groundY + 4 * p
    val pathH = 26 * p
    val pathBaseColor = when {
        isSnow -> Color(0xFFB0C4DE)
        isNight -> Color(0xFF3D405B)
        isSunset -> Color(0xFF635666)
        else -> Color(0xFF7D828A)
    }
    scope.drawRect(pathBaseColor, Offset(0f, pathY), Size(cw, pathH))
    val stoneColor = when {
        isSnow -> Color(0xFFD4E2EE)
        isNight -> Color(0xFF555979)
        isSunset -> Color(0xFF7A6B80)
        else -> Color(0xFF9EA3AB)
    }
    val stoneHighlight = when {
        isSnow -> Color(0xFFFFFFFF)
        isNight -> Color(0xFF6B7096)
        isSunset -> Color(0xFF96869C)
        else -> Color(0xFFBFC4CD)
    }
    var sx = 0f
    while (sx < cw) {
        scope.drawRect(stoneColor, Offset(sx, pathY + 2 * p), Size(9 * p, 6 * p))
        scope.drawRect(stoneHighlight, Offset(sx, pathY + 2 * p), Size(9 * p, 1 * p))
        scope.drawRect(stoneColor, Offset(sx + 5 * p, pathY + 10 * p), Size(8 * p, 6 * p))
        scope.drawRect(stoneHighlight, Offset(sx + 5 * p, pathY + 10 * p), Size(8 * p, 1 * p))
        scope.drawRect(stoneColor, Offset(sx + 2 * p, pathY + 18 * p), Size(10 * p, 5 * p))
        scope.drawRect(stoneHighlight, Offset(sx + 2 * p, pathY + 18 * p), Size(10 * p, 1 * p))
        sx += 15 * p
    }

    if (isSnow) {
        // Snow accumulation along path edges and stones
        scope.drawRect(Color.White, Offset(0f, pathY - p), Size(cw, 2.5f * p))
        scope.drawRect(Color.White, Offset(0f, pathY + pathH - p), Size(cw, 2.5f * p))
        for (i in 0 until 16) {
            val px = ((i * 53) % cw.toInt()).toFloat()
            val py = pathY + ((i * 7) % (pathH.toInt() - 6))
            scope.drawRect(Color.White, Offset(px, py), Size(3 * p, 2 * p))
        }
    }

    // --- Ground-level Curb & Roadside Embankment Terraces ---
    val curbY = pathY + pathH

    // 1. Cobblestone Curb Rim with 3D drop shadow
    val curbTop = if (isSnow) Color(0xFFD4E2EE) else Color(0xFF6B7096)
    val curbFace = if (isSnow) Color(0xFF8E9AAF) else Color(0xFF2B2D42)
    scope.drawRect(curbTop, Offset(0f, curbY), Size(cw, 2.5f * p))
    scope.drawRect(curbFace, Offset(0f, curbY + 2.5f * p), Size(cw, 3f * p))
    // Vertical curb stone joints spaced every 18 * p
    var cx = 0f
    while (cx < cw) {
        scope.drawRect(Color(0xFF1B1D28), Offset(cx, curbY), Size(1.2f * p, 5.5f * p))
        cx += 18 * p
    }

    val curbH = ch - curbY

    // 2. Stepped Roadside Embankment Terraces (proportional full ground depth)
    val t1Y = curbY + 5.5f * p
    val t1H = curbH * 0.28f
    val t2Y = curbY + curbH * 0.28f
    val t2H = curbH * 0.32f
    val t3Y = curbY + curbH * 0.60f
    val t3H = ch - t3Y
    val t1Col = if (isSnow) Color(0xFFE8F1F5) else if (isAutumn) Color(0xFF4A4228) else Color(0xFF1B4332)
    val t2Col = if (isSnow) Color(0xFFDCE6EE) else if (isAutumn) Color(0xFF38321B) else Color(0xFF143023)
    val t3Col = if (isSnow) Color(0xFFCADBE8) else if (isAutumn) Color(0xFF282312) else Color(0xFF0E1E17)
    scope.drawRect(t1Col, Offset(0f, t1Y), Size(cw, t1H))
    scope.drawRect(t2Col, Offset(0f, t2Y), Size(cw, t2H))
    scope.drawRect(t3Col, Offset(0f, t3Y), Size(cw, t3H))

    // Dither stepped retro RPG seams between terraces
    fun drawTerraceDither(seamY: Float, topCol: Color, botCol: Color) {
        val dStep = 6f * p
        val count = (cw / dStep).toInt() + 1
        for (i in 0..count) {
            val sx = i * dStep
            val stepH = if ((i % 3) == 0) 2.5f * p else 1.5f * p
            scope.drawRect(botCol, Offset(sx, seamY - stepH), Size(2.5f * p, stepH))
            if (i % 2 == 1) {
                scope.drawRect(topCol, Offset(sx + 3f * p, seamY), Size(2.5f * p, 2f * p))
            }
        }
    }
    drawTerraceDither(t2Y, t1Col, t2Col)
    drawTerraceDither(t3Y, t2Col, t3Col)

    // Smooth river stones / garden pebbles scattered along terrace steps
    val pebColor = if (isSnow) Color(0xFFB0C4DE) else Color(0xFF495057)
    val pebHighlight = if (isSnow) Color.White else Color(0xFF6C757D)
    for (idx in RIVER_PEB_POSITIONS.indices) {
        val px = cw * RIVER_PEB_POSITIONS[idx]
        val py = t1Y + 3 * p + (idx % 3) * 4 * p
        scope.drawRect(pebColor, Offset(px, py), Size(4 * p, 2.5f * p))
        scope.drawRect(pebHighlight, Offset(px + 0.8f * p, py), Size(2.4f * p, 0.8f * p))
    }

    // Lower terrace large flat stepping stones
    for (i in RIVER_STEP_STONES_X.indices) {
        val sx = cw * RIVER_STEP_STONES_X[i]
        val sy = curbY + curbH * RIVER_STEP_STONES_Y[i]
        scope.drawRect(pebColor, Offset(sx - 5 * p, sy), Size(10 * p, 4.5f * p))
        scope.drawRect(pebHighlight, Offset(sx - 4 * p, sy), Size(8 * p, 1.2f * p))
        scope.drawRect(Color(0xFF212529), Offset(sx - 5 * p, sy + 3.5f * p), Size(10 * p, p))
    }

    // Clover patches & grass tufts along embankment
    val clovCol = if (isSnow) Color(0xFFBDE0FE) else if (isAutumn) Color(0xFF9E9055) else Color(0xFF2D6A4F)
    for (i in RIVER_CLOV_POSITIONS.indices) {
        val cxPos = cw * RIVER_CLOV_POSITIONS[i]
        val cyPos = t2Y + 2 * p
        scope.drawRect(clovCol, Offset(cxPos, cyPos), Size(3 * p, 3 * p))
        scope.drawRect(clovCol, Offset(cxPos + 2.5f * p, cyPos - p), Size(2.5f * p, 2.5f * p))
    }

    // 3. Scene-specific Foreground Props
    if (isWalk) {
        // --- WALK / PATH_NIGHT Scene: Painterly Natural Landscape Clusters ---

        // Cluster A: Left Riverside Pagoda Lantern & Reeds (cw * 0.20f)
        val pagX = cw * 0.20f
        val pagY = curbY + curbH * 0.38f
        // Earthy moss rock footing
        scope.drawRect(Color(0xFF2D6A4F), Offset(pagX - 10 * p, pagY + 13 * p), Size(20 * p, 4 * p))
        scope.drawRect(Color(0xFF495057), Offset(pagX - 8 * p, pagY + 11 * p), Size(16 * p, 4.5f * p))
        scope.drawRect(Color(0xFF343A40), Offset(pagX - 6.5f * p, pagY + 8.5f * p), Size(13 * p, 2.5f * p))
        // Stone light chamber
        scope.drawRect(Color(0xFF212529), Offset(pagX - 5.5f * p, pagY + 2 * p), Size(11 * p, 6.5f * p))
        // Warm glowing candle window inside
        scope.drawRect(Color(0xFFFFD166), Offset(pagX - 3.5f * p, pagY + 3 * p), Size(7 * p, 4.5f * p))
        scope.drawRect(Color.White, Offset(pagX - 1.8f * p, pagY + 4 * p), Size(3.6f * p, 2.5f * p))
        // Tiered pagoda roof with flared upturned eaves
        scope.drawRect(Color(0xFF343A40), Offset(pagX - 9 * p, pagY - 2.5f * p), Size(18 * p, 4.5f * p))
        scope.drawRect(Color(0xFF495057), Offset(pagX - 6 * p, pagY - 5 * p), Size(12 * p, 2.5f * p))
        scope.drawRect(Color(0xFF6C757D), Offset(pagX - 2 * p, pagY - 7 * p), Size(4 * p, 2f * p))
        // Soft warm ambient glow
        val pagFlick = sin(timeSeconds * 4f) * 0.08f + 0.92f
        scope.drawCircle(Color(0x35FFAA00).copy(alpha = 0.22f * pagFlick), 18 * p, Offset(pagX, pagY + 5 * p))
        // Reeds and wild grasses nestling the lantern base
        for (i in RIVER_REED_OFFSETS.indices) {
            val rx = RIVER_REED_OFFSETS[i]
            val rH = (6f + abs(rx) * 0.5f) * p
            scope.drawRect(Color(0xFF1B4332), Offset(pagX + rx * p, pagY + 14 * p - rH), Size(1.2f * p, rH))
            scope.drawRect(Color(0xFF52B788), Offset(pagX + rx * p - 0.5f * p, pagY + 14 * p - rH), Size(2f * p, 2f * p))
        }

        // Cluster B: Right Wild Night Lavender Bank & Fireflies (cw * 0.80f)
        val lavX = cw * 0.80f
        val lavY = curbY + curbH * 0.36f
        // Stalks and purple flower heads
        for (i in RIVER_STALKS_X.indices) {
            val lx = lavX + RIVER_STALKS_X[i] * p
            val ly = lavY + RIVER_STALKS_Y[i] * p
            scope.drawRect(Color(0xFF1B4332), Offset(lx, ly + 4 * p), Size(1.2f * p, 9 * p))
            scope.drawRect(Color(0xFF7209B7), Offset(lx - 1.2f * p, ly + 2 * p), Size(3.5f * p, 3.5f * p))
            scope.drawRect(Color(0xFF9D4EDD), Offset(lx - 0.6f * p, ly), Size(2.4f * p, 3 * p))
            scope.drawRect(Color(0xFFC77DFF), Offset(lx, ly - 2 * p), Size(1.8f * p, 2.2f * p))
        }
        // Floating glowing fireflies over lavender
        val ff1Alpha = (sin(timeSeconds * 3f) * 0.4f + 0.6f).coerceIn(0.2f, 1f)
        val ff2Alpha = (cos(timeSeconds * 2.5f + 1f) * 0.4f + 0.6f).coerceIn(0.2f, 1f)
        scope.drawRect(Color(0xFFE0AAFF).copy(alpha = ff1Alpha), Offset(lavX - 4 * p + sin(timeSeconds * 2f) * 2.5f * p, lavY - 6 * p), Size(2.2f * p, 2.2f * p))
        scope.drawRect(Color(0xFFE0AAFF).copy(alpha = ff2Alpha), Offset(lavX + 5 * p + cos(timeSeconds * 2f) * 2.5f * p, lavY - 8 * p), Size(2.2f * p, 2.2f * p))

        // Cluster C: Lower River Shoreline & Bioluminescent Night Mushrooms (Cluster in bottom terrace)
        val mushBaseY = curbY + curbH * 0.78f
        val mushCenterX = cw * 0.50f
        for (idx in RIVER_MUSH_OFFSETS.indices) {
            val mx = mushCenterX + RIVER_MUSH_OFFSETS[idx] * p
            val my = mushBaseY + (idx % 2) * 3 * p
            val mPulse = (sin(timeSeconds * 3.5f + idx * 2f) * 0.25f + 0.75f).coerceIn(0.5f, 1f)
            scope.drawRect(Color(0xFFEDE0D4), Offset(mx, my), Size(1.8f * p, 4.5f * p))
            scope.drawRect(Color(0xFF48CAE4).copy(alpha = mPulse), Offset(mx - 2 * p, my - 2 * p), Size(5.8f * p, 2.5f * p))
            scope.drawRect(Color(0xFF90E0EF).copy(alpha = mPulse), Offset(mx - p, my - 2.8f * p), Size(3.8f * p, p))
            scope.drawCircle(Color(0x3048CAE4).copy(alpha = 0.20f * mPulse), 7 * p, Offset(mx + 2 * p, my))
        }
    } else {
        // --- MOMO_STALL Scene: Authentic Night Market Layout ---

        // ZONE 1: Sidewalk Ordering & Prep (Upper Sidewalk Level beside the Cart)

        // 1. Classic Wooden A-Frame Chalkboard Menu (Left of stall, cw * 0.28f, pathY + 6 * p)
        val chkW = 15 * p
        val chkH = 17 * p
        val chkX = cw * 0.28f - chkW / 2f
        val chkY = pathY + 6 * p
        // Wooden A-frame easel legs
        scope.drawRect(Color(0xFF582F0E), Offset(chkX, chkY), Size(2 * p, chkH))
        scope.drawRect(Color(0xFF582F0E), Offset(chkX + chkW - 2 * p, chkY), Size(2 * p, chkH))
        scope.drawRect(Color(0xFF582F0E), Offset(chkX, chkY + chkH * 0.75f), Size(chkW, 1.5f * p))
        // Chalkboard surface
        scope.drawRect(Color(0xFF264653), Offset(chkX + 2 * p, chkY + 2 * p), Size(chkW - 4 * p, chkH - 5 * p))
        // Chalk letters "MOMO" & dumpling doodle
        scope.drawRect(Color.White, Offset(chkX + 3.5f * p, chkY + 3.5f * p), Size(8 * p, 1.2f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(chkX + 4.5f * p, chkY + 6.5f * p), Size(6 * p, 3.2f * p))

        // 2. Bamboo Momo Steamers Delivery Crate (Right of stall, cw * 0.74f, pathY + 6 * p)
        val crtX = cw * 0.74f
        val crtY = pathY + 6 * p
        val crtW = 20 * p
        val crtH = 15 * p
        val crtLeft = crtX - crtW / 2f
        scope.drawRect(Color(0xFF5C3A21), Offset(crtLeft, crtY), Size(crtW, crtH))
        scope.drawRect(Color(0xFF8B5E3C), Offset(crtLeft + p, crtY + p), Size(crtW - 2 * p, crtH - 2 * p))
        scope.drawRect(Color(0xFF5C3A21), Offset(crtLeft, crtY + 6 * p), Size(crtW, 1.5f * p))
        // Stack of round bamboo steamers on crate
        val stmY = crtY - 11 * p
        val stmW = 16 * p
        val stmLeft = crtX - stmW / 2f
        scope.drawRect(Color(0xFFB08968), Offset(stmLeft, stmY + 5 * p), Size(stmW, 5.5f * p))
        scope.drawRect(Color(0xFFDDB892), Offset(stmLeft + p, stmY + 5.5f * p), Size(stmW - 2 * p, 4.5f * p))
        scope.drawRect(Color(0xFFB08968), Offset(stmLeft, stmY), Size(stmW, 5f * p))
        scope.drawRect(Color(0xFFDDB892), Offset(stmLeft + p, stmY + 0.5f * p), Size(stmW - 2 * p, 4f * p))
        scope.drawRect(Color(0xFF7F5539), Offset(crtX - 1.5f * p, stmY - 2 * p), Size(3 * p, 2 * p))
        if (sin(timeSeconds * 2.8f) > 0.25f) {
            scope.drawRect(Color(0x77FFFFFF), Offset(crtX - 1.5f * p, stmY - 5 * p), Size(1.5f * p, 3 * p))
        }

        // 3. Kitty Milk Saucer (tucked beside cart base on sidewalk, cw * 0.38f)
        val sauX = cw * 0.38f
        val sauY = pathY + 16 * p
        scope.drawRect(Color(0xFF0096C7), Offset(sauX - 6 * p, sauY), Size(12 * p, 4 * p))
        scope.drawRect(Color(0xFF48CAE4), Offset(sauX - 5 * p, sauY), Size(10 * p, 2.5f * p))
        scope.drawRect(Color(0xFFFAF9F6), Offset(sauX - 4 * p, sauY + 0.5f * p), Size(8 * p, 1.8f * p))

        // ZONE 2: Outdoor Street Dining Table & Stools (Lower Center Terrace, cw * 0.50f)
        val tblW = 38 * p
        val tblH = 15 * p
        val tblX = cw * 0.50f - tblW / 2f
        val tblY = curbY + curbH * 0.55f
        // Wooden folding street table legs
        scope.drawRect(Color(0xFF45240F), Offset(tblX + 3 * p, tblY + 3 * p), Size(2.5f * p, tblH - 3 * p))
        scope.drawRect(Color(0xFF45240F), Offset(tblX + tblW - 5.5f * p, tblY + 3 * p), Size(2.5f * p, tblH - 3 * p))
        scope.drawRect(Color(0xFF45240F), Offset(tblX + 3 * p, tblY + tblH * 0.60f), Size(tblW - 6 * p, 1.5f * p))
        // Table top
        scope.drawRect(Color(0xFF7F4F24), Offset(tblX, tblY), Size(tblW, 3.5f * p))
        scope.drawRect(Color(0xFF9C6644), Offset(tblX + p, tblY + 0.5f * p), Size(tblW - 2 * p, 1.2f * p))

        // Tabletop Dining Setup:
        // Steaming plate of momos
        val pltX = tblX + 5 * p
        val pltY = tblY - 4 * p
        scope.drawRect(Color(0xFFCED4DA), Offset(pltX, pltY + 2 * p), Size(11 * p, 2.5f * p))
        scope.drawRect(Color(0xFFFAF0CA), Offset(pltX + 1.5f * p, pltY), Size(3.5f * p, 2.5f * p))
        scope.drawRect(Color(0xFFFAF0CA), Offset(pltX + 5.5f * p, pltY), Size(3.5f * p, 2.5f * p))
        if (sin(timeSeconds * 3f) > 0.3f) {
            scope.drawRect(Color(0x66FFFFFF), Offset(pltX + 4.5f * p, pltY - 3 * p), Size(1.5f * p, 2 * p))
        }
        // Fiery red spicy chili chutney jar with dipping spoon
        val chutX = tblX + 18 * p
        val chutY = tblY - 5 * p
        scope.drawRect(Color(0xFFE63946), Offset(chutX, chutY + 1.5f * p), Size(5 * p, 4 * p))
        scope.drawRect(Color(0xFF8B5E3C), Offset(chutX + 1.5f * p, chutY - 2 * p), Size(p, 4 * p)) // spoon
        // Bamboo chopstick cylinder
        val cpdX = tblX + 25 * p
        val cpdY = tblY - 5.5f * p
        scope.drawRect(Color(0xFFD4A373), Offset(cpdX, cpdY + 2 * p), Size(4 * p, 4.5f * p))
        scope.drawRect(Color(0xFF582F0E), Offset(cpdX + p, cpdY - 2 * p), Size(0.8f * p, 4.5f * p))
        scope.drawRect(Color(0xFF582F0E), Offset(cpdX + 2.2f * p, cpdY - 2 * p), Size(0.8f * p, 4.5f * p))

        // Brass tabletop kerosene lantern with warm flickering glow
        val lanX = tblX + tblW - 6 * p
        val lanY = tblY - 6.5f * p
        scope.drawRect(Color(0xFFB08968), Offset(lanX, lanY), Size(4.5f * p, 7 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(lanX + 0.8f * p, lanY + 1.5f * p), Size(3 * p, 4 * p))
        val lFlick = sin(timeSeconds * 4.5f) * 0.10f + 0.90f
        scope.drawCircle(Color(0x35FFAA00).copy(alpha = 0.22f * lFlick), 10 * p, Offset(lanX + 2.2f * p, lanY + 3.5f * p))

        // Two wooden street stools beside the table
        fun drawStreetStool(sx: Float) {
            val stW = 10 * p
            val stH = 11 * p
            val sl = sx - stW / 2f
            val sy = tblY + 2 * p
            scope.drawRect(Color(0xFF45240F), Offset(sl + 1.5f * p, sy + 2.5f * p), Size(2 * p, stH - 2.5f * p))
            scope.drawRect(Color(0xFF45240F), Offset(sl + stW - 3.5f * p, sy + 2.5f * p), Size(2 * p, stH - 2.5f * p))
            scope.drawRect(Color(0xFF7F4F24), Offset(sl, sy), Size(stW, 2.5f * p))
            scope.drawRect(Color(0xFF9C6644), Offset(sl + p, sy + 0.5f * p), Size(stW - 2 * p, p))
        }
        drawStreetStool(tblX - 7 * p)
        drawStreetStool(tblX + tblW + 7 * p)
    }
}

private fun drawGroundFallenParticles(
    scope: DrawScope,
    fallen: List<FallenParticle>,
    p: Float,
    cw: Float,
    ch: Float
) {
    if (fallen.isEmpty()) return
    for (i in fallen.indices) {
        val fp = fallen[i]
        val px = fp.normX * cw
        val py = fp.normY * ch
        val color = fp.color.copy(alpha = fp.alpha)
        when (fp.type) {
            ParticleType.AUTUMN_LEAF -> {
                // Retro 16-bit pixel art fallen autumn leaf on grass
                val s = fp.size * p * 0.65f
                when (fp.styleVariant % 3) {
                    0 -> {
                        // Tilted leaf with curl & darker shadow pixel
                        scope.drawRect(color, Offset(px, py), Size(s * 1.5f, s * 0.9f))
                        scope.drawRect(color, Offset(px + s * 0.4f, py - s * 0.5f), Size(s * 0.8f, s * 0.5f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.55f).coerceIn(0f, 1f)), Offset(px + s * 0.2f, py + s * 0.9f), Size(s * 1.1f, s * 0.35f))
                        scope.drawRect(Color(0xFF5E2B0C).copy(alpha = fp.alpha), Offset(px - s * 0.3f, py + s * 0.2f), Size(s * 0.35f, s * 0.3f))
                    }
                    1 -> {
                        // Staggered stepped cluster leaf
                        scope.drawRect(color, Offset(px, py), Size(s * 1.2f, s * 1.2f))
                        scope.drawRect(color, Offset(px - s * 0.4f, py + s * 0.3f), Size(s * 0.5f, s * 0.6f))
                        scope.drawRect(color, Offset(px + s * 1.1f, py + s * 0.2f), Size(s * 0.5f, s * 0.7f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.50f).coerceIn(0f, 1f)), Offset(px, py + s * 1.2f), Size(s * 1.2f, s * 0.3f))
                    }
                    else -> {
                        // Compact curled leaf
                        scope.drawRect(color, Offset(px, py), Size(s * 1.4f, s * 0.8f))
                        scope.drawRect(color, Offset(px + s * 0.2f, py + s * 0.7f), Size(s * 0.8f, s * 0.4f))
                    }
                }
            }
            ParticleType.SAKURA_PETAL -> {
                // Soft pink fallen sakura petals resting on meadow grass
                val s = fp.size * p * 0.60f
                when (fp.styleVariant % 3) {
                    0 -> {
                        // Single delicate petal
                        scope.drawRect(color, Offset(px, py), Size(s * 1.3f, s * 0.8f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.75f).coerceIn(0f, 1f)), Offset(px + s * 0.3f, py - s * 0.35f), Size(s * 0.7f, s * 0.4f))
                    }
                    1 -> {
                        // Pair of resting petals
                        scope.drawRect(color, Offset(px, py), Size(s * 1.2f, s * 0.7f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.85f).coerceIn(0f, 1f)), Offset(px + s * 0.8f, py + s * 0.4f), Size(s * 1.0f, s * 0.6f))
                    }
                    else -> {
                        // Tiny curved petal
                        scope.drawRect(color, Offset(px, py), Size(s * 0.9f, s * 0.9f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.65f).coerceIn(0f, 1f)), Offset(px + s * 0.2f, py - s * 0.25f), Size(s * 0.5f, s * 0.3f))
                    }
                }
            }
            ParticleType.SNOWFLAKE -> {
                // Small fallen snow tufts/patches resting on the grass (not swipeable)
                val s = fp.size * p * 0.55f
                when (fp.styleVariant % 3) {
                    0 -> {
                        // Horizontal snow cap on grass blade
                        scope.drawRect(color, Offset(px, py), Size(s * 2.0f, s * 0.7f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.60f).coerceIn(0f, 1f)), Offset(px + s * 0.3f, py + s * 0.7f), Size(s * 1.4f, s * 0.4f))
                    }
                    1 -> {
                        // Soft snow puff
                        scope.drawRect(color, Offset(px, py), Size(s * 1.4f, s * 1.1f))
                        scope.drawRect(color, Offset(px - s * 0.4f, py + s * 0.3f), Size(s * 0.5f, s * 0.6f))
                    }
                    else -> {
                        // Little snow dusting
                        scope.drawRect(color, Offset(px, py), Size(s * 1.6f, s * 0.6f))
                    }
                }
            }
            else -> {}
        }
    }
}

private fun drawBackgroundSeasonalParticles(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    for (i in particles.indices) {
        val pt = particles[i]
        if (pt.type == ParticleType.SAKURA_PETAL || pt.type == ParticleType.AUTUMN_LEAF ||
            pt.type == ParticleType.SNOWFLAKE || pt.type == ParticleType.DANDELION_FLUFF ||
            pt.type == ParticleType.WIND_BREEZE) {
            drawSingleParticle(scope, pt, p)
        }
    }
}

private fun drawForegroundParticles(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    for (i in particles.indices) {
        val pt = particles[i]
        if (pt.type != ParticleType.SAKURA_PETAL && pt.type != ParticleType.AUTUMN_LEAF &&
            pt.type != ParticleType.SNOWFLAKE && pt.type != ParticleType.DANDELION_FLUFF &&
            pt.type != ParticleType.WIND_BREEZE) {
            drawSingleParticle(scope, pt, p)
        }
    }
}

private fun drawParticles(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    drawForegroundParticles(scope, particles, p)
}

private fun drawSingleParticle(scope: DrawScope, pt: PixelParticle, p: Float) {
    val color = pt.color.copy(alpha = pt.alpha)
    when (pt.type) {
        ParticleType.HEART -> {
            val s = pt.size
            scope.drawRect(color, Offset(pt.x - s * 0.4f, pt.y - s * 0.3f), Size(s * 0.4f, s * 0.4f))
            scope.drawRect(color, Offset(pt.x + s * 0.1f, pt.y - s * 0.3f), Size(s * 0.4f, s * 0.4f))
            scope.drawRect(color, Offset(pt.x - s * 0.5f, pt.y), Size(s * 1.1f, s * 0.4f))
            scope.drawRect(color, Offset(pt.x - s * 0.3f, pt.y + s * 0.4f), Size(s * 0.7f, s * 0.3f))
            scope.drawRect(color, Offset(pt.x - s * 0.1f, pt.y + s * 0.7f), Size(s * 0.3f, s * 0.2f))
        }
        ParticleType.LEAF, ParticleType.PETAL -> {
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size, pt.size * 0.7f))
        }
        ParticleType.STAR, ParticleType.SPARKLE -> {
            val s = pt.size
            scope.drawRect(color, Offset(pt.x, pt.y - s * 0.5f), Size(s * 0.35f, s * 1.4f))
            scope.drawRect(color, Offset(pt.x - s * 0.5f, pt.y), Size(s * 1.4f, s * 0.35f))
        }
        ParticleType.STEAM, ParticleType.CHIMNEY_SMOKE -> {
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size, pt.size))
        }
        ParticleType.FIREFLY -> {
            // Pulsating glowing firefly
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size, pt.size))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.4f), Offset(pt.x - p, pt.y - p), Size(pt.size + 2 * p, pt.size + 2 * p))
        }
        ParticleType.SHOOTING_STAR -> {
            // Fast streak with fading tail
            scope.drawRect(color, Offset(pt.x, pt.y), Size(5 * p, 2 * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.5f), Offset(pt.x - 6 * p, pt.y - 3 * p), Size(6 * p, 1.5f * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.2f), Offset(pt.x - 12 * p, pt.y - 6 * p), Size(6 * p, 1 * p))
        }
        ParticleType.SLEEP_Z -> {
            val s = pt.size
            scope.drawRect(color, Offset(pt.x, pt.y), Size(s, s * 0.3f))
            scope.drawRect(color, Offset(pt.x + s * 0.35f, pt.y + s * 0.35f), Size(s * 0.35f, s * 0.35f))
            scope.drawRect(color, Offset(pt.x, pt.y + s * 0.7f), Size(s, s * 0.3f))
        }
        ParticleType.WATER_RIPPLE -> {
            val rx = (pt.size * p * 0.9f).coerceAtLeast(6f)
            val ry = rx * 0.42f
            scope.drawOval(
                color = color.copy(alpha = (pt.alpha * 0.65f).coerceIn(0f, 1f)),
                topLeft = Offset(pt.x - rx, pt.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.3f * p)
            )
        }
        ParticleType.MUSIC_NOTE -> {
            // Retro 16-bit musical eighth note
            scope.drawOval(color.copy(alpha = pt.alpha), topLeft = Offset(pt.x, pt.y + 4f * p), size = Size(3f * p, 2.5f * p))
            scope.drawRect(color.copy(alpha = pt.alpha), Offset(pt.x + 2.4f * p, pt.y), Size(1.2f * p, 5f * p))
            scope.drawRect(color.copy(alpha = pt.alpha), Offset(pt.x + 3.2f * p, pt.y), Size(2f * p, 1.8f * p))
        }
        ParticleType.RAIN_DROP -> {
            // Proper authentic retro rain streak: crisp vertical slant falling towards ground
            val rw = (1.4f * p).coerceAtLeast(4f)
            val rh = (pt.size * 0.95f) * (p / 3.0f).coerceAtLeast(1f) // ~18-24 screen pixels tall
            // Main streak core (bright crisp rain blue/white)
            scope.drawRect(color, Offset(pt.x, pt.y), Size(rw, rh * 0.65f))
            // Softer trail trailing up
            scope.drawRect(color.copy(alpha = (pt.alpha * 0.45f).coerceIn(0f, 1f)), Offset(pt.x + 0.6f * p, pt.y - rh * 0.45f), Size(rw * 0.75f, rh * 0.45f))
        }
        ParticleType.RAIN_SPLASH -> {
            // Expanding ground splash puddle ripple
            val r = (pt.size * p * 0.85f).coerceAtLeast(8f)
            scope.drawRect(color.copy(alpha = (pt.alpha * 0.85f).coerceIn(0f, 1f)), Offset(pt.x - r, pt.y), Size(r * 2f, 1.6f * p))
            scope.drawRect(color.copy(alpha = (pt.alpha * 0.40f).coerceIn(0f, 1f)), Offset(pt.x - r * 0.5f, pt.y - 1f * p), Size(r, 1f * p))
        }
        ParticleType.SAKURA_PETAL -> {
            // Romantic pink drifting sakura petal with tumbling width and highlight
            val s = (p * 1.45f).coerceIn(5.5f, 9.0f)
            val flip = kotlin.math.cos(pt.phase * 1.4f)
            val wFrac = kotlin.math.abs(flip).coerceIn(0.35f, 1.0f)
            val pw = s * 2.2f * wFrac
            val ph = s * 1.5f
            // Petal body
            scope.drawRect(color, Offset(pt.x - pw * 0.5f, pt.y - ph * 0.5f), Size(pw, ph))
            // Soft highlight on upper edge
            scope.drawRect(Color(0xFFFFF0F5).copy(alpha = (pt.alpha * 0.75f).coerceIn(0f, 1f)), Offset(pt.x - pw * 0.35f, pt.y - ph * 0.5f), Size(pw * 0.6f, ph * 0.35f))
            // Delicate petal notch
            scope.drawRect(color.copy(alpha = (pt.alpha * 0.80f).coerceIn(0f, 1f)), Offset(pt.x + pw * 0.1f, pt.y + ph * 0.1f), Size(pw * 0.45f, ph * 0.45f))
        }
        ParticleType.AUTUMN_LEAF -> {
            // Romantic warm autumn leaf with tumbling flutter and silhouette
            val s = (p * 1.50f).coerceIn(6.0f, 9.5f)
            val flip = kotlin.math.cos(pt.phase * 1.2f)
            val wFrac = kotlin.math.abs(flip).coerceIn(0.40f, 1.0f)
            val pw = s * 2.3f * wFrac
            val ph = s * 1.7f
            // Leaf body
            scope.drawRect(color, Offset(pt.x - pw * 0.5f, pt.y - ph * 0.5f), Size(pw, ph))
            // Upper lobe / curl
            scope.drawRect(color, Offset(pt.x - pw * 0.25f, pt.y - ph * 0.85f), Size(pw * 0.55f, ph * 0.45f))
            // Subtle stem / shadow pixel
            scope.drawRect(Color(0xFF5E2B0C).copy(alpha = (pt.alpha * 0.85f).coerceIn(0f, 1f)), Offset(pt.x - pw * 0.55f, pt.y + ph * 0.2f), Size(pw * 0.25f, ph * 0.25f))
        }
        ParticleType.SNOWFLAKE -> {
            // Delicate crystal pixel snowflake (clearly visible 16-bit retro cross)
            val s = (p * 1.35f).coerceIn(4.5f, 7.5f)
            if (pt.size > 2.6f) {
                // Classic 5-pixel cross snowflake with white/ice-blue core
                scope.drawRect(color, Offset(pt.x - s * 1.2f, pt.y - s * 0.35f), Size(s * 2.4f, s * 0.7f))
                scope.drawRect(color, Offset(pt.x - s * 0.35f, pt.y - s * 1.2f), Size(s * 0.7f, s * 2.4f))
                scope.drawRect(Color.White.copy(alpha = pt.alpha), Offset(pt.x - s * 0.35f, pt.y - s * 0.35f), Size(s * 0.7f, s * 0.7f))
            } else {
                // Soft falling snow tuft
                scope.drawRect(color, Offset(pt.x - s * 0.6f, pt.y - s * 0.6f), Size(s * 1.2f, s * 1.2f))
            }
        }
        ParticleType.DANDELION_FLUFF -> {
            // Soft white floating tuft
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size * 0.7f, pt.size * 0.7f))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.6f), Offset(pt.x - pt.size * 0.3f, pt.y - pt.size * 0.2f), Size(pt.size * 1.3f, pt.size * 0.3f))
        }
        ParticleType.WIND_BREEZE -> {
            // Wind gust line
            val len = pt.size * 4f
            scope.drawRect(color, Offset(pt.x, pt.y), Size(len, 1.6f * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.5f), Offset(pt.x - len * 0.25f, pt.y), Size(len * 0.25f, 1f * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.4f), Offset(pt.x + len, pt.y), Size(len * 0.25f, 1f * p))
        }
    }
}

private class BubblePos(var left: Float = 0f, var top: Float = 0f)

@Composable
private fun BoxScope.PixelSpeechBubblesOverlay(
    boyText: String?,
    girlText: String?,
    boyHeadX: Float,
    boyHeadY: Float,
    girlHeadX: Float,
    girlHeadY: Float,
    viewportWidth: Float,
    viewportHeight: Float
) {
    if (boyText.isNullOrEmpty() && girlText.isNullOrEmpty()) return

    val density = LocalDensity.current
    val boyPos = remember { BubblePos() }
    val girlPos = remember { BubblePos() }

    Layout(
        content = {
            if (!boyText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .layoutId("boy")
                        .widthIn(min = 64.dp, max = 155.dp)
                        .drawBehind {
                            drawPixelBubble(
                                isGirl = false,
                                charHeadX = boyHeadX,
                                bubbleLeft = boyPos.left
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BubbleInnerRow(text = boyText, isGirl = false)
                }
            }
            if (!girlText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .layoutId("girl")
                        .widthIn(min = 64.dp, max = 155.dp)
                        .drawBehind {
                            drawPixelBubble(
                                isGirl = true,
                                charHeadX = girlHeadX,
                                bubbleLeft = girlPos.left
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BubbleInnerRow(text = girlText, isGirl = true)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        val boyMeasurable = measurables.firstOrNull { it.layoutId == "boy" }
        val girlMeasurable = measurables.firstOrNull { it.layoutId == "girl" }

        val boyPlaceable = boyMeasurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val girlPlaceable = girlMeasurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))

        val margin = with(density) { 10.dp.toPx() }
        val minGap = with(density) { 8.dp.toPx() }
        val tailHeight = with(density) { 5.dp.toPx() }
        val headGap = with(density) { 3.dp.toPx() }
        val topMargin = with(density) { 72.dp.toPx() }

        var targetBoyX = 0
        var targetBoyY = 0
        var targetGirlX = 0
        var targetGirlY = 0

        if (boyPlaceable != null && girlPlaceable == null) {
            val bw = boyPlaceable.width.toFloat()
            val bh = boyPlaceable.height.toFloat()
            val bx = (boyHeadX - bw / 2f).coerceIn(margin, (viewportWidth - bw - margin).coerceAtLeast(margin))
            val by = (boyHeadY - bh - tailHeight - headGap).coerceAtLeast(topMargin)
            targetBoyX = bx.toInt()
            targetBoyY = by.toInt()
            boyPos.left = bx
            boyPos.top = by
        } else if (boyPlaceable == null && girlPlaceable != null) {
            val gw = girlPlaceable.width.toFloat()
            val gh = girlPlaceable.height.toFloat()
            val gx = (girlHeadX - gw / 2f).coerceIn(margin, (viewportWidth - gw - margin).coerceAtLeast(margin))
            val gy = (girlHeadY - gh - tailHeight - headGap).coerceAtLeast(topMargin)
            targetGirlX = gx.toInt()
            targetGirlY = gy.toInt()
            girlPos.left = gx
            girlPos.top = gy
        } else if (boyPlaceable != null && girlPlaceable != null) {
            val bw = boyPlaceable.width.toFloat()
            val bh = boyPlaceable.height.toFloat()
            val gw = girlPlaceable.width.toFloat()
            val gh = girlPlaceable.height.toFloat()

            val boyIsLeft = boyHeadX <= girlHeadX
            val leftPlaceable = if (boyIsLeft) boyPlaceable else girlPlaceable
            val rightPlaceable = if (boyIsLeft) girlPlaceable else boyPlaceable
            val leftHeadX = if (boyIsLeft) boyHeadX else girlHeadX
            val rightHeadX = if (boyIsLeft) girlHeadX else boyHeadX
            val leftHeadY = if (boyIsLeft) boyHeadY else girlHeadY
            val rightHeadY = if (boyIsLeft) girlHeadY else boyHeadY
            val lw = leftPlaceable.width.toFloat()
            val lh = leftPlaceable.height.toFloat()
            val rw = rightPlaceable.width.toFloat()
            val rh = rightPlaceable.height.toFloat()

            val totalNeeded = lw + minGap + rw
            val availWidth = (viewportWidth - 2 * margin).coerceAtLeast(100f)

            var lx: Float
            var rx: Float
            var ly: Float
            var ry: Float

            if (totalNeeded <= availWidth) {
                // Both fit side-by-side cleanly at natural character height!
                val idealLx = leftHeadX - lw / 2f
                val idealRx = rightHeadX - rw / 2f

                if (idealLx + lw + minGap <= idealRx) {
                    // No horizontal collision when centered!
                    lx = idealLx.coerceIn(margin, (viewportWidth - lw - margin).coerceAtLeast(margin))
                    rx = idealRx.coerceIn(margin, (viewportWidth - rw - margin).coerceAtLeast(margin))
                } else {
                    // Overlap would occur: separate gracefully around midpoint between the two characters!
                    val midX = (leftHeadX + rightHeadX) / 2f
                    lx = midX - minGap / 2f - lw
                    rx = midX + minGap / 2f

                    // Clamp within screen boundaries while maintaining at least minGap separation
                    if (lx < margin) {
                        val shift = margin - lx
                        lx += shift
                        rx += shift
                    } else if (rx + rw > viewportWidth - margin) {
                        val shift = (rx + rw) - (viewportWidth - margin)
                        lx -= shift
                        rx -= shift
                    }
                    lx = lx.coerceAtLeast(margin)
                    rx = rx.coerceAtLeast(lx + lw + minGap)
                }
                ly = (leftHeadY - lh - tailHeight - headGap).coerceAtLeast(topMargin)
                ry = (rightHeadY - rh - tailHeight - headGap).coerceAtLeast(topMargin)
            } else {
                // Screen too narrow for both full bubbles side-by-side:
                // Minimal vertical stagger (only one bubble height + 6dp, NOT far away!)
                lx = (leftHeadX - lw / 2f).coerceIn(margin, (viewportWidth - lw - margin).coerceAtLeast(margin))
                rx = (rightHeadX - rw / 2f).coerceIn(margin, (viewportWidth - rw - margin).coerceAtLeast(margin))
                // Lower bubble right above its character
                ry = (rightHeadY - rh - tailHeight - headGap).coerceAtLeast(topMargin)
                // Upper bubble placed just above the lower bubble
                ly = (ry - lh - with(density) { 6.dp.toPx() }).coerceAtLeast(topMargin)
            }

            if (boyIsLeft) {
                targetBoyX = lx.toInt()
                targetBoyY = ly.toInt()
                targetGirlX = rx.toInt()
                targetGirlY = ry.toInt()
                boyPos.left = lx
                boyPos.top = ly
                girlPos.left = rx
                girlPos.top = ry
            } else {
                targetGirlX = lx.toInt()
                targetGirlY = ly.toInt()
                targetBoyX = rx.toInt()
                targetBoyY = ry.toInt()
                girlPos.left = lx
                girlPos.top = ly
                boyPos.left = rx
                boyPos.top = ry
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            boyPlaceable?.place(targetBoyX, targetBoyY)
            girlPlaceable?.place(targetGirlX, targetGirlY)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelBubble(
    isGirl: Boolean,
    charHeadX: Float,
    bubbleLeft: Float
) {
    val w = size.width
    val h = size.height
    val p = 2.dp.toPx()

    // 1. Pixelated drop shadow (offset down-right)
    drawRect(Color(0x4D000000), Offset(p, p), Size(w, h))

    val borderColor = if (isGirl) Color(0xFFFF3366) else Color(0xFF1E202C)
    val bgColor = if (isGirl) Color(0xFFFFF0F5) else Color(0xFFFFFFFF)

    // 2. Stepped corner border
    drawRect(borderColor, Offset(0f, p), Size(w, h - 2 * p))
    drawRect(borderColor, Offset(p, 0f), Size(w - 2 * p, h))

    // 3. Background fill
    drawRect(bgColor, Offset(p, p), Size(w - 2 * p, h - 2 * p))

    // 4. Pixelated speech tail pointing to character's head
    val localTailX = (charHeadX - bubbleLeft).coerceIn(6 * p, w - 6 * p)
    // Outer tail border (downward pointer)
    drawRect(borderColor, Offset(localTailX - 2.5f * p, h), Size(5 * p, 2.5f * p))
    // Inner tail fill
    drawRect(bgColor, Offset(localTailX - 1.5f * p, h - p), Size(3 * p, 2.5f * p))
}

@Composable
private fun BubbleInnerRow(
    text: String,
    isGirl: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val iconColor = if (isGirl) Color(0xFFFF4D6D) else Color(0xFFC9184A)
                val s = size.width
                // Pixel heart icon
                drawRect(iconColor, Offset(s * 0.15f, s * 0.15f), Size(s * 0.32f, s * 0.32f))
                drawRect(iconColor, Offset(s * 0.53f, s * 0.15f), Size(s * 0.32f, s * 0.32f))
                drawRect(iconColor, Offset(s * 0.05f, s * 0.40f), Size(s * 0.90f, s * 0.25f))
                drawRect(iconColor, Offset(s * 0.20f, s * 0.65f), Size(s * 0.60f, s * 0.20f))
                drawRect(iconColor, Offset(s * 0.35f, s * 0.85f), Size(s * 0.30f, s * 0.15f))
            }
        }
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = if (isGirl) Color(0xFFC9184A) else Color(0xFF1E202C),
            letterSpacing = 0.3.sp,
            lineHeight = 15.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun BoxScope.PixelMessageBox(
    message: String,
    alpha: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 82.dp, start = 18.dp, end = 18.dp)
            .fillMaxWidth(0.88f)
            .graphicsLayer { this.alpha = alpha() }
            .drawBehind {
                val w = size.width
                val h = size.height
                val p = 2.5.dp.toPx() // Stepped pixel unit

                // 1. Pixelated drop shadow (offset down-right)
                drawRect(Color(0x77000000), Offset(p, p), Size(w, h))

                // 2. Stepped corner outer pixel border
                drawRect(Color(0xFF1E202C), Offset(0f, p), Size(w, h - 2 * p))
                drawRect(Color(0xFF1E202C), Offset(p, 0f), Size(w - 2 * p, h))

                // 3. Warm cream parchment background
                drawRect(Color(0xFFFFFDF8), Offset(p, p), Size(w - 2 * p, h - 2 * p))

                // 4. Retro inner trim line (blush pink / rose)
                val trimColor = Color(0xFFFFB5C2)
                drawRect(trimColor, Offset(2 * p, 2 * p), Size(w - 4 * p, p)) // Top
                drawRect(trimColor, Offset(2 * p, h - 3 * p), Size(w - 4 * p, p)) // Bottom
                drawRect(trimColor, Offset(2 * p, 2 * p), Size(p, h - 4 * p)) // Left
                drawRect(trimColor, Offset(w - 3 * p, 2 * p), Size(p, h - 4 * p)) // Right

                // 5. Corner pixel gem rivets
                val gemColor = Color(0xFFC9184A)
                drawRect(gemColor, Offset(2 * p, 2 * p), Size(p, p))
                drawRect(gemColor, Offset(w - 3 * p, 2 * p), Size(p, p))
                drawRect(gemColor, Offset(2 * p, h - 3 * p), Size(p, p))
                drawRect(gemColor, Offset(w - 3 * p, h - 3 * p), Size(p, p))
            }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF2B2D42),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
