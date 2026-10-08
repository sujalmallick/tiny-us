package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.AmbientAudio
import com.example.engine.GameText
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The tree on the hill in each season, by day, at sunset and at night, in the real scene. TREE_PREVIEW_DIR=dir. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SeasonalTreePreviewTest {
    private val out: File? get() = System.getenv("TREE_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }

    @Test
    fun theTreeThroughTheYear() {
        val dir = out ?: return
        runBlocking { GameText.load() }
        val cw = 1080f
        val ch = 2400f
        val shots = listOf(
            Triple(WeatherType.SAKURA, "DAY", "spring"),
            Triple(WeatherType.SUNNY, "DAY", "summer"),
            Triple(WeatherType.AUTUMN, "DAY", "autumn"),
            Triple(WeatherType.SNOW, "DAY", "winter"),
            Triple(WeatherType.RAIN, "DAY", "rain"),
            Triple(WeatherType.SUNNY, "SUNSET", "summer_sunset"),
            Triple(WeatherType.AUTUMN, "NIGHT", "autumn_night"),
            Triple(WeatherType.SUNNY, "DAY", "summer_heart")
        )
        for ((weather, mode, name) in shots) {
            val camera = WorldCamera.forScreen(cw, ch, SceneType.UNDER_TREE, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                weatherDriftEnabled = false
                updateAtmosphereMode(mode)
                loadScene(SceneType.UNDER_TREE)
                autonomyEnabled = false
                this.weather = weather
            }
            var t = 0f
            while (t < 9f) { engine.update(1f / 20f, camera.worldW, camera.worldH); t += 1f / 20f }
            if (name.endsWith("heart")) {
                // The two of them stepped aside, so the carved heart on the trunk shows
                engine.boy.worldX = 0.16f
                engine.girl.worldX = 0.86f
            }
            val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(cw, ch)) {
                drawWorld(engine, LowResWorldBuffer(), camera)
            }
            val crop = Bitmap.createBitmap(bmp, 0, (ch * 0.22f).toInt(), cw.toInt(), (ch * 0.62f).toInt())
            File(dir, "tree_$name.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }
        assertTrue(true)
    }
}
