package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.CatState
import kotlin.math.cos
import kotlin.math.sin

private val LOFT_STARS = arrayOf(
    Triple(0.08f, 0.06f, 1.2f),
    Triple(0.22f, 0.12f, 2.5f),
    Triple(0.38f, 0.05f, 3.1f),
    Triple(0.50f, 0.15f, 1.8f),
    Triple(0.88f, 0.04f, 2.2f),
    Triple(0.14f, 0.20f, 4.0f),
    Triple(0.62f, 0.22f, 1.5f),
    Triple(0.92f, 0.18f, 3.4f),
    Triple(0.32f, 0.26f, 2.8f),
    Triple(0.76f, 0.28f, 4.5f),
    Triple(0.44f, 0.32f, 2.1f)
)

/**
 * A skyscraper in the loft's window: left edge and top as shares of the window, width in scene
 * pixels, and whether it has a narrower crown and a blinking antenna.
 */
private class LoftTower(val x: Float, val top: Float, val w: Float, val crown: Boolean, val spire: Boolean)

// Tall and slim, with sky between them; the ones under the moon and sun stay lower.
private val LOFT_BACK_BUILDINGS = arrayOf(
    LoftTower(0.01f, 0.18f, 9f, crown = false, spire = false),
    LoftTower(0.10f, 0.08f, 8f, crown = true, spire = false),
    LoftTower(0.19f, 0.02f, 10f, crown = true, spire = true),
    LoftTower(0.30f, 0.13f, 8f, crown = false, spire = false),
    LoftTower(0.39f, 0.05f, 9f, crown = true, spire = false),
    LoftTower(0.49f, 0.16f, 8f, crown = false, spire = false),
    LoftTower(0.57f, 0.07f, 10f, crown = true, spire = true),
    LoftTower(0.67f, 0.36f, 9f, crown = false, spire = false),
    LoftTower(0.77f, 0.40f, 8f, crown = true, spire = false),
    LoftTower(0.87f, 0.10f, 9f, crown = true, spire = false)
)

private val LOFT_FRONT_BUILDINGS = arrayOf(
    Pair(0.04f, 16f),
    Pair(0.20f, 20f),
    Pair(0.38f, 18f),
    Pair(0.55f, 22f),
    Pair(0.72f, 17f),
    Pair(0.86f, 19f)
)

private val LOFT_BOOK_COLORS = arrayOf(
    Color(0xFF9E2A2B),
    Color(0xFF2A6F68),
    Color(0xFF1D3557),
    Color(0xFFE76F51),
    Color(0xFFE9C46A),
    Color(0xFF457B9D),
    Color(0xFF588157),
    Color(0xFF6D597A),
    Color(0xFFDDA15E)
)

private val LOFT_ALBUM_COLORS = arrayOf(
    Color(0xFF9E2A2B),
    Color(0xFF2A6F68),
    Color(0xFFE76F51),
    Color(0xFF1D3557),
    Color(0xFFE9C46A)
)

private val LOFT_BULB_POSITIONS = floatArrayOf(0.05f, 0.16f, 0.28f, 0.42f, 0.56f, 0.70f, 0.85f, 0.95f)
private val LOFT_VINE_POSITIONS = floatArrayOf(0.10f, 0.22f, 0.36f, 0.50f, 0.64f, 0.78f, 0.90f)

object LoftSprites {

    // Cozy Loft Timber Palette
    val DarkWoodBeam = Color(0xFF28150A)
    val RafterWood = Color(0xFF381F0E)
    val PlankWood = Color(0xFF4E2B14)
    val PlankWoodLight = Color(0xFF6E3E1E)
    val WarmWall = Color(0xFF2D180C)
    val WarmWallPanel = Color(0xFF3D2211)

    // Window & Night Skyline Palette
    val NightSkyDark = Color(0xFF080C22)
    val NightSkyMid = Color(0xFF0F1538)
    val SkyCloud = Color(0x35192348)
    val MoonBody = Color(0xFFFFF4B8)
    val MoonShadow = Color(0xFFE8CA72)
    val MoonGlowOuter = Color(0x18FFEAA7)
    val MoonGlowInner = Color(0x35FFEAA7)
    val CityWater = Color(0xFF0A122E)
    val CityWaterDeep = Color(0xFF060B1C)
    val CityWaterReflection = Color(0x70F4D06F)
    val CityWaterReflectionCyan = Color(0x5074C69D)
    val SkyscraperDark = Color(0xFF0B1124)
    val SkyscraperMid = Color(0xFF131D38)
    val SkyscraperLight = Color(0xFF1B284C)
    val WindowGold = Color(0xFFFFEAA7)
    val WindowAmber = Color(0xFFFFB703)
    val WindowCyan = Color(0xFF74C69D)
    val WindowWarmWhite = Color(0xFFFFF8E7)

    // Furniture & Props Palette
    val SofaLeather = Color(0xFFC77838)
    val SofaLeatherHighlight = Color(0xFFE08E4A)
    val SofaLeatherShadow = Color(0xFF8F4C1B)
    val SofaLeatherDark = Color(0xFF5A2C0D)

    // Checkered Blanket Palette
    val BlanketRed = Color(0xFF9E2A2B)
    val BlanketOrange = Color(0xFFD97746)
    val BlanketCream = Color(0xFFF7E6CE)
    val BlanketDark = Color(0xFF5E1718)
    val BlanketYellow = Color(0xFFE9C46A)

    // Persian Area Rug Palette
    val RugCrimson = Color(0xFF8B1E22)
    val RugBorder = Color(0xFFFFF0DC)
    val RugGold = Color(0xFFD4A373)
    val RugTeal = Color(0xFF235E58)
    val RugDark = Color(0xFF501215)

    // Lighting Palette
    val AmberLightLow = Color(0x18FFAA00)
    val AmberLightMid = Color(0x30FFAA00)
    val AmberLightBright = Color(0x60FFD166)

    // Character Colors for Cozy Loft
    val SweaterNavy = Color(0xFF1D2D44)
    val SweaterNavyLight = Color(0xFF2E4668)
    val SweaterNavyDark = Color(0xFF121D2C)
    val SweaterPink = Color(0xFFF7A8B8)
    val SweaterPinkLight = Color(0xFFFFCAD4)
    val SweaterPinkDark = Color(0xFFE5989B)

    /**
     * Complete background layer of the Cozy Loft:
     * Slanted ceiling, angled panoramic window, night city skyline, moon, stars,
     * wall-to-ceiling bookshelf, side tables, wall art, lamps, rugs, and sofa base.
     */
    fun drawLoftBackground(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean,
        recordSpinning: Boolean,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
        isNight: Boolean = true,
        isSunset: Boolean = false,
        roomTheme: RoomTheme = RoomTheme.WARM_AUTUMN_COTTAGE,
        boyLook: AvatarLook = AvatarLook.DEFAULT_A,
        girlLook: AvatarLook = AvatarLook.DEFAULT_B
    ) {
        val floorY = ch * 0.55f
        val windowStartX = cw * 0.32f
        val windowBottomY = floorY - 6 * p
        val roofSlopeStartY = ch * 0.01f
        val roofSlopeEndY = ch * 0.25f

        // 1. NIGHT / SUNSET / DAY SKY & DISTANT GLOWING CITY SKYLINE (Outside the window)
        drawWindowSkyline(
            scope = scope,
            cw = cw,
            ch = ch,
            startX = windowStartX,
            windowBottomY = windowBottomY,
            roofStartY = roofSlopeStartY,
            roofEndY = roofSlopeEndY,
            p = p,
            timeSeconds = timeSeconds,
            weather = weather,
            isNight = isNight,
            isSunset = isSunset
        )

        // 2. TIMBER SLANTED CEILING & RAFTERS
        drawSlantedCeiling(
            scope = scope,
            cw = cw,
            ch = ch,
            p = p,
            roofStartY = roofSlopeStartY,
            roofEndY = roofSlopeEndY,
            windowStartX = windowStartX
        )

        // 3. WINDOW FRAMES & STRUCTURAL BEAMS
        drawLoftWindowFrames(
            scope = scope,
            cw = cw,
            ch = ch,
            startX = windowStartX,
            windowBottomY = windowBottomY,
            roofStartY = roofSlopeStartY,
            roofEndY = roofSlopeEndY,
            p = p
        )

        // 4. LOWER WOODEN WALL BEHIND SOFA
        drawLowerWallPanels(scope, cw, windowStartX, windowBottomY, floorY, p, lampLit)

        // 5. POLISHED WOODEN FLOOR & VINTAGE PERSIAN RUG
        drawLoftFloorAndRug(scope, cw, ch, floorY, p, roomTheme, isNight, isSunset)

        // 6. WALL-TO-CEILING LIBRARY BOOKSHELF (LEFT WALL)
        drawLibraryBookshelf(scope, cw, ch, windowStartX, floorY, p, timeSeconds, lampLit, boyLook, girlLook)

        // 7. SOFA BASE & CUSHIONS (Behind characters)
        drawLoftSofaBase(scope, cw, floorY, p, lampLit)

        // 8. RECORD PLAYER & SIDE TABLE (RIGHT WALL)
        drawRecordPlayerStation(scope, cw, floorY, p, timeSeconds, recordSpinning, lampLit)

        // 9. TABLE LAMPS & LIGHTING
        drawWallArtAndTableLamp(scope, cw, ch, windowStartX, floorY, p, timeSeconds, lampLit)
    }

    /**
     * Dedicated couple renderer for the Cozy Loft:
     * Boy in cozy sweater holding a steaming heart mug,
     * Girl in cozy sweater cuddled lovingly into his shoulder!
     */
    fun drawCuddledCouple(
        scope: DrawScope,
        boyX: Float,
        girlX: Float,
        floorY: Float,
        p: Float,
        timeSeconds: Float,
        boyEmotion: CharacterEmotion = CharacterEmotion.LOVING,
        girlEmotion: CharacterEmotion = CharacterEmotion.LOVING,
        isKissing: Boolean = false,
        isReadingBook: Boolean = false,
        boyOutfitIndex: Int = 0,
        girlOutfitIndex: Int = 0,
        boyAccessoryIndex: Int = 0,
        girlAccessoryIndex: Int = 0,
        boyWearsGlasses: Boolean = true,
        boyLook: AvatarLook = AvatarLook.DEFAULT_A,
        girlLook: AvatarLook = AvatarLook.DEFAULT_B
    ) {
        val seatY = floorY - 5 * p

        // Living breathing bob
        val breathBoy = sin(timeSeconds * 2.2f) * 1.6f * p
        val breathGirl = sin(timeSeconds * 2.2f + 0.5f) * 1.6f * p

        // 1. The Boy sitting on the left side of the couch
        val boyXPos = if (isKissing) (boyX + girlX) / 2f - 2.5f * p else boyX
        val boyYPos = seatY + breathBoy
        drawBoyOnSofa(scope, boyXPos, boyYPos, p, timeSeconds, boyEmotion, isKissing, isReadingBook, boyOutfitIndex, boyAccessoryIndex, boyWearsGlasses, boyLook)

        // 2. The Girl snuggling sweetly against the boy
        val girlXPos = if (isKissing) (boyX + girlX) / 2f + 2.5f * p else girlX
        val girlY = seatY + breathGirl
        drawGirlOnSofa(scope, girlXPos, girlY, p, timeSeconds, girlEmotion, isKissing, isReadingBook, girlOutfitIndex, girlAccessoryIndex, girlLook)

        // 3. Floating little cuddle heart
        if (isKissing || (boyEmotion == CharacterEmotion.LOVING && sin(timeSeconds * 2.5f) > 0.70f)) {
            val hx = (boyXPos + girlXPos) / 2f + sin(timeSeconds * 4f) * 2 * p
            val hy = seatY - 22 * p - (sin(timeSeconds * 3f) * 4 * p)
            scope.drawRect(Color(0xFFFF3366), Offset(hx - 2 * p, hy), Size(5 * p, 4 * p))
            scope.drawRect(Color(0xFFFF3366), Offset(hx - p, hy + 3 * p), Size(3 * p, 2 * p))
        }
    }

    /** Sofa-pose hair: long wavy locks with a ribbon, or short tousled bangs, in the partner's colours. */
    private fun drawSofaHair(scope: DrawScope, hx: Float, headY: Float, headW: Float, p: Float, look: AvatarLook) {
        val hair = look.hair
        val hairHigh = look.hairHighlight
        if (look.longHair) {
            scope.drawRect(hair, Offset(hx - 1.5f * p, headY), Size(headW + 3 * p, 3.5f * p))
            scope.drawRect(hairHigh, Offset(hx + 1.5f * p, headY), Size(4 * p, p))
            // Cascading side locks framing face
            scope.drawRect(hair, Offset(hx - 2 * p, headY + 3.2f * p), Size(2.8f * p, 9 * p))
            scope.drawRect(look.hairShadow, Offset(hx - 2.5f * p, headY + 5.5f * p), Size(1.6f * p, 6.5f * p))
            scope.drawRect(hair, Offset(hx + headW - 0.8f * p, headY + 3.2f * p), Size(2.8f * p, 9 * p))
            // Soft bangs
            scope.drawRect(hair, Offset(hx + 0.8f * p, headY + 2.5f * p), Size(2.5f * p, 2 * p))
            scope.drawRect(hair, Offset(hx + 4f * p, headY + 2.5f * p), Size(2.5f * p, 2 * p))
            // Cute pink hair ribbon
            scope.drawRect(PixelArtRenderer.RibbonGirl, Offset(hx + headW - 2.5f * p, headY + 0.8f * p), Size(2.8f * p, 2 * p))
            scope.drawRect(PixelArtRenderer.RibbonCenter, Offset(hx + headW - 1.6f * p, headY + 1.2f * p), Size(1.2f * p, 1.2f * p))
        } else {
            scope.drawRect(hair, Offset(hx - 0.8f * p, headY), Size(headW + 1.6f * p, 3.5f * p))
            scope.drawRect(hairHigh, Offset(hx + 1.5f * p, headY), Size(4 * p, p))
            // Bangs hanging over forehead
            scope.drawRect(hair, Offset(hx, headY + 2.5f * p), Size(2.5f * p, 3 * p))
            scope.drawRect(hair, Offset(hx + 3.5f * p, headY + 2.5f * p), Size(2 * p, 2 * p))
            scope.drawRect(hair, Offset(hx + 6.5f * p, headY + 2.5f * p), Size(3 * p, 3 * p))
        }
    }

