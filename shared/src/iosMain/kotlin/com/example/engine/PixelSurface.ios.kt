package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.floor
import org.jetbrains.skia.Bitmap as SkiaBitmap
import org.jetbrains.skia.BlendMode as SkiaBlendMode
import org.jetbrains.skia.Canvas as SkiaCanvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint as SkiaPaint
import org.jetbrains.skia.Rect as SkiaRect
import org.jetbrains.skia.SamplingMode

actual class PixelSurface actual constructor(actual val width: Int, actual val height: Int) {
    private val bitmap = SkiaBitmap().apply {
        allocN32Pixels(width, height, false)
        erase(0)
    }
    private val skia = SkiaCanvas(bitmap)

    /** Pixels are exchanged as little-endian ARGB ints, which is BGRA byte order. */
    private val rowInfo = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)

    actual val image: ImageBitmap = bitmap.asComposeImageBitmap()

    actual val canvas: Canvas = HardEdgeCanvas(skia.asComposeCanvas(), skia)

    actual fun readPixels(dst: IntArray) {
        val bytes = bitmap.readPixels(rowInfo, width * 4, 0, 0) ?: return
        val count = minOf(width * height, dst.size)
        for (i in 0 until count) {
            val b = i * 4
            dst[i] = (bytes[b].toInt() and 0xFF) or
                ((bytes[b + 1].toInt() and 0xFF) shl 8) or
                ((bytes[b + 2].toInt() and 0xFF) shl 16) or
                ((bytes[b + 3].toInt() and 0xFF) shl 24)
        }
    }

    actual fun writePixels(src: IntArray, srcOffset: Int, startRow: Int, rows: Int) {
        if (rows <= 0) return
        val count = width * rows
        val bytes = ByteArray(count * 4)
        for (i in 0 until count) {
            val c = src[srcOffset + i]
            val b = i * 4
            bytes[b] = c.toByte()
            bytes[b + 1] = (c shr 8).toByte()
            bytes[b + 2] = (c shr 16).toByte()
            bytes[b + 3] = (c ushr 24).toByte()
        }
        val rowsImage = SkiaImage.makeRaster(ImageInfo(width, rows, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL), bytes, width * 4)
        // Replace the rows exactly (no blending, no smoothing, no transform).
        val copy = SkiaPaint().apply { blendMode = SkiaBlendMode.SRC }
        skia.save()
        skia.resetMatrix()
        skia.drawImageRect(
            rowsImage,
            SkiaRect.makeWH(width.toFloat(), rows.toFloat()),
            SkiaRect.makeXYWH(0f, startRow.toFloat(), width.toFloat(), rows.toFloat()),
            SamplingMode.DEFAULT,
            copy,
            true
        )
        skia.restore()
        copy.close()
        rowsImage.close()
    }

    /** Skia may cache an image of the bitmap; tell it the pixels changed this frame. */
    actual fun commit() = bitmap.notifyPixelsChanged()
}

/**
 * The iOS twin of Android's pixel-art canvas (see [PixelSurface]): the same rules, applied to
 * Compose drawing calls on top of Skia, so a frame lands on the same pixels on both platforms.
 */
private class HardEdgeCanvas(private val inner: Canvas, private val skia: SkiaCanvas) : Canvas by inner {
    private var sx = 1f
    private var sy = 1f
    private var tx = 0f
    private var ty = 0f

    private fun Paint.hard(): Paint {
        if (isAntiAlias) isAntiAlias = false
        if (filterQuality != FilterQuality.None) filterQuality = FilterQuality.None
        return this
    }

    /** Reads the current transform; false when it rotates or skews. */
    private fun axisAligned(): Boolean {
        val m = skia.localToDevice.mat
        sx = m[0]
        sy = m[5]
        tx = m[3]
        ty = m[7]
        return m[1] == 0f && m[4] == 0f && sx > 0f && sy > 0f
    }

    /** Half-up rounding, like Android's Math.round (Kotlin's round() rounds halves to even). */
    private fun roundHalfUp(v: Float): Float = floor(v + 0.5f)

    private fun snappedRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint): Boolean {
        if (paint.style != PaintingStyle.Fill || !axisAligned()) return false
        val l = minOf(left, right) * sx + tx
        val r = maxOf(left, right) * sx + tx
        val t = minOf(top, bottom) * sy + ty
        val b = maxOf(top, bottom) * sy + ty
        if (r - l <= 0f || b - t <= 0f) return true
        val x0 = roundHalfUp(l)
        var x1 = roundHalfUp(r)
        val y0 = roundHalfUp(t)
        var y1 = roundHalfUp(b)
        if (x1 <= x0) x1 = x0 + 1f
        if (y1 <= y0) y1 = y0 + 1f
        // Back into the caller's coordinates, so clips and layers still apply.
        inner.drawRect((x0 - tx) / sx, (y0 - ty) / sy, (x1 - tx) / sx, (y1 - ty) / sy, paint.hard())
        return true
    }

    /** True when a [w] x [h] shape is under 1.5 device pixels either way. */
    private fun isTiny(w: Float, h: Float): Boolean = axisAligned() && (abs(w * sx) < 1.5f || abs(h * sy) < 1.5f)

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        if (!snappedRect(left, top, right, bottom, paint)) inner.drawRect(left, top, right, bottom, paint.hard())
    }

    override fun drawRect(rect: Rect, paint: Paint) = drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        if (paint.style == PaintingStyle.Fill && isTiny(right - left, bottom - top) && snappedRect(left, top, right, bottom, paint)) return
        inner.drawOval(left, top, right, bottom, paint.hard())
    }

    override fun drawOval(rect: Rect, paint: Paint) = drawOval(rect.left, rect.top, rect.right, rect.bottom, paint)

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        if (paint.style == PaintingStyle.Fill && isTiny(2f * radius, 2f * radius) &&
            snappedRect(center.x - radius, center.y - radius, center.x + radius, center.y + radius, paint)
        ) return
        inner.drawCircle(center, radius, paint.hard())
    }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radiusX: Float, radiusY: Float, paint: Paint) {
        if (paint.style == PaintingStyle.Fill && isTiny(right - left, bottom - top) && snappedRect(left, top, right, bottom, paint)) return
        inner.drawRoundRect(left, top, right, bottom, radiusX, radiusY, paint.hard())
    }

    override fun drawArc(
        left: Float, top: Float, right: Float, bottom: Float,
        startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint
    ) = inner.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint.hard())

    override fun drawArc(rect: Rect, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) =
        drawArc(rect.left, rect.top, rect.right, rect.bottom, startAngle, sweepAngle, useCenter, paint)

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        // A stroke thinner than a pixel would vanish; a hairline (width 0) is exactly one pixel.
        val stroke = paint.strokeWidth
        if (stroke > 0f && axisAligned() && stroke * sx < 1f) {
            paint.strokeWidth = 0f
            inner.drawLine(p1, p2, paint.hard())
            paint.strokeWidth = stroke
        } else {
            inner.drawLine(p1, p2, paint.hard())
        }
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) =
        inner.drawPoints(pointMode, points, paint.hard())

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) =
        inner.drawRawPoints(pointMode, points, paint.hard())

    override fun drawPath(path: Path, paint: Paint) = inner.drawPath(path, paint.hard())

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) =
        inner.drawImage(image, topLeftOffset, paint.hard())

    override fun drawImageRect(image: ImageBitmap, srcOffset: IntOffset, srcSize: IntSize, dstOffset: IntOffset, dstSize: IntSize, paint: Paint) =
        inner.drawImageRect(image, srcOffset, srcSize, dstOffset, dstSize, paint.hard())
}
