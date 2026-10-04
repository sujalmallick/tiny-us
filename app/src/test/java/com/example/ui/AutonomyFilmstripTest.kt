package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.autonomy.DiscoveryKind
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/**
 * Opt-in visual check of the autonomous behavior without an emulator: with
 * AUTONOMY_PREVIEW_DIR set (and --rerun), renders each scene every few seconds while the
 * couple acts on their own and writes one contact sheet per scene. AUTONOMY_PREVIEW_SCENES=A,B
 * limits the scenes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutonomyFilmstripTest {
    private val cw = 1080f
    private val ch = 2400f

    private fun render(w: Float, h: Float, block: DrawScope.() -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w, h), block)
        return bmp
    }

    @Test
    fun writesAutonomyFilmstripsWhenAsked() {
        val out = File(System.getenv("AUTONOMY_PREVIEW_DIR") ?: return).apply { mkdirs() }
        val only = System.getenv("AUTONOMY_PREVIEW_SCENES")?.split(',')?.map { it.trim() }?.toSet()
        val scenes = listOf(SceneType.SUNROOM, SceneType.CAMPFIRE, SceneType.FLOWER, SceneType.COZY_LOFT, SceneType.SEASIDE_PIER)
        val columns = 4
        val rows = 3
        val thumbW = cw / 3f
        val thumbH = ch / 3f
        for (scene in scenes) {
            if (only != null && scene.name !in only) continue
            reallocateNightStars(42L)
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
            engine.weatherDriftEnabled = false
            engine.updateAtmosphereMode("DAY")
            engine.behaviorBrain.random = Random(5)
            engine.loadScene(scene)
            repeat(60 * 9) { engine.update(1f / 60f, camera.worldW, camera.worldH) }
            engine.discovery.place(DiscoveryKind.WILDFLOWER, 0.30f, 0.76f)
            engine.discovery.age = 2.5f

            val sheet = Bitmap.createBitmap((thumbW * columns).toInt(), (thumbH * rows).toInt(), Bitmap.Config.ARGB_8888)
            val sheetCanvas = Canvas(sheet)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            val label = Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; isAntiAlias = true }
            for (i in 0 until columns * rows) {
                val frame = render(cw, ch) { drawWorld(engine, LowResWorldBuffer(), camera) }
                val thumb = Bitmap.createScaledBitmap(frame, thumbW.toInt(), thumbH.toInt(), true)
                val x = (i % columns) * thumbW
                val y = (i / columns) * thumbH
                sheetCanvas.drawBitmap(thumb, x, y, paint)
                val doing = "${engine.boyAgent.behavior ?: "-"} / ${engine.girlAgent.behavior ?: "-"}"
                sheetCanvas.drawText("t=${(9 + i * 4)}s  $doing", x + 10f, y + 36f, label)
                engine.sceneMessage?.let { sheetCanvas.drawText(it.take(40), x + 10f, y + thumbH - 16f, label) }
                frame.recycle()
                thumb.recycle()
                repeat(60 * 4) { engine.update(1f / 60f, camera.worldW, camera.worldH) }
            }
            File(out, "autonomy_${scene.name.lowercase()}.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
            sheet.recycle()
        }
    }
}
