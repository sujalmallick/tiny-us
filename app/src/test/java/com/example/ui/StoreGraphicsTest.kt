package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Google Play graphics (plan 06, E3): the 1024 x 500 feature graphic and the 512 x 512 icon.
 * Runs only when STORE_SHOTS_DIR is set (use --rerun).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreGraphicsTest {

    private val outDir: File? get() = System.getenv("STORE_SHOTS_DIR")?.let { File(it, "graphics") }

    /**
     * The meadow drawn on a wide stage at 4x, cropped to a band from above the cottage roof to the
     * grass, with the title in the pixel font over the open sky on the right.
     */
    @Test
    fun featureGraphic() {
        val dir = outDir
        assumeTrue("Set STORE_SHOTS_DIR to render store graphics", dir != null)
        dir!!.mkdirs()
        val screenW = 1024f
        val screenH = 768f // 192 game pixels at 4x: a wide 256 x 192 stage
        val camera = WorldCamera.forScreen(screenW, screenH, SceneType.FLOWER, pixelRenderer = true)
        val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateNames("Leo", "Mia")
            loadScene(SceneType.FLOWER)
            updateAtmosphereMode("DAY")
            weatherDriftEnabled = false
            weather = com.example.scene.WeatherType.SUNNY
            repeat(180) { update(1f / 60f, camera.worldW, camera.worldH) }
        }
        val world = Bitmap.createBitmap(screenW.toInt(), screenH.toInt(), Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(
            Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(Canvas(world)), Size(screenW, screenH)
        ) { drawWorld(engine, LowResWorldBuffer(), camera) }

        // Ground line in screen pixels; the band runs from above the roof down past the couple's feet.
        val groundY = camera.toScreenY(com.example.scene.MeadowLayout.groundY(camera.worldH)).toInt()
        val top = (groundY - 400).coerceIn(0, screenH.toInt() - 500)
        val banner = Bitmap.createBitmap(world, 0, top, 1024, 500)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val font = ResourcesCompat.getFont(context, R.font.tiny_pixel)
        val canvas = Canvas(banner)
        // Largest size (in steps of 8, which keeps the pixel font's blocks even) that fits maxW.
        fun fit(s: String, maxW: Float, start: Float): Float {
            var size = start
            val paint = Paint().apply { typeface = font }
            while (size > 8f) { paint.textSize = size; if (paint.measureText(s) <= maxW) break; size -= 8f }
            return size
        }
        fun text(s: String, x: Float, y: Float, size: Float, color: Int, outline: Int) {
            val paint = Paint().apply { typeface = font; textSize = size; isAntiAlias = false }
            paint.color = outline
            for (dx in -1..1) for (dy in -1..1) if (dx != 0 || dy != 0) canvas.drawText(s, x + dx * size / 16f, y + dy * size / 16f, paint)
            paint.color = color
            canvas.drawText(s, x, y, paint)
        }
        val outline = Color.rgb(0x5A, 0x2E, 0x3C)
        val left = 560f
        val titleSize = fit("Tiny Us", 1000f - left, 128f)
        val tagSize = fit("a cozy pixel world for two", 1000f - left, 40f)
        text("Tiny Us", left, 24f + titleSize, titleSize, Color.WHITE, outline)
        text("a cozy pixel world for two", left + 4f, 24f + titleSize + 16f + tagSize, tagSize, Color.rgb(0xFF, 0xF4, 0xE6), outline)
        save(banner, File(dir, "feature_graphic_1024x500.png"))
    }

    /**
     * The launcher icon as a full 512 x 512 square, as Play asks for (Play applies its own mask).
     * The adaptive icon's layers are 108 units with the middle 72 visible, so they're drawn 1.5x
     * larger and centred.
     */
    @Test
    fun icon512() {
        val dir = outDir
        assumeTrue("Set STORE_SHOTS_DIR to render store graphics", dir != null)
        dir!!.mkdirs()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        for (layer in listOf(icon.background, icon.foreground)) {
            layer.setBounds(-128, -128, 640, 640)
            layer.draw(canvas)
        }
        save(out, File(dir, "icon_512.png"))
    }

    private fun save(bmp: Bitmap, file: File) = file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
