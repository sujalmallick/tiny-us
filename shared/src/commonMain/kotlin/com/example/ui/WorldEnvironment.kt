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

fun drawEnvironment(
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
            drawDistantHills(scope, cw, ch * 0.66f, p, isNight, isSunset, isMorning, engine.weather)
            drawMeadowGround(scope, cw, ch, isNight, isSunset, timeSeconds, p, engine.weather)
            drawCloudShadows(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
            // Cottage house in background
            val cottageP = p * com.example.scene.MeadowLayout.COTTAGE_SCALE
            WorldSprites.drawCottage(scope, com.example.scene.MeadowLayout.cottageX(p), com.example.scene.MeadowLayout.groundY(ch), cottageP, timeSeconds, isNight, engine.weather)
            // Porch wind chimes hanging from cottage eaves
            val chimes = com.example.scene.MeadowLayout.windChimes(cw, ch, p)
            WorldSprites.drawPorchWindChimes(scope, chimes.x, chimes.y, p, engine.windChimeSwayTimer / 2.5f, timeSeconds)
            // Animated chimney smoke
            if (sin(timeSeconds * 3f) > 0.7f) {
                val chimney = com.example.scene.MeadowLayout.chimneyTop(cw, ch, p)
                engine.particles.spawnChimneySmoke(chimney.x, chimney.y)
            }
            // Flowers with dynamic garden growth
            drawWildFlowers(scope, cw, ch * 0.70f, p, timeSeconds, engine.gardenStage, engine.flowerWiggleTimer, engine.weather, engine.gardenBlooms)
            // Curated picnic basket
            WorldSprites.drawPicnicBasket(scope, cw * 0.84f, ch * 0.70f, p)
            // Ambient birds
            engine.birdSystem.drawBirds(scope, p)
        }
        EnvironmentType.TWILIGHT -> {
            drawSkyAndClouds(scope, cw, ch, isNight = isNight, isSunset = isSunset, isMorning = isMorning, time = timeSeconds, p = p, weather = engine.weather, isPinkSunset = true)
            drawDistantHills(scope, cw, ch * 0.66f, p, isNight, isSunset, isMorning, engine.weather)
            drawMeadowGround(scope, cw, ch, isNight = isNight, isSunset = isSunset, timeSeconds = timeSeconds, p = p, weather = engine.weather)
            drawCloudShadows(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
            drawWildFlowers(scope, cw, ch * 0.70f, p, timeSeconds, engine.gardenStage, engine.flowerWiggleTimer, engine.weather, engine.gardenBlooms)
            // Twinkling fairy light jar on the grass
            WorldSprites.drawFairyJar(scope, cw * 0.76f, ch * 0.70f, p, timeSeconds)
            // Stargazing Vintage Telescope on the knoll
            WorldSprites.drawVintageTelescope(scope, cw * 0.25f, ch * 0.70f, p, timeSeconds)
            // Ambient birds
            engine.birdSystem.drawBirds(scope, p)
        }
        EnvironmentType.TREE_HILL -> {
            drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, timeSeconds, p, weather = engine.weather)
            drawDistantHills(scope, cw, ch * 0.66f, p, isNight, isSunset, isMorning, engine.weather)
            drawMeadowGround(scope, cw, ch, isNight, isSunset, timeSeconds, p, engine.weather)
            drawCloudShadows(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
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
            drawWildFlowers(scope, cw, ch * 0.70f, p, timeSeconds, engine.gardenStage, engine.flowerWiggleTimer, engine.weather, engine.gardenBlooms)
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

            // Treat jar and the dropped biscuit are part of the kitchen scene, not a separate interaction layer.
            drawKitchenTreatJar(scope, cw, ch, p, timeSeconds, engine)

            // --- Home Evolution progressive artifacts ---
            val floorY = ch * 0.65f
            val floorH = ch - floorY
            if (engine.homeEvolutionState.hasCopperTeakettle || engine.teakettleWhistleTimer > 0f) {
                WorldSprites.drawCopperTeakettle(scope, cw * 0.60f, ch * 0.67f - 18 * p, p, timeSeconds)
            }
            if (engine.homeEvolutionState.hasFridgePolaroid || engine.homeEvolutionState.hasFridgeLoveNote) {
                val fridgeX = cw * 0.88f
                val fridgeW = 20 * p
                val fridgeTop = floorY - 64 * p
                WorldSprites.drawFridgeDecorations(
                    scope = scope,
                    fridgeLeft = fridgeX - fridgeW / 2f,
                    fridgeTop = fridgeTop,
                    p = p,
                    hasPolaroid = engine.homeEvolutionState.hasFridgePolaroid,
                    hasLoveNote = engine.homeEvolutionState.hasFridgeLoveNote
                )
            }
            val juteY = floorY + floorH * 0.44f
            val tblY = juteY + 7f * p
            WorldSprites.drawSeasonalTableArtifact(scope, cw * 0.50f, tblY, p, engine.homeEvolutionState.seasonalArtifact, timeSeconds)
            if (engine.homeEvolutionState.hasWindowsillPlant || engine.plantWaterTimer > 0f) {
                WorldSprites.drawWindowsillPlant(scope, cw * 0.28f + 9 * p, com.example.scene.KitchenLayout.windowSill(ch, p), p)
            }
            if (engine.homeEvolutionState.hasOrigamiHeart) {
                WorldSprites.drawOrigamiHeart(scope, cw * 0.28f - 7 * p, com.example.scene.KitchenLayout.windowSill(ch, p), p)
            }
            if (engine.homeEvolutionState.hasAdventurePicnicBasket) {
                WorldSprites.drawAdventurePicnicBasket(scope, cw * 0.76f, floorY, p)
            }

            // --- Kitchen dynamic overlays (drawn on top of static sprites) ---

            // 1. Wall Clock hands: spin fast then settle to real device time
            if (engine.clockSpinTimer > 0f) {
                val spinDur = 2.4f
                val elapsed = spinDur - engine.clockSpinTimer
                val spinFrac = elapsed / spinDur
                // Spin phase: 0..0.55 = fast spinning, 0.55..1.0 = settle to real time
                val clockCenter = com.example.scene.KitchenLayout.clockCenter(cw, ch, p)
                val clockCx = clockCenter.x
                val clockCy = clockCenter.y
                val clockR = 12f * p
                val clockNow = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                val realHourAngle = ((clockNow.hour % 12 + clockNow.minute / 60f) / 12f) * (2f * kotlin.math.PI.toFloat())
                val realMinAngle  = (clockNow.minute / 60f) * (2f * kotlin.math.PI.toFloat())
                val spinRev = if (spinFrac < 0.55f) {
                    val t = spinFrac / 0.55f
                    (1f - t * t) * 8f * (2f * kotlin.math.PI.toFloat())  // 8 full spins decelerating
                } else {
                    0f
                }
                val hourAngle  = realHourAngle  + spinRev - kotlin.math.PI.toFloat() / 2f
                val minuteAngle = realMinAngle  + spinRev * 1.5f - kotlin.math.PI.toFloat() / 2f
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
            drawLivingRoom(scope, cw, ch, isNight = livingRoomIsNight, p = p, lampLit = isLampOn, couchPhase = couchPhase, timeSeconds = timeSeconds, candleLit = engine.hearthCandleLit, roomTheme = engine.roomTheme)
            if (engine.homeEvolutionState.hasFairyStringLights) {
                WorldSprites.drawFairyStringLights(
                    scope, cw * 0.08f, cw * 0.92f, ch * 0.16f, p, timeSeconds,
                    bulbColor = engine.roomTheme.light,
                    bulbStyle = engine.roomTheme.ordinal
                )
            }
            if (engine.homeEvolutionState.hasCornerMonstera || engine.plantWaterTimer > 0f) {
                WorldSprites.drawCornerMonstera(scope, cw * 0.10f, ch * 0.65f, p)
            }
            WorldSprites.drawPhotoFrame(scope, cw * 0.28f, ch * 0.38f, p)
            WorldSprites.drawWindow(scope, cw * 0.70f, ch * 0.28f, isWindowNight, p, engine.weather, isSunset = isWindowSunset)
            WorldSprites.drawWallCalendar(scope, cw * 0.49f, ch * 0.30f, p, timeSeconds)
            WorldSprites.drawCouch(scope, cw * 0.48f, ch * 0.68f, p, engine.roomTheme)
            if (engine.homeEvolutionState.hasCozyKnitThrow || engine.cuddleBlanketTimer > 0f) {
                WorldSprites.drawCouchKnitThrow(scope, cw * 0.48f - 29 * p, ch * 0.68f - 24 * p, p)
            }
            if (engine.cuddleBlanketTimer > 0f) {
                val blkX = cw * 0.48f - 18 * p
                val blkY = ch * 0.68f - 12 * p
                val blkW = 36 * p
                val blkH = 14 * p
                scope.drawRect(Color(0xFFD4A373), Offset(blkX, blkY), Size(blkW, blkH))
                scope.drawRect(Color(0xFFFAEDCD), Offset(blkX + p, blkY + p), Size(blkW - 2 * p, blkH - 2 * p))
                for (kx in 0 until 6) {
                    scope.drawRect(Color(0xFFE9D8A6), Offset(blkX + 3 * p + kx * 5 * p, blkY + p), Size(1.5f * p, blkH - 2 * p))
                }
            }
            WorldSprites.drawFloorLamp(scope, cw * 0.20f, ch * 0.68f, p, engine.livingRoomLampLit)
            WorldSprites.drawWardrobe(scope, cw * 0.85f, ch * 0.65f, p, timeSeconds)

            // --- Living Room dynamic overlays (drawn on top of static sprites) ---
            val lrFloorY = ch * 0.65f
            val lrFloorH = ch - lrFloorY
            WorldSprites.drawSeasonalTableArtifact(scope, cw * 0.50f + 10 * p, lrFloorY + 23 * p, p, engine.homeEvolutionState.seasonalArtifact, timeSeconds)
            if (engine.homeEvolutionState.hasBedsideNotepad) {
                WorldSprites.drawBedsideNotepad(scope, cw * 0.50f - 14 * p, lrFloorY + 23 * p, p)
            }
            if (engine.homeEvolutionState.hasMiniGameBoard) {
                WorldSprites.drawMiniGameBoard(scope, cw * 0.50f - 1 * p, lrFloorY + 23 * p, p)
            }
            if (engine.homeEvolutionState.hasAdventurePicnicBasket) {
                WorldSprites.drawAdventurePicnicBasket(scope, cw * 0.77f, lrFloorY, p)
            }
            if (engine.homeEvolutionState.hasOrigamiHeart) {
                WorldSprites.drawOrigamiHeart(scope, cw * 0.70f + 14 * p, ch * 0.28f + 18 * p, p)
            }
            // Feather wand beside Mochi's cardboard box
            WorldSprites.drawFeatherWand(
                scope = scope,
                baseX = cw * 0.23f,
                baseY = lrFloorY + lrFloorH * 0.75f + 11f * p,
                p = p,
                wiggleProgress = engine.featherWandWiggleTimer / 2.4f,
                timeSeconds = timeSeconds
            )

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
                drawPixelGlow(scope, Color(0xFFFFB703).copy(alpha = glowAlpha), glowRadius, androidx.compose.ui.geometry.Offset(cndX + 2.2f * p, cndY + 2f * p), p)
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
                val squish = sin(t * kotlin.math.PI.toFloat() * 3f) * (1f - t) * 3.8f * p
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

            // 3. Mochi's Cozy Cardboard Box: Mochi herself, in it, peeking out now and then (plan 10, C)
            if (engine.mochiInBox) {
                val dur = 1.3f
                // A peek every couple of seconds while she's in there, ducking down between
                val t = ((engine.sceneTime % 2.2f) / dur).coerceIn(0f, 1f)
                val cBoxX = cw * 0.17f
                val cBoxY = lrFloorY + lrFloorH * 0.75f
                val cBoxW = 20 * p
                val cBoxH = 13 * p
                val cbLeft = cBoxX - cBoxW / 2f
                val peek = sin(t * kotlin.math.PI.toFloat()).coerceIn(0f, 1f)
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
                val rollDist = 22f * p * sin(t * kotlin.math.PI.toFloat() * 0.90f).coerceAtLeast(0f)
                val bounceY = abs(sin(t * kotlin.math.PI.toFloat() * 2f)) * 4f * p * (1f - t)
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
                val lift = sin(t * kotlin.math.PI.toFloat()) * 13f * p
                val albumX = rLeft + 9 * p
                val albumY = rackY - 6 * p - lift

                // Animated album jacket lifting
                scope.drawRect(Color(0xFFE76F51), androidx.compose.ui.geometry.Offset(albumX, albumY), Size(8 * p, 13 * p))
                scope.drawRect(Color(0xFFFFD166), androidx.compose.ui.geometry.Offset(albumX + 1.8f * p, albumY + 2.5f * p), Size(4.4f * p, 4.4f * p))

                // Vinyl disc peeking/spinning out to the right
                val discSlide = sin(t * kotlin.math.PI.toFloat()) * 7f * p
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
                isSunset = isSunset,
                roomTheme = engine.roomTheme,
                boyLook = engine.boy.look,
                girlLook = engine.girl.look
            )

            // Loft panoramic window breeze shimmer & leaves
            if (engine.loftWindowTimer > 0f) {
                val dur = 2.0f
                val t = ((dur - engine.loftWindowTimer) / dur).coerceIn(0f, 1f)
                val windowStartX = cw * 0.32f
                val floorY = ch * 0.55f
                val breezePulse = sin(t * kotlin.math.PI.toFloat())
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
        EnvironmentType.RAINY_CAFE -> drawRainyCafeScene(scope, cw, ch, p, timeSeconds, engine)
        EnvironmentType.SUNROOM -> drawCottageSunroom(scope, cw, ch, p, timeSeconds, engine)
        EnvironmentType.CAMPFIRE -> drawCampfireScene(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
        EnvironmentType.SEASIDE_PIER -> drawSeasidePierScene(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
        EnvironmentType.PATH_NIGHT -> {
            drawSkyAndClouds(scope, cw, ch, isNight, isSunset, isMorning, timeSeconds, p, weather = engine.weather)
            drawPathGround(scope, cw, ch, p, engine.weather, isWalk = true, timeSeconds = timeSeconds, isNight = isNight, isSunset = isSunset)
            drawCloudShadows(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
            WorldSprites.drawStreetlamp(scope, cw * 0.65f, ch * 0.68f, engine.lampLit && (isNight || isSunset), p)
            WorldSprites.drawMailbox(scope, cw * 0.82f, ch * 0.68f, hasLetter = true, p)
            // Stargazing Vintage Telescope on the overlook
            WorldSprites.drawVintageTelescope(scope, cw * 0.38f, ch * 0.68f, p, timeSeconds)

            val curbY = ch * 0.66f + 30f * p
            val curbH = ch - curbY

            // 1. Pagoda Lantern Glow: expanding warm golden radial glow
            if (engine.pagodaGlowTimer > 0f) {
                val dur = 1.8f
                val t = ((dur - engine.pagodaGlowTimer) / dur).coerceIn(0f, 1f)
                val pulse = sin(t * kotlin.math.PI.toFloat())
                val pagX = cw * 0.20f
                val pagY = curbY + curbH * 0.38f + 5 * p

                drawPixelGlow(scope, Color(0xFFFFAA00).copy(alpha = (pulse * 0.38f).coerceIn(0f, 1f)), (18f + pulse * 14f) * p, androidx.compose.ui.geometry.Offset(pagX, pagY), p)
                drawPixelGlow(scope, Color(0xFFFFD166).copy(alpha = (pulse * 0.55f).coerceIn(0f, 1f)), (10f + pulse * 8f) * p, androidx.compose.ui.geometry.Offset(pagX, pagY), p)
                drawPixelGlow(scope, Color.White.copy(alpha = (pulse * 0.85f).coerceIn(0f, 1f)), (3.5f + pulse * 2f) * p, androidx.compose.ui.geometry.Offset(pagX, pagY - 1f * p), p)
            }

            // 2. Lavender Patch Sway & Scent Waft
            if (engine.lavenderSwayTimer > 0f) {
                val dur = 1.5f
                val t = ((dur - engine.lavenderSwayTimer) / dur).coerceIn(0f, 1f)
                val lavX = cw * 0.80f
                val lavY = curbY + curbH * 0.36f
                val sway = sin(t * kotlin.math.PI.toFloat() * 4f) * (1f - t) * 3.5f * p

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
                    val bounce = sin(capT * kotlin.math.PI.toFloat()) * 4.5f * p
                    val glowPulse = sin(t * kotlin.math.PI.toFloat())

                    drawPixelGlow(scope, Color(0xFF48CAE4).copy(alpha = (glowPulse * 0.40f).coerceIn(0f, 1f)), (8f + bounce * 0.8f) * p, androidx.compose.ui.geometry.Offset(mx + 2 * p, my - bounce), p)

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
            drawCloudShadows(scope, cw, ch, p, timeSeconds, engine, isNight, isSunset, isMorning)
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
                val pulse = sin(t * kotlin.math.PI.toFloat())
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
                val lidLift = sin(t * kotlin.math.PI.toFloat()) * 13 * p

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
                val steamFade = sin(t * kotlin.math.PI.toFloat())
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
                val wobble = sin(t * kotlin.math.PI.toFloat() * 10f) * (1f - t) * 3f * p

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
                val bWobble = sin(t * kotlin.math.PI.toFloat() * 6f) * (1f - t) * 1.5f * p
                val heartAlpha = sin(t * kotlin.math.PI.toFloat()).coerceIn(0f, 1f)

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
                val topSlide = sin(t * kotlin.math.PI.toFloat() * 4f) * (1f - t) * 3f * p

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
                val pulse = sin(t * kotlin.math.PI.toFloat())

                drawPixelGlow(scope, Color(0xFFFFAA00).copy(alpha = pulse * 0.35f), (10f + pulse * 10f) * p, androidx.compose.ui.geometry.Offset(lanX + 2.2f * p, lanY + 3.5f * p), p)
                drawPixelGlow(scope, Color(0xFFFFD166).copy(alpha = pulse * 0.55f), (6f + pulse * 5f) * p, androidx.compose.ui.geometry.Offset(lanX + 2.2f * p, lanY + 3.5f * p), p)

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
                val pulse = sin(t * kotlin.math.PI.toFloat())
                val templeBaseY = ch * 0.70f
                val farParallaxSpeed = 22f * p
                val templeLoopW = cw + 360f * p
                fun posMod(v: Float, m: Float): Float = ((v % m) + m) % m
                val t2X = posMod(cw + 280f * p - timeSeconds * farParallaxSpeed, templeLoopW) - 180f * p
                val spireY = templeBaseY - 135 * p

                if (t2X in -50f * p..(cw + 50f * p)) {
                    drawPixelGlow(scope, Color(0xFFFFD166).copy(alpha = pulse * 0.40f), (24f + pulse * 18f) * p, androidx.compose.ui.geometry.Offset(t2X, spireY), p)
                    drawPixelGlow(scope, Color(0xFFFFF3B0).copy(alpha = pulse * 0.65f), (12f + pulse * 8f) * p, androidx.compose.ui.geometry.Offset(t2X, spireY), p)
                    drawPixelGlow(scope, Color.White.copy(alpha = pulse * 0.85f), 4 * p, androidx.compose.ui.geometry.Offset(t2X, spireY), p)
                    for (ray in 0..3) {
                        val rAngle = (ray * 45f) * (kotlin.math.PI.toFloat() / 180f)
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

    // Stargazing (plan 07, C2)
    if (isNight) drawStarPuzzle(scope, engine.starPuzzle, cw, ch, p, engine.sceneTime, skyDrift(engine.sceneTime))

    if (isNight && engine.constellationConnectTimer > 0f) {
        drawConstellationOverlay(scope, cw, ch, p, engine.constellationConnectTimer, engine.activeConstellationIndex, engine.sceneTime)
    }
}
