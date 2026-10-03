package com.example

import androidx.compose.ui.geometry.Offset
import com.example.scene.PierLayout
import com.example.scene.PierProp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Every Seaside Pier prop is tappable on its own sprite, on phone and tablet canvases. */
class PierLayoutHitTest {

    private data class Canvas(val cw: Float, val ch: Float) {
        val p: Float get() = (cw / 115f).coerceIn(3f, 5f)
    }

    private val canvases = listOf(Canvas(1080f, 2400f), Canvas(720f, 1600f), Canvas(1600f, 2560f))

    // Mochi's load spot (SceneEngine.loadScene) and a railing perch for Pip.
    private val cat = Offset(0.66f, 0.73f)
    private val perch = Offset(PierLayout.PERCH_XS[1], PierLayout.RAIL_Y)

    private fun hit(c: Canvas, tap: Offset, gullVisible: Boolean = true, bottleVisible: Boolean = true) =
        PierLayout.hitTest(
            tap, c.cw, c.ch, c.p, time = 0f,
            catWorldX = cat.x, catWorldY = cat.y,
            gullX = perch.x, gullY = perch.y,
            gullVisible = gullVisible, bottleVisible = bottleVisible
        )

    @Test
    fun `each prop is hit at its drawn position`() {
        for (c in canvases) {
            val p = c.p
            assertEquals(c.toString(), PierProp.BAO, hit(c, PierLayout.bao(c.cw, c.ch) - Offset(0f, 12f * p)))
            assertEquals(c.toString(), PierProp.CART, hit(c, PierLayout.cart(c.cw, c.ch) - Offset(0f, 15f * p)))
            assertEquals(c.toString(), PierProp.BOTTLE, hit(c, PierLayout.bottle(c.cw, c.ch)))
            assertEquals(c.toString(), PierProp.LIGHTHOUSE, hit(c, PierLayout.lighthouseLamp(c.cw, c.ch, p)))
            assertEquals(c.toString(), PierProp.LIGHTHOUSE, hit(c, PierLayout.lighthouseBase(c.cw, c.ch) - Offset(0f, 10f * p)))
            assertEquals(c.toString(), PierProp.PIP, hit(c, Offset(c.cw * perch.x, c.ch * perch.y - 4f * p)))
            assertEquals(c.toString(), PierProp.MOCHI, hit(c, Offset(c.cw * cat.x, c.ch * cat.y - 7f * p)))
        }
    }

    @Test
    fun `hidden props cannot be tapped and fall through to the sea`() {
        val c = canvases.first()
        val bottle = PierLayout.bottle(c.cw, c.ch)
        assertEquals(PierProp.SEA, hit(c, bottle, bottleVisible = false))
        val pip = Offset(c.cw * perch.x, c.ch * perch.y - 4f * c.p)
        assertNotEquals(PierProp.PIP, hit(c, pip, gullVisible = false))
    }

    @Test
    fun `open water and empty boardwalk`() {
        val c = canvases.first()
        assertEquals(PierProp.SEA, hit(c, Offset(c.cw * 0.50f, c.ch * 0.50f)))
        assertNull(hit(c, Offset(c.cw * 0.30f, c.ch * 0.90f)))
        // The sky above the horizon is left for constellations.
        assertNull(hit(c, Offset(c.cw * 0.40f, c.ch * 0.20f)))
    }
}
