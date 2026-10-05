package com.example.engine

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The offscreen image the low-res world is drawn into: one image pixel per game pixel.
 *
 * [canvas] is a pixel-art canvas: anti-aliasing and bitmap filtering are off on every paint,
 * filled rectangles snap to whole pixels (never thinner than one), tiny ovals and circles become a
 * single pixel block, and strokes thinner than a pixel stay one pixel wide. Each platform builds it
 * on its own graphics library (Android canvas, Skia on iOS) so both produce the same pixels.
 */
expect class PixelSurface(width: Int, height: Int) {
    val width: Int
    val height: Int

    /** The surface as a Compose image, for drawing it to the screen. */
    val image: ImageBitmap

    /** Draws into the surface, with the pixel-art rules above. */
    val canvas: Canvas

    /** Copies every pixel (ARGB, row by row) into [dst], which holds at least width * height. */
    fun readPixels(dst: IntArray)

    /** Writes [rows] full rows from [src] (starting at [srcOffset]) into the surface at [startRow]. */
    fun writePixels(src: IntArray, srcOffset: Int, startRow: Int, rows: Int)

    /** Call after drawing and pixel writes, before showing [image]. */
    fun commit()
}
