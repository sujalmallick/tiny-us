package com.example.ui

import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

/**
 * The low-res world renderer (Plan 03, Phase 1).
 *
 * The world is drawn into a small offscreen image, one image pixel per game pixel, with
 * anti-aliasing switched off, then enlarged by a whole number with no smoothing. Every shape,
 * character and particle therefore lands on the same pixel grid.
 *
 * Drawing code is unchanged: it still sees the full screen size and screen coordinates, and the
 * canvas shrinks them. Taps and Compose overlays keep working in screen coordinates.
 */
internal class LowResWorldBuffer {
    private var bitmap: ImageBitmap? = null
    private var canvas: Canvas? = null
    private val drawScope = CanvasDrawScope()

    /** Hard pixel edges: no anti-aliasing and no bitmap filtering inside the world. */
    private val noSmoothing = PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG, 0)

    /** The last frame at game resolution (for tests). */
    val frame: ImageBitmap? get() = bitmap

    fun draw(target: DrawScope, scale: Int, block: DrawScope.() -> Unit) {
        val screen = target.size
        val gw = ceil(screen.width / scale).toInt().coerceAtLeast(1)
        val gh = ceil(screen.height / scale).toInt().coerceAtLeast(1)
        var bmp = bitmap
        var cnv = canvas
        if (bmp == null || cnv == null || bmp.width != gw || bmp.height != gh) {
            bmp = ImageBitmap(gw, gh)
            cnv = Canvas(bmp).also { it.nativeCanvas.drawFilter = noSmoothing }
            bitmap = bmp
            canvas = cnv
        }
        drawScope.draw(target, target.layoutDirection, cnv, screen) {
            drawRect(Color.Transparent, Offset.Zero, size, blendMode = BlendMode.Clear)
            scale(1f / scale, 1f / scale, pivot = Offset.Zero) { block() }
        }
        target.drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(gw, gh),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(gw * scale, gh * scale),
            filterQuality = FilterQuality.None
        )
    }
}
