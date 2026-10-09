package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.Expression
import com.example.engine.GameText
import com.example.scene.SceneEngine
import com.example.scene.SceneType
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

/** Plan 12 glitch pass: the faces, the walk over to use something, and scene changes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlitchFixesTest {

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
    fun `in the kitchen she doesn't sulk all the while he nibbles`() {
        engine.loadScene(SceneType.COOKING)
        var frames = 0
        var pouting = 0
        var t = 0f
        while (t < 40f) {
            engine.update(dt, cw, ch)
            t += dt
            frames++
            if (engine.girl.expression == Expression.POUT) pouting++
        }
        assertTrue("Pouting ${pouting * 100 / frames}% of the time", pouting < frames / 10)
    }

    @Test
    fun `a new scene frees whoever was on their way to use something`() {
        engine.loadScene(SceneType.FLOWER)
        run(10f)
        assertTrue(engine.sendToUse(SpotAction.LISTEN_CHIMES, cw, ch) {})
        val walker = listOf(engine.boy, engine.girl).maxByOrNull { it.reactionTimer }!!
        run(0.3f)
        engine.loadScene(SceneType.UNDER_TREE)
        assertFalse(engine.isGoingToUseSomething)
        assertTrue("Free to join in the new scene", walker.reactionTimer <= 0f)
        assertEquals(Expression.NONE, walker.expression)
    }

    @Test
    fun `when the thing's own response poses them, that pose stands`() {
        // At the campfire they roast marshmallows, rather than sit down to warm their hands
        engine.loadScene(SceneType.CAMPFIRE)
        run(12f)
        var roasting = false
        assertTrue(engine.sendToUse(SpotAction.WARM_HANDS, cw, ch) {
            engine.onTouchCampfire(cw, ch, cw * 0.5f, ch * 0.6f)
            roasting = true
        })
        runUntil(10f, "them to get to the fire") { roasting }
        run(0.6f)
        assertTrue("Both roasting", listOf(engine.boy, engine.girl).all { it.pose == CharacterPose.EAT_SNEAK })
        // And they're kept at it till the visit's over
        runUntil(8f, "the visit to end") { !engine.isGoingToUseSomething }
    }
}
