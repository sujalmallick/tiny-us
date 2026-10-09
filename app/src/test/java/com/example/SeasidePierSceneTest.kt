package com.example

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.EmoteType
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
        // These tests check the scene's own mechanics; the couple's autonomous routine is
        // covered by AutonomyEngineTest.
        engine.autonomyEnabled = false
        engine.weatherDriftEnabled = false
        engine.weather = WeatherType.SUNNY
        engine.pierRng = Random(7)
        engine.dailyPromptProvider = { "What tiny thing made you smile today?" }
        // Pin the time of day so Bao's day/night routine is deterministic, and the clock's
        // hour away from his four o'clock tea (when he won't fish).
        engine.updateAtmosphereMode("DAY")
        engine.clockHourOverride = 10
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

    @Test
    fun `Grandpa Bao waves hello, sips his tea and fishes on his own`() {
        run(2.2f)
        assertTrue("Bao should wave when the couple arrives", engine.pierBaoWaveTimer > 0f)

        runUntil(10f, "a sip of tea") { engine.pierBaoSipTimer > 0f }
        runUntil(20f, "Bao to cast by himself") { engine.pierFishingPhase != PierFishingPhase.IDLE }
        assertEquals(PierFishingPhase.CASTING, engine.pierFishingPhase)
    }

    @Test
    fun `Bao dozes on quiet nights and wakes when tapped`() {
        engine.updateAtmosphereMode("NIGHT")
        runUntil(SceneEngine.PIER_BAO_DOZE_SECONDS + 2f, "Bao to doze") { engine.isBaoDozing }
        assertEquals("No fishing by himself at night", PierFishingPhase.IDLE, engine.pierFishingPhase)

        engine.onTouchGrandpaBao(cw, ch)
        assertFalse(engine.isBaoDozing)
        assertTrue(engine.sceneMessage?.contains("resting my eyes") == true)
        assertEquals("Waking him doesn't start a cast", PierFishingPhase.IDLE, engine.pierFishingPhase)
    }

    @Test
    fun `telescope spots dolphins`() {
        engine.onTouchPierTelescope(cw, ch)
        assertTrue(engine.pierDolphinTimer > 0f)
        assertEquals(EmoteType.EXCLAMATION, engine.girl.emote)
        assertTrue(engine.girl.emoteTimer > 0f)

        run(SceneEngine.PIER_DOLPHIN_SECONDS + 0.2f)
        assertEquals(0f, engine.pierDolphinTimer, 0f)
    }

    @Test
    fun `sailboat toots back`() {
        engine.onTouchPierBoat(cw, ch, cw * 0.4f, ch * 0.4f)
        assertTrue(engine.pierBoatHornTimer > 0f)
        assertTrue(engine.sceneMessage?.contains("Toot") == true)
    }

    @Test
    fun `Pinchy the crab flees, hides and comes back while Mochi gives chase`() {
        assertTrue(engine.isPierCrabVisible)
        engine.onTouchPierCrab(cw, ch)
        assertTrue(engine.pierCrabStartledTimer > 0f)
        assertEquals(CatState.WALK_FOLLOW, engine.catState)

        runUntil(3f, "Pinchy to hide") { !engine.isPierCrabVisible }
        run(19f)
        assertTrue("Pinchy peeks back out", engine.isPierCrabVisible)
        assertTrue(engine.pierCrabX in PierLayout.CRAB_MIN_X..PierLayout.CRAB_MAX_X)
    }

    @Test
    fun `bait bucket fish gets Mochi's attention`() {
        engine.onTouchPierBucket(cw, ch)
        assertTrue(engine.pierBucketFlopTimer > 0f)
        assertEquals(CatState.WALK_FOLLOW, engine.catState)
        assertTrue(engine.sceneMessage?.contains("bait") == true)
    }

    @Test
    fun `string lights cycle through their colours`() {
        val start = engine.pierLightsPalette
        repeat(SceneEngine.PIER_LIGHT_PALETTES) { engine.onTouchPierLights(cw, ch, cw * 0.5f) }
        assertEquals(start, engine.pierLightsPalette)
        engine.onTouchPierLights(cw, ch, cw * 0.5f)
        assertEquals((start + 1) % SceneEngine.PIER_LIGHT_PALETTES, engine.pierLightsPalette)
        assertTrue(engine.pierLightsSparkleTimer > 0f)
    }

    @Test
    fun `pier watch scene shares a cone, loses it to Pip, and settles back`() {
        engine.triggerWatchScene()
        run(2.5f)
        assertTrue("A cone is shared", engine.pierIceCreamTimer > 0f)

        runUntil(5f, "Pip's raid") { engine.pierIceCreamTimer <= 0f }
        runUntil(8f, "the cinematic to end") { !engine.isWatchSceneActive }
        run(1f)
        assertEquals(CharacterPose.SIT, engine.boy.pose)
        assertEquals(CharacterPose.SIT_SNUGGLE, engine.girl.pose)
    }
}

private object PierLayoutLimits {
    /** Perch wait (at most 1.5s with ice cream out) + the longest railing shuffle + the dive. */
    const val THEFT_SECONDS = 6f
}
