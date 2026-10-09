package com.example

import androidx.compose.ui.geometry.Offset
import com.example.engine.StageExtension
import com.example.engine.WorldCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldCameraTest {
    private fun stageAspect(c: WorldCamera) = c.stageH.toFloat() / c.stageW

    @Test
    fun phonesGetTheCloseUpStageWithBackgroundAboveAndBelow() {
        val c = WorldCamera.forScreen(1080f, 2400f, pixelRenderer = true)
        assertEquals(7, c.zoom)
        assertEquals(155, c.gameW)
        assertEquals(343, c.gameH)
        assertEquals(c.gameW, c.stageW)
        assertTrue(stageAspect(c) in WorldCamera.STAGE_MIN_ASPECT..WorldCamera.STAGE_MAX_ASPECT)
        assertTrue("background continues above", c.stageY > 0)
        assertTrue("background continues below", c.stageY + c.stageH < c.gameH)
        // One scene pixel is five world units, so the drawing code keeps its usual pixel size.
        assertEquals(c.stageW * 5f, c.worldW)
    }

    @Test
    fun everyScreenShowsAboutTheSameStageWidth() {
        for ((w, h) in listOf(720f to 1600f, 1080f to 2400f, 1344f to 2992f, 1440f to 3120f, 1600f to 2560f, 1600f to 1600f, 1200f to 1920f)) {
            val c = WorldCamera.forScreen(w, h, pixelRenderer = true)
            assertTrue("$w x $h stage ${c.stageW}", c.stageW >= WorldCamera.STAGE_MIN_W)
            assertTrue("$w x $h stage height ${c.stageH}", c.stageH <= c.gameH)
            if (h / w >= 4f / 3f) assertTrue("$w x $h is close-up", c.stageW < 175)
        }
    }

    @Test
    fun shortScreensShowTheWholeStageInsteadOfCroppingIt() {
        // The nearly square tablet window the scenes used to be cut off on.
        val c = WorldCamera.forScreen(1600f, 1600f, pixelRenderer = true)
        assertEquals(0, c.stageY)
        assertEquals(c.gameH, c.stageH)
        assertTrue(c.stageH >= (WorldCamera.STAGE_MIN_W * WorldCamera.STAGE_MIN_ASPECT).toInt())
    }

    @Test
    fun screenAndWorldPositionsConvertBothWays() {
        val c = WorldCamera.forScreen(1080f, 2400f, pixelRenderer = true)
        val world = Offset(312.5f, 640f)
        val back = c.toWorld(Offset(c.toScreenX(world.x), c.toScreenY(world.y)))
        assertEquals(world.x, back.x, 0.01f)
        assertEquals(world.y, back.y, 0.01f)
        // The stage's top-left corner is where the stage starts on screen.
        assertEquals(c.stageY * c.zoom.toFloat(), c.toScreenY(0f), 0.01f)
    }

    @Test
    fun theStageStaysClearOfTheButtonsWhenThereIsRoom() {
        // 1080 x 2400 phone, buttons in the top 200 px and bottom 240 px, an outdoor scene.
        val c = WorldCamera.forScreen(1080f, 2400f, pixelRenderer = true, aboveShare = 1f, topReservePx = 200f, bottomReservePx = 240f)
        assertTrue("clear of the top buttons", c.toScreenY(0f) >= 200f)
        assertTrue("clear of the heart button", c.toScreenY(c.worldH) <= 2400f - 240f)
        // Nearly all the spare height is sky, as asked.
        assertTrue(c.stageY * c.zoom > 2400 - c.stageH * c.zoom - 300)
    }

    @Test
    fun shortScreensShareTheSpareRowsBetweenTheButtonStrips() {
        assertEquals(0, WorldCamera.placeStage(0, 30, 30, 0.5f))
        assertEquals(10, WorldCamera.placeStage(20, 30, 30, 0.5f))
        assertEquals(30 + 20, WorldCamera.placeStage(100, 30, 30, 0.5f))
    }

    @Test
    fun theClassicRendererUsesTheScreenAsTheWorld() {
        val c = WorldCamera.forScreen(1080f, 2400f, pixelRenderer = false)
        assertFalse(c.staged)
        assertEquals(1080f, c.worldW)
        assertEquals(Offset(10f, 20f), c.toWorld(Offset(10f, 20f)))
    }

    @Test
    fun backgroundExtensionRepeatsPlanksAndCarriesColumnsOn() {
        val w = 20
        val h = 140
        val wood = 0xFF8B5A2B.toInt()
        val seam = 0xFF5A3A1A.toInt()
        val cord = 0xFF222222.toInt()
        val px = IntArray(w * h)
        val top = 20
        val bottom = 120 // a realistic stage: tall enough for a full sample band at each edge
        for (y in top until bottom) for (x in 0 until w) {
            px[y * w + x] = when {
                x == 7 -> cord
                (y - top) % 4 == 3 -> seam // a plank seam every fourth row
                else -> wood
            }
        }
        StageExtension.fill(px, w, h, top, bottom)
        // Below the stage everything settles into a soft foreground shade (a few 8% steps), so a
        // colour there is the expected one at one of those shade steps.
        fun shadeOf(c: Int, base: Int): Boolean = (0..3).any { l ->
            val k = 1f - l * 0.08f
            listOf(16, 8, 0).all { s -> ((c shr s) and 0xFF) == (((base shr s) and 0xFF) * k).toInt() }
        }
        // Below: seams keep coming every fourth row.
        for (y in bottom until h) {
            val expected = if ((y - top) % 4 == 3) seam else wood
            assertTrue("row $y", shadeOf(px[y * w + 3], expected))
        }
        // The cord column runs up and down through the extension.
        assertEquals(cord, px[0 * w + 7])
        assertTrue(shadeOf(px[(h - 1) * w + 7], cord))
        // Nothing is left transparent.
        assertTrue(px.none { it == 0 })
    }
}
