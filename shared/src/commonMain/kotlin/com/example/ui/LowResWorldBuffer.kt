package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.engine.PixelDissolve
import com.example.engine.PixelSurface
import com.example.engine.StageExtension
import com.example.engine.WorldCamera
import kotlin.math.ceil

/**
 * The low-res world renderer (Plan 03, Phase 1).
 *
 * The world is drawn into a small offscreen image, one image pixel per game pixel, with
 * anti-aliasing switched off (see [PixelSurface]), then enlarged by a whole number with no
 * smoothing. Every shape, character and particle therefore lands on the same pixel grid.
 *
 * Drawing code is unchanged: it still sees the full screen size and screen coordinates, and the
 * canvas shrinks them. Taps and Compose overlays keep working in screen coordinates.
 */
class LowResWorldBuffer {
    private companion object {
        /** The scene-change colour (the classic renderer fades to the same colour). */
        const val WIPE_COLOUR = 0xFF0F1423.toInt()
    }

    private var surface: PixelSurface? = null
    private val drawScope = CanvasDrawScope()
    private var pixels = IntArray(0)

    /** The last frame at game resolution (for tests). */
    val frame: ImageBitmap? get() = surface?.image

    private fun ensure(gw: Int, gh: Int): Canvas {
        val current = surface
        if (current != null && current.width == gw && current.height == gh) return current.canvas
        return PixelSurface(gw, gh).also { surface = it }.canvas
    }

