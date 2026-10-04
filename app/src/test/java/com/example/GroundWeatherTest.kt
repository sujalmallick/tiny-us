package com.example

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.FallenParticle
import com.example.engine.ParticleSystem
import com.example.engine.ParticleType
import com.example.engine.SnowPrintKind
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

/** Playing with the weather on the ground: tossed leaves, snow prints and puddles. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroundWeatherTest {

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

    private fun leafAt(ps: ParticleSystem, normX: Float, normY: Float): FallenParticle =
        FallenParticle(normX, normY, ParticleType.AUTUMN_LEAF, androidx.compose.ui.graphics.Color.Red, 3.5f).also {
            ps.fallenParticles.add(it)
        }

    private fun step(ps: ParticleSystem, seconds: Float) {
        var left = seconds
        while (left > 0f) {
            ps.update(minOf(0.05f, left), cw, ch)
            left -= 0.05f
        }
    }

    @Test
    fun `a swept leaf tumbles through the air and settles back on the ground`() {
        val ps = ParticleSystem()
        val leaf = leafAt(ps, 0.5f, 0.8f)
        assertEquals(1, ps.sweepGroundParticles(cw * 0.5f, ch * 0.8f, cw, ch, radiusPx = 60f))
        assertTrue(leaf.isSwept)

        step(ps, 0.3f)
        assertTrue("It rises first", leaf.normY < 0.8f)
        assertTrue("Still visible in flight", ps.fallenParticles.contains(leaf))

        step(ps, 3f)
        assertFalse("It has landed again", leaf.isSwept)
        assertTrue(ps.fallenParticles.contains(leaf))
        assertEquals(leaf.restNormY, leaf.normY, 0.0001f)
    }

    @Test
    fun `a hard flick throws leaves off the screen`() {
        val ps = ParticleSystem()
        val leaf = leafAt(ps, 0.9f, 0.8f)
        ps.sweepGroundParticles(cw * 0.9f, ch * 0.8f, cw, ch, radiusPx = 60f, dragDeltaX = 120f)
        step(ps, 2f)
        assertFalse(ps.fallenParticles.contains(leaf))
    }

    @Test
    fun `snow is drawn in, not swept`() {
        val ps = ParticleSystem()
        ps.fallenParticles.add(FallenParticle(0.5f, 0.8f, ParticleType.SNOWFLAKE, androidx.compose.ui.graphics.Color.White, 3f))
        assertEquals(0, ps.sweepGroundParticles(cw * 0.5f, ch * 0.8f, cw, ch, radiusPx = 60f))
    }

    @Test
    fun `walking through autumn leaves kicks them up`() {
        // The campfire has no opening script, so the boy walks exactly where told.
        engine.loadScene(SceneType.CAMPFIRE)
        engine.weather = WeatherType.AUTUMN
        run(1.6f)
        engine.particles.fallenParticles.clear()
        val leaf = leafAt(engine.particles, 0.32f, engine.boy.worldY)
        engine.boy.moveTo(0.20f)
        run(1.2f)
        assertTrue("The leaf was kicked (and may already have landed again)", leaf.isSwept || leaf.normX != 0.32f)
    }

    @Test
    fun `footprints and pawprints follow walkers through the snow and then fade`() {
        engine.loadScene(SceneType.CAMPFIRE)
        engine.weather = WeatherType.SNOW
        run(1.6f)
        engine.boy.moveTo(0.20f)
        engine.commandCatWalkTo(0.40f, 0.70f, cw, ch)
        run(2.5f)
        val prints = engine.particles.snowPrints
        assertTrue(prints.count { it.kind == SnowPrintKind.FOOT } >= 3)
        assertTrue(prints.any { it.kind == SnowPrintKind.PAW })

        val firstPrints = prints.toList()
        run(ParticleSystem.SNOW_PRINT_SECONDS + 1f)
        assertTrue("Fresh snow covers the old prints", firstPrints.none { old -> engine.particles.snowPrints.any { it === old } })
    }

    @Test
    fun `drawing in the snow leaves a spaced trail and stops when the snow does`() {
        engine.weather = WeatherType.SNOW
        run(0.1f)
        for (i in 0..40) engine.onDrawInSnow(300f + i * 4f, 1900f, cw, ch)
        val traces = engine.particles.snowPrints.count { it.kind == SnowPrintKind.TRACE }
        assertTrue("Spaced out, not one per event: $traces", traces in 8..30)
        assertTrue(engine.sceneMessage?.contains("snow") == true)

        engine.weather = WeatherType.SUNNY
        run(0.1f)
        engine.onDrawInSnow(700f, 1900f, cw, ch)
        assertTrue(engine.particles.snowPrints.none { it.kind == SnowPrintKind.TRACE })
    }

    @Test
    fun `puddles fill in the rain, can be splashed in, and dry afterwards`() {
        val unit = WeatherLayout.weatherUnit(cw, 5f)
        val spot = engine.particles.puddles.first()
        assertNull("No puddle before it rains", engine.particles.puddleAt(spot.normX, spot.normY, cw, ch, unit))

        engine.weather = WeatherType.RAIN
        run(ParticleSystem.PUDDLE_FILL_SECONDS / 2f)
        val puddle = engine.particles.puddleAt(spot.normX, spot.normY, cw, ch, unit)
        assertNotNull(puddle)

        engine.onTouchPuddle(puddle!!, cw, ch)
        val jumper = if (kotlin.math.abs(engine.boy.worldX - spot.normX) <= kotlin.math.abs(engine.girl.worldX - spot.normX)) engine.boy else engine.girl
        run(0.3f)
        assertEquals(CharacterPose.JOY_JUMP, jumper.pose)
        assertTrue(engine.sceneMessage?.contains("Splash") == true)

        engine.weather = WeatherType.SUNNY
        run(ParticleSystem.PUDDLE_DRY_SECONDS + 1f)
        assertEquals(0f, spot.size, 0f)
    }

    @Test
    fun `each outdoor scene puts its puddles on its own ground`() {
        val outdoor = listOf(
            SceneType.FLOWER, SceneType.UNDER_TREE, SceneType.LOOKING, SceneType.WALK, SceneType.MOMO_STALL,
            SceneType.EVENING_RIDE, SceneType.CAMPFIRE, SceneType.SEASIDE_PIER
        )
        for (scene in outdoor) {
            val spots = WeatherLayout.puddleSpotsFor(scene)
            assertEquals(scene.name, 3, spots.size)
            assertTrue(scene.name, spots.all { (x, y) -> x in 0.05f..0.95f && y in 0.66f..0.97f })
            assertEquals(scene.name, 3, spots.toSet().size)
        }
        engine.loadScene(SceneType.WALK)
        assertEquals(WeatherLayout.puddleSpotsFor(SceneType.WALK).first().second, engine.particles.puddles.first().normY)
    }
}
