package com.example.engine

import androidx.compose.ui.geometry.Offset
import com.example.scene.SceneType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Places the world on the screen (Plan 03, Phase 3: the same close-up scene on every screen).
 *
 * Every scene is laid out on a *stage*: about 144 game pixels wide and between 4:3 and 3:2 tall.
 * The stage is enlarged by a whole number ([zoom]) to fit the screen, so phones and tablets show the
 * same composition at the same closeness. When the screen is taller than the stage, the stage sits
 * in the middle and the renderer continues its sky and ground above and below
 * ([LowResWorldBuffer][com.example.ui.LowResWorldBuffer]); props are never spread apart.
 *
 * Drawing code, layouts and the engine work in *world* units: the stage's size in the classic
 * renderer's units, where one scene pixel is [WORLD_PIXEL] world units. [toWorld] and [toScreenX] /
 * [toScreenY] convert between world units and screen pixels.
 */
class WorldCamera private constructor(
    /** Screen pixels per game pixel. */
    val zoom: Int,
    /** The whole screen in game pixels (partial pixels at the edges round up). */
    val gameW: Int,
    val gameH: Int,
    /** The stage in game pixels, and its top-left corner inside the screen. */
    val stageW: Int,
    val stageH: Int,
    val stageX: Int,
    val stageY: Int,
    /** The world's size in world units, as the drawing code and the engine see it. */
    val worldW: Float,
    val worldH: Float,
    /** Screen pixels per world unit. */
    val screenPerWorld: Float,
    /** Whether this camera stages the world (false: the classic full-screen layout). */
    val staged: Boolean
) {
    /** Screen position of world point ([x], [y]). */
    fun toScreenX(x: Float): Float = stageX * zoom + x * screenPerWorld

    fun toScreenY(y: Float): Float = stageY * zoom + y * screenPerWorld

    /** World position of a screen point (may fall outside the stage). */
    fun toWorld(screen: Offset): Offset =
        Offset((screen.x - stageX * zoom) / screenPerWorld, (screen.y - stageY * zoom) / screenPerWorld)

    /** A screen distance in world units. */
    fun toWorldLength(screenPx: Float): Float = screenPx / screenPerWorld

    companion object {
        /** World units per game pixel: the classic renderer's scene pixel on a phone. */
        const val WORLD_PIXEL = 5

        /** The stage is at least this many game pixels wide (the close-up the tablet showed). */
        const val STAGE_MIN_W = 144

        /** Stage height / width, shortest and tallest. 4:3 keeps every scene whole; 3:2 is the limit before props drift apart. */
        const val STAGE_MIN_ASPECT = 4f / 3f
        const val STAGE_MAX_ASPECT = 1.5f

        /**
         * How much of a tall screen's spare height goes above the stage (the rest goes below), per
         * scene: wherever the scene's edge continues best. The kitchen's striped wall extends well
         * but its checkerboard floor doesn't; the sunroom's tiled floor extends well but its glass
         * roof doesn't.
         */
        fun aboveShareFor(scene: SceneType): Float = when (scene) {
            SceneType.COOKING -> 1f
            SceneType.SUNROOM -> 0.1f
            else -> 0.5f
        }

        /** The camera for a [screenW] x [screenH] pixel screen showing [scene]. */
        fun forScreen(screenW: Float, screenH: Float, scene: SceneType, pixelRenderer: Boolean = WorldViewport.pixelRenderer): WorldCamera =
            forScreen(screenW, screenH, pixelRenderer, aboveShareFor(scene))

        /** The camera for a [screenW] x [screenH] pixel screen; [aboveShare] of the spare height goes above the stage. */
        fun forScreen(
            screenW: Float,
            screenH: Float,
            pixelRenderer: Boolean = WorldViewport.pixelRenderer,
            aboveShare: Float = 0.5f
        ): WorldCamera {
            if (!pixelRenderer || screenW <= 0f || screenH <= 0f) return classic(screenW, screenH)
            val minStageH = ceil(STAGE_MIN_W * STAGE_MIN_ASPECT).toInt()
            val zoom = minOf(floor(screenW / STAGE_MIN_W).toInt(), floor(screenH / minStageH).toInt()).coerceAtLeast(1)
            val gameW = ceil(screenW / zoom).toInt()
            val gameH = ceil(screenH / zoom).toInt()
            val stageW = gameW
            val stageH = if (gameH < stageW * STAGE_MIN_ASPECT) gameH else minOf(gameH, floor(stageW * STAGE_MAX_ASPECT).toInt())
            val stageY = ((gameH - stageH) * aboveShare.coerceIn(0f, 1f)).roundToInt()
            return WorldCamera(
                zoom = zoom, gameW = gameW, gameH = gameH,
                stageW = stageW, stageH = stageH, stageX = 0, stageY = stageY,
                worldW = (stageW * WORLD_PIXEL).toFloat(), worldH = (stageH * WORLD_PIXEL).toFloat(),
                screenPerWorld = zoom.toFloat() / WORLD_PIXEL, staged = true
            )
        }

        /** The classic renderer: the world is the screen. */
        fun classic(screenW: Float, screenH: Float) = WorldCamera(
            zoom = 1, gameW = screenW.toInt(), gameH = screenH.toInt(),
            stageW = screenW.toInt(), stageH = screenH.toInt(), stageX = 0, stageY = 0,
            worldW = screenW, worldH = screenH, screenPerWorld = 1f, staged = false
        )
    }
}
