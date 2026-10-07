package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.engine.HeldItem
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.autonomy.Behavior
import com.example.scene.autonomy.BehaviorBrain
import com.example.scene.autonomy.BehaviorContext
import com.example.scene.autonomy.BehaviorMemory
import com.example.scene.autonomy.CoupleRequests
import com.example.scene.autonomy.RequestKind
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

/** Plan 07 D1: the couple now and then asks for something, granted by tapping the matching prop. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoupleRequestsTest {

    private lateinit var engine: SceneEngine
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f

    @Before
    fun setup() {
        engine = SceneEngine(
            audio = AmbientAudio().apply { isEnabled = false },
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.behaviorBrain.random = Random(7)
    }

    private fun run(seconds: Float, each: () -> Unit = {}) {
        var t = 0f
        while (t < seconds) {
            engine.update(dt, cw, ch)
            each()
            t += dt
        }
    }

    // ── Timing on its own ──

    @Test
    fun `none in a scene's first minute and a half, then one at a time`() {
        val r = CoupleRequests()
        r.reset()
        assertFalse(r.canStart)
        repeat((CoupleRequests.FIRST_DELAY / dt).toInt() + 1) { r.tick(dt) }
        assertTrue(r.canStart)
        r.start(RequestKind.TEA, askerIsGirl = true)
        assertTrue(r.active)
        assertFalse(r.canStart)
    }

    @Test
    fun `an unanswered request fades after its window and waits longer before the next`() {
        val r = CoupleRequests()
        r.forceStart(RequestKind.SONG, askerIsGirl = true)
        var faded = false
        var t = 0f
        while (r.active) { faded = r.tick(dt) || faded; t += dt }
        assertTrue(faded)
        assertEquals(CoupleRequests.WINDOW, t, 0.1f)
        assertEquals(CoupleRequests.IGNORED_GAP, r.cooldown, 0.01f)
    }

    @Test
    fun `granting needs the matching kind and starts the usual gap`() {
        val r = CoupleRequests()
        r.forceStart(RequestKind.TEA, askerIsGirl = false)
        assertFalse(r.grant(RequestKind.SNACK, Random(1)))
        assertTrue(r.active)
        assertTrue(r.grant(RequestKind.TEA, Random(1)))
        assertFalse(r.active)
        assertTrue(r.cooldown in CoupleRequests.GAP_MIN..(CoupleRequests.GAP_MIN + CoupleRequests.GAP_RANGE))
    }

    @Test
    fun `only kinds that fit are picked, with the usual asker favoured`() {
        val r = CoupleRequests()
        assertNull(r.pickKind(askerIsGirl = true, Random(3)) { false })
        val random = Random(5)
        var songs = 0
        repeat(400) { if (r.pickKind(askerIsGirl = true, random) { it == RequestKind.SONG || it == RequestKind.TEA } == RequestKind.SONG) songs++ }
        assertTrue("The girl asks for a song more often than tea: $songs", songs > 220)
    }

    @Test
    fun `the brain only asks when a request is available`() {
        val brain = BehaviorBrain(Random(1))
        val ctx = BehaviorContext().apply { requestAvailable = false }
        brain.computeWeights(ctx, BehaviorMemory())
        assertEquals(0f, brain.lastWeight(Behavior.ASK_FOR_SOMETHING), 0f)
        ctx.requestAvailable = true
        brain.computeWeights(ctx, BehaviorMemory())
        assertTrue(brain.lastWeight(Behavior.ASK_FOR_SOMETHING) > 0f)
        ctx.canWalk = false // the scooter ride
        brain.computeWeights(ctx, BehaviorMemory())
        assertEquals(0f, brain.lastWeight(Behavior.ASK_FOR_SOMETHING), 0f)
    }

    // ── In the real engine ──

    @Test
    fun `tea in the kitchen is granted at the stove, with a mug to sip`() {
        engine.loadScene(SceneType.COOKING)
        run(9f)
        engine.startRequestForTest(RequestKind.TEA, engine.girl)
        assertEquals(engine.girl, engine.requestAsker)
        var granted: RequestKind? = null
        engine.onRequestGranted = { granted = it }

        engine.onTouchFridge(cw * 0.78f, ch * 0.6f) // a snack: not what she asked for
        assertTrue(engine.requests.active)

        engine.onTouchPot(cw, ch)
        assertFalse(engine.requests.active)
        assertNull(engine.requestAsker)
        assertEquals(RequestKind.TEA, granted)
        assertEquals(HeldItem.MUG, engine.girl.heldItem)
        assertTrue(engine.girl.emoteTimer > 0f && engine.boy.emoteTimer > 0f)
    }

    @Test
    fun `a request nobody answers just fades`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        run(2f)
        engine.startRequestForTest(RequestKind.SNACK, engine.boy)
        var granted = false
        engine.onRequestGranted = { granted = true }
        run(CoupleRequests.WINDOW + 1f)
        assertFalse(engine.requests.active)
        assertFalse(granted)
        assertTrue(engine.requests.cooldown > CoupleRequests.GAP_MIN)
    }

    @Test
    fun `a mini-game drops the request quietly`() {
        engine.loadScene(SceneType.SEASIDE_PIER)
        run(2f)
        engine.startRequestForTest(RequestKind.MOCHI, engine.girl)
        engine.catchGame.start()
        run(0.2f)
        assertFalse(engine.requests.active)
        assertTrue(engine.requests.quiet > 0f)
    }

    @Test
    fun `left alone in the loft one evening, someone asks for something, but not straight away`() {
        engine.loadScene(SceneType.COZY_LOFT)
        engine.updateAtmosphereMode("SUNSET")
        var firstAt = -1f
        var t = 0f
        var asked: RequestKind? = null
        var askLogged = false
        run(600f) {
            t += dt
            if (firstAt < 0f && engine.requests.startedCount > 0) {
                // Counted, not polled: a start is never missed, even if it ends within a frame.
                if (!engine.requests.active) println("Request started and ended within one update at t=$t")
                firstAt = t
                asked = engine.requests.kind
                // The log keeps only the last few dozen activities, so look now. Both of them
                // tick in the same frame, so the partner may have logged something just after.
                askLogged = Behavior.ASK_FOR_SOMETHING in engine.autonomyLog.takeLast(3)
            }
        }
        assertTrue("A request should start within ten minutes (cooldown ${engine.requests.cooldown}, scene ${engine.currentScene}, log ${engine.autonomyLog.takeLast(8)})", firstAt > 0f)
        assertTrue("None in the first ${CoupleRequests.FIRST_DELAY}s: $firstAt", firstAt >= CoupleRequests.FIRST_DELAY)
        assertNotNull(asked)
        assertTrue("Loft at sunset asks for warmth, tea or a song: $asked", asked != RequestKind.SNACK && asked != RequestKind.MOCHI)
        assertTrue("The routine asked, as its own activity", askLogged)
    }

    @Test
    fun `never on the scooter ride`() {
        engine.loadScene(SceneType.EVENING_RIDE)
        run(400f) { assertFalse(engine.requests.active) }
    }
}
