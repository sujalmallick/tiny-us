package com.example

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.scene.CafeLayout
import com.example.scene.CampfireLayout
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

/**
 * The cafe, sunroom and campfire have no scripted idle loop, so the couple must settle
 * back to their seats after any tap reaction, watch scene or floor stroll.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeatedScenesTest {

    private lateinit var engine: SceneEngine
    private val cw = 1080f
    private val ch = 2400f
    private val p = 5f

    @Before
    fun setup() {
        engine = SceneEngine(
            audio = AmbientAudio().apply { isEnabled = false },
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )
    }

    /** Advances in small steps, like real frames. Keep totals under the 14s autonomous-moment interval. */
    private fun run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(0.05f, left)
            engine.update(dt, cw, ch)
            left -= dt
        }
    }

    @Test
    fun `campfire roast reaction settles back to seats`() {
        engine.loadScene(SceneType.CAMPFIRE)
        val boyX = engine.boy.worldX
        engine.onTouchCampfire(cw, ch, cw * 0.48f, ch * 0.72f)
        assertEquals(CharacterPose.EAT_SNEAK, engine.boy.pose)
        assertTrue(engine.boy.emoteTimer > 0f && engine.girl.emoteTimer > 0f)

        run(4.5f)
        assertEquals(CharacterPose.SIT, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)
        assertEquals(boyX, engine.boy.worldX, 0.001f)
    }

    @Test
    fun `cafe watch scene does not leave the couple stuck kissing`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        val boyX = engine.boy.worldX
        val girlX = engine.girl.worldX
        engine.triggerWatchScene()
        run(3f)
        assertEquals(CharacterPose.KISS, engine.boy.pose)

        run(10f)
        assertFalse(engine.isWatchSceneActive)
        assertEquals(CharacterPose.SIT, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)
        assertEquals(boyX, engine.boy.worldX, 0.001f)
        assertEquals(girlX, engine.girl.worldX, 0.001f)
    }

    @Test
    fun `sunroom floor stroll lingers then walks home`() {
        engine.loadScene(SceneType.SUNROOM)
        run(1.6f) // past the scene wipe
        val boyHomeX = engine.boy.worldX
        assertTrue(engine.onTouchWalkableGround(cw * 0.20f, ch * 0.74f, cw, ch))

        run(4f)
        assertTrue("Boy should still be over by the watering can", engine.boy.worldX < 0.30f)

        run(8f)
        assertEquals(boyHomeX, engine.boy.worldX, 0.001f)
        assertEquals(CharacterPose.IDLE, engine.boy.pose)
    }

    @Test
    fun `barista and guitar emotes are visible`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        engine.onTouchCafeBarista(cw, ch)
        assertTrue(engine.boy.emoteTimer > 0f && engine.girl.emoteTimer > 0f)

        engine.loadScene(SceneType.CAMPFIRE)
        engine.onTouchCampGuitar(cw, ch)
        assertTrue(engine.boy.emoteTimer > 0f && engine.girl.emoteTimer > 0f)
    }

    @Test
    fun `croissant empties on the last bite and refills on the next tap`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        repeat(CafeLayout.MAX_PASTRY_BITES) { engine.onTouchCafePastry(cw, ch) }
        assertEquals(CafeLayout.MAX_PASTRY_BITES, engine.cafePastryBites)
        assertEquals(0f, CafeLayout.croissantWidth(engine.cafePastryBites, p), 0.001f)

        engine.onTouchCafePastry(cw, ch)
        assertEquals(0, engine.cafePastryBites)
    }

    @Test
    fun `window fog hearts stay on the glass`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        val glass = CafeLayout.glass(cw, ch, p)
        engine.onTouchCafeWindow(cw * 0.05f, ch * 0.02f, cw, ch) // far top-left, on the bricks
        assertTrue(cw * engine.cafeWindowHeartX >= glass.left)
        assertTrue(ch * engine.cafeWindowHeartY >= glass.top)
    }

    @Test
    fun `nobody walks into the campfire`() {
        engine.loadScene(SceneType.CAMPFIRE)
        run(1.6f)
        assertFalse(engine.onTouchWalkableGround(cw * CampfireLayout.PIT_X, ch * 0.75f, cw, ch))

        engine.commandCatWalkTo(CampfireLayout.PIT_X, 0.72f, cw, ch)
        assertTrue(kotlin.math.abs(engine.catTargetX - CampfireLayout.PIT_X) >= CampfireLayout.PIT_KEEP_OUT_HALF_WIDTH - 0.0001f)
        assertTrue(engine.sceneMessage?.contains("grass") == true)
    }
}
