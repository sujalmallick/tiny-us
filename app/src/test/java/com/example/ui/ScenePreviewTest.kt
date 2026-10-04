package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.FeatureFlags
import com.example.engine.AmbientAudio
import com.example.engine.TimeOfDayPhase
import com.example.engine.WorldViewport
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real world frame offscreen (Plan 03, Phase 0) and checks the low-res renderer's grid.
 *
 * Set SCENE_PREVIEW_DIR to also write PNGs of every scene at morning, sunset and night, drawn by
 * both renderers, for side-by-side review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScenePreviewTest {
    private val cw = 1080f
    private val ch = 2400f

    private fun engineFor(scene: SceneType, phase: TimeOfDayPhase) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            loadScene(scene)
            updateAtmosphereMode(if (phase == TimeOfDayPhase.NIGHT || phase == TimeOfDayPhase.SUNSET) phase.name else "DAY")
            // Let the scene-change fade finish and the characters settle (two seconds of frames).
            repeat(120) { update(1f / 60f, cw, ch) }
            check(wipeAlpha == 0f)
        }

    private fun render(block: DrawScope.() -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(cw, ch), block)
        return bmp
    }

    @Test
    fun campfireUsesTheLowResRenderer() {
        assertTrue(FeatureFlags.PIXEL_RENDERER)
        assertTrue(engineFor(SceneType.CAMPFIRE, TimeOfDayPhase.NIGHT).usesLowResRenderer)
        assertTrue(!engineFor(SceneType.FLOWER, TimeOfDayPhase.NIGHT).usesLowResRenderer)
    }

    @Test
    fun lowResFrameIsBuiltFromWholeGamePixels() {
        reallocateNightStars(42L)
        val scale = WorldViewport.gameScale(cw)
        assertEquals(5, scale)
        val engine = engineFor(SceneType.CAMPFIRE, TimeOfDayPhase.NIGHT)
        val buffer = LowResWorldBuffer()
        val screen = render { buffer.draw(this, scale) { drawWorldFrame(engine, lowRes = true) } }

        val frame = buffer.frame!!
        assertEquals(WorldViewport.gameWidth(cw), frame.width)
        assertEquals(WorldViewport.gameHeight(cw, ch), frame.height)

        // Every screen pixel equals the top-left pixel of its scale-by-scale block.
        var mismatches = 0
        for (y in 0 until ch.toInt()) for (x in 0 until cw.toInt()) {
            if (screen.getPixel(x, y) != screen.getPixel(x - x % scale, y - y % scale)) mismatches++
        }
        assertEquals("pixels that break the game-pixel grid", 0, mismatches)

        // The scene actually drew something: many distinct colours, nothing left transparent.
        val small = frame.asAndroidBitmap()
        val colours = HashSet<Int>()
        var transparent = 0
        for (y in 0 until small.height) for (x in 0 until small.width) {
            val c = small.getPixel(x, y)
            colours += c
            if (c ushr 24 == 0) transparent++
        }
        assertTrue("only ${colours.size} colours", colours.size > 40)
        assertEquals("transparent game pixels", 0, transparent)
    }

    @Test
    fun writesPreviewPngsWhenAsked() {
        val out = File(System.getenv("SCENE_PREVIEW_DIR") ?: return).apply { mkdirs() }
        val only = System.getenv("SCENE_PREVIEW_SCENES")?.split(',')?.map { it.trim() }?.toSet()
        val phases = listOf(TimeOfDayPhase.AFTERNOON to "day", TimeOfDayPhase.SUNSET to "sunset", TimeOfDayPhase.NIGHT to "night")
        for (scene in SceneType.values()) {
            if (only != null && scene.name !in only) continue
            for ((phase, label) in phases) {
                reallocateNightStars(42L)
                val engine = engineFor(scene, phase)
                val classic = render { drawWorldFrame(engine) }
                save(classic, File(out, "${scene.name.lowercase()}_${label}_classic.png"))
                classic.recycle()
                val pixel = render { LowResWorldBuffer().draw(this, WorldViewport.gameScale(cw)) { drawWorldFrame(engine, lowRes = true) } }
                save(pixel, File(out, "${scene.name.lowercase()}_${label}_pixel.png"))
                pixel.recycle()
            }
        }
    }

    private fun save(bmp: Bitmap, file: File) = file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
