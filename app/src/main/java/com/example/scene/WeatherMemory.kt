package com.example.scene

import android.content.Context
import com.example.engine.SeasonalWeather
import java.util.Calendar
import java.util.Locale

/**
 * The weather carries on between visits: reopening the app within [CARRY_OVER_MS] keeps the same
 * weather; after that the season picks a fresh one.
 */
object WeatherMemory {
    private const val PREFS = "tiny_us_weather"
    private const val KEY_WEATHER = "weather"
    private const val KEY_SAVED_AT = "saved_at"

    /** How long the weather is remembered after the app was last open. */
    const val CARRY_OVER_MS = 45L * 60L * 1000L

    fun save(context: Context, weather: WeatherType, now: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_WEATHER, weather.name).putLong(KEY_SAVED_AT, now).apply()
    }

    /** The weather to open with: the remembered one if recent, else one from today's season. */
    fun startWeather(context: Context, now: Long = System.currentTimeMillis()): WeatherType {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = WeatherType.values().firstOrNull { it.name == prefs.getString(KEY_WEATHER, null) }
        if (saved != null && now - prefs.getLong(KEY_SAVED_AT, 0L) in 0..CARRY_OVER_MS) return saved
        return SeasonalWeather.pick(currentMonth(now), Locale.getDefault().country)
    }

    /** The month (1-12) at [now]. */
    fun currentMonth(now: Long = System.currentTimeMillis()): Int =
        Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.MONTH) + 1
}
