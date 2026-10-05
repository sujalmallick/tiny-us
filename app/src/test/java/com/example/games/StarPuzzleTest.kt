package com.example.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarPuzzleTest {
    private val cw = 720f
    private val ch = 1080f

    private fun tapStar(p: StarPuzzle, c: Constellation, i: Int, drift: Float = 0f) =
        p.tap(p.starX(c, i, drift) * cw, c.stars[i].second * ch, cw, ch, drift)

    @Test
    fun connectingEveryStarInOrderFinishesAndGlows() {
        val c = Constellations.ALL[1]
        val p = StarPuzzle()
        p.start(c)
        for (i in 0 until c.stars.size - 1) assertEquals(StarPuzzle.Tap.CONNECTED, tapStar(p, c, i))
        assertEquals(StarPuzzle.Tap.DONE, tapStar(p, c, c.stars.size - 1))
        assertNull(p.current)
        assertEquals(c, p.glowing)
        p.update(StarPuzzle.GLOW_SECONDS + 0.1f)
        assertNull(p.glowing)
    }

    @Test
    fun aStarOutOfOrderJustWiggles() {
        val c = Constellations.ALL[0]
        val p = StarPuzzle()
        p.start(c)
        assertEquals(StarPuzzle.Tap.WRONG, tapStar(p, c, 3))
        assertEquals(3, p.wiggleStar)
        assertEquals(0, p.connected)
        assertEquals(StarPuzzle.Tap.CONNECTED, tapStar(p, c, 0))
    }

    @Test
    fun theStarsTurnWithTheSky() {
        val c = Constellations.ALL[2]
        val p = StarPuzzle()
        p.start(c)
        val drift = 0.3f
        // Where the star used to be no longer counts; where it has turned to does.
        assertEquals(StarPuzzle.Tap.MISSED, p.tap(c.stars[0].first * cw, c.stars[0].second * ch, cw, ch, drift))
        assertEquals(StarPuzzle.Tap.CONNECTED, tapStar(p, c, 0, drift))
    }

    @Test
    fun eachConstellationHasItsOwnPatchOfSky() {
        for (c in Constellations.ALL) {
            val cx = c.stars.map { it.first }.average().toFloat()
            val cy = c.stars.map { it.second }.average().toFloat()
            assertEquals(c.id, Constellations.at(cx, cy)?.id)
        }
        assertNotNull(Constellations.at(0.14f, 0.11f))
        assertNull(Constellations.at(0.5f, 0.6f))
        assertTrue(Constellations.ALL.map { it.id }.toSet().size == Constellations.ALL.size)
    }
}
