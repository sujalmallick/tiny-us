package com.example.engine

import com.example.scene.WeatherType
import kotlin.test.Test
import kotlin.test.assertTrue

class SeasonalTreeTest {
    @Test
    fun birdsHaveBranchesToSitOnInEverySeason() {
        for (weather in WeatherType.entries) {
            val perches = SeasonalTree.perches(weather)
            assertTrue(perches.isNotEmpty(), "Somewhere to sit in $weather")
            // Above the trunk's fork, on a branch
            assertTrue(perches.all { it[1] > 45 }, "On the branches in $weather")
            // Low enough that the wind never bends the branch out from under them
            assertTrue(perches.all { it[1] <= 72 }, "On still branches in $weather")
        }
        // The bare winter branches have more room than the leafy ones
        assertTrue(SeasonalTree.perches(WeatherType.SNOW).size > SeasonalTree.perches(WeatherType.SUNNY).size)
    }

    @Test
    fun theSeasonsFollowTheWeather() {
        assertTrue(SeasonalTree.Season.of(WeatherType.SAKURA) == SeasonalTree.Season.SPRING)
        assertTrue(SeasonalTree.Season.of(WeatherType.SNOW) == SeasonalTree.Season.WINTER)
        assertTrue(SeasonalTree.lightFor(isNight = true, isSunset = false) == SeasonalTree.Light.NIGHT)
    }
}
