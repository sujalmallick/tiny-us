package com.example.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.engine.StageExtension
import com.example.engine.WorldCamera
import kotlin.math.ceil

/**
 * The low-res world renderer (Plan 03, Phase 1).
 *
 * The world is drawn into a small offscreen image, one image pixel per game pixel, with
 * anti-aliasing switched off (see [HardEdgeCanvas]), then enlarged by a whole number with no
 * smoothing. Every shape, character and particle therefore lands on the same pixel grid.
 *
 * Drawing code is unchanged: it still sees the full screen size and screen coordinates, and the
 * canvas shrinks them. Taps and Compose overlays keep working in screen coordinates.
 */
internal class LowResWorldBuffer {
    private var bitmap: ImageBitmap? = null
    private var pixelsBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val drawScope = CanvasDrawScope()
    private var pixels = IntArray(0)

    /** The last frame at game resolution (for tests). */
    val frame: ImageBitmap? get() = bitmap

    private fun ensure(gw: Int, gh: Int): Canvas {
        val cnv = canvas
        if (cnv != null && bitmap?.width == gw && bitmap?.height == gh) return cnv
        val raw = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888)
        pixelsBitmap = raw
        bitmap = raw.asImageBitmap()
        return Canvas(HardEdgeCanvas(raw)).also { canvas = it }
    }

    /**
     * Draws [block] (which works in world units, see [WorldCamera]) onto the camera's stage, continues
     * the background above and below it, and enlarges the frame to the screen. [beyondStage], if
     * given, is drawn again over the continued areas, shifted by one stage height, so falling
     * weather covers the whole screen.
     */
    fun drawStaged(
        target: DrawScope,
        camera: WorldCamera,
        starrySky: Boolean = false,
        beyondStage: (DrawScope.() -> Unit)? = null,
        block: DrawScope.() -> Unit
    ) {
        val cnv = ensure(camera.gameW, camera.gameH)
        val worldPerGame = WorldCamera.WORLD_PIXEL.toFloat()
        drawScope.draw(target, target.layoutDirection, cnv, androidx.compose.ui.geometry.Size(camera.worldW, camera.worldH)) {
            drawRect(Color.Transparent, Offset.Zero, androidx.compose.ui.geometry.Size(camera.gameW.toFloat(), camera.gameH.toFloat()), blendMode = BlendMode.Clear)
            translate(camera.stageX.toFloat(), camera.stageY.toFloat()) {
                clipRect(0f, 0f, camera.stageW.toFloat(), camera.stageH.toFloat()) {
                    scale(1f / worldPerGame, 1f / worldPerGame, pivot = Offset.Zero) { block() }
                }
            }
        }
        if (camera.stageH < camera.gameH) {
            extendBackground(camera, starrySky)
            if (beyondStage != null) drawBeyondStage(target, cnv, camera, beyondStage)
        }
        val bmp = bitmap!!
        target.drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(camera.gameW, camera.gameH),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(camera.gameW * camera.zoom, camera.gameH * camera.zoom),
            filterQuality = FilterQuality.None
        )
    }

    private fun drawBeyondStage(target: DrawScope, cnv: Canvas, camera: WorldCamera, layer: DrawScope.() -> Unit) {
        val worldPerGame = WorldCamera.WORLD_PIXEL.toFloat()
        val top = camera.stageY.toFloat()
        val bottom = (camera.stageY + camera.stageH).toFloat()
        drawScope.draw(target, target.layoutDirection, cnv, androidx.compose.ui.geometry.Size(camera.worldW, camera.worldH)) {
            // Above the stage: the stage's lower part, moved up one stage height.
            clipRect(0f, 0f, camera.gameW.toFloat(), top) {
                translate(camera.stageX.toFloat(), top - camera.stageH) {
                    scale(1f / worldPerGame, 1f / worldPerGame, pivot = Offset.Zero) { layer() }
                }
            }
            // Below the stage: its upper part, moved down one stage height.
            clipRect(0f, bottom, camera.gameW.toFloat(), camera.gameH.toFloat()) {
                translate(camera.stageX.toFloat(), bottom) {
                    scale(1f / worldPerGame, 1f / worldPerGame, pivot = Offset.Zero) { layer() }
                }
            }
        }
    }

    /** Fills the rows above and below the stage by continuing the stage's own top and bottom edges. */
    private fun extendBackground(camera: WorldCamera, starrySky: Boolean) {
        val raw = pixelsBitmap ?: return
        val w = camera.gameW
        val h = camera.gameH
        if (pixels.size != w * h) pixels = IntArray(w * h)
        raw.getPixels(pixels, 0, w, 0, 0, w, h)
        StageExtension.fill(pixels, w, h, camera.stageY, camera.stageY + camera.stageH, starrySky)
        raw.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    fun draw(target: DrawScope, scale: Int, block: DrawScope.() -> Unit) {
        val screen = target.size
        val gw = ceil(screen.width / scale).toInt().coerceAtLeast(1)
        val gh = ceil(screen.height / scale).toInt().coerceAtLeast(1)
        val cnv = ensure(gw, gh)
        val bmp = bitmap!!
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

/**
 * A canvas for pixel art:
 * - anti-aliasing and bitmap filtering are switched off on every paint, so each shape covers
 *   whole pixels or none (a DrawFilter would do the same, but not every renderer honours one);
 * - filled rectangles snap to whole pixels and are never thinner than one, so sprite details
 *   drawn finer than a game pixel (eyes, mouths, trims) still show instead of dropping out;
 * - tiny ovals and circles become a single pixel block, and hairline strokes stay one pixel wide.
 */
private class HardEdgeCanvas(bitmap: Bitmap) : android.graphics.Canvas(bitmap) {
    private val matrixNow = Matrix()
    private val m = FloatArray(9)

    private fun Paint.hard(): Paint {
        if (isAntiAlias) isAntiAlias = false
        if (isFilterBitmap) isFilterBitmap = false
        return this
    }

    /** Scale and offset of the current transform, or false when it rotates or skews. */
    @Suppress("DEPRECATION")
    private fun axisAligned(): Boolean {
        getMatrix(matrixNow)
        matrixNow.getValues(m)
        return m[Matrix.MSKEW_X] == 0f && m[Matrix.MSKEW_Y] == 0f && m[Matrix.MSCALE_X] > 0f && m[Matrix.MSCALE_Y] > 0f
    }

    /**
     * Draws [left]..[bottom] as whole device pixels, at least one each way. Returns false when the
     * transform isn't a plain scale-and-move (the caller then draws normally).
     */
    private fun snappedRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint, minSize: Boolean): Boolean {
        if (paint.style != Paint.Style.FILL || !axisAligned()) return false
        val sx = m[Matrix.MSCALE_X]
        val sy = m[Matrix.MSCALE_Y]
        val tx = m[Matrix.MTRANS_X]
        val ty = m[Matrix.MTRANS_Y]
        val l = minOf(left, right) * sx + tx
        val r = maxOf(left, right) * sx + tx
        val t = minOf(top, bottom) * sy + ty
        val b = maxOf(top, bottom) * sy + ty
        if (r - l <= 0f || b - t <= 0f) return true
        val x0 = Math.round(l).toFloat()
        var x1 = Math.round(r).toFloat()
        val y0 = Math.round(t).toFloat()
        var y1 = Math.round(b).toFloat()
        if (x1 <= x0) { if (!minSize) return true; x1 = x0 + 1f }
        if (y1 <= y0) { if (!minSize) return true; y1 = y0 + 1f }
        // Back into the caller's coordinates, so clips and layers still apply.
        super.drawRect((x0 - tx) / sx, (y0 - ty) / sy, (x1 - tx) / sx, (y1 - ty) / sy, paint.hard())
        return true
    }

    /** True when an [w] x [h] shape is under 1.5 device pixels either way. */
    private fun isTiny(w: Float, h: Float): Boolean =
        axisAligned() && (kotlin.math.abs(w * m[Matrix.MSCALE_X]) < 1.5f || kotlin.math.abs(h * m[Matrix.MSCALE_Y]) < 1.5f)

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        if (!snappedRect(left, top, right, bottom, paint, minSize = true)) super.drawRect(left, top, right, bottom, paint.hard())
    }

    override fun drawRect(rect: RectF, paint: Paint) = drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        if (paint.style == Paint.Style.FILL && isTiny(right - left, bottom - top) &&
            snappedRect(left, top, right, bottom, paint, minSize = true)
        ) return
        super.drawOval(left, top, right, bottom, paint.hard())
    }

    override fun drawOval(oval: RectF, paint: Paint) = drawOval(oval.left, oval.top, oval.right, oval.bottom, paint)

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
        if (paint.style == Paint.Style.FILL && isTiny(2f * radius, 2f * radius) &&
            snappedRect(cx - radius, cy - radius, cx + radius, cy + radius, paint, minSize = true)
        ) return
        super.drawCircle(cx, cy, radius, paint.hard())
    }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, paint: Paint) {
        if (paint.style == Paint.Style.FILL && isTiny(right - left, bottom - top) &&
            snappedRect(left, top, right, bottom, paint, minSize = true)
        ) return
        super.drawRoundRect(left, top, right, bottom, rx, ry, paint.hard())
    }

    override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) =
        drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint)

    override fun drawArc(
        left: Float, top: Float, right: Float, bottom: Float,
        startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint
    ) = super.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint.hard())

    override fun drawArc(oval: RectF, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) =
        super.drawArc(oval, startAngle, sweepAngle, useCenter, paint.hard())

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        // A stroke thinner than a pixel would vanish; a hairline (width 0) is exactly one pixel.
        val width = paint.strokeWidth
        if (width > 0f && axisAligned() && width * m[Matrix.MSCALE_X] < 1f) {
            paint.strokeWidth = 0f
            super.drawLine(startX, startY, stopX, stopY, paint.hard())
            paint.strokeWidth = width
        } else {
            super.drawLine(startX, startY, stopX, stopY, paint.hard())
        }
    }

    override fun drawLines(pts: FloatArray, offset: Int, count: Int, paint: Paint) =
        super.drawLines(pts, offset, count, paint.hard())

    override fun drawLines(pts: FloatArray, paint: Paint) = super.drawLines(pts, paint.hard())

    override fun drawPoint(x: Float, y: Float, paint: Paint) = super.drawPoint(x, y, paint.hard())

    override fun drawPoints(pts: FloatArray, offset: Int, count: Int, paint: Paint) =
        super.drawPoints(pts, offset, count, paint.hard())

    override fun drawPoints(pts: FloatArray, paint: Paint) = super.drawPoints(pts, paint.hard())

    override fun drawPath(path: Path, paint: Paint) = super.drawPath(path, paint.hard())

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) =
        super.drawBitmap(bitmap, left, top, paint?.hard())

    override fun drawBitmap(bitmap: Bitmap, src: android.graphics.Rect?, dst: RectF, paint: Paint?) =
        super.drawBitmap(bitmap, src, dst, paint?.hard())

    override fun drawBitmap(bitmap: Bitmap, src: android.graphics.Rect?, dst: android.graphics.Rect, paint: Paint?) =
        super.drawBitmap(bitmap, src, dst, paint?.hard())
}
