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
import com.example.scene.CafeLayout
import com.example.scene.CampfireLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal fun drawRainyCafeScene(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    // 1. Warm interior cafe walls: Exposed warm brick and oak wainscoting
    val brickBase = Color(0xFF6E4033)
    val brickHighlight = Color(0xFF8A5343)
    val mortar = Color(0xFF4A2B22)
    val oakWainscot = Color(0xFF4E3122)
    val oakTrim = Color(0xFF6A432E)
    val floorColor = Color(0xFF382319)
    val floorPlank = Color(0xFF2C1B13)

    // Base brick wall
    scope.drawRect(brickBase, Offset.Zero, Size(cw, ch * 0.60f))
    // Staggered brick rows
    for (row in 0..11) {
        val ry = row * (ch * 0.05f)
        scope.drawRect(mortar, Offset(0f, ry), Size(cw, 1.2f * p))
        val rowOffset = if (row % 2 == 0) 0f else (cw * 0.05f)
        for (col in 0..10) {
            val rx = col * (cw * 0.10f) + rowOffset
            scope.drawRect(mortar, Offset(rx, ry), Size(1.2f * p, ch * 0.05f))
            scope.drawRect(brickHighlight, Offset(rx + 2f * p, ry + 1.5f * p), Size(cw * 0.09f, 2f * p))
        }
    }

    // Oak wainscot paneling lower wall
    scope.drawRect(oakWainscot, Offset(0f, ch * 0.52f), Size(cw, ch * 0.16f))
    scope.drawRect(oakTrim, Offset(0f, ch * 0.52f), Size(cw, 3f * p))
    // Dark hardwood floor
    scope.drawRect(floorColor, Offset(0f, ch * 0.68f), Size(cw, ch * 0.32f))
    for (plank in 1..4) {
        scope.drawRect(floorPlank, Offset(0f, ch * (0.68f + plank * 0.08f)), Size(cw, 1.5f * p))
    }

    // 2. Hanging Pendant Edison Lamps
    val lampCoords = listOf(cw * 0.22f, cw * 0.70f)
    for (lx in lampCoords) {
        // Black wire from ceiling
        scope.drawRect(Color(0xFF1E140F), Offset(lx - 0.7f * p, 0f), Size(1.4f * p, ch * 0.10f))
        // Brass socket fixture
        scope.drawRect(Color(0xFFB07D38), Offset(lx - 3f * p, ch * 0.10f), Size(6f * p, 4f * p))
        // Glass bulb
        scope.drawOval(Color(0xFFFFF2B2).copy(alpha = 0.85f), Offset(lx - 4.5f * p, ch * 0.10f + 3f * p), Size(9f * p, 11f * p))
        // Glowing filament
        val bulbFlicker = sin(time * 8f) * 1.5f * p
        scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.30f), 22f * p + bulbFlicker, Offset(lx, ch * 0.10f + 8f * p))
        scope.drawCircle(Color(0xFFFFEAA7), 2f * p, Offset(lx, ch * 0.10f + 8f * p))
    }

    // 3. Large Rainy Street Window Frame (Center & Right)
    val window = CafeLayout.window(cw, ch)
    val winLeft = window.left
    val winTop = window.top
    val winW = window.width
    val winH = window.height
    val winFrameDark = Color(0xFF32221A)
    val winFrameOak = Color(0xFF5A3825)
    val rainySky = Color(0xFF455A64)
    val distantCity = Color(0xFF2E3E47)
    val distantLight = Color(0xFFFFD166)

    // Outer and inner frame
    scope.drawRect(winFrameDark, Offset(winLeft, winTop), Size(winW, winH))
    scope.drawRect(winFrameOak, Offset(winLeft + 3f * p, winTop + 3f * p), Size(winW - 6f * p, winH - 6f * p))
    // Glass pane showing rainy outside street
    scope.drawRect(rainySky, Offset(winLeft + 6f * p, winTop + 6f * p), Size(winW - 12f * p, winH - 12f * p))

    // Distant street rooftops & glowing warm streetlamp through rain
    scope.drawRect(distantCity, Offset(winLeft + 6f * p, winTop + winH * 0.45f), Size(winW - 12f * p, winH * 0.55f))
    for (i in 0..4) {
        val bldgX = winLeft + 10f * p + i * (winW * 0.18f)
        val bldgH = (18f + (i * 9 % 11) * 3f) * p
        scope.drawRect(Color(0xFF263238), Offset(bldgX, winTop + winH * 0.45f - bldgH), Size(winW * 0.14f, bldgH))
        scope.drawRect(distantLight.copy(alpha = 0.45f), Offset(bldgX + 4f * p, winTop + winH * 0.45f - bldgH + 4f * p), Size(3f * p, 4f * p))
    }
    scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.40f), 10f * p, Offset(winLeft + winW * 0.78f, winTop + winH * 0.40f))

    // 4. ANIMATED CHARACTER: Rainy Street Umbrella Passerby (walking outside on sidewalk)
    val passer = CafeLayout.passerby(cw, ch, p, time)
    if (passer != null) {
        val passerX = passer.x
        val passerY = passer.y
        val umbW = 20f * p
        val umbH = 10f * p
        scope.drawOval(Color(0xFFD90429), Offset(passerX - umbW / 2f, passerY - 22f * p), Size(umbW, umbH))
        scope.drawOval(Color(0xFFEF233C), Offset(passerX - umbW / 2f + 2f * p, passerY - 21f * p), Size(umbW - 4f * p, umbH * 0.6f))
        scope.drawLine(Color(0xFF2B2B2B), Offset(passerX, passerY - 17f * p), Offset(passerX, passerY - 10f * p), strokeWidth = 1.5f * p)
        scope.drawCircle(Color(0xFFFFD8B8), 2.5f * p, Offset(passerX, passerY - 11f * p))
        scope.drawRect(Color(0xFF4A4E69), Offset(passerX - 3.5f * p, passerY - 8f * p), Size(7f * p, 8f * p))
        val legCycle = sin(time * 10f) * 2.5f * p
        scope.drawLine(Color(0xFF22223B), Offset(passerX - 1.5f * p, passerY), Offset(passerX - 1.5f * p - legCycle, passerY + 5f * p), strokeWidth = 1.8f * p)
        scope.drawLine(Color(0xFF22223B), Offset(passerX + 1.5f * p, passerY), Offset(passerX + 1.5f * p + legCycle, passerY + 5f * p), strokeWidth = 1.8f * p)
    }

    // Window Muntin Grid
    scope.drawRect(winFrameOak, Offset(winLeft + winW * 0.50f - 2f * p, winTop + 6f * p), Size(4f * p, winH - 12f * p))
    scope.drawRect(winFrameOak, Offset(winLeft + 6f * p, winTop + winH * 0.48f - 2f * p), Size(winW - 12f * p, 4f * p))

    // Slanted Falling Rain & Sliding Condensation Beads
    for (i in 0..22) {
        val rx = winLeft + 8f * p + ((i * 47) % 89) / 90f * (winW - 16f * p)
        val rSpeed = 0.18f + (i % 5) * 0.04f
        val rFall = (time * rSpeed + i * 0.09f) % 1f
        val ry = winTop + 6f * p + rFall * (winH - 14f * p)
        scope.drawLine(
            Color(0xFFCFE8EF).copy(alpha = 0.65f),
            Offset(rx + 2f * p, ry),
            Offset(rx, ry + (4f + i % 3) * p),
            strokeWidth = 1.2f * p
        )
    }

    // Condensation Fog & Drawn Heart
    if (engine.cafeWindowHeartTimer > 0f) {
        val alpha = (engine.cafeWindowHeartTimer / 2.8f).coerceIn(0f, 1f)
        val hx = cw * engine.cafeWindowHeartX
        val hy = ch * engine.cafeWindowHeartY
        val s = 1.6f * p
        val c = Color(0xFFFFB5CA).copy(alpha = alpha)
        scope.drawRect(c, Offset(hx - 2 * s, hy), Size(2 * s, 2 * s))
        scope.drawRect(c, Offset(hx + s, hy), Size(2 * s, 2 * s))
        scope.drawRect(c, Offset(hx - 3 * s, hy + s), Size(7 * s, 2 * s))
        scope.drawRect(c, Offset(hx - 2 * s, hy + 3 * s), Size(5 * s, s))
        scope.drawRect(c, Offset(hx - s, hy + 4 * s), Size(3 * s, s))
    }

    // 5. FRAMED CHALKBOARD MENU (Center Wall, left of window)
    val menuTopLeft = CafeLayout.menuTopLeft(cw, ch, p)
    val menuX = menuTopLeft.x
    val menuY = menuTopLeft.y
    val menuW = 32f * p
    val menuH = 42f * p
    scope.drawRect(Color(0xFF4A3425), Offset(menuX, menuY), Size(menuW, menuH))
    scope.drawRect(Color(0xFF1E282D), Offset(menuX + 2.5f * p, menuY + 2.5f * p), Size(menuW - 5f * p, menuH - 5f * p))
    scope.drawRect(Color(0xFFFFEEAA), Offset(menuX + 6f * p, menuY + 6f * p), Size(menuW - 12f * p, 2f * p))
    for (line in 0..3) {
        val ly = menuY + 13f * p + line * 6f * p
        val lw = (menuW - 14f * p) * (if (line == 2) 0.6f else 0.85f)
        scope.drawRect(Color(0xFFE2EFF2).copy(alpha = 0.75f), Offset(menuX + 5f * p, ly), Size(lw, 1.3f * p))
    }
    scope.drawRect(Color(0xFFFF9AA2), Offset(menuX + 7f * p, menuY + 34f * p), Size(5f * p, 3.5f * p))
    scope.drawCircle(Color(0xFFFF9AA2), 1.2f * p, Offset(menuX + 13f * p, menuY + 36f * p))

    // 6. THE BARISTA COUNTER & LEO THE BARISTA (Left side)
    val barX = CafeLayout.barX(cw)
    val barY = CafeLayout.barY(ch)
    val barW = CafeLayout.barW(cw)
    val barH = ch * 0.20f

    for (shelf in 0..1) {
        val sy = ch * (0.24f + shelf * 0.12f)
        scope.drawRect(Color(0xFF5A3825), Offset(barX, sy), Size(barW, 3f * p))
        scope.drawRect(Color(0xFF6B4423), Offset(barX + 6f * p, sy - 9f * p), Size(7f * p, 9f * p))
        scope.drawRect(Color(0xFFD4A373), Offset(barX + 7f * p, sy - 8f * p), Size(5f * p, 7f * p))
        scope.drawRect(Color(0xFF2C190D), Offset(barX + 8f * p, sy - 6f * p), Size(3f * p, 4f * p))
        scope.drawRect(Color(0xFFE76F51), Offset(barX + 20f * p, sy - 7f * p), Size(6f * p, 7f * p))
        scope.drawRect(Color(0xFF2A9D8F), Offset(barX + 28f * p, sy - 7f * p), Size(6f * p, 7f * p))
    }

    // LEO THE BARISTA SPRITE (standing behind the counter)
    val barista = CafeLayout.barista(cw, ch, p)
    val baristaX = barista.x
    val baristaY = barista.y
    scope.drawCircle(Color(0xFFFFD8B8), 4.5f * p, Offset(baristaX, baristaY - 24f * p))
    scope.drawRect(Color(0xFF4A3425), Offset(baristaX - 5.5f * p, baristaY - 29f * p), Size(11f * p, 4f * p))
    scope.drawRect(Color(0xFF5A3825), Offset(baristaX - 6.5f * p, baristaY - 26f * p), Size(13f * p, 1.8f * p))
    scope.drawRect(Color(0xFF2C190D), Offset(baristaX - 4.5f * p, baristaY - 26f * p), Size(2f * p, 4f * p))
    scope.drawRect(Color(0xFF2C190D), Offset(baristaX + 2.5f * p, baristaY - 26f * p), Size(2f * p, 4f * p))
    scope.drawRect(Color(0xFFD00000), Offset(baristaX - p, baristaY - 22f * p), Size(2f * p, p))
    scope.drawRect(Color(0xFFFFF1E6), Offset(baristaX - 5f * p, baristaY - 19f * p), Size(10f * p, 19f * p))
    scope.drawRect(Color(0xFF38523A), Offset(baristaX - 4f * p, baristaY - 17f * p), Size(8f * p, 17f * p))
    scope.drawLine(Color(0xFF2B3E2D), Offset(baristaX - 3f * p, baristaY - 19f * p), Offset(baristaX - 3f * p, baristaY - 17f * p), strokeWidth = 1.2f * p)
    scope.drawLine(Color(0xFF2B3E2D), Offset(baristaX + 3f * p, baristaY - 19f * p), Offset(baristaX + 3f * p, baristaY - 17f * p), strokeWidth = 1.2f * p)

    // Stainless Steel & Copper Espresso Machine on counter
    val espX = barX + 3f * p
    val espY = barY - 16f * p
    val espW = 20f * p
    val espH = 18f * p
    scope.drawRoundRect(Color(0xFFB0BEC5), Offset(espX, espY), Size(espW, espH), CornerRadius(3f * p, 3f * p))
    scope.drawRect(Color(0xFFCFD8DC), Offset(espX + 2f * p, espY + 2f * p), Size(espW - 4f * p, espH * 0.45f))
    scope.drawCircle(Color(0xFFE9C46A), 2.5f * p, Offset(espX + 6f * p, espY + 5f * p))
    scope.drawRect(Color(0xFF37474F), Offset(espX + 11f * p, espY + 9f * p), Size(6f * p, 3f * p))
    scope.drawLine(Color(0xFF78909C), Offset(espX + 18f * p, espY + 6f * p), Offset(espX + 19f * p, espY + 14f * p), strokeWidth = 1.5f * p)

    val steamRise = (time * 16f) % (18f * p)
    scope.drawCircle(Color.White.copy(alpha = 0.55f), 2f * p, Offset(espX + 19f * p + sin(time * 3f) * 2f * p, espY + 12f * p - steamRise))
    if (engine.cafeBaristaBrewTimer > 0f) {
        for (st in 0..3) {
            val sx = espX + 18f * p + sin(time * 6f + st) * 3f * p
            val sy = espY + 10f * p - ((time * 24f + st * 6f) % (24f * p))
            scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.5f), 3f * p, Offset(sx, sy))
        }
    }

    // Wooden Counter Base
    scope.drawRect(Color(0xFF5A3825), Offset(barX, barY), Size(barW, barH))
    scope.drawRect(Color(0xFF3E2718), Offset(barX, barY + 4f * p), Size(barW, 2f * p))
    scope.drawRect(Color(0xFFE0C9A6), Offset(barX - 2f * p, barY - 2f * p), Size(barW + 4f * p, 4f * p))

    // 7. COUPLE'S CAFE TABLE & DELIGHTFUL PROPS (Center)
    val tblX = CafeLayout.tableX(cw)
    val tblY = CafeLayout.tableY(ch)
    val tblW = CafeLayout.tableW(cw)
    val tblH = ch * 0.12f

    // Walnut Table Surface
    scope.drawRoundRect(Color(0xFF6B4423), Offset(tblX, tblY), Size(tblW, 5f * p), CornerRadius(2.5f * p, 2.5f * p))
    scope.drawRect(Color(0xFF8B5A2B), Offset(tblX + 2f * p, tblY + p), Size(tblW - 4f * p, 2f * p))
    scope.drawRect(Color(0xFF3E2718), Offset(tblX + tblW / 2f - 3f * p, tblY + 5f * p), Size(6f * p, tblH - 5f * p))
    scope.drawRoundRect(Color(0xFF3E2718), Offset(tblX + tblW / 2f - 12f * p, tblY + tblH - 4f * p), Size(24f * p, 4f * p), CornerRadius(2f * p, 2f * p))

    // A. Heart Foam Latte
    val latte = CafeLayout.latte(cw, ch, p)
    val latteX = latte.x
    val latteY = latte.y
    scope.drawOval(Color(0xFFEAE2B7), Offset(latteX - 8f * p, latteY + 4f * p), Size(16f * p, 4f * p))
    scope.drawRoundRect(Color(0xFFFAF0CA), Offset(latteX - 6f * p, latteY - 3f * p), Size(12f * p, 8f * p), CornerRadius(2f * p, 2f * p))
    scope.drawRect(Color(0xFF6F4E37), Offset(latteX - 4.5f * p, latteY - 2f * p), Size(9f * p, 2.5f * p))
    scope.drawArc(Color(0xFFFAF0CA), 270f, 180f, false, Offset(latteX - 9f * p, latteY - p), Size(4f * p, 5f * p), style = Stroke(1.5f * p))

    val heartColor = Color(0xFFE76F51)
    // The foam heart puffs up briefly after a tap, then settles back.
    val hs = 1.3f * p * (1f + 0.4f * (engine.cafeLatteTimer / 2.2f).coerceIn(0f, 1f))
    val hx = latteX
    val hy = latteY - 1f * p
    scope.drawRect(heartColor, Offset(hx - 2 * hs, hy), Size(2 * hs, 2 * hs))
    scope.drawRect(heartColor, Offset(hx + hs, hy), Size(2 * hs, 2 * hs))
    scope.drawRect(heartColor, Offset(hx - 3 * hs, hy + hs), Size(7 * hs, 2 * hs))
    scope.drawRect(heartColor, Offset(hx - 2 * hs, hy + 3 * hs), Size(5 * hs, hs))
    scope.drawRect(heartColor, Offset(hx - hs, hy + 4 * hs), Size(3 * hs, hs))

    for (i in 0..2) {
        val sway = sin(time * 2.4f + i) * 2f * p
        val steamY = latteY - 7f * p - ((time * 8f + i * 5f) % (12f * p))
        scope.drawCircle(Color.White.copy(alpha = 0.55f), 1.5f * p, Offset(latteX + (i - 1) * 2.5f * p + sway, steamY))
    }

    // B. Bud Vase with Daisy Flower
    val vaseX = tblX + tblW * 0.50f
    val vaseY = tblY - 11f * p
    scope.drawRoundRect(Color(0x88A8DADC), Offset(vaseX - 2.5f * p, vaseY + 4f * p), Size(5f * p, 7f * p), CornerRadius(2f * p, 2f * p))
    scope.drawLine(Color(0xFF2A9D8F), Offset(vaseX, vaseY + 4f * p), Offset(vaseX, vaseY - 2f * p), strokeWidth = 1.2f * p)
    scope.drawCircle(Color.White, 2f * p, Offset(vaseX, vaseY - 2f * p))
    scope.drawCircle(Color(0xFFE9C46A), 1.2f * p, Offset(vaseX, vaseY - 2f * p))

    // C. Flaky Croissant & Jam Plate
    val plate = CafeLayout.plate(cw, ch, p)
    val plateX = plate.x
    val plateY = plate.y
    scope.drawOval(Color(0xFFF1FAEE), Offset(plateX - 9f * p, plateY + 3f * p), Size(18f * p, 5f * p))
    val croissantW = CafeLayout.croissantWidth(engine.cafePastryBites, p)
    if (croissantW > 2f * p) {
        scope.drawRoundRect(Color(0xFFE76F51), Offset(plateX - croissantW / 2f, plateY), Size(croissantW, 5f * p), CornerRadius(2.5f * p, 2.5f * p))
        scope.drawRoundRect(Color(0xFFF4A261), Offset(plateX - croissantW / 2f + p, plateY + 0.8f * p), Size(croissantW - 2f * p, 2.5f * p), CornerRadius(1.5f * p, 1.5f * p))
    }
    scope.drawCircle(Color(0xFFE63946), 2f * p, Offset(plateX + 6f * p, plateY + 2f * p))

    // 8. BOBA THE CAFE PUP & MOCHI (Floor Rug on Right)
    val rug = CafeLayout.rug(cw, ch)
    val rugX = rug.x
    val rugY = rug.y
    val rugW = 44f * p
    val rugH = 18f * p
    scope.drawOval(Color(0xFFE9D8A6), Offset(rugX - rugW / 2f, rugY), Size(rugW, rugH))
    scope.drawOval(Color(0xFFD4A373), Offset(rugX - rugW / 2f + 2f * p, rugY + p), Size(rugW - 4f * p, rugH - 2f * p))
    scope.drawOval(Color(0xFFCCD5AE), Offset(rugX - rugW / 2f + 4f * p, rugY + 2f * p), Size(rugW - 8f * p, rugH - 4f * p))

    // BOBA THE GOLDEN PUP
    val pup = CafeLayout.pup(cw, ch, p)
    val pupX = pup.x
    val pupY = pup.y
    val pupBody = Color(0xFFE0A96D)
    val pupShadow = Color(0xFFC68B45)
    val pupEar = Color(0xFFA66E38)

    scope.drawOval(pupBody, Offset(pupX - 7f * p, pupY - 4f * p), Size(14f * p, 9f * p))
    scope.drawOval(pupShadow, Offset(pupX - 5f * p, pupY - 2f * p), Size(10f * p, 6f * p))
    scope.drawCircle(pupBody, 4f * p, Offset(pupX - 4f * p, pupY - 1f * p))
    scope.drawOval(pupEar, Offset(pupX - 8f * p, pupY - 3f * p), Size(3.5f * p, 5f * p))
    scope.drawOval(pupEar, Offset(pupX - 2f * p, pupY - 4f * p), Size(3.5f * p, 4.5f * p))
    scope.drawRect(Color(0xFFE63946), Offset(pupX - 3.5f * p, pupY + 2f * p), Size(4f * p, 1.8f * p))
    scope.drawArc(Color(0xFF3D210F), 180f, 180f, false, Offset(pupX - 5.5f * p, pupY - 2f * p), Size(2.5f * p, 1.8f * p), style = Stroke(0.9f * p))

    val tailWag = if (engine.cafePupPetTimer > 0f) sin(time * 24f) * 5f * p else sin(time * 3f) * 1.5f * p
    scope.drawLine(pupBody, Offset(pupX + 6f * p, pupY), Offset(pupX + 10f * p, pupY - 3f * p + tailWag), strokeWidth = 2.4f * p)

    if (engine.cafePupPetTimer > 0f) {
        scope.drawCircle(Color(0xFFFFCAD4), 2.5f * p, Offset(pupX, pupY - 12f * p))
    } else {
        val zFall = (time * 0.4f) % 1f
        scope.drawCircle(Color(0xFFBDE0FE).copy(alpha = 0.6f - zFall * 0.5f), (1.2f + zFall) * p, Offset(pupX - 2f * p + sin(time * 2f) * 2f * p, pupY - 8f * p - zFall * 12f * p))
    }
}

