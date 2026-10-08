package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.GameText
import com.example.engine.HeldItem
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.autonomy.SceneSpots
import com.example.scene.autonomy.SpotAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/** Plan 12, C: tap something one of them can use, and the nearer one walks over and uses it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoUseEngineTest {

    private lateinit var engine: SceneEngine
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f

    @Before
    fun setup() {
        runBlocking { GameText.load() }
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.autonomyEnabled = false
    }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) { engine.update(dt, cw, ch); t += dt }
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
    fun `the nearer one walks over, and the thing responds when they get there`() {
        engine.loadScene(SceneType.FLOWER)
        run(10f)
        val spot = SceneSpots.forScene(SceneType.FLOWER).first { it.action == SpotAction.LISTEN_CHIMES }
        val nearer = listOf(engine.boy, engine.girl).minByOrNull { abs(it.worldX - spot.x) }!!
        var rang = 0
        assertTrue(engine.sendToUse(SpotAction.LISTEN_CHIMES, cw, ch) { rang++ })
        assertEquals("Not until they're there", 0, rang)
        assertTrue(engine.isGoingToUseSomething)
        runUntil(8f, "them to get to the chimes") { rang > 0 }
        assertEquals(1, rang)
        assertTrue("At the chimes", abs(nearer.worldX - spot.x) < 0.02f)
        assertEquals(if (spot.faceLeft) Direction.LEFT else Direction.RIGHT, nearer.direction)
        run(spot.dwellSeconds * 0.5f)
        assertEquals("Still facing it while they use it", if (spot.faceLeft) Direction.LEFT else Direction.RIGHT, nearer.direction)
        run(spot.dwellSeconds + 1f)
        assertFalse(engine.isGoingToUseSomething)
        assertEquals("Only once", 1, rang)
    }

    @Test
    fun `at the stove they take up the pan, at the table they sit`() {
        engine.loadScene(SceneType.COOKING)
        run(10f)
        var cooked = false
        assertTrue(engine.sendToUse(SpotAction.STIR_POT, cw, ch) { cooked = true })
        runUntil(8f, "them to get to the stove") { cooked }
        run(0.3f)
        assertTrue("The pan in hand", listOf(engine.boy, engine.girl).any { it.heldItem == HeldItem.PAN })
        runUntil(10f, "them to finish at the stove") { !engine.isGoingToUseSomething }
        var sat = false
        assertTrue(engine.sendToUse(SpotAction.SIT_TABLE, cw, ch) { sat = true })
        runUntil(8f, "them to get to the table") { sat }
        run(0.5f)
        assertTrue("Sitting at the table", listOf(engine.boy, engine.girl).any { it.pose == CharacterPose.SIT })
    }

    @Test
    fun `nothing to walk to, or one already on the way, and the thing just responds`() {
        engine.loadScene(SceneType.FLOWER)
        run(10f)
        assertFalse("No stove in the meadow", engine.sendToUse(SpotAction.STIR_POT, cw, ch) {})
        assertTrue(engine.sendToUse(SpotAction.SMELL_FLOWERS, cw, ch) {})
        assertFalse("One at a time", engine.sendToUse(SpotAction.LISTEN_CHIMES, cw, ch) {})
        engine.loadScene(SceneType.COZY_LOFT)
        assertFalse(engine.isGoingToUseSomething)
        assertFalse("Not in the loft", engine.sendToUse(SpotAction.LOOK_WINDOW, cw, ch) {})
    }
}
