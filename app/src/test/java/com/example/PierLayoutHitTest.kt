package com.example

import androidx.compose.ui.geometry.Offset
import com.example.scene.PierLayout
import com.example.scene.PierProp
import com.example.scene.PierTapState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Every Seaside Pier prop is tappable on its own sprite, on phone and tablet canvases. */
class PierLayoutHitTest {

    private data class Canvas(val cw: Float, val ch: Float) {
        val p: Float get() = (cw / 115f).coerceIn(3f, 5f)
    }

    private val canvases = listOf(Canvas(1080f, 2400f), Canvas(720f, 1600f), Canvas(1600f, 2560f))

    // Mochi's load spot (SceneEngine.loadScene), a railing perch for Pip, and Pinchy mid-patrol.
    private val cat = Offset(0.66f, 0.73f)
    private val perch = Offset(PierLayout.PERCH_XS[1], PierLayout.RAIL_Y)
    private val crabX = 0.36f

    private fun state(gullVisible: Boolean = true, bottleVisible: Boolean = true, crabVisible: Boolean = true) =
        PierTapState(perch.x, perch.y, gullVisible, bottleVisible, crabX, crabVisible)

    private fun hit(c: Canvas, tap: Offset, time: Float = 0f, state: PierTapState = state()) =
        PierLayout.hitTest(tap, c.cw, c.ch, c.p, time, cat.x, cat.y, state)

    /** A moment when the sailboat is mid-way along the horizon. */
    private val boatTime = (0 until 2000).map { it * 0.5f }.first { t ->
        PierLayout.boat(1f, 1f, t)?.let { it.x in 0.40f..0.50f } == true
    }

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
            assertEquals(c.toString(), PierProp.TELESCOPE, hit(c, PierLayout.telescope(c.cw, c.ch) - Offset(0f, 9f * p)))
            assertEquals(c.toString(), PierProp.BUCKET, hit(c, PierLayout.bucket(c.cw, c.ch) - Offset(0f, 4f * p)))
            assertEquals(c.toString(), PierProp.CRAB, hit(c, PierLayout.crab(c.cw, c.ch, crabX) - Offset(0f, 2f * p)))
            val boat = PierLayout.boat(c.cw, c.ch, boatTime)
            assertNotNull(boat)
            assertEquals(c.toString(), PierProp.BOAT, hit(c, boat!! - Offset(0f, 6f * p), time = boatTime))
        }
    }

    @Test
    fun `string lights run along the railing between perches`() {
        val c = canvases.first()
        assertEquals(PierProp.LIGHTS, hit(c, Offset(c.cw * 0.10f, c.ch * PierLayout.RAIL_Y - 2f * c.p)))
    }

    @Test
    fun `hidden props cannot be tapped and fall through`() {
        val c = canvases.first()
        val bottle = PierLayout.bottle(c.cw, c.ch)
        assertEquals(PierProp.SEA, hit(c, bottle, state = state(bottleVisible = false)))
        val pip = Offset(c.cw * perch.x, c.ch * perch.y - 4f * c.p)
        assertNotEquals(PierProp.PIP, hit(c, pip, state = state(gullVisible = false)))
        val crab = PierLayout.crab(c.cw, c.ch, crabX) - Offset(0f, 2f * c.p)
        assertNull(hit(c, crab, state = state(crabVisible = false)))
    }

    @Test
    fun `open water and empty boardwalk`() {
        val c = canvases.first()
        assertEquals(PierProp.SEA, hit(c, Offset(c.cw * 0.50f, c.ch * 0.50f)))
        assertNull(hit(c, Offset(c.cw * 0.55f, c.ch * 0.92f)))
        // The sky above the horizon is left for constellations.
        assertNull(hit(c, Offset(c.cw * 0.40f, c.ch * 0.20f)))
    }
}
