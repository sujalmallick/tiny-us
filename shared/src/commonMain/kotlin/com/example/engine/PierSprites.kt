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
    private val Thermos = Color(0xFFC0392B)
    private val ThermosCap = Color(0xFF9E9E9E)
    private val EyeDark = Color(0xFF2B2B2B)
    private val Steam = Color(0x99FFFFFF)

    /**
     * Grandpa Bao sitting on an upturned crate, facing left, rod out over the railing.
     * [cx]/[groundY] is the crate's foot point; [waterY] is where the line meets the sea.
     * Between taps he breathes, blinks, sips tea from his thermos cup, waves, or dozes off.
     */
    fun drawGrandpaBao(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        p: Float,
        time: Float,
        phase: PierFishingPhase,
        catch: PierCatch?,
        waterY: Float,
        sipping: Boolean = false,
        waving: Boolean = false,
        dozing: Boolean = false
    ) {
        // Crate, with his red thermos beside him
        drawContactShadow(scope, cx, groundY, 18, p)
        scope.drawRect(CrateDark, Offset(cx - 8f * p, groundY - 8f * p), Size(16f * p, 8f * p))
        scope.drawRect(CrateWood, Offset(cx - 7f * p, groundY - 7f * p), Size(14f * p, 2.5f * p))
        scope.drawRect(CrateWood, Offset(cx - 7f * p, groundY - 3.5f * p), Size(14f * p, 2.5f * p))
        scope.drawRect(Thermos, Offset(cx + 9f * p, groundY - 6f * p), Size(3f * p, 6f * p))
        scope.drawRect(ThermosCap, Offset(cx + 9f * p, groundY - 7f * p), Size(3f * p, 1.2f * p))

        val seatY = groundY - 8f * p
        // Slow breathing lifts the upper body; dozing slumps it a little.
        val breath = sin(time * 2.2f) * 0.35f * p + if (dozing) 0.8f * p else 0f
        // Legs dangling forward and boots, swinging gently
        val legSwing = if (dozing) 0f else sin(time * 1.4f) * 0.5f * p
        scope.drawRect(CoatShadow, Offset(cx - 6f * p, seatY - 1f * p), Size(5f * p, 6f * p))
        scope.drawRect(CrateDark, Offset(cx - 7f * p + legSwing, seatY + 4f * p), Size(4f * p, 2f * p))
        // Coat body
        scope.drawRect(CoatBlue, Offset(cx - 4f * p, seatY - 12f * p + breath), Size(9f * p, 12f * p - breath))
        scope.drawRect(CoatShadow, Offset(cx + 2f * p, seatY - 12f * p + breath), Size(3f * p, 12f * p - breath))

        // Head, beard and bucket hat; the head nods forward while dozing.
        val headX = cx - if (dozing) 0.8f * p else 0f
        val headY = seatY - 15f * p + breath + if (dozing) 1.2f * p else 0f
        scope.drawCircle(Skin, 3.6f * p, Offset(headX, headY))
        val beardSway = sin(time * 1.7f) * 0.3f * p
        scope.drawRect(Beard, Offset(headX - 3.5f * p + beardSway, headY + 0.5f * p), Size(6f * p, 3.5f * p))
        val blinking = dozing || (time % 4.3f) < 0.14f
        if (blinking) {
            scope.drawRect(EyeDark, Offset(headX - 2.5f * p, headY - 0.6f * p), Size(1.8f * p, 0.5f * p))
        } else {
            scope.drawRect(EyeDark, Offset(headX - 2f * p, headY - 1f * p), Size(1f * p, 1f * p))
        }
        scope.drawRect(HatShadow, Offset(headX - 5.5f * p, headY - 3.5f * p), Size(11f * p, 1.6f * p))
        scope.drawRect(HatYellow, Offset(headX - 3.5f * p, headY - 6.5f * p), Size(7f * p, 3.5f * p))

        // Free arm: a tea cup to the beard, a wave to the couple, or resting on his knee.
        val shoulderX = cx + 3f * p
        val shoulderY = seatY - 10f * p + breath
        when {
            waving && !dozing -> {
                val wag = sin(time * 12f) * 1.2f * p
                scope.drawLine(CoatBlue, Offset(shoulderX, shoulderY), Offset(shoulderX + 3f * p + wag, shoulderY - 8f * p), strokeWidth = 2.4f * p)
                scope.drawCircle(Skin, 1.4f * p, Offset(shoulderX + 3f * p + wag, shoulderY - 9f * p))
            }
            sipping && !dozing -> {
                val cupX = headX - 2.5f * p
                val cupY = headY + 2.2f * p
                scope.drawLine(CoatBlue, Offset(shoulderX, shoulderY), Offset(cupX + 1f * p, cupY + 1.5f * p), strokeWidth = 2.4f * p)
                scope.drawRect(ThermosCap, Offset(cupX - 1.2f * p, cupY - 1.2f * p), Size(2.6f * p, 2.6f * p))
                val rise = (time * 6f) % (5f * p)
                scope.drawCircle(Steam, 0.9f * p, Offset(cupX + sin(time * 3f) * 0.6f * p, cupY - 2.5f * p - rise))
            }
            else -> {
                scope.drawRect(CoatBlue, Offset(shoulderX - 1f * p, shoulderY), Size(2.4f * p, 6f * p))
                scope.drawRect(Skin, Offset(shoulderX - 1f * p, shoulderY + 5.5f * p), Size(2f * p, 1.6f * p))
            }
        }

        // Rod hand. The rod sways in the breeze, bends while reeling, and droops while he dozes.
        val handX = cx - 4f * p
        val handY = seatY - 7f * p + breath
        scope.drawRect(Skin, Offset(handX - 1.5f * p, handY - 1f * p), Size(2.5f * p, 2.5f * p))
        val bend = when {
            phase == PierFishingPhase.REELING -> 7f * p + sin(time * 18f) * 1.5f * p
            dozing -> 5f * p
            else -> sin(time * 1.3f) * 0.8f * p
        }
        // A long rod reaching out over the railing, so its tip sits above the water and the line hangs down.
        val tipX = handX - 24f * p
        val tipY = minOf(handY - 26f * p, waterY - 10f * p) + bend
        scope.drawLine(RodBrown, Offset(handX, handY), Offset(tipX, tipY), strokeWidth = 1.2f * p)

        // The bobber rides the swell; a nibble makes it dip.
        val bobberX = tipX - 4f * p
        val swell = sin(time * 1.8f) * 0.6f * p
        val bobberY = when (phase) {
            PierFishingPhase.WAITING -> waterY + swell + (if (sin(time * 9f) > 0.6f) 1.5f * p else 0f)
            PierFishingPhase.REELING -> waterY - 3f * p
            PierFishingPhase.CASTING -> tipY + (waterY - tipY) * 0.5f
            else -> waterY + swell
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

    private val CrabRed = Color(0xFFE76F51)
    private val CrabDark = Color(0xFFB4452B)

    /** Pinchy the crab, scuttling sideways. Claws go up when he's startled. */
    fun drawCrab(scope: DrawScope, x: Float, y: Float, p: Float, time: Float, startled: Boolean) {
        val step = sin(time * if (startled) 30f else 12f) * 0.6f * p
        for (sideIndex in 0..1) {
            val side = if (sideIndex == 0) -1f else 1f
            for (leg in 0..2) {
                val lx = x + side * (2.5f + leg * 1.2f) * p
                scope.drawRect(CrabDark, Offset(lx - 0.4f * p, y - 1.5f * p + (if (leg % 2 == 0) step else -step)), Size(0.8f * p, 1.8f * p))
            }
        }
        scope.drawOval(CrabRed, Offset(x - 4f * p, y - 4.5f * p), Size(8f * p, 4f * p))
        val clawLift = if (startled) 3f * p else 0f
        scope.drawCircle(CrabRed, 1.4f * p, Offset(x - 5f * p, y - 4.5f * p - clawLift))
        scope.drawCircle(CrabRed, 1.4f * p, Offset(x + 5f * p, y - 4.5f * p - clawLift))
        scope.drawRect(Color(0xFF1B1B1B), Offset(x - 1.6f * p, y - 6f * p), Size(0.8f * p, 1.4f * p))
        scope.drawRect(Color(0xFF1B1B1B), Offset(x + 0.8f * p, y - 6f * p), Size(0.8f * p, 1.4f * p))
    }

    private val DolphinBlue = Color(0xFF6C8EAD)
    private val DolphinBelly = Color(0xFFC9D6E3)

    /**
     * One dolphin mid-leap. [arc] runs 0..1 from leaving the water to diving back in;
     * [x]/[waterY] is where it breaks the surface.
     */
    fun drawDolphin(scope: DrawScope, x: Float, waterY: Float, p: Float, arc: Float) {
        if (arc !in 0f..1f) return
        val height = sin(arc * kotlin.math.PI.toFloat()) * 12f * p
        val cx = x + (arc - 0.5f) * 20f * p
        val cy = waterY - height
        scope.drawOval(DolphinBlue, Offset(cx - 5f * p, cy - 2f * p), Size(10f * p, 4f * p))
        scope.drawOval(DolphinBelly, Offset(cx - 3.5f * p, cy), Size(7f * p, 1.6f * p))
        scope.drawRect(DolphinBlue, Offset(cx - 0.5f * p, cy - 4f * p), Size(1.8f * p, 2.2f * p))
        scope.drawRect(DolphinBlue, Offset(cx - 7f * p, cy - 1.5f * p + (arc - 0.5f) * 2f * p), Size(2.5f * p, 1.4f * p))
        if (arc < 0.12f || arc > 0.88f) {
            val splashX = if (arc < 0.5f) x - 10f * p else x + 10f * p
            scope.drawCircle(Color(0xCCFFFFFF), 1.6f * p, Offset(splashX, waterY - 1f * p))
            scope.drawCircle(Color(0x99FFFFFF), 1f * p, Offset(splashX + 2f * p, waterY - 2.5f * p))
        }
    }

    /** The little sailboat on the horizon; its pennant flaps hard when it toots. */
    fun drawSailboat(scope: DrawScope, x: Float, horizonY: Float, p: Float, time: Float, tooting: Boolean, isNight: Boolean) {
        val bob = sin(time * 1.4f) * 0.4f * p
        val hull = if (isNight) Color(0xFF3B3F55) else Color(0xFF8D5A3B)
        val sail = if (isNight) Color(0xFFAEB6CC) else Color(0xFFFDF8EE)
        scope.drawRect(hull, Offset(x - 5f * p, horizonY - 2f * p + bob), Size(10f * p, 2f * p))
        scope.drawRect(hull, Offset(x - 4f * p, horizonY + bob), Size(8f * p, 1f * p))
        scope.drawRect(Color(0xFF5D4037), Offset(x - 0.4f * p, horizonY - 12f * p + bob), Size(0.8f * p, 10f * p))
        for (row in 0..4) {
            val w = (row + 1) * 1.2f * p
            scope.drawRect(sail, Offset(x + 0.6f * p, horizonY - 11.5f * p + row * 1.8f * p + bob), Size(w, 1.8f * p))
        }
        val flap = if (tooting) sin(time * 20f) * 1f * p else sin(time * 4f) * 0.4f * p
        scope.drawRect(Color(0xFFE63946), Offset(x - 0.4f * p, horizonY - 13.5f * p + bob + flap * 0.2f), Size(3f * p, 1.2f * p + flap * 0.3f))
        if (isNight) scope.drawCircle(Color(0xFFFFE9A0), 0.8f * p, Offset(x + 4f * p, horizonY - 2.5f * p + bob))
    }

    /** Coin-operated viewer bolted to the deck. */
    fun drawTelescope(scope: DrawScope, x: Float, groundY: Float, p: Float, glinting: Boolean) {
        val metal = Color(0xFF4E7A8C)
        val metalDark = Color(0xFF355766)
        scope.drawRect(metalDark, Offset(x - 3f * p, groundY - 1.5f * p), Size(6f * p, 1.5f * p))
        scope.drawRect(metal, Offset(x - 0.8f * p, groundY - 9f * p), Size(1.6f * p, 8f * p))
        scope.drawRect(metal, Offset(x - 4f * p, groundY - 13f * p), Size(7f * p, 4f * p))
        scope.drawRect(metalDark, Offset(x - 5.5f * p, groundY - 12.5f * p), Size(1.8f * p, 3f * p))
        scope.drawRect(metalDark, Offset(x + 2.8f * p, groundY - 12.2f * p), Size(1.5f * p, 2.4f * p))
        scope.drawRect(Color(0xFFFFD166), Offset(x + 0.4f * p, groundY - 8f * p), Size(1f * p, 1.4f * p))
        if (glinting) scope.drawCircle(Color(0xCCFFF3B0), 1.4f * p, Offset(x - 5f * p, groundY - 11f * p))
    }

    /** A distant gull, just a soft "v", for the flock that takes off when the foghorn sounds. */
    fun drawDistantGull(scope: DrawScope, x: Float, y: Float, p: Float, time: Float, isNight: Boolean) {
        val flap = sin(time * 10f) * 1.2f * p
        val c = if (isNight) Color(0xFFB0BCD0) else Color(0xFF4A4E57)
        scope.drawLine(c, Offset(x - 3f * p, y - flap), Offset(x, y), strokeWidth = 0.8f * p)
        scope.drawLine(c, Offset(x, y), Offset(x + 3f * p, y - flap), strokeWidth = 0.8f * p)
    }
}
