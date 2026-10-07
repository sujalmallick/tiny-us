package com.example.autonomy

import com.example.data.FoxStore
import com.example.data.FridayFox
import com.example.data.InMemoryKeyValueStorage
import com.example.engine.AmbientAudio
import com.example.engine.WorldViewport
import com.example.progress.ProgressEvent
import com.example.scene.FoxVisit
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlinx.datetime.LocalDate
import org.junit.After
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
import kotlin.random.Random

/** Plan 10, D: the Friday fox comes to play, and leaves its ball when they miss a Friday. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FridayFoxEngineTest {

    private lateinit var engine: SceneEngine
    private lateinit var store: FoxStore
    private val events = mutableListOf<ProgressEvent>()
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f
    private val friday = LocalDate(2026, 10, 9)

    @Before
    fun setup() {
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.behaviorBrain.random = Random(9)
        engine.autonomyEnabled = false
        store = FoxStore(InMemoryKeyValueStorage()).apply { noteSeen(LocalDate(2026, 9, 1)) }
        engine.foxStore = store
        engine.onProgress = { events += it }
    }

    @After
    fun clear() { FridayFox.todayOverride = null }

    private fun runUntil(maxSeconds: Float, what: String, condition: () -> Boolean) {
        var t = 0f
        while (!condition()) {
            assertTrue("Timed out waiting for $what", t < maxSeconds)
            engine.update(dt, cw, ch)
            t += dt
        }
    }

    @Test
    fun `on a Friday the fox comes to play catch with Mochi and goes home`() {
        FridayFox.todayOverride = friday
        engine.loadScene(SceneType.FLOWER)
        runUntil(SceneEngine.FOX_ARRIVES_AFTER + 2f, "the fox to turn up") { engine.foxVisit.active }
        assertTrue(store.visited(friday))
        assertTrue(ProgressEvent.FoxVisited in events)
        runUntil(20f, "them to start playing") { engine.foxVisit.phase == FoxVisit.Phase.PLAYING }
        runUntil(10f, "the ball to reach Mochi") { engine.foxVisit.ballWithMochi }
        runUntil(FoxVisit.PLAY_SECONDS + 20f, "the fox to go home") { !engine.foxVisit.active }

        // Once a Friday: going back to the meadow doesn't bring it again.
        engine.loadScene(SceneType.WALK)
        engine.loadScene(SceneType.FLOWER)
        repeat(((SceneEngine.FOX_ARRIVES_AFTER + 5f) / dt).toInt()) { engine.update(dt, cw, ch) }
        assertFalse(engine.foxVisit.active)
    }

    @Test
    fun `not at night, and not indoors`() {
        FridayFox.todayOverride = friday
        engine.loadScene(SceneType.COOKING)
        repeat(((SceneEngine.FOX_ARRIVES_AFTER + 3f) / dt).toInt()) { engine.update(dt, cw, ch) }
        assertFalse(engine.foxVisit.active)
        engine.updateAtmosphereMode("NIGHT")
        engine.loadScene(SceneType.FLOWER)
        repeat(((SceneEngine.FOX_ARRIVES_AFTER + 3f) / dt).toInt()) { engine.update(dt, cw, ch) }
        assertFalse(engine.foxVisit.active)
        assertFalse(store.visited(friday))
    }

    @Test
    fun `after a missed Friday its ball is waiting in the grass`() {
        FridayFox.todayOverride = LocalDate(2026, 10, 11)
        engine.loadScene(SceneType.FLOWER)
        engine.update(dt, cw, ch)
        assertEquals(friday, engine.foxBallFriday)

        val p = WorldViewport.pixelScale(cw)
        assertFalse("A tap elsewhere misses it", engine.onFoxTap(cw * 0.2f, ch * 0.3f, cw, ch, p))
        assertTrue(engine.onFoxTap(cw * SceneEngine.FOX_BALL_X, ch * SceneEngine.FOX_BALL_Y, cw, ch, p))
        assertNull(engine.foxBallFriday)
        assertTrue(store.ballCollected(friday))
        assertTrue(ProgressEvent.FoxBallFound in events)

        engine.loadScene(SceneType.WALK)
        engine.update(dt, cw, ch)
        assertNull("Picked up for good", engine.foxBallFriday)
    }

    @Test
    fun `the ball isn't indoors either`() {
        FridayFox.todayOverride = LocalDate(2026, 10, 11)
        engine.loadScene(SceneType.SLEEP)
        engine.update(dt, cw, ch)
        assertNull(engine.foxBallFriday)
        engine.loadScene(SceneType.UNDER_TREE)
        engine.update(dt, cw, ch)
        assertNotNull(engine.foxBallFriday)
    }
}
