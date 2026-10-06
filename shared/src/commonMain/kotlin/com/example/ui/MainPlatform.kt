package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalWindowInfo
import com.example.data.PolaroidMemory
import com.example.data.PreferencesManager
import com.example.engine.WorldAudio
import com.example.scene.SceneType
import com.example.scene.WeatherType

/**
 * What the main screen needs from the platform app: the saved data, the sound, the photos, and the
 * few things only the platform can do (the widget, the app's background and foreground, taking a
 * picture of the screen). Android's lives in the app; iOS gets its own with the shared shell (S5).
 */
interface MainPlatform {
    val prefs: PreferencesManager

    /** Polaroids taken so far, for the gallery and Our Story. */
    val photos: PolaroidPhotos

    /** The world's sound for this screen; the screen releases it when it closes. */
    fun createAudio(soundOn: Boolean): WorldAudio

    /** The weather to open with: the last visit's if recent, else one from the season. */
    fun startWeather(): WeatherType

    /** Remembers the weather, so a quick return finds the same sky. */
    fun saveWeather(weather: WeatherType)

    /** Keeps the home-screen widget on the scene and weather the couple is in. */
    fun updateWidget(scene: SceneType, weather: WeatherType) {}

    /** The birthdays or their reminder setting changed: the platform reschedules its reminder (plan 09, A). */
    fun birthdaysChanged() {}

    /** Loads a profile file shipped with the app, if there is one; true when it was found. */
    fun loadLocalProfile(): Boolean = false

    /** The screen's size in pixels, which places effects and the camera. */
    @Composable
    fun screenSizePx(): Size {
        val size = LocalWindowInfo.current.containerSize
        return Size(size.width.toFloat(), size.height.toFloat())
    }

    /** Calls [onPause] when the app goes to the background and [onResume] when it comes back. */
    @Composable
    fun OnAppPauseResume(onPause: () -> Unit, onResume: () -> Unit)

    /** The camera for the heart button's Polaroids. */
    @Composable
    fun rememberPolaroidCamera(): PolaroidCamera
}

/** Takes a Polaroid of what is on screen. */
fun interface PolaroidCamera {
    /**
     * Captures the screen, makes the card (a title for [sceneEnvKey] and the time of day, today's
     * date, [sceneName]), saves it with the other Polaroids and returns the card and its memory;
     * null if the screen could not be captured.
     */
    suspend fun take(sceneName: String, sceneEnvKey: String, isNight: Boolean, isSunset: Boolean): Pair<ImageBitmap, PolaroidMemory>?
}
