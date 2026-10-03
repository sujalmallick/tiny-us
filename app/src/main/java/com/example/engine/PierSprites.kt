package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.GullState
import com.example.scene.PierCatch
import com.example.scene.PierFishingPhase
import kotlin.math.sin

/** Pixel sprites for the Seaside Pier's two locals: Grandpa Bao and Pip the seagull. */
object PierSprites {

    private val CrateWood = Color(0xFF8D6E63)
    private val CrateDark = Color(0xFF5D4037)
    private val CoatBlue = Color(0xFF4F6D8A)
    private val CoatShadow = Color(0xFF3B536B)
    private val Skin = Color(0xFFE8B98F)
    private val Beard = Color(0xFFF1F1F1)
    private val HatYellow = Color(0xFFF2C14E)
    private val HatShadow = Color(0xFFC9982F)
    private val RodBrown = Color(0xFF6D4C41)
    private val Line = Color(0xCCFFFFFF)
    private val Bobber = Color(0xFFE63946)

    /**
     * Grandpa Bao sitting on an upturned crate, facing left, rod out over the railing.
     * [cx]/[groundY] is the crate's foot point; [waterY] is where the line meets the sea.
     */
    fun drawGrandpaBao(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float,
        time: Float,
        phase: PierFishingPhase,
        catch: PierCatch?,
        waterY: Float
    ) {
        // Crate
        scope.drawRect(CrateDark, Offset(cx - 8f * p, groundY - 8f * p), Size(16f * p, 8f * p))
        scope.drawRect(CrateWood, Offset(cx - 7f * p, groundY - 7f * p), Size(14f * p, 2.5f * p))
        scope.drawRect(CrateWood, Offset(cx - 7f * p, groundY - 3.5f * p), Size(14f * p, 2.5f * p))

        val seatY = groundY - 8f * p
        // Legs dangling forward and boots
        scope.drawRect(CoatShadow, Offset(cx - 6f * p, seatY - 1f * p), Size(5f * p, 6f * p))
        scope.drawRect(CrateDark, Offset(cx - 7f * p, seatY + 4f * p), Size(4f * p, 2f * p))
        // Coat body
        scope.drawRect(CoatBlue, Offset(cx - 4f * p, seatY - 12f * p), Size(9f * p, 12f * p))
        scope.drawRect(CoatShadow, Offset(cx + 2f * p, seatY - 12f * p), Size(3f * p, 12f * p))
        // Head, beard and bucket hat
        val headY = seatY - 15f * p
        scope.drawCircle(Skin, 3.6f * p, Offset(cx, headY))
        scope.drawRect(Beard, Offset(cx - 3.5f * p, headY + 0.5f * p), Size(6f * p, 3.5f * p))
        scope.drawRect(Color(0xFF2B2B2B), Offset(cx - 2f * p, headY - 1f * p), Size(1f * p, 1f * p))
        scope.drawRect(HatShadow, Offset(cx - 5.5f * p, headY - 3.5f * p), Size(11f * p, 1.6f * p))
        scope.drawRect(HatYellow, Offset(cx - 3.5f * p, headY - 6.5f * p), Size(7f * p, 3.5f * p))

        // Rod: bends while reeling, line dips while a fish nibbles.
        val handX = cx - 4f * p
        val handY = seatY - 7f * p
        scope.drawRect(Skin, Offset(handX - 1.5f * p, handY - 1f * p), Size(2.5f * p, 2.5f * p))
        val bend = when (phase) {
            PierFishingPhase.REELING -> 7f * p + sin(time * 18f) * 1.5f * p
            else -> 0f
        }
        val tipX = handX - 22f * p
        val tipY = handY - 26f * p + bend
        scope.drawLine(RodBrown, Offset(handX, handY), Offset(tipX, tipY), strokeWidth = 1.4f * p)

        val bobberX = tipX - 4f * p
        val bobberY = when (phase) {
            PierFishingPhase.WAITING -> waterY + (if (sin(time * 9f) > 0.6f) 1.5f * p else 0f)
            PierFishingPhase.REELING -> waterY - 3f * p
            PierFishingPhase.CASTING -> tipY + (waterY - tipY) * 0.5f
            else -> waterY
        }
        scope.drawLine(Line, Offset(tipX, tipY), Offset(bobberX, bobberY), strokeWidth = 0.6f * p)
        scope.drawCircle(Bobber, 1.2f * p, Offset(bobberX, bobberY))

        if (phase == PierFishingPhase.SHOWING && catch != null) {
            drawCatch(scope, cx - 2f * p, headY - 14f * p, p, time, catch)
        }
    }

