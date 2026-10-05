package com.example.widget

import android.content.Context
import com.example.scene.SceneType
import com.example.scene.WeatherType

/**
 * What the home-screen widget shows (plan 06, H2): the scene and weather the app last had.
 * Kept in its own small preferences file, written by the app and read by the widget.
 */
object WidgetState {
    private const val PREFS = "tiny_us_widget"
    private const val KEY_SCENE = "scene"
    private const val KEY_WEATHER = "weather"

    /** Remembers the app's scene and weather; true when they changed (the widget should redraw). */
    fun save(context: Context, scene: SceneType, weather: WeatherType): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_SCENE, null) == scene.name && prefs.getString(KEY_WEATHER, null) == weather.name) return false
        prefs.edit().putString(KEY_SCENE, scene.name).putString(KEY_WEATHER, weather.name).apply()
        return true
    }

    /** The scene to show: the app's last one, or the meadow before the app has run. */
    fun scene(context: Context): SceneType {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SCENE, null)
        return SceneType.values().firstOrNull { it.name == name } ?: SceneType.FLOWER
    }

    fun weather(context: Context): WeatherType {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_WEATHER, null)
        return WeatherType.values().firstOrNull { it.name == name } ?: WeatherType.SUNNY
    }
}
