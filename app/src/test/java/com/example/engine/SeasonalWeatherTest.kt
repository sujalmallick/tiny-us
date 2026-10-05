package com.example.engine

import com.example.scene.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SeasonalWeatherTest {
    private fun seen(month: Int, country: String = ""): Set<WeatherType> {
        val r = Random(7)
        return (0 until 500).map { SeasonalWeather.pick(month, country, r) }.toSet()
    }

    @Test
    fun neverSnowsInSummerAndNoBlossomInAutumn() {
        for (m in 6..8) assertFalse("snow in month $m", WeatherType.SNOW in seen(m))
        for (m in 10..11) assertFalse("blossom in month $m", WeatherType.SAKURA in seen(m))
    }

    @Test
    fun eachSeasonHasItsOwnWeatherMostOfTheTime() {
        val r = Random(3)
        fun share(month: Int, w: WeatherType) = (0 until 2000).count { SeasonalWeather.pick(month, "", r) == w } / 2000f
        assertTrue(share(1, WeatherType.SNOW) > 0.45f)
        assertTrue(share(4, WeatherType.SAKURA) > 0.5f)
        assertTrue(share(10, WeatherType.AUTUMN) > 0.5f)
    }

    @Test
    fun southOfTheEquatorDecemberIsSummer() {
        assertFalse(WeatherType.SNOW in seen(12, "AU"))
        assertTrue(WeatherType.SNOW in seen(7, "AU"))
    }

    @Test
    fun theWeatherChangesWithinTheSeason() {
        val r = Random(11)
        var w = WeatherType.SUNNY
        repeat(200) {
            val next = SeasonalWeather.next(w, 7, "", r)
            assertNotEquals(w, next)
            assertTrue(next in setOf(WeatherType.SUNNY, WeatherType.RAIN))
            w = next
        }
        // Picked by hand out of season: the season takes over at the next change.
        assertTrue(SeasonalWeather.next(WeatherType.SNOW, 7, "", r) in setOf(WeatherType.SUNNY, WeatherType.RAIN))
    }

    @Test
    fun everyMonthHasWeather() {
        for (m in 1..12) assertEquals(1f, SeasonalWeather.choices(m).sumOf { it.second.toDouble() }.toFloat(), 0.001f)
    }
}
