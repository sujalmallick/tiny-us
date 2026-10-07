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

fun drawKitchenTreatJar(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    val jar = com.example.scene.KitchenLayout.treatJar(cw, ch, p)
    val x = jar.x
    val y = jar.y
    // Checker-glass jar with a warm gingham lid and a few visible biscuits.
    scope.drawRect(Color(0xFF8A5636), Offset(x - 10f * p, y - 15f * p), Size(20f * p, 20f * p))
    scope.drawRect(Color(0xFFD8B89A), Offset(x - 8f * p, y - 14f * p), Size(16f * p, 16f * p))
    scope.drawRect(Color(0x667AC6D4), Offset(x - 7f * p, y - 12f * p), Size(14f * p, 13f * p))
    scope.drawRect(Color(0xFFE5A85B), Offset(x - 6f * p, y - 4f * p), Size(5f * p, 4f * p))
    scope.drawRect(Color(0xFFFFD38A), Offset(x + 1f * p, y - 7f * p), Size(5f * p, 4f * p))
    scope.drawRect(Color(0xFF70452F), Offset(x - 10f * p, y - 17f * p), Size(20f * p, 3f * p))
    scope.drawRect(Color(0xFFC78A56), Offset(x - 4f * p, y - 20f * p), Size(8f * p, 3f * p))
    scope.drawRect(Color(0xFFFFF1CF), Offset(x - 5f * p, y - 11f * p), Size(2f * p, 6f * p))
    if (engine.catTreatJarTimer > 0f) {
        val t = ((0.85f - engine.catTreatJarTimer) / 0.85f).coerceIn(0f, 1f)
        scope.drawRect(Color(0xFF70452F), Offset(x - 9f * p, y - 19f * p - 4f * p * t), Size(18f * p, 2.5f * p))
    }
    if (engine.catTreatDropTimer > 0f) {
        val t = (1f - engine.catTreatDropTimer / 0.48f).coerceIn(0f, 1f)
        val treatX = x + (cw * 0.48f - x) * t
        val treatY = y + (ch * 0.72f - y) * t + sin(t * kotlin.math.PI.toFloat()) * 9f * p
        scope.drawCircle(Color(0xFFE6AA5C), 3.5f * p, Offset(treatX, treatY))
        scope.drawCircle(Color(0xFFFFD78C), 1.8f * p, Offset(treatX - 0.8f * p, treatY - 0.7f * p))
    } else if (engine.isCatTreatOnFloor) {
        scope.drawCircle(Color(0xFFE6AA5C), 3.5f * p, Offset(cw * 0.48f, ch * 0.72f))
        scope.drawCircle(Color(0xFFFFD78C), 1.8f * p, Offset(cw * 0.48f - 0.8f * p, ch * 0.72f - 0.7f * p))
    }
    if (engine.catTreatMunchTimer > 0f && sin(time * 15f) > 0f) {
        scope.drawRect(Color(0xFFFF7194), Offset(cw * 0.48f - 2f * p, ch * 0.72f - 19f * p), Size(1.4f * p, 1.4f * p))
        scope.drawRect(Color(0xFFFF7194), Offset(cw * 0.48f + 2f * p, ch * 0.72f - 19f * p), Size(1.4f * p, 1.4f * p))
    }
}

