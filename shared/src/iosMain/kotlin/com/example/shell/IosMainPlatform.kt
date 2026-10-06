package com.example.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.data.IosUserDefaultsStorage
import com.example.data.PreferencesManager
import com.example.data.CoupleDates
import com.example.engine.TimeOfDayPhase
import com.example.engine.WorldAudio
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.ui.IosPolaroidPhotos
import com.example.ui.MainPlatform
import com.example.ui.PolaroidCamera
import com.example.ui.PolaroidPhotos
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * The main screen's platform side on iOS. Saved data lives in the App Group defaults, where the
 * SwiftUI app already kept the couple's names and anniversary under the same keys, so a couple
 * who started in the classic app carries on.
 */
class IosMainPlatform : MainPlatform {
    private val storage = IosUserDefaultsStorage.defaultStorage()
    override val prefs = PreferencesManager(storage)
    private val polaroids = IosPolaroidPhotos(storage)
    override val photos: PolaroidPhotos get() = polaroids

    init {
        // The classic app had its own welcome; a couple who went through it is not asked again.
        if (storage.getString("bf_name", null) != null && !prefs.isOnboardingCompleted) {
            prefs.isOnboardingCompleted = true
            prefs.namePromptAnswered = true
        }
    }

    override fun createAudio(soundOn: Boolean): WorldAudio = SharedSound.audio.apply { isEnabled = soundOn }

    override fun startWeather(): WeatherType = WeatherMemory.startWeather()

    override fun saveWeather(weather: WeatherType) = WeatherMemory.save(weather)

    override fun updateWidget(scene: SceneType, weather: WeatherType) {
        val publish = IosWidget.publish ?: return
        val data = prefs.getWidgetData(
            currentWeather = weather.displayName,
            timePhase = TimeOfDayPhase.resolve(prefs.atmosphereMode).displayName,
            sceneName = scene.title
        )
        publish(
            IosWidgetSnapshot(
                coupleNames = data.coupleNames,
                daysTogether = data.daysTogether,
                anniversary = CoupleDates.anniversary.toString(),
                sceneName = data.sceneName,
                weatherName = data.weatherName,
                timePhase = data.timePhase,
                dailyMomentPrompt = data.dailyMomentPrompt ?: "",
                latestSignalText = data.latestSignalText ?: ""
            )
        )
    }

    @Composable
    override fun OnAppPauseResume(onPause: () -> Unit, onResume: () -> Unit) {
        val pause by rememberUpdatedState(onPause)
        val resume by rememberUpdatedState(onResume)
        DisposableEffect(Unit) {
            val center = NSNotificationCenter.defaultCenter
            val queue = NSOperationQueue.mainQueue
            val background = center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, queue) { _ -> pause() }
            val foreground = center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, queue) { _ -> resume() }
            onDispose {
                center.removeObserver(background)
                center.removeObserver(foreground)
            }
        }
    }

    @Composable
    override fun rememberPolaroidCamera(): PolaroidCamera = remember {
        PolaroidCamera { sceneName, sceneEnvKey, isNight, isSunset -> polaroids.take(sceneName, sceneEnvKey, isNight, isSunset) }
    }
}
