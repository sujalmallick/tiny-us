package com.example.scene

import com.example.engine.GameText
import com.example.data.CoupleDates
import kotlinx.datetime.daysUntil
import com.example.engine.WorldViewport

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.DailyPromptCatalog
import com.example.engine.WorldAudio
import com.example.engine.MusicBoxState
import com.example.engine.AntiRepeatRandomPicker
import com.example.engine.ShuffledDice
import com.example.engine.CharacterEmotion
import com.example.engine.CharacterPose
import com.example.engine.CharacterState
import com.example.engine.Direction
import com.example.engine.EmoteType
import com.example.engine.HeldItem
import com.example.engine.ParticleSystem
import com.example.engine.ParticleType
import com.example.engine.SnowPrintKind
import com.example.engine.Puddle
import com.example.engine.PixelCharacter
import com.example.engine.CharacterMotionTween
import com.example.engine.RoomTheme
import com.example.engine.TimeOfDayPhase
import com.example.engine.BirdEntity
import com.example.engine.BirdSpecies
import com.example.engine.BirdSystem
import com.example.engine.PerchSurface
import com.example.scene.autonomy.AgentPhase
import com.example.scene.autonomy.AutonomyAgent
import com.example.scene.autonomy.Behavior
import com.example.scene.autonomy.CoupleRequests
import com.example.scene.autonomy.RequestKind
import com.example.scene.autonomy.BehaviorBrain
import com.example.scene.autonomy.BehaviorContext
import com.example.scene.autonomy.Discovery
import com.example.scene.autonomy.DiscoveryKind
import com.example.scene.autonomy.SceneSpot
import com.example.scene.autonomy.SceneSpots
import com.example.scene.autonomy.SpotAction
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import com.example.resources.*
import org.jetbrains.compose.resources.StringResource
import kotlinx.datetime.number
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toLocalDateTime

private val OUTDOOR_ENVIRONMENTS = setOf(
    EnvironmentType.MEADOW,
    EnvironmentType.TREE_HILL,
    EnvironmentType.PATH_NIGHT,
    EnvironmentType.TWILIGHT,
    EnvironmentType.EVENING_ROAD,
    EnvironmentType.MOMO_STALL,
    EnvironmentType.CAMPFIRE,
    EnvironmentType.SEASIDE_PIER
)

/** Converts a per-second event rate to a frame-rate-independent per-frame chance. */
private fun eventChance(ratePerSecond: Float, deltaSeconds: Float): Boolean =
    Random.nextFloat() < (1f - exp(-ratePerSecond * deltaSeconds.coerceAtLeast(0f)))

