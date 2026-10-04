package com.example.engine

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The single source of the world's pixel size (Plan 03, Phase 0).
 *
 * The world is about 115 "scene pixels" across. On phones one scene pixel is 5 screen pixels, which
 * is also the whole-number factor the low-res renderer enlarges by.
 */
object WorldViewport {
    private const val SCENE_PIXELS_ACROSS = 115f
    private const val MIN_PIXEL = 3f
    private const val MAX_PIXEL = 5f

    /** Characters are drawn 1.38x larger than scenery in the classic renderer. */
    private const val CLASSIC_CHARACTER_SCALE = 1.38f

    /**
     * Game pixels per character-sprite pixel in the low-res renderer. The sprites carry half-pixel
     * details (they were designed for the classic 1.38x size), so 2 keeps every detail on the grid
     * and makes the couple about 11% of a phone's height.
     */
    const val LOW_RES_CHARACTER_BLOCKS = 2

    /** Screen pixels per scene pixel for a world [widthPx] wide. */
    fun pixelScale(widthPx: Float): Float = (widthPx / SCENE_PIXELS_ACROSS).coerceIn(MIN_PIXEL, MAX_PIXEL)

    /**
     * The `pixelSize` to pass to [PixelArtRenderer.drawCharacter]. With [lowRes] a character pixel
     * is exactly [LOW_RES_CHARACTER_BLOCKS] game pixels, so it stays on the grid.
     */
    fun characterPixelScale(widthPx: Float, lowRes: Boolean = false): Float {
        val p = pixelScale(widthPx)
        return if (lowRes) {
            gameScale(widthPx) * LOW_RES_CHARACTER_BLOCKS / PixelArtRenderer.CHARACTER_SCALE_FACTOR
        } else {
            p * CLASSIC_CHARACTER_SCALE
        }
    }

    /** Whole-number enlargement from game pixels to screen pixels. */
    fun gameScale(widthPx: Float): Int = pixelScale(widthPx).roundToInt().coerceAtLeast(1)

    /** Game-resolution size of a world [widthPx] by [heightPx]; partial edge pixels round up. */
    fun gameWidth(widthPx: Float): Int = ceil(widthPx / gameScale(widthPx)).toInt()

    fun gameHeight(widthPx: Float, heightPx: Float): Int = ceil(heightPx / gameScale(widthPx)).toInt()

    /** Screen coordinate to game coordinate, and back. */
    fun toGame(screenPx: Float, widthPx: Float): Float = screenPx / gameScale(widthPx)

    fun toScreen(gamePx: Float, widthPx: Float): Float = gamePx * gameScale(widthPx)
}
