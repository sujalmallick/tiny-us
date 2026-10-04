package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rough per-scene frame cost of the pixel renderer on the JVM (plan 04, D6). Only runs with
 * FRAME_COST=1; prints milliseconds per frame so scenes can be compared with each other (the JVM
 * is not a phone, so the absolute numbers mean little).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrameCostTest {
    @Test
    fun printFrameCosts() {
        if (System.getenv("FRAME_COST") == null) return
        val cw = 1080f
        val ch = 2400f
        val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        val scope = CanvasDrawScope()
        val buffer = LowResWorldBuffer()
        val report = StringBuilder()
        for (scene in SceneType.values()) {
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
            engine.loadScene(scene)
            engine.updateAtmosphereMode("NIGHT")
            repeat(60) { engine.update(1f / 60f, camera.worldW, camera.worldH) }
            fun frame() {
                engine.update(1f / 60f, camera.worldW, camera.worldH)
                scope.draw(Density(1f), LayoutDirection.Ltr, canvas, Size(cw, ch)) { drawWorld(engine, buffer, camera) }
            }
            repeat(30) { frame() } // warm up
            val frames = 120
            val start = System.nanoTime()
            repeat(frames) { frame() }
            val ms = (System.nanoTime() - start) / 1e6 / frames
            // The world alone at game resolution (what the phone's CPU does), without the final
            // enlargement to the screen (which a phone does on the GPU).
            val tiny = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)))
            val start2 = System.nanoTime()
            repeat(frames) {
                engine.update(1f / 60f, camera.worldW, camera.worldH)
                scope.draw(Density(1f), LayoutDirection.Ltr, tiny, Size(cw, ch)) { drawWorld(engine, buffer, camera) }
            }
            val worldMs = (System.nanoTime() - start2) / 1e6 / frames
            val start3 = System.nanoTime()
            repeat(frames) { engine.update(1f / 60f, camera.worldW, camera.worldH) }
            val updateMs = (System.nanoTime() - start3) / 1e6 / frames
            report.append(String.format("FRAMECOST %-14s total %6.2f  world %6.2f  engine %5.2f ms/frame%n", scene.name, ms, worldMs, updateMs))
        }
        println(report)
    }
}