    /** The catch, held up proudly above Bao's hat. */
    private fun drawCatch(scope: DrawScope, x: Float, y: Float, p: Float, time: Float, catch: PierCatch) {
        val bob = sin(time * 4f) * 0.8f * p
        when (catch) {
            PierCatch.FISH -> {
                scope.drawOval(Color(0xFFB0C4DE), Offset(x - 5f * p, y - 2f * p + bob), Size(9f * p, 4.5f * p))
                scope.drawRect(Color(0xFF8DA2BD), Offset(x + 4f * p, y - 2.5f * p + bob), Size(2.5f * p, 5.5f * p))
                scope.drawRect(Color(0xFF1B1B1B), Offset(x - 3f * p, y - 0.8f * p + bob), Size(0.9f * p, 0.9f * p))
            }
            PierCatch.STARFISH -> {
                val c = Color(0xFFFF9F68)
                scope.drawRect(c, Offset(x - 1.2f * p, y - 4f * p + bob), Size(2.4f * p, 8f * p))
                scope.drawRect(c, Offset(x - 4f * p, y - 1.2f * p + bob), Size(8f * p, 2.4f * p))
                scope.drawCircle(Color(0xFFFFC59E), 1f * p, Offset(x, y + bob))
            }
            PierCatch.OLD_BOOT -> {
                scope.drawRect(Color(0xFF5D4037), Offset(x - 2f * p, y - 4f * p + bob), Size(4f * p, 7f * p))
                scope.drawRect(Color(0xFF5D4037), Offset(x - 2f * p, y + 1f * p + bob), Size(7f * p, 2.5f * p))
                scope.drawRect(Color(0xFF3E2723), Offset(x - 2f * p, y + 3f * p + bob), Size(7f * p, 0.8f * p))
            }
            PierCatch.LOVE_LETTER -> {
                scope.drawRect(Color(0x99A8DADC), Offset(x - 3f * p, y - 4f * p + bob), Size(6f * p, 8f * p))
                scope.drawRect(Color(0xFF8D6E63), Offset(x - 2f * p, y - 5.5f * p + bob), Size(4f * p, 1.5f * p))
                scope.drawRect(Color(0xFFFFF1E6), Offset(x - 2f * p, y - 1.5f * p + bob), Size(4f * p, 3f * p))
                scope.drawRect(Color(0xFFFF729F), Offset(x - 0.6f * p, y - 0.6f * p + bob), Size(1.2f * p, 1.2f * p))
            }
            PierCatch.SEAWEED -> {
                val c = Color(0xFF2A9D8F)
                for (i in 0..2) {
                    scope.drawRect(c, Offset(x - 3f * p + i * 2.5f * p, y - 4f * p + bob + i * p), Size(1.4f * p, 8f * p - i * p))
                }
            }
        }
    }

    private val GullWhite = Color(0xFFF7F7F7)
    private val GullGrey = Color(0xFFB8C2CC)
    private val GullDark = Color(0xFF59636E)
    private val GullBeak = Color(0xFFF4A261)

    /**
     * Pip the seagull. [x]/[y] is the feet point (perched) or body centre (flying).
     * [tucked] draws a rounder, head-down pose for waiting out the rain.
     */
    fun drawSeagull(
        scope: DrawScope,
        x: Float,
        y: Float,
        p: Float,
        time: Float,
        state: GullState,
        facingLeft: Boolean,
        tucked: Boolean = false
    ) {
        val s = if (facingLeft) -1f else 1f
        val airborne = state.isAirborne
        val bodyY = if (airborne) y else y - 4f * p
        val waddle = if (state == GullState.SNEAKING) sin(time * 14f) * 0.6f * p else 0f

        // Body
        scope.drawOval(GullWhite, Offset(x - 4.5f * p, bodyY - 2.5f * p + waddle), Size(9f * p, 5f * p))
        // Tail
        scope.drawRect(GullDark, Offset(x - s * 5.5f * p - (if (s < 0) 0f else 2f * p), bodyY - 1.5f * p + waddle), Size(2f * p, 1.5f * p))

        if (airborne) {
            val flap = sin(time * 16f) * 4f * p
            scope.drawLine(GullGrey, Offset(x - 1f * p, bodyY - 1f * p), Offset(x - 7f * p, bodyY - 3f * p - flap), strokeWidth = 2f * p)
            scope.drawLine(GullGrey, Offset(x + 1f * p, bodyY - 1f * p), Offset(x + 7f * p, bodyY - 3f * p - flap), strokeWidth = 2f * p)
        } else {
            scope.drawOval(GullGrey, Offset(x - 3.5f * p, bodyY - 2f * p + waddle), Size(6f * p, 3f * p))
            // Legs
            scope.drawRect(GullBeak, Offset(x - 1.5f * p, y - 2f * p), Size(0.8f * p, 2f * p))
            scope.drawRect(GullBeak, Offset(x + 0.7f * p, y - 2f * p), Size(0.8f * p, 2f * p))
        }

        // Head, eye and beak
        val headDrop = if (tucked) 2f * p else 0f
        val headX = x + s * 4f * p
        val headY = bodyY - 3f * p + headDrop + waddle
        scope.drawCircle(GullWhite, 2.4f * p, Offset(headX, headY))
        scope.drawRect(Color(0xFF1B1B1B), Offset(headX + s * 0.6f * p - 0.4f * p, headY - 0.9f * p), Size(0.9f * p, 0.9f * p))
        if (!tucked) {
            val beakX = if (s > 0) headX + 1.8f * p else headX - 4.3f * p
            scope.drawRect(GullBeak, Offset(beakX, headY - 0.2f * p), Size(2.5f * p, 1.1f * p))
        }
    }
}
