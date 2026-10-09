package com.example.ui

import com.example.engine.drawPixelGlow
import com.example.engine.CastLight
import com.example.engine.SceneLight
import com.example.engine.GameText
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import com.example.engine.SpriteClock
import com.example.engine.WorldCamera
import com.example.engine.WorldViewport
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.scene.autonomy.SpotAction
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
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
import com.example.scene.CafeLayout
import com.example.scene.CafeProp
import com.example.scene.CampfireLayout
import com.example.scene.CampfireProp
import com.example.scene.PierLayout
import com.example.scene.PierProp
import com.example.scene.PierTapState
import com.example.scene.WeatherLayout
import com.example.scene.CatState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import com.example.resources.*

enum class TapTargetKind {
    BOY,
    GIRL,
    BOTH_CHARACTERS,
    RIDE_VEHICLE
}

@Composable
fun PixelWorldView(
    engine: SceneEngine,
    atmosphereMode: String,
    modifier: Modifier = Modifier,
    onOpenDateAdventures: (() -> Unit)? = null,
    onOpenDailyMoment: (() -> Unit)? = null,
    onOpenMiniGames: (() -> Unit)? = null,
    onOpenLongDistance: (() -> Unit)? = null,
    /** The Thank-You Jar on the kitchen wall was tapped (plan 09, C). */
    onOpenThankYouJar: (() -> Unit)? = null
) {
    var frameNanos by remember { mutableLongStateOf(0L) }
    var viewportWidth by remember { mutableFloatStateOf(1080f) }
    var viewportHeight by remember { mutableFloatStateOf(2400f) }
    // Where the world sits on the screen for the current scene; taps, the engine and overlays work
    // in its world units.
    // The buttons along the top (below the status bar) and the heart button and message box along
    // the bottom (above the navigation bar).
    val density = LocalDensity.current
    val topReserve = WindowInsets.statusBars.getTop(density) + with(density) { 58.dp.toPx() }
    val bottomReserve = WindowInsets.navigationBars.getBottom(density) + with(density) { 70.dp.toPx() }
    val reserves by rememberUpdatedState(topReserve to bottomReserve)
    val cameraCache = remember { CameraCache() }
    fun cameraNow(): WorldCamera =
        cameraCache.get(viewportWidth, viewportHeight, engine.currentScene, reserves.first, reserves.second)
    val haptic = LocalHapticFeedback.current
    val lowResBuffer = remember { LowResWorldBuffer() }
    val coroutineScope = rememberCoroutineScope()

    // High refresh rate adaptive frame ticker loop
    LaunchedEffect(Unit) {
        if (frameTickerPaused) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val delta = (nanos - lastNanos) / 1_000_000_000f
                    val dt = delta.coerceIn(0.005f, 0.05f)
                    val camera = cameraNow()
                    engine.update(dt, camera.worldW, camera.worldH)
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
        reallocateNightStars(kotlin.time.Clock.System.now().toEpochMilliseconds())
    }

    // Determine current lighting & atmosphere from engine's unified time phase
    val timePhase = engine.timeOfDayPhase
    val isNight = timePhase.isNight
    val isSunset = timePhase.isSunset
    val isMorning = timePhase.isMorning

    // The world is drawn, not laid out, so screen readers get a description of it (plan 06, I4).
    val worldDescription = org.jetbrains.compose.resources.stringResource(
        Res.string.ui_world_description, engine.boy.name, engine.girl.name, engine.currentScene.title
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = worldDescription }
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
                    onLongPress = { screenTap ->
                        val camera = cameraNow()
                        val tapOffset = camera.toWorld(screenTap)
                        pendingTapJob?.cancel()
                        pendingTapJob = null
                        pendingAction = null
                        tapCount = 0
                        lastTargetKind = null

                        val w = camera.worldW
                        val h = camera.worldH
                        val pixelScale = WorldViewport.pixelScale(w)
                        val charPixelScale = WorldViewport.characterPixelScale(w, engine.usesLowResRenderer)

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
                    onTap = { screenTap ->
                        val camera = cameraNow()
                        val tapOffset = camera.toWorld(screenTap)
                        // The user is playing: the couple's own routine steps aside for a moment.
                        engine.notifyUserInteraction()
                        // The birthday surprise (plan 09, A) takes taps while it runs.
                        if (engine.onBirthdayTap(camera.worldW, camera.worldH)) return@detectTapGestures
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

                        val w = camera.worldW
                        val h = camera.worldH
                        val pixelScale = WorldViewport.pixelScale(w)
                        val charPixelScale = WorldViewport.characterPixelScale(w, engine.usesLowResRenderer)
                        val ny = tapOffset.y / h
                        // Plan 12, C: tapping something one of them can use sends the nearer one over to
                        // use it (the thing's own response comes when they get there); when neither is
                        // free, it responds straight away as before. The kitchen's stove, oven, sink and
                        // table, the flowers and the watering stay as they were: they set the two of them
                        // up themselves.
                        fun goUse(action: SpotAction, act: () -> Unit) {
                            if (!engine.sendToUse(action, w, h, act)) act()
                        }

                        // The garden plots in the meadow come first: they sit low, where the
                        // couple's generous touch boxes would otherwise reach (plan 07, C5).
                        if (engine.cozy.onGardenTap(tapOffset.x, tapOffset.y, w, h, pixelScale)) {
                            flushPendingTap()
                            return@detectTapGestures
                        }
                        // The Friday fox and the ball it left (plan 10, D)
                        if (engine.onFoxTap(tapOffset.x, tapOffset.y, w, h, pixelScale)) {
                            flushPendingTap()
                            return@detectTapGestures
                        }
                        // The painter, the old couple, their note (plan 11)
                        if (engine.onVisitorTap(tapOffset.x, tapOffset.y, w, h, pixelScale)) {
                            flushPendingTap()
                            return@detectTapGestures
                        }
                        // A pet they haven't met yet (plan 10, E)
                        if (engine.onPetVisitorTap(tapOffset.x, tapOffset.y, w, h, pixelScale)) {
                            flushPendingTap()
                            return@detectTapGestures
                        }

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
                            engine.onTouchRideFireflies(tapOffset.x, tapOffset.y, w, h)
                            return@detectTapGestures
                        }

                        // These small scene props share vertical space with the characters' generous
                        // touch boxes, so resolve them first to keep their interactions reachable.
                        when (engine.currentScene.environment) {
                            EnvironmentType.RAINY_CAFE -> {
                                val cafeProp = CafeLayout.hitTest(
                                    tapOffset, w, h, pixelScale, engine.sceneTime, engine.catWorldX, engine.catWorldY
                                )
                                when (cafeProp) {
                                    CafeProp.MOCHI -> engine.onTouchCat(w, h)
                                    CafeProp.BARISTA -> engine.onTouchCafeBarista(w, h)
                                    CafeProp.MENU -> engine.onTouchCafeMenu()
                                    CafeProp.PUP -> goUse(SpotAction.PET_PUP) { engine.onTouchCafePup(w, h) }
                                    CafeProp.LATTE -> engine.onTouchCafeLatte(w, h)
                                    CafeProp.PASTRY -> engine.onTouchCafePastry(w, h)
                                    CafeProp.PASSERBY -> engine.onTouchCafePasserby(tapOffset.x, tapOffset.y)
                                    CafeProp.WINDOW -> goUse(SpotAction.FOG_WINDOW) { engine.onTouchCafeWindow(tapOffset.x, tapOffset.y, w, h) }
                                    null -> Unit
                                }
                                if (cafeProp != null) return@detectTapGestures
                            }
                            EnvironmentType.SUNROOM -> {
                                // The herb pots, the winter greenhouse (plan 09, E1).
                                if (engine.cozy.onPotTap(tapOffset.x, tapOffset.y, w, h, pixelScale)) return@detectTapGestures
                                if (tapOffset.y < h * 0.25f) {
                                    engine.onTouchSunroomSkylight(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                if (kotlin.math.hypot(tapOffset.x - w * 0.20f, tapOffset.y - h * 0.72f) < 22f * pixelScale) {
                                    goUse(SpotAction.WATER_PLANT) { engine.onTouchSunroomWateringCan(w, h) }
                                    return@detectTapGestures
                                }
                                // The potting bench and the plant shelf above it (clear of the couple standing in the middle)
                                if (tapOffset.x > w * 0.64f && tapOffset.y in (h * 0.40f)..(h * 0.665f)) {
                                    goUse(SpotAction.MIST_PLANTS) { engine.onTouchSunroomPlants(w, h) }
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.KITCHEN -> {
                                if (engine.coupleLifeStore != null && onOpenThankYouJar != null) {
                                    val thanks = com.example.scene.KitchenLayout.thankYouJar(w, h, pixelScale)
                                    if (abs(tapOffset.x - thanks.x) < 10f * pixelScale && tapOffset.y in (thanks.y - 16f * pixelScale)..(thanks.y + 4f * pixelScale)) {
                                        onOpenThankYouJar()
                                        return@detectTapGestures
                                    }
                                }
                                val jar = com.example.scene.KitchenLayout.treatJar(w, h, pixelScale)
                                if (kotlin.math.hypot(tapOffset.x - jar.x, tapOffset.y - (jar.y - 6f * pixelScale)) < 16f * pixelScale) {
                                    engine.onTouchKitchenTreatJar()
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.CAMPFIRE -> {
                                val campProp = CampfireLayout.hitTest(
                                    tapOffset, w, h, pixelScale, engine.catWorldX, engine.catWorldY
                                )
                                when (campProp) {
                                    CampfireProp.MOCHI -> engine.onTouchCampMochi(w, h)
                                    CampfireProp.FIRE -> goUse(SpotAction.WARM_HANDS) { engine.onTouchCampfire(w, h, tapOffset.x, tapOffset.y) }
                                    CampfireProp.GUITAR -> goUse(SpotAction.STRUM_GUITAR) { engine.onTouchCampGuitar(w, h) }
                                    CampfireProp.LANTERN -> goUse(SpotAction.TEND_LANTERN) { engine.onTouchCampLantern(w, h) }
                                    null -> Unit
                                }
                                if (campProp != null) return@detectTapGestures
                            }
                            EnvironmentType.SEASIDE_PIER -> {
                                val pierProp = PierLayout.hitTest(
                                    tapOffset, w, h, pixelScale, engine.sceneTime,
                                    engine.catWorldX, engine.catWorldY,
                                    PierTapState(
                                        gullX = engine.pierGullX,
                                        gullY = engine.pierGullY,
                                        gullVisible = engine.pierGullState.isVisible,
                                        bottleVisible = engine.pierBottleVisible,
                                        crabX = engine.pierCrabX,
                                        crabVisible = engine.isPierCrabVisible
                                    )
                                )
                                when (pierProp) {
                                    PierProp.MOCHI -> engine.onTouchCat(w, h)
                                    PierProp.PIP -> engine.onTouchPip(w, h)
                                    PierProp.BAO -> engine.onTouchGrandpaBao(w, h)
                                    PierProp.CART -> engine.onTouchPierIceCream(w, h)
                                    PierProp.BOTTLE -> engine.onTouchPierBottle(w, h)
                                    PierProp.LIGHTHOUSE -> engine.onTouchLighthouse(w, h)
                                    PierProp.TELESCOPE -> engine.onTouchPierTelescope(w, h)
                                    PierProp.BOAT -> engine.onTouchPierBoat(w, h, tapOffset.x, tapOffset.y)
                                    PierProp.CRAB -> engine.onTouchPierCrab(w, h)
                                    PierProp.BUCKET -> goUse(SpotAction.PEEK_BUCKET) { engine.onTouchPierBucket(w, h) }
                                    PierProp.LIGHTS -> engine.onTouchPierLights(w, h, tapOffset.x)
                                    PierProp.SEA -> engine.onTouchPierSea(tapOffset.x, tapOffset.y)
                                    null -> Unit
                                }
                                if (pierProp != null) return@detectTapGestures
                            }
                            else -> Unit
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
                                val door = com.example.scene.MeadowLayout.cottageDoor(w, h, pixelScale)
                                if (abs(tapOffset.x - door.x) < 26f * pixelScale && abs(tapOffset.y - door.y) < 26f * pixelScale) {
                                    engine.onTouchCottageDoor()
                                    return@detectTapGestures
                                }
                                // Porch Wind Chimes
                                val chimes = com.example.scene.MeadowLayout.windChimes(w, h, pixelScale)
                                if (abs(tapOffset.x - chimes.x) < 22f * pixelScale &&
                                    abs(tapOffset.y - (chimes.y + 6f * pixelScale)) < 24f * pixelScale) {
                                    goUse(SpotAction.LISTEN_CHIMES) { engine.onTouchWindChimes(tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                // Flowers
                                if (tapOffset.y > h * 0.65f) {
                                    if (tapOffset.y > h * 0.75f &&
                                        engine.onTouchWalkableGround(tapOffset.x, tapOffset.y, w, h)
                                    ) return@detectTapGestures
                                    engine.onTouchFlower(w, h)
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.TREE_HILL -> {
                                if (abs(tapOffset.x - w * 0.5f) < 45f * pixelScale && tapOffset.y < h * 0.62f) {
                                    goUse(SpotAction.LOOK_UP_TREE) { engine.onTouchTree(w, h) }
                                    return@detectTapGestures
                                }
                                if (tapOffset.y > h * 0.66f) {
                                    if (tapOffset.y > h * 0.75f &&
                                        engine.onTouchWalkableGround(tapOffset.x, tapOffset.y, w, h)
                                    ) return@detectTapGestures
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
                                // 2. Whistling Copper Teakettle & Stew Pot on Stovetop (left burner of counter)
                                if (abs(tapOffset.x - w * 0.60f) < 20f * pixelScale && abs(tapOffset.y - (h * 0.67f - 18f * pixelScale)) < 18f * pixelScale) {
                                    if (engine.homeEvolutionState.hasCopperTeakettle || engine.teakettleWhistleTimer > 0f) {
                                        engine.onTouchTeakettle(w, h, tapOffset.x, tapOffset.y)
                                    } else {
                                        engine.onTouchPot(w, h)
                                    }
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
                                val clock = com.example.scene.KitchenLayout.clockCenter(w, h, pixelScale)
                                if (abs(tapOffset.x - clock.x) < 16f * pixelScale && abs(tapOffset.y - clock.y) < 16f * pixelScale) {
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
                                // 13. Picnic Basket (Home Evolution artifact)
                                if (engine.homeEvolutionState.hasAdventurePicnicBasket &&
                                    abs(tapOffset.x - w * 0.76f) < 22f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f - 8f * pixelScale)) < 20f * pixelScale) {
                                    onOpenDateAdventures?.invoke()
                                    return@detectTapGestures
                                }
                                // 14. Origami Heart on Kitchen Windowsill
                                if (engine.homeEvolutionState.hasOrigamiHeart &&
                                    abs(tapOffset.x - (w * 0.28f - 7f * pixelScale)) < 18f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f - 52f * pixelScale)) < 18f * pixelScale) {
                                    onOpenLongDistance?.invoke()
                                    return@detectTapGestures
                                }
                                // 15. Windowsill Herb Planter Watering
                                if (abs(tapOffset.x - (w * 0.28f + 9f * pixelScale)) < 18f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f - 52f * pixelScale)) < 18f * pixelScale) {
                                    engine.onTouchPlantWatering(tapOffset.x, tapOffset.y)
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

                                // Corner Monstera plant watering
                                if ((engine.homeEvolutionState.hasCornerMonstera || engine.plantWaterTimer > 0f) &&
                                    abs(tapOffset.x - w * 0.10f) < 22f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f - 20f * pixelScale)) < 26f * pixelScale) {
                                    engine.onTouchPlantWatering(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }

                                // Couch Throw / Snuggle Blanket (left side of sofa)
                                if (abs(tapOffset.x - (w * 0.48f - 26f * pixelScale)) < 22f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.68f - 20f * pixelScale)) < 22f * pixelScale) {
                                    engine.onTouchCouchThrow(w, h)
                                    return@detectTapGestures
                                }

                                // Home Evolution Artifacts in Living Room:
                                // Picnic Basket (near wardrobe)
                                if (engine.homeEvolutionState.hasAdventurePicnicBasket &&
                                    abs(tapOffset.x - w * 0.77f) < 22f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f - 8f * pixelScale)) < 20f * pixelScale) {
                                    onOpenDateAdventures?.invoke()
                                    return@detectTapGestures
                                }
                                // Origami Heart on Windowsill
                                if (engine.homeEvolutionState.hasOrigamiHeart &&
                                    abs(tapOffset.x - (w * 0.70f + 14f * pixelScale)) < 18f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.28f + 18f * pixelScale)) < 18f * pixelScale) {
                                    onOpenLongDistance?.invoke()
                                    return@detectTapGestures
                                }
                                // Bedside / Coffee Table Notepad
                                if (engine.homeEvolutionState.hasBedsideNotepad &&
                                    abs(tapOffset.x - (w * 0.50f - 13f * pixelScale)) < 16f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f + 22f * pixelScale)) < 16f * pixelScale) {
                                    onOpenDailyMoment?.invoke()
                                    return@detectTapGestures
                                }
                                // Mini-Game Board on Coffee Table
                                if (engine.homeEvolutionState.hasMiniGameBoard &&
                                    abs(tapOffset.x - (w * 0.50f - 1f * pixelScale)) < 16f * pixelScale &&
                                    abs(tapOffset.y - (h * 0.65f + 23f * pixelScale)) < 16f * pixelScale) {
                                    onOpenMiniGames?.invoke()
                                    return@detectTapGestures
                                }

                                // Low Wooden Coffee Table (on rug directly in front of couch)
                                val tableX = w * 0.50f
                                val tableY = h * 0.65f + 23f * pixelScale
                                val candleX = tableX + 13f * pixelScale
                                val candleY = tableY - 4f * pixelScale
                                if (abs(tapOffset.x - candleX) < 14f * pixelScale && abs(tapOffset.y - candleY) < 14f * pixelScale) {
                                    goUse(SpotAction.LIGHT_CANDLE) { engine.onTouchAromatherapyCandle(w, h, tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                if (abs(tapOffset.x - tableX) < 28f * pixelScale && abs(tapOffset.y - tableY) < 16f * pixelScale) {
                                    engine.onTouchCoffeeTable(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Knitted Pouf (right edge of rug, beside coffee table)
                                val pfX = w * 0.70f
                                val pfY = h * 0.65f + 25f * pixelScale
                                if (abs(tapOffset.x - pfX) < 16f * pixelScale && abs(tapOffset.y - pfY) < 16f * pixelScale) {
                                    goUse(SpotAction.SIT_POUF) { engine.onTouchPouf(tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                // Mochi's Cozy Cardboard Box (bottom-left corner)
                                val boxX = w * 0.17f
                                val boxY = h * 0.65f + floorH * 0.75f
                                if (abs(tapOffset.x - boxX) < 18f * pixelScale && abs(tapOffset.y - boxY) < 16f * pixelScale) {
                                    engine.onTouchCardboardBox(tapOffset.x, tapOffset.y)
                                    return@detectTapGestures
                                }
                                // Feather Wand (beside Mochi's cardboard box)
                                val wandX = w * 0.23f
                                val wandY = boxY + 6f * pixelScale
                                if (abs(tapOffset.x - wandX) < 16f * pixelScale && abs(tapOffset.y - wandY) < 20f * pixelScale) {
                                    engine.onTouchFeatherWand(tapOffset.x, tapOffset.y)
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
                                // Vintage Stargazing Telescope on overlook
                                val teleX = w * 0.38f
                                val teleGroundY = h * 0.68f
                                if (abs(tapOffset.x - teleX) < 22f * pixelScale && abs(tapOffset.y - (teleGroundY - 20f * pixelScale)) < 26f * pixelScale) {
                                    goUse(SpotAction.USE_TELESCOPE) { engine.onTouchTelescope(w, h, tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                val curbY = h * 0.66f + 30f * pixelScale
                                val curbH = h - curbY
                                // Miniature Stone Garden Pagoda Lantern (Cluster A)
                                val pagX = w * 0.20f
                                val pagY = curbY + curbH * 0.38f
                                if (abs(tapOffset.x - pagX) < 20f * pixelScale && abs(tapOffset.y - pagY) < 18f * pixelScale) {
                                    goUse(SpotAction.ADMIRE_LANTERN) { engine.onTouchPagodaLantern(tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                // Night Lavender & Fireflies (Cluster B)
                                val lavX = w * 0.80f
                                val lavY = curbY + curbH * 0.36f
                                if (abs(tapOffset.x - lavX) < 20f * pixelScale && abs(tapOffset.y - lavY) < 18f * pixelScale) {
                                    goUse(SpotAction.SMELL_LAVENDER) { engine.onTouchLavenderPatch(tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                // Stepping river stones / glowing mushrooms (Cluster C)
                                if (tapOffset.y > curbY + curbH * 0.60f) {
                                    goUse(SpotAction.POKE_MUSHROOMS) { engine.onTouchMushrooms(tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                            }
                            EnvironmentType.TWILIGHT -> {
                                // Vintage Stargazing Telescope on grassy knoll
                                val teleX = w * 0.25f
                                val teleGroundY = h * 0.70f
                                if (abs(tapOffset.x - teleX) < 22f * pixelScale && abs(tapOffset.y - (teleGroundY - 20f * pixelScale)) < 26f * pixelScale) {
                                    goUse(SpotAction.USE_TELESCOPE) { engine.onTouchTelescope(w, h, tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                if (tapOffset.y > h * 0.65f) {
                                    if (tapOffset.y > h * 0.75f &&
                                        engine.onTouchWalkableGround(tapOffset.x, tapOffset.y, w, h)
                                    ) return@detectTapGestures
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
                                    goUse(SpotAction.SNIFF_STEAMER) { engine.onTouchMomoSteamer(w, h) }
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
                                    goUse(SpotAction.READ_CHALKBOARD) { engine.onTouchChalkboard(tapOffset.x, tapOffset.y) }
                                    return@detectTapGestures
                                }
                                // Bamboo momo steamers crate (on sidewalk right of stall)
                                val crateX = w * 0.74f
                                val crateY = pathY + 12f * pixelScale
                                if (abs(tapOffset.x - crateX) < 18f * pixelScale && abs(tapOffset.y - crateY) < 16f * pixelScale) {
                                    goUse(SpotAction.CHECK_CRATE) { engine.onTouchBambooCrate(tapOffset.x, tapOffset.y) }
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
                            EnvironmentType.RAINY_CAFE, EnvironmentType.SUNROOM, EnvironmentType.CAMPFIRE, EnvironmentType.SEASIDE_PIER -> Unit // props handled before character hit-testing
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

                        // The snowday snowman in the corner
                        val outdoorTap = engine.isCurrentSceneOutdoor
                        if (outdoorTap && WeatherLayout.isOnSnowman(tapOffset, w, h, pixelScale, engine.snowmanStage)) {
                            engine.onTouchSnowman(w, h)
                            return@detectTapGestures
                        }
                        // Rain puddles: tap for a splash and a little puddle jump
                        if (outdoorTap) {
                            val puddle = engine.particles.puddleAt(
                                tapOffset.x / w, tapOffset.y / h, w, h, WeatherLayout.weatherUnit(w, pixelScale)
                            )
                            if (puddle != null) {
                                engine.onTouchPuddle(puddle, w, h)
                                return@detectTapGestures
                            }
                        }

                        // Tap open floor/ground to invite the nearest character into a
                        // short depth-aware stroll and a tiny scene/weather response.
                        if (engine.onTouchWalkableGround(tapOffset.x, tapOffset.y, w, h)) {
                            return@detectTapGestures
                        }

                        // Catch a falling snowflake, petal, leaf or dandelion wish; or wish on the rainbow
                        if (outdoorTap) {
                            val caught = engine.particles.catchWeatherParticleAt(
                                tapOffset.x, tapOffset.y, WeatherLayout.catchRadius(w, pixelScale)
                            )
                            if (caught != null) {
                                engine.onCatchWeather(caught)
                                return@detectTapGestures
                            }
                            if (engine.rainbowTimer > 0f && WeatherLayout.isOnRainbow(tapOffset, w, h)) {
                                engine.onTouchRainbow(w, h)
                                return@detectTapGestures
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
                        // A tap on the ground puts a stargazing puzzle away.
                        if (ny >= 0.48f && engine.starPuzzle.current != null) engine.stopStarPuzzle()
                        if (ny < 0.48f) {
                            val hasStarfield = isNight && engine.currentScene.environment in listOf(
                                EnvironmentType.MEADOW,
                                EnvironmentType.TREE_HILL,
                                EnvironmentType.PATH_NIGHT,
                                EnvironmentType.MOMO_STALL,
                                EnvironmentType.TWILIGHT,
                                EnvironmentType.CAMPFIRE,
                                EnvironmentType.SEASIDE_PIER
                            )
                            if (hasStarfield) {
                                // Stargazing (plan 07, C2): tap a constellation to connect its stars.
                                if (engine.onNightSkyTap(tapOffset.x, tapOffset.y, w, h, skyDrift(engine.sceneTime))) {
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
                    onDragStart = { screenOffset ->
                        val camera = cameraNow()
                        val offset = camera.toWorld(screenOffset)
                        engine.notifyUserInteraction()
                        val w = camera.worldW
                        val h = camera.worldH
                        val pixelScale = WorldViewport.pixelScale(w)
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
                            engine.onGroundSwept(swept)
                        }
                        // Finger drawing in fresh snow
                        if (!isMochiBeingDragged && offset.y >= h * 0.65f && engine.isCurrentSceneOutdoor &&
                            engine.weather == WeatherType.SNOW
                        ) {
                            engine.endSnowStroke()
                            engine.onDrawInSnow(offset.x, offset.y, w, h)
                        }
                    },
                    onDrag = { change, screenDrag ->
                        val camera = cameraNow()
                        val position = camera.toWorld(change.position)
                        val dragAmount = Offset(camera.toWorldLength(screenDrag.x), camera.toWorldLength(screenDrag.y))
                        if (isMochiBeingDragged) {
                            change.consume()
                            val w = camera.worldW
                            val h = camera.worldH
                            val newX = (position.x / w).coerceIn(0.10f, 0.90f)
                            val newY = (position.y / h).coerceIn(0.55f, 0.85f)
                            engine.catWorldX = newX
                            engine.catWorldY = newY
                            engine.catTargetX = newX
                            engine.catTargetY = newY
                            engine.catState = CatState.WALK_FOLLOW
                        } else {
                            val w = camera.worldW
                            val h = camera.worldH
                            val isGrassArea = position.y >= h * 0.65f
                            if (engine.isCurrentSceneOutdoor && isGrassArea &&
                                (engine.weather == WeatherType.SAKURA || engine.weather == WeatherType.AUTUMN)
                            ) {
                                change.consume()
                                val pixelScale = WorldViewport.pixelScale(w)
                                val sweepRadius = 42f * pixelScale
                                val swept = engine.particles.sweepGroundParticles(
                                    touchX = position.x,
                                    touchY = position.y,
                                    cw = w,
                                    ch = h,
                                    radiusPx = sweepRadius,
                                    dragDeltaX = dragAmount.x,
                                    dragDeltaY = dragAmount.y
                                )
                                engine.onGroundSwept(swept)
                            } else if (engine.isCurrentSceneOutdoor && isGrassArea && engine.weather == WeatherType.SNOW) {
                                change.consume()
                                engine.onDrawInSnow(position.x, position.y, w, h)
                            }
                        }
                    },
                    onDragEnd = {
                        val camera = cameraNow()
                        if (isMochiBeingDragged) {
                            engine.catState = CatState.SITTING_PURR
                            engine.catSleeping = false
                            engine.catFacingLeft = false
                            engine.audio.playCatPurr()
                            val w = camera.worldW
                            val h = camera.worldH
                            engine.particles.spawnHeart(w * engine.catWorldX, h * engine.catWorldY - 20f, Color(0xFFFF8FA3))
                            engine.showMessage(GameText.get(Res.string.scene_mochi_settled_cozily_right_here), duration = 2.0f)
                            isMochiBeingDragged = false
                        }
                    },
                    onDragCancel = { isMochiBeingDragged = false }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val currentFrame = frameNanos // Explicitly read frameNanos to trigger continuous 60 FPS redraws!
            drawWorld(engine, lowResBuffer, cameraNow())
            // The birthday surprise opens in the dark (plan 09, A).
            val dark = engine.birthdaySurprise.darkness
            if (dark > 0f) drawRect(Color(0xFF05070F).copy(alpha = 0.88f * dark))
        }
        // The surprise's wish and sealed letter, over the world (only once the app gave the engine its store).
        if (engine.birthdayStore != null) {
            BirthdaySurpriseOverlay(engine)
        }

        val camera = cameraNow()
        val pixelScale = WorldViewport.pixelScale(camera.worldW)
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
        val charPixelScale = WorldViewport.characterPixelScale(camera.worldW, engine.usesLowResRenderer)
        val targetHugOffset = when {
            isKissing -> 6.2f * charPixelScale
            else -> 4.8f * charPixelScale
        }

        val rawBoyX = camera.worldW * engine.boy.worldX
        val rawGirlX = camera.worldW * engine.girl.worldX
        val midCharX = (rawBoyX + rawGirlX) / 2f

        val effectiveBoyX = rawBoyX + ((midCharX - targetHugOffset) - rawBoyX) * cuddleEased
        val effectiveGirlX = rawGirlX + ((midCharX + targetHugOffset) - rawGirlX) * cuddleEased
        val rawBoyY = camera.worldH * engine.boy.worldY
        val rawGirlY = camera.worldH * engine.girl.worldY
        val effectiveBoyY = rawBoyY + (maxOf(rawBoyY, rawGirlY) - rawBoyY) * cuddleEased
        val effectiveGirlY = rawGirlY + (maxOf(rawBoyY, rawGirlY) - rawGirlY) * cuddleEased

        val isBoySitting = engine.boy.pose == com.example.engine.CharacterPose.SIT || engine.boy.pose == com.example.engine.CharacterPose.SIT_SNUGGLE
        val isGirlSitting = engine.girl.pose == com.example.engine.CharacterPose.SIT || engine.girl.pose == com.example.engine.CharacterPose.SIT_SNUGGLE

        val boyHeadX = when {
            isRideScene -> camera.worldW * 0.50f - 14f * pixelScale
            isLoftScene -> rawBoyX
            else -> effectiveBoyX + engine.boy.idleSwayOffset
        }
        val girlHeadX = when {
            isRideScene -> camera.worldW * 0.50f + 6f * pixelScale
            isLoftScene -> rawGirlX
            else -> effectiveGirlX + engine.girl.idleSwayOffset
        }

        val boyHeadY = when {
            isRideScene -> camera.worldH * 0.70f - 52f * pixelScale
            isLoftScene -> camera.worldH * 0.55f - 24f * WorldViewport.loftCouplePixelScale(camera.worldW)
            else -> effectiveBoyY - (38.5f * pixelScale) - engine.boy.bounceOffset + (if (isBoySitting) 7.5f * pixelScale else 0f)
        }
        val girlHeadY = when {
            isRideScene -> camera.worldH * 0.70f - 52f * pixelScale
            isLoftScene -> camera.worldH * 0.55f - 24f * WorldViewport.loftCouplePixelScale(camera.worldW)
            else -> effectiveGirlY - (38.5f * pixelScale) - engine.girl.bounceOffset + (if (isGirlSitting) 7.5f * pixelScale else 0f)
        }

        if (!engine.isDreamMode) {
            PixelSpeechBubblesOverlay(
                boyText = if (showBoyBubble) engine.boySpeechText else null,
                girlText = if (showGirlBubble) engine.girlSpeechText else null,
                boyHeadX = camera.toScreenX(boyHeadX),
                boyHeadY = camera.toScreenY(boyHeadY),
                girlHeadX = camera.toScreenX(girlHeadX),
                girlHeadY = camera.toScreenY(girlHeadY),
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight
            )
        }

        // The scene's caption, at the top so the ground and the couple stay clear.
        if (!engine.isDreamMode && !engine.sceneMessage.isNullOrEmpty()) {
            PixelMessageBox(
                message = engine.sceneMessage!!,
                alpha = { engine.messageAlpha },
                belowHud = engine.catchActive || engine.cozy.fishingActive
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
                pixelScale = WorldViewport.pixelScale(viewportWidth),
                frameNanos = frameNanos
            )
        }
    }
}

/**
 * Draws one frame of the world (scenery, characters, particles, light) in screen coordinates.
 * With [lowRes] the caller has set up a downscaled canvas (see [LowResWorldBuffer]), so characters
 * use one game pixel and snap to whole game pixels.
 */
fun DrawScope.drawWorldFrame(engine: SceneEngine, lowRes: Boolean = false) {
        val timePhase = engine.timeOfDayPhase
        val isNight = timePhase.isNight
        val isSunset = timePhase.isSunset
        val isMorning = timePhase.isMorning
        val cw = size.width
        val ch = size.height
        val pixelScale = WorldViewport.pixelScale(cw)
        // Low-res renderer: characters use whole game pixels and snap to the grid.
        val charPixelScale = WorldViewport.characterPixelScale(cw, lowRes)
        fun snap(v: Float) = if (lowRes) kotlin.math.round(v / pixelScale) * pixelScale else v
        val sceneDepthBaseY = when (engine.currentScene) {
            com.example.scene.SceneType.COZY_LOFT -> 0.55f
            com.example.scene.SceneType.EVENING_RIDE -> 0.70f
            else -> 0.68f
        }
        // Depth scaling would give characters fractional pixels, so the low-res renderer skips it.
        val boyDepthScale = if (lowRes) 1f else (1f + (engine.boy.worldY - sceneDepthBaseY) * 0.85f).coerceIn(0.94f, 1.08f)
        val girlDepthScale = if (lowRes) 1f else (1f + (engine.girl.worldY - sceneDepthBaseY) * 0.85f).coerceIn(0.94f, 1.08f)

        // Dynamic Weather outdoor check
        val isOutdoor = engine.isCurrentSceneOutdoor

        // The light every cast shadow in this frame falls away from (depth, part 2): the sun on its
        // way across the sky, or by night the campfire
        SceneLight.current = CastLight(
            sunProgress = celestialProgress(isNight = false, isSunset = isSunset, isMorning = isMorning),
            outdoor = isOutdoor,
            night = isNight,
            weather = engine.weather,
            lampX = if (isNight && engine.currentScene == com.example.scene.SceneType.CAMPFIRE) cw * com.example.scene.CampfireLayout.PIT_X else null
        )

        // 1. Environmental Background (the after-rain rainbow goes in just after the sky)
        afterSkyBands = if (engine.rainbowTimer > 0f && engine.isCurrentSceneOutdoor) {
            { s -> drawRainbow(s, cw, ch, pixelScale, engine.rainbowTimer) }
        } else null
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
        // The newest painting from the street painter hangs on the loft wall (plan 11)
        if (engine.currentScene.environment == EnvironmentType.COZY_LOFT) {
            engine.visitorStore?.paintings()?.lastOrNull()?.let { painting ->
                val pw = cw * LOFT_PAINTING_W
                drawPainting(this, painting, cw * LOFT_PAINTING_X, ch * LOFT_PAINTING_Y, pw, pw * 0.72f, engine.boy.look, engine.girl.look)
            }
        }
        afterSkyBands = null

        // 1b. Ground fallen particles (leaves, sakura petals, snow on grass)
        if (isOutdoor) {
            // Puddles and snow prints lie under the fallen leaves and petals
            drawPuddles(this, cw, ch, pixelScale, engine, isNight)
            drawSnowPrints(this, cw, ch, pixelScale, engine.particles.snowPrints)
            drawGroundFallenParticles(this, engine.particles.fallenParticles, pixelScale, cw, ch)
            // Rainbow after the rain, and the snowday snowman
            drawWeatherKeepsakes(this, cw, ch, pixelScale, engine)
        }
        // A little something the couple might notice (flower, note, Mochi's toy...)
        drawDiscovery(this, cw, ch, pixelScale, engine.discovery, engine.sceneTime)

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
                -sin(t * kotlin.math.PI.toFloat()) * 5f * pixelScale
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
                boyWearsGlasses = engine.boy.wearsGlasses,
                boyLook = engine.boy.look,
                girlLook = engine.girl.look
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
                val flashAlpha = sin(t * kotlin.math.PI.toFloat()).coerceIn(0f, 1f)

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
                p = WorldViewport.loftCouplePixelScale(cw, lowRes),
                timeSeconds = engine.sceneTime,
                boyEmotion = engine.boy.emotion,
                girlEmotion = engine.girl.emotion,
                isKissing = isKissing,
                isReadingBook = engine.loftBookReading,
                boyOutfitIndex = engine.boy.outfitIndex,
                girlOutfitIndex = engine.girl.outfitIndex,
                boyAccessoryIndex = engine.boy.accessoryIndex,
                girlAccessoryIndex = engine.girl.accessoryIndex,
                boyWearsGlasses = engine.boy.wearsGlasses,
                boyLook = engine.boy.look,
                girlLook = engine.girl.look,
                earphones = engine.earphonesActive,
                boyRequest = engine.boy.requestIcon,
                girlRequest = engine.girl.requestIcon,
                requestFade = (engine.requestAsker ?: engine.boy).requestFade,
                requestSeconds = (engine.requestAsker ?: engine.boy).requestSeconds
            )
        } else {
            val isHoldingUmbrella = engine.weather == com.example.scene.WeatherType.RAIN && isOutdoor
            val isSnow = engine.weather == com.example.scene.WeatherType.SNOW && isOutdoor

            // Ensure proper facing direction when kissing or hugging
            val origBoyDir = engine.boy.direction
            val origGirlDir = engine.girl.direction
            if (isKissing || isHugging) {
                engine.boy.direction = com.example.engine.Direction.RIGHT
                engine.girl.direction = com.example.engine.Direction.LEFT
            }
            // With the rod in his hands he faces the water, whatever else the scene would have him do (plan 10, A).
            if (engine.boy.heldItem == com.example.engine.HeldItem.ROD) {
                engine.boy.direction = if (com.example.scene.CozyGames.BOBBER_X < engine.boy.worldX) com.example.engine.Direction.LEFT else com.example.engine.Direction.RIGHT
            }

            // In the rain he holds the umbrella up in the hand on her side when she's beside him,
            // so it covers them both, and its pole runs through that fist wherever he sways or
            // hops to.
            val girlGap = effectiveGirlX - effectiveBoyX
            val girlBeside = isHoldingUmbrella && abs(girlGap) < 26f * charPixelScale * PixelArtRenderer.CHARACTER_SCALE_FACTOR
            val umbrellaBackHand = girlBeside && abs(girlGap) > 3f * charPixelScale &&
                (girlGap > 0f) != (engine.boy.direction == com.example.engine.Direction.RIGHT)
            val umbrellaGrip = if (isHoldingUmbrella) {
                PixelArtRenderer.umbrellaGrip(engine.boy, snap(effectiveBoyX), snap(effectiveBoyY), charPixelScale * boyDepthScale, lowRes, umbrellaBackHand)
            } else null
            val girlSits = engine.girl.pose == com.example.engine.CharacterPose.SIT || engine.girl.pose == com.example.engine.CharacterPose.SIT_SNUGGLE
            val shareHeadTop = if (girlBeside) {
                snap(effectiveGirlY) - (if (girlSits) 21f else 26f) * charPixelScale * girlDepthScale * PixelArtRenderer.CHARACTER_SCALE_FACTOR
            } else null

            fun drawBoy() {
                PixelArtRenderer.drawCharacter(
                    drawScope = this,
                    char = engine.boy,
                    centerX = snap(effectiveBoyX),
                    bottomY = snap(effectiveBoyY),
                    pixelSize = charPixelScale * boyDepthScale,
                    isHoldingUmbrella = isHoldingUmbrella,
                    isSnow = isSnow,
                    isSpeaking = !engine.boySpeechText.isNullOrEmpty(),
                    snapToPixel = lowRes,
                    castLight = SceneLight.current,
                    umbrellaBackHand = umbrellaBackHand
                )
            }

            fun drawGirl() {
                PixelArtRenderer.drawCharacter(
                    drawScope = this,
                    char = engine.girl,
                    centerX = snap(effectiveGirlX),
                    bottomY = snap(effectiveGirlY),
                    pixelSize = charPixelScale * girlDepthScale,
                    isHoldingUmbrella = isHoldingUmbrella,
                    isSnow = isSnow,
                    isSpeaking = !engine.girlSpeechText.isNullOrEmpty(),
                    snapToPixel = lowRes,
                    castLight = SceneLight.current
                )
            }

            fun drawMochi() {
                // A move of their own (plan 12, A): a stretch, a sniff, a chase after a tail...
                val move = engine.petMove
                if (isCatInScene && !engine.mochiInBox && move != null) {
                    if (PetSprites.drawsKind(engine.petKind)) {
                        PetSprites.drawMove(this, engine.petKind, move, engine.petMoveTime, cw * engine.catWorldX, catY, pixelScale, engine.catFacingLeft)
                    } else {
                        drawCatMove(
                            this, move, engine.petMoveTime, cw * engine.catWorldX, catY, pixelScale, engine.sceneTime,
                            engine.catFacingLeft, isSnow,
                            if (engine.mochiPartyCollar) com.example.engine.WorldSprites.PARTY_COLLAR else engine.mochiCollarStyle
                        )
                    }
                    return
                }
                // In her cardboard box only her peeking head shows, drawn with the box (plan 10, C).
                if (isCatInScene && !engine.mochiInBox && PetSprites.drawsKind(engine.petKind)) {
                    // Whoever lives with them now (plan 10, E)
                    PetSprites.drawPet(
                        this, engine.petKind, cw * engine.catWorldX, catY, pixelScale, engine.sceneTime,
                        engine.catState, engine.catFacingLeft, night = engine.timeOfDayPhase.isNight,
                        castLight = SceneLight.current
                    )
                } else if (isCatInScene && !engine.mochiInBox) {
                    WorldSprites.drawCat(
                        scope = this,
                        cx = cw * engine.catWorldX,
                        groundY = catY,
                        p = pixelScale,
                        timeSeconds = engine.sceneTime,
                        catState = engine.catState,
                        isSnow = isSnow,
                        facingLeft = engine.catFacingLeft,
                        collarStyle = if (engine.mochiPartyCollar) com.example.engine.WorldSprites.PARTY_COLLAR else engine.mochiCollarStyle,
                        castLight = SceneLight.current
                    )
                }
            }

            // The umbrella's pole and handle go behind the couple (his own fist in front of the
            // pole); its canopy is drawn after them.
            if (umbrellaGrip != null) {
                WorldSprites.drawBoyHoldingUmbrella(
                    scope = this,
                    grip = umbrellaGrip,
                    timeSeconds = engine.sceneTime,
                    shareHeadTop = shareHeadTop,
                    boyLook = engine.boy.look,
                    part = WorldSprites.UmbrellaPart.POLE
                )
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
            if (umbrellaGrip != null) {
                WorldSprites.drawBoyHoldingUmbrella(
                    scope = this,
                    grip = umbrellaGrip,
                    timeSeconds = engine.sceneTime,
                    shareHeadTop = shareHeadTop,
                    boyLook = engine.boy.look,
                    part = WorldSprites.UmbrellaPart.CANOPY
                )
            }

            // Put back whatever facing the drawing borrowed.
            engine.boy.direction = origBoyDir
            engine.girl.direction = origGirlDir
        }

        // 2a. Cozy Rainy Cafe: the little table stands in front of the seated couple
        if (engine.currentScene.environment == EnvironmentType.RAINY_CAFE) {
            drawCafeTableForeground(this, cw, ch, pixelScale, engine.sceneTime, engine)
        }

        // 2a. Seaside Pier: ice-cream cones in hand and Pip flying in front of the couple
        if (engine.currentScene.environment == EnvironmentType.SEASIDE_PIER) {
            drawPierForeground(this, cw, ch, pixelScale, engine.sceneTime, engine)
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
                isSnow = engine.weather == com.example.scene.WeatherType.SNOW,
                roomTheme = engine.roomTheme,
                // Whoever lives with them sleeps on the blanket instead of Mochi (plan 10, E)
                drawSleepingPet = if (PetSprites.drawsKind(engine.petKind)) { { x: Float, y: Float ->
                    PetSprites.drawPet(this, engine.petKind, x, y + 3f * pixelScale, pixelScale, engine.sceneTime, com.example.scene.CatState.SLEEPING, facingLeft = false)
                } } else null
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
                val pageWiggle = sin(t * kotlin.math.PI.toFloat() * 6f) * (1f - t) * 2f * p

                val openBookX = stackX - 5 * p
                val openBookY = stackY - 4 * p - sin(t * kotlin.math.PI.toFloat()) * 3 * p
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
                    val bGlow = sin(bT * kotlin.math.PI.toFloat()).coerceIn(0f, 1f)
                    if (bGlow > 0f) {
                        val col = bulbColors[i % bulbColors.size]
                        drawPixelGlow(this, col.copy(alpha = bGlow * 0.55f), (5f + bGlow * 7f) * p, androidx.compose.ui.geometry.Offset(bx, by + 2 * p), p)
                        drawPixelGlow(this, Color.White.copy(alpha = bGlow * 0.85f), 2.2f * p, androidx.compose.ui.geometry.Offset(bx, by + 2 * p), p)
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

        // Mochi's heart meter and the kitchen's keepsake shelf (plan 07, D2-D3).
        drawMochiMeter(this, engine, cw, ch, pixelScale)
        if (engine.currentScene == com.example.scene.SceneType.COOKING) drawKeepsakeShelf(this, engine.keepsakeShelf, cw, ch, pixelScale)

        // Catch together (plan 07, C1): the basket and the golden stars.
        drawCatchGame(this, engine.catchGame, cw, ch, pixelScale, engine.sceneTime)

        // Cooking, fishing and garden care (plan 07, C3-C5).
        drawFridayFox(this, engine, cw, ch, pixelScale)
        drawVisitors(this, engine, cw, ch, pixelScale)
        drawPetVisitor(this, engine, cw, ch, pixelScale)
        drawCozyGames(this, engine, cw, ch, pixelScale, engine.sceneTime)

        // Special days (plan 06, G2): a garland and the day's touch, lit like the rest of the scene.
        com.example.engine.SpecialDays.today()?.let { day ->
            drawSpecialDayDecor(this, day, cw, ch, pixelScale, engine.sceneTime, isNight || timePhase.isMidnight)
        }
        // The birthday party (plan 09, A): a banner, and the gift box until it's opened.
        drawBirthdayParty(this, engine, cw, ch, pixelScale)
        // Couple life (plan 09, C): the Thank-You Jar, and the Make-Up Bench's cloud and rainbow.
        drawCoupleLife(this, engine, cw, ch, pixelScale)
        // Festivals (plan 09, D): the poster, the decorations and the lanterns rising.
        drawFestival(this, engine, cw, ch, pixelScale)

        // Atmospheric Lighting & Time-of-Day Layering
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
            drawIndoorLight(timePhase, Offset.Zero, size)
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
        // The pixel renderer dissolves scene changes block by block instead (LowResWorldBuffer).
        if (engine.wipeAlpha > 0f && !lowRes) {
            val fadeAlpha = (engine.wipeAlpha * engine.wipeAlpha).coerceIn(0f, 1f)
            drawRect(
                color = Color(0xFF0F1423).copy(alpha = fadeAlpha),
                topLeft = Offset.Zero,
                size = size
            )
        }
}

/**
 * Indoor lighting over [topLeft]..[size]: distinctly warmer and cozier than the chilly outdoors,
 * with soft shading in the evening. Shared by the frame and by floor or wall continued beyond the
 * stage, so both match.
 */
fun DrawScope.drawIndoorLight(timePhase: com.example.engine.TimeOfDayPhase, topLeft: Offset, size: Size) {
    val warmth = when {
        timePhase.isMidnight -> 0.14f
        timePhase.isNight -> 0.10f
        timePhase.isSunset -> 0.08f
        else -> 0.04f
    }
    drawRect(Color(0xFFFFB703).copy(alpha = warmth), topLeft, size)
    if (timePhase.isNight || timePhase.isMidnight) {
        drawRect(Color(0xFF1B1124).copy(alpha = if (timePhase.isMidnight) 0.12f else 0.07f), topLeft, size)
    }
}

/** The loft's lighting (indoor warmth, dimming, the lamp switched off) over the roof or room continued beyond its stage. */
private fun DrawScope.drawLoftLight(engine: SceneEngine, topLeft: Offset, size: Size) {
    if (!engine.lampLit) drawRect(Color(0x50090D24), topLeft, size)
    drawIndoorLight(engine.timeOfDayPhase, topLeft, size)
    if (engine.ambientDimming > 0f) drawRect(Color(0xFF0D1117).copy(alpha = engine.ambientDimming), topLeft, size)
}

/**
 * Keeps the frame ticker from starting, so the world stays exactly as it was set up. Only for
 * JVM screenshot tests: there, frames arrive back to back without the clock moving and the
 * ticker would spin.
 */
var frameTickerPaused = false

/** True when the current scene is drawn by the low-res pixel renderer (Plan 03, Phase 1). */
@Suppress("UnusedReceiverParameter")
val SceneEngine.usesLowResRenderer: Boolean
    get() = WorldViewport.pixelRenderer

/** Draws the world, through the low-res pixel renderer for the scenes that use it (Plan 03, Phase 1). */
fun DrawScope.drawWorld(engine: SceneEngine, lowResBuffer: LowResWorldBuffer, camera: WorldCamera) {
    if (engine.usesLowResRenderer && camera.staged) {
        val weather: (DrawScope.() -> Unit)? = if (engine.isCurrentSceneOutdoor) {
            { drawFallingWeather(this, engine.particles.particles, WorldViewport.pixelScale(camera.worldW)) }
        } else null
        // The sky continued above an outdoor stage gets clouds by day and the scene's own kind of
        // stars by night.
        val phase = engine.timeOfDayPhase
        val p = WorldViewport.pixelScale(camera.worldW)
        val skyAbove: (DrawScope.(Float) -> Unit)? = if (engine.currentScene == com.example.scene.SceneType.RAINY_CAFE) {
            { ceilingH -> drawCafeCeilingAbove(this, camera.worldW, ceilingH, p) }
        } else if (engine.currentScene == com.example.scene.SceneType.SLEEP) {
            { wallH -> drawBedroomWallAbove(this, camera.worldW, wallH, p) }
        } else if (engine.currentScene == com.example.scene.SceneType.COZY_LOFT) {
            { ceilingH ->
                drawLoftCeilingAbove(this, camera.worldW, camera.worldH, ceilingH, p, phase.isNight || phase.isMidnight, phase.isSunset)
                // Light it like the room, so the continued roof matches the stage.
                drawLoftLight(engine, Offset(0f, -ceilingH), Size(camera.worldW, ceilingH))
            }
        } else if (engine.isCurrentSceneOutdoor) {
            { skyH ->
                if (phase.isNight) {
                    drawNightStarsAbove(this, camera.worldW, skyH, engine.sceneTime, p)
                } else if (skyH > 30f * p) {
                    drawSkyCloud(this, camera.worldW, camera.worldW * 0.70f, -skyH * 0.72f, 0.6f, engine.sceneTime, p, phase.isSunset, phase.isMorning)
                    drawSkyCloud(this, camera.worldW, camera.worldW * 0.20f, -skyH * 0.32f, 0.85f, engine.sceneTime, p, phase.isSunset, phase.isMorning)
                }
            }
        } else null
        val floorBelow: (DrawScope.(Float) -> Unit)? = if (engine.currentScene == com.example.scene.SceneType.SUNROOM) {
            { floorH ->
                val sunny = !phase.isNight &&
                    engine.weather != com.example.scene.WeatherType.RAIN && engine.weather != com.example.scene.WeatherType.SNOW
                drawSunroomFloorBelow(this, camera.worldW, camera.worldH, floorH, p, night = phase.isNight, sunny = sunny)
                // Light it like the room, so the continued floor matches the stage's floor.
                drawIndoorLight(engine.timeOfDayPhase, Offset(0f, camera.worldH), Size(camera.worldW, floorH))
                if (engine.ambientDimming > 0f) {
                    drawRect(Color(0xFF0D1117).copy(alpha = engine.ambientDimming), Offset(0f, camera.worldH), Size(camera.worldW, floorH))
                }
            }
        } else if (engine.currentScene == com.example.scene.SceneType.RAINY_CAFE) {
            { floorH ->
                drawCafeFloorBelow(
                    this, camera.worldW, camera.worldH, floorH, p,
                    night = phase.isNight, lampsBright = phase.isNight || phase.isSunset
                )
                // Light it like the room, so the continued floor matches the stage's floor.
                drawIndoorLight(engine.timeOfDayPhase, Offset(0f, camera.worldH), Size(camera.worldW, floorH))
                if (engine.ambientDimming > 0f) {
                    drawRect(Color(0xFF0D1117).copy(alpha = engine.ambientDimming), Offset(0f, camera.worldH), Size(camera.worldW, floorH))
                }
            }
        } else if (engine.currentScene == com.example.scene.SceneType.COZY_LOFT) {
            { belowH ->
                com.example.engine.LoftSprites.drawLoftBelowStage(this, camera.worldW, camera.worldH, belowH, p)
                drawLoftLight(engine, Offset(0f, camera.worldH), Size(camera.worldW, belowH))
            }
        } else if (engine.currentScene == com.example.scene.SceneType.COOKING) {
            { floorH ->
                drawKitchenFloorBelow(this, camera.worldW, camera.worldH, floorH, p)
                // Light it like the room, so the continued floor matches the stage's floor.
                drawIndoorLight(engine.timeOfDayPhase, Offset(0f, camera.worldH), Size(camera.worldW, floorH))
                if (engine.ambientDimming > 0f) {
                    drawRect(Color(0xFF0D1117).copy(alpha = engine.ambientDimming), Offset(0f, camera.worldH), Size(camera.worldW, floorH))
                }
            }
        } else null
        // Sprite animation (flames, water, bobbing, twinkles) steps like a pixel game: the frame is
        // drawn with the scene clock rounded down to the sprite frame rate. Movement itself still
        // updates every frame, on whole pixels.
        val realTime = engine.sceneTime
        engine.sceneTime = SpriteClock.step(realTime)
        try {
            lowResBuffer.drawStaged(
                this, camera, starrySky = false, beyondStage = weather, aboveStage = skyAbove, belowStage = floorBelow,
                dissolve = (engine.wipeAlpha * engine.wipeAlpha).coerceIn(0f, 1f),
                frameKey = (engine.sceneTime * SpriteClock.FPS).toLong() + engine.currentScene.ordinal * 1_000_000L
            ) {
                drawWorldFrame(engine, lowRes = true)
            }
        } finally {
            engine.sceneTime = realTime
        }
    } else {
        drawWorldFrame(engine)
    }
}

/** Keeps the last camera until the screen, the scene or the button strips change (no per-frame allocation). */
class CameraCache {
    private var w = -1f
    private var h = -1f
    private var scene: com.example.scene.SceneType? = null
    private var top = -1f
    private var bottom = -1f
    private var camera: WorldCamera? = null

    fun get(width: Float, height: Float, scene: com.example.scene.SceneType, topReserve: Float, bottomReserve: Float): WorldCamera {
        val cached = camera
        if (cached != null && width == w && height == h && scene == this.scene && topReserve == top && bottomReserve == bottom) return cached
        w = width; h = height; this.scene = scene; top = topReserve; bottom = bottomReserve
        return WorldCamera.forScreen(width, height, scene, topReservePx = topReserve, bottomReservePx = bottomReserve).also { camera = it }
    }
}

/** Where the newest painting hangs on the loft wall (plan 11), as shares of the world. */
private const val LOFT_PAINTING_X = 0.08f
private const val LOFT_PAINTING_Y = 0.20f
private const val LOFT_PAINTING_W = 0.14f
