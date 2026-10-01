package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.data.PolaroidManager
import com.example.data.PolaroidMemory
import com.example.engine.AmbientAudio
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    targetScene: String? = null,
    targetToken: Long = 0L,
    targetAtmosphere: String? = null,
    targetBirdSurface: String? = null
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val audio = remember {
        AmbientAudio(context.applicationContext).apply {
            isEnabled = prefs.soundEnabled
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val rootView = LocalView.current
    val polaroidManager = remember { PolaroidManager(context) }

    // Modal dialogs state
    var showMemories by remember { mutableStateOf(false) }
    var showLoveNotes by remember { mutableStateOf(false) }
    var showTinyMoment by remember { mutableStateOf(false) }
    var showScenePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSecretKeepsake by remember { mutableStateOf(false) }
    var showMusicBox by remember { mutableStateOf(false) }
    var showSpecialCalendar by remember { mutableStateOf(false) }
    var showWardrobe by remember { mutableStateOf(false) }
    var showDreamJournal by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(!prefs.isOnboardingCompleted) }

    var girlOutfitIndex by remember { mutableStateOf(prefs.girlOutfitIndex) }
    var girlAccessoryIndex by remember { mutableStateOf(prefs.girlAccessoryIndex) }
    var boyOutfitIndex by remember { mutableStateOf(prefs.boyOutfitIndex) }
    var boyAccessoryIndex by remember { mutableStateOf(prefs.boyAccessoryIndex) }

    // Polaroid capture state
    var polaroidCaptureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var polaroidCaptureMemory by remember { mutableStateOf<PolaroidMemory?>(null) }
    var showPolaroidOverlay by remember { mutableStateOf(false) }
    var showPolaroidGallery by remember { mutableStateOf(false) }

    var glassIntensity by remember { mutableStateOf(prefs.buttonGlassIntensity) }

    // Hoisted glassmorphism styling tokens (dynamic based on glassIntensity, zero per-frame allocations)
    val capsuleShape = remember { RoundedCornerShape(24.dp) }
    val heartButtonShape = remember { CircleShape }
    val glassyCircleShape = remember { CircleShape }

    val buttonFillBrush = remember(glassIntensity) {
        val topAlpha = (0.20f + 0.75f * glassIntensity).coerceIn(0.12f, 0.95f)
        val bottomAlpha = (0.08f + 0.55f * glassIntensity).coerceIn(0.06f, 0.75f)
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = bottomAlpha)
            )
        )
    }

    val buttonActiveFillBrush = remember(glassIntensity) {
        val topAlpha = (0.25f + 0.70f * glassIntensity).coerceIn(0.15f, 0.95f)
        val bottomAlpha = (0.15f + 0.55f * glassIntensity).coerceIn(0.10f, 0.80f)
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFCCD5).copy(alpha = topAlpha),
                Color(0xFFFFB6C1).copy(alpha = bottomAlpha)
            )
        )
    }

    val buttonBorderBrush = remember(glassIntensity) {
        val topAlpha = (0.35f + 0.60f * glassIntensity).coerceIn(0.20f, 0.98f)
        val bottomAlpha = (0.10f + 0.35f * glassIntensity).coerceIn(0.08f, 0.50f)
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = bottomAlpha)
            )
        )
    }
    val buttonBorderStroke = remember(glassIntensity) { BorderStroke(1.dp, buttonBorderBrush) }

    val buttonElevation = remember(glassIntensity) { (1.5.dp + 3.dp * glassIntensity).coerceAtLeast(1.dp) }
    val glassShadowAmbient = remember(glassIntensity) {
        val alpha = (0.10f + 0.25f * glassIntensity).coerceIn(0.05f, 0.35f)
        Color.Black.copy(alpha = alpha)
    }
    val glassShadowSpot = remember(glassIntensity) {
        val alpha = (0.08f + 0.20f * glassIntensity).coerceIn(0.04f, 0.28f)
        Color.Black.copy(alpha = alpha)
    }

    // Scene Engine
    val engine = remember {
        SceneEngine(
            audio = audio,
            onOpenLoveNotes = { showLoveNotes = true },
            onOpenMemories = { showMemories = true },
            onOpenCalendar = { showSpecialCalendar = true },
            onOpenWardrobe = { showWardrobe = true },
            onOpenDreamJournal = { showDreamJournal = true }
        ).apply {
            updateNames(prefs.boyfriendName, prefs.girlfriendName)
            updateAtmosphereMode(prefs.atmosphereMode)
            gardenStage = prefs.gardenStage
            girl.outfitIndex = prefs.girlOutfitIndex
            girl.accessoryIndex = prefs.girlAccessoryIndex
            boy.outfitIndex = prefs.boyOutfitIndex
            boy.accessoryIndex = prefs.boyAccessoryIndex
            val initial = SceneType.values().firstOrNull { it.name == prefs.lastSceneId }
            val chosen = nextRandomScene(initial)
            prefs.addRecentScene(chosen.name)
            loadScene(chosen)
        }
    }

    var atmosphere by remember { mutableStateOf(prefs.atmosphereMode) }

    LaunchedEffect(atmosphere) {
        engine.updateAtmosphereMode(atmosphere)
    }

    LaunchedEffect(Unit) {
        val loadedLocal = com.example.data.ProfileManager.loadFromLocalFile(context)
        if (loadedLocal) {
            prefs.isOnboardingCompleted = true
            showOnboarding = false
            engine.updateNames(prefs.boyfriendName, prefs.girlfriendName)
        }
        val anniv = runCatching { java.time.LocalDate.parse(prefs.anniversaryDate) }.getOrDefault(java.time.LocalDate.now())
        com.example.data.RelationshipTimeManager.relationshipStartDate = anniv
        com.example.data.SpecialCalendarManager.boyName = prefs.boyfriendName
        com.example.data.SpecialCalendarManager.girlName = prefs.girlfriendName
        com.example.data.SpecialCalendarManager.boyBirthday = runCatching { java.time.LocalDate.parse(prefs.boyfriendBirthday) }.getOrNull()
        com.example.data.SpecialCalendarManager.girlBirthday = runCatching { java.time.LocalDate.parse(prefs.girlfriendBirthday) }.getOrNull()
    }

    val isDark = engine.timeOfDayPhase.isNight || engine.timeOfDayPhase.isSunset
    val clearFactor = (1f - glassIntensity).coerceIn(0f, 1f)

    // Dynamic contrast-boosted icon colors: as glass becomes clear/subtle, icon contrast is boosted
    val neutralIconTint = remember(glassIntensity, isDark) {
        if (isDark) {
            androidx.compose.ui.graphics.lerp(DarkSlate, Color(0xFFFFF0F5), clearFactor)
        } else {
            androidx.compose.ui.graphics.lerp(DarkSlate.copy(alpha = 0.80f), Color(0xFF0F172A), clearFactor)
        }
    }

    val mutedIconTint = remember(glassIntensity, isDark) {
        if (isDark) {
            androidx.compose.ui.graphics.lerp(DarkSlate.copy(alpha = 0.65f), Color.White.copy(alpha = 0.90f), clearFactor)
        } else {
            androidx.compose.ui.graphics.lerp(DarkSlate.copy(alpha = 0.65f), Color(0xFF334155), clearFactor)
        }
    }

    val activeHeartTint = remember(glassIntensity, isDark) {
        if (isDark) {
            androidx.compose.ui.graphics.lerp(DeepRose, Color(0xFFFF6B81), clearFactor)
        } else {
            androidx.compose.ui.graphics.lerp(DeepRose, Color(0xFFBE123C), clearFactor)
        }
    }

    val shuffleIconTint = remember(glassIntensity, isDark) {
        if (isDark) {
            androidx.compose.ui.graphics.lerp(Color(0xFF1D4ED8), Color(0xFF93C5FD), clearFactor)
        } else {
            androidx.compose.ui.graphics.lerp(Color(0xFF1D4ED8), Color(0xFF1E40AF), clearFactor)
        }
    }

    val weatherIconTint = remember(glassIntensity, isDark, engine.weather) {
        val baseColor = when (engine.weather) {
            com.example.scene.WeatherType.SUNNY -> Color(0xFFE65100)
            com.example.scene.WeatherType.RAIN -> Color(0xFF1565C0)
            com.example.scene.WeatherType.SAKURA -> Color(0xFFD81B60)
            com.example.scene.WeatherType.AUTUMN -> Color(0xFFBF360C)
            com.example.scene.WeatherType.SNOW -> Color(0xFF0288D1)
        }
        if (isDark) {
            val brightNight = when (engine.weather) {
                com.example.scene.WeatherType.SUNNY -> Color(0xFFFFA726)
                com.example.scene.WeatherType.RAIN -> Color(0xFF60A5FA)
                com.example.scene.WeatherType.SAKURA -> Color(0xFFFF80AB)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFFFF8A65)
                com.example.scene.WeatherType.SNOW -> Color(0xFF81D4FA)
            }
            androidx.compose.ui.graphics.lerp(baseColor, brightNight, clearFactor)
        } else {
            baseColor
        }
    }

    val titleTextColor = remember(glassIntensity, isDark) {
        if (isDark) {
            androidx.compose.ui.graphics.lerp(DarkSlate, Color(0xFFFFF0F5), clearFactor)
        } else {
            androidx.compose.ui.graphics.lerp(DarkSlate, Color(0xFF0F172A), clearFactor)
        }
    }

    val dotColor = remember(glassIntensity, isDark) {
        if (isDark) {
            androidx.compose.ui.graphics.lerp(DarkSlate.copy(alpha = 0.45f), Color.White.copy(alpha = 0.70f), clearFactor)
        } else {
            androidx.compose.ui.graphics.lerp(DarkSlate.copy(alpha = 0.45f), Color(0xFF475569), clearFactor)
        }
    }

    LaunchedEffect(targetScene, targetToken, targetAtmosphere, targetBirdSurface) {
        android.util.Log.d("TinyUs", "MainScreen LaunchedEffect targetScene: $targetScene, token: $targetToken, targetAtmosphere: $targetAtmosphere, birdSurface: $targetBirdSurface")
        atmosphere = targetAtmosphere ?: prefs.atmosphereMode
        if (!targetScene.isNullOrBlank()) {
            val sc = SceneType.values().firstOrNull { it.name.equals(targetScene, ignoreCase = true) }
            android.util.Log.d("TinyUs", "MainScreen matched SceneType: $sc")
            if (sc != null) {
                engine.loadScene(sc)
            }
        }
        if (!targetBirdSurface.isNullOrBlank()) {
            val surf = try { com.example.engine.PerchSurface.valueOf(targetBirdSurface) } catch (e: Exception) { null }
            if (surf != null) {
                val p = (screenWidthPx / 115f).coerceIn(3.0f, 5.0f)
                val (destX, destY) = engine.birdSystem.getSurfaceCoordinates(surf, screenWidthPx, screenHeightPx, p)
                val bird = engine.forceSpawnBirdForTest(surface = surf, cw = screenWidthPx, ch = screenHeightPx, p = p)
                if (bird != null) {
                    bird.x = destX
                    bird.y = destY
                    bird.state = when (surf) {
                        com.example.engine.PerchSurface.MEADOW_GROUND,
                        com.example.engine.PerchSurface.TREE_GROUND,
                        com.example.engine.PerchSurface.TWILIGHT_GROUND -> com.example.engine.BirdState.PECKING
                        else -> com.example.engine.BirdState.PERCHED
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit, engine.weather) {
        prefs.markAppOpenedToday()
        engine.gardenStage = prefs.gardenStage
        engine.homeEvolutionState = prefs.getHomeEvolutionState(engine.weather)
    }

    // Subtle interaction hint fade
    var showHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(7000)
        showHint = false
    }

    var isSoundOn by remember { mutableStateOf(prefs.soundEnabled) }
    var memoriesList by remember { mutableStateOf(prefs.getMemories()) }
    var notesList by remember { mutableStateOf(prefs.getLoveNotes()) }

    // Synchronize audio playback state with isSoundOn and app preferences
    LaunchedEffect(isSoundOn) {
        audio.isEnabled = isSoundOn
        prefs.soundEnabled = isSoundOn
        if (isSoundOn && audio.currentWeather == null) {
            audio.playWeatherBgm(engine.weather, isAutomaticDrift = false)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> audio.pauseAll()
                Lifecycle.Event.ON_RESUME -> audio.resumeAll()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audio.release()
        }
    }

    val screenBgColor = remember(isDark, engine.weather) {
        if (isDark) DarkSlate else when (engine.weather) {
            com.example.scene.WeatherType.SAKURA -> Color(0xFF6BA8D6)
            com.example.scene.WeatherType.AUTUMN -> Color(0xFF4A85B8)
            com.example.scene.WeatherType.SNOW -> Color(0xFF5D99C6)
            com.example.scene.WeatherType.RAIN -> Color(0xFF334B68)
            else -> Color(0xFF3B9FE2)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        // 1. Edge-to-Edge Pure Pixel World
        PixelWorldView(
            engine = engine,
            atmosphereMode = atmosphere,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Glassmorphism Top Controls (Translucent frosted capsule design)
        // 2. Glassmorphism Top Controls (Translucent frosted capsule design)
        // Responsive Top Controls: Single-line layout guaranteed across all phone screen sizes
        val renderTitlePill: @Composable (Modifier, androidx.compose.ui.unit.Dp) -> Unit = { pillModifier, screenWidth ->
            val isCompact = screenWidth < 360.dp
            val isMedium = screenWidth < 410.dp
            val pillPadH = if (isCompact) 6.dp else if (isMedium) 7.dp else 8.dp
            val pillPadV = if (isCompact) 4.dp else 5.dp
            val heartSize = if (isCompact) 12.dp else if (isMedium) 13.dp else 14.dp
            val titleFontSize = if (isCompact) 10.sp else if (isMedium) 11.sp else 11.5.sp
            val dotFontSize = if (isCompact) 9.sp else if (isMedium) 10.sp else 10.5.sp
            val namesFontSize = if (isCompact) 9.5.sp else if (isMedium) 10.sp else 10.5.sp

            Box(
                modifier = pillModifier
                    .testTag("app_title_chip")
                    .shadow(
                        elevation = buttonElevation,
                        shape = capsuleShape,
                        ambientColor = glassShadowAmbient,
                        spotColor = glassShadowSpot
                    )
                    .clip(capsuleShape)
                    .background(buttonFillBrush)
                    .border(buttonBorderStroke, capsuleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = pillPadH, vertical = pillPadV),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { showTinyMoment = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContrastIcon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = activeHeartTint,
                            modifier = Modifier.size(heartSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tiny Us",
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize,
                            fontFamily = FontFamily.Serif,
                            color = titleTextColor,
                            style = if (clearFactor > 0.15f) {
                                TextStyle(
                                    shadow = Shadow(
                                        color = if (isDark) Color.Black.copy(alpha = (0.75f * clearFactor).coerceIn(0.2f, 0.85f)) else Color.White.copy(alpha = (0.75f * clearFactor).coerceIn(0.2f, 0.85f)),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            } else {
                                TextStyle.Default
                            },
                            maxLines = 1
                        )
                    }

                    Text(
                        text = " • ",
                        fontSize = dotFontSize,
                        color = dotColor,
                        modifier = Modifier.padding(horizontal = 1.dp)
                    )

                    // Secretive interactive tap on couple names
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showSecretKeepsake = true
                                audio.playHeartChime()
                                engine.particles.spawnHeart(400f, 250f)
                            }
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${prefs.boyfriendName} & ${prefs.girlfriendName}",
                            fontSize = namesFontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = activeHeartTint,
                            style = if (clearFactor > 0.15f) {
                                TextStyle(
                                    shadow = Shadow(
                                        color = if (isDark) Color.Black.copy(alpha = (0.75f * clearFactor).coerceIn(0.2f, 0.85f)) else Color.White.copy(alpha = (0.75f * clearFactor).coerceIn(0.2f, 0.85f)),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            } else {
                                TextStyle.Default
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val renderButtonsRow: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { screenWidth ->
            val buttonTouchSize = when {
                screenWidth < 360.dp -> 36.dp
                screenWidth < 410.dp -> 38.dp
                else -> 42.dp
            }
            val buttonVisualSize = when {
                screenWidth < 360.dp -> 31.dp
                screenWidth < 410.dp -> 33.dp
                else -> 36.dp
            }
            val iconSize = when {
                screenWidth < 360.dp -> 15.dp
                screenWidth < 410.dp -> 16.dp
                else -> 17.dp
            }
            val spacing = when {
                screenWidth < 360.dp -> 1.5.dp
                screenWidth < 410.dp -> 2.dp
                else -> 3.dp
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic Weather Cycle Button
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = { engine.cycleWeather() }
                        )
                        .testTag("weather_cycle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonVisualSize)
                            .shadow(
                                elevation = buttonElevation,
                                shape = glassyCircleShape,
                                ambientColor = glassShadowAmbient,
                                spotColor = glassShadowSpot
                            )
                            .clip(glassyCircleShape)
                            .background(buttonFillBrush)
                            .border(buttonBorderStroke, glassyCircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ContrastIcon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Weather: ${engine.weather.displayName}",
                            tint = weatherIconTint,
                            modifier = Modifier.size(iconSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                    }
                }

                // Audio Toggle
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                val target = !isSoundOn
                                isSoundOn = target
                                prefs.soundEnabled = target
                                audio.isEnabled = target
                            }
                        )
                        .testTag("sound_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonVisualSize)
                            .shadow(
                                elevation = buttonElevation,
                                shape = glassyCircleShape,
                                ambientColor = glassShadowAmbient,
                                spotColor = glassShadowSpot
                            )
                            .clip(glassyCircleShape)
                            .background(buttonFillBrush)
                            .border(buttonBorderStroke, glassyCircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ContrastIcon(
                            imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle Audio",
                            tint = if (isSoundOn) activeHeartTint else mutedIconTint,
                            modifier = Modifier.size(iconSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                    }
                }

                // Shared Earphones & Music Player Button
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                showMusicBox = true
                            }
                        )
                        .testTag("earphones_music_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonVisualSize)
                            .shadow(
                                elevation = buttonElevation,
                                shape = glassyCircleShape,
                                ambientColor = glassShadowAmbient,
                                spotColor = glassShadowSpot
                            )
                            .clip(glassyCircleShape)
                            .background(if (engine.earphonesActive) buttonActiveFillBrush else buttonFillBrush)
                            .border(buttonBorderStroke, glassyCircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ContrastIcon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Music Box & Earphones",
                            tint = if (engine.earphonesActive) activeHeartTint else neutralIconTint,
                            modifier = Modifier.size(iconSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                    }
                }

                // Random Scene Surprise Button
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                val next = engine.nextRandomScene()
                                prefs.addRecentScene(next.name)
                                engine.loadScene(next)
                            }
                        )
                        .testTag("random_scene_icon_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonVisualSize)
                            .shadow(
                                elevation = buttonElevation,
                                shape = glassyCircleShape,
                                ambientColor = glassShadowAmbient,
                                spotColor = glassShadowSpot
                            )
                            .clip(glassyCircleShape)
                            .background(buttonFillBrush)
                            .border(buttonBorderStroke, glassyCircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ContrastIcon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Random Scene",
                            tint = shuffleIconTint,
                            modifier = Modifier.size(iconSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                    }
                }

                // Settings Button
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = { showSettings = true }
                        )
                        .testTag("dock_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(buttonVisualSize)
                            .shadow(
                                elevation = buttonElevation,
                                shape = glassyCircleShape,
                                ambientColor = glassShadowAmbient,
                                spotColor = glassShadowSpot
                            )
                            .clip(glassyCircleShape)
                            .background(buttonFillBrush)
                            .border(buttonBorderStroke, glassyCircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ContrastIcon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = neutralIconTint,
                            modifier = Modifier.size(iconSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !engine.isDreamMode,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(250))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val screenWidth = maxWidth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    renderTitlePill(Modifier.weight(1f, fill = false), screenWidth)
                    Spacer(modifier = Modifier.width(if (screenWidth < 360.dp) 4.dp else 6.dp))
                    renderButtonsRow(screenWidth)
                }
            }
        }

        // 3. Subtle In-World Interaction Hint (Fades out automatically)
        AnimatedVisibility(
            visible = showHint && !engine.isDreamMode,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(800)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.42f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Tap characters, cottage, tree, sky, or mailbox to explore",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // 4. Floating Heart / Tiny Moment Capture Button
        // Single tap → capture Polaroid. Long press → open gallery.
        AnimatedVisibility(
            visible = !engine.isDreamMode,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = buttonElevation,
                        shape = heartButtonShape,
                        ambientColor = glassShadowAmbient,
                        spotColor = glassShadowSpot
                    )
                    .clip(heartButtonShape)
                    .background(buttonFillBrush)
                    .border(buttonBorderStroke, heartButtonShape)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // 1. Sparkle burst & camera shutter sound
                            engine.triggerThinkingOfYou(screenWidthPx, screenHeightPx)
                            audio.playCameraShutter()

                            // 2. Capture scene as bitmap, render complete Polaroid card, then show Polaroid overlay
                            coroutineScope.launch {
                                val bmp = withContext(Dispatchers.Main) {
                                    runCatching {
                                        val v = rootView
                                        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
                                        val c = AndroidCanvas(b)
                                        v.draw(c)
                                        b
                                    }.getOrNull()
                                }

                                if (bmp != null) {
                                    val scene = engine.currentScene
                                    val envKey = scene.environment.name
                                    val title = polaroidManager.pickTitle(
                                        sceneEnvKey = envKey,
                                        isNight = isDark,
                                        isSunset = engine.timeOfDayPhase.isSunset
                                    )
                                    val dateStr = polaroidManager.formattedDate()
                                    val timeStr = polaroidManager.formattedTime()

                                    val polaroidCardBmp = withContext(Dispatchers.Default) {
                                        polaroidManager.renderPolaroidCard(
                                            sceneBitmap = bmp,
                                            title = title,
                                            date = dateStr,
                                            time = timeStr,
                                            sceneName = scene.title
                                        )
                                    }

                                    val imagePath = withContext(Dispatchers.IO) {
                                        polaroidManager.saveBitmap(polaroidCardBmp)
                                    }
                                    val memory = PolaroidMemory(
                                        id = UUID.randomUUID().toString(),
                                        title = title,
                                        date = dateStr,
                                        time = timeStr,
                                        sceneName = scene.title,
                                        sceneEnvKey = envKey,
                                        imagePath = imagePath
                                    )
                                    polaroidManager.savePolaroid(memory)
                                    polaroidCaptureBitmap = polaroidCardBmp
                                    polaroidCaptureMemory = memory
                                    showPolaroidOverlay = true
                                }
                            }
                        },
                        onLongClick = {
                            audio.playHeartChime()
                            showPolaroidGallery = true
                        }
                    )
                    .testTag("thinking_of_you_button"),
                contentAlignment = Alignment.Center
            ) {
                ContrastIcon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Capture Tiny Moment (Long press for Gallery)",
                    tint = activeHeartTint,
                    modifier = Modifier.size(26.dp),
                    clearFactor = clearFactor,
                    isDark = isDark
                )
            }
        }


        // 5. In-World Pixel Dialogs
        if (showMemories) {
            MemoriesDialog(
                memories = memoriesList,
                onDismiss = { showMemories = false },
                onAddMemory = { title, note, date, iconType ->
                    prefs.addMemory(title, note, date, iconType)
                    memoriesList = prefs.getMemories()
                }
            )
        }

        if (showLoveNotes) {
            LoveNotesDialog(
                notes = notesList,
                boyfriendName = prefs.boyfriendName,
                girlfriendName = prefs.girlfriendName,
                onDismiss = { showLoveNotes = false },
                onAddNote = { text, author ->
                    prefs.addLoveNote(text, author)
                    notesList = prefs.getLoveNotes()
                }
            )
        }

        if (showTinyMoment) {
            DailyTinyMomentDialog(
                moment = prefs.getTodayTinyMoment(),
                daysTogether = prefs.getDaysTogether(),
                boyfriendName = prefs.boyfriendName,
                girlfriendName = prefs.girlfriendName,
                allMoments = prefs.getAllMoments(),
                onDismiss = { showTinyMoment = false },
                onJumpToScene = { sc ->
                    engine.loadScene(sc)
                    prefs.addRecentScene(sc.name)
                    engine.triggerWatchScene()
                }
            )
        }

        if (showScenePicker) {
            ScenePickerDialog(
                currentScene = engine.currentScene,
                onDismiss = { showScenePicker = false },
                onSelectScene = { sc ->
                    engine.loadScene(sc)
                    prefs.addRecentScene(sc.name)
                    engine.triggerWatchScene()
                },
                onRandomScene = {
                    val next = engine.nextRandomScene()
                    prefs.addRecentScene(next.name)
                    engine.loadScene(next)
                }
            )
        }

        if (showSettings) {
            SettingsBottomSheet(
                prefs = prefs,
                onDismiss = { showSettings = false },
                onSettingsChanged = {
                    atmosphere = prefs.atmosphereMode
                    isSoundOn = prefs.soundEnabled
                    audio.isEnabled = isSoundOn
                    engine.updateNames(prefs.boyfriendName, prefs.girlfriendName)
                    glassIntensity = prefs.buttonGlassIntensity
                    val anniv = runCatching { java.time.LocalDate.parse(prefs.anniversaryDate) }.getOrDefault(java.time.LocalDate.now())
                    com.example.data.RelationshipTimeManager.relationshipStartDate = anniv
                    com.example.data.SpecialCalendarManager.boyName = prefs.boyfriendName
                    com.example.data.SpecialCalendarManager.girlName = prefs.girlfriendName
                    com.example.data.SpecialCalendarManager.boyBirthday = runCatching { java.time.LocalDate.parse(prefs.boyfriendBirthday) }.getOrNull()
                    com.example.data.SpecialCalendarManager.girlBirthday = runCatching { java.time.LocalDate.parse(prefs.girlfriendBirthday) }.getOrNull()
                },
                onReplayScene = {
                    engine.loadScene(engine.currentScene)
                    engine.triggerWatchScene()
                },
                onOpenScenePicker = {
                    showSettings = false
                    showScenePicker = true
                },
                onOpenMemories = {
                    memoriesList = prefs.getMemories()
                    showSettings = false
                    showMemories = true
                },
                onOpenLoveNotes = {
                    notesList = prefs.getLoveNotes()
                    showSettings = false
                    showLoveNotes = true
                },
                onOpenPolaroids = {
                    showSettings = false
                    showPolaroidGallery = true
                },
                onOpenDreamJournal = {
                    showSettings = false
                    showDreamJournal = true
                },
                onOpenWardrobe = {
                    showSettings = false
                    showWardrobe = true
                },
                onJumpToScene = { sc ->
                    showSettings = false
                    engine.loadScene(sc)
                    prefs.addRecentScene(sc.name)
                    engine.triggerWatchScene()
                }
            )
        }

        if (showDreamJournal) {
            DreamJournalDialog(
                prefs = prefs,
                onDismiss = { showDreamJournal = false },
                onVisualizeDream = { theme, text ->
                    showDreamJournal = false
                    engine.activateDream(theme, text)
                }
            )
        }

        if (showSecretKeepsake) {
            SecretKeepsakeDialog(
                onDismiss = { showSecretKeepsake = false },
                prefs = prefs
            )
        }

        if (showOnboarding) {
            OnboardingDialog(
                initialBoyName = prefs.boyfriendName,
                initialGirlName = prefs.girlfriendName,
                initialAnniversaryDate = runCatching { java.time.LocalDate.parse(prefs.anniversaryDate) }.getOrDefault(java.time.LocalDate.now()),
                initialSecretCode = prefs.secretCode,
                initialSecretNote = prefs.secretCodeBody,
                onDismiss = {
                    prefs.isOnboardingCompleted = true
                    showOnboarding = false
                },
                onComplete = { bName, gName, annivDate, code, note ->
                    prefs.boyfriendName = bName
                    prefs.girlfriendName = gName
                    prefs.anniversaryDate = annivDate.toString()
                    if (code.isNotEmpty()) prefs.secretCode = code
                    if (note.isNotEmpty()) prefs.secretCodeBody = note
                    prefs.isOnboardingCompleted = true

                    engine.updateNames(bName, gName)
                    com.example.data.RelationshipTimeManager.relationshipStartDate = annivDate
                    com.example.data.SpecialCalendarManager.boyName = bName
                    com.example.data.SpecialCalendarManager.girlName = gName

                    showOnboarding = false
                }
            )
        }

        if (showMusicBox) {
            MusicPlayerDialog(
                audio = audio,
                engine = engine,
                onEnableAudio = {
                    isSoundOn = true
                    prefs.soundEnabled = true
                    audio.isEnabled = true
                },
                onDismiss = { showMusicBox = false }
            )
        }

        if (showSpecialCalendar) {
            SpecialCalendarDialog(
                onDismiss = { showSpecialCalendar = false },
                audio = audio,
                boyfriendName = prefs.boyfriendName,
                girlfriendName = prefs.girlfriendName
            )
        }

        if (showWardrobe) {
            WardrobeDialog(
                currentGirlOutfitIndex = girlOutfitIndex,
                currentGirlAccessoryIndex = girlAccessoryIndex,
                currentBoyOutfitIndex = boyOutfitIndex,
                currentBoyAccessoryIndex = boyAccessoryIndex,
                girlfriendName = prefs.girlfriendName,
                boyfriendName = prefs.boyfriendName,
                onSelectGirlOutfit = { index ->
                    girlOutfitIndex = index
                    engine.selectGirlDress(index)
                    prefs.girlOutfitIndex = index
                },
                onSelectGirlAccessory = { index ->
                    girlAccessoryIndex = index
                    engine.selectGirlAccessory(index)
                    prefs.girlAccessoryIndex = index
                },
                onSelectBoyOutfit = { index ->
                    boyOutfitIndex = index
                    engine.selectBoyOutfit(index)
                    prefs.boyOutfitIndex = index
                },
                onSelectBoyAccessory = { index ->
                    boyAccessoryIndex = index
                    engine.selectBoyAccessory(index)
                    prefs.boyAccessoryIndex = index
                },
                onDismiss = { showWardrobe = false }
            )
        }

        // ── Polaroid Gallery Dialog ────────────────────────────────────────────
        if (showPolaroidGallery) {
            PolaroidGalleryDialog(
                polaroidManager = polaroidManager,
                audio = audio,
                onDismiss = { showPolaroidGallery = false }
            )
        }

        // ── Polaroid Capture Overlay (full-screen, always on top) ─────────────
        val capturedBmp = polaroidCaptureBitmap
        val capturedMem = polaroidCaptureMemory
        if (showPolaroidOverlay && capturedBmp != null && capturedMem != null) {
            PolaroidCaptureOverlay(
                bitmap = capturedBmp,
                memory = capturedMem,
                polaroidManager = polaroidManager,
                audio = audio,
                onDismiss = {
                    showPolaroidOverlay = false
                    polaroidCaptureBitmap = null
                    polaroidCaptureMemory = null
                },
                onOpenGallery = {
                    showPolaroidOverlay = false
                    polaroidCaptureBitmap = null
                    polaroidCaptureMemory = null
                    showPolaroidGallery = true
                }
            )
        }
    }
}


@Composable
private fun ContrastIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    clearFactor: Float = 0f,
    isDark: Boolean = false
) {
    if (clearFactor > 0.15f) {
        val haloColor = if (isDark) {
            Color.Black.copy(alpha = (0.75f * clearFactor).coerceIn(0.2f, 0.85f))
        } else {
            Color.White.copy(alpha = (0.75f * clearFactor).coerceIn(0.2f, 0.85f))
        }
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = haloColor,
                modifier = modifier.offset(x = (0.8).dp, y = (0.8).dp)
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
    } else {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
        )
    }
}
