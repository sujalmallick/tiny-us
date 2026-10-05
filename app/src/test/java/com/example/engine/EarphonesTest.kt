package com.example.engine

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The shared earphones: buds beside the face, and a cord only between buds really drawn. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EarphonesTest {

    private fun render(block: DrawScope.() -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(200f, 200f), block)
        return bmp
    }

    private fun couple() = PixelCharacter(isGirl = false, name = "boy", worldX = 0.4f, worldY = 0.8f, direction = Direction.RIGHT, wearsEarphone = true) to
        PixelCharacter(isGirl = true, name = "girl", worldX = 0.6f, worldY = 0.8f, direction = Direction.LEFT, wearsEarphone = true)

    @Test
    fun theBudSitsBesideTheFaceOnTheSideTheyLook() {
        val (boy, girl) = couple()
        render {
            PixelArtRenderer.drawCharacter(this, boy, centerX = 60f, bottomY = 150f, pixelSize = 2f)
            PixelArtRenderer.drawCharacter(this, girl, centerX = 140f, bottomY = 150f, pixelSize = 2f)
        }
        val b = PixelArtRenderer.getEarphoneAttachmentOffset(boy, 0f, 0f, 1f)
        val g = PixelArtRenderer.getEarphoneAttachmentOffset(girl, 0f, 0f, 1f)
        assertTrue(b.isSpecified && g.isSpecified)
        // Facing each other, each bud is on the side toward the partner.
        assertTrue("boy's bud at ${b.x}", b.x > 60f)
        assertTrue("girl's bud at ${g.x}", g.x < 140f)
        // Mirror images of each other, at the same height.
        assertEquals(60f - b.x, g.x - 140f, 0.01f)
        assertEquals(b.y, g.y, 0.01f)
    }

    @Test
    fun theCordOnlyJoinsBudsDrawnThisFrame() {
        val (boy, girl) = couple()
        render {
            PixelArtRenderer.drawCharacter(this, boy, centerX = 60f, bottomY = 150f, pixelSize = 2f)
            PixelArtRenderer.drawCharacter(this, girl, centerX = 140f, bottomY = 150f, pixelSize = 2f)
        }
        val start = PixelArtRenderer.getEarphoneAttachmentOffset(boy, 0f, 0f, 1f)
        val end = PixelArtRenderer.getEarphoneAttachmentOffset(girl, 0f, 0f, 1f)
        val withCord = render { PixelArtRenderer.drawEarphoneCord(this, start, end, 2f, 0f) }
        assertTrue(drawnPixels(withCord) > 0)

        // Nothing drawn since (like the loft, which draws its own couple): no loose cord.
        val again = PixelArtRenderer.getEarphoneAttachmentOffset(boy, 0f, 0f, 1f)
        assertFalse(again.isSpecified)
        val none = render { PixelArtRenderer.drawEarphoneCord(this, again, PixelArtRenderer.getEarphoneAttachmentOffset(girl, 0f, 0f, 1f), 2f, 0f) }
        assertEquals(0, drawnPixels(none))
    }

    private fun drawnPixels(bmp: Bitmap): Int {
        var n = 0
        for (y in 0 until bmp.height) for (x in 0 until bmp.width) if (bmp.getPixel(x, y) ushr 24 != 0) n++
        return n
    }
}
