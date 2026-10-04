package com.example

import com.example.engine.PixelDissolve
import com.example.engine.SpriteClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMotionTest {
    @Test
    fun spriteTimeStepsInWholeFrames() {
        assertEquals(0f, SpriteClock.step(0.08f), 1e-6f)
        assertEquals(1f / SpriteClock.FPS, SpriteClock.step(0.09f), 1e-6f)
        assertEquals(10f, SpriteClock.step(10.04f), 1e-6f)
        // Never ahead of the real clock, never more than one frame behind.
        for (i in 0..500) {
            val t = i * 0.0137f
            val s = SpriteClock.step(t)
            assertTrue(s <= t + 1e-6f && t - s < 1f / SpriteClock.FPS + 1e-6f)
        }
    }

    @Test
    fun dissolveCoversTheFrameInBlocksAsItProgresses() {
        val w = 16
        val h = 16
        fun covered(amount: Float): Int {
            val px = IntArray(w * h) { 1 }
            PixelDissolve.apply(px, w, h, amount, 7)
            // Each 2x2 cell is all-or-nothing.
            for (cy in 0 until h step 2) for (cx in 0 until w step 2) {
                val c = px[cy * w + cx]
                assertTrue(px[cy * w + cx + 1] == c && px[(cy + 1) * w + cx] == c && px[(cy + 1) * w + cx + 1] == c)
            }
            return px.count { it == 7 }
        }
        assertEquals(0, covered(0f))
        assertEquals(w * h / 2, covered(0.5f))
        assertEquals(w * h, covered(1f))
        assertTrue(covered(0.25f) < covered(0.75f))
    }
}