internal fun drawCottageSunroom(scope: DrawScope, cw: Float, ch: Float, p: Float, time: Float, engine: SceneEngine) {
    scope.drawRect(Color(0xFFBCDCCB), Offset.Zero, Size(cw, ch))
    scope.drawRect(Color(0xFFE7D8BB), Offset(0f, ch * 0.61f), Size(cw, ch * 0.39f))
    scope.drawRect(Color(0xFF9BC9BF), Offset(cw * 0.08f, ch * 0.05f), Size(cw * 0.84f, ch * 0.31f))
    scope.drawRect(Color(0xFFECF6DA).copy(alpha = 0.32f + 0.08f * sin(time)), Offset(cw * 0.10f, ch * 0.07f), Size(cw * 0.80f, ch * 0.27f))
    // Skylight lattice and warm sun patches.
    for (i in 0..5) scope.drawRect(Color(0xFF688F82), Offset(cw * (0.10f + i * 0.16f), ch * 0.06f), Size(2.2f * p, ch * 0.30f))
    scope.drawRect(Color(0xFF688F82), Offset(cw * 0.09f, ch * 0.20f), Size(cw * 0.82f, 2.2f * p))
    scope.drawRect(Color(0x55FFF2B2), Offset(cw * 0.16f, ch * 0.34f), Size(cw * 0.21f, ch * 0.25f))
    scope.drawRect(Color(0x55FFF2B2), Offset(cw * 0.60f, ch * 0.36f), Size(cw * 0.18f, ch * 0.22f))
    // Potting shelves with individually arranged pots and layered foliage.
    for (shelf in 0..1) {
        val sy = ch * (0.43f + shelf * 0.14f)
        scope.drawRect(Color(0xFF8A5B3D), Offset(cw * 0.10f, sy), Size(cw * 0.80f, 4f * p))
        scope.drawRect(Color(0xFFC08A5D), Offset(cw * 0.10f, sy), Size(cw * 0.80f, 1.2f * p))
        for (i in 0..5) {
            val px = cw * (0.16f + i * 0.135f)
            val potTop = sy - (10f + (i % 2) * 2f) * p
            val terracotta = if (i % 2 == 0) Color(0xFFC87652) else Color(0xFFB76648)
            scope.drawRect(terracotta, Offset(px, potTop), Size(8f * p, 9f * p))
            scope.drawRect(Color(0xFF824A3D), Offset(px - p, potTop), Size(10f * p, 2f * p))
            val leaf = if (i % 2 == 0) Color(0xFF548B5B) else Color(0xFF78A96E)
            scope.drawRect(leaf, Offset(px + 2*p, potTop - 5*p - sin(time * 1.4f + i) * p), Size(2*p, 6*p))
            scope.drawRect(leaf, Offset(px - p, potTop - 3*p), Size(4*p, 2*p))
            scope.drawRect(leaf, Offset(px + 4*p, potTop - 4*p), Size(4*p, 2*p))
            if (engine.sunroomBloomStage > i / 2) {
                val flowerY = potTop - 5*p - sin(time * 1.4f + i) * p
                scope.drawRect(Color(0xFFFFC6D6), Offset(px + 1*p, flowerY - 2*p), Size(2*p, 2*p))
                scope.drawRect(Color(0xFFFFC6D6), Offset(px + 4*p, flowerY - 2*p), Size(2*p, 2*p))
                scope.drawRect(Color(0xFFFFE08A), Offset(px + 2.5f*p, flowerY - p), Size(2*p, 2*p))
            }
        }
    }
    // Hanging fern fronds, swaying independently in the greenhouse breeze.
    for (i in 0..3) {
        val hx = cw * (0.25f + i * 0.18f)
        val sway = sin(time * 1.2f + i * 1.7f) * 2.5f * p
        scope.drawRect(Color(0xFF79583F), Offset(hx, ch * 0.05f), Size(p, 9*p))
        for (leaf in 0..3) {
            scope.drawRect(Color(0xFF3F8052), Offset(hx - (leaf % 2 + 1)*p + sway, ch * 0.12f + leaf*3*p), Size(3*p, 2*p))
            scope.drawRect(Color(0xFF70A66A), Offset(hx + p + sway, ch * 0.13f + leaf*3*p), Size(3*p, 2*p))
        }
    }
    // Watering can and brief hand-sprayed mist.
    scope.drawRect(Color(0xFF719A9A), Offset(cw * 0.15f, ch * 0.68f), Size(cw * 0.09f, ch * 0.07f))
    scope.drawRect(Color(0xFF4F777B), Offset(cw * 0.22f, ch * 0.70f), Size(cw * 0.055f, 2.4f * p))
    scope.drawRect(Color(0xFF4F777B), Offset(cw * 0.16f, ch * 0.65f), Size(cw * 0.06f, 2.2f * p))
    if (engine.sunroomMistTimer > 0f) for (i in 0..7) {
        val mx = cw * (0.27f + (i % 4) * 0.10f) + sin(time * 2f + i) * 3f * p
        val my = ch * (0.59f - (i / 4) * 0.045f - ((time * 0.16f + i * 0.11f) % 0.09f))
        scope.drawCircle(Color(0xFFAEE6E3).copy(alpha = 0.42f), (1.5f + i % 2) * p, Offset(mx, my))
    }
}

