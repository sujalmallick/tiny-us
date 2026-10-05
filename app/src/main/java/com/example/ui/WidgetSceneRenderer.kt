package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlin.math.ceil

/**
 * The couple's scene as a small pixel picture for the home-screen widget (plan 06, H1).
 *
 * The scene is set up off screen, run forward a moment (so the fade is done and the couple has
 * settled), drawn with the same pixel renderer as the app, and cropped to a band around the couple
 * in the widget's shape. It's drawn at a whole-number zoom close to the widget's width, so the
 * launcher barely scales it and the pixels stay crisp. Safe off the main thread.
 */
object WidgetSceneRenderer {
    /** Game pixels across the stage; see [WorldCamera]. */
    private const val STAGE_W = WorldCamera.STAGE_MIN_W

    /** How far down the band the couple's feet sit: near the bottom, clear of the text strip at the top. */
    private const val FEET_IN_BAND = 0.93f

    fun render(
        scene: SceneType,
        weather: WeatherType,
        atmosphereMode: String,
        boyName: String,
        girlName: String,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val zoom = ceil(widthPx / STAGE_W.toFloat()).toInt().coerceIn(1, 6)
        val screenW = (STAGE_W * zoom).toFloat()
        val screenH = (192 * zoom).toFloat() // a full 4:3 stage
        val camera = WorldCamera.forScreen(screenW, screenH, scene, pixelRenderer = true)
        val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateNames(boyName, girlName)
            loadScene(scene)
            updateAtmosphereMode(atmosphereMode)
            weatherDriftEnabled = false
            this.weather = weather
            repeat(SETTLE_FRAMES) { update(1f / 60f, camera.worldW, camera.worldH) }
        }
        val world = Bitmap.createBitmap(screenW.toInt(), screenH.toInt(), Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(android.graphics.Canvas(world)), Size(screenW, screenH)) {
            drawWorld(engine, LowResWorldBuffer(), camera)
        }

        // A band in the widget's shape, with the couple near its bottom.
        val bandH = (screenW * heightPx / widthPx.coerceAtLeast(1)).toInt().coerceIn(1, screenH.toInt())
        val feetY = camera.toScreenY(camera.worldH * maxOf(engine.boy.worldY, engine.girl.worldY))
        val top = (feetY - bandH * FEET_IN_BAND).toInt().coerceIn(0, screenH.toInt() - bandH)
        return Bitmap.createBitmap(world, 0, top, screenW.toInt(), bandH)
    }

    /** Two and a half seconds of scene, enough for the fade and the couple's first steps. */
    private const val SETTLE_FRAMES = 150
}
