package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.PolaroidManager
import com.example.data.PolaroidMemory
import com.example.data.PreferencesManager
import com.example.data.ProfileManager
import com.example.engine.AmbientAudio
import com.example.engine.WorldAudio
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherMemory
import com.example.scene.WeatherType
import com.example.widget.TinyUsWidgetProvider
import com.example.widget.WidgetState
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The shared main screen with Android's saved data, sound, photos, widget and lifecycle. */
@Composable
fun MainScreen(
    targetScene: String? = null,
    targetToken: Long = 0L,
    targetAtmosphere: String? = null,
    targetBirdSurface: String? = null,
    /** Pins the weather (a WeatherType name) instead of letting the season drift; for store screenshots. */
    targetWeather: String? = null,
    /** An engine already set up and run forward, shown instead of a new one (store screenshots). */
    previewEngine: SceneEngine? = null
) {
    val context = LocalContext.current
    val platform = remember { AndroidMainPlatform(context) }
    MainScreen(
        platform = platform,
        targetScene = targetScene,
        targetToken = targetToken,
        targetAtmosphere = targetAtmosphere,
        targetBirdSurface = targetBirdSurface,
        targetWeather = targetWeather,
        previewEngine = previewEngine
    )
}

/** Android's side of [MainPlatform], as MainScreen did it before it moved to shared code. */
class AndroidMainPlatform(private val context: Context) : MainPlatform {
    override val prefs = PreferencesManager(context)
    private val polaroids = PolaroidManager(context)
    override val photos: PolaroidPhotos get() = polaroids

    override fun createAudio(soundOn: Boolean): WorldAudio =
        AmbientAudio(context.applicationContext).apply { isEnabled = soundOn }

    override fun startWeather(): WeatherType = WeatherMemory.startWeather(context)

    override fun saveWeather(weather: WeatherType) = WeatherMemory.save(context, weather)

    override fun updateWidget(scene: SceneType, weather: WeatherType) {
        if (WidgetState.save(context, scene, weather)) TinyUsWidgetProvider.updateAllWidgets(context)
    }

    override fun loadLocalProfile(): Boolean = ProfileManager.loadFromLocalFile(context)

    @Composable
    override fun screenSizePx(): Size {
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        return with(density) { Size(config.screenWidthDp.dp.toPx(), config.screenHeightDp.dp.toPx()) }
    }

    @Composable
    override fun OnAppPauseResume(onPause: () -> Unit, onResume: () -> Unit) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> onPause()
                    Lifecycle.Event.ON_RESUME -> onResume()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    @Composable
    override fun rememberPolaroidCamera(): PolaroidCamera {
        val rootView = LocalView.current
        return remember(rootView) {
            PolaroidCamera { sceneName, sceneEnvKey, isNight, isSunset ->
                // Capture the screen as it is drawn now.
                val shot = withContext(Dispatchers.Main) {
                    runCatching {
                        val b = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                        rootView.draw(AndroidCanvas(b))
                        b
                    }.getOrNull()
                } ?: return@PolaroidCamera null

                val title = polaroids.pickTitle(sceneEnvKey = sceneEnvKey, isNight = isNight, isSunset = isSunset)
                val date = polaroids.formattedDate()
                val time = polaroids.formattedTime()
                val card = withContext(Dispatchers.Default) {
                    polaroids.renderPolaroidCard(sceneBitmap = shot, title = title, date = date, time = time, sceneName = sceneName)
                }
                val imagePath = withContext(Dispatchers.IO) { polaroids.saveBitmap(card) }
                val memory = PolaroidMemory(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    date = date,
                    time = time,
                    sceneName = sceneName,
                    sceneEnvKey = sceneEnvKey,
                    imagePath = imagePath
                )
                polaroids.savePolaroid(memory)
                card.asImageBitmap() to memory
            }
        }
    }
}
