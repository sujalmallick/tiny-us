package com.example.autonomy

import com.example.data.InMemoryKeyValueStorage
import com.example.data.VisitorStore
import com.example.data.Visitors
import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.WorldViewport
import com.example.progress.ProgressEvent
import com.example.scene.OldCoupleVisit
import com.example.scene.PainterVisit
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Plan 11: the street painter, and Billionaire and The Great on the pier. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisitorsEngineTest {

    private lateinit var engine: SceneEngine
    private lateinit var store: VisitorStore
    private val events = mutableListOf<ProgressEvent>()
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f
    private val today = LocalDate(2026, 10, 7)

    @Before
    fun setup() {
        kotlinx.coroutines.runBlocking { com.example.engine.GameText.load() }
        Visitors.todayOverride = today
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.behaviorBrain.random = Random(6)
        engine.autonomyEnabled = false
        store = VisitorStore(InMemoryKeyValueStorage())
        engine.visitorStore = store
        engine.onProgress = { events += it }
    }

    @After
    fun clear() { Visitors.todayOverride = null }

    private fun run(seconds: Float, each: () -> Unit = {}) {
        var t = 0f
        while (t < seconds) { engine.update(dt, cw, ch); each(); t += dt }
    }

    private fun runUntil(maxSeconds: Float, what: String, condition: () -> Boolean) {
        var t = 0f
        while (!condition()) {
            assertTrue("Timed out waiting for $what", t < maxSeconds)
            engine.update(dt, cw, ch)
            t += dt
        }
    }

    @Test
    fun `the painter paints them as they are, and it's kept`() {
        engine.loadScene(SceneType.FLOWER)
        engine.weather = WeatherType.SAKURA
        engine.boy.outfitIndex = 3
        runUntil(SceneEngine.VISITOR_AFTER + 2f, "the painter to come") { engine.painter.active }
        runUntil(30f, "the easel to go up") { engine.painter.phase == PainterVisit.Phase.PAINTING }
        runUntil(PainterVisit.PAINT_SECONDS + 2f, "the painting to be finished") { engine.painter.showing }
        val painting = store.paintings().single()
        assertEquals("FLOWER", painting.scene)
        assertEquals("SAKURA", painting.weather)
        assertEquals(3, painting.boyOutfit)
        assertNotNull(engine.shownPainting)
        assertTrue(ProgressEvent.VisitorKeepsake("visitor:PAINTING") in events)
        runUntil(PainterVisit.REVEAL_SECONDS + 30f, "the painter to go") { !engine.painter.active }

        // Once a week, and nobody else today
        engine.loadScene(SceneType.WALK)
        run(SceneEngine.VISITOR_AFTER + 3f)
        assertFalse(engine.painter.active)
        engine.updateAtmosphereMode("SUNSET")
        engine.loadScene(SceneType.SEASIDE_PIER)
        run(SceneEngine.VISITOR_AFTER + 3f)
        assertFalse("One visitor a day", engine.oldCouple.active)
    }

    @Test
    fun `no painter at night or indoors`() {
        engine.loadScene(SceneType.COOKING)
        run(SceneEngine.VISITOR_AFTER + 3f)
        assertFalse(engine.painter.active)
        engine.updateAtmosphereMode("NIGHT")
        engine.loadScene(SceneType.FLOWER)
        run(SceneEngine.VISITOR_AFTER + 3f)
        assertFalse(engine.painter.active)
    }

    @Test
    fun `the old couple echo the two of them, a beat behind`() {
        engine.updateAtmosphereMode("SUNSET")
        engine.loadScene(SceneType.SEASIDE_PIER)
        runUntil(SceneEngine.VISITOR_AFTER + 2f, "the old couple to come") { engine.oldCouple.active }
        assertTrue(ProgressEvent.VisitorKeepsake("visitor:OLD_COUPLE") in events)
        runUntil(20f, "them to sit down") { engine.oldCouple.phase == OldCoupleVisit.Phase.SITTING }
        // The couple hold hands; a moment later, so do they
        val holdHands = { engine.boy.pose = CharacterPose.HOLD_HANDS; engine.girl.pose = CharacterPose.HOLD_HANDS }
        holdHands()
        run(0.5f, holdHands)
        // On the pier the two of them start snuggled on their bench, and the old couple echo that first
        assertTrue("Not straight away", engine.oldCouple.mood != OldCoupleVisit.Mood.HOLD_HANDS)
        run(OldCoupleVisit.MIRROR_DELAY + 0.5f, holdHands)
        assertEquals(OldCoupleVisit.Mood.HOLD_HANDS, engine.oldCouple.mood)

        // Each has their own lines: him on the left of the bench, The Great on the right
        val p = WorldViewport.pixelScale(cw)
        val bx = cw * OldCoupleVisit.BENCH_X
        val by = ch * OldCoupleVisit.BENCH_Y - 12f * p
        assertTrue(engine.onVisitorTap(bx + 8f * p, by, cw, ch, p))
        assertEquals("Her first line", 1, engine.oldCouple.herNext)
        assertEquals(0, engine.oldCouple.hisNext)
        assertTrue(engine.sceneMessage?.contains("Elegance is the only beauty that never fades") == true)
        assertTrue(engine.onVisitorTap(bx - 8f * p, by, cw, ch, p))
        assertEquals(1, engine.oldCouple.hisNext)
    }

    @Test
    fun `on their anniversary the old couple always come, and leave a note`() {
        engine.togetherSince = "2023-10-07"
        engine.loadScene(SceneType.SEASIDE_PIER) // by day, not even evening
        runUntil(SceneEngine.VISITOR_AFTER + 2f, "the old couple to come") { engine.oldCouple.active }
        assertTrue(engine.oldCouple.anniversary)
        runUntil(OldCoupleVisit.ANNIVERSARY_STAY + 30f, "the note") { engine.oldCouple.noteWaiting }
        runUntil(10f, "them to get up") { engine.oldCouple.phase != OldCoupleVisit.Phase.SITTING }
        val p = WorldViewport.pixelScale(cw)
        assertTrue(engine.onVisitorTap(cw * OldCoupleVisit.BENCH_X, ch * OldCoupleVisit.BENCH_Y - 4f * p, cw, ch, p))
        assertTrue(engine.shownNote)
        assertEquals(setOf(2026), store.noteYears())
        assertTrue(ProgressEvent.VisitorKeepsake("visitor:NOTE") in events)
    }
}
