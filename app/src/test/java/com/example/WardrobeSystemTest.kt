package com.example

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.engine.Direction
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WardrobeSystemTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val sharedPrefs = context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        prefs = PreferencesManager(context)
    }

    @Test
    fun `verify girl dress and hoodie palettes count and fallback integrity`() {
        assertEquals(11, PixelArtRenderer.GirlDressPalettes.size)

        // Palette 0: Signature Blush Strawberry
        assertEquals(Color(0xFFF5CAC3), PixelArtRenderer.GirlDressPalettes[0].sweater)
        assertEquals(Color(0xFFD6587A), PixelArtRenderer.GirlDressPalettes[0].skirt)

        // Palette 7: Blush Rose Cropped Hoodie
        val blushRoseHoodie = PixelArtRenderer.GirlDressPalettes[7]
        assertEquals(Color(0xFFF4ACB7), blushRoseHoodie.sweater)
        assertEquals(Color(0xFFFFCAD4), blushRoseHoodie.trim)

        // Palette 8: Sage & Cream Colorblock Hoodie
        val sageCreamHoodie = PixelArtRenderer.GirlDressPalettes[8]
        assertEquals(Color(0xFF84A98C), sageCreamHoodie.sweater)
        assertEquals(Color(0xFFF8F9FA), sageCreamHoodie.trim)

        // Palette 9: Lavender Cloud Oversized Hoodie
        val lavenderCloudHoodie = PixelArtRenderer.GirlDressPalettes[9]
        assertEquals(Color(0xFFD8BBFF), lavenderCloudHoodie.sweater)

        // Palette 10: Buttercream Star Shimmer Hoodie
        val buttercreamHoodie = PixelArtRenderer.GirlDressPalettes[10]
        assertEquals(Color(0xFFFFF1C5), buttercreamHoodie.sweater)

        // Fallback checks for negative and out-of-bounds indices
        val fallbackUnder = PixelArtRenderer.getGirlDressPalette(-1)
        val fallbackOver = PixelArtRenderer.getGirlDressPalette(99)
        assertEquals(PixelArtRenderer.GirlDressPalettes[0], fallbackUnder)
        assertEquals(PixelArtRenderer.GirlDressPalettes[0], fallbackOver)
    }

    @Test
    fun `verify boy outfit and hoodie palettes count and fallback integrity`() {
        assertEquals(5, PixelArtRenderer.BoyOutfitPalettes.size)

        // Outfit 0: Classic Spruce Knit & Navy Pants
        val classicSpruce = PixelArtRenderer.BoyOutfitPalettes[0]
        assertEquals(Color(0xFF2D6A4F), classicSpruce.sweater)
        assertEquals(Color(0xFF2B334B), classicSpruce.pants)
        assertFalse(classicSpruce.isHoodie)

        // Outfit 1: White & Emerald Varsity Hoodie (Required by prompt)
        val varsityHoodie = PixelArtRenderer.BoyOutfitPalettes[1]
        assertEquals(Color(0xFFF8F9FA), varsityHoodie.sweater)
        assertEquals(Color(0xFF2D6A4F), varsityHoodie.collar)
        assertEquals(Color(0xFFB08968), varsityHoodie.pants)
        assertTrue(varsityHoodie.isHoodie)

        // Outfit 2: Charcoal Streetwear Zip Hoodie
        val charcoalHoodie = PixelArtRenderer.BoyOutfitPalettes[2]
        assertEquals(Color(0xFF343A40), charcoalHoodie.sweater)
        assertTrue(charcoalHoodie.isHoodie)

        // Outfit 3: Oatmeal Cloud Oversized Hoodie
        val oatmealHoodie = PixelArtRenderer.BoyOutfitPalettes[3]
        assertEquals(Color(0xFFEDE0D4), oatmealHoodie.sweater)
        assertTrue(oatmealHoodie.isHoodie)

        // Outfit 4: Midnight Starlight Graphic Hoodie
        val midnightHoodie = PixelArtRenderer.BoyOutfitPalettes[4]
        assertEquals(Color(0xFF1E293B), midnightHoodie.sweater)
        assertTrue(midnightHoodie.isHoodie)

        // Fallback checks
        val fallbackUnder = PixelArtRenderer.getBoyOutfitPalette(-5)
        val fallbackOver = PixelArtRenderer.getBoyOutfitPalette(50)
        assertEquals(PixelArtRenderer.BoyOutfitPalettes[0], fallbackUnder)
        assertEquals(PixelArtRenderer.BoyOutfitPalettes[0], fallbackOver)
    }

    @Test
    fun `verify preferences persistence for both characters outfits and accessories`() {
        // Defaults
        assertEquals(0, prefs.girlOutfitIndex)
        assertEquals(0, prefs.girlAccessoryIndex)
        assertEquals(0, prefs.boyOutfitIndex)
        assertEquals(0, prefs.boyAccessoryIndex)

        // Mutate selections
        prefs.girlOutfitIndex = 7      // Blush Rose Hoodie
        prefs.girlAccessoryIndex = 1   // Cozy Beanie
        prefs.boyOutfitIndex = 1       // White & Emerald Varsity Hoodie
        prefs.boyAccessoryIndex = 3    // Baseball Cap

        // Verify direct read
        assertEquals(7, prefs.girlOutfitIndex)
        assertEquals(1, prefs.girlAccessoryIndex)
        assertEquals(1, prefs.boyOutfitIndex)
        assertEquals(3, prefs.boyAccessoryIndex)

        // Reload fresh PreferencesManager instance to verify disk persistence
        val reloadedPrefs = PreferencesManager(context)
        assertEquals(7, reloadedPrefs.girlOutfitIndex)
        assertEquals(1, reloadedPrefs.girlAccessoryIndex)
        assertEquals(1, reloadedPrefs.boyOutfitIndex)
        assertEquals(3, reloadedPrefs.boyAccessoryIndex)

        // Clamping checks
        prefs.girlOutfitIndex = 100
        assertEquals(20, prefs.girlOutfitIndex)
        prefs.girlAccessoryIndex = 99
        assertEquals(10, prefs.girlAccessoryIndex)
        prefs.boyOutfitIndex = -10
        assertEquals(0, prefs.boyOutfitIndex)
        prefs.boyAccessoryIndex = -5
        assertEquals(0, prefs.boyAccessoryIndex)
    }

    @Test
    fun `verify layering of outfit and accessory on PixelCharacter`() {
        val boy = PixelCharacter(
            isGirl = false,
            name = "Him",
            worldX = 0.5f,
            worldY = 0.7f,
            wearsGlasses = true,
            outfitIndex = 1,      // White & Emerald Hoodie
            accessoryIndex = 1    // Cozy Beanie
        )

        // Independent selection test
        assertEquals(1, boy.outfitIndex)
        assertEquals(1, boy.accessoryIndex)
        assertTrue(boy.wearsGlasses)

        // Change accessory without altering outfit
        boy.accessoryIndex = 2 // Scarf
        assertEquals(1, boy.outfitIndex)
        assertEquals(2, boy.accessoryIndex)

        // Change outfit without altering accessory
        boy.outfitIndex = 2 // Charcoal Hoodie
        assertEquals(2, boy.outfitIndex)
        assertEquals(2, boy.accessoryIndex)
    }

    @Test
    fun `verify glasses render in front of accessories`() {
        // Record draw operations to verify glasses frame is drawn after accessory
        val drawLog = mutableListOf<String>()
        val mockCanvas = object : Canvas by Canvas(android.graphics.Canvas()) {
            override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: androidx.compose.ui.graphics.Paint) {
                val color = paint.color
                if (color == PixelArtRenderer.GlassesFrame || color == PixelArtRenderer.GlassesFrameLight) {
                    drawLog.add("GLASSES")
                } else if (color == Color(0xFF264653) || color == Color(0xFF1B4332) || color == Color(0xFFE9C46A)) {
                    drawLog.add("ACCESSORY_BEANIE")
                }
            }
        }

        val scope = CanvasDrawScope()
        val boy = PixelCharacter(
            isGirl = false,
            name = "Him",
            worldX = 0.5f,
            worldY = 0.7f,
            wearsGlasses = true,
            outfitIndex = 1,
            accessoryIndex = 1 // Beanie
        )

        scope.draw(Density(1f), LayoutDirection.Ltr, mockCanvas, Size(400f, 400f)) {
            PixelArtRenderer.drawCharacter(
                drawScope = this,
                char = boy,
                centerX = 200f,
                bottomY = 200f,
                pixelSize = 3.5f
            )
        }

        val firstBeanieIdx = drawLog.indexOf("ACCESSORY_BEANIE")
        val lastGlassesIdx = drawLog.lastIndexOf("GLASSES")

        assertTrue("Beanie accessory should be drawn", firstBeanieIdx >= 0)
        assertTrue("Glasses should be drawn", lastGlassesIdx >= 0)
        assertTrue(
            "Glasses must render AFTER (in front of) beanie accessory. firstBeanie=$firstBeanieIdx, lastGlasses=$lastGlassesIdx",
            lastGlassesIdx > firstBeanieIdx
        )
    }
}
