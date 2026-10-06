package com.example.engine

import com.example.scene.WeatherType

/**
 * The weather carries on between visits: reopening the app within [CARRY_OVER_MS] keeps the same
 * weather; after that the season picks a fresh one. The platforms store the weather and when it
 * was saved; this decides what to open with.
 */
object WeatherCarryOver {
    /** How long the weather is remembered after the app was last open. */
    const val CARRY_OVER_MS = 45L * 60L * 1000L

    /** The weather to open with: [savedName] if saved within [CARRY_OVER_MS], else one from [month]'s season. */
    fun startWeather(savedName: String?, savedAtMs: Long, nowMs: Long, month: Int, region: String): WeatherType {
        val saved = WeatherType.entries.firstOrNull { it.name == savedName }
        if (saved != null && nowMs - savedAtMs in 0..CARRY_OVER_MS) return saved
        return SeasonalWeather.pick(month, region)
    }
}
