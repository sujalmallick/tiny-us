package com.example

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.scene.CatState
import com.example.scene.GullState
import com.example.scene.PierCatch
import com.example.scene.PierFishingPhase
import com.example.scene.PierLayout
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeasidePierSceneTest {

    private lateinit var engine: SceneEngine
    private val cw = 1080f
    private val ch = 2400f

    @Before
    fun setup() {
        engine = SceneEngine(
            audio = AmbientAudio().apply { isEnabled = false },
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )
        engine.weatherDriftEnabled = false
        engine.weather = WeatherType.SUNNY
        engine.pierRng = Random(7)
        engine.dailyPromptProvider = { "What tiny thing made you smile today?" }
        engine.loadScene(SceneType.SEASIDE_PIER)
    }

    private fun run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(0.05f, left)
            engine.update(dt, cw, ch)
            left -= dt
        }
    }

    /** Steps frames until [condition] holds; fails if it takes longer than [maxSeconds]. */
    private fun runUntil(maxSeconds: Float, what: String, condition: () -> Boolean) {
        var t = 0f
        while (!condition()) {
            assertTrue("Timed out waiting for $what", t < maxSeconds)
            engine.update(0.05f, cw, ch)
            t += 0.05f
        }
    }

    @Test
    fun `pier loads as a calm outdoor scene`() {
        assertEquals(SceneType.SEASIDE_PIER, engine.currentScene)
        assertTrue(engine.isCurrentSceneOutdoor)
        assertEquals(CharacterPose.SIT, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)
        assertEquals(0f, engine.pierIceCreamTimer, 0f)
        assertEquals(PierFishingPhase.IDLE, engine.pierFishingPhase)
        assertTrue(engine.pierBottleVisible)
        assertEquals(GullState.AWAY, engine.pierGullState)
    }

    @Test
    fun `ice cream shows emotes, melts away and the couple stays seated`() {
        engine.onTouchPierIceCream(cw, ch)
        assertEquals(SceneEngine.PIER_ICE_CREAM_SECONDS, engine.pierIceCreamTimer, 0.001f)
        assertTrue(engine.boy.emoteTimer > 0f && engine.girl.emoteTimer > 0f)

        engine.onTouchPierIceCream(cw, ch)
        assertTrue(engine.sceneMessage?.contains("Still") == true)

        run(11f)
        assertEquals(0f, engine.pierIceCreamTimer, 0f)
        assertEquals(CharacterPose.SIT, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)
    }

    @Test
    fun `Grandpa Bao casts, waits, reels in and shows a catch`() {
        engine.onTouchGrandpaBao(cw, ch)
        assertEquals(PierFishingPhase.CASTING, engine.pierFishingPhase)
        assertTrue(engine.sceneMessage?.startsWith("Grandpa Bao") == true)

        runUntil(2f, "waiting") { engine.pierFishingPhase == PierFishingPhase.WAITING }
        engine.onTouchGrandpaBao(cw, ch)
        assertTrue(engine.sceneMessage?.contains("nibbling") == true)

        runUntil(5f, "reeling") { engine.pierFishingPhase == PierFishingPhase.REELING }
        runUntil(2f, "showing") { engine.pierFishingPhase == PierFishingPhase.SHOWING }
        // The first catch of the day is always a fish for Mochi.
        assertEquals(PierCatch.FISH, engine.pierLastCatch)
        assertEquals(CatState.WALK_FOLLOW, engine.catState)
        assertEquals(PierLayout.MOCHI_FISH_X, engine.catTargetX, 0.001f)

        runUntil(4f, "idle") { engine.pierFishingPhase == PierFishingPhase.IDLE }
        assertNull(engine.pierLastCatch)

        // The next cast brings up something else.
        engine.onTouchGrandpaBao(cw, ch)
        runUntil(8f, "second catch") { engine.pierFishingPhase == PierFishingPhase.SHOWING }
        assertNotEquals(PierCatch.FISH, engine.pierLastCatch)
    }

    @Test
    fun `Pip steals an unguarded ice cream`() {
        runUntil(10f, "Pip to perch") { engine.pierGullState == GullState.PERCHED }
        engine.onTouchPierIceCream(cw, ch)

        runUntil(PierLayoutLimits.THEFT_SECONDS, "the theft") { engine.pierGullState == GullState.ESCAPING }
        assertEquals(0f, engine.pierIceCreamTimer, 0f)
        assertEquals(CharacterPose.JOY_JUMP, engine.boy.pose)
        assertTrue(engine.sceneMessage?.contains("stole") == true)

        runUntil(6f, "Pip to leave") { engine.pierGullState == GullState.AWAY }
    }

    @Test
    fun `shooing Pip mid-sneak saves the ice cream`() {
        runUntil(10f, "Pip to perch") { engine.pierGullState == GullState.PERCHED }
        engine.onTouchPierIceCream(cw, ch)
        runUntil(3f, "Pip to sneak") { engine.pierGullState == GullState.SNEAKING }

        engine.onTouchPip(cw, ch)
        assertEquals(GullState.ESCAPING, engine.pierGullState)
        assertTrue(engine.sceneMessage?.contains("red-beaked") == true)

        run(2f)
        assertTrue("Ice cream should survive", engine.pierIceCreamTimer > 0f)
        assertNotEquals(CharacterPose.JOY_JUMP, engine.boy.pose)
    }

    @Test
    fun `Pip waits out the rain on the railing`() {
        runUntil(10f, "Pip to perch") { engine.pierGullState == GullState.PERCHED }
        engine.weather = WeatherType.RAIN
        engine.onTouchPierIceCream(cw, ch)
        run(7f)
        assertEquals(GullState.PERCHED, engine.pierGullState)
        assertTrue(engine.pierIceCreamTimer > 0f)
    }

    @Test
    fun `message in a bottle reveals today's prompt and drifts back later`() {
        engine.onTouchPierBottle(cw, ch)
        assertFalse(engine.pierBottleVisible)
        assertTrue(engine.sceneMessage?.contains("What tiny thing made you smile today?") == true)

        engine.showMessage("unchanged")
        engine.onTouchPierBottle(cw, ch)
        assertEquals("unchanged", engine.sceneMessage)

        run(SceneEngine.PIER_BOTTLE_RESPAWN_SECONDS + 1f)
        assertTrue(engine.pierBottleVisible)
    }

    @Test
    fun `walks stay between the cart and Bao`() {
        run(1.6f)
        assertFalse(engine.onTouchWalkableGround(cw * 0.13f, ch * 0.76f, cw, ch))
        assertTrue(engine.onTouchWalkableGround(cw * 0.28f, ch * 0.80f, cw, ch))
    }

    @Test
    fun `leaving and returning resets the pier`() {
        engine.onTouchPierIceCream(cw, ch)
        engine.onTouchGrandpaBao(cw, ch)
        engine.onTouchPierBottle(cw, ch)

        engine.loadScene(SceneType.RAINY_CAFE)
        engine.loadScene(SceneType.SEASIDE_PIER)
        assertEquals(0f, engine.pierIceCreamTimer, 0f)
        assertEquals(PierFishingPhase.IDLE, engine.pierFishingPhase)
        assertTrue(engine.pierBottleVisible)
        assertEquals(GullState.AWAY, engine.pierGullState)
    }

    @Test
    fun `lighthouse answers with a foghorn line`() {
        engine.onTouchLighthouse(cw, ch)
        assertTrue(engine.pierLighthouseTimer > 0f)
        assertTrue(engine.sceneMessage != null)
    }
}

private object PierLayoutLimits {
    /** Perch wait (at most 1.5s with ice cream out) + the longest railing shuffle + the dive. */
    const val THEFT_SECONDS = 6f
}
