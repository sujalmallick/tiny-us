package com.example.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.engine.QuietWorldAudio
import com.example.engine.WorldAudio
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.ui.PixelWorldView

/**
 * Lets a platform's own screens steer the shared world (plan 08, until S4 shares the screens too):
 * the couple's names and which place to show.
 */
object SharedWorldBridge {
    internal var names by mutableStateOf("You" to "Your person")
    internal var requestedScene by mutableStateOf<SceneType?>(null)

    fun setNames(first: String, second: String) {
        names = first to second
    }

    /** Shows the place named like [SceneType] (e.g. "CAMPFIRE"); unknown names are ignored. */
    fun showScene(sceneName: String) {
        requestedScene = SceneType.entries.firstOrNull { it.name == sceneName } ?: return
    }
}

/** The living pixel world, exactly as Android draws it: scenes, the couple, Mochi, weather, taps. */
@Composable
fun SharedWorldScreen(audio: WorldAudio = remember { QuietWorldAudio() }, modifier: Modifier = Modifier.fillMaxSize()) {
    val engine = remember {
        SceneEngine(audio = audio, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            val (first, second) = SharedWorldBridge.names
            updateNames(first, second)
            updateAtmosphereMode("AUTO")
            loadScene(SharedWorldBridge.requestedScene ?: nextRandomScene())
        }
    }
    val names = SharedWorldBridge.names
    LaunchedEffect(names) { engine.updateNames(names.first, names.second) }
    val requested = SharedWorldBridge.requestedScene
    LaunchedEffect(requested) {
        if (requested != null && requested != engine.currentScene) engine.loadScene(requested)
    }
    PixelWorldView(engine = engine, atmosphereMode = "AUTO", modifier = modifier)
}
