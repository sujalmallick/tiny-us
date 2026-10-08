package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.engine.drawCastShadow
import com.example.engine.drawContactShadow
import com.example.scene.FoxVisit
import com.example.scene.SceneEngine
import kotlin.math.PI
import kotlin.math.sin

/*
 * Plan 10, D: the Friday fox, drawn in front of the couple: a fox cub trotting in with its ball,
 * catch with Mochi, and the ball it leaves in the grass on a Friday they missed.
 */

private val FOX_COLORS = mapOf(
    'O' to Color(0xFFE8743B),
    'o' to Color(0xFFC4572A),
    'W' to Color(0xFFFFF4E6),
    'K' to Color(0xFF3D2C2E)
)

/** The fox facing right, without its tail and legs (18 x 9 cells, legs below). */
private val FOX_BODY = listOf(
    "...........K...K..",
    "...........OO..OO.",
    "...........OOOOOO.",
    "..........OOOKOOOO",
    "..........OWWOOOOK",
    "..........OWWWWW..",
    "...OOOOOOOOWWW....",
    "...OOOOOOOOOWW....",
    "....OoOOOOOoO....."
)

private val FOX_LEGS_STAND = listOf("....K.K...K.K.....", "....K.K...K.K.....")
private val FOX_LEGS_TROT_A = listOf("...K...K..K...K...", "...K...K..K...K...")
private val FOX_LEGS_TROT_B = listOf(".....KK....KK.....", ".....KK....KK.....")

/** The tail: up, then down, as it wags. Cells (x, y, colour key). */
private val FOX_TAIL_UP = listOf(Triple(2, 6, 'O'), Triple(2, 5, 'O'), Triple(1, 5, 'O'), Triple(1, 4, 'O'), Triple(0, 4, 'W'), Triple(0, 3, 'W'))
private val FOX_TAIL_DOWN = listOf(Triple(2, 6, 'O'), Triple(1, 6, 'O'), Triple(1, 7, 'O'), Triple(0, 7, 'W'), Triple(0, 8, 'W'))

private val BALL_RED = Color(0xFFE63946)
private val BALL_STRIPE = Color(0xFFFFD166)

/** The fox is drawn a little bigger than the world's pixels, about Mochi's size. */
private const val FOX_SCALE = 1.6f

fun drawFridayFox(scope: DrawScope, engine: SceneEngine, cw: Float, ch: Float, worldP: Float) {
    val p = worldP * FOX_SCALE
    engine.foxBallFriday?.let { drawLeftBall(scope, cw * SceneEngine.FOX_BALL_X, ch * SceneEngine.FOX_BALL_Y, p, engine.sceneTime) }
    val fox = engine.foxVisit
    if (!fox.active) return
    val cx = cw * fox.x
    val bottom = ch * fox.y
    val hopLift = if (fox.hop > 0f) sin(fox.hop / 0.45f * PI.toFloat()) * 5f * p else 0f
    val left = cx - 9f * p
    val top = bottom - 11f * p - hopLift
    val flip = fox.facingLeft
    fun cell(x: Int, y: Int, c: Color, w: Int = 1) {
        val col = if (flip) 17 - (x + w - 1) else x
        scope.drawRect(c, Offset(left + col * p, top + y * p), Size(w * p, p))
    }

    // A soft shadow on the grass
    drawCastShadow(scope, cx, bottom, 16, p, heightPx = 14)
    drawContactShadow(scope, cx, bottom, 16, p)

    val trotting = fox.phase == FoxVisit.Phase.ARRIVING || fox.phase == FoxVisit.Phase.LEAVING
    val step = ((fox.age * 8f).toInt() and 1) == 0
    val legs = when {
        fox.hop > 0f -> FOX_LEGS_TROT_A
        trotting -> if (step) FOX_LEGS_TROT_A else FOX_LEGS_TROT_B
        else -> FOX_LEGS_STAND
    }
    val wag = if (trotting) step else ((fox.age * 5f).toInt() and 1) == 0
    for ((x, y, k) in if (wag) FOX_TAIL_UP else FOX_TAIL_DOWN) cell(x, y, FOX_COLORS.getValue(k))
    for ((y, row) in (FOX_BODY + legs).withIndex()) {
        for ((x, ch0) in row.withIndex()) {
            val c = FOX_COLORS[ch0] ?: continue
            cell(x, y, c)
        }
    }

    // The ball: in its mouth, in the air between them, or at Mochi's paws
    val mouthX = if (flip) left + (17 - 17) * p else left + 17f * p
    val mouthY = top + 5f * p
    when {
        fox.ballFlying -> {
            val t = fox.ballT.coerceIn(0f, 1f)
            val bx = cw * (fox.ballFrom + (fox.ballTo - fox.ballFrom) * t)
            val by = bottom - 3f * p - sin(t * PI.toFloat()) * 12f * p
            drawBall(scope, bx, by, p)
        }
        fox.ballWithMochi -> {
            val mx = cw * engine.catWorldX + (if (engine.catFacingLeft) -9f else 9f) * p
            drawBall(scope, mx, ch * engine.catWorldY - 2f * p, p)
        }
        else -> drawBall(scope, mouthX + p / 2f, mouthY + p / 2f, p)
    }
}

/** A 3 x 3 ball centred on ([x], [y]). */
private fun drawBall(scope: DrawScope, x: Float, y: Float, p: Float) {
    scope.drawRect(BALL_RED, Offset(x - 0.5f * p, y - 1.5f * p), Size(p, p))
    scope.drawRect(BALL_RED, Offset(x - 1.5f * p, y - 0.5f * p), Size(3f * p, p))
    scope.drawRect(BALL_STRIPE, Offset(x - 0.5f * p, y - 0.5f * p), Size(p, p))
    scope.drawRect(BALL_RED, Offset(x - 0.5f * p, y + 0.5f * p), Size(p, p))
}

/** The ball left in the grass, glinting now and then, with little paw prints trotting away. */
private fun drawLeftBall(scope: DrawScope, x: Float, y: Float, p: Float, time: Float) {
    val print = Color(0x553D2C2E)
    for (i in 0 until 4) {
        val px = x + (8f + i * 7f) * p
        val py = y + (if (i % 2 == 0) 1f else -1f) * p
        scope.drawRect(print, Offset(px, py), Size(p, p))
        scope.drawRect(print, Offset(px + 1.5f * p, py - 0.5f * p), Size(p * 0.6f, p * 0.6f))
    }
    drawBall(scope, x, y - 1.5f * p, p)
    if (((time * 1.5f).toInt() % 3) == 0) scope.drawRect(Color(0xFFFFF3B0), Offset(x + p, y - 4f * p), Size(p, p))
    // A couple of grass blades in front of it, so it's lying in the grass
    val blade = Color(0xFF55A630)
    scope.drawRect(blade, Offset(x - 2f * p, y - 1f * p), Size(p * 0.6f, 2f * p))
    scope.drawRect(blade, Offset(x + 1.5f * p, y - 0.5f * p), Size(p * 0.6f, 1.5f * p))
}
