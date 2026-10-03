package com.example.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Shared prop geometry for the cafe and campfire scenes. The drawing code, the tap
 * hit-tests and the particle spawns all read from here, so a tap on a sprite always
 * reaches that sprite. Positions are canvas pixels for a [cw] x [ch] canvas at pixel scale [p].
 */
private data class PropTarget<T>(val prop: T, val center: Offset, val radius: Float)

/** Mochi's tap box, matching the generic cat hit-test in PixelWorldView. */
fun mochiTapCenter(catWorldX: Float, catWorldY: Float, cw: Float, ch: Float, p: Float): Offset =
    Offset(cw * catWorldX, ch * catWorldY - 7f * p)

private fun isInMochiBox(tap: Offset, mochi: Offset, p: Float): Boolean =
    abs(tap.x - mochi.x) < 22f * p && abs(tap.y - mochi.y) < 18f * p

/**
 * Picks the prop whose center is nearest to the tap among every target the tap lands in.
 * Mochi takes part as [mochiProp] so neighbouring props never swallow her taps.
 */
private fun <T> nearestHit(tap: Offset, targets: List<PropTarget<T>>, mochi: Offset, mochiProp: T, p: Float): T? {
    var best: T? = null
    var bestDist = Float.MAX_VALUE
    for (t in targets) {
        val d = hypot(tap.x - t.center.x, tap.y - t.center.y)
        if (d < t.radius && d < bestDist) {
            best = t.prop
            bestDist = d
        }
    }
    if (isInMochiBox(tap, mochi, p)) {
        val d = hypot(tap.x - mochi.x, tap.y - mochi.y)
        if (d < bestDist) best = mochiProp
    }
    return best
}

enum class CafeProp { MOCHI, BARISTA, MENU, PUP, LATTE, PASTRY, PASSERBY, WINDOW }

object CafeLayout {
    const val MAX_PASTRY_BITES = 3

    /** Where the back wall meets the floor. The couple's feet rest a little in front of it. */
    const val WALL_BOTTOM = 0.655f

    /** Feet line of the seated couple (their home spot in SceneEngine). */
    const val SEAT_FEET_Y = 0.68f

    fun window(cw: Float, ch: Float): Rect = Rect(cw * 0.36f, ch * 0.08f, cw * 0.94f, ch * 0.52f)

    /** The glass pane inside the window frame, where fog hearts can be drawn. */
    fun glass(cw: Float, ch: Float, p: Float): Rect = window(cw, ch).deflate(6f * p)

    fun barX(cw: Float) = cw * 0.05f
    fun barW(cw: Float) = cw * 0.28f

    /** Counter top: a waist-high counter standing against the back wall. */
    fun barY(ch: Float, p: Float) = ch * (WALL_BOTTOM + 0.01f) - 20f * p

    /** Leo's feet line, behind the counter. */
    fun barista(cw: Float, ch: Float, p: Float) = Offset(barX(cw) + barW(cw) * 0.56f, barY(ch, p) + 12f * p)

    /** Chalkboard menu hanging on the brick wall above the counter. */
    fun menuTopLeft(cw: Float, ch: Float, p: Float) = Offset(barX(cw) + barW(cw) / 2f - 16f * p, ch * 0.21f)

    /**
     * Small round cafe table standing just in front of the seated couple, drawn after them so
     * it hides their laps like a real table. [tableY] is the table top.
     */
    fun tableW(p: Float) = 40f * p
    fun tableX(cw: Float, p: Float) = cw * 0.5f - tableW(p) / 2f
    fun tableY(ch: Float, p: Float) = ch * SEAT_FEET_Y - 8f * p

    fun latte(cw: Float, ch: Float, p: Float) = Offset(tableX(cw, p) + tableW(p) * 0.24f, tableY(ch, p) - 4f * p)
    fun plate(cw: Float, ch: Float, p: Float) = Offset(tableX(cw, p) + tableW(p) * 0.76f, tableY(ch, p) - 2.5f * p)

    /** Croissant width per bite, so the last bite leaves an empty plate. */
    fun croissantWidth(bites: Int, p: Float): Float =
        12f * p * (1f - bites.coerceIn(0, MAX_PASTRY_BITES) / MAX_PASTRY_BITES.toFloat())

    fun rug(cw: Float, ch: Float) = Offset(cw * 0.82f, ch * 0.72f)
    fun pup(cw: Float, ch: Float, p: Float) = rug(cw, ch) + Offset(11f * p, 8f * p)

    /** The umbrella passerby's feet, or null while they are out of sight of the window. */
    fun passerby(cw: Float, ch: Float, p: Float, time: Float): Offset? {
        val win = window(cw, ch)
        val norm = ((time * 0.045f) % 1.3f) - 0.15f
        val x = win.left + norm * win.width
        if (x !in (win.left + 2f * p)..(win.right - 14f * p)) return null
        return Offset(x, win.top + win.height * 0.65f)
    }

