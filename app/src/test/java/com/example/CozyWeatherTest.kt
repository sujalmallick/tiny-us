package com.example

import androidx.compose.ui.geometry.Offset
import com.example.engine.AmbientAudio
import com.example.engine.ParticleSystem
import com.example.engine.ParticleType
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherLayout
import com.example.scene.WeatherType
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

/** Weather falls slowly enough to watch, and the couple can play with it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CozyWeatherTest {

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
        engine.updateAtmosphereMode("DAY")
        engine.loadScene(SceneType.FLOWER)
    }

    private fun run(seconds: Float) {
        var left = seconds
        while (left > 0f) {
            val dt = minOf(0.05f, left)
            engine.update(dt, cw, ch)
            left -= dt
        }
    }

    @Test
    fun `weather falls slowly enough to follow`() {
        val ps = ParticleSystem()
        repeat(40) {
            ps.spawnRainDrop(cw, ch)
            ps.spawnSnowflake(cw, ch)
            ps.spawnSakuraPetal(cw, ch)
            ps.spawnAutumnLeaf(cw, ch)
        }
        fun fastestTraversal(type: ParticleType) = ch / ps.particles.filter { it.type == type }.maxOf { it.vy }
        assertTrue("Rain takes at least 2s to cross the screen", fastestTraversal(ParticleType.RAIN_DROP) >= 2.0f)
        assertTrue("Snow drifts for well over 15s", fastestTraversal(ParticleType.SNOWFLAKE) >= 15f)
        assertTrue("Petals drift for well over 14s", fastestTraversal(ParticleType.SAKURA_PETAL) >= 14f)
        assertTrue("Leaves drift for well over 12s", fastestTraversal(ParticleType.AUTUMN_LEAF) >= 12f)
    }

    @Test
    fun `a tap catches the nearest snowflake but never a raindrop`() {
        val ps = ParticleSystem()
        ps.spawnSnowflake(cw, ch, startY = 500f)
        ps.spawnRainDrop(cw, ch)
        val flake = ps.particles.first { it.type == ParticleType.SNOWFLAKE }
        val drop = ps.particles.first { it.type == ParticleType.RAIN_DROP }

        assertNull(ps.catchWeatherParticleAt(drop.x, drop.y, 2f))
        val caught = ps.catchWeatherParticleAt(flake.x + 5f, flake.y, 40f)
        assertNotNull(caught)
        assertEquals(ParticleType.SNOWFLAKE, caught!!.type)
        assertNull("Already caught", ps.catchWeatherParticleAt(flake.x, flake.y, 40f))
        ps.update(0.016f, cw, ch)
        assertFalse(ps.particles.any { it === flake })
    }

    @Test
    fun `catching snowflakes builds the snowman, which melts when the snow stops`() {
        engine.weather = WeatherType.SNOW
        run(0.1f)
        val flake = ParticleSystem.CaughtWeather(ParticleType.SNOWFLAKE, 500f, 800f, androidx.compose.ui.graphics.Color.White)

        engine.onCatchWeather(flake)
        assertTrue(engine.sceneMessage?.contains("snowflake") == true)
        repeat(WeatherLayout.SNOWFLAKES_PER_STAGE - 1) { engine.onCatchWeather(flake) }
        assertEquals(1, engine.snowmanStage)

        repeat(WeatherLayout.SNOWFLAKES_PER_STAGE * 5) { engine.onCatchWeather(flake) }
        assertEquals(WeatherLayout.SNOWMAN_MAX_STAGE, engine.snowmanStage)

        engine.onTouchSnowman(cw, ch)
        assertTrue(engine.snowmanWobbleTimer > 0f)

        engine.weather = WeatherType.SUNNY
        run(0.1f)
        assertEquals(0, engine.snowmanStage)
        assertEquals(0, engine.weatherCatchCount)
    }

    @Test
    fun `every fifth catch is celebrated together`() {
        engine.weather = WeatherType.SAKURA
        run(0.1f)
        val petal = ParticleSystem.CaughtWeather(ParticleType.SAKURA_PETAL, 500f, 800f, androidx.compose.ui.graphics.Color.Magenta)
        repeat(5) { engine.onCatchWeather(petal) }
        assertTrue(engine.sceneMessage?.contains("5 petals caught together") == true)
        assertTrue(engine.boy.emoteTimer > 0f && engine.girl.emoteTimer > 0f)
        assertEquals("The snowman only grows in the snow", 0, engine.snowmanStage)
    }

    @Test
    fun `a rainbow follows the rain by day and can be wished on`() {
        engine.weather = WeatherType.RAIN
        run(0.1f)
        engine.weather = WeatherType.SUNNY
        run(0.1f)
        assertTrue(engine.rainbowTimer > 0f)

        val c = WeatherLayout.rainbowCenter(cw, ch)
        val onBand = Offset(c.x, c.y - WeatherLayout.rainbowOuterRadius(cw) + WeatherLayout.rainbowBandWidth(cw) / 2f)
        assertTrue(WeatherLayout.isOnRainbow(onBand, cw, ch))
        assertFalse("Below the arc is not the rainbow", WeatherLayout.isOnRainbow(Offset(c.x, c.y + 20f), cw, ch))

        engine.onTouchRainbow(cw, ch)
        assertTrue(engine.sceneMessage?.contains("rainbow") == true)

        run(WeatherLayout.RAINBOW_SECONDS + 1f)
        assertEquals(0f, engine.rainbowTimer, 0f)
    }

    @Test
    fun `no rainbow when the rain stops at night`() {
        engine.updateAtmosphereMode("NIGHT")
        engine.weather = WeatherType.RAIN
        run(0.1f)
        engine.weather = WeatherType.SNOW
        run(0.1f)
        assertEquals(0f, engine.rainbowTimer, 0f)
    }

    @Test
    fun `snowman tap target only exists once it has started`() {
        val p = 5f
        val base = WeatherLayout.snowmanBase(cw, ch)
        val onIt = Offset(base.x, base.y - 8f * p)
        assertFalse(WeatherLayout.isOnSnowman(onIt, cw, ch, p, stage = 0))
        assertTrue(WeatherLayout.isOnSnowman(onIt, cw, ch, p, stage = 1))
        assertTrue(WeatherLayout.isOnSnowman(Offset(base.x, base.y - 26f * p), cw, ch, p, stage = 4))
        assertFalse(WeatherLayout.isOnSnowman(Offset(base.x, base.y - 26f * p), cw, ch, p, stage = 1))
    }
}
