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
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelIcons

@Composable
internal fun BoxScope.DreamOverlay(
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
                shape = PixelCornerShape(16.dp),
                ambientColor = tintColor.copy(alpha = 0.5f),
                spotColor = tintColor.copy(alpha = 0.6f)
            ),
        shape = PixelCornerShape(16.dp),
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
                            imageVector = PixelIcons.AutoAwesome,
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
                    shape = PixelCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f + 0.08f * breathe),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f + 0.30f * breathe)),
                    modifier = Modifier.clip(PixelCornerShape(20.dp))
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

internal fun drawDreamStardust(
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

internal fun drawDreamArt(
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

internal fun drawJapanDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawNorwayDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawOceanDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawFlyingDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawStarsDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawForestDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawHomeDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawRainDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawCityDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawSweetDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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

internal fun drawFallbackDream(scope: DrawScope, cw: Float, ch: Float, t: Float, p: Float, alpha: Float) {
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
