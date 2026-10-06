package com.example

import com.example.data.CoupleLifeStore
import com.example.data.InMemoryKeyValueStorage
import com.example.data.MakeUpChoice
import com.example.data.ThankYouJar
import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.GameText
import com.example.engine.HeldItem
import com.example.progress.ProgressEvent
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plan 09, C in the world: the Decider's pick, the jar, the Make-Up Bench and Phones Down. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoupleLifeEngineTest {
    private lateinit var engine: SceneEngine
    private lateinit var store: CoupleLifeStore
    private val events = mutableListOf<ProgressEvent>()
    private val cw = 1080f
    private val ch = 2400f

    @Before
    fun setup() {
        runBlocking { GameText.load() }
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.onProgress = { events += it }
        store = CoupleLifeStore(InMemoryKeyValueStorage())
        engine.coupleLifeStore = store
        engine.loadScene(SceneType.COOKING)
        run(9f)
    }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) { engine.update(0.05f, cw, ch); t += 0.05f }
    }

    @Test
    fun `the decider's pick is said out loud`() {
        engine.reactToDinnerPick("Dumplings")
        val said = listOfNotNull(engine.boySpeechText, engine.girlSpeechText)
        assertTrue(said.single().contains("Dumplings"))
    }

    @Test
    fun `a full jar is celebrated and one old thank-you is read aloud`() {
        repeat(ThankYouJar.FULL - 1) { store.addThankYou(it % 2 == 0, "thanks $it") }
        // Make the old ones from another day, so one can be read aloud.
        val filled = store.addThankYou(true, "for the tea") != null
        assertTrue(filled)
        engine.onThankYou(fromBoy = true, text = "for the tea", filledJar = true)
        // The giver says thank you; the thanked one blushes; Mochi heads over to the two of them.
        assertTrue(engine.boySpeechText?.contains("for the tea") == true)
        assertEquals(com.example.engine.EmoteType.BLUSH, engine.girl.emote)
        assertEquals(com.example.scene.CatState.WALK_FOLLOW, engine.catState)
        assertEquals((engine.boy.worldX + engine.girl.worldX) / 2f, engine.catTargetX, 0.01f)
    }

    @Test
    fun `the make-up bench sits them apart, then clears to a rainbow and brings them together`() {
        engine.startMakeUpBench()
        run(4f)
        assertTrue(engine.makeUpActive)
        assertTrue(engine.makeUpCloud > 0.9f)
        assertTrue(engine.girl.worldX - engine.boy.worldX > 0.3f)
        engine.finishMakeUpBench(MakeUpChoice.TEA)
        run(1f)
        assertTrue(engine.makeUpRainbow > 0f)
        assertEquals(HeldItem.MUG, engine.boy.heldItem)
        run(8f)
        assertFalse(engine.makeUpActive)
        assertTrue(engine.girl.worldX - engine.boy.worldX < 0.2f)
    }

    @Test
    fun `phones down runs its time, then blooms and counts`() {
        engine.startPhonesDown(15)
        assertTrue(engine.phonesDownActive)
        run(1f)
        assertTrue(engine.phonesDownSecondsLeft in 890L..900L)
        // Pretend the time is up (the session is timed by the wall clock).
        store.startPhonesDown(15, at = System.currentTimeMillis() - 16 * 60_000L)
        run(1f)
        assertFalse(engine.phonesDownActive)
        assertNotNull(events.firstOrNull { it is ProgressEvent.PhonesDown })
        assertEquals(1, store.phonesDownSessions().size)
    }

    @Test
    fun `ending phones down early is fine and not counted`() {
        engine.startPhonesDown(30)
        run(1f)
        engine.stopPhonesDown()
        assertFalse(engine.phonesDownActive)
        assertTrue(store.phonesDownSessions().isEmpty())
        assertTrue(events.none { it is ProgressEvent.PhonesDown })
        assertEquals(CharacterPose.IDLE, engine.boy.pose.let { if (it == CharacterPose.IDLE_BLINK) CharacterPose.IDLE else it })
    }
}
