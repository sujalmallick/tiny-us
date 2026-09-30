package com.example.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.engine.AmbientAudio
import com.example.engine.MusicBoxState
import com.example.engine.AntiRepeatRandomPicker
import com.example.engine.ShuffledDice
import com.example.engine.CharacterEmotion
import com.example.engine.CharacterPose
import com.example.engine.CharacterState
import com.example.engine.Direction
import com.example.engine.EmoteType
import com.example.engine.ParticleSystem
import com.example.engine.PixelCharacter
import com.example.engine.CharacterMotionTween
import com.example.engine.TimeOfDayPhase
import com.example.engine.BirdEntity
import com.example.engine.BirdSpecies
import com.example.engine.BirdSystem
import com.example.engine.PerchSurface
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val OUTDOOR_ENVIRONMENTS = setOf(
    EnvironmentType.MEADOW,
    EnvironmentType.TREE_HILL,
    EnvironmentType.PATH_NIGHT,
    EnvironmentType.TWILIGHT,
    EnvironmentType.EVENING_ROAD,
    EnvironmentType.MOMO_STALL
)

class SceneEngine(
    val audio: AmbientAudio,
    private val onOpenLoveNotes: () -> Unit,
    private val onOpenMemories: () -> Unit,
    private val onOpenCalendar: () -> Unit = {},
    private val onOpenWardrobe: () -> Unit = {},
    private val onOpenDreamJournal: () -> Unit = {}
) {
    companion object {
        const val MIN_CAT_WALK_THRESHOLD_PIXELS = 48f
    }

    val isCurrentSceneOutdoor: Boolean
        get() = currentScene.environment in OUTDOOR_ENVIRONMENTS

    var atmosphereMode: String by mutableStateOf("AUTO")
        private set
    var timeOfDayPhase: TimeOfDayPhase by mutableStateOf(TimeOfDayPhase.resolve("AUTO"))
        private set
    private var hourCheckTimer: Float = 0f
    var currentScene: SceneType = SceneType.FLOWER
        private set

    var boy: PixelCharacter = PixelCharacter(
        isGirl = false,
        name = com.example.data.ProfileManager.getProfile().boyName,
        worldX = 0.35f,
        worldY = 0.68f
    )

    var girl: PixelCharacter = PixelCharacter(
        isGirl = true,
        name = com.example.data.ProfileManager.getProfile().girlName,
        worldX = 0.65f,
        worldY = 0.68f
    )

    val particles = ParticleSystem()
    val birdSystem = BirdSystem()

    var sceneTime: Float = 0f
        internal set

    var sceneMessage: String? by mutableStateOf(null)
        private set

    var messageAlpha: Float by mutableFloatStateOf(0f)
        private set

    var messageTimer: Float = 0f
        private set

    var ambientDimming: Float = 0f
        private set

    var lampLit: Boolean = true
    var recordSpinning: Boolean by mutableStateOf(false)
    var loftBookReading: Boolean by mutableStateOf(false)
    var treeTapReactionCount: Int = 0
    var catSleeping: Boolean = true
    var catState: CatState by mutableStateOf(CatState.SLEEPING)
    var catFacingLeft: Boolean by mutableStateOf(false)
    var catTargetX: Float = 0.82f
    var catTargetY: Float = 0.69f
    var catRoamSpeed: Float = 0.052f
    var weather: WeatherType by mutableStateOf(WeatherType.SUNNY)
    var idleAffectionTimer: Float = 0f
    var catWorldX: Float = 0.82f
    var catWorldY: Float = 0.69f

    // Feature 1: Mochi Matchmaker state
    var mochiMatchmakerActive: Boolean by mutableStateOf(false)
        internal set
    var mochiMatchmakerCooldown: Float = 22f
    var mochiMatchmakerVariant: Int = 0
    var mochiMatchmakerStep: Int = 0
    var mochiMatchmakerStepTimer: Float = 0f
    var mochiNudgeActiveTimer: Float = 0f
    val mochiMatchmakerPicker = AntiRepeatRandomPicker(listOf(0, 1, 2))

    // Feature 2: Tree Bark Growth state
    var treeMossDebugYearOverride: Int? = null
    var treeMossGrowthStage: Int by mutableIntStateOf(0)
        internal set
    var girlfriendInitial: Char = 'T'

    // Feature 3: Shared Dream Journal — isolated temporary Dream Mode
    var dreamState: com.example.data.DreamState? by mutableStateOf(null)
        private set

    val isDreamMode: Boolean get() = dreamState != null

    val activeDreamTheme: String? get() = dreamState?.theme
    val activeDreamText: String? get() = dreamState?.text
    val dreamOverlayAlpha: Float get() = dreamState?.alpha ?: 0f

    // Feature 5: Living Room floor lamp state (separate from loft lampLit)
    var livingRoomLampLit: Boolean by mutableStateOf(true)

    // Feature 6: Couch Snooze visual time cycle — scene-scoped, never touches global isNight/isSunset
    var couchVisualPhase: CouchPhase by mutableStateOf(CouchPhase.NIGHT)
    var couchPhaseTimer: Float = 0f
    var couchWakeTimer: Float = 0f

    // Feature 3: Kitchen appliance animation timers
    var fridgeDoorOpenTimer: Float by mutableFloatStateOf(0f)
    var cabinetOpenTimer: Float by mutableFloatStateOf(0f)
    var sinkRunningTimer: Float by mutableFloatStateOf(0f)

    // Kitchen tap-interaction animation timers
    var clockSpinTimer: Float by mutableFloatStateOf(0f)       // drives clock-hand spin overlay (2.4 s)
    var binLidTimer: Float by mutableFloatStateOf(0f)          // drives dustbin lid pop (1.2 s)
    var crateShakeTimer: Float by mutableFloatStateOf(0f)      // drives produce crate shake (0.7 s)
    var planterAnimTimer: Float by mutableFloatStateOf(0f)     // drives water-drop + leaf emerge (2.0 s)
    var stoolWobbleTimer: Float by mutableFloatStateOf(0f)     // drives step-stool wobble (0.8 s)

    // Living Room tap-interaction animation timers
    var tableCandleTimer: Float by mutableFloatStateOf(0f)     // drives candle flicker & warm glow (1.5 s)
    var poufBounceTimer: Float by mutableFloatStateOf(0f)      // drives knitted pouf squish-bounce (0.8 s)
    var cardboardBoxTimer: Float by mutableFloatStateOf(0f)    // drives Mochi peeking out of box (1.3 s)
    var basketYarnTimer: Float by mutableFloatStateOf(0f)      // drives yarn ball rolling & basket wobble (1.4 s)
    var magazineRackTimer: Float by mutableFloatStateOf(0f)    // drives magazine/vinyl card flip & spin (1.3 s)

    // Lantern Stroll tap-interaction animation timers
    var pagodaGlowTimer: Float by mutableFloatStateOf(0f)      // drives pagoda lantern glow & floating motes (1.8 s)
    var lavenderSwayTimer: Float by mutableFloatStateOf(0f)    // drives lavender sway & scent particles (1.5 s)
    var mushroomBounceTimer: Float by mutableFloatStateOf(0f)  // drives bioluminescent mushroom cap bounce (1.4 s)
    var constellationConnectTimer: Float by mutableFloatStateOf(0f) // drives glowing constellation star connect lines (2.4 s)
    var activeConstellationIndex: Int by mutableIntStateOf(0)  // 0: none, 1: Two Hearts, 2: Teapot, 3: Starlight Trail

    // Momo Stall tap-interaction animation timers
    var momoSignFlickerTimer: Float by mutableFloatStateOf(0f) // drives neon sign color cycling and glow pulse (1.4 s)
    var momoSteamerTimer: Float by mutableFloatStateOf(0f)     // drives bamboo lid rising, steam jet, and momo jumping (1.5 s)
    var chutneySpiceTimer: Float by mutableFloatStateOf(0f)    // drives spice puffs and bowl wobble (1.2 s)
    var chalkboardTimer: Float by mutableFloatStateOf(0f)      // drives chalk heart appearing and board shake (1.6 s)
    var bambooCrateTimer: Float by mutableFloatStateOf(0f)     // drives top crate bumping sideways (1.0 s)
    var milkSaucerTimer: Float by mutableFloatStateOf(0f)      // drives milk ripples and Mochi trot (1.8 s)
    var streetDiningTableTimer: Float by mutableFloatStateOf(0f) // drives steam from plate and loving glances (1.6 s)

    // Evening Ride tap-interaction animation timers
    var scooterHonkTimer: Float by mutableFloatStateOf(0f)     // drives scooter hop + headlight flash + exhaust puff (0.8 s)
    var templeGlowTimer: Float by mutableFloatStateOf(0f)      // drives golden blessing aura & falling sparkles from spire (2.0 s)

    // Midnight Loft tap-interaction animation timers
    var loftTableTimer: Float by mutableFloatStateOf(0f)       // drives tea cup tilt + steam + crumb drop (1.4 s)
    var loftBookNookTimer: Float by mutableFloatStateOf(0f)    // drives book opening and fluttering dust motes (1.8 s)
    var loftFairyLightsTimer: Float by mutableFloatStateOf(0f) // drives string lights cascading twinkle (1.6 s)
    var loftWindowTimer: Float by mutableFloatStateOf(0f)      // drives window panel sliding open with breeze/leaves (2.0 s)

    var lightningFlashAlpha: Float by mutableFloatStateOf(0f)
    var lightningTimer: Float = 0f
    private var nextLightningTime: Float = 14f
    private var isRainAmbientPlaying: Boolean = false

    var gardenStage: Int by mutableIntStateOf(0)
    var flowerWiggleTimer: Float by mutableFloatStateOf(0f)
    var wipeAlpha: Float by mutableFloatStateOf(0f)

    private var petBehaviorTimer: Float = 0f
    private var nextPetBehaviorInterval: Float = 6f

    private val boyTapPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3, 4))
    private val girlTapPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3, 4))
    private val hugPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))
    private val petTapPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3, 4))
    private val flowerDialoguePicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))
    private val thoughtPicker = AntiRepeatRandomPicker<String>(emptyList())
    private val cookingAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3, 4, 5))

    // Feature 3: Ambient Weather Drift configuration
    var weatherDriftEnabled: Boolean = true
    var weatherDriftInterval: Float = 360f // 6 minutes (configurable)
    private var weatherDriftTimer: Float = 0f

    // Feature 1: Weather-reactive and generic dialogue pools
    private val genericThoughts = listOf(
        "Just happy to be right here beside you.",
        "Every moment feels a little sweeter with you.",
        "Our quiet little moments together are my favorite.",
        "Forever and always, my favorite person."
    )

    private val weatherThoughts = mapOf(
        WeatherType.RAIN to listOf(
            "Listening to the rain fall while staying warm with you.",
            "Under one umbrella is my favorite place to be.",
            "The sound of rain is so peaceful when we are together."
        ),
        WeatherType.SNOW to listOf(
            "Cold outside, but so warm right here beside you.",
            "Watching the gentle snow drift past our window.",
            "Bundled up close, sharing all the winter warmth."
        ),
        WeatherType.SAKURA to listOf(
            "Pink petals floating on the breeze with you.",
            "Every spring blossom feels like a love note.",
            "Walking under the blooming cherry trees together."
        ),
        WeatherType.AUTUMN to listOf(
            "Crisp golden air and cozy sweater season.",
            "Watching amber leaves tumble in the wind together.",
            "Warm tea and golden autumn afternoons with you."
        ),
        WeatherType.SUNNY to listOf(
            "Warm sunny skies and you make every day bright.",
            "Soaking in the golden sunshine side by side.",
            "A gentle breeze on a lovely sunny day."
        )
    )

    fun getEligibleThoughts(): List<String> {
        val weatherList = weatherThoughts[weather] ?: emptyList()
        return genericThoughts + weatherList
    }

    fun pickSpontaneousThought(): String {
        return thoughtPicker.pickFrom(getEligibleThoughts())
    }

    var boySpeechText: String? by mutableStateOf(null)
        internal set
    var boySpeechTimer: Float = 0f
        internal set

    var girlSpeechText: String? by mutableStateOf(null)
        internal set
    var girlSpeechTimer: Float = 0f
        internal set
    internal var rideBoySpoke: Boolean = false
    internal var rideGirlSpoke: Boolean = false

    private var _earphonesActive by mutableStateOf(false)
    var earphonesActive: Boolean
        get() = _earphonesActive
        set(value) {
            _earphonesActive = value
            boy.wearsEarphone = value
            girl.wearsEarphone = value
        }

    fun setEarphones(value: Boolean) {
        earphonesActive = value
    }

    var isWatchSceneActive: Boolean by mutableStateOf(false)
        private set
    var watchSceneTimer: Float = 0f
        private set
    private var watchSceneScene: SceneType? = null

    private var lastFootstepStep: Int = -1

    private fun triggerWalkFootstep(stepIndex: Int) {
        if (stepIndex != lastFootstepStep) {
            lastFootstepStep = stepIndex
            if (stepIndex % 2 == 0) {
                audio.playFootstep()
            }
        }
    }

    // Autonomous spontaneous interaction timer (frequent cute movements!)
    private var autonomousTimer: Float = 0f
    private var nextAutonomousInterval = 5.5f

    /** Smooth sitting/snuggle/kiss offset interpolation progress (0f = separated, 1f = fully cuddled) */
    var cuddleProgress: Float by mutableFloatStateOf(0f)
        internal set

    // Living Room autonomous return timer (walks back to couch and sits)
    var livingRoomReturnTimer: Float = 0f
        internal set
    internal var livingRoomReturnTargetX: Float = 0.44f
    internal var livingRoomReturnChar: PixelCharacter? = null

    // Scene-specific autonomous behavior pickers
    val livingRoomAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3, 4))
    val meadowAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))
    val treeAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))
    val walkAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))
    val momoAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))
    val lookingAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2, 3))

    // History of recently shown scenes to prevent repeats
    private val recentSceneHistory = mutableListOf<SceneType>()
    private val allScenes = SceneType.values().toList()

    init {
        loadScene(SceneType.FLOWER)
        audio.setIndoor(!isCurrentSceneOutdoor, smooth = false)
        if (audio.isEnabled) {
            audio.playWeatherBgm(weather, isAutomaticDrift = false)
        }
    }

    fun triggerWatchSceneKiss(cw: Float = 1000f, ch: Float = 1000f) {
        watchSceneScene = currentScene
        isWatchSceneActive = true
        watchSceneTimer = 0f
        lastFootstepStep = -1
        boy.reactionTimer = 8.0f
        girl.reactionTimer = 8.0f
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        audio.playHeartChime()
    }

    /** Public entry point for the Watch Scene button — routes to the current scene's unique cinematic. */
    fun triggerWatchScene() {
        triggerWatchSceneKiss()
    }

    /** Activate a dream overlay visual state from keyword theme in isolated Dream Mode. */
    fun activateDream(theme: String, text: String) {
        dreamState = com.example.data.DreamState(theme = theme, text = text, alpha = 1.0f)
        audio.playHeartChime()
    }

    /** Dismiss the active dream overlay and return to normal scene. */
    fun clearDream() {
        dreamState = null
    }

    fun loadScene(type: SceneType) {
        currentScene = type
        audio.setIndoor(!isCurrentSceneOutdoor, smooth = true)
        sceneTime = 0f
        sceneMessage = null
        messageAlpha = 0f
        messageTimer = 0f
        ambientDimming = 0f
        autonomousTimer = 0f
        boySpeechText = null
        boySpeechTimer = 0f
        girlSpeechText = null
        girlSpeechTimer = 0f
        rideBoySpoke = false
        rideGirlSpoke = false
        isWatchSceneActive = false
        watchSceneTimer = 0f
        lastFootstepStep = -1
        wipeAlpha = 1.0f
        particles.particles.clear()
        birdSystem.clear()
        boy.wearsEarphone = earphonesActive
        girl.wearsEarphone = earphonesActive

        // Track history (keep max 3)
        recentSceneHistory.add(type)
        if (recentSceneHistory.size > 3) {
            recentSceneHistory.removeAt(0)
        }

        // Reset appliance timers to ensure clean visual state on scene load
        fridgeDoorOpenTimer = 0f
        cabinetOpenTimer = 0f
        sinkRunningTimer = 0f
        clockSpinTimer = 0f
        binLidTimer = 0f
        crateShakeTimer = 0f
        planterAnimTimer = 0f
        stoolWobbleTimer = 0f
        tableCandleTimer = 0f
        poufBounceTimer = 0f
        cardboardBoxTimer = 0f
        basketYarnTimer = 0f
        magazineRackTimer = 0f
        pagodaGlowTimer = 0f
        lavenderSwayTimer = 0f
        mushroomBounceTimer = 0f
        constellationConnectTimer = 0f
        activeConstellationIndex = 0
        momoSignFlickerTimer = 0f
        momoSteamerTimer = 0f
        chutneySpiceTimer = 0f
        chalkboardTimer = 0f
        bambooCrateTimer = 0f
        milkSaucerTimer = 0f
        streetDiningTableTimer = 0f
        scooterHonkTimer = 0f
        templeGlowTimer = 0f
        loftTableTimer = 0f
        loftBookNookTimer = 0f
        loftFairyLightsTimer = 0f
        loftWindowTimer = 0f
        livingRoomReturnTimer = 0f
        livingRoomReturnChar = null
        cuddleProgress = 0f

        // Reset Feature 1 (Mochi Matchmaker) & compute Feature 2 (Tree Bark Growth)
        mochiMatchmakerActive = false
        mochiMatchmakerStep = 0
        mochiMatchmakerStepTimer = 0f
        mochiMatchmakerCooldown = 22f + Random.nextFloat() * 8f
        mochiNudgeActiveTimer = 0f
        if (type == SceneType.UNDER_TREE) {
            treeMossGrowthStage = computeTreeMossGrowthStage()
        }

        // Cancel any in-flight motion transitions so scene loads cleanly
        boy.isTransitioningPosition = false
        boy.isTransitioningPose = false
        boy.posTransitionProgress = 1f
        boy.poseTransitionProgress = 1f
        girl.isTransitioningPosition = false
        girl.isTransitioningPose = false
        girl.posTransitionProgress = 1f
        girl.poseTransitionProgress = 1f

        // Reset positions & poses for each scene
        when (type) {
            SceneType.FLOWER -> {
                boy.worldX = 0.43f
                boy.worldY = 0.68f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.HAPPY
                boy.emote = EmoteType.NONE

                girl.worldX = 0.57f
                girl.worldY = 0.68f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.IDLE
                girl.emotion = CharacterEmotion.IDLE
                girl.emote = EmoteType.NONE

                catSleeping = true
                catWorldX = 0.86f
                catWorldY = 0.69f
            }
            SceneType.UNDER_TREE -> {
                boy.worldX = 0.43f
                boy.worldY = 0.69f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.57f
                girl.worldY = 0.69f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.SIT
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = true
                catWorldX = 0.30f
                catWorldY = 0.69f
            }
            SceneType.COOKING -> {
                boy.worldX = 0.18f
                boy.worldY = 0.67f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.PLAYFUL
                boy.emote = EmoteType.NONE

                girl.worldX = 0.58f
                girl.worldY = 0.67f
                girl.direction = Direction.RIGHT
                girl.pose = CharacterPose.COOK
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = false
                catWorldX = 0.28f
                catWorldY = 0.70f
            }
            SceneType.SLEEP -> {
                boy.worldX = 0.43f
                boy.worldY = 0.67f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.SLEEPY
                boy.emote = EmoteType.NONE

                girl.worldX = 0.57f
                girl.worldY = 0.67f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.SIT
                girl.emotion = CharacterEmotion.SLEEPY
                girl.emote = EmoteType.NONE

                catSleeping = true
                catWorldX = 0.72f
                catWorldY = 0.67f
            }
            SceneType.WALK -> {
                boy.worldX = 0.12f
                boy.worldY = 0.68f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.WALK_1
                boy.emotion = CharacterEmotion.HAPPY
                boy.emote = EmoteType.NONE

                girl.worldX = 0.26f
                girl.worldY = 0.68f
                girl.direction = Direction.RIGHT
                girl.pose = CharacterPose.WALK_1
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = false
                catWorldX = 0.05f
                catWorldY = 0.69f
            }
            SceneType.LOOKING -> {
                boy.worldX = 0.44f
                boy.worldY = 0.68f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.56f
                girl.worldY = 0.68f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.IDLE
                girl.emotion = CharacterEmotion.SHY
                girl.emote = EmoteType.NONE

                catSleeping = true
                catWorldX = 0.78f
                catWorldY = 0.69f
            }
            SceneType.MOMO_STALL -> {
                boy.worldX = 0.36f
                boy.worldY = 0.69f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.FEED_MOMO
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.62f
                girl.worldY = 0.69f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.EAT_MOMO
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = false
                catWorldX = 0.16f
                catWorldY = 0.69f
            }
            SceneType.EVENING_RIDE -> {
                boy.worldX = 0.44f
                boy.worldY = 0.70f
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
                boy.pose = CharacterPose.IDLE
                boy.emote = EmoteType.NONE

                girl.worldX = 0.54f
                girl.worldY = 0.70f
                girl.direction = Direction.RIGHT
                girl.emotion = CharacterEmotion.HAPPY
                girl.pose = CharacterPose.IDLE
                girl.emote = EmoteType.NONE

                catSleeping = true
                catWorldX = 0.12f
                catWorldY = 0.70f
            }
            SceneType.COZY_LOFT -> {
                boy.worldX = 0.58f
                boy.worldY = 0.55f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.64f
                girl.worldY = 0.55f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.emotion = CharacterEmotion.LOVING
                girl.emote = EmoteType.NONE

                catSleeping = true
                catState = CatState.SLEEPING
                catWorldX = 0.50f
                catWorldY = 0.55f

                lampLit = true
                recordSpinning = false
                loftBookReading = false
                ambientDimming = 0.08f
            }
        }
        catTargetX = catWorldX
        catTargetY = catWorldY
        catFacingLeft = false
    }

    /**
     * Pick random scene avoiding recently shown ones to ensure variety.
     */
    fun nextRandomScene(lastScene: SceneType? = null): SceneType {
        val candidates = allScenes.filter { it != currentScene && it !in recentSceneHistory && (lastScene == null || it != lastScene) }
        val next = candidates.randomOrNull() ?: allScenes.filter { it != currentScene }.randomOrNull() ?: allScenes.first()
        loadScene(next)
        return next
    }

    fun updateNames(boyName: String, girlName: String) {
        boy.name = boyName
        girl.name = girlName
        girlfriendInitial = girlName.trim().firstOrNull()?.uppercaseChar() ?: 'T'
    }

    fun computeTreeMossGrowthStage(currentDate: java.time.LocalDate = java.time.LocalDate.now()): Int {
        treeMossDebugYearOverride?.let { return it.coerceIn(0, 4) }
        val years = java.time.Period.between(com.example.data.RelationshipTimeManager.relationshipStartDate, currentDate).years
        return years.coerceIn(0, 4)
    }

    fun updateAtmosphereMode(mode: String) {
        atmosphereMode = mode
        timeOfDayPhase = TimeOfDayPhase.resolve(mode)
    }

    fun refreshTimeOfDayPhase() {
        timeOfDayPhase = TimeOfDayPhase.resolve(atmosphereMode)
    }

    fun showMessage(msg: String, duration: Float = 4.0f) {
        sceneMessage = msg
        messageTimer = duration
        messageAlpha = 0f
    }

    fun update(deltaSeconds: Float, canvasWidth: Float, canvasHeight: Float) {
        sceneTime += deltaSeconds
        val pixelScale = (canvasWidth / 115f).coerceIn(3.0f, 5.0f)

        // Per-Scene Cinematic Watch Sequences
        if (isWatchSceneActive) {
            watchSceneTimer += deltaSeconds
            val t = watchSceneTimer
            val cw = canvasWidth
            val ch = canvasHeight
            val pxScale = (cw / 115f).coerceIn(3.0f, 5.0f)

            when (watchSceneScene) {

                // FLOWER: Boy picks flowers, presents bouquet, girl blushes and hugs
                SceneType.FLOWER -> {
                    val endTime = 8.5f
                    when {
                        t < 1.0f -> {
                            // Boy walks left to flower patch
                            val wf = ((t * 8).toInt() % 4)
                            boy.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.direction = Direction.LEFT
                            boy.worldX = (0.45f - t * 0.12f).coerceAtLeast(0.33f)
                            girl.pose = CharacterPose.IDLE
                            girl.direction = Direction.LEFT
                            girl.worldX = 0.64f
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < 2.5f -> {
                            // Boy picks flowers (give_flower pose, crouching)
                            boy.worldX = 0.33f
                            boy.pose = CharacterPose.GIVE_FLOWER
                            boy.emotion = CharacterEmotion.HAPPY
                            boy.direction = Direction.RIGHT
                            girl.pose = CharacterPose.IDLE
                            girl.direction = Direction.LEFT
                            girl.worldX = 0.64f
                            if (t > 1.5f && boy.emote == EmoteType.NONE) {
                                boy.emote = EmoteType.SPARKLE
                                boy.emoteTimer = 1.5f
                            }
                        }
                        t < 4.0f -> {
                            // Boy walks back toward girl
                            val wf = ((t * 8).toInt() % 4)
                            boy.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.direction = Direction.RIGHT
                            boy.worldX = (0.33f + (t - 2.5f) * 0.07f).coerceAtMost(0.44f)
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < 5.8f -> {
                            // Boy presents bouquet — girl surprised
                            boy.worldX = 0.44f
                            boy.pose = CharacterPose.GIVE_FLOWER
                            boy.emotion = CharacterEmotion.SHY
                            girl.worldX = 0.58f
                            if (t > 4.4f && girl.emote == EmoteType.NONE) {
                                girl.pose = CharacterPose.SURPRISED
                                girl.emotion = CharacterEmotion.LOVING
                                girl.emote = EmoteType.HEART
                                girl.emoteTimer = 2.5f
                                audio.playHeartChime()
                                particles.spawnHeart(cw * 0.51f, ch * 0.58f)
                                showMessage("For you, always", duration = 4.0f)
                            }
                        }
                        t < endTime -> {
                            // Warm hug
                            boy.worldX = 0.44f
                            girl.worldX = 0.54f
                            boy.pose = CharacterPose.HUG
                            girl.pose = CharacterPose.HUG
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (kotlin.random.Random.nextFloat() < 0.10f) {
                                particles.spawnHeart(cw * 0.49f + (kotlin.random.Random.nextFloat() - 0.5f) * 30f, ch * 0.55f)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // UNDER_TREE: Sit together, boy points at sky, they fall asleep leaning
                SceneType.UNDER_TREE -> {
                    val endTime = 9.0f
                    when {
                        t < 1.5f -> {
                            // Walk to tree base
                            val wf = ((t * 8).toInt() % 4)
                            boy.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            girl.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.RIGHT
                            boy.worldX = (0.30f + t * 0.08f).coerceAtMost(0.42f)
                            girl.worldX = (0.36f + t * 0.08f).coerceAtMost(0.50f)
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < 3.5f -> {
                            // Sit together under tree
                            boy.worldX = 0.42f
                            girl.worldX = 0.52f
                            boy.pose = CharacterPose.SIT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.LEFT
                            boy.emotion = CharacterEmotion.HAPPY
                            girl.emotion = CharacterEmotion.HAPPY
                        }
                        t < 5.5f -> {
                            // Boy points at constellation
                            boy.worldX = 0.42f
                            girl.worldX = 0.52f
                            boy.pose = CharacterPose.WAVE
                            boy.direction = Direction.RIGHT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            if (t > 4.0f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.SPARKLE
                                girl.emoteTimer = 2.0f
                                girl.emotion = CharacterEmotion.CURIOUS
                                audio.playHeartChime()
                                showMessage("See that star? That is ours", duration = 4.5f)
                                particles.spawnSparkles(cw * 0.47f, ch * 0.35f, 5)
                            }
                        }
                        t < endTime -> {
                            // Lean together and doze
                            boy.worldX = 0.42f
                            girl.worldX = 0.52f
                            boy.pose = CharacterPose.SIT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            boy.emotion = CharacterEmotion.SLEEPY
                            girl.emotion = CharacterEmotion.SLEEPY
                            if (kotlin.random.Random.nextFloat() < 0.05f) {
                                particles.spawnHeart(cw * 0.47f, ch * 0.55f)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // COOKING: Girl sneaks food from boy's pan, gets caught, playful chase, hug
                SceneType.COOKING -> {
                    val endTime = 9.0f
                    when {
                        t < 1.8f -> {
                            // Normal cooking — boy stirs, girl watches
                            boy.worldX = 0.38f
                            girl.worldX = 0.60f
                            boy.pose = CharacterPose.COOK
                            girl.pose = CharacterPose.IDLE
                            girl.direction = Direction.LEFT
                            boy.direction = Direction.RIGHT
                        }
                        t < 3.2f -> {
                            // Girl sneaks toward pan
                            val wf = ((t * 8).toInt() % 4)
                            girl.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            girl.direction = Direction.LEFT
                            girl.worldX = (0.60f - (t - 1.8f) * 0.10f).coerceAtLeast(0.46f)
                            boy.pose = CharacterPose.COOK
                            boy.direction = Direction.RIGHT
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < 4.5f -> {
                            // Girl steals a bite — boy turns and catches her!
                            girl.worldX = 0.46f
                            girl.pose = CharacterPose.EAT_SNEAK
                            girl.emotion = CharacterEmotion.PLAYFUL
                            boy.pose = CharacterPose.COOK
                            if (t > 3.8f && boy.emote == EmoteType.NONE) {
                                boy.pose = CharacterPose.SURPRISED
                                boy.emote = EmoteType.EXCLAMATION
                                boy.emoteTimer = 1.5f
                                boy.emotion = CharacterEmotion.SURPRISED
                                audio.playHeartChime()
                                showMessage("Hey! That is mine!", duration = 3.0f)
                            }
                        }
                        t < 6.0f -> {
                            // Girl runs away giggling
                            val wf = ((t * 8).toInt() % 4)
                            girl.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            girl.direction = Direction.RIGHT
                            girl.worldX = (0.46f + (t - 4.5f) * 0.10f).coerceAtMost(0.62f)
                            girl.emotion = CharacterEmotion.PLAYFUL
                            // Boy chases
                            boy.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.direction = Direction.RIGHT
                            boy.worldX = (0.38f + (t - 4.5f) * 0.10f).coerceAtMost(0.52f)
                            boy.emotion = CharacterEmotion.HAPPY
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < endTime -> {
                            // Caught — hug
                            boy.worldX = 0.48f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.HUG
                            girl.pose = CharacterPose.HUG
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (t > 6.5f && boySpeechText == null) {
                                boySpeechText = "You little thief"
                                boySpeechTimer = 3.0f
                                girlSpeechText = "So yummy though"
                                girlSpeechTimer = 3.0f
                            }
                            if (kotlin.random.Random.nextFloat() < 0.10f) {
                                particles.spawnHeart(cw * 0.53f, ch * 0.55f)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // SLEEP / LIVING ROOM: Both yawn, boy leads girl to couch, she snuggles on his shoulder
                SceneType.SLEEP -> {
                    val endTime = 9.0f
                    when {
                        t < 1.5f -> {
                            // Both yawn
                            boy.pose = CharacterPose.SLEEP_YAWN
                            girl.pose = CharacterPose.SLEEP_YAWN
                            boy.emotion = CharacterEmotion.SLEEPY
                            girl.emotion = CharacterEmotion.SLEEPY
                            boy.worldX = 0.38f
                            girl.worldX = 0.60f
                            if (t > 0.5f && boy.emote == EmoteType.NONE) {
                                boy.emote = EmoteType.SLEEP_Z
                                boy.emoteTimer = 2.0f
                            }
                        }
                        t < 3.2f -> {
                            // Boy walks to girl, offers hand
                            val wf = ((t * 8).toInt() % 4)
                            boy.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.direction = Direction.RIGHT
                            boy.worldX = (0.38f + (t - 1.5f) * 0.10f).coerceAtMost(0.52f)
                            girl.pose = CharacterPose.IDLE
                            girl.direction = Direction.LEFT
                            girl.emotion = CharacterEmotion.SLEEPY
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < 5.0f -> {
                            // Walk together to couch
                            val wf = ((t * 8).toInt() % 4)
                            val wp = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.pose = wp
                            girl.pose = wp
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.RIGHT
                            boy.worldX = (0.52f + (t - 3.2f) * 0.06f).coerceAtMost(0.42f)
                            girl.worldX = (0.60f + (t - 3.2f) * 0.06f).coerceAtMost(0.52f)
                            triggerWalkFootstep((t * 8).toInt())
                            showMessage("Come, lets rest a while", duration = 3.0f)
                        }
                        t < endTime -> {
                            // Sit and snuggle on couch
                            boy.worldX = 0.42f
                            girl.worldX = 0.52f
                            boy.pose = CharacterPose.SIT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.SLEEPY
                            if (t > 5.5f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.SLEEP_Z
                                girl.emoteTimer = 3.0f
                            }
                            if (kotlin.random.Random.nextFloat() < 0.06f) {
                                particles.spawnHeart(cw * 0.47f, ch * 0.52f)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // WALK / LANTERN STROLL: Boy offers hand, they walk together, boy points at lantern, she leans
                SceneType.WALK -> {
                    val endTime = 9.0f
                    when {
                        t < 1.5f -> {
                            // Standing still, boy extends hand
                            boy.worldX = 0.38f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.WAVE
                            girl.pose = CharacterPose.IDLE
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.LEFT
                            boy.emotion = CharacterEmotion.LOVING
                        }
                        t < 4.5f -> {
                            // Walk together hand-in-hand across the path
                            val wf = ((t * 8).toInt() % 4)
                            val wp = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.pose = wp
                            girl.pose = wp
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.RIGHT
                            val progress = (t - 1.5f) / 3.0f
                            boy.worldX = (0.28f + progress * 0.22f).coerceIn(0.28f, 0.50f)
                            girl.worldX = (0.40f + progress * 0.22f).coerceIn(0.40f, 0.62f)
                            triggerWalkFootstep((t * 8).toInt())
                            if (t > 2.5f && boySpeechText == null) {
                                showMessage("The lanterns are so beautiful tonight", duration = 4.0f)
                                boySpeechText = "Look at the lanterns"
                                boySpeechTimer = 4.0f
                            }
                        }
                        t < 6.5f -> {
                            // Stop — boy points at lantern glow
                            boy.worldX = 0.50f
                            girl.worldX = 0.62f
                            boy.pose = CharacterPose.WAVE
                            boy.direction = Direction.RIGHT
                            girl.pose = CharacterPose.IDLE
                            girl.direction = Direction.LEFT
                            girl.emotion = CharacterEmotion.LOVING
                            if (t > 5.0f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.HEART
                                girl.emoteTimer = 2.5f
                                audio.playHeartChime()
                                particles.spawnHeart(cw * 0.56f, ch * 0.55f)
                                particles.spawnSparkles(cw * 0.56f, ch * 0.52f, 3)
                            }
                        }
                        t < endTime -> {
                            // Girl leans on boy
                            boy.worldX = 0.44f
                            girl.worldX = 0.54f
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            boy.direction = Direction.RIGHT
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (kotlin.random.Random.nextFloat() < 0.08f) {
                                particles.spawnHeart(cw * 0.49f, ch * 0.55f)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // LOOKING / STARGAZE: Boy shows Two Hearts constellation, she gasps and hugs
                SceneType.LOOKING -> {
                    val endTime = 9.0f
                    when {
                        t < 1.8f -> {
                            // Both looking at sky
                            boy.worldX = 0.38f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.IDLE
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.LEFT
                            boy.emotion = CharacterEmotion.CURIOUS
                            girl.emotion = CharacterEmotion.CURIOUS
                        }
                        t < 3.5f -> {
                            // Boy points at Two Hearts constellation
                            boy.worldX = 0.38f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.WAVE
                            boy.direction = Direction.RIGHT
                            girl.pose = CharacterPose.IDLE
                            if (t > 2.5f && boySpeechText == null) {
                                boySpeechText = "See those two stars?"
                                boySpeechTimer = 3.5f
                                showMessage("The Two Hearts constellation — just like us", duration = 5.0f)
                                particles.spawnSparkles(cw * 0.50f, ch * 0.25f, 6)
                                audio.playHeartChime()
                            }
                        }
                        t < 5.5f -> {
                            // Girl gasps and reacts
                            boy.worldX = 0.38f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.SURPRISED
                            girl.emotion = CharacterEmotion.LOVING
                            if (t > 4.0f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.HEART
                                girl.emoteTimer = 2.0f
                                girlSpeechText = "That is us up there!"
                                girlSpeechTimer = 3.0f
                                particles.spawnHeart(cw * 0.53f, ch * 0.40f)
                            }
                        }
                        t < endTime -> {
                            // Head pat and hug under the stars
                            boy.worldX = 0.42f
                            girl.worldX = 0.54f
                            boy.pose = CharacterPose.HEAD_PAT
                            girl.pose = CharacterPose.HEAD_PAT_RECEIVE
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.SHY
                            if (t > 7.0f) {
                                boy.pose = CharacterPose.HUG
                                girl.pose = CharacterPose.HUG
                            }
                            if (kotlin.random.Random.nextFloat() < 0.08f) {
                                particles.spawnHeart(cw * 0.48f + (kotlin.random.Random.nextFloat() - 0.5f) * 25f, ch * 0.45f)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // MOMO_STALL: Feed-momo dance, shared dumpling, hearts overflow
                SceneType.MOMO_STALL -> {
                    val endTime = 9.0f
                    when {
                        t < 1.5f -> {
                            // At the stall, boy cooks
                            boy.worldX = 0.36f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.COOK
                            girl.pose = CharacterPose.IDLE
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.LEFT
                            boy.emotion = CharacterEmotion.HAPPY
                        }
                        t < 3.5f -> {
                            // Boy brings momo to girl
                            val wf = ((t * 8).toInt() % 4)
                            boy.pose = when (wf) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.direction = Direction.RIGHT
                            boy.worldX = (0.36f + (t - 1.5f) * 0.08f).coerceAtMost(0.50f)
                            girl.pose = CharacterPose.IDLE
                            triggerWalkFootstep((t * 8).toInt())
                            if (t > 2.5f && boySpeechText == null) {
                                boySpeechText = "First momo for you"
                                boySpeechTimer = 3.0f
                                showMessage("Fresh and hot, just for my love", duration = 4.0f)
                            }
                        }
                        t < 5.5f -> {
                            // Feed momo
                            boy.worldX = 0.46f
                            girl.worldX = 0.58f
                            boy.pose = CharacterPose.FEED_MOMO
                            girl.pose = CharacterPose.EAT_MOMO
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.HAPPY
                            if (t > 4.0f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.SPARKLE
                                girl.emoteTimer = 2.0f
                                girlSpeechText = "So yummy!"
                                girlSpeechTimer = 2.5f
                                audio.playHeartChime()
                                particles.spawnHeart(cw * 0.52f, ch * 0.52f)
                                particles.spawnSparkles(cw * 0.52f, ch * 0.50f, 4)
                            }
                        }
                        t < 7.5f -> {
                            // Dance together in joy
                            val df = ((t * 4).toInt() % 2)
                            boy.pose = if (df == 0) CharacterPose.JOY_JUMP else CharacterPose.IDLE
                            girl.pose = if (df == 0) CharacterPose.HUG else CharacterPose.JOY_JUMP
                            boy.worldX = 0.42f
                            girl.worldX = 0.56f
                            boy.emotion = CharacterEmotion.HAPPY
                            girl.emotion = CharacterEmotion.HAPPY
                            if (kotlin.random.Random.nextFloat() < 0.12f) {
                                particles.spawnHeart(cw * 0.49f + (kotlin.random.Random.nextFloat() - 0.5f) * 40f, ch * 0.52f)
                            }
                        }
                        t < endTime -> {
                            // Hug at the stall
                            boy.worldX = 0.44f
                            girl.worldX = 0.54f
                            boy.pose = CharacterPose.HUG
                            girl.pose = CharacterPose.HUG
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // EVENING_RIDE: Speed boost, girl hugs boy tighter, hearts trail behind scooter
                SceneType.EVENING_RIDE -> {
                    val endTime = 8.0f
                    when {
                        t < 2.0f -> {
                            // Normal riding
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.IDLE
                            boy.direction = Direction.RIGHT
                            girl.direction = Direction.RIGHT
                            boy.emotion = CharacterEmotion.HAPPY
                            girl.emotion = CharacterEmotion.HAPPY
                        }
                        t < 4.5f -> {
                            // Speed up — girl reacts, hugs tighter
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.HUG
                            boy.emotion = CharacterEmotion.HAPPY
                            girl.emotion = CharacterEmotion.LOVING
                            if (t > 2.5f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.HEART
                                girl.emoteTimer = 3.0f
                                girlSpeechText = "Not too fast!"
                                girlSpeechTimer = 3.0f
                                boySpeechText = "Hold on tight!"
                                boySpeechTimer = 3.0f
                                audio.playHeartChime()
                                showMessage("Racing down the open road", duration = 4.0f)
                            }
                        }
                        t < endTime -> {
                            // Slow down, boy grins, hearts trail
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.IDLE
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (kotlin.random.Random.nextFloat() < 0.12f) {
                                particles.spawnHeart(
                                    cw * 0.30f + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                                    ch * 0.58f + (kotlin.random.Random.nextFloat() - 0.5f) * 10f
                                )
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }

                // COZY_LOFT: Record player, slow dance, tea clink, cozy snuggle
                SceneType.COZY_LOFT -> {
                    val endTime = 9.5f
                    when {
                        t < 1.5f -> {
                            // Both sitting, loft cozy
                            boy.worldX = 0.58f
                            girl.worldX = 0.65f
                            boy.pose = CharacterPose.SIT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            boy.emotion = CharacterEmotion.HAPPY
                            girl.emotion = CharacterEmotion.HAPPY
                        }
                        t < 3.5f -> {
                            // Boy stands up, extends hand for dance
                            boy.worldX = 0.50f
                            girl.worldX = 0.65f
                            boy.pose = CharacterPose.IDLE
                            boy.direction = Direction.RIGHT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            if (t > 2.0f && boySpeechText == null) {
                                boySpeechText = "Dance with me?"
                                boySpeechTimer = 3.0f
                                showMessage("The record player hums softly", duration = 4.0f)
                                audio.playHeartChime()
                                particles.spawnSparkles(cw * 0.54f, ch * 0.45f, 1)
                            }
                        }
                        t < 6.5f -> {
                            // Slow dance together
                            val df = ((t * 3).toInt() % 2)
                            boy.pose = if (df == 0) CharacterPose.JOY_JUMP else CharacterPose.HUG
                            girl.pose = if (df == 0) CharacterPose.HUG else CharacterPose.JOY_JUMP
                            boy.worldX = 0.50f
                            girl.worldX = 0.60f
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (kotlin.random.Random.nextFloat() < 0.08f) {
                                particles.spawnSparkles(cw * 0.55f + (kotlin.random.Random.nextFloat() - 0.5f) * 30f, ch * 0.45f, 1)
                            }
                            if (kotlin.random.Random.nextFloat() < 0.07f) {
                                particles.spawnHeart(cw * 0.55f, ch * 0.50f)
                            }
                        }
                        t < endTime -> {
                            // Back to couch — cozy blanket snuggle
                            boy.worldX = 0.58f
                            girl.worldX = 0.65f
                            boy.pose = CharacterPose.SIT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (t > 7.5f && girl.emote == EmoteType.NONE) {
                                girl.emote = EmoteType.HEART
                                girl.emoteTimer = 2.5f
                                particles.spawnHeart(cw * 0.615f, ch * 0.50f)
                            }
                        }
                        else -> {
                            isWatchSceneActive = false
                            boy.worldX = 0.58f
                            girl.worldX = 0.65f
                            boy.pose = CharacterPose.SIT
                            girl.pose = CharacterPose.SIT_SNUGGLE
                        }
                    }
                }

                // Fallback: original kiss sequence
                else -> {
                    when {
                        t < 0.9f -> {
                            val step = ((t * 8).toInt() % 4)
                            boy.walkFrame = step
                            girl.walkFrame = step
                            val walkPose = when (step) { 0 -> CharacterPose.WALK_1; 1 -> CharacterPose.WALK_2; 2 -> CharacterPose.WALK_3; else -> CharacterPose.WALK_4 }
                            boy.pose = walkPose
                            girl.pose = walkPose
                            boy.worldX = (boy.worldX + (0.45f - boy.worldX) * deltaSeconds * 4.0f).coerceIn(0.2f, 0.45f)
                            girl.worldX = (girl.worldX - (girl.worldX - 0.53f) * deltaSeconds * 4.0f).coerceIn(0.53f, 0.8f)
                            triggerWalkFootstep((t * 8).toInt())
                        }
                        t < 6.5f -> {
                            boy.worldX = 0.45f
                            girl.worldX = 0.53f
                            boy.pose = CharacterPose.KISS
                            girl.pose = CharacterPose.KISS
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            val kissMidX = 0.49f
                            if (boySpeechText == null) {
                                boySpeechText = "i love u ${girl.name}"
                                boySpeechTimer = 5.5f
                                girlSpeechText = "i love u ${boy.name}"
                                girlSpeechTimer = 5.5f
                                audio.playHeartChime()
                                showMessage("${boy.name} & ${girl.name} - Forever and always", duration = 5.0f)
                                particles.spawnHeart(canvasWidth * kissMidX, canvasHeight * boy.worldY - 32f * pxScale)
                                particles.spawnSparkles(canvasWidth * kissMidX, canvasHeight * boy.worldY - 26f * pxScale, 4)
                            }
                            if (kotlin.random.Random.nextFloat() < 0.12f) {
                                particles.spawnHeart(canvasWidth * kissMidX + (kotlin.random.Random.nextFloat() - 0.5f) * 40f, canvasHeight * boy.worldY - 32f * pxScale)
                            }
                        }
                        else -> { isWatchSceneActive = false }
                    }
                }
            }
        }

        // Character breathing & lively idle motion (clearly visible on pixel canvas!)
        val breath = (sin(sceneTime * 2.4f) * 1.8f * pixelScale)
        val girlBreath = (sin(sceneTime * 2.4f + 0.8f) * 1.8f * pixelScale)

        val boySway = sin(sceneTime * 1.2f) * (0.8f * pixelScale)
        val girlSway = sin(sceneTime * 1.2f + 1.4f) * (0.8f * pixelScale)
        val isBoyStationary = when (boy.pose) {
            CharacterPose.IDLE, CharacterPose.HOLD_HANDS, CharacterPose.SIT, CharacterPose.RECEIVE_FLOWER, CharacterPose.GIVE_FLOWER, CharacterPose.EAT_MOMO, CharacterPose.FEED_MOMO -> true
            else -> false
        }
        val isGirlStationary = when (girl.pose) {
            CharacterPose.IDLE, CharacterPose.HOLD_HANDS, CharacterPose.SIT, CharacterPose.SIT_SNUGGLE, CharacterPose.RECEIVE_FLOWER, CharacterPose.EAT_MOMO -> true
            else -> false
        }
        boy.idleSwayOffset = if (isBoyStationary) boySway else 0f
        girl.idleSwayOffset = if (isGirlStationary) girlSway else 0f

        // If earphones are on and music is playing, cute rhythm head-bobbing & floating notes!
        if (earphonesActive && audio.isPlayingMusic) {
            val rhythm = kotlin.math.abs(sin(sceneTime * 6.5f)) * (2.8f * pixelScale)
            boy.bounceOffset = rhythm
            girl.bounceOffset = kotlin.math.abs(sin(sceneTime * 6.5f + 0.35f)) * (2.8f * pixelScale)
            if (Random.nextFloat() < 0.045f) {
                particles.spawnMusicNote(
                    canvasWidth * ((boy.worldX + girl.worldX) / 2f) + (Random.nextFloat() - 0.5f) * 60f,
                    canvasHeight * boy.worldY - (28f * pixelScale)
                )
            }
        } else {
            boy.breathingOffset = breath
            girl.breathingOffset = girlBreath
        }

        // Shared motion tweening update (smooth transitions using deltaSeconds)
        boy.updateMotion(deltaSeconds)
        girl.updateMotion(deltaSeconds)

        // Smooth sitting / cuddle / kiss offset interpolation
        val isCuddled = (boy.pose == CharacterPose.KISS || girl.pose == CharacterPose.KISS ||
                         boy.pose == CharacterPose.HUG || girl.pose == CharacterPose.HUG ||
                         boy.pose == CharacterPose.SIT_SNUGGLE || girl.pose == CharacterPose.SIT_SNUGGLE)
        val targetCuddle = if (isCuddled) 1f else 0f
        if (cuddleProgress != targetCuddle) {
            val cuddleSpeed = 3.5f // ~0.28s transition
            cuddleProgress = if (targetCuddle > cuddleProgress) {
                (cuddleProgress + deltaSeconds * cuddleSpeed).coerceAtMost(1f)
            } else {
                (cuddleProgress - deltaSeconds * cuddleSpeed).coerceAtLeast(0f)
            }
        }

        // Living room autonomous activity return timer
        if (livingRoomReturnTimer > 0f) {
            livingRoomReturnTimer = (livingRoomReturnTimer - deltaSeconds).coerceAtLeast(0f)
            if (livingRoomReturnTimer <= 0f) {
                val returningChar = livingRoomReturnChar
                if (returningChar != null && !returningChar.isMovingOrTransitioning) {
                    returningChar.moveTo(livingRoomReturnTargetX, arrivePose = CharacterPose.SIT)
                    returningChar.emotion = CharacterEmotion.HAPPY
                    returningChar.emote = EmoteType.HEART
                    returningChar.emoteTimer = 2.0f
                }
                livingRoomReturnChar = null
            }
        }

        // Character natural eye blinking (clean 150ms blink every 2.5-5.5s)
        boy.blinkTimer += deltaSeconds
        if (boy.blinkTimer >= boy.nextBlinkTime) {
            boy.isBlinking = true
            if (boy.blinkTimer >= boy.nextBlinkTime + 0.15f) {
                boy.isBlinking = false
                boy.blinkTimer = 0f
                boy.nextBlinkTime = 2.5f + Random.nextFloat() * 3.0f
            }
        }

        girl.blinkTimer += deltaSeconds
        if (girl.blinkTimer >= girl.nextBlinkTime) {
            girl.isBlinking = true
            if (girl.blinkTimer >= girl.nextBlinkTime + 0.15f) {
                girl.isBlinking = false
                girl.blinkTimer = 0f
                girl.nextBlinkTime = 2.5f + Random.nextFloat() * 3.0f
            }
        }

        // Smooth bounce settlement for jumps/reactions
        val isWalking = when (boy.pose) {
            CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4 -> true
            else -> false
        }
        if (!isWalking && !earphonesActive && boy.bounceOffset > 0f) {
            boy.bounceOffset = (boy.bounceOffset - deltaSeconds * 22f).coerceAtLeast(0f)
        }
        val isGirlWalking = when (girl.pose) {
            CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4 -> true
            else -> false
        }
        if (!isGirlWalking && !earphonesActive && girl.bounceOffset > 0f) {
            girl.bounceOffset = (girl.bounceOffset - deltaSeconds * 22f).coerceAtLeast(0f)
        }

        // Emote timers
        if (boy.emoteTimer > 0) {
            boy.emoteTimer -= deltaSeconds
            if (boy.emoteTimer <= 0) boy.emote = EmoteType.NONE
        }
        if (girl.emoteTimer > 0) {
            girl.emoteTimer -= deltaSeconds
            if (girl.emoteTimer <= 0) girl.emote = EmoteType.NONE
        }

        // Reaction timers (locks poses for user tap reactions / hugs)
        if (boy.reactionTimer > 0) {
            boy.reactionTimer -= deltaSeconds
        }
        if (girl.reactionTimer > 0) {
            girl.reactionTimer -= deltaSeconds
        }

        // Speech timers for double-tap dialogue bubbles
        if (boySpeechTimer > 0f) {
            boySpeechTimer -= deltaSeconds
            if (boySpeechTimer <= 0f) {
                boySpeechText = null
            }
        }
        if (girlSpeechTimer > 0f) {
            girlSpeechTimer -= deltaSeconds
            if (girlSpeechTimer <= 0f) {
                girlSpeechText = null
            }
        }
        boy.isSpeaking = !boySpeechText.isNullOrEmpty()
        girl.isSpeaking = !girlSpeechText.isNullOrEmpty()

        // Extra floating hearts during warm couple hug or kiss
        if ((boy.pose == CharacterPose.HUG || boy.pose == CharacterPose.KISS) && Random.nextFloat() < 0.10f) {
            particles.spawnHeart(canvasWidth * ((boy.worldX + girl.worldX) / 2f), canvasHeight * boy.worldY - 25f)
        }

        // Subtitle message alpha animation
        if (sceneMessage != null) {
            if (messageTimer > 0) {
                messageTimer -= deltaSeconds
                messageAlpha = (messageAlpha + deltaSeconds * 2.0f).coerceAtMost(1f)
            } else {
                messageAlpha = (messageAlpha - deltaSeconds * 1.5f).coerceAtLeast(0f)
                if (messageAlpha <= 0f) {
                    sceneMessage = null
                }
            }
        }

        // Autonomous interactions (keep the world feeling alive even when idle)
        val isScriptActive = when (currentScene) {
            SceneType.FLOWER -> sceneTime < 8.2f
            SceneType.UNDER_TREE -> sceneTime < 7.2f
            SceneType.COOKING -> sceneTime < 7.6f
            SceneType.SLEEP -> sceneTime < 6.9f
            SceneType.WALK -> sceneTime < 6.6f
            SceneType.LOOKING -> sceneTime < 7.6f
            SceneType.MOMO_STALL -> sceneTime < 6.2f
            SceneType.EVENING_RIDE -> sceneTime < 7.0f
            SceneType.COZY_LOFT -> sceneTime < 5.6f
        }
        autonomousTimer += deltaSeconds
        if (!isScriptActive && !isWatchSceneActive && autonomousTimer >= nextAutonomousInterval &&
            boy.reactionTimer <= 0 && girl.reactionTimer <= 0 &&
            !boy.isMovingOrTransitioning && !girl.isMovingOrTransitioning &&
            livingRoomReturnTimer <= 0f) {
            autonomousTimer = 0f
            nextAutonomousInterval = 3.5f + Random.nextFloat() * 2.5f
            triggerAutonomousMoment(canvasWidth, canvasHeight)
        }

        // Living Dynamic Weather System (particles & soundscapes)
        if (weatherDriftEnabled) {
            weatherDriftTimer += deltaSeconds
            if (weatherDriftTimer >= weatherDriftInterval) {
                weatherDriftTimer = 0f
                val candidates = WeatherType.values().filter { it != weather }
                val next = candidates.randomOrNull()
                if (next != null) {
                    weather = next
                    audio.playWeatherBgm(next, isAutomaticDrift = true)
                }
            }
        }
        hourCheckTimer += deltaSeconds
        if (hourCheckTimer >= 30f) {
            hourCheckTimer = 0f
            timeOfDayPhase = TimeOfDayPhase.resolve(atmosphereMode)
        }
        val isOutdoor = isCurrentSceneOutdoor
        if (audio.isIndoor != (!isOutdoor)) {
            audio.setIndoor(!isOutdoor, smooth = true)
        }
        if (audio.isEnabled && audio.currentWeather == null) {
            audio.playWeatherBgm(weather, isAutomaticDrift = false)
        }
        val isNight = timeOfDayPhase.isNight
        // Spawn and update weather particles every frame!
        particles.updateWeatherEffects(weather, canvasWidth, canvasHeight, isOutdoor, isNight, deltaSeconds)

        if (weather == WeatherType.RAIN && audio.isEnabled) {
            if (!isRainAmbientPlaying) {
                audio.startRainAmbient()
                isRainAmbientPlaying = true
            }
            if (isOutdoor) {
                lightningTimer += deltaSeconds
                if (lightningTimer >= nextLightningTime) {
                    triggerLightning()
                }
            }
        } else if (isRainAmbientPlaying) {
            audio.stopRainAmbient()
            isRainAmbientPlaying = false
        }

        // Ambient soundscapes for other weather types (non-rain)
        if (isOutdoor && audio.isEnabled && !audio.isPlayingMusic) {
            when (weather) {
                WeatherType.AUTUMN -> {
                    if (Random.nextFloat() < 0.005f) audio.playLeafRustle()
                }
                WeatherType.SAKURA -> {
                    if (Random.nextFloat() < 0.004f) audio.playStarTwinkle()
                }
                WeatherType.SUNNY -> {
                    if (!isNight && Random.nextFloat() < 0.005f) audio.playBirdChirp()
                }
                WeatherType.SNOW -> {
                    if (Random.nextFloat() < 0.003f) audio.playStarTwinkle()
                }
                else -> {}
            }
        }

        // Cute sleeping Z particles rising from sleeping cat
        if (catState == CatState.SLEEPING && currentScene != SceneType.EVENING_RIDE) {
            if (Random.nextFloat() < 0.008f) {
                particles.spawnSleepZ(canvasWidth * catWorldX + 6f, canvasHeight * catWorldY - 14f * pixelScale)
            }
        }

        if (lightningFlashAlpha > 0f) {
            lightningFlashAlpha = (lightningFlashAlpha - deltaSeconds * 3.5f).coerceAtLeast(0f)
        }

        if (flowerWiggleTimer > 0f) {
            flowerWiggleTimer = (flowerWiggleTimer - deltaSeconds).coerceAtLeast(0f)
        }

        if (wipeAlpha > 0f) {
            wipeAlpha = (wipeAlpha - deltaSeconds * 2.8f).coerceAtLeast(0f)
        }

        // Kitchen interactive appliance timers
        if (fridgeDoorOpenTimer > 0f) {
            fridgeDoorOpenTimer = (fridgeDoorOpenTimer - deltaSeconds).coerceAtLeast(0f)
            if (fridgeDoorOpenTimer <= 0f && (boy.worldX > 0.70f || boy.targetWorldX > 0.70f)) {
                boy.moveTo(0.46f)
                boy.transitionPoseTo(CharacterPose.IDLE)
                boy.emotion = CharacterEmotion.HAPPY
            }
        }
        if (cabinetOpenTimer > 0f) {
            cabinetOpenTimer = (cabinetOpenTimer - deltaSeconds).coerceAtLeast(0f)
        }
        if (sinkRunningTimer > 0f) {
            sinkRunningTimer = (sinkRunningTimer - deltaSeconds).coerceAtLeast(0f)
            if (sinkRunningTimer <= 0f && (girl.worldX < 0.38f || girl.targetWorldX < 0.38f)) {
                girl.moveTo(0.54f)
                girl.transitionPoseTo(CharacterPose.COOK)
            }
        }

        // Kitchen tap-interaction animation countdowns
        if (clockSpinTimer > 0f)  clockSpinTimer  = (clockSpinTimer  - deltaSeconds).coerceAtLeast(0f)
        if (binLidTimer > 0f)     binLidTimer     = (binLidTimer     - deltaSeconds).coerceAtLeast(0f)
        if (crateShakeTimer > 0f) crateShakeTimer = (crateShakeTimer - deltaSeconds).coerceAtLeast(0f)
        if (planterAnimTimer > 0f) planterAnimTimer = (planterAnimTimer - deltaSeconds).coerceAtLeast(0f)
        if (stoolWobbleTimer > 0f) stoolWobbleTimer = (stoolWobbleTimer - deltaSeconds).coerceAtLeast(0f)

        // Living Room tap-interaction animation countdowns
        if (tableCandleTimer > 0f)  tableCandleTimer  = (tableCandleTimer  - deltaSeconds).coerceAtLeast(0f)
        if (poufBounceTimer > 0f)   poufBounceTimer   = (poufBounceTimer   - deltaSeconds).coerceAtLeast(0f)
        if (cardboardBoxTimer > 0f) cardboardBoxTimer = (cardboardBoxTimer - deltaSeconds).coerceAtLeast(0f)
        if (basketYarnTimer > 0f)   basketYarnTimer   = (basketYarnTimer   - deltaSeconds).coerceAtLeast(0f)
        if (magazineRackTimer > 0f) magazineRackTimer = (magazineRackTimer - deltaSeconds).coerceAtLeast(0f)

        // Lantern Stroll tap-interaction countdowns
        if (pagodaGlowTimer > 0f) pagodaGlowTimer = (pagodaGlowTimer - deltaSeconds).coerceAtLeast(0f)
        if (lavenderSwayTimer > 0f) lavenderSwayTimer = (lavenderSwayTimer - deltaSeconds).coerceAtLeast(0f)
        if (mushroomBounceTimer > 0f) mushroomBounceTimer = (mushroomBounceTimer - deltaSeconds).coerceAtLeast(0f)
        if (constellationConnectTimer > 0f) constellationConnectTimer = (constellationConnectTimer - deltaSeconds).coerceAtLeast(0f)

        // Momo Stall tap-interaction countdowns
        if (momoSignFlickerTimer > 0f) momoSignFlickerTimer = (momoSignFlickerTimer - deltaSeconds).coerceAtLeast(0f)
        if (momoSteamerTimer > 0f) momoSteamerTimer = (momoSteamerTimer - deltaSeconds).coerceAtLeast(0f)
        if (chutneySpiceTimer > 0f) chutneySpiceTimer = (chutneySpiceTimer - deltaSeconds).coerceAtLeast(0f)
        if (chalkboardTimer > 0f) chalkboardTimer = (chalkboardTimer - deltaSeconds).coerceAtLeast(0f)
        if (bambooCrateTimer > 0f) bambooCrateTimer = (bambooCrateTimer - deltaSeconds).coerceAtLeast(0f)
        if (milkSaucerTimer > 0f) milkSaucerTimer = (milkSaucerTimer - deltaSeconds).coerceAtLeast(0f)
        if (streetDiningTableTimer > 0f) streetDiningTableTimer = (streetDiningTableTimer - deltaSeconds).coerceAtLeast(0f)

        // Evening Ride tap-interaction countdowns
        if (scooterHonkTimer > 0f) scooterHonkTimer = (scooterHonkTimer - deltaSeconds).coerceAtLeast(0f)
        if (templeGlowTimer > 0f) templeGlowTimer = (templeGlowTimer - deltaSeconds).coerceAtLeast(0f)

        // Midnight Loft tap-interaction countdowns
        if (loftTableTimer > 0f) loftTableTimer = (loftTableTimer - deltaSeconds).coerceAtLeast(0f)
        if (loftBookNookTimer > 0f) loftBookNookTimer = (loftBookNookTimer - deltaSeconds).coerceAtLeast(0f)
        if (loftFairyLightsTimer > 0f) loftFairyLightsTimer = (loftFairyLightsTimer - deltaSeconds).coerceAtLeast(0f)
        if (loftWindowTimer > 0f) loftWindowTimer = (loftWindowTimer - deltaSeconds).coerceAtLeast(0f)
        // Feature 1: Mochi Matchmaker countdown timers
        if (mochiMatchmakerCooldown > 0f) {
            mochiMatchmakerCooldown = (mochiMatchmakerCooldown - deltaSeconds).coerceAtLeast(0f)
        }
        if (mochiNudgeActiveTimer > 0f) {
            mochiNudgeActiveTimer = (mochiNudgeActiveTimer - deltaSeconds).coerceAtLeast(0f)
        }

        // Autonomous Pet Personality behavior cycle
        petBehaviorTimer += deltaSeconds
        if (petBehaviorTimer >= nextPetBehaviorInterval && !isWatchSceneActive) {
            petBehaviorTimer = 0f
            nextPetBehaviorInterval = 4f + Random.nextFloat() * 5f
            if (!mochiMatchmakerActive && canTriggerMochiMatchmaker()) {
                triggerMochiMatchmaker(canvasWidth, canvasHeight)
            } else if (!mochiMatchmakerActive) {
                triggerAutonomousPetBehavior(canvasWidth, canvasHeight)
            }
        }

        // Continuous smooth cat roaming across the scene
        updateCatMovement(deltaSeconds, canvasWidth, canvasHeight)

        // Scripted scene animations
        when (currentScene) {
            SceneType.FLOWER -> updateFlowerScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.UNDER_TREE -> updateTreeScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.COOKING -> updateCookingScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.SLEEP -> updateSleepScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.WALK -> updateWalkScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.LOOKING -> updateLookingScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.MOMO_STALL -> updateMomoScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.EVENING_RIDE -> updateEveningRideScene(deltaSeconds, canvasWidth, canvasHeight)
            SceneType.COZY_LOFT -> updateCozyLoftScene(deltaSeconds, canvasWidth, canvasHeight)
        }

        // Feature 2: High-level CharacterState synchronization (wrapper layer only)
        updateCharacterState(boy)
        updateCharacterState(girl)

        // Particle physics
        particles.update(deltaSeconds, canvasWidth, canvasHeight)

        // Ambient bird system
        birdSystem.update(
            dt = deltaSeconds,
            cw = canvasWidth,
            ch = canvasHeight,
            p = pixelScale,
            scene = currentScene,
            weather = weather,
            isNight = timeOfDayPhase.isNight,
            catX = canvasWidth * catWorldX,
            catY = canvasHeight * catWorldY
        )
    }

    fun forceSpawnBirdForTest(
        surface: PerchSurface? = null,
        species: BirdSpecies? = null,
        cw: Float = 1000f,
        ch: Float = 1000f,
        p: Float = 3.5f
    ): BirdEntity? {
        return birdSystem.spawnBird(cw, ch, p, currentScene, forcedSurface = surface, forcedSpecies = species)
    }

    private fun updateCharacterState(char: PixelCharacter) {
        char.characterState = when (char.pose) {
            CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN -> CharacterState.SLEEPING
            CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4 -> CharacterState.WALKING
            CharacterPose.SIT, CharacterPose.SIT_SNUGGLE -> CharacterState.SITTING
            CharacterPose.HUG, CharacterPose.KISS, CharacterPose.COOK, CharacterPose.EAT_SNEAK,
            CharacterPose.EAT_MOMO, CharacterPose.FEED_MOMO, CharacterPose.GIVE_FLOWER,
            CharacterPose.RECEIVE_FLOWER, CharacterPose.HOLD_HANDS, CharacterPose.HEAD_PAT,
            CharacterPose.HEAD_PAT_RECEIVE, CharacterPose.WAVE -> CharacterState.INTERACTING
            CharacterPose.SURPRISED -> CharacterState.SURPRISED
            CharacterPose.JOY_JUMP -> CharacterState.HAPPY
            else -> when (char.emotion) {
                CharacterEmotion.SLEEPY -> CharacterState.SLEEPY
                CharacterEmotion.SURPRISED -> CharacterState.SURPRISED
                CharacterEmotion.HAPPY, CharacterEmotion.PLAYFUL, CharacterEmotion.LOVING -> CharacterState.HAPPY
                CharacterEmotion.CURIOUS, CharacterEmotion.SHY -> CharacterState.LOOKING
                else -> CharacterState.IDLE
            }
        }
    }

    private fun updateFlowerScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        if (t < 1.8f) {
            // Boy spots girlfriend, smiles tenderly
            boy.worldX = 0.36f
            girl.worldX = 0.57f
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.IDLE
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.IDLE
                girl.direction = Direction.LEFT
            }
        } else if (t < 4.0f) {
            // Boy walks toward girlfriend with 4-frame walk cycle
            val walkProgress = (t - 1.8f) / 2.2f
            boy.worldX = (0.36f + walkProgress * 0.07f).coerceAtMost(0.43f)
            girl.worldX = 0.57f
            val step = ((t * 8).toInt() % 4)
            boy.walkFrame = step
            if (boy.reactionTimer <= 0) {
                boy.pose = when (step) {
                    0 -> CharacterPose.WALK_1
                    1 -> CharacterPose.WALK_2
                    2 -> CharacterPose.WALK_3
                    else -> CharacterPose.WALK_4
                }
                boy.bounceOffset = if (step % 2 == 0) 3f else 0f
            }
            triggerWalkFootstep((t * 8).toInt())
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.IDLE
            }
        } else if (t < 5.8f) {
            // Boy takes out flower with a loving blush
            boy.worldX = 0.43f
            girl.worldX = 0.57f
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.GIVE_FLOWER
                boy.bounceOffset = 0f
                boy.emotion = CharacterEmotion.SHY
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.IDLE
            }
            if (girl.emote == EmoteType.NONE && t > 4.5f) {
                if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SURPRISED
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * 0.57f, ch * 0.58f)
            }
        } else if (t < 8.0f) {
            // Girl receives flower gratefully, holding to chest
            boy.worldX = 0.43f
            girl.worldX = 0.57f
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.RECEIVE_FLOWER
                girl.emotion = CharacterEmotion.LOVING
            }
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.HAPPY
            }
            if (sceneMessage == null && t > 6.0f) {
                showMessage("For you", duration = 4.5f)
                particles.spawnPetals(cw * 0.50f, ch * 0.60f, count = 10)
            }
        } else if (t < 11.0f) {
            // Stand close holding hands
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) boy.pose = CharacterPose.HOLD_HANDS
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) girl.pose = CharacterPose.RECEIVE_FLOWER
        } else {
            // Cozy living idle together
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) boy.pose = CharacterPose.HOLD_HANDS
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) girl.pose = CharacterPose.RECEIVE_FLOWER
            if (Random.nextFloat() < 0.025f) {
                particles.spawnPetals(cw * Random.nextFloat(), ch * 0.35f, 1)
            }
        }
    }

    private fun updateTreeScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        // Leaves constantly drift down from canopy
        if (Random.nextFloat() < 0.045f) {
            particles.spawnLeaf(cw * 0.5f + (Random.nextFloat() - 0.5f) * (cw * 0.35f), ch * 0.28f)
        }

        if (t < 2.0f) {
            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.SIT
            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SIT
        } else if (t < 4.5f) {
            boy.direction = Direction.RIGHT
            girl.direction = Direction.LEFT
            if (boy.emote == EmoteType.NONE && t > 2.5f) {
                boy.emote = EmoteType.HEART
                boy.emoteTimer = 1.8f
                audio.playHeartChime()
            }
        } else if (t < 7.0f) {
            // Boyfriend scoots closer, girlfriend snuggles comfortably
            if (boy.reactionTimer <= 0 && girl.reactionTimer <= 0) {
                boy.worldX = 0.44f
                girl.worldX = 0.56f
            }
            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.SIT_SNUGGLE
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.emotion = CharacterEmotion.LOVING
            }
            if (sceneMessage == null && t > 5.0f) {
                showMessage("Just being here with you is my favorite place.", duration = 4.5f)
                particles.spawnHeart(cw * 0.5f, ch * 0.58f)
            }
        } else {
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) boy.pose = CharacterPose.SIT_SNUGGLE
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) girl.pose = CharacterPose.SIT_SNUGGLE
            if (Random.nextFloat() < 0.015f) {
                audio.playBirdChirp()
            }
        }
    }

    private fun updateCookingScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        // Stew pot always simmering with steam & bubbling
        if (Random.nextFloat() < 0.22f) {
            particles.spawnSteam(cw * 0.58f, ch * 0.60f)
        }
        if (Random.nextFloat() < 0.03f) {
            audio.playCookingBubbles()
        }

        if (t < 1.8f) {
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.COOK
                girl.direction = Direction.RIGHT
            }
            boy.worldX = 0.18f
            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.IDLE
        } else if (t < 4.2f) {
            // Boy sneaks closer with tip-toe walk
            val prog = (t - 1.8f) / 2.4f
            boy.worldX = 0.18f + prog * 0.27f
            val step = ((t * 7).toInt() % 4)
            boy.walkFrame = step
            if (boy.reactionTimer <= 0) {
                boy.pose = when (step) {
                    0 -> CharacterPose.WALK_1
                    1 -> CharacterPose.WALK_2
                    2 -> CharacterPose.WALK_3
                    else -> CharacterPose.WALK_4
                }
                boy.bounceOffset = if (step % 2 == 0) 2f else 0f
            }
            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.COOK
        } else if (t < 6.5f) {
            // Boy grabs food bite sneakily
            if (boy.reactionTimer <= 0 && girl.reactionTimer <= 0) {
                boy.worldX = 0.45f
                girl.worldX = 0.58f
            }
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.EAT_SNEAK
                boy.bounceOffset = 0f
            }
            if (boy.emote == EmoteType.NONE) {
                boy.emote = EmoteType.MUSIC_NOTE
                boy.emoteTimer = 1.5f
            }
        } else if (t < 8.8f) {
            // Girl spins around and catches him!
            if (boy.reactionTimer <= 0 && girl.reactionTimer <= 0) {
                boy.worldX = 0.45f
                girl.worldX = 0.58f
            }
            girl.direction = Direction.LEFT
            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SURPRISED
            if (girl.emote == EmoteType.NONE) {
                girl.emote = EmoteType.EXCLAMATION
                girl.emoteTimer = 1.8f
                audio.playBubblePop()
            }
            boy.emote = EmoteType.SWEAT
            boy.emoteTimer = 1.8f
            if (sceneMessage == null) {
                showMessage("Caught you! ...You can have a taste", duration = 4.0f)
                particles.spawnHeart(cw * 0.48f, ch * 0.58f)
            }
        } else {
            // Sharing food happily together on the rug
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.EAT_SNEAK
            }
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.IDLE
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.HAPPY
            }
        }
    }

    private fun updateSleepScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        // ── Feature 5 & 6: Lamp-gated sleep & Visual Time Cycle ─────────────
        // Lamp ON → characters stay awake regardless of visual time: sit, cuddle, talk.
        // Lamp OFF → simulated cosmetic cycle: NIGHT → DAY → MIDDAY → EVENING → NIGHT.
        //   - NIGHT: dark room, sleeping pose, sleep Z particles, sleeping cat.
        //   - DAY: daylight fills room, smooth wake transition, characters awake.
        //   - MIDDAY: brightest sunlit room, energetic cozy sitting variety.
        //   - EVENING: warm sunset peach, calming down, trending sleepy.
        if (livingRoomLampLit) {
            // When lamp is lit, characters stay awake
            if (couchWakeTimer > 0f) {
                couchWakeTimer -= dt
                if (couchWakeTimer > 1.6f) {
                    // Subtle movement, stretch & blink
                    if (boy.reactionTimer <= 0) {
                        boy.pose = CharacterPose.SLEEP_YAWN
                        boy.emotion = CharacterEmotion.SLEEPY
                        boy.isBlinking = true
                    }
                    if (girl.reactionTimer <= 0) {
                        girl.pose = CharacterPose.SLEEP_YAWN
                        girl.emotion = CharacterEmotion.SLEEPY
                        girl.isBlinking = true
                    }
                } else {
                    // Sit up, look around, smile
                    if (boy.reactionTimer <= 0) {
                        boy.pose = CharacterPose.SIT
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.SPARKLE
                        boy.emoteTimer = 1.5f
                    }
                    if (girl.reactionTimer <= 0) {
                        girl.pose = CharacterPose.SIT
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.emote = EmoteType.SPARKLE
                        girl.emoteTimer = 1.5f
                    }
                    catSleeping = false
                    catState = CatState.SITTING_PURR
                }
            } else {
                // Full awake idle sitting together on couch
                if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0 && boy.pose != CharacterPose.SIT_SNUGGLE) {
                    boy.pose = CharacterPose.SIT
                }
                if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0 && girl.pose != CharacterPose.SIT_SNUGGLE) {
                    girl.pose = CharacterPose.SIT
                }
            }

            // Brighten room back up when lamp is on
            if (ambientDimming > 0f) ambientDimming = (ambientDimming - dt * 0.35f).coerceAtLeast(0f)
            couchPhaseTimer = 0f
            couchVisualPhase = CouchPhase.NIGHT
        } else {
            // ── Feature 6: Isolated visual time cycle (scene-scoped only) ──────
            // Lamp is OFF: advances NIGHT → DAY → MIDDAY → EVENING → NIGHT
            // Never reads or writes real date, calendar, or global atmosphere.
            val phaseDuration = 20f
            couchPhaseTimer += dt
            if (couchPhaseTimer >= phaseDuration) {
                couchPhaseTimer -= phaseDuration
                val prevPhase = couchVisualPhase
                couchVisualPhase = when (couchVisualPhase) {
                    CouchPhase.NIGHT   -> CouchPhase.DAY
                    CouchPhase.DAY     -> CouchPhase.MIDDAY
                    CouchPhase.MIDDAY  -> CouchPhase.EVENING
                    CouchPhase.EVENING -> CouchPhase.NIGHT
                }
                if (couchVisualPhase == CouchPhase.DAY && prevPhase == CouchPhase.NIGHT) {
                    // Morning wake transition
                    couchWakeTimer = 3.0f
                }
            }

            when (couchVisualPhase) {
                CouchPhase.NIGHT -> {
                    // Dim room into midnight
                    if (ambientDimming < 0.58f) ambientDimming = (ambientDimming + dt * 0.20f).coerceAtMost(0.58f)
                    if (couchPhaseTimer < 3.0f) {
                        // Sleep transition: yawn & stretch
                        if (boy.reactionTimer <= 0) {
                            boy.pose = CharacterPose.SLEEP_YAWN
                            boy.emotion = CharacterEmotion.SLEEPY
                        }
                        if (girl.reactionTimer <= 0) {
                            girl.pose = CharacterPose.SLEEP_YAWN
                            girl.emotion = CharacterEmotion.SLEEPY
                        }
                        if (couchPhaseTimer < 1.0f && sceneMessage == null) {
                            showMessage("Drifting off to sleep as midnight falls...", duration = 2.5f)
                        }
                    } else {
                        // Sleeping peacefully
                        if (!boy.isMovingOrTransitioning && !girl.isMovingOrTransitioning) {
                            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.SLEEP
                            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SLEEP
                        }
                        catSleeping = true
                        catState = CatState.SLEEPING
                        if (Random.nextFloat() < 0.04f) {
                            particles.spawnSleepZ(cw * 0.44f, ch * 0.55f)
                            particles.spawnSleepZ(cw * 0.56f, ch * 0.55f)
                        }
                        if (couchPhaseTimer in 3.2f..4.0f && sceneMessage == null) {
                            showMessage("Sweet dreams, my love.", duration = 3.5f)
                        }
                    }
                }
                CouchPhase.DAY -> {
                    // Daylight fills the room
                    if (ambientDimming > 0.05f) ambientDimming = (ambientDimming - dt * 0.25f).coerceAtLeast(0.05f)
                    if (couchWakeTimer > 0f) {
                        couchWakeTimer -= dt
                        if (couchWakeTimer > 1.6f) {
                            // Stretch & blink
                            if (boy.reactionTimer <= 0) {
                                boy.pose = CharacterPose.SLEEP_YAWN
                                boy.emotion = CharacterEmotion.SLEEPY
                                boy.isBlinking = true
                            }
                            if (girl.reactionTimer <= 0) {
                                girl.pose = CharacterPose.SLEEP_YAWN
                                girl.emotion = CharacterEmotion.SLEEPY
                                girl.isBlinking = true
                            }
                        } else {
                            // Sit up & look around
                            if (boy.reactionTimer <= 0) {
                                boy.pose = CharacterPose.SIT
                                boy.emotion = CharacterEmotion.HAPPY
                                boy.emote = EmoteType.SPARKLE
                                boy.emoteTimer = 1.5f
                            }
                            if (girl.reactionTimer <= 0) {
                                girl.pose = CharacterPose.SIT
                                girl.emotion = CharacterEmotion.HAPPY
                                girl.emote = EmoteType.SPARKLE
                                girl.emoteTimer = 1.5f
                            }
                            catSleeping = false
                            catState = CatState.SITTING_PURR
                        }
                    } else {
                        // Awake on couch in daytime
                        if (boy.reactionTimer <= 0) boy.pose = CharacterPose.SIT
                        if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SIT
                        if (couchPhaseTimer in 3.2f..4.0f && sceneMessage == null) {
                            showMessage("Morning light brings another day with you.", duration = 3.0f)
                        }
                    }
                }
                CouchPhase.MIDDAY -> {
                    // Brightest sunlit room
                    if (ambientDimming > 0f) ambientDimming = (ambientDimming - dt * 0.30f).coerceAtLeast(0f)
                    // Characters awake with cozy idle variety
                    if (boy.reactionTimer <= 0) boy.pose = CharacterPose.SIT
                    if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SIT
                    catSleeping = false
                    if (couchPhaseTimer in 1.0f..1.8f && sceneMessage == null) {
                        showMessage("Bright midday sunshine streams through our window.", duration = 3.0f)
                    }
                }
                CouchPhase.EVENING -> {
                    // Sunset amber tones, trending sleepy
                    if (ambientDimming < 0.25f) ambientDimming = (ambientDimming + dt * 0.15f).coerceAtMost(0.25f)
                    if (couchPhaseTimer > phaseDuration * 0.55f) {
                        // Yawn and stretch, calming down toward night
                        if (boy.reactionTimer <= 0) {
                            boy.pose = CharacterPose.SLEEP_YAWN
                            boy.emotion = CharacterEmotion.SLEEPY
                        }
                        if (girl.reactionTimer <= 0) {
                            girl.pose = CharacterPose.SLEEP_YAWN
                            girl.emotion = CharacterEmotion.SLEEPY
                        }
                    } else {
                        if (boy.reactionTimer <= 0) {
                            boy.pose = CharacterPose.SIT
                            boy.emotion = CharacterEmotion.LOVING
                        }
                        if (girl.reactionTimer <= 0) {
                            girl.pose = CharacterPose.SIT
                            girl.emotion = CharacterEmotion.LOVING
                        }
                    }
                    if (couchPhaseTimer in 1.0f..1.8f && sceneMessage == null) {
                        showMessage("Golden hour twilight — peaceful evening together.", duration = 3.0f)
                    }
                }
            }
        }
    }

    private fun updateWalkScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        if (t < 4.2f) {
            // Both walk in unison across the stone path
            val speed = 0.065f * dt
            if (boy.reactionTimer <= 0 && girl.reactionTimer <= 0) {
                boy.worldX = (boy.worldX + speed).coerceAtMost(0.42f)
                girl.worldX = (girl.worldX + speed).coerceAtMost(0.55f)
            }

            val step = ((t * 7).toInt() % 4)
            boy.walkFrame = step
            girl.walkFrame = step
            val walkPose = when (step) {
                0 -> CharacterPose.WALK_1
                1 -> CharacterPose.WALK_2
                2 -> CharacterPose.WALK_3
                else -> CharacterPose.WALK_4
            }
            if (boy.reactionTimer <= 0) {
                boy.pose = walkPose
                boy.bounceOffset = if (step % 2 == 0) 3f else 0f
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = walkPose
                girl.bounceOffset = if (step % 2 == 0) 3f else 0f
            }

            triggerWalkFootstep((t * 7).toInt())
        } else if (t < 6.5f) {
            // Stop together under streetlamp and look up at the night sky
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.IDLE
                boy.bounceOffset = 0f
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.IDLE
                girl.bounceOffset = 0f
            }
            if (sceneMessage == null) {
                showMessage("Wherever we go, as long as it's together.", duration = 4.5f)
                audio.playStarTwinkle()
                particles.spawnShootingStar(cw * 0.2f, ch * 0.1f)
                particles.spawnSparkles(cw * 0.65f, ch * 0.22f, 8)
            }
        } else {
            // Holding hands under the lantern light
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) boy.pose = CharacterPose.HOLD_HANDS
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) girl.pose = CharacterPose.HOLD_HANDS
            boy.direction = Direction.RIGHT
            girl.direction = Direction.LEFT
            if (Random.nextFloat() < 0.02f) {
                particles.spawnSparkles(cw * 0.48f, ch * 0.62f, 3)
            }
        }
    }

    private fun updateLookingScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        if (t < 2.5f) {
            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.IDLE
            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.IDLE
            boy.direction = Direction.RIGHT
            girl.direction = Direction.LEFT
        } else if (t < 5.0f) {
            // Boy reaches out and gently pats her head
            if (boy.reactionTimer <= 0 && girl.reactionTimer <= 0) {
                boy.worldX = 0.44f
                girl.worldX = 0.56f
            }
            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.HEAD_PAT
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.HEAD_PAT_RECEIVE
                girl.emotion = CharacterEmotion.LOVING
            }
            if (girl.emote == EmoteType.NONE && t > 3.0f) {
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * 0.56f, ch * 0.58f)
            }
        } else if (t < 7.5f) {
            // Hold hands and smile softly
            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.HOLD_HANDS
            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.HOLD_HANDS
            if (sceneMessage == null) {
                showMessage("I love you. Just wanted to look at you.", duration = 4.5f)
                particles.spawnHeart(cw * 0.50f, ch * 0.52f, Color(0xFFFF3366))
            }
        } else {
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) boy.pose = CharacterPose.HOLD_HANDS
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) girl.pose = CharacterPose.HOLD_HANDS
            if (Random.nextFloat() < 0.02f) {
                particles.spawnSparkles(cw * 0.50f, ch * 0.65f, 3)
            }
        }
    }

    private fun updateMomoScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime
        val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)

        // Continuous steam wisps rising softly from the momo steamer
        if (Random.nextFloat() < 0.14f) {
            particles.spawnSteam(cw * 0.44f + (Random.nextFloat() - 0.5f) * 16f, ch * 0.69f - 24f * pixelScale)
        }

        if (t < 2.5f) {
            // Boy offering momo, girl leaning in eagerly
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.FEED_MOMO
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.EAT_MOMO
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.HAPPY
            }
            if (sceneMessage == null && t > 0.8f) {
                showMessage("Welcome to our cozy street food stall!", duration = 3.5f)
            }
        } else if (t < 5.8f) {
            // Girl takes a bite with spicy red chutney!
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.SURPRISED
                girl.emotion = CharacterEmotion.SHY
                if (girl.emote == EmoteType.NONE) {
                    girl.emote = EmoteType.HEART
                    girl.emoteTimer = 2.2f
                    audio.playHeartChime()
                    repeat(4) {
                        particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f)
                    }
                }
            }
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.JOY_JUMP
                boy.emotion = CharacterEmotion.HAPPY
                boy.bounceOffset = 6f
                if (boy.emote == EmoteType.NONE) {
                    boy.emote = EmoteType.MUSIC_NOTE
                    boy.emoteTimer = 2.0f
                }
            }
            if (sceneMessage == null && t > 3.0f) {
                showMessage("Steaming hot momos with extra spicy chutney!", duration = 3.5f)
            }
        } else if (t < 9.5f) {
            // Enjoying together sharing the delicious plate
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.EAT_MOMO
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.EAT_MOMO
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.HAPPY
            }
            if (sceneMessage == null && t > 6.2f) {
                showMessage("Everything tastes sweeter with you.", duration = 4.0f)
                particles.spawnSparkles(cw * 0.5f, ch * 0.65f, 5)
            }
        } else {
            // Sweet idle momo date loop
            if (!boy.isMovingOrTransitioning && boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.FEED_MOMO
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (!girl.isMovingOrTransitioning && girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.EAT_MOMO
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.HAPPY
            }
        }
    }

    private fun updateEveningRideScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime
        // Subtle roadside breeze & wind particles
        if (Random.nextFloat() < 0.16f) {
            particles.spawnWindBreezeStreak(cw + 30f, ch * (0.64f + Random.nextFloat() * 0.14f), cw)
        }

        if (t < 2.5f) {
            if (sceneMessage == null && t > 0.8f) {
                showMessage("Evening scooter ride together through the glowing city", duration = 3.5f)
            }
        } else if (t >= 3.0f && !rideBoySpoke && boySpeechText == null && girlSpeechText == null) {
            rideBoySpoke = true
            boySpeechText = "Holding you forever, my love."
            boySpeechTimer = 3.5f
            boy.emotion = CharacterEmotion.LOVING
            particles.spawnHeart(cw * 0.44f, ch * 0.60f, Color(0xFFFF3366))
        } else if (t >= 6.8f && !rideGirlSpoke && girlSpeechText == null && boySpeechText == null) {
            rideGirlSpoke = true
            girlSpeechText = "Hold tight ${boy.name}! The scooter is fast hehe!"
            girlSpeechTimer = 3.5f
            girl.emotion = CharacterEmotion.HAPPY
            particles.spawnHeart(cw * 0.52f, ch * 0.60f, Color(0xFFFF5D8F))
        }
    }

    private fun updateCozyLoftScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime
        val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)
        val floorY = ch * 0.58f

        // 1. Gentle steam wisps rising from hot mugs on coffee table
        if (Random.nextFloat() < 0.12f) {
            val mugX = cw * 0.50f + 15 * pixelScale + (Random.nextFloat() - 0.5f) * 6f
            particles.spawnSteam(mugX, floorY + 2 * pixelScale)
        }

        // 2. Vinyl music notes floating from turntable
        if (recordSpinning || audio.isPlayingMusic) {
            if (Random.nextFloat() < 0.08f) {
                particles.spawnMusicNote(cw * 0.88f + (Random.nextFloat() - 0.5f) * 16f, floorY - 14 * pixelScale)
            }
        }

        // 3. Occasional shooting star across panoramic window
        if (Random.nextFloat() < 0.007f) {
            val windowStartX = cw * 0.35f
            val starX = windowStartX + Random.nextFloat() * (cw * 0.50f)
            val starY = ch * (0.05f + Random.nextFloat() * 0.20f)
            particles.spawnShootingStar(starX, starY)
        }

        // 4. Romantic progression
        if (t < 2.0f) {
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.SIT
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.LOVING
            }
            if (sceneMessage == null && t > 0.8f) {
                showMessage("Midnight in our cozy loft...", duration = 3.5f)
            }
        } else if (t < 5.5f) {
            if (girl.reactionTimer <= 0) {
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.emotion = CharacterEmotion.LOVING
            }
            if (boy.reactionTimer <= 0) {
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (t in 2.8f..3.0f && girlSpeechText == null && boySpeechText == null) {
                girlSpeechText = "It's so peaceful up here with you."
                girlSpeechTimer = 3.2f
                particles.spawnHeart(cw * 0.62f, ch * 0.50f, Color(0xFFFF758F))
            }
            if (t in 5.2f..5.4f && boySpeechText == null) {
                boySpeechText = "My favorite place in the whole world."
                boySpeechTimer = 3.2f
                particles.spawnHeart(cw * 0.53f, ch * 0.50f, Color(0xFFFF3366))
            }
        } else {
            if (boy.reactionTimer <= 0) {
                boy.worldX = 0.58f
                boy.worldY = 0.575f
                boy.pose = CharacterPose.SIT
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (girl.reactionTimer <= 0) {
                girl.worldX = 0.65f
                girl.worldY = 0.575f
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.LOVING
            }
        }
        if (catState == CatState.SLEEPING) {
            catWorldX = 0.74f
            catWorldY = 0.575f
        }
    }

    fun triggerLightning() {
        lightningFlashAlpha = 0.94f
        lightningTimer = 0f
        nextLightningTime = 12f + Random.nextFloat() * 12f
        audio.playThunder()
    }

    // ── Feature 1: Mochi Matchmaker Engine ──────────────────────────────
    fun canTriggerMochiMatchmaker(): Boolean {
        if (currentScene !in listOf(
            SceneType.FLOWER,
            SceneType.UNDER_TREE,
            SceneType.COOKING,
            SceneType.SLEEP,
            SceneType.WALK,
            SceneType.LOOKING,
            SceneType.MOMO_STALL
        )) return false

        val isScriptActive = when (currentScene) {
            SceneType.FLOWER -> sceneTime < 8.2f
            SceneType.UNDER_TREE -> sceneTime < 7.2f
            SceneType.COOKING -> sceneTime < 7.6f
            SceneType.SLEEP -> sceneTime < 6.9f
            SceneType.WALK -> sceneTime < 6.6f
            SceneType.LOOKING -> sceneTime < 7.6f
            SceneType.MOMO_STALL -> sceneTime < 6.2f
            SceneType.EVENING_RIDE -> sceneTime < 7.0f
            SceneType.COZY_LOFT -> sceneTime < 5.6f
        }
        if (isScriptActive || isWatchSceneActive) return false

        if (boy.reactionTimer > 0f || girl.reactionTimer > 0f ||
            boy.isMovingOrTransitioning || girl.isMovingOrTransitioning ||
            livingRoomReturnTimer > 0f) return false

        val charDistance = kotlin.math.abs(boy.worldX - girl.worldX)
        if (charDistance < 0.20f) return false

        // Living Room sleep-transition gate: lamp off or characters drowsy/sleeping
        if (currentScene == SceneType.SLEEP) {
            if (!livingRoomLampLit ||
                boy.pose in listOf(CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN) ||
                girl.pose in listOf(CharacterPose.SLEEP, CharacterPose.SLEEP_YAWN) ||
                boy.emotion == CharacterEmotion.SLEEPY ||
                girl.emotion == CharacterEmotion.SLEEPY ||
                couchWakeTimer > 0f) {
                return false
            }
        }

        if (catState == CatState.WALK_FOLLOW && (kotlin.math.abs(catTargetX - catWorldX) > 0.02f || kotlin.math.abs(catTargetY - catWorldY) > 0.02f)) {
            return false
        }
        if (mochiMatchmakerCooldown > 0f) return false

        return true
    }

    fun triggerMochiMatchmaker(cw: Float, ch: Float) {
        mochiMatchmakerActive = true
        mochiMatchmakerVariant = mochiMatchmakerPicker.pick()
        mochiMatchmakerStep = 0
        mochiMatchmakerStepTimer = 0f
        catSleeping = false
        setupMochiMatchmakerStep(cw, ch)
    }

    private fun setupMochiMatchmakerStep(cw: Float, ch: Float) {
        val leftX = minOf(boy.worldX, girl.worldX)
        val rightX = maxOf(boy.worldX, girl.worldX)
        val midX = (leftX + rightX) / 2f
        val catY = if (currentScene.environment in listOf(EnvironmentType.LIVING_ROOM, EnvironmentType.KITCHEN)) 0.69f else 0.70f

        when (mochiMatchmakerVariant) {
            0 -> {
                // Variant 0: Invite / Lead
                when (mochiMatchmakerStep) {
                    0 -> {
                        // Walk to left character
                        catTargetX = (leftX + 0.04f).coerceIn(0.08f, 0.92f)
                        catTargetY = catY
                        catFacingLeft = catTargetX < catWorldX
                        catState = CatState.WALK_FOLLOW
                    }
                    1 -> {
                        // Arrived, look up and purr
                        catState = CatState.SITTING_PURR
                        catFacingLeft = false
                        audio.playCatPurr()
                        particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 18f, 4)
                    }
                    2 -> {
                        // Trots halfway toward right character
                        catTargetX = midX.coerceIn(0.08f, 0.92f)
                        catTargetY = catY
                        catFacingLeft = catTargetX < catWorldX
                        catState = CatState.WALK_FOLLOW
                    }
                    3 -> {
                        // Look back at left character, invite
                        catState = CatState.SITTING_PURR
                        catFacingLeft = true
                        particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF85A1))
                    }
                }
            }
            1 -> {
                // Variant 1: Weave / Patrol
                when (mochiMatchmakerStep) {
                    0 -> {
                        // Walk to left character
                        catTargetX = (leftX + 0.04f).coerceIn(0.08f, 0.92f)
                        catTargetY = catY
                        catFacingLeft = catTargetX < catWorldX
                        catState = CatState.WALK_FOLLOW
                    }
                    1 -> {
                        // Arrived at left, heart emote
                        catState = CatState.SITTING_PURR
                        catFacingLeft = false
                        particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF85A1))
                    }
                    2 -> {
                        // Turn and walk all the way to right character
                        catTargetX = (rightX - 0.04f).coerceIn(0.08f, 0.92f)
                        catTargetY = catY
                        catFacingLeft = catTargetX < catWorldX
                        catState = CatState.WALK_FOLLOW
                    }
                    3 -> {
                        // Arrived at right, heart emote
                        catState = CatState.SITTING_PURR
                        catFacingLeft = true
                        particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF85A1))
                    }
                    4 -> {
                        // Walk to center midpoint
                        catTargetX = midX.coerceIn(0.08f, 0.92f)
                        catTargetY = catY
                        catFacingLeft = catTargetX < catWorldX
                        catState = CatState.WALK_FOLLOW
                    }
                    5 -> {
                        // Settle at center
                        catState = CatState.SITTING_PURR
                        audio.playCatPurr()
                        particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 18f, 3)
                    }
                }
            }
            else -> {
                // Variant 2: Center Sit & Look
                when (mochiMatchmakerStep) {
                    0 -> {
                        // Walk directly to midpoint
                        catTargetX = midX.coerceIn(0.08f, 0.92f)
                        catTargetY = catY
                        catFacingLeft = catTargetX < catWorldX
                        catState = CatState.WALK_FOLLOW
                    }
                    1 -> {
                        // Arrived at center, face left toward boy
                        catState = CatState.SITTING_PURR
                        catFacingLeft = true
                        particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 18f, 4)
                    }
                    2 -> {
                        // Turn and face right toward girl
                        catState = CatState.SITTING_PURR
                        catFacingLeft = false
                        audio.playCatPurr()
                        particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF85A1))
                    }
                }
            }
        }
    }

    private fun updateMochiMatchmaker(dt: Float, cw: Float, ch: Float) {
        if (isWatchSceneActive || boy.reactionTimer > 0f || girl.reactionTimer > 0f) {
            mochiMatchmakerActive = false
            catState = CatState.SITTING_PURR
            return
        }

        if (catState == CatState.WALK_FOLLOW) {
            val dx = catTargetX - catWorldX
            val dy = catTargetY - catWorldY
            val dist = kotlin.math.hypot(dx, dy)
            if (dist > 0.006f) {
                catFacingLeft = dx < 0
                val move = catRoamSpeed * dt
                val stepX = if (dist > 0.0001f) (dx / dist) * move else 0f
                val stepY = if (dist > 0.0001f) (dy / dist) * move else 0f
                if (dx > 0) catWorldX = (catWorldX + stepX).coerceAtMost(catTargetX)
                else if (dx < 0) catWorldX = (catWorldX + stepX).coerceAtLeast(catTargetX)
                if (dy > 0) catWorldY = (catWorldY + stepY).coerceAtMost(catTargetY)
                else if (dy < 0) catWorldY = (catWorldY + stepY).coerceAtLeast(catTargetY)
                if (Random.nextFloat() < 0.02f) {
                    particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 5f, 1)
                }
            } else {
                // Waypoint reached: advance to the next step
                catWorldX = catTargetX
                catWorldY = catTargetY
                mochiMatchmakerStep++
                mochiMatchmakerStepTimer = 0f
                setupMochiMatchmakerStep(cw, ch)
            }
        } else {
            // Stationary pause/look phase
            mochiMatchmakerStepTimer += dt
            val pauseThreshold = when (mochiMatchmakerVariant) {
                0 -> if (mochiMatchmakerStep == 1) 1.2f else 1.5f
                1 -> 0.85f
                else -> 1.1f
            }

            if (mochiMatchmakerStepTimer >= pauseThreshold) {
                mochiMatchmakerStep++
                mochiMatchmakerStepTimer = 0f
                val isFinished = when (mochiMatchmakerVariant) {
                    0 -> mochiMatchmakerStep > 3
                    1 -> mochiMatchmakerStep > 5
                    else -> mochiMatchmakerStep > 2
                }
                if (isFinished) {
                    mochiMatchmakerActive = false
                    mochiNudgeActiveTimer = 8.0f
                    mochiMatchmakerCooldown = 32f + Random.nextFloat() * 16f
                    catState = CatState.SITTING_PURR
                } else {
                    setupMochiMatchmakerStep(cw, ch)
                }
            }
        }
    }

    private fun updateCatMovement(dt: Float, cw: Float, ch: Float) {
        if (currentScene == SceneType.EVENING_RIDE) return
        if (currentScene == SceneType.COZY_LOFT) return

        if (mochiMatchmakerActive) {
            updateMochiMatchmaker(dt, cw, ch)
            return
        }

        if (catState == CatState.WALK_FOLLOW) {
            val dx = catTargetX - catWorldX
            val dy = catTargetY - catWorldY
            val dist = kotlin.math.hypot(dx, dy)
            if (dist > 0.005f) {
                catFacingLeft = dx < 0
                val move = catRoamSpeed * dt
                val stepX = if (dist > 0.0001f) (dx / dist) * move else 0f
                val stepY = if (dist > 0.0001f) (dy / dist) * move else 0f
                if (dx > 0) {
                    catWorldX = (catWorldX + stepX).coerceAtMost(catTargetX)
                } else if (dx < 0) {
                    catWorldX = (catWorldX + stepX).coerceAtLeast(catTargetX)
                }
                if (dy > 0) {
                    catWorldY = (catWorldY + stepY).coerceAtMost(catTargetY)
                } else if (dy < 0) {
                    catWorldY = (catWorldY + stepY).coerceAtLeast(catTargetY)
                }
                if (Random.nextFloat() < 0.02f) {
                    particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 5f, 1)
                }
            } else {
                // Arrived at roaming destination! Settle down comfortably
                catWorldX = catTargetX
                catWorldY = catTargetY
                val roll = Random.nextInt(10)
                when {
                    roll < 4 -> {
                        catState = CatState.SITTING_PURR
                        catSleeping = false
                        audio.playCatPurr()
                        particles.spawnMusicNote(cw * catWorldX, ch * catWorldY - 20f)
                    }
                    roll < 7 -> {
                        catState = CatState.BELLY_ROLL
                        catSleeping = false
                        particles.spawnHeart(cw * catWorldX, ch * catWorldY - 22f, Color(0xFFFF85A1))
                    }
                    else -> {
                        catState = CatState.SLEEPING
                        catSleeping = true
                    }
                }
            }
        }
    }

    /**
     * User tapped on the grass or ground to call Mochi over!
     */
    fun commandCatWalkTo(targetX: Float, targetY: Float, cw: Float, ch: Float) {
        if (currentScene == SceneType.EVENING_RIDE || currentScene == SceneType.COZY_LOFT) return
        mochiMatchmakerActive = false
        val isOutdoor = currentScene.environment in listOf(
            EnvironmentType.MEADOW,
            EnvironmentType.TWILIGHT,
            EnvironmentType.TREE_HILL,
            EnvironmentType.PATH_NIGHT,
            EnvironmentType.MOMO_STALL
        )
        val clampedTargetX = targetX.coerceIn(0.08f, 0.92f)
        val clampedTargetY = if (isOutdoor) {
            targetY.coerceIn(0.66f, 0.74f)
        } else {
            targetY.coerceIn(0.66f, 0.72f)
        }

        // Aspect-corrected distance guard: prevent waking Mochi or playing sounds on tiny nudges/taps
        val effCw = if (cw > 0f) cw else 1080f
        val effCh = if (ch > 0f) ch else 2400f
        val dxPixels = (clampedTargetX - catWorldX) * effCw
        val dyPixels = (clampedTargetY - catWorldY) * effCh
        val distPixels = kotlin.math.hypot(dxPixels, dyPixels)
        if (distPixels < MIN_CAT_WALK_THRESHOLD_PIXELS) {
            return
        }

        catTargetX = clampedTargetX
        catTargetY = clampedTargetY
        catFacingLeft = catTargetX < catWorldX
        catState = CatState.WALK_FOLLOW
        catSleeping = false
        audio.playCatPurr()
        particles.spawnHeart(effCw * catWorldX, effCh * catWorldY - 20f, Color(0xFFFF85A1))
        particles.spawnSparkles(effCw * catTargetX, effCh * catTargetY, 4)
        showMessage(if (isOutdoor) "Mochi trots through the grass towards you!" else "Mochi trots across the room to you!", duration = 2.0f)
    }

    private fun triggerAutonomousPetBehavior(cw: Float, ch: Float) {
        if (currentScene == SceneType.COZY_LOFT) {
            val states = listOf(
                CatState.SLEEPING,
                CatState.SITTING_PURR,
                CatState.BELLY_ROLL
            )
            val next = states.random()
            catState = next
            catSleeping = (next == CatState.SLEEPING)
            catWorldX = 0.74f
            catWorldY = 0.575f
            when (next) {
                CatState.SITTING_PURR -> {
                    audio.playCatPurr()
                    particles.spawnMusicNote(cw * catWorldX, ch * catWorldY - 20f)
                }
                CatState.BELLY_ROLL -> {
                    particles.spawnHeart(cw * catWorldX, ch * catWorldY - 22f, Color(0xFFFF85A1))
                }
                CatState.SLEEPING -> {}
                else -> {}
            }
            return
        }

        if (currentScene == SceneType.EVENING_RIDE) return

        // If the cat is currently walking to its destination, let it finish walking
        if (catState == CatState.WALK_FOLLOW && (kotlin.math.abs(catTargetX - catWorldX) > 0.02f || kotlin.math.abs(catTargetY - catWorldY) > 0.02f)) {
            return
        }

        // Cat chooses next mood: 55% chance to get up and roam to a new spot!
        val shouldRoam = Random.nextFloat() < 0.55f
        if (shouldRoam) {
            val isOutdoor = currentScene.environment in listOf(
                EnvironmentType.MEADOW,
                EnvironmentType.TWILIGHT,
                EnvironmentType.TREE_HILL,
                EnvironmentType.PATH_NIGHT,
                EnvironmentType.MOMO_STALL
            )
            val newTargetX = when (currentScene.environment) {
                EnvironmentType.LIVING_ROOM -> {
                    // Living room floor spans between 0.18f (left door) and 0.84f (right floor lamp)
                    val spots = listOf(0.18f, 0.28f, 0.38f, 0.52f, 0.70f, 0.84f)
                    spots.filter { kotlin.math.abs(it - catWorldX) > 0.12f }.randomOrNull() ?: (0.20f + Random.nextFloat() * 0.65f)
                }
                EnvironmentType.KITCHEN -> {
                    val spots = listOf(0.14f, 0.28f, 0.42f, 0.58f, 0.74f, 0.84f)
                    spots.filter { kotlin.math.abs(it - catWorldX) > 0.12f }.randomOrNull() ?: (0.15f + Random.nextFloat() * 0.70f)
                }
                else -> {
                    // Outdoor scenes: trot towards boy, girl, or anywhere across the grass
                    val spots = listOf(
                        (boy.worldX - 0.10f).coerceIn(0.12f, 0.88f),
                        (girl.worldX + 0.10f).coerceIn(0.12f, 0.88f),
                        (boy.worldX + girl.worldX) / 2f,
                        0.12f + Random.nextFloat() * 0.76f
                    )
                    spots.filter { kotlin.math.abs(it - catWorldX) > 0.10f }.randomOrNull() ?: (0.12f + Random.nextFloat() * 0.76f)
                }
            }
            catTargetX = newTargetX.coerceIn(0.10f, 0.90f)
            catTargetY = if (isOutdoor) {
                0.67f + Random.nextFloat() * 0.07f
            } else {
                0.67f + Random.nextFloat() * 0.04f
            }
            catFacingLeft = catTargetX < catWorldX
            catState = CatState.WALK_FOLLOW
            catSleeping = false
            return
        }

        // Otherwise stationary playful action at current spot
        val states = listOf(
            CatState.SITTING_PURR,
            CatState.BELLY_ROLL,
            CatState.PLAYFUL_POUNCE,
            CatState.SLEEPING
        )
        val next = states.random()
        catState = next
        catSleeping = (next == CatState.SLEEPING)
        when (next) {
            CatState.SITTING_PURR -> {
                audio.playCatPurr()
                particles.spawnMusicNote(cw * catWorldX, ch * catWorldY - 20f)
            }
            CatState.BELLY_ROLL -> {
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 22f, Color(0xFFFF85A1))
            }
            CatState.PLAYFUL_POUNCE -> {
                audio.playBubblePop()
                particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 20f, 4)
            }
            CatState.SLEEPING -> {}
            else -> {}
        }
    }

    // Autonomous Spontaneous Affectionate Moments — scene-aware (Feature 2 & 8)
    private fun triggerAutonomousMoment(cw: Float, ch: Float) {
        nextAutonomousInterval = 4.5f + Random.nextFloat() * 3.5f

        // Scenes with no autonomous walking
        if (currentScene == SceneType.COZY_LOFT || currentScene == SceneType.EVENING_RIDE) {
            return
        }

        // Scene-specific autonomous moments (Feature 8)
        when (currentScene) {
            SceneType.FLOWER -> {
                when (meadowAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Boy walks to flower, kneels to pick it, offers to girl
                        val targetX = 0.48f
                        val walkTime = abs(targetX - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        boy.moveTo(targetX, arrivePose = CharacterPose.GIVE_FLOWER)
                        boy.direction = Direction.RIGHT
                        boy.emotion = CharacterEmotion.LOVING
                        girl.direction = Direction.LEFT
                        girl.transitionPoseTo(CharacterPose.RECEIVE_FLOWER)
                        girl.emotion = CharacterEmotion.SHY
                        boy.reactionTimer = walkTime + 3.0f
                        girl.reactionTimer = walkTime + 3.0f
                        audio.playHeartChime()
                        particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 28f)
                        showMessage("A tiny flower just for you.", duration = 3.0f)
                    }
                    1 -> {
                        // 2. Girl takes a few joyful steps across meadow
                        val targetX = 0.65f
                        val walkTime = abs(targetX - girl.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        girl.moveTo(targetX, arrivePose = CharacterPose.IDLE)
                        girl.direction = Direction.RIGHT
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.emote = EmoteType.SPARKLE
                        girl.emoteTimer = walkTime + 2.0f
                        girl.reactionTimer = walkTime + 2.5f
                        audio.playStarTwinkle()
                        particles.spawnPetals(cw * 0.65f, ch * 0.62f, 8)
                    }
                    2 -> {
                        // 3. Close hand-holding with gentle sway and petal shower
                        boy.moveTo(0.44f, arrivePose = CharacterPose.HOLD_HANDS)
                        girl.moveTo(0.56f, arrivePose = CharacterPose.RECEIVE_FLOWER)
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.LOVING
                        boy.reactionTimer = 3.0f
                        girl.reactionTimer = 3.0f
                        audio.playLeafRustle()
                        particles.spawnPetals(cw * 0.50f, ch * 0.55f, 10)
                    }
                    3 -> {
                        // 4. Loving glance and blush
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.2f
                        girl.emotion = CharacterEmotion.SHY
                        girl.emote = EmoteType.BLUSH
                        girl.emoteTimer = 2.2f
                        boy.reactionTimer = 2.5f
                        girl.reactionTimer = 2.5f
                        audio.playHeartChime()
                    }
                }
                return
            }
            SceneType.UNDER_TREE -> {
                when (treeAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Bench shift — lean closer into warm snuggle
                        boy.moveTo(0.46f, arrivePose = CharacterPose.SIT_SNUGGLE)
                        girl.moveTo(0.54f, arrivePose = CharacterPose.SIT_SNUGGLE)
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.LOVING
                        boy.reactionTimer = 3.0f
                        girl.reactionTimer = 3.0f
                        audio.playLeafRustle()
                        particles.spawnLeaf(cw * 0.5f, ch * 0.38f)
                    }
                    1 -> {
                        // 2. Gaze up at tree canopy together
                        boy.direction = Direction.LEFT
                        girl.direction = Direction.RIGHT
                        boy.emotion = CharacterEmotion.CURIOUS
                        girl.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.DOTS
                        boy.emoteTimer = 2.2f
                        girl.emote = EmoteType.MUSIC_NOTE
                        girl.emoteTimer = 2.2f
                        boy.reactionTimer = 2.5f
                        girl.reactionTimer = 2.5f
                        audio.playLeafRustle()
                        repeat(3) { particles.spawnLeaf(cw * (0.4f + Random.nextFloat() * 0.2f), ch * 0.30f) }
                    }
                    2 -> {
                        // 3. Gentle head pat on bench
                        boy.transitionPoseTo(CharacterPose.HEAD_PAT)
                        girl.transitionPoseTo(CharacterPose.HEAD_PAT_RECEIVE)
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.HAPPY
                        audio.playHeartChime()
                        particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 26f)
                    }
                    3 -> {
                        // 4. Sweet whisper on bench
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emote = EmoteType.MUSIC_NOTE
                        boy.emoteTimer = 2.0f
                        girl.emote = EmoteType.HEART
                        girl.emoteTimer = 2.0f
                        boy.reactionTimer = 2.5f
                        girl.reactionTimer = 2.5f
                        audio.playStarTwinkle()
                    }
                }
                return
            }
            SceneType.SLEEP -> {
                // Living Room Autonomous Behaviors (lamp is NEVER touched)
                when (livingRoomAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Wardrobe check: boy walks to wardrobe, inspects cozy clothes, walks back
                        val targetX = 0.20f
                        val walkTime = abs(targetX - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        val returnWalkTime = abs(0.44f - targetX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        boy.moveTo(targetX, arrivePose = CharacterPose.IDLE)
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.SPARKLE
                        boy.emoteTimer = walkTime + 2.0f
                        boy.reactionTimer = walkTime + 2.0f + returnWalkTime + 0.5f
                        livingRoomReturnTimer = walkTime + 2.0f
                        livingRoomReturnTargetX = 0.44f
                        livingRoomReturnChar = boy
                        audio.playStarTwinkle()
                        particles.spawnSparkles(cw * 0.20f, ch * 0.58f, 5)
                    }
                    1 -> {
                        // 2. Photo frame gaze: girl walks to photo frame, gazes lovingly, walks back
                        val targetX = 0.68f
                        val walkTime = abs(targetX - girl.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        val returnWalkTime = abs(0.54f - targetX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        girl.moveTo(targetX, arrivePose = CharacterPose.IDLE)
                        girl.emotion = CharacterEmotion.LOVING
                        girl.emote = EmoteType.HEART
                        girl.emoteTimer = walkTime + 2.2f
                        girl.reactionTimer = walkTime + 2.2f + returnWalkTime + 0.5f
                        livingRoomReturnTimer = walkTime + 2.2f
                        livingRoomReturnTargetX = 0.54f
                        livingRoomReturnChar = girl
                        audio.playHeartChime()
                        particles.spawnHeart(cw * 0.68f, ch * 0.55f, Color(0xFFFF758F))
                    }
                    2 -> {
                        // 3. Wall calendar glance: boy walks to calendar, smiles, walks back
                        val targetX = 0.32f
                        val walkTime = abs(targetX - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        val returnWalkTime = abs(0.44f - targetX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        boy.moveTo(targetX, arrivePose = CharacterPose.IDLE)
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.MUSIC_NOTE
                        boy.emoteTimer = walkTime + 2.0f
                        boy.reactionTimer = walkTime + 2.0f + returnWalkTime + 0.5f
                        livingRoomReturnTimer = walkTime + 2.0f
                        livingRoomReturnTargetX = 0.44f
                        livingRoomReturnChar = boy
                        audio.playBubblePop()
                        particles.spawnSparkles(cw * 0.32f, ch * 0.54f, 4)
                    }
                    3 -> {
                        // 4. Couch Snuggle: boy scoots close and both cuddle
                        boy.transitionPoseTo(CharacterPose.SIT_SNUGGLE)
                        girl.transitionPoseTo(CharacterPose.SIT_SNUGGLE)
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.LOVING
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.5f
                        girl.emote = EmoteType.BLUSH
                        girl.emoteTimer = 2.5f
                        boy.reactionTimer = 3.5f
                        girl.reactionTimer = 3.5f
                        audio.playHeartChime()
                        particles.spawnHeart(cw * 0.49f, ch * 0.58f, Color(0xFFFF5D8F))
                    }
                    4 -> {
                        // 5. Couch stretch & yawn
                        boy.transitionPoseTo(CharacterPose.SLEEP_YAWN)
                        boy.emotion = CharacterEmotion.SLEEPY
                        boy.isBlinking = true
                        girl.direction = Direction.LEFT
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.emote = EmoteType.SPARKLE
                        girl.emoteTimer = 2.0f
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        audio.playStarTwinkle()
                    }
                }
                return
            }
            SceneType.WALK -> {
                when (walkAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Lantern pause & sweet blush
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.transitionPoseTo(CharacterPose.IDLE)
                        girl.transitionPoseTo(CharacterPose.IDLE)
                        boy.emotion = CharacterEmotion.HAPPY
                        girl.emotion = CharacterEmotion.SHY
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.2f
                        girl.emote = EmoteType.BLUSH
                        girl.emoteTimer = 2.2f
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        audio.playHeartChime()
                    }
                    1 -> {
                        // 2. Synchronized stroll step
                        val boyTarget = (boy.worldX + 0.05f).coerceAtMost(0.48f)
                        val girlTarget = (girl.worldX + 0.05f).coerceAtMost(0.62f)
                        boy.moveTo(boyTarget, arrivePose = CharacterPose.HOLD_HANDS)
                        girl.moveTo(girlTarget, arrivePose = CharacterPose.HOLD_HANDS)
                        val walkTime = abs(boyTarget - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        boy.reactionTimer = walkTime + 1.0f
                        girl.reactionTimer = walkTime + 1.0f
                        audio.playFootstep()
                    }
                    2 -> {
                        // 3. Starlight & firefly gaze
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.CURIOUS
                        girl.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.SPARKLE
                        boy.emoteTimer = 2.0f
                        girl.emote = EmoteType.MUSIC_NOTE
                        girl.emoteTimer = 2.0f
                        boy.reactionTimer = 2.5f
                        girl.reactionTimer = 2.5f
                        audio.playStarTwinkle()
                        particles.spawnSparkles(cw * 0.50f, ch * 0.55f, 6)
                    }
                    3 -> {
                        // 4. Joyful hop
                        boy.transitionPoseTo(CharacterPose.JOY_JUMP)
                        boy.bounceOffset = 6f
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.MUSIC_NOTE
                        boy.emoteTimer = 1.8f
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.emote = EmoteType.HEART
                        girl.emoteTimer = 1.8f
                        boy.reactionTimer = 2.2f
                        girl.reactionTimer = 2.2f
                        audio.playBubblePop()
                    }
                }
                return
            }
            SceneType.MOMO_STALL -> {
                when (momoAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Boy feeds momo to girl
                        boy.transitionPoseTo(CharacterPose.FEED_MOMO)
                        girl.transitionPoseTo(CharacterPose.EAT_MOMO)
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.HAPPY
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        audio.playCookingBubbles()
                        repeat(4) { particles.spawnSteam(cw * 0.44f, ch * 0.62f) }
                        particles.spawnHeart(cw * 0.50f, ch * 0.60f)
                    }
                    1 -> {
                        // 2. Spicy chutney reaction!
                        girl.transitionPoseTo(CharacterPose.SURPRISED)
                        girl.emotion = CharacterEmotion.SURPRISED
                        girl.emote = EmoteType.EXCLAMATION
                        girl.emoteTimer = 2.0f
                        girl.reactionTimer = 2.2f
                        boy.transitionPoseTo(CharacterPose.JOY_JUMP)
                        boy.bounceOffset = 5f
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.0f
                        boy.reactionTimer = 2.2f
                        audio.playBubblePop()
                        showMessage("Extra spicy red chutney! ${boy.name} laughs happily.", duration = 3.0f)
                    }
                    2 -> {
                        // 3. Girl feeds momo to boy
                        girl.direction = Direction.LEFT
                        girl.transitionPoseTo(CharacterPose.FEED_MOMO)
                        girl.emotion = CharacterEmotion.LOVING
                        boy.direction = Direction.RIGHT
                        boy.transitionPoseTo(CharacterPose.EAT_MOMO)
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        audio.playHeartChime()
                        particles.spawnHeart(cw * 0.50f, ch * 0.60f, Color(0xFFFF5D8F))
                    }
                    3 -> {
                        // 4. Cheers / clinking toast
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.HAPPY
                        girl.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.MUSIC_NOTE
                        boy.emoteTimer = 2.0f
                        girl.emote = EmoteType.SPARKLE
                        girl.emoteTimer = 2.0f
                        boy.reactionTimer = 2.5f
                        girl.reactionTimer = 2.5f
                        audio.playBubblePop()
                        particles.spawnSparkles(cw * 0.50f, ch * 0.62f, 5)
                    }
                }
                return
            }
            SceneType.LOOKING -> {
                when (lookingAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Point at shooting star
                        boy.direction = Direction.LEFT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.CURIOUS
                        girl.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.SPARKLE
                        boy.emoteTimer = 2.2f
                        girl.emote = EmoteType.HEART
                        girl.emoteTimer = 2.2f
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        audio.playStarTwinkle()
                        particles.spawnShootingStar(cw * 0.3f, ch * 0.15f)
                    }
                    1 -> {
                        // 2. Gentle head pat
                        boy.transitionPoseTo(CharacterPose.HEAD_PAT)
                        girl.transitionPoseTo(CharacterPose.HEAD_PAT_RECEIVE)
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.HAPPY
                        boy.reactionTimer = 2.8f
                        girl.reactionTimer = 2.8f
                        audio.playHeartChime()
                        particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 28f)
                    }
                    2 -> {
                        // 3. Starlight cuddle
                        boy.transitionPoseTo(CharacterPose.SIT_SNUGGLE)
                        girl.transitionPoseTo(CharacterPose.SIT_SNUGGLE)
                        boy.emotion = CharacterEmotion.LOVING
                        girl.emotion = CharacterEmotion.LOVING
                        boy.reactionTimer = 3.2f
                        girl.reactionTimer = 3.2f
                        audio.playHeartChime()
                        particles.spawnHeart(cw * 0.50f, ch * 0.55f, Color(0xFFFF5D8F))
                    }
                    3 -> {
                        // 4. Glance & blush
                        boy.direction = Direction.RIGHT
                        girl.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.2f
                        girl.emotion = CharacterEmotion.SHY
                        girl.emote = EmoteType.BLUSH
                        girl.emoteTimer = 2.2f
                        boy.reactionTimer = 2.5f
                        girl.reactionTimer = 2.5f
                        audio.playStarTwinkle()
                    }
                }
                return
            }
            SceneType.COOKING -> {
                when (cookingAutonomousPicker.pick()) {
                    0 -> {
                        // 1. Stir pot on stove & tender moment
                        girl.moveTo(0.58f)
                        girl.transitionPoseTo(CharacterPose.COOK)
                        girl.direction = Direction.RIGHT
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.emote = EmoteType.SPARKLE
                        girl.emoteTimer = 2.5f
                        girl.reactionTimer = 3.5f
                        boy.moveTo(0.46f)
                        boy.transitionPoseTo(CharacterPose.IDLE)
                        boy.direction = Direction.RIGHT
                        boy.emotion = CharacterEmotion.LOVING
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.0f
                        boy.reactionTimer = 3.5f
                        audio.playCookingBubbles()
                        repeat(6) { particles.spawnSteam(cw * 0.58f, ch * 0.60f) }
                        particles.spawnHeart(cw * 0.58f, ch * 0.56f, Color(0xFFFF758F))
                    }
                    1 -> {
                        // 2. Wipe counter & farmhouse apron sink (self-resolves via sinkRunningTimer countdown)
                        val girlWalk = abs(0.32f - girl.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        sinkRunningTimer = girlWalk + 2.5f
                        girl.moveTo(0.32f)
                        girl.transitionPoseTo(CharacterPose.IDLE)
                        girl.direction = Direction.LEFT
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.emote = EmoteType.SPARKLE
                        girl.emoteTimer = 2.0f
                        girl.reactionTimer = sinkRunningTimer + 0.5f
                        boy.moveTo(0.48f)
                        boy.transitionPoseTo(CharacterPose.IDLE)
                        boy.direction = Direction.LEFT
                        boy.emotion = CharacterEmotion.LOVING
                        boy.reactionTimer = 3.2f
                        audio.playBubblePop()
                        repeat(5) { particles.spawnSteam(cw * 0.28f, ch * 0.58f) }
                        particles.spawnSparkles(cw * 0.28f, ch * 0.56f, 5)
                    }
                    2 -> {
                        // 3. Open & glance in the fridge (self-resolves via fridgeDoorOpenTimer countdown)
                        val boyWalk = abs(0.78f - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        fridgeDoorOpenTimer = boyWalk + 2.5f
                        boy.moveTo(0.78f)
                        boy.transitionPoseTo(CharacterPose.JOY_JUMP)
                        boy.direction = Direction.RIGHT
                        boy.emotion = CharacterEmotion.CURIOUS
                        boy.emote = EmoteType.MUSIC_NOTE
                        boy.emoteTimer = 2.0f
                        boy.reactionTimer = fridgeDoorOpenTimer + 0.5f
                        girl.direction = Direction.RIGHT
                        girl.emotion = CharacterEmotion.PLAYFUL
                        girl.reactionTimer = 2.8f
                        audio.playStarTwinkle()
                        particles.spawnSparkles(cw * 0.88f, ch * 0.52f, 5)
                        particles.spawnHeart(cw * 0.88f, ch * 0.48f, Color(0xFFFF85A1))
                    }
                    3 -> {
                        // 4. Check the oven / warm cookies (cabinet, self-resolves via cabinetOpenTimer countdown)
                        val boyWalk = abs(0.58f - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                        cabinetOpenTimer = boyWalk + 2.5f
                        boy.moveTo(0.58f)
                        boy.transitionPoseTo(CharacterPose.COOK)
                        boy.direction = Direction.RIGHT
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.SPARKLE
                        boy.emoteTimer = 2.0f
                        boy.reactionTimer = cabinetOpenTimer + 0.5f
                        girl.moveTo(0.46f)
                        girl.transitionPoseTo(CharacterPose.IDLE)
                        girl.direction = Direction.RIGHT
                        girl.emotion = CharacterEmotion.HAPPY
                        girl.reactionTimer = 3.2f
                        audio.playCookingBubbles()
                        repeat(5) { particles.spawnSteam(cw * 0.62f, ch * 0.62f) }
                        particles.spawnHeart(cw * 0.62f, ch * 0.58f, Color(0xFFFFD166))
                    }
                    4 -> {
                        // 5. Taste-test spoonful of food together
                        boy.moveTo(0.46f)
                        girl.moveTo(0.54f)
                        girl.direction = Direction.LEFT
                        girl.transitionPoseTo(CharacterPose.FEED_MOMO)
                        girl.emotion = CharacterEmotion.LOVING
                        girl.emote = EmoteType.HEART
                        girl.emoteTimer = 2.2f
                        girl.reactionTimer = 3.2f
                        boy.direction = Direction.RIGHT
                        boy.transitionPoseTo(CharacterPose.EAT_MOMO)
                        boy.emotion = CharacterEmotion.HAPPY
                        boy.emote = EmoteType.MUSIC_NOTE
                        boy.emoteTimer = 2.2f
                        boy.reactionTimer = 3.2f
                        audio.playHeartChime()
                        particles.spawnHeart(cw * 0.50f, ch * 0.58f, Color(0xFFFF5D8F))
                    }
                    5 -> {
                        // 6. Glance at each other and smile mid-task
                        girl.moveTo(0.58f)
                        girl.transitionPoseTo(CharacterPose.IDLE)
                        girl.direction = Direction.LEFT
                        girl.emotion = CharacterEmotion.SHY
                        girl.emote = EmoteType.BLUSH
                        girl.emoteTimer = 2.2f
                        girl.reactionTimer = 3.0f
                        boy.moveTo(0.44f)
                        boy.transitionPoseTo(CharacterPose.IDLE)
                        boy.direction = Direction.RIGHT
                        boy.emotion = CharacterEmotion.LOVING
                        boy.emote = EmoteType.HEART
                        boy.emoteTimer = 2.2f
                        boy.reactionTimer = 3.0f
                        audio.playStarTwinkle()
                        particles.spawnHeart(cw * 0.51f, ch * 0.58f, Color(0xFFFF758F))
                    }
                }
                return
            }
            else -> { /* fall through to generic */ }
        }

        triggerGenericAutonomousMoment(cw, ch)
    }

    // Generic cross-scene autonomous moments (posture shifts, head turns, affectionate moments)
    private fun triggerGenericAutonomousMoment(cw: Float, ch: Float) {
        val rand = Random.nextInt(6)
        when (rand) {
            0 -> {
                // Boyfriend turns and blows a kiss
                boy.direction = Direction.RIGHT
                boy.reactionTimer = 2.2f
                boy.emote = EmoteType.KISS
                boy.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * boy.worldX + 15f, ch * boy.worldY - 20f)
                girl.emotion = CharacterEmotion.SHY
                girl.reactionTimer = 2.0f
            }
            1 -> {
                // Girlfriend waves happily and hops
                girl.pose = CharacterPose.JOY_JUMP
                girl.reactionTimer = 2.2f
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                girl.bounceOffset = 8f
                audio.playBubblePop()
                particles.spawnSparkles(cw * girl.worldX, ch * girl.worldY - 20f, 4)
            }
            2 -> {
                // Gentle head pat
                if (abs(boy.worldX - girl.worldX) < 0.28f) {
                    boy.pose = CharacterPose.HEAD_PAT
                    girl.pose = CharacterPose.HEAD_PAT_RECEIVE
                    boy.reactionTimer = 2.8f
                    girl.reactionTimer = 2.8f
                    audio.playHeartChime()
                    particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f)
                } else {
                    boy.emote = EmoteType.HEART
                    boy.emoteTimer = 2.0f
                    boy.reactionTimer = 2.0f
                    audio.playHeartChime()
                }
            }
            3 -> {
                // Mutual melody moment
                boy.reactionTimer = 2.0f
                girl.reactionTimer = 2.0f
                boy.emote = EmoteType.MUSIC_NOTE
                boy.emoteTimer = 2.0f
                girl.emote = EmoteType.MUSIC_NOTE
                girl.emoteTimer = 2.0f
                audio.playStarTwinkle()
            }
            4 -> {
                // Sweet spontaneous kiss or affectionate wave
                if (abs(boy.worldX - girl.worldX) < 0.28f) {
                    val kissMidX = (boy.worldX + girl.worldX) / 2f
                    boy.worldX = kissMidX - 0.02f
                    girl.worldX = kissMidX + 0.02f
                    boy.direction = Direction.RIGHT
                    girl.direction = Direction.LEFT
                    boy.pose = CharacterPose.KISS
                    girl.pose = CharacterPose.KISS
                    boy.emotion = CharacterEmotion.LOVING
                    girl.emotion = CharacterEmotion.LOVING
                    boy.reactionTimer = 3.0f
                    girl.reactionTimer = 3.0f
                    audio.playHeartChime()
                    particles.spawnHeart(cw * kissMidX, ch * boy.worldY - 30f)
                    showMessage("A sweet surprise kiss", duration = 3.0f)
                } else {
                    boy.pose = CharacterPose.WAVE
                    boy.emote = EmoteType.HEART
                    boy.emoteTimer = 2.0f
                    boy.reactionTimer = 2.0f
                    girl.pose = CharacterPose.JOY_JUMP
                    girl.emote = EmoteType.BLUSH
                    girl.emoteTimer = 2.0f
                    girl.reactionTimer = 2.0f
                    audio.playHeartChime()
                }
            }
            5 -> {
                // Spontaneous cozy thought / weather-reactive dialogue
                boy.direction = Direction.RIGHT
                girl.direction = Direction.LEFT
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.HAPPY
                boy.reactionTimer = 2.5f
                girl.reactionTimer = 2.5f
                if (sceneMessage == null) {
                    val thought = pickSpontaneousThought()
                    showMessage(thought, duration = 3.8f)
                    audio.playHeartChime()
                    particles.spawnHeart(cw * (boy.worldX + girl.worldX) / 2f, ch * boy.worldY - 24f)
                }
            }
        }
    }

    // Touch Interactions
    fun onTouchBoy(cw: Float, ch: Float) {
        if (mochiNudgeActiveTimer > 0f && kotlin.math.abs(boy.worldX - girl.worldX) >= 0.16f) {
            mochiNudgeActiveTimer = 0f
            val stepDir = if (girl.worldX > boy.worldX) 1f else -1f
            boy.worldX = (boy.worldX + stepDir * 0.05f).coerceIn(0.15f, 0.85f)
            boy.direction = if (stepDir > 0) Direction.RIGHT else Direction.LEFT
            boy.reactionTimer = 2.8f
            boy.pose = CharacterPose.JOY_JUMP
            boy.emotion = CharacterEmotion.LOVING
            boy.emote = EmoteType.HEART
            boy.emoteTimer = 2.5f
            audio.playHeartChime()
            particles.spawnHeart(cw * boy.worldX, ch * boy.worldY - 30f)
            boySpeechText = "Coming closer to you!"
            boySpeechTimer = 3.0f
            return
        }
        boy.reactionTimer = 2.8f
        val idx = boyTapPicker.pick()
        when (idx) {
            0 -> {
                boy.pose = CharacterPose.JOY_JUMP
                boy.emote = EmoteType.HEART
                boy.emoteTimer = 2.0f
                boy.bounceOffset = 9f
                audio.playBubblePop()
                particles.spawnHeart(cw * boy.worldX, ch * boy.worldY - 30f)
                boySpeechText = "Hey gorgeous!"
                boySpeechTimer = 3.2f
            }
            1 -> {
                boy.pose = CharacterPose.WAVE
                boy.emote = EmoteType.MUSIC_NOTE
                boy.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnSparkles(cw * boy.worldX, ch * boy.worldY - 30f, 6)
                boySpeechText = "My favorite person in the world."
                boySpeechTimer = 3.2f
            }
            2 -> {
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.SHY
                boy.emote = EmoteType.BLUSH
                boy.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * boy.worldX, ch * boy.worldY - 30f, Color(0xFFFF758F))
                boySpeechText = "You make my whole world brighter."
                boySpeechTimer = 3.2f
            }
            3 -> {
                boy.pose = CharacterPose.JOY_JUMP
                boy.emote = EmoteType.HEART
                boy.emoteTimer = 2.0f
                boy.bounceOffset = 6f
                audio.playBubblePop()
                particles.spawnHeart(cw * boy.worldX, ch * boy.worldY - 30f)
                boySpeechText = com.example.data.ProfileManager.getProfile().boyTapWhispers.firstOrNull() ?: "You are my entire world"
                boySpeechTimer = 3.5f
            }
            4 -> {
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.HEART
                boy.emoteTimer = 2.0f
                audio.playHeartChime()
                boySpeechText = "Still completely head over heels for you."
                boySpeechTimer = 3.2f
            }
        }
    }

    fun onTouchGirl(cw: Float, ch: Float) {
        if (mochiNudgeActiveTimer > 0f && kotlin.math.abs(boy.worldX - girl.worldX) >= 0.16f) {
            mochiNudgeActiveTimer = 0f
            val stepDir = if (boy.worldX > girl.worldX) 1f else -1f
            girl.worldX = (girl.worldX + stepDir * 0.05f).coerceIn(0.15f, 0.85f)
            girl.direction = if (stepDir > 0) Direction.RIGHT else Direction.LEFT
            girl.reactionTimer = 2.8f
            girl.pose = CharacterPose.JOY_JUMP
            girl.emotion = CharacterEmotion.HAPPY
            girl.emote = EmoteType.HEART
            girl.emoteTimer = 2.5f
            audio.playHeartChime()
            particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f)
            girlSpeechText = "Getting closer to you!"
            girlSpeechTimer = 3.0f
            return
        }
        girl.reactionTimer = 2.8f
        val idx = girlTapPicker.pick()
        when (idx) {
            0 -> {
                girl.pose = CharacterPose.JOY_JUMP
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                girl.bounceOffset = 9f
                audio.playHeartChime()
                particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f)
                girlSpeechText = "${boy.name}!"
                girlSpeechTimer = 3.0f
            }
            1 -> {
                girl.pose = CharacterPose.WAVE
                girl.emote = EmoteType.SPARKLE
                girl.emoteTimer = 2.0f
                audio.playStarTwinkle()
                particles.spawnSparkles(cw * girl.worldX, ch * girl.worldY - 30f, 6)
                girlSpeechText = "My heart feels so full with you."
                girlSpeechTimer = 3.2f
            }
            2 -> {
                girl.pose = CharacterPose.IDLE
                girl.emotion = CharacterEmotion.SHY
                girl.emote = EmoteType.BLUSH
                girl.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f, Color(0xFFFF5D8F))
                girlSpeechText = com.example.data.ProfileManager.getProfile().girlTapWhispers.firstOrNull() ?: "i love you with all my heart"
                girlSpeechTimer = 3.5f
            }
            3 -> {
                girl.pose = CharacterPose.JOY_JUMP
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                girl.bounceOffset = 6f
                audio.playBubblePop()
                girlSpeechText = "Warmest cuddles only with you."
                girlSpeechTimer = 3.2f
            }
            4 -> {
                girl.pose = CharacterPose.IDLE
                girl.emotion = CharacterEmotion.LOVING
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f, Color(0xFFFF85A1))
                girlSpeechText = "Forever by your side."
                girlSpeechTimer = 3.2f
            }
        }
    }

    /**
     * Double tap easter eggs:
     * Boy and girl speak sweet affectionate whispers
     */
    fun onDoubleTapBoy(cw: Float, ch: Float) {
        val whispers = com.example.data.ProfileManager.getProfile().boyTapWhispers.ifEmpty {
            listOf(
                "You make me the happiest person alive",
                "i love u ${girl.name}",
                "My heart belongs to you",
                "Forever your ${boy.name}"
            )
        }
        val chosen = whispers.random()
        boySpeechText = chosen
        boySpeechTimer = 4.5f
        boy.reactionTimer = 4.0f
        boy.pose = CharacterPose.JOY_JUMP
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 3.5f
        boy.bounceOffset = 12f
        audio.playHeartChime()
        repeat(8) {
            particles.spawnHeart(cw * boy.worldX + (Random.nextFloat() - 0.5f) * 24f, ch * boy.worldY - 35f)
        }
        showMessage("${boy.name}: $chosen", duration = 3.5f)
    }

    fun onDoubleTapGirl(cw: Float, ch: Float) {
        val whispers = com.example.data.ProfileManager.getProfile().girlTapWhispers.ifEmpty {
            listOf(
                "i love you so much",
                "My ${boy.name}... my whole heart",
                "${girl.name} loves ${boy.name} forever"
            )
        }
        val chosen = whispers.random()
        girlSpeechText = chosen
        girlSpeechTimer = 4.5f
        girl.reactionTimer = 4.0f
        girl.pose = CharacterPose.JOY_JUMP
        girl.emotion = CharacterEmotion.SHY
        girl.emote = EmoteType.BLUSH
        girl.emoteTimer = 3.5f
        girl.bounceOffset = 12f
        audio.playHeartChime()
        repeat(8) {
            particles.spawnHeart(cw * girl.worldX + (Random.nextFloat() - 0.5f) * 24f, ch * girl.worldY - 35f, Color(0xFFFF5D8F))
        }
        showMessage("${girl.name}: $chosen", duration = 3.5f)
    }

    /**
     * Tapping both characters or between them triggers an affectionate couple hug!
     */
    fun onTouchBothCharacters(cw: Float, ch: Float) {
        val midX = if (currentScene == SceneType.COZY_LOFT) 0.615f else (boy.worldX + girl.worldX) / 2f
        val idx = hugPicker.pick()
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        boy.reactionTimer = 4.0f
        girl.reactionTimer = 4.0f
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING

        when (idx) {
            0 -> {
                // Classic Warm Snuggle Hug
                boy.worldX = midX - 0.02f
                girl.worldX = midX + 0.02f
                boy.pose = CharacterPose.HUG
                girl.pose = CharacterPose.HUG
                boy.emote = EmoteType.HEART
                girl.emote = EmoteType.HEART
                boy.emoteTimer = 3.5f
                girl.emoteTimer = 3.5f
                audio.playHeartChime()
                repeat(8) {
                    particles.spawnHeart(cw * midX + (Random.nextFloat() - 0.5f) * 32f, ch * boy.worldY - 28f)
                }
                showMessage("Always safe in your arms.", duration = 3.5f)
            }
            1 -> {
                // Lift & Spin Hug
                boy.worldX = midX - 0.02f
                girl.worldX = midX + 0.02f
                boy.pose = CharacterPose.HUG
                girl.pose = CharacterPose.JOY_JUMP
                boy.bounceOffset = 4f
                girl.bounceOffset = 9f
                boy.emote = EmoteType.HEART
                girl.emote = EmoteType.SPARKLE
                boy.emoteTimer = 3.5f
                girl.emoteTimer = 3.5f
                audio.playBubblePop()
                audio.playHeartChime()
                repeat(10) {
                    particles.spawnHeart(cw * midX + (Random.nextFloat() - 0.5f) * 36f, ch * boy.worldY - 32f)
                    particles.spawnSparkles(cw * midX + (Random.nextFloat() - 0.5f) * 36f, ch * boy.worldY - 26f, 1)
                }
                showMessage("Spinning around with my whole world.", duration = 3.5f)
            }
            2 -> {
                // Gentle Forehead Touch / Soft Cuddle
                boy.worldX = midX - 0.02f
                girl.worldX = midX + 0.02f
                boy.pose = CharacterPose.SIT_SNUGGLE
                girl.pose = CharacterPose.SIT_SNUGGLE
                boy.emote = EmoteType.BLUSH
                girl.emote = EmoteType.BLUSH
                boy.emoteTimer = 3.5f
                girl.emoteTimer = 3.5f
                audio.playHeartChime()
                repeat(6) {
                    particles.spawnHeart(cw * midX + (Random.nextFloat() - 0.5f) * 20f, ch * boy.worldY - 25f, Color(0xFFFFCAD4))
                }
                showMessage("Listening to your heartbeat...", duration = 3.5f)
            }
            3 -> {
                // Sweet Forehead Kiss
                boy.worldX = midX - 0.02f
                girl.worldX = midX + 0.02f
                boy.pose = CharacterPose.KISS
                girl.pose = CharacterPose.KISS
                boy.emote = EmoteType.HEART
                girl.emote = EmoteType.BLUSH
                boy.emoteTimer = 3.5f
                girl.emoteTimer = 3.5f
                audio.playHeartChime()
                repeat(8) {
                    particles.spawnHeart(cw * midX + (Random.nextFloat() - 0.5f) * 28f, ch * boy.worldY - 32f, Color(0xFFFF5D8F))
                }
                showMessage("My whole world right here.", duration = 3.5f)
            }
        }
    }

    fun onLongPressCharacter(cw: Float, ch: Float) {
        // Partner comes over for a sweet tight hug
        val midX = if (currentScene == SceneType.COZY_LOFT) 0.615f else (boy.worldX + girl.worldX) / 2f
        boy.worldX = midX - 0.02f
        girl.worldX = midX + 0.02f
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        boy.pose = CharacterPose.HUG
        girl.pose = CharacterPose.HUG
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.reactionTimer = 4.0f
        girl.reactionTimer = 4.0f
        boy.emoteTimer = 3.5f
        girl.emoteTimer = 3.5f
        audio.playHeartChime()
        repeat(8) {
            particles.spawnHeart(cw * midX + (Random.nextFloat() - 0.5f) * 32f, ch * boy.worldY - 28f)
        }
        showMessage("Holding you close.", duration = 3.5f)
    }

    fun onTouchTree(cw: Float, ch: Float) {
        treeTapReactionCount++
        audio.playLeafRustle()
        audio.playBirdChirp()
        repeat(14) {
            particles.spawnLeaf(cw * 0.5f + (Random.nextFloat() - 0.5f) * (cw * 0.4f), ch * 0.30f)
        }
    }

    fun onTouchSky(x: Float, y: Float, cw: Float, isNight: Boolean) {
        if (weather == WeatherType.RAIN) {
            triggerLightning()
            return
        }
        if (isNight) {
            audio.playStarTwinkle()
            particles.spawnShootingStar(cw * 0.15f, y.coerceAtMost(300f))
            repeat(4) {
                particles.spawnFirefly(x, y)
            }
            val quotes = listOf(
                "A quiet moment under the endless starry sky.",
                "Every star shines a little brighter with you here.",
                "Make a wish on the shooting star!",
                "Wrapped in starlight and gentle evening whispers."
            )
            showMessage(quotes.random(), duration = 3.0f)
        } else {
            audio.playStarTwinkle()
            particles.spawnSparkles(x, y, 10)
        }
    }

    fun onTouchPot(cw: Float, ch: Float) {
        audio.playCookingBubbles()
        audio.playBubblePop()
        repeat(8) {
            particles.spawnSteam(cw * 0.58f, ch * 0.59f)
        }
        particles.spawnHeart(cw * 0.58f, ch * 0.55f)
        val rand = Random.nextInt(3)
        when (rand) {
            0 -> {
                // Stir pot & smile
                girl.worldX = 0.58f
                girl.direction = Direction.RIGHT
                girl.pose = CharacterPose.COOK
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.SPARKLE
                girl.emoteTimer = 2.5f
                girl.reactionTimer = 3.5f
                boy.worldX = 0.50f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.EAT_SNEAK
                boy.emotion = CharacterEmotion.LOVING
                boy.reactionTimer = 3.5f
                showMessage("Cooking with love — ${boy.name} sneaks a taste!", duration = 3.0f)
            }
            1 -> {
                // Offer food to other character
                boy.worldX = 0.50f
                girl.worldX = 0.58f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.FEED_MOMO
                girl.emotion = CharacterEmotion.LOVING
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.5f
                girl.reactionTimer = 3.5f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.EAT_MOMO
                boy.emotion = CharacterEmotion.HAPPY
                boy.reactionTimer = 3.5f
                showMessage("${girl.name} offers a warm spoonful to ${boy.name}.", duration = 3.0f)
            }
            else -> {
                // Playful food-stealing
                boy.worldX = 0.52f
                girl.worldX = 0.60f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.EAT_SNEAK
                boy.emotion = CharacterEmotion.PLAYFUL
                boy.emote = EmoteType.HEART
                boy.emoteTimer = 2.5f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.JOY_JUMP
                girl.emotion = CharacterEmotion.PLAYFUL
                boy.reactionTimer = 3.5f
                girl.reactionTimer = 3.5f
                showMessage("${boy.name} playfully steals a taste before it's ready!", duration = 3.0f)
            }
        }
    }

    fun onTouchIngredients(cw: Float, ch: Float) {
        audio.playBubblePop()
        particles.spawnSparkles(cw * 0.72f, ch * 0.60f, 6)
        particles.spawnHeart(cw * 0.72f, ch * 0.56f, Color(0xFFFF758F))
        girl.worldX = 0.68f
        girl.direction = Direction.RIGHT
        girl.pose = CharacterPose.COOK
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.MUSIC_NOTE
        girl.emoteTimer = 2.5f
        girl.reactionTimer = 3.5f
        boy.worldX = 0.52f
        boy.direction = Direction.RIGHT
        boy.emotion = CharacterEmotion.HAPPY
        boy.pose = CharacterPose.EAT_SNEAK
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 2.5f
        boy.reactionTimer = 3.5f
        val lines = listOf(
            "Can I have some?",
            "You weren't supposed to see that!",
            "Chopping sweet carrots and fresh herbs together."
        )
        showMessage(lines.random(), duration = 3.0f)
    }

    fun onTouchFridge(touchX: Float, touchY: Float) {
        audio.playStarTwinkle()
        particles.spawnSparkles(touchX, touchY, 6)
        particles.spawnHeart(touchX, touchY - 20f, Color(0xFFFF85A1))
        val boyWalk = abs(0.78f - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
        fridgeDoorOpenTimer = boyWalk + 2.5f
        boy.moveTo(0.78f)
        boy.transitionPoseTo(CharacterPose.JOY_JUMP)
        boy.direction = Direction.RIGHT
        boy.bounceOffset = 6f
        boy.emotion = CharacterEmotion.HAPPY
        boy.emote = EmoteType.MUSIC_NOTE
        boy.emoteTimer = 2.5f
        boy.reactionTimer = fridgeDoorOpenTimer + 0.5f
        girl.direction = Direction.RIGHT
        girl.emotion = CharacterEmotion.PLAYFUL
        girl.reactionTimer = 3.0f
        val snackQuotes = listOf(
            "Midnight snack stash discovered!",
            "Cold drinks and sweet treats for us.",
            "Look, the photo from our trip is on the fridge!",
            "Found your favorite sweet treat in the fridge!"
        )
        showMessage(snackQuotes.random(), duration = 3.0f)
    }

    fun onTouchCabinet(cw: Float, ch: Float) {
        audio.playCookingBubbles()
        audio.playHeartChime()
        repeat(5) {
            particles.spawnSteam(cw * 0.62f, ch * 0.62f)
        }
        particles.spawnHeart(cw * 0.62f, ch * 0.58f, Color(0xFFFFD166))
        val boyWalk = abs(0.58f - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
        cabinetOpenTimer = boyWalk + 2.5f
        boy.moveTo(0.58f)
        boy.transitionPoseTo(CharacterPose.JOY_JUMP)
        boy.direction = Direction.RIGHT
        boy.emotion = CharacterEmotion.HAPPY
        boy.emote = EmoteType.SWEAT
        boy.emoteTimer = 2.5f
        boy.reactionTimer = cabinetOpenTimer + 0.5f
        showMessage("Warm cookies baking golden inside the oven!", duration = 3.0f)
    }

    fun onTouchSink(cw: Float, ch: Float) {
        audio.playBubblePop()
        val girlWalk = abs(0.32f - girl.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
        sinkRunningTimer = girlWalk + 2.5f
        repeat(6) {
            particles.spawnSteam(cw * 0.28f, ch * 0.58f)
        }
        particles.spawnSparkles(cw * 0.28f, ch * 0.56f, 6)
        girl.moveTo(0.32f)
        girl.transitionPoseTo(CharacterPose.IDLE)
        girl.direction = Direction.LEFT
        girl.emotion = CharacterEmotion.PLAYFUL
        girl.emote = EmoteType.SPARKLE
        girl.emoteTimer = 2.5f
        girl.reactionTimer = sinkRunningTimer + 0.5f
        boy.direction = Direction.LEFT
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 2.5f
        boy.reactionTimer = 3.5f
        showMessage("Splashing fresh water — washing veggies and tea cups!", duration = 3.0f)
    }

    fun onTouchKitchenTable(cw: Float, ch: Float) {
        audio.playHeartChime()
        particles.spawnHeart(cw * 0.44f, ch * 0.58f, Color(0xFFFF5D8F))
        particles.spawnSteam(cw * 0.44f, ch * 0.60f)
        boy.moveTo(0.40f)
        girl.moveTo(0.48f)
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        boy.transitionPoseTo(CharacterPose.SIT)
        girl.transitionPoseTo(CharacterPose.SIT)
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.reactionTimer = 4.0f
        girl.reactionTimer = 4.0f
        boy.emoteTimer = 3.0f
        girl.emoteTimer = 3.0f
        showMessage("Pulling up two chairs for warm tea and honest talks.", duration = 3.5f)
    }

    fun onTouchFlower(cw: Float, ch: Float) {
        flowerWiggleTimer = 1.2f
        audio.playHeartChime()
        boy.pose = CharacterPose.GIVE_FLOWER
        girl.pose = CharacterPose.RECEIVE_FLOWER
        boy.reactionTimer = 3.0f
        girl.reactionTimer = 3.0f
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 2.5f
        girl.emote = EmoteType.BLUSH
        girl.emoteTimer = 2.5f
        particles.spawnPetals(cw * 0.5f, ch * 0.65f, 10)

        val quotes = listOf(
            "This reminded me of you.",
            "Our garden is growing with our days together.",
            "Every petal holds a sweet memory of us.",
            "Blooming more beautifully every single day."
        )
        showMessage(quotes[flowerDialoguePicker.pick()], duration = 3.5f)
    }

    fun onTouchCat(cw: Float, ch: Float) {
        if (currentScene == SceneType.COZY_LOFT) {
            onTouchLoftMochi(cw, ch)
            return
        }
        // If cat was walking, stop and greet with love!
        if (catState == CatState.WALK_FOLLOW) {
            catTargetX = catWorldX
            catState = CatState.SITTING_PURR
            catSleeping = false
            audio.playCatPurr()
            particles.spawnMusicNote(cw * catWorldX, ch * catWorldY - 24f)
            particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
            showMessage("Mochi stopped to get your love!", duration = 2.5f)
            return
        }
        catState = when (catState) {
            CatState.SLEEPING -> CatState.SITTING_PURR
            CatState.SITTING_PURR -> CatState.BELLY_ROLL
            CatState.BELLY_ROLL -> CatState.SLEEPING
            CatState.PLAYFUL_POUNCE -> CatState.SLEEPING
            CatState.WALK_FOLLOW -> CatState.SLEEPING
        }
        catSleeping = (catState == CatState.SLEEPING)
        audio.playCatPurr()

        when (catState) {
            CatState.SITTING_PURR -> {
                particles.spawnMusicNote(cw * catWorldX, ch * catWorldY - 24f)
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
                showMessage("Mochi is purring happily!", duration = 2.5f)
            }
            CatState.BELLY_ROLL -> {
                repeat(4) {
                    particles.spawnHeart(cw * catWorldX + (Random.nextFloat() - 0.5f) * 20f, ch * catWorldY - 22f, Color(0xFFFF5D8F))
                }
                showMessage("Mochi wants gentle belly rubs!", duration = 2.5f)
            }
            CatState.PLAYFUL_POUNCE -> {
                audio.playBubblePop()
                particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 20f, 6)
                showMessage("Mochi pounces playfully!", duration = 2.5f)
            }
            CatState.WALK_FOLLOW -> {
                val target = if (boy.worldX < girl.worldX) (boy.worldX - 0.08f).coerceAtLeast(0.12f) else (girl.worldX + 0.08f).coerceAtMost(0.88f)
                catTargetX = target
                catFacingLeft = catTargetX < catWorldX
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF5D8F))
                showMessage("Mochi is trotting right beside you!", duration = 2.5f)
            }
            CatState.SLEEPING -> {
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
                showMessage("Mochi curled up for a warm snooze.", duration = 2.5f)
            }
        }
    }

    fun onTouchMailbox() {
        audio.playHeartChime()
        onOpenLoveNotes()
    }

    fun onTouchNightstandJournal() {
        audio.playHeartChime()
        onOpenDreamJournal()
    }

    fun onTouchPhotoFrame() {
        livingRoomReturnTimer = 0f
        livingRoomReturnChar = null
        audio.playHeartChime()
        onOpenMemories()
    }

    fun onTouchWallCalendar() {
        livingRoomReturnTimer = 0f
        livingRoomReturnChar = null
        audio.playHeartChime()
        onOpenCalendar()
    }

    fun onTouchWardrobe() {
        livingRoomReturnTimer = 0f
        livingRoomReturnChar = null
        audio.playStarTwinkle()
        onOpenWardrobe()
    }

    fun selectGirlDress(index: Int) {
        girl.outfitIndex = index
        girl.emotion = CharacterEmotion.LOVING
        girl.pose = CharacterPose.JOY_JUMP
        girl.bounceOffset = 8f
        audio.playHeartChime()
        val dressNames = listOf(
            "Strawberry Cream Sundress",
            "Lavender Dream Wrap Dress",
            "Emerald Velvet Romance",
            "Lemon Sunshine Picnic Dress",
            "Midnight Starlight Gown",
            "Mint Macaron Tea Dress",
            "${boy.name}'s Oversized Flannel",
            "Blush Rose Cropped Hoodie",
            "Sage & Cream Colorblock Hoodie",
            "Lavender Cloud Oversized Hoodie",
            "Buttercream Star Shimmer Hoodie"
        )
        val name = dressNames.getOrElse(index) { "Lovely Outfit" }
        showMessage("${girl.name} is now wearing the $name! Looking stunning!", duration = 3.5f)
    }

    fun selectGirlAccessory(index: Int) {
        girl.accessoryIndex = index
        audio.playStarTwinkle()
        val accessoryNames = listOf(
            "Natural look",
            "Cozy Ribbed Beanie",
            "Warm Wool Fringe Scarf",
            "Casual Streetwear Baseball Cap"
        )
        val name = accessoryNames.getOrElse(index) { "Accessory" }
        if (index == 0) {
            showMessage("${girl.name} took off her accessory.", duration = 2.5f)
        } else {
            showMessage("${girl.name} is now wearing the $name!", duration = 3.0f)
        }
    }

    fun selectBoyOutfit(index: Int) {
        boy.outfitIndex = index
        boy.emotion = CharacterEmotion.LOVING
        boy.pose = CharacterPose.JOY_JUMP
        boy.bounceOffset = 8f
        audio.playHeartChime()
        val boyOutfitNames = listOf(
            "Classic Spruce Knit & Navy Pants",
            "White & Emerald Varsity Hoodie",
            "Charcoal Streetwear Zip Hoodie",
            "Oatmeal Cloud Oversized Hoodie",
            "Midnight Starlight Graphic Hoodie"
        )
        val name = boyOutfitNames.getOrElse(index) { "Sharp Outfit" }
        showMessage("${boy.name} is now wearing the $name! Looking handsome!", duration = 3.5f)
    }

    fun selectBoyAccessory(index: Int) {
        boy.accessoryIndex = index
        audio.playStarTwinkle()
        val accessoryNames = listOf(
            "Natural look",
            "Cozy Ribbed Beanie",
            "Warm Wool Fringe Scarf",
            "Casual Streetwear Baseball Cap"
        )
        val name = accessoryNames.getOrElse(index) { "Accessory" }
        if (index == 0) {
            showMessage("${boy.name} took off his accessory.", duration = 2.5f)
        } else {
            showMessage("${boy.name} is now wearing the $name!", duration = 3.0f)
        }
    }

    fun onTouchDustbin(touchX: Float, touchY: Float) {
        if (binLidTimer > 0f) return
        binLidTimer = 1.2f
        audio.playBubblePop()
        particles.spawnSparkles(touchX, touchY - 20f, 4)
    }

    fun onTouchKitchenClock(touchX: Float, touchY: Float) {
        if (clockSpinTimer > 0f) return
        clockSpinTimer = 2.4f
        audio.playTickTick()
        girl.emotion = CharacterEmotion.CURIOUS
        girl.direction = Direction.RIGHT
        girl.reactionTimer = 2.0f
    }

    fun onTouchKitchenCrate(touchX: Float, touchY: Float) {
        if (crateShakeTimer > 0f) return
        crateShakeTimer = 0.7f
        audio.playWoodKnock()
        particles.spawnSparkles(touchX, touchY - 10f, 5)
        val boyWalk = abs(0.15f - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
        boy.moveTo(0.15f)
        boy.direction = Direction.RIGHT
        boy.emotion = CharacterEmotion.CURIOUS
        boy.reactionTimer = (boyWalk + 1.5f).coerceAtMost(3.0f)
    }

    fun onTouchKitchenPlanter(touchX: Float, touchY: Float) {
        if (planterAnimTimer > 0f) return
        planterAnimTimer = 2.0f
        audio.playWaterDrip()
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.MUSIC_NOTE
        girl.emoteTimer = 1.8f
        girl.reactionTimer = 2.0f
    }

    fun onTouchKitchenStool(touchX: Float, touchY: Float) {
        if (stoolWobbleTimer > 0f) return
        stoolWobbleTimer = 0.8f
        audio.playWoodCreak()
        val boyWalk = abs(0.40f - boy.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
        boy.moveTo(0.40f)
        boy.direction = Direction.RIGHT
        boy.transitionPoseTo(CharacterPose.JOY_JUMP)
        boy.emotion = CharacterEmotion.HAPPY
        boy.reactionTimer = (boyWalk + 1.2f).coerceAtMost(2.5f)
    }

    fun onTouchCoffeeTable(touchX: Float, touchY: Float) {
        if (tableCandleTimer > 0f) return
        tableCandleTimer = 1.5f
        audio.playCandleFlicker()
        particles.spawnSparkles(touchX, touchY - 10f, 4)
        girl.emotion = CharacterEmotion.LOVING
        boy.emotion = CharacterEmotion.LOVING
        girl.emote = EmoteType.HEART
        girl.emoteTimer = 1.5f
    }

    fun onTouchPouf(touchX: Float, touchY: Float) {
        if (poufBounceTimer > 0f) return
        poufBounceTimer = 0.8f
        audio.playSoftThud()
        particles.spawnSparkles(touchX, touchY - 8f, 3)
    }

    fun onTouchCardboardBox(touchX: Float, touchY: Float) {
        if (cardboardBoxTimer > 0f) return
        cardboardBoxTimer = 1.3f
        audio.playCatChirp()
        particles.spawnHeart(touchX, touchY - 14f, Color(0xFFFF85A1))
    }

    fun onTouchStorageBasket(touchX: Float, touchY: Float) {
        if (basketYarnTimer > 0f) return
        basketYarnTimer = 1.4f
        audio.playSoftRoll()
        particles.spawnSparkles(touchX, touchY - 8f, 3)
    }

    fun onTouchMagazineRack(touchX: Float, touchY: Float) {
        if (magazineRackTimer > 0f) return
        magazineRackTimer = 1.3f
        audio.playPaperFlip()
        particles.spawnMusicNote(touchX, touchY - 18f)
    }

    fun onTouchStreetlamp() {
        lampLit = !lampLit
        audio.playBubblePop()
    }

    // Lantern Stroll Touch Interactions
    fun onTouchPagodaLantern(touchX: Float, touchY: Float) {
        if (pagodaGlowTimer > 0f) return
        pagodaGlowTimer = 1.8f
        audio.playPagodaChime()
        particles.spawnSparkles(touchX, touchY - 8f, 7, Color(0xFFFFD166))
        particles.spawnHeart(touchX, touchY - 20f, Color(0xFFFFD6A5))
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 1.6f
        girl.emote = EmoteType.HEART
        girl.emoteTimer = 1.6f
    }

    fun onTouchLavenderPatch(touchX: Float, touchY: Float) {
        if (lavenderSwayTimer > 0f) return
        lavenderSwayTimer = 1.5f
        audio.playLavenderRustle()
        particles.spawnSparkles(touchX, touchY - 6f, 8, Color(0xFFC77DFF))
        particles.spawnSparkles(touchX + 12f, touchY - 14f, 4, Color(0xFFE0AAFF))
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.SPARKLE
        girl.emoteTimer = 1.8f
    }

    fun onTouchMushrooms(touchX: Float, touchY: Float) {
        if (mushroomBounceTimer > 0f) return
        mushroomBounceTimer = 1.4f
        audio.playMushroomChime()
        particles.spawnSparkles(touchX, touchY - 4f, 6, Color(0xFF48CAE4))
        particles.spawnSparkles(touchX - 8f, touchY - 10f, 4, Color(0xFF90E0EF))
    }

    fun onTouchMomoSteamer(cw: Float, ch: Float) {
        if (momoSteamerTimer > 0f) return
        momoSteamerTimer = 1.5f
        val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)
        audio.playSteamHiss()
        audio.playCookingBubbles()
        repeat(8) {
            particles.spawnSteam(cw * 0.44f + (Random.nextFloat() - 0.5f) * 20f, ch * 0.69f - 24f * pixelScale)
        }
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 1.6f
    }

    fun onTouchMomoSign(cw: Float, ch: Float) {
        if (momoSignFlickerTimer > 0f) return
        momoSignFlickerTimer = 1.4f
        audio.playNeonBuzz()
        particles.spawnSparkles(cw * 0.50f, ch * 0.44f, 8, Color(0xFFFFD166))
        repeat(3) {
            particles.spawnHeart(cw * 0.50f + (Random.nextFloat() - 0.5f) * 30f, ch * 0.44f - 10f, Color(0xFFFF5D8F))
        }
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.SPARKLE
        girl.emoteTimer = 1.8f
    }

    fun onTouchChutney(cw: Float, ch: Float) {
        if (chutneySpiceTimer > 0f) return
        chutneySpiceTimer = 1.2f
        audio.playSpiceZing()
        girl.emote = EmoteType.BLUSH
        girl.emoteTimer = 1.8f
        girl.emotion = CharacterEmotion.SHY
        boy.emote = EmoteType.SWEAT
        boy.emoteTimer = 1.8f
        repeat(5) {
            particles.spawnHeart(cw * 0.58f, ch * 0.65f - 15f, Color(0xFFD90429))
        }
    }

    fun onTouchChalkboard(touchX: Float, touchY: Float) {
        if (chalkboardTimer > 0f) return
        chalkboardTimer = 1.6f
        audio.playChalkSqueak()
        particles.spawnSparkles(touchX, touchY - 8f, 5, Color(0xFFFFD166))
        girl.emote = EmoteType.MUSIC_NOTE
        girl.emoteTimer = 1.6f
    }

    fun onTouchBambooCrate(touchX: Float, touchY: Float) {
        if (bambooCrateTimer > 0f) return
        bambooCrateTimer = 1.0f
        audio.playBambooKnock()
        particles.spawnSteam(touchX, touchY - 14f)
    }

    fun onTouchMilkSaucer(touchX: Float, touchY: Float) {
        if (milkSaucerTimer > 0f) return
        milkSaucerTimer = 1.8f
        audio.playSaucerSip()
        audio.playCatPurr()
        catTargetX = 0.38f
        catFacingLeft = catTargetX < catWorldX
        catState = CatState.SITTING_PURR
        particles.spawnHeart(touchX, touchY - 10f, Color(0xFFFF85A1))
    }

    fun onTouchDiningTable(touchX: Float, touchY: Float) {
        if (streetDiningTableTimer > 0f) return
        streetDiningTableTimer = 1.6f
        audio.playHeartChime()
        particles.spawnHeart(touchX, touchY - 12f, Color(0xFFFF758F))
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 1.6f
        girl.emoteTimer = 1.6f
    }

    fun onTouchScooter(cw: Float, ch: Float) {
        if (scooterHonkTimer > 0f) return
        scooterHonkTimer = 0.8f
        audio.playScooterHorn()
        girl.emotion = CharacterEmotion.HAPPY
        boy.emotion = CharacterEmotion.LOVING
        showMessage("Scooter road trip with my favourite person", duration = 2.5f)
        repeat(6) {
            particles.spawnHeart(cw * 0.50f + (Random.nextFloat() - 0.5f) * 35f, ch * 0.65f - 25f)
        }
        particles.spawnSparkles(cw * 0.50f, ch * 0.68f, 6)
    }

    fun onTouchGirlScooter(cw: Float, ch: Float) {
        girlSpeechText = "Hold tight ${boy.name}! The scooter is fast hehe!"
        girlSpeechTimer = 4.0f
        girl.emotion = CharacterEmotion.HAPPY
        audio.playHeartChime()
        particles.spawnHeart(cw * 0.53f, ch * 0.60f, Color(0xFFFF5D8F))
    }

    fun onTouchBoyScooter(cw: Float, ch: Float) {
        boySpeechText = "Holding you forever, my love."
        boySpeechTimer = 4.0f
        boy.emotion = CharacterEmotion.LOVING
        audio.playHeartChime()
        particles.spawnHeart(cw * 0.43f, ch * 0.60f, Color(0xFFFF3366))
    }

    fun onTripleTapRide(cw: Float, ch: Float) {
        girl.pose = CharacterPose.HUG
        girl.emotion = CharacterEmotion.LOVING
        boy.emotion = CharacterEmotion.LOVING
        girl.emote = EmoteType.HEART
        girl.emoteTimer = 3.5f
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 3.5f
        girlSpeechText = "Hold on tight!"
        girlSpeechTimer = 3.5f
        boySpeechText = "Holding you forever, my love."
        boySpeechTimer = 3.5f
        audio.playHeartChime()
        repeat(8) {
            particles.spawnHeart(cw * 0.48f + (Random.nextFloat() - 0.5f) * 40f, ch * 0.65f - 20f, Color(0xFFFF3366))
        }
        particles.spawnSparkles(cw * 0.50f, ch * 0.68f, 6)
        showMessage("Holding tight together on our evening ride", duration = 3.5f)
    }

    fun onTouchTempleSpire(cw: Float, ch: Float) {
        if (templeGlowTimer > 0f) return
        templeGlowTimer = 2.0f
        audio.playStarArpeggio()
        showMessage("The temple in the distance glows in the evening light", duration = 3.0f)
        particles.spawnSparkles(cw * 0.72f, ch * 0.45f, 10, Color(0xFFFFD166))
        particles.spawnHeart(cw * 0.72f, ch * 0.40f, Color(0xFFFFD6A5))
        boy.emote = EmoteType.SPARKLE
        girl.emote = EmoteType.SPARKLE
        boy.emoteTimer = 2.0f
        girl.emoteTimer = 2.0f
    }

    fun onTouchCottageDoor() {
        audio.playBubblePop()
        loadScene(SceneType.COOKING) // Enter Cottage Kitchen
        showMessage("Entering our cozy cottage.", duration = 2.5f)
    }

    fun onCycleCottageRoom() {
        audio.playBubblePop()
        when (currentScene) {
            SceneType.COOKING -> {
                loadScene(SceneType.SLEEP)
                showMessage("Cottage Living Room - Warm couch and memories.", duration = 2.5f)
            }
            SceneType.SLEEP -> {
                loadScene(SceneType.COZY_LOFT)
                showMessage("Cottage Bedroom & Loft - Stargazing under the roof.", duration = 2.5f)
            }
            SceneType.COZY_LOFT -> {
                loadScene(SceneType.FLOWER)
                showMessage("Stepping back out into the fresh meadow.", duration = 2.5f)
            }
            else -> {
                loadScene(SceneType.COOKING)
                showMessage("Entering our cozy cottage.", duration = 2.5f)
            }
        }
    }

    fun driftWeather() {
        val candidates = WeatherType.values().filter { it != weather }
        val next = candidates.randomOrNull() ?: return
        weather = next
        audio.playWeatherBgm(next, isAutomaticDrift = true)
    }

    fun cycleWeather(): WeatherType {
        weatherDriftTimer = 0f
        val all = WeatherType.values()
        val nextIdx = (weather.ordinal + 1) % all.size
        weather = all[nextIdx]
        particles.clearSeasonalParticles()
        audio.playWindChime()
        audio.playWeatherBgm(weather, isAutomaticDrift = false)
        showMessage("Weather: ${weather.displayName}", duration = 2.5f)
        return weather
    }

    fun onTouchConstellation(name: String, description: String, x: Float, y: Float) {
        val targetIdx = when (name) {
            "The Two Hearts" -> 1
            "The Celestial Teapot" -> 2
            "Starlight Trail" -> 3
            else -> 1
        }
        if (constellationConnectTimer > 0f && activeConstellationIndex == targetIdx) return

        activeConstellationIndex = targetIdx
        constellationConnectTimer = 2.4f
        audio.playStarArpeggio()
        particles.spawnSparkles(x, y, 7, Color(0xFFCAF0F8))
        particles.spawnHeart(x, y - 14f, Color(0xFFFF85A1))

        // Boy & girl look up in sweet wonder together without changing their positions or sitting poses
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.SPARKLE
        boy.emoteTimer = 2.2f
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.HEART
        girl.emoteTimer = 2.2f
        showMessage("Constellation: $name - $description", duration = 3.0f)
    }

    fun triggerThinkingOfYou(cw: Float, ch: Float) {
        audio.playThinkingOfYouChime()
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 4.0f
        boy.emotion = CharacterEmotion.LOVING
        girl.emote = EmoteType.BLUSH
        girl.emoteTimer = 4.0f
        girl.emotion = CharacterEmotion.SHY
        repeat(14) {
            val rx = cw * (0.15f + Random.nextFloat() * 0.70f)
            val ry = ch * (0.65f + Random.nextFloat() * 0.20f)
            particles.spawnHeart(rx, ry, Color(0xFFFF3366))
        }
        showMessage("${boy.name} is thinking of ${girl.name} right now", duration = 4.0f)
    }

    // Cozy Loft Touch Interactions
    fun onTouchLoftSofa(cw: Float, ch: Float) {
        audio.playHeartChime()
        boy.reactionTimer = 3.0f
        girl.reactionTimer = 3.0f
        boy.pose = CharacterPose.SIT
        girl.pose = CharacterPose.SIT_SNUGGLE
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING
        val rand = Random.nextInt(3)
        when (rand) {
            0 -> {
                girlSpeechText = "Stay right here with me..."
                girlSpeechTimer = 3.5f
                boySpeechText = "Never letting go."
                boySpeechTimer = 3.5f
                repeat(4) {
                    particles.spawnHeart(cw * 0.62f + (Random.nextFloat() - 0.5f) * 30f, ch * 0.54f - 10f, Color(0xFFFF5D8F))
                }
            }
            1 -> {
                boy.emote = EmoteType.HEART
                boy.emoteTimer = 2.5f
                girl.emote = EmoteType.BLUSH
                girl.emoteTimer = 2.5f
                showMessage("Wrapped together under our warm quilt.", duration = 3.0f)
                particles.spawnHeart(cw * 0.62f, ch * 0.52f, Color(0xFFFF3366))
            }
            else -> {
                boySpeechText = "You are my entire world."
                boySpeechTimer = 3.5f
                girlSpeechText = "Always and forever."
                girlSpeechTimer = 3.5f
                repeat(5) {
                    particles.spawnHeart(cw * 0.62f + (Random.nextFloat() - 0.5f) * 40f, ch * 0.54f - 15f)
                }
            }
        }
    }

    fun onTouchLoftWindow(cw: Float, ch: Float) {
        if (loftWindowTimer > 0f) return
        loftWindowTimer = 2.0f
        audio.playStarTwinkle()
        val windowStartX = cw * 0.33f
        val starX = windowStartX + 20f + Random.nextFloat() * (cw * 0.40f)
        val starY = ch * (0.08f + Random.nextFloat() * 0.16f)
        particles.spawnShootingStar(starX, starY)
        particles.spawnSparkles(cw * 0.68f, ch * 0.22f, 7, Color(0xFFCAF0F8))
    }

    fun onTouchLoftMoon(cw: Float, ch: Float) {
        audio.playStarTwinkle()
        val windowStartX = cw * 0.33f
        val moonX = windowStartX + (cw - windowStartX) * 0.74f
        val moonY = ch * 0.11f
        particles.spawnShootingStar(moonX - 20f, moonY + 20f)
        particles.spawnSparkles(moonX, moonY, 8)
        girlSpeechText = "Look, the moon is so luminous tonight!"
        girlSpeechTimer = 3.5f
        boySpeechText = "Not as radiant as you."
        boySpeechTimer = 3.5f
    }

    fun onTouchLoftBookshelf(cw: Float, ch: Float) {
        audio.playLeafRustle()
        loftBookReading = !loftBookReading
        particles.spawnSparkles(cw * 0.18f, ch * 0.42f, 6)
        boy.emote = EmoteType.MUSIC_NOTE
        boy.emoteTimer = 2.0f
        val quotes = listOf(
            "Browsing through old love stories on the shelf...",
            "Chapter 1: The boy with big dreams and the girl he loves.",
            "Found our couple photo tucked between vintage poems!",
            "A tiny white bunny plush rests peacefully on tier 3."
        )
        showMessage(quotes.random(), duration = 3.5f)
    }

    fun onTouchLoftRecordPlayer(cw: Float, ch: Float) {
        val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)
        val floorY = ch * 0.58f
        audio.playBubblePop()
        if (audio.musicBoxState == MusicBoxState.PLAYING) {
            audio.pauseMusic()
            recordSpinning = false
            showMessage("Turntable paused.", duration = 2.0f)
        } else {
            audio.togglePlayPause()
            recordSpinning = true
            repeat(4) {
                particles.spawnMusicNote(cw * 0.88f + (Random.nextFloat() - 0.5f) * 20f, floorY - 14 * pixelScale)
            }
            showMessage("Vinyl needle drops... warm cozy melodies play.", duration = 3.0f)
        }
    }

    fun onTouchLoftTable(cw: Float, ch: Float) {
        if (loftTableTimer > 0f) return
        loftTableTimer = 1.4f
        val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)
        val floorY = ch * 0.58f
        audio.playCookingBubbles()
        repeat(5) {
            val mx = cw * 0.50f + 15 * pixelScale + (Random.nextFloat() - 0.5f) * 8f
            particles.spawnSteam(mx, floorY + 2 * pixelScale)
        }
        girl.emotion = CharacterEmotion.LOVING
        girl.emote = EmoteType.HEART
        girl.emoteTimer = 1.6f
        boy.emote = EmoteType.MUSIC_NOTE
        boy.emoteTimer = 1.6f
    }

    fun onTouchLoftReadingNook(touchX: Float, touchY: Float) {
        if (loftBookNookTimer > 0f) return
        loftBookNookTimer = 1.8f
        audio.playPaperFlip()
        particles.spawnSparkles(touchX, touchY - 8f, 5, Color(0xFFFFD166))
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 1.8f
    }

    fun onTouchLoftLamp(cw: Float, ch: Float) {
        val pixelScale = (cw / 115f).coerceIn(3.0f, 5.0f)
        val floorY = ch * 0.58f
        lampLit = !lampLit
        audio.playBubblePop()
        ambientDimming = if (lampLit) 0.08f else 0.42f
        if (lampLit) {
            particles.spawnSparkles(cw * 0.38f, floorY - 18 * pixelScale, 5)
            showMessage("Warm amber lamp lights up the loft.", duration = 2.5f)
        } else {
            showMessage("Moonlight only... romantic midnight ambience.", duration = 2.5f)
        }
    }

    fun onTouchLoftPlant(cw: Float, ch: Float) {
        if (loftFairyLightsTimer > 0f) return
        loftFairyLightsTimer = 1.6f
        audio.playLeafRustle()
        particles.spawnSparkles(cw * (0.2f + Random.nextFloat() * 0.6f), ch * 0.78f, 6, Color(0xFFFFD166))
        repeat(4) {
            particles.spawnLeaf(cw * (0.1f + Random.nextFloat() * 0.8f), ch * 0.80f)
        }
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.SPARKLE
        girl.emoteTimer = 1.8f
    }

    fun onTouchLoftMochi(cw: Float, ch: Float) {
        catWorldX = 0.50f
        catWorldY = 0.55f
        val nextIdx = petTapPicker.pick()
        catState = when (nextIdx % 3) {
            0 -> CatState.SITTING_PURR
            1 -> CatState.BELLY_ROLL
            else -> CatState.SLEEPING
        }
        catSleeping = (catState == CatState.SLEEPING)
        audio.playCatPurr()
        when (catState) {
            CatState.SITTING_PURR -> {
                particles.spawnMusicNote(cw * catWorldX, ch * catWorldY - 24f)
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
                showMessage("Mochi purrs warmly on the sofa blanket.", duration = 2.5f)
            }
            CatState.BELLY_ROLL -> {
                repeat(4) {
                    particles.spawnHeart(cw * catWorldX + (Random.nextFloat() - 0.5f) * 20f, ch * catWorldY - 22f, Color(0xFFFF5D8F))
                }
                showMessage("Mochi stretches and rolls over on the quilt!", duration = 2.5f)
            }
            else -> {
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
                showMessage("Mochi curled up for a warm snooze on the couch.", duration = 2.5f)
            }
        }
    }

    // Feature 5: Floor lamp in Living Room / Couch Snooze scene
    fun onTouchLivingRoomLamp(cw: Float, ch: Float) {
        livingRoomReturnTimer = 0f
        livingRoomReturnChar = null
        livingRoomLampLit = !livingRoomLampLit
        audio.playBubblePop()
        if (livingRoomLampLit) {
            couchWakeTimer = 3.0f
            particles.spawnSparkles(cw * 0.20f, ch * 0.45f, 6)
            showMessage("Lamp on — warm amber light fills the room.", duration = 2.5f)
        } else {
            couchPhaseTimer = 0f
            couchVisualPhase = CouchPhase.NIGHT
            showMessage("Lamp off — time to drift away together.", duration = 2.5f)
        }
    }
}