    /**
     * Draws [block] (which works in world units, see [WorldCamera]) onto the camera's stage, continues
     * the background above and below it, and enlarges the frame to the screen. [beyondStage], if
     * given, is drawn again over the continued areas, shifted by one stage height, so falling
     * weather covers the whole screen. [aboveStage], if given, draws into the continued area above
     * the stage in world units (y runs from minus its height up to 0), given that height.
     */
    fun drawStaged(
        target: DrawScope,
        camera: WorldCamera,
        starrySky: Boolean = false,
        beyondStage: (DrawScope.() -> Unit)? = null,
        aboveStage: (DrawScope.(Float) -> Unit)? = null,
        belowStage: (DrawScope.(Float) -> Unit)? = null,
        dissolve: Float = 0f,
        frameKey: Long = -1L,
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
            extendBackground(camera, starrySky, frameKey)
            if (aboveStage != null && camera.stageY > 0) drawAboveStage(target, cnv, camera, aboveStage)
            if (belowStage != null && camera.stageY + camera.stageH < camera.gameH) drawBelowStage(target, cnv, camera, belowStage)
            if (beyondStage != null) drawBeyondStage(target, cnv, camera, beyondStage)
        }
        if (dissolve > 0f) dissolveFrame(camera.gameW, camera.gameH, dissolve)
        val shown = surface!!
        shown.commit()
        target.drawImage(
            image = shown.image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(camera.gameW, camera.gameH),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(camera.gameW * camera.zoom, camera.gameH * camera.zoom),
            filterQuality = FilterQuality.None
        )
    }

    private fun drawAboveStage(target: DrawScope, cnv: Canvas, camera: WorldCamera, layer: DrawScope.(Float) -> Unit) {
        val worldPerGame = WorldCamera.WORLD_PIXEL.toFloat()
        drawScope.draw(target, target.layoutDirection, cnv, androidx.compose.ui.geometry.Size(camera.worldW, camera.worldH)) {
            clipRect(0f, 0f, camera.gameW.toFloat(), camera.stageY.toFloat()) {
                translate(camera.stageX.toFloat(), camera.stageY.toFloat()) {
                    scale(1f / worldPerGame, 1f / worldPerGame, pivot = Offset.Zero) { layer(camera.stageY * worldPerGame) }
                }
            }
        }
    }

    /** Like [drawAboveStage], below the stage: y continues past the stage's height; given the area's height. */
    private fun drawBelowStage(target: DrawScope, cnv: Canvas, camera: WorldCamera, layer: DrawScope.(Float) -> Unit) {
        val worldPerGame = WorldCamera.WORLD_PIXEL.toFloat()
        val bottom = (camera.stageY + camera.stageH).toFloat()
        drawScope.draw(target, target.layoutDirection, cnv, androidx.compose.ui.geometry.Size(camera.worldW, camera.worldH)) {
            clipRect(0f, bottom, camera.gameW.toFloat(), camera.gameH.toFloat()) {
                translate(camera.stageX.toFloat(), camera.stageY.toFloat()) {
                    scale(1f / worldPerGame, 1f / worldPerGame, pivot = Offset.Zero) {
                        layer((camera.gameH - camera.stageY - camera.stageH) * worldPerGame)
                    }
                }
            }
        }
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

    /**
     * Scene-change dissolve: [amount] of the frame's 2x2-pixel cells turn to the night-blue wipe
     * colour, in an ordered (Bayer) pattern, so the scene breaks up into blocks instead of fading.
     */
    private fun dissolveFrame(w: Int, h: Int, amount: Float) {
        val raw = surface ?: return
        if (pixels.size != w * h) pixels = IntArray(w * h)
        raw.readPixels(pixels)
        PixelDissolve.apply(pixels, w, h, amount, WIPE_COLOUR)
        raw.writePixels(pixels, 0, 0, h)
    }

    /** Fills the rows above and below the stage by continuing the stage's own top and bottom edges. */
    private fun extendBackground(camera: WorldCamera, starrySky: Boolean, frameKey: Long) {
        val raw = surface ?: return
        val w = camera.gameW
        val h = camera.gameH
        val top = camera.stageY
        val bottom = camera.stageY + camera.stageH
        // The continued background only changes when the stage's sprite frame does (or the layout
        // does): between those, put back the rows worked out last time instead of redoing them.
        val sameLayout = w == extW && h == extH && top == extTop && bottom == extBottom && starrySky == extStarry
        if (frameKey >= 0 && frameKey == extFrame && sameLayout && extensionRows.size == w * (h - camera.stageH)) {
            if (top > 0) raw.writePixels(extensionRows, 0, 0, top)
            if (bottom < h) raw.writePixels(extensionRows, top * w, bottom, h - bottom)
            return
        }
        if (pixels.size != w * h) pixels = IntArray(w * h)
        raw.readPixels(pixels)
        StageExtension.fill(pixels, w, h, top, bottom, starrySky)
        raw.writePixels(pixels, 0, 0, h)
        if (extensionRows.size != w * (h - camera.stageH)) extensionRows = IntArray(w * (h - camera.stageH))
        pixels.copyInto(extensionRows, 0, 0, top * w)
        pixels.copyInto(extensionRows, top * w, bottom * w, h * w)
        extW = w; extH = h; extTop = top; extBottom = bottom; extStarry = starrySky; extFrame = frameKey
    }

    private var extensionRows = IntArray(0)
    private var extW = -1
    private var extH = -1
    private var extTop = -1
    private var extBottom = -1
    private var extStarry = false
    private var extFrame = Long.MIN_VALUE

    fun draw(target: DrawScope, scale: Int, block: DrawScope.() -> Unit) {
        val screen = target.size
        val gw = ceil(screen.width / scale).toInt().coerceAtLeast(1)
        val gh = ceil(screen.height / scale).toInt().coerceAtLeast(1)
        val cnv = ensure(gw, gh)
        val shown = surface!!
        drawScope.draw(target, target.layoutDirection, cnv, screen) {
            drawRect(Color.Transparent, Offset.Zero, size, blendMode = BlendMode.Clear)
            scale(1f / scale, 1f / scale, pivot = Offset.Zero) { block() }
        }
        shown.commit()
        target.drawImage(
            image = shown.image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(gw, gh),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(gw * scale, gh * scale),
            filterQuality = FilterQuality.None
        )
    }
}
