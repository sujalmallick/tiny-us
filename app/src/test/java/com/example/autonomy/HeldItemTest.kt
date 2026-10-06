package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.HeldItem
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.autonomy.DiscoveryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Things the couple carry and use: handed out by the routine, used on a tap, put away in time. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HeldItemTest {

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
        engine.behaviorBrain.random = Random(11)
    }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            engine.update(dt, cw, ch)
            t += dt
        }
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
    fun `a tap on someone holding something has them use it`() {
        engine.autonomyEnabled = false
        engine.loadScene(SceneType.SUNROOM)
        run(1.6f)
        engine.boy.hold(HeldItem.MUG, 20f)
        engine.onTouchBoy(cw, ch)
        assertTrue(engine.boy.heldItemUse > 0f)
        assertNotEquals(CharacterPose.JOY_JUMP, engine.boy.pose)
        assertEquals(HeldItem.MUG, engine.boy.heldItem)

        // Empty hands get the usual reaction.
        engine.onTouchGirl(cw, ch)
        assertEquals(HeldItem.NONE, engine.girl.heldItem)
        assertTrue(engine.girl.reactionTimer > 2f)
    }

    @Test
    fun `items are put away when their time is up and on a scene change`() {
        engine.autonomyEnabled = false
        engine.loadScene(SceneType.SUNROOM)
        engine.boy.hold(HeldItem.BOOK, 2f, useSeconds = 1f)
        run(2.5f)
        assertEquals(HeldItem.NONE, engine.boy.heldItem)

        engine.girl.hold(HeldItem.FLOWER, 30f)
        engine.loadScene(SceneType.RAINY_CAFE)
        assertEquals(HeldItem.NONE, engine.girl.heldItem)
    }

    @Test
    fun `a find is picked up and held`() {
        engine.loadScene(SceneType.SUNROOM)
        run(3f)
        engine.discovery.place(DiscoveryKind.LOVE_NOTE, 0.30f, 0.76f)
        engine.discovery.age = 3f
        runUntil(90f, "someone to find the note") { !engine.discovery.active }
        assertTrue(
            "The finder holds the note up",
            listOf(engine.boy, engine.girl).any { it.heldItem == HeldItem.LOVE_NOTE && it.heldItemUse > 0f }
        )
    }

    @Test
    fun `watering the sunroom plants uses a can that goes back afterwards`() {
        engine.loadScene(SceneType.SUNROOM)
        val tools = setOf(HeldItem.WATERING_CAN, HeldItem.MISTER)
        runUntil(240f, "someone to pick up the watering can or mister") {
            engine.boy.heldItem in tools || engine.girl.heldItem in tools
        }
        val who = if (engine.boy.heldItem in tools) engine.boy else engine.girl
        assertTrue("The tool is in use at the plants", who.heldItemUse > 0f)
        runUntil(8f, "the tool to be put back") { who.heldItem !in tools }
    }
}
