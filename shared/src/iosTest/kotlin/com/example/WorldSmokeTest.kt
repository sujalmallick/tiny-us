package com.example

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.QuietWorldAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.ui.LowResWorldBuffer
import com.example.ui.drawWorld
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs the whole shared world headless on the iOS simulator (CI), where the app itself must run:
 * the engine for every scene, a few seconds of frames, and the pixel renderer drawing them.
 */
class WorldSmokeTest {
    private val width = 1080f
    private val height = 2340f

    @Test
    fun everySceneRunsAndDrawsHeadless() {
        val engine = SceneEngine(audio = QuietWorldAudio(), onOpenLoveNotes = {}, onOpenMemories = {})
        engine.updateNames("Bean", "Sprout")
        engine.updateAtmosphereMode("AUTO")
        val image = ImageBitmap(width.toInt(), height.toInt())
        val drawScope = CanvasDrawScope()
        val buffer = LowResWorldBuffer()
        for (scene in SceneType.entries) {
            engine.loadScene(scene)
            repeat(90) { engine.update(1f / 30f, width, height) }
            val camera = WorldCamera.forScreen(width, height, scene)
            drawScope.draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width, height)) {
                drawWorld(engine, buffer, camera)
            }
            val frame = buffer.frame
            assertTrue(frame != null && frame.width > 0 && frame.height > 0, "no frame drawn for $scene")
        }
    }
}