    private fun drawBoyOnSofa(
        scope: DrawScope,
        bx: Float,
        by: Float,
        p: Float,
        timeSeconds: Float,
        emotion: CharacterEmotion,
        isKissing: Boolean,
        isReadingBook: Boolean,
        boyOutfitIndex: Int = 0,
        boyAccessoryIndex: Int = 0,
        boyWearsGlasses: Boolean = true,
        look: AvatarLook = AvatarLook.DEFAULT_A
    ) {
        val headY = by - 19 * p
        val headW = 10 * p
        val headH = 9 * p
        val hx = bx - headW / 2f

        // Head / Skin
        val skinBoy = look.skin
        val skinBoyShadow = look.skinShadow
        scope.drawRect(skinBoy, Offset(hx + 0.8f * p, headY + 2.5f * p), Size(headW - 1.6f * p, headH - 2.5f * p))
        scope.drawRect(skinBoyShadow, Offset(hx + headW - 1.8f * p, headY + 4 * p), Size(p, 3.5f * p))

        drawSofaHair(scope, hx, headY, headW, p, look)

        // Eyes
        val eyeDark = PixelArtRenderer.EyeDark
        if (isKissing || emotion == CharacterEmotion.LOVING) {
            // Happy closed curved eyes (^_^)
            scope.drawRect(eyeDark, Offset(hx + 2.5f * p, headY + 5.2f * p), Size(2 * p, 0.8f * p))
            scope.drawRect(eyeDark, Offset(hx + 6f * p, headY + 5.2f * p), Size(2 * p, 0.8f * p))
        } else {
            scope.drawRect(eyeDark, Offset(hx + 2.8f * p, headY + 4.8f * p), Size(1.8f * p, 1.8f * p))
            scope.drawRect(eyeDark, Offset(hx + 6f * p, headY + 4.8f * p), Size(1.8f * p, 1.8f * p))
            scope.drawRect(Color.White, Offset(hx + 2.8f * p, headY + 4.8f * p), Size(0.8f * p, 0.8f * p))
            scope.drawRect(Color.White, Offset(hx + 6f * p, headY + 4.8f * p), Size(0.8f * p, 0.8f * p))
        }

        // Rosy Blushing Cheeks
        scope.drawRect(PixelArtRenderer.BlushCoral, Offset(hx + 1.8f * p, headY + 6.5f * p), Size(2 * p, p))
        scope.drawRect(PixelArtRenderer.BlushCoral, Offset(hx + 6f * p, headY + 6.5f * p), Size(2 * p, p))
        // Gentle smile
        scope.drawRect(Color(0xFFC9184A), Offset(hx + 4 * p, headY + 7f * p), Size(1.8f * p, 0.8f * p))

        // Boy Outfit resolution
        val boyOutfit = PixelArtRenderer.getBoyOutfitPalette(boyOutfitIndex)
        val sweaterColor = boyOutfit.sweater
        val sweaterHighlight = boyOutfit.sweaterHighlight
        val collarColor = boyOutfit.collar

        // Torso: Dynamic sweater / hoodie
        val torsoY = headY + headH
        val torsoW = 12 * p
        val torsoH = 9 * p
        val tx = bx - torsoW / 2f
        scope.drawRect(sweaterColor, Offset(tx, torsoY), Size(torsoW, torsoH))
        scope.drawRect(sweaterHighlight, Offset(tx + 1.5f * p, torsoY + 0.8f * p), Size(torsoW - 3 * p, 1.2f * p))
        scope.drawRect(collarColor, Offset(tx + 3.5f * p, torsoY), Size(3.5f * p, 1.2f * p)) // collar

        if (boyOutfit.isHoodie) {
            // Folded hood collar resting behind neck and over shoulders
            scope.drawRect(collarColor, Offset(tx - 0.8f * p, torsoY - 0.8f * p), Size(torsoW + 1.6f * p, 2.2f * p))
            scope.drawRect(sweaterColor, Offset(tx + 2f * p, torsoY - 0.5f * p), Size(torsoW - 4f * p, 1.8f * p))
            // Kangaroo pocket pouch
            scope.drawRect(collarColor, Offset(tx + 2.5f * p, torsoY + 4.5f * p), Size(7 * p, 3.5f * p))
            scope.drawRect(sweaterHighlight, Offset(tx + 3.5f * p, torsoY + 4.8f * p), Size(5 * p, 0.8f * p))
            // Contrasting drawstrings hanging down with knotted tips
            val cordColor = if (boyOutfitIndex == 1) collarColor else Color.White
            scope.drawRect(cordColor, Offset(tx + 4f * p, torsoY + 1.2f * p), Size(0.9f * p, 3.2f * p))
            scope.drawRect(cordColor, Offset(tx + 6.3f * p, torsoY + 1.2f * p), Size(0.9f * p, 3.2f * p))
            scope.drawRect(Color(0xFFFFD166), Offset(tx + 3.8f * p, torsoY + 4.2f * p), Size(1.3f * p, p))
            scope.drawRect(Color(0xFFFFD166), Offset(tx + 6.1f * p, torsoY + 4.2f * p), Size(1.3f * p, p))
        }

        // Boy Accessory on sofa (beanie / scarf / cap)
        when (boyAccessoryIndex) {
            1 -> {
                // Beanie
                scope.drawRect(Color(0xFF264653), Offset(hx - 1.5f * p, headY - 1.5f * p), Size(headW + 3 * p, 4 * p))
                scope.drawRect(Color(0xFF1B4332), Offset(hx - p, headY + 1.8f * p), Size(headW + 2 * p, 1.8f * p))
                scope.drawRect(Color(0xFFE9C46A), Offset(hx + 3.5f * p, headY - 3.5f * p), Size(3 * p, 2.5f * p)) // pom-pom
            }
            2 -> {
                // Scarf wrapped around neck
                scope.drawRect(Color(0xFFE9C46A), Offset(tx + 2 * p, torsoY - 0.5f * p), Size(torsoW - 4 * p, 3 * p))
                scope.drawRect(Color(0xFFD4A373), Offset(tx + 4 * p, torsoY + 2.5f * p), Size(2.5f * p, 3.5f * p))
            }
            3 -> {
                // Baseball cap
                scope.drawRect(Color(0xFF1D3557), Offset(hx - 1.5f * p, headY - 1.5f * p), Size(headW + 3 * p, 4.5f * p))
                scope.drawRect(Color(0xFF0F172A), Offset(hx + 4f * p, headY + 2.5f * p), Size(5.5f * p, 1.6f * p))
            }
        }

        // Square-frame glasses ALWAYS in front of accessories and face!
        if (boyWearsGlasses) {
            // Left square rim
            scope.drawRect(PixelArtRenderer.GlassesFrame, Offset(hx + 1.8f * p, headY + 4f * p), Size(3.2f * p, 3.2f * p))
            scope.drawRect(PixelArtRenderer.GlassesGlint, Offset(hx + 2.4f * p, headY + 4.6f * p), Size(1.4f * p, 1.4f * p))
            // Right square rim
            scope.drawRect(PixelArtRenderer.GlassesFrame, Offset(hx + 5.2f * p, headY + 4f * p), Size(3.2f * p, 3.2f * p))
            scope.drawRect(PixelArtRenderer.GlassesGlint, Offset(hx + 5.8f * p, headY + 4.6f * p), Size(1.4f * p, 1.4f * p))
            // Bridge
            scope.drawRect(PixelArtRenderer.GlassesFrameLight, Offset(hx + 4.2f * p, headY + 4.8f * p), Size(1.8f * p, 0.8f * p))
            // Temple
            scope.drawRect(PixelArtRenderer.GlassesFrame, Offset(hx + 0.8f * p, headY + 4.8f * p), Size(1.2f * p, 0.8f * p))
        }

        // Arms & Actions
        if (isReadingBook) {
            // Holding open book across laps
            val bookW = 14 * p
            val bookH = 6 * p
            val bookX = bx
            val bookY = torsoY + 5 * p
            scope.drawRect(Color(0xFF1D3557), Offset(bookX, bookY), Size(bookW, bookH))
            scope.drawRect(Color(0xFFFFFDF0), Offset(bookX + 0.8f * p, bookY + 0.8f * p), Size(bookW - 1.6f * p, bookH - 1.6f * p))
            scope.drawRect(Color(0xFFD4A373), Offset(bookX + bookW / 2f - 0.4f * p, bookY), Size(0.8f * p, bookH))
            // Hands holding sides
            scope.drawRect(skinBoy, Offset(bookX - 0.8f * p, bookY + 1.5f * p), Size(1.5f * p, 2.5f * p))
            scope.drawRect(skinBoy, Offset(bookX + bookW - 0.8f * p, bookY + 1.5f * p), Size(1.5f * p, 2.5f * p))
        } else {
            // Holding ceramic mug with red heart with both hands
            val mugW = 4 * p
            val mugH = 5 * p
            val mugX = bx - mugW / 2f + 1.5f * p
            val mugY = torsoY + 4 * p

            // Sweater arms wrapping forward
            scope.drawRect(sweaterColor, Offset(tx + 0.8f * p, torsoY + 1.5f * p), Size(2.5f * p, 5 * p))
            scope.drawRect(sweaterColor, Offset(tx + torsoW - 3.2f * p, torsoY + 1.5f * p), Size(2.5f * p, 5 * p))
            // Hands holding mug
            scope.drawRect(skinBoy, Offset(mugX - 1.2f * p, mugY + 1.2f * p), Size(1.5f * p, 2.5f * p))
            scope.drawRect(skinBoy, Offset(mugX + mugW - 0.4f * p, mugY + 1.2f * p), Size(1.5f * p, 2.5f * p))

            // White ceramic mug
            scope.drawRect(Color(0xFFFFF0F5), Offset(mugX, mugY), Size(mugW, mugH))
            scope.drawRect(Color(0xFFFFF0F5), Offset(mugX + mugW, mugY + 0.8f * p), Size(1.2f * p, 2.8f * p))
            // Little red heart on mug
            scope.drawRect(Color(0xFFFF3366), Offset(mugX + 1.2f * p, mugY + 1.5f * p), Size(1.6f * p, 1.6f * p))

            // Gentle steam rising from mug
            val steamSway = sin(timeSeconds * 3.5f) * 1.2f * p
            scope.drawRect(Color(0x55FFFFFF), Offset(mugX + 1.2f * p + steamSway, mugY - 2.5f * p), Size(1.2f * p, 2 * p))
            scope.drawRect(Color(0x35FFFFFF), Offset(mugX + 0.8f * p - steamSway * 0.8f, mugY - 5 * p), Size(1.6f * p, 2 * p))
        }
    }

    private fun drawGirlOnSofa(
        scope: DrawScope,
        gx: Float,
        gy: Float,
        p: Float,
        timeSeconds: Float,
        emotion: CharacterEmotion,
        isKissing: Boolean,
        isReadingBook: Boolean,
        girlOutfitIndex: Int = 0,
        girlAccessoryIndex: Int = 0,
        look: AvatarLook = AvatarLook.DEFAULT_B
    ) {
        val headY = gy - 18 * p
        val headW = 9 * p
        val headH = 9 * p
        val hx = gx - headW / 2f

        // Head / Skin (warm light golden tone)
        val skinGirl = look.skin
        val skinGirlShadow = look.skinShadow
        scope.drawRect(skinGirl, Offset(hx + 0.8f * p, headY + 2.5f * p), Size(headW - 1.6f * p, headH - 2.5f * p))
        scope.drawRect(skinGirlShadow, Offset(hx + headW - 1.6f * p, headY + 4 * p), Size(0.8f * p, 3.5f * p))

        drawSofaHair(scope, hx, headY, headW, p, look)

        // Sweet peaceful closed smiling eyes leaning into him (^_^)
        val eyeDark = PixelArtRenderer.EyeDark
        scope.drawRect(eyeDark, Offset(hx + 2 * p, headY + 5.2f * p), Size(2 * p, 0.8f * p))
        scope.drawRect(eyeDark, Offset(hx + 5.2f * p, headY + 5.2f * p), Size(2 * p, 0.8f * p))

        // Rosy Blushing Cheeks
        scope.drawRect(PixelArtRenderer.BlushRose, Offset(hx + 1.2f * p, headY + 6.2f * p), Size(2 * p, 1.2f * p))
        scope.drawRect(PixelArtRenderer.BlushRose, Offset(hx + 5.8f * p, headY + 6.2f * p), Size(2 * p, 1.2f * p))
        // Sweet smile
        scope.drawRect(Color(0xFFC9184A), Offset(hx + 3.6f * p, headY + 7f * p), Size(1.6f * p, 0.8f * p))

        // Girl Outfit resolution
        val girlDress = PixelArtRenderer.getGirlDressPalette(girlOutfitIndex)
        val sweaterColor = girlDress.sweater
        val sweaterHighlight = girlDress.trim
        val collarColor = girlDress.skirtShadow

        // Torso: Dynamic dress / hoodie
        val torsoY = headY + headH
        val torsoW = 10 * p
        val torsoH = 8 * p
        val tx = gx - torsoW / 2f
        scope.drawRect(sweaterColor, Offset(tx, torsoY), Size(torsoW, torsoH))
        scope.drawRect(sweaterHighlight, Offset(tx + 1.5f * p, torsoY + 0.8f * p), Size(torsoW - 3 * p, 1.2f * p))
        scope.drawRect(collarColor, Offset(tx + 2.5f * p, torsoY), Size(3 * p, 1.2f * p)) // collar

        if (girlDress.isHoodie || girlOutfitIndex >= 7) {
            // Folded hood collar resting behind neck and over shoulders
            scope.drawRect(girlDress.skirtShadow, Offset(tx - 0.8f * p, torsoY - 0.8f * p), Size(torsoW + 1.6f * p, 2.2f * p))
            scope.drawRect(sweaterColor, Offset(tx + 1.5f * p, torsoY - 0.5f * p), Size(torsoW - 3f * p, 1.8f * p))
            // Kangaroo pocket pouch
            scope.drawRect(girlDress.skirtShadow, Offset(tx + 1.8f * p, torsoY + 4f * p), Size(6.4f * p, 3.2f * p))
            scope.drawRect(girlDress.trim, Offset(tx + 2.5f * p, torsoY + 4.2f * p), Size(5f * p, 0.8f * p))
            // Cute white drawstrings hanging down with ribbon-colored tips
            scope.drawRect(Color.White, Offset(tx + 3.2f * p, torsoY + 1.2f * p), Size(0.8f * p, 3f * p))
            scope.drawRect(Color.White, Offset(tx + 5.5f * p, torsoY + 1.2f * p), Size(0.8f * p, 3f * p))
            scope.drawRect(girlDress.ribbon, Offset(tx + 3f * p, torsoY + 4f * p), Size(1.2f * p, 0.9f * p))
            scope.drawRect(girlDress.ribbon, Offset(tx + 5.3f * p, torsoY + 4f * p), Size(1.2f * p, 0.9f * p))
        }

        // Girl Accessory on sofa (beanie / scarf / cap)
        when (girlAccessoryIndex) {
            1 -> {
                // Beanie
                scope.drawRect(Color(0xFFE8998D), Offset(hx - 1.5f * p, headY - 1.5f * p), Size(headW + 3 * p, 4 * p))
                scope.drawRect(Color(0xFFD6587A), Offset(hx - p, headY + 1.8f * p), Size(headW + 2 * p, 1.8f * p))
                scope.drawRect(Color(0xFFFFF0F3), Offset(hx + 3.5f * p, headY - 3.5f * p), Size(3 * p, 2.5f * p)) // pom-pom
            }
            2 -> {
                // Scarf wrapped around neck
                scope.drawRect(Color(0xFFFFB5A7), Offset(tx + 1.5f * p, torsoY - 0.5f * p), Size(torsoW - 3 * p, 2.8f * p))
                scope.drawRect(Color(0xFFFFF0F3), Offset(tx + 2.5f * p, torsoY + 2.3f * p), Size(2.2f * p, 3f * p))
            }
            3 -> {
                // Baseball cap
                scope.drawRect(Color(0xFF6B9080), Offset(hx - 1.5f * p, headY - 1.5f * p), Size(headW + 3 * p, 4.5f * p))
                scope.drawRect(Color(0xFF4E6E60), Offset(hx + 3.5f * p, headY + 2.5f * p), Size(5.5f * p, 1.6f * p))
            }
        }

        // Arms: cuddled softly onto his arm/side
        scope.drawRect(sweaterColor, Offset(tx - 0.8f * p, torsoY + 1.5f * p), Size(2.5f * p, 5 * p))
        scope.drawRect(sweaterColor, Offset(tx + torsoW - 1.6f * p, torsoY + 1.5f * p), Size(2.5f * p, 5 * p))
        scope.drawRect(skinGirl, Offset(tx - 1.6f * p, torsoY + 5 * p), Size(2 * p, 2 * p))
    }

