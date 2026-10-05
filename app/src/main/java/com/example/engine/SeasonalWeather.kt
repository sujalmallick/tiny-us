package com.example.engine

import com.example.scene.WeatherType
import kotlin.random.Random

/**
 * The weather follows the real season: mostly snow in winter, blossom in spring, sun and rain in
 * summer, falling leaves in autumn, with the odd sunny or rainy day mixed in. It never snows in
 * summer. South of the equator the seasons are turned around.
 */
object SeasonalWeather {
    /** Countries (ISO codes) south of the equator, where December is summer. */
    private val SOUTHERN = setOf("AU", "NZ", "AR", "CL", "UY", "PY", "ZA", "BR", "BO", "PE", "NA", "BW", "ZW", "MZ", "MG", "LS", "SZ", "FJ")

    /** Weighted weather for each month (January first), northern hemisphere. */
    private val BY_MONTH: List<List<Pair<WeatherType, Float>>> = listOf(
        listOf(WeatherType.SNOW to 0.55f, WeatherType.SUNNY to 0.30f, WeatherType.RAIN to 0.15f), // Jan
        listOf(WeatherType.SNOW to 0.55f, WeatherType.SUNNY to 0.30f, WeatherType.RAIN to 0.15f), // Feb
        listOf(WeatherType.SAKURA to 0.60f, WeatherType.SUNNY to 0.25f, WeatherType.RAIN to 0.15f), // Mar
        listOf(WeatherType.SAKURA to 0.60f, WeatherType.SUNNY to 0.25f, WeatherType.RAIN to 0.15f), // Apr
        listOf(WeatherType.SUNNY to 0.50f, WeatherType.SAKURA to 0.20f, WeatherType.RAIN to 0.30f), // May
        listOf(WeatherType.SUNNY to 0.55f, WeatherType.RAIN to 0.45f), // Jun
        listOf(WeatherType.SUNNY to 0.55f, WeatherType.RAIN to 0.45f), // Jul
        listOf(WeatherType.SUNNY to 0.55f, WeatherType.RAIN to 0.45f), // Aug
        listOf(WeatherType.SUNNY to 0.35f, WeatherType.AUTUMN to 0.35f, WeatherType.RAIN to 0.30f), // Sep
        listOf(WeatherType.AUTUMN to 0.60f, WeatherType.SUNNY to 0.20f, WeatherType.RAIN to 0.20f), // Oct
        listOf(WeatherType.AUTUMN to 0.60f, WeatherType.SUNNY to 0.20f, WeatherType.RAIN to 0.20f), // Nov
        listOf(WeatherType.SNOW to 0.55f, WeatherType.SUNNY to 0.30f, WeatherType.RAIN to 0.15f) // Dec
    )

    /** The weathers [month] (1-12) can have, with their weights; [country] flips it south of the equator. */
    fun choices(month: Int, country: String = ""): List<Pair<WeatherType, Float>> {
        val m = if (country.uppercase() in SOUTHERN) (month + 5) % 12 + 1 else month
        return BY_MONTH[(m - 1).coerceIn(0, 11)]
    }

    /** A weather for [month], picked by its weights. */
    fun pick(month: Int, country: String = "", random: Random = Random.Default): WeatherType = weighted(choices(month, country), random)

    /**
     * The next weather after [current]: another one of the season's (so it never snows in summer).
     * If [current] isn't one of the season's (it was picked by hand), the season takes over again.
     */
    fun next(current: WeatherType, month: Int, country: String = "", random: Random = Random.Default): WeatherType {
        val season = choices(month, country)
        val others = season.filter { it.first != current }
        return if (others.isEmpty()) current else weighted(others, random)
    }

    private fun weighted(options: List<Pair<WeatherType, Float>>, random: Random): WeatherType {
        var r = random.nextFloat() * options.sumOf { it.second.toDouble() }.toFloat()
        for ((w, weight) in options) {
            r -= weight
            if (r <= 0f) return w
        }
        return options.last().first
    }
}
