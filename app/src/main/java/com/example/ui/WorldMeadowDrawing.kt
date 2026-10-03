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

internal val PEBBLE_FRACS = floatArrayOf(
    0.04f, 0.11f, 0.19f, 0.28f, 0.35f, 0.43f, 0.51f, 0.58f, 0.66f, 0.73f, 0.81f, 0.89f, 0.95f
)

internal val TUFT_ROW_FACTORS = floatArrayOf(0.18f, 0.38f, 0.60f, 0.82f)

internal val SAKURA_FLOWERS = listOf(
    Pair(Color(0xFFFFB5C2), Color(0xFFFFE66D)),
    Pair(Color(0xFFFF85A1), Color(0xFFFFF0F5)),
    Pair(Color(0xFFFFF0F5), Color(0xFFFFB5C2))
)

internal val AUTUMN_FLOWERS = listOf(
    Pair(Color(0xFFF4A261), Color(0xFF8B1E1E)),
    Pair(Color(0xFFE9C46A), Color(0xFFD46A28)),
    Pair(Color(0xFFD62828), Color(0xFFFFD166))
)

internal val DEFAULT_FLOWERS = listOf(
    Pair(Color(0xFFFFFFFF), Color(0xFFFFD166)),
    Pair(Color(0xFFFF4D6D), Color(0xFFFFE66D)),
    Pair(Color(0xFF48CAE4), Color(0xFFFFFFFF)),
    Pair(Color(0xFFFFB703), Color(0xFF9D4EDD)),
    Pair(Color(0xFFB5179E), Color(0xFFFFCAD4))
)

internal val FLOWER_POSITIONS = listOf(
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

// Precomputed Grass Blade Height Table to eliminate trigonometry per blade
internal val GRASS_HEIGHT_LOOKUP = FloatArray(64) { i ->
    val fakeBx = i * 8.8f
    2.5f + kotlin.math.abs(sin(fakeBx * 0.18f + 0.7f)) * 1.8f + kotlin.math.abs(sin(fakeBx * 0.08f)) * 1.4f
}
internal val GRASS_SPARSE_HEIGHT_LOOKUP = FloatArray(64) { i ->
    val fakeBx = i * 19.2f + 7.68f
    1.5f + kotlin.math.abs(sin(fakeBx * 0.14f)) * 1.2f
}

internal val MEADOW_PUDDLE_X = floatArrayOf(0.24f, 0.53f, 0.79f)
internal val MEADOW_PUDDLE_Y = floatArrayOf(0.74f, 0.79f, 0.73f)
internal val MEADOW_PUDDLE_SIZE = floatArrayOf(22f, 28f, 20f)

internal fun drawMeadowGround(
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
        for (i in MEADOW_PUDDLE_X.indices) {
            val pxRel = MEADOW_PUDDLE_X[i]
            val pyRel = MEADOW_PUDDLE_Y[i]
            val pSize = MEADOW_PUDDLE_SIZE[i]
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

        // Thin wet sheen catches the overcast sky; low pixel mist softens the horizon.
        val sheenAlpha = 0.035f + (sin(timeSeconds * 0.45f) * 0.012f + 0.012f)
        scope.drawRect(
            Color(0xFFB7D1E0).copy(alpha = sheenAlpha),
            Offset(0f, groundY + totalH * 0.15f),
            Size(cw, totalH * 0.08f)
        )
        val mistPhase = sin(timeSeconds * 0.32f)
        val mistY = groundY + totalH * (0.27f + mistPhase * 0.018f)
        scope.drawRect(Color(0xFFCFDCE2).copy(alpha = 0.045f), Offset(0f, mistY), Size(cw, 10f * p))
        scope.drawRect(Color(0xFFCFDCE2).copy(alpha = 0.025f), Offset(cw * 0.18f, mistY + 7f * p), Size(cw * 0.64f, 8f * p))
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

internal fun drawWildFlowers(
    scope: DrawScope,
    cw: Float,
    groundY: Float,
    p: Float,
    timeSeconds: Float = 0f,
    gardenStage: Int = 3,
    wiggleTimer: Float = 0f,
    weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
    blooms: List<com.example.data.GardenBloom> = emptyList()
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

            // Stage 4+: a brief butterfly visit, then long quiet intervals.
            val butterflyVisit = (timeSeconds % 46f) < 7f
            if (gardenStage >= 4 && weather == com.example.scene.WeatherType.SUNNY && butterflyVisit) {
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

    if (blooms.isNotEmpty()) drawKeepsakeBlooms(scope, cw, groundY, p, timeSeconds, blooms)
}

// Front-row slots for keepsake plants, chosen to sit between the wildflowers.
private val KEEPSAKE_X = floatArrayOf(0.05f, 0.93f, 0.17f, 0.81f, 0.27f, 0.69f, 0.11f, 0.87f, 0.22f, 0.75f, 0.33f, 0.63f)
private val KEEPSAKE_Y = floatArrayOf(13f, 12f, 15f, 14f, 12f, 15f, 17f, 16f, 17f, 13f, 16f, 17f)

/** Keepsake plants earned by visiting (see GardenGrowth). They never wilt or disappear. */
private fun drawKeepsakeBlooms(scope: DrawScope, cw: Float, groundY: Float, p: Float, timeSeconds: Float, blooms: List<com.example.data.GardenBloom>) {
    var golden = 0
    for (bloom in blooms) {
        if (bloom.isGolden) { golden++; continue }
        val slot = bloom.index % KEEPSAKE_X.size
        val fx = cw * KEEPSAKE_X[slot]
        val fy = groundY + KEEPSAKE_Y[slot] * p
        val sway = sin(timeSeconds * 2.6f + slot) * p * 1.6f
        val petal = Color(bloom.plant.petal)
        val center = Color(bloom.plant.center)
        // Taller stem with two leaves so they read as special
        scope.drawRect(Color(0xFF2D6A4F), Offset(fx + sway * 0.4f, fy - 2 * p), Size(1.6f * p, 11 * p))
        scope.drawRect(Color(0xFF52B788), Offset(fx - 2.5f * p + sway * 0.2f, fy + 3 * p), Size(2.5f * p, 2 * p))
        scope.drawRect(Color(0xFF52B788), Offset(fx + 1.6f * p + sway * 0.2f, fy + 5 * p), Size(2.5f * p, 2 * p))
        // Rounded 7x7 bloom
        val bx = fx - 3 * p + sway
        val by = fy - 9 * p
        scope.drawRect(petal, Offset(bx + p, by), Size(5 * p, 7 * p))
        scope.drawRect(petal, Offset(bx, by + p), Size(7 * p, 5 * p))
        scope.drawRect(center, Offset(bx + 2.5f * p, by + 2.5f * p), Size(2 * p, 2 * p))
    }
    // Golden blooms: soft twinkles drifting over the meadow (capped so the scene stays calm)
    for (k in 0 until minOf(golden, 8)) {
        val twinkle = (sin(timeSeconds * 2.2f + k * 1.7f) + 1f) * 0.5f
        if (twinkle < 0.35f) continue
        val tx = cw * (0.1f + 0.11f * k)
        val ty = groundY - (6f + (k % 3) * 5f) * p
        val gold = Color(0xFFFFD700).copy(alpha = twinkle)
        scope.drawRect(gold, Offset(tx, ty - p), Size(p, 3 * p))
        scope.drawRect(gold, Offset(tx - p, ty), Size(3 * p, p))
    }
}