    /**
     * Foreground layer drawn over the characters:
     * Plaid patchwork blanket over their laps, mugs in hand, coffee table with snacks,
     * glowing lantern, footstool, and balcony railing with glowing fairy lights.
     */
    fun drawLoftForeground(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean,
        isSnow: Boolean = false,
        drawCat: () -> Unit = {},
        roomTheme: RoomTheme = RoomTheme.WARM_AUTUMN_COTTAGE
    ) {
        val floorY = ch * 0.55f

        // 1. Plaid patchwork blanket draped across the spacious daybed & couple laps
        drawPatchworkBlanket(scope, cw, floorY, p, roomTheme)

        // 2. Pet Mochi sleeping peacefully curled up on the left side of the blanket (dedicated loft sleeping cat, matches reference image!)
        val sofaStartX = cw * 0.38f
        val mochiX = sofaStartX + 14 * p
        val mochiY = floorY - 3 * p
        drawLoftSleepingCat(scope, mochiX, mochiY, p, timeSeconds, isSnow)

        // 3. Coffee Table with flower vase, books, lantern, mugs, and cookies
        drawCoffeeTable(scope, cw, floorY, p, timeSeconds, lampLit, roomTheme)

        // 4. Footstool with red checkered cushion
        drawFootstool(scope, cw, floorY, p)

        // 5. Floor space decorations: velvet floor meditation cushion, vintage books with vinyl, brass candle lantern, and woven basket
        drawLoftFloorDecorations(scope, cw, floorY, ch * 0.72f - floorY, p, timeSeconds, lampLit)

        // 6. Loft Balcony Railing in immediate foreground with fairy string lights and vines
        drawBalconyRailing(scope, cw, ch, p, timeSeconds, lampLit, roomTheme)

        // 7. Lighting ambiance overlay (dim romantic blue if lamp is off, warm amber if lit)
        if (!lampLit) {
            scope.drawRect(Color(0x50090D24), Offset.Zero, Size(cw, ch))
        } else {
            // Gentle ambient warmth in the center
            scope.drawRect(Color(0x0CFFB703), Offset(0f, ch * 0.15f), Size(cw, ch * 0.85f))
        }
    }

