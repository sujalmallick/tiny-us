package com.example.autonomy

import com.example.engine.AmbientAudio
import com.example.engine.CharacterMotionTween
import com.example.engine.CharacterPose
import com.example.engine.PixelCharacter
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.scene.autonomy.AgentPhase
import com.example.scene.autonomy.Behavior
import com.example.scene.autonomy.DiscoveryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/** The couple's own routine, running inside the real engine. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutonomyEngineTest {

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

    /** Steps the engine, calling [each] after every frame. */
    private fun run(seconds: Float, each: () -> Unit = {}) {
        var t = 0f
        while (t < seconds) {
            engine.update(dt, cw, ch)
            each()
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
    fun `left alone, the couple keeps finding things to do`() {
        engine.loadScene(SceneType.SUNROOM)
        var travelled = 0f
        var lastX = engine.boy.worldX
        var lastGirlX = engine.girl.worldX
        run(90f) {
            travelled += abs(engine.boy.worldX - lastX) + abs(engine.girl.worldX - lastGirlX)
            lastX = engine.boy.worldX
            lastGirlX = engine.girl.worldX
        }
        val distinct = engine.autonomyLog.toSet()
        assertTrue("They should move around: $travelled", travelled > 0.6f)
        assertTrue("Several different activities: $distinct", distinct.size >= 5)
        assertTrue("At least a dozen activities in 90 s: ${engine.autonomyLog.size}", engine.autonomyLog.size >= 12)
    }

    @Test
    fun `nobody ever teleports`() {
        val maxStep = CharacterMotionTween.SHARED_WALKING_SPEED * dt * 2.2f + 0.0005f
        for (scene in listOf(SceneType.CAMPFIRE, SceneType.RAINY_CAFE, SceneType.SEASIDE_PIER, SceneType.FLOWER, SceneType.COOKING, SceneType.COZY_LOFT)) {
            engine.loadScene(scene)
            run(9f) // past every opening script
            fun check(c: PixelCharacter, prev: FloatArray, who: String) {
                val step = hypot(c.worldX - prev[0], c.worldY - prev[1])
                assertTrue("$scene: $who jumped $step", step <= maxStep)
                prev[0] = c.worldX
                prev[1] = c.worldY
            }
            val b = floatArrayOf(engine.boy.worldX, engine.boy.worldY)
            val g = floatArrayOf(engine.girl.worldX, engine.girl.worldY)
            run(70f) {
                check(engine.boy, b, "boy")
                check(engine.girl, g, "girl")
            }
        }
    }

    @Test
    fun `a tap makes the routine step aside, then it carries on`() {
        engine.loadScene(SceneType.CAMPFIRE)
        runUntil(40f, "an autonomous walk") { engine.boyAgent.phase == AgentPhase.WALKING || engine.girlAgent.phase == AgentPhase.WALKING }
        engine.notifyUserInteraction()
        assertEquals(AgentPhase.IDLE, engine.boyAgent.phase)
        assertEquals(AgentPhase.IDLE, engine.girlAgent.phase)
        run(SceneEngine.AUTONOMY_USER_PAUSE_SECONDS - 0.2f) {
            assertEquals(AgentPhase.IDLE, engine.boyAgent.phase)
            assertEquals(AgentPhase.IDLE, engine.girlAgent.phase)
        }
        val before = engine.autonomyLog.size
        run(15f)
        assertTrue("Life resumes after the pause", engine.autonomyLog.size > before)
    }

    @Test
    fun `a tap's reaction pose does not linger once the routine resumes`() {
        engine.loadScene(SceneType.CAMPFIRE)
        run(3f)
        engine.notifyUserInteraction()
        engine.onTouchCampfire(cw, ch, cw * 0.48f, ch * 0.72f)
        assertEquals(CharacterPose.EAT_SNEAK, engine.boy.pose)
        run(SceneEngine.AUTONOMY_USER_PAUSE_SECONDS + 8f)
        assertNotEquals(CharacterPose.EAT_SNEAK, engine.boy.pose)
        assertNotEquals(CharacterPose.EAT_SNEAK, engine.girl.pose)
    }

    @Test
    fun `cinematics always win`() {
        engine.loadScene(SceneType.CAMPFIRE)
        run(12f)
        engine.triggerWatchScene()
        run(5f) {
            assertEquals(AgentPhase.IDLE, engine.boyAgent.phase)
            assertEquals(AgentPhase.IDLE, engine.girlAgent.phase)
        }
    }

    @Test
    fun `they roam, then drift home`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        run(3f)
        val homeX = engine.boy.worldX
        var wasAway = false
        var cameBack = false
        run(150f) {
            val away = abs(engine.boy.worldX - homeX) > 0.06f
            if (away) wasAway = true
            if (wasAway && !away && engine.boyAgent.phase != AgentPhase.WALKING) cameBack = true
        }
        assertTrue("The boy went somewhere", wasAway)
        assertTrue("...and came home again", cameBack)
    }

    @Test
    fun `small discoveries get noticed and picked up`() {
        engine.loadScene(SceneType.SUNROOM)
        run(3f)
        engine.discovery.place(DiscoveryKind.WILDFLOWER, 0.30f, 0.76f)
        engine.discovery.age = 3f
        runUntil(90f, "someone to find the flower") { !engine.discovery.active }
        assertTrue(engine.autonomyLog.contains(Behavior.DISCOVER))
        assertTrue(engine.sceneMessage?.contains("wildflower") == true)
    }

    @Test
    fun `on the scooter they stay seated and only react in place`() {
        engine.loadScene(SceneType.EVENING_RIDE)
        run(8f)
        val bx = engine.boy.worldX
        val gx = engine.girl.worldX
        run(60f)
        assertEquals(bx, engine.boy.worldX, 0.0001f)
        assertEquals(gx, engine.girl.worldX, 0.0001f)
        assertFalse(engine.autonomyLog.any { it == Behavior.WANDER || it == Behavior.VISIT_PROP })
    }

    @Test
    fun `they talk to each other out loud`() {
        engine.loadScene(SceneType.CAMPFIRE)
        var heard = false
        run(120f) {
            val said = engine.boySpeechText ?: engine.girlSpeechText
            if (!said.isNullOrBlank()) heard = true
        }
        assertTrue("No speech in 120 s; they did: ${engine.autonomyLog}", heard)
    }

    @Test
    fun `rain outdoors keeps them close together`() {
        engine.loadScene(SceneType.CAMPFIRE)
        engine.weather = WeatherType.RAIN
        var farFrames = 0
        var frames = 0
        run(120f) {
            frames++
            if (abs(engine.boy.worldX - engine.girl.worldX) > 0.35f) farFrames++
        }
        assertTrue("Mostly huddled in the rain: ${farFrames.toFloat() / frames}", farFrames < frames * 0.2f)
    }

    @Test
    fun `switching the routine off keeps the old quiet behavior`() {
        engine.autonomyEnabled = false
        engine.loadScene(SceneType.RAINY_CAFE)
        run(40f)
        assertTrue(engine.autonomyLog.isEmpty())
    }
}
