package com.example

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.AvatarAppearance
import com.example.data.AvatarPalette
import com.example.data.PreferencesManager
import com.example.engine.AvatarLook
import com.example.engine.Direction
import com.example.engine.LoftSprites
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.engine.WorldSprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Counts how many pixels of each colour a draw call paints. */
private class ColorCountingCanvas(private val delegate: Canvas = Canvas(android.graphics.Canvas())) : Canvas by delegate {
    val areaByColor = HashMap<Int, Float>()

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: androidx.compose.ui.graphics.Paint) {
        val c = paint.color.toArgb()
        areaByColor[c] = (areaByColor[c] ?: 0f) + (right - left) * (bottom - top)
        delegate.drawRect(left, top, right, bottom, paint)
    }

    fun area(color: Color): Float = areaByColor[color.toArgb()] ?: 0f
}

private fun render(block: DrawScope.() -> Unit): ColorCountingCanvas {
    val canvas = ColorCountingCanvas()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(600f, 600f), block)
    return canvas
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AvatarCustomizationTest {

    private fun character(isSlotB: Boolean, appearance: AvatarAppearance? = null) = PixelCharacter(
        isGirl = isSlotB, name = "", worldX = 0f, worldY = 0f, direction = Direction.RIGHT
    ).apply { if (appearance != null) look = AvatarLook.of(appearance) }

    @Test
    fun `appearance is saved per slot and defaults to the original look`() {
        val prefs = PreferencesManager(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(AvatarAppearance.DEFAULT_A, prefs.getAvatarAppearance(isSlotB = false))
        assertEquals(AvatarAppearance.DEFAULT_B, prefs.getAvatarAppearance(isSlotB = true))

        val custom = AvatarAppearance(skinTone = 6, hairColor = 4, longHair = true, wearsDress = false)
        prefs.setAvatarAppearance(isSlotB = true, appearance = custom)
        assertEquals(custom, PreferencesManager(ApplicationProvider.getApplicationContext<Context>()).getAvatarAppearance(isSlotB = true))
        assertEquals(AvatarAppearance.DEFAULT_A, prefs.getAvatarAppearance(isSlotB = false))
    }

    @Test
    fun `default characters still render with the original skin tones`() {
        val a = render { PixelArtRenderer.drawCharacter(this, character(isSlotB = false), 300f, 500f, 4f) }
        val b = render { PixelArtRenderer.drawCharacter(this, character(isSlotB = true), 300f, 500f, 4f) }
        assertTrue(a.area(PixelArtRenderer.SkinToneBoy) > 0f)
        assertTrue(b.area(PixelArtRenderer.SkinToneGirl) > 0f)
    }

    @Test
    fun `chosen skin and hair colours replace the defaults`() {
        val look = AvatarAppearance(skinTone = 6, hairColor = 3, longHair = false, wearsDress = false)
        val canvas = render { PixelArtRenderer.drawCharacter(this, character(isSlotB = false, appearance = look), 300f, 500f, 4f) }
        assertTrue(canvas.area(Color(AvatarPalette.skinTones[6].base)) > 0f)
        assertTrue(canvas.area(Color(AvatarPalette.hairColors[3].base)) > 0f)
        assertEquals(0f, canvas.area(PixelArtRenderer.SkinToneBoy))
    }

    @Test
    fun `long hair paints more hair than short hair for the same slot`() {
        val short = AvatarAppearance.DEFAULT_A
        val long = short.copy(longHair = true)
        val hair = Color(AvatarPalette.hairColors[short.hairColor].base)
        val shortArea = render { PixelArtRenderer.drawCharacter(this, character(false, short), 300f, 500f, 4f) }.area(hair)
        val longArea = render { PixelArtRenderer.drawCharacter(this, character(false, long), 300f, 500f, 4f) }.area(hair)
        assertTrue("long=$longArea short=$shortArea", longArea > shortArea)
    }

    @Test
    fun `scooter and sofa sprites use each partner's chosen skin`() {
        val deep = AvatarLook.of(AvatarAppearance(skinTone = 7, hairColor = 2, longHair = false, wearsDress = false))
        val scooter = render {
            WorldSprites.drawScooterWithCouple(this, cx = 300f, groundY = 450f, p = 4f, girlLook = deep)
        }
        assertTrue(scooter.area(deep.skin) > 0f)
        assertFalse(scooter.area(PixelArtRenderer.SkinToneGirl) > 0f)

        val sofa = render {
            LoftSprites.drawCuddledCouple(this, boyX = 250f, girlX = 330f, floorY = 450f, p = 4f, timeSeconds = 0f, boyLook = deep)
        }
        assertTrue(sofa.area(deep.skin) > 0f)
        assertEquals(0f, sofa.area(PixelArtRenderer.SkinToneBoy))
    }
}