    /**
     * Dedicated sleeping calico cat for the Cozy Loft Daybed:
     * Curled peacefully in a round donut loaf on top of the blanket,
     * to the left of the boy, with rhythmic breathing and sleepy z's!
     */
    fun drawLoftSleepingCat(
        scope: DrawScope,
        cx: Float,
        cy: Float,
        p: Float,
        timeSeconds: Float,
        isSnow: Boolean = false
    ) {
        val breath = sin(timeSeconds * 2.2f) * 0.6f * p
        val catW = 12 * p
        val catH = 8 * p
        val x = cx - catW / 2f
        val y = cy - catH / 2f

        // Soft shadow under cat on the blanket
        scope.drawRect(Color(0x35000000), Offset(x + p, cy + 3 * p), Size(catW - 2 * p, 2.5f * p))

        // Fur colors
        val furWhite = Color(0xFFFDFBF7)
        val furCalicoOrange = Color(0xFFD48344)
        val furCalicoDark = Color(0xFF4A3525)
        val pinkInner = Color(0xFFFFCAD4)

        // Main curled body
        scope.drawRect(furWhite, Offset(x + p, y + 1.5f * p - breath), Size(catW - 2 * p, catH - 1.5f * p + breath))
        scope.drawRect(furWhite, Offset(x + 2 * p, y - breath), Size(catW - 4 * p, catH + breath))

        // Calico ginger patches on body & back
        scope.drawRect(furCalicoOrange, Offset(x + 4.5f * p, y - breath), Size(4.5f * p, 3.5f * p))
        scope.drawRect(furCalicoOrange, Offset(x + 5.5f * p, y + 3.5f * p), Size(3 * p, 2.5f * p))

        // Dark brown/black calico patch on rear flank
        scope.drawRect(furCalicoDark, Offset(x + 8f * p, y + 1.5f * p - breath), Size(2.5f * p, 3.5f * p))

        // Curled Tail wrapping around front paws
        scope.drawRect(furCalicoOrange, Offset(x + catW - 2 * p, y + 2.5f * p), Size(2.2f * p, 4 * p))
        scope.drawRect(furWhite, Offset(x + catW - 3 * p, y + 5.5f * p), Size(3 * p, 2 * p))

        // Cat Head curled to the left side
        val headX = x + 1.2f * p
        val headY = y + 1.8f * p - breath * 0.5f
        scope.drawRect(furWhite, Offset(headX, headY), Size(5.5f * p, 4.5f * p))

        // Ears
        // Left ear (Ginger)
        scope.drawRect(furCalicoOrange, Offset(headX, headY - 2.2f * p), Size(2 * p, 2.2f * p))
        scope.drawRect(pinkInner, Offset(headX + 0.5f * p, headY - 1.5f * p), Size(p, 1.5f * p))
        // Right ear (Dark)
        scope.drawRect(furCalicoDark, Offset(headX + 3.2f * p, headY - 2.2f * p), Size(2 * p, 2.2f * p))
        scope.drawRect(pinkInner, Offset(headX + 3.7f * p, headY - 1.5f * p), Size(p, 1.5f * p))

        // Cute curved sleeping eyes (- -)
        val eyeColor = Color(0xFF3D2619)
        scope.drawRect(eyeColor, Offset(headX + 1.2f * p, headY + 2.2f * p), Size(1.4f * p, 0.7f * p))
        scope.drawRect(eyeColor, Offset(headX + 3.2f * p, headY + 2.2f * p), Size(1.4f * p, 0.7f * p))

        // Tiny pink nose & sweet mouth
        scope.drawRect(pinkInner, Offset(headX + 2.4f * p, headY + 3.2f * p), Size(0.8f * p, 0.6f * p))

        if (isSnow) {
            val scarfRed = Color(0xFFD90429)
            val scarfDark = Color(0xFFA0001E)
            val scarfFringe = Color(0xFFFF4D6D)
            scope.drawRect(scarfRed, Offset(headX + 4.8f * p, headY + 1.5f * p), Size(2.2f * p, 3.8f * p))
            scope.drawRect(scarfDark, Offset(headX + 4.8f * p, headY + 1.5f * p), Size(2.2f * p, 0.7f * p))
            scope.drawRect(scarfRed, Offset(headX + 5.8f * p, headY + 3.8f * p), Size(2f * p, 2.2f * p))
            scope.drawRect(scarfFringe, Offset(headX + 6.2f * p, headY + 5.6f * p), Size(2.4f * p, 0.8f * p))
        }

        // Sleepy "z z Z" particles drifting up
        if (sin(timeSeconds * 1.8f) > 0.3f) {
            val zAlpha = ((sin(timeSeconds * 1.8f) - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val zY = headY - 4 * p - (timeSeconds * 3f % 6f) * p
            val zX = headX - p + sin(timeSeconds * 2f) * p
            scope.drawRect(Color(0xFF8E9AAF).copy(alpha = zAlpha), Offset(zX, zY), Size(2 * p, 0.8f * p))
            scope.drawRect(Color(0xFF8E9AAF).copy(alpha = zAlpha), Offset(zX + 0.8f * p, zY + 0.8f * p), Size(0.8f * p, 0.8f * p))
            scope.drawRect(Color(0xFF8E9AAF).copy(alpha = zAlpha), Offset(zX, zY + 1.6f * p), Size(2 * p, 0.8f * p))
        }
    }

    private fun drawWindowSkyline(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        startX: Float,
        windowBottomY: Float,
        roofStartY: Float,
        roofEndY: Float,
        p: Float,
        timeSeconds: Float,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
        isNight: Boolean = true,
        isSunset: Boolean = false
    ) {
        val windowW = cw - startX
        val windowH = windowBottomY

        // 1. Sky gradient per season and time-of-day
        val skyTop = when {
            isNight -> when (weather) {
                com.example.scene.WeatherType.SNOW -> Color(0xFF040816)
                com.example.scene.WeatherType.SAKURA -> Color(0xFF0F0B24)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFF0C091C)
                com.example.scene.WeatherType.RAIN -> Color(0xFF060818)
                else -> NightSkyDark
            }
            isSunset -> when (weather) {
                com.example.scene.WeatherType.SAKURA -> Color(0xFF8B2662)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFFC4451C)
                com.example.scene.WeatherType.SNOW -> Color(0xFF6A4C7D)
                com.example.scene.WeatherType.RAIN -> Color(0xFF5A3E52)
                else -> Color(0xFFE07A5F)
            }
            else -> when (weather) { // Daytime
                com.example.scene.WeatherType.SAKURA -> Color(0xFF98C5E8)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFF7CA6CD)
                com.example.scene.WeatherType.SNOW -> Color(0xFF8EBCDE)
                com.example.scene.WeatherType.RAIN -> Color(0xFF526E88)
                else -> Color(0xFF8ECAE6) // Summer clear light blue
            }
        }
        val skyBottom = when {
            isNight -> when (weather) {
                com.example.scene.WeatherType.SNOW -> Color(0xFF0D173C)
                com.example.scene.WeatherType.SAKURA -> Color(0xFF1B143A)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFF16122E)
                com.example.scene.WeatherType.RAIN -> Color(0xFF0D1226)
                else -> NightSkyMid
            }
            isSunset -> when (weather) {
                com.example.scene.WeatherType.SAKURA -> Color(0xFFFFB5C2)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFFF7BA3E)
                com.example.scene.WeatherType.SNOW -> Color(0xFFDF9FB8)
                com.example.scene.WeatherType.RAIN -> Color(0xFFA66D60)
                else -> Color(0xFFFFD166)
            }
            else -> when (weather) { // Daytime
                com.example.scene.WeatherType.SAKURA -> Color(0xFFFFD6E7)
                com.example.scene.WeatherType.AUTUMN -> Color(0xFFEAD5A8)
                com.example.scene.WeatherType.SNOW -> Color(0xFFEBF4FA)
                com.example.scene.WeatherType.RAIN -> Color(0xFF9BB1C2)
                else -> Color(0xFFD0EEF8) // Summer soft light blue
            }
        }
        scope.drawRect(skyTop, Offset(startX, 0f), Size(windowW, windowH * 0.45f))
        scope.drawRect(skyBottom, Offset(startX, windowH * 0.45f), Size(windowW, windowH * 0.55f))

        // Soft distant clouds
        val cloudColor = when {
            isNight -> SkyCloud
            isSunset -> Color(0xFFFFCCD5).copy(alpha = 0.65f)
            else -> Color(0xE0FFFFFF)
        }
        scope.drawRect(cloudColor, Offset(startX + 14 * p, windowH * 0.16f), Size(42 * p, 7 * p))
        scope.drawRect(cloudColor, Offset(startX + 40 * p, windowH * 0.22f), Size(56 * p, 9 * p))
        scope.drawRect(cloudColor, Offset(startX + 8 * p, windowH * 0.28f), Size(32 * p, 6 * p))

        val celestialX = startX + windowW * 0.74f
        val celestialY = ch * 0.12f

        if (isNight) {
            // 2. Full Luminous Moon (top right)
            val moonR = 14 * p
            scope.drawCircle(MoonGlowOuter, moonR + 9 * p, Offset(celestialX, celestialY))
            scope.drawCircle(MoonGlowInner, moonR + 4 * p, Offset(celestialX, celestialY))
            scope.drawCircle(MoonBody, moonR, Offset(celestialX, celestialY))

            // Soft craters on moon surface
            scope.drawCircle(MoonShadow, 4.0f * p, Offset(celestialX - 4 * p, celestialY - 3 * p))
            scope.drawCircle(MoonShadow, 4.5f * p, Offset(celestialX + 3 * p, celestialY + 2 * p))
            scope.drawCircle(MoonShadow, 2.8f * p, Offset(celestialX - 2 * p, celestialY + 5 * p))

            // Tonight's phase: the unlit part in faint earthshine, one pixel row at a time.
            val fraction = com.example.ui.currentMoonFraction()
            for (k in 0 until 28) {
                val dy = k + 0.5f - 14f
                val half = kotlin.math.sqrt(196f - dy * dy)
                val span = MoonPhase.shadowSpan(fraction, half) ?: continue
                scope.drawRect(
                    Color(0xFF141A38),
                    Offset(celestialX + span.first * p, celestialY - 14f * p + k * p),
                    Size((span.second - span.first) * p, p)
                )
            }

            // 3. Twinkling stars
            for ((sxRel, syRel, freq) in LOFT_STARS) {
                val sx = startX + windowW * sxRel
                val sy = windowH * syRel
                val twinkle = (sin(timeSeconds * freq) * 0.4f + 0.6f).coerceIn(0.2f, 1.0f)
                scope.drawRect(Color.White.copy(alpha = twinkle), Offset(sx, sy), Size(1.5f * p, 1.5f * p))
                if (twinkle > 0.85f) {
                    scope.drawRect(Color(0x70FFEAA7), Offset(sx - p, sy), Size(3.5f * p, 1.5f * p))
                    scope.drawRect(Color(0x70FFEAA7), Offset(sx, sy - p), Size(1.5f * p, 3.5f * p))
                }
            }
        } else if (isSunset) {
            // Warm sunset sun
            val sunR = 13 * p
            scope.drawCircle(Color(0x40FFB703), sunR + 10 * p, Offset(celestialX, celestialY + 12 * p))
            scope.drawCircle(Color(0x70FB8500), sunR + 5 * p, Offset(celestialX, celestialY + 12 * p))
            scope.drawCircle(Color(0xFFFFB703), sunR, Offset(celestialX, celestialY + 12 * p))
        } else {
            // Daytime sun glow
            val sunR = 12 * p
            scope.drawCircle(Color(0x35FFF3B0), sunR + 12 * p, Offset(celestialX, celestialY))
            scope.drawCircle(Color(0x60FFEAA7), sunR + 6 * p, Offset(celestialX, celestialY))
            scope.drawCircle(Color(0xFFFFF9DB), sunR, Offset(celestialX, celestialY))
        }

        // 4. Distant Skyscraper Silhouettes (Back layer & Front layer)
        val riverTopY = windowH * 0.62f
        val riverH = windowH * 0.15f

        val backBldgColor = when {
            // A shade lighter than the night sky, so the towers keep their shape after dark.
            isNight -> Color(0xFF161D3A)
            isSunset -> Color(0xFF353B48)
            else -> Color(0xFF57606F)
        }
        val spireColor = when {
            isNight -> Color(0xFF26345E)
            isSunset -> Color(0xFF57606F)
            else -> Color(0xFF747D8C)
        }

        for (tower in LOFT_BACK_BUILDINGS) {
            val bw = tower.w * p
            val bx = startX + windowW * tower.x
            val by = windowH * tower.top
            // A narrower crown on top, then the full-width tower below it.
            val bodyY = if (tower.crown) by + 7 * p else by
            if (tower.crown) scope.drawRect(backBldgColor, Offset(bx + 2 * p, by), Size(bw - 4 * p, 7 * p))
            scope.drawRect(backBldgColor, Offset(bx, bodyY), Size(bw, riverTopY - bodyY))
            // A lit edge down one side and along the tops gives the slim towers some depth.
            scope.drawRect(spireColor, Offset(bx + bw - p, bodyY), Size(p, riverTopY - bodyY))
            if (tower.crown) scope.drawRect(spireColor, Offset(bx + 2 * p, by), Size(bw - 4 * p, p))
            scope.drawRect(spireColor, Offset(bx, bodyY), Size(bw, p))

            // Radio antenna spire
            if (tower.spire) {
                scope.drawRect(spireColor, Offset(bx + bw / 2f - 0.7f * p, by - 12 * p), Size(1.4f * p, 12 * p))
                val blink = if (sin(timeSeconds * 5f) > 0f) Color(0xFFFF3366) else Color(0x30FF3366)
                scope.drawRect(blink, Offset(bx + bw / 2f - 1.2f * p, by - 14 * p), Size(2.4f * p, 2 * p))
            }

            // Window Grid
            var wy = bodyY + 3 * p
            while (wy < riverTopY - 4 * p) {
                var wx = bx + 2 * p
                while (wx < bx + bw - 2 * p) {
                    val hash = ((wx * 17 + wy * 31).toInt()) % 100
                    if (isNight) {
                        if (hash > 40) {
                            val windowColor = when (hash % 4) {
                                0 -> WindowGold
                                1 -> WindowAmber
                                2 -> WindowCyan
                                else -> WindowWarmWhite
                            }
                            scope.drawRect(windowColor, Offset(wx, wy), Size(1.8f * p, 2.2f * p))
                        }
                    } else if (isSunset) {
                        if (hash > 50) {
                            val windowColor = if (hash % 2 == 0) Color(0xFFFFB703) else Color(0xFFFB8500)
                            scope.drawRect(windowColor.copy(alpha = 0.85f), Offset(wx, wy), Size(1.8f * p, 2.2f * p))
                        }
                    } else {
                        // Daylight glass glints
                        if (hash > 65) {
                            scope.drawRect(Color(0x90E2EAFC), Offset(wx, wy), Size(1.8f * p, 2.2f * p))
                        }
                    }
                    wx += 3.6f * p
                }
                wy += 4.6f * p
            }
        }

        // 5. City River / Harbor with shimmering horizontal reflections
        val riverWater = when {
            isNight -> CityWater
            isSunset -> Color(0xFFB56576)
            else -> Color(0xFF2B6CB0)
        }
        val riverWaterDeep = when {
            isNight -> CityWaterDeep
            isSunset -> Color(0xFF6D3B47)
            else -> Color(0xFF1E4E8C)
        }
        scope.drawRect(riverWater, Offset(startX, riverTopY), Size(windowW, riverH))
        scope.drawRect(riverWaterDeep, Offset(startX, riverTopY + riverH * 0.5f), Size(windowW, riverH * 0.5f))

        // Horizontal shimmering reflections rippling across water
        for (i in 0 until 18) {
            val rippleY = riverTopY + (i % 6) * (riverH / 6.5f) + 1.5f * p
            val waveOffset = sin(timeSeconds * 2.5f + i * 1.3f) * 6 * p
            val rx = startX + ((i * 15 * p + waveOffset) % windowW)
            val rw = (8 + (i % 4) * 6) * p
            val rColor = when {
                isNight -> if (i % 4 == 0) CityWaterReflectionCyan else CityWaterReflection
                isSunset -> if (i % 3 == 0) Color(0xFFFFB703) else Color(0xFFF28482)
                else -> if (i % 3 == 0) Color(0x90FFFFFF) else Color(0x7090E0EF)
            }
            scope.drawRect(rColor, Offset(rx, rippleY), Size(rw, 1.4f * p))
        }

        // 6. Waterfront Promenade & Highway (Lower Shoreline)
        val shoreY = riverTopY + riverH
        val shoreH = windowBottomY - shoreY
        scope.drawRect(backBldgColor, Offset(startX, shoreY), Size(windowW, shoreH))

        // Waterfront buildings along the bank
        val frontBldgMid = when {
            isNight -> SkyscraperMid
            isSunset -> Color(0xFF4A4E69)
            else -> Color(0xFF747D8C)
        }
        val frontBldgLight = when {
            isNight -> SkyscraperLight
            isSunset -> Color(0xFF6C757D)
            else -> Color(0xFF90A4AE)
        }
        for ((fbxRel, fbwFactor) in LOFT_FRONT_BUILDINGS) {
            val fbw = fbwFactor * p
            val fbx = startX + windowW * fbxRel
            scope.drawRect(frontBldgMid, Offset(fbx, shoreY), Size(fbw, shoreH))
            scope.drawRect(frontBldgLight, Offset(fbx + 2 * p, shoreY), Size(fbw - 4 * p, 2 * p))

            // Building window rows
            var fwy = shoreY + 3 * p
            while (fwy < windowBottomY - 4 * p) {
                var fwx = fbx + 2 * p
                while (fwx < fbx + fbw - 2 * p) {
                    val hash = ((fwx * 31 + fwy * 59).toInt()) % 100
                    if (isNight && hash > 35) {
                        val c = if (hash % 2 == 0) WindowAmber else WindowGold
                        scope.drawRect(c, Offset(fwx, fwy), Size(1.6f * p, 2.0f * p))
                    } else if (isSunset && hash > 45) {
                        val c = if (hash % 2 == 0) Color(0xFFFFB703) else Color(0xFFF4A261)
                        scope.drawRect(c, Offset(fwx, fwy), Size(1.6f * p, 2.0f * p))
                    } else if (!isNight && !isSunset && hash > 60) {
                        scope.drawRect(Color(0x80E2EAFC), Offset(fwx, fwy), Size(1.6f * p, 2.0f * p))
                    }
                    fwx += 3.2f * p
                }
                fwy += 4.0f * p
            }
        }

        // Waterfront Highway / Bridge with moving car light streaks
        val roadColor = if (isNight) Color(0xFF151828) else Color(0xFF343A40)
        val roadY = windowBottomY - 5 * p
        scope.drawRect(roadColor, Offset(startX, roadY), Size(windowW, 4 * p))
        for (i in 0..8) {
            // White/Amber headlights moving right
            val carX1 = startX + ((i * 24 * p + timeSeconds * 18 * p) % windowW)
            val headColor = if (isNight) Color(0xFFFFEEAA) else Color(0x90FFEEAA)
            scope.drawRect(headColor, Offset(carX1, roadY + 0.8f * p), Size(4 * p, 1.2f * p))

            // Red taillights moving left
            val carX2 = startX + windowW - ((i * 26 * p + timeSeconds * 14 * p) % windowW)
            val tailColor = if (isNight) Color(0xFFFF3366) else Color(0x90FF3366)
            scope.drawRect(tailColor, Offset(carX2, roadY + 2.2f * p), Size(3.5f * p, 1.2f * p))
        }

        // 7. Weather effects drifting outside the panoramic window
        when (weather) {
            com.example.scene.WeatherType.SNOW -> {
                for (i in 0 until 24) {
                    val sx = startX + ((i * 37 * p + timeSeconds * 8 * p + sin(timeSeconds + i) * 12 * p) % windowW)
                    val sy = ((i * 29 * p + timeSeconds * 22 * p) % windowH)
                    val sSize = if (i % 3 == 0) 2.2f * p else 1.4f * p
                    scope.drawRect(Color.White.copy(alpha = 0.85f), Offset(sx, sy), Size(sSize, sSize))
                }
            }
            com.example.scene.WeatherType.SAKURA -> {
                for (i in 0 until 18) {
                    val px = startX + ((i * 43 * p + timeSeconds * 12 * p + sin(timeSeconds * 1.5f + i) * 16 * p) % windowW)
                    val py = ((i * 31 * p + timeSeconds * 18 * p) % windowH)
                    scope.drawRect(Color(0xFFFFCAD4).copy(alpha = 0.8f), Offset(px, py), Size(2.5f * p, 2f * p))
                }
            }
            com.example.scene.WeatherType.RAIN -> {
                for (i in 0 until 28) {
                    val rx = startX + ((i * 29 * p + timeSeconds * 10 * p) % windowW)
                    val ry = ((i * 23 * p + timeSeconds * 65 * p) % windowH)
                    scope.drawRect(Color(0x99B0C4DE), Offset(rx, ry), Size(1.2f * p, 7f * p))
                }
            }
            com.example.scene.WeatherType.AUTUMN -> {
                for (i in 0 until 16) {
                    val ax = startX + ((i * 47 * p + timeSeconds * 14 * p + cos(timeSeconds + i) * 14 * p) % windowW)
                    val ay = ((i * 33 * p + timeSeconds * 16 * p) % windowH)
                    val leafCol = if (i % 2 == 0) Color(0xFFE76F51) else Color(0xFFF4A261)
                    scope.drawRect(leafCol.copy(alpha = 0.85f), Offset(ax, ay), Size(2.8f * p, 2.2f * p))
                }
            }
            else -> {}
        }
    }

    private fun drawSlantedCeiling(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        p: Float,
        roofStartY: Float,
        roofEndY: Float,
        windowStartX: Float
    ) {
        // Angled ceiling solid timber panels on the left (above the library bookshelf)
        val stepCountLeft = 25
        val stepWLeft = windowStartX / stepCountLeft
        for (i in 0 until stepCountLeft) {
            val sx = i * stepWLeft
            val roofY = roofStartY + (sx / cw) * (roofEndY - roofStartY)
            scope.drawRect(RafterWood, Offset(sx, 0f), Size(stepWLeft + 0.5f, roofY + 4 * p))
        }

        // Horizontal plank grooves in left ceiling
        for (i in 0 until 14) {
            val py = i * 4.5f * p
            scope.drawRect(Color(0x35000000), Offset(0f, py), Size(windowStartX, 1.2f * p))
        }

        // Heavy dark timber main diagonal roof rafter beam cutting across from top-left to right wall
        val stepCount = 60
        val stepW = cw / stepCount
        val beamThick = 7 * p
        for (i in 0 until stepCount) {
            val sx = i * stepW
            val roofY = roofStartY + (sx / cw) * (roofEndY - roofStartY)
            scope.drawRect(DarkWoodBeam, Offset(sx, roofY), Size(stepW + 0.5f, beamThick))
            scope.drawRect(PlankWoodLight, Offset(sx, roofY + beamThick - 1.5f * p), Size(stepW + 0.5f, 1.5f * p))
        }

        // Hanging rustic ceiling lantern on the left with climbing ivy
        val lanternBeamX = windowStartX * 0.45f
        val lanternBeamY = roofStartY + (lanternBeamX / cw) * (roofEndY - roofStartY) + beamThick
        val lanternY = lanternBeamY + 12 * p

        // Metal chain
        scope.drawRect(Color(0xFF150A05), Offset(lanternBeamX + 3.5f * p, lanternBeamY), Size(1.5f * p, 12 * p))

        // Wooden lantern body
        scope.drawRect(DarkWoodBeam, Offset(lanternBeamX, lanternY), Size(8 * p, 12 * p))
        scope.drawRect(PlankWood, Offset(lanternBeamX + p, lanternY + p), Size(6 * p, 10 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(lanternBeamX + 2 * p, lanternY + 3 * p), Size(4 * p, 6 * p))

        // Radiant glow
        scope.drawCircle(Color(0x25FFAA00), 18 * p, Offset(lanternBeamX + 4 * p, lanternY + 6 * p))

        // Ivy vines wrapped around lantern and rafter
        val vineDark = Color(0xFF1B4332)
        val vineMid = Color(0xFF2D6A4F)
        val vineLight = Color(0xFF52B788)
        scope.drawRect(vineDark, Offset(lanternBeamX - 2 * p, lanternBeamY + 2 * p), Size(4 * p, 6 * p))
        scope.drawRect(vineMid, Offset(lanternBeamX - 3 * p, lanternBeamY + 6 * p), Size(5 * p, 8 * p))
        scope.drawRect(vineLight, Offset(lanternBeamX - 2 * p, lanternBeamY + 10 * p), Size(3 * p, 5 * p))
    }

    private fun drawLoftWindowFrames(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        startX: Float,
        windowBottomY: Float,
        roofStartY: Float,
        roofEndY: Float,
        p: Float
    ) {
        val windowW = cw - startX

        // 1. Left border vertical timber pillar
        scope.drawRect(DarkWoodBeam, Offset(startX, 0f), Size(6 * p, windowBottomY))
        scope.drawRect(PlankWoodLight, Offset(startX + 4.5f * p, 0f), Size(1.5f * p, windowBottomY))

        // 2. Right border timber pillar
        scope.drawRect(DarkWoodBeam, Offset(cw - 6 * p, 0f), Size(6 * p, windowBottomY))

        // 3. Central Vertical Mullion dividing the window into two massive glass panes
        val midMullionX = startX + windowW * 0.48f
        scope.drawRect(DarkWoodBeam, Offset(midMullionX, 0f), Size(5 * p, windowBottomY))
        scope.drawRect(PlankWoodLight, Offset(midMullionX + 3.5f * p, 0f), Size(1.5f * p, windowBottomY))

        // 4. Horizontal sill beam at window base
        scope.drawRect(DarkWoodBeam, Offset(startX, windowBottomY - 4 * p), Size(windowW, 5 * p))
        scope.drawRect(PlankWoodLight, Offset(startX, windowBottomY - 4 * p), Size(windowW, 1.5f * p))

        // 5. Heavy diagonal architectural cross-brace trusses cutting through the glass
        val braceSteps = 45
        for (i in 0 until braceSteps) {
            val progress = i / braceSteps.toFloat()
            val bx = startX + progress * windowW
            val by = (windowBottomY * 0.35f) + progress * (windowBottomY * 0.45f)
            scope.drawRect(DarkWoodBeam, Offset(bx, by), Size(4.5f * p, 4 * p))
            scope.drawRect(PlankWoodLight, Offset(bx, by), Size(4.5f * p, 1.2f * p))
        }

        // 6. Cascading ivy vines hanging along the central timber mullion
        for (i in 0 until 16) {
            val vy = i * 8 * p
            val vx = startX + 2 * p
            scope.drawRect(Color(0xFF1B4332), Offset(vx, vy), Size(4 * p, 6 * p))
            scope.drawRect(Color(0xFF2D6A4F), Offset(vx - 2 * p, vy + 2 * p), Size(5 * p, 5 * p))
            scope.drawRect(Color(0xFF52B788), Offset(vx + p, vy + 3 * p), Size(3 * p, 3 * p))
        }
    }

    private fun drawLowerWallPanels(
        scope: DrawScope,
        cw: Float,
        windowStartX: Float,
        windowBottomY: Float,
        floorY: Float,
        p: Float,
        lampLit: Boolean
    ) {
        val wallH = floorY - windowBottomY
        scope.drawRect(WarmWall, Offset(windowStartX, windowBottomY), Size(cw - windowStartX, wallH))

        // Vertical wainscoting timber grooved slats
        var px = windowStartX
        while (px < cw) {
            scope.drawRect(DarkWoodBeam, Offset(px, windowBottomY), Size(1.2f * p, wallH))
            scope.drawRect(WarmWallPanel, Offset(px + 1.2f * p, windowBottomY), Size(5 * p, wallH))
            px += 6.2f * p
        }
    }

    /** Where the loft's floor ends at the front, under the balcony railing. */
    fun floorEdgeY(ch: Float) = ch * 0.80f

    /**
     * Floorboards in perspective: the seams fan out from a vanishing point above the window wall,
     * boards widen and butt joints spread out toward the viewer, with a shadow along the wall,
     * the window's light on the boards and the front edge in shade.
     */
    private fun drawPerspectiveFloor(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        edgeY: Float,
        p: Float,
        isNight: Boolean,
        isSunset: Boolean
    ) {
        val seam = Color(0xFF28150A)
        val toneDark = Color(0xFF472611)
        val toneLight = Color(0xFF55301A)
        scope.drawRect(PlankWood, Offset(0f, floorY), Size(cw, edgeY - floorY))
        val vpX = cw * 0.56f
        val vpY = floorY - 70f * p
        val farW = 7f * p
        // Butt-joint rows: evenly spaced in depth, so further apart toward the viewer.
        val joints = FloatArray(6) { j -> vpY + (floorY - vpY) / (1f - (j + 1) * 0.12f) }
        var y = floorY
        while (y < edgeY) {
            val w = farW * (y - vpY) / (floorY - vpY)
            val band = joints.count { it <= y }
            val atJoint = joints.any { y >= it && y < it + p }
            val kMin = kotlin.math.floor(-vpX / w).toInt() - 1
            val kMax = kotlin.math.ceil((cw - vpX) / w).toInt()
            for (k in kMin..kMax) {
                val x = vpX + k * w
                val tone = ((k % 3) + 3) % 3
                if (tone == 1) scope.drawRect(toneDark, Offset(x, y), Size(w, p))
                if (tone == 2) scope.drawRect(toneLight, Offset(x, y), Size(w, p))
                if (atJoint && ((k + band * 2) % 3 + 3) % 3 == 0) scope.drawRect(seam, Offset(x, y), Size(w, p))
                scope.drawRect(seam, Offset(x, y), Size(p, p))
            }
            y += p
        }
        // Shadow along the wall and under the sofa.
        scope.drawRect(Color(0x45000000), Offset(0f, floorY), Size(cw, 2f * p))
        scope.drawRect(Color(0x22000000), Offset(0f, floorY + 2f * p), Size(cw, 3f * p))
        // The window's light falling on the boards, widening toward the viewer.
        val light = when {
            isNight -> Color(0x10A8BEFF)
            isSunset -> Color(0x1CFFA060)
            else -> Color(0x1CFFE2B0)
        }
        var ly = floorY + 5f * p
        while (ly < edgeY - 8f * p) {
            val t = (ly - vpY) / (floorY - vpY)
            val left = vpX + (cw * 0.34f - vpX) * t
            val right = vpX + (cw * 0.98f - vpX) * t
            scope.drawRect(light, Offset(left, ly), Size(right - left, 2f * p))
            ly += 2f * p
        }
        // The far corners and the front edge fall into shade.
        scope.drawRect(Color(0x26000000), Offset(0f, floorY), Size(cw * 0.08f, edgeY - floorY))
        scope.drawRect(Color(0x26000000), Offset(cw * 0.94f, floorY), Size(cw * 0.06f, edgeY - floorY))
        scope.drawRect(Color(0x30000000), Offset(0f, edgeY - 5f * p), Size(cw, 5f * p))
    }

    /**
     * The room below the loft, seen past the floor's edge: a dim panelled wall that darkens with
     * depth. Shades go by distance from the floor's edge, so the stage and the strip continued below
     * it (drawLoftBelowStage) join without a seam.
     */
    internal fun drawLoftLowerLevel(scope: DrawScope, cw: Float, ch: Float, top: Float, bottom: Float, p: Float) {
        val edgeY = floorEdgeY(ch)
        val shades = arrayOf(Color(0xFF2B170B), Color(0xFF231309), Color(0xFF1C0F07), Color(0xFF160B05))
        val bandH = 10f * p
        var y = top
        while (y < bottom) {
            val band = ((y - edgeY) / bandH).toInt().coerceIn(0, shades.size - 1)
            val bandEnd = if (band == shades.size - 1) bottom else minOf(bottom, edgeY + (band + 1) * bandH)
            scope.drawRect(shades[band], Offset(0f, y), Size(cw, bandEnd - y))
            // A dithered row where one shade meets the next.
            if (band < shades.size - 1 && bandEnd - 2f * p >= top && bandEnd < bottom + p) {
                var dx = if (band % 2 == 0) 0f else 3f * p
                while (dx < cw) {
                    scope.drawRect(shades[band + 1], Offset(dx, bandEnd - 2f * p), Size(3f * p, 2f * p))
                    dx += 6f * p
                }
            }
            y = bandEnd
        }
        // Wall panel seams, faint in the dark.
        var px = 2f * p
        while (px < cw) {
            scope.drawRect(Color(0x30000000), Offset(px, top), Size(p, bottom - top))
            px += 12f * p
        }
        // A little warm spill from the fairy lights just under the edge.
        val glowTop = maxOf(top, edgeY)
        val glowBottom = minOf(bottom, edgeY + 8f * p)
        if (glowBottom > glowTop) scope.drawRect(Color(0x14FFB703), Offset(0f, glowTop), Size(cw, glowBottom - glowTop))
        drawLoftStairs(scope, cw, ch, p, top, bottom)
    }

    /** The stairs down from the loft on the right, clipped to [clipTop]..[clipBottom] (they run on below the stage). */
    private fun drawLoftStairs(scope: DrawScope, cw: Float, ch: Float, p: Float, clipTop: Float, clipBottom: Float) {
        val start = ch * 0.72f + 6 * p
        val stepCount = 5
        val stepH = (ch - start) / stepCount
        val stairStartX = cw * 0.65f
        val stepDx = (cw - stairStartX) / stepCount * 0.4f
        var s = 0
        while (true) {
            val sy = start + s * stepH
            val sx = stairStartX + s * stepDx
            if (sy >= clipBottom || sx >= cw) break
            val t = maxOf(sy, clipTop)
            val b = minOf(sy + stepH, clipBottom)
            if (b > t) {
                scope.drawRect(Color(0xFF381F0E), Offset(sx, t), Size(cw - sx, b - t))
                if (sy >= clipTop) scope.drawRect(PlankWoodLight, Offset(sx, sy), Size(cw - sx, 1.2f * p))
                val lip = sy + stepH - p
                if (lip >= clipTop && lip < clipBottom) scope.drawRect(DarkWoodBeam, Offset(sx, lip), Size(cw - sx, p))
            }
            s++
        }
    }

    /** The lower level and stairs continued below the stage on tall screens (world units, stage origin). */
    fun drawLoftBelowStage(scope: DrawScope, cw: Float, stageH: Float, belowH: Float, p: Float) {
        drawLoftLowerLevel(scope, cw, stageH, stageH, stageH + belowH, p)
    }

    private fun drawLoftFloorAndRug(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        floorY: Float,
        p: Float,
        roomTheme: RoomTheme,
        isNight: Boolean,
        isSunset: Boolean
    ) {
        val edgeY = floorEdgeY(ch)
        // The loft's floorboards run away from the viewer toward the window wall.
        drawPerspectiveFloor(scope, cw, floorY, edgeY, p, isNight, isSunset)
        // Past the floor's edge: the room below, in warm shadow.
        drawLoftLowerLevel(scope, cw, ch, edgeY, ch, p)

        // VINTAGE ORNATE PERSIAN AREA RUG IN FRONT OF SOFA
        drawPersianRug(scope, cw, floorY, p, roomTheme)
    }

    private fun drawPersianRug(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        p: Float,
        roomTheme: RoomTheme
    ) {
        val rugW = 56 * p
        val rugH = 22 * p
        val rugX = cw * 0.58f - rugW / 2f
        val rugY = floorY - 2 * p

        // 1. Fringe Tassels along left and right ends
        scope.drawRect(roomTheme.rugTrim, Offset(rugX - 2.5f * p, rugY + 2 * p), Size(2.5f * p, rugH - 4 * p))
        scope.drawRect(roomTheme.rugTrim, Offset(rugX + rugW, rugY + 2 * p), Size(2.5f * p, rugH - 4 * p))

        // 2. Main Crimson Red Rug Field
        scope.drawRect(roomTheme.rugTrim, Offset(rugX, rugY), Size(rugW, rugH))
        scope.drawRect(roomTheme.rug.copy(alpha = 0.65f), Offset(rugX + p, rugY + p), Size(rugW - 2 * p, rugH - 2 * p))
        scope.drawRect(roomTheme.rug, Offset(rugX + 2 * p, rugY + 2 * p), Size(rugW - 4 * p, rugH - 4 * p))

        // 3. Ornate Floral & Medallion Border (Ivory / Gold / Teal)
        scope.drawRect(roomTheme.wall, Offset(rugX + 2.5f * p, rugY + 2.5f * p), Size(rugW - 5 * p, rugH - 5 * p))
        scope.drawRect(roomTheme.rug, Offset(rugX + 4f * p, rugY + 4f * p), Size(rugW - 8 * p, rugH - 8 * p))

        // 4. Central Geometric Medallions (4 repeating diamond medallions)
        val medW = 8 * p
        val medH = 8 * p
        val medCount = 4
        val gap = (rugW - 14 * p) / medCount
        for (i in 0 until medCount) {
            val mx = rugX + 5 * p + i * gap
            val my = rugY + rugH / 2f - medH / 2f
            scope.drawRect(roomTheme.rugTrim, Offset(mx, my), Size(medW, medH))
            scope.drawRect(roomTheme.beddingAccent, Offset(mx + 1.5f * p, my + 1.5f * p), Size(medW - 3 * p, medH - 3 * p))
            scope.drawRect(roomTheme.wall, Offset(mx + 2.8f * p, my + 2.8f * p), Size(medW - 5.6f * p, medH - 5.6f * p))
        }
    }

    internal fun drawLibraryBookshelf(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        windowStartX: Float,
        floorY: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean,
        boyLook: AvatarLook = AvatarLook.DEFAULT_A,
        girlLook: AvatarLook = AvatarLook.DEFAULT_B
    ) {
        val shelfW = windowStartX - 5 * p

        // Solid wood background backing
        scope.drawRect(Color(0xFF381F0E), Offset(0f, 0f), Size(shelfW, floorY))

        // Outer sturdy bookshelf timber framework
        scope.drawRect(DarkWoodBeam, Offset(0f, 0f), Size(3.5f * p, floorY))
        scope.drawRect(DarkWoodBeam, Offset(shelfW - 3.5f * p, 0f), Size(3.5f * p, floorY))

        // 5 Shelf tiers
        val tierCount = 5
        val shelfGap = floorY / (tierCount + 0.3f)
        val shelfThickness = 3 * p

        val bookColors = LOFT_BOOK_COLORS

        for (tier in 0 until tierCount) {
            val sy = (tier + 1) * shelfGap
            // Shelf wooden ledge
            scope.drawRect(DarkWoodBeam, Offset(0f, sy), Size(shelfW, shelfThickness))
            scope.drawRect(PlankWoodLight, Offset(0f, sy), Size(shelfW, 1.2f * p))

            // Books and decorative trinkets on this shelf
            val tierBaseY = sy - 0.5f * p
            var curX = 4 * p
            val maxX = shelfW - 5 * p
            var bookIdx = tier * 7

            while (curX < maxX) {
                // Tier 1: Globe on brass stand + ceramic teapot
                if (tier == 1 && curX in (shelfW * 0.35f)..(shelfW * 0.70f)) {
                    // Globe
                    scope.drawRect(Color(0xFFB07D62), Offset(curX + 2 * p, tierBaseY - 2 * p), Size(4 * p, 2 * p))
                    scope.drawRect(Color(0xFFDDA15E), Offset(curX + 3.5f * p, tierBaseY - 8 * p), Size(0.8f * p, 6 * p))
                    scope.drawRect(Color(0xFF48CAE4), Offset(curX + 1.5f * p, tierBaseY - 11 * p), Size(5 * p, 5 * p))
                    scope.drawRect(Color(0xFF52B788), Offset(curX + 3 * p, tierBaseY - 10 * p), Size(2 * p, 2 * p))
                    // Teapot
                    scope.drawRect(Color(0xFFF8F9FA), Offset(curX + 8 * p, tierBaseY - 5 * p), Size(4 * p, 5 * p))
                    scope.drawRect(Color(0xFFDDA15E), Offset(curX + 9 * p, tierBaseY - 6.5f * p), Size(2 * p, 1.5f * p))
                    curX += 14 * p
                    continue
                }

                // Tier 2: Framed Couple Picture
                if (tier == 2 && curX in (shelfW * 0.25f)..(shelfW * 0.60f)) {
                    scope.drawRect(Color(0xFFDDA15E), Offset(curX, tierBaseY - 10 * p), Size(10 * p, 10 * p))
                    scope.drawRect(Color(0xFFFFF0F5), Offset(curX + 0.8f * p, tierBaseY - 9.2f * p), Size(8.4f * p, 8.4f * p))
                    // The couple, in their Make Us hair and skin
                    scope.drawRect(boyLook.hair, Offset(curX + 1.5f * p, tierBaseY - 7.5f * p), Size(2.5f * p, 2.5f * p))
                    scope.drawRect(boyLook.skin, Offset(curX + 1.5f * p, tierBaseY - 5 * p), Size(2.5f * p, 2.5f * p))
                    scope.drawRect(girlLook.hair, Offset(curX + 5.5f * p, tierBaseY - 7.5f * p), Size(2.5f * p, 2.5f * p))
                    scope.drawRect(girlLook.skin, Offset(curX + 5.5f * p, tierBaseY - 5 * p), Size(2.5f * p, 2.5f * p))
                    if (girlLook.longHair) { // strands down both sides of the face
                        scope.drawRect(girlLook.hair, Offset(curX + 4.7f * p, tierBaseY - 6.5f * p), Size(0.8f * p, 4 * p))
                        scope.drawRect(girlLook.hair, Offset(curX + 8f * p, tierBaseY - 6.5f * p), Size(0.8f * p, 4 * p))
                    }
                    if (boyLook.longHair) {
                        scope.drawRect(boyLook.hair, Offset(curX + 0.8f * p, tierBaseY - 6.5f * p), Size(0.7f * p, 4 * p))
                        scope.drawRect(boyLook.hair, Offset(curX + 4f * p, tierBaseY - 6.5f * p), Size(0.7f * p, 4 * p))
                    }
                    // Red heart between them
                    scope.drawRect(Color(0xFFFF4D6D), Offset(curX + 4.2f * p, tierBaseY - 4 * p), Size(1.5f * p, 1.5f * p))
                    curX += 11 * p
                    continue
                }

                // Tier 3: White Plush Bunny Toy with pink ears
                if (tier == 3 && curX in (shelfW * 0.10f)..(shelfW * 0.40f)) {
                    scope.drawRect(Color(0xFFF8F9FA), Offset(curX + 1.5f * p, tierBaseY - 7 * p), Size(5.5f * p, 7 * p))
                    scope.drawRect(Color(0xFFF8F9FA), Offset(curX + 2 * p, tierBaseY - 11.5f * p), Size(1.5f * p, 4.5f * p))
                    scope.drawRect(Color(0xFFFFCAD4), Offset(curX + 2.4f * p, tierBaseY - 10.5f * p), Size(0.7f * p, 3.5f * p))
                    scope.drawRect(Color(0xFFF8F9FA), Offset(curX + 5 * p, tierBaseY - 11.5f * p), Size(1.5f * p, 4.5f * p))
                    scope.drawRect(Color(0xFFFFCAD4), Offset(curX + 5.4f * p, tierBaseY - 10.5f * p), Size(0.7f * p, 3.5f * p))
                    scope.drawRect(Color(0xFF22223B), Offset(curX + 3 * p, tierBaseY - 5.5f * p), Size(0.8f * p, 0.8f * p))
                    scope.drawRect(Color(0xFF22223B), Offset(curX + 5 * p, tierBaseY - 5.5f * p), Size(0.8f * p, 0.8f * p))
                    scope.drawRect(Color(0xFFFF758F), Offset(curX + 4 * p, tierBaseY - 4.2f * p), Size(p, p))
                    curX += 10 * p
                    continue
                }

                // Tier 4: Storage boxes with brass handles
                if (tier == 4 && curX in (shelfW * 0.40f)..(shelfW * 0.80f)) {
                    scope.drawRect(Color(0xFFD4A373), Offset(curX, tierBaseY - 8 * p), Size(9 * p, 8 * p))
                    scope.drawRect(Color(0xFFBC6C25), Offset(curX, tierBaseY - 8 * p), Size(9 * p, 1.5f * p))
                    scope.drawRect(Color(0xFFFFD166), Offset(curX + 3.5f * p, tierBaseY - 4 * p), Size(2 * p, p))
                    curX += 11 * p
                    continue
                }

                // Varied rows of books
                val bookW = (1.8f + (bookIdx % 3) * 0.8f) * p
                val bookH = (8 + (bookIdx % 5) * 1.5f) * p
                val bColor = bookColors[bookIdx % bookColors.size]
                scope.drawRect(bColor, Offset(curX, tierBaseY - bookH), Size(bookW, bookH))
                scope.drawRect(Color(0x30FFFFFF), Offset(curX, tierBaseY - bookH), Size(0.6f * p, bookH))
                curX += bookW + 0.6f * p
                bookIdx++
            }
        }

        // Wooden Library Rolling Ladder leaning diagonally against the shelves
        val ladderTopX = shelfW * 0.22f
        val ladderBottomX = shelfW * 0.65f
        val ladderSteps = 10
        for (s in 0..ladderSteps) {
            val progress = s / ladderSteps.toFloat()
            val lx = ladderTopX + (ladderBottomX - ladderTopX) * progress
            val ly = progress * (floorY - 2 * p)
            scope.drawRect(Color(0xFF5A341A), Offset(lx - 3 * p, ly), Size(1.5f * p, 5 * p))
            scope.drawRect(Color(0xFF5A341A), Offset(lx + 3 * p, ly), Size(1.5f * p, 5 * p))
            scope.drawRect(Color(0xFF754522), Offset(lx - 2.5f * p, ly + 1.5f * p), Size(5.5f * p, 1.2f * p))
        }

        // Small study desk & stool in the bottom-left corner
        val deskW = 18 * p
        val deskH = 12 * p
        val deskY = floorY - deskH
        scope.drawRect(Color(0xFF58311B), Offset(2 * p, deskY), Size(deskW, 2.5f * p))
        scope.drawRect(Color(0xFF45240F), Offset(3 * p, deskY + 2.5f * p), Size(1.8f * p, deskH - 2.5f * p))
        scope.drawRect(Color(0xFF45240F), Offset(deskW - 4 * p, deskY + 2.5f * p), Size(1.8f * p, deskH - 2.5f * p))
        // Mini glowing brass desk lantern
        scope.drawRect(Color(0xFFB07D62), Offset(5 * p, deskY - 5 * p), Size(4 * p, 5 * p))
        scope.drawRect(if (lampLit) Color(0xFFFFEAA7) else Color(0x60FFEAA7), Offset(6 * p, deskY - 3.5f * p), Size(2 * p, 2.5f * p))
    }

    private fun drawWallArtAndTableLamp(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        windowStartX: Float,
        floorY: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean
    ) {
        val tableW = 12 * p
        val tableH = 14 * p
        val tableX = windowStartX + p
        val tableY = floorY - tableH

        // Wooden nightstand beside bookshelf
        scope.drawRect(Color(0xFF502914), Offset(tableX, tableY), Size(tableW, tableH))
        scope.drawRect(Color(0xFF6B3A19), Offset(tableX - p, tableY), Size(tableW + 2 * p, 2 * p))
        scope.drawRect(Color(0xFF381B0B), Offset(tableX + 1.5f * p, tableY + 3 * p), Size(tableW - 3 * p, 4 * p))
        // Drawer brass handle
        scope.drawRect(Color(0xFFFFD166), Offset(tableX + tableW / 2f - 0.8f * p, tableY + 4.5f * p), Size(1.6f * p, 0.8f * p))

        // Books stacked on table
        scope.drawRect(Color(0xFF2A6F68), Offset(tableX + 1.5f * p, tableY - 1.8f * p), Size(6 * p, 1.8f * p))
        scope.drawRect(Color(0xFF9E2A2B), Offset(tableX + 2 * p, tableY - 3.2f * p), Size(5 * p, 1.4f * p))

        // Potted green plant on nightstand
        scope.drawRect(Color(0xFFBC6C25), Offset(tableX + 1.5f * p, tableY - 7 * p), Size(4 * p, 3.8f * p))
        scope.drawRect(Color(0xFF2D6A4F), Offset(tableX + 0.8f * p, tableY - 11 * p), Size(5.5f * p, 4 * p))
        scope.drawRect(Color(0xFF52B788), Offset(tableX + 1.5f * p, tableY - 13 * p), Size(4 * p, 2.5f * p))

        // Shaded warm bedside lamp on the table
        val lampBaseX = tableX + tableW - 5.5f * p
        val lampBaseY = tableY - 1.5f * p
        scope.drawRect(Color(0xFFB07D62), Offset(lampBaseX, lampBaseY), Size(4 * p, 1.5f * p))
        scope.drawRect(Color(0xFFDDA15E), Offset(lampBaseX + 1.5f * p, lampBaseY - 6 * p), Size(0.8f * p, 6 * p))

        // Fluted warm amber lamp shade
        val shadeY = lampBaseY - 14 * p
        scope.drawRect(Color(0xFFFFF3B0), Offset(lampBaseX - 3 * p, shadeY), Size(10 * p, 8 * p))
        scope.drawRect(Color(0xFFFFEAA7), Offset(lampBaseX - 1.5f * p, shadeY - 1.5f * p), Size(7 * p, 1.5f * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(lampBaseX - 3 * p, shadeY + 7 * p), Size(10 * p, p))

        // Soft, warm stepped ambient glow (No square box artifacts!)
        if (lampLit) {
            val flicker = sin(timeSeconds * 5f) * 0.05f + 0.95f
            val glowCenter = Offset(lampBaseX + 2 * p, shadeY + 3.5f * p)
            scope.drawCircle(AmberLightLow.copy(alpha = 0.08f * flicker), 55 * p, glowCenter)
            scope.drawCircle(AmberLightLow.copy(alpha = 0.15f * flicker), 36 * p, glowCenter)
            scope.drawCircle(AmberLightMid.copy(alpha = 0.24f * flicker), 20 * p, glowCenter)
            scope.drawCircle(AmberLightBright.copy(alpha = 0.38f * flicker), 10 * p, glowCenter)
        }

        // 4 Framed picture prints stacked on the vertical column between bookshelf & window
        val artX = windowStartX - 5 * p

        // Print 1: Red Heart print
        scope.drawRect(Color(0xFFDDA15E), Offset(artX, floorY * 0.24f), Size(5 * p, 6 * p))
        scope.drawRect(Color(0xFFFFF0F5), Offset(artX + 0.6f * p, floorY * 0.24f + 0.6f * p), Size(3.8f * p, 4.8f * p))
        scope.drawRect(Color(0xFFFF4D6D), Offset(artX + 1.6f * p, floorY * 0.24f + 1.8f * p), Size(1.8f * p, 1.8f * p))

        // Print 2: Mountain Sunset landscape
        scope.drawRect(Color(0xFFDDA15E), Offset(artX, floorY * 0.36f), Size(5 * p, 6 * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(artX + 0.6f * p, floorY * 0.36f + 0.6f * p), Size(3.8f * p, 4.8f * p))
        scope.drawRect(Color(0xFF264653), Offset(artX + 0.8f * p, floorY * 0.36f + 2.5f * p), Size(3.4f * p, 2.5f * p))

        // Print 3: Botanical green fern leaf
        scope.drawRect(Color(0xFFDDA15E), Offset(artX, floorY * 0.48f), Size(5 * p, 6 * p))
        scope.drawRect(Color(0xFFF8F9FA), Offset(artX + 0.6f * p, floorY * 0.48f + 0.6f * p), Size(3.8f * p, 4.8f * p))
        scope.drawRect(Color(0xFF2D6A4F), Offset(artX + 1.6f * p, floorY * 0.48f + 1.5f * p), Size(1.8f * p, 3 * p))

        // Print 4: Couple silhouette print
        scope.drawRect(Color(0xFFDDA15E), Offset(artX, floorY * 0.60f), Size(5 * p, 6 * p))
        scope.drawRect(Color(0xFFFFF1E6), Offset(artX + 0.6f * p, floorY * 0.60f + 0.6f * p), Size(3.8f * p, 4.8f * p))
        scope.drawRect(Color(0xFF2B2D42), Offset(artX + 1.5f * p, floorY * 0.60f + 1.6f * p), Size(2 * p, 2.6f * p))
    }

    private fun drawLoftSofaBase(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        p: Float,
        lampLit: Boolean
    ) {
        val sofaStartX = cw * 0.38f
        val sofaEndX = cw * 0.81f
        val sofaW = sofaEndX - sofaStartX
        val sofaH = 26 * p
        val sofaY = floorY - sofaH + 6 * p

        // Sofa wooden turned legs
        scope.drawRect(DarkWoodBeam, Offset(sofaStartX + 4 * p, floorY - 2.5f * p), Size(3 * p, 2.5f * p))
        scope.drawRect(DarkWoodBeam, Offset(sofaEndX - 7 * p, floorY - 2.5f * p), Size(3 * p, 2.5f * p))
        scope.drawRect(DarkWoodBeam, Offset(sofaStartX + sofaW / 2f - 1.5f * p, floorY - 2.5f * p), Size(3 * p, 2.5f * p))

        // High tufted backrest cushions
        scope.drawRect(SofaLeatherShadow, Offset(sofaStartX + 4 * p, sofaY - 5 * p), Size(sofaW - 8 * p, 16 * p))
        scope.drawRect(SofaLeather, Offset(sofaStartX + 4.5f * p, sofaY - 4.5f * p), Size(sofaW - 9 * p, 15 * p))
        scope.drawRect(SofaLeatherHighlight, Offset(sofaStartX + 5 * p, sofaY - 4.5f * p), Size(sofaW - 10 * p, 2 * p))

        // Tufted vertical button creases (5 segments across wide bed)
        val tuftSegments = 5
        val segW = (sofaW - 10 * p) / tuftSegments
        for (i in 1 until tuftSegments) {
            val bx = sofaStartX + 5 * p + i * segW
            scope.drawRect(SofaLeatherDark, Offset(bx - 0.6f * p, sofaY - 2 * p), Size(1.2f * p, 12 * p))
            scope.drawRect(SofaLeatherDark, Offset(bx - 1.2f * p, sofaY + 2 * p), Size(2.4f * p, 1.5f * p))
            scope.drawRect(SofaLeatherDark, Offset(bx - 1.2f * p, sofaY + 7 * p), Size(2.4f * p, 1.5f * p))
        }

        // Left rolled armrest
        scope.drawRect(SofaLeatherShadow, Offset(sofaStartX, sofaY + 2 * p), Size(6.5f * p, sofaH - 2 * p))
        scope.drawRect(SofaLeather, Offset(sofaStartX + 0.8f * p, sofaY + 3 * p), Size(5f * p, sofaH - 4 * p))
        scope.drawRect(SofaLeatherHighlight, Offset(sofaStartX + 1.5f * p, sofaY + 3 * p), Size(3.8f * p, 2.2f * p))

        // Right rolled armrest
        scope.drawRect(SofaLeatherShadow, Offset(sofaEndX - 6.5f * p, sofaY + 2 * p), Size(6.5f * p, sofaH - 2 * p))
        scope.drawRect(SofaLeather, Offset(sofaEndX - 5.8f * p, sofaY + 3 * p), Size(5f * p, sofaH - 4 * p))
        scope.drawRect(SofaLeatherHighlight, Offset(sofaEndX - 5.2f * p, sofaY + 3 * p), Size(3.8f * p, 2.2f * p))

        // 1. Left throw cushion: Green plaid pillow propped against left armrest
        val pillLx = sofaStartX + 5 * p
        val pillLy = sofaY + 4 * p
        scope.drawRect(Color(0xFF2D6A4F), Offset(pillLx, pillLy), Size(9 * p, 9 * p))
        scope.drawRect(Color(0xFF52B788), Offset(pillLx + 3.2f * p, pillLy), Size(1.8f * p, 9 * p))
        scope.drawRect(Color(0xFF52B788), Offset(pillLx, pillLy + 3.2f * p), Size(9 * p, 1.8f * p))
        scope.drawRect(Color(0xFF1B4332), Offset(pillLx + 1.2f * p, pillLy + 1.2f * p), Size(1.2f * p, 1.2f * p))

        // 2. Right throw cushion: Cream pillow with red embroidered heart propped against right armrest
        val pillRx = sofaEndX - 14 * p
        val pillRy = sofaY + 5.5f * p
        scope.drawRect(Color(0xFFFFF0F3), Offset(pillRx, pillRy), Size(9 * p, 9 * p))
        scope.drawRect(Color(0xFFFF758F), Offset(pillRx + 2.8f * p, pillRy + 2.5f * p), Size(3.5f * p, 3.2f * p))
        scope.drawRect(Color(0xFFFF4D6D), Offset(pillRx + 3.8f * p, pillRy + 5.7f * p), Size(1.5f * p, 1.2f * p))
    }

    fun drawPatchworkBlanket(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        p: Float,
        roomTheme: RoomTheme = RoomTheme.WARM_AUTUMN_COTTAGE
    ) {
        val sofaStartX = cw * 0.38f
        val sofaEndX = cw * 0.81f
        val sofaW = sofaEndX - sofaStartX
        val blanketStartX = sofaStartX + 3.5f * p
        val blanketW = sofaW - 7 * p
        val blanketH = 13 * p
        val blanketY = floorY - 7 * p

        // Base dark fleece shadow
        scope.drawRect(roomTheme.beddingAccent, Offset(blanketStartX, blanketY), Size(blanketW, blanketH))

        // Checkered patchwork fleece blanket covering daybed & couple laps
        val checkW = 3.6f * p
        val checkH = 3.0f * p
        var cy = 0
        while (cy * checkH < blanketH) {
            var cx = 0
            while (cx * checkW < blanketW) {
                val color = when ((cx + cy) % 4) {
                    0 -> roomTheme.bedding
                    1 -> roomTheme.beddingAccent
                    2 -> roomTheme.rugTrim
                    else -> roomTheme.wall
                }
                val curW = checkW.coerceAtMost(blanketW - cx * checkW)
                val curH = checkH.coerceAtMost(blanketH - cy * checkH)
                scope.drawRect(color, Offset(blanketStartX + cx * checkW, blanketY + cy * checkH), Size(curW, curH))
                cx++
            }
            cy++
        }

        // Folded blanket edge draping down the sofa front
        scope.drawRect(roomTheme.wall, Offset(blanketStartX - p, blanketY + blanketH - 1.8f * p), Size(blanketW + 2 * p, 1.8f * p))

        // Cozy fringe threads along bottom edge
        val tasselCount = (blanketW / (2.6f * p)).toInt()
        for (i in 0..tasselCount) {
            val fx = blanketStartX + i * 2.6f * p
            scope.drawRect(roomTheme.rugTrim, Offset(fx, blanketY + blanketH), Size(1.2f * p, 2.5f * p))
        }
    }

    private fun drawCoffeeTable(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean,
        roomTheme: RoomTheme = RoomTheme.WARM_AUTUMN_COTTAGE
    ) {
        val tableW = 34 * p
        val tableH = 13 * p
        val tableX = cw * 0.55f - tableW / 2f
        val tableY = floorY + 4 * p

        // Table wooden legs
        scope.drawRect(DarkWoodBeam, Offset(tableX + 2.5f * p, tableY + tableH - 4 * p), Size(2.5f * p, 4 * p))
        scope.drawRect(DarkWoodBeam, Offset(tableX + tableW - 5 * p, tableY + tableH - 4 * p), Size(2.5f * p, 4 * p))

        // Table surface slab
        scope.drawRect(Color(0xFF45240F), Offset(tableX, tableY), Size(tableW, 4.5f * p))
        scope.drawRect(Color(0xFF6B3A19), Offset(tableX, tableY), Size(tableW, 1.6f * p))
        scope.drawRect(Color(0xFF28150A), Offset(tableX, tableY + 3.8f * p), Size(tableW, 0.8f * p))

        // 1. Flower vase with white daisies
        val vaseX = tableX + 3 * p
        val vaseY = tableY - 6.5f * p
        scope.drawRect(Color(0xFF2A6F68), Offset(vaseX, vaseY), Size(3.5f * p, 6.5f * p))
        scope.drawRect(Color(0xFF52B788), Offset(vaseX + 0.6f * p, vaseY), Size(2.2f * p, 1.8f * p))
        // Daisy flowers
        scope.drawRect(Color.White, Offset(vaseX - 1.5f * p, vaseY - 3.5f * p), Size(2.5f * p, 2.5f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(vaseX - 0.7f * p, vaseY - 2.8f * p), Size(p, p))
        scope.drawRect(Color.White, Offset(vaseX + 2.5f * p, vaseY - 4.5f * p), Size(3 * p, 3 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(vaseX + 3.5f * p, vaseY - 3.5f * p), Size(p, p))

        // 2. Stack of books with steaming cup
        val bookX = tableX + 9 * p
        val bookY = tableY - 3 * p
        scope.drawRect(Color(0xFF264653), Offset(bookX, bookY), Size(7 * p, 3 * p))
        scope.drawRect(Color(0xFFE76F51), Offset(bookX + 0.8f * p, bookY - 1.5f * p), Size(5.5f * p, 1.5f * p))

        // 3. Warm Glowing Glass Hurricane Lantern in center
        val lanternX = tableX + tableW * 0.44f
        val lanternY = tableY - 8 * p
        scope.drawRect(DarkWoodBeam, Offset(lanternX, lanternY), Size(5.5f * p, 8 * p))
        scope.drawRect(Color(0xFF45240F), Offset(lanternX - 0.8f * p, lanternY + 7 * p), Size(7 * p, 1.5f * p))
        val lanternLight = if (lampLit) Color(0xFFFFD166) else Color(0x80FFEAA7)
        scope.drawRect(lanternLight, Offset(lanternX + p, lanternY + 1.2f * p), Size(3.5f * p, 5f * p))
        if (lampLit) {
            val flicker = sin(timeSeconds * 6f) * 0.08f + 0.92f
            scope.drawCircle(AmberLightMid.copy(alpha = 0.22f * flicker), 14 * p, Offset(lanternX + 2.8f * p, lanternY + 3.5f * p))
        }

        // 4. Plate of freshly baked chocolate chip cookies
        val plateX = tableX + tableW * 0.63f
        val plateY = tableY - 3 * p
        scope.drawRect(Color(0xFFF8F9FA), Offset(plateX, plateY), Size(7 * p, 2 * p))
        scope.drawRect(Color(0xFFD4A373), Offset(plateX + 0.5f * p, plateY - 1.6f * p), Size(2.8f * p, 2 * p))
        scope.drawRect(Color(0xFF58311B), Offset(plateX + 1.2f * p, plateY - 1.2f * p), Size(0.7f * p, 0.7f * p))
        scope.drawRect(Color(0xFFD4A373), Offset(plateX + 3.8f * p, plateY - 1.6f * p), Size(2.8f * p, 2 * p))
        scope.drawRect(Color(0xFF58311B), Offset(plateX + 4.6f * p, plateY - 1.2f * p), Size(0.7f * p, 0.7f * p))

        // 5. Hot Ceramic Mugs with Red Heart & Gentle Steam
        val mugX = tableX + tableW - 5.5f * p
        val mugY = tableY - 4.5f * p
        scope.drawRect(roomTheme.mugAccent, Offset(mugX, mugY), Size(3.5f * p, 4.5f * p))
        scope.drawRect(roomTheme.mugAccent, Offset(mugX + 3.5f * p, mugY + 0.6f * p), Size(1.2f * p, 2.5f * p))
        scope.drawRect(roomTheme.mug, Offset(mugX + p, mugY + 1.2f * p), Size(1.5f * p, 1.5f * p))

        // Gentle steam rising
        val steamSway = sin(timeSeconds * 3f) * 1.2f * p
        scope.drawRect(Color(0x40FFFFFF), Offset(mugX + 1.2f * p + steamSway, mugY - 2.8f * p), Size(1.2f * p, 2f * p))
        scope.drawRect(Color(0x25FFFFFF), Offset(mugX + 0.8f * p - steamSway * 0.8f, mugY - 5.2f * p), Size(1.6f * p, 2f * p))
    }

    private fun drawFootstool(scope: DrawScope, cw: Float, floorY: Float, p: Float) {
        val stoolW = 12 * p
        val stoolH = 9 * p
        val stoolX = cw * 0.67f
        val stoolY = floorY + 8 * p

        scope.drawRect(DarkWoodBeam, Offset(stoolX + p, stoolY + 3.5f * p), Size(2f * p, 5.5f * p))
        scope.drawRect(DarkWoodBeam, Offset(stoolX + stoolW - 3f * p, stoolY + 3.5f * p), Size(2f * p, 5.5f * p))

        // Checkered upholstered cushion
        scope.drawRect(BlanketRed, Offset(stoolX, stoolY), Size(stoolW, 4.2f * p))
        scope.drawRect(BlanketCream, Offset(stoolX + 2.5f * p, stoolY), Size(2.2f * p, 4.2f * p))
        scope.drawRect(BlanketCream, Offset(stoolX + 7.5f * p, stoolY), Size(2.2f * p, 4.2f * p))
    }

    private fun drawLoftFloorDecorations(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        loftFloorH: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean
    ) {
        // --- COHESIVE FLOOR READING NOOK (Grouped on the left side) ---
        val nookBaseY = floorY + loftFloorH * 0.48f

        // 1. Velvet Round Floor Meditation Cushion (cw * 0.22f)
        val cushX = cw * 0.22f
        val cushY = nookBaseY
        val cushW = 18 * p
        val cushH = 9 * p
        scope.drawRect(Color(0xFFBC6C25), Offset(cushX - cushW / 2f, cushY + 2 * p), Size(cushW, cushH - 2 * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(cushX - cushW / 2f + p, cushY), Size(cushW - 2 * p, cushH - 2 * p))
        scope.drawRect(Color(0xFFF4A261), Offset(cushX - cushW / 2f + 2 * p, cushY + p), Size(cushW - 4 * p, 2.5f * p))
        scope.drawRect(Color(0xFF9A5210), Offset(cushX - p, cushY + cushH * 0.4f), Size(2 * p, 2 * p))

        // 2. Stack of Vintage Hardcover Books & Leaning Vinyl propped right beside cushion (cw * 0.31f)
        val stackX = cw * 0.31f
        val stackY = nookBaseY - p
        // Book 1 (bottom): Burgundy
        scope.drawRect(Color(0xFF6B1D2F), Offset(stackX - 7 * p, stackY + 4 * p), Size(14 * p, 4 * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(stackX + 5.5f * p, stackY + 4.5f * p), Size(1.5f * p, 3 * p))
        // Book 2 (middle): Deep Forest Green
        scope.drawRect(Color(0xFF1E3F30), Offset(stackX - 6 * p, stackY + 0.8f * p), Size(12 * p, 3.5f * p))
        scope.drawRect(Color(0xFFE9C46A), Offset(stackX + 4.5f * p, stackY + 1.2f * p), Size(1.5f * p, 2.5f * p))
        // Book 3 (top): Dark Navy
        scope.drawRect(Color(0xFF1D3557), Offset(stackX - 5 * p, stackY - 2.5f * p), Size(10 * p, 3.5f * p))
        scope.drawRect(Color(0xFFFAF0CA), Offset(stackX + 3.5f * p, stackY - 2f * p), Size(1.5f * p, 2.5f * p))
        // Leaning vinyl album propped gently between cushion and books
        scope.drawRect(Color(0xFF264653), Offset(stackX - 9.5f * p, stackY - p), Size(3.5f * p, 8.5f * p))
        scope.drawRect(Color(0xFFE76F51), Offset(stackX - 9f * p, stackY), Size(2.5f * p, 3.5f * p))

        // 3. Brass Floor Candle Lantern placed right beside the reading stack (cw * 0.38f)
        val lantX = cw * 0.38f
        val lantY = nookBaseY + 2 * p
        val lantW = 8 * p
        val lantH = 13 * p
        scope.drawRect(Color(0xFFB08968), Offset(lantX - lantW / 2f, lantY), Size(lantW, lantH))
        scope.drawRect(Color(0xFF58311B), Offset(lantX - lantW / 2f + p, lantY + p), Size(lantW - 2 * p, lantH - 2 * p))
        // Glowing candle inside illuminating the books
        val candleLight = if (lampLit) Color(0xFFFFD166) else Color(0x99FFEAA7)
        scope.drawRect(candleLight, Offset(lantX - 1.8f * p, lantY + 3.5f * p), Size(3.6f * p, 6 * p))
        scope.drawRect(Color.White, Offset(lantX - 0.9f * p, lantY + 4f * p), Size(1.8f * p, 2.5f * p))
        if (lampLit) {
            val flick = sin(timeSeconds * 5f) * 0.08f + 0.92f
            scope.drawCircle(AmberLightMid.copy(alpha = 0.18f * flick), 12 * p, Offset(lantX, lantY + 6 * p))
        }

        // --- STAIRCASE CORNER BLANKET STORAGE (Tucked on right side) ---
        // 4. Woven floor basket with rolled fleece blankets (cw * 0.64f, beside stair post)
        val bskX = cw * 0.64f
        val bskY = nookBaseY
        val bskW = 16 * p
        val bskH = 12 * p
        val bLeft = bskX - bskW / 2f
        scope.drawRect(Color(0xFFB08968), Offset(bLeft, bskY), Size(bskW, bskH))
        scope.drawRect(Color(0xFFD4A373), Offset(bLeft + 1.5f * p, bskY + 1.5f * p), Size(bskW - 3 * p, bskH - 3 * p))
        // Rolled throws inside
        scope.drawRect(Color(0xFFE07A5F), Offset(bLeft + 2 * p, bskY - 3 * p), Size(5 * p, 6 * p))
        scope.drawRect(Color(0xFF81B29A), Offset(bLeft + 6 * p, bskY - 4 * p), Size(5 * p, 7 * p))
        scope.drawRect(Color(0xFFF4F1DE), Offset(bLeft + 10 * p, bskY - 2.5f * p), Size(4.5f * p, 5.5f * p))
    }

    private fun drawRecordPlayerStation(
        scope: DrawScope,
        cw: Float,
        floorY: Float,
        p: Float,
        timeSeconds: Float,
        recordSpinning: Boolean,
        lampLit: Boolean
    ) {
        val standW = 14 * p
        val standH = 18 * p
        val standX = cw * 0.81f
        val standY = floorY - standH + 4 * p

        // Tiered wooden nightstand / audio rack
        scope.drawRect(Color(0xFF45240F), Offset(standX, standY), Size(standW, standH))
        scope.drawRect(Color(0xFF6B3A19), Offset(standX - p, standY), Size(standW + 2 * p, 2 * p))
        scope.drawRect(DarkWoodBeam, Offset(standX, standY + 8 * p), Size(standW, 1.5f * p))

        // Lower shelf: stacked vinyl records & albums
        var rx = standX + 1.5f * p
        val albumColors = LOFT_ALBUM_COLORS
        for (i in 0..4) {
            scope.drawRect(albumColors[i], Offset(rx, standY + 10 * p), Size(1.8f * p, 7 * p))
            rx += 2.4f * p
        }

        // Vintage Turntable / Record Player on top
        val playerX = standX + p
        val playerY = standY - 8 * p
        scope.drawRect(Color(0xFFBC6C25), Offset(playerX, playerY), Size(12 * p, 8 * p))
        scope.drawRect(Color(0xFF7F5539), Offset(playerX + 0.8f * p, playerY + 0.8f * p), Size(10.4f * p, 6.4f * p))

        // Vinyl Disc (Spinning if active!)
        val discX = playerX + 4.5f * p
        val discY = playerY + 4 * p
        val discR = 3.2f * p
        scope.drawRect(Color(0xFF1A1A1A), Offset(discX - discR, discY - discR), Size(discR * 2, discR * 2))
        scope.drawRect(Color(0xFFFF4D6D), Offset(discX - p, discY - p), Size(2 * p, 2 * p))
        scope.drawRect(Color(0xFFFFD166), Offset(discX - 0.4f * p, discY - 0.4f * p), Size(0.8f * p, 0.8f * p))

        // Spinning sheen highlight
        if (recordSpinning) {
            val rot = timeSeconds * 12f
            val hx = cos(rot) * 2 * p
            val hy = sin(rot) * 2 * p
            scope.drawRect(Color(0x70FFFFFF), Offset(discX + hx, discY + hy), Size(p, p))
        }

        // Tonearm
        scope.drawRect(Color(0xFFC0C0C0), Offset(playerX + 9 * p, playerY + 2 * p), Size(0.8f * p, 4 * p))
        scope.drawRect(Color(0xFFD4A373), Offset(playerX + 8 * p, playerY + 5 * p), Size(2 * p, 0.8f * p))

        // Status indicator LED
        val ledColor = if (recordSpinning) Color(0xFF52B788) else Color(0xFFFF4D6D)
        scope.drawRect(ledColor, Offset(playerX + 10.5f * p, playerY + 1.2f * p), Size(p, p))

        // Framed night art piece on the right wall
        scope.drawRect(Color(0xFFDDA15E), Offset(standX + standW + p, standY - 11 * p), Size(7 * p, 9 * p))
        scope.drawRect(NightSkyMid, Offset(standX + standW + 1.6f * p, standY - 10.4f * p), Size(5.8f * p, 7.8f * p))
        scope.drawRect(MoonBody, Offset(standX + standW + 4.5f * p, standY - 9 * p), Size(2 * p, 2 * p))

        // Potted houseplant on the floor beside the audio rack
        val plantX = standX + standW + 1.5f * p
        val plantY = floorY
        scope.drawRect(Color(0xFFB07D62), Offset(plantX, plantY - 5.5f * p), Size(5.5f * p, 5.5f * p))
        scope.drawRect(Color(0xFF2D6A4F), Offset(plantX - 1.5f * p, plantY - 10 * p), Size(8.5f * p, 5 * p))
        scope.drawRect(Color(0xFF52B788), Offset(plantX, plantY - 13 * p), Size(5.5f * p, 3.5f * p))
    }

    private fun drawBalconyRailing(
        scope: DrawScope,
        cw: Float,
        ch: Float,
        p: Float,
        timeSeconds: Float,
        lampLit: Boolean,
        roomTheme: RoomTheme
    ) {
        val railTopY = ch * 0.72f
        val railBottomY = floorEdgeY(ch)

        // Balcony heavy wooden top rail
        scope.drawRect(DarkWoodBeam, Offset(0f, railTopY), Size(cw, 4.5f * p))
        scope.drawRect(PlankWoodLight, Offset(0f, railTopY), Size(cw, 1.2f * p))

        // Balcony vertical timber posts
        val postW = 4 * p
        val postGap = cw / 4f
        for (i in 0..4) {
            val px = i * postGap - p
            scope.drawRect(DarkWoodBeam, Offset(px, railTopY + 4.5f * p), Size(postW, railBottomY - (railTopY + 4.5f * p)))
            scope.drawRect(PlankWoodLight, Offset(px + p, railTopY + 4.5f * p), Size(0.8f * p, railBottomY - (railTopY + 4.5f * p)))
        }

        // Bottom rail and the floor's front edge beam, left of the stair opening.
        val stairX = cw * 0.65f
        scope.drawRect(DarkWoodBeam, Offset(0f, railBottomY - 2.5f * p), Size(stairX, 2.5f * p))
        scope.drawRect(DarkWoodBeam, Offset(0f, railBottomY), Size(stairX, 4f * p))
        scope.drawRect(PlankWoodLight, Offset(0f, railBottomY), Size(stairX, 1.2f * p))
        // The stairs down on the right, in front of the railing.
        drawLoftStairs(scope, cw, ch, p, railTopY, ch)

        // Hanging string of cozy fairy lights along the balcony railing
        for (bxRel in LOFT_BULB_POSITIONS) {
            val bx = cw * bxRel
            val by = railTopY + 8 * p + sin(bxRel * 10f) * 2 * p

            // Little wire drop
            scope.drawRect(Color(0xFF1D0E07), Offset(bx + p, railTopY + 4.5f * p), Size(0.8f * p, by - (railTopY + 4.5f * p)))

            // Glowing mini Edison bulb
            scope.drawRect(DarkWoodBeam, Offset(bx, by), Size(3 * p, 4 * p))
            val bulbGlow = if (lampLit) roomTheme.light else roomTheme.light.copy(alpha = 0.55f)
            when (roomTheme.ordinal) {
                1 -> {
                    scope.drawRect(bulbGlow, Offset(bx + p, by + p), Size(p, p))
                    scope.drawRect(bulbGlow, Offset(bx + 0.4f*p, by + 2*p), Size(2.2f*p, p))
                    scope.drawRect(bulbGlow, Offset(bx + p, by + 3*p), Size(p, p))
                }
                2 -> {
                    scope.drawRect(bulbGlow, Offset(bx + 0.7f*p, by + p), Size(0.8f*p, p))
                    scope.drawRect(bulbGlow, Offset(bx + 2*p, by + p), Size(0.8f*p, p))
                    scope.drawRect(bulbGlow, Offset(bx + 0.4f*p, by + 2*p), Size(2.7f*p, p))
                    scope.drawRect(bulbGlow, Offset(bx + p, by + 3*p), Size(1.5f*p, p))
                }
                3 -> {
                    scope.drawRect(bulbGlow, Offset(bx + p, by + 0.5f*p), Size(p, 3*p))
                    scope.drawRect(bulbGlow, Offset(bx + 0.3f*p, by + 1.5f*p), Size(3*p, p))
                    scope.drawRect(bulbGlow, Offset(bx + 0.8f*p, by + p), Size(2*p, 2*p))
                }
                else -> scope.drawRect(bulbGlow, Offset(bx + 0.8f * p, by + 0.8f * p), Size(1.4f * p, 2.4f * p))
            }
            if (lampLit) {
                val flicker = sin(timeSeconds * 4f + bxRel * 8f) * 0.06f + 0.94f
                scope.drawCircle(roomTheme.light.copy(alpha = 0.20f * flicker), 8 * p, Offset(bx + 1.5f * p, by + 2 * p))
            }
        }

        // Cascading green ivy vines draping over the railing
        for (vxRel in LOFT_VINE_POSITIONS) {
            val vx = cw * vxRel
            val vineLen = (14 + (vxRel * 20).toInt() % 12) * p
            var vy = railTopY + 2.5f * p
            while (vy < railTopY + vineLen) {
                val sway = sin(timeSeconds * 2.2f + vxRel * 5f) * 0.8f * p
                scope.drawRect(Color(0xFF1B4332), Offset(vx + sway, vy), Size(1.6f * p, 3.2f * p))
                scope.drawRect(Color(0xFF2D6A4F), Offset(vx - 1.5f * p + sway, vy + 0.8f * p), Size(2.8f * p, 2.4f * p))
                scope.drawRect(Color(0xFF52B788), Offset(vx + 0.8f * p + sway, vy + 1.6f * p), Size(2 * p, 2 * p))
                vy += 4 * p
            }
        }

        // Planter pots along the railing & staircase
        val potX1 = cw * 0.06f
        scope.drawRect(Color(0xFFB07D62), Offset(potX1, railTopY - 5.5f * p), Size(5.5f * p, 5.5f * p))
        scope.drawRect(Color(0xFF2D6A4F), Offset(potX1 - 1.5f * p, railTopY - 11 * p), Size(8.5f * p, 5.5f * p))
        scope.drawRect(Color(0xFF52B788), Offset(potX1, railTopY - 13.5f * p), Size(5.5f * p, 3 * p))

        val potX2 = cw * 0.62f
        scope.drawRect(Color(0xFFB07D62), Offset(potX2, railTopY - 5.5f * p), Size(5.5f * p, 5.5f * p))
        scope.drawRect(Color(0xFF2D6A4F), Offset(potX2 - 1.5f * p, railTopY - 10 * p), Size(8.5f * p, 5 * p))
        scope.drawRect(Color(0xFF52B788), Offset(potX2, railTopY - 12.5f * p), Size(5.5f * p, 3 * p))
    }
}