    fun hitTest(
        tap: Offset, cw: Float, ch: Float, p: Float, time: Float,
        catWorldX: Float, catWorldY: Float
    ): CafeProp? {
        val leo = barista(cw, ch, p)
        val menu = menuTopLeft(cw, ch, p)
        val targets = buildList {
            add(PropTarget(CafeProp.BARISTA, leo - Offset(0f, 15f * p), 17f * p))
            add(PropTarget(CafeProp.MENU, menu + Offset(16f * p, 21f * p), 21f * p))
            add(PropTarget(CafeProp.PUP, pup(cw, ch, p), 11f * p))
            add(PropTarget(CafeProp.LATTE, latte(cw, ch, p), 11f * p))
            add(PropTarget(CafeProp.PASTRY, plate(cw, ch, p) + Offset(0f, 2f * p), 11f * p))
            passerby(cw, ch, p, time)?.let { add(PropTarget(CafeProp.PASSERBY, it - Offset(0f, 9f * p), 14f * p)) }
        }
        val hit = nearestHit(tap, targets, mochiTapCenter(catWorldX, catWorldY, cw, ch, p), CafeProp.MOCHI, p)
        if (hit != null) return hit
        return if (glass(cw, ch, p).contains(tap)) CafeProp.WINDOW else null
    }
}

enum class CampfireProp { MOCHI, FIRE, GUITAR, LANTERN }

object CampfireLayout {
    const val PIT_X = 0.48f
    const val PIT_Y = 0.74f

    /** Half-width of the strip around the fire pit that walkers and Mochi keep out of. */
    const val PIT_KEEP_OUT_HALF_WIDTH = 0.085f

    /** Feet at or below this line would stand in the flames; higher up they pass behind the fire. */
    const val PIT_KEEP_OUT_MIN_Y = 0.70f

    const val BLANKET_X = 0.72f
    const val BLANKET_Y = 0.69f

    fun fire(cw: Float, ch: Float) = Offset(cw * PIT_X, ch * PIT_Y)

    /** Ground line the tent is pitched on: behind the log, in front of the treeline. */
    const val TENT_BASE_Y = 0.60f

    fun tentSize(p: Float) = Offset(54f * p, 50f * p)
    fun tentTopLeft(cw: Float, ch: Float, p: Float) = Offset(cw * 0.06f, ch * TENT_BASE_Y - tentSize(p).y)
    /** The lantern hangs from a shepherd's-hook pole planted beside the tent door. */
    fun lantern(cw: Float, ch: Float, p: Float): Offset {
        val tent = tentTopLeft(cw, ch, p)
        val size = tentSize(p)
        return Offset(tent.x + size.x + 4f * p, tent.y + size.y * 0.45f)
    }

    fun logX(cw: Float) = cw * 0.33f
    fun logW(cw: Float) = cw * 0.34f
    /** Top of the log the couple sits on: seat height above their feet line (0.68). */
    fun logY(ch: Float, p: Float) = ch * 0.68f - 7f * p
    fun guitar(cw: Float, ch: Float, p: Float) = Offset(logX(cw) + logW(cw) + 4f * p, logY(ch, p) - 2f * p)

    /** True when Mochi is resting on the plaid blanket rather than somewhere else in the clearing. */
    fun isOnBlanket(catWorldX: Float, catWorldY: Float): Boolean =
        catWorldX in (BLANKET_X - 0.02f)..(BLANKET_X + 0.12f) && abs(catWorldY - BLANKET_Y) < 0.04f

    /** Nudges an x position out to the nearest side of the fire pit when it would land in the flames. */
    fun avoidPit(x: Float, y: Float): Float {
        if (y < PIT_KEEP_OUT_MIN_Y) return x
        val dx = x - PIT_X
        if (abs(dx) >= PIT_KEEP_OUT_HALF_WIDTH) return x
        return PIT_X + if (dx < 0f) -PIT_KEEP_OUT_HALF_WIDTH else PIT_KEEP_OUT_HALF_WIDTH
    }

    fun hitTest(tap: Offset, cw: Float, ch: Float, p: Float, catWorldX: Float, catWorldY: Float): CampfireProp? {
        val gtr = guitar(cw, ch, p)
        val lant = lantern(cw, ch, p)
        val tent = tentTopLeft(cw, ch, p)
        val tentSize = tentSize(p)
        val targets = listOf(
            PropTarget(CampfireProp.FIRE, fire(cw, ch) - Offset(0f, 4f * p), 18f * p),
            PropTarget(CampfireProp.GUITAR, gtr + Offset(p, 2f * p), 13f * p),
            PropTarget(CampfireProp.LANTERN, lant + Offset(0f, 5f * p), 12f * p)
        )
        val hit = nearestHit(tap, targets, mochiTapCenter(catWorldX, catWorldY, cw, ch, p), CampfireProp.MOCHI, p)
        if (hit != null) return hit
        val onTent = tap.x in tent.x..(tent.x + tentSize.x) && tap.y in tent.y..(tent.y + tentSize.y)
        return if (onTent) CampfireProp.LANTERN else null
    }
}

enum class PierProp { MOCHI, PIP, BAO, CART, BOTTLE, LIGHTHOUSE, TELESCOPE, BOAT, CRAB, BUCKET, LIGHTS, SEA }

