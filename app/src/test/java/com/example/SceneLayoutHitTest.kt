package com.example

import androidx.compose.ui.geometry.Offset
import com.example.scene.CafeLayout
import com.example.scene.CafeProp
import com.example.scene.CampfireLayout
import com.example.scene.CampfireProp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Taps on each drawn prop must reach that prop, on phone and tablet canvases alike. */
class SceneLayoutHitTest {

    private data class Canvas(val cw: Float, val ch: Float) {
        val p: Float get() = (cw / 115f).coerceIn(3f, 5f)
    }

    private val canvases = listOf(Canvas(1080f, 2400f), Canvas(720f, 1600f), Canvas(1600f, 2560f))

    // Mochi's load positions in each scene (SceneEngine.loadScene).
    private val cafeCat = Offset(0.84f, 0.70f)
    private val campCat = Offset(0.76f, 0.70f)

    private fun cafeHit(c: Canvas, tap: Offset, time: Float = 0f) =
        CafeLayout.hitTest(tap, c.cw, c.ch, c.p, time, cafeCat.x, cafeCat.y)

    private fun campHit(c: Canvas, tap: Offset) =
        CampfireLayout.hitTest(tap, c.cw, c.ch, c.p, campCat.x, campCat.y)

    @Test
    fun `cafe props are hit at their drawn centers`() {
        for (c in canvases) {
            val p = c.p
            assertEquals(c.toString(), CafeProp.LATTE, cafeHit(c, CafeLayout.latte(c.cw, c.ch, p)))
            assertEquals(c.toString(), CafeProp.PASTRY, cafeHit(c, CafeLayout.plate(c.cw, c.ch, p)))
            assertEquals(c.toString(), CafeProp.PUP, cafeHit(c, CafeLayout.pup(c.cw, c.ch, p)))
            assertEquals(c.toString(), CafeProp.BARISTA, cafeHit(c, CafeLayout.barista(c.cw, c.ch, p) - Offset(0f, 15f * p)))
            assertEquals(c.toString(), CafeProp.MENU, cafeHit(c, CafeLayout.menuTopLeft(c.cw, c.ch, p) + Offset(16f * p, 21f * p)))
        }
    }

    @Test
    fun `tapping Mochi in the cafe never pets the pup`() {
        for (c in canvases) {
            val mochi = Offset(c.cw * cafeCat.x, c.ch * cafeCat.y - 7f * c.p)
            assertEquals(c.toString(), CafeProp.MOCHI, cafeHit(c, mochi))
        }
    }

    @Test
    fun `window hearts only on the glass and passerby only when visible`() {
        val c = canvases.first()
        // Brick wall to the left of the window is not glass.
        assertNull(cafeHit(c, Offset(c.cw * 0.30f, c.ch * 0.10f)))

        // Find a moment when the passerby is mid-window, and one when they are out of view.
        val windowMidX = CafeLayout.window(c.cw, c.ch).center.x
        val visibleTime = (0 until 300).map { it * 0.1f }.first { (CafeLayout.passerby(c.cw, c.ch, c.p, it)?.x ?: 0f) > windowMidX }
        val hiddenTime = (0 until 300).map { it * 0.1f }.first { CafeLayout.passerby(c.cw, c.ch, c.p, it) == null }
        val passer = CafeLayout.passerby(c.cw, c.ch, c.p, visibleTime)!! - Offset(0f, 9f * c.p)
        assertEquals(CafeProp.PASSERBY, cafeHit(c, passer, visibleTime))
        assertNotEquals(CafeProp.PASSERBY, cafeHit(c, passer, hiddenTime))
        assertEquals(CafeProp.WINDOW, cafeHit(c, passer, hiddenTime))
    }

    @Test
    fun `campfire props are hit at their drawn centers`() {
        for (c in canvases) {
            val p = c.p
            assertEquals(c.toString(), CampfireProp.FIRE, campHit(c, CampfireLayout.fire(c.cw, c.ch)))
            assertEquals(c.toString(), CampfireProp.LANTERN, campHit(c, CampfireLayout.lantern(c.cw, c.ch, p) + Offset(0f, 5f * p)))
            assertEquals(c.toString(), CampfireProp.LANTERN, campHit(c, CampfireLayout.tentTopLeft(c.cw, c.ch, p) + CampfireLayout.tentSize(p) * 0.5f))
            assertEquals(c.toString(), CampfireProp.GUITAR, campHit(c, CampfireLayout.guitar(c.cw, c.ch, p) + Offset(p, 6f * p)))
            val mochi = Offset(c.cw * campCat.x, c.ch * campCat.y - 7f * p)
            assertEquals(c.toString(), CampfireProp.MOCHI, campHit(c, mochi))
        }
    }

    @Test
    fun `pit keep-out nudges positions to the nearest side`() {
        val half = CampfireLayout.PIT_KEEP_OUT_HALF_WIDTH
        assertEquals(CampfireLayout.PIT_X - half, CampfireLayout.avoidPit(CampfireLayout.PIT_X - 0.01f, 0.72f), 0.0001f)
        assertEquals(CampfireLayout.PIT_X + half, CampfireLayout.avoidPit(CampfireLayout.PIT_X + 0.01f, 0.72f), 0.0001f)
        // Behind the fire, or well to the side, nothing changes.
        assertEquals(CampfireLayout.PIT_X, CampfireLayout.avoidPit(CampfireLayout.PIT_X, 0.68f), 0.0001f)
        assertEquals(0.20f, CampfireLayout.avoidPit(0.20f, 0.75f), 0.0001f)
    }
}
