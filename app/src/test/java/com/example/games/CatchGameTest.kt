package com.example.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CatchGameTest {

    @Test
    fun aRoundLastsThirtySecondsAndEndsOnce() {
        val g = CatchGame(Random(1))
        g.start()
        var ends = 0
        repeat(40 * 20) { if (g.update(0.05f)) ends++ }
        assertEquals(1, ends)
        assertFalse(g.active)
        assertEquals(0f, g.timeLeft, 0f)
    }

    @Test
    fun theBasketGlidesToTheFingerAndStaysOnStage() {
        val g = CatchGame(Random(1))
        g.start()
        g.moveTo(5f)
        assertEquals(1f - CatchGame.HALF_WIDTH, g.targetX, 0.0001f)
        g.update(0.05f)
        assertTrue("moved part way", g.basketX > 0.5f && g.basketX < g.targetX)
        repeat(40) { g.update(0.05f) }
        assertEquals(g.targetX, g.basketX, 0.0001f)
    }

    @Test
    fun catchesCountOnlyDuringARound() {
        val g = CatchGame(Random(1))
        g.addCatches(5)
        assertEquals(0, g.score)
        g.start()
        g.addCatches(4)
        assertEquals(4, g.score)
    }

    @Test
    fun goldenStarsCountThreeWhenCaught() {
        val g = CatchGame(Random(2))
        g.start()
        // Follow every golden star: all of them land in the basket.
        var caughtGolden = 0
        repeat(30 * 20) {
            g.goldens.firstOrNull()?.let { g.moveTo(it.x) }
            val before = g.score
            g.update(0.05f)
            if (g.score - before == CatchGame.GOLDEN_VALUE) caughtGolden++
        }
        assertTrue("caught some golden stars", caughtGolden >= 2)
        assertEquals(caughtGolden * CatchGame.GOLDEN_VALUE, g.score)
    }
}
