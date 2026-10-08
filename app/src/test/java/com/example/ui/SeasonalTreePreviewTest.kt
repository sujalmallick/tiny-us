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
            Triple(WeatherType.AUTUMN, "NIGHT", "autumn_night")
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

    /** The tree on its own in each season, so the carved heart on the trunk shows too. */
    @Test
    fun theTreeAlone() {
        val dir = out ?: return
        val p = 5f
        val cellW = 760
        val cellH = 820
        val weathers = listOf(WeatherType.SAKURA, WeatherType.SUNNY, WeatherType.AUTUMN, WeatherType.SNOW)
        val bmp = Bitmap.createBitmap(cellW * weathers.size, cellH, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(bmp.width.toFloat(), cellH.toFloat())) {
            for ((i, weather) in weathers.withIndex()) {
                val left = cellW * i.toFloat()
                drawRect(androidx.compose.ui.graphics.Color(0xFF9CD6F5), androidx.compose.ui.geometry.Offset(left, 0f), Size(cellW.toFloat(), cellH.toFloat()))
                val ground = cellH - 60f
                drawRect(androidx.compose.ui.graphics.Color(if (weather == WeatherType.SNOW) 0xFFF1F5F9 else 0xFF7DC26A), androidx.compose.ui.geometry.Offset(left, ground), Size(cellW.toFloat(), 60f))
                com.example.engine.WorldSprites.drawTree(this, left + cellW / 2f, ground, p, 3f, weather, mossStage = 2, gfInitial = 'M')
                com.example.engine.WorldSprites.drawTreeSwing(this, left + cellW / 2f, ground, p, 3f, weather)
            }
        }
        File(dir, "tree_alone.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(bmp.width > 0)
    }
}