fun drawKitchenRoom(
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
    val floorY = com.example.scene.KitchenLayout.floorY(ch)
    val ceilingY = com.example.scene.KitchenLayout.ceilingY(ch)

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
    val tile1 = com.example.scene.KitchenLayout.FLOOR_TILE_A
    val tile2 = com.example.scene.KitchenLayout.FLOOR_TILE_B
    val tileSize = com.example.scene.KitchenLayout.FLOOR_TILE * p
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
    val winW = com.example.scene.KitchenLayout.WINDOW_W * p
    val winH = com.example.scene.KitchenLayout.WINDOW_H * p
    val winX = cw * 0.28f - winW / 2f
    val winY = com.example.scene.KitchenLayout.windowTop(ch, p)
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
    val clockCenter = com.example.scene.KitchenLayout.clockCenter(cw, ch, p)
    val clockX = clockCenter.x
    val clockY = clockCenter.y

    // Sage upper cabinets on the wall to the right, matching the fridge.
    run {
        val sage = Color(0xFFA7C4B5)
        val sageDark = Color(0xFF8BA899)
        val sageLight = Color(0xFFC3DBD0)
        val brass = Color(0xFFE0B868)
        val cabTop = winY + 2 * p
        val cabH = 22 * p
        val doorW = 13 * p
        val left = cw * 0.585f
        val w = 2 * doorW + 3 * p
        scope.drawRect(sageDark, Offset(left, cabTop), Size(w, cabH))
        scope.drawRect(sageDark, Offset(left - p, cabTop + cabH), Size(w + 2 * p, 2 * p))
        for (d in 0 until 2) {
            val dx = left + p + d * (doorW + p)
            scope.drawRect(sage, Offset(dx, cabTop + p), Size(doorW, cabH - 2 * p))
            scope.drawRect(sageLight, Offset(dx + p, cabTop + 2 * p), Size(doorW - 2 * p, p))
            scope.drawRect(brass, Offset(if (d % 2 == 0) dx + doorW - 3 * p else dx + p, cabTop + cabH - 7 * p), Size(2 * p, 2 * p))
        }
    }
    val clockR = 7.5f * p
    scope.drawCircle(Color(0xFF6B7F6E), clockR + 1.5f * p, Offset(clockX, clockY))
    scope.drawCircle(Color(0xFFFCF6BD), clockR, Offset(clockX, clockY))
    scope.drawCircle(Color(0xFF333333), 1.2f * p, Offset(clockX, clockY))
    val minuteAngle = (timeSeconds * 0.1f) % (2f * kotlin.math.PI.toFloat())
    val hourAngle = (timeSeconds * 0.015f + 1.2f) % (2f * kotlin.math.PI.toFloat())
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
    // Mochi's treat jar sits on top of the fridge (drawKitchenTreatJar).

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
    drawContactShadow(scope, cw * 0.50f, tblY + tblH, 54, p)
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
        drawContactShadow(scope, chairX, chY + chH, 12, p)
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
    drawContactShadow(scope, stlX, stlY + stlH, 15, p)
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
    drawContactShadow(scope, crateX, crateY + crateH, 26, p)
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
    drawContactShadow(scope, sackX + 6 * p, sackY + 15 * p, 14, p)
    scope.drawRect(Color(0xFFD4A373), Offset(sackX, sackY), Size(12 * p, 15 * p))
    scope.drawRect(Color(0xFFB08968), Offset(sackX + 1.5f * p, sackY + 1.5f * p), Size(9 * p, 11 * p))
    scope.drawRect(Color(0xFF7F5539), Offset(sackX + 3.5f * p, sackY - 2 * p), Size(5 * p, 2.5f * p))

    // 15. Right Corner: Glazed Ceramic Floor Planter with Split Monstera (anchors right corner)
    val plantX = cw * 0.86f
    val plantY = floorY + floorH * 0.74f
    val floorPotW = 18 * p
    val floorPotH = 14 * p
    val pLeft = plantX - floorPotW / 2f
    drawContactShadow(scope, plantX, plantY + floorPotH + 5 * p, 19, p)
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

fun drawLivingRoom(
    scope: DrawScope,
    cw: Float,
    ch: Float,
    isNight: Boolean,
    p: Float,
    lampLit: Boolean = true,
    couchPhase: CouchPhase = CouchPhase.NIGHT,
    timeSeconds: Float = 0f,
    candleLit: Boolean = true,
    roomTheme: RoomTheme = RoomTheme.WARM_AUTUMN_COTTAGE
) {
    val floorY = ch * 0.65f
    val wallColor = when {
        lampLit -> roomTheme.wall
        couchPhase == CouchPhase.NIGHT -> Color(0xFF78798C) // Dim cozy midnight slate
        couchPhase == CouchPhase.DAY -> Color(0xFFF4ECE1) // Morning daylight cream
        couchPhase == CouchPhase.MIDDAY -> Color(0xFFFFF9EE) // Bright noon sunlight
        couchPhase == CouchPhase.EVENING -> Color(0xFFE5B5A2) // Warm sunset peach
        else -> if (isNight) Color(0xFFBCB1A6) else Color(0xFFF6EDE2)
    }
    scope.drawRect(wallColor, Offset.Zero, Size(cw, floorY))

    // Warm low wainscot breaks up the tall wall while keeping the existing room layout intact.
    val panelTop = floorY * 0.76f
    val panelColor = when {
        !lampLit && couchPhase == CouchPhase.NIGHT -> Color(0xFF6A6975)
        couchPhase == CouchPhase.EVENING -> Color(0xFFD8A895)
        else -> roomTheme.panel
    }
    scope.drawRect(panelColor, Offset(0f, panelTop), Size(cw, floorY - panelTop - 3 * p))
    scope.drawRect(Color(0xFF8A6247).copy(alpha = 0.48f), Offset(0f, panelTop), Size(cw, 2.2f * p))
    scope.drawRect(Color(0xFFFFF7EB).copy(alpha = 0.38f), Offset(0f, panelTop + 2.2f * p), Size(cw, 1.2f * p))
    val panelStep = cw / 9f
    var panelX = panelStep
    while (panelX < cw) {
        scope.drawRect(Color(0xFF8A6247).copy(alpha = 0.15f), Offset(panelX, panelTop + 5 * p), Size(1.1f * p, floorY - panelTop - 12 * p))
        scope.drawRect(Color(0xFFFFF7EB).copy(alpha = 0.22f), Offset(panelX + 1.1f * p, panelTop + 5 * p), Size(0.8f * p, floorY - panelTop - 12 * p))
        panelX += panelStep
    }

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
    val rugColor = if (!lampLit && couchPhase == CouchPhase.NIGHT) roomTheme.rug.copy(alpha = 0.72f) else roomTheme.rug
    val rugBorder = roomTheme.rugTrim
    val rugCx = cw * 0.50f
    val rugW = 88 * p
    val rugH = 46 * p
    val rugY = floorY + 4 * p
    scope.drawRect(rugBorder, Offset(rugCx - rugW / 2f, rugY), Size(rugW, rugH))
    scope.drawRect(rugColor, Offset(rugCx - rugW / 2f + 2.5f * p, rugY + 2.5f * p), Size(rugW - 5 * p, rugH - 5 * p))
    scope.drawRect(Color(0xFFFFE1C7).copy(alpha = 0.25f), Offset(rugCx - rugW / 2f + 4 * p, rugY + 4 * p), Size(rugW - 8 * p, 1.1f * p))
    scope.drawRect(Color(0xFF633F3B).copy(alpha = 0.22f), Offset(rugCx - rugW / 2f + 4 * p, rugY + rugH - 5 * p), Size(rugW - 8 * p, 1.1f * p))
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
    drawContactShadow(scope, cw * 0.50f, tblY + tblH, 50, p)
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
    scope.drawRect(roomTheme.mug, Offset(mug1X, mugY), Size(4 * p, 4.5f * p))
    scope.drawRect(roomTheme.mug, Offset(mug1X - 1.2f * p, mugY + p), Size(1.2f * p, 2.5f * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(mug1X + 0.8f * p, mugY + 0.6f * p), Size(2.4f * p, 1.2f * p))
    scope.drawRect(Color.White, Offset(mug1X + p, mugY + 0.8f * p), Size(p, 0.8f * p))
    // Girl's soft blush mug
    scope.drawRect(roomTheme.mugAccent, Offset(mug2X, mugY), Size(4 * p, 4.5f * p))
    scope.drawRect(roomTheme.mugAccent, Offset(mug2X + 4 * p, mugY + p), Size(1.2f * p, 2.5f * p))
    scope.drawRect(Color(0xFF5C3A21), Offset(mug2X + 0.8f * p, mugY + 0.6f * p), Size(2.4f * p, 1.2f * p))
    scope.drawRect(Color.White, Offset(mug2X + 1.8f * p, mugY + 0.8f * p), Size(p, 0.8f * p))
    // Steam from mugs
    if (sin(timeSeconds * 3f) > 0.2f) {
        scope.drawRect(Color(0x77FFFFFF), Offset(mug1X + 1.5f * p, mugY - 3 * p), Size(1.2f * p, 2 * p))
        scope.drawRect(Color(0x77FFFFFF), Offset(mug2X + 1.5f * p, mugY - 3 * p), Size(1.2f * p, 2 * p))
    }
    // Aromatherapy candle on coffee table
    val cndX = tblX + 37 * p
    WorldSprites.drawAromatherapyCandle(scope, cndX + 2.2f * p, tblY, p, candleLit, timeSeconds)

    // 3. Knitted Round Floor Pouf / Ottoman (at right edge of rug, beside table)
    val pfX = cw * 0.70f
    val pfY = floorY + 25 * p
    val pfW = 18 * p
    val pfH = 12 * p
    val pfLeft = pfX - pfW / 2f
    drawContactShadow(scope, pfX, pfY + pfH, 19, p)
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
    drawContactShadow(scope, cBoxX, cBoxY + cBoxH, 22, p)
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
    drawContactShadow(scope, bskX, bskY + bskH, 20, p)
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
    drawContactShadow(scope, rackX, rackY + rackH, 28, p)
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


fun drawCottageInteriorDoor(
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
