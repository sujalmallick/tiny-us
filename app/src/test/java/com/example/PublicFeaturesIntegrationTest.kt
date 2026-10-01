package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdventureStatus
import com.example.data.DailyMomentResponse
import com.example.data.DailyPromptCatalog
import com.example.data.DateAdventure
import com.example.data.DateAdventureCatalog
import com.example.data.LongDistanceSignal
import com.example.data.LongDistanceSignalType
import com.example.data.MiniGameCatalog
import com.example.data.MiniGameRound
import com.example.data.MiniGameType
import com.example.data.PartnerMoodState
import com.example.data.PreferencesManager
import com.example.data.RelationshipTimeManager
import com.example.data.SharedMoodType
import com.example.data.WorldEvent
import com.example.data.WorldEventBus
import com.example.data.WorldEventListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PublicFeaturesIntegrationTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = PreferencesManager(context)
        // Reset test state in SharedPreferences
        val sp = context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().commit()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. WorldEventBus & Decoupled Event System
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test WorldEventBus subscribes, receives events and unsubscribes cleanly`() {
        val receivedEvents = mutableListOf<WorldEvent>()
        val listener = WorldEventListener { event ->
            receivedEvents.add(event)
        }

        WorldEventBus.subscribe(listener)

        val testEvent = WorldEvent.DateAdventureCompleted(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            adventureId = "adv_cook_treat",
            title = "Cook Cozy Meal",
            completedBy = "both"
        )
        WorldEventBus.post(testEvent)

        assertEquals("Should receive 1 event", 1, receivedEvents.size)
        assertEquals(testEvent.id, receivedEvents[0].id)

        // Unsubscribe
        WorldEventBus.unsubscribe(listener)
        WorldEventBus.post(
            WorldEvent.TinyMomentCompleted(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                promptId = "prompt_1",
                promptText = "Question",
                isBothAnswered = true
            )
        )

        assertEquals("No new events should be received after unsubscribe", 1, receivedEvents.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Feature 1: Tiny Date Adventures
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test Tiny Date Adventures lifecycle and asynchronous completion`() {
        val initialAdventures = prefs.getDateAdventures()
        assertTrue("Catalog should provide initial default adventures", initialAdventures.isNotEmpty())

        val adventure = initialAdventures.first()
        assertEquals(AdventureStatus.AVAILABLE, adventure.status)
        assertFalse(adventure.isFullyCompleted)

        // Partner A (Boy) completes first
        val inProgress = adventure.copy(
            status = AdventureStatus.IN_PROGRESS,
            completedByBoy = true
        )
        prefs.saveDateAdventures(listOf(inProgress))

        var loaded = prefs.getDateAdventures().first()
        assertEquals(AdventureStatus.IN_PROGRESS, loaded.status)
        assertTrue(loaded.completedByBoy)
        assertFalse(loaded.completedByGirl)
        assertFalse(loaded.isFullyCompleted)

        // Partner B (Girl) completes later
        val fullyDone = loaded.copy(
            status = AdventureStatus.COMPLETED,
            completedByGirl = true,
            completedTimestamp = System.currentTimeMillis()
        )
        prefs.saveDateAdventures(listOf(fullyDone))

        loaded = prefs.getDateAdventures().first()
        assertEquals(AdventureStatus.COMPLETED, loaded.status)
        assertTrue(loaded.completedByBoy)
        assertTrue(loaded.completedByGirl)
        assertTrue(loaded.isFullyCompleted)
        assertEquals(1, prefs.getCompletedAdventuresCount())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Feature 2: Daily Tiny Moment
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test Daily Tiny Moment asynchronous answers and reveal condition`() {
        val prompt = DailyPromptCatalog.getPromptForDay(1)
        assertNotNull(prompt)
        assertTrue(prompt.question.isNotBlank())

        val todayDateStr = "2026-10-01"
        var response = prefs.getDailyMomentResponseForDate(todayDateStr, prompt.id)
        assertFalse(response.isBothAnswered)
        assertFalse(response.isRevealed)

        // Boy answers first
        response = response.copy(boyAnswer = "She made me tea when I was tired.")
        prefs.saveDailyMomentResponse(response)

        var saved = prefs.getDailyMomentResponseForDate(todayDateStr, prompt.id)
        assertTrue(saved.isAnsweredByBoy)
        assertFalse(saved.isAnsweredByGirl)
        assertFalse(saved.isBothAnswered)

        // Girl answers later
        saved = saved.copy(
            girlAnswer = "He gave me a warm hug before leaving.",
            isRevealed = true,
            completedTimestamp = System.currentTimeMillis()
        )
        prefs.saveDailyMomentResponse(saved)

        val finalSaved = prefs.getDailyMomentResponseForDate(todayDateStr, prompt.id)
        assertTrue(finalSaved.isBothAnswered)
        assertTrue(finalSaved.isRevealed)
        assertEquals("She made me tea when I was tired.", finalSaved.boyAnswer)
        assertEquals("He gave me a warm hug before leaving.", finalSaved.girlAnswer)
        assertEquals(1, prefs.getCompletedMomentsCount())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Feature 3: Two-Person Mini-Games
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test Mini-Games matching and asynchronous choices`() {
        val question = MiniGameCatalog.questions.first()
        val roundId = UUID.randomUUID().toString()

        // Partner A answers Option 0
        var round = MiniGameRound(
            id = roundId,
            questionId = question.id,
            type = question.type,
            prompt = question.prompt,
            options = question.options,
            boyChosenIndex = 0
        )
        prefs.saveMiniGameRound(round)

        var loaded = prefs.getMiniGameRounds().find { it.id == roundId }
        assertNotNull(loaded)
        assertTrue(loaded!!.isAnsweredByBoy)
        assertFalse(loaded.isAnsweredByGirl)
        assertFalse(loaded.isBothAnswered)

        // Partner B answers Option 0 (Match!)
        round = loaded.copy(
            girlChosenIndex = 0,
            isRevealed = true,
            timestamp = System.currentTimeMillis()
        )
        prefs.saveMiniGameRound(round)

        loaded = prefs.getMiniGameRounds().find { it.id == roundId }
        assertNotNull(loaded)
        assertTrue(loaded!!.isBothAnswered)
        assertTrue(loaded.isMatch)
        assertEquals(1, prefs.getCompletedMiniGamesCount())

        // Non-matching round
        val diffRound = MiniGameRound(
            id = UUID.randomUUID().toString(),
            questionId = question.id,
            type = question.type,
            prompt = question.prompt,
            options = question.options,
            boyChosenIndex = 0,
            girlChosenIndex = 1,
            isRevealed = true
        )
        prefs.saveMiniGameRound(diffRound)
        val loadedDiff = prefs.getMiniGameRounds().find { it.id == diffRound.id }
        assertNotNull(loadedDiff)
        assertTrue(loadedDiff!!.isBothAnswered)
        assertFalse(loadedDiff.isMatch)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Feature 4: Shared Mood with Privacy Rules
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test Shared Mood privacy control and visibility`() {
        // Initially default
        val initial = prefs.getPartnerMoodState()
        assertEquals(SharedMoodType.GOOD, initial.boyMood)
        assertEquals(SharedMoodType.GOOD, initial.girlMood)

        // Boy sets mood to TIRED and shared = true
        prefs.setPartnerMood("boy", SharedMoodType.TIRED, isShared = true)
        var state = prefs.getPartnerMoodState()
        assertEquals(SharedMoodType.TIRED, state.boyMood)
        assertTrue(state.boyMoodShared)
        assertEquals(SharedMoodType.TIRED, state.getVisibleMoodForPartner(isViewerGirl = true))

        // Boy switches to private
        prefs.setPartnerMood("boy", SharedMoodType.LOW, isShared = false)
        state = prefs.getPartnerMoodState()
        assertEquals(SharedMoodType.LOW, state.boyMood)
        assertFalse(state.boyMoodShared)
        assertNull("Private mood must not be visible to partner", state.getVisibleMoodForPartner(isViewerGirl = true))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Feature 5: Tiny Home Evolution Progression
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test Home Evolution unlocks new artifacts when features are experienced`() {
        // Initial state: 0 completed adventures, 0 moments, 0 mini games, 0 signals
        var home = prefs.getHomeEvolutionState()
        assertFalse("Picnic basket should be locked initially", home.hasAdventurePicnicBasket)
        assertFalse("Bedside notepad should be locked initially", home.hasBedsideNotepad)
        assertFalse("Mini-game board should be locked initially", home.hasMiniGameBoard)
        assertFalse("Origami heart should be locked initially", home.hasOrigamiHeart)

        // 1. Complete an adventure
        val adv = DateAdventureCatalog.defaultAdventures.first().copy(
            status = AdventureStatus.COMPLETED,
            completedByBoy = true,
            completedByGirl = true
        )
        prefs.saveDateAdventures(listOf(adv))
        home = prefs.getHomeEvolutionState()
        assertTrue("Picnic basket should unlock after completing an adventure", home.hasAdventurePicnicBasket)

        // 2. Complete a moment
        val moment = DailyMomentResponse(
            promptId = "p1",
            dateString = "2026-10-01",
            boyAnswer = "Answer",
            girlAnswer = "Answer",
            isRevealed = true
        )
        prefs.saveDailyMomentResponse(moment)
        home = prefs.getHomeEvolutionState()
        assertTrue("Bedside notepad should unlock after completing a moment", home.hasBedsideNotepad)

        // 3. Complete a mini game
        val round = MiniGameRound(
            id = "r1",
            questionId = "q1",
            type = MiniGameType.WOULD_YOU_RATHER,
            prompt = "Prompt",
            options = listOf("A", "B"),
            boyChosenIndex = 0,
            girlChosenIndex = 0,
            isRevealed = true
        )
        prefs.saveMiniGameRound(round)
        home = prefs.getHomeEvolutionState()
        assertTrue("Mini-game board should unlock after a mini-game round", home.hasMiniGameBoard)

        // 4. Send a long-distance signal
        val sig = LongDistanceSignal(
            id = "s1",
            sender = "boy",
            type = LongDistanceSignalType.SEND_HEART,
            note = "Miss you",
            timestamp = System.currentTimeMillis()
        )
        prefs.sendLongDistanceSignal(sig)
        home = prefs.getHomeEvolutionState()
        assertTrue("Origami heart should unlock after sending a long-distance signal", home.hasOrigamiHeart)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Feature 6 & 7: Long-Distance Mode and Widget Payload
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `test Long-Distance Signals queueing and widget data generation`() {
        val signal = LongDistanceSignal(
            id = UUID.randomUUID().toString(),
            sender = "boy",
            type = LongDistanceSignalType.GOOD_MORNING,
            note = "Have a wonderful day, sunshine!",
            timestamp = System.currentTimeMillis()
        )
        prefs.sendLongDistanceSignal(signal)

        val signals = prefs.getLongDistanceSignals()
        assertEquals(1, signals.size)
        assertEquals(LongDistanceSignalType.GOOD_MORNING, signals[0].type)
        assertEquals("Have a wonderful day, sunshine!", signals[0].note)
        assertFalse(signals[0].isViewed)

        // Mark viewed
        prefs.markSignalViewed(signal.id)
        val viewedSignals = prefs.getLongDistanceSignals()
        assertTrue(viewedSignals[0].isViewed)

        // Generate Widget Data Payload
        prefs.boyfriendName = "Alex"
        prefs.girlfriendName = "Maya"
        val widgetData = prefs.getWidgetData(
            currentWeather = "Sakura",
            timePhase = "Sunset",
            sceneName = "Living Room"
        )

        assertEquals("Alex & Maya", widgetData.coupleNames)
        assertEquals("Sakura", widgetData.weatherName)
        assertEquals("Sunset", widgetData.timePhase)
        assertEquals("Living Room", widgetData.sceneName)
        assertNotNull(widgetData.dailyMomentPrompt)
        assertTrue(widgetData.latestSignalText?.contains("Good Morning") == true)
    }
}
