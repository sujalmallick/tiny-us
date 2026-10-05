package com.example.ui

import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.engine.WorldViewport
import com.example.ui.theme.PixelCornerShape
import com.example.ui.theme.PixelCircleShape
import com.example.ui.theme.PixelIcons
import com.example.data.PersonalProfile

import com.example.engine.AvatarLook
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.example.scene.EnvironmentType
import com.example.engine.RoomTheme
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepRose
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyType
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
    targetBirdSurface: String? = null,
    /** Pins the weather (a WeatherType name) instead of letting the season drift; for store screenshots. */
    targetWeather: String? = null,
    /**
     * An engine already set up and run forward, shown instead of a new one. Store screenshots use
     * it: stepping the engine directly is far faster than letting the whole screen recompose for
     * every frame in a JVM test.
     */
    previewEngine: SceneEngine? = null
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
    var showRoomCustomizer by remember { mutableStateOf(false) }
    var showAvatarCustomizer by remember { mutableStateOf(false) }
    var showOurStory by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(!prefs.isOnboardingCompleted) }
    // Couples from older builds may still be "Him" and "Her": ask once, kindly, instead of renaming.
    var showNamePrompt by remember {
        mutableStateOf(
            PersonalProfile.shouldOfferNamePrompt(
                prefs.boyfriendName, prefs.girlfriendName, prefs.isOnboardingCompleted, prefs.namePromptAnswered
            )
        )
    }

    var showDateAdventures by remember { mutableStateOf(false) }
    var showDailyMomentPrompt by remember { mutableStateOf(false) }
    var showMiniGames by remember { mutableStateOf(false) }
    var showSharedMood by remember { mutableStateOf(false) }
    var showLongDistance by remember { mutableStateOf(false) }

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
    val capsuleShape = remember { PixelCornerShape(24.dp) }
    val heartButtonShape = remember { PixelCircleShape }
    val glassyCircleShape = remember { PixelCircleShape }

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
        previewEngine ?: SceneEngine(
            audio = audio,
            onOpenLoveNotes = { showLoveNotes = true },
            onOpenMemories = { showMemories = true },
            onOpenCalendar = { showSpecialCalendar = true },
            onOpenWardrobe = { showWardrobe = true },
            onOpenDreamJournal = { showDreamJournal = true }
        ).apply {
            updateNames(prefs.boyfriendName, prefs.girlfriendName)
            updateAtmosphereMode(prefs.atmosphereMode)
            setRoomTheme(RoomTheme.values().firstOrNull { it.name == prefs.roomThemeId } ?: RoomTheme.WARM_AUTUMN_COTTAGE, announce = false)
            gardenStage = prefs.gardenStage
            girl.outfitIndex = prefs.girlOutfitIndex
            girl.accessoryIndex = prefs.girlAccessoryIndex
            boy.outfitIndex = prefs.boyOutfitIndex
            boy.accessoryIndex = prefs.boyAccessoryIndex
            boy.look = AvatarLook.of(prefs.getAvatarAppearance(isSlotB = false))
            girl.look = AvatarLook.of(prefs.getAvatarAppearance(isSlotB = true))
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
        com.example.engine.SpecialDays.refresh()
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

    LaunchedEffect(targetScene, targetToken, targetAtmosphere, targetBirdSurface, targetWeather) {
        com.example.scene.WeatherType.values().firstOrNull { it.name.equals(targetWeather, ignoreCase = true) }?.let {
            engine.weatherDriftEnabled = false
            engine.weather = it
        }
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
                // The engine works in world units (see WorldCamera), not screen pixels.
                val camera = com.example.engine.WorldCamera.forScreen(screenWidthPx, screenHeightPx, engine.currentScene)
                val p = WorldViewport.pixelScale(camera.worldW)
                val (destX, destY) = engine.birdSystem.getSurfaceCoordinates(surf, camera.worldW, camera.worldH, p)
                val bird = engine.forceSpawnBirdForTest(surface = surf, cw = camera.worldW, ch = camera.worldH, p = p)
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
        val bloomsBefore = com.example.data.GardenGrowth.bloomsFor(prefs.uniqueDaysOpened).size
        val isNewDay = prefs.markAppOpenedToday()
        engine.gardenStage = prefs.gardenStage
        engine.gardenBlooms = com.example.data.GardenGrowth.bloomsFor(prefs.uniqueDaysOpened)
        engine.homeEvolutionState = prefs.getHomeEvolutionState(engine.weather)

        if (isNewDay) {
            // Gentle garden news: a warm welcome after time away, then any new bloom. Never a loss.
            val welcome = com.example.data.GardenGrowth.welcomeBackMessage(prefs.consumeDaysAway(), prefs.catName)
            val newBloom = engine.gardenBlooms.takeIf { it.size > bloomsBefore }?.lastOrNull()
            if (welcome != null || newBloom != null) delay(3500)
            if (welcome != null) {
                engine.showMessage(welcome, duration = 4.5f)
                if (newBloom != null) delay(5000)
            }
            if (newBloom != null) {
                val text = if (newBloom.isGolden) context.getString(R.string.ui_golden_bloom)
                else context.getString(R.string.ui_new_bloom, newBloom.plant.name)
                engine.showMessage(text, duration = 4.5f)
            }
            // Special days (plan 06, G2): the couple greets the day once, on its first open.
            com.example.engine.SpecialDays.today()?.let { day ->
                delay(if (welcome != null || newBloom != null) 5000 else 3500)
                val (boyLine, girlLine) = specialDayLines(context, day, prefs.boyfriendName, prefs.girlfriendName)
                engine.greetSpecialDay(boyLine, girlLine)
            }
        }
    }

    // Keep the home-screen widget on the scene and weather the couple is in (plan 06, H2).
    LaunchedEffect(engine.currentScene, engine.weather) {
        if (previewEngine == null && com.example.widget.WidgetState.save(context, engine.currentScene, engine.weather)) {
            com.example.widget.TinyUsWidgetProvider.updateAllWidgets(context)
        }
    }

    // Subtle interaction hint fade (not on store screenshots, which show the app mid-use)
    var showHint by remember { mutableStateOf(previewEngine == null) }
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
            modifier = Modifier.fillMaxSize(),
            onOpenDateAdventures = { showDateAdventures = true },
            onOpenDailyMoment = { showDailyMomentPrompt = true },
            onOpenMiniGames = { showMiniGames = true },
            onOpenLongDistance = if (com.example.FeatureFlags.PARTNER_SYNC) ({ showLongDistance = true }) else null
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
            // The room customizer adds a sixth button; then the pill shows just the heart and the names
            // (no "Tiny Us", no separator dot), so the names never get cut off, even on small phones or
            // in longer languages.
            val crowded = engine.currentScene.environment == EnvironmentType.LIVING_ROOM || engine.currentScene.environment == EnvironmentType.COZY_LOFT

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
                            imageVector = PixelIcons.Favorite,
                            contentDescription = null,
                            tint = activeHeartTint,
                            modifier = Modifier.size(heartSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                        if (!crowded) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.ui_tiny_us),
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
                    }

                    if (crowded) {
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        // Small drawn separator dot (was a bullet glyph sized by dotFontSize)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = if (isCompact) 4.dp else 5.dp)
                                .size(3.dp)
                                .background(dotColor, PixelCircleShape)
                        )
                    }

                    // Secretive interactive tap on couple names
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(PixelCornerShape(8.dp))
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

        val haptic = LocalHapticFeedback.current
        val renderButtonsRow: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { screenWidth ->
            val buttonTouchSize = when {
                screenWidth < 360.dp -> 38.dp
                screenWidth < 410.dp -> 40.dp
                else -> 42.dp
            }
            val buttonVisualSize = when {
                screenWidth < 360.dp -> 32.dp
                screenWidth < 410.dp -> 34.dp
                else -> 36.dp
            }
            val iconSize = 16.dp
            // With six buttons (room customizer visible) the touch-target margins alone keep the
            // glass circles apart, which frees width for the couple's names in the title pill.
            val crowded = engine.currentScene.environment == EnvironmentType.LIVING_ROOM || engine.currentScene.environment == EnvironmentType.COZY_LOFT
            val spacing = when {
                crowded -> 0.dp
                screenWidth < 360.dp -> 2.dp
                screenWidth < 410.dp -> 2.5.dp
                else -> 3.5.dp
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic Weather Cycle Button
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(PixelCircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isSoundOn) audio.playWindChime()
                                engine.cycleWeather()
                            }
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
                            imageVector = PixelIcons.AutoAwesome,
                            contentDescription = stringResource(R.string.ui_weather_desc, engine.weather.displayName),
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
                        .clip(PixelCircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val target = !isSoundOn
                                isSoundOn = target
                                prefs.soundEnabled = target
                                audio.isEnabled = target
                                if (target) audio.playHeartChime()
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
                            imageVector = if (isSoundOn) PixelIcons.VolumeUp else PixelIcons.VolumeOff,
                            contentDescription = stringResource(R.string.ui_toggle_audio),
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
                        .clip(PixelCircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                            imageVector = PixelIcons.Headphones,
                            contentDescription = stringResource(R.string.ui_music_box_earphones),
                            tint = if (engine.earphonesActive) activeHeartTint else neutralIconTint,
                            modifier = Modifier.size(iconSize),
                            clearFactor = clearFactor,
                            isDark = isDark
                        )
                    }
                }

                if (engine.currentScene.environment == EnvironmentType.LIVING_ROOM || engine.currentScene.environment == EnvironmentType.COZY_LOFT) {
                    Box(
                        modifier = Modifier.size(buttonTouchSize).clip(PixelCircleShape).clickable(role = Role.Button) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showRoomCustomizer = true
                        }.testTag("room_customizer_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(buttonVisualSize).shadow(buttonElevation, glassyCircleShape, ambientColor = glassShadowAmbient, spotColor = glassShadowSpot)
                                .clip(glassyCircleShape).background(buttonFillBrush).border(buttonBorderStroke, glassyCircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            ContrastIcon(PixelIcons.Palette, stringResource(R.string.ui_customize_room), activeHeartTint, Modifier.size(iconSize), clearFactor, isDark)
                        }
                    }
                }

                // Random Scene Surprise Button
                Box(
                    modifier = Modifier
                        .size(buttonTouchSize)
                        .clip(PixelCircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isSoundOn) audio.playWoodKnock()
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
                            imageVector = PixelIcons.Shuffle,
                            contentDescription = stringResource(R.string.ui_random_scene),
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
                        .clip(PixelCircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSettings = true
                            }
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
                            imageVector = PixelIcons.Settings,
                            contentDescription = stringResource(R.string.ui_settings),
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
                shape = PixelCornerShape(16.dp),
                color = TinyColors.Scrim.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_tap_characters_cottage_tree_sky_or_mailb),
                    style = TinyType.Caption.copy(color = Color.White),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                    imageVector = PixelIcons.Favorite,
                    contentDescription = stringResource(R.string.ui_capture_tiny_moment_long_press_for_galle),
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
        com.example.engine.SpecialDays.refresh()
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
                onOpenAvatarCustomizer = {
                    showSettings = false
                    showAvatarCustomizer = true
                },
                onOpenOurStory = {
                    showSettings = false
                    showOurStory = true
                },
                onOpenDateAdventures = {
                    showSettings = false
                    showDateAdventures = true
                },
                onOpenDailyMoment = {
                    showSettings = false
                    showDailyMomentPrompt = true
                },
                onOpenMiniGames = {
                    showSettings = false
                    showMiniGames = true
                },
                onOpenSharedMood = {
                    showSettings = false
                    showSharedMood = true
                },
                onOpenLongDistance = {
                    showSettings = false
                    showLongDistance = true
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

        if (showNamePrompt && !showOnboarding) {
            NamePromptDialog(
                onSetNames = {
                    prefs.namePromptAnswered = true
                    showNamePrompt = false
                    showOnboarding = true
                },
                onKeep = {
                    prefs.namePromptAnswered = true
                    showNamePrompt = false
                }
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
                    com.example.engine.SpecialDays.refresh()
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

        if (showOurStory) {
            OurStoryDialog(onDismiss = { showOurStory = false })
        }

        if (showAvatarCustomizer) {
            AvatarCustomizerDialog(
                nameA = prefs.boyfriendName,
                nameB = prefs.girlfriendName,
                outfitIndexA = boyOutfitIndex,
                outfitIndexB = girlOutfitIndex,
                initialA = engine.boy.look.appearance,
                initialB = engine.girl.look.appearance,
                onChange = { isSlotB, appearance ->
                    prefs.setAvatarAppearance(isSlotB, appearance)
                    val look = AvatarLook.of(appearance)
                    if (isSlotB) engine.girl.look = look else engine.boy.look = look
                },
                onDismiss = { showAvatarCustomizer = false }
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
                girlWearsDress = engine.girl.look.wearsDress,
                boyWearsDress = engine.boy.look.wearsDress,
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

        if (showRoomCustomizer) {
            RoomCustomizerDialog(
                selectedTheme = engine.roomTheme,
                isLoft = engine.currentScene.environment == EnvironmentType.COZY_LOFT,
                onSelectTheme = { theme ->
                    engine.setRoomTheme(theme)
                    prefs.roomThemeId = theme.name
                },
                onDismiss = { showRoomCustomizer = false }
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

        // ── Public Feature Dialogs ──────────────────────────────────────────
        if (showDateAdventures) {
            DateAdventuresDialog(
                prefs = prefs,
                onDismiss = { showDateAdventures = false }
            )
        }

        if (showDailyMomentPrompt) {
            DailyMomentPromptDialog(
                prefs = prefs,
                onDismiss = { showDailyMomentPrompt = false }
            )
        }

        if (showMiniGames) {
            TwoPersonMiniGameDialog(
                prefs = prefs,
                onDismiss = { showMiniGames = false }
            )
        }

        if (showSharedMood) {
            SharedMoodDialog(
                prefs = prefs,
                onDismiss = { showSharedMood = false }
            )
        }

        if (showLongDistance) {
            LongDistanceSheet(
                prefs = prefs,
                onDismiss = { showLongDistance = false }
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

/** The couple's two lines for a special day: (boy, girl). */
internal fun specialDayLines(
    context: android.content.Context,
    day: com.example.engine.SpecialDay,
    boyName: String,
    girlName: String
): Pair<String, String> {
    val r = context.resources
    return when (day) {
        com.example.engine.SpecialDay.BOY_BIRTHDAY ->
            r.getString(R.string.special_boy_birthday_boy) to r.getString(R.string.special_boy_birthday_girl, boyName)
        com.example.engine.SpecialDay.GIRL_BIRTHDAY ->
            r.getString(R.string.special_girl_birthday_boy, girlName) to r.getString(R.string.special_girl_birthday_girl)
        com.example.engine.SpecialDay.ANNIVERSARY -> {
            val years = com.example.engine.SpecialDays.yearsTogether(
                java.time.LocalDate.now(), com.example.data.RelationshipTimeManager.relationshipStartDate
            ).coerceAtLeast(1)
            r.getQuantityString(R.plurals.special_anniversary_boy, years, years) to r.getString(R.string.special_anniversary_girl)
        }
        com.example.engine.SpecialDay.NEW_YEAR -> r.getString(R.string.special_new_year_boy) to r.getString(R.string.special_new_year_girl)
        com.example.engine.SpecialDay.VALENTINES -> r.getString(R.string.special_valentines_boy) to r.getString(R.string.special_valentines_girl)
        com.example.engine.SpecialDay.HOLI -> r.getString(R.string.special_holi_boy) to r.getString(R.string.special_holi_girl)
        com.example.engine.SpecialDay.DIWALI -> r.getString(R.string.special_diwali_boy) to r.getString(R.string.special_diwali_girl)
        com.example.engine.SpecialDay.CHRISTMAS -> r.getString(R.string.special_christmas_boy) to r.getString(R.string.special_christmas_girl)
    }
}
