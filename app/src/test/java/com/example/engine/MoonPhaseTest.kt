package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MoonPhaseTest {
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    /** Distance around the cycle, so 0.99 and 0.01 count as close. */
    private fun cycleDistance(a: Float, b: Float): Float {
        val d = kotlin.math.abs(a - b) % 1f
        return minOf(d, 1f - d)
    }

    @Test
    fun solarEclipsesFallOnNewMoons() {
        // Total solar eclipses happen only at new moon.
        for (date in listOf("2017-08-21T18:26:00Z", "2024-04-08T18:21:00Z")) {
            val f = MoonPhase.fraction(at(date))
            assertTrue("$date gave $f", cycleDistance(f, 0f) < 0.03f)
            assertTrue(MoonPhase.illumination(f) < 0.02f)
        }
    }

    @Test
    fun lunarEclipsesFallOnFullMoons() {
        // Total lunar eclipses happen only at full moon.
        for (date in listOf("2022-11-08T11:00:00Z", "2025-03-14T06:59:00Z")) {
            val f = MoonPhase.fraction(at(date))
            assertTrue("$date gave $f", cycleDistance(f, 0.5f) < 0.03f)
            assertTrue(MoonPhase.illumination(f) > 0.98f)
        }
    }

    @Test
    fun shadowFollowsTheCycle() {
        // New: the whole row is dark. Full: no shadow at all.
        assertEquals(-9f to 9f, MoonPhase.shadowSpan(0f, 9f))
        assertNull(MoonPhase.shadowSpan(0.5f, 9f))
        // First quarter: the left half is dark. Last quarter: the right half.
        val first = MoonPhase.shadowSpan(0.25f, 9f)!!
        assertEquals(-9f, first.first, 0.01f)
        assertEquals(0f, first.second, 0.01f)
        val last = MoonPhase.shadowSpan(0.75f, 9f)!!
        assertEquals(0f, last.first, 0.01f)
        assertEquals(9f, last.second, 0.01f)
    }
}
