package com.example.ui

import com.example.engine.drawPixelGlow
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
import com.example.engine.drawCastShadow
import com.example.engine.drawContactShadow
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

// Fixed landscape arrays
val RIVER_PEB_POSITIONS = floatArrayOf(0.08f, 0.28f, 0.42f, 0.62f, 0.76f, 0.92f)
val RIVER_STEP_STONES_X = floatArrayOf(0.15f, 0.34f, 0.52f, 0.72f, 0.88f)
val RIVER_STEP_STONES_Y = floatArrayOf(0.74f, 0.82f, 0.76f, 0.84f, 0.78f)
val RIVER_CLOV_POSITIONS = floatArrayOf(0.12f, 0.35f, 0.54f, 0.70f, 0.88f)
val RIVER_REED_OFFSETS = floatArrayOf(-9f, -6f, 7f, 9f)
val RIVER_STALKS_X = floatArrayOf(-6f, -3f, 0f, 3f, 6f)
val RIVER_STALKS_Y = floatArrayOf(0f, -2.5f, -4f, -1.5f, 1.5f)
val RIVER_MUSH_OFFSETS = floatArrayOf(-8f, 0f, 8f)

fun drawPathGround(
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

    if (weather == com.example.scene.WeatherType.RAIN) {
        // Rain-darkened cobbles with a few broken pixel reflections.
        scope.drawRect(Color(0xFF9FC3D8).copy(alpha = 0.075f), Offset(0f, pathY + 2 * p), Size(cw, pathH - 4 * p))
        val glintShift = (timeSeconds * 7f) % (24f * p)
        var glintX = -24f * p + glintShift
        while (glintX < cw) {
            scope.drawRect(Color(0xFFCEE7F2).copy(alpha = 0.22f), Offset(glintX, pathY + 5 * p), Size(5f * p, p))
            scope.drawRect(Color(0xFFCEE7F2).copy(alpha = 0.14f), Offset(glintX + 10f * p, pathY + 14 * p), Size(3f * p, p))
            glintX += 24f * p
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
        drawPixelGlow(scope, Color(0x35FFAA00).copy(alpha = 0.22f * pagFlick), 18 * p, Offset(pagX, pagY + 5 * p), p)
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
            drawPixelGlow(scope, Color(0x3048CAE4).copy(alpha = 0.20f * mPulse), 7 * p, Offset(mx + 2 * p, my), p)
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
        drawCastShadow(scope, cw * 0.50f, tblY + tblH, 40, p, heightPx = 15)
        drawContactShadow(scope, cw * 0.50f, tblY + tblH, 40, p)
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
        drawPixelGlow(scope, Color(0x35FFAA00).copy(alpha = 0.22f * lFlick), 10 * p, Offset(lanX + 2.2f * p, lanY + 3.5f * p), p)

        // Two wooden street stools beside the table
        fun drawStreetStool(sx: Float) {
            val stW = 10 * p
            val stH = 11 * p
            val sl = sx - stW / 2f
            val sy = tblY + 2 * p
            drawCastShadow(scope, sx, sy + stH, 12, p, heightPx = 11)
            drawContactShadow(scope, sx, sy + stH, 12, p)
            scope.drawRect(Color(0xFF45240F), Offset(sl + 1.5f * p, sy + 2.5f * p), Size(2 * p, stH - 2.5f * p))
            scope.drawRect(Color(0xFF45240F), Offset(sl + stW - 3.5f * p, sy + 2.5f * p), Size(2 * p, stH - 2.5f * p))
            scope.drawRect(Color(0xFF7F4F24), Offset(sl, sy), Size(stW, 2.5f * p))
            scope.drawRect(Color(0xFF9C6644), Offset(sl + p, sy + 0.5f * p), Size(stW - 2 * p, p))
        }
        drawStreetStool(tblX - 7 * p)
        drawStreetStool(tblX + tblW + 7 * p)
    }
}