class SceneEngine(
    val audio: WorldAudio,
    private val onOpenLoveNotes: () -> Unit,
    private val onOpenMemories: () -> Unit,
    private val onOpenCalendar: () -> Unit = {},
    private val onOpenWardrobe: () -> Unit = {},
    private val onOpenDreamJournal: () -> Unit = {}
) {
    companion object {
        /** How long a weather picked with the weather button stays before the season moves it on. */
        const val MANUAL_WEATHER_HOLD_SECONDS = 20f * 60f

        /** How long the heart meter shows over Mochi after a pet. */
        const val MOCHI_METER_SECONDS = 2.6f

        /** How long the couple's special-day greeting stays up. */
        const val SPECIAL_DAY_LINE_SECONDS = 6f

        const val MIN_CAT_WALK_THRESHOLD_PIXELS = 48f
        const val PIER_ICE_CREAM_SECONDS = 8f
        const val PIER_BOTTLE_RESPAWN_SECONDS = 30f
        const val PIER_BAO_DOZE_SECONDS = 25f
        const val PIER_DOLPHIN_SECONDS = 3.5f

        /** How long autonomy steps aside after the user touches the world. */
        const val AUTONOMY_USER_PAUSE_SECONDS = 5f
        /** Breathing room after a cinematic before the couple's own routine resumes. */
        const val AUTONOMY_AFTER_CINEMATIC_PAUSE = 3f
        /** Rest between a character's activities: min + up to range seconds. */
        const val AUTONOMY_REST_MIN = 2.5f
        const val AUTONOMY_REST_RANGE = 4f
        /** How long a mug, book or find is carried about before it is put away. */
        const val CARRIED_ITEM_SECONDS = 22f
        /** How long the released lanterns take to drift out of sight (plan 09, D). */
        const val LANTERN_SECONDS = 14f
        /** Give up on a walk that hasn't arrived after this long. */
        const val AUTONOMY_WALK_TIMEOUT = 12f
        /** Rare surprises: none in the first minute or so, then at least this far apart. */
        const val RARE_EVENT_FIRST_DELAY = 75f
        const val RARE_EVENT_GAP = 150f
        const val MOCHI_ROAM_SPEED = 0.052f
        const val MOCHI_ZOOM_SPEED = 0.16f
        /** Discoveries: the first turns up after about a minute, then every 45-90 s. */
        const val DISCOVERY_FIRST_DELAY = 40f
        const val DISCOVERY_GAP_MIN = 45f
        const val DISCOVERY_GAP_RANGE = 45f
        /** Unnoticed finds fade away after this long. */
        const val DISCOVERY_LIFETIME = 50f
        const val PIER_FLOCK_SECONDS = 4f
        const val PIER_LIGHT_PALETTES = 3
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
    /** Cooking, fishing and garden care (plan 07, C3-C5). Declared early: loadScene puts them away. */
    val cozy = CozyGames(this)
    val birdSystem = BirdSystem()

    var sceneTime: Float = 0f

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

    var roomTheme: RoomTheme by mutableStateOf(RoomTheme.WARM_AUTUMN_COTTAGE)
        private set
    var cafeLatteTimer: Float by mutableFloatStateOf(0f)
        private set
    var cafeWindowHeartTimer: Float by mutableFloatStateOf(0f)
        private set
    var cafePastryBites: Int by mutableIntStateOf(0)
        private set
    var cafeWindowHeartX: Float by mutableFloatStateOf(0.50f)
        private set
    var cafeWindowHeartY: Float by mutableFloatStateOf(0.30f)
        private set
    var sunroomSkylightTimer: Float by mutableFloatStateOf(0f)
        private set
    var sunroomMistTimer: Float by mutableFloatStateOf(0f)
        private set
    var sunroomBloomStage: Int by mutableIntStateOf(0)
        private set
    var catTreatJarTimer: Float by mutableFloatStateOf(0f)
        private set
    var catTreatDropTimer: Float by mutableFloatStateOf(0f)
        private set
    var catTreatMunchTimer: Float by mutableFloatStateOf(0f)
        private set
    var mochiCollarStyle: Int by mutableIntStateOf(0)
        private set
    var campfireEmbersTimer: Float by mutableFloatStateOf(0f)
        private set
    var campLanternLit: Boolean by mutableStateOf(true)
        private set
    var campGuitarStrumTimer: Float by mutableFloatStateOf(0f)
        private set
    var marshmallowRoastingTimer: Float by mutableFloatStateOf(0f)
        private set
    var cafeBaristaBrewTimer: Float by mutableFloatStateOf(0f)
        private set
    var cafePupPetTimer: Float by mutableFloatStateOf(0f)
        private set

    // Seaside Pier: ice cream cart, Grandpa Bao's fishing, the bottle, the lighthouse and Pip the seagull
    var pierIceCreamTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierLighthouseTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierFishingPhase: PierFishingPhase by mutableStateOf(PierFishingPhase.IDLE)
        private set
    var pierFishingTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierLastCatch: PierCatch? by mutableStateOf(null)
        private set
    var pierBottleVisible: Boolean by mutableStateOf(true)
        private set
    private var pierBottleRespawnTimer = 0f
    var pierGullState: GullState by mutableStateOf(GullState.AWAY)
        private set
    var pierGullX: Float by mutableFloatStateOf(1.1f)
        private set
    var pierGullY: Float by mutableFloatStateOf(0.20f)
        private set
    var pierGullFacingLeft: Boolean by mutableStateOf(true)
        private set
    private var pierGullTimer = 0f
    private var pierGullTargetX = 0.62f
    /** Random source for the pier's characters; tests swap in a seeded one. */
    var pierRng: Random = Random.Default
    private val pierCatchPicker = AntiRepeatRandomPicker(PierCatch.entries.toList())
    private val baoLinePicker = AntiRepeatRandomPicker(GRANDPA_BAO_LINES)
    /** Today's Daily Tiny Moments question, revealed by the message in a bottle. */
    var dailyPromptProvider: () -> String = {
        DailyPromptCatalog.today(
            com.example.data.RelationshipTimeCalculator.calculateTinyUsDay(com.example.data.CoupleDates.anniversary, com.example.data.CoupleDates.today()).toInt()
        ).question
    }

    // Grandpa Bao's own little routine between taps: tea, waves, casting on his own, dozing at night.
    var pierBaoSipTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierBaoWaveTimer: Float by mutableFloatStateOf(0f)
        private set
    private var pierBaoSipCooldown = 6f
    private var pierBaoSelfCastTimer = 14f
    private var pierBaoGreetPending = false
    /** Seconds since anyone (or anything) last bothered Bao; long quiet nights make him doze. */
    private var pierBaoQuietTime = 0f
    val isBaoDozing: Boolean
        get() = currentScene == SceneType.SEASIDE_PIER && timeOfDayPhase.isNight &&
            pierFishingPhase == PierFishingPhase.IDLE && pierBaoQuietTime > PIER_BAO_DOZE_SECONDS

    // More pier things to poke at.
    var pierDolphinTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierBoatHornTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierFlockTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierBucketFlopTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierLightsPalette: Int by mutableIntStateOf(0)
        private set
    var pierLightsSparkleTimer: Float by mutableFloatStateOf(0f)
        private set
    var pierCrabX: Float by mutableFloatStateOf(0.40f)
        private set
    var pierCrabFacingLeft: Boolean by mutableStateOf(false)
        private set
    var pierCrabStartledTimer: Float by mutableFloatStateOf(0f)
        private set
    private var pierCrabHiddenTimer = 0f
    private var pierCrabPauseTimer = 1.5f
    val isPierCrabVisible: Boolean get() = pierCrabHiddenTimer <= 0f
    private val pierAutonomousPicker = AntiRepeatRandomPicker(listOf(0, 1, 2))

    // Cozy weather keepsakes: catching what falls, the rainbow after rain, the snowday snowman.
    var weatherCatchCount: Int by mutableIntStateOf(0)
        private set
    var rainbowTimer: Float by mutableFloatStateOf(0f)
    var snowmanStage: Int by mutableIntStateOf(0)
        private set
    var snowmanWobbleTimer: Float by mutableFloatStateOf(0f)
        private set
    private var lastSeenWeather: WeatherType? = null

    // Ground weather play: footprints in the snow, kicked-up leaves, puddle splashes.
    private val lastPrintX = floatArrayOf(-1f, -1f, -1f)
    private val lastPrintY = floatArrayOf(-1f, -1f, -1f)
    private val puddleSplashCooldown = floatArrayOf(0f, 0f, 0f)
    private var lastRustleTime = -10f
    private var lastSnowTraceX = -1f
    private var lastSnowTraceY = -1f
    private var groundHintShown = false
    private val snowCatchLines = AntiRepeatRandomPicker(listOf(
        "You caught a snowflake. No two are alike, just like you two.",
        "A snowflake melts on your fingertip. Warm hands, warm hearts.",
        "Caught one! It sparkles like the first winter you shared."
    ))
    private val petalCatchLines = AntiRepeatRandomPicker(listOf(
        "A cherry petal landed in your hand. They say that means luck in love.",
        "Caught a petal before it touched the ground. Make a wish together.",
        "A soft pink petal, just for you two."
    ))
    private val leafCatchLines = AntiRepeatRandomPicker(listOf(
        "You caught a falling leaf! Make a wish before the next one lands.",
        "A crunchy golden leaf for your keepsake box.",
        "Caught a little autumn leaf, warm as a hand to hold."
    ))
    private val fluffCatchLines = AntiRepeatRandomPicker(listOf(
        "You caught a dandelion wish. Make it a sweet one.",
        "A dandelion puff! Whisper a wish and let it go.",
        "Caught a floating wish on the breeze."
    ))

    private var catTreatInProgress = false
    private var catTreatWalking = false
    val isCatTreatOnFloor: Boolean get() = catTreatInProgress && catTreatDropTimer <= 0f && catTreatMunchTimer <= 0f

    // Feature 1: Mochi Matchmaker state
    var mochiMatchmakerActive: Boolean by mutableStateOf(false)
    var mochiMatchmakerCooldown: Float = 22f
    var mochiMatchmakerVariant: Int = 0
    var mochiMatchmakerStep: Int = 0
    var mochiMatchmakerStepTimer: Float = 0f
    var mochiNudgeActiveTimer: Float = 0f
    val mochiMatchmakerPicker = AntiRepeatRandomPicker(listOf(0, 1, 2))

    // Feature 2: Tree Bark Growth state
    var treeMossDebugYearOverride: Int? = null
    var treeMossGrowthStage: Int by mutableIntStateOf(0)
    var boyfriendInitial: Char = com.example.data.ProfileManager.getProfile().boyName.firstOrNull()?.uppercaseChar() ?: 'H'
    var girlfriendInitial: Char = com.example.data.ProfileManager.getProfile().girlName.firstOrNull()?.uppercaseChar() ?: 'H'

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
    var rideFireflyTimer: Float by mutableFloatStateOf(0f)

    // Midnight Loft tap-interaction animation timers
    var loftTableTimer: Float by mutableFloatStateOf(0f)       // drives tea cup tilt + steam + crumb drop (1.4 s)
    var loftBookNookTimer: Float by mutableFloatStateOf(0f)    // drives book opening and fluttering dust motes (1.8 s)
    var loftFairyLightsTimer: Float by mutableFloatStateOf(0f) // drives string lights cascading twinkle (1.6 s)
    var loftWindowTimer: Float by mutableFloatStateOf(0f)      // drives window panel sliding open with breeze/leaves (2.0 s)

    // Interactive Cozy Objects states and timers
    var hearthCandleLit: Boolean by mutableStateOf(false)
    var teakettleWhistleTimer: Float by mutableFloatStateOf(0f)
    var cuddleBlanketTimer: Float by mutableFloatStateOf(0f)
    var windChimeSwayTimer: Float by mutableFloatStateOf(0f)
    var featherWandWiggleTimer: Float by mutableFloatStateOf(0f)
    var plantWaterTimer: Float by mutableFloatStateOf(0f)
    var telescopeStarTimer: Float by mutableFloatStateOf(0f)

    var lightningFlashAlpha: Float by mutableFloatStateOf(0f)
    var lightningTimer: Float = 0f
    private var nextLightningTime: Float = 14f
    private var isRainAmbientPlaying: Boolean = false

    var gardenStage: Int by mutableIntStateOf(0)
    /** Keepsake plants earned by total visit days; only ever grows. */
    var gardenBlooms: List<com.example.data.GardenBloom> by mutableStateOf(emptyList())
    var homeEvolutionState: com.example.data.HomeEvolutionState by mutableStateOf(com.example.data.HomeEvolutionState())
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
    var boySpeechTimer: Float = 0f

    var girlSpeechText: String? by mutableStateOf(null)
    var girlSpeechTimer: Float = 0f
    var rideBoySpoke: Boolean = false
    var rideGirlSpoke: Boolean = false

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
    private var nextAutonomousInterval = 14f
    private var lastIdleBehavior = -1
    private var boyIdleWaveTimer = 0f
    private var girlIdleWaveTimer = 0f
    private var boyIdleWaveReturnPose: CharacterPose? = null
    private var girlIdleWaveReturnPose: CharacterPose? = null
    private var groundInteractionCharacter: PixelCharacter? = null
    private var groundInteractionWait = 0f
    private var groundInteractionX = 0f
    private var groundInteractionY = 0f

    /** Where each character rests in scenes without a scripted idle loop (cafe, sunroom, campfire). */
    private data class HomeSpot(val x: Float, val y: Float, val pose: CharacterPose, val direction: Direction)
    private var boyHome = HomeSpot(0.43f, 0.68f, CharacterPose.IDLE, Direction.RIGHT)
    private var girlHome = HomeSpot(0.57f, 0.68f, CharacterPose.IDLE, Direction.LEFT)
    private var boyAwayIdle = 0f
    private var girlAwayIdle = 0f

    /** Smooth sitting/snuggle/kiss offset interpolation progress (0f = separated, 1f = fully cuddled) */
    var cuddleProgress: Float by mutableFloatStateOf(0f)

    // Living Room autonomous return timer (walks back to couch and sits)
    var livingRoomReturnTimer: Float = 0f
    var livingRoomReturnTargetX: Float = 0.44f
    var livingRoomReturnChar: PixelCharacter? = null

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

    private val worldEventListener = com.example.data.WorldEventListener { event ->
        triggerWorldEventReaction(event)
    }

    // ── Autonomous behavior state (declared before init, which loads the first scene) ──
    /** Master switch, e.g. for tests that need the couple to stay put. */
    var autonomyEnabled: Boolean = true

    /** Exposed for tests: the boy's and girl's routine. */
    val boyAgent = AutonomyAgent()
    val girlAgent = AutonomyAgent()
    val behaviorBrain = BehaviorBrain()
    private val behaviorContext = BehaviorContext()
    private val walkBounds = FloatArray(4) // minX, maxX, minY, maxY
    private var autonomyUserPause = 0f
    private var autonomyHomeCaptured = false
    /** Seconds until any rare event may happen again (shared by the couple). */
    var rareEventCooldown = RARE_EVENT_FIRST_DELAY
    private var mochiZoomTimer = 0f

    /** The last few things the couple did on their own (newest last); for tests and debugging. */
    val autonomyLog = ArrayDeque<Behavior>()

    val discovery = Discovery()

    /** Small requests from the couple (plan 07 D1): timing here, bubble drawn from it. */
    val requests = CoupleRequests()

    /** Birthdays, sealed letters and parties (plan 09, A); set by the app once it has storage. */
    var birthdayStore: com.example.data.BirthdayStore? by mutableStateOf(null)
    /** Today's birthday surprise, if there is one; the overlay reads it too. */
    val birthdaySurprise = BirthdaySurprise()
    /** Set by the overlay while it's on screen, so the wish and the letter wait for it. */
    var birthdayOverlayShowing = false
    private var birthdayCheckedDay = Long.MIN_VALUE
    private var birthdayLinesSaid = 0
    /** Canvas size from the last frame, for reactions to taps that don't pass it. */
    private var lastCanvasW = 1f
    private var lastCanvasH = 1f
    private var discoverySpawnTimer = DISCOVERY_FIRST_DELAY

    init {
        loadScene(SceneType.FLOWER)
        audio.setIndoor(!isCurrentSceneOutdoor, smooth = false)
        if (audio.isEnabled) {
            audio.playWeatherBgm(weather, isAutomaticDrift = false)
        }
        com.example.data.WorldEventBus.subscribe(worldEventListener)
    }

    fun triggerWorldEventReaction(event: com.example.data.WorldEvent) {
        if (isWatchSceneActive) return // Respect cinematic priority

        when (event) {
            is com.example.data.WorldEvent.PartnerSignalReceived -> {
                val receiver = if (event.sender.equals("boy", ignoreCase = true)) girl else boy
                receiver.reactionTimer = 3.5f
                receiver.emotion = CharacterEmotion.LOVING
                receiver.emote = when (event.signalTypeName) {
                    "Need a Hug" -> EmoteType.HEART
                    "Thinking of You" -> EmoteType.DOTS
                    "Good Morning" -> EmoteType.SPARKLE
                    "Good Night" -> EmoteType.SLEEP_Z
                    else -> EmoteType.HEART
                }
                receiver.emoteTimer = 3.0f
                audio.playHeartChime()
                particles.spawnHeart(800f * receiver.worldX, 600f * receiver.worldY)
            }
            is com.example.data.WorldEvent.DateAdventureCompleted -> {
                boy.reactionTimer = 3.0f
                girl.reactionTimer = 3.0f
                boy.pose = CharacterPose.JOY_JUMP
                girl.pose = CharacterPose.JOY_JUMP
                boy.emote = EmoteType.SPARKLE
                girl.emote = EmoteType.SPARKLE
                boy.emoteTimer = 2.8f
                girl.emoteTimer = 2.8f
                audio.playHeartChime()
                particles.spawnHeart(400f, 300f)
            }
            is com.example.data.WorldEvent.TinyMomentCompleted -> {
                boy.reactionTimer = 2.8f
                girl.reactionTimer = 2.8f
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.MUSIC_NOTE
                girl.emote = EmoteType.MUSIC_NOTE
                boy.emoteTimer = 2.5f
                girl.emoteTimer = 2.5f
                audio.playHeartChime()
            }
            is com.example.data.WorldEvent.MiniGameCompleted -> reactToTinyGame(event.isMatch)
            is com.example.data.WorldEvent.SharedMoodChanged -> {
                when (event.moodName) {
                    "Tired" -> {
                        val target = if (event.partner.equals("boy", ignoreCase = true)) boy else girl
                        target.reactionTimer = 3.5f
                        target.emotion = CharacterEmotion.SLEEPY
                        target.pose = CharacterPose.SLEEP_YAWN
                        catState = CatState.SLEEPING
                    }
                    "Missing You" -> {
                        val target = if (event.partner.equals("boy", ignoreCase = true)) boy else girl
                        target.reactionTimer = 3.0f
                        target.emotion = CharacterEmotion.SHY
                        target.emote = EmoteType.HEART
                        target.emoteTimer = 2.5f
                    }
                    "Great" -> {
                        val target = if (event.partner.equals("boy", ignoreCase = true)) boy else girl
                        target.reactionTimer = 2.5f
                        target.emotion = CharacterEmotion.HAPPY
                        target.emote = EmoteType.SPARKLE
                        target.emoteTimer = 2.0f
                    }
                    else -> {}
                }
            }
            else -> {}
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
        cozy.stopAll()
        boy.putAwayHeldItem()
        girl.putAwayHeldItem()
        flowerHandoverFrom = null
        onProgress?.invoke(com.example.progress.ProgressEvent.SceneVisited(type.name))
        particles.placePuddles(WeatherLayout.puddleSpotsFor(type))
        audio.setIndoor(!isCurrentSceneOutdoor, smooth = true)
        sceneTime = 0f
        sceneMessage = null
        messageAlpha = 0f
        messageTimer = 0f
        ambientDimming = 0f
        autonomousTimer = 0f
        lastIdleBehavior = -1
        boyIdleWaveTimer = 0f
        girlIdleWaveTimer = 0f
        boyIdleWaveReturnPose = null
        girlIdleWaveReturnPose = null
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
        rideFireflyTimer = 0f
        loftTableTimer = 0f
        loftBookNookTimer = 0f
        loftFairyLightsTimer = 0f
        loftWindowTimer = 0f
        livingRoomReturnTimer = 0f
        livingRoomReturnChar = null
        groundInteractionCharacter = null
        groundInteractionWait = 0f
        cuddleProgress = 0f
        teakettleWhistleTimer = 0f
        cuddleBlanketTimer = 0f
        windChimeSwayTimer = 0f
        featherWandWiggleTimer = 0f
        plantWaterTimer = 0f
        telescopeStarTimer = 0f
        cafeLatteTimer = 0f
        cafeWindowHeartTimer = 0f
        cafePastryBites = 0
        sunroomSkylightTimer = 0f
        sunroomMistTimer = 0f
        catTreatJarTimer = 0f
        catTreatDropTimer = 0f
        catTreatMunchTimer = 0f
        campfireEmbersTimer = 0f
        campLanternLit = true
        campGuitarStrumTimer = 0f
        marshmallowRoastingTimer = 0f
        cafeBaristaBrewTimer = 0f
        cafePupPetTimer = 0f
        pierIceCreamTimer = 0f
        pierLighthouseTimer = 0f
        pierFishingPhase = PierFishingPhase.IDLE
        pierFishingTimer = 0f
        pierLastCatch = null
        pierBottleVisible = true
        pierBottleRespawnTimer = 0f
        pierGullState = GullState.AWAY
        pierGullX = 1.1f
        pierGullY = 0.20f
        pierGullTimer = 2.5f
        pierBaoSipTimer = 0f
        pierBaoWaveTimer = 0f
        pierBaoSipCooldown = 6f
        pierBaoSelfCastTimer = 14f
        pierBaoGreetPending = type == SceneType.SEASIDE_PIER
        pierBaoQuietTime = 0f
        pierDolphinTimer = 0f
        pierBoatHornTimer = 0f
        pierFlockTimer = 0f
        pierBucketFlopTimer = 0f
        pierLightsSparkleTimer = 0f
        pierCrabX = 0.36f
        pierCrabFacingLeft = false
        pierCrabStartledTimer = 0f
        pierCrabHiddenTimer = 0f
        pierCrabPauseTimer = 1.5f
        catTreatInProgress = false
        catTreatWalking = false

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
            SceneType.RAINY_CAFE -> {
                boy.worldX = 0.40f
                boy.worldY = 0.68f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.60f
                girl.worldY = 0.68f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = true
                catState = CatState.SLEEPING
                catWorldX = 0.84f
                catWorldY = 0.70f
            }
            SceneType.SUNROOM -> {
                boy.worldX = 0.42f
                boy.worldY = 0.68f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.HAPPY
                boy.emote = EmoteType.NONE

                girl.worldX = 0.58f
                girl.worldY = 0.68f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.IDLE
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = false
                catState = CatState.SITTING_PURR
                catWorldX = 0.82f
                catWorldY = 0.70f
            }
            SceneType.CAMPFIRE -> {
                boy.worldX = 0.38f
                boy.worldY = 0.68f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.58f
                girl.worldY = 0.68f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = true
                catState = CatState.SLEEPING
                catWorldX = 0.76f
                catWorldY = 0.70f
            }
            SceneType.SEASIDE_PIER -> {
                boy.worldX = 0.42f
                boy.worldY = 0.74f
                boy.direction = Direction.RIGHT
                boy.pose = CharacterPose.SIT
                boy.emotion = CharacterEmotion.LOVING
                boy.emote = EmoteType.NONE

                girl.worldX = 0.56f
                girl.worldY = 0.74f
                girl.direction = Direction.LEFT
                girl.pose = CharacterPose.SIT_SNUGGLE
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.NONE

                catSleeping = false
                catState = CatState.SITTING_PURR
                catWorldX = 0.66f
                catWorldY = 0.73f
            }
        }
        catTargetX = catWorldX
        catTargetY = catWorldY
        catFacingLeft = false
        boyHome = HomeSpot(boy.worldX, boy.worldY, boy.pose, boy.direction)
        girlHome = HomeSpot(girl.worldX, girl.worldY, girl.pose, girl.direction)
        resetAutonomy()
        boyAwayIdle = 0f
        girlAwayIdle = 0f
    }

    fun setRoomTheme(theme: RoomTheme, announce: Boolean = true) {
        if (roomTheme == theme) return
        roomTheme = theme
        if (announce) {
            audio.playBubblePop()
            showMessage(GameText.get(Res.string.scene_is_ready_for_a_cozy_evening, theme.title), duration = 2.4f)
        }
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
        boyfriendInitial = boyName.trim().firstOrNull()?.uppercaseChar() ?: 'H'
        girlfriendInitial = girlName.trim().firstOrNull()?.uppercaseChar() ?: 'H'
    }

    fun computeTreeMossGrowthStage(currentDate: kotlinx.datetime.LocalDate = com.example.data.CoupleDates.today()): Int {
        treeMossDebugYearOverride?.let { return it.coerceIn(0, 4) }
        val years = com.example.data.CoupleDates.anniversary.periodUntil(currentDate).years
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
        if (specialDayGreetingHold > 0f) specialDayGreetingHold -= deltaSeconds
        updateCatchGame(deltaSeconds, canvasWidth, canvasHeight)
        if (mochiMeterTimer > 0f) mochiMeterTimer = (mochiMeterTimer - deltaSeconds).coerceAtLeast(0f)
        lastWorldW = canvasWidth
        lastWorldH = canvasHeight
        cozy.update(deltaSeconds, canvasWidth, canvasHeight)
        updateBirthday(deltaSeconds, canvasWidth, canvasHeight)
        updateCoupleLife(deltaSeconds)
        updateFestival(deltaSeconds)
        starPuzzle.update(deltaSeconds)
        // The stars are only there at night outdoors.
        if (starPuzzle.current != null && (!timeOfDayPhase.isNight || !isCurrentSceneOutdoor)) starPuzzle.stop()
        val pixelScale = WorldViewport.pixelScale(canvasWidth)

        // Per-Scene Cinematic Watch Sequences
        if (isWatchSceneActive) {
            watchSceneTimer += deltaSeconds
            val t = watchSceneTimer
            val cw = canvasWidth
            val ch = canvasHeight
            val pxScale = WorldViewport.pixelScale(cw)

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
                                showMessage(GameText.get(Res.string.scene_for_you_always), duration = 4.0f)
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
                            if (eventChance(0.9f, deltaSeconds)) {
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
                                showMessage(GameText.get(Res.string.scene_see_that_star_that_is_ours), duration = 4.5f)
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
                            if (eventChance(0.42f, deltaSeconds)) {
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
                                showMessage(GameText.get(Res.string.scene_hey_that_is_mine), duration = 3.0f)
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
                                boySpeechText = GameText.get(Res.string.scene_you_little_thief)
                                boySpeechTimer = 3.0f
                                girlSpeechText = GameText.get(Res.string.scene_so_yummy_though)
                                girlSpeechTimer = 3.0f
                            }
                            if (eventChance(0.9f, deltaSeconds)) {
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
                            showMessage(GameText.get(Res.string.scene_come_lets_rest_a_while), duration = 3.0f)
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
                            if (eventChance(0.52f, deltaSeconds)) {
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
                                showMessage(GameText.get(Res.string.scene_the_lanterns_are_so_beautiful_tonight), duration = 4.0f)
                                boySpeechText = GameText.get(Res.string.scene_look_at_the_lanterns)
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
                            if (eventChance(0.7f, deltaSeconds)) {
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
                                boySpeechText = GameText.get(Res.string.scene_see_those_two_stars)
                                boySpeechTimer = 3.5f
                                showMessage(GameText.get(Res.string.scene_the_two_hearts_constellation_just_like_u), duration = 5.0f)
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
                                girlSpeechText = GameText.get(Res.string.scene_that_is_us_up_there)
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
                            if (eventChance(0.7f, deltaSeconds)) {
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
                                boySpeechText = GameText.get(Res.string.scene_first_momo_for_you)
                                boySpeechTimer = 3.0f
                                showMessage(GameText.get(Res.string.scene_fresh_and_hot_just_for_my_love), duration = 4.0f)
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
                                girlSpeechText = GameText.get(Res.string.scene_so_yummy)
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
                            if (eventChance(1.05f, deltaSeconds)) {
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
                                girlSpeechText = GameText.get(Res.string.scene_not_too_fast)
                                girlSpeechTimer = 3.0f
                                boySpeechText = GameText.get(Res.string.scene_hold_on_tight)
                                boySpeechTimer = 3.0f
                                audio.playHeartChime()
                                showMessage(GameText.get(Res.string.scene_racing_down_the_open_road), duration = 4.0f)
                            }
                        }
                        t < endTime -> {
                            // Slow down, boy grins, hearts trail
                            boy.pose = CharacterPose.IDLE
                            girl.pose = CharacterPose.IDLE
                            boy.emotion = CharacterEmotion.LOVING
                            girl.emotion = CharacterEmotion.LOVING
                            if (eventChance(1.05f, deltaSeconds)) {
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
                                boySpeechText = GameText.get(Res.string.scene_dance_with_me)
                                boySpeechTimer = 3.0f
                                showMessage(GameText.get(Res.string.scene_the_record_player_hums_softly), duration = 4.0f)
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
                            if (eventChance(0.7f, deltaSeconds)) {
                                particles.spawnSparkles(cw * 0.55f + (kotlin.random.Random.nextFloat() - 0.5f) * 30f, ch * 0.45f, 1)
                            }
                            if (eventChance(0.62f, deltaSeconds)) {
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

                SceneType.SEASIDE_PIER -> {
                    if (!updatePierWatchScene(t, deltaSeconds, cw, ch)) isWatchSceneActive = false
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
                                boySpeechText = GameText.get(Res.string.scene_i_love_u, girl.name)
                                boySpeechTimer = 5.5f
                                girlSpeechText = GameText.get(Res.string.scene_i_love_u, boy.name)
                                girlSpeechTimer = 5.5f
                                audio.playHeartChime()
                                showMessage(GameText.get(Res.string.scene_forever_and_always, boy.name, girl.name), duration = 5.0f)
                                particles.spawnHeart(canvasWidth * kissMidX, canvasHeight * boy.worldY - 32f * pxScale)
                                particles.spawnSparkles(canvasWidth * kissMidX, canvasHeight * boy.worldY - 26f * pxScale, 4)
                            }
                            if (eventChance(1.05f, deltaSeconds)) {
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
            if (eventChance(1.1f, deltaSeconds)) {
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
        updateIdleWaveReturn(boy, deltaSeconds, true)
        updateIdleWaveReturn(girl, deltaSeconds, false)
        this.updateGroundInteraction(deltaSeconds, canvasWidth, canvasHeight)

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

        // Living breathing offset (upper body chest rise/fall during idle and sleep)
        val boyBreathFreq = if (boy.pose == CharacterPose.SLEEP || boy.pose == CharacterPose.SLEEP_YAWN) 1.5f else 2.1f
        val girlBreathFreq = if (girl.pose == CharacterPose.SLEEP || girl.pose == CharacterPose.SLEEP_YAWN) 1.6f else 2.2f

        boy.breathingOffset = if (!isWalking && !boy.isTransitioningPosition) {
            (sin(sceneTime * boyBreathFreq) * 0.85f).coerceIn(-1.2f, 1.2f)
        } else 0f

        girl.breathingOffset = if (!isGirlWalking && !girl.isTransitioningPosition) {
            (sin(sceneTime * girlBreathFreq + 0.9f) * 0.85f).coerceIn(-1.2f, 1.2f)
        } else 0f

        // Natural gentle idle sway (subtle organic weight shift during idle standing)
        val isBoyStandingIdle = (boy.pose == CharacterPose.IDLE || boy.pose == CharacterPose.IDLE_BLINK)
        boy.idleSwayOffset = if (isBoyStandingIdle && !boy.isMovingOrTransitioning) {
            sin(sceneTime * 0.95f) * 0.6f
        } else 0f

        val isGirlStandingIdle = (girl.pose == CharacterPose.IDLE || girl.pose == CharacterPose.IDLE_BLINK)
        girl.idleSwayOffset = if (isGirlStandingIdle && !girl.isMovingOrTransitioning) {
            sin(sceneTime * 0.90f + 1.3f) * 0.6f
        } else 0f

        tickHeldItem(boy, deltaSeconds)
        tickHeldItem(girl, deltaSeconds)
        tickFlowerHandover(deltaSeconds)

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
        if ((boy.pose == CharacterPose.HUG || boy.pose == CharacterPose.KISS) && eventChance(1.4f, deltaSeconds)) {
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
            SceneType.RAINY_CAFE, SceneType.SUNROOM, SceneType.CAMPFIRE, SceneType.SEASIDE_PIER -> sceneTime < 1.5f // no opening script; just the scene wipe
        }
        if (autonomyEnabled) {
            updateAutonomy(deltaSeconds, canvasWidth, canvasHeight, isScriptActive)
        } else {
            autonomousTimer += deltaSeconds
            if (!isScriptActive && !isWatchSceneActive && autonomousTimer >= nextAutonomousInterval &&
                boy.reactionTimer <= 0 && girl.reactionTimer <= 0 &&
                !boy.isMovingOrTransitioning && !girl.isMovingOrTransitioning &&
                livingRoomReturnTimer <= 0f) {
                autonomousTimer = 0f
                nextAutonomousInterval = 11f + Random.nextFloat() * 9f
                triggerAutonomousMoment(canvasWidth, canvasHeight)
            }
        }

        // Living Dynamic Weather System (particles & soundscapes): the weather changes now and
        // then, within the real season (SeasonalWeather); a weather picked by hand holds a while.
        if (manualWeatherHold > 0f) manualWeatherHold -= deltaSeconds
        if (weatherDriftEnabled && manualWeatherHold <= 0f) {
            weatherDriftTimer += deltaSeconds
            if (weatherDriftTimer >= weatherDriftInterval) {
                weatherDriftTimer = 0f
                driftWeather()
            }
        }
        hourCheckTimer += deltaSeconds
        if (hourCheckTimer >= 30f) {
            hourCheckTimer = 0f
            timeOfDayPhase = TimeOfDayPhase.resolve(atmosphereMode)
        }
        updateWeatherKeepsakes(deltaSeconds)
        updateGroundWeatherPlay(deltaSeconds, canvasWidth, canvasHeight)
        val isOutdoor = isCurrentSceneOutdoor
        if (audio.isIndoor != (!isOutdoor)) {
            audio.setIndoor(!isOutdoor, smooth = true)
        }
        if (audio.isEnabled && audio.currentWeather == null) {
            audio.playWeatherBgm(weather, isAutomaticDrift = false)
        }
        val isNight = timeOfDayPhase.isNight
        // Match rain contact to the visible floor in each outdoor backdrop. Path scenes
        // include their raised cobbled strip; meadow scenes meet the grass sooner.
        val rainGroundY = when (currentScene) {
            SceneType.FLOWER, SceneType.UNDER_TREE, SceneType.LOOKING, SceneType.CAMPFIRE -> canvasHeight * 0.74f
            SceneType.WALK, SceneType.MOMO_STALL -> canvasHeight * 0.66f + 30f * pixelScale
            SceneType.EVENING_RIDE -> canvasHeight * 0.73f
            else -> canvasHeight * 0.84f
        }
        // Spawn and update weather particles every frame!
        particles.updateWeatherEffects(
            weather, canvasWidth, canvasHeight, isOutdoor, isNight, deltaSeconds, rainGroundY
        )

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
                    if (eventChance(0.08f, deltaSeconds)) audio.playLeafRustle()
                }
                WeatherType.SAKURA -> {
                    if (eventChance(0.035f, deltaSeconds)) audio.playStarTwinkle()
                }
                WeatherType.SUNNY -> {
                    if (!isNight && eventChance(0.10f, deltaSeconds)) audio.playBirdChirp()
                }
                WeatherType.SNOW -> {
                    if (eventChance(0.025f, deltaSeconds)) audio.playStarTwinkle()
                }
                else -> {}
            }
        }

        // Cute sleeping Z particles rising from sleeping cat
        if (catState == CatState.SLEEPING && currentScene != SceneType.EVENING_RIDE) {
            if (eventChance(0.18f, deltaSeconds)) {
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
        if (cafeLatteTimer > 0f) cafeLatteTimer = (cafeLatteTimer - deltaSeconds).coerceAtLeast(0f)
        if (cafeWindowHeartTimer > 0f) cafeWindowHeartTimer = (cafeWindowHeartTimer - deltaSeconds).coerceAtLeast(0f)
        if (sunroomSkylightTimer > 0f) sunroomSkylightTimer = (sunroomSkylightTimer - deltaSeconds).coerceAtLeast(0f)
        if (sunroomMistTimer > 0f) sunroomMistTimer = (sunroomMistTimer - deltaSeconds).coerceAtLeast(0f)
        if (catTreatJarTimer > 0f) catTreatJarTimer = (catTreatJarTimer - deltaSeconds).coerceAtLeast(0f)
        if (catTreatDropTimer > 0f) catTreatDropTimer = (catTreatDropTimer - deltaSeconds).coerceAtLeast(0f)
        if (campfireEmbersTimer > 0f) campfireEmbersTimer = (campfireEmbersTimer - deltaSeconds).coerceAtLeast(0f)
        if (campGuitarStrumTimer > 0f) campGuitarStrumTimer = (campGuitarStrumTimer - deltaSeconds).coerceAtLeast(0f)
        if (marshmallowRoastingTimer > 0f) marshmallowRoastingTimer = (marshmallowRoastingTimer - deltaSeconds).coerceAtLeast(0f)
        if (cafeBaristaBrewTimer > 0f) cafeBaristaBrewTimer = (cafeBaristaBrewTimer - deltaSeconds).coerceAtLeast(0f)
        if (cafePupPetTimer > 0f) cafePupPetTimer = (cafePupPetTimer - deltaSeconds).coerceAtLeast(0f)
        if (pierIceCreamTimer > 0f) pierIceCreamTimer = (pierIceCreamTimer - deltaSeconds).coerceAtLeast(0f)
        if (pierLighthouseTimer > 0f) pierLighthouseTimer = (pierLighthouseTimer - deltaSeconds).coerceAtLeast(0f)

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
        if (rideFireflyTimer > 0f) rideFireflyTimer = (rideFireflyTimer - deltaSeconds).coerceAtLeast(0f)

        // Midnight Loft tap-interaction countdowns
        if (loftTableTimer > 0f) loftTableTimer = (loftTableTimer - deltaSeconds).coerceAtLeast(0f)
        if (loftBookNookTimer > 0f) loftBookNookTimer = (loftBookNookTimer - deltaSeconds).coerceAtLeast(0f)
        if (loftFairyLightsTimer > 0f) loftFairyLightsTimer = (loftFairyLightsTimer - deltaSeconds).coerceAtLeast(0f)
        if (loftWindowTimer > 0f) loftWindowTimer = (loftWindowTimer - deltaSeconds).coerceAtLeast(0f)

        // Interactive Cozy Objects countdowns
        if (teakettleWhistleTimer > 0f) teakettleWhistleTimer = (teakettleWhistleTimer - deltaSeconds).coerceAtLeast(0f)
        if (cuddleBlanketTimer > 0f) cuddleBlanketTimer = (cuddleBlanketTimer - deltaSeconds).coerceAtLeast(0f)
        if (windChimeSwayTimer > 0f) windChimeSwayTimer = (windChimeSwayTimer - deltaSeconds).coerceAtLeast(0f)
        if (featherWandWiggleTimer > 0f) featherWandWiggleTimer = (featherWandWiggleTimer - deltaSeconds).coerceAtLeast(0f)
        if (plantWaterTimer > 0f) plantWaterTimer = (plantWaterTimer - deltaSeconds).coerceAtLeast(0f)
        if (telescopeStarTimer > 0f) telescopeStarTimer = (telescopeStarTimer - deltaSeconds).coerceAtLeast(0f)

        // Feature 1: Mochi Matchmaker countdown timers
        if (mochiMatchmakerCooldown > 0f) {
            mochiMatchmakerCooldown = (mochiMatchmakerCooldown - deltaSeconds).coerceAtLeast(0f)
        }
        if (mochiNudgeActiveTimer > 0f) {
            mochiNudgeActiveTimer = (mochiNudgeActiveTimer - deltaSeconds).coerceAtLeast(0f)
        }

        // Autonomous Pet Personality behavior cycle (calm, living pet pacing)
        petBehaviorTimer += deltaSeconds
        if (petBehaviorTimer >= nextPetBehaviorInterval && !isWatchSceneActive) {
            petBehaviorTimer = 0f
            nextPetBehaviorInterval = 12f + Random.nextFloat() * 16f
            if (!mochiMatchmakerActive && canTriggerMochiMatchmaker()) {
                triggerMochiMatchmaker(canvasWidth, canvasHeight)
            } else if (!mochiMatchmakerActive) {
                triggerAutonomousPetBehavior(canvasWidth, canvasHeight)
            }
        }

        // Continuous smooth cat roaming across the scene
        updateCatMovement(deltaSeconds, canvasWidth, canvasHeight)
        updateKitchenTreat(deltaSeconds, canvasWidth, canvasHeight)

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
            SceneType.RAINY_CAFE, SceneType.SUNROOM, SceneType.CAMPFIRE -> {
                boyAwayIdle = settleToHome(boy, boyHome, boyAwayIdle, boyIdleWaveTimer, deltaSeconds)
                girlAwayIdle = settleToHome(girl, girlHome, girlAwayIdle, girlIdleWaveTimer, deltaSeconds)
            }
            SceneType.SEASIDE_PIER -> {
                boyAwayIdle = settleToHome(boy, boyHome, boyAwayIdle, boyIdleWaveTimer, deltaSeconds)
                girlAwayIdle = settleToHome(girl, girlHome, girlAwayIdle, girlIdleWaveTimer, deltaSeconds)
                updatePierScene(deltaSeconds, canvasWidth, canvasHeight)
            }
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
                showMessage(GameText.get(Res.string.scene_for_you), duration = 4.5f)
                particles.spawnPetals(cw * 0.50f, ch * 0.60f, count = 10)
            }
        } else if (t < 11.0f) {
            // Stand close holding hands
            if (idleLoopMayPose(boy)) boy.pose = CharacterPose.HOLD_HANDS
            if (idleLoopMayPose(girl)) girl.pose = CharacterPose.RECEIVE_FLOWER
        } else {
            // Cozy living idle together
            if (idleLoopMayPose(boy)) boy.pose = CharacterPose.HOLD_HANDS
            if (idleLoopMayPose(girl)) girl.pose = CharacterPose.RECEIVE_FLOWER
            if (eventChance(0.35f, dt)) {
                particles.spawnPetals(cw * Random.nextFloat(), ch * 0.35f, 1)
            }
        }
    }

    private fun updateTreeScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        // Leaves constantly drift down from canopy
        if (eventChance(0.8f, dt)) {
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
                showMessage(GameText.get(Res.string.scene_just_being_here_with_you_is_my_favorite), duration = 4.5f)
                particles.spawnHeart(cw * 0.5f, ch * 0.58f)
            }
        } else {
            if (idleLoopMayPose(boy)) boy.pose = CharacterPose.SIT_SNUGGLE
            if (idleLoopMayPose(girl)) girl.pose = CharacterPose.SIT_SNUGGLE
            if (eventChance(0.18f, dt)) {
                audio.playBirdChirp()
            }
        }
    }

    private fun updateCookingScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime

        // Stew pot always simmering with steam & bubbling
        if (eventChance(3.0f, dt)) {
            particles.spawnSteam(cw * 0.58f, ch * 0.60f)
        }
        if (eventChance(0.18f, dt)) {
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
                showMessage(GameText.get(Res.string.scene_caught_you_you_can_have_a_taste), duration = 4.0f)
                particles.spawnHeart(cw * 0.48f, ch * 0.58f)
            }
        } else {
            // Sharing food happily together on the rug
            if (idleLoopMayPose(boy)) {
                boy.pose = CharacterPose.EAT_SNEAK
            }
            if (idleLoopMayPose(girl)) {
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
                if (idleLoopMayPose(boy) && boy.pose != CharacterPose.SIT_SNUGGLE) {
                    boy.pose = CharacterPose.SIT
                }
                if (idleLoopMayPose(girl) && girl.pose != CharacterPose.SIT_SNUGGLE) {
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
                            showMessage(GameText.get(Res.string.scene_drifting_off_to_sleep_as_midnight_falls), duration = 2.5f)
                        }
                    } else {
                        // Sleeping peacefully
                        if (!boy.isMovingOrTransitioning && !girl.isMovingOrTransitioning) {
                            if (boy.reactionTimer <= 0) boy.pose = CharacterPose.SLEEP
                            if (girl.reactionTimer <= 0) girl.pose = CharacterPose.SLEEP
                        }
                        catSleeping = true
                        catState = CatState.SLEEPING
                        if (eventChance(0.55f, dt)) {
                            particles.spawnSleepZ(cw * 0.44f, ch * 0.55f)
                            particles.spawnSleepZ(cw * 0.56f, ch * 0.55f)
                        }
                        if (couchPhaseTimer in 3.2f..4.0f && sceneMessage == null) {
                            showMessage(GameText.get(Res.string.scene_sweet_dreams_my_love), duration = 3.5f)
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
                            showMessage(GameText.get(Res.string.scene_morning_light_brings_another_day_with_yo), duration = 3.0f)
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
                        showMessage(GameText.get(Res.string.scene_bright_midday_sunshine_streams_through_o), duration = 3.0f)
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
                        showMessage(GameText.get(Res.string.scene_golden_hour_twilight_peaceful_evening_to), duration = 3.0f)
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
                showMessage(GameText.get(Res.string.scene_wherever_we_go_as_long_as_it_s_together), duration = 4.5f)
                audio.playStarTwinkle()
                particles.spawnShootingStar(cw * 0.2f, ch * 0.1f)
                particles.spawnSparkles(cw * 0.65f, ch * 0.22f, 8)
            }
        } else {
            // Holding hands under the lantern light
            if (idleLoopMayPose(boy)) boy.pose = CharacterPose.HOLD_HANDS
            if (idleLoopMayPose(girl)) girl.pose = CharacterPose.HOLD_HANDS
            if (!autonomyHolds(boy)) boy.direction = Direction.RIGHT
            if (!autonomyHolds(girl)) girl.direction = Direction.LEFT
            if (eventChance(0.20f, dt)) {
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
                showMessage(GameText.get(Res.string.scene_i_love_you_just_wanted_to_look_at_you), duration = 4.5f)
                particles.spawnHeart(cw * 0.50f, ch * 0.52f, Color(0xFFFF3366))
            }
        } else {
            if (idleLoopMayPose(boy)) boy.pose = CharacterPose.HOLD_HANDS
            if (idleLoopMayPose(girl)) girl.pose = CharacterPose.HOLD_HANDS
            if (eventChance(0.20f, dt)) {
                particles.spawnSparkles(cw * 0.50f, ch * 0.65f, 3)
            }
        }
    }

    /**
     * Scenes without a scripted idle loop settle each character back to its seat once a
     * tap reaction, watch scene or floor stroll is over, so reaction poses never stick.
     * Returns the new away-from-home linger time; a negative value marks a walk home in progress.
     */
    private fun settleToHome(c: PixelCharacter, home: HomeSpot, awayIdle: Float, waveTimer: Float, dt: Float): Float {
        if (autonomyHolds(c)) return 0f
        if (isWatchSceneActive || c.reactionTimer > 0f || c.isMovingOrTransitioning ||
            waveTimer > 0f || groundInteractionCharacter === c
        ) return if (awayIdle < 0f) awayIdle else 0f

        if (kotlin.math.hypot(c.worldX - home.x, c.worldY - home.y) > 0.01f) {
            // After a floor stroll, linger a moment before heading back; leftover
            // reaction poses (a kiss, a jump) head home straight away.
            val linger = awayIdle.coerceAtLeast(0f) + dt
            if (c.pose == CharacterPose.IDLE && linger < 3.5f) return linger
            c.moveTo(home.x, home.y, arrivePose = home.pose)
            return -1f
        }
        if (c.pose != home.pose || awayIdle < 0f) {
            c.transitionPoseTo(home.pose)
            c.direction = home.direction
        }
        return 0f
    }

    private fun updateMomoScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime
        val pixelScale = WorldViewport.pixelScale(cw)

        // Continuous steam wisps rising softly from the momo steamer
        if (eventChance(1.8f, dt)) {
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
                showMessage(GameText.get(Res.string.scene_welcome_to_our_cozy_street_food_stall), duration = 3.5f)
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
                showMessage(GameText.get(Res.string.scene_steaming_hot_momos_with_extra_spicy_chut), duration = 3.5f)
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
                showMessage(GameText.get(Res.string.scene_everything_tastes_sweeter_with_you), duration = 4.0f)
                particles.spawnSparkles(cw * 0.5f, ch * 0.65f, 5)
            }
        } else {
            // Sweet idle momo date loop
            if (idleLoopMayPose(boy)) {
                boy.pose = CharacterPose.FEED_MOMO
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (idleLoopMayPose(girl)) {
                girl.pose = CharacterPose.EAT_MOMO
                girl.direction = Direction.LEFT
                girl.emotion = CharacterEmotion.HAPPY
            }
        }
    }

    private fun updateEveningRideScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime
        // Subtle roadside breeze & wind particles
        if (eventChance(0.75f, dt)) {
            particles.spawnWindBreezeStreak(cw + 30f, ch * (0.64f + Random.nextFloat() * 0.14f), cw)
        }

        if (t < 2.5f) {
            if (sceneMessage == null && t > 0.8f) {
                showMessage(GameText.get(Res.string.scene_evening_scooter_ride_together_through_th), duration = 3.5f)
            }
        } else if (t >= 3.0f && !rideBoySpoke && boySpeechText == null && girlSpeechText == null) {
            rideBoySpoke = true
            boySpeechText = GameText.get(Res.string.scene_holding_you_forever_my_love)
            boySpeechTimer = 3.5f
            boy.emotion = CharacterEmotion.LOVING
            particles.spawnHeart(cw * 0.44f, ch * 0.60f, Color(0xFFFF3366))
        } else if (t >= 6.8f && !rideGirlSpoke && girlSpeechText == null && boySpeechText == null) {
            rideGirlSpoke = true
            girlSpeechText = GameText.get(Res.string.scene_hold_tight_the_scooter_is_fast_hehe, boy.name)
            girlSpeechTimer = 3.5f
            girl.emotion = CharacterEmotion.HAPPY
            particles.spawnHeart(cw * 0.52f, ch * 0.60f, Color(0xFFFF5D8F))
        }
    }

    private fun updateCozyLoftScene(dt: Float, cw: Float, ch: Float) {
        val t = sceneTime
        val pixelScale = WorldViewport.pixelScale(cw)
        val floorY = ch * 0.58f

        // 1. Gentle steam wisps rising from hot mugs on coffee table
        if (eventChance(0.90f, dt)) {
            val mugX = cw * 0.50f + 15 * pixelScale + (Random.nextFloat() - 0.5f) * 6f
            particles.spawnSteam(mugX, floorY + 2 * pixelScale)
        }

        // 2. Vinyl music notes floating from turntable
        if (recordSpinning || audio.isPlayingMusic) {
            if (eventChance(0.22f, dt)) {
                particles.spawnMusicNote(cw * 0.88f + (Random.nextFloat() - 0.5f) * 16f, floorY - 14 * pixelScale)
            }
        }

        // 3. Occasional shooting star across panoramic window
        if (timeOfDayPhase.isNight && weather != WeatherType.RAIN && eventChance(1f / 85f, dt)) {
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
                showMessage(GameText.get(Res.string.scene_midnight_in_our_cozy_loft), duration = 3.5f)
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
                girlSpeechText = GameText.get(Res.string.scene_it_s_so_peaceful_up_here_with_you)
                girlSpeechTimer = 3.2f
                particles.spawnHeart(cw * 0.62f, ch * 0.50f, Color(0xFFFF758F))
            }
            if (t in 5.2f..5.4f && boySpeechText == null) {
                boySpeechText = GameText.get(Res.string.scene_my_favorite_place_in_the_whole_world)
                boySpeechTimer = 3.2f
                particles.spawnHeart(cw * 0.53f, ch * 0.50f, Color(0xFFFF3366))
            }
        } else {
            if (boy.reactionTimer <= 0 && !boy.isMovingOrTransitioning && !autonomyHolds(boy)) {
                boy.worldX = 0.58f
                boy.worldY = 0.575f
                boy.pose = CharacterPose.SIT
                boy.direction = Direction.RIGHT
                boy.emotion = CharacterEmotion.LOVING
            }
            if (girl.reactionTimer <= 0 && !girl.isMovingOrTransitioning && !autonomyHolds(girl)) {
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
            SceneType.RAINY_CAFE, SceneType.SUNROOM, SceneType.CAMPFIRE, SceneType.SEASIDE_PIER -> sceneTime < 1.5f // no opening script; just the scene wipe
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

        if (catTreatInProgress) return false
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
                if (eventChance(0.20f, dt)) {
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
                if (eventChance(0.20f, dt)) {
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
        val isOutdoor = isCurrentSceneOutdoor
        val clampedTargetY = if (isOutdoor) {
            targetY.coerceIn(0.66f, 0.74f)
        } else {
            targetY.coerceIn(0.66f, 0.72f)
        }
        val clampedTargetX = avoidCampfirePit(targetX.coerceIn(0.08f, 0.92f), clampedTargetY)

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
        val trotMessage = when {
            currentScene == SceneType.SEASIDE_PIER -> "Mochi pads along the boardwalk to you!"
            isOutdoor -> "Mochi trots through the grass towards you!"
            else -> "Mochi trots across the room to you!"
        }
        showMessage(trotMessage, duration = 2.0f)
    }

    private fun triggerAutonomousPetBehavior(cw: Float, ch: Float) {
        if (catTreatInProgress) return
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

        // If cat is already sleeping peacefully, let it sleep most of the time (70% chance to remain sleeping)
        if (catState == CatState.SLEEPING && Random.nextFloat() < 0.70f) {
            return
        }

        // Cat chooses next mood: 25% chance to gently roam to a new spot (replaces frantic 55% constant wandering)
        val shouldRoam = Random.nextFloat() < 0.25f
        if (shouldRoam) {
            val isOutdoor = isCurrentSceneOutdoor
            val coupleClose = kotlin.math.abs(boy.worldX - girl.worldX) < 0.25f
            val newTargetX = when (currentScene.environment) {
                EnvironmentType.LIVING_ROOM -> {
                    // Settle near the warm couch rug, floor lamp, or cozy box
                    val spots = listOf(0.22f, 0.35f, 0.48f, 0.62f, 0.76f)
                    spots.filter { kotlin.math.abs(it - catWorldX) > 0.12f }.randomOrNull() ?: (0.24f + Random.nextFloat() * 0.55f)
                }
                EnvironmentType.KITCHEN -> {
                    // Near the warm stove, kitchen table jute rug, or window
                    val spots = listOf(0.24f, 0.38f, 0.50f, 0.68f)
                    spots.filter { kotlin.math.abs(it - catWorldX) > 0.12f }.randomOrNull() ?: (0.25f + Random.nextFloat() * 0.50f)
                }
                else -> {
                    // Outdoor scenes:
                    // If raining, shelter close to couple or cottage door
                    if (weather == WeatherType.RAIN) {
                        if (Random.nextBoolean()) (boy.worldX + (if (Random.nextBoolean()) 0.06f else -0.06f)).coerceIn(0.15f, 0.85f)
                        else 0.22f // near cottage porch
                    } else if (coupleClose && Random.nextFloat() < 0.60f) {
                        // Snuggle up right beside the couple
                        val offset = if (Random.nextBoolean()) 0.08f else -0.08f
                        ((boy.worldX + girl.worldX) / 2f + offset).coerceIn(0.12f, 0.88f)
                    } else {
                        val spots = listOf(
                            (boy.worldX - 0.08f).coerceIn(0.12f, 0.88f),
                            (girl.worldX + 0.08f).coerceIn(0.12f, 0.88f),
                            (boy.worldX + girl.worldX) / 2f,
                            0.18f + Random.nextFloat() * 0.65f
                        )
                        spots.filter { kotlin.math.abs(it - catWorldX) > 0.10f }.randomOrNull() ?: (0.18f + Random.nextFloat() * 0.65f)
                    }
                }
            }
            catTargetY = if (isOutdoor) {
                0.67f + Random.nextFloat() * 0.06f
            } else {
                0.67f + Random.nextFloat() * 0.03f
            }
            catTargetX = avoidCampfirePit(newTargetX.coerceIn(0.12f, 0.88f), catTargetY)
            catFacingLeft = catTargetX < catWorldX
            catState = CatState.WALK_FOLLOW
            catSleeping = false
            return
        }

        // Otherwise stationary cozy action at current spot
        // Weather-weighted state choices
        val next = when {
            weather == WeatherType.RAIN || weather == WeatherType.SNOW -> {
                // In cold/wet weather, mostly cozy loafing or purring
                val wetStates = listOf(CatState.SLEEPING, CatState.SLEEPING, CatState.SITTING_PURR, CatState.BELLY_ROLL)
                wetStates.random()
            }
            weather == WeatherType.SUNNY -> {
                // In sunshine, love sunbathing belly rolls and purring
                val sunStates = listOf(CatState.BELLY_ROLL, CatState.SITTING_PURR, CatState.SLEEPING, CatState.PLAYFUL_POUNCE)
                sunStates.random()
            }
            weather == WeatherType.SAKURA || weather == WeatherType.AUTUMN -> {
                // In falling leaves or petals, playful pounce and purr
                val playfulStates = listOf(CatState.PLAYFUL_POUNCE, CatState.SITTING_PURR, CatState.BELLY_ROLL, CatState.SLEEPING)
                playfulStates.random()
            }
            else -> {
                val states = listOf(CatState.SITTING_PURR, CatState.BELLY_ROLL, CatState.PLAYFUL_POUNCE, CatState.SLEEPING)
                states.random()
            }
        }
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

    /**
     * Weather-specific living autonomous moments that occur naturally in the world.
     * Preserves:
     * - Sunny: Butterflies, Cloud Shadow
     * - Rain: Puddle interaction & shelter under umbrella
     * - Sakura: Petal catching, Mochi chasing petals
     * - Autumn: Leaf kick, Leaf on head
     * - Snow: Snowball moment, Snowflake watching
     */
    private fun triggerWeatherAutonomousMoment(cw: Float, ch: Float): Boolean {
        if (!isCurrentSceneOutdoor) return false
        val variant = Random.nextInt(2)
        when (weather) {
            WeatherType.SUNNY -> {
                if (variant == 0) {
                    // Butterflies: Girl follows a butterfly, boy watches happily
                    val targetX = (girl.worldX + 0.08f).coerceAtMost(0.72f)
                    val walkTime = abs(targetX - girl.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED
                    girl.moveTo(targetX, arrivePose = CharacterPose.IDLE)
                    girl.emotion = CharacterEmotion.CURIOUS
                    girl.emote = EmoteType.SPARKLE
                    girl.emoteTimer = walkTime + 2.0f
                    boy.direction = Direction.RIGHT
                    boy.emotion = CharacterEmotion.HAPPY
                    boy.reactionTimer = walkTime + 2.5f
                    girl.reactionTimer = walkTime + 2.5f
                    audio.playStarTwinkle()
                    particles.spawnSparkles(cw * targetX, ch * girl.worldY - 30f, 6)
                } else {
                    // Cloud Shadow: Couple stops, glances up at a passing cloud together
                    boy.transitionPoseTo(CharacterPose.IDLE)
                    girl.transitionPoseTo(CharacterPose.IDLE)
                    boy.emotion = CharacterEmotion.CURIOUS
                    girl.emotion = CharacterEmotion.CURIOUS
                    boy.reactionTimer = 3.0f
                    girl.reactionTimer = 3.0f
                    audio.playWindChime()
                }
                return true
            }
            WeatherType.RAIN -> {
                // Rain: Puddle interaction & stepping closer under the umbrella
                val targetBoyX = 0.46f
                val targetGirlX = 0.54f
                boy.moveTo(targetBoyX, arrivePose = CharacterPose.IDLE)
                girl.moveTo(targetGirlX, arrivePose = CharacterPose.IDLE)
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.HAPPY
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.5f
                boy.reactionTimer = 3.2f
                girl.reactionTimer = 3.2f
                audio.playWaterDrip()
                particles.spawnRainSplash(cw * 0.50f, ch * 0.72f)
                return true
            }
            WeatherType.SAKURA -> {
                if (variant == 0) {
                    // Petal Catching: Girl reaches out, petal drifts into hand
                    girl.transitionPoseTo(CharacterPose.RECEIVE_FLOWER)
                    girl.emotion = CharacterEmotion.HAPPY
                    girl.emote = EmoteType.SPARKLE
                    girl.emoteTimer = 2.4f
                    boy.direction = Direction.RIGHT
                    boy.emotion = CharacterEmotion.LOVING
                    boy.reactionTimer = 2.8f
                    girl.reactionTimer = 2.8f
                    audio.playStarTwinkle()
                    particles.spawnPetals(cw * girl.worldX, ch * girl.worldY - 24f, 8)
                } else {
                    // Mochi chasing petals across the grass!
                    catState = CatState.PLAYFUL_POUNCE
                    catSleeping = false
                    catTargetX = (catWorldX + (if (catFacingLeft) -0.10f else 0.10f)).coerceIn(0.20f, 0.85f)
                    audio.playCatChirp()
                    particles.spawnPetals(cw * catWorldX, ch * catWorldY - 14f, 5)
                }
                return true
            }
            WeatherType.AUTUMN -> {
                if (variant == 0) {
                    // Leaf Kick: Joyful kick through leaves, rustling leaves scatter
                    boy.transitionPoseTo(CharacterPose.JOY_JUMP)
                    boy.emotion = CharacterEmotion.PLAYFUL
                    boy.reactionTimer = 1.8f
                    girl.emotion = CharacterEmotion.HAPPY
                    girl.emote = EmoteType.BLUSH
                    girl.emoteTimer = 2.0f
                    girl.reactionTimer = 2.0f
                    audio.playLeafRustle()
                    repeat(6) { particles.spawnLeaf(cw * boy.worldX, ch * boy.worldY) }
                } else {
                    // Leaf on Head: Leaf drifts onto head, partner gently pats/picks it off
                    girl.transitionPoseTo(CharacterPose.HEAD_PAT)
                    girl.emotion = CharacterEmotion.LOVING
                    boy.transitionPoseTo(CharacterPose.HEAD_PAT_RECEIVE)
                    boy.emotion = CharacterEmotion.SHY
                    boy.emote = EmoteType.HEART
                    boy.emoteTimer = 2.2f
                    boy.reactionTimer = 2.8f
                    girl.reactionTimer = 2.8f
                    audio.playHeartChime()
                    repeat(3) { particles.spawnLeaf(cw * boy.worldX, ch * boy.worldY - 32f) }
                }
                return true
            }
            WeatherType.SNOW -> {
                if (variant == 0) {
                    // Snowball Moment: Playful snowball reveal & joy jump
                    boy.transitionPoseTo(CharacterPose.JOY_JUMP)
                    boy.emotion = CharacterEmotion.PLAYFUL
                    boy.emote = EmoteType.SPARKLE
                    boy.emoteTimer = 2.0f
                    girl.emotion = CharacterEmotion.HAPPY
                    girl.emote = EmoteType.HEART
                    girl.emoteTimer = 2.2f
                    boy.reactionTimer = 2.5f
                    girl.reactionTimer = 2.5f
                    audio.playBubblePop()
                    particles.spawnSparkles(cw * boy.worldX, ch * boy.worldY - 26f, 8)
                } else {
                    // Snowflake Watching: Standing close in peaceful silence admiring the snow
                    val targetBoyX = 0.46f
                    val targetGirlX = 0.54f
                    boy.moveTo(targetBoyX, arrivePose = CharacterPose.IDLE)
                    girl.moveTo(targetGirlX, arrivePose = CharacterPose.IDLE)
                    boy.emotion = CharacterEmotion.LOVING
                    girl.emotion = CharacterEmotion.LOVING
                    boy.reactionTimer = 3.5f
                    girl.reactionTimer = 3.5f
                    audio.playStarTwinkle()
                }
                return true
            }
        }
    }

    // Autonomous Spontaneous Affectionate Moments — scene-aware (Feature 2 & 8)
    /** [sceneOnly]: play this scene's own hand-made moment (the autonomy brain asked for it). */
    private fun triggerAutonomousMoment(cw: Float, ch: Float, sceneOnly: Boolean = false) {
        nextAutonomousInterval = 12f + Random.nextFloat() * 10f

        if (!sceneOnly) {
            // Quiet idle beats carry most of the time; existing scene-specific moments remain
            // occasional surprises. Loft and scooter poses stay planted by design.
            if (currentScene == SceneType.COZY_LOFT || currentScene == SceneType.EVENING_RIDE) {
                triggerWeightedIdleMoment(cw, ch)
                return
            }
            if (Random.nextInt(100) < 72) {
                triggerWeightedIdleMoment(cw, ch)
                return
            }

            // Occasional weather-specific autonomous moment in outdoor scenes
            if (isCurrentSceneOutdoor && Random.nextFloat() < 0.35f) {
                if (triggerWeatherAutonomousMoment(cw, ch)) {
                    return
                }
            }
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
                        showMessage(GameText.get(Res.string.scene_a_tiny_flower_just_for_you), duration = 3.0f)
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
                        showMessage(GameText.get(Res.string.scene_extra_spicy_red_chutney_laughs_happily, boy.name), duration = 3.0f)
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
            SceneType.SEASIDE_PIER -> {
                triggerPierAutonomousMoment(cw, ch)
                return
            }
            else -> { /* fall through to generic */ }
        }

        triggerWeightedIdleMoment(cw, ch)
    }

    private fun triggerWeightedIdleMoment(cw: Float, ch: Float) {
        // Cumulative weights: shared glance 32, conversation 23, look-around 17,
        // small gesture 12, pet/weather reaction 11, short stroll 5.
        val roll = Random.nextInt(100)
        var behavior = when {
            roll < 32 -> 0
            roll < 55 -> 1
            roll < 72 -> 2
            roll < 84 -> 3
            roll < 95 -> 4
            else -> 5
        }
        if (behavior == lastIdleBehavior) behavior = (behavior + 1) % 6
        lastIdleBehavior = behavior

        when (behavior) {
            0 -> {
                // A quiet glance, then a shared pause.
                boy.direction = if (girl.worldX >= boy.worldX) Direction.RIGHT else Direction.LEFT
                girl.direction = if (boy.worldX >= girl.worldX) Direction.RIGHT else Direction.LEFT
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.HAPPY
                boy.reactionTimer = 1.8f
                girl.reactionTimer = 1.8f
            }
            1 -> {
                // Short personal conversation rather than a large scripted event.
                val speaker = if (Random.nextBoolean()) boy else girl
                val reply = if (speaker === boy) girl else boy
                speaker.direction = if (reply.worldX >= speaker.worldX) Direction.RIGHT else Direction.LEFT
                reply.direction = if (speaker.worldX >= reply.worldX) Direction.RIGHT else Direction.LEFT
                speaker.emotion = CharacterEmotion.LOVING
                reply.emotion = CharacterEmotion.HAPPY
                speakerSpeech(speaker, when (Random.nextInt(4)) {
                    0 -> "I like being right here with you."
                    1 -> "This little moment is my favorite."
                    2 -> if (weather == WeatherType.RAIN) "I could listen to this rain with you all day." else "You make this place feel like home."
                    else -> "Look how peaceful everything feels."
                }, 2.7f)
                speaker.reactionTimer = 2.7f
                reply.reactionTimer = 1.8f
            }
            2 -> {
                // One partner looks around while the other stays relaxed.
                val observer = if (Random.nextBoolean()) boy else girl
                observer.direction = if (Random.nextBoolean()) Direction.LEFT else Direction.RIGHT
                observer.emotion = CharacterEmotion.CURIOUS
                observer.emote = EmoteType.DOTS
                observer.emoteTimer = 1.4f
                observer.reactionTimer = 1.6f
            }
            3 -> {
                // Small wave / hair-adjusting beat. Restore the exact idle pose afterward.
                val observer = if (Random.nextBoolean()) boy else girl
                if ((observer.pose == CharacterPose.IDLE || observer.pose == CharacterPose.IDLE_BLINK) &&
                    !observer.isMovingOrTransitioning
                ) {
                    observer.transitionPoseTo(CharacterPose.WAVE)
                    if (observer === boy) {
                        boyIdleWaveReturnPose = CharacterPose.IDLE
                        boyIdleWaveTimer = 1.35f
                    } else {
                        girlIdleWaveReturnPose = CharacterPose.IDLE
                        girlIdleWaveTimer = 1.35f
                    }
                } else {
                    observer.isBlinking = true
                    observer.emote = EmoteType.SPARKLE
                    observer.emoteTimer = 1.2f
                }
                observer.emotion = CharacterEmotion.HAPPY
                observer.reactionTimer = 1.35f
                audio.playStarTwinkle()
            }
            4 -> {
                val isRainyWindow = currentScene == SceneType.RAINY_CAFE
                val isFallingFoliage = weather == WeatherType.SAKURA || weather == WeatherType.AUTUMN
                val isNight = timeOfDayPhase.isNight
                if (isNight && Random.nextFloat() < 0.12f) {
                    boy.emotion = CharacterEmotion.CURIOUS
                    girl.emotion = CharacterEmotion.CURIOUS
                    boy.emote = EmoteType.SPARKLE
                    girl.emote = EmoteType.SPARKLE
                    boy.emoteTimer = 1.8f
                    girl.emoteTimer = 1.8f
                    particles.spawnShootingStar(cw * (0.22f + Random.nextFloat() * 0.56f), ch * 0.12f)
                    boy.reactionTimer = 1.8f
                    girl.reactionTimer = 1.8f
                } else if (isFallingFoliage || isRainyWindow || (isCurrentSceneOutdoor && weather == WeatherType.SNOW)) {
                    boy.emotion = CharacterEmotion.CURIOUS
                    girl.emotion = CharacterEmotion.HAPPY
                    boy.emote = EmoteType.SPARKLE
                    girl.emote = EmoteType.SPARKLE
                    boy.emoteTimer = 1.5f
                    girl.emoteTimer = 1.5f
                    boy.reactionTimer = 1.6f
                    girl.reactionTimer = 1.6f
                } else {
                    boy.direction = if (catWorldX >= boy.worldX) Direction.RIGHT else Direction.LEFT
                    girl.direction = if (catWorldX >= girl.worldX) Direction.RIGHT else Direction.LEFT
                    boy.emotion = CharacterEmotion.HAPPY
                    girl.emotion = CharacterEmotion.HAPPY
                    boy.emote = EmoteType.HEART
                    girl.emote = EmoteType.HEART
                    boy.emoteTimer = 1.5f
                    girl.emoteTimer = 1.5f
                    boy.reactionTimer = 1.6f
                    girl.reactionTimer = 1.6f
                    if (catState != CatState.SLEEPING) {
                        particles.spawnHeart(cw * catWorldX, ch * catWorldY - 16f, Color(0xFFFF8FA3))
                    }
                }
            }
            5 -> {
                // A few unhurried steps around the open meadow or greenhouse aisle.
                if (currentScene == SceneType.FLOWER || currentScene == SceneType.SUNROOM) {
                    val walker = if (Random.nextBoolean()) boy else girl
                    val step = if (Random.nextBoolean()) 0.045f else -0.045f
                    val targetX = (walker.worldX + step).coerceIn(0.16f, 0.84f)
                    walker.moveTo(targetX, walker.worldY, arrivePose = CharacterPose.IDLE)
                    walker.emotion = CharacterEmotion.CURIOUS
                    walker.reactionTimer = abs(targetX - walker.worldX) / CharacterMotionTween.SHARED_WALKING_SPEED + 0.8f
                    audio.playFootstep()
                } else {
                    // Replace an inapplicable walk with a calm mutual glance.
                    boy.direction = Direction.RIGHT
                    girl.direction = Direction.LEFT
                    boy.reactionTimer = 1.4f
                    girl.reactionTimer = 1.4f
                }
            }
        }
    }

    /**
     * Told about things that count toward the couple's progress (plan 07): rainbow wishes,
     * constellations, snowmen, discoveries, catches, scene visits. Set by the screen, which keeps
     * the progress and celebrates any "little first" earned.
     */
    var onProgress: ((com.example.progress.ProgressEvent) -> Unit)? = null

    // ── Mochi's fondness and gifts (plan 07, D2-D3) ──
    /** Mochi's fondness (set by the screen from the progress); decides the heart meter and the slow blink. */
    var mochiFondness: Int = 0
    /** Seconds left showing the heart meter over Mochi after a pet. */
    var mochiMeterTimer: Float = 0f
        private set
    /** The gifts on the kitchen's keepsake shelf (set by the screen from the progress). */
    var keepsakeShelf: List<String> = emptyList()

    private fun careForMochi(points: Int) {
        onProgress?.invoke(com.example.progress.ProgressEvent.MochiCare(points, com.example.data.CoupleDates.today().toEpochDays().toLong()))
        mochiMeterTimer = MOCHI_METER_SECONDS
    }

    /** The world size from the last update, for reactions started from outside the frame loop. */
    internal var lastWorldW = 1080f
        private set
    internal var lastWorldH = 2400f
        private set


    /** One partner gives the other a keepsake called [itemName]: both react, and it goes on the shelf. */
    fun giveGift(fromBoy: Boolean, itemName: String) {
        val cw = lastWorldW
        val ch = lastWorldH
        val giver = if (fromBoy) boy else girl
        val partner = if (fromBoy) girl else boy
        giver.emote = EmoteType.HEART
        giver.emoteTimer = 2.4f
        giver.emotion = CharacterEmotion.LOVING
        partner.emote = EmoteType.BLUSH
        partner.emoteTimer = 2.4f
        partner.emotion = CharacterEmotion.LOVING
        partner.pose = CharacterPose.JOY_JUMP
        partner.bounceOffset = 6f
        particles.spawnSparkles(cw * partner.worldX, ch * partner.worldY - 40f, 8, Color(0xFFFFD166))
        particles.spawnHeart(cw * partner.worldX, ch * partner.worldY - 46f, Color(0xFFFF85A1))
        audio.playHeartChime()
        showMessage(GameText.get(Res.string.gift_given, giver.name, partner.name, itemName), duration = 4f)
    }

    // ── Stargazing (plan 07, C2) ──
    val starPuzzle = com.example.games.StarPuzzle()
    /** The constellations found so far (set by the screen from the progress), so found ones just glow. */
    var foundConstellations: Set<String> = emptySet()

    /**
     * A tap on the night sky at ([x], [y]) on a [cw] x [ch] stage, with the sky turned by [drift].
     * Connects stars while a puzzle is going; otherwise starts one on the constellation tapped
     * (or makes a found one glow). Returns false when the tap wasn't about constellations.
     */
    fun onNightSkyTap(x: Float, y: Float, cw: Float, ch: Float, drift: Float): Boolean {
        val puzzle = starPuzzle.current
        if (puzzle != null) {
            when (starPuzzle.tap(x, y, cw, ch, drift)) {
                com.example.games.StarPuzzle.Tap.CONNECTED -> {
                    audio.playStarTwinkle()
                    particles.spawnSparkles(x, y, 4, Color(0xFFFFF3B0))
                }
                com.example.games.StarPuzzle.Tap.WRONG -> audio.playBubblePop()
                com.example.games.StarPuzzle.Tap.DONE -> finishStarPuzzle(puzzle, x, y)
                com.example.games.StarPuzzle.Tap.MISSED -> Unit
            }
            return true
        }
        val skyX = (x / cw + drift) % 1f
        val c = com.example.games.Constellations.at(skyX, y / ch) ?: return false
        if (c.id in foundConstellations) {
            starPuzzle.glow(c)
            audio.playStarArpeggio()
            showMessage(GameText.get(Res.string.star_found_again, GameText.get(c.name)), duration = 3f)
        } else {
            starPuzzle.start(c)
            audio.playStarTwinkle()
            showMessage(GameText.get(Res.string.star_puzzle_hint, GameText.get(c.name)), duration = 3.5f)
        }
        return true
    }

    fun stopStarPuzzle() = starPuzzle.stop()

    private fun finishStarPuzzle(c: com.example.games.Constellation, x: Float, y: Float) {
        foundConstellations = foundConstellations + c.id
        audio.playStarArpeggio()
        particles.spawnSparkles(x, y, 9, Color(0xFFCAF0F8))
        particles.spawnHeart(x, y - 14f, Color(0xFFFF85A1))
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.SPARKLE
        boy.emoteTimer = 2.2f
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.HEART
        girl.emoteTimer = 2.2f
        showMessage(GameText.get(Res.string.scene_constellation, GameText.get(c.name), GameText.get(c.story)), duration = 4f)
        onProgress?.invoke(com.example.progress.ProgressEvent.ConstellationFound(c.id))
    }

    // ── Catch together (plan 07, C1) ──
    val catchGame = com.example.games.CatchGame()
    /** The round's score, time left and state, as Compose state for the screen's counter. */
    var catchScore by mutableIntStateOf(0)
        private set
    var catchSecondsLeft by mutableIntStateOf(0)
        private set
    var catchActive by mutableStateOf(false)
        private set

    /** Weathers that drop something to catch. */
    val hasCatchableWeather: Boolean
        get() = isCurrentSceneOutdoor && (weather == WeatherType.SNOW || weather == WeatherType.SAKURA || weather == WeatherType.AUTUMN)

    fun startCatchGame() {
        if (!hasCatchableWeather || catchGame.active) return
        catchGame.start()
        catchActive = true
        catchScore = 0
        catchSecondsLeft = com.example.games.CatchGame.DURATION_SECONDS.toInt()
        audio.playHeartChime()
    }

    /** Called with the round's result when it ends (the screen shows it and keeps the best). */
    var onCatchGameOver: ((score: Int) -> Unit)? = null

    private fun updateCatchGame(dt: Float, cw: Float, ch: Float) {
        if (!catchGame.active) return
        val halfW = com.example.games.CatchGame.HALF_WIDTH * cw
        val rim = com.example.games.CatchGame.RIM_Y * ch
        val x = catchGame.basketX * cw
        val caught = particles.catchInBasket(x - halfW, x + halfW, rim, rim + com.example.games.CatchGame.RIM_DEPTH * ch)
        if (caught > 0) {
            catchGame.addCatches(caught)
            audio.playStarTwinkle()
        }
        val before = catchGame.score
        val ended = catchGame.update(dt)
        if (catchGame.score - before >= com.example.games.CatchGame.GOLDEN_VALUE) {
            particles.spawnSparkles(x, rim, 8, Color(0xFFFFD166))
            audio.playStarArpeggio()
        }
        catchScore = catchGame.score
        catchSecondsLeft = kotlin.math.ceil(catchGame.timeLeft).toInt()
        if (ended) {
            catchActive = false
            boy.emote = EmoteType.HEART
            girl.emote = EmoteType.HEART
            boy.emoteTimer = 2.2f
            girl.emoteTimer = 2.2f
            // The result first (it compares with the best so far), then record the round.
            onCatchGameOver?.invoke(catchGame.score)
            onProgress?.invoke(com.example.progress.ProgressEvent.GamePlayed(com.example.progress.Game.CATCH, catchGame.score))
        }
    }

    /** A little first was earned: both light up with a heart, a few sparkles, and the news. */
    /**
     * A Tiny Games reveal (plan 09, B): a match has them jump with hearts; a miss gets a playful
     * shrug and "Opposites attract!". They stay put; the game is played over the world.
     */
    fun reactToTinyGame(match: Boolean) {
        val cw = lastWorldW
        val ch = lastWorldH
        for (c in charactersBoyGirl) {
            c.reactionTimer = 2.6f
            c.emotion = if (match) CharacterEmotion.HAPPY else CharacterEmotion.PLAYFUL
        }
        if (match) {
            for (c in charactersBoyGirl) {
                if (c.pose != CharacterPose.SIT && c.pose != CharacterPose.SIT_SNUGGLE) c.pose = CharacterPose.JOY_JUMP
                c.bounceOffset = 6f
                c.emote = EmoteType.HEART
                c.emoteTimer = 2.2f
            }
            if (cw > 0f) particles.spawnHeart(cw * (boy.worldX + girl.worldX) / 2f, ch * boy.worldY - 90f)
            audio.playHeartChime()
        } else {
            boy.emote = EmoteType.QUESTION
            girl.emote = EmoteType.SWEAT
            boy.emoteTimer = 2f
            girl.emoteTimer = 2f
            speakerSpeech(if (Random.nextBoolean()) boy else girl, GameText.get(Res.string.tg_opposites_line), 2.4f)
            audio.playBubblePop()
        }
    }

    /** The end of a Tiny Games round: a bigger cheer for a good score. */
    fun celebrateTinyGameRound(score: Int, total: Int) {
        if (total <= 0) return
        val cw = lastWorldW
        val ch = lastWorldH
        for (c in charactersBoyGirl) {
            c.reactionTimer = 3f
            c.emotion = CharacterEmotion.LOVING
            c.emote = EmoteType.HEART
            c.emoteTimer = 2.6f
        }
        if (score * 5 >= total * 4 && cw > 0f) {
            repeat(4) { particles.spawnHeart(cw * (0.35f + it * 0.1f), ch * boy.worldY - 100f) }
            particles.spawnSparkles(cw * 0.5f, ch * boy.worldY - 120f, 8)
        }
        audio.playStarTwinkle()
    }

    fun celebrateLittleFirst(message: String) {
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 2.6f
        girl.emoteTimer = 2.6f
        audio.playStarArpeggio()
        showMessage(message, duration = 4.0f)
    }

    /** Seconds left in which the special-day greeting keeps its bubbles (other speech waits). */
    private var specialDayGreetingHold = 0f

    /** The couple greets a special day (plan 06, G2): both say their line at once. */
    fun greetSpecialDay(boyLine: String, girlLine: String) {
        // On a birthday with the surprise set up, the party does the greeting.
        if (birthdaySurprise.isRunning || birthdaySurprise.partyOn(CoupleDates.today())) return
        specialDayGreetingHold = 0f
        speakerSpeech(boy, boyLine, SPECIAL_DAY_LINE_SECONDS)
        speakerSpeech(girl, girlLine, SPECIAL_DAY_LINE_SECONDS)
        specialDayGreetingHold = SPECIAL_DAY_LINE_SECONDS
    }

    private fun speakerSpeech(character: PixelCharacter, text: String, duration: Float) {
        // Chatter (autonomy, scene moments) waits until the special-day greeting has been read.
        if (specialDayGreetingHold > 0f) return
        if (character === boy) {
            boySpeechText = text
            boySpeechTimer = duration
            boy.isSpeaking = true
        } else {
            girlSpeechText = text
            girlSpeechTimer = duration
            girl.isSpeaking = true
        }
    }

    private fun updateIdleWaveReturn(character: PixelCharacter, dt: Float, isBoy: Boolean) {
        val timer = if (isBoy) boyIdleWaveTimer else girlIdleWaveTimer
        if (timer <= 0f) return
        val next = (timer - dt).coerceAtLeast(0f)
        if (isBoy) boyIdleWaveTimer = next else girlIdleWaveTimer = next
        if (next > 0f) return
        val returnPose = if (isBoy) boyIdleWaveReturnPose else girlIdleWaveReturnPose
        if (character.pose == CharacterPose.WAVE && returnPose != null && !character.isMovingOrTransitioning) {
            character.transitionPoseTo(returnPose)
        }
        if (isBoy) boyIdleWaveReturnPose = null else girlIdleWaveReturnPose = null
    }

    // Touch Interactions
    /** A quiet, scene-aware stroll toward a tapped open spot on the floor/ground. */
    fun onTouchWalkableGround(touchX: Float, touchY: Float, cw: Float, ch: Float): Boolean {
        if (isDreamMode || isWatchSceneActive || isSceneOpeningScriptActive() ||
            boy.isMovingOrTransitioning || girl.isMovingOrTransitioning ||
            groundInteractionCharacter != null
        ) return false

        val px = WorldViewport.pixelScale(cw)
        val minY: Float
        val maxY: Float
        val minX: Float
        val maxX: Float
        when (currentScene) {
            SceneType.FLOWER, SceneType.LOOKING -> { minY = 0.68f; maxY = 0.80f; minX = 0.12f; maxX = 0.88f }
            SceneType.UNDER_TREE -> { minY = 0.69f; maxY = 0.80f; minX = 0.14f; maxX = 0.86f }
            SceneType.WALK -> {
                minY = 0.68f
                maxY = (0.66f + (30f * px / ch) + 0.08f).coerceAtMost(0.84f)
                minX = 0.12f
                maxX = 0.88f
            }
            SceneType.MOMO_STALL -> {
                minY = 0.69f
                maxY = (0.66f + (30f * px / ch) + 0.10f).coerceAtMost(0.86f)
                minX = 0.12f
                maxX = 0.88f
            }
            SceneType.COOKING -> { minY = 0.69f; maxY = 0.79f; minX = 0.16f; maxX = 0.84f }
            SceneType.SLEEP -> { minY = 0.68f; maxY = 0.77f; minX = 0.18f; maxX = 0.82f }
            SceneType.COZY_LOFT -> { minY = 0.56f; maxY = 0.66f; minX = 0.40f; maxX = 0.80f }
            SceneType.RAINY_CAFE -> { minY = 0.69f; maxY = 0.79f; minX = 0.16f; maxX = 0.84f }
            SceneType.SUNROOM -> { minY = 0.67f; maxY = 0.80f; minX = 0.14f; maxX = 0.86f }
            SceneType.CAMPFIRE -> { minY = 0.68f; maxY = 0.80f; minX = 0.14f; maxX = 0.86f }
            // Between the ice-cream cart and Grandpa Bao's crate.
            SceneType.SEASIDE_PIER -> {
                minY = PierLayout.WALK_MIN_Y; maxY = PierLayout.WALK_MAX_Y
                minX = PierLayout.WALK_MIN_X; maxX = PierLayout.WALK_MAX_X
            }
            SceneType.EVENING_RIDE -> return false // They are sharing the scooter in this scene.
        }

        val requestedX = touchX / cw
        val requestedY = touchY / ch
        if (requestedX !in minX..maxX || requestedY !in minY..maxY) return false

        val boyDistance = kotlin.math.hypot(boy.worldX - requestedX, boy.worldY - requestedY)
        val girlDistance = kotlin.math.hypot(girl.worldX - requestedX, girl.worldY - requestedY)
        val walker = if (boyDistance <= girlDistance) boy else girl
        val partner = if (walker === boy) girl else boy
        val targetX = requestedX.coerceIn(minX, maxX)
        val targetY = requestedY.coerceIn(minY, maxY)
        // Nobody strolls into the campfire flames.
        if (avoidCampfirePit(targetX, targetY) != targetX) return false

        // Leave a little personal space so a depth walk cannot end with the sprites stacked.
        if (kotlin.math.hypot(partner.worldX - targetX, partner.worldY - targetY) < 0.105f) return false
        val distance = kotlin.math.hypot(walker.worldX - targetX, walker.worldY - targetY)
        if (distance < 0.035f) return false

        walker.moveTo(targetX, targetY, arrivePose = CharacterPose.IDLE)
        walker.emotion = CharacterEmotion.CURIOUS
        walker.reactionTimer = distance / CharacterMotionTween.SHARED_WALKING_SPEED + 1.8f
        partner.direction = if (targetX < partner.worldX) Direction.LEFT else Direction.RIGHT
        partner.reactionTimer = distance / CharacterMotionTween.SHARED_WALKING_SPEED + 1.2f
        groundInteractionCharacter = walker
        groundInteractionX = targetX
        groundInteractionY = targetY
        groundInteractionWait = distance / CharacterMotionTween.SHARED_WALKING_SPEED + 0.55f
        audio.playFootstep()
        return true
    }

    private fun avoidCampfirePit(x: Float, y: Float): Float =
        if (currentScene == SceneType.CAMPFIRE) CampfireLayout.avoidPit(x, y) else x

    private fun isSceneOpeningScriptActive(): Boolean = when (currentScene) {
        SceneType.FLOWER -> sceneTime < 8.2f
        SceneType.UNDER_TREE -> sceneTime < 7.2f
        SceneType.COOKING -> sceneTime < 7.6f
        SceneType.SLEEP -> sceneTime < 6.9f
        SceneType.WALK -> sceneTime < 6.6f
        SceneType.LOOKING -> sceneTime < 7.6f
        SceneType.MOMO_STALL -> sceneTime < 6.2f
        SceneType.EVENING_RIDE -> sceneTime < 7.0f
        SceneType.COZY_LOFT -> sceneTime < 5.6f
        SceneType.RAINY_CAFE, SceneType.SUNROOM, SceneType.CAMPFIRE, SceneType.SEASIDE_PIER -> sceneTime < 1.5f // no opening script; just the scene wipe
    }

    private fun updateGroundInteraction(dt: Float, cw: Float, ch: Float) {
        val walker = groundInteractionCharacter ?: return
        groundInteractionWait = (groundInteractionWait - dt).coerceAtLeast(0f)
        if (groundInteractionWait > 0f || walker.isMovingOrTransitioning) return

        walker.emotion = CharacterEmotion.HAPPY
        walker.emote = EmoteType.SPARKLE
        walker.emoteTimer = 1.3f
        audio.playStarTwinkle()
        val x = cw * groundInteractionX
        val y = ch * groundInteractionY
        when (weather) {
            WeatherType.RAIN -> particles.spawnRainSplash(x, y)
            WeatherType.SAKURA -> particles.spawnPetals(x, y - 8f, 2)
            WeatherType.AUTUMN -> particles.spawnLeaf(x, y - 8f)
            WeatherType.SNOW, WeatherType.SUNNY -> particles.spawnSparkles(x, y - 12f, 3)
        }
        groundInteractionCharacter = null
    }

    /** A little roadside firefly moment for the ride scene, where walking is not appropriate. */
    fun onTouchRideFireflies(touchX: Float, touchY: Float, cw: Float, ch: Float) {
        if (rideFireflyTimer > 0f) return
        rideFireflyTimer = 1.5f
        audio.playStarTwinkle()
        particles.spawnSparkles(touchX, touchY, 5, Color(0xFFFFD166))
        boy.direction = if (touchX < cw * boy.worldX) Direction.LEFT else Direction.RIGHT
        girl.direction = if (touchX < cw * girl.worldX) Direction.LEFT else Direction.RIGHT
        boy.emotion = CharacterEmotion.CURIOUS
        girl.emotion = CharacterEmotion.CURIOUS
        boy.emote = EmoteType.SPARKLE
        girl.emote = EmoteType.SPARKLE
        boy.emoteTimer = 1.5f
        girl.emoteTimer = 1.5f
        particles.spawnHeart(cw * 0.50f, ch * 0.60f, Color(0xFFFFD166))
    }

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
            boySpeechText = GameText.get(Res.string.scene_coming_closer_to_you)
            boySpeechTimer = 3.0f
            return
        }
        if (useHeldItem(boy, cw, ch)) return
        if (sayBirthdayLine(boy)) return
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
                boySpeechText = GameText.get(Res.string.scene_hey_gorgeous)
                boySpeechTimer = 3.2f
            }
            1 -> {
                boy.pose = CharacterPose.WAVE
                boy.emote = EmoteType.MUSIC_NOTE
                boy.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnSparkles(cw * boy.worldX, ch * boy.worldY - 30f, 6)
                boySpeechText = GameText.get(Res.string.scene_my_favorite_person_in_the_world)
                boySpeechTimer = 3.2f
            }
            2 -> {
                boy.pose = CharacterPose.IDLE
                boy.emotion = CharacterEmotion.SHY
                boy.emote = EmoteType.BLUSH
                boy.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * boy.worldX, ch * boy.worldY - 30f, Color(0xFFFF758F))
                boySpeechText = GameText.get(Res.string.scene_you_make_my_whole_world_brighter)
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
                boySpeechText = GameText.get(Res.string.scene_still_completely_head_over_heels_for_you)
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
            girlSpeechText = GameText.get(Res.string.scene_getting_closer_to_you)
            girlSpeechTimer = 3.0f
            return
        }
        if (useHeldItem(girl, cw, ch)) return
        if (sayBirthdayLine(girl)) return
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
                girlSpeechText = GameText.get(Res.string.scene_my_heart_feels_so_full_with_you)
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
                girlSpeechText = GameText.get(Res.string.scene_warmest_cuddles_only_with_you)
                girlSpeechTimer = 3.2f
            }
            4 -> {
                girl.pose = CharacterPose.IDLE
                girl.emotion = CharacterEmotion.LOVING
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.0f
                audio.playHeartChime()
                particles.spawnHeart(cw * girl.worldX, ch * girl.worldY - 30f, Color(0xFFFF85A1))
                girlSpeechText = GameText.get(Res.string.scene_forever_by_your_side)
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
                showMessage(GameText.get(Res.string.scene_always_safe_in_your_arms), duration = 3.5f)
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
                showMessage(GameText.get(Res.string.scene_spinning_around_with_my_whole_world), duration = 3.5f)
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
                showMessage(GameText.get(Res.string.scene_listening_to_your_heartbeat), duration = 3.5f)
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
                showMessage(GameText.get(Res.string.scene_my_whole_world_right_here), duration = 3.5f)
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
        showMessage(GameText.get(Res.string.scene_holding_you_close), duration = 3.5f)
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
        grantRequest(RequestKind.TEA)
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
                showMessage(GameText.get(Res.string.scene_cooking_with_love_sneaks_a_taste, boy.name), duration = 3.0f)
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
                showMessage(GameText.get(Res.string.scene_offers_a_warm_spoonful_to, girl.name, boy.name), duration = 3.0f)
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
                showMessage(GameText.get(Res.string.scene_playfully_steals_a_taste_before_it_s_rea, boy.name), duration = 3.0f)
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
        grantRequest(RequestKind.SNACK)
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
        showMessage(GameText.get(Res.string.scene_warm_cookies_baking_golden_inside_the_ov), duration = 3.0f)
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
        showMessage(GameText.get(Res.string.scene_splashing_fresh_water_washing_veggies_an), duration = 3.0f)
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
        showMessage(GameText.get(Res.string.scene_pulling_up_two_chairs_for_warm_tea_and_h), duration = 3.5f)
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
        grantRequest(RequestKind.MOCHI)
        careForMochi(1)
        // A best friend sometimes answers with a slow blink: cat for "I love you".
        if (com.example.progress.MochiFondness.level(mochiFondness) >= 3 && Random.nextFloat() < 0.3f) {
            boy.emote = EmoteType.HEART
            girl.emote = EmoteType.HEART
            boy.emoteTimer = 2.2f
            girl.emoteTimer = 2.2f
            audio.playCatPurr()
            showMessage(GameText.get(Res.string.mochi_slow_blink), duration = 3.5f)
            return
        }
        if (currentScene == SceneType.COOKING) {
            mochiCollarStyle = if (mochiCollarStyle >= 2) 1 else mochiCollarStyle + 1
            audio.playStarTwinkle()
            showMessage(if (mochiCollarStyle == 1) "Mochi looks dapper in a tiny red bowtie!" else "A little daisy collar for Mochi!", duration = 2.6f)
        }
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
            showMessage(GameText.get(Res.string.scene_mochi_stopped_to_get_your_love), duration = 2.5f)
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
                showMessage(GameText.get(Res.string.scene_mochi_is_purring_happily), duration = 2.5f)
            }
            CatState.BELLY_ROLL -> {
                repeat(4) {
                    particles.spawnHeart(cw * catWorldX + (Random.nextFloat() - 0.5f) * 20f, ch * catWorldY - 22f, Color(0xFFFF5D8F))
                }
                showMessage(GameText.get(Res.string.scene_mochi_wants_gentle_belly_rubs), duration = 2.5f)
            }
            CatState.PLAYFUL_POUNCE -> {
                audio.playBubblePop()
                particles.spawnSparkles(cw * catWorldX, ch * catWorldY - 20f, 6)
                showMessage(GameText.get(Res.string.scene_mochi_pounces_playfully), duration = 2.5f)
            }
            CatState.WALK_FOLLOW -> {
                val target = if (boy.worldX < girl.worldX) (boy.worldX - 0.08f).coerceAtLeast(0.12f) else (girl.worldX + 0.08f).coerceAtMost(0.88f)
                catTargetX = target
                catFacingLeft = catTargetX < catWorldX
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF5D8F))
                showMessage(GameText.get(Res.string.scene_mochi_is_trotting_right_beside_you), duration = 2.5f)
            }
            CatState.SLEEPING -> {
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
                showMessage(GameText.get(Res.string.scene_mochi_curled_up_for_a_warm_snooze), duration = 2.5f)
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

    // Outfit names follow the character's chosen style (dress vs trousers), not its slot.
    private fun dressOutfitNames(partnerName: String): List<String> = listOf(
        "Strawberry Cream Sundress",
        "Lavender Dream Wrap Dress",
        "Emerald Velvet Romance",
        "Lemon Sunshine Picnic Dress",
        "Midnight Starlight Gown",
        "Mint Macaron Tea Dress",
        "$partnerName's Oversized Flannel",
        "Blush Rose Cropped Hoodie",
        "Sage & Cream Colorblock Hoodie",
        "Lavender Cloud Oversized Hoodie",
        "Buttercream Star Shimmer Hoodie"
    )

    private fun trouserOutfitNames(): List<String> = listOf(
        "Classic Spruce Knit & Navy Pants",
        "White & Emerald Varsity Hoodie",
        "Charcoal Streetwear Zip Hoodie",
        "Oatmeal Cloud Oversized Hoodie",
        "Midnight Starlight Graphic Hoodie"
    )

    private fun outfitDisplayName(char: PixelCharacter, index: Int): String =
        if (char.look.wearsDress) dressOutfitNames(partnerName = if (char === girl) boy.name else girl.name).getOrElse(index) { "Lovely Outfit" }
        else trouserOutfitNames().getOrElse(index) { "Sharp Outfit" }

    fun selectGirlDress(index: Int) {
        girl.outfitIndex = index
        girl.emotion = CharacterEmotion.LOVING
        girl.pose = CharacterPose.JOY_JUMP
        girl.bounceOffset = 8f
        audio.playHeartChime()
        val name = outfitDisplayName(girl, index)
        showMessage(GameText.get(Res.string.scene_is_now_wearing_the_looking_lovely, girl.name, name), duration = 3.5f)
    }

    fun selectGirlAccessory(index: Int) {
        girl.accessoryIndex = index
        audio.playStarTwinkle()
        val accessoryNames = listOf(
            "Natural look",
            "Cozy Ribbed Beanie",
            "Warm Wool Fringe Scarf",
            "Casual Streetwear Baseball Cap",
            "Rainbow Scarf",
            "Snowman Beanie"
        )
        val name = accessoryNames.getOrElse(index) { "Accessory" }
        if (index == 0) {
            showMessage(GameText.get(Res.string.scene_took_off_the_accessory, girl.name), duration = 2.5f)
        } else {
            showMessage(GameText.get(Res.string.scene_is_now_wearing_the, girl.name, name), duration = 3.0f)
        }
    }

    fun selectBoyOutfit(index: Int) {
        boy.outfitIndex = index
        boy.emotion = CharacterEmotion.LOVING
        boy.pose = CharacterPose.JOY_JUMP
        boy.bounceOffset = 8f
        audio.playHeartChime()
        val name = outfitDisplayName(boy, index)
        showMessage(GameText.get(Res.string.scene_is_now_wearing_the_looking_wonderful, boy.name, name), duration = 3.5f)
    }

    fun selectBoyAccessory(index: Int) {
        boy.accessoryIndex = index
        audio.playStarTwinkle()
        val accessoryNames = listOf(
            "Natural look",
            "Cozy Ribbed Beanie",
            "Warm Wool Fringe Scarf",
            "Casual Streetwear Baseball Cap",
            "Rainbow Scarf",
            "Snowman Beanie"
        )
        val name = accessoryNames.getOrElse(index) { "Accessory" }
        if (index == 0) {
            showMessage(GameText.get(Res.string.scene_took_off_the_accessory, boy.name), duration = 2.5f)
        } else {
            showMessage(GameText.get(Res.string.scene_is_now_wearing_the, boy.name, name), duration = 3.0f)
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

    fun onTouchKitchenTreatJar() {
        grantRequest(RequestKind.MOCHI)
        if (catTreatInProgress) {
            showMessage(GameText.get(Res.string.scene_mochi_is_still_enjoying_the_last_little), duration = 1.8f)
            return
        }
        catTreatInProgress = true
        careForMochi(3)
        catTreatWalking = false
        catTreatJarTimer = 0.85f
        catTreatDropTimer = 0.48f
        catTreatMunchTimer = 0f
        audio.playBubblePop()
        showMessage(GameText.get(Res.string.scene_a_crunchy_little_treat_drops_for_mochi), duration = 2.2f)
    }

    private fun updateKitchenTreat(dt: Float, cw: Float, ch: Float) {
        if (currentScene != SceneType.COOKING || !catTreatInProgress) return
        if (catTreatDropTimer > 0f) return

        if (catTreatMunchTimer > 0f) {
            catTreatMunchTimer = (catTreatMunchTimer - dt).coerceAtLeast(0f)
            if (eventChance(1.2f, dt)) particles.spawnHeart(cw * 0.48f, ch * 0.67f, Color(0xFFFF6B8A))
            if (catTreatMunchTimer <= 0f) {
                catTreatInProgress = false
                catTreatWalking = false
                catState = CatState.SITTING_PURR
                catTargetX = catWorldX
                catTargetY = catWorldY
            }
            return
        }

        if (!catTreatWalking) {
            catTreatWalking = true
            catTargetX = 0.48f
            catTargetY = 0.72f
            catFacingLeft = catTargetX < catWorldX
            catState = CatState.WALK_FOLLOW
            catSleeping = false
        }

        val distance = kotlin.math.hypot(catTargetX - catWorldX, catTargetY - catWorldY)
        if (distance <= 0.018f) {
            catWorldX = catTargetX
            catWorldY = catTargetY
            catState = CatState.SITTING_PURR
            catTreatMunchTimer = 2.6f
            audio.playCatPurr()
            particles.spawnHeart(cw * catWorldX, ch * catWorldY - 14f * WorldViewport.pixelScale(cw), Color(0xFFFF6B8A))
            showMessage(GameText.get(Res.string.scene_mochi_munches_happily_with_a_swishy_tail), duration = 2.5f)
        }
    }

    fun onTouchMochiCollar() {
        mochiCollarStyle = if (mochiCollarStyle >= 2) 1 else mochiCollarStyle + 1
        audio.playStarTwinkle()
        showMessage(if (mochiCollarStyle == 1) "Mochi looks dapper in a tiny red bowtie!" else "A little daisy collar for Mochi!", duration = 2.6f)
    }

    fun onTouchCafeLatte(cw: Float, ch: Float) {
        cafeLatteTimer = 2.2f
        audio.playHeartChime()
        val latte = CafeLayout.latte(cw, ch, WorldViewport.pixelScale(cw))
        particles.spawnHeart(latte.x, latte.y - 8f, Color(0xFFFF729F))
        showMessage(GameText.get(Res.string.scene_a_tiny_heart_in_the_latte_foam_made_just), duration = 2.4f)
    }

    fun onTouchCafePastry(cw: Float, ch: Float) {
        grantRequest(RequestKind.SNACK)
        val plate = CafeLayout.plate(cw, ch, WorldViewport.pixelScale(cw))
        if (cafePastryBites >= CafeLayout.MAX_PASTRY_BITES) {
            cafePastryBites = 0
            cafeBaristaBrewTimer = 1.2f
            audio.playHeartChime()
            particles.spawnSparkles(plate.x, plate.y, 4, Color(0xFFFFD166))
            showMessage(GameText.get(Res.string.scene_barista_leo_brings_a_fresh_warm_croissan), duration = 2.2f)
            return
        }
        cafePastryBites += 1
        audio.playBubblePop()
        particles.spawnSparkles(plate.x, plate.y, 3, Color(0xFFFFD166))
        showMessage(if (cafePastryBites >= CafeLayout.MAX_PASTRY_BITES) "The croissant disappeared between you!" else "A tiny croissant nibble for two.", duration = 2.0f)
    }

    fun onTouchCafeWindow(touchX: Float, touchY: Float, cw: Float, ch: Float) {
        // Keep the whole fog heart on the glass (it spans -3..+4 heart pixels around the tap).
        val p = WorldViewport.pixelScale(cw)
        val glass = CafeLayout.glass(cw, ch, p)
        cafeWindowHeartTimer = 2.8f
        cafeWindowHeartX = touchX.coerceIn(glass.left + 5f * p, glass.right - 7f * p) / cw
        cafeWindowHeartY = touchY.coerceIn(glass.top + p, glass.bottom - 8f * p) / ch
        audio.playWaterDrip()
        particles.spawnHeart(touchX, touchY, Color(0xFFFFB6C9))
        showMessage(GameText.get(Res.string.scene_a_little_heart_fogs_the_rainy_window), duration = 2.2f)
    }

    fun onTouchCafeBarista(cw: Float, ch: Float) {
        cafeBaristaBrewTimer = 2.5f
        audio.playSteamHiss()
        audio.playHeartChime()
        val leo = CafeLayout.barista(cw, ch, WorldViewport.pixelScale(cw))
        particles.spawnSparkles(leo.x, leo.y - 30f, 6, Color(0xFFFFD166))
        particles.spawnHeart(leo.x, leo.y - 50f, Color(0xFFFF729F))
        boy.emotion = CharacterEmotion.HAPPY
        girl.emotion = CharacterEmotion.HAPPY
        boy.emote = EmoteType.SPARKLE
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 2.5f
        girl.emoteTimer = 2.5f
        boy.reactionTimer = 2.5f
        girl.reactionTimer = 2.5f
        showMessage(GameText.get(Res.string.scene_barista_leo_fresh_espresso_brewing_extra), duration = 3.0f)
    }

    fun onTouchCafeMenu() {
        audio.playPaperFlip()
        showMessage(GameText.get(Res.string.scene_today_s_specials_1_caramel_cloud_latte_2), duration = 3.2f)
    }

    fun onTouchCafePup(cw: Float, ch: Float) {
        cafePupPetTimer = 2.0f
        audio.playBubblePop()
        val pup = CafeLayout.pup(cw, ch, WorldViewport.pixelScale(cw))
        particles.spawnHeart(pup.x, pup.y - 30f, Color(0xFFFFCAD4))
        particles.spawnSparkles(pup.x, pup.y - 10f, 4, Color(0xFFFFD166))
        showMessage(GameText.get(Res.string.scene_boba_the_cafe_pup_wags_his_tail_and_naps), duration = 2.8f)
    }

    fun onTouchCafePasserby(touchX: Float, touchY: Float) {
        audio.playBubblePop()
        particles.spawnSparkles(touchX, touchY, 5, Color(0xFFFFE066))
        particles.spawnHeart(touchX, touchY - 14f, Color(0xFFFF9AA2))
        showMessage(GameText.get(Res.string.scene_a_friendly_neighbor_strolls_by_with_an_u), duration = 2.5f)
    }

    fun onTouchSunroomSkylight(touchX: Float, touchY: Float) {
        sunroomSkylightTimer = 1.8f
        audio.playWaterDrip()
        particles.spawnRainSplash(touchX, touchY)
        showMessage(GameText.get(Res.string.scene_raindrops_patter_softly_on_the_glass_roo), duration = 2.0f)
    }

    fun onTouchSunroomPlants(cw: Float, ch: Float) {
        sunroomBloomStage = (sunroomBloomStage + 1).coerceAtMost(4)
        sunroomMistTimer = 1.4f
        audio.playWaterDrip()
        particles.spawnSparkles(cw * 0.70f, ch * 0.55f, 5, Color(0xFFFFC1D7))
        showMessage(if (sunroomBloomStage == 1) "Fresh water reaches the little seedlings." else "Tiny flowers open among the succulents!", duration = 2.4f)
    }

    fun onTouchSunroomWateringCan(cw: Float, ch: Float) {
        sunroomMistTimer = 1.8f
        audio.playWaterDrip()
        particles.spawnSparkles(cw * 0.24f, ch * 0.69f, 6, Color(0xFFBFE9F7))
        showMessage(GameText.get(Res.string.scene_a_cool_morning_mist_curls_through_the_gr), duration = 2.3f)
    }

    fun onTouchCampfire(cw: Float, ch: Float, touchX: Float, touchY: Float) {
        grantRequest(RequestKind.WARM)
        campfireEmbersTimer = 2.8f
        marshmallowRoastingTimer = 3.5f
        audio.playCandleFlicker()
        repeat(5) {
            particles.spawnSparkles(touchX + (Random.nextFloat() - 0.5f) * 20f, touchY - 14f - Random.nextFloat() * 18f, 2, Color(0xFFFFB703))
        }
        particles.spawnHeart(cw * CampfireLayout.PIT_X, ch * 0.60f, Color(0xFFFF9AA2))
        boy.pose = CharacterPose.EAT_SNEAK
        girl.pose = CharacterPose.EAT_SNEAK
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 3.0f
        girl.emoteTimer = 3.0f
        boy.reactionTimer = 3.5f
        girl.reactionTimer = 3.5f
        showMessage(GameText.get(Res.string.scene_roasting_sweet_golden_marshmallows_over), duration = 3.2f)
    }

    fun onTouchCampGuitar(cw: Float, ch: Float) {
        grantRequest(RequestKind.SONG)
        campGuitarStrumTimer = 2.6f
        audio.playStarArpeggio()
        val guitar = CampfireLayout.guitar(cw, ch, WorldViewport.pixelScale(cw))
        particles.spawnSparkles(guitar.x, guitar.y, 5, Color(0xFFFFD166))
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.MUSIC_NOTE
        girl.emotion = CharacterEmotion.LOVING
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 2.6f
        girl.emoteTimer = 2.6f
        boy.reactionTimer = 3.0f
        girl.reactionTimer = 3.0f
        showMessage(GameText.get(Res.string.scene_strumming_a_quiet_acoustic_melody_beneat), duration = 3.0f)
    }

    fun onTouchCampLantern(cw: Float, ch: Float) {
        campLanternLit = !campLanternLit
        audio.playWoodKnock()
        val lantern = CampfireLayout.lantern(cw, ch, WorldViewport.pixelScale(cw))
        particles.spawnSparkles(lantern.x, lantern.y + 10f, 4, if (campLanternLit) Color(0xFFFFE066) else Color(0xFF888888))
        val dimmedMessage = if (timeOfDayPhase.isNight) "Dimmed the lantern for better stargazing" else "Lantern off until the stars come out"
        showMessage(if (campLanternLit) "The warm camp lantern glows bright beside our tent" else dimmedMessage, duration = 2.5f)
    }

    /** Mochi at the campfire: the usual cat reaction, with a blanket line when she was napping on it. */
    fun onTouchCampMochi(cw: Float, ch: Float) {
        grantRequest(RequestKind.MOCHI)
        val wasNappingOnBlanket = catState == CatState.SLEEPING && CampfireLayout.isOnBlanket(catWorldX, catWorldY)
        onTouchCat(cw, ch)
        if (wasNappingOnBlanket) {
            showMessage(GameText.get(Res.string.scene_mochi_stretches_and_purrs_on_the_cozy_pl), duration = 2.8f)
        }
    }

    // ── Seaside Pier ─────────────────────────────────────────────────────

    fun onTouchPierIceCream(cw: Float, ch: Float) {
        grantRequest(RequestKind.SNACK)
        if (pierIceCreamTimer > 0f) {
            showMessage(GameText.get(Res.string.scene_still_working_on_these_cones), duration = 1.8f)
            return
        }
        pierIceCreamTimer = PIER_ICE_CREAM_SECONDS
        audio.playHeartChime()
        val cart = PierLayout.cart(cw, ch)
        particles.spawnSparkles(cart.x, cart.y - 60f, 6, Color(0xFFFFC8DD))
        boy.emotion = CharacterEmotion.HAPPY
        girl.emotion = CharacterEmotion.HAPPY
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 2.5f
        girl.emoteTimer = 2.5f
        // Bao approves; Pip has noticed.
        pierBaoWaveTimer = 1.6f
        if (pierGullState == GullState.PERCHED) pierGullTimer = pierGullTimer.coerceAtMost(1.5f)
        showMessage(GameText.get(Res.string.scene_two_strawberry_cones_from_the_cart_one_f), duration = 3.0f)
    }

    fun onTouchGrandpaBao(cw: Float, ch: Float) {
        if (isBaoDozing) {
            pierBaoQuietTime = 0f
            pierBaoWaveTimer = 1.4f
            audio.playBubblePop()
            showMessage(GameText.get(Res.string.scene_grandpa_bao_hm_oh_i_was_only_resting_my), duration = 2.6f)
            return
        }
        pierBaoQuietTime = 0f
        when (pierFishingPhase) {
            PierFishingPhase.IDLE, PierFishingPhase.SHOWING -> startBaoCast(announce = true)
            PierFishingPhase.CASTING, PierFishingPhase.WAITING ->
                showMessage(GameText.get(Res.string.scene_grandpa_bao_shh_something_s_nibbling), duration = 2.2f)
            PierFishingPhase.REELING ->
                showMessage(GameText.get(Res.string.scene_grandpa_bao_easy_now_easy), duration = 1.8f)
        }
    }

    private fun startBaoCast(announce: Boolean) {
        pierFishingPhase = PierFishingPhase.CASTING
        pierFishingTimer = 0.8f
        pierLastCatch = null
        pierBaoSipTimer = 0f
        audio.playReelClick()
        if (announce) showMessage(baoLinePicker.pick(), duration = 3.2f)
    }

    fun onTouchPip(cw: Float, ch: Float) {
        if (!pierGullState.isVisible || pierGullState == GullState.ESCAPING) return
        val caughtInTheAct = pierGullState == GullState.SNEAKING || pierGullState == GullState.STEALING
        pierGullState = GullState.ESCAPING
        audio.playSeagullCall()
        particles.spawnSparkles(cw * pierGullX, ch * pierGullY, 6, Color(0xFFF5F5F5))
        showMessage(
            if (caughtInTheAct) "Caught red-beaked! Pip flaps away empty-winged"
            else "Shoo, Pip! The seagull flaps off with an offended squawk",
            duration = 2.6f
        )
    }

    fun onTouchPierBottle(cw: Float, ch: Float) {
        if (!pierBottleVisible) return
        pierBottleVisible = false
        pierBottleRespawnTimer = PIER_BOTTLE_RESPAWN_SECONDS
        audio.playPaperFlip()
        val bottle = PierLayout.bottle(cw, ch)
        particles.spawnSparkles(bottle.x, bottle.y, 5, Color(0xFFBDE0FE))
        showMessage(GameText.get(Res.string.scene_a_message_in_a_bottle, dailyPromptProvider()), duration = 5.0f)
    }

    fun onTouchLighthouse(cw: Float, ch: Float) {
        pierLighthouseTimer = 4f
        audio.playFoghorn()
        val lamp = PierLayout.lighthouseLamp(cw, ch, WorldViewport.pixelScale(cw))
        particles.spawnSparkles(lamp.x, lamp.y, 6, Color(0xFFFFF3B0))
        if (timeOfDayPhase.isNight) {
            showMessage(GameText.get(Res.string.scene_the_lighthouse_sweeps_its_beam_across_th), duration = 2.8f)
        } else {
            // The foghorn startles a little flock off the rocks.
            pierFlockTimer = PIER_FLOCK_SECONDS
            showMessage(GameText.get(Res.string.scene_a_low_foghorn_hums_across_the_bay_and_th), duration = 2.8f)
        }
    }

    fun onTouchPierSea(touchX: Float, touchY: Float) {
        audio.playWaterDrip()
        particles.spawnRainSplash(touchX, touchY)
        particles.spawnSparkles(touchX, touchY - 6f, 3, Color(0xFFCFEFFF))
    }

    /** The coin telescope: a pod of dolphins leaps past. */
    fun onTouchPierTelescope(cw: Float, ch: Float) {
        if (pierDolphinTimer > 0f) return
        pierDolphinTimer = PIER_DOLPHIN_SECONDS
        audio.playBubblePop()
        audio.playHeartChime()
        val scope = PierLayout.telescope(cw, ch)
        particles.spawnSparkles(scope.x, scope.y - 50f, 4, Color(0xFFFFD166))
        girl.emotion = CharacterEmotion.SURPRISED
        girl.emote = EmoteType.EXCLAMATION
        girl.emoteTimer = 2.2f
        boy.emotion = CharacterEmotion.HAPPY
        boy.emote = EmoteType.SPARKLE
        boy.emoteTimer = 2.2f
        showMessage(GameText.get(Res.string.scene_clink_through_the_telescope_dolphins_lea), duration = 3.0f)
    }

    /** The little sailboat on the horizon toots back and waves its flag. */
    fun onTouchPierBoat(cw: Float, ch: Float, touchX: Float, touchY: Float) {
        if (pierBoatHornTimer > 0f) return
        pierBoatHornTimer = 2.4f
        audio.playBoatHorn()
        particles.spawnSparkles(touchX, touchY - 10f, 3, Color(0xFFFFFFFF))
        boy.emote = EmoteType.MUSIC_NOTE
        boy.emoteTimer = 1.8f
        showMessage(GameText.get(Res.string.scene_toot_toot_the_little_sailboat_waves_its), duration = 2.6f)
    }

    /** Pinchy the crab scuttles for cover, and Mochi gives chase. */
    fun onTouchPierCrab(cw: Float, ch: Float) {
        if (!isPierCrabVisible || pierCrabStartledTimer > 0f) return
        pierCrabStartledTimer = 1.2f
        pierCrabFacingLeft = pierCrabX < 0.5f
        audio.playWoodKnock()
        val crab = PierLayout.crab(cw, ch, pierCrabX)
        particles.spawnSparkles(crab.x, crab.y - 20f, 3, Color(0xFFFF8C69))
        if (catState != CatState.WALK_FOLLOW) {
            catTargetX = (pierCrabX + if (pierCrabFacingLeft) -0.08f else 0.08f).coerceIn(0.26f, 0.74f)
            catTargetY = 0.74f
            catFacingLeft = catTargetX < catWorldX
            catState = CatState.WALK_FOLLOW
            catSleeping = false
        }
        showMessage(GameText.get(Res.string.scene_pinchy_the_crab_scuttles_off_sideways_an), duration = 2.6f)
    }

    /** A fish flops out of Bao's bait bucket, which Mochi finds very interesting. */
    fun onTouchPierBucket(cw: Float, ch: Float) {
        if (pierBucketFlopTimer > 0f) return
        pierBucketFlopTimer = 1.6f
        pierBaoQuietTime = 0f
        pierBaoWaveTimer = 1.4f
        audio.playWaterDrip()
        val bucket = PierLayout.bucket(cw, ch)
        particles.spawnRainSplash(bucket.x, bucket.y - 30f)
        catTargetX = PierLayout.bucket(1f, 1f).x - 0.04f
        catTargetY = 0.74f
        catFacingLeft = catTargetX < catWorldX
        catState = CatState.WALK_FOLLOW
        catSleeping = false
        showMessage(GameText.get(Res.string.scene_grandpa_bao_hey_that_s_my_bait_mochi), duration = 2.6f)
    }

    /** The railing string lights cycle through their colours. */
    fun onTouchPierLights(cw: Float, ch: Float, touchX: Float) {
        pierLightsPalette = (pierLightsPalette + 1) % PIER_LIGHT_PALETTES
        pierLightsSparkleTimer = 1.2f
        audio.playStarTwinkle()
        particles.spawnSparkles(touchX, ch * PierLayout.RAIL_Y - 10f, 5, Color(0xFFFFF3B0))
    }

    private fun updatePierScene(dt: Float, cw: Float, ch: Float) {
        updatePierGull(dt, cw, ch)
        updatePierFishing(dt, cw, ch)
        updateGrandpaBao(dt, cw, ch)
        updatePierCrab(dt)
        if (!pierBottleVisible) {
            pierBottleRespawnTimer -= dt
            if (pierBottleRespawnTimer <= 0f) pierBottleVisible = true
        }
        if (pierDolphinTimer > 0f) pierDolphinTimer = (pierDolphinTimer - dt).coerceAtLeast(0f)
        if (pierBoatHornTimer > 0f) pierBoatHornTimer = (pierBoatHornTimer - dt).coerceAtLeast(0f)
        if (pierFlockTimer > 0f) pierFlockTimer = (pierFlockTimer - dt).coerceAtLeast(0f)
        if (pierBucketFlopTimer > 0f) pierBucketFlopTimer = (pierBucketFlopTimer - dt).coerceAtLeast(0f)
        if (pierLightsSparkleTimer > 0f) pierLightsSparkleTimer = (pierLightsSparkleTimer - dt).coerceAtLeast(0f)
    }

    /** Bao keeps busy between taps: a wave hello, sips of tea, casting on his own, dozing late at night. */
    private fun updateGrandpaBao(dt: Float, cw: Float, ch: Float) {
        if (pierBaoWaveTimer > 0f) pierBaoWaveTimer = (pierBaoWaveTimer - dt).coerceAtLeast(0f)
        if (pierBaoSipTimer > 0f) pierBaoSipTimer = (pierBaoSipTimer - dt).coerceAtLeast(0f)

        if (pierBaoGreetPending && sceneTime > 1.8f) {
            pierBaoGreetPending = false
            pierBaoWaveTimer = 2.0f
        }

        if (pierFishingPhase == PierFishingPhase.IDLE) pierBaoQuietTime += dt else pierBaoQuietTime = 0f
        if (isBaoDozing) {
            if (eventChance(0.35f, dt)) {
                val bao = PierLayout.bao(cw, ch)
                particles.spawnSleepZ(bao.x + 10f, bao.y - 26f * WorldViewport.pixelScale(cw))
            }
            return
        }

        val canSip = pierFishingPhase == PierFishingPhase.IDLE || pierFishingPhase == PierFishingPhase.WAITING
        pierBaoSipCooldown -= dt
        if (canSip && pierBaoSipCooldown <= 0f && pierBaoWaveTimer <= 0f) {
            pierBaoSipTimer = 2.4f
            pierBaoSipCooldown = 9f + pierRng.nextFloat() * 7f
        }

        // By day he fishes on his own; at night he'd rather nod off.
        if (pierFishingPhase == PierFishingPhase.IDLE && !timeOfDayPhase.isNight) {
            pierBaoSelfCastTimer -= dt
            if (pierBaoSelfCastTimer <= 0f && pierBaoSipTimer <= 0f) {
                pierBaoSelfCastTimer = 20f + pierRng.nextFloat() * 12f
                startBaoCast(announce = false)
            }
        }
    }

    /** Pinchy pootles along the boardwalk in little bursts; when startled he dashes off and hides. */
    private fun updatePierCrab(dt: Float) {
        if (pierCrabHiddenTimer > 0f) {
            pierCrabHiddenTimer -= dt
            if (pierCrabHiddenTimer <= 0f) {
                // Peeks back out from whichever end he fled to.
                pierCrabFacingLeft = pierCrabX > 0.5f
                pierCrabPauseTimer = 1f
            }
            return
        }
        if (pierCrabStartledTimer > 0f) {
            pierCrabStartledTimer -= dt
            val edge = if (pierCrabFacingLeft) PierLayout.CRAB_MIN_X else PierLayout.CRAB_MAX_X
            pierCrabX += (if (pierCrabFacingLeft) -1f else 1f) * 0.32f * dt
            if (pierCrabFacingLeft && pierCrabX <= edge || !pierCrabFacingLeft && pierCrabX >= edge) {
                pierCrabX = edge
                pierCrabStartledTimer = 0f
                pierCrabHiddenTimer = 12f + pierRng.nextFloat() * 6f
            }
            return
        }
        if (pierCrabPauseTimer > 0f) {
            pierCrabPauseTimer -= dt
            return
        }
        pierCrabX += (if (pierCrabFacingLeft) -1f else 1f) * 0.05f * dt
        if (pierCrabX <= PierLayout.CRAB_MIN_X || pierCrabX >= PierLayout.CRAB_MAX_X) {
            pierCrabX = pierCrabX.coerceIn(PierLayout.CRAB_MIN_X, PierLayout.CRAB_MAX_X)
            pierCrabFacingLeft = !pierCrabFacingLeft
        }
        if (eventChance(0.5f, dt)) pierCrabPauseTimer = 0.6f + pierRng.nextFloat() * 1.4f
    }

    private fun updatePierGull(dt: Float, cw: Float, ch: Float) {
        when (pierGullState) {
            GullState.AWAY -> {
                pierGullTimer -= dt
                if (pierGullTimer > 0f) return
                pierGullX = 1.1f
                pierGullY = 0.18f + pierRng.nextFloat() * 0.08f
                pierGullTargetX = PierLayout.PERCH_XS[pierRng.nextInt(PierLayout.PERCH_XS.size)]
                pierGullState = GullState.FLYING
            }
            GullState.FLYING -> {
                if (moveGullToward(pierGullTargetX, PierLayout.RAIL_Y, 0.22f, dt)) {
                    pierGullState = GullState.PERCHED
                    pierGullTimer = 4f + pierRng.nextFloat() * 4f
                }
            }
            GullState.PERCHED -> {
                // In the rain Pip tucks in and waits it out.
                if (weather == WeatherType.RAIN) return
                if (pierIceCreamTimer > 0f) pierGullTimer = pierGullTimer.coerceAtMost(1.5f)
                pierGullTimer -= dt
                if (pierGullTimer > 0f) return
                if (pierIceCreamTimer > 0f) {
                    pierGullTargetX = (boy.worldX + girl.worldX) / 2f
                    pierGullState = GullState.SNEAKING
                } else if (pierRng.nextFloat() < 0.5f) {
                    val others = PierLayout.PERCH_XS.filter { abs(it - pierGullX) > 0.05f }
                    pierGullTargetX = others[pierRng.nextInt(others.size)]
                    pierGullState = GullState.FLYING
                } else {
                    pierGullTimer = 4f + pierRng.nextFloat() * 4f
                }
            }
            GullState.SNEAKING -> {
                if (pierIceCreamTimer <= 0f) {
                    pierGullState = GullState.PERCHED
                    pierGullTimer = 3f
                    return
                }
                if (moveGullToward(pierGullTargetX, PierLayout.RAIL_Y, 0.12f, dt)) {
                    pierGullState = GullState.STEALING
                    pierGullTimer = 0.6f
                }
            }
            GullState.STEALING -> {
                moveGullToward(pierGullTargetX, boy.worldY - 0.06f, 0.35f, dt)
                pierGullTimer -= dt
                if (pierGullTimer <= 0f) stealPierIceCream(cw, ch)
            }
            GullState.ESCAPING -> {
                if (moveGullToward(-0.12f, 0.08f, 0.45f, dt)) {
                    pierGullState = GullState.AWAY
                    pierGullTimer = 10f + pierRng.nextFloat() * 6f
                }
            }
        }
    }

    /** Moves Pip toward a point at [speed] (normalized units per second); true once there. */
    private fun moveGullToward(targetX: Float, targetY: Float, speed: Float, dt: Float): Boolean {
        val dx = targetX - pierGullX
        val dy = targetY - pierGullY
        val dist = kotlin.math.hypot(dx, dy)
        if (abs(dx) > 0.002f) pierGullFacingLeft = dx < 0f
        val step = speed * dt
        if (dist <= step) {
            pierGullX = targetX
            pierGullY = targetY
            return true
        }
        pierGullX += dx / dist * step
        pierGullY += dy / dist * step
        return false
    }

    private fun stealPierIceCream(cw: Float, ch: Float) {
        pierIceCreamTimer = 0f
        pierGullState = GullState.ESCAPING
        audio.playSeagullCall()
        particles.spawnSparkles(cw * pierGullX, ch * pierGullY, 5, Color(0xFFFFC8DD))
        for (c in listOf(boy, girl)) {
            c.pose = CharacterPose.JOY_JUMP
            c.emotion = CharacterEmotion.SURPRISED
            c.emote = EmoteType.EXCLAMATION
            c.emoteTimer = 2.2f
            c.reactionTimer = 2.2f
        }
        showMessage(GameText.get(Res.string.scene_pip_stole_the_ice_cream), duration = 3.0f)
    }

    private fun updatePierFishing(dt: Float, cw: Float, ch: Float) {
        if (pierFishingPhase == PierFishingPhase.IDLE) return
        pierFishingTimer -= dt
        if (pierFishingTimer > 0f) return
        when (pierFishingPhase) {
            PierFishingPhase.CASTING -> {
                pierFishingPhase = PierFishingPhase.WAITING
                pierFishingTimer = 2f + pierRng.nextFloat() * 1.5f
            }
            PierFishingPhase.WAITING -> {
                pierFishingPhase = PierFishingPhase.REELING
                pierFishingTimer = 1.2f
                audio.playReelClick()
            }
            PierFishingPhase.REELING -> {
                val catch = pierCatchPicker.pick()
                pierLastCatch = catch
                pierFishingPhase = PierFishingPhase.SHOWING
                pierFishingTimer = 3f
                audio.playHeartChime()
                val bao = PierLayout.bao(cw, ch)
                particles.spawnSparkles(bao.x, bao.y - 40f, 5, Color(0xFFBDE0FE))
                showMessage(catch.message, duration = 3.2f)
                if (catch == PierCatch.FISH) {
                    // Mochi knows exactly who that fish is for.
                    catTargetX = PierLayout.MOCHI_FISH_X
                    catTargetY = PierLayout.MOCHI_FISH_Y
                    catFacingLeft = catTargetX < catWorldX
                    catState = CatState.WALK_FOLLOW
                    catSleeping = false
                }
            }
            PierFishingPhase.SHOWING -> {
                pierFishingPhase = PierFishingPhase.IDLE
                pierLastCatch = null
            }
            PierFishingPhase.IDLE -> Unit
        }
    }

    /**
     * Pier watch scene: one cone shared on the bench, Pip dives in to steal it,
     * the couple dissolve into laughter and end in a hug as Bao shakes his head.
     */
    private fun updatePierWatchScene(t: Float, dt: Float, cw: Float, ch: Float): Boolean {
        val mid = (boy.worldX + girl.worldX) / 2f
        when {
            t < 1.4f -> {
                boy.pose = CharacterPose.SIT
                girl.pose = CharacterPose.SIT_SNUGGLE
                boy.direction = Direction.RIGHT
                girl.direction = Direction.LEFT
                if (pierBaoWaveTimer <= 0f && t < 0.2f) {
                    pierBaoWaveTimer = 1.6f
                    showMessage(GameText.get(Res.string.scene_grandpa_bao_go_on_share_a_cone_with_your), duration = 3.0f)
                }
            }
            t < 4.2f -> {
                // Serve the cone once, on the first frame of this beat.
                if (t - dt < 1.4f) {
                    pierIceCreamTimer = PIER_ICE_CREAM_SECONDS
                    audio.playHeartChime()
                    girlSpeechText = GameText.get(Res.string.scene_one_cone_two_of_us)
                    girlSpeechTimer = 2.4f
                    girl.emote = EmoteType.HEART
                    girl.emoteTimer = 2.0f
                }
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.LOVING
            }
            t < 5.6f -> {
                // Pip has been waiting for exactly this.
                if (pierGullState != GullState.STEALING && pierGullState != GullState.ESCAPING && pierIceCreamTimer > 0f) {
                    pierGullX = (mid + 0.30f).coerceAtMost(1.05f)
                    pierGullY = 0.36f
                    pierGullTargetX = mid
                    pierGullState = GullState.STEALING
                    pierGullTimer = 1.0f
                }
            }
            t < 7.6f -> {
                val laugh = ((t * 4).toInt() % 2) == 0
                boy.pose = if (laugh) CharacterPose.JOY_JUMP else CharacterPose.SIT
                girl.pose = if (laugh) CharacterPose.SIT_SNUGGLE else CharacterPose.JOY_JUMP
                boy.emotion = CharacterEmotion.HAPPY
                girl.emotion = CharacterEmotion.HAPPY
                if (boySpeechText == null && t > 5.8f) {
                    boySpeechText = GameText.get(Res.string.scene_that_bird)
                    boySpeechTimer = 1.8f
                    showMessage(GameText.get(Res.string.scene_grandpa_bao_told_you_a_criminal), duration = 2.4f)
                }
            }
            t < 9.2f -> {
                boy.pose = CharacterPose.HUG
                girl.pose = CharacterPose.HUG
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.LOVING
                if (eventChance(1.2f, dt)) {
                    particles.spawnHeart(cw * mid + (Random.nextFloat() - 0.5f) * 40f, ch * boy.worldY - 60f)
                }
            }
            else -> return false
        }
        return true
    }

    /** Idle pier moments for the couple when nobody has tapped in a while. */
    private fun triggerPierAutonomousMoment(cw: Float, ch: Float) {
        when (pierAutonomousPicker.pick()) {
            0 -> {
                // She spots dolphins all on her own.
                pierDolphinTimer = PIER_DOLPHIN_SECONDS
                girl.emotion = CharacterEmotion.SURPRISED
                girl.emote = EmoteType.EXCLAMATION
                girl.emoteTimer = 2.2f
                girl.reactionTimer = 2.2f
                boy.direction = Direction.RIGHT
                audio.playBubblePop()
                showMessage(GameText.get(Res.string.scene_points_out_to_sea_dolphins, girl.name), duration = 2.8f)
            }
            1 -> {
                // A quiet lean together while the waves roll in.
                boy.emotion = CharacterEmotion.LOVING
                girl.emotion = CharacterEmotion.LOVING
                girl.emote = EmoteType.HEART
                girl.emoteTimer = 2.4f
                pierBaoWaveTimer = 1.4f
                particles.spawnHeart(cw * (boy.worldX + girl.worldX) / 2f, ch * boy.worldY - 70f)
                audio.playStarTwinkle()
                showMessage(GameText.get(Res.string.scene_just_the_waves_the_breeze_and_the_two_of), duration = 3.0f)
            }
            else -> {
                // Pip drops by to see what's on offer.
                if (pierGullState == GullState.AWAY) pierGullTimer = 0f
                boy.emote = EmoteType.QUESTION
                boy.emoteTimer = 2.0f
                boySpeechText = GameText.get(Res.string.scene_don_t_even_think_about_it_pip)
                boySpeechTimer = 2.4f
            }
        }
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
        val pixelScale = WorldViewport.pixelScale(cw)
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
        showMessage(GameText.get(Res.string.scene_scooter_road_trip_with_my_favourite_pers), duration = 2.5f)
        repeat(6) {
            particles.spawnHeart(cw * 0.50f + (Random.nextFloat() - 0.5f) * 35f, ch * 0.65f - 25f)
        }
        particles.spawnSparkles(cw * 0.50f, ch * 0.68f, 6)
    }

    fun onTouchGirlScooter(cw: Float, ch: Float) {
        girlSpeechText = GameText.get(Res.string.scene_hold_tight_the_scooter_is_fast_hehe, boy.name)
        girlSpeechTimer = 4.0f
        girl.emotion = CharacterEmotion.HAPPY
        audio.playHeartChime()
        particles.spawnHeart(cw * 0.53f, ch * 0.60f, Color(0xFFFF5D8F))
    }

    fun onTouchBoyScooter(cw: Float, ch: Float) {
        boySpeechText = GameText.get(Res.string.scene_holding_you_forever_my_love)
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
        girlSpeechText = GameText.get(Res.string.scene_hold_on_tight)
        girlSpeechTimer = 3.5f
        boySpeechText = GameText.get(Res.string.scene_holding_you_forever_my_love)
        boySpeechTimer = 3.5f
        audio.playHeartChime()
        repeat(8) {
            particles.spawnHeart(cw * 0.48f + (Random.nextFloat() - 0.5f) * 40f, ch * 0.65f - 20f, Color(0xFFFF3366))
        }
        particles.spawnSparkles(cw * 0.50f, ch * 0.68f, 6)
        showMessage(GameText.get(Res.string.scene_holding_tight_together_on_our_evening_ri), duration = 3.5f)
    }

    fun onTouchTempleSpire(cw: Float, ch: Float) {
        if (templeGlowTimer > 0f) return
        templeGlowTimer = 2.0f
        audio.playStarArpeggio()
        showMessage(GameText.get(Res.string.scene_the_temple_in_the_distance_glows_in_the), duration = 3.0f)
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
        showMessage(GameText.get(Res.string.scene_entering_our_cozy_cottage), duration = 2.5f)
    }

    fun onCycleCottageRoom() {
        audio.playBubblePop()
        when (currentScene) {
            SceneType.COOKING -> {
                loadScene(SceneType.SLEEP)
                showMessage(GameText.get(Res.string.scene_cottage_living_room_warm_couch_and_memor), duration = 2.5f)
            }
            SceneType.SLEEP -> {
                loadScene(SceneType.COZY_LOFT)
                showMessage(GameText.get(Res.string.scene_cottage_bedroom_loft_stargazing_under_th), duration = 2.5f)
            }
            SceneType.COZY_LOFT -> {
                loadScene(SceneType.FLOWER)
                showMessage(GameText.get(Res.string.scene_stepping_back_out_into_the_fresh_meadow), duration = 2.5f)
            }
            else -> {
                loadScene(SceneType.COOKING)
                showMessage(GameText.get(Res.string.scene_entering_our_cozy_cottage), duration = 2.5f)
            }
        }
    }

    /**
     * Rain clearing by day leaves a rainbow; the snowman only lasts while it snows,
     * and catches are counted per weather spell.
     */
    private fun updateWeatherKeepsakes(dt: Float) {
        val previous = lastSeenWeather
        if (previous != weather) {
            if (previous == WeatherType.RAIN && weather != WeatherType.RAIN && !timeOfDayPhase.isNight) {
                rainbowTimer = WeatherLayout.RAINBOW_SECONDS
            }
            if (weather != WeatherType.SNOW) snowmanStage = 0
            weatherCatchCount = 0
            groundHintShown = false
            lastSeenWeather = weather
        }
        if (rainbowTimer > 0f) rainbowTimer = (rainbowTimer - dt).coerceAtLeast(0f)
        if (snowmanWobbleTimer > 0f) snowmanWobbleTimer = (snowmanWobbleTimer - dt).coerceAtLeast(0f)
    }

    /**
     * Walking through the weather: footprints and pawprints in snow, leaves and petals
     * kicked up underfoot, and splashes when someone steps through a puddle.
     */
    private fun updateGroundWeatherPlay(dt: Float, cw: Float, ch: Float) {
        for (i in puddleSplashCooldown.indices) {
            if (puddleSplashCooldown[i] > 0f) puddleSplashCooldown[i] -= dt
        }
        if (!isCurrentSceneOutdoor || currentScene == SceneType.EVENING_RIDE || cw <= 0f || ch <= 0f) return
        val unit = WeatherLayout.weatherUnit(cw, WorldViewport.pixelScale(cw))
        val catMoving = catState == CatState.WALK_FOLLOW &&
            (abs(catTargetX - catWorldX) > 0.01f || abs(catTargetY - catWorldY) > 0.01f)
        for (i in 0..2) {
            val x: Float
            val y: Float
            val moving: Boolean
            val facingLeft: Boolean
            when (i) {
                0 -> { x = boy.worldX; y = boy.worldY; moving = boy.isTransitioningPosition; facingLeft = boy.direction == Direction.LEFT }
                1 -> { x = girl.worldX; y = girl.worldY; moving = girl.isTransitioningPosition; facingLeft = girl.direction == Direction.LEFT }
                else -> { x = catWorldX; y = catWorldY; moving = catMoving; facingLeft = catFacingLeft }
            }
            if (!moving) {
                lastPrintX[i] = -1f
                continue
            }
            when (weather) {
                WeatherType.SNOW -> {
                    val step = if (i == 2) 0.028f else 0.045f
                    if (lastPrintX[i] < 0f || kotlin.math.hypot(x - lastPrintX[i], y - lastPrintY[i]) >= step) {
                        particles.addSnowPrint(x, y, if (i == 2) SnowPrintKind.PAW else SnowPrintKind.FOOT, facingLeft)
                        lastPrintX[i] = x
                        lastPrintY[i] = y
                    }
                }
                WeatherType.AUTUMN, WeatherType.SAKURA -> {
                    val kicked = particles.kickGroundParticles(cw * x, ch * y, cw, ch, unit * 7f, facingLeft)
                    if (kicked > 0) playRustleThrottled()
                }
                WeatherType.RAIN -> {
                    if (puddleSplashCooldown[i] <= 0f && particles.puddleAt(x, y, cw, ch, unit) != null) {
                        puddleSplashCooldown[i] = 0.6f
                        splashAt(cw * x, ch * y)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun playRustleThrottled() {
        if (sceneTime - lastRustleTime < 0.25f) return
        lastRustleTime = sceneTime
        audio.playLeafRustle()
    }

    private fun splashAt(x: Float, y: Float) {
        audio.playWaterDrip()
        particles.spawnRainSplash(x, y)
        particles.spawnRainSplash(x - 14f, y + 3f)
        particles.spawnRainSplash(x + 14f, y + 3f)
        particles.spawnSparkles(x, y - 12f, 3, Color(0xFFBFE6FF))
    }

    /** A swipe tossed some leaves or petals into the air. */
    fun onGroundSwept(count: Int) {
        if (count <= 0) return
        playRustleThrottled()
        if (!groundHintShown) {
            groundHintShown = true
            showMessage(
                if (weather == WeatherType.AUTUMN) "Leaves crunch and tumble under your fingers."
                else "Petals swirl up and drift back down like pink snow.",
                duration = 2.6f
            )
        }
    }

    /** A finger drawing in fresh snow leaves a trail of little hollows. */
    fun onDrawInSnow(x: Float, y: Float, cw: Float, ch: Float) {
        if (weather != WeatherType.SNOW || !isCurrentSceneOutdoor) return
        val unit = WeatherLayout.weatherUnit(cw, WorldViewport.pixelScale(cw))
        if (lastSnowTraceX >= 0f && kotlin.math.hypot(x - lastSnowTraceX, y - lastSnowTraceY) < unit * 1.1f) return
        lastSnowTraceX = x
        lastSnowTraceY = y
        particles.addSnowPrint(x / cw, y / ch, SnowPrintKind.TRACE, facingLeft = false)
        if (!groundHintShown) {
            groundHintShown = true
            audio.playStarTwinkle()
            showMessage(GameText.get(Res.string.scene_you_draw_in_the_fresh_snow_maybe_a_littl), duration = 2.6f)
        }
    }

    /** Lift the finger: the next stroke starts fresh instead of joining the last one. */
    fun endSnowStroke() {
        lastSnowTraceX = -1f
        lastSnowTraceY = -1f
    }

    /** Tap a puddle: a big splash, and whoever is nearest does a happy little puddle jump. */
    fun onTouchPuddle(puddle: Puddle, cw: Float, ch: Float) {
        val x = cw * puddle.normX
        val y = ch * puddle.normY
        splashAt(x, y)
        particles.spawnRainSplash(x, y - 6f)
        val jumper = if (abs(boy.worldX - puddle.normX) <= abs(girl.worldX - puddle.normX)) boy else girl
        if (jumper.reactionTimer <= 0f && !jumper.isMovingOrTransitioning && !isWatchSceneActive) {
            jumper.transitionPoseTo(CharacterPose.JOY_JUMP)
            jumper.bounceOffset = 5f
            jumper.emotion = CharacterEmotion.HAPPY
            jumper.emote = EmoteType.SPARKLE
            jumper.emoteTimer = 1.6f
            jumper.reactionTimer = 1.4f
        }
        if (!groundHintShown) {
            groundHintShown = true
            showMessage(GameText.get(Res.string.scene_splash_perfect_puddle_jumping_weather), duration = 2.4f)
        }
    }

    /** A tap caught a falling snowflake, petal, leaf or dandelion puff. */
    fun onCatchWeather(caught: ParticleSystem.CaughtWeather) {
        weatherCatchCount++
        onProgress?.invoke(com.example.progress.ProgressEvent.Caught(caught.type.name))
        val firstCatch = weatherCatchCount == 1
        when (caught.type) {
            ParticleType.SNOWFLAKE -> {
                audio.playStarTwinkle()
                particles.spawnSparkles(caught.x, caught.y, 5, Color(0xFFF2FAFF))
                if (firstCatch) showMessage(snowCatchLines.pick(), duration = 3.0f)
                growSnowman()
            }
            ParticleType.SAKURA_PETAL -> {
                audio.playHeartChime()
                particles.spawnHeart(caught.x, caught.y, Color(0xFFFFB7C5))
                if (firstCatch) showMessage(petalCatchLines.pick(), duration = 3.0f)
            }
            ParticleType.AUTUMN_LEAF -> {
                audio.playLeafRustle()
                particles.spawnSparkles(caught.x, caught.y, 4, Color(0xFFF4A261))
                if (firstCatch) showMessage(leafCatchLines.pick(), duration = 3.0f)
            }
            else -> {
                audio.playStarTwinkle()
                particles.spawnHeart(caught.x, caught.y, Color(0xFFFFF3B0))
                if (firstCatch) showMessage(fluffCatchLines.pick(), duration = 3.0f)
            }
        }
        if (weatherCatchCount % 5 == 0) {
            val noun = when (caught.type) {
                ParticleType.SNOWFLAKE -> "snowflakes"
                ParticleType.SAKURA_PETAL -> "petals"
                ParticleType.AUTUMN_LEAF -> "leaves"
                else -> "wishes"
            }
            boy.emote = EmoteType.HEART
            girl.emote = EmoteType.HEART
            boy.emoteTimer = 2.2f
            girl.emoteTimer = 2.2f
            audio.playStarArpeggio()
            showMessage(GameText.get(Res.string.scene_caught_together, weatherCatchCount, noun), duration = 2.6f)
        }
    }

    /** Every few snowflakes caught, the snowman in the corner grows a little more. */
    private fun growSnowman() {
        if (weather != WeatherType.SNOW || !isCurrentSceneOutdoor) return
        val target = (weatherCatchCount / WeatherLayout.SNOWFLAKES_PER_STAGE).coerceAtMost(WeatherLayout.SNOWMAN_MAX_STAGE)
        if (target <= snowmanStage) return
        snowmanStage = target
        if (snowmanStage == WeatherLayout.SNOWMAN_MAX_STAGE) onProgress?.invoke(com.example.progress.ProgressEvent.SnowmanBuilt)
        snowmanWobbleTimer = 0.8f
        audio.playBubblePop()
        showMessage(
            when (snowmanStage) {
                1 -> "A little snowball starts rolling in the corner..."
                2 -> "The snowman has a body now!"
                3 -> "Head on! The snowman is smiling at you two."
                else -> "Scarf and carrot nose: your snowman is complete!"
            },
            duration = 3.0f
        )
    }

    fun onTouchSnowman(cw: Float, ch: Float) {
        if (snowmanStage <= 0) return
        snowmanWobbleTimer = 0.8f
        audio.playBubblePop()
        val base = WeatherLayout.snowmanBase(cw, ch)
        particles.spawnSparkles(base.x, base.y - 60f, 4, Color(0xFFF2FAFF))
        if (snowmanStage >= WeatherLayout.SNOWMAN_MAX_STAGE) {
            boy.emote = EmoteType.HEART
            girl.emote = EmoteType.HEART
            boy.emoteTimer = 2.0f
            girl.emoteTimer = 2.0f
            showMessage(GameText.get(Res.string.scene_the_snowman_wobbles_happily_it_looks_a_b), duration = 2.8f)
        } else {
            showMessage(GameText.get(Res.string.scene_the_snowman_wobbles_catch_a_few_more_sno), duration = 2.6f)
        }
    }

    fun onTouchRainbow(cw: Float, ch: Float) {
        if (rainbowTimer <= 0f) return
        onProgress?.invoke(com.example.progress.ProgressEvent.RainbowWish)
        audio.playStarArpeggio()
        val c = WeatherLayout.rainbowCenter(cw, ch)
        val r = WeatherLayout.rainbowOuterRadius(cw) - WeatherLayout.rainbowBandWidth(cw) / 2f
        for (i in 0..4) {
            val a = kotlin.math.PI.toFloat() * (0.15f + i * 0.175f)
            particles.spawnSparkles(c.x - kotlin.math.cos(a) * r, c.y - kotlin.math.sin(a) * r, 2, Color(0xFFFFF3B0))
        }
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.SPARKLE
        girl.emote = EmoteType.HEART
        boy.emoteTimer = 2.4f
        girl.emoteTimer = 2.4f
        showMessage(GameText.get(Res.string.scene_a_rainbow_after_the_rain_make_a_wish_on), duration = 3.2f)
    }

    // ── Autonomous behavior ──────────────────────────────────────────────
    // Each character runs a small decide → walk → perform → rest routine. The brain picks
    // what to do with weighted randomness shaped by the scene, weather, time of day, the
    // partner, Mochi and what they did recently. User taps, cinematics and opening scripts
    // always win: autonomy pauses and picks up again naturally afterwards.

    private val talkLinePicker = AntiRepeatRandomPicker(listOf(
        Res.string.scene_auto_talk_cloud_mochi, Res.string.scene_auto_talk_happy_now,
        Res.string.scene_auto_talk_keep_you, Res.string.scene_auto_talk_what_next,
        Res.string.scene_auto_talk_thinking_of_you, Res.string.scene_auto_talk_stay_like_this,
        Res.string.scene_auto_talk_tea_later, Res.string.scene_auto_talk_your_laugh
    ))
    private val replyLinePicker = AntiRepeatRandomPicker(listOf(
        Res.string.scene_auto_reply_me_too, Res.string.scene_auto_reply_always,
        Res.string.scene_auto_reply_silly, Res.string.scene_auto_reply_okay,
        Res.string.scene_auto_reply_mhm
    ))

    /** All of autonomy's randomness, so tests can seed it through [behaviorBrain]. */
    private val rng: Random get() = behaviorBrain.random

    private fun agentFor(c: PixelCharacter): AutonomyAgent = if (c === boy) boyAgent else girlAgent
    private fun homeFor(c: PixelCharacter): HomeSpot = if (c === boy) boyHome else girlHome

    /**
     * True while autonomy owns this character: mid-activity, or out and about away from
     * their home spot. Scene idle loops leave such a character's pose and place alone.
     */
    private fun autonomyHolds(c: PixelCharacter): Boolean {
        if (!autonomyEnabled || !autonomyHomeCaptured) return false
        val agent = agentFor(c)
        if (agent.phase != AgentPhase.IDLE) return true
        val home = homeFor(c)
        // Anything short of exactly home is "out": the walk home finishes the trip, so scene
        // idle loops never snap a character the last little bit (the loft's bed snap did).
        return kotlin.math.hypot(c.worldX - home.x, c.worldY - home.y) > BehaviorBrain.HOME_TOLERANCE
    }

    /** Scene idle loops may set this character's pose only when nobody else owns them. */
    private fun idleLoopMayPose(c: PixelCharacter): Boolean =
        !c.isMovingOrTransitioning && c.reactionTimer <= 0 && !autonomyHolds(c)

    /** The user touched the world: autonomy steps aside for a few seconds. */
    fun notifyUserInteraction() {
        autonomyUserPause = AUTONOMY_USER_PAUSE_SECONDS
        stopAutonomy(halt = true)
    }

    private fun stopAutonomy(halt: Boolean) {
        for (c in charactersBoyGirl) {
            val agent = agentFor(c)
            if (agent.phase == AgentPhase.WALKING && halt && c.isTransitioningPosition) {
                c.moveTo(c.worldX, c.worldY)
            }
            if (agent.phase != AgentPhase.IDLE) agent.rest(3f + rng.nextFloat() * 2f)
            agent.waitForPartner = 0f
        }
        discovery.claimed = false
    }

    private val charactersBoyGirl: Array<PixelCharacter> by lazy { arrayOf(boy, girl) }

    private fun resetAutonomy() {
        autonomyHomeCaptured = false
        autonomyUserPause = 0f
        mochiZoomTimer = 0f
        boyAgent.reset(firstDecisionIn = 1.5f + rng.nextFloat() * 1.5f)
        girlAgent.reset(firstDecisionIn = 4f + rng.nextFloat() * 3f)
        if (rareEventCooldown < RARE_EVENT_FIRST_DELAY) rareEventCooldown = RARE_EVENT_FIRST_DELAY
        resetDiscovery()
        requests.reset()
    }

    /** Fills [walkBounds] for the current scene; false where walking makes no sense (the scooter). */
    private fun fillWalkBounds(cw: Float, ch: Float): Boolean {
        val px = WorldViewport.pixelScale(cw)
        when (currentScene) {
            SceneType.FLOWER, SceneType.LOOKING -> setBounds(0.12f, 0.88f, 0.68f, 0.80f)
            SceneType.UNDER_TREE -> setBounds(0.14f, 0.86f, 0.69f, 0.80f)
            SceneType.WALK -> setBounds(0.12f, 0.88f, 0.68f, (0.66f + (30f * px / ch) + 0.08f).coerceAtMost(0.84f))
            SceneType.MOMO_STALL -> setBounds(0.12f, 0.88f, 0.69f, (0.66f + (30f * px / ch) + 0.10f).coerceAtMost(0.86f))
            SceneType.COOKING -> setBounds(0.16f, 0.84f, 0.69f, 0.79f)
            SceneType.SLEEP -> setBounds(0.18f, 0.82f, 0.68f, 0.77f)
            SceneType.COZY_LOFT -> setBounds(0.40f, 0.80f, 0.56f, 0.66f)
            SceneType.RAINY_CAFE -> setBounds(0.16f, 0.84f, 0.69f, 0.79f)
            SceneType.SUNROOM -> setBounds(0.14f, 0.86f, 0.67f, 0.80f)
            SceneType.CAMPFIRE -> setBounds(0.14f, 0.86f, 0.68f, 0.80f)
            SceneType.SEASIDE_PIER -> setBounds(PierLayout.WALK_MIN_X, PierLayout.WALK_MAX_X, PierLayout.WALK_MIN_Y, PierLayout.WALK_MAX_Y)
            SceneType.EVENING_RIDE -> return false
        }
        return true
    }

    private fun setBounds(minX: Float, maxX: Float, minY: Float, maxY: Float) {
        walkBounds[0] = minX; walkBounds[1] = maxX; walkBounds[2] = minY; walkBounds[3] = maxY
    }

    private fun isCharacterFree(c: PixelCharacter): Boolean =
        c.reactionTimer <= 0f && !c.isMovingOrTransitioning && groundInteractionCharacter !== c &&
            (if (c === boy) boyIdleWaveTimer else girlIdleWaveTimer) <= 0f

    private fun updateAutonomy(dt: Float, cw: Float, ch: Float, isScriptActive: Boolean) {
        boyAgent.memory.tick(dt)
        girlAgent.memory.tick(dt)
        if (rareEventCooldown > 0f) rareEventCooldown -= dt
        if (autonomyUserPause > 0f) autonomyUserPause -= dt
        lastCanvasW = cw
        lastCanvasH = ch
        updateMochiZoomies(dt)
        updateRequests(dt)

        val sleepingOnCouch = currentScene == SceneType.SLEEP && !lampLit
        if (isWatchSceneActive || isDreamMode || sleepingOnCouch || birthdaySurprise.isRunning || makeUpActive || phonesDownActive) {
            requests.interrupt(rng)
            stopAutonomy(halt = false)
            if (autonomyUserPause < AUTONOMY_AFTER_CINEMATIC_PAUSE) autonomyUserPause = AUTONOMY_AFTER_CINEMATIC_PAUSE
            return
        }
        updateDiscovery(dt, cw, ch)
        if (isScriptActive || autonomyUserPause > 0f || mochiMatchmakerActive ||
            livingRoomReturnTimer > 0f || catTreatInProgress
        ) return

        // Homes are where each scene's opening script leaves the couple.
        if (!autonomyHomeCaptured) {
            if (boy.isMovingOrTransitioning || girl.isMovingOrTransitioning || sceneTime < scriptEndTime() + 0.6f) return
            boyHome = HomeSpot(boy.worldX, boy.worldY, boy.pose, boy.direction)
            girlHome = HomeSpot(girl.worldX, girl.worldY, girl.pose, girl.direction)
            autonomyHomeCaptured = true
        }

        updateAgent(boy, boyAgent, girl, girlAgent, dt, cw, ch)
        updateAgent(girl, girlAgent, boy, boyAgent, dt, cw, ch)
    }

    /** When each scene's opening script ends (the same table that gates the old moments). */
    private fun scriptEndTime(): Float = when (currentScene) {
        SceneType.FLOWER -> 8.2f
        SceneType.UNDER_TREE -> 7.2f
        SceneType.COOKING -> 7.6f
        SceneType.SLEEP -> 6.9f
        SceneType.WALK -> 6.6f
        SceneType.LOOKING -> 7.6f
        SceneType.MOMO_STALL -> 6.2f
        SceneType.EVENING_RIDE -> 7.0f
        SceneType.COZY_LOFT -> 5.6f
        SceneType.RAINY_CAFE, SceneType.SUNROOM, SceneType.CAMPFIRE, SceneType.SEASIDE_PIER -> 1.5f
    }

    private fun updateAgent(
        c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, partnerAgent: AutonomyAgent,
        dt: Float, cw: Float, ch: Float
    ) {
        when (agent.phase) {
            AgentPhase.IDLE -> {
                if (agent.waitForPartner > 0f) {
                    // Someone's on their way over: stay put for them.
                    agent.waitForPartner -= dt
                    return
                }
                if (!isCharacterFree(c)) return
                agent.decisionTimer -= dt
                if (agent.decisionTimer > 0f) return
                decideAndStart(c, agent, partner, partnerAgent, cw, ch)
            }
            AgentPhase.WALKING -> {
                agent.walkTimer += dt
                if (c.isTransitioningPosition && agent.walkTimer < AUTONOMY_WALK_TIMEOUT) return
                if (agent.walkTimer >= AUTONOMY_WALK_TIMEOUT) {
                    c.moveTo(c.worldX, c.worldY)
                    agent.rest(3f)
                    return
                }
                arrive(c, agent, partner, partnerAgent, cw, ch)
            }
            AgentPhase.PERFORMING -> {
                agent.performTimer -= dt
                agent.stepTimer += dt
                if (!agent.isFollower) performStep(c, agent, partner, cw, ch)
                if (agent.performTimer <= 0f) finishActivity(c, agent)
            }
        }
    }

    private fun decideAndStart(
        c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, partnerAgent: AutonomyAgent,
        cw: Float, ch: Float
    ) {
        val canWalk = fillWalkBounds(cw, ch)
        val ctx = behaviorContext
        val home = homeFor(c)
        ctx.scene = currentScene
        ctx.isOutdoor = isCurrentSceneOutdoor
        ctx.weather = weather
        ctx.isNight = timeOfDayPhase.isNight
        ctx.isMorning = timeOfDayPhase == TimeOfDayPhase.MORNING
        ctx.isSunset = timeOfDayPhase.isSunset
        ctx.canWalk = canWalk
        ctx.prefersSeated = currentScene == SceneType.COZY_LOFT
        ctx.distanceToPartner = kotlin.math.hypot(partner.worldX - c.worldX, partner.worldY - c.worldY)
        ctx.partnerAvailable = isPartnerJoinable(partner, partnerAgent)
        val mochiHere = currentScene != SceneType.EVENING_RIDE
        ctx.mochiPresent = mochiHere
        ctx.distanceToMochi = kotlin.math.hypot(catWorldX - c.worldX, catWorldY - c.worldY)
        ctx.mochiWalking = catState == CatState.WALK_FOLLOW && abs(catTargetX - catWorldX) > 0.02f
        ctx.mochiAsleep = catState == CatState.SLEEPING
        ctx.distanceFromHome = kotlin.math.hypot(c.worldX - home.x, c.worldY - home.y)
        ctx.activitiesSinceHome = agent.activitiesSinceHome
        ctx.propAvailable = canWalk && pickSpot(c, agent, partner, partnerAgent, dryRun = true) != null
        ctx.sceneMomentAvailable = sceneHasMoments() &&
            ctx.distanceFromHome < 0.05f &&
            kotlin.math.hypot(partner.worldX - homeFor(partner).x, partner.worldY - homeFor(partner).y) < 0.05f
        ctx.discoveryAvailable = canWalk && discoveryNear(c) != null
        ctx.isSitting = c.pose == CharacterPose.SIT || c.pose == CharacterPose.SIT_SNUGGLE
        ctx.rareCooldown = rareEventCooldown
        ctx.requestAvailable = requestAvailableNow(partner)

        settleStrayPose(c, home)
        val choice = behaviorBrain.choose(ctx, agent.memory)
        agent.memory.remember(choice)
        startBehavior(choice, c, agent, partner, partnerAgent, cw, ch)
    }

    /** After walking over: mostly a chat, sometimes a hug or holding hands (whatever isn't cooling down). */
    private fun pickTogetherBehavior(agent: AutonomyAgent): Behavior {
        val roll = rng.nextFloat()
        val preferred = when {
            roll < 0.6f -> Behavior.TALK
            roll < 0.8f -> Behavior.HUG
            else -> Behavior.HOLD_HANDS
        }
        if (agent.memory.cooldownOf(preferred) <= 0f) return preferred
        return if (agent.memory.cooldownOf(Behavior.TALK) <= 0f) Behavior.TALK else Behavior.HOLD_HANDS
    }

    /**
     * The partner can be drawn into something together when they're free, or only doing
     * something casual on their own (looking around, resting, watching Mochi).
     */
    private fun isPartnerJoinable(partner: PixelCharacter, partnerAgent: AutonomyAgent): Boolean {
        if (partner.isMovingOrTransitioning || groundInteractionCharacter === partner) return false
        if (partnerAgent.phase == AgentPhase.IDLE) return isCharacterFree(partner)
        if (partnerAgent.phase != AgentPhase.PERFORMING || partnerAgent.isFollower) return false
        return when (partnerAgent.behavior) {
            Behavior.LOOK_AROUND, Behavior.LOOK_AT_SKY, Behavior.REST, Behavior.WATCH_MOCHI, Behavior.WEATHER_REACT -> true
            else -> false
        }
    }

    /** A pose left over from a tap or a moment goes back to the resting pose for where they are. */
    private fun settleStrayPose(c: PixelCharacter, home: HomeSpot) {
        val atHome = abs(c.worldX - home.x) < 0.03f && abs(c.worldY - home.y) < 0.03f
        val restPose = if (atHome) home.pose else CharacterPose.IDLE
        if (c.pose != restPose && c.pose != CharacterPose.IDLE_BLINK) c.transitionPoseTo(restPose)
    }

    private fun sceneHasMoments(): Boolean = when (currentScene) {
        SceneType.FLOWER, SceneType.UNDER_TREE, SceneType.COOKING, SceneType.SLEEP, SceneType.WALK,
        SceneType.LOOKING, SceneType.MOMO_STALL, SceneType.SEASIDE_PIER -> true
        else -> false
    }

    private fun startBehavior(
        b: Behavior, c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, partnerAgent: AutonomyAgent,
        cw: Float, ch: Float
    ) {
        agent.behavior = b
        agent.step = 0
        agent.stepTimer = 0f
        agent.isFollower = false
        when (b) {
            Behavior.WANDER -> {
                val minX = walkBounds[0]; val maxX = walkBounds[1]; val minY = walkBounds[2]; val maxY = walkBounds[3]
                var tx = c.worldX
                var ty = c.worldY
                for (attempt in 0 until 6) {
                    val x = minX + rng.nextFloat() * (maxX - minX)
                    val y = minY + rng.nextFloat() * (maxY - minY)
                    if (abs(x - c.worldX) < 0.08f) continue
                    if (kotlin.math.hypot(partner.worldX - x, partner.worldY - y) < 0.11f) continue
                    if (avoidCampfirePit(x, y) != x) continue
                    tx = x; ty = y
                    break
                }
                walkTo(c, agent, tx, ty)
            }
            Behavior.VISIT_PROP -> {
                val spot = pickSpot(c, agent, partner, partnerAgent, dryRun = false)
                if (spot == null) { agent.rest(2f); return }
                agent.spot = spot
                agent.lastSpotId = spot.id
                walkTo(c, agent, spot.x, spot.y)
            }
            Behavior.APPROACH_PARTNER, Behavior.SHELTER_CLOSE, Behavior.RARE_FLOWER_GIFT -> {
                val gap = if (b == Behavior.SHELTER_CLOSE) 0.10f else 0.12f
                val side = if (c.worldX < partner.worldX) -1f else 1f
                fillWalkBounds(cw, ch)
                val tx = (partner.worldX + side * gap).coerceIn(walkBounds[0], walkBounds[1])
                val ty = partner.worldY.coerceIn(walkBounds[2], walkBounds[3])
                if (b != Behavior.APPROACH_PARTNER) {
                    claimPartner(partner, partnerAgent, b, 30f)
                } else if (partnerAgent.phase == AgentPhase.IDLE) {
                    // They notice and wait while the other one walks over.
                    val walkSeconds = kotlin.math.hypot(tx - c.worldX, ty - c.worldY) / CharacterMotionTween.SHARED_WALKING_SPEED
                    partnerAgent.waitForPartner = walkSeconds + 1.5f
                }
                walkTo(c, agent, avoidCampfirePit(tx, ty), ty)
            }
            Behavior.APPROACH_MOCHI -> {
                val side = if (c.worldX < catWorldX) -1f else 1f
                fillWalkBounds(cw, ch)
                val tx = (catWorldX + side * 0.07f).coerceIn(walkBounds[0], walkBounds[1])
                val ty = catWorldY.coerceIn(walkBounds[2], walkBounds[3])
                if (kotlin.math.hypot(partner.worldX - tx, partner.worldY - ty) < 0.10f) { agent.rest(2f); return }
                walkTo(c, agent, avoidCampfirePit(tx, ty), ty)
            }
            Behavior.GO_HOME -> {
                val home = homeFor(c)
                walkTo(c, agent, home.x, home.y)
            }
            Behavior.DISCOVER -> walkToDiscovery(c, agent, cw, ch)
            Behavior.ASK_FOR_SOMETHING -> {
                val kind = if (requests.canStart) requests.pickKind(c === girl, rng) { requestFits(it) } else null
                if (kind == null) { agent.rest(2f); return }
                requests.start(kind, c === girl)
                beginPerforming(c, agent, performDuration(b), partner, cw, ch)
            }
            Behavior.SCENE_MOMENT -> {
                // The scene's own hand-made moments, played as one shared activity.
                triggerAutonomousMoment(cw, ch, sceneOnly = true)
                autonomyLog.addLast(Behavior.SCENE_MOMENT)
                if (autonomyLog.size > 64) autonomyLog.removeFirst()
                partnerAgent.memory.remember(Behavior.SCENE_MOMENT)
                agent.rest(3f + rng.nextFloat() * 3f)
                partnerAgent.rest(4f + rng.nextFloat() * 3f)
            }
            Behavior.TALK, Behavior.HUG, Behavior.HOLD_HANDS, Behavior.SIT_TOGETHER,
            Behavior.RARE_SHOOTING_STAR, Behavior.RARE_DOZE_OFF, Behavior.RARE_DANCE -> {
                val duration = performDuration(b)
                claimPartner(partner, partnerAgent, b, duration)
                beginPerforming(c, agent, duration, partner, cw, ch)
            }
            else -> beginPerforming(c, agent, performDuration(b), partner, cw, ch)
        }
    }

    /** The partner joins in: they hold still (or walk with) for [duration] seconds. */
    private fun claimPartner(partner: PixelCharacter, partnerAgent: AutonomyAgent, b: Behavior, duration: Float) {
        if (partnerAgent.phase == AgentPhase.PERFORMING && partnerAgent.behavior == Behavior.REST &&
            partner.pose == CharacterPose.SIT && homeFor(partner).pose != CharacterPose.SIT
        ) partner.transitionPoseTo(CharacterPose.IDLE)
        partnerAgent.phase = AgentPhase.PERFORMING
        partnerAgent.behavior = b
        partnerAgent.isFollower = true
        partnerAgent.performTimer = duration
        partnerAgent.memory.remember(b)
        partner.reactionTimer = duration
    }

    private fun releasePartner(c: PixelCharacter) {
        val partnerAgent = if (c === boy) girlAgent else boyAgent
        val partner = if (c === boy) girl else boy
        if (partnerAgent.isFollower) {
            partnerAgent.rest(2.5f + rng.nextFloat() * 3f)
            partner.reactionTimer = 0f
        }
    }

    private fun performDuration(b: Behavior): Float = when (b) {
        Behavior.LOOK_AROUND -> 3.0f
        Behavior.LOOK_AT_SKY -> 3.4f
        Behavior.STRETCH -> 2.4f
        Behavior.REST -> 6f + rng.nextFloat() * 3f
        Behavior.WANDER -> 1.6f
        Behavior.APPROACH_PARTNER -> 2.2f
        Behavior.TALK -> 4.4f
        Behavior.HUG -> 3.2f
        Behavior.HOLD_HANDS -> 4.5f
        Behavior.SIT_TOGETHER -> 7f
        Behavior.APPROACH_MOCHI, Behavior.PET_MOCHI -> 2.8f
        Behavior.WATCH_MOCHI -> 3.0f
        Behavior.WEATHER_REACT -> 2.8f
        Behavior.SHELTER_CLOSE -> 2.4f
        Behavior.GO_HOME -> 0.3f
        Behavior.DISCOVER -> 3.2f
        Behavior.RARE_FLOWER_GIFT -> 3.4f
        Behavior.RARE_SHOOTING_STAR -> 3.6f
        Behavior.RARE_DOZE_OFF -> 9f
        Behavior.RARE_DANCE -> 3.6f
        Behavior.RARE_MOCHI_ZOOMIES -> 3.2f
        Behavior.VISIT_PROP, Behavior.SCENE_MOMENT -> 3.2f
        Behavior.ASK_FOR_SOMETHING -> 2.4f
    }

    private fun walkTo(c: PixelCharacter, agent: AutonomyAgent, x: Float, y: Float) {
        agent.targetX = x
        agent.targetY = y
        agent.walkTimer = 0f
        if (kotlin.math.hypot(x - c.worldX, y - c.worldY) < 0.012f) {
            agent.phase = AgentPhase.WALKING // arrives on the next frame
            return
        }
        agent.phase = AgentPhase.WALKING
        c.emotion = CharacterEmotion.CURIOUS
        c.moveTo(x, y, arrivePose = CharacterPose.IDLE)
    }

    private fun arrive(c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, partnerAgent: AutonomyAgent, cw: Float, ch: Float) {
        when (agent.behavior) {
            Behavior.GO_HOME -> {
                val home = homeFor(c)
                c.transitionPoseTo(home.pose)
                c.direction = home.direction
                agent.activitiesSinceHome = -1 // finishing the walk home counts as zero
            }
            Behavior.VISIT_PROP -> {
                val spot = agent.spot
                if (spot == null) { agent.rest(2f); return }
                c.direction = if (spot.faceLeft) Direction.LEFT else Direction.RIGHT
                if (spot.pose != CharacterPose.IDLE) c.transitionPoseTo(spot.pose)
                playSpotAction(c, spot, cw, ch)
                beginPerforming(c, agent, spot.dwellSeconds, partner, cw, ch)
                return
            }
            Behavior.APPROACH_PARTNER, Behavior.SHELTER_CLOSE, Behavior.RARE_FLOWER_GIFT -> {
                faceEachOther(c, partner)
                if (agent.behavior == Behavior.APPROACH_PARTNER) {
                    if (isPartnerJoinable(partner, partnerAgent)) {
                        // Walked over to them: now talk, hug or take their hand.
                        val together = pickTogetherBehavior(agent)
                        agent.behavior = together
                        agent.memory.remember(together)
                        val duration = performDuration(together)
                        claimPartner(partner, partnerAgent, together, duration)
                        beginPerforming(c, agent, duration, partner, cw, ch)
                        return
                    }
                    // They got busy meanwhile; just smile at them.
                    c.emote = EmoteType.HEART
                    c.emoteTimer = 1.6f
                }
            }
            Behavior.APPROACH_MOCHI -> {
                // Walking over to Mochi turns into a pet once they're close.
                agent.behavior = Behavior.PET_MOCHI
            }
            else -> Unit
        }
        beginPerforming(c, agent, performDuration(agent.behavior ?: Behavior.LOOK_AROUND), partner, cw, ch)
    }

    private fun beginPerforming(c: PixelCharacter, agent: AutonomyAgent, duration: Float, partner: PixelCharacter, cw: Float, ch: Float) {
        agent.behavior?.let {
            autonomyLog.addLast(it)
            if (autonomyLog.size > 64) autonomyLog.removeFirst()
        }
        agent.phase = AgentPhase.PERFORMING
        agent.performTimer = duration
        agent.stepTimer = 0f
        agent.step = 0
        c.reactionTimer = duration
        startPerformance(c, agent, partner, cw, ch)
    }

    /** The first beat of a performance. */
    private fun startPerformance(c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, cw: Float, ch: Float) {
        val headX = cw * c.worldX
        val headY = ch * c.worldY - 70f
        when (agent.behavior) {
            Behavior.LOOK_AROUND -> {
                c.direction = if (rng.nextBoolean()) Direction.LEFT else Direction.RIGHT
                c.emotion = CharacterEmotion.CURIOUS
                if (rng.nextFloat() < 0.25f) emote(c, EmoteType.DOTS, 1.4f)
            }
            Behavior.LOOK_AT_SKY -> {
                c.emotion = CharacterEmotion.HAPPY
                emote(c, EmoteType.SPARKLE, 1.8f)
                if (timeOfDayPhase.isNight && isCurrentSceneOutdoor && rng.nextFloat() < 0.25f) {
                    particles.spawnShootingStar(cw * (0.1f + rng.nextFloat() * 0.5f), ch * 0.10f)
                }
            }
            Behavior.STRETCH -> {
                c.transitionPoseTo(CharacterPose.WAVE)
                c.emotion = CharacterEmotion.SLEEPY
                emote(c, EmoteType.SLEEP_Z, 1.4f)
            }
            Behavior.REST -> {
                if (currentScene != SceneType.EVENING_RIDE) c.transitionPoseTo(CharacterPose.SIT)
                c.emotion = if (timeOfDayPhase.isNight) CharacterEmotion.SLEEPY else CharacterEmotion.HAPPY
                if (timeOfDayPhase.isNight) emote(c, EmoteType.SLEEP_Z, 2f)
            }
            Behavior.WANDER -> {
                c.emotion = CharacterEmotion.HAPPY
                if (rng.nextFloat() < 0.4f) c.direction = if (c.direction == Direction.LEFT) Direction.RIGHT else Direction.LEFT
            }
            Behavior.APPROACH_PARTNER -> {
                faceEachOther(c, partner)
                c.transitionPoseTo(CharacterPose.WAVE)
                c.emotion = CharacterEmotion.LOVING
                emote(c, EmoteType.HEART, 1.8f)
            }
            Behavior.TALK -> {
                faceEachOther(c, partner)
                c.emotion = CharacterEmotion.HAPPY
                partner.emotion = CharacterEmotion.HAPPY
                speakerSpeech(c, GameText.get(talkLine()), 2.6f)
            }
            Behavior.HUG -> {
                faceEachOther(c, partner)
                c.transitionPoseTo(CharacterPose.HUG)
                partner.transitionPoseTo(CharacterPose.HUG)
                c.emotion = CharacterEmotion.LOVING
                partner.emotion = CharacterEmotion.LOVING
                particles.spawnHeart(cw * (c.worldX + partner.worldX) / 2f, ch * c.worldY - 80f)
                audio.playHeartChime()
            }
            Behavior.HOLD_HANDS -> {
                faceEachOther(c, partner)
                c.transitionPoseTo(CharacterPose.HOLD_HANDS)
                partner.transitionPoseTo(CharacterPose.HOLD_HANDS)
                c.emotion = CharacterEmotion.LOVING
                partner.emotion = CharacterEmotion.SHY
                emote(partner, EmoteType.BLUSH, 2f)
            }
            Behavior.SIT_TOGETHER -> {
                faceEachOther(c, partner)
                c.transitionPoseTo(CharacterPose.SIT)
                partner.transitionPoseTo(CharacterPose.SIT_SNUGGLE)
                c.emotion = CharacterEmotion.LOVING
                partner.emotion = CharacterEmotion.LOVING
            }
            Behavior.PET_MOCHI, Behavior.APPROACH_MOCHI -> {
                c.direction = if (catWorldX < c.worldX) Direction.LEFT else Direction.RIGHT
                if (abs(catWorldX - c.worldX) < 0.16f) {
                    c.transitionPoseTo(CharacterPose.HEAD_PAT)
                    if (catState != CatState.WALK_FOLLOW) {
                        catState = CatState.SITTING_PURR
                        catSleeping = false
                    }
                    audio.playCatPurr()
                    particles.spawnHeart(cw * catWorldX, ch * catWorldY - 40f, Color(0xFFFF8FA3))
                    if (rng.nextFloat() < 0.3f) speakerSpeech(c, GameText.get(Res.string.scene_auto_pet_mochi_soft), 2.2f)
                } else {
                    emote(c, EmoteType.HEART, 1.6f)
                }
                c.emotion = CharacterEmotion.LOVING
            }
            Behavior.WATCH_MOCHI -> {
                c.direction = if (catWorldX < c.worldX) Direction.LEFT else Direction.RIGHT
                c.emotion = CharacterEmotion.CURIOUS
                emote(c, if (rng.nextBoolean()) EmoteType.QUESTION else EmoteType.HEART, 1.6f)
            }
            Behavior.WEATHER_REACT -> playWeatherReaction(c, cw, ch)
            Behavior.SHELTER_CLOSE -> {
                faceEachOther(c, partner)
                c.emotion = CharacterEmotion.LOVING
                partner.emotion = CharacterEmotion.LOVING
                emote(c, EmoteType.HEART, 1.6f)
                if (rng.nextFloat() < 0.5f) speakerSpeech(c, GameText.get(Res.string.scene_auto_rain_stay_close), 2.4f)
            }
            Behavior.RARE_FLOWER_GIFT -> {
                faceEachOther(c, partner)
                c.transitionPoseTo(CharacterPose.GIVE_FLOWER)
                partner.transitionPoseTo(CharacterPose.RECEIVE_FLOWER)
                partner.emotion = CharacterEmotion.SHY
                emote(partner, EmoteType.BLUSH, 2.4f)
                particles.spawnPetals(cw * partner.worldX, ch * partner.worldY - 60f, 4)
                audio.playHeartChime()
                showMessage(GameText.get(Res.string.scene_auto_rare_flower_gift, c.name, partner.name), duration = 3.2f)
                markRareEvent()
            }
            Behavior.RARE_SHOOTING_STAR -> {
                for (k in charactersBoyGirl) {
                    k.emotion = CharacterEmotion.SURPRISED
                    emote(k, EmoteType.SPARKLE, 2.4f)
                }
                particles.spawnShootingStar(cw * 0.15f, ch * 0.08f)
                particles.spawnShootingStar(cw * 0.35f, ch * 0.05f)
                audio.playStarArpeggio()
                showMessage(GameText.get(Res.string.scene_auto_rare_shooting_star), duration = 3.2f)
                markRareEvent()
            }
            Behavior.RARE_DOZE_OFF -> {
                faceEachOther(c, partner)
                c.emotion = CharacterEmotion.SLEEPY
                emote(c, EmoteType.SLEEP_Z, 4f)
                if (c.pose != CharacterPose.SIT_SNUGGLE) c.transitionPoseTo(CharacterPose.SIT_SNUGGLE)
                partner.emotion = CharacterEmotion.LOVING
                showMessage(GameText.get(Res.string.scene_auto_rare_doze_off, c.name, partner.name), duration = 3.4f)
                markRareEvent()
            }
            Behavior.RARE_DANCE -> {
                faceEachOther(c, partner)
                audio.playHeartChime()
                showMessage(GameText.get(Res.string.scene_auto_rare_dance), duration = 2.8f)
                markRareEvent()
            }
            Behavior.RARE_MOCHI_ZOOMIES -> {
                startMochiZoomies(cw)
                c.emotion = CharacterEmotion.SURPRISED
                emote(c, EmoteType.EXCLAMATION, 1.8f)
                showMessage(GameText.get(Res.string.scene_auto_rare_mochi_zoomies), duration = 2.8f)
                markRareEvent()
            }
            Behavior.DISCOVER -> startDiscovery(c, cw, ch)
            Behavior.ASK_FOR_SOMETHING -> announceRequest(c, requests.kind)
            else -> Unit
        }
        if (agent.behavior != Behavior.LOOK_AT_SKY && agent.behavior != Behavior.WEATHER_REACT) {
            // Tiny touch of life: a sparkle above the head now and then
            if (rng.nextFloat() < 0.08f) particles.spawnSparkles(headX, headY, 2)
        }
    }

    /** Later beats of multi-step performances. */
    private fun performStep(c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, cw: Float, ch: Float) {
        when (agent.behavior) {
            Behavior.LOOK_AROUND -> {
                if (agent.step == 0 && agent.stepTimer > 1.0f || agent.step == 1 && agent.stepTimer > 2.0f) {
                    c.direction = if (c.direction == Direction.LEFT) Direction.RIGHT else Direction.LEFT
                    agent.step++
                }
            }
            Behavior.STRETCH, Behavior.APPROACH_PARTNER -> {
                if (agent.step == 0 && agent.stepTimer > 1.5f) {
                    c.transitionPoseTo(CharacterPose.IDLE)
                    agent.step++
                }
            }
            Behavior.TALK -> {
                if (agent.step == 0 && agent.stepTimer > 2.2f) {
                    speakerSpeech(partner, GameText.get(replyLinePicker.pick()), 1.8f)
                    emote(partner, if (rng.nextBoolean()) EmoteType.HEART else EmoteType.BLUSH, 1.6f)
                    agent.step++
                }
            }
            Behavior.WATCH_MOCHI -> {
                // Keep following Mochi with their eyes.
                c.direction = if (catWorldX < c.worldX) Direction.LEFT else Direction.RIGHT
            }
            Behavior.RARE_DANCE -> {
                val beat = (agent.stepTimer * 3f).toInt()
                if (beat != agent.step) {
                    agent.step = beat
                    val up = beat % 2 == 0
                    c.pose = if (up) CharacterPose.JOY_JUMP else CharacterPose.IDLE
                    partner.pose = if (up) CharacterPose.IDLE else CharacterPose.JOY_JUMP
                    if (up) particles.spawnMusicNote(cw * c.worldX, ch * c.worldY - 80f)
                }
            }
            Behavior.HUG -> {
                if (eventChance(1.2f, 1f / 60f) && agent.step < 3) {
                    particles.spawnHeart(cw * (c.worldX + partner.worldX) / 2f, ch * c.worldY - 80f)
                    agent.step++
                }
            }
            else -> Unit
        }
    }

    private fun finishActivity(c: PixelCharacter, agent: AutonomyAgent) {
        val b = agent.behavior
        if (agent.isFollower) {
            agent.rest(2.5f + rng.nextFloat() * 3f)
            return
        }
        c.reactionTimer = 0f
        when (b) {
            // Poses that shouldn't linger once the moment is over.
            Behavior.HUG, Behavior.HOLD_HANDS, Behavior.RARE_FLOWER_GIFT, Behavior.RARE_DANCE,
            Behavior.PET_MOCHI, Behavior.APPROACH_MOCHI, Behavior.STRETCH, Behavior.APPROACH_PARTNER -> {
                c.transitionPoseTo(CharacterPose.IDLE)
                val partner = if (c === boy) girl else boy
                if (b == Behavior.HUG || b == Behavior.HOLD_HANDS || b == Behavior.RARE_FLOWER_GIFT || b == Behavior.RARE_DANCE) {
                    partner.transitionPoseTo(CharacterPose.IDLE)
                }
            }
            Behavior.VISIT_PROP -> {
                if (agent.spot?.pose != CharacterPose.IDLE) c.transitionPoseTo(CharacterPose.IDLE)
                if (agent.spot?.action == SpotAction.PICK_FLOWER) c.hold(HeldItem.FLOWER, CARRIED_ITEM_SECONDS)
            }
            Behavior.REST, Behavior.SIT_TOGETHER, Behavior.RARE_DOZE_OFF -> {
                // Stand up again unless this is where they sit at home.
                val home = homeFor(c)
                val atHome = abs(c.worldX - home.x) < 0.03f
                if (!atHome || (home.pose != CharacterPose.SIT && home.pose != CharacterPose.SIT_SNUGGLE)) {
                    c.transitionPoseTo(CharacterPose.IDLE)
                }
                if (b != Behavior.REST) {
                    val partner = if (c === boy) girl else boy
                    val partnerHome = homeFor(partner)
                    if (abs(partner.worldX - partnerHome.x) > 0.03f ||
                        (partnerHome.pose != CharacterPose.SIT && partnerHome.pose != CharacterPose.SIT_SNUGGLE)
                    ) partner.transitionPoseTo(CharacterPose.IDLE)
                }
            }
            else -> Unit
        }
        releasePartner(c)
        agent.activitiesSinceHome++
        agent.rest(AUTONOMY_REST_MIN + rng.nextFloat() * AUTONOMY_REST_RANGE)
    }

    /** A free spot this character hasn't just used and nobody else is standing at. */
    private fun pickSpot(c: PixelCharacter, agent: AutonomyAgent, partner: PixelCharacter, partnerAgent: AutonomyAgent, dryRun: Boolean): SceneSpot? {
        val spots = SceneSpots.forScene(currentScene)
        if (spots.isEmpty()) return null
        val preferWindows = weather == WeatherType.RAIN && !isCurrentSceneOutdoor
        var chosen: SceneSpot? = null
        var bestScore = -1f
        for (spot in spots) {
            if (spot.id == agent.lastSpotId) continue
            if (partnerAgent.spot?.id == spot.id) continue
            if (kotlin.math.hypot(partner.worldX - spot.x, partner.worldY - spot.y) < 0.10f) continue
            if (dryRun) return spot
            var score = rng.nextFloat()
            if (preferWindows && spot.isWindow) score += 1f
            if (score > bestScore) { bestScore = score; chosen = spot }
        }
        return chosen
    }

    /** What happens at each interaction spot: the prop's own little animation, a sound, a feeling. */
    private fun playSpotAction(c: PixelCharacter, spot: SceneSpot, cw: Float, ch: Float) {
        val x = cw * spot.x
        val y = ch * spot.y
        c.emotion = CharacterEmotion.HAPPY
        when (spot.action) {
            SpotAction.SMELL_FLOWERS -> { particles.spawnPetals(x, y - 30f, 3); emote(c, EmoteType.HEART, 1.8f); audio.playStarTwinkle() }
            SpotAction.PICK_FLOWER -> { flowerWiggleTimer = 1.2f; particles.spawnPetals(x, y - 20f, 2); emote(c, EmoteType.SPARKLE, 1.8f) }
            SpotAction.LISTEN_CHIMES -> { windChimeSwayTimer = 2.5f; audio.playWindChime(); emote(c, EmoteType.MUSIC_NOTE, 2f) }
            SpotAction.LOOK_UP_TREE -> { if (weather == WeatherType.AUTUMN) c.hold(HeldItem.LEAF, CARRIED_ITEM_SECONDS, useSeconds = 1.6f); particles.spawnPetals(x, ch * 0.40f, 3); emote(c, EmoteType.SPARKLE, 1.8f); c.emotion = CharacterEmotion.LOVING }
            SpotAction.SIT_GRASS -> { c.emotion = CharacterEmotion.HAPPY; emote(c, EmoteType.HEART, 1.4f) }
            SpotAction.STIR_POT -> { repeat(3) { particles.spawnSteam(x + 20f, y - 90f) }; audio.playCookingBubbles() }
            SpotAction.RINSE_DISHES -> { particles.spawnSparkles(x - 20f, y - 70f, 4, Color(0xFFBFE6FF)); audio.playWaterDrip() }
            SpotAction.PEEK_OVEN -> { cabinetOpenTimer = 2.4f; emote(c, EmoteType.QUESTION, 1.6f); audio.playWoodKnock() }
            SpotAction.SIT_TABLE -> { c.emotion = CharacterEmotion.HAPPY }
            SpotAction.WATER_PLANT -> { c.hold(HeldItem.WATERING_CAN, spot.dwellSeconds + 0.6f, useSeconds = spot.dwellSeconds - 0.4f); plantWaterTimer = 2.4f; sunroomMistTimer = 1.6f; particles.spawnSparkles(x - 30f, y - 60f, 4, Color(0xFFBFE6FF)); audio.playWaterDrip() }
            SpotAction.PEEK_BOX -> { cardboardBoxTimer = 2.4f; emote(c, EmoteType.QUESTION, 1.6f) }
            SpotAction.SIT_POUF -> { poufBounceTimer = 0.8f; audio.playBubblePop() }
            SpotAction.LIGHT_CANDLE -> { tableCandleTimer = 3f; audio.playCandleFlicker(); emote(c, EmoteType.SPARKLE, 1.6f) }
            SpotAction.USE_TELESCOPE -> { telescopeStarTimer = 3f; particles.spawnSparkles(cw * 0.5f, ch * 0.12f, 6); emote(c, EmoteType.SPARKLE, 2f); audio.playStarTwinkle() }
            SpotAction.ADMIRE_LANTERN -> { pagodaGlowTimer = 2.4f; emote(c, EmoteType.HEART, 1.6f) }
            SpotAction.SMELL_LAVENDER -> { lavenderSwayTimer = 2.4f; particles.spawnSparkles(x, y - 30f, 3, Color(0xFFC9B6FF)); emote(c, EmoteType.HEART, 1.6f) }
            SpotAction.POKE_MUSHROOMS -> { mushroomBounceTimer = 1.6f; emote(c, EmoteType.SPARKLE, 1.4f); audio.playBubblePop() }
            SpotAction.SNIFF_STEAMER -> { momoSteamerTimer = 2.4f; repeat(2) { particles.spawnSteam(x, y - 120f) }; emote(c, EmoteType.HEART, 1.6f) }
            SpotAction.READ_CHALKBOARD -> { chalkboardTimer = 2.4f; emote(c, EmoteType.DOTS, 1.6f); audio.playPaperFlip() }
            SpotAction.CHECK_CRATE -> { bambooCrateTimer = 1.6f; audio.playWoodKnock() }
            SpotAction.FILL_SAUCER -> { milkSaucerTimer = 2.4f; emote(c, EmoteType.HEART, 1.6f) }
            SpotAction.LOOK_WINDOW -> { loftWindowTimer = 3f; emote(c, EmoteType.SPARKLE, 1.8f); c.emotion = CharacterEmotion.LOVING }
            SpotAction.BROWSE_BOOKS -> { c.hold(HeldItem.BOOK, CARRIED_ITEM_SECONDS, useSeconds = spot.dwellSeconds); loftBookNookTimer = 3f; emote(c, EmoteType.DOTS, 1.6f); audio.playPaperFlip() }
            SpotAction.ADMIRE_FAIRY_LIGHTS -> { loftFairyLightsTimer = 3f; emote(c, EmoteType.SPARKLE, 1.6f) }
            SpotAction.FOG_WINDOW -> {
                cafeWindowHeartTimer = 2.8f
                cafeWindowHeartX = (spot.x + if (spot.faceLeft) -0.06f else 0.06f).coerceIn(0.40f, 0.90f)
                cafeWindowHeartY = 0.34f
                audio.playWaterDrip()
                emote(c, EmoteType.HEART, 1.6f)
            }
            SpotAction.PET_PUP -> { cafePupPetTimer = 2.4f; audio.playBubblePop(); particles.spawnHeart(x + 40f, y - 30f, Color(0xFFFFCAD4)) }
            SpotAction.ORDER_COFFEE -> { c.hold(HeldItem.MUG, CARRIED_ITEM_SECONDS); cafeBaristaBrewTimer = 2.5f; audio.playSteamHiss(); emote(c, EmoteType.HEART, 1.6f) }
            SpotAction.MIST_PLANTS -> { c.hold(HeldItem.MISTER, spot.dwellSeconds + 0.6f, useSeconds = spot.dwellSeconds - 0.4f); sunroomMistTimer = 2f; particles.spawnSparkles(x + 30f, y - 70f, 4, Color(0xFFBFE6FF)); audio.playWaterDrip() }
            SpotAction.LOOK_SKYLIGHT -> { sunroomSkylightTimer = 2f; emote(c, EmoteType.SPARKLE, 1.6f) }
            SpotAction.WARM_HANDS -> { campfireEmbersTimer = 2.2f; c.emotion = CharacterEmotion.LOVING; audio.playCandleFlicker() }
            SpotAction.STRUM_GUITAR -> { campGuitarStrumTimer = 3f; audio.playStarArpeggio(); emote(c, EmoteType.MUSIC_NOTE, 2.4f) }
            SpotAction.TEND_LANTERN -> { c.hold(HeldItem.LANTERN, spot.dwellSeconds + 0.6f, useSeconds = spot.dwellSeconds - 0.4f); emote(c, EmoteType.SPARKLE, 1.6f); audio.playWoodKnock() }
            SpotAction.SPOT_DOLPHINS -> {
                if (pierDolphinTimer <= 0f) pierDolphinTimer = PIER_DOLPHIN_SECONDS
                c.emotion = CharacterEmotion.SURPRISED
                emote(c, EmoteType.EXCLAMATION, 2f)
                audio.playBubblePop()
            }
            SpotAction.LOOK_SEA -> { c.emotion = CharacterEmotion.LOVING; emote(c, EmoteType.HEART, 1.6f) }
            SpotAction.PEEK_BUCKET -> { pierBucketFlopTimer = 1.6f; audio.playWaterDrip(); emote(c, EmoteType.QUESTION, 1.4f) }
        }
    }

    private fun playWeatherReaction(c: PixelCharacter, cw: Float, ch: Float) {
        val x = cw * c.worldX
        val y = ch * c.worldY
        when (weather) {
            WeatherType.SNOW -> {
                c.emotion = CharacterEmotion.HAPPY
                emote(c, EmoteType.SPARKLE, 2f)
                particles.spawnSparkles(x, y - 90f, 4, Color(0xFFF2FAFF))
                audio.playStarTwinkle()
            }
            WeatherType.SAKURA -> {
                c.transitionPoseTo(CharacterPose.RECEIVE_FLOWER)
                particles.spawnPetals(x, y - 90f, 3)
                emote(c, EmoteType.HEART, 1.8f)
            }
            WeatherType.AUTUMN -> {
                c.transitionPoseTo(CharacterPose.JOY_JUMP)
                c.bounceOffset = 4f
                particles.kickGroundParticles(x, y, cw, ch, cw * 0.06f, c.direction == Direction.LEFT)
                particles.spawnLeaf(x, y - 20f)
                audio.playLeafRustle()
            }
            WeatherType.RAIN -> {
                c.emotion = CharacterEmotion.CURIOUS
                emote(c, if (rng.nextBoolean()) EmoteType.DOTS else EmoteType.SWEAT, 1.6f)
                particles.spawnRainSplash(x, y)
            }
            WeatherType.SUNNY -> {
                c.emotion = CharacterEmotion.HAPPY
                particles.spawnSunSparkle(x, y - 100f)
                emote(c, EmoteType.SPARKLE, 1.6f)
            }
        }
    }

    private fun faceEachOther(a: PixelCharacter, b: PixelCharacter) {
        a.direction = if (b.worldX < a.worldX) Direction.LEFT else Direction.RIGHT
        b.direction = if (a.worldX < b.worldX) Direction.LEFT else Direction.RIGHT
    }

    private fun heldItemFor(kind: DiscoveryKind): HeldItem = when (kind) {
        DiscoveryKind.WILDFLOWER -> HeldItem.FLOWER
        DiscoveryKind.RED_LEAF -> HeldItem.LEAF
        DiscoveryKind.LOVE_NOTE -> HeldItem.LOVE_NOTE
        DiscoveryKind.MOCHI_TOY -> HeldItem.YARN_BALL
        DiscoveryKind.SEASHELL -> HeldItem.SEASHELL
        DiscoveryKind.STAR_PEBBLE -> HeldItem.STAR_PEBBLE
    }

    /** Ages the item in [c]'s hand, puts it away when its time is up, and now and then uses it. */
    private fun tickHeldItem(c: PixelCharacter, dt: Float) {
        if (c.heldItem == HeldItem.NONE) return
        c.heldItemAge += dt
        if (c.heldItemUse > 0f) c.heldItemUse -= dt
        c.heldItemTimeLeft -= dt
        if (c.heldItemTimeLeft <= 0f && c.heldItemUse <= 0f) {
            c.putAwayHeldItem()
            return
        }
        // A sip of coffee or a sniff of the flower while they stand about.
        val sipping = c.heldItem == HeldItem.MUG || c.heldItem == HeldItem.FLOWER
        if (sipping && c.heldItemUse <= 0f && !c.isMovingOrTransitioning && rng.nextFloat() < dt / 7f) {
            c.heldItemUse = 1.4f
        }
    }

    /** A found flower changes hands a moment after it's found. */
    private var flowerHandoverFrom: PixelCharacter? = null
    private var flowerHandoverTimer = 0f

    private fun tickFlowerHandover(dt: Float) {
        val from = flowerHandoverFrom ?: return
        flowerHandoverTimer -= dt
        if (flowerHandoverTimer > 0f) return
        flowerHandoverFrom = null
        if (from.heldItem != HeldItem.FLOWER) return
        val to = if (from === boy) girl else boy
        from.putAwayHeldItem()
        to.hold(HeldItem.FLOWER, CARRIED_ITEM_SECONDS, useSeconds = 1.6f)
        to.emotion = CharacterEmotion.LOVING
        emote(to, EmoteType.HEART, 2f)
        audio.playHeartChime()
    }

    /**
     * A tap on someone holding something has them use it: a sip, a page, a sniff. Returns false
     * when their hands are empty, so the usual tap reaction plays instead.
     */
    private fun useHeldItem(c: PixelCharacter, cw: Float, ch: Float): Boolean {
        val item = c.heldItem
        if (item == HeldItem.NONE) return false
        c.heldItemUse = 1.8f
        c.heldItemTimeLeft = maxOf(c.heldItemTimeLeft, 8f)
        c.reactionTimer = 1.8f
        if (c.pose != CharacterPose.SIT && c.pose != CharacterPose.SIT_SNUGGLE) c.pose = CharacterPose.IDLE
        c.emotion = CharacterEmotion.HAPPY
        val x = cw * c.worldX
        val y = ch * c.worldY
        when (item) {
            HeldItem.MUG -> { emote(c, EmoteType.HEART, 1.6f); audio.playBubblePop() }
            HeldItem.BOOK -> { emote(c, EmoteType.DOTS, 1.6f); audio.playPaperFlip() }
            HeldItem.FLOWER -> { emote(c, EmoteType.HEART, 1.6f); particles.spawnPetals(x, y - 60f, 2) }
            HeldItem.LEAF -> { emote(c, EmoteType.SPARKLE, 1.4f); particles.spawnLeaf(x, y - 60f) }
            HeldItem.LOVE_NOTE -> { emote(c, EmoteType.BLUSH, 1.8f); audio.playPaperFlip() }
            HeldItem.SEASHELL -> { emote(c, EmoteType.MUSIC_NOTE, 1.6f); audio.playWaterDrip() }
            HeldItem.STAR_PEBBLE -> { emote(c, EmoteType.SPARKLE, 1.6f); audio.playStarTwinkle(); particles.spawnSparkles(x, y - 60f, 3, Color(0xFFFFF3B0)) }
            HeldItem.YARN_BALL -> { emote(c, EmoteType.SPARKLE, 1.4f); audio.playBubblePop() }
            HeldItem.WATERING_CAN, HeldItem.MISTER -> { audio.playWaterDrip(); particles.spawnSparkles(x + 30f, y - 50f, 3, Color(0xFFBFE6FF)) }
            HeldItem.LANTERN -> { emote(c, EmoteType.SPARKLE, 1.4f); audio.playCandleFlicker() }
            HeldItem.CAKE -> { emote(c, EmoteType.HEART, 1.6f); audio.playHeartChime() }
            HeldItem.NONE -> Unit
        }
        return true
    }

    private fun emote(c: PixelCharacter, e: EmoteType, seconds: Float) {
        c.emote = e
        c.emoteTimer = seconds
    }

    private fun talkLine(): StringResource = when {
        isCurrentSceneOutdoor && weather == WeatherType.RAIN && rng.nextFloat() < 0.5f -> Res.string.scene_auto_rain_stay_close
        isCurrentSceneOutdoor && weather == WeatherType.SNOW && rng.nextFloat() < 0.4f -> Res.string.scene_auto_talk_snow_nose
        isCurrentSceneOutdoor && weather == WeatherType.SAKURA && rng.nextFloat() < 0.4f -> Res.string.scene_auto_talk_petal_hair
        timeOfDayPhase.isNight && isCurrentSceneOutdoor && rng.nextFloat() < 0.4f -> Res.string.scene_auto_talk_stars_closer
        else -> talkLinePicker.pick()
    }

    private fun markRareEvent() {
        rareEventCooldown = RARE_EVENT_GAP + rng.nextFloat() * 60f
    }

    private fun startMochiZoomies(cw: Float) {
        if (currentScene == SceneType.EVENING_RIDE || currentScene == SceneType.COZY_LOFT) return
        val farSide = if (catWorldX < 0.5f) 0.80f else 0.20f
        catTargetX = avoidCampfirePit(farSide, catWorldY)
        catTargetY = catWorldY
        catFacingLeft = catTargetX < catWorldX
        catState = CatState.WALK_FOLLOW
        catSleeping = false
        catRoamSpeed = MOCHI_ZOOM_SPEED
        mochiZoomTimer = 3f
        audio.playCatPurr()
    }

    private fun updateMochiZoomies(dt: Float) {
        if (mochiZoomTimer <= 0f) return
        mochiZoomTimer -= dt
        if (mochiZoomTimer <= 0f) catRoamSpeed = MOCHI_ROAM_SPEED
    }

    // ── Couple life (plan 09, C) ──────────────────────────────────────────
    // The Dinner Decider's pick, the Thank-You Jar, the Make-Up Bench and Phones Down play out
    // in the world here; their screens live in CoupleLifeDialogs.

    /** Section C's saved data; set by the app once it has storage (like [birthdayStore]). */
    var coupleLifeStore: com.example.data.CoupleLifeStore? by mutableStateOf(null)

    /** The Decider landed on [pick]: they cheer and one of them says so. */
    fun reactToDinnerPick(pick: String) {
        val cw = lastWorldW
        val ch = lastWorldH
        for (c in charactersBoyGirl) {
            c.reactionTimer = 2.4f
            c.emotion = CharacterEmotion.HAPPY
            if (c.pose != CharacterPose.SIT && c.pose != CharacterPose.SIT_SNUGGLE) c.pose = CharacterPose.JOY_JUMP
            emote(c, EmoteType.SPARKLE, 1.8f)
        }
        speakerSpeech(if (rng.nextBoolean()) boy else girl, GameText.get(Res.string.decider_world_line, pick), 2.8f)
        particles.spawnSparkles(cw * (boy.worldX + girl.worldX) / 2f, ch * boy.worldY - 100f, 6)
        audio.playStarTwinkle()
    }

    /**
     * A thank-you went in the jar: the one thanked blushes, the other smiles. When it filled the
     * jar, Mochi comes to celebrate and one old thank-you is read aloud (the twist).
     */
    fun onThankYou(fromBoy: Boolean, text: String, filledJar: Boolean) {
        val giver = if (fromBoy) boy else girl
        val thanked = if (fromBoy) girl else boy
        thanked.emotion = CharacterEmotion.SHY
        thanked.reactionTimer = 2.4f
        giver.emotion = CharacterEmotion.LOVING
        giver.reactionTimer = 2.4f
        emote(thanked, EmoteType.BLUSH, 2f)
        emote(giver, EmoteType.HEART, 2f)
        speakerSpeech(giver, GameText.get(Res.string.jar_thanks_line, text), 2.8f)
        audio.playHeartChime()
        if (!filledJar) return
        val cw = lastWorldW
        val ch = lastWorldH
        particles.spawnSparkles(cw * 0.5f, ch * 0.45f, 10, Color(0xFFFFD166))
        repeat(3) { particles.spawnHeart(cw * (0.4f + it * 0.1f), ch * boy.worldY - 110f) }
        // Mochi trots over to see what the fuss is about.
        if (currentScene != SceneType.COZY_LOFT && currentScene != SceneType.EVENING_RIDE) {
            catTargetX = ((boy.worldX + girl.worldX) / 2f).coerceIn(0.15f, 0.85f)
            catTargetY = catWorldY
            catFacingLeft = catTargetX < catWorldX
            catSleeping = false
            catState = CatState.WALK_FOLLOW
            audio.playCatPurr()
        }
        coupleLifeStore?.pickToReadAloud(rng)?.let { old ->
            val reader = if (old.fromBoy) girl else boy
            val writer = if (old.fromBoy) boy else girl
            jarReadAloudPending = GameText.get(Res.string.jar_read_aloud, writer.name, old.text) to reader
            jarReadAloudTimer = 3.2f
        }
    }

    private var jarReadAloudPending: Pair<String, PixelCharacter>? = null
    private var jarReadAloudTimer = 0f

    private fun updateCoupleLife(dt: Float) {
        jarReadAloudPending?.let { (line, reader) ->
            jarReadAloudTimer -= dt
            if (jarReadAloudTimer <= 0f) {
                jarReadAloudPending = null
                speakerSpeech(reader, line, 4.2f)
                emote(if (reader === boy) girl else boy, EmoteType.HEART, 2.4f)
            }
        }
        updateRainyReading()
        updateMakeUpBench(dt)
        updatePhonesDown(dt)
    }

    /** On a rainy day in the kitchen, once a day, one old thank-you is remembered. */
    private fun updateRainyReading() {
        val store = coupleLifeStore ?: return
        if (weather != WeatherType.RAIN || currentScene != SceneType.COOKING || sceneTime < 6f) return
        val today = CoupleDates.today().toString()
        if (store.lastRainyReading == today || sceneMessage != null || isWatchSceneActive) return
        val old = store.pickToReadAloud(rng) ?: return
        store.lastRainyReading = today
        val writer = if (old.fromBoy) boy else girl
        showMessage(GameText.get(Res.string.jar_rainy_line, writer.name, old.text), duration = 5f)
        emote(if (old.fromBoy) girl else boy, EmoteType.HEART, 2.4f)
    }

    // The Make-Up Bench: they sit apart under a small grey cloud while each writes privately;
    // when both are done the cloud clears to a rainbow and they come back together.

    /** True from "We need a moment" until they've come back together. */
    var makeUpActive: Boolean by mutableStateOf(false)
        private set
    /** 1 while the grey cloud hangs over them, fading to 0 as it clears. */
    var makeUpCloud: Float = 0f
        private set
    /** 0 to 1 while the rainbow shows after it clears. */
    var makeUpRainbow: Float = 0f
        private set
    private var makeUpClearing = false

    /** The scene's opening script would hold their places, so a moment that moves them skips the rest of it. */
    private fun skipOpeningScript() {
        if (sceneTime < scriptEndTime() + 0.6f) sceneTime = scriptEndTime() + 0.6f
    }

    fun startMakeUpBench() {
        skipOpeningScript()
        makeUpActive = true
        makeUpCloud = 1f
        makeUpRainbow = 0f
        makeUpClearing = false
        notifyUserInteraction()
        for ((c, x) in listOf(boy to 0.30f, girl to 0.70f)) {
            c.moveTo(x, c.worldY)
            c.emotion = CharacterEmotion.SHY
            emote(c, EmoteType.DOTS, 2.4f)
        }
        boy.direction = Direction.LEFT
        girl.direction = Direction.RIGHT
    }

    /** Both have written and chosen what's next: the cloud clears and they come together. */
    fun finishMakeUpBench(choice: com.example.data.MakeUpChoice) {
        if (!makeUpActive) return
        makeUpClearing = true
        audio.playStarTwinkle()
        boy.moveTo(0.45f, boy.worldY, arrivePose = if (choice == com.example.data.MakeUpChoice.HUG) CharacterPose.HUG else null)
        girl.moveTo(0.55f, girl.worldY, arrivePose = if (choice == com.example.data.MakeUpChoice.HUG) CharacterPose.HUG else null)
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        for (c in charactersBoyGirl) {
            c.emotion = CharacterEmotion.LOVING
            c.reactionTimer = 6f
        }
        when (choice) {
            com.example.data.MakeUpChoice.HUG -> Unit
            com.example.data.MakeUpChoice.TEA -> for (c in charactersBoyGirl) c.hold(HeldItem.MUG, CARRIED_ITEM_SECONDS, useSeconds = 2f)
            com.example.data.MakeUpChoice.TALK_LATER -> speakerSpeech(girl, GameText.get(Res.string.bench_talk_later_line), 3f)
        }
        showMessage(GameText.get(Res.string.bench_world_line), duration = 3f)
    }

    /** They left the bench without finishing: everything goes back to normal. */
    fun cancelMakeUpBench() {
        makeUpActive = false
        makeUpCloud = 0f
        makeUpRainbow = 0f
        makeUpClearing = false
    }

    private fun updateMakeUpBench(dt: Float) {
        if (!makeUpActive) return
        // Keep their places while the cloud hangs; the routine waits too (see updateAutonomy).
        if (!makeUpClearing) {
            for (c in charactersBoyGirl) c.reactionTimer = maxOf(c.reactionTimer, 0.3f)
            return
        }
        makeUpCloud = (makeUpCloud - dt * 0.8f).coerceAtLeast(0f)
        makeUpRainbow = if (makeUpCloud > 0f) (1f - makeUpCloud) else (makeUpRainbow - dt * 0.25f).coerceAtLeast(0f)
        if (makeUpCloud <= 0f && makeUpRainbow <= 0f) {
            makeUpActive = false
            makeUpClearing = false
            for (c in charactersBoyGirl) if (c.pose == CharacterPose.HUG) c.transitionPoseTo(CharacterPose.IDLE)
        }
    }

    // Phones Down: the couple settle down together and the world goes quiet under a night-light,
    // until the time is up (then a little bloom) or they end it.

    /** True while a Phones Down session is running. */
    var phonesDownActive: Boolean by mutableStateOf(false)
        private set
    /** Seconds left, for the overlay's clock. */
    var phonesDownSecondsLeft: Long by mutableStateOf(0L)
        private set
    private var phonesDownTick = 0f

    /** Starts (or, after a restart, resumes) the session saved in [coupleLifeStore]. */
    fun startPhonesDown(minutes: Int? = null) {
        val store = coupleLifeStore ?: return
        if (minutes != null) store.startPhonesDown(minutes)
        val left = store.phonesDownSecondsLeft() ?: return
        skipOpeningScript()
        phonesDownActive = true
        phonesDownSecondsLeft = left
        notifyUserInteraction()
        boy.moveTo(0.45f, boy.worldY)
        girl.moveTo(0.55f, girl.worldY, arrivePose = CharacterPose.SIT_SNUGGLE)
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        for (c in charactersBoyGirl) c.emotion = CharacterEmotion.LOVING
    }

    /** They ended it early: nothing lost, and it still counts as time together. */
    fun stopPhonesDown() {
        val store = coupleLifeStore ?: return
        if (!phonesDownActive) return
        store.endPhonesDown()
        phonesDownActive = false
        for (c in charactersBoyGirl) c.transitionPoseTo(CharacterPose.IDLE)
        showMessage(GameText.get(Res.string.pd_ended_early), duration = 3f)
    }

    private fun updatePhonesDown(dt: Float) {
        if (!phonesDownActive) return
        val store = coupleLifeStore ?: return
        for (c in charactersBoyGirl) c.reactionTimer = maxOf(c.reactionTimer, 0.3f)
        phonesDownTick += dt
        if (phonesDownTick < 0.5f) return
        phonesDownTick = 0f
        val left = store.phonesDownSecondsLeft() ?: run { phonesDownActive = false; return }
        phonesDownSecondsLeft = left
        if (left > 0L) return
        val minutes = store.phonesDownMinutes
        if (store.endPhonesDown()) {
            onProgress?.invoke(com.example.progress.ProgressEvent.PhonesDown(minutes))
            showMessage(GameText.get(Res.string.pd_finished, minutes), duration = 5f)
            val cw = lastWorldW
            val ch = lastWorldH
            repeat(6) { particles.spawnPetals(cw * (0.3f + it * 0.08f), ch * 0.35f, 2) }
            particles.spawnSparkles(cw * 0.5f, ch * boy.worldY - 100f, 8)
            audio.playHeartChime()
        }
        phonesDownActive = false
        for (c in charactersBoyGirl) {
            c.transitionPoseTo(CharacterPose.IDLE)
            emote(c, EmoteType.HEART, 2.4f)
        }
    }

    // ── Festivals (plan 09, D) ─────────────────────────────────────────────
    // A poster two days before, then three days of a festival in its own scene: a picnic with
    // flower crowns picked in secret, lanterns carrying sealed wishes, gifts under a little tree.
    // The screens are in FestivalDialogs; the world's side is here and in WorldFestivals.

    /** The festivals' saved data; set by the app once it has storage. */
    var festivalStore: com.example.data.FestivalStore? by mutableStateOf(null)
    /** Today's festival (or its poster), refreshed once a day. */
    var festivalDay: com.example.data.FestivalDay? by mutableStateOf(null)
        private set
    /** Wrapped gifts wait under the tree (both picked, not opened yet). */
    var giftBoxesWaiting: Boolean by mutableStateOf(false)
        private set
    /** Seconds since the lanterns were let go, or 0 when there are none in the sky. */
    var lanternRise: Float = 0f
        private set
    private var festivalCheckedDay = Long.MIN_VALUE
    private var festivalAnnouncedDay = Long.MIN_VALUE

    fun festivalName(f: com.example.data.Festival): String = GameText.get(
        when (f) {
            com.example.data.Festival.BLOSSOM_PICNIC -> Res.string.fest_blossom_picnic
            com.example.data.Festival.LANTERN_NIGHT -> Res.string.fest_lantern_night
            com.example.data.Festival.GIFT_EXCHANGE -> Res.string.fest_gift_exchange
        }
    )

    /** Today's festival, if it's on and not celebrated yet this year (so it can be joined). */
    val festivalToJoin: com.example.data.Festival?
        get() {
            val day = festivalDay ?: return null
            val store = festivalStore ?: return null
            if (day.phase != com.example.data.FestivalPhase.ON) return null
            return day.festival.takeIf { !store.celebrated(it, day.year) || (it == com.example.data.Festival.GIFT_EXCHANGE && giftBoxesWaiting) }
        }

    /** Re-reads today's festival and what's been done (after a dialog changes something). */
    fun refreshFestival() {
        val store = festivalStore ?: return
        val day = com.example.data.Festivals.today()
        festivalDay = day
        val on = day?.phase == com.example.data.FestivalPhase.ON
        // The crowns are worn on the picnic's days once they've been revealed.
        val picnic = if (on && day?.festival == com.example.data.Festival.BLOSSOM_PICNIC) store.picks(day.festival, day.year) else null
        val crowns = picnic?.takeIf { it.revealed }
        boy.crownFlower = crowns?.girl?.toIntOrNull() ?: -1 // she picked his
        girl.crownFlower = crowns?.boy?.toIntOrNull() ?: -1
        val gifts = if (on && day?.festival == com.example.data.Festival.GIFT_EXCHANGE) store.picks(day.festival, day.year) else null
        giftBoxesWaiting = gifts != null && gifts.boy.isNotEmpty() && gifts.girl.isNotEmpty() && !gifts.revealed
    }

    private fun updateFestival(dt: Float) {
        festivalStore ?: return
        val today = CoupleDates.today().toEpochDays().toLong()
        if (today != festivalCheckedDay) {
            festivalCheckedDay = today
            refreshFestival()
        }
        if (lanternRise > 0f) {
            lanternRise += dt
            if (lanternRise > LANTERN_SECONDS) lanternRise = 0f
        }
        // Once a day: the poster, or "it's today".
        val day = festivalDay ?: return
        if (festivalAnnouncedDay == today || sceneTime < 4f || sceneMessage != null || isWatchSceneActive || birthdaySurprise.isRunning) return
        festivalAnnouncedDay = today
        val name = festivalName(day.festival)
        val line = when (day.phase) {
            com.example.data.FestivalPhase.POSTER -> {
                val start = kotlinx.datetime.LocalDate(day.year, com.example.data.Festivals.month(day.festival, com.example.data.Festivals.isSouthern(androidx.compose.ui.text.intl.Locale.current.region)), com.example.data.Festivals.FIRST_DAY)
                val days = CoupleDates.today().daysUntil(start)
                if (days <= 1) GameText.get(Res.string.fest_poster_tomorrow, name) else GameText.get(Res.string.fest_poster, name, days)
            }
            com.example.data.FestivalPhase.ON -> if (festivalToJoin != null) GameText.get(Res.string.fest_today, name) else return
        }
        showMessage(line, duration = 4.5f)
    }

    /** The picnic's crowns are revealed: [boyPickedForGirl] and [girlPickedForBoy] are flowers. */
    fun celebratePicnic(boyPickedForGirl: Int, girlPickedForBoy: Int) {
        boy.crownFlower = girlPickedForBoy
        girl.crownFlower = boyPickedForGirl
        for (c in charactersBoyGirl) {
            c.reactionTimer = 2.6f
            c.emotion = CharacterEmotion.LOVING
            if (c.pose != CharacterPose.SIT && c.pose != CharacterPose.SIT_SNUGGLE) c.pose = CharacterPose.JOY_JUMP
            emote(c, EmoteType.HEART, 2.2f)
        }
        particles.spawnPetals(lastWorldW * 0.5f, lastWorldH * 0.40f, 8)
        audio.playHeartChime()
    }

    /** The two lanterns are let go: they rise from the couple into the sky. */
    fun releaseLanterns() {
        lanternRise = 0.01f
        for (c in charactersBoyGirl) {
            c.reactionTimer = 4f
            c.emotion = CharacterEmotion.LOVING
            emote(c, EmoteType.SPARKLE, 2.4f)
        }
        audio.playStarTwinkle()
    }

    /** The gifts are opened together; Mochi makes off with a ribbon. */
    fun openFestivalGifts() {
        giftBoxesWaiting = false
        for (c in charactersBoyGirl) {
            c.reactionTimer = 3f
            c.emotion = CharacterEmotion.LOVING
            emote(c, EmoteType.HEART, 2.4f)
        }
        particles.spawnSparkles(lastWorldW * GiftTree.X, lastWorldH * GiftTree.FLOOR_Y - 20f, 10, Color(0xFFFFD166))
        audio.playHeartChime()
        if (currentScene != SceneType.COZY_LOFT && currentScene != SceneType.EVENING_RIDE) {
            catTargetX = GiftTree.X + 0.06f
            catTargetY = catWorldY
            catFacingLeft = catTargetX < catWorldX
            catSleeping = false
            catState = CatState.WALK_FOLLOW
            showMessage(GameText.get(Res.string.fest_gift_ribbon), duration = 3.5f)
        }
    }

    /** Where the little tree stands in the living room. */
    object GiftTree {
        const val X = 0.13f
        const val FLOOR_Y = 0.70f
    }

    // ── Birthday surprise (plan 09, A) ────────────────────────────────────
    // On a birthday (or up to three days late, if the app wasn't opened on the day) the couple
    // throws a surprise party at home: the room is dark, a tap turns the light on, and the
    // partner is there with a cake. Candles, a wish, a gift with the sealed letter, and the party
    // carries on for the rest of the day. Taps reach it through [onBirthdayTap].

    private fun partnerChar(p: com.example.data.Partner): PixelCharacter = if (p == com.example.data.Partner.BOY) boy else girl

    /** True when nothing else owns the screen, so a surprise may start. */
    private fun canStartBirthday(): Boolean =
        wipeAlpha == 0f && !isWatchSceneActive && !isDreamMode && sceneTime > 1.0f &&
            !catchGame.active && !cozy.cookingActive && !cozy.fishingActive && starPuzzle.current == null

    private fun updateBirthday(dt: Float, cw: Float, ch: Float) {
        val store = birthdayStore ?: return
        val today = CoupleDates.today()
        val surprise = birthdaySurprise
        if (!surprise.isRunning) {
            // Party mode after the surprise (also after the app is reopened the same day).
            if (surprise.partyDay != today) {
                val heldToday = com.example.data.Partner.entries.filter { store.partyHeldOn(it) == today }
                if (heldToday.isNotEmpty()) {
                    surprise.partyDay = today
                    if (surprise.birthdayOf.isEmpty()) surprise.birthdayOf = heldToday
                }
            }
            val party = surprise.partyDay == today
            for (p in com.example.data.Partner.entries) partnerChar(p).wearsPartyHat = party && p in surprise.birthdayOf
            val epochDay = today.toEpochDays().toLong()
            if (epochDay != birthdayCheckedDay && canStartBirthday()) {
                birthdayCheckedDay = epochDay
                val due = store.partyDue(today)
                if (due.isNotEmpty()) startBirthdaySurprise(store, due, today)
            }
            return
        }
        surprise.stepTime += dt
        val t = surprise.stepTime
        // Hold their places and poses: the room's idle loops wait until the surprise is over.
        boy.reactionTimer = maxOf(boy.reactionTimer, 0.3f)
        girl.reactionTimer = maxOf(girl.reactionTimer, 0.3f)
        when (surprise.step) {
            SurpriseStep.DARK -> if (t > 3.5f && t - dt <= 3.5f) {
                showMessage(GameText.get(Res.string.bday_dark_hint), duration = 30f)
            }
            SurpriseStep.LIGHTS -> if (t >= BirthdaySurprise.LIGHTS_SECONDS) beginSurpriseReveal(cw, ch)
            SurpriseStep.SURPRISE -> {
                if (eventChance(6f, dt)) confettiBurst(cw, ch, 2)
                if (t >= BirthdaySurprise.SURPRISE_SECONDS) {
                    surprise.go(SurpriseStep.CANDLES)
                    showMessage(GameText.get(Res.string.bday_candles_hint), duration = 30f)
                }
            }
            SurpriseStep.CANDLES -> if (cakeHolder()?.heldItemState == 0 && t >= BirthdaySurprise.AFTER_CANDLES_SECONDS) {
                if (birthdayOverlayShowing) {
                    surprise.go(SurpriseStep.WISH)
                    showMessage(GameText.get(Res.string.bday_make_a_wish), duration = 3f)
                } else {
                    toGift()
                }
            }
            SurpriseStep.WISH -> if (!birthdayOverlayShowing || t >= BirthdaySurprise.WISH_TIMEOUT_SECONDS) toGift()
            SurpriseStep.LETTER -> if (!birthdayOverlayShowing) finishBirthdaySurprise()
            else -> Unit
        }
    }

    private fun startBirthdaySurprise(store: com.example.data.BirthdayStore, due: List<com.example.data.Partner>, today: kotlinx.datetime.LocalDate) {
        val birthdays = store.birthdays()
        val first = due.first()
        val birthday = birthdays[first] ?: return
        val date = com.example.data.Birthdays.last(birthday, today)
        val letters = due.mapNotNull { p -> birthdays[p]?.let { store.birthdayLetter(p, com.example.data.Birthdays.last(it, today)) } }
        loadScene(SceneType.SLEEP)
        sceneTime = scriptEndTime() + 1f // the party replaces the sleepy opening
        wipeAlpha = 0f
        birthdaySurprise.begin(due, date, com.example.data.Birthdays.isBelated(birthday, today), letters)
        birthdayLinesSaid = 0
        notifyUserInteraction()
        // Places for the reveal: standing in front of the sofa, facing each other.
        for ((c, x) in listOf(boy to 0.44f, girl to 0.56f)) {
            c.moveTo(x, 0.68f)
            c.pose = CharacterPose.IDLE
        }
        boy.direction = Direction.RIGHT
        girl.direction = Direction.LEFT
        if (due.size == 1) partnerChar(first.other).emotion = CharacterEmotion.PLAYFUL
        catSleeping = false
        catState = CatState.SITTING_PURR
        catWorldX = 0.70f
        showMessage(GameText.get(Res.string.bday_dark), duration = 30f)
    }

    /** Who holds the cake: the partner, or the girl when it's both their birthdays. */
    private fun cakeHolder(): PixelCharacter? {
        val who = birthdaySurprise.birthdayOf
        if (who.isEmpty()) return null
        return if (who.size > 1) girl else partnerChar(who.first().other)
    }

    private fun beginSurpriseReveal(cw: Float, ch: Float) {
        val surprise = birthdaySurprise
        surprise.go(SurpriseStep.SURPRISE)
        sceneMessage = null
        val holder = cakeHolder() ?: return
        holder.hold(com.example.engine.HeldItem.CAKE, 600f)
        holder.heldItemState = (1 shl BirthdaySurprise.CANDLES) - 1
        holder.emotion = CharacterEmotion.LOVING
        for (p in surprise.birthdayOf) {
            val c = partnerChar(p)
            c.wearsPartyHat = true
            c.emotion = CharacterEmotion.SURPRISED
            emote(c, EmoteType.EXCLAMATION, 1.6f)
        }
        val name = partnerChar(surprise.birthdayOf.first()).name
        val line = when {
            surprise.birthdayOf.size > 1 -> GameText.get(Res.string.bday_surprise_both)
            surprise.belated -> GameText.get(Res.string.bday_surprise_belated, name)
            else -> GameText.get(Res.string.bday_surprise_one, name)
        }
        speakerSpeech(holder, line, 3.2f)
        if (surprise.birthdayOf.size == 1) {
            speakerSpeech(partnerChar(surprise.birthdayOf.first()), GameText.get(Res.string.bday_reaction), 2.4f)
        }
        confettiBurst(cw, ch, 10)
        audio.playHeartChime()
        audio.playStarTwinkle()
    }

    private fun confettiBurst(cw: Float, ch: Float, count: Int) {
        val colors = listOf(Color(0xFFFF6B9A), Color(0xFFFFD166), Color(0xFF8ECAE6), Color(0xFFB497E7), Color(0xFF6BBF59))
        repeat(count) {
            particles.spawnSparkles(cw * (0.15f + rng.nextFloat() * 0.7f), ch * (0.35f + rng.nextFloat() * 0.15f), 2, colors[rng.nextInt(colors.size)])
        }
    }

    private fun toGift() {
        birthdaySurprise.go(SurpriseStep.GIFT)
        showMessage(GameText.get(Res.string.bday_gift_hint), duration = 30f)
    }

    /**
     * A tap during the surprise. Returns true when the surprise used it (the tap then does
     * nothing else): the light, a candle, the gift.
     */
    fun onBirthdayTap(cw: Float, ch: Float): Boolean {
        val surprise = birthdaySurprise
        if (!surprise.isRunning) return false
        when (surprise.step) {
            SurpriseStep.DARK -> {
                surprise.go(SurpriseStep.LIGHTS)
                sceneMessage = null
                audio.playBubblePop()
            }
            SurpriseStep.CANDLES -> {
                val holder = cakeHolder() ?: return true
                val lit = holder.heldItemState
                if (lit != 0) {
                    val candle = 31 - lit.countLeadingZeroBits()
                    holder.heldItemState = lit and (1 shl candle).inv()
                    surprise.stepTime = 0f
                    particles.spawnSteam(cw * holder.worldX, ch * holder.worldY - 60f)
                    audio.playBubblePop()
                    if (holder.heldItemState == 0) {
                        sceneMessage = null
                        for (p in surprise.birthdayOf) emote(partnerChar(p), EmoteType.HEART, 2f)
                        particles.spawnHeart(cw * holder.worldX, ch * holder.worldY - 90f)
                        audio.playHeartChime()
                    }
                }
            }
            SurpriseStep.GIFT -> {
                sceneMessage = null
                audio.playStarTwinkle()
                particles.spawnSparkles(cw * 0.5f, ch * 0.70f, 8)
                if (surprise.letters.isNotEmpty() && birthdayOverlayShowing) {
                    surprise.go(SurpriseStep.LETTER)
                } else {
                    cakeHolder()?.let { speakerSpeech(it, GameText.get(Res.string.bday_no_letter_line), 3f) }
                    finishBirthdaySurprise()
                }
            }
            else -> Unit
        }
        return true
    }

    /** The wish from the overlay (empty to keep it secret). */
    fun submitBirthdayWish(wish: String) {
        if (birthdaySurprise.step != SurpriseStep.WISH) return
        birthdaySurprise.wish = wish.trim()
        toGift()
    }

    /** The overlay finished showing the current letter; the next one shows, or the surprise ends. */
    fun closeBirthdayLetter() {
        val surprise = birthdaySurprise
        if (surprise.step != SurpriseStep.LETTER) return
        surprise.currentLetter?.let { birthdayStore?.markOpened(it.id) }
        if (surprise.letterIndex + 1 < surprise.letters.size) {
            surprise.letterIndex++
            surprise.stepTime = 0f
        } else {
            finishBirthdaySurprise()
        }
    }

    private fun finishBirthdaySurprise() {
        val surprise = birthdaySurprise
        val store = birthdayStore
        val today = CoupleDates.today()
        val birthdays = store?.birthdays().orEmpty()
        val opened = store?.letters()?.filter { it.isOpened }?.map { it.id }?.toSet().orEmpty()
        for (p in surprise.birthdayOf) {
            val birthday = birthdays[p] ?: continue
            val thisYear = com.example.data.Birthdays.last(birthday, today)
            store?.recordParty(
                com.example.data.BirthdayRecord(
                    partner = p,
                    date = thisYear.toString(),
                    age = com.example.data.Birthdays.ageOn(birthday, thisYear),
                    wish = surprise.wish,
                    letterId = surprise.letters.firstOrNull { it.recipient == p && it.id in opened }?.id,
                    belated = surprise.belated
                )
            )
            onProgress?.invoke(com.example.progress.ProgressEvent.BirthdayCelebrated(p == com.example.data.Partner.BOY, surprise.belated))
        }
        surprise.partyDay = today
        surprise.go(SurpriseStep.DONE)
        sceneMessage = null
        // The cake stays out a little longer, then they settle down for the party.
        cakeHolder()?.heldItemTimeLeft = 20f
        for (p in surprise.birthdayOf) emote(partnerChar(p), EmoteType.HEART, 2.4f)
    }

    /** During the party, the first tap on each of them says something for the day. */
    private fun sayBirthdayLine(c: PixelCharacter): Boolean {
        val surprise = birthdaySurprise
        if (surprise.isRunning || !surprise.partyOn(CoupleDates.today())) return false
        val bit = if (c === boy) 1 else 2
        if (birthdayLinesSaid and bit != 0) return false
        val birthdayPerson = surprise.birthdayOf.firstOrNull() ?: return false
        birthdayLinesSaid = birthdayLinesSaid or bit
        val isBirthday = surprise.birthdayOf.any { partnerChar(it) === c }
        val line = if (isBirthday) GameText.get(Res.string.bday_party_line_birthday)
        else GameText.get(Res.string.bday_party_line_partner, partnerChar(birthdayPerson).name)
        speakerSpeech(c, line, 2.8f)
        emote(c, EmoteType.HEART, 1.6f)
        c.reactionTimer = 1.6f
        return true
    }

    /** Mochi wears a party ruff while the party is on. */
    val mochiPartyCollar: Boolean get() = birthdaySurprise.partyOn(CoupleDates.today())

    // ── Requests (plan 07 D1) ─────────────────────────────────────────────
    // Now and then one of them asks for something that fits the moment: a blanket, tea, a
    // song, a snack, Mochi. Tapping the matching prop grants it; otherwise it fades quietly.

    /** Who is asking right now, if anyone. */
    val requestAsker: PixelCharacter?
        get() = if (!requests.active) null else if (requests.askerIsGirl) girl else boy

    private fun isMiniGameActive(): Boolean =
        cozy.cookingActive || cozy.fishingActive || catchGame.active || starPuzzle.current != null

    private fun isAsleep(c: PixelCharacter): Boolean =
        c.pose == CharacterPose.SLEEP || c.pose == CharacterPose.SLEEP_YAWN ||
            agentFor(c).behavior == Behavior.RARE_DOZE_OFF

    private fun updateRequests(dt: Float) {
        // A game or the scooter ride takes over: the wish is dropped without a word.
        if (requests.active && (isMiniGameActive() || currentScene == SceneType.EVENING_RIDE)) requests.interrupt(rng)
        // An unanswered request just fades; nothing else happens, and nothing counts it.
        requests.tick(dt)
        // The asker carries the bubble (drawn by the renderer, or over the loft's sofa couple).
        val asker = requestAsker
        for (c in listOf(boy, girl)) {
            if (c === asker) {
                if (c.requestIcon != requests.kind.name) c.requestSeconds = 0f
                c.requestIcon = requests.kind.name
                c.requestFade = requests.fade
                c.requestSeconds += dt
            } else {
                c.requestIcon = null
            }
        }
    }

    /** Whether a request may start now: its timing allows it, nothing else is going on, and some kind fits. */
    private fun requestAvailableNow(partner: PixelCharacter): Boolean {
        if (!requests.canStart || currentScene == SceneType.EVENING_RIDE || isMiniGameActive()) return false
        if (sceneMessage != null || isAsleep(boy) || isAsleep(girl)) return false
        return RequestKind.entries.any { requestFits(it) }
    }

    /** Which requests make sense in this scene at this time of day. */
    private fun requestFits(kind: RequestKind): Boolean {
        val phase = timeOfDayPhase
        val evening = phase.isSunset || phase.isNight
        return when (kind) {
            RequestKind.WARM -> (evening && (currentScene == SceneType.COZY_LOFT || currentScene == SceneType.SLEEP)) ||
                (phase.isNight && currentScene == SceneType.CAMPFIRE)
            RequestKind.TEA -> (currentScene == SceneType.COZY_LOFT || currentScene == SceneType.COOKING) && !phase.isMidnight
            RequestKind.SONG -> (currentScene == SceneType.COZY_LOFT || currentScene == SceneType.CAMPFIRE) && evening
            RequestKind.SNACK -> currentScene == SceneType.COOKING || currentScene == SceneType.RAINY_CAFE ||
                currentScene == SceneType.SEASIDE_PIER
            // Mochi isn't drawn in the loft, and must be awake to come over.
            RequestKind.MOCHI -> currentScene != SceneType.COZY_LOFT && currentScene != SceneType.EVENING_RIDE &&
                catState != CatState.SLEEPING && !catSleeping
        }
    }

    /** Roughly where the thing they're asking for is, so they can turn toward it. */
    private fun requestTargetX(kind: RequestKind): Float? = when {
        kind == RequestKind.MOCHI -> catWorldX
        kind == RequestKind.WARM && currentScene == SceneType.CAMPFIRE -> CampfireLayout.PIT_X
        kind == RequestKind.TEA && currentScene == SceneType.COOKING -> 0.60f
        kind == RequestKind.SNACK && currentScene == SceneType.COOKING -> 0.78f
        else -> null
    }

    /** The asker turns toward the prop and asks. GROWTH adds the spoken line and the bubble. */
    private fun announceRequest(c: PixelCharacter, kind: RequestKind) {
        requestTargetX(kind)?.let { x ->
            if (abs(x - c.worldX) > 0.02f) c.direction = if (x < c.worldX) Direction.LEFT else Direction.RIGHT
        }
        c.emotion = CharacterEmotion.SHY
        // Said once, as the caption, so screen readers hear it too.
        val line = when (kind) {
            RequestKind.WARM -> Res.string.request_warm
            RequestKind.TEA -> Res.string.request_tea
            RequestKind.SONG -> Res.string.request_song
            RequestKind.SNACK -> Res.string.request_snack
            RequestKind.MOCHI -> Res.string.request_mochi
        }
        showMessage(GameText.get(line, c.name), duration = 3.5f)
    }

    /**
     * A tap on a prop that answers the current request grants it: both light up and the asker
     * says thanks (a mug in hand for tea). Taps that don't match change nothing here.
     */
    private fun grantRequest(kind: RequestKind) {
        if (!requests.grant(kind, rng)) return
        val asker = if (requests.askerIsGirl) girl else boy
        val partner = if (asker === boy) girl else boy
        asker.emotion = CharacterEmotion.LOVING
        partner.emotion = CharacterEmotion.LOVING
        emote(asker, EmoteType.HEART, 2.2f)
        emote(partner, EmoteType.HEART, 2.2f)
        particles.spawnSparkles(lastCanvasW * asker.worldX, lastCanvasH * asker.worldY - 90f, 6)
        audio.playHeartChime()
        if (kind == RequestKind.TEA) asker.hold(HeldItem.MUG, CARRIED_ITEM_SECONDS, useSeconds = 1.6f)
        asker.requestIcon = null
        val thanks = when (kind) {
            RequestKind.WARM -> Res.string.request_thanks_warm
            RequestKind.TEA -> Res.string.request_thanks_tea
            RequestKind.SONG -> Res.string.request_thanks_song
            RequestKind.SNACK -> Res.string.request_thanks_snack
            RequestKind.MOCHI -> Res.string.request_thanks_mochi
        }
        // Said by the asker in their own speech bubble; the prop's usual caption still plays.
        if (asker === boy) {
            boySpeechText = GameText.get(thanks)
            boySpeechTimer = 3f
        } else {
            girlSpeechText = GameText.get(thanks)
            girlSpeechTimer = 3f
        }
        onProgress?.invoke(com.example.progress.ProgressEvent.RequestGranted(kind.name))
        onRequestGranted?.invoke(kind)
    }

    /** Called when a request is granted (GROWTH's progress events and thank-you line hook in here). */
    var onRequestGranted: ((RequestKind) -> Unit)? = null

    /** For tests and previews: [asker] asks for [kind] right away, whatever the timing. */
    fun startRequestForTest(kind: RequestKind, asker: PixelCharacter) {
        requests.forceStart(kind, asker === girl)
        announceRequest(asker, kind)
    }

    // ── Discoveries ──────────────────────────────────────────────────────
    // Every minute or so something small turns up: a wildflower, a red leaf, a folded note,
    // Mochi's lost toy, a seashell. A character may notice it, wander over and react.

    private val noteLinePicker = AntiRepeatRandomPicker(listOf(
        Res.string.scene_auto_note_ordinary_days, Res.string.scene_auto_note_under_stars,
        Res.string.scene_auto_note_favorite_person, Res.string.scene_auto_note_choose_you
    ))

    private fun resetDiscovery() {
        discovery.clear()
        discoverySpawnTimer = DISCOVERY_FIRST_DELAY + rng.nextFloat() * 20f
    }

    /** Ages the current discovery, or places a new one when it's time. */
    private fun updateDiscovery(dt: Float, cw: Float, ch: Float) {
        if (discovery.active) {
            discovery.age += dt
            // A claimed find whose finder got interrupted still fades away eventually.
            if (discovery.age > DISCOVERY_LIFETIME && (!discovery.claimed || discovery.age > DISCOVERY_LIFETIME * 2f)) {
                discovery.clear()
                discoverySpawnTimer = DISCOVERY_GAP_MIN + rng.nextFloat() * DISCOVERY_GAP_RANGE
            }
            return
        }
        discoverySpawnTimer -= dt
        if (discoverySpawnTimer > 0f) return
        discoverySpawnTimer = DISCOVERY_GAP_MIN + rng.nextFloat() * DISCOVERY_GAP_RANGE
        if (!fillWalkBounds(cw, ch)) return
        val kind = pickDiscoveryKind()
        for (attempt in 0 until 8) {
            val x = walkBounds[0] + 0.04f + rng.nextFloat() * (walkBounds[1] - walkBounds[0] - 0.08f)
            val y = walkBounds[2] + 0.02f + rng.nextFloat() * (walkBounds[3] - walkBounds[2] - 0.02f)
            if (kotlin.math.hypot(boy.worldX - x, boy.worldY - y) < 0.15f) continue
            if (kotlin.math.hypot(girl.worldX - x, girl.worldY - y) < 0.15f) continue
            if (avoidCampfirePit(x, y) != x) continue
            discovery.place(kind, x, y)
            return
        }
    }

    private fun pickDiscoveryKind(): DiscoveryKind {
        val roll = rng.nextFloat()
        return when {
            currentScene == SceneType.SEASIDE_PIER -> if (roll < 0.6f) DiscoveryKind.SEASHELL else DiscoveryKind.LOVE_NOTE
            !isCurrentSceneOutdoor -> if (roll < 0.5f) DiscoveryKind.MOCHI_TOY else DiscoveryKind.LOVE_NOTE
            timeOfDayPhase.isNight && roll < 0.5f -> DiscoveryKind.STAR_PEBBLE
            weather == WeatherType.AUTUMN && roll < 0.7f -> DiscoveryKind.RED_LEAF
            weather == WeatherType.SNOW -> if (roll < 0.5f) DiscoveryKind.STAR_PEBBLE else DiscoveryKind.LOVE_NOTE
            roll < 0.55f -> DiscoveryKind.WILDFLOWER
            roll < 0.8f -> DiscoveryKind.LOVE_NOTE
            else -> DiscoveryKind.MOCHI_TOY
        }
    }

    /** Something worth noticing, once it has been there a moment and nobody is already going for it. */
    private fun discoveryNear(c: PixelCharacter): Discovery? =
        if (discovery.active && !discovery.claimed && discovery.age > 2f) discovery else null

    /** Walk up beside the discovery (called when the brain picks DISCOVER). */
    private fun walkToDiscovery(c: PixelCharacter, agent: AutonomyAgent, cw: Float, ch: Float) {
        val d = discoveryNear(c)
        if (d == null || !fillWalkBounds(cw, ch)) {
            agent.rest(2f)
            return
        }
        d.claimed = true
        val side = if (c.worldX < d.x) -1f else 1f
        val tx = (d.x + side * 0.05f).coerceIn(walkBounds[0], walkBounds[1])
        walkTo(c, agent, avoidCampfirePit(tx, d.y), d.y)
    }

    /** They've reached it: pick it up and react. */
    private fun startDiscovery(c: PixelCharacter, cw: Float, ch: Float) {
        if (!discovery.active) return
        onProgress?.invoke(com.example.progress.ProgressEvent.DiscoveryFound(discovery.kind.name, if (c === boy) "BOY" else "GIRL"))
        if (discovery.kind == com.example.scene.autonomy.DiscoveryKind.MOCHI_TOY) careForMochi(4)
        val partner = if (c === boy) girl else boy
        val x = cw * discovery.x
        val y = ch * discovery.y
        c.direction = if (discovery.x < c.worldX) Direction.LEFT else Direction.RIGHT
        c.transitionPoseTo(CharacterPose.IDLE)
        c.hold(heldItemFor(discovery.kind), CARRIED_ITEM_SECONDS, useSeconds = 2.4f)
        c.emotion = CharacterEmotion.SURPRISED
        audio.playStarTwinkle()
        particles.spawnSparkles(x, y - 20f, 5)
        when (discovery.kind) {
            DiscoveryKind.WILDFLOWER -> {
                emote(c, EmoteType.HEART, 2f)
                val closeToPartner = kotlin.math.hypot(partner.worldX - c.worldX, partner.worldY - c.worldY) < 0.3f
                showMessage(
                    if (closeToPartner) GameText.get(Res.string.scene_auto_found_flower_for, c.name, partner.name)
                    else GameText.get(Res.string.scene_auto_found_flower, c.name),
                    duration = 3f
                )
                if (closeToPartner) {
                    emote(partner, EmoteType.BLUSH, 2f)
                    // Show it off for a moment, then hand it over.
                    flowerHandoverFrom = c
                    flowerHandoverTimer = 2.6f
                }
            }
            DiscoveryKind.RED_LEAF -> {
                emote(c, EmoteType.SPARKLE, 2f)
                particles.spawnLeaf(x, y - 30f)
                showMessage(GameText.get(Res.string.scene_auto_found_leaf, c.name), duration = 3f)
            }
            DiscoveryKind.LOVE_NOTE -> {
                emote(c, EmoteType.BLUSH, 2.4f)
                audio.playPaperFlip()
                showMessage(GameText.get(Res.string.scene_auto_found_note, c.name, GameText.get(noteLinePicker.pick())), duration = 4f)
            }
            DiscoveryKind.MOCHI_TOY -> {
                emote(c, EmoteType.EXCLAMATION, 1.8f)
                showMessage(GameText.get(Res.string.scene_auto_found_mochi_toy, c.name), duration = 3f)
                if (currentScene != SceneType.COZY_LOFT && catState != CatState.WALK_FOLLOW) {
                    catTargetX = avoidCampfirePit(discovery.x, catWorldY)
                    catTargetY = catWorldY
                    catFacingLeft = catTargetX < catWorldX
                    catState = CatState.WALK_FOLLOW
                    catSleeping = false
                    audio.playCatPurr()
                }
            }
            DiscoveryKind.SEASHELL -> {
                emote(c, EmoteType.HEART, 2f)
                showMessage(GameText.get(Res.string.scene_auto_found_shell, c.name), duration = 3f)
            }
            DiscoveryKind.STAR_PEBBLE -> {
                emote(c, EmoteType.SPARKLE, 2.2f)
                particles.spawnSparkles(x, y - 40f, 6, Color(0xFFFFF3B0))
                showMessage(GameText.get(Res.string.scene_auto_found_pebble, c.name), duration = 3f)
            }
        }
        discovery.clear()
        discoverySpawnTimer = DISCOVERY_GAP_MIN + rng.nextFloat() * DISCOVERY_GAP_RANGE
    }

    private fun currentMonth(): Int =
        kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).month.number

    /** Moves on to another of the season's weathers. */
    fun driftWeather() {
        val next = com.example.engine.SeasonalWeather.next(
            weather, currentMonth(), androidx.compose.ui.text.intl.Locale.current.region
        )
        if (next == weather) return
        weather = next
        audio.playWeatherBgm(next, isAutomaticDrift = true)
    }

    /** Sets the weather quietly (no message), e.g. the one the app opens with. */
    fun changeWeather(next: WeatherType) {
        weatherDriftTimer = 0f
        if (next == weather) return
        weather = next
        particles.clearSeasonalParticles()
        audio.playWeatherBgm(next, isAutomaticDrift = false)
    }

    /** Seconds left in which a weather picked by hand stays put. */
    private var manualWeatherHold = 0f

    fun cycleWeather(): WeatherType {
        weatherDriftTimer = 0f
        manualWeatherHold = MANUAL_WEATHER_HOLD_SECONDS
        val all = WeatherType.values()
        val nextIdx = (weather.ordinal + 1) % all.size
        weather = all[nextIdx]
        particles.clearSeasonalParticles()
        audio.playWindChime()
        audio.playWeatherBgm(weather, isAutomaticDrift = false)
        showMessage(GameText.get(Res.string.scene_weather, weather.displayName), duration = 2.5f)
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
        onProgress?.invoke(com.example.progress.ProgressEvent.ConstellationFound("constellation_$targetIdx"))

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
        showMessage(GameText.get(Res.string.scene_constellation, name, description), duration = 3.0f)
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
        showMessage(GameText.get(Res.string.scene_is_thinking_of_right_now, boy.name, girl.name), duration = 4.0f)
    }

    // Cozy Loft Touch Interactions
    fun onTouchLoftSofa(cw: Float, ch: Float) {
        grantRequest(RequestKind.WARM)
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
                girlSpeechText = GameText.get(Res.string.scene_stay_right_here_with_me)
                girlSpeechTimer = 3.5f
                boySpeechText = GameText.get(Res.string.scene_never_letting_go)
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
                showMessage(GameText.get(Res.string.scene_wrapped_together_under_our_warm_quilt), duration = 3.0f)
                particles.spawnHeart(cw * 0.62f, ch * 0.52f, Color(0xFFFF3366))
            }
            else -> {
                boySpeechText = GameText.get(Res.string.scene_you_are_my_entire_world)
                boySpeechTimer = 3.5f
                girlSpeechText = GameText.get(Res.string.scene_always_and_forever)
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
        girlSpeechText = GameText.get(Res.string.scene_look_the_moon_is_so_luminous_tonight)
        girlSpeechTimer = 3.5f
        boySpeechText = GameText.get(Res.string.scene_not_as_radiant_as_you)
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
            "Chapter 1: Two dreamers who found each other.",
            "Found our couple photo tucked between vintage poems!",
            "A tiny white bunny plush rests peacefully on tier 3."
        )
        showMessage(quotes.random(), duration = 3.5f)
    }

    fun onTouchLoftRecordPlayer(cw: Float, ch: Float) {
        grantRequest(RequestKind.SONG)
        val pixelScale = WorldViewport.pixelScale(cw)
        val floorY = ch * 0.58f
        audio.playBubblePop()
        if (audio.musicBoxState == MusicBoxState.PLAYING) {
            audio.pauseMusic()
            recordSpinning = false
            showMessage(GameText.get(Res.string.scene_turntable_paused), duration = 2.0f)
        } else {
            audio.togglePlayPause()
            recordSpinning = true
            repeat(4) {
                particles.spawnMusicNote(cw * 0.88f + (Random.nextFloat() - 0.5f) * 20f, floorY - 14 * pixelScale)
            }
            showMessage(GameText.get(Res.string.scene_vinyl_needle_drops_warm_cozy_melodies_pl), duration = 3.0f)
        }
    }

    fun onTouchLoftTable(cw: Float, ch: Float) {
        grantRequest(RequestKind.TEA)
        if (loftTableTimer > 0f) return
        loftTableTimer = 1.4f
        val pixelScale = WorldViewport.pixelScale(cw)
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
        val pixelScale = WorldViewport.pixelScale(cw)
        val floorY = ch * 0.58f
        lampLit = !lampLit
        audio.playBubblePop()
        ambientDimming = if (lampLit) 0.08f else 0.42f
        if (lampLit) {
            particles.spawnSparkles(cw * 0.38f, floorY - 18 * pixelScale, 5)
            showMessage(GameText.get(Res.string.scene_warm_amber_lamp_lights_up_the_loft), duration = 2.5f)
        } else {
            showMessage(GameText.get(Res.string.scene_moonlight_only_romantic_midnight_ambienc), duration = 2.5f)
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
                showMessage(GameText.get(Res.string.scene_mochi_purrs_warmly_on_the_sofa_blanket), duration = 2.5f)
            }
            CatState.BELLY_ROLL -> {
                repeat(4) {
                    particles.spawnHeart(cw * catWorldX + (Random.nextFloat() - 0.5f) * 20f, ch * catWorldY - 22f, Color(0xFFFF5D8F))
                }
                showMessage(GameText.get(Res.string.scene_mochi_stretches_and_rolls_over_on_the_qu), duration = 2.5f)
            }
            else -> {
                particles.spawnHeart(cw * catWorldX, ch * catWorldY - 20f, Color(0xFFFF8FA3))
                showMessage(GameText.get(Res.string.scene_mochi_curled_up_for_a_warm_snooze_on_the), duration = 2.5f)
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
            showMessage(GameText.get(Res.string.scene_lamp_on_warm_amber_light_fills_the_room), duration = 2.5f)
        } else {
            couchPhaseTimer = 0f
            couchVisualPhase = CouchPhase.NIGHT
            showMessage(GameText.get(Res.string.scene_lamp_off_time_to_drift_away_together), duration = 2.5f)
        }
    }

    // ── Interactive Cozy Objects Touch Handlers ──────────────────────────────

    fun onTouchAromatherapyCandle(cw: Float, ch: Float, touchX: Float, touchY: Float) {
        hearthCandleLit = !hearthCandleLit
        audio.playCandleFlicker()
        if (hearthCandleLit) {
            particles.spawnSparkles(touchX, touchY, 6)
            particles.spawnHeart(touchX, touchY - 15f, Color(0xFFFFD166))
            boy.emotion = CharacterEmotion.LOVING
            girl.emotion = CharacterEmotion.LOVING
            boy.emote = EmoteType.HEART
            girl.emote = EmoteType.SPARKLE
            boy.reactionTimer = 3.0f
            girl.reactionTimer = 3.0f
            showMessage(GameText.get(Res.string.scene_lit_the_lavender_soy_candle_warm_calming), duration = 3.0f)
        } else {
            repeat(3) { particles.spawnSteam(touchX, touchY - 8f) }
            showMessage(GameText.get(Res.string.scene_blew_out_the_candle_with_a_gentle_breath), duration = 2.5f)
        }
    }

    fun onTouchTeakettle(cw: Float, ch: Float, touchX: Float, touchY: Float) {
        grantRequest(RequestKind.TEA)
        if (teakettleWhistleTimer > 0f) return
        teakettleWhistleTimer = 2.2f
        audio.playSteamHiss()
        audio.playCookingBubbles()
        repeat(8) {
            particles.spawnSteam(touchX + (Random.nextFloat() - 0.5f) * 12f, touchY - 14f)
        }
        particles.spawnHeart(touchX, touchY - 26f, Color(0xFFFF9EAA))
        girl.worldX = 0.58f
        girl.direction = Direction.RIGHT
        girl.pose = CharacterPose.COOK
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.MUSIC_NOTE
        girl.emoteTimer = 2.5f
        girl.reactionTimer = 3.5f

        boy.worldX = 0.50f
        boy.direction = Direction.RIGHT
        boy.pose = CharacterPose.EAT_SNEAK
        boy.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        boy.emoteTimer = 2.5f
        boy.reactionTimer = 3.5f

        showMessage(GameText.get(Res.string.scene_whistling_teakettle_fresh_hot_tea_steepi), duration = 3.0f)
    }

    fun onTouchCouchThrow(cw: Float, ch: Float) {
        grantRequest(RequestKind.WARM)
        if (cuddleBlanketTimer > 0f) return
        cuddleBlanketTimer = 3.5f
        audio.playLeafRustle()
        particles.spawnHeart(cw * 0.48f, ch * 0.65f, Color(0xFFFF758F))
        boy.moveTo(0.46f)
        girl.moveTo(0.50f)
        boy.pose = CharacterPose.SIT_SNUGGLE
        girl.pose = CharacterPose.SIT_SNUGGLE
        boy.targetPose = CharacterPose.SIT_SNUGGLE
        girl.targetPose = CharacterPose.SIT_SNUGGLE
        boy.emotion = CharacterEmotion.LOVING
        girl.emotion = CharacterEmotion.LOVING
        boy.emote = EmoteType.HEART
        girl.emote = EmoteType.HEART
        boy.reactionTimer = 4.0f
        girl.reactionTimer = 4.0f

        // Mochi curls up at the edge of the blanket
        catWorldX = 0.40f
        catWorldY = 0.68f
        catState = CatState.SLEEPING
        catSleeping = true

        showMessage(GameText.get(Res.string.scene_snuggling_warm_under_the_chunky_knit_thr), duration = 3.2f)
    }

    fun onTouchWindChimes(touchX: Float, touchY: Float) {
        if (windChimeSwayTimer > 0f) return
        windChimeSwayTimer = 2.5f
        audio.playWindChime()
        particles.spawnSparkles(touchX, touchY + 16f, 6)
        particles.spawnSparkles(touchX, touchY + 36f, 4)
        boy.emotion = CharacterEmotion.HAPPY
        girl.emotion = CharacterEmotion.HAPPY
        boy.emote = EmoteType.MUSIC_NOTE
        girl.emote = EmoteType.SPARKLE
        boy.reactionTimer = 3.0f
        girl.reactionTimer = 3.0f
        showMessage(GameText.get(Res.string.scene_the_crystalline_porch_wind_chime_sings_i), duration = 3.0f)
    }

    fun onTouchFeatherWand(touchX: Float, touchY: Float) {
        if (featherWandWiggleTimer > 0f) return
        featherWandWiggleTimer = 2.4f
        audio.playCatChirp()
        audio.playCatPurr()
        catWorldX = 0.22f
        catWorldY = 0.69f
        catState = CatState.PLAYFUL_POUNCE
        catSleeping = false
        particles.spawnSparkles(touchX, touchY, 5)
        particles.spawnHeart(touchX, touchY - 20f, Color(0xFFFFB5C2))
        boy.emotion = CharacterEmotion.PLAYFUL
        girl.emotion = CharacterEmotion.PLAYFUL
        boy.emote = EmoteType.SPARKLE
        girl.emote = EmoteType.HEART
        boy.reactionTimer = 3.0f
        girl.reactionTimer = 3.0f
        showMessage(GameText.get(Res.string.scene_mochi_pounces_on_the_feather_wand_with_p), duration = 3.0f)
    }

    fun onTouchPlantWatering(touchX: Float, touchY: Float) {
        if (plantWaterTimer > 0f) return
        plantWaterTimer = 2.0f
        audio.playWaterDrip()
        particles.spawnSparkles(touchX, touchY - 12f, 6)
        particles.spawnHeart(touchX, touchY - 24f, Color(0xFF80ED99))
        girl.worldX = 0.32f
        girl.direction = Direction.LEFT
        girl.emotion = CharacterEmotion.HAPPY
        girl.emote = EmoteType.SPARKLE
        girl.reactionTimer = 3.0f
        showMessage(GameText.get(Res.string.scene_watering_the_tender_green_leaves_dewdrop), duration = 3.0f)
    }

    fun onTouchTelescope(cw: Float, ch: Float, touchX: Float, touchY: Float) {
        if (telescopeStarTimer > 0f) return
        telescopeStarTimer = 3.0f
        audio.playStarArpeggio()
        audio.playStarTwinkle()
        particles.spawnShootingStar(cw * 0.15f, ch * 0.12f)
        particles.spawnSparkles(touchX, touchY - 16f, 8)
        particles.spawnHeart(touchX, touchY - 30f, Color(0xFFFFCAD4))
        boy.direction = Direction.RIGHT
        girl.direction = Direction.RIGHT
        boy.emote = EmoteType.SPARKLE
        girl.emote = EmoteType.HEART
        boy.reactionTimer = 3.5f
        girl.reactionTimer = 3.5f
        showMessage(GameText.get(Res.string.scene_a_shooting_star_crossed_the_night_sky_ma), duration = 3.5f)
    }
}
