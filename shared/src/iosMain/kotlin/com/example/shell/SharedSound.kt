package com.example.shell

import androidx.compose.ui.text.intl.Locale
import com.example.engine.IosWorldAudio
import com.example.engine.WeatherCarryOver
import com.example.scene.WeatherType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSUserDefaults
import kotlin.time.Clock

/**
 * The shared world's sound for SwiftUI (`SharedSound.shared`): Settings' sound switch and volume,
 * pausing in the background, and stopping when the world is closed.
 */
object SharedSound {
    val audio: IosWorldAudio by lazy { IosWorldAudio() }

    fun configure(enabled: Boolean, volume: Float) {
        audio.volume = volume
        audio.isEnabled = enabled
    }

    fun pauseAll() = audio.pauseAll()

    fun resumeAll() = audio.resumeAll()

    /** The world view went away: silence it (a new world starts its own music). */
    fun stop() = audio.release()
}

/** Android's WeatherMemory for iOS: a quick return finds the same sky. */
internal object WeatherMemory {
    private const val KEY_WEATHER = "tinyus.shared.weather"
    private const val KEY_SAVED_AT = "tinyus.shared.weatherSavedAt"
    private val defaults get() = NSUserDefaults.standardUserDefaults

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    fun save(weather: WeatherType) {
        defaults.setObject(weather.name, KEY_WEATHER)
        defaults.setDouble(nowMs().toDouble(), KEY_SAVED_AT)
    }

    fun startWeather(): WeatherType {
        val month = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).month.number
        return WeatherCarryOver.startWeather(
            savedName = defaults.stringForKey(KEY_WEATHER),
            savedAtMs = defaults.doubleForKey(KEY_SAVED_AT).toLong(),
            nowMs = nowMs(),
            month = month,
            region = Locale.current.region
        )
    }
}
