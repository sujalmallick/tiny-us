package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.scene.CatState
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Plan 10, C: Mochi really goes to her things: into the box, over to the saucer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MochiThingsTest {

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
        engine.behaviorBrain.random = Random(5)
        engine.autonomyEnabled = false
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
    fun `a tap on the box sends her into it, and she hops out again`() {
        engine.loadScene(SceneType.SLEEP)
        runUntil(2f, "the scene to settle") { engine.sceneTime > 1.6f }
        assertFalse(engine.mochiInBox)
        engine.onTouchCardboardBox(0f, 0f)
        assertEquals(CatState.WALK_FOLLOW, engine.catState)
        runUntil(40f, "her to get into the box") { engine.mochiInBox }
        runUntil(SceneEngine.MOCHI_BOX_SECONDS + 1f, "her to hop out") { !engine.mochiInBox }
        assertEquals(SceneEngine.MOCHI_BOX_X + 0.07f, engine.catWorldX, 0.01f)
    }

    @Test
    fun `the saucer makes her trot over for a drink`() {
        engine.loadScene(SceneType.MOMO_STALL)
        runUntil(2f, "the scene to settle") { engine.sceneTime > 1.6f }
        engine.onTouchMilkSaucer(0f, 0f)
        runUntil(40f, "her to reach the saucer") { engine.catState != CatState.WALK_FOLLOW }
        assertEquals(SceneEngine.MOCHI_SAUCER_X, engine.catWorldX, 0.01f)
        assertEquals(SceneEngine.MOCHI_SAUCER_Y, engine.catWorldY, 0.01f)
        assertEquals(CatState.SITTING_PURR, engine.catState)
    }

    @Test
    fun `she's never left in the box after a scene change`() {
        engine.loadScene(SceneType.SLEEP)
        engine.onTouchCardboardBox(0f, 0f)
        runUntil(40f, "her to get into the box") { engine.mochiInBox }
        engine.loadScene(SceneType.FLOWER)
        assertFalse(engine.mochiInBox)
        // And the box elsewhere doesn't send her anywhere.
        engine.onTouchCardboardBox(0f, 0f)
        assertFalse(engine.mochiInBox)
    }
}
