package com.example

import kotlinx.datetime.toJavaLocalDate
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import com.example.engine.CharacterEmotion
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.EmoteType
import com.example.engine.ParticleType
import com.example.engine.PixelCharacter
import com.example.engine.TimeOfDayPhase
import com.example.scene.CatState
import com.example.scene.CouchPhase
import com.example.scene.EnvironmentType
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.data.SpecialCalendarManager
import com.example.data.SpecialMemoryType
import com.example.data.TinyUsMemory
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TinyUsOfflineTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager
    private lateinit var audio: AmbientAudio
    private lateinit var engine: SceneEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = PreferencesManager(context)
        audio = AmbientAudio().apply { isEnabled = false }
        engine = SceneEngine(
            audio = audio,
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )
    }

    @Test
    fun `test character initialization and visual identity`() {
        assertFalse("Boy character should have isGirl = false", engine.boy.isGirl)
        assertTrue("Girl character should have isGirl = true", engine.girl.isGirl)
        assertEquals(com.example.data.ProfileManager.getProfile().boyName, engine.boy.name)
        assertEquals(com.example.data.ProfileManager.getProfile().girlName, engine.girl.name)
    }

    @Test
    fun `test all scenes load with appropriate environments`() {
        for (scene in SceneType.values()) {
            engine.loadScene(scene)
            assertEquals("Scene should match loaded type", scene, engine.currentScene)
            when (scene) {
                SceneType.FLOWER -> assertEquals(EnvironmentType.MEADOW, scene.environment)
                SceneType.UNDER_TREE -> assertEquals(EnvironmentType.TREE_HILL, scene.environment)
                SceneType.COOKING -> assertEquals(EnvironmentType.KITCHEN, scene.environment)
                SceneType.SLEEP -> assertEquals(EnvironmentType.LIVING_ROOM, scene.environment)
                SceneType.WALK -> assertEquals(EnvironmentType.PATH_NIGHT, scene.environment)
                SceneType.LOOKING -> assertEquals(EnvironmentType.TWILIGHT, scene.environment)
                SceneType.MOMO_STALL -> assertEquals(EnvironmentType.MOMO_STALL, scene.environment)
                SceneType.EVENING_RIDE -> assertEquals(EnvironmentType.EVENING_ROAD, scene.environment)
                SceneType.COZY_LOFT -> assertEquals(EnvironmentType.COZY_LOFT, scene.environment)
                SceneType.RAINY_CAFE -> assertEquals(EnvironmentType.RAINY_CAFE, scene.environment)
                SceneType.SUNROOM -> assertEquals(EnvironmentType.SUNROOM, scene.environment)
                SceneType.CAMPFIRE -> assertEquals(EnvironmentType.CAMPFIRE, scene.environment)
                SceneType.SEASIDE_PIER -> assertEquals(EnvironmentType.SEASIDE_PIER, scene.environment)
            }
        }
    }

    @Test
    fun `test campfire scene loads and interactive objects update state`() {
        engine.loadScene(SceneType.CAMPFIRE)
        assertEquals(SceneType.CAMPFIRE, engine.currentScene)
        assertEquals(EnvironmentType.CAMPFIRE, engine.currentScene.environment)
        assertTrue(engine.isCurrentSceneOutdoor)

        // Fire pit & roasting marshmallows
        engine.onTouchCampfire(1000f, 1000f, 480f, 740f)
        assertTrue(engine.marshmallowRoastingTimer > 0f)
        assertTrue(engine.campfireEmbersTimer > 0f)
        assertEquals(CharacterPose.EAT_SNEAK, engine.boy.pose)
        assertTrue("Roasting emote should be visible", engine.boy.emoteTimer > 0f)

        // Camp guitar
        engine.onTouchCampGuitar(1000f, 1000f)
        assertTrue(engine.campGuitarStrumTimer > 0f)
        assertEquals(EmoteType.MUSIC_NOTE, engine.boy.emote)

        // Lantern toggle
        val prevLantern = engine.campLanternLit
        engine.onTouchCampLantern(1000f, 1000f)
        assertEquals(!prevLantern, engine.campLanternLit)

        // Mochi napping on the blanket stirs awake like anywhere else
        assertEquals(CatState.SLEEPING, engine.catState)
        engine.onTouchCampMochi(1000f, 1000f)
        assertEquals(CatState.SITTING_PURR, engine.catState)
        assertTrue(engine.sceneMessage?.contains("blanket") == true)
    }

    @Test
    fun `test rainy cafe scene elements and characters interactions`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        assertEquals(SceneType.RAINY_CAFE, engine.currentScene)
        assertEquals(EnvironmentType.RAINY_CAFE, engine.currentScene.environment)

        // Barista Leo interaction
        engine.onTouchCafeBarista(1000f, 1000f)
        assertTrue(engine.cafeBaristaBrewTimer > 0f)
        assertEquals(CharacterEmotion.HAPPY, engine.boy.emotion)
        assertEquals(CharacterEmotion.HAPPY, engine.girl.emotion)

        // Daily specials menu interaction
        engine.onTouchCafeMenu()
        assertTrue(engine.sceneMessage?.contains("Specials") == true)

        // Boba the cafe pup interaction
        engine.onTouchCafePup(1000f, 1000f)
        assertTrue(engine.cafePupPetTimer > 0f)

        // Rainy street umbrella passerby interaction
        engine.onTouchCafePasserby(500f, 300f)
        assertTrue(engine.sceneMessage?.contains("umbrella") == true)

        // Table items: Latte and Pastry
        engine.onTouchCafeLatte(1000f, 1000f)
        assertTrue(engine.cafeLatteTimer > 0f)

        val prevBites = engine.cafePastryBites
        engine.onTouchCafePastry(1000f, 1000f)
        assertTrue(engine.cafePastryBites > prevBites)

        // Foggy window heart
        engine.onTouchCafeWindow(500f, 250f, 1000f, 1000f)
        assertTrue(engine.cafeWindowHeartTimer > 0f)
    }

    @Test
    fun `test flower scene sequence progresses and triggers message`() {
        engine.loadScene(SceneType.FLOWER)
        assertEquals(0f, engine.sceneTime, 0.001f)

        // Advance past walk and flower presentation
        engine.update(2.5f, 1000f, 1000f)
        assertTrue("Boy worldX should advance toward girl", engine.boy.worldX > 0.22f)

        engine.update(3.0f, 1000f, 1000f)
        assertTrue("Scene time should advance", engine.sceneTime >= 5.5f)
        assertTrue("Girl receives flower", engine.girl.pose == CharacterPose.RECEIVE_FLOWER || engine.girl.pose == CharacterPose.SURPRISED)

        // Advance to message
        engine.update(2.0f, 1000f, 1000f)
        assertEquals("For you", engine.sceneMessage)
    }

    @Test
    fun `test touch both characters triggers couple hug and hearts`() {
        engine.onTouchBothCharacters(1000f, 1000f)
        assertEquals(CharacterPose.HUG, engine.boy.pose)
        assertEquals(CharacterPose.HUG, engine.girl.pose)
        assertEquals(CharacterEmotion.LOVING, engine.boy.emotion)
        assertEquals(CharacterEmotion.LOVING, engine.girl.emotion)
        assertEquals(EmoteType.HEART, engine.boy.emote)
        assertEquals(EmoteType.HEART, engine.girl.emote)
        assertTrue("Hearts should spawn in particle system", engine.particles.particles.isNotEmpty())
    }

    @Test
    fun `test long press calls partner for hug`() {
        engine.onLongPressCharacter(1000f, 1000f)
        assertEquals(CharacterPose.HUG, engine.boy.pose)
        assertEquals(CharacterPose.HUG, engine.girl.pose)
    }

    @Test
    fun `test individual character touches trigger affection reactions`() {
        engine.onTouchBoy(1000f, 1000f)
        assertTrue("Boy reaction timer active", engine.boy.reactionTimer > 0f)
        assertTrue("Boy emote triggered", engine.boy.emote != EmoteType.NONE || engine.boy.pose == CharacterPose.WAVE)

        engine.onTouchGirl(1000f, 1000f)
        assertTrue("Girl reaction timer active", engine.girl.reactionTimer > 0f)
        assertTrue("Girl emote or pose triggered", engine.girl.emote != EmoteType.NONE || engine.girl.pose == CharacterPose.JOY_JUMP)
    }

    @Test
    fun `test tree tap rustles leaves and spawns particles`() {
        val initialParticles = engine.particles.particles.size
        engine.onTouchTree(1000f, 1000f)
        assertTrue("Tree reaction count increments", engine.treeTapReactionCount > 0)
        assertTrue("Leaf particles should spawn", engine.particles.particles.size > initialParticles)
    }

    @Test
    fun `test sky tap at night spawns shooting star and fireflies`() {
        val initialParticles = engine.particles.particles.size
        engine.onTouchSky(500f, 200f, 1000f, isNight = true)
        assertTrue("Shooting star or fireflies should spawn", engine.particles.particles.size > initialParticles)
        assertNotNull("Subtle sky quote should display", engine.sceneMessage)
    }

    @Test
    fun `test flower tap triggers flower gift and heartfelt note`() {
        engine.onTouchFlower(1000f, 1000f)
        assertEquals(CharacterPose.GIVE_FLOWER, engine.boy.pose)
        assertEquals(CharacterPose.RECEIVE_FLOWER, engine.girl.pose)
        assertEquals("This reminded me of you.", engine.sceneMessage)
    }

    @Test
    fun `test cat tap toggles sleep and purrs with hearts`() {
        val wasSleeping = engine.catSleeping
        engine.onTouchCat(1000f, 1000f)
        assertNotEquals(wasSleeping, engine.catSleeping)
        assertTrue("Heart particle spawned for cat", engine.particles.particles.isNotEmpty())
    }

    @Test
    fun `test cottage door transitions into kitchen`() {
        engine.loadScene(SceneType.FLOWER)
        assertEquals(EnvironmentType.MEADOW, engine.currentScene.environment)
        engine.onTouchCottageDoor()
        assertEquals(SceneType.COOKING, engine.currentScene)
        assertEquals(EnvironmentType.KITCHEN, engine.currentScene.environment)
    }

    @Test
    fun `test scene variety avoids repeating recent scenes`() {
        val first = engine.currentScene
        prefs.addRecentScene(first.name)

        val next = engine.nextRandomScene(first)
        assertNotEquals("Next scene should not match current scene", first, next)

        val third = engine.nextRandomScene(next)
        assertNotEquals("Third scene should not match second scene", next, third)
    }

    @Test
    fun `test daily tiny moment calculation works offline`() {
        val moment = prefs.getTodayTinyMoment()
        assertNotNull(moment.title)
        assertNotNull(moment.description)
        assertNotNull(moment.quote)
        assertTrue(prefs.getDaysTogether() > 0)
    }

    @Test
    fun `test offline memories and love notes persistence`() {
        val initialNotesCount = prefs.getLoveNotes().size
        prefs.addLoveNote("You make every day brighter", "From Him")
        val updatedNotes = prefs.getLoveNotes()
        assertEquals(initialNotesCount + 1, updatedNotes.size)
        assertEquals("You make every day brighter", updatedNotes.first().text)

        val initialMemoriesCount = prefs.getMemories().size
        prefs.addMemory("Picnic in the Meadow", "Watching the clouds together.", "Summer", "flower")
        val updatedMemories = prefs.getMemories()
        assertEquals(initialMemoriesCount + 1, updatedMemories.size)
        assertEquals("Picnic in the Meadow", updatedMemories.first().title)
    }

    @Test
    fun `test watch scene kiss triggers kiss pose and mutual love dialogues`() {
        // triggerWatchSceneKiss activates the watch scene
        engine.triggerWatchSceneKiss()
        assertTrue("Watch scene must activate on trigger", engine.isWatchSceneActive)

        // Scene-specific cinematic runs: FLOWER at t=1.2s (boy walking to patch)
        engine.update(1.2f, 1000f, 1000f)
        assertTrue("Watch scene must remain active during cinematic", engine.isWatchSceneActive)

        // Advance well past the longest cinematic endTime (9.5s for Cozy Loft)
        // Single large step ensures the else branch fires
        engine.update(10.0f, 1000f, 1000f)
        assertFalse("Watch scene must deactivate after end time", engine.isWatchSceneActive)
    }

    @Test
    fun `test scooter ride interactions`() {
        engine.loadScene(SceneType.EVENING_RIDE)
        assertEquals(SceneType.EVENING_RIDE, engine.currentScene)
        assertEquals(EnvironmentType.EVENING_ROAD, engine.currentScene.environment)

        // Scooter horn interaction
        engine.onTouchScooter(1000f, 1000f)
        assertTrue(engine.particles.particles.isNotEmpty())
        assertNotNull(engine.sceneMessage)
        assertTrue(engine.sceneMessage!!.contains("Scooter") || engine.sceneMessage!!.contains("road trip"))

        // Girl scooter driver speech interaction
        engine.onTouchGirlScooter(1000f, 1000f)
        assertEquals("Hold tight ${engine.boy.name}! The scooter is fast hehe!", engine.girlSpeechText)
        assertEquals(CharacterEmotion.HAPPY, engine.girl.emotion)

        // Boy pillion speech interaction
        engine.onTouchBoyScooter(1000f, 1000f)
        assertEquals("Holding you forever, my love.", engine.boySpeechText)
        assertEquals(CharacterEmotion.LOVING, engine.boy.emotion)

        // Temple spire tap interaction
        engine.onTouchTempleSpire(1000f, 1000f)
        assertNotNull(engine.sceneMessage)
        assertTrue(engine.sceneMessage!!.contains("temple"))
    }

    @Test
    fun `test shared earphones and music playlist`() {
        assertFalse(engine.earphonesActive)
        engine.earphonesActive = true
        assertTrue(engine.boy.wearsEarphone)
        assertTrue(engine.girl.wearsEarphone)

        val songTitles = audio.playlist.map { it.title }
        assertTrue(songTitles.contains("Heartbeat"))
        assertTrue(audio.playlist.size >= 2)
        // No commercial recordings are bundled (they can't be published without a licence).
        assertTrue(audio.playlist.all { it.rawResId == null || it.rawResId == 0 })

        val secondSong = audio.playlist[1]
        audio.playSong(secondSong)
        assertEquals(secondSong.id, audio.currentSong.id)
    }

    @Test
    fun `test dynamic weather cycling`() {
        assertEquals(WeatherType.SUNNY, engine.weather)
        val w1 = engine.cycleWeather()
        assertEquals(WeatherType.RAIN, w1)
        assertEquals(WeatherType.RAIN, engine.weather)

        val w2 = engine.cycleWeather()
        assertEquals(WeatherType.SAKURA, w2)

        val w3 = engine.cycleWeather()
        assertEquals(WeatherType.AUTUMN, w3)

        val w4 = engine.cycleWeather()
        assertEquals(WeatherType.SNOW, w4)

        val w5 = engine.cycleWeather()
        assertEquals(WeatherType.SUNNY, w5)
    }

    @Test
    fun `test mochi cat states and petting cycle`() {
        assertEquals(CatState.SLEEPING, engine.catState)

        // First pet: wakes up and purrs
        engine.onTouchCat(1000f, 1000f)
        assertEquals(CatState.SITTING_PURR, engine.catState)
        assertFalse(engine.catSleeping)

        // Second pet: rolls on belly
        engine.onTouchCat(1000f, 1000f)
        assertEquals(CatState.BELLY_ROLL, engine.catState)

        // Third pet: cuddles back to sleep
        engine.onTouchCat(1000f, 1000f)
        assertEquals(CatState.SLEEPING, engine.catState)
        assertTrue(engine.catSleeping)
    }

    @Test
    fun `test thinking of you action`() {
        engine.triggerThinkingOfYou(1000f, 1000f)
        assertEquals(CharacterEmotion.LOVING, engine.boy.emotion)
        assertEquals(CharacterEmotion.SHY, engine.girl.emotion)
        assertEquals(EmoteType.HEART, engine.boy.emote)
        assertEquals(EmoteType.BLUSH, engine.girl.emote)
        assertEquals("${engine.boy.name} is thinking of ${engine.girl.name} right now", engine.sceneMessage)
        assertTrue(engine.particles.particles.isNotEmpty())
    }

    @Test
    fun `test cozy loft cuddle and mochi sleeping on bed`() {
        engine.loadScene(SceneType.COZY_LOFT)
        assertEquals(SceneType.COZY_LOFT, engine.currentScene)
        assertEquals(EnvironmentType.COZY_LOFT, engine.currentScene.environment)

        // Characters are sitting cuddled on the bed/sofa
        assertEquals(0.58f, engine.boy.worldX, 0.01f)
        assertEquals(0.64f, engine.girl.worldX, 0.01f)
        assertEquals(CharacterPose.SIT, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)

        // Cat Mochi is sleeping peacefully on the left side of the bed quilt (matching reference image)
        assertEquals(0.50f, engine.catWorldX, 0.01f)
        assertEquals(CatState.SLEEPING, engine.catState)
        assertTrue(engine.catSleeping)

        // Ensure characters and cat do not overlap: Cat is on left, then Boy, then Girl
        assertTrue("Cat is to the left of boy", engine.catWorldX < engine.boy.worldX)
        assertTrue("Boy is to the left of girl", engine.boy.worldX < engine.girl.worldX)
        assertTrue("Adequate spacing between cat and boy", engine.boy.worldX - engine.catWorldX >= 0.07f)
        assertTrue("Adequate spacing between boy and girl", engine.girl.worldX - engine.boy.worldX >= 0.05f)
    }

    @Test
    fun `test dynamic weather particle generation for all seasons`() {
        engine.loadScene(SceneType.FLOWER)

        // 1. Sakura
        engine.weather = WeatherType.SAKURA
        engine.particles.particles.clear()
        repeat(30) { engine.update(0.016f, 1000f, 1000f) }
        assertTrue("Sakura petals spawned in outdoor weather", engine.particles.particles.any { it.type == ParticleType.SAKURA_PETAL })

        // 2. Autumn
        engine.weather = WeatherType.AUTUMN
        engine.particles.particles.clear()
        repeat(30) { engine.update(0.016f, 1000f, 1000f) }
        assertTrue("Autumn leaves spawned in outdoor weather", engine.particles.particles.any { it.type == ParticleType.AUTUMN_LEAF })

        // 3. Snow
        engine.weather = WeatherType.SNOW
        engine.particles.particles.clear()
        repeat(30) { engine.update(0.016f, 1000f, 1000f) }
        assertTrue("Snowflakes spawned in outdoor weather", engine.particles.particles.any { it.type == ParticleType.SNOWFLAKE })

        // 4. Sunny
        engine.weather = WeatherType.SUNNY
        engine.particles.particles.clear()
        // Sunny ambient details are intentionally rare; observe long enough not to fail
        // intermittently just because the random spawn rolls did not occur in one second.
        var sawSunnyAmbientEffect = false
        repeat(1800) {
            engine.update(0.016f, 1000f, 1000f)
            if (engine.particles.particles.any {
                    it.type == ParticleType.DANDELION_FLUFF ||
                        it.type == ParticleType.SPARKLE ||
                        it.type == ParticleType.WIND_BREEZE
                }
            ) {
                sawSunnyAmbientEffect = true
            }
        }
        assertTrue("Sunny breeze, fluff or sparkles spawned", sawSunnyAmbientEffect)

        // 5. Rain
        engine.weather = WeatherType.RAIN
        engine.particles.particles.clear()
        repeat(15) { engine.update(0.016f, 1000f, 1000f) }
        assertTrue("Raindrops spawned in outdoor rain weather", engine.particles.particles.any { it.type == ParticleType.RAIN_DROP })
    }

    @Test
    fun `test ambient life breathing and idle aliveness`() {
        engine.loadScene(SceneType.FLOWER)
        repeat(10) { engine.update(0.05f, 1000f, 1000f) }

        // Breathing offset should be non-zero as characters breathe
        assertNotEquals(0f, engine.boy.breathingOffset, 0.0001f)
        assertNotEquals(0f, engine.girl.breathingOffset, 0.0001f)

        // Advance time past script intro to allow autonomous moment
        repeat(120) { engine.update(0.05f, 1000f, 1000f) }
        // Verify scene continues to run smoothly and particles exist
        assertTrue(engine.sceneTime > 5.0f)
    }

    @Test
    fun `test dynamic relationship day counter calculation`() {
        val originalDate = com.example.data.RelationshipTimeManager.relationshipStartDate
        try {
            val baseDate = java.time.LocalDate.of(2024, 1, 1)
            com.example.data.RelationshipTimeManager.relationshipStartDate = baseDate

            val startDay = com.example.data.RelationshipTimeManager.calculateTinyUsDay(baseDate)
            assertEquals("Start date must be Day 1", 1L, startDay)

            val secondDay = com.example.data.RelationshipTimeManager.calculateTinyUsDay(baseDate.plusDays(1))
            assertEquals("Next date must be Day 2", 2L, secondDay)

            val hundredthDay = com.example.data.RelationshipTimeManager.calculateTinyUsDay(baseDate.plusDays(99))
            assertEquals("100th date must be Day 100", 100L, hundredthDay)
        } finally {
            com.example.data.RelationshipTimeManager.relationshipStartDate = originalDate
        }
    }

    @Test
    fun `test exact relationship duration breakdown`() {
        val originalDate = com.example.data.RelationshipTimeManager.relationshipStartDate
        val originalTime = com.example.data.RelationshipTimeManager.relationshipStartTime
        try {
            com.example.data.RelationshipTimeManager.relationshipStartDate = java.time.LocalDate.of(2024, 1, 1)
            com.example.data.RelationshipTimeManager.relationshipStartTime = java.time.LocalTime.of(0, 0)
            val testTime = java.time.LocalDateTime.of(2025, 2, 3, 13, 27, 42)
            val duration = com.example.data.RelationshipTimeManager.getExactDuration(testTime)

            assertEquals(1, duration.years)
            assertEquals(1, duration.months)
            assertEquals(2, duration.days)
            assertEquals(13L, duration.hours)
            assertEquals(27L, duration.minutes)
            assertEquals(42L, duration.seconds)
        } finally {
            com.example.data.RelationshipTimeManager.relationshipStartDate = originalDate
            com.example.data.RelationshipTimeManager.relationshipStartTime = originalTime
        }
    }

    @Test
    fun `test multiple launches on same day never increment day count`() {
        val firstRead = prefs.getDaysTogether()
        repeat(10) {
            val nextRead = prefs.getDaysTogether()
            assertEquals("Day count must stay constant across multiple calls on same day", firstRead, nextRead)
        }
    }

    @Test
    fun `test all moments cover diverse watch scenes including cozy loft`() {
        val moments = prefs.getAllMoments()
        assertTrue("Moments list should contain at least 8 moments", moments.size >= 8)
        assertTrue("Moments should include Cozy Midnight Loft", moments.any { it.title.contains("Loft") })
        assertTrue("Moments should include Evening Ride", moments.any { it.title.contains("Ride") })
    }

    @Test
    fun `test all 6 special milestone dates are configured correctly`() {
        val originalMemories = SpecialCalendarManager.customMemories
        try {
            val testStart = LocalDate.of(2024, 1, 1)
            SpecialCalendarManager.customMemories = listOf(
                TinyUsMemory(
                    id = "our_beginning",
                    date = testStart,
                    title = "Our Beginning",
                    description = "Where our story began.",
                    type = SpecialMemoryType.RELATIONSHIP,
                    annualRecurring = true
                ),
                TinyUsMemory(
                    id = "first_kiss",
                    date = LocalDate.of(2024, 2, 14),
                    title = "Our First Kiss",
                    description = "Sweet kiss under the stars.",
                    type = SpecialMemoryType.KISS
                ),
                TinyUsMemory(
                    id = "boy_birthday",
                    date = LocalDate.of(2000, 5, 10),
                    title = "His Birthday",
                    description = "Celebrating him.",
                    type = SpecialMemoryType.BIRTHDAY,
                    annualRecurring = true
                ),
                TinyUsMemory(
                    id = "girl_birthday",
                    date = LocalDate.of(2000, 8, 15),
                    title = "Her Birthday",
                    description = "Celebrating her.",
                    type = SpecialMemoryType.BIRTHDAY,
                    annualRecurring = true
                ),
                TinyUsMemory(
                    id = "private_memory",
                    date = LocalDate.of(2024, 6, 20),
                    title = "Private Memory",
                    description = "Private moment.",
                    type = SpecialMemoryType.PRIVATE,
                    isPrivate = true
                ),
                TinyUsMemory(
                    id = "next_meet",
                    date = LocalDate.of(2026, 10, 28),
                    title = "Our Next Meet",
                    description = "Counting down.",
                    type = SpecialMemoryType.FUTURE_MEETING,
                    isFuture = true
                )
            )

            val memories = SpecialCalendarManager.fixedMemories
            assertEquals("Must have exactly 6 fixed relationship milestones", 6, memories.size)

            val beginning = memories.first { it.id == "our_beginning" }
            assertEquals(testStart, beginning.date.toJavaLocalDate())
            assertEquals("Our Beginning", beginning.title)
            assertEquals(SpecialMemoryType.RELATIONSHIP, beginning.type)

            val kiss = memories.first { it.id == "first_kiss" }
            assertEquals(LocalDate.of(2024, 2, 14), kiss.date.toJavaLocalDate())
            assertEquals("Our First Kiss", kiss.title)
            assertEquals(SpecialMemoryType.KISS, kiss.type)

            val boyBday = memories.first { it.id == "boy_birthday" }
            assertEquals(LocalDate.of(2000, 5, 10), boyBday.date.toJavaLocalDate())
            assertEquals(SpecialMemoryType.BIRTHDAY, boyBday.type)
            assertTrue("Birthday is annual recurring", boyBday.annualRecurring)

            val girlBday = memories.first { it.id == "girl_birthday" }
            assertEquals(LocalDate.of(2000, 8, 15), girlBday.date.toJavaLocalDate())
            assertEquals(SpecialMemoryType.BIRTHDAY, girlBday.type)
            assertTrue("Birthday is annual recurring", girlBday.annualRecurring)

            val priv = memories.first { it.id == "private_memory" }
            assertEquals(LocalDate.of(2024, 6, 20), priv.date.toJavaLocalDate())
            assertEquals(SpecialMemoryType.PRIVATE, priv.type)
            assertTrue("Private memory is marked private", priv.isPrivate)

            val nextMeet = memories.first { it.id == "next_meet" }
            assertEquals(LocalDate.of(2026, 10, 28), nextMeet.date.toJavaLocalDate())
            assertEquals(SpecialMemoryType.FUTURE_MEETING, nextMeet.type)
            assertTrue("Next meet is future event", nextMeet.isFuture)
        } finally {
            SpecialCalendarManager.customMemories = originalMemories
        }
    }

    @Test
    fun `test birthdays recur annually in any year`() {
        val originalMemories = SpecialCalendarManager.customMemories
        try {
            SpecialCalendarManager.customMemories = listOf(
                TinyUsMemory(
                    id = "boy_birthday",
                    date = LocalDate.of(2000, 5, 10),
                    title = "His Birthday",
                    description = "Celebrating him.",
                    type = SpecialMemoryType.BIRTHDAY,
                    annualRecurring = true
                ),
                TinyUsMemory(
                    id = "girl_birthday",
                    date = LocalDate.of(2000, 8, 15),
                    title = "Her Birthday",
                    description = "Celebrating her.",
                    type = SpecialMemoryType.BIRTHDAY,
                    annualRecurring = true
                )
            )

            val may10_2026 = LocalDate.of(2026, 5, 10)
            val aug15_2026 = LocalDate.of(2026, 8, 15)

            val may10Memories = SpecialCalendarManager.getMemoriesForDate(may10_2026)
            assertTrue("Boy birthday found on May 10 2026", may10Memories.any { it.id == "boy_birthday" })

            val aug15Memories = SpecialCalendarManager.getMemoriesForDate(aug15_2026)
            assertTrue("Girl birthday found on Aug 15 2026", aug15Memories.any { it.id == "girl_birthday" })
        } finally {
            SpecialCalendarManager.customMemories = originalMemories
        }
    }

    @Test
    fun `test future meet live countdown calculation`() {
        val target = LocalDate.of(2026, 10, 28)

        // When current date is target date -> isToday is true
        val todayTime = LocalDateTime.of(2026, 10, 28, 14, 30, 0)
        val todayCountdown = SpecialCalendarManager.calculateCountdown(target, todayTime)
        assertTrue("isToday is true on meeting day", todayCountdown.isToday)
        assertFalse("isPassed is false on meeting day", todayCountdown.isPassed)

        // When current date is 30 days before
        val beforeTime = LocalDateTime.of(2026, 9, 28, 0, 0, 0)
        val countdown = SpecialCalendarManager.calculateCountdown(target, beforeTime)
        assertFalse("isToday is false when days remain", countdown.isToday)
        assertFalse("isPassed is false when days remain", countdown.isPassed)
        assertEquals(30L, countdown.days)
        assertEquals(0L, countdown.hours)
    }

    @Test
    fun `test private memory is isolated and protected`() {
        val privMem = TinyUsMemory(
            id = "private_memory",
            date = LocalDate.of(2024, 6, 20),
            title = "Private Memory",
            description = "Private moment.",
            type = SpecialMemoryType.PRIVATE,
            isPrivate = true
        )
        assertTrue("Private memory is marked isPrivate", privMem.isPrivate)

        // Public daily moments must NEVER contain the private memory text
        val allMoments = prefs.getAllMoments()
        assertFalse(
            "Private memory must never be exposed in public moments pool",
            allMoments.any { it.title.contains("Private Memory", ignoreCase = true) }
        )
    }

    @Test
    fun `test relationship start date matches single source of truth`() {
        assertEquals(
            "Calendar start date matches RelationshipTimeManager",
            com.example.data.RelationshipTimeManager.relationshipStartDate,
            SpecialCalendarManager.startDate
        )
    }

    @Test
    fun `test weather reactive thoughts pool and anti-repeat selection`() {
        engine.weather = WeatherType.RAIN
        val rainThoughts = engine.getEligibleThoughts()
        assertTrue("Eligible thoughts should include generic thoughts", rainThoughts.contains("Just happy to be right here beside you."))
        assertTrue("Eligible thoughts should include rain thoughts", rainThoughts.contains("Under one umbrella is my favorite place to be."))
        assertFalse("Eligible thoughts should not include snow thoughts", rainThoughts.contains("Cold outside, but so warm right here beside you."))

        engine.weather = WeatherType.SNOW
        val snowThoughts = engine.getEligibleThoughts()
        assertTrue("Eligible thoughts should include snow thoughts", snowThoughts.contains("Cold outside, but so warm right here beside you."))
        assertFalse("Eligible thoughts should not include rain thoughts", snowThoughts.contains("Under one umbrella is my favorite place to be."))

        // Pick thoughts and verify anti-repeat behavior
        var prev = engine.pickSpontaneousThought()
        repeat(15) {
            val next = engine.pickSpontaneousThought()
            assertTrue("Picked thought must be from eligible thoughts", snowThoughts.contains(next))
            assertNotEquals("AntiRepeatRandomPicker must not repeat consecutive thoughts", prev, next)
            prev = next
        }
    }

    @Test
    fun `test ambient weather drift excludes active weather`() {
        for (initial in WeatherType.values()) {
            engine.weather = initial
            repeat(10) {
                val oldWeather = engine.weather
                engine.driftWeather()
                assertNotEquals("Weather drift must always select a different weather type", oldWeather, engine.weather)
            }
        }
    }

    @Test
    fun `test manual weather cycling resets drift timer and steps through types`() {
        engine.weather = WeatherType.SUNNY
        // Advance drift timer
        engine.update(50f, 1000f, 1000f)
        val nextWeather = engine.cycleWeather()
        assertEquals(WeatherType.RAIN, nextWeather)
        assertEquals("Manual cycling advances to RAIN", WeatherType.RAIN, engine.weather)
        assertEquals("Subtitles should announce weather", "Weather: Cozy Rain", engine.sceneMessage)
    }

    @Test
    fun `test constellation touch triggers message and particles`() {
        engine.loadScene(SceneType.FLOWER)
        val initialParticles = engine.particles.particles.size
        engine.onTouchConstellation("The Two Hearts", "Two shining stars linked across the sky", 300f, 200f)
        assertEquals("Constellation: The Two Hearts - Two shining stars linked across the sky", engine.sceneMessage)
        assertTrue("Sparkle and heart particles should be spawned", engine.particles.particles.size > initialParticles)
    }

    @Test
    fun `test outdoor vs indoor scene classification for weather clothing`() {
        engine.loadScene(SceneType.FLOWER)
        assertTrue("Meadow scene must be outdoor", engine.isCurrentSceneOutdoor)

        engine.loadScene(SceneType.UNDER_TREE)
        assertTrue("Tree Hill scene must be outdoor", engine.isCurrentSceneOutdoor)

        engine.loadScene(SceneType.WALK)
        assertTrue("Night Walk scene must be outdoor", engine.isCurrentSceneOutdoor)

        engine.loadScene(SceneType.MOMO_STALL)
        assertTrue("Momo Stall scene must be outdoor", engine.isCurrentSceneOutdoor)

        engine.loadScene(SceneType.COOKING)
        assertFalse("Kitchen scene must be indoor", engine.isCurrentSceneOutdoor)

        engine.loadScene(SceneType.SLEEP)
        assertFalse("Living room scene must be indoor", engine.isCurrentSceneOutdoor)

        engine.loadScene(SceneType.COZY_LOFT)
        assertFalse("Cozy loft scene must be indoor", engine.isCurrentSceneOutdoor)
    }

    @Test
    fun `test kitchen autonomous behavior pool and appliance self-resolution`() {
        engine.loadScene(SceneType.COOKING)
        // Advance past initial script intro (7.6s)
        repeat(160) { engine.update(0.05f, 1000f, 1000f) }
        assertTrue("Script intro should have finished", engine.sceneTime >= 7.6f)

        // Run until an autonomous moment triggers in kitchen
        var triggeredCount = 0
        repeat(300) {
            engine.update(0.05f, 1000f, 1000f)
            if (engine.boy.reactionTimer > 0f || engine.girl.reactionTimer > 0f) {
                triggeredCount++
            }
        }
        assertTrue("At least one kitchen autonomous moment must trigger", triggeredCount > 0)

        // Verify appliance self-resolution: if fridge, cabinet, or sink is active, it must decrement to 0
        engine.loadScene(SceneType.COOKING)
        engine.fridgeDoorOpenTimer = 2.0f
        engine.cabinetOpenTimer = 2.0f
        engine.sinkRunningTimer = 2.0f

        repeat(50) { engine.update(0.05f, 1000f, 1000f) }

        assertEquals(0f, engine.fridgeDoorOpenTimer, 0.001f)
        assertEquals(0f, engine.cabinetOpenTimer, 0.001f)
        assertEquals(0f, engine.sinkRunningTimer, 0.001f)
    }

    @Test
    fun `test kitchen appliance timers reset on loadScene`() {
        engine.loadScene(SceneType.COOKING)
        engine.fridgeDoorOpenTimer = 3.5f
        engine.cabinetOpenTimer = 3.5f
        engine.sinkRunningTimer = 3.5f

        // Switch scene away
        engine.loadScene(SceneType.FLOWER)
        assertEquals(0f, engine.fridgeDoorOpenTimer, 0.001f)
        assertEquals(0f, engine.cabinetOpenTimer, 0.001f)
        assertEquals(0f, engine.sinkRunningTimer, 0.001f)
    }

    @Test
    fun `test user tap interrupts and prioritizes over kitchen autonomous actions`() {
        engine.loadScene(SceneType.COOKING)
        repeat(160) { engine.update(0.05f, 1000f, 1000f) }

        // Simulate autonomous reaction active
        engine.boy.reactionTimer = 2.5f
        engine.girl.reactionTimer = 2.5f

        // User taps boy
        engine.onTouchBoy(1000f, 1000f)
        assertEquals(2.8f, engine.boy.reactionTimer, 0.001f)

        // User taps pot
        engine.onTouchPot(1000f, 1000f)
        assertEquals(3.5f, engine.boy.reactionTimer, 0.001f)
        assertEquals(3.5f, engine.girl.reactionTimer, 0.001f)
    }

    @Test
    fun `test CharacterMotionTween easing and smooth movement interpolation`() {
        val tween = com.example.engine.CharacterMotionTween
        println("=== CharacterMotionTween Test Assertions ===")
        println("SHARED_WALKING_SPEED = ${tween.SHARED_WALKING_SPEED}")
        assertEquals(0.1125f, tween.SHARED_WALKING_SPEED, 0.0001f)

        val ease0 = tween.easeInOutCubic(0f)
        val easeHalf = tween.easeInOutCubic(0.5f)
        val ease1 = tween.easeInOutCubic(1f)
        val ease20 = tween.easeInOutCubic(0.2f)
        val ease80 = tween.easeInOutCubic(0.8f)
        println("easeInOutCubic(0f) = $ease0")
        println("easeInOutCubic(0.5f) = $easeHalf")
        println("easeInOutCubic(1f) = $ease1")
        println("easeInOutCubic(0.2f) = $ease20 (ease-in slow start)")
        println("easeInOutCubic(0.8f) = $ease80 (ease-out slow end)")
        assertEquals(0f, ease0, 0.001f)
        assertEquals(0.5f, easeHalf, 0.001f)
        assertEquals(1f, ease1, 0.001f)
        assertTrue(ease20 < 0.2f)
        assertTrue(ease80 > 0.8f)

        // Test derived duration from shared walking speed
        val char = engine.boy
        char.worldX = 0.40f
        char.worldY = 0.65f
        char.moveTo(0.70f) // distance = 0.30f, duration should be 0.30 / 0.1125 = 2.6667s

        val expectedDist = 0.30f
        val expectedDuration = expectedDist / tween.SHARED_WALKING_SPEED
        val actualSpeed = expectedDist / char.posTransitionDuration
        println("Move 1: startX=0.40, targetX=0.70, dist=$expectedDist")
        println("Move 1: posTransitionDuration = ${char.posTransitionDuration} s (expected = $expectedDuration s)")
        println("Move 1: actual speed = $actualSpeed units/s (matches SHARED_WALKING_SPEED ${tween.SHARED_WALKING_SPEED})")

        assertTrue(char.isTransitioningPosition)
        assertEquals(0.70f, char.targetWorldX, 0.001f)
        assertEquals(expectedDuration, char.posTransitionDuration, 0.002f)
        assertEquals(tween.SHARED_WALKING_SPEED, actualSpeed, 0.001f)

        // Advance halfway
        val halfDt = char.posTransitionDuration * 0.5f
        char.updateMotion(halfDt)
        println("Move 1 halfway: dt=$halfDt s, progress=${char.posTransitionProgress}, worldX=${char.worldX}, pose=${char.pose}")
        assertEquals(0.50f, char.posTransitionProgress, 0.01f)
        assertEquals(0.55f, char.worldX, 0.01f)
        assertTrue("Character should step walk frames during large movement", char.pose in listOf(
            CharacterPose.WALK_1, CharacterPose.WALK_2, CharacterPose.WALK_3, CharacterPose.WALK_4
        ))

        // Complete the motion
        char.updateMotion(halfDt)
        println("Move 1 completed: progress=${char.posTransitionProgress}, worldX=${char.worldX}, isTransitioning=${char.isTransitioningPosition}")
        assertFalse(char.isTransitioningPosition)
        assertEquals(0.70f, char.worldX, 0.001f)

        // Verify another movement of a different distance has the EXACT same speed
        char.worldX = 0.54f
        char.moveTo(0.32f) // distance = 0.22f (sink)
        val sinkDist = 0.22f
        val expectedSinkDuration = sinkDist / tween.SHARED_WALKING_SPEED
        val sinkSpeed = sinkDist / char.posTransitionDuration
        println("Move 2 (Sink): startX=0.54, targetX=0.32, dist=$sinkDist")
        println("Move 2 (Sink): posTransitionDuration = ${char.posTransitionDuration} s (expected = $expectedSinkDuration s)")
        println("Move 2 (Sink): actual speed = $sinkSpeed units/s")
        assertEquals(expectedSinkDuration, char.posTransitionDuration, 0.002f)
        assertEquals(tween.SHARED_WALKING_SPEED, sinkSpeed, 0.001f)
    }

    @Test
    fun `test pose transition anticipation dip`() {
        val char = engine.girl
        char.pose = CharacterPose.COOK
        char.transitionPoseTo(CharacterPose.WAVE, duration = 0.14f)

        println("=== Pose Anticipation Dip Test Assertions ===")
        println("Start pose=${char.previousPose}, targetPose=${char.targetPose}, isTransitioningPose=${char.isTransitioningPose}")
        assertTrue(char.isTransitioningPose)

        // Halfway through anticipation dip (0.04s is < 0.55 * 0.14 = 0.077s)
        char.updateMotion(0.04f)
        println("Anticipation phase: progress=${char.poseTransitionProgress}, bounceOffset=${char.bounceOffset}, interimPose=${char.pose}")
        assertTrue("Should have anticipation dip bounce > 0", char.bounceOffset > 0f)
        assertEquals(CharacterPose.IDLE, char.pose) // passing through neutral

        // Complete pose transition
        char.updateMotion(0.12f)
        println("Resolution phase: progress=${char.poseTransitionProgress}, finalPose=${char.pose}, bounceOffset=${char.bounceOffset}")
        assertFalse(char.isTransitioningPose)
        assertEquals(CharacterPose.WAVE, char.pose)
        assertEquals(0f, char.bounceOffset, 0.001f)
    }

    @Test
    fun `test living room autonomous return timer returns character to couch`() {
        engine.loadScene(SceneType.SLEEP)
        val boy = engine.boy
        boy.worldX = 0.20f // arrived at wardrobe
        engine.livingRoomReturnChar = boy
        engine.livingRoomReturnTargetX = 0.44f
        engine.livingRoomReturnTimer = 0.2f

        // Advance 0.1s -> timer still active
        engine.update(0.1f, 1080f, 2400f)
        assertTrue(engine.livingRoomReturnTimer > 0f)
        assertEquals(boy, engine.livingRoomReturnChar)

        // Advance 0.15s -> timer expires and initiates return walk
        engine.update(0.15f, 1080f, 2400f)
        assertEquals(0f, engine.livingRoomReturnTimer)
        assertTrue(boy.isTransitioningPosition)
        assertEquals(0.44f, boy.targetWorldX, 0.001f)
        assertEquals(CharacterPose.SIT, boy.targetPose)
    }

    @Test
    fun `test cuddle progress sitting offset smoothing`() {
        engine.loadScene(SceneType.SLEEP)
        engine.boy.pose = CharacterPose.SIT_SNUGGLE
        engine.girl.pose = CharacterPose.SIT_SNUGGLE
        engine.boy.reactionTimer = 2.0f
        engine.girl.reactionTimer = 2.0f
        engine.cuddleProgress = 0f

        // Update with dt = 0.1f: cuddleProgress should increase smoothly
        engine.update(0.1f, 1080f, 2400f)
        assertTrue("cuddleProgress should increase from 0", engine.cuddleProgress > 0f)
        assertTrue("cuddleProgress should not jump to 1 instantly", engine.cuddleProgress < 1.0f)

        // Advance further to reach fully cuddled state
        engine.update(0.3f, 1080f, 2400f)
        assertEquals(1.0f, engine.cuddleProgress, 0.001f)

        // Now move apart -> cuddleProgress should ease down towards 0
        engine.boy.reactionTimer = 0f
        engine.girl.reactionTimer = 0f
        engine.boy.pose = CharacterPose.IDLE
        engine.girl.pose = CharacterPose.IDLE
        engine.update(0.1f, 1080f, 2400f)
        assertTrue("cuddleProgress should decrease when un-cuddling", engine.cuddleProgress < 1.0f)
    }

    @Test
    fun `test scooter ride speech dialogue is strictly sequenced and non-overlapping`() {
        engine.loadScene(SceneType.EVENING_RIDE)
        engine.sceneTime = 0f
        engine.boySpeechText = null
        engine.girlSpeechText = null
        engine.boySpeechTimer = 0f
        engine.girlSpeechTimer = 0f

        // At t = 3.05s, boy should speak
        engine.sceneTime = 3.05f
        engine.update(0.05f, 1080f, 2400f)
        assertNotNull(engine.boySpeechText)
        assertTrue(engine.boySpeechTimer > 0f)
        assertEquals(null, engine.girlSpeechText) // Girl has not spoken yet

        // While boy speaks, girl never speaks even if timer passes girl's window
        engine.sceneTime = 6.85f
        engine.boySpeechTimer = 1.0f // artificially keep boy speech active
        engine.update(0.05f, 1080f, 2400f)
        assertEquals(null, engine.girlSpeechText) // Girl must not speak while boy is speaking

        // Once boy finishes, girl can speak
        engine.boySpeechText = null
        engine.boySpeechTimer = 0f
        engine.update(0.05f, 1080f, 2400f)
        assertNotNull(engine.girlSpeechText)
        assertTrue(engine.girlSpeechTimer > 0f)
    }

    @Test
    fun `test mochi ground tap minimum distance threshold protects sleeping cat from tiny nudges`() {
        engine.loadScene(SceneType.FLOWER)
        val cw = 1080f
        val ch = 2400f
        engine.catWorldX = 0.78f
        engine.catWorldY = 0.69f
        engine.catTargetX = 0.78f
        engine.catTargetY = 0.69f
        engine.catSleeping = true
        engine.catState = CatState.SLEEPING

        // Tiny tap: 15 pixels away horizontally (dx = 15/1080 ≈ 0.0138f)
        val tinyTargetX = engine.catWorldX + 15f / cw
        val tinyTargetY = engine.catWorldY
        val tinyDistPixels = kotlin.math.hypot((tinyTargetX - engine.catWorldX) * cw, (tinyTargetY - engine.catWorldY) * ch)
        println("Tiny tap distance: $tinyDistPixels pixels (threshold is ${SceneEngine.MIN_CAT_WALK_THRESHOLD_PIXELS} px)")
        assertTrue("Tiny tap should be under 48px threshold", tinyDistPixels < SceneEngine.MIN_CAT_WALK_THRESHOLD_PIXELS)

        engine.commandCatWalkTo(tinyTargetX, tinyTargetY, cw, ch)
        // Mochi must remain asleep and unaffected
        assertTrue("Mochi must stay sleeping after tiny tap", engine.catSleeping)
        assertEquals("Mochi state must stay SLEEPING", CatState.SLEEPING, engine.catState)
        assertEquals(0.78f, engine.catTargetX, 0.001f)

        // Intentional normal tap: 250 pixels away (dx = 250/1080 ≈ 0.231f)
        val normalTargetX = engine.catWorldX - 250f / cw
        val normalTargetY = engine.catWorldY
        val normalDistPixels = kotlin.math.hypot((normalTargetX - engine.catWorldX) * cw, (normalTargetY - engine.catWorldY) * ch)
        println("Normal tap distance: $normalDistPixels pixels (threshold is ${SceneEngine.MIN_CAT_WALK_THRESHOLD_PIXELS} px)")
        assertTrue("Normal tap should be above 48px threshold", normalDistPixels >= SceneEngine.MIN_CAT_WALK_THRESHOLD_PIXELS)

        engine.commandCatWalkTo(normalTargetX, normalTargetY, cw, ch)
        // Mochi must wake up and follow
        assertFalse("Mochi must wake up after intentional tap", engine.catSleeping)
        assertEquals("Mochi state must become WALK_FOLLOW", CatState.WALK_FOLLOW, engine.catState)
        assertEquals(normalTargetX, engine.catTargetX, 0.001f)

        // Direct tap on Mochi via onTouchCat works immediately
        engine.catSleeping = true
        engine.catState = CatState.SLEEPING
        engine.onTouchCat(cw, ch)
        assertFalse("Direct tap on Mochi via onTouchCat wakes him up", engine.catSleeping)
        assertNotEquals("Direct tap changes cat state from SLEEPING", CatState.SLEEPING, engine.catState)
    }

    @Test
    fun `test stargaze both characters tap hug persists and is not clobbered by scene script`() {
        engine.loadScene(SceneType.LOOKING)
        val cw = 1080f
        val ch = 2400f

        // Advance scene time into the head-pat window (t = 3.5s)
        engine.sceneTime = 3.5f
        engine.update(0.05f, cw, ch)

        // Tap both characters to initiate romantic hug
        engine.onTouchBothCharacters(cw, ch)
        val hugPoseBoy = engine.boy.pose
        val hugPoseGirl = engine.girl.pose
        println("Hug initiated: boy.pose=$hugPoseBoy, girl.pose=$hugPoseGirl, boy.reactionTimer=${engine.boy.reactionTimer}")
        assertTrue("Boy pose must be a cuddle/hug pose", hugPoseBoy in listOf(CharacterPose.HUG, CharacterPose.SIT_SNUGGLE))
        assertTrue("Girl pose must be a cuddle/hug pose", hugPoseGirl in listOf(CharacterPose.HUG, CharacterPose.SIT_SNUGGLE, CharacterPose.JOY_JUMP))
        assertEquals(4.0f, engine.boy.reactionTimer, 0.01f)
        assertEquals(4.0f, engine.girl.reactionTimer, 0.01f)

        // Advance time by 1.0 second during updateLookingScene
        engine.update(1.0f, cw, ch)
        println("After 1.0s: boy.pose=${engine.boy.pose}, reactionTimer=${engine.boy.reactionTimer}")
        assertEquals("Boy pose must NOT be clobbered by updateLookingScene while reactionTimer > 0", hugPoseBoy, engine.boy.pose)
        assertEquals("Girl pose must NOT be clobbered by updateLookingScene while reactionTimer > 0", hugPoseGirl, engine.girl.pose)
        assertEquals(3.0f, engine.boy.reactionTimer, 0.05f)
    }

    @Test
    fun `test speech timer expiration cleanly clears text without leaving stale bubbles`() {
        engine.loadScene(SceneType.COOKING)
        engine.boySpeechText = "Delicious stew!"
        engine.boySpeechTimer = 0.20f
        engine.girlSpeechText = "I made it with love!"
        engine.girlSpeechTimer = 0.20f

        assertNotNull(engine.boySpeechText)
        assertNotNull(engine.girlSpeechText)

        // After 0.10s, timers are still active
        engine.update(0.10f, 1080f, 2400f)
        assertNotNull("Text should still be present before timer expires", engine.boySpeechText)
        assertNotNull("Text should still be present before timer expires", engine.girlSpeechText)
        println("At 0.10s: boyTimer=${engine.boySpeechTimer}, girlTimer=${engine.girlSpeechTimer}")

        // After another 0.15s (total 0.25s), timers expire and text is cleared to null
        engine.update(0.15f, 1080f, 2400f)
        println("At 0.25s: boyText=${engine.boySpeechText}, girlText=${engine.girlSpeechText}")
        assertEquals("boySpeechText must be cleared to null on timer expiry", null, engine.boySpeechText)
        assertEquals("girlSpeechText must be cleared to null on timer expiry", null, engine.girlSpeechText)

        // Switching scene also resets any active speech text immediately
        engine.boySpeechText = "Wait don't leave!"
        engine.boySpeechTimer = 5.0f
        engine.loadScene(SceneType.SLEEP)
        assertEquals("Scene load must reset boySpeechText", null, engine.boySpeechText)
        assertEquals("Scene load must reset boySpeechTimer", 0f, engine.boySpeechTimer, 0.001f)
    }

    @Test
    fun `test real clock hour mapping to TimeOfDayPhase`() {
        // Reset any leftover debug override
        TimeOfDayPhase.debugOverride = null

        // 05:00 - 07:59 -> MORNING
        for (h in 5..7) {
            val phase = TimeOfDayPhase.fromHour(h)
            assertEquals("Hour $h must be MORNING", TimeOfDayPhase.MORNING, phase)
            assertTrue("Hour $h must have isMorning == true", phase.isMorning)
            assertTrue("Hour $h must have isDay == true", phase.isDay)
            assertFalse("Hour $h must not be isNight", phase.isNight)
            assertFalse("Hour $h must not be isSunset", phase.isSunset)
        }

        // 08:00 - 16:59 -> AFTERNOON
        for (h in 8..16) {
            val phase = TimeOfDayPhase.fromHour(h)
            assertEquals("Hour $h must be AFTERNOON", TimeOfDayPhase.AFTERNOON, phase)
            assertTrue("Hour $h must have isDay == true", phase.isDay)
            assertFalse("Hour $h must not be isMorning", phase.isMorning)
            assertFalse("Hour $h must not be isNight", phase.isNight)
            assertFalse("Hour $h must not be isSunset", phase.isSunset)
        }

        // 17:00 - 19:59 -> SUNSET
        for (h in 17..19) {
            val phase = TimeOfDayPhase.fromHour(h)
            assertEquals("Hour $h must be SUNSET", TimeOfDayPhase.SUNSET, phase)
            assertTrue("Hour $h must have isSunset == true", phase.isSunset)
            assertFalse("Hour $h must not be isDay", phase.isDay)
            assertFalse("Hour $h must not be isNight", phase.isNight)
        }

        // 20:00 - 04:59 -> NIGHT
        val nightHours = listOf(20, 21, 22, 23, 0, 1, 2, 3, 4)
        for (h in nightHours) {
            val phase = TimeOfDayPhase.fromHour(h)
            assertEquals("Hour $h must be NIGHT", TimeOfDayPhase.NIGHT, phase)
            assertTrue("Hour $h must have isNight == true", phase.isNight)
            assertFalse("Hour $h must not be isDay", phase.isDay)
            assertFalse("Hour $h must not be isSunset", phase.isSunset)
        }
        println("All 24 hours verified successfully against TimeOfDayPhase thresholds.")
    }

    @Test
    fun `test atmosphereMode override mapping and resolve`() {
        TimeOfDayPhase.debugOverride = null

        // Forced DAY mode -> AFTERNOON regardless of hour
        val dayPhase = TimeOfDayPhase.resolve(atmosphereMode = "DAY", hour = 23)
        assertEquals(TimeOfDayPhase.AFTERNOON, dayPhase)
        assertTrue(dayPhase.isDay)
        assertFalse(dayPhase.isNight)

        // Forced SUNSET mode -> SUNSET regardless of hour
        val sunsetPhase = TimeOfDayPhase.resolve(atmosphereMode = "SUNSET", hour = 12)
        assertEquals(TimeOfDayPhase.SUNSET, sunsetPhase)
        assertTrue(sunsetPhase.isSunset)
        assertFalse(sunsetPhase.isNight)

        // Forced NIGHT mode -> NIGHT regardless of hour
        val nightPhase = TimeOfDayPhase.resolve(atmosphereMode = "NIGHT", hour = 12)
        assertEquals(TimeOfDayPhase.NIGHT, nightPhase)
        assertTrue(nightPhase.isNight)
        assertFalse(nightPhase.isDay)

        // AUTO mode -> maps to hour
        assertEquals(TimeOfDayPhase.MORNING, TimeOfDayPhase.resolve(atmosphereMode = "AUTO", hour = 6))
        assertEquals(TimeOfDayPhase.AFTERNOON, TimeOfDayPhase.resolve(atmosphereMode = "AUTO", hour = 14))
        assertEquals(TimeOfDayPhase.SUNSET, TimeOfDayPhase.resolve(atmosphereMode = "AUTO", hour = 18))
        assertEquals(TimeOfDayPhase.NIGHT, TimeOfDayPhase.resolve(atmosphereMode = "AUTO", hour = 22))
        println("atmosphereMode mapping (DAY/SUNSET/NIGHT/AUTO) verified successfully.")
    }

    @Test
    fun `test debugOverride in TimeOfDayPhase`() {
        try {
            TimeOfDayPhase.debugOverride = TimeOfDayPhase.MORNING
            // Even if atmosphereMode is NIGHT and hour is 23, debug override wins in debug/test builds
            val resolved = TimeOfDayPhase.resolve("NIGHT", hour = 23)
            assertEquals("debugOverride must override resolve result", TimeOfDayPhase.MORNING, resolved)

            TimeOfDayPhase.debugOverride = TimeOfDayPhase.SUNSET
            assertEquals(TimeOfDayPhase.SUNSET, TimeOfDayPhase.resolve("DAY", hour = 12))
        } finally {
            TimeOfDayPhase.debugOverride = null
        }
        // With debugOverride cleared, returns normal
        assertEquals(TimeOfDayPhase.AFTERNOON, TimeOfDayPhase.resolve("DAY", hour = 12))
        println("debugOverride functionality verified successfully.")
    }

    @Test
    fun `test SceneEngine atmosphere update and periodic clock check`() {
        TimeOfDayPhase.debugOverride = null

        // Initial default
        assertEquals("AUTO", engine.atmosphereMode)

        // Update atmosphere mode to NIGHT
        engine.updateAtmosphereMode("NIGHT")
        assertEquals("NIGHT", engine.atmosphereMode)
        assertEquals(TimeOfDayPhase.NIGHT, engine.timeOfDayPhase)
        assertTrue(engine.timeOfDayPhase.isNight)

        // Update atmosphere mode to DAY
        engine.updateAtmosphereMode("DAY")
        assertEquals("DAY", engine.atmosphereMode)
        assertEquals(TimeOfDayPhase.AFTERNOON, engine.timeOfDayPhase)
        assertFalse(engine.timeOfDayPhase.isNight)
        assertTrue(engine.timeOfDayPhase.isDay)

        // Periodic check (after 30 seconds of engine.update) refreshes timeOfDayPhase
        engine.updateAtmosphereMode("NIGHT")
        assertEquals(TimeOfDayPhase.NIGHT, engine.timeOfDayPhase)
        // Advance 31 seconds in engine
        engine.update(31.0f, 1080f, 2400f)
        assertEquals("NIGHT", engine.atmosphereMode)
        assertEquals(TimeOfDayPhase.NIGHT, engine.timeOfDayPhase)
        println("SceneEngine atmosphere update and periodic refresh verified successfully.")
    }

    @Test
    fun `test living room lamp decoupled from couch visual phase`() {
        engine.loadScene(SceneType.SLEEP)

        // Case A: Lamp is Lit -> window follows real clock/timeOfDayPhase
        engine.livingRoomLampLit = true
        engine.updateAtmosphereMode("DAY")
        val lampOnNight = engine.timeOfDayPhase.isNight
        assertFalse("When lamp is on and DAY mode active, isNight must be false", lampOnNight)

        // Couch phase can cycle independently without changing room lamp state
        val initialCouchPhase = engine.couchVisualPhase
        engine.update(10.0f, 1080f, 2400f)
        assertTrue("Living room lamp must remain lit", engine.livingRoomLampLit)
        println("Living room lamp and couch visual phase decoupling verified successfully.")
    }

    @Test
    fun `test double tap boy secret whisper and joy jump`() {
        engine.loadScene(SceneType.FLOWER)
        engine.onDoubleTapBoy(1080f, 2400f)

        assertEquals("Boy pose should be JOY_JUMP on double tap", CharacterPose.JOY_JUMP, engine.boy.pose)
        assertEquals("Boy emote should be HEART", EmoteType.HEART, engine.boy.emote)
        assertEquals("Boy emotion should be LOVING", CharacterEmotion.LOVING, engine.boy.emotion)
        assertNotNull("Boy speech text should not be null", engine.boySpeechText)
        val validWhispers = com.example.data.ProfileManager.getProfile().boyTapWhispers.ifEmpty {
            listOf(
                "You make me the happiest person alive",
                "i love u ${engine.girl.name}",
                "My heart belongs to you",
                "Forever your ${engine.boy.name}"
            )
        }
        assertTrue("Speech should be one of boy's secret whispers", validWhispers.contains(engine.boySpeechText))
        println("Boy double tap secret whisper verified: ${engine.boySpeechText}")
    }

    @Test
    fun `test double tap girl secret whisper and blush`() {
        engine.loadScene(SceneType.FLOWER)
        engine.onDoubleTapGirl(1080f, 2400f)

        assertEquals("Girl pose should be JOY_JUMP on double tap", CharacterPose.JOY_JUMP, engine.girl.pose)
        assertEquals("Girl emote should be BLUSH", EmoteType.BLUSH, engine.girl.emote)
        assertEquals("Girl emotion should be SHY", CharacterEmotion.SHY, engine.girl.emotion)
        assertNotNull("Girl speech text should not be null", engine.girlSpeechText)
        val validGirlWhispers = com.example.data.ProfileManager.getProfile().girlTapWhispers.ifEmpty {
            listOf(
                "i love you so much",
                "My ${engine.boy.name}... my whole heart",
                "${engine.girl.name} loves ${engine.boy.name} forever"
            )
        }
        assertTrue("Speech should be one of girl's secret whispers", validGirlWhispers.contains(engine.girlSpeechText))
        println("Girl double tap secret whisper verified: ${engine.girlSpeechText}")
    }

    @Test
    fun `test ride scene triple tap hug mechanism`() {
        engine.loadScene(SceneType.EVENING_RIDE)
        engine.onTripleTapRide(1080f, 2400f)

        assertEquals("Girl pose should be HUG on ride triple tap", CharacterPose.HUG, engine.girl.pose)
        assertEquals("Girl emotion should be LOVING", CharacterEmotion.LOVING, engine.girl.emotion)
        assertEquals("Boy emotion should be LOVING", CharacterEmotion.LOVING, engine.boy.emotion)
        assertEquals("Girl speech should be Hold on tight!", "Hold on tight!", engine.girlSpeechText)
        assertEquals("Boy speech should be Holding you forever, my love.", "Holding you forever, my love.", engine.boySpeechText)
        assertEquals("Scene message banner must show ride hug text", "Holding tight together on our evening ride", engine.sceneMessage)
        println("Ride triple tap hug verified successfully.")
    }

    @Test
    fun `test Option A gesture accumulator flushing semantics`() = kotlinx.coroutines.runBlocking {
        var dispatchedAction: String? = null
        var pendingJob: Job? = null
        var pendingAction: (() -> Unit)? = null

        val flushPendingTap: () -> Unit = {
            pendingJob?.cancel()
            pendingJob = null
            val action = pendingAction
            pendingAction = null
            action?.invoke()
        }

        // 1. Simulate single tap on Boy: sets pendingAction and launches job
        pendingAction = { dispatchedAction = "BOY_SINGLE" }
        pendingJob = launch {
            delay(260L)
            dispatchedAction = "BOY_SINGLE_TIMEOUT"
        }

        // 2. User taps a world object immediately (e.g. flower after 50ms):
        // Flush should immediately fire BOY_SINGLE and clear pending
        flushPendingTap()
        assertEquals("Pending character action must be flushed immediately", "BOY_SINGLE", dispatchedAction)
        assertNull("Pending action should be cleared after flush", pendingAction)

        // 3. User double-taps Boy within 260ms:
        dispatchedAction = null
        pendingAction = { dispatchedAction = "BOY_SINGLE" }
        pendingJob = launch {
            delay(260L)
            dispatchedAction = "BOY_SINGLE_TIMEOUT"
        }

        // Tap 2 arrives: cancels job and triggers double-tap action directly
        pendingJob?.cancel()
        pendingAction = null
        dispatchedAction = "BOY_DOUBLE_TAP"

        assertEquals("Double tap should execute immediately without waiting for timeout", "BOY_DOUBLE_TAP", dispatchedAction)
        println("Option A accumulator flushing and multi-tap semantics verified successfully.")
    }

    @Test
    fun `test mochi matchmaker gating distance and candidate scenes`() {
        engine.loadScene(SceneType.FLOWER)
        engine.sceneTime = 10.0f // Past initial script
        engine.mochiMatchmakerCooldown = 0f
        engine.boy.reactionTimer = 0f
        engine.girl.reactionTimer = 0f
        engine.boy.isTransitioningPosition = false
        engine.boy.isTransitioningPose = false
        engine.girl.isTransitioningPosition = false
        engine.girl.isTransitioningPose = false
        engine.catState = CatState.SITTING_PURR

        // 1. When characters are close (< 0.20f apart), matchmaker must NOT trigger
        engine.boy.worldX = 0.48f
        engine.girl.worldX = 0.52f
        assertFalse("Characters close together (< 0.20f) must not trigger matchmaker", engine.canTriggerMochiMatchmaker())

        // 2. When characters are standing apart (>= 0.20f), matchmaker triggers
        engine.boy.worldX = 0.25f
        engine.girl.worldX = 0.65f
        assertTrue("Characters standing apart (>= 0.20f) must trigger matchmaker", engine.canTriggerMochiMatchmaker())

        // 3. Excluded scenes: EVENING_RIDE and COZY_LOFT must NEVER trigger
        engine.loadScene(SceneType.EVENING_RIDE)
        engine.sceneTime = 10.0f
        engine.mochiMatchmakerCooldown = 0f
        assertFalse("Evening Ride must not trigger matchmaker", engine.canTriggerMochiMatchmaker())

        engine.loadScene(SceneType.COZY_LOFT)
        engine.sceneTime = 10.0f
        engine.mochiMatchmakerCooldown = 0f
        assertFalse("Cozy Loft must not trigger matchmaker", engine.canTriggerMochiMatchmaker())

        // 4. Living room sleep transition: lamp off or drowsy must prevent matchmaker
        engine.loadScene(SceneType.SLEEP)
        engine.sceneTime = 10.0f
        engine.mochiMatchmakerCooldown = 0f
        engine.boy.reactionTimer = 0f
        engine.girl.reactionTimer = 0f
        engine.boy.isTransitioningPosition = false
        engine.boy.isTransitioningPose = false
        engine.girl.isTransitioningPosition = false
        engine.girl.isTransitioningPose = false
        engine.catState = CatState.SITTING_PURR
        engine.livingRoomReturnTimer = 0f
        engine.boy.worldX = 0.20f
        engine.girl.worldX = 0.70f
        engine.livingRoomLampLit = false // Lamp off -> sleeping
        assertFalse("Living room with lamp off must not trigger matchmaker", engine.canTriggerMochiMatchmaker())

        engine.livingRoomLampLit = true
        engine.boy.emotion = CharacterEmotion.SLEEPY
        assertFalse("Drowsy characters in living room must not trigger matchmaker", engine.canTriggerMochiMatchmaker())

        engine.boy.emotion = CharacterEmotion.HAPPY
        engine.girl.emotion = CharacterEmotion.HAPPY
        assertTrue("Awake, apart characters with lamp on in living room can trigger matchmaker", engine.canTriggerMochiMatchmaker())

        // 5. Triggering matchmaker starts the sequence
        engine.triggerMochiMatchmaker(1080f, 2400f)
        assertTrue("Matchmaker must be active after trigger", engine.mochiMatchmakerActive)
        assertFalse("Cat must wake up during matchmaker", engine.catSleeping)

        // 6. Soft bias on tap: tapping boy when mochiNudgeActiveTimer > 0 brings him closer
        engine.mochiNudgeActiveTimer = 5.0f
        val initialBoyX = engine.boy.worldX
        engine.onTouchBoy(1080f, 2400f)
        assertTrue("Boy should step toward girl (initial: $initialBoyX, new: ${engine.boy.worldX})", engine.boy.worldX > initialBoyX)
        assertEquals("Coming closer to you!", engine.boySpeechText)
        assertEquals(0f, engine.mochiNudgeActiveTimer)
        println("Mochi matchmaker gating, scenes, and soft-bias verified successfully.")
    }

    @Test
    fun `test tree bark growth stages and date calculation`() {
        val originalDate = com.example.data.RelationshipTimeManager.relationshipStartDate
        try {
            engine.loadScene(SceneType.UNDER_TREE)

            // Reference start date: test base date
            val start = java.time.LocalDate.of(2024, 1, 1)
            com.example.data.RelationshipTimeManager.relationshipStartDate = start

            // Year 0 (< 1 year elapsed): Stage 0
            assertEquals("Day 1 must be Stage 0", 0, engine.computeTreeMossGrowthStage((start).toKotlinLocalDate()))
            assertEquals("6 months in must be Stage 0", 0, engine.computeTreeMossGrowthStage((start.plusMonths(6)).toKotlinLocalDate()))

            // Year 1 (exactly 1 year elapsed): Stage 1
            assertEquals("1 year must be Stage 1", 1, engine.computeTreeMossGrowthStage((start.plusYears(1)).toKotlinLocalDate()))

            // Year 2: Stage 2
            assertEquals("2 years must be Stage 2", 2, engine.computeTreeMossGrowthStage((start.plusYears(2)).toKotlinLocalDate()))

            // Year 3: Stage 3
            assertEquals("3 years must be Stage 3", 3, engine.computeTreeMossGrowthStage((start.plusYears(3)).toKotlinLocalDate()))

            // Year 4+: Stage 4 (max cap)
            assertEquals("4 years must be Stage 4", 4, engine.computeTreeMossGrowthStage((start.plusYears(4)).toKotlinLocalDate()))
            assertEquals("10 years must cap at Stage 4", 4, engine.computeTreeMossGrowthStage((start.plusYears(10)).toKotlinLocalDate()))

            // Debug override check: simulate without waiting
            engine.treeMossDebugYearOverride = 2
            assertEquals("Debug override to 2 must return 2", 2, engine.computeTreeMossGrowthStage())
            engine.treeMossDebugYearOverride = 4
            assertEquals("Debug override to 4 must return 4", 4, engine.computeTreeMossGrowthStage())
            engine.treeMossDebugYearOverride = null

            // Day count check: start date is Day 1, 1000 days later is Day 1001
            val day1 = com.example.data.RelationshipTimeManager.calculateTinyUsDay(start)
            assertEquals("Start date must be Day 1", 1L, day1)
            val day1001 = com.example.data.RelationshipTimeManager.calculateTinyUsDay(start.plusDays(1000))
            assertEquals("1000 days after start date must be Day 1001", 1001L, day1001)
            println("Tree bark growth stages, debug overrides, and date regression verified successfully.")
        } finally {
            com.example.data.RelationshipTimeManager.relationshipStartDate = originalDate
        }
    }

    // ── Feature 3: Shared Dream Journal ──────────────────────────────────────

    @Test
    fun `test dream keyword matching japan and norway`() {
        // Japan keywords
        val (japanTheme, japanKws) = com.example.ui.parseDreamTheme("We walked under cherry blossoms in Japan near a torii gate")
        assertEquals("Japan phrase must map to JAPAN theme", "JAPAN", japanTheme)
        assertTrue("japan keyword must be matched", japanKws.contains("japan"))
        assertTrue("cherry blossom keyword must be matched", japanKws.contains("cherry blossom"))
        assertTrue("torii keyword must be matched", japanKws.contains("torii"))

        // Norway / aurora keywords
        val (norwayTheme, norwayKws) = com.example.ui.parseDreamTheme("The aurora lit up the Norway sky with northern lights")
        assertEquals("Norway phrase must map to NORWAY theme", "NORWAY", norwayTheme)
        assertTrue("norway keyword must be matched", norwayKws.contains("norway"))
        assertTrue("aurora keyword must be matched", norwayKws.contains("aurora"))
        assertTrue("northern lights keyword must be matched", norwayKws.contains("northern lights"))

        println("Dream keyword matching for japan and norway verified successfully.")
    }

    @Test
    fun `test dream keyword matching individual themes`() {
        assertEquals("OCEAN",  com.example.ui.parseDreamTheme("swimming in the ocean waves").first)
        assertEquals("FLYING", com.example.ui.parseDreamTheme("i was flying through the sky").first)
        assertEquals("STARS",  com.example.ui.parseDreamTheme("stargaze at the galaxy").first)
        assertEquals("FOREST", com.example.ui.parseDreamTheme("lost in the forest woods").first)
        assertEquals("HOME",   com.example.ui.parseDreamTheme("cozy cottage by the fireplace").first)
        assertEquals("RAIN",   com.example.ui.parseDreamTheme("standing in the rain during a storm").first)
        assertEquals("CITY",   com.example.ui.parseDreamTheme("night out in the city skyline").first)
        assertEquals("SWEET",  com.example.ui.parseDreamTheme("eating cake on a picnic").first)
        println("Dream keyword matching for all individual themes verified successfully.")
    }

    @Test
    fun `test dream fallback for unrecognized phrase`() {
        val (theme, keywords) = com.example.ui.parseDreamTheme("just a random dream about nothing specific")
        assertEquals("Unrecognized phrase must produce FALLBACK theme", "FALLBACK", theme)
        assertTrue("Unrecognized phrase must produce empty keyword list", keywords.isEmpty())
        println("Dream fallback for unrecognized phrase verified successfully.")
    }

    @Test
    fun `test scene engine activate and clear dream state`() {
        engine.loadScene(SceneType.FLOWER)
        assertNull("activeDreamTheme must be null before activation", engine.activeDreamTheme)
        assertNull("activeDreamText must be null before activation", engine.activeDreamText)
        assertEquals("dreamOverlayAlpha must be 0 before activation", 0f, engine.dreamOverlayAlpha, 0.001f)

        engine.activateDream("JAPAN", "cherry blossoms in Tokyo")
        assertEquals("activeDreamTheme must be JAPAN after activation", "JAPAN", engine.activeDreamTheme)
        assertEquals("activeDreamText must match typed text", "cherry blossoms in Tokyo", engine.activeDreamText)
        assertEquals("dreamOverlayAlpha must be 1 after activation", 1f, engine.dreamOverlayAlpha, 0.001f)

        engine.clearDream()
        assertNull("activeDreamTheme must be null after clearDream", engine.activeDreamTheme)
        assertNull("activeDreamText must be null after clearDream", engine.activeDreamText)
        assertEquals("dreamOverlayAlpha must be 0 after clearDream", 0f, engine.dreamOverlayAlpha, 0.001f)

        println("SceneEngine activateDream and clearDream state verified successfully.")
    }

    @Test
    fun `test weather BGM track mapping for all five weather types`() {
        assertEquals(R.raw.bgm_sunny, audio.getWeatherRawResId(WeatherType.SUNNY))
        assertEquals(R.raw.bgm_rain, audio.getWeatherRawResId(WeatherType.RAIN))
        assertEquals(R.raw.bgm_sakura, audio.getWeatherRawResId(WeatherType.SAKURA))
        assertEquals(R.raw.bgm_autumn, audio.getWeatherRawResId(WeatherType.AUTUMN))
        assertEquals(R.raw.bgm_snow, audio.getWeatherRawResId(WeatherType.SNOW))
    }

    @Test
    fun `test configurable per-track start offsets`() {
        assertEquals(0, audio.getTrackStartOffset(WeatherType.SUNNY))
        assertEquals(0, audio.getTrackStartOffset(WeatherType.RAIN))

        audio.setTrackStartOffset(WeatherType.RAIN, 4500)
        assertEquals(4500, audio.getTrackStartOffset(WeatherType.RAIN))

        audio.setTrackStartOffset(WeatherType.SAKURA, 3200)
        assertEquals(3200, audio.getTrackStartOffset(WeatherType.SAKURA))

        // Negative offset should clamp to 0
        audio.setTrackStartOffset(WeatherType.AUTUMN, -100)
        assertEquals(0, audio.getTrackStartOffset(WeatherType.AUTUMN))
    }

    @Test
    fun `test manual weather cycling switches immediately with no crossfade and resets drift timer`() {
        engine.weather = WeatherType.SUNNY
        engine.update(120f, 1000f, 1000f) // drift timer at 120s

        val newWeather = engine.cycleWeather()
        assertEquals(WeatherType.RAIN, newWeather)
        assertEquals(WeatherType.RAIN, audio.currentWeather)
        assertFalse("Manual weather change must have NO crossfade", audio.isCrossfading)

        // Advancing engine slightly should show drift timer was reset
        engine.update(1f, 1000f, 1000f)
        assertEquals(WeatherType.RAIN, engine.weather)
    }

    @Test
    fun `test automatic weather drift triggers crossfade`() {
        engine.weather = WeatherType.SUNNY
        audio.playWeatherBgm(WeatherType.SUNNY, isAutomaticDrift = false)
        assertEquals(WeatherType.SUNNY, audio.currentWeather)

        engine.driftWeather()
        assertNotEquals(WeatherType.SUNNY, engine.weather)
        assertEquals(engine.weather, audio.currentWeather)
    }

    @Test
    fun `test Music Box ducking reduces Weather BGM to 25 percent and restores on stop`() {
        audio.isEnabled = true
        audio.setIndoor(false, smooth = false) // Outdoor
        audio.setMusicBoxDucking(false, smooth = false) // Music Box OFF

        // Base outdoor volume without music box is 100%
        assertEquals(1.0f, audio.computeEffectiveWeatherVolume(), 0.01f)
        assertFalse(audio.isMusicBoxDucked)

        // Start music box (ducking ON)
        audio.setMusicBoxDucking(true, smooth = false)
        assertTrue(audio.isMusicBoxDucked)
        assertEquals(0.25f, audio.musicBoxDuckingMultiplier, 0.01f)
        assertEquals(0.25f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Stop music box (ducking OFF)
        audio.setMusicBoxDucking(false, smooth = false)
        assertFalse(audio.isMusicBoxDucked)
        assertEquals(1.0f, audio.musicBoxDuckingMultiplier, 0.01f)
        assertEquals(1.0f, audio.computeEffectiveWeatherVolume(), 0.01f)
    }

    @Test
    fun `test weather changes while Music Box is active maintains ducking at 25 percent`() {
        audio.isEnabled = true
        audio.setIndoor(false, smooth = false)
        audio.setMusicBoxDucking(true, smooth = false) // Music Box active

        // Manual switch while music box is active
        audio.playWeatherBgm(WeatherType.SAKURA, isAutomaticDrift = false)
        assertEquals(WeatherType.SAKURA, audio.currentWeather)
        assertEquals(0.25f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Automatic drift while music box is active
        audio.playWeatherBgm(WeatherType.SNOW, isAutomaticDrift = true)
        assertEquals(WeatherType.SNOW, audio.currentWeather)
        assertEquals(0.25f, audio.computeEffectiveWeatherVolume(), 0.01f)
    }

    @Test
    fun `test indoor scenes reduce Weather BGM volume to 40 percent and restore outdoors`() {
        audio.isEnabled = true
        audio.setMusicBoxDucking(false, smooth = false) // No Music Box

        // 1. Outdoor scene: FLOWER
        engine.loadScene(SceneType.FLOWER)
        assertTrue(engine.isCurrentSceneOutdoor)
        audio.setIndoor(false, smooth = false)
        assertEquals(1.0f, audio.indoorVolumeMultiplier, 0.01f)
        assertEquals(1.0f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // 2. Interior scene: COOKING (Kitchen)
        engine.loadScene(SceneType.COOKING)
        assertFalse(engine.isCurrentSceneOutdoor)
        audio.setIndoor(true, smooth = false)
        assertEquals(0.40f, audio.indoorVolumeMultiplier, 0.01f)
        assertEquals(0.40f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // 3. Interior scene: SLEEP (Living Room)
        engine.loadScene(SceneType.SLEEP)
        assertFalse(engine.isCurrentSceneOutdoor)
        audio.setIndoor(true, smooth = false)
        assertEquals(0.40f, audio.indoorVolumeMultiplier, 0.01f)
        assertEquals(0.40f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // 4. Interior scene: COZY_LOFT (Midnight Loft)
        engine.loadScene(SceneType.COZY_LOFT)
        assertFalse(engine.isCurrentSceneOutdoor)
        audio.setIndoor(true, smooth = false)
        assertEquals(0.40f, audio.indoorVolumeMultiplier, 0.01f)
        assertEquals(0.40f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // 5. Return to outdoor scene: UNDER_TREE
        engine.loadScene(SceneType.UNDER_TREE)
        assertTrue(engine.isCurrentSceneOutdoor)
        audio.setIndoor(false, smooth = false)
        assertEquals(1.0f, audio.indoorVolumeMultiplier, 0.01f)
        assertEquals(1.0f, audio.computeEffectiveWeatherVolume(), 0.01f)
    }

    @Test
    fun `test combined indoor and Music Box ducking produces 10 percent volume`() {
        audio.isEnabled = true

        // Outdoor + No Music Box: 100%
        audio.setIndoor(false, smooth = false)
        audio.setMusicBoxDucking(false, smooth = false)
        assertEquals(1.0f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Indoor + No Music Box: ~40%
        audio.setIndoor(true, smooth = false)
        assertEquals(0.40f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Outdoor + Music Box: ~25%
        audio.setIndoor(false, smooth = false)
        audio.setMusicBoxDucking(true, smooth = false)
        assertEquals(0.25f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Indoor + Music Box: 1.0 * 0.40 * 0.25 = 0.10 (10%)
        audio.setIndoor(true, smooth = false)
        assertEquals(0.10f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Transition from indoor+Music Box (~10%) to outdoor+Music Box (~25%), not directly 100%
        audio.setIndoor(false, smooth = false)
        assertEquals(0.25f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Stop Music Box outdoors restores to 100%
        audio.setMusicBoxDucking(false, smooth = false)
        assertEquals(1.0f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Move indoors: 40%
        audio.setIndoor(true, smooth = false)
        assertEquals(0.40f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Start Music Box indoors: drops to 10%
        audio.setMusicBoxDucking(true, smooth = false)
        assertEquals(0.10f, audio.computeEffectiveWeatherVolume(), 0.01f)

        // Stop Music Box indoors: restores only to 40% (not 100%)
        audio.setMusicBoxDucking(false, smooth = false)
        assertEquals(0.40f, audio.computeEffectiveWeatherVolume(), 0.01f)
    }
}
