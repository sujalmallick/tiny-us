package com.example.engine

import com.example.scene.SceneType
import com.example.scene.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BirdSystemTest {

    private lateinit var birdSystem: BirdSystem

    @Before
    fun setUp() {
        birdSystem = BirdSystem()
    }

    @Test
    fun testPoolInitialization() {
        assertEquals(3, birdSystem.getPoolAvailableCount())
        assertEquals(0, birdSystem.getActiveCount())
    }

    @Test
    fun testMaxActiveBirdsCap() {
        val cw = 1000f
        val ch = 1000f
        val p = 3.5f

        val b1 = birdSystem.spawnBird(cw, ch, p, SceneType.FLOWER)
        val b2 = birdSystem.spawnBird(cw, ch, p, SceneType.FLOWER)
        val b3 = birdSystem.spawnBird(cw, ch, p, SceneType.FLOWER)

        assertNotNull("First bird should spawn", b1)
        assertNotNull("Second bird should spawn", b2)
        assertNull("Third bird must not spawn due to max active cap of 2", b3)
        assertEquals("Active birds count must be 2", 2, birdSystem.getActiveCount())
        assertEquals("Pool available count must be 1", 1, birdSystem.getPoolAvailableCount())
    }

    @Test
    fun testCompleteResetOnDespawnAndRecycle() {
        val cw = 1000f
        val ch = 1000f
        val p = 3.5f

        val bird = birdSystem.spawnBird(cw, ch, p, SceneType.FLOWER, forcedSurface = PerchSurface.MEADOW_ROOF, forcedSpecies = BirdSpecies.BLUEBIRD)
        assertNotNull(bird)
        assertEquals(BirdSpecies.BLUEBIRD, bird!!.species)
        assertTrue(bird.isActive)

        // Simulate flight towards and off screen
        bird.x = cw + 80f * p
        bird.state = BirdState.FLYING_AWAY

        // Update loop should detect off-screen and recycle
        birdSystem.update(
            dt = 0.1f,
            cw = cw,
            ch = ch,
            p = p,
            scene = SceneType.FLOWER,
            weather = WeatherType.SUNNY,
            isNight = false,
            catX = 500f,
            catY = 700f
        )

        assertEquals("Active count must drop to 0 after despawn", 0, birdSystem.getActiveCount())
        assertEquals("Pool must have all 3 birds restored", 3, birdSystem.getPoolAvailableCount())

        // Verify entity fields were completely reset
        assertFalse(bird.isActive)
        assertEquals(BirdState.INACTIVE, bird.state)
        assertEquals(0f, bird.vx, 0.001f)
        assertEquals(0f, bird.vy, 0.001f)
        assertEquals(0f, bird.stateTimer, 0.001f)
        assertEquals(0f, bird.peckTimer, 0.001f)
        assertFalse(bird.isPeckingDown)
        assertEquals(-100f, bird.x, 0.001f)
        assertEquals(-100f, bird.y, 0.001f)
    }

    @Test
    fun testReusedBirdReceivesFreshStateWithoutInheritance() {
        val cw = 1000f
        val ch = 1000f
        val p = 3.5f

        // Spawn, dirty state, then despawn
        val b1 = birdSystem.spawnBird(cw, ch, p, SceneType.FLOWER, forcedSpecies = BirdSpecies.WHITE_DOVE)
        assertNotNull(b1)
        b1!!.startle()
        assertTrue(b1.vy < -100f)

        birdSystem.clear()
        assertEquals(3, birdSystem.getPoolAvailableCount())

        // Spawn a new bird from the pool
        val b2 = birdSystem.spawnBird(cw, ch, p, SceneType.UNDER_TREE, forcedSurface = PerchSurface.TREE_BRANCH_RIGHT, forcedSpecies = BirdSpecies.SPARROW)
        assertNotNull(b2)
        assertEquals(BirdSpecies.SPARROW, b2!!.species)
        assertEquals(BirdState.FLYING_IN, b2.state)
        assertEquals(0f, b2.stateTimer, 0.001f)
        assertFalse(b2.isPeckingDown)
    }

    @Test
    fun testWeatherGating() {
        // Snow should be strictly disabled
        assertFalse("Snow weather must block ambient birds", birdSystem.isWeatherEligible(WeatherType.SNOW, isNight = false))

        // Night must block birds
        assertFalse("Night time must block birds", birdSystem.isWeatherEligible(WeatherType.SUNNY, isNight = true))

        // Normal rain has high barrier
        assertFalse("Normal rain is not eligible for regular spawn cycle", birdSystem.isWeatherEligible(WeatherType.RAIN, isNight = false))

        // Sunny, Sakura, Autumn eligible
        assertTrue("Sunny weather must be eligible", birdSystem.isWeatherEligible(WeatherType.SUNNY, isNight = false))
        assertTrue("Sakura weather must be eligible", birdSystem.isWeatherEligible(WeatherType.SAKURA, isNight = false))
        assertTrue("Autumn weather must be eligible", birdSystem.isWeatherEligible(WeatherType.AUTUMN, isNight = false))
    }

    @Test
    fun testStartleOnTap() {
        val cw = 1000f
        val ch = 1000f
        val p = 3.5f

        val bird = birdSystem.spawnBird(cw, ch, p, SceneType.FLOWER, forcedSurface = PerchSurface.MEADOW_ROOF)
        assertNotNull(bird)

        // Fast-forward to perched state
        bird!!.state = BirdState.PERCHED
        bird.x = 240f
        bird.y = 500f

        // Tap far away: should not startle
        val farMiss = birdSystem.onTouchBird(tapX = 100f, tapY = 100f, p = p)
        assertFalse("Tap far away must not startle bird", farMiss)
        assertEquals(BirdState.PERCHED, bird.state)

        // Tap right on bird: should startle
        val directHit = birdSystem.onTouchBird(tapX = 242f, tapY = 501f, p = p)
        assertTrue("Direct tap on perched bird must startle", directHit)
        assertEquals(BirdState.STARTLED, bird.state)
        assertTrue("Startled bird must accelerate upward", bird.vy < -100f)

        // Second tap while already startled: should not re-trigger
        val secondHit = birdSystem.onTouchBird(tapX = 242f, tapY = 501f, p = p)
        assertFalse("Already startled/flying bird should ignore taps", secondHit)
    }

    @Test
    fun testTwilightPeckingCoordinatesDoNotOverlapCouple() {
        val cw = 1000f
        val ch = 1000f
        val p = 3.5f

        // In Twilight scene, Boy stands at 0.38f - 0.44f.
        // Bird pecking ground coordinates must strictly stay <= 0.28f * cw.
        for (i in 0 until 50) {
            val (destX, destY) = birdSystem.getSurfaceCoordinates(PerchSurface.TWILIGHT_GROUND, cw, ch, p)
            assertTrue("Twilight bird ground X ($destX) must stay below 280 (cw * 0.28f)", destX <= 280f)
            assertTrue("Twilight bird ground X ($destX) must stay above 100 (cw * 0.10f)", destX >= 100f)
            assertEquals("Twilight bird ground Y should match ground level", ch * 0.71f, destY, 0.001f)
        }
    }
}
