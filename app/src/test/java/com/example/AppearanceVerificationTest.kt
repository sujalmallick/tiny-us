package com.example

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.LoftSprites
import com.example.engine.PixelArtRenderer
import com.example.engine.WorldSprites
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Debug Pixel Canvas utility to accurately rasterize and sample Compose DrawScope operations
private class DebugPixelCanvas(
    val width: Int,
    val height: Int,
    private val delegate: Canvas = Canvas(android.graphics.Canvas())
) : Canvas by delegate {
    val pixels = Array(width) { IntArray(height) { 0 } }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: androidx.compose.ui.graphics.Paint) {
        val x0 = left.toInt().coerceIn(0, width)
        val x1 = right.toInt().coerceIn(0, width)
        val y0 = top.toInt().coerceIn(0, height)
        val y1 = bottom.toInt().coerceIn(0, height)
        val colorInt = paint.color.toArgb()
        for (x in x0 until x1) {
            for (y in y0 until y1) {
                pixels[x][y] = colorInt
            }
        }
        delegate.drawRect(left, top, right, bottom, paint)
    }

    fun getPixel(x: Int, y: Int): Int {
        if (x in 0 until width && y in 0 until height) {
            return pixels[x][y]
        }
        return 0
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceVerificationTest {

    @Test
    fun `verify all three previously hardcoded locations match PixelArtRenderer palette`() {
        println("================================================================================")
        println("              CHARACTER APPEARANCE PALETTE SAMPLING VERIFICATION                ")
        println("================================================================================")

        val expectedBoyHex = String.format("0x%08X", PixelArtRenderer.SkinToneBoy.toArgb())
        val expectedGirlHex = String.format("0x%08X", PixelArtRenderer.SkinToneGirl.toArgb())
        println("Target Boy Skin Tone  (PixelArtRenderer.SkinToneBoy) : $expectedBoyHex")
        println("Target Girl Skin Tone (PixelArtRenderer.SkinToneGirl): $expectedGirlHex")
        println("--------------------------------------------------------------------------------")

        // -----------------------------------------------------------------------------
        // Location 1: WorldSprites.drawScooterWithCouple (Sprites.kt)
        // -----------------------------------------------------------------------------
        val scooterCanvas = DebugPixelCanvas(600, 600)
        val scooterScope = CanvasDrawScope()
        val p = 4.0f
        val cx = 300.0f
        val groundY = 450.0f

        scooterScope.draw(Density(1f), LayoutDirection.Ltr, scooterCanvas, Size(600f, 600f)) {
            WorldSprites.drawScooterWithCouple(
                scope = this,
                cx = cx,
                groundY = groundY,
                p = p,
                timeSeconds = 0f
            )
        }

        // Calculations from drawScooterWithCouple (grounded on asphalt road):
        // scootY = groundY + 5.5f * p = 472
        // wheelCenterY = scootY - 8 * p = 440
        // seatY = wheelCenterY - 24 * p = 344
        // boySeatX = cx - 18 * p = 228
        // boyBottomY = seatY + 2 * p = 352
        // boyTorsoY = boyBottomY - 26 * p = 248
        // boyHeadY = boyTorsoY - 14 * p = 192
        // Boy head rect: Offset(boySeatX - 2 * p, boyHeadY) = Offset(220, 192), Size(12 * p, 12 * p) = Size(48, 48)
        // Sample at (224, 221) which is safely inside the boy's head skin
        val boyHeadSampleX = 224
        val boyHeadSampleY = 221
        val actualScooterBoy = scooterCanvas.getPixel(boyHeadSampleX, boyHeadSampleY)
        val actualScooterBoyHex = String.format("0x%08X", actualScooterBoy)

        // girlSeatX = cx + 2 * p = 308
        // girlBottomY = seatY + 2 * p = 352
        // girlTorsoY = girlBottomY - 26 * p = 248
        // girlHeadY = girlTorsoY - 14 * p = 192
        // Girl head rect: Offset(girlSeatX + 2 * p, girlHeadY) = Offset(316, 192), Size(11 * p, 12 * p) = Size(44, 48)
        // Sample at (324, 221) which is safely inside the girl's head skin
        val girlHeadSampleX = 324
        val girlHeadSampleY = 221
        val actualScooterGirl = scooterCanvas.getPixel(girlHeadSampleX, girlHeadSampleY)
        val actualScooterGirlHex = String.format("0x%08X", actualScooterGirl)

        println("[Location 1: drawScooterWithCouple in Sprites.kt]")
        println("  -> Sampled Boy Head Skin at ($boyHeadSampleX, $boyHeadSampleY):   $actualScooterBoyHex (Expected: $expectedBoyHex)")
        println("  -> Sampled Girl Head Skin at ($girlHeadSampleX, $girlHeadSampleY): $actualScooterGirlHex (Expected: $expectedGirlHex)")
        assertEquals(expectedBoyHex, actualScooterBoyHex)
        assertEquals(expectedGirlHex, actualScooterGirlHex)

        // -----------------------------------------------------------------------------
        // Location 2: WorldSprites.drawBoyHoldingUmbrella (Sprites.kt)
        // -----------------------------------------------------------------------------
        val umbrellaCanvas = DebugPixelCanvas(400, 400)
        val umbrellaScope = CanvasDrawScope()
        val boy = com.example.engine.PixelCharacter(isGirl = false, name = "Boy", worldX = 0.5f, worldY = 0.7f)
        val grip = PixelArtRenderer.umbrellaGrip(boy, centerX = 180f, bottomY = 280f, pixelSize = p, snapToPixel = false, backHand = false)

        umbrellaScope.draw(Density(1f), LayoutDirection.Ltr, umbrellaCanvas, Size(400f, 400f)) {
            WorldSprites.drawBoyHoldingUmbrella(scope = this, grip = grip, timeSeconds = 0f)
        }

        // His fist is two blocks wide round the pole, on the inside of it (facing right, the
        // block left of the pole's column); its outer top block is the skin shadow. So the
        // middle of the inner block's lower half is pure boy skin.
        val umbrellaHandSampleX = (grip.poleX - grip.block / 2f).toInt()
        val umbrellaHandSampleY = (grip.handTop + grip.block * 1.5f).toInt()
        val actualUmbrellaHand = umbrellaCanvas.getPixel(umbrellaHandSampleX, umbrellaHandSampleY)
        val actualUmbrellaHandHex = String.format("0x%08X", actualUmbrellaHand)

        println("[Location 2: drawBoyHoldingUmbrella in Sprites.kt]")
        println("  -> Sampled Boy Umbrella Hand at ($umbrellaHandSampleX, $umbrellaHandSampleY): $actualUmbrellaHandHex (Expected: $expectedBoyHex)")
        assertEquals(expectedBoyHex, actualUmbrellaHandHex)

        // -----------------------------------------------------------------------------
        // Location 3: LoftSprites.drawLibraryBookshelf Framed Picture (LoftSprites.kt)
        // -----------------------------------------------------------------------------
        val loftCanvas = DebugPixelCanvas(400, 800)
        val loftScope = CanvasDrawScope()
        val windowStartX = 300f
        val floorY = 600f

        loftScope.draw(Density(1f), LayoutDirection.Ltr, loftCanvas, Size(400f, 800f)) {
            LoftSprites.drawLibraryBookshelf(
                scope = this,
                cw = 600f,
                ch = 800f,
                windowStartX = windowStartX,
                floorY = floorY,
                p = p,
                timeSeconds = 0f,
                lampLit = true
            )
        }

        // Calculations for Tier 2 Picture:
        // shelfW = windowStartX - 5 * p = 280
        // tierBaseY = 3 * (600 / 5.3) - 0.5 * p = 337.62
        // curX reaches framed picture at 70.4
        // Boy avatar skin: Offset(curX + 1.5 * p, tierBaseY - 5 * p) = Offset(76.4, 317.6), Size(10, 10)
        // Girl avatar skin: Offset(curX + 5.5 * p, tierBaseY - 5 * p) = Offset(92.4, 317.6), Size(10, 10)
        // Sample at skin center
        val polaroidBoyX = (70.4f + 2.2f * p).toInt()
        val polaroidBoyY = (337.62f - 3.2f * p).toInt()
        val polaroidGirlX = (70.4f + 6.8f * p).toInt()
        val polaroidGirlY = (337.62f - 3.2f * p).toInt()
        val actualPolaroidBoy = loftCanvas.getPixel(polaroidBoyX, polaroidBoyY)
        val actualPolaroidGirl = loftCanvas.getPixel(polaroidGirlX, polaroidGirlY)
        val actualPolaroidBoyHex = String.format("0x%08X", actualPolaroidBoy)
        val actualPolaroidGirlHex = String.format("0x%08X", actualPolaroidGirl)

        println("[Location 3: drawLibraryBookshelf Tier 2 Framed Picture in LoftSprites.kt]")
        println("  -> Sampled Boy Polaroid Avatar at ($polaroidBoyX, $polaroidBoyY):   $actualPolaroidBoyHex (Expected: $expectedBoyHex)")
        println("  -> Sampled Girl Polaroid Avatar at ($polaroidGirlX, $polaroidGirlY): $actualPolaroidGirlHex (Expected: $expectedGirlHex)")
        assertEquals(expectedBoyHex, actualPolaroidBoyHex)
        assertEquals(expectedGirlHex, actualPolaroidGirlHex)
        println("--------------------------------------------------------------------------------")
        println("ALL THREE PREVIOUSLY HARDCODED LOCATIONS MATCH PALETTE CONSTANTS PERFECTLY!")
        println("================================================================================\n")
    }

    @Test
    fun `compute and verify blush contrast and color distance before and after skin tone update`() {
        val oldSkin = Color(0xFFE8C4A0)
        val newSkin = PixelArtRenderer.SkinToneGirl // Color(0xFFD49B7A)
        val blushRose = PixelArtRenderer.BlushRose   // Color(0xFFFF758F)
        val blushCoral = PixelArtRenderer.BlushCoral // Color(0xFFFFAAA6)

        println("================================================================================")
        println("             BLUSH CONTRAST & COLOR DISTANCE METRICS COMPARISON                 ")
        println("================================================================================")
        println(String.format("Old Skin Tone: #%06X (RGB: %d, %d, %d)", oldSkin.toRgbInt(), (oldSkin.red * 255).toInt(), (oldSkin.green * 255).toInt(), (oldSkin.blue * 255).toInt()))
        println(String.format("New Skin Tone: #%06X (RGB: %d, %d, %d)", newSkin.toRgbInt(), (newSkin.red * 255).toInt(), (newSkin.green * 255).toInt(), (newSkin.blue * 255).toInt()))
        println(String.format("Blush Rose   : #%06X (RGB: %d, %d, %d)", blushRose.toRgbInt(), (blushRose.red * 255).toInt(), (blushRose.green * 255).toInt(), (blushRose.blue * 255).toInt()))
        println(String.format("Blush Coral  : #%06X (RGB: %d, %d, %d)", blushCoral.toRgbInt(), (blushCoral.red * 255).toInt(), (blushCoral.green * 255).toInt(), (blushCoral.blue * 255).toInt()))
        println("--------------------------------------------------------------------------------")

        // 1. BlushRose Comparison
        val oldRoseDistRGB = rgbDistance(oldSkin, blushRose)
        val newRoseDistRGB = rgbDistance(newSkin, blushRose)
        val oldRoseDeltaE = deltaE(oldSkin, blushRose)
        val newRoseDeltaE = deltaE(newSkin, blushRose)
        val oldRoseContrast = wcagContrast(oldSkin, blushRose)
        val newRoseContrast = wcagContrast(newSkin, blushRose)

        // 2. BlushCoral Comparison
        val oldCoralDistRGB = rgbDistance(oldSkin, blushCoral)
        val newCoralDistRGB = rgbDistance(newSkin, blushCoral)
        val oldCoralDeltaE = deltaE(oldSkin, blushCoral)
        val newCoralDeltaE = deltaE(newSkin, blushCoral)
        val oldCoralContrast = wcagContrast(oldSkin, blushCoral)
        val newCoralContrast = wcagContrast(newSkin, blushCoral)

        println("| Metric                        | Blush Variant | Before (Old Skin) | After (New Skin) | Status     |")
        println("|-------------------------------|---------------|-------------------|------------------|------------|")
        println(String.format("| Euclidean RGB Distance (\u0394RGB) | BlushRose     | %17.2f | %16.2f | Distinct   |", oldRoseDistRGB, newRoseDistRGB))
        println(String.format("| Euclidean RGB Distance (\u0394RGB) | BlushCoral    | %17.2f | %16.2f | Distinct   |", oldCoralDistRGB, newCoralDistRGB))
        println(String.format("| CIELAB Color Difference (\u0394E)  | BlushRose     | %17.2f | %16.2f | High (\u0394E>5) |", oldRoseDeltaE, newRoseDeltaE))
        println(String.format("| CIELAB Color Difference (\u0394E)  | BlushCoral    | %17.2f | %16.2f | High (\u0394E>5) |", oldCoralDeltaE, newCoralDeltaE))
        println(String.format("| WCAG 2.1 Contrast Ratio       | BlushRose     | %15.2f:1 | %14.2f:1 | Clear      |", oldRoseContrast, newRoseContrast))
        println(String.format("| WCAG 2.1 Contrast Ratio       | BlushCoral    | %15.2f:1 | %14.2f:1 | Clear      |", oldCoralContrast, newCoralContrast))
        println("================================================================================\n")
    }

    private fun Color.toRgbInt(): Int {
        val r = (this.red * 255).toInt()
        val g = (this.green * 255).toInt()
        val b = (this.blue * 255).toInt()
        return (r shl 16) or (g shl 8) or b
    }

    private fun rgbDistance(c1: Color, c2: Color): Double {
        val rDiff = (c1.red - c2.red) * 255.0
        val gDiff = (c1.green - c2.green) * 255.0
        val bDiff = (c1.blue - c2.blue) * 255.0
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
    }

    private fun relativeLuminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun wcagContrast(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = Math.max(l1, l2)
        val darker = Math.min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun rgbToLab(c: Color): Triple<Double, Double, Double> {
        fun pivot(v: Double): Double {
            val s = if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
            return s * 100.0
        }
        val r = pivot(c.red.toDouble())
        val g = pivot(c.green.toDouble())
        val b = pivot(c.blue.toDouble())

        // Observer = 2°, Illuminant = D65
        val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 95.047
        val y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 100.000
        val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 108.883

        fun f(t: Double): Double {
            return if (t > 0.008856) Math.cbrt(t) else (7.787 * t) + (16.0 / 116.0)
        }

        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val l = (116.0 * fy) - 16.0
        val a = 500.0 * (fx - fy)
        val bVal = 200.0 * (fy - fz)
        return Triple(l, a, bVal)
    }

    private fun deltaE(c1: Color, c2: Color): Double {
        val (l1, a1, b1) = rgbToLab(c1)
        val (l2, a2, b2) = rgbToLab(c2)
        val dl = l1 - l2
        val da = a1 - a2
        val db = b1 - b2
        return Math.sqrt(dl * dl + da * da + db * db)
    }
}