internal fun drawCampfireScene(
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
    // 1. Sky and distant pine trees
    drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, time, p, weather = engine.weather)

    val horizonY = ch * 0.58f
    val pineDark = if (isNight) Color(0xFF0F1E16) else if (isSunset) Color(0xFF2C1E26) else Color(0xFF1E3A2B)
    val pineMid = if (isNight) Color(0xFF162D21) else if (isSunset) Color(0xFF3E2B35) else Color(0xFF2E533E)
    val groundColor = if (isNight) Color(0xFF1D3525) else if (isSunset) Color(0xFF382C22) else Color(0xFF3D6B42)
    val pathDirt = if (isNight) Color(0xFF2E241E) else if (isSunset) Color(0xFF4A3528) else Color(0xFF5D4037)

    // Pine trees layer in background
    for (i in 0..12) {
        val tx = cw * (0.04f + i * 0.082f)
        val th = (24f + ((i * 13) % 7) * 3f) * p
        val treeBaseY = horizonY + 8f * p
        scope.drawRect(Color(0xFF1B130E), Offset(tx - p, treeBaseY - th * 0.25f), Size(2f * p, th * 0.25f))
        for (tier in 0..2) {
            val tierW = (14f - tier * 3.5f) * p
            val tierH = th * 0.35f
            val tierY = treeBaseY - th * (0.25f + tier * 0.25f)
            scope.drawRect(if (tier == 2) pineMid else pineDark, Offset(tx - tierW / 2f, tierY), Size(tierW, tierH))
        }
    }

    // Ground grass & forest floor clearing
    scope.drawRect(groundColor, Offset(0f, horizonY + 6f * p), Size(cw, ch - (horizonY + 6f * p)))
    scope.drawOval(pathDirt, Offset(cw * 0.20f, ch * 0.65f), Size(cw * 0.60f, ch * 0.28f))

    // 2. Cozy Canvas Camp Tent on the Left
    val tentTopLeft = CampfireLayout.tentTopLeft(cw, ch)
    val tentSize = CampfireLayout.tentSize(p)
    val tentX = tentTopLeft.x
    val tentY = tentTopLeft.y
    val tentW = tentSize.x
    val tentH = tentSize.y
    val canvasShadow = Color(0xFFC7B69B)
    val tentPole = Color(0xFF4E3629)

    for (step in 0..20) {
        val fy = tentY + step * (tentH / 20f)
        val fw = (step / 20f) * tentW
        scope.drawRect(canvasShadow, Offset(tentX + (tentW - fw) / 2f, fy), Size(fw, tentH / 20f + 1f))
    }
    for (step in 0..14) {
        val fy = tentY + (step + 6) * (tentH / 20f)
        val fw = (step / 14f) * (tentW * 0.45f)
        scope.drawRect(Color(0xFF261D17), Offset(tentX + (tentW - fw) / 2f, fy), Size(fw, tentH / 20f + 1f))
    }
    scope.drawRect(tentPole, Offset(tentX + tentW / 2f - p, tentY - 2f * p), Size(2f * p, tentH + 4f * p))
    scope.drawLine(tentPole, Offset(tentX, tentY + tentH), Offset(tentX + tentW / 2f, tentY), strokeWidth = 2f * p)
    scope.drawLine(tentPole, Offset(tentX + tentW, tentY + tentH), Offset(tentX + tentW / 2f, tentY), strokeWidth = 2f * p)

    // Camp Lantern hanging from tent entrance pole
    val lantern = CampfireLayout.lantern(cw, ch, p)
    val lantX = lantern.x
    val lantY = lantern.y
    scope.drawLine(Color(0xFF3E2723), Offset(tentX + tentW / 2f, tentY + 6f * p), Offset(lantX, lantY), strokeWidth = 1.5f * p)
    scope.drawRect(Color(0xFF3E2723), Offset(lantX - 4f * p, lantY), Size(8f * p, 10f * p))
    if (engine.campLanternLit) {
        val flicker = sin(time * 6f) * 1.5f * p
        scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.35f), 18f * p + flicker, Offset(lantX, lantY + 5f * p))
        scope.drawRect(Color(0xFFFFF3B0), Offset(lantX - 2.5f * p, lantY + 2f * p), Size(5f * p, 6f * p))
        scope.drawRect(Color.White, Offset(lantX - p, lantY + 3.5f * p), Size(2f * p, 3f * p))
    } else {
        scope.drawRect(Color(0xFF757575), Offset(lantX - 2.5f * p, lantY + 2f * p), Size(5f * p, 6f * p))
    }

    // 3. Rustic Wooden Log Bench in Center
    val logX = CampfireLayout.logX(cw)
    val logY = CampfireLayout.logY(ch)
    val logW = CampfireLayout.logW(cw)
    val logH = 10f * p
    scope.drawRoundRect(Color(0xFF4A3428), Offset(logX, logY), Size(logW, logH), CornerRadius(4f * p, 4f * p))
    scope.drawRoundRect(Color(0xFF704D3B), Offset(logX + 2f * p, logY + p), Size(logW - 4f * p, logH * 0.45f), CornerRadius(3f * p, 3f * p))
    scope.drawOval(Color(0xFF8D624A), Offset(logX - 3f * p, logY), Size(6f * p, logH))
    scope.drawOval(Color(0xFF53392B), Offset(logX - 1.5f * p, logY + 2f * p), Size(3f * p, logH - 4f * p))
    scope.drawOval(Color(0xFF8D624A), Offset(logX + logW - 3f * p, logY), Size(6f * p, logH))

    // 4. Acoustic Guitar resting beside the log
    val guitar = CampfireLayout.guitar(cw, ch, p)
    val gtrX = guitar.x
    val gtrY = guitar.y
    scope.drawLine(Color(0xFF5D4037), Offset(gtrX + 4f * p, gtrY - 14f * p), Offset(gtrX, gtrY + 12f * p), strokeWidth = 2.5f * p)
    scope.drawRect(Color(0xFF3E2723), Offset(gtrX + 3f * p, gtrY - 18f * p), Size(4f * p, 5f * p))
    scope.drawOval(Color(0xFFBA6E32), Offset(gtrX - 5f * p, gtrY + 2f * p), Size(11f * p, 13f * p))
    scope.drawOval(Color(0xFF8B4716), Offset(gtrX - 4f * p, gtrY + 3f * p), Size(9f * p, 11f * p))
    scope.drawCircle(Color(0xFF2E190D), 2.2f * p, Offset(gtrX + 0.5f * p, gtrY + 8f * p))
    if (engine.campGuitarStrumTimer > 0f) {
        val wave = sin(time * 8f) * 3f * p
        scope.drawCircle(Color(0xFFFFD166).copy(alpha = 0.5f), 12f * p, Offset(gtrX, gtrY + 6f * p))
        scope.drawCircle(Color(0xFFFF7597), 2f * p, Offset(gtrX + 6f * p + wave, gtrY - 6f * p))
        scope.drawLine(Color(0xFFFF7597), Offset(gtrX + 8f * p + wave, gtrY - 6f * p), Offset(gtrX + 8f * p + wave, gtrY - 14f * p), strokeWidth = 1.2f * p)
    }

    // 5. Red Plaid Fleece Camp Blanket on Right
    val blkX = cw * CampfireLayout.BLANKET_X
    val blkY = ch * CampfireLayout.BLANKET_Y
    val blkW = 38f * p
    val blkH = 18f * p
    scope.drawRoundRect(Color(0xFFB71C1C), Offset(blkX, blkY), Size(blkW, blkH), CornerRadius(4f * p, 4f * p))
    for (i in 0..3) {
        val sx = blkX + 4f * p + i * 9f * p
        scope.drawRect(Color(0xFF1B2A32).copy(alpha = 0.55f), Offset(sx, blkY), Size(2.5f * p, blkH))
    }
    for (j in 0..1) {
        val sy = blkY + 4f * p + j * 7f * p
        scope.drawRect(Color(0xFF1B2A32).copy(alpha = 0.55f), Offset(blkX, sy), Size(blkW, 2.5f * p))
    }

    // 6. Stone Fire Pit in Front of Bench
    val fireX = cw * CampfireLayout.PIT_X
    val fireY = ch * CampfireLayout.PIT_Y
    val fireRadius = 14f * p
    val emberPulse = sin(time * 5f) * 3f * p
    val glowAlpha = (0.28f + 0.12f * sin(time * 7f)).coerceIn(0.15f, 0.45f)
    scope.drawCircle(Color(0xFFFF9F1C).copy(alpha = glowAlpha), 36f * p + emberPulse, Offset(fireX, fireY))
    scope.drawCircle(Color(0xFFFFD166).copy(alpha = glowAlpha * 0.8f), 22f * p + emberPulse * 0.7f, Offset(fireX, fireY))

    for (stone in 0..7) {
        val ang = (stone * 45f) * (Math.PI.toFloat() / 180f)
        val sx = fireX + cos(ang) * (fireRadius + 2f * p)
        val sy = fireY + sin(ang) * (fireRadius * 0.75f)
        val stColor = if (stone % 2 == 0) Color(0xFF6B6B6B) else Color(0xFF4F4F4F)
        scope.drawOval(stColor, Offset(sx - 3.5f * p, sy - 2.5f * p), Size(7f * p, 5f * p))
    }
    scope.drawLine(Color(0xFF281812), Offset(fireX - 8f * p, fireY + 2f * p), Offset(fireX + 8f * p, fireY - 2f * p), strokeWidth = 3f * p)
    scope.drawLine(Color(0xFF281812), Offset(fireX - 7f * p, fireY - 2f * p), Offset(fireX + 7f * p, fireY + 2f * p), strokeWidth = 3f * p)
    scope.drawCircle(Color(0xFFBF211E), 4f * p, Offset(fireX, fireY))

    // Animated Pixel Flames
    val flameFlickerA = sin(time * 12f) * 2f * p
    val flameFlickerB = sin(time * 16f + 1.5f) * 2.5f * p
    scope.drawOval(Color(0xFFE85D04), Offset(fireX - 7f * p, fireY - 10f * p + flameFlickerA), Size(14f * p, 12f * p))
    scope.drawOval(Color(0xFFF48C06), Offset(fireX - 5f * p, fireY - 13f * p + flameFlickerB), Size(10f * p, 11f * p))
    scope.drawOval(Color(0xFFFFBA08), Offset(fireX - 3f * p, fireY - 15f * p + flameFlickerA * 0.8f), Size(6f * p, 9f * p))
    scope.drawCircle(Color(0xFFFFF3B0), 2f * p, Offset(fireX, fireY - 14f * p + flameFlickerB * 0.5f))
    // A tapped fire flares up for a moment.
    val flare = (engine.campfireEmbersTimer / 2.8f).coerceIn(0f, 1f)
    if (flare > 0f) {
        val flareH = 18f * p * flare
        scope.drawOval(Color(0xFFF48C06).copy(alpha = 0.85f), Offset(fireX - 5f * p, fireY - 13f * p - flareH + flameFlickerB), Size(10f * p, 11f * p + flareH))
        scope.drawOval(Color(0xFFFFBA08).copy(alpha = 0.9f), Offset(fireX - 3f * p, fireY - 15f * p - flareH * 0.8f + flameFlickerA), Size(6f * p, 9f * p + flareH * 0.8f))
    }

    // Rising Campfire Embers
    for (e in 0..4) {
        val ex = fireX + sin(time * 3f + e * 1.8f) * (8f + e * 2f) * p
        val ey = (fireY - 16f * p) - ((time * (18f + e * 4f) + e * 15f) % (42f * p))
        val eAlpha = (1f - (fireY - ey) / (58f * p)).coerceIn(0f, 1f)
        scope.drawRect(Color(0xFFFFD166).copy(alpha = eAlpha), Offset(ex, ey), Size(1.5f * p, 1.5f * p))
    }

    // 7. Marshmallow Roasting Sticks
    if (engine.marshmallowRoastingTimer > 0f) {
        val bStickX1 = cw * engine.boy.worldX + 6f * p
        val bStickY1 = ch * engine.boy.worldY - 10f * p
        val bMallowX = fireX - 4f * p
        val bMallowY = fireY - 10f * p
        scope.drawLine(Color(0xFF5D4037), Offset(bStickX1, bStickY1), Offset(bMallowX, bMallowY), strokeWidth = 1.6f * p)
        scope.drawRoundRect(Color(0xFFFFFEE0), Offset(bMallowX - 2.5f * p, bMallowY - 2.5f * p), Size(5f * p, 4f * p), CornerRadius(1.5f * p, 1.5f * p))
        scope.drawRect(Color(0xFFD4A373), Offset(bMallowX - p, bMallowY - 2.5f * p), Size(2f * p, 4f * p))

        val gStickX1 = cw * engine.girl.worldX - 6f * p
        val gStickY1 = ch * engine.girl.worldY - 10f * p
        val gMallowX = fireX + 4f * p
        val gMallowY = fireY - 9f * p
        scope.drawLine(Color(0xFF5D4037), Offset(gStickX1, gStickY1), Offset(gMallowX, gMallowY), strokeWidth = 1.6f * p)
        scope.drawRoundRect(Color(0xFFFFFEE0), Offset(gMallowX - 2.5f * p, gMallowY - 2.5f * p), Size(5f * p, 4f * p), CornerRadius(1.5f * p, 1.5f * p))
        scope.drawRect(Color(0xFFD4A373), Offset(gMallowX - p, gMallowY - 2.5f * p), Size(2f * p, 4f * p))

        val puffY = bMallowY - 6f * p - (time * 10f % (14f * p))
        scope.drawCircle(Color.White.copy(alpha = 0.5f), 2f * p, Offset(bMallowX, puffY))
    }
}
