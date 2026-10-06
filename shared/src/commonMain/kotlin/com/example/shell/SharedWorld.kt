package com.example.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.example.engine.GameText
import com.example.engine.QuietWorldAudio
import com.example.engine.WorldAudio
import com.example.resources.Res
import com.example.resources.scene_weather
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.ui.PixelWorldView
import kotlinx.coroutines.delay

/**
 * Lets a platform's own screens steer the shared world (plan 08, until S4 shares the screens too):
 * the couple's names, which place to show and the weather picked by hand. The world's weather
 * reads back through [weatherName].
 */
object SharedWorldBridge {
    internal var names by mutableStateOf("You" to "Your person")
    internal var requestedScene by mutableStateOf<SceneType?>(null)

    /** The latest weather picked by hand, numbered so picking the same one twice still counts. */
    internal var weatherPick by mutableStateOf<Pair<WeatherType, Int>?>(null)

    /** The weather the world is showing, or null before it starts. */
    internal var weather by mutableStateOf<WeatherType?>(null)

    fun setNames(first: String, second: String) {
        names = first to second
    }

    /** Shows the place named like [SceneType] (e.g. "CAMPFIRE"); unknown names are ignored. */
    fun showScene(sceneName: String) {
        requestedScene = SceneType.entries.firstOrNull { it.name == sceneName } ?: return
    }

    /** Changes the weather to the one named like [WeatherType] (e.g. "SNOW"), as the weather button does. */
    fun pickWeather(weatherName: String) {
        val picked = WeatherType.entries.firstOrNull { it.name == weatherName } ?: return
        weatherPick = picked to ((weatherPick?.second ?: 0) + 1)
    }

    /** The world's weather by [WeatherType] name ("SUNNY", "RAIN", ...), or "" before it starts. */
    fun weatherName(): String = weather?.name ?: ""
}

/** The living pixel world, exactly as Android draws it: scenes, the couple, Mochi, weather, taps. */
@Composable
fun SharedWorldScreen(
    audio: WorldAudio = remember { QuietWorldAudio() },
    modifier: Modifier = Modifier.fillMaxSize(),
    startWeather: WeatherType? = null,
    onWeather: (WeatherType) -> Unit = {},
    onFirstFrame: () -> Unit = {}
) {
    val engine = remember {
        SceneEngine(audio = audio, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            val (first, second) = SharedWorldBridge.names
            updateNames(first, second)
            updateAtmosphereMode("AUTO")
            loadScene(SharedWorldBridge.requestedScene ?: nextRandomScene())
            // Carry on the last visit's weather, or start from today's season (as Android does).
            if (startWeather != null) changeWeather(startWeather)
        }
    }
    val names = SharedWorldBridge.names
    LaunchedEffect(names) { engine.updateNames(names.first, names.second) }
    val requested = SharedWorldBridge.requestedScene
    LaunchedEffect(requested) {
        if (requested != null && requested != engine.currentScene) engine.loadScene(requested)
    }
    // A weather picked by hand: the same chime and message as Android's weather button, and the
    // season leaves it alone for a while.
    val pick = SharedWorldBridge.weatherPick
    LaunchedEffect(pick) {
        val picked = pick?.first ?: return@LaunchedEffect
        engine.changeWeather(picked)
        audio.playWindChime()
        engine.showMessage(GameText.get(Res.string.scene_weather, picked.displayName), duration = 2.5f)
        engine.weatherDriftEnabled = false
        try {
            delay((SceneEngine.MANUAL_WEATHER_HOLD_SECONDS * 1000).toLong())
        } finally {
            engine.weatherDriftEnabled = true
        }
    }
    LaunchedEffect(engine.weather) {
        SharedWorldBridge.weather = engine.weather
        onWeather(engine.weather)
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        onFirstFrame()
    }
    PixelWorldView(engine = engine, atmosphereMode = "AUTO", modifier = modifier)
}