/** The moving or disappearing parts of the pier a tap needs to know about. */
data class PierTapState(
    val gullX: Float,
    val gullY: Float,
    val gullVisible: Boolean,
    val bottleVisible: Boolean,
    val crabX: Float,
    val crabVisible: Boolean
)

object PierLayout {
    const val HORIZON_Y = 0.42f
    /** Top of the wooden railing; Pip perches here. */
    const val RAIL_Y = 0.60f
    const val DECK_Y = 0.63f

    /** Railing spots Pip lands on between raids. */
    val PERCH_XS = listOf(0.24f, 0.62f, 0.72f)

    const val WALK_MIN_X = 0.24f
    const val WALK_MAX_X = 0.76f
    const val WALK_MIN_Y = 0.68f
    const val WALK_MAX_Y = 0.84f

    /** Pinchy the crab patrols this strip of boardwalk, in front of the couple. */
    const val CRAB_Y = 0.83f
    const val CRAB_MIN_X = 0.28f
    const val CRAB_MAX_X = 0.72f

    fun lighthouseBase(cw: Float, ch: Float) = Offset(cw * 0.88f, ch * HORIZON_Y)
    /** Lamp room at the top of the tower; the night beam sweeps from here. */
    fun lighthouseLamp(cw: Float, ch: Float, p: Float) = lighthouseBase(cw, ch) - Offset(0f, 38f * p)

    /** The bottle's resting point on the water; it bobs a little around this. */
    fun bottle(cw: Float, ch: Float) = Offset(cw * 0.30f, ch * 0.52f)

    /** Ice-cream cart wheels, on the deck at the far left. */
    fun cart(cw: Float, ch: Float) = Offset(cw * 0.13f, ch * 0.70f)

    /** Grandpa Bao's crate on the deck at the far right. */
    fun bao(cw: Float, ch: Float) = Offset(cw * 0.85f, ch * 0.71f)
    /** Bao's sprite scale relative to the scene's pixel size, close to the couple's. */
    const val BAO_SCALE = 1.3f

    /** Where Mochi trots to collect a fish from Bao. */
    const val MOCHI_FISH_X = 0.74f
    const val MOCHI_FISH_Y = 0.73f

    fun bucket(cw: Float, ch: Float) = Offset(cw * 0.71f, ch * 0.76f)

    /** Coin-operated viewer bolted to the deck by the railing. */
    fun telescope(cw: Float, ch: Float) = Offset(cw * 0.29f, ch * 0.665f)

    /** Where the dolphins leap when spotted through the telescope. */
    fun dolphinWaterline(cw: Float, ch: Float) = Offset(cw * 0.44f, ch * 0.53f)

    /** The little sailboat drifting along the horizon, or null while it is out of view. */
    fun boat(cw: Float, ch: Float, time: Float): Offset? {
        val norm = ((time * 0.010f) % 1.4f) - 0.2f
        if (norm !in 0.02f..0.80f) return null
        return Offset(cw * norm, ch * HORIZON_Y + 1f)
    }

    fun crab(cw: Float, ch: Float, crabX: Float) = Offset(cw * crabX, ch * CRAB_Y)

    fun hitTest(tap: Offset, cw: Float, ch: Float, p: Float, time: Float, catWorldX: Float, catWorldY: Float, state: PierTapState): PierProp? {
        val bottleBob = kotlin.math.sin(time * 1.8f) * 1.5f * p
        val targets = buildList {
            if (state.gullVisible) add(PropTarget(PierProp.PIP, Offset(cw * state.gullX, ch * state.gullY - 4f * p), 12f * p))
            add(PropTarget(PierProp.BAO, bao(cw, ch) - Offset(0f, 16f * p), 16f * p))
            add(PropTarget(PierProp.CART, cart(cw, ch) - Offset(0f, 15f * p), 17f * p))
            if (state.bottleVisible) add(PropTarget(PierProp.BOTTLE, bottle(cw, ch) + Offset(0f, bottleBob), 10f * p))
            add(PropTarget(PierProp.LIGHTHOUSE, lighthouseBase(cw, ch) - Offset(0f, 22f * p), 20f * p))
            add(PropTarget(PierProp.TELESCOPE, telescope(cw, ch) - Offset(0f, 9f * p), 10f * p))
            boat(cw, ch, time)?.let { add(PropTarget(PierProp.BOAT, it - Offset(0f, 6f * p), 12f * p)) }
            if (state.crabVisible) add(PropTarget(PierProp.CRAB, crab(cw, ch, state.crabX) - Offset(0f, 2f * p), 9f * p))
            add(PropTarget(PierProp.BUCKET, bucket(cw, ch) - Offset(0f, 4f * p), 7f * p))
        }
        val hit = nearestHit(tap, targets, mochiTapCenter(catWorldX, catWorldY, cw, ch, p), PierProp.MOCHI, p)
        if (hit != null) return hit
        // The string lights run along the top of the railing.
        if (tap.y in (ch * RAIL_Y - 5f * p)..(ch * RAIL_Y + 2f * p)) return PierProp.LIGHTS
        return if (tap.y in (ch * HORIZON_Y)..(ch * RAIL_Y)) PierProp.SEA else null
    }
}
