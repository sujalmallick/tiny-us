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
import androidx.compose.ui.graphics.drawscope.Fill
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
import com.example.scene.CafeLayout
import com.example.scene.CampfireLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.datetime.offsetAt

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers for the three "special" scenes (cafe, sunroom, campfire)
// ─────────────────────────────────────────────────────────────────────────────

/** Deterministic 0..1 noise: scenery looks hand-placed and varied, yet identical every frame. */
private fun nz(i: Int, salt: Int = 0): Float {
    var x = i * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

/*
 * Drawing shorthands for these scenes. The pixel renderer (LowResWorldBuffer) puts every shape on
 * the game-pixel grid, so these draw plain shapes; they used to snap to a grid of their own.
 */
private fun DrawScope.px(color: Color, x: Float, y: Float, w: Float, h: Float) {
    if (w > 0f && h > 0f) drawRect(color, Offset(x, y), Size(w, h))
}

private fun DrawScope.pRect(color: Color, topLeft: Offset, size: Size) = px(color, topLeft.x, topLeft.y, size.width, size.height)

private fun DrawScope.pOval(color: Color, topLeft: Offset, size: Size, style: Stroke? = null) =
    drawOval(color, topLeft, size, style = style ?: Fill)

private fun DrawScope.pCircle(color: Color, radius: Float, center: Offset) = drawCircle(color, radius, center)

private fun DrawScope.pRoundRect(color: Color, topLeft: Offset, size: Size, cornerRadius: CornerRadius) =
    drawRoundRect(color, topLeft, size, cornerRadius)

private fun DrawScope.pLine(color: Color, start: Offset, end: Offset, strokeWidth: Float) =
    drawLine(color, start, end, strokeWidth)

private fun DrawScope.pArc(color: Color, startAngle: Float, sweepAngle: Float, useCenter: Boolean, topLeft: Offset, size: Size, style: Stroke? = null) =
    drawArc(color, startAngle, sweepAngle, useCenter, topLeft, size, style = style ?: Fill)

/** Stepped (pixel-art) triangle pointing up, with its base centred on [cx], [baseY]. */
private fun DrawScope.pixelTriangle(color: Color, cx: Float, baseY: Float, halfW: Float, h: Float, step: Float) {
    var y = 0f
    while (y < h) {
        val hw = halfW * ((y + step) / h).coerceAtMost(1f)
        px(color, cx - hw, baseY - h + y, hw * 2f, step + 0.6f)
        y += step
    }
}

private fun mix(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

private var tzOffsetMs = 0L
private var tzCheckedAt = Long.MIN_VALUE

/** Seconds since local midnight; drives wall clocks and where the sun shines in from. */
private fun localDaySeconds(): Float {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    if (now - tzCheckedAt > 60_000L) {
        tzOffsetMs = kotlinx.datetime.TimeZone.currentSystemDefault()
            .offsetAt(kotlin.time.Instant.fromEpochMilliseconds(now)).totalSeconds * 1000L
        tzCheckedAt = now
    }
    return (((now + tzOffsetMs) / 1000L) % 86_400L).toFloat()
}

/** A sagging string of warm bulbs that twinkle out of step with each other. */
private fun drawFairyLights(scope: DrawScope, x0: Float, x1: Float, y: Float, sag: Float, p: Float, time: Float, count: Int, lit: Boolean) {
    val colors = FAIRY_COLORS
    var prevX = x0
    var prevY = y
    for (i in 0..count) {
        val t = i / count.toFloat()
        val x = x0 + (x1 - x0) * t
        val by = y + sag * 4f * t * (1f - t)
        scope.pLine(Color(0xFF2B211B), Offset(prevX, prevY), Offset(x, by), strokeWidth = 0.6f * p)
        prevX = x
        prevY = by
        if (i == 0 || i == count) continue
        val c = colors[i % colors.size]
        if (lit) {
            val twinkle = 0.65f + 0.35f * sin(time * 2.1f + i * 1.7f)
            scope.pCircle(c.copy(alpha = 0.22f * twinkle), 3.6f * p, Offset(x, by + 2.2f * p))
            scope.px(c.copy(alpha = twinkle), x - 0.9f * p, by + 0.8f * p, 1.8f * p, 2.4f * p)
        } else {
            scope.px(c.copy(alpha = 0.45f), x - 0.9f * p, by + 0.8f * p, 1.8f * p, 2.4f * p)
        }
    }
}

private val FAIRY_COLORS = arrayOf(Color(0xFFFFD37A), Color(0xFFFFA9B8), Color(0xFFB8F2D0), Color(0xFFFFC08A))

// ─────────────────────────────────────────────────────────────────────────────
// COZY RAINY CAFE
// ─────────────────────────────────────────────────────────────────────────────

fun drawRainyCafeScene(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    val phase = engine.timeOfDayPhase
    val night = phase.isNight
    val dusk = phase.isSunset
    val lampsBright = night || dusk
    val wallBottom = ch * CafeLayout.WALL_BOTTOM
    val feetY = ch * CafeLayout.SEAT_FEET_Y
    val wainscotTop = wallBottom - 30f * p
    val win = CafeLayout.window(cw, ch)

    // 1. Exposed brick wall with brick-by-brick colour variation
    scope.px(Color(0xFF3F241C), 0f, 0f, cw, wainscotTop)
    val brickW = 15f * p
    val brickH = 6f * p
    var row = 0
    var by = 0f
    while (by < wainscotTop) {
        val off = if (row % 2 == 0) 0f else brickW * 0.5f
        var col = 0
        var bx = -off
        while (bx < cw) {
            val hidden = bx > win.left && bx + brickW < win.right && by > win.top && by + brickH < win.bottom
            if (!hidden) {
                val v = nz(row * 97 + col, 3)
                val c = when {
                    v < 0.18f -> Color(0xFF5E3227)
                    v < 0.55f -> Color(0xFF6B3A2D)
                    v < 0.90f -> Color(0xFF7A4434)
                    else -> Color(0xFF8C4E3A)
                }
                val h = minOf(brickH - p, wainscotTop - by - p)
                scope.px(c, bx, by, brickW - p, h)
                if (v > 0.62f) scope.px(Color(0x1FFFFFFF), bx, by, brickW - p, p)
            }
            bx += brickW
            col++
        }
        by += brickH
        row++
    }

    // Ceiling beam and a string of fairy lights along it
    scope.px(Color(0xFF33201A), 0f, 0f, cw, 8f * p)
    scope.px(Color(0xFF4E3123), 0f, 6.5f * p, cw, 1.5f * p)
    for (b in 0..5) scope.px(Color(0xFF2A1A14), cw * (0.04f + b * 0.19f), 0f, 3f * p, 8f * p)
    drawFairyLights(scope, 0f, cw, 9f * p, 9f * p, p, time, 26, lit = true)

    // 2. Sage-green wainscot panelling, chair rail and skirting
    scope.px(Color(0xFF51705D), 0f, wainscotTop, cw, wallBottom - wainscotTop)
    scope.px(Color(0xFF7A5236), 0f, wainscotTop - 1.2f * p, cw, 2.4f * p)
    var panelX = 3f * p
    while (panelX < cw) {
        scope.px(Color(0xFF45614F), panelX, wainscotTop + 4f * p, 18f * p, 0.7f * p)
        scope.px(Color(0xFF45614F), panelX, wainscotTop + 4f * p, 0.7f * p, 21f * p)
        scope.px(Color(0xFF66897A), panelX + 18f * p, wainscotTop + 4f * p, 0.7f * p, 21f * p)
        scope.px(Color(0xFF66897A), panelX, wainscotTop + 25f * p, 18.7f * p, 0.7f * p)
        panelX += 22f * p
    }
    scope.px(Color(0xFF2E1D15), 0f, wallBottom - 2.5f * p, cw, 2.5f * p)

    // 3. Warm plank floor; rows widen toward the viewer for depth
    drawCafePlanks(scope, cw, wallBottom, wallBottom, ch, p)

    // 4. The big window onto a rainy street (sky follows the real time of day)
    val glass = CafeLayout.glass(cw, ch, p)
    val skyTop = when { night -> Color(0xFF131A2D); dusk -> Color(0xFF4A3F63); else -> Color(0xFF6F8797) }
    val skyLow = when { night -> Color(0xFF2B3350); dusk -> Color(0xFFB0857F); else -> Color(0xFFA7B7C0) }
    val streetY = glass.top + glass.height * 0.82f
    for (b in 0 until 10) {
        val t = b / 9f
        scope.px(mix(skyTop, skyLow, t), glass.left, glass.top + (streetY - glass.top) * b / 10f, glass.width, (streetY - glass.top) / 10f + 1f)
    }
    // Far buildings with windows that light up after dark
    var bx2 = glass.left
    var bi = 0
    while (bx2 < glass.right) {
        val bw = (16f + nz(bi, 21) * 14f) * p
        val bh = (40f + nz(bi, 22) * 55f) * p
        val w = minOf(bw, glass.right - bx2)
        scope.px(if (night) Color(0xFF1C2133) else Color(0xFF56646E), bx2, streetY - bh, w, bh)
        scope.px(if (night) Color(0xFF252B40) else Color(0xFF63727C), bx2, streetY - bh, w, 1.5f * p)
        var wy = streetY - bh + 4f * p
        var wr = 0
        while (wy < streetY - 7f * p) {
            var wx = bx2 + 3f * p
            var wc = 0
            while (wx < bx2 + w - 4f * p) {
                val lit = nz(bi * 131 + wr * 17 + wc, 9) < (if (night) 0.55f else if (dusk) 0.35f else 0.12f)
                scope.px(if (lit) (if (night || dusk) Color(0xFFFFD27A) else Color(0xFFE9E3CB)) else Color(0x2A000000), wx, wy, 2.6f * p, 3.2f * p)
                wx += 6f * p
                wc++
            }
            wy += 7.5f * p
            wr++
        }
        bx2 += bw + 2f * p
        bi++
    }
    // Wet street, kerb and shimmering reflections
    scope.px(if (night) Color(0xFF151922) else Color(0xFF3D464C), glass.left, streetY, glass.width, glass.bottom - streetY)
    scope.px(if (night) Color(0xFF2B303C) else Color(0xFF6E777C), glass.left, streetY, glass.width, 3f * p)
    val lampPostX = glass.left + glass.width * 0.80f
    val lampOn = night || dusk
    for (k in 0..5) {
        val rx = glass.left + glass.width * (0.08f + k * 0.16f)
        for (seg in 0..3) {
            val shimmer = 0.10f + 0.10f * sin(time * 3f + k + seg * 1.3f)
            val wob = sin(time * 2f + seg + k) * 1f * p
            scope.px((if (lampOn) Color(0xFFFFD27A) else Color(0xFFC9D3D8)).copy(alpha = shimmer), rx + wob, streetY + (4f + seg * 3.2f) * p, (3.5f - seg * 0.6f) * p, 1.2f * p)
        }
    }
    for (k in 0..3) {
        val rp = (time * 0.7f + k * 0.27f) % 1f
        val cx = glass.left + glass.width * (0.15f + k * 0.22f)
        val cy = streetY + (glass.bottom - streetY) * (0.45f + 0.12f * (k % 2))
        scope.pOval(Color(0xFFCFE8EF).copy(alpha = 0.45f * (1f - rp)), Offset(cx - 6f * p * rp, cy - 1.5f * p * rp), Size(12f * p * rp, 3f * p * rp), style = Stroke(0.5f * p))
    }
    // Street lamp: glows after dark, stays dark by day
    scope.px(Color(0xFF1E2228), lampPostX - 0.9f * p, streetY - 46f * p, 1.8f * p, 49f * p)
    scope.px(Color(0xFF1E2228), lampPostX - 4f * p, streetY - 48f * p, 8f * p, 2.5f * p)
    scope.px(if (lampOn) Color(0xFFFFE6A3) else Color(0xFF8E979C), lampPostX - 3f * p, streetY - 46f * p, 6f * p, 3f * p)
    if (lampOn) {
        scope.pCircle(Color(0xFFFFD27A).copy(alpha = 0.28f), 16f * p, Offset(lampPostX, streetY - 44f * p))
        scope.px(Color(0xFFFFD27A).copy(alpha = 0.10f), lampPostX - 10f * p, streetY - 43f * p, 20f * p, 43f * p)
    }

    // Umbrella passerby walking past outside
    val passer = CafeLayout.passerby(cw, ch, p, time)
    if (passer != null) {
        val passerX = passer.x
        val passerY = passer.y
        val umbW = 20f * p
        val umbH = 10f * p
        scope.pOval(Color(0xFFD90429), Offset(passerX - umbW / 2f, passerY - 22f * p), Size(umbW, umbH))
        scope.pOval(Color(0xFFEF233C), Offset(passerX - umbW / 2f + 2f * p, passerY - 21f * p), Size(umbW - 4f * p, umbH * 0.6f))
        scope.pLine(Color(0xFF2B2B2B), Offset(passerX, passerY - 17f * p), Offset(passerX, passerY - 10f * p), strokeWidth = 1.5f * p)
        scope.pCircle(Color(0xFFFFD8B8), 2.5f * p, Offset(passerX, passerY - 11f * p))
        scope.pRect(Color(0xFF4A4E69), Offset(passerX - 3.5f * p, passerY - 8f * p), Size(7f * p, 8f * p))
        val legCycle = sin(time * 10f) * 2.5f * p
        scope.pLine(Color(0xFF22223B), Offset(passerX - 1.5f * p, passerY), Offset(passerX - 1.5f * p - legCycle, passerY + 5f * p), strokeWidth = 1.8f * p)
        scope.pLine(Color(0xFF22223B), Offset(passerX + 1.5f * p, passerY), Offset(passerX + 1.5f * p + legCycle, passerY + 5f * p), strokeWidth = 1.8f * p)
    }

    // Striped awning outside the top of the glass, dripping rainwater
    val awningH = 7f * p
    var ax = glass.left
    var ai = 0
    while (ax < glass.right) {
        val sw = minOf(6f * p, glass.right - ax)
        scope.px(if (ai % 2 == 0) Color(0xFFB9473A) else Color(0xFFF1E4CC), ax, glass.top, sw, awningH)
        scope.pCircle(if (ai % 2 == 0) Color(0xFFB9473A) else Color(0xFFF1E4CC), sw / 2f, Offset(ax + sw / 2f, glass.top + awningH))
        ax += 6f * p
        ai++
    }
    for (k in 0..6) {
        val dx = glass.left + glass.width * (k + 0.5f) / 7f
        val fall = (time * 0.6f + k * 0.37f) % 1f
        val dy = glass.top + awningH + 3f * p + fall * (streetY - glass.top - awningH)
        scope.px(Color(0xFFCFE8EF).copy(alpha = 0.8f), dx, dy, 0.8f * p, 2.6f * p)
    }

    // Rain streaking down the glass
    for (i in 0..22) {
        val rx = glass.left + 2f * p + ((i * 47) % 89) / 90f * (glass.width - 4f * p)
        val rSpeed = 0.12f + (i % 5) * 0.03f
        val rFall = (time * rSpeed + i * 0.09f) % 1f
        val ry = glass.top + rFall * (glass.height - 8f * p)
        scope.pLine(Color(0xFFCFE8EF).copy(alpha = 0.65f), Offset(rx + 2f * p, ry), Offset(rx, ry + (4f + i % 3) * p), strokeWidth = 1.2f * p)
    }
    // Condensation fogging the cold lower glass, with beads of water
    for (b in 0 until 6) {
        val t = b / 6f
        scope.px(Color.White.copy(alpha = 0.03f + 0.035f * t), glass.left, glass.bottom - glass.height * 0.22f * (1f - t), glass.width, glass.height * 0.22f / 6f + 1f)
    }
    for (d in 0..18) {
        val dx = glass.left + nz(d, 31) * glass.width
        val dy = glass.bottom - nz(d, 32) * glass.height * 0.25f
        scope.px(Color.White.copy(alpha = 0.35f), dx, dy, 0.9f * p, 0.9f * p)
    }
    // Fog heart drawn with a fingertip
    if (engine.cafeWindowHeartTimer > 0f) {
        val alpha = (engine.cafeWindowHeartTimer / 2.8f).coerceIn(0f, 1f)
        val hx = cw * engine.cafeWindowHeartX
        val hy = ch * engine.cafeWindowHeartY
        val s = 1.6f * p
        val c = Color(0xFFFFB5CA).copy(alpha = alpha)
        scope.pRect(c, Offset(hx - 2 * s, hy), Size(2 * s, 2 * s))
        scope.pRect(c, Offset(hx + s, hy), Size(2 * s, 2 * s))
        scope.pRect(c, Offset(hx - 3 * s, hy + s), Size(7 * s, 2 * s))
        scope.pRect(c, Offset(hx - 2 * s, hy + 3 * s), Size(5 * s, s))
        scope.pRect(c, Offset(hx - s, hy + 4 * s), Size(3 * s, s))
    }

    // Window frame, mullions and a deep sill
    val frame = Color(0xFF32221A)
    val oak = Color(0xFF6A432E)
    scope.px(frame, win.left, win.top, win.width, 3f * p)
    scope.px(frame, win.left, win.bottom - 3f * p, win.width, 3f * p)
    scope.px(frame, win.left, win.top, 3f * p, win.height)
    scope.px(frame, win.right - 3f * p, win.top, 3f * p, win.height)
    scope.px(oak, win.left + 3f * p, win.top + 3f * p, win.width - 6f * p, 3f * p)
    scope.px(oak, win.left + 3f * p, win.top + 3f * p, 3f * p, win.height - 6f * p)
    scope.px(oak, win.right - 6f * p, win.top + 3f * p, 3f * p, win.height - 6f * p)
    scope.px(oak, win.left + win.width * 0.5f - 2f * p, win.top + 6f * p, 4f * p, win.height - 12f * p)
    scope.px(oak, win.left + 6f * p, win.top + win.height * 0.42f - 2f * p, win.width - 12f * p, 4f * p)
    val sillY = win.bottom - 1f * p
    scope.px(Color(0xFF8A5B3C), win.left - 4f * p, sillY, win.width + 8f * p, 3.5f * p)
    scope.px(Color(0xFFA9744F), win.left - 4f * p, sillY, win.width + 8f * p, 1f * p)
    scope.px(Color(0x55000000), win.left - 2f * p, sillY + 3.5f * p, win.width + 4f * p, 1.5f * p)
    // On the sill: a succulent, a jar candle (lit in the evening) and two books
    val potX = win.left + 10f * p
    scope.px(Color(0xFFC87652), potX, sillY - 5f * p, 7f * p, 5f * p)
    scope.px(Color(0xFF9E5A3E), potX - 0.5f * p, sillY - 5f * p, 8f * p, 1.2f * p)
    scope.px(Color(0xFF6FA36A), potX + 1f * p, sillY - 8f * p, 2f * p, 3f * p)
    scope.px(Color(0xFF8CC084), potX + 3f * p, sillY - 9f * p, 2f * p, 4f * p)
    scope.px(Color(0xFF6FA36A), potX + 5f * p, sillY - 7.5f * p, 2f * p, 2.5f * p)
    val candleX = win.right - 20f * p
    scope.px(Color(0x99E8D5B5), candleX, sillY - 6f * p, 6f * p, 6f * p)
    scope.px(Color(0xFFF4E9D8), candleX + 1f * p, sillY - 4.5f * p, 4f * p, 4.5f * p)
    if (lampsBright) {
        val fl = sin(time * 9f) * 0.4f * p
        scope.pCircle(Color(0xFFFFC46B).copy(alpha = 0.25f), 7f * p, Offset(candleX + 3f * p, sillY - 7f * p))
        scope.px(Color(0xFFFFB347), candleX + 2.4f * p, sillY - 7.5f * p + fl, 1.2f * p, 2.2f * p)
    }
    scope.px(Color(0xFF3D5A80), win.right - 34f * p, sillY - 3f * p, 9f * p, 3f * p)
    scope.px(Color(0xFFB5838D), win.right - 33f * p, sillY - 5.5f * p, 8f * p, 2.5f * p)

    // 5. Left wall: wall clock showing the real time, menu board, shelves
    val barX = CafeLayout.barX(cw)
    val barW = CafeLayout.barW(cw)
    val barY = CafeLayout.barY(ch, p)
    val clockC = Offset(barX + barW * 0.5f, ch * 0.13f)
    val clockR = 12f * p
    scope.pCircle(Color(0xFF8C6239), clockR + 1.5f * p, clockC)
    scope.pCircle(Color(0xFFF6EEDC), clockR, clockC)
    for (h in 0 until 12) {
        val a = h * (kotlin.math.PI.toFloat() / 6f)
        val len = if (h % 3 == 0) 2.2f * p else 1.2f * p
        val ox = sin(a)
        val oy = -cos(a)
        scope.pLine(Color(0xFF3B2A20), Offset(clockC.x + ox * (clockR - len - 1f * p), clockC.y + oy * (clockR - len - 1f * p)), Offset(clockC.x + ox * (clockR - 1f * p), clockC.y + oy * (clockR - 1f * p)), strokeWidth = 0.8f * p)
    }
    val daySec = localDaySeconds()
    val minuteA = (daySec % 3600f) / 3600f * 2f * kotlin.math.PI.toFloat()
    val hourA = ((daySec / 3600f) % 12f) / 12f * 2f * kotlin.math.PI.toFloat()
    val secondA = (daySec % 60f) / 60f * 2f * kotlin.math.PI.toFloat()
    scope.pLine(Color(0xFF2B1D16), clockC, Offset(clockC.x + sin(hourA) * clockR * 0.5f, clockC.y - cos(hourA) * clockR * 0.5f), strokeWidth = 1.4f * p)
    scope.pLine(Color(0xFF2B1D16), clockC, Offset(clockC.x + sin(minuteA) * clockR * 0.78f, clockC.y - cos(minuteA) * clockR * 0.78f), strokeWidth = 0.9f * p)
    scope.pLine(Color(0xFFC0392B), clockC, Offset(clockC.x + sin(secondA) * clockR * 0.82f, clockC.y - cos(secondA) * clockR * 0.82f), strokeWidth = 0.45f * p)
    scope.pCircle(Color(0xFF2B1D16), 0.9f * p, clockC)

    // Chalkboard menu hanging above the counter
    val menu = CafeLayout.menuTopLeft(cw, ch, p)
    val menuW = 32f * p
    val menuH = 42f * p
    scope.pLine(Color(0xFF2B1D16), Offset(menu.x + 6f * p, menu.y), Offset(menu.x + menuW / 2f, menu.y - 6f * p), strokeWidth = 0.7f * p)
    scope.pLine(Color(0xFF2B1D16), Offset(menu.x + menuW - 6f * p, menu.y), Offset(menu.x + menuW / 2f, menu.y - 6f * p), strokeWidth = 0.7f * p)
    scope.px(Color(0xFF6B4423), menu.x, menu.y, menuW, menuH)
    scope.px(Color(0xFF1F2A2E), menu.x + 2.5f * p, menu.y + 2.5f * p, menuW - 5f * p, menuH - 5f * p)
    scope.px(Color(0xFFFFE8A3), menu.x + 8f * p, menu.y + 5.5f * p, menuW - 16f * p, 2f * p)
    for (line in 0..4) {
        val ly = menu.y + 12f * p + line * 5.5f * p
        val lw = (menuW - 18f * p) * (0.55f + 0.35f * nz(line, 41))
        scope.px(Color(0xFFE2EFF2).copy(alpha = 0.78f), menu.x + 5f * p, ly, lw, 1.2f * p)
        scope.px(Color(0xFFFFC6A8), menu.x + menuW - 9f * p, ly, 4f * p, 1.2f * p)
    }
    scope.px(Color(0xFFF4F1EA), menu.x + 6f * p, menu.y + 36f * p, 5f * p, 3.5f * p)
    scope.px(Color(0xFFF4F1EA), menu.x + 11f * p, menu.y + 37f * p, 1.2f * p, 2f * p)
    scope.pCircle(Color(0xFFFF9AA2), 1.2f * p, Offset(menu.x + menuW - 8f * p, menu.y + 37.5f * p))
    scope.px(Color(0x33FFFFFF), menu.x + 3f * p, menu.y + menuH - 4f * p, menuW - 6f * p, 1f * p)

    // Two wall shelves: coffee bean jars, stacked cups, a trailing pothos
    for (shelf in 0..1) {
        val sy = ch * (0.40f + shelf * 0.075f)
        scope.px(Color(0xFF6B4423), barX, sy, barW, 2.5f * p)
        scope.px(Color(0xFF3B2618), barX + 3f * p, sy + 2.5f * p, 1.5f * p, 3f * p)
        scope.px(Color(0xFF3B2618), barX + barW - 4.5f * p, sy + 2.5f * p, 1.5f * p, 3f * p)
        var ix = barX + 3f * p
        var item = 0
        while (ix < barX + barW - 10f * p) {
            when ((item + shelf * 2) % 4) {
                0 -> { // glass jar of beans
                    scope.px(Color(0x88D8E8EC), ix, sy - 9f * p, 7f * p, 9f * p)
                    scope.px(Color(0xFF5B3A23), ix + 0.8f * p, sy - 6f * p, 5.4f * p, 6f * p)
                    scope.px(Color(0xFF8B5A2B), ix - 0.4f * p, sy - 10f * p, 7.8f * p, 1.6f * p)
                }
                1 -> { // stack of cups
                    for (cup in 0..2) scope.px(if (cup % 2 == 0) Color(0xFFF4F1EA) else Color(0xFF7FB7BE), ix, sy - (cup + 1) * 3f * p, 6f * p, 2.6f * p)
                }
                2 -> { // trailing pothos in a little pot
                    scope.px(Color(0xFFE9E1D2), ix, sy - 5f * p, 6f * p, 5f * p)
                    val sway = sin(time * 1.3f + shelf) * 0.8f * p
                    for (v in 0..4) scope.px(if (v % 2 == 0) Color(0xFF4F8A55) else Color(0xFF6FAE68), ix + 1f * p + (v % 2) * 3f * p + sway, sy - 7f * p + v * 3.2f * p, 2.4f * p, 2f * p)
                }
                else -> { // books
                    scope.px(Color(0xFF9C6644), ix, sy - 8f * p, 2.2f * p, 8f * p)
                    scope.px(Color(0xFF457B9D), ix + 2.4f * p, sy - 7f * p, 2.2f * p, 7f * p)
                    scope.px(Color(0xFFE9C46A), ix + 4.8f * p, sy - 8.5f * p, 2.2f * p, 8.5f * p)
                }
            }
            ix += 11f * p
            item++
        }
    }

    // 6. Hanging pendant lamps: one over the counter, one over the couple's table
    drawPendantLamp(scope, barX + barW * 0.30f, barY - 34f * p, p, time, lampsBright)
    drawPendantLamp(scope, cw * 0.5f, CafeLayout.tableY(ch, p) - 44f * p, p, time, lampsBright)

    // 7. Leo the barista behind a waist-high counter
    val leo = CafeLayout.barista(cw, ch, p)
    // After hours (plan 09, I) he wipes the counter on even hours and reads on odd ones
    val afterHours = when {
        engine.cafeOpen -> 0
        engine.clockHour % 2 == 0 -> 1
        else -> 2
    }
    drawBarista(scope, leo.x, leo.y, CafeLayout.baristaScale(p), time, engine.cafeBaristaBrewTimer > 0f, afterHours)
    drawOpenSign(scope, CafeLayout.window(cw, ch), p, engine.cafeOpen)
    val counterBase = ch * (CafeLayout.WALL_BOTTOM + 0.01f)
    scope.px(Color(0xFF5A3825), barX, barY, barW, counterBase - barY)
    var slat = barX + 3f * p
    while (slat < barX + barW - 2f * p) {
        scope.px(Color(0xFF4A2D1D), slat, barY + 4f * p, 0.8f * p, counterBase - barY - 7f * p)
        slat += 5f * p
    }
    scope.px(Color(0xFF2E1D15), barX, counterBase - 2.5f * p, barW, 2.5f * p)
    scope.px(Color(0xFFE6DDD0), barX - 2f * p, barY - 2.5f * p, barW + 4f * p, 3f * p)
    scope.px(Color(0xFFCFC3B3), barX + barW * 0.2f, barY - 1.8f * p, barW * 0.25f, 0.6f * p)
    // Espresso machine with its own steam wisp (and a puff while Leo brews)
    val espX = barX + 3f * p
    val espY = barY - 18.5f * p
    scope.pRoundRect(Color(0xFFB0BEC5), Offset(espX, espY), Size(20f * p, 16f * p), CornerRadius(2.5f * p, 2.5f * p))
    scope.px(Color(0xFFCFD8DC), espX + 2f * p, espY + 2f * p, 16f * p, 5f * p)
    scope.pCircle(Color(0xFFE9C46A), 2f * p, Offset(espX + 5f * p, espY + 4.5f * p))
    scope.px(Color(0xFF37474F), espX + 9f * p, espY + 9f * p, 7f * p, 2.5f * p)
    scope.px(Color(0xFFF4F1EA), espX + 11f * p, espY + 12f * p, 3f * p, 3f * p)
    val steamRise = (time * 16f) % (18f * p)
    scope.pCircle(Color.White.copy(alpha = 0.5f), 2f * p, Offset(espX + 19f * p + sin(time * 3f) * 2f * p, espY + 4f * p - steamRise))
    if (engine.cafeBaristaBrewTimer > 0f) {
        for (st in 0..3) {
            val sx = espX + 17f * p + sin(time * 6f + st) * 3f * p
            val sy = espY + 2f * p - ((time * 24f + st * 6f) % (24f * p))
            scope.pCircle(Color(0xFFFFD166).copy(alpha = 0.5f), 3f * p, Offset(sx, sy))
        }
    }
    // Cake under a glass dome, tip jar and a little bell
    val domeX = barX + barW - 18f * p
    scope.px(Color(0xFFDBC8B0), domeX - 1f * p, barY - 3f * p, 12f * p, 1.2f * p)
    scope.px(Color(0xFFF7D9C4), domeX + 1f * p, barY - 7.5f * p, 8f * p, 4.5f * p)
    scope.px(Color(0xFFE5989B), domeX + 1f * p, barY - 7.5f * p, 8f * p, 1.5f * p)
    scope.pCircle(Color(0xFFD62828), 0.9f * p, Offset(domeX + 5f * p, barY - 8.5f * p))
    scope.pArc(Color(0x66E0F2F7), 180f, 180f, true, Offset(domeX - 0.5f * p, barY - 14f * p), Size(11f * p, 22f * p))
    scope.px(Color(0x88D8E8EC), domeX - 9f * p, barY - 7f * p, 5f * p, 6f * p)
    scope.px(Color(0xFFE9C46A), domeX - 8f * p, barY - 3f * p, 3f * p, 1.2f * p)

    // 8. Velvet booth the couple sits on, with a round rug beneath the table
    val rugC = Offset(cw * 0.5f, feetY + 7f * p)
    scope.pOval(Color(0xFF7D4E57), Offset(rugC.x - 48f * p, rugC.y - 7f * p), Size(96f * p, 16f * p))
    scope.pOval(Color(0xFFC9A66B), Offset(rugC.x - 44f * p, rugC.y - 5.5f * p), Size(88f * p, 13f * p))
    scope.pOval(Color(0xFF9E6A73), Offset(rugC.x - 36f * p, rugC.y - 4f * p), Size(72f * p, 10f * p))
    val boothW = 64f * p
    val boothL = cw * 0.5f - boothW / 2f
    scope.pRoundRect(Color(0xFF2E5B55), Offset(boothL, feetY - 25f * p), Size(boothW, 19f * p), CornerRadius(3f * p, 3f * p))
    for (bt in 0..6) scope.pCircle(Color(0xFF1F4440), 0.7f * p, Offset(boothL + (5f + bt * 9f) * p, feetY - 18f * p))
    scope.px(Color(0xFF3B746C), boothL + 2f * p, feetY - 24f * p, boothW - 4f * p, 1.5f * p)
    scope.pRoundRect(Color(0xFF3B746C), Offset(boothL - 1f * p, feetY - 8f * p), Size(boothW + 2f * p, 4.5f * p), CornerRadius(2f * p, 2f * p))
    scope.px(Color(0xFF5A3825), boothL, feetY - 3.5f * p, boothW, 4f * p)
    scope.px(Color(0xFF2E1D15), boothL + 2f * p, feetY + 0.5f * p, 2f * p, 1.5f * p)
    scope.px(Color(0xFF2E1D15), boothL + boothW - 4f * p, feetY + 0.5f * p, 2f * p, 1.5f * p)

    // 9. A tall potted fiddle-leaf fig and a coat stand with a dripping umbrella
    drawFiddleFig(scope, cw * 0.70f, ch * 0.672f, p, time, scale = 0.9f)
    val rackX = cw * 0.955f
    val rackBase = ch * 0.705f
    scope.px(Color(0xFF3B2618), rackX - 0.9f * p, rackBase - 44f * p, 1.8f * p, 44f * p)
    scope.px(Color(0xFF3B2618), rackX - 6f * p, rackBase - 1.5f * p, 12f * p, 1.5f * p)
    scope.px(Color(0xFFB5838D), rackX - 4f * p, rackBase - 41f * p, 3f * p, 14f * p)
    scope.px(Color(0xFFE9C46A), rackX + 1f * p, rackBase - 40f * p, 3f * p, 9f * p)
    scope.px(Color(0xFF5E6472), rackX - 9f * p, rackBase - 9f * p, 6f * p, 9f * p)
    scope.pLine(Color(0xFF1D3557), Offset(rackX - 6f * p, rackBase - 9f * p), Offset(rackX - 4f * p, rackBase - 24f * p), strokeWidth = 1.4f * p)
    scope.pArc(Color(0xFF1D3557), 200f, 140f, false, Offset(rackX - 5f * p, rackBase - 26f * p), Size(2.5f * p, 3f * p), style = Stroke(0.8f * p))
    scope.pOval(Color(0x553D5A80), Offset(rackX - 12f * p, rackBase - 1f * p), Size(12f * p, 3f * p))
    val drip = (time * 0.9f) % 1f
    scope.px(Color(0xFFBFE3EE), rackX - 6.5f * p, rackBase - 1f * p - (1f - drip) * 4f * p, 0.8f * p, 1.4f * p)

    // 10. Boba the pup napping on his rug, near Mochi
    val rug = CafeLayout.rug(cw, ch)
    val rugW = 44f * p
    val rugH = 18f * p
    scope.pOval(Color(0xFFE9D8A6), Offset(rug.x - rugW / 2f, rug.y), Size(rugW, rugH))
    scope.pOval(Color(0xFFD4A373), Offset(rug.x - rugW / 2f + 2f * p, rug.y + p), Size(rugW - 4f * p, rugH - 2f * p))
    scope.pOval(Color(0xFFCCD5AE), Offset(rug.x - rugW / 2f + 4f * p, rug.y + 2f * p), Size(rugW - 8f * p, rugH - 4f * p))
    scope.px(Color(0xFF8E9AAF), rug.x + 16f * p, rug.y + 4f * p, 6f * p, 2.5f * p) // water bowl
    scope.px(Color(0xFFBFE3EE), rug.x + 16.8f * p, rug.y + 4f * p, 4.4f * p, 0.8f * p)
    val pup = CafeLayout.pup(cw, ch, p)
    val pupX = pup.x
    val pupY = pup.y
    val pupBody = Color(0xFFE0A96D)
    scope.pOval(pupBody, Offset(pupX - 7f * p, pupY - 4f * p), Size(14f * p, 9f * p))
    scope.pOval(Color(0xFFC68B45), Offset(pupX - 5f * p, pupY - 2f * p), Size(10f * p, 6f * p))
    scope.pCircle(pupBody, 4f * p, Offset(pupX - 4f * p, pupY - 1f * p))
    scope.pOval(Color(0xFFA66E38), Offset(pupX - 8f * p, pupY - 3f * p), Size(3.5f * p, 5f * p))
    scope.pOval(Color(0xFFA66E38), Offset(pupX - 2f * p, pupY - 4f * p), Size(3.5f * p, 4.5f * p))
    scope.pRect(Color(0xFFE63946), Offset(pupX - 3.5f * p, pupY + 2f * p), Size(4f * p, 1.8f * p))
    scope.pArc(Color(0xFF3D210F), 180f, 180f, false, Offset(pupX - 5.5f * p, pupY - 2f * p), Size(2.5f * p, 1.8f * p), style = Stroke(0.9f * p))
    val tailWag = if (engine.cafePupPetTimer > 0f) sin(time * 24f) * 5f * p else sin(time * 3f) * 1.5f * p
    scope.pLine(pupBody, Offset(pupX + 6f * p, pupY), Offset(pupX + 10f * p, pupY - 3f * p + tailWag), strokeWidth = 2.4f * p)
    if (engine.cafePupPetTimer > 0f) {
        scope.pCircle(Color(0xFFFFCAD4), 2.5f * p, Offset(pupX, pupY - 12f * p))
    } else {
        val zFall = (time * 0.4f) % 1f
        scope.pCircle(Color(0xFFBDE0FE).copy(alpha = 0.6f - zFall * 0.5f), (1.2f + zFall) * p, Offset(pupX - 2f * p + sin(time * 2f) * 2f * p, pupY - 8f * p - zFall * 12f * p))
    }

    // 11. Front of the room (nearer the viewer, so drawn larger): a light pool under the lamp,
    // two neighbouring tables and a bookcase at the edge
    if (lampsBright) scope.pOval(Color(0xFFFFD27A).copy(alpha = 0.07f), Offset(cw * 0.5f - 60f * p, feetY + 2f * p), Size(120f * p, 22f * p))
    drawCafeTableSet(scope, cw * 0.70f, ch * 0.88f, p * 1.7f, time, lampsBright, reserved = true)
    drawCafeTableSet(scope, cw * 0.22f, ch * 0.80f, p * 1.45f, time, lampsBright, reserved = false)
    drawCafeBookcase(scope, cw, ch * 0.74f, ch * 0.74f, ch, p)

    // Evening: the room dims a touch so the lamps and candle carry the light
    if (night) scope.px(Color(0x1E05070F), 0f, 0f, cw, ch)
}

/**
 * The cafe's plank floor, rows widening toward the viewer from [wallBottom], drawn between
 * [fromY] and [toY]. The floor continued below the stage calls this too, so its rows carry on.
 */
internal fun drawCafePlanks(scope: DrawScope, cw: Float, wallBottom: Float, fromY: Float, toY: Float, p: Float) {
    var fy = wallBottom
    var r = 0
    while (fy < toY) {
        val rowH = (4.5f + r * 0.9f) * p
        if (fy + rowH > fromY) {
            val top = maxOf(fy, fromY)
            val h = minOf(fy + rowH, toY) - top
            scope.px(if (r % 2 == 0) Color(0xFF6B4630) else Color(0xFF5E3D29), 0f, top, cw, h)
            val seamY = fy + rowH - 0.7f * p
            if (seamY >= fromY && seamY < toY) scope.px(Color(0xFF3B2618), 0f, seamY, cw, 0.7f * p)
            val seamGap = (26f + r * 5f) * p
            var sx = nz(r, 11) * seamGap
            while (sx < cw) {
                scope.px(Color(0xFF3B2618), sx, top, 0.7f * p, h)
                sx += seamGap
            }
            val sheenY = fy + rowH * 0.35f
            if (sheenY >= fromY && sheenY < toY) scope.px(Color(0x14FFFFFF), cw * nz(r, 5), sheenY, cw * 0.18f, 0.6f * p)
        }
        fy += rowH
        r++
    }
}

/**
 * The tall bookcase at the cafe's right edge, starting at [top], drawn between [fromY] and
 * [toY]. Below the stage it carries on with the same shelves.
 */
internal fun drawCafeBookcase(scope: DrawScope, cw: Float, top: Float, fromY: Float, toY: Float, p: Float) {
    val bcX = cw - 24f * p
    scope.px(Color(0xFF4A2D1D), bcX, fromY, 26f * p, toY - fromY)
    var shelfRow = 0
    var sY = top + 4f * p
    while (sY < toY - 6f * p) {
        if (sY + 17f * p > fromY) {
            var bkX = bcX + 2f * p
            var bk = 0
            while (bkX < cw) {
                val h = (9f + nz(shelfRow * 13 + bk, 191) * 5f) * p
                val c = when ((shelfRow + bk) % 5) {
                    0 -> Color(0xFF9C6644)
                    1 -> Color(0xFF457B9D)
                    2 -> Color(0xFFE9C46A)
                    3 -> Color(0xFF6B9080)
                    else -> Color(0xFFB5838D)
                }
                val bookTop = maxOf(sY + 14f * p - h, fromY)
                scope.px(c, bkX, bookTop, 3.2f * p, sY + 14f * p - bookTop)
                bkX += 3.6f * p
                bk++
            }
            val boardTop = maxOf(sY + 14f * p, fromY)
            scope.px(Color(0xFF6B4423), bcX, boardTop, 26f * p, sY + 16.5f * p - boardTop)
        }
        sY += 17f * p
        shelfRow++
    }
}

/** Brass pendant lamp; its pool of light is stronger once it's dark outside. */
private fun drawPendantLamp(scope: DrawScope, x: Float, bottomY: Float, p: Float, time: Float, bright: Boolean) {
    scope.px(Color(0xFF1E140F), x - 0.4f * p, 8f * p, 0.8f * p, bottomY - 8f * p - 6f * p)
    scope.pArc(Color(0xFF2F5D57), 180f, 180f, true, Offset(x - 7f * p, bottomY - 8f * p), Size(14f * p, 12f * p))
    scope.px(Color(0xFFB07D38), x - 1.5f * p, bottomY - 9f * p, 3f * p, 2f * p)
    val flicker = sin(time * 7f) * 0.6f * p
    val a = if (bright) 0.30f else 0.14f
    // Light cone spreading downwards
    for (k in 0 until 8) {
        val t = k / 8f
        val hw = (6f + t * 26f) * p
        scope.px(Color(0xFFFFE2A0).copy(alpha = a * (1f - t) * 0.35f), x - hw, bottomY - 2f * p + t * 40f * p, hw * 2f, 5f * p)
    }
    scope.pCircle(Color(0xFFFFD166).copy(alpha = a), 10f * p + flicker, Offset(x, bottomY - 1f * p))
    scope.pCircle(Color(0xFFFFF2C2), 1.8f * p, Offset(x, bottomY - 1.5f * p))
}

private fun drawBarista(scope: DrawScope, x: Float, feetY: Float, p: Float, time: Float, brewing: Boolean, afterHours: Int = 0) {
    if (afterHours != 0) {
        drawBaristaAfterHours(scope, x, feetY, p, time, reading = afterHours == 2)
        return
    }
    val bob = if (brewing) sin(time * 10f) * 0.5f * p else 0f
    val top = feetY - 29f * p + bob
    scope.pCircle(Color(0xFFFFD8B8), 4.5f * p, Offset(x, top + 5f * p))
    scope.px(Color(0xFF4A3425), x - 5f * p, top, 10f * p, 3.5f * p)
    scope.px(Color(0xFF4A3425), x - 5.5f * p, top + 1.5f * p, 2f * p, 4f * p)
    scope.px(Color(0xFF2C190D), x - 2.5f * p, top + 4.5f * p, 1.2f * p, 1.4f * p)
    scope.px(Color(0xFF2C190D), x + 1.3f * p, top + 4.5f * p, 1.2f * p, 1.4f * p)
    scope.px(Color(0xFFFFAAA6), x - 3.5f * p, top + 6.5f * p, 1.5f * p, 0.9f * p)
    scope.px(Color(0xFFFFAAA6), x + 2f * p, top + 6.5f * p, 1.5f * p, 0.9f * p)
    scope.px(Color(0xFF8D5524), x - 0.8f * p, top + 7.5f * p, 1.6f * p, 0.6f * p)
    scope.px(Color(0xFFFFF1E6), x - 5f * p, top + 10f * p, 10f * p, 19f * p)
    scope.px(Color(0xFF38523A), x - 4f * p, top + 12f * p, 8f * p, 17f * p)
    scope.px(Color(0xFFE9C46A), x - 1f * p, top + 15f * p, 2f * p, 2f * p)
    scope.px(Color(0xFFFFD8B8), x + 5f * p, top + 13f * p + (if (brewing) sin(time * 12f) * p else 0f), 2.2f * p, 2.2f * p)
}

/**
 * Leo after hours (plan 09, I): looking down at the counter, either wiping it in slow circles with
 * a cloth or reading a paperback propped on it.
 */
private fun drawBaristaAfterHours(scope: DrawScope, x: Float, feetY: Float, p: Float, time: Float, reading: Boolean) {
    val top = feetY - 29f * p
    scope.pCircle(Color(0xFFFFD8B8), 4.5f * p, Offset(x, top + 5.5f * p))
    scope.px(Color(0xFF4A3425), x - 5f * p, top + 0.5f * p, 10f * p, 3.5f * p)
    scope.px(Color(0xFF4A3425), x - 5.5f * p, top + 2f * p, 2f * p, 4f * p)
    // Eyes down
    scope.px(Color(0xFF2C190D), x - 2.5f * p, top + 6f * p, 1.4f * p, 0.6f * p)
    scope.px(Color(0xFF2C190D), x + 1.3f * p, top + 6f * p, 1.4f * p, 0.6f * p)
    scope.px(Color(0xFFFFAAA6), x - 3.5f * p, top + 7f * p, 1.5f * p, 0.9f * p)
    scope.px(Color(0xFFFFAAA6), x + 2f * p, top + 7f * p, 1.5f * p, 0.9f * p)
    scope.px(Color(0xFFFFF1E6), x - 5f * p, top + 10f * p, 10f * p, 19f * p)
    scope.px(Color(0xFF38523A), x - 4f * p, top + 12f * p, 8f * p, 17f * p)
    scope.px(Color(0xFFE9C46A), x - 1f * p, top + 15f * p, 2f * p, 2f * p)
    if (reading) {
        // A paperback held up open in front of him, above the cups and cakes on the counter
        scope.px(Color(0xFF4A7C9B), x - 1f * p, top + 11f * p, 10f * p, 5f * p)
        scope.px(Color(0xFFFFF8E7), x - 0.5f * p, top + 11.5f * p, 4.2f * p, 4f * p)
        scope.px(Color(0xFFFFF8E7), x + 4.3f * p, top + 11.5f * p, 4.2f * p, 4f * p)
        scope.px(Color(0x55000000), x + 0.5f * p, top + 12.5f * p, 2.5f * p, 0.5f * p)
        scope.px(Color(0x55000000), x + 5f * p, top + 12.5f * p, 2.5f * p, 0.5f * p)
        scope.px(Color(0xFFFFD8B8), x - 2f * p, top + 13f * p, 1.6f * p, 1.6f * p)
        scope.px(Color(0xFFFFD8B8), x + 8.6f * p, top + 13f * p, 1.6f * p, 1.6f * p)
        // Turning a page now and then
        if ((time % 5f) < 0.4f) scope.px(Color(0xFFFFFFFF), x + 4.3f * p, top + 10.5f * p, 2.5f * p, 4f * p)
    } else {
        // Wiping the open end of the counter in slow circles, arm reaching out
        val wx = x + 10f * p + sin(time * 2.4f) * 2.5f * p
        val wy = top + 18.5f * p + kotlin.math.cos(time * 2.4f) * 0.5f * p
        scope.px(Color(0xFFFFF1E6), x + 5f * p, top + 14f * p, (wx - x - 5f * p).coerceAtLeast(p), 1.6f * p)
        scope.px(Color(0xFFFFD8B8), wx, wy - 2f * p, 2.2f * p, 2.2f * p)
        scope.px(Color(0xFF8ECAE6), wx - 1f * p, wy, 4.5f * p, 1.4f * p)
    }
}

/** The pixel letters for the cafe sign, 3 x 5 each. */
private val SIGN_GLYPHS = mapOf(
    'O' to listOf("###", "#.#", "#.#", "#.#", "###"),
    'P' to listOf("###", "#.#", "###", "#..", "#.."),
    'E' to listOf("###", "#..", "##.", "#..", "###"),
    'N' to listOf("#.#", "###", "###", "###", "#.#"),
    'C' to listOf("###", "#..", "#..", "#..", "###"),
    'L' to listOf("#..", "#..", "#..", "#..", "###"),
    'S' to listOf("###", "#..", "###", "..#", "###"),
    'D' to listOf("##.", "#.#", "#.#", "#.#", "##.")
)

/** A little wooden sign hanging in the window (plan 09, I): OPEN from 7 till 21, then CLOSED. */
private fun drawOpenSign(scope: DrawScope, window: androidx.compose.ui.geometry.Rect, p: Float, open: Boolean) {
    val word = if (open) "OPEN" else "CLOSED"
    val u = p
    val w = (word.length * 4 + 1) * u
    val h = 7f * u
    val left = window.left + 10f * p
    val top = window.top + 24f * p
    // Two strings from the frame
    scope.px(Color(0xFF3B2A1E), left + 2f * u, window.top + 4f * p, 0.5f * u, top - window.top - 4f * p)
    scope.px(Color(0xFF3B2A1E), left + w - 2.5f * u, window.top + 4f * p, 0.5f * u, top - window.top - 4f * p)
    scope.px(Color(0xFF6B4226), left - u, top - u, w + 2f * u, h + 2f * u)
    scope.px(Color(0xFFFFF1E6), left, top, w, h)
    val ink = if (open) Color(0xFF2D6A4F) else Color(0xFFC1121F)
    word.forEachIndexed { i, ch ->
        val glyph = SIGN_GLYPHS[ch] ?: return@forEachIndexed
        for ((gy, row) in glyph.withIndex()) for ((gx, c) in row.withIndex()) {
            if (c == '#') scope.px(ink, left + (1 + i * 4 + gx) * u, top + (1 + gy) * u, u, u)
        }
    }
}

/** A big fiddle-leaf fig in a terracotta pot; leaves sway a little. */
private fun drawFiddleFig(scope: DrawScope, x: Float, baseY: Float, p: Float, time: Float, scale: Float) {
    val s = p * scale
    scope.px(Color(0xFFB8653F), x - 7f * s, baseY - 11f * s, 14f * s, 11f * s)
    scope.px(Color(0xFF9E5235), x - 8f * s, baseY - 12f * s, 16f * s, 2.5f * s)
    scope.px(Color(0xFF5B3A23), x - 6f * s, baseY - 12.5f * s, 12f * s, 1f * s)
    scope.px(Color(0xFF6B4A2E), x - 0.8f * s, baseY - 40f * s, 1.6f * s, 28f * s)
    for (leaf in 0..8) {
        val ly = baseY - 44f * s + leaf * 3.6f * s
        val side = if (leaf % 2 == 0) -1f else 1f
        val sway = sin(time * 1.1f + leaf * 0.9f) * 0.8f * s
        val lx = x + side * (2f + (leaf % 3) * 1.5f) * s + sway
        scope.pOval(if (leaf % 3 == 0) Color(0xFF3F7D4E) else Color(0xFF5A9A5F), Offset(lx - 4f * s, ly - 2.5f * s), Size(8f * s, 5.5f * s))
        scope.px(Color(0x332B5A35), lx - 0.3f * s, ly - 2f * s, 0.6f * s, 4.5f * s)
    }
}

/**
 * The cafe table in front of the seated couple, drawn after the characters so it hides their
 * laps like a real table: heart latte, bud vase and the croissant they share.
 */
fun drawCafeTableForeground(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    val tx = CafeLayout.tableX(cw, p)
    val ty = CafeLayout.tableY(ch, p)
    val tw = CafeLayout.tableW(p)
    val cx = tx + tw / 2f
    val floorY = ch * CafeLayout.SEAT_FEET_Y + 9f * p
    scope.px(Color(0xFF2B1D16), cx - 1.8f * p, ty + 3f * p, 3.6f * p, floorY - ty - 3f * p)
    scope.pRoundRect(Color(0xFF2B1D16), Offset(cx - 10f * p, floorY - 2f * p), Size(20f * p, 2.5f * p), CornerRadius(1.2f * p, 1.2f * p))
    scope.pOval(Color(0x33000000), Offset(cx - 12f * p, floorY), Size(24f * p, 3f * p))
    scope.pOval(Color(0xFF5A3825), Offset(tx, ty - 1f * p), Size(tw, 6.5f * p))
    scope.pOval(Color(0xFF8B5A2B), Offset(tx + 1f * p, ty - 1.5f * p), Size(tw - 2f * p, 5f * p))
    scope.pOval(Color(0x22FFFFFF), Offset(tx + 6f * p, ty - 1f * p), Size(tw * 0.4f, 1.6f * p))

    // Heart-foam latte on a saucer
    val latte = CafeLayout.latte(cw, ch, p)
    val latteX = latte.x
    val latteY = latte.y
    scope.pOval(Color(0xFFEAE2B7), Offset(latteX - 6f * p, latteY + 2.5f * p), Size(12f * p, 3f * p))
    scope.pRoundRect(Color(0xFFFAF0CA), Offset(latteX - 4.5f * p, latteY - 2.5f * p), Size(9f * p, 6f * p), CornerRadius(1.5f * p, 1.5f * p))
    scope.px(Color(0xFF6F4E37), latteX - 3.5f * p, latteY - 2f * p, 7f * p, 2f * p)
    scope.pArc(Color(0xFFFAF0CA), 270f, 180f, false, Offset(latteX + 3f * p, latteY - 1.5f * p), Size(3.5f * p, 4f * p), style = Stroke(1.2f * p))
    val hs = 0.9f * p * (1f + 0.4f * (engine.cafeLatteTimer / 2.2f).coerceIn(0f, 1f))
    val hy = latteY - 1.6f * p
    val heart = Color(0xFFF7E1C6)
    scope.px(heart, latteX - 2 * hs, hy, 2 * hs, hs)
    scope.px(heart, latteX + 0.2f * hs, hy, 2 * hs, hs)
    scope.px(heart, latteX - 1.4f * hs, hy + hs, 3.2f * hs, hs * 0.8f)
    for (i in 0..2) {
        val sway = sin(time * 2.4f + i) * 1.5f * p
        val steamY = latteY - 5f * p - ((time * 8f + i * 5f) % (10f * p))
        scope.pCircle(Color.White.copy(alpha = 0.5f), 1.2f * p, Offset(latteX + (i - 1) * 2f * p + sway, steamY))
    }

    // Bud vase with a daisy in the middle
    scope.pRoundRect(Color(0x88A8DADC), Offset(cx - 1.8f * p, ty - 6f * p), Size(3.6f * p, 5.5f * p), CornerRadius(1.5f * p, 1.5f * p))
    scope.pLine(Color(0xFF2A9D8F), Offset(cx, ty - 5.5f * p), Offset(cx + 0.5f * p, ty - 10f * p), strokeWidth = 0.8f * p)
    scope.pCircle(Color.White, 1.6f * p, Offset(cx + 0.5f * p, ty - 10.5f * p))
    scope.pCircle(Color(0xFFE9C46A), 0.8f * p, Offset(cx + 0.5f * p, ty - 10.5f * p))

    // Croissant on a plate with a dab of jam (shrinks with every bite)
    val plate = CafeLayout.plate(cw, ch, p)
    scope.pOval(Color(0xFFF1FAEE), Offset(plate.x - 7f * p, plate.y + 1.5f * p), Size(14f * p, 3.5f * p))
    val croissantW = CafeLayout.croissantWidth(engine.cafePastryBites, p)
    if (croissantW > 2f * p) {
        scope.pRoundRect(Color(0xFFD9822B), Offset(plate.x - croissantW / 2f, plate.y - 1.5f * p), Size(croissantW, 4f * p), CornerRadius(2f * p, 2f * p))
        scope.pRoundRect(Color(0xFFF4A261), Offset(plate.x - croissantW / 2f + p, plate.y - 0.8f * p), Size(croissantW - 2f * p, 2f * p), CornerRadius(1f * p, 1f * p))
    }
    scope.pCircle(Color(0xFFE63946), 1.4f * p, Offset(plate.x + 5f * p, plate.y + 2.5f * p))
}

// ─────────────────────────────────────────────────────────────────────────────
// COTTAGE SUNROOM — a glass conservatory looking out on the garden
// ─────────────────────────────────────────────────────────────────────────────

fun drawCottageSunroom(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    val phase = engine.timeOfDayPhase
    val night = phase.isNight
    val dusk = phase.isSunset
    val morning = phase.isMorning
    val weather = engine.weather
    val raining = weather == WeatherType.RAIN
    val snowing = weather == WeatherType.SNOW
    val roofEave = ch * 0.20f
    val kneeTop = ch * 0.575f
    val floorTop = ch * 0.645f

    // 1. Outside, seen through the glass: the real sky, then the garden
    drawSkyAndClouds(scope, cw, ch, night, dusk, morning, time, p, weather = weather)
    val hillFar = when { night -> Color(0xFF1C2A3A); dusk -> Color(0xFF7A6A86); else -> Color(0xFF8DB596) }
    val hillNear = when { night -> Color(0xFF1A3328); dusk -> Color(0xFF5E6B5A); else -> Color(0xFF6E9E68) }
    val lawn = when { snowing -> Color(0xFFE9EEF2); night -> Color(0xFF1E3B2A); dusk -> Color(0xFF5F7A4A); else -> Color(0xFF7FB069) }
    var hx = 0f
    while (hx < cw) {
        val t = hx / cw
        val farH = (10f + 7f * sin(t * 6.3f) + 4f * sin(t * 13f + 2f)) * p
        val nearH = (6f + 5f * sin(t * 9.1f + 1.2f)) * p
        scope.px(if (snowing) Color(0xFFDDE4EA) else hillFar, hx, kneeTop - 30f * p - farH, 3f * p + 0.5f, farH + 30f * p)
        scope.px(if (snowing) Color(0xFFE6ECF0) else hillNear, hx, kneeTop - 20f * p - nearH, 3f * p + 0.5f, nearH + 20f * p)
        hx += 3f * p
    }
    scope.px(lawn, 0f, kneeTop - 16f * p, cw, 16f * p)
    // Round garden trees and a white picket fence
    for (tr in 0..5) {
        val tx = cw * (0.07f + tr * 0.18f) + nz(tr, 51) * 12f * p
        val tBase = kneeTop - 14f * p
        val crown = (9f + nz(tr, 52) * 5f) * p
        scope.px(Color(0xFF5B4632), tx - 1f * p, tBase - crown, 2f * p, crown)
        val leaf = when { snowing -> Color(0xFFCFD8DC); night -> Color(0xFF173226); else -> if (tr % 2 == 0) Color(0xFF4F8A55) else Color(0xFF3F7D4E) }
        scope.pCircle(leaf, crown * 0.75f, Offset(tx, tBase - crown))
        if (!night && !snowing) scope.pCircle(Color(0x33FFFFFF), crown * 0.3f, Offset(tx - crown * 0.25f, tBase - crown * 1.2f))
    }
    val fenceY = kneeTop - 11f * p
    val picket = if (night) Color(0xFF9AA5B4) else Color(0xFFF4F1EA)
    scope.px(picket, 0f, fenceY + 2f * p, cw, 1.2f * p)
    scope.px(picket, 0f, fenceY + 5f * p, cw, 1.2f * p)
    var fx = 1f * p
    while (fx < cw) {
        scope.px(picket, fx, fenceY, 1.8f * p, 8f * p)
        fx += 5f * p
    }
    if (!snowing && !night) {
        for (fl in 0..30) {
            val flx = nz(fl, 61) * cw
            val c = when (fl % 4) { 0 -> Color(0xFFFF8FAB); 1 -> Color(0xFFFFD166); 2 -> Color(0xFFCDB4DB); else -> Color(0xFFFFFFFF) }
            scope.px(c, flx, kneeTop - 4f * p - nz(fl, 62) * 3f * p, 1.4f * p, 1.4f * p)
        }
    }
    // A bird feeder outside; a little bird visits in the daytime
    val feederX = cw * 0.86f
    val feederY = kneeTop - 30f * p
    scope.px(Color(0xFF5B4632), feederX - 0.6f * p, feederY, 1.2f * p, 30f * p)
    scope.px(Color(0xFFB5838D), feederX - 5f * p, feederY - 3f * p, 10f * p, 1.5f * p)
    scope.px(Color(0x99E0F2F7), feederX - 3.5f * p, feederY - 1.5f * p, 7f * p, 4f * p)
    scope.px(Color(0xFFD4A373), feederX - 3f * p, feederY + 0.5f * p, 6f * p, 2f * p)
    if (!night && !raining && (time % 24f) < 10f) {
        val hop = if (((time * 3f).toInt()) % 2 == 0) 0f else 0.6f * p
        scope.px(Color(0xFF6D4C41), feederX + 3f * p, feederY - 1f * p - hop, 3.5f * p, 2.5f * p)
        scope.px(Color(0xFFE76F51), feederX + 3f * p, feederY + 0.2f * p - hop, 2f * p, 1.2f * p)
        scope.px(Color(0xFF212121), feederX + 5.8f * p, feederY - 0.6f * p - hop, 1f * p, 0.7f * p)
    }

    // 2. The conservatory itself: pitched glass roof, glass walls, white-painted frames
    val wood = if (night) Color(0xFFBFC6C9) else Color(0xFFF2EEE6)
    val woodShade = if (night) Color(0xFF8E979C) else Color(0xFFD8D1C3)
    val glassTint = if (night) Color(0x55101828) else Color(0x1AD8F3F7)
    scope.px(glassTint, 0f, 0f, cw, kneeTop)
    for (k in -5..5) {
        val topX = cw * 0.5f + k * cw * 0.055f
        val botX = cw * 0.5f + k * cw * 0.17f
        scope.pLine(woodShade, Offset(topX, 0f), Offset(botX, roofEave), strokeWidth = 2.6f * p)
        scope.pLine(wood, Offset(topX - 0.4f * p, 0f), Offset(botX - 0.4f * p, roofEave), strokeWidth = 1.6f * p)
    }
    scope.px(wood, 0f, 0f, cw, 3f * p)
    scope.px(wood, 0f, roofEave - 2f * p, cw, 4.5f * p)
    scope.px(woodShade, 0f, roofEave + 2.5f * p, cw, 1f * p)
    val transom = roofEave + (kneeTop - roofEave) * 0.24f
    scope.px(wood, 0f, transom, cw, 2.2f * p)
    var mx = cw / 12f
    while (mx < cw) {
        scope.px(woodShade, mx - 1.6f * p, roofEave, 3.2f * p, kneeTop - roofEave)
        scope.px(wood, mx - 1.2f * p, roofEave, 2.4f * p, kneeTop - roofEave)
        mx += cw / 6f
    }
    // Soft diagonal glare on a few panes
    for (g in 0..2) {
        val gx = cw * (0.18f + g * 0.33f)
        for (s in 0 until 10) scope.px(Color.White.copy(alpha = 0.07f), gx + s * 1.8f * p, transom + 6f * p + s * 3f * p, 3f * p, 3f * p)
    }
    // Weather on the glass
    if (raining) {
        for (i in 0..34) {
            val rx = nz(i, 71) * cw
            val fall = (time * (0.09f + nz(i, 72) * 0.07f) + nz(i, 73)) % 1f
            val ry = roofEave + fall * (kneeTop - roofEave)
            scope.px(Color(0xFFDDEFF5).copy(alpha = 0.6f), rx, ry, 0.8f * p, (2.5f + (i % 3)) * p)
        }
        for (i in 0..12) {
            val sx = nz(i, 74) * cw
            val sp = (time * 0.9f + nz(i, 75)) % 1f
            scope.pCircle(Color(0xFFDDEFF5).copy(alpha = 0.5f * (1f - sp)), (0.6f + sp * 2f) * p, Offset(sx, roofEave * nz(i, 76)))
        }
    }
    if (snowing) {
        scope.px(Color(0xFFF7FBFF), 0f, roofEave - 3.5f * p, cw, 2f * p)
        for (i in 0..20) scope.pCircle(Color(0xFFF7FBFF), (1.2f + nz(i, 77)) * p, Offset(nz(i, 78) * cw, roofEave - 2.5f * p))
    }

    // 3. Fairy lights strung along the eave, switched on at dusk and night
    drawFairyLights(scope, 0f, cw, roofEave + 3.5f * p, 6f * p, p, time, 24, lit = night || dusk)

    // 4. Low painted brick knee wall with a deep sill of little pots
    scope.px(Color(0xFFE8DCC8), 0f, kneeTop, cw, floorTop - kneeTop)
    var kr = 0
    var ky = kneeTop + 4f * p
    while (ky < floorTop) {
        scope.px(Color(0xFFD8C9B0), 0f, ky, cw, 0.6f * p)
        var kx = if (kr % 2 == 0) 0f else 6f * p
        while (kx < cw) {
            scope.px(Color(0xFFD8C9B0), kx, ky - 4f * p, 0.6f * p, 4f * p)
            kx += 12f * p
        }
        ky += 4f * p
        kr++
    }
    scope.px(Color(0xFFB08968), 0f, kneeTop - 1.5f * p, cw, 4f * p)
    scope.px(Color(0xFFC9A27E), 0f, kneeTop - 1.5f * p, cw, 1.2f * p)
    for (sp in 0..7) {
        val sx = cw * (0.04f + sp * 0.125f) + nz(sp, 81) * 4f * p
        if (sx > cw * 0.62f && sx < cw * 0.98f) continue // the potting bench sits in front of this stretch
        scope.px(if (sp % 2 == 0) Color(0xFFC87652) else Color(0xFFE9E1D2), sx, kneeTop - 6f * p, 6f * p, 4.5f * p)
        when (sp % 3) {
            0 -> { scope.pCircle(Color(0xFF7FB069), 2.4f * p, Offset(sx + 3f * p, kneeTop - 7.5f * p)); scope.pCircle(Color(0xFF9CC5A1), 1.2f * p, Offset(sx + 3f * p, kneeTop - 8f * p)) }
            1 -> { scope.px(Color(0xFF52796F), sx + 2f * p, kneeTop - 12f * p, 2f * p, 6f * p); scope.px(Color(0xFF52796F), sx + 0.5f * p, kneeTop - 9f * p, 1.5f * p, 2f * p) }
            else -> { scope.px(Color(0xFF6A994E), sx + 1f * p, kneeTop - 9f * p, 4f * p, 3f * p); scope.px(Color(0xFFFFAFCC), sx + 2f * p, kneeTop - 10f * p, 1.5f * p, 1.5f * p) }
        }
    }

    // 5. Sunlight: beams slant in from the side the sun is on (east in the morning, west later)
    val hour = localDaySeconds() / 3600f
    val sunny = !night && !raining && !snowing
    if (sunny) {
        val fromLeft = hour < 13f
        val slope = if (fromLeft) 0.55f else -0.55f
        val beamA = if (dusk) 0.09f else 0.07f
        val beamColor = if (dusk) Color(0xFFFFB37A) else Color(0xFFFFF2B2)
        for (b in 0..2) {
            val startX = cw * (if (fromLeft) 0.08f + b * 0.26f else 0.42f + b * 0.26f)
            var y = roofEave
            while (y < floorTop + 14f * p) {
                val x = startX + (y - roofEave) * slope
                scope.px(beamColor.copy(alpha = beamA), x, y, cw * 0.09f, 6f * p)
                y += 6f * p
            }
        }
    }

    // 6. Terracotta tile floor with a jute rug
    drawSunroomTiles(scope, cw, floorTop, floorTop, ch, p)
    if (sunny) {
        val fromLeft = hour < 13f
        for (b in 0..2) {
            val px0 = cw * (if (fromLeft) 0.22f + b * 0.26f else 0.28f + b * 0.26f)
            scope.pOval(Color(0xFFFFF2B2).copy(alpha = 0.14f), Offset(px0, floorTop + 10f * p), Size(cw * 0.16f, 14f * p))
        }
    }
    val rugY = ch * 0.68f + 4f * p
    scope.pOval(Color(0xFFC9AE82), Offset(cw * 0.5f - 46f * p, rugY - 6f * p), Size(92f * p, 15f * p))
    scope.pOval(Color(0xFFD9C29A), Offset(cw * 0.5f - 40f * p, rugY - 4.5f * p), Size(80f * p, 12f * p))
    scope.pOval(Color(0xFFC9AE82), Offset(cw * 0.5f - 30f * p, rugY - 3f * p), Size(60f * p, 9f * p), style = Stroke(0.6f * p))

    // 7. Left: a wicker armchair with a cushion and an open book
    val chairX = cw * 0.03f
    val chairBase = ch * 0.665f
    scope.pRoundRect(Color(0xFFC9A066), Offset(chairX, chairBase - 30f * p), Size(30f * p, 22f * p), CornerRadius(8f * p, 8f * p))
    for (w in 0..5) scope.px(Color(0x55805A2E), chairX + 2f * p, chairBase - 27f * p + w * 3.5f * p, 26f * p, 0.6f * p)
    for (w in 0..6) scope.px(Color(0x33805A2E), chairX + 3f * p + w * 4f * p, chairBase - 29f * p, 0.6f * p, 20f * p)
    scope.pRoundRect(Color(0xFFB8894F), Offset(chairX - 1f * p, chairBase - 12f * p), Size(32f * p, 7f * p), CornerRadius(3f * p, 3f * p))
    scope.pRoundRect(Color(0xFF84A59D), Offset(chairX + 3f * p, chairBase - 14f * p), Size(24f * p, 4f * p), CornerRadius(2f * p, 2f * p))
    scope.px(Color(0xFFF4F1EA), chairX + 9f * p, chairBase - 16f * p, 5f * p, 2f * p)
    scope.px(Color(0xFFEDE6D6), chairX + 14f * p, chairBase - 16f * p, 5f * p, 2f * p)
    scope.px(Color(0xFF8B5E34), chairX + 3f * p, chairBase - 5f * p, 2f * p, 5f * p)
    scope.px(Color(0xFF8B5E34), chairX + 25f * p, chairBase - 5f * p, 2f * p, 5f * p)

    // Tall fiddle-leaf fig between the chair and the couple
    drawFiddleFig(scope, cw * 0.30f, ch * 0.668f, p, time, scale = 1.15f)

    // 8. Right: potting bench, seedlings, and a shelf of plants above it
    val benchL = cw * 0.64f
    val benchR = cw * 0.97f
    val benchTop = ch * 0.605f
    val benchBase = ch * 0.668f
    scope.px(Color(0xFF8A5B3D), benchL, benchTop, benchR - benchL, 3f * p)
    scope.px(Color(0xFFA9744F), benchL, benchTop, benchR - benchL, 1f * p)
    scope.px(Color(0xFF6B4423), benchL + 2f * p, benchTop + 3f * p, 2.5f * p, benchBase - benchTop - 3f * p)
    scope.px(Color(0xFF6B4423), benchR - 4.5f * p, benchTop + 3f * p, 2.5f * p, benchBase - benchTop - 3f * p)
    scope.px(Color(0xFF7A4E33), benchL + 2f * p, benchBase - 9f * p, benchR - benchL - 4f * p, 2f * p)
    // Lower shelf: a soil sack and stacked empty pots
    scope.pRoundRect(Color(0xFF9C8C74), Offset(benchL + 6f * p, benchBase - 17f * p), Size(14f * p, 8f * p), CornerRadius(2f * p, 2f * p))
    scope.px(Color(0xFF6B8F71), benchL + 9f * p, benchBase - 14f * p, 8f * p, 2f * p)
    for (st in 0..2) scope.px(Color(0xFFC87652), benchL + 26f * p + st * 0.5f * p, benchBase - 11f * p - st * 2f * p, 8f * p, 2.5f * p)
    // Pots on the bench (they flower as you tend them)
    val potCount = 6
    for (i in 0 until potCount) {
        val potX = benchL + 4f * p + i * ((benchR - benchL - 12f * p) / potCount)
        val potTop = benchTop - 7f * p
        scope.px(if (i % 2 == 0) Color(0xFFC87652) else Color(0xFFB76648), potX, potTop, 7f * p, 7f * p)
        scope.px(Color(0xFF824A3D), potX - 0.5f * p, potTop, 8f * p, 1.6f * p)
        val sway = sin(time * 1.4f + i) * 0.7f * p
        when (i % 3) {
            0 -> { // fern
                for (f in 0..3) scope.px(Color(0xFF4F8A55), potX + 1f * p + f * 1.5f * p + sway, potTop - 6f * p + (f % 2) * 1.5f * p, 1.2f * p, 6f * p)
            }
            1 -> { // succulent rosette
                scope.pCircle(Color(0xFF8DB596), 3f * p, Offset(potX + 3.5f * p, potTop - 1.5f * p))
                scope.pCircle(Color(0xFFB5D6B2), 1.6f * p, Offset(potX + 3.5f * p, potTop - 2f * p))
            }
            else -> { // leafy herb
                scope.px(Color(0xFF6FAE68), potX + 1f * p + sway, potTop - 5f * p, 5f * p, 4f * p)
                scope.px(Color(0xFF4F8A55), potX + 2.5f * p + sway, potTop - 7f * p, 2f * p, 3f * p)
            }
        }
        if (engine.sunroomBloomStage > i / 2) {
            val flowerY = potTop - 7f * p + sway
            val petal = if (i % 2 == 0) Color(0xFFFFC6D6) else Color(0xFFFFE08A)
            scope.px(petal, potX + 1f * p + sway, flowerY - 1.5f * p, 2f * p, 2f * p)
            scope.px(petal, potX + 4f * p + sway, flowerY - 1.5f * p, 2f * p, 2f * p)
            scope.px(Color(0xFFFFB703), potX + 2.6f * p + sway, flowerY - 0.5f * p, 1.6f * p, 1.6f * p)
        }
    }
    // Trowel and a seed tray of sprouts
    scope.px(Color(0xFF9AA5B1), benchR - 16f * p, benchTop - 1.5f * p, 6f * p, 1.5f * p)
    scope.px(Color(0xFF8B5E34), benchR - 10f * p, benchTop - 1.2f * p, 4f * p, 1f * p)
    // Wall shelf above the bench with trailing ivy
    val shelfY = ch * 0.50f
    scope.px(Color(0xFF8A5B3D), benchL + 6f * p, shelfY, benchR - benchL - 12f * p, 2f * p)
    for (i in 0..3) {
        val sx = benchL + 10f * p + i * ((benchR - benchL - 24f * p) / 4f)
        scope.px(Color(0xFFE9E1D2), sx, shelfY - 5f * p, 6f * p, 5f * p)
        val sway = sin(time * 1.2f + i * 1.3f) * 0.8f * p
        for (v in 0..(3 + i % 2)) scope.px(if (v % 2 == 0) Color(0xFF4F8A55) else Color(0xFF6FAE68), sx + 1f * p + (v % 2) * 2.5f * p + sway, shelfY - 6f * p + v * 3f * p, 2.4f * p, 2f * p)
    }
    // A greenhouse thermometer on a mullion: warmer when the sun is in
    val thermoX = cw * 0.5f + cw / 6f * 0f + cw / 12f * 1f
    val thermoTop = transom + 10f * p
    scope.px(Color(0xFFF4F1EA), thermoX - 1.5f * p, thermoTop, 3f * p, 16f * p)
    val warm = if (sunny) 0.75f else if (night) 0.3f else 0.5f
    scope.px(Color(0xFFD62828), thermoX - 0.5f * p, thermoTop + 14f * p - 12f * p * warm, 1f * p, 12f * p * warm + 1f * p)
    scope.pCircle(Color(0xFFD62828), 1.4f * p, Offset(thermoX, thermoTop + 15f * p))

    // 9. Macramé plant hangers swaying from the eave
    for (i in 0..3) {
        val hx2 = cw * (0.16f + i * 0.22f)
        val len = (16f + (i % 2) * 8f) * p
        val sway = sin(time * 1.1f + i * 1.7f) * 2f * p
        val potY = roofEave + 4f * p + len
        scope.pLine(Color(0xFFD4C2A8), Offset(hx2, roofEave + 2f * p), Offset(hx2 - 3f * p + sway, potY), strokeWidth = 0.6f * p)
        scope.pLine(Color(0xFFD4C2A8), Offset(hx2, roofEave + 2f * p), Offset(hx2 + 3f * p + sway, potY), strokeWidth = 0.6f * p)
        scope.px(Color(0xFFE9E1D2), hx2 - 4f * p + sway, potY, 8f * p, 5f * p)
        for (leaf in 0..4) {
            scope.px(Color(0xFF3F8052), hx2 - (leaf % 2 + 2) * p + sway * (1f + leaf * 0.1f), potY + 4f * p + leaf * 2.8f * p, 2.6f * p, 1.8f * p)
            scope.px(Color(0xFF70A66A), hx2 + 1f * p + sway * (1f + leaf * 0.12f), potY + 5f * p + leaf * 2.8f * p, 2.6f * p, 1.8f * p)
        }
    }
    // Wind chime by the door frame
    val chimeX = cw * 0.06f
    scope.px(Color(0xFFB08968), chimeX - 4f * p, roofEave + 4f * p, 8f * p, 1f * p)
    for (c in 0..3) {
        val swing = sin(time * 2.2f + c * 0.8f) * 1f * p
        scope.pLine(Color(0xFF9AA5B1), Offset(chimeX - 3f * p + c * 2f * p, roofEave + 5f * p), Offset(chimeX - 3f * p + c * 2f * p + swing, roofEave + (11f + c * 1.5f) * p), strokeWidth = 0.8f * p)
    }

    // 10. Galvanised watering can on the floor (tap it for a cool mist)
    val canX = cw * 0.20f
    val canY = ch * 0.72f
    scope.pRoundRect(Color(0xFF9DB4BC), Offset(canX - 7f * p, canY - 5f * p), Size(14f * p, 10f * p), CornerRadius(2f * p, 2f * p))
    scope.px(Color(0xFFBFD0D6), canX - 6f * p, canY - 4f * p, 12f * p, 1.5f * p)
    scope.px(Color(0xFF7E959D), canX - 7f * p, canY + 1f * p, 14f * p, 1f * p)
    scope.pArc(Color(0xFF7E959D), 180f, 180f, false, Offset(canX - 5f * p, canY - 10f * p), Size(10f * p, 9f * p), style = Stroke(1.2f * p))
    scope.pLine(Color(0xFF7E959D), Offset(canX + 6f * p, canY), Offset(canX + 13f * p, canY - 7f * p), strokeWidth = 1.6f * p)
    scope.pOval(Color(0xFF6C838B), Offset(canX + 12f * p, canY - 9f * p), Size(3f * p, 3.5f * p))
    scope.pOval(Color(0x33000000), Offset(canX - 8f * p, canY + 4.5f * p), Size(16f * p, 2.5f * p))
    if (engine.sunroomMistTimer > 0f) for (i in 0..7) {
        val mxx = cw * (0.27f + (i % 4) * 0.10f) + sin(time * 2f + i) * 3f * p
        val my = ch * (0.59f - (i / 4) * 0.045f - ((time * 0.16f + i * 0.11f) % 0.09f))
        scope.pCircle(Color(0xFFAEE6E3).copy(alpha = 0.42f), (1.5f + i % 2) * p, Offset(mxx, my))
    }
    // Mochi's water bowl by the bench
    scope.px(Color(0xFFE5989B), cw * 0.90f, ch * 0.703f, 7f * p, 2.5f * p)
    scope.px(Color(0xFFBFE3EE), cw * 0.90f + 1f * p, ch * 0.703f, 5f * p, 0.8f * p)

    // Front of the room: a tea table, a floor cushion and garden boots by the door
    drawSunroomTeaTable(scope, cw * 0.70f, ch * 0.80f, p * 1.4f, time, night || dusk)
    val pouf = Offset(cw * 0.30f, ch * 0.815f)
    scope.pOval(Color(0x33000000), Offset(pouf.x - 15f * p, pouf.y + 4f * p), Size(30f * p, 5f * p))
    scope.pRoundRect(Color(0xFF84A59D), Offset(pouf.x - 14f * p, pouf.y - 8f * p), Size(28f * p, 13f * p), CornerRadius(6f * p, 6f * p))
    scope.pRoundRect(Color(0xFF9CC5B9), Offset(pouf.x - 12f * p, pouf.y - 7f * p), Size(24f * p, 4f * p), CornerRadius(3f * p, 3f * p))
    scope.px(Color(0xFFE9D8A6), pouf.x - 6f * p, pouf.y - 11f * p, 13f * p, 3.5f * p)
    scope.px(Color(0xFFD4A373), pouf.x - 6f * p, pouf.y - 9f * p, 13f * p, 0.8f * p)
    val bootX = cw * 0.86f
    val bootY = ch * 0.885f
    for (b in 0..1) {
        val bx3 = bootX + b * 9f * p
        scope.px(Color(0xFF3D6B4F), bx3, bootY - 16f * p, 7f * p, 14f * p)
        scope.px(Color(0xFF3D6B4F), bx3, bootY - 4f * p, 11f * p, 4f * p)
        scope.px(Color(0xFF2E5240), bx3, bootY - 1f * p, 11f * p, 1.5f * p)
        scope.px(Color(0xFF5D8A6E), bx3 + 1f * p, bootY - 15f * p, 1.5f * p, 10f * p)
    }
    if (sunny) { // a few fallen leaves by the door
        for (l in 0..4) scope.px(if (l % 2 == 0) Color(0xFFE9C46A) else Color(0xFFE76F51), cw * (0.74f + nz(l, 201) * 0.10f), ch * (0.86f + nz(l, 202) * 0.05f), 2f * p, 1.2f * p)
    }

    // 11. Foreground leaves framing the bottom corners
    drawCornerLeaves(scope, 0f, ch, p, time, mirror = false, night = night)
    drawCornerLeaves(scope, cw, ch, p, time, mirror = true, night = night)
    if (night) scope.px(Color(0x1A05070F), 0f, kneeTop, cw, ch - kneeTop)
}

/**
 * The sunroom's terracotta tiles, rows growing toward the viewer from [floorTop], drawn between
 * [fromY] and [toY]. The floor continued below the stage calls this too, so its rows carry on.
 */
internal fun drawSunroomTiles(scope: DrawScope, cw: Float, floorTop: Float, fromY: Float, toY: Float, p: Float) {
    scope.px(Color(0xFFE7D2BF), 0f, fromY, cw, toY - fromY) // grout shows between tiles
    var ty = floorTop
    var tr2 = 0
    while (ty < toY) {
        val rowH = (5f + tr2 * 1.1f) * p
        if (ty + rowH > fromY) {
            val top = maxOf(ty, fromY)
            val h = minOf(ty + rowH - p, toY) - top
            val tileW = (11f + tr2 * 2.2f) * p
            var tx = if (tr2 % 2 == 0) 0f else -tileW * 0.5f
            var tc = 0
            while (tx < cw && h > 0f) {
                val v = nz(tr2 * 71 + tc, 83)
                scope.px(if (v < 0.33f) Color(0xFFC7764E) else if (v < 0.7f) Color(0xFFB9694A) else Color(0xFFD08458), tx, top, tileW - p, h)
                tx += tileW
                tc++
            }
        }
        ty += rowH
        tr2++
    }
}

/** Big monstera-like leaves poking in from a bottom corner, for depth. */
private fun drawCornerLeaves(scope: DrawScope, x: Float, bottom: Float, p: Float, time: Float, mirror: Boolean, night: Boolean) {
    val dir = if (mirror) -1f else 1f
    val dark = if (night) Color(0xFF173226) else Color(0xFF2F6B3F)
    val light = if (night) Color(0xFF214636) else Color(0xFF3F8052)
    for (l in 0..3) {
        val sway = sin(time * 0.9f + l * 1.3f) * 1.2f * p
        val lx = x + dir * (6f + l * 11f) * p + sway
        val ly = bottom - (14f + (l % 2) * 10f) * p
        scope.pOval(if (l % 2 == 0) dark else light, Offset(lx - 10f * p, ly - 6f * p), Size(20f * p, 13f * p))
        scope.px(Color(0x33000000), lx - 0.4f * p, ly - 5f * p, 0.8f * p, 11f * p)
        scope.px(if (l % 2 == 0) light else dark, lx - 0.6f * p, ly + 6f * p, 1.2f * p, bottom - ly)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STARRY CAMPFIRE
// ─────────────────────────────────────────────────────────────────────────────

fun drawCampfireScene(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    p: Float,
    time: Float,
    engine: SceneEngine,
    isNight: Boolean,
    isSunset: Boolean,
    isMorning: Boolean
) {
    // 1. Sky (stars, moon or sun, clouds) follows the real time and weather
    drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, time, p, weather = engine.weather)
    val snowing = engine.weather == WeatherType.SNOW
    val horizon = ch * 0.50f

    // 2. Distant mountains, two layers, with snowy peaks
    val farMtn = when { isNight -> Color(0xFF1B2440); isSunset -> Color(0xFF5B4A6B); else -> Color(0xFF8197AC) }
    val nearMtn = when { isNight -> Color(0xFF151C33); isSunset -> Color(0xFF45374F); else -> Color(0xFF627A8F) }
    var mx = 0f
    while (mx < cw) {
        val t = mx / cw
        val farH = (34f + 18f * sin(t * 6.8f + 0.5f) + 10f * sin(t * 17f)) * p
        val nearH = (18f + 10f * sin(t * 9.5f + 2f) + 6f * sin(t * 23f + 1f)) * p
        scope.px(farMtn, mx, horizon - farH, 3f * p + 0.5f, farH)
        if (farH > 50f * p) scope.px(if (isNight) Color(0xFF8C97B5) else Color(0xFFF1F4F8), mx, horizon - farH, 3f * p + 0.5f, 3f * p)
        scope.px(nearMtn, mx, horizon - nearH, 3f * p + 0.5f, nearH)
        mx += 3f * p
    }

    // 3. Pine treeline: a small back row and a taller front row
    val pineBack = when { isNight -> Color(0xFF0E1B15); isSunset -> Color(0xFF2B2230); else -> Color(0xFF2F5A43) }
    val pineFront = when { isNight -> Color(0xFF0B1611); isSunset -> Color(0xFF231B27); else -> Color(0xFF244A36) }
    val pineLight = when { isNight -> Color(0xFF14261D); isSunset -> Color(0xFF3A2D3C); else -> Color(0xFF3B6E52) }
    var bx = 0f
    var bi = 0
    while (bx < cw + 10f * p) {
        val h = (12f + nz(bi, 91) * 10f) * p
        scope.pixelTriangle(pineBack, bx, horizon + 2f * p, h * 0.38f, h, 3f * p)
        bx += (7f + nz(bi, 92) * 4f) * p
        bi++
    }
    var fx = 0f
    var fi = 0
    while (fx < cw + 14f * p) {
        val h = (22f + nz(fi, 93) * 18f) * p
        val base = horizon + 6f * p
        scope.px(Color(0xFF2A1C14), fx - 1f * p, base - 4f * p, 2f * p, 4f * p)
        scope.pixelTriangle(pineFront, fx, base - 3f * p, h * 0.32f, h * 0.62f, 3f * p)
        scope.pixelTriangle(pineFront, fx, base - 3f * p - h * 0.32f, h * 0.24f, h * 0.55f, 3f * p)
        scope.px(pineLight, fx - h * 0.1f, base - 3f * p - h * 0.5f, 1.5f * p, h * 0.3f)
        if (snowing) scope.pixelTriangle(Color(0xFFF1F4F8), fx, base - 3f * p - h * 0.62f, h * 0.08f, h * 0.25f, 2f * p)
        fx += (12f + nz(fi, 94) * 8f) * p
        fi++
    }

    // 4. Meadow: graded from distant to near, scattered tufts in several greens
    val farGround = when { snowing -> Color(0xFFCFD8E0); isNight -> Color(0xFF16291F); isSunset -> Color(0xFF3B3424); else -> Color(0xFF4E7A45) }
    val nearGround = when { snowing -> Color(0xFFEAF0F5); isNight -> Color(0xFF1F3A2A); isSunset -> Color(0xFF5A4E33); else -> Color(0xFF6E9E58) }
    val groundTop = horizon + 4f * p
    for (b in 0 until 12) {
        val t = b / 11f
        val y0 = groundTop + (ch - groundTop) * b / 12f
        scope.px(mix(farGround, nearGround, t), 0f, y0, cw, (ch - groundTop) / 12f + 1f)
    }
    val tuftDark = if (snowing) Color(0xFFB7C4CE) else if (isNight) Color(0xFF10211A) else Color(0xFF3F6B39)
    val tuftMid = if (snowing) Color(0xFFC9D4DC) else if (isNight) Color(0xFF244533) else Color(0xFF5E8F4C)
    val tuftLight = if (snowing) Color(0xFFDCE4EA) else if (isNight) Color(0xFF2E563F) else Color(0xFF8DBA68)
    val pitX = cw * CampfireLayout.PIT_X
    val pitY = ch * CampfireLayout.PIT_Y
    val clearRx = cw * 0.21f
    val clearRy = ch * 0.062f
    for (i in 0 until 170) {
        val tx = nz(i, 101) * cw
        val depth = nz(i, 102)
        val tyy = groundTop + 4f * p + depth * depth * 0.0f + depth * (ch - groundTop - 6f * p)
        val ddx = (tx - pitX) / clearRx
        val ddy = (tyy - pitY) / clearRy
        if (ddx * ddx + ddy * ddy < 1.1f) continue
        val s = 0.55f + depth * 0.9f
        val c = when ((i * 7) % 3) { 0 -> tuftDark; 1 -> tuftMid; else -> tuftLight }
        val sway = sin(time * 1.6f + i) * 0.4f * p * s
        scope.px(c, tx + sway, tyy - 4f * p * s, 0.9f * p * s, 4f * p * s)
        scope.px(c, tx - 1.4f * p * s + sway * 0.6f, tyy - 2.8f * p * s, 0.9f * p * s, 2.8f * p * s)
        scope.px(c, tx + 1.4f * p * s + sway * 0.8f, tyy - 3.2f * p * s, 0.9f * p * s, 3.2f * p * s)
    }
    if (!snowing) {
        for (i in 0 until 26) {
            val wx = nz(i, 111) * cw
            val wy = groundTop + 8f * p + nz(i, 112) * (ch - groundTop - 12f * p)
            val ddx = (wx - pitX) / clearRx
            val ddy = (wy - pitY) / clearRy
            if (ddx * ddx + ddy * ddy < 1.2f) continue
            val c = when (i % 4) { 0 -> Color(0xFFF4F1EA); 1 -> Color(0xFFFFD166); 2 -> Color(0xFFCDB4DB); else -> Color(0xFFFF8FAB) }
            scope.px(if (isNight) c.copy(alpha = 0.45f) else c, wx, wy, 1.5f * p, 1.5f * p)
        }
    }
    for (i in 0 until 8) { // pebbles and pinecones
        val rx = nz(i, 121) * cw
        val ry = groundTop + 10f * p + nz(i, 122) * (ch - groundTop - 14f * p)
        if (i % 2 == 0) scope.pOval(if (isNight) Color(0xFF3A4048) else Color(0xFF8D99A6), Offset(rx, ry), Size(4f * p, 2.5f * p))
        else scope.px(if (isNight) Color(0xFF3B2A1E) else Color(0xFF7A4E2D), rx, ry, 2f * p, 2.6f * p)
    }

    // 5. Bare-earth clearing worn around the fire, with a footpath to the tent
    val dirt = when { snowing -> Color(0xFF9C8E80); isNight -> Color(0xFF2E241E); isSunset -> Color(0xFF5A4232); else -> Color(0xFF8A6B4E) }
    val dirtDark = when { snowing -> Color(0xFF85776A); isNight -> Color(0xFF241C17); isSunset -> Color(0xFF4A3528); else -> Color(0xFF735842) }
    val rowStep = 2.5f * p
    var dy = -clearRy
    var dr = 0
    while (dy < clearRy) {
        val k = 1f - (dy / clearRy) * (dy / clearRy)
        if (k > 0f) {
            val hw = clearRx * kotlin.math.sqrt(k) + (nz(dr, 131) - 0.5f) * 6f * p
            scope.px(dirt, pitX - hw, pitY + dy, hw * 2f, rowStep + 0.5f)
            // a ragged, grassier fringe so the bare earth blends into the meadow
            scope.px(tuftMid.copy(alpha = 0.55f), pitX - hw - 1.5f * p, pitY + dy, 2.5f * p, rowStep + 0.5f)
            scope.px(tuftMid.copy(alpha = 0.55f), pitX + hw - 1f * p, pitY + dy, 2.5f * p, rowStep + 0.5f)
        }
        dy += rowStep
        dr++
    }
    // darker trampled ring right around the pit, with a few pebbles
    scope.pOval(dirtDark, Offset(pitX - clearRx * 0.55f, pitY - clearRy * 0.45f), Size(clearRx * 1.1f, clearRy * 0.9f))
    for (i in 0 until 10) scope.px(if (isNight) Color(0xFF3A4048) else Color(0xFF9C8E80), pitX + (nz(i, 133) - 0.5f) * clearRx * 1.6f, pitY + (nz(i, 134) - 0.5f) * clearRy * 1.4f, 1.4f * p, 1f * p)
    val tent = CampfireLayout.tentTopLeft(cw, ch, p)
    val tentSize = CampfireLayout.tentSize(p)
    val doorX = tent.x + tentSize.x / 2f
    val doorY = tent.y + tentSize.y
    for (s in 0..5) {
        val t = s / 5f
        val sx = doorX + (pitX - clearRx * 0.8f - doorX) * t
        val sy = doorY + 3f * p + (pitY - doorY - 3f * p) * t
        scope.pOval(dirt, Offset(sx - (4f + t * 4f) * p, sy - 1.5f * p), Size((8f + t * 8f) * p, (3f + t * 2f) * p))
    }

    // 6. A-frame canvas tent pitched on the ground, guy ropes pegged out
    val lanternLit = engine.campLanternLit
    val canvasLit = isNight && lanternLit
    val canvas = when { canvasLit -> Color(0xFFE6B877); isNight -> Color(0xFF6F6A5C); isSunset -> Color(0xFFC9A98A); else -> Color(0xFFDCC9A3) }
    val canvasShade = when { canvasLit -> Color(0xFFC98E4E); isNight -> Color(0xFF55513F); isSunset -> Color(0xFFA9876A); else -> Color(0xFFBBA67F) }
    val tentCx = tent.x + tentSize.x / 2f
    val tentBase = tent.y + tentSize.y
    scope.pOval(Color(0x44000000), Offset(tent.x - 4f * p, tentBase - 2f * p), Size(tentSize.x + 8f * p, 5f * p))
    scope.pixelTriangle(canvas, tentCx, tentBase, tentSize.x / 2f, tentSize.y, 2.5f * p)
    // shaded right-hand slope
    var sy = 0f
    while (sy < tentSize.y) {
        val hw = (tentSize.x / 2f) * ((sy + 2.5f * p) / tentSize.y)
        scope.px(canvasShade, tentCx + hw * 0.35f, tent.y + sy, hw * 0.65f, 2.5f * p + 0.6f)
        sy += 2.5f * p
    }
    // open door with tied-back flaps and a sleeping bag inside
    val doorH = tentSize.y * 0.62f
    scope.pixelTriangle(if (canvasLit) Color(0xFF7A4B22) else Color(0xFF2A1E18), tentCx, tentBase, tentSize.x * 0.17f, doorH, 2.5f * p)
    if (canvasLit) scope.pCircle(Color(0xFFFFD27A).copy(alpha = 0.25f), 9f * p, Offset(tentCx, tentBase - doorH * 0.35f))
    scope.px(Color(0xFF9D3C3C), tentCx - 5f * p, tentBase - 4f * p, 10f * p, 3.5f * p)
    scope.px(Color(0xFFE9C46A), tentCx - 5f * p, tentBase - 4f * p, 2f * p, 3.5f * p)
    scope.pixelTriangle(canvasShade, tentCx - tentSize.x * 0.19f, tentBase, tentSize.x * 0.07f, doorH * 0.75f, 2.5f * p)
    scope.pixelTriangle(canvasShade, tentCx + tentSize.x * 0.19f, tentBase, tentSize.x * 0.07f, doorH * 0.75f, 2.5f * p)
    scope.px(Color(0xFF8B5E34), tentCx - tentSize.x * 0.24f, tentBase - doorH * 0.45f, 3f * p, 1f * p)
    scope.px(Color(0xFF8B5E34), tentCx + tentSize.x * 0.24f - 3f * p, tentBase - doorH * 0.45f, 3f * p, 1f * p)
    if (snowing) scope.pixelTriangle(Color(0xFFF1F4F8), tentCx, tent.y + tentSize.y * 0.25f, tentSize.x * 0.13f, tentSize.y * 0.25f, 2.5f * p)
    scope.px(Color(0xFF4E3629), tentCx - 0.8f * p, tent.y - 3f * p, 1.6f * p, 4f * p)
    val rope = Color(0xFFBFAF8F).copy(alpha = if (isNight) 0.5f else 0.9f)
    scope.pLine(rope, Offset(tentCx, tent.y + 2f * p), Offset(tent.x - 9f * p, tentBase + 1f * p), strokeWidth = 0.5f * p)
    scope.pLine(rope, Offset(tentCx, tent.y + 2f * p), Offset(tent.x + tentSize.x + 9f * p, tentBase + 1f * p), strokeWidth = 0.5f * p)
    scope.px(Color(0xFF5D4037), tent.x - 10f * p, tentBase, 1.6f * p, 2f * p)
    scope.px(Color(0xFF5D4037), tent.x + tentSize.x + 8.4f * p, tentBase, 1.6f * p, 2f * p)

    // Camp lantern hanging from a shepherd's-hook pole planted beside the tent door
    val lantern = CampfireLayout.lantern(cw, ch, p)
    val lantX = lantern.x
    val lantY = lantern.y
    val poleX = lantX + 5f * p
    scope.px(Color(0xFF3E2723), poleX - 0.6f * p, lantY - 6f * p, 1.2f * p, tentBase + 1f * p - (lantY - 6f * p))
    scope.pArc(Color(0xFF3E2723), 180f, 180f, false, Offset(lantX - 0.5f * p, lantY - 8.5f * p), Size(6f * p, 5f * p), style = Stroke(1.2f * p))
    scope.pLine(Color(0xFF3E2723), Offset(lantX, lantY - 6f * p), Offset(lantX, lantY), strokeWidth = 0.8f * p)
    scope.px(Color(0xFF3E2723), lantX - 4f * p, lantY, 8f * p, 2f * p)
    scope.px(Color(0xFF3E2723), lantX - 4f * p, lantY + 9f * p, 8f * p, 2f * p)
    if (lanternLit) {
        val flicker = sin(time * 6f) * 1.5f * p
        val glowA = if (isNight || isSunset) 0.35f else 0.15f
        scope.pCircle(Color(0xFFFFD166).copy(alpha = glowA), 18f * p + flicker, Offset(lantX, lantY + 5.5f * p))
        scope.px(Color(0xFFFFF3B0), lantX - 3f * p, lantY + 2f * p, 6f * p, 7f * p)
        scope.px(Color.White, lantX - 1f * p, lantY + 3.5f * p, 2f * p, 3.5f * p)
    } else {
        scope.px(Color(0xFF8E979C), lantX - 3f * p, lantY + 2f * p, 6f * p, 7f * p)
    }
    scope.px(Color(0xFF3E2723), lantX - 3.5f * p, lantY + 2f * p, 0.8f * p, 7f * p)
    scope.px(Color(0xFF3E2723), lantX + 2.7f * p, lantY + 2f * p, 0.8f * p, 7f * p)

    // 7. Firewood stack with an axe in a chopping stump
    val woodX = cw * 0.13f
    val woodY = ch * 0.795f
    for (row in 0..2) {
        val n = 4 - row
        for (l in 0 until n) {
            val lx = woodX + (l * 6.5f + row * 3.2f) * p
            val ly = woodY - row * 5.5f * p
            scope.pCircle(if (isNight) Color(0xFF4A3528) else Color(0xFF8B5E34), 3f * p, Offset(lx, ly))
            scope.pCircle(if (isNight) Color(0xFF6B5240) else Color(0xFFD4A373), 2f * p, Offset(lx, ly))
            scope.pCircle(if (isNight) Color(0xFF4A3528) else Color(0xFFB07D50), 0.8f * p, Offset(lx, ly))
        }
    }
    val stumpX = cw * 0.06f
    val stumpY = ch * 0.83f
    scope.px(if (isNight) Color(0xFF3B2A1E) else Color(0xFF6B4A2E), stumpX - 6f * p, stumpY - 6f * p, 12f * p, 7f * p)
    scope.pOval(if (isNight) Color(0xFF5A4232) else Color(0xFFC8A47A), Offset(stumpX - 6f * p, stumpY - 8f * p), Size(12f * p, 4f * p))
    scope.pLine(Color(0xFF8B5E34), Offset(stumpX + 1f * p, stumpY - 7f * p), Offset(stumpX + 7f * p, stumpY - 17f * p), strokeWidth = 1.4f * p)
    scope.px(Color(0xFF9AA5B1), stumpX - 2f * p, stumpY - 9f * p, 5f * p, 3f * p)

    // 8. The log bench the couple sits on, with a backpack leaning on one end
    val logX = CampfireLayout.logX(cw)
    val logY = CampfireLayout.logY(ch, p)
    val logW = CampfireLayout.logW(cw)
    val logH = 9f * p
    val bark = if (isNight) Color(0xFF3B2A20) else Color(0xFF5C4033)
    val barkHi = if (isNight) Color(0xFF55402F) else Color(0xFF7B5845)
    scope.pOval(Color(0x44000000), Offset(logX - 2f * p, logY + logH - 1.5f * p), Size(logW + 4f * p, 4f * p))
    scope.pRoundRect(bark, Offset(logX, logY), Size(logW, logH), CornerRadius(4f * p, 4f * p))
    scope.px(barkHi, logX + 3f * p, logY + 1.2f * p, logW - 6f * p, 1.6f * p)
    var gx = logX + 6f * p
    var gi = 0
    while (gx < logX + logW - 6f * p) {
        scope.px(Color(0x33000000), gx, logY + 3.5f * p + (gi % 2) * 2f * p, (5f + nz(gi, 141) * 6f) * p, 0.7f * p)
        gx += 9f * p
        gi++
    }
    scope.px(if (isNight) Color(0xFF26402E) else Color(0xFF6A994E), logX + logW * 0.62f, logY - 0.6f * p, 7f * p, 1.6f * p)
    scope.pOval(if (isNight) Color(0xFF6B5240) else Color(0xFFC8A47A), Offset(logX - 3f * p, logY), Size(6f * p, logH))
    scope.pOval(if (isNight) Color(0xFF4A3528) else Color(0xFF9C7A54), Offset(logX - 1.5f * p, logY + 2f * p), Size(3f * p, logH - 4f * p))
    scope.pOval(if (isNight) Color(0xFF6B5240) else Color(0xFFC8A47A), Offset(logX + logW - 3f * p, logY), Size(6f * p, logH))
    val packX = logX - 12f * p
    val packBase = logY + logH
    scope.pRoundRect(if (isNight) Color(0xFF3A4A3A) else Color(0xFF6B7F4E), Offset(packX, packBase - 13f * p), Size(10f * p, 13f * p), CornerRadius(3f * p, 3f * p))
    scope.pRoundRect(if (isNight) Color(0xFF2E3B2E) else Color(0xFF55663E), Offset(packX + 1.5f * p, packBase - 7f * p), Size(7f * p, 5f * p), CornerRadius(1.5f * p, 1.5f * p))
    scope.pRoundRect(if (isNight) Color(0xFF5E3A3A) else Color(0xFFB5534A), Offset(packX - 0.5f * p, packBase - 16f * p), Size(11f * p, 4f * p), CornerRadius(2f * p, 2f * p))

    // 9. Acoustic guitar leaning against the far end of the log
    val guitar = CampfireLayout.guitar(cw, ch, p)
    val gtrX = guitar.x
    val gtrY = guitar.y
    scope.pLine(Color(0xFF5D4037), Offset(gtrX + 4f * p, gtrY - 14f * p), Offset(gtrX, gtrY + 4f * p), strokeWidth = 2.2f * p)
    scope.px(Color(0xFF3E2723), gtrX + 3f * p, gtrY - 18f * p, 4f * p, 5f * p)
    scope.pOval(Color(0xFFBA6E32), Offset(gtrX - 5f * p, gtrY - 1f * p), Size(11f * p, 13f * p))
    scope.pOval(Color(0xFF8B4716), Offset(gtrX - 4f * p, gtrY), Size(9f * p, 11f * p))
    scope.pCircle(Color(0xFF2E190D), 2f * p, Offset(gtrX + 0.5f * p, gtrY + 5f * p))
    if (engine.campGuitarStrumTimer > 0f) {
        val wave = sin(time * 8f) * 3f * p
        scope.pCircle(Color(0xFFFFD166).copy(alpha = 0.5f), 12f * p, Offset(gtrX, gtrY + 4f * p))
        scope.pCircle(Color(0xFFFF7597), 2f * p, Offset(gtrX + 6f * p + wave, gtrY - 8f * p))
        scope.pLine(Color(0xFFFF7597), Offset(gtrX + 8f * p + wave, gtrY - 8f * p), Offset(gtrX + 8f * p + wave, gtrY - 16f * p), strokeWidth = 1.2f * p)
    }

    // 10. Plaid camp blanket on the grass (Mochi naps here), with a fringe
    val blkX = cw * CampfireLayout.BLANKET_X
    val blkY = ch * CampfireLayout.BLANKET_Y
    val blkW = 38f * p
    val blkH = 18f * p
    scope.pRoundRect(Color(0xFFB71C1C), Offset(blkX, blkY), Size(blkW, blkH), CornerRadius(3f * p, 3f * p))
    for (i in 0..3) scope.px(Color(0xFF1B2A32).copy(alpha = 0.5f), blkX + 4f * p + i * 9f * p, blkY, 2.5f * p, blkH)
    for (j in 0..1) scope.px(Color(0xFF1B2A32).copy(alpha = 0.5f), blkX, blkY + 4f * p + j * 7f * p, blkW, 2.5f * p)
    for (i in 0..1) scope.px(Color(0xFFFFD166).copy(alpha = 0.6f), blkX + 8.5f * p + i * 18f * p, blkY, 0.6f * p, blkH)
    var fr = blkX + 1f * p
    while (fr < blkX + blkW - 1f * p) {
        scope.px(Color(0xFF8E1616), fr, blkY + blkH, 0.6f * p, 1.6f * p)
        fr += 1.8f * p
    }
    // A red cooler and a tin mug nearby
    val coolX = cw * 0.84f
    val coolY = ch * 0.80f
    scope.pRoundRect(if (isNight) Color(0xFF6E2A2A) else Color(0xFFC0392B), Offset(coolX, coolY - 9f * p), Size(16f * p, 9f * p), CornerRadius(1.5f * p, 1.5f * p))
    scope.px(if (isNight) Color(0xFFB8B8B8) else Color(0xFFF4F1EA), coolX - 0.5f * p, coolY - 11f * p, 17f * p, 2.5f * p)
    scope.px(Color(0xFF555555), coolX + 6f * p, coolY - 12.5f * p, 4f * p, 1.2f * p)
    scope.px(if (isNight) Color(0xFF5A7EA0) else Color(0xFF6A9BC3), coolX - 6f * p, coolY - 4f * p, 4f * p, 4f * p)

    // 11. The fire: warm light pooling on the ground (strong at night, faint by day)
    val glowR = if (isNight) 70f * p else if (isSunset) 52f * p else 34f * p
    val glowA = if (isNight) 0.30f else if (isSunset) 0.20f else 0.08f
    val pulse = 1f + 0.04f * sin(time * 5f)
    for (ring in 0..2) { // stepped rings, brighter toward the flames
        val rr = glowR * pulse * (1f - ring * 0.3f)
        scope.pOval(Color(0xFFFFB347).copy(alpha = glowA * 0.4f), Offset(pitX - rr, pitY - rr * 0.45f), Size(rr * 2f, rr * 0.9f))
    }
    val fireRadius = 14f * p
    // back stones, ember bed, then the burning logs
    for (stone in 0..9) {
        val ang = stone * 36f * (kotlin.math.PI.toFloat() / 180f)
        if (sin(ang) > 0.2f) continue // front stones are drawn after the flames
        val sx = pitX + cos(ang) * (fireRadius + 2f * p)
        val syy = pitY + sin(ang) * (fireRadius * 0.55f)
        scope.pOval(if (stone % 2 == 0) Color(0xFF6B6B6B) else Color(0xFF555555), Offset(sx - 3.5f * p, syy - 2.5f * p), Size(7f * p, 5f * p))
    }
    scope.pOval(Color(0xFF3A1A10), Offset(pitX - 11f * p, pitY - 3f * p), Size(22f * p, 7f * p))
    for (e in 0..5) {
        val ex = pitX + (nz(e, 151) - 0.5f) * 16f * p
        val eg = 0.5f + 0.5f * sin(time * 4f + e * 1.3f)
        scope.px(Color(0xFFFF6B35).copy(alpha = 0.5f + 0.5f * eg), ex, pitY - 0.5f * p + (e % 2) * 1.5f * p, 1.6f * p, 1.2f * p)
    }
    scope.pLine(Color(0xFF3B2A20), Offset(pitX - 9f * p, pitY + 2f * p), Offset(pitX + 2f * p, pitY - 9f * p), strokeWidth = 2.8f * p)
    scope.pLine(Color(0xFF3B2A20), Offset(pitX + 9f * p, pitY + 2f * p), Offset(pitX - 2f * p, pitY - 9f * p), strokeWidth = 2.8f * p)
    scope.pLine(Color(0xFF4A3528), Offset(pitX, pitY + 3f * p), Offset(pitX, pitY - 10f * p), strokeWidth = 2.6f * p)

    // Layered flames that lick upward
    val fa = sin(time * 12f) * 2f * p
    val fb = sin(time * 16f + 1.5f) * 2.5f * p
    val fc = sin(time * 9f + 0.7f) * 1.8f * p
    scope.pOval(Color(0xFFD62828), Offset(pitX - 8f * p, pitY - 8f * p + fc), Size(16f * p, 10f * p))
    scope.pOval(Color(0xFFE85D04), Offset(pitX - 7f * p, pitY - 11f * p + fa), Size(14f * p, 12f * p))
    scope.pOval(Color(0xFFF48C06), Offset(pitX - 5f * p, pitY - 14f * p + fb), Size(10f * p, 12f * p))
    scope.pOval(Color(0xFFF48C06), Offset(pitX - 9f * p + fc, pitY - 10f * p), Size(5f * p, 8f * p))
    scope.pOval(Color(0xFFF48C06), Offset(pitX + 4f * p - fc, pitY - 11f * p), Size(5f * p, 9f * p))
    scope.pOval(Color(0xFFFFBA08), Offset(pitX - 3f * p, pitY - 16f * p + fa * 0.8f), Size(6f * p, 10f * p))
    scope.pCircle(Color(0xFFFFF3B0), 2f * p, Offset(pitX, pitY - 13f * p + fb * 0.5f))
    val flare = (engine.campfireEmbersTimer / 2.8f).coerceIn(0f, 1f)
    if (flare > 0f) {
        val flareH = 18f * p * flare
        scope.pOval(Color(0xFFF48C06).copy(alpha = 0.85f), Offset(pitX - 5f * p, pitY - 14f * p - flareH + fb), Size(10f * p, 12f * p + flareH))
        scope.pOval(Color(0xFFFFBA08).copy(alpha = 0.9f), Offset(pitX - 3f * p, pitY - 16f * p - flareH * 0.8f + fa), Size(6f * p, 10f * p + flareH * 0.8f))
    }
    // front stones
    for (stone in 0..9) {
        val ang = stone * 36f * (kotlin.math.PI.toFloat() / 180f)
        if (sin(ang) <= 0.2f) continue
        val sx = pitX + cos(ang) * (fireRadius + 2f * p)
        val syy = pitY + sin(ang) * (fireRadius * 0.55f)
        scope.pOval(if (stone % 2 == 0) Color(0xFF7A7A7A) else Color(0xFF616161), Offset(sx - 3.8f * p, syy - 2.6f * p), Size(7.6f * p, 5.2f * p))
        scope.pOval(Color(0x33FFB347), Offset(sx - 2.5f * p, syy - 2.6f * p), Size(5f * p, 1.6f * p))
    }

    // Embers rising, and a ribbon of smoke drifting with the breeze
    for (e in 0..6) {
        val ex = pitX + sin(time * 3f + e * 1.8f) * (6f + e * 2f) * p
        val ey = (pitY - 16f * p) - ((time * (18f + e * 4f) + e * 15f) % (46f * p))
        val eAlpha = (1f - (pitY - ey) / (62f * p)).coerceIn(0f, 1f)
        scope.px(Color(0xFFFFD166).copy(alpha = eAlpha), ex, ey, 1.2f * p, 1.2f * p)
    }
    val smokeA = if (isNight) 0.05f else 0.13f
    for (s in 0..5) {
        val prog = ((time * 0.18f) + s / 6f) % 1f
        val sx = pitX + 6f * p + prog * prog * 46f * p + sin(time * 0.8f + s) * 2f * p // the breeze carries it off to the side
        val syy = pitY - 22f * p - prog * 60f * p
        scope.pCircle(Color(0xFFB0B7BF).copy(alpha = smokeA * (1f - prog)), (3f + prog * 7f) * p, Offset(sx, syy))
    }

    // 12. Marshmallows on sticks after the fire is tapped
    if (engine.marshmallowRoastingTimer > 0f) {
        val bStickX1 = cw * engine.boy.worldX + 6f * p
        val bStickY1 = ch * engine.boy.worldY - 10f * p
        val bMallowX = pitX - 4f * p
        val bMallowY = pitY - 10f * p
        scope.pLine(Color(0xFF5D4037), Offset(bStickX1, bStickY1), Offset(bMallowX, bMallowY), strokeWidth = 1.6f * p)
        scope.pRoundRect(Color(0xFFFFFEE0), Offset(bMallowX - 2.5f * p, bMallowY - 2.5f * p), Size(5f * p, 4f * p), CornerRadius(1.5f * p, 1.5f * p))
        scope.pRect(Color(0xFFD4A373), Offset(bMallowX - p, bMallowY - 2.5f * p), Size(2f * p, 4f * p))
        val gStickX1 = cw * engine.girl.worldX - 6f * p
        val gStickY1 = ch * engine.girl.worldY - 10f * p
        val gMallowX = pitX + 4f * p
        val gMallowY = pitY - 9f * p
        scope.pLine(Color(0xFF5D4037), Offset(gStickX1, gStickY1), Offset(gMallowX, gMallowY), strokeWidth = 1.6f * p)
        scope.pRoundRect(Color(0xFFFFFEE0), Offset(gMallowX - 2.5f * p, gMallowY - 2.5f * p), Size(5f * p, 4f * p), CornerRadius(1.5f * p, 1.5f * p))
        scope.pRect(Color(0xFFD4A373), Offset(gMallowX - p, gMallowY - 2.5f * p), Size(2f * p, 4f * p))
        val puffY = bMallowY - 6f * p - (time * 10f % (14f * p))
        scope.pCircle(Color.White.copy(alpha = 0.5f), 2f * p, Offset(bMallowX, puffY))
    }

    // 13. Fireflies drifting near the trees after dark
    if (isNight || isSunset) {
        for (i in 0 until 14) {
            val fxp = nz(i, 161) * cw + sin(time * 0.6f + i) * 10f * p
            val fyp = horizon + (2f + nz(i, 162) * 26f) * p + cos(time * 0.8f + i * 1.3f) * 4f * p
            val blink = (0.5f + 0.5f * sin(time * (1.5f + nz(i, 163)) + i * 2f)).coerceIn(0f, 1f)
            if (blink < 0.2f) continue
            scope.pCircle(Color(0xFFD9F99D).copy(alpha = 0.25f * blink), 2.6f * p, Offset(fxp, fyp))
            scope.px(Color(0xFFF0FFB0).copy(alpha = blink), fxp - 0.5f * p, fyp - 0.5f * p, 1f * p, 1f * p)
        }
    }

    // 14. Ferns framing the bottom edge, darkest nearest the viewer
    val fern = if (isNight) Color(0xFF0B1611) else if (snowing) Color(0xFF8FA39A) else Color(0xFF2F5A3A)
    for (i in 0 until 9) {
        val bx2 = cw * (i / 8f) + (nz(i, 171) - 0.5f) * 20f * p
        val sway = sin(time * 1.2f + i) * 1.2f * p
        val h = (10f + nz(i, 172) * 10f) * p
        for (leaf in 0..3) {
            val side = if (leaf % 2 == 0) -1f else 1f
            scope.pLine(fern, Offset(bx2, ch), Offset(bx2 + side * (4f + leaf * 3f) * p + sway, ch - h + leaf * 2f * p), strokeWidth = 1.6f * p)
        }
    }
}

/** A low table with a teapot and two cups; the pot steams, and a tealight glows in the evening. */
private fun drawSunroomTeaTable(scope: DrawScope, cx: Float, baseY: Float, s: Float, time: Float, evening: Boolean) {
    scope.pOval(Color(0x33000000), Offset(cx - 18f * s, baseY - 1f * s), Size(36f * s, 4f * s))
    scope.px(Color(0xFF6B4423), cx - 15f * s, baseY - 8f * s, 2f * s, 8f * s)
    scope.px(Color(0xFF6B4423), cx + 13f * s, baseY - 8f * s, 2f * s, 8f * s)
    scope.pOval(Color(0xFF8A5B3D), Offset(cx - 18f * s, baseY - 11f * s), Size(36f * s, 5f * s))
    scope.pOval(Color(0xFFA9744F), Offset(cx - 17f * s, baseY - 11.5f * s), Size(34f * s, 3.5f * s))
    // teapot
    scope.pOval(Color(0xFFF4F1EA), Offset(cx - 5f * s, baseY - 18f * s), Size(10f * s, 8f * s))
    scope.px(Color(0xFF7FB7BE), cx - 5f * s, baseY - 15f * s, 10f * s, 1.2f * s)
    scope.px(Color(0xFFF4F1EA), cx - 1.2f * s, baseY - 19.5f * s, 2.4f * s, 2f * s)
    scope.pLine(Color(0xFFF4F1EA), Offset(cx + 4.5f * s, baseY - 14f * s), Offset(cx + 8f * s, baseY - 17f * s), strokeWidth = 1.4f * s)
    scope.pArc(Color(0xFFF4F1EA), 90f, 180f, false, Offset(cx - 8f * s, baseY - 17f * s), Size(4f * s, 5f * s), style = Stroke(1f * s))
    for (i in 0..2) {
        val rise = (time * 6f + i * 4f) % (10f * s)
        scope.pCircle(Color.White.copy(alpha = 0.45f * (1f - rise / (10f * s))), 1.2f * s, Offset(cx + 8f * s + sin(time * 2f + i) * s, baseY - 18f * s - rise))
    }
    // two cups
    for (c in 0..1) {
        val ux = cx + (if (c == 0) -13f else 10f) * s
        scope.px(Color(0xFFF4F1EA), ux, baseY - 13.5f * s, 4f * s, 3f * s)
        scope.px(Color(0xFFB08968), ux + 0.6f * s, baseY - 13.5f * s, 2.8f * s, 0.8f * s)
    }
    if (evening) {
        scope.pCircle(Color(0xFFFFC46B).copy(alpha = 0.25f), 6f * s, Offset(cx + 1f * s, baseY - 12f * s))
        scope.px(Color(0xFFFFB347), cx + 0.5f * s, baseY - 13f * s, 1f * s, 1.6f * s)
    }
}

/** A small cafe table with two bentwood chairs, drawn at scale [s] (bigger = nearer the viewer). */
private fun drawCafeTableSet(scope: DrawScope, cx: Float, baseY: Float, s: Float, time: Float, evening: Boolean, reserved: Boolean) {
    val wood = Color(0xFF4A2D1D)
    val woodLight = Color(0xFF6B4423)
    scope.pOval(Color(0x33000000), Offset(cx - 26f * s, baseY - 2f * s), Size(52f * s, 5f * s))
    for (side in 0..1) {
        val chX = cx + (if (side == 0) -20f else 12f) * s
        scope.px(wood, chX, baseY - 26f * s, 1.6f * s, 26f * s)
        scope.px(wood, chX + 7f * s, baseY - 12f * s, 1.6f * s, 12f * s)
        scope.px(woodLight, chX - 0.5f * s, baseY - 13f * s, 9.5f * s, 2.4f * s)
        scope.pArc(wood, 180f, 180f, false, Offset(chX - 1f * s, baseY - 30f * s), Size(9f * s, 8f * s), style = Stroke(1.4f * s))
    }
    scope.px(wood, cx - 1.2f * s, baseY - 16f * s, 2.4f * s, 16f * s)
    scope.px(wood, cx - 6f * s, baseY - 1.5f * s, 12f * s, 1.5f * s)
    scope.pOval(woodLight, Offset(cx - 13f * s, baseY - 19f * s), Size(26f * s, 4f * s))
    scope.pOval(Color(0xFF8B5A2B), Offset(cx - 12f * s, baseY - 19.5f * s), Size(24f * s, 2.8f * s))
    if (reserved) {
        scope.px(Color(0xFFF4F1EA), cx - 4f * s, baseY - 25f * s, 8f * s, 5.5f * s)
        scope.px(Color(0xFFC0392B), cx - 3f * s, baseY - 23.5f * s, 6f * s, 0.8f * s)
        scope.px(Color(0xFF9A9A9A), cx - 3f * s, baseY - 22f * s, 4f * s, 0.6f * s)
    } else {
        scope.px(Color(0xFFF4F1EA), cx - 6f * s, baseY - 22f * s, 4f * s, 3f * s)
        scope.px(Color(0xFFEDE6D6), cx + 1f * s, baseY - 20.6f * s, 8f * s, 1.2f * s)
    }
    scope.px(Color(0x99E8D5B5), cx + 5f * s, baseY - 22.5f * s, 3f * s, 3f * s)
    if (evening) {
        scope.pCircle(Color(0xFFFFC46B).copy(alpha = 0.22f), 5f * s, Offset(cx + 6.5f * s, baseY - 22f * s))
        scope.px(Color(0xFFFFB347), cx + 6.2f * s, baseY - 23.5f * s + sin(time * 9f) * 0.3f * s, 0.7f * s, 1.2f * s)
    }
}
