package com.example.scene

import com.example.engine.WorldViewport

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

    /** The barista's drawn scale for scene pixel [p] (whole pixels in the pixel renderer). */
    fun baristaScale(p: Float) = WorldViewport.spriteScale(p, 1.38f)

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
            add(PropTarget(CafeProp.BARISTA, leo - Offset(0f, 10.9f * baristaScale(p)), 12.3f * baristaScale(p)))
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

    /** Bao's drawn scale for scene pixel [p] (whole pixels in the pixel renderer). */
    fun baoScale(p: Float) = WorldViewport.spriteScale(p, BAO_SCALE)

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
            add(PropTarget(PierProp.BAO, bao(cw, ch) - Offset(0f, 12.3f * baoScale(p)), 12.3f * baoScale(p)))
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

/** Where the outdoor weather keepsakes sit: the rainbow after rain and the snowday snowman. */
/**
 * The kitchen's wall layout, shared by its drawing and its taps. The timber beam sits high so the
 * wall is tall, with the window centred on it above the sink and the clock beside the window.
 */
object KitchenLayout {
    fun floorY(ch: Float) = ch * 0.65f
    fun ceilingY(ch: Float) = ch * 0.14f
    fun wainscotTop(ch: Float, p: Float) = floorY(ch) - 42f * p
    const val WINDOW_W = 34f
    const val WINDOW_H = 44f
    /** The window's top edge: its sill sits a little above the wainscoting, clear of the faucet. */
    fun windowTop(ch: Float, p: Float) = wainscotTop(ch, p) - (WINDOW_H + 14f) * p
    fun clockCenter(cw: Float, ch: Float, p: Float) = Offset(cw * 0.49f, windowTop(ch, p) + WINDOW_H * 0.35f * p)
    /** Checkerboard floor tiles: size in scene pixels and the two colours. */
    const val FLOOR_TILE = 16f
    val FLOOR_TILE_A = androidx.compose.ui.graphics.Color(0xFFE6CCB2)
    val FLOOR_TILE_B = androidx.compose.ui.graphics.Color(0xFFC59B76)
    /** Where the windowsill is (the bottom of the window). */
    fun windowSill(ch: Float, p: Float) = windowTop(ch, p) + WINDOW_H * p
    /**
     * Mochi's treat jar sits on top of the fridge, out of reach and clear of the couple (it used
     * to hang on the wall right behind the boy). This is the jar's base centre.
     */
    /** The Thank-You Jar's spot (plan 09, C): bottom centre, on its own little shelf right of the keepsakes. */
    fun thankYouJar(cw: Float, ch: Float, p: Float): Offset {
        val clock = clockCenter(cw, ch, p)
        return Offset(clock.x + 22f * p, clock.y + 16f * p)
    }
    fun treatJar(cw: Float, ch: Float, p: Float) = Offset(cw * 0.88f, floorY(ch) - 64f * p - 5f * p)
}

/**
 * The meadow's cottage, shared by its drawing and its taps. It is drawn [COTTAGE_SCALE] times the
 * scene's pixel so it stands taller than the couple instead of looking like a toy beside them.
 */
object MeadowLayout {
    const val COTTAGE_SCALE = 1.2f
    /** The cottage's centre: its roof (38 cottage pixels each side) starts just inside the left edge. */
    fun cottageX(p: Float) = (38f * COTTAGE_SCALE + 2f) * p
    fun groundY(ch: Float) = ch * 0.67f
    /** The front door's centre (the door is 12 x 20 cottage pixels, 10 in from the left wall). */
    fun cottageDoor(cw: Float, ch: Float, p: Float): Offset {
        val cp = p * COTTAGE_SCALE
        return Offset(cottageX(p) - 30f * cp + 16f * cp, groundY(ch) - 14f * cp)
    }
    /** Where the wind chimes hang from the eaves: left of the front door, clear of the couple. */
    fun windChimes(cw: Float, ch: Float, p: Float): Offset {
        val cp = p * COTTAGE_SCALE
        return Offset(cottageX(p) - 26f * cp, groundY(ch) - 34f * cp - 6f * p)
    }
    /** The top of the chimney, where smoke rises from. */
    fun chimneyTop(cw: Float, ch: Float, p: Float): Offset {
        val cp = p * COTTAGE_SCALE
        return Offset(cottageX(p) - 30f * cp + 11f * cp, groundY(ch) - 54f * cp)
    }
}

/**
 * Where the cozy pixel props (engine/CozyProps) stand in each scene. Drawing and taps both read
 * from here, so a tap always lands on what is drawn. Bottom-centre anchors, canvas pixels.
 */
object CozyPropLayout {
    // Lantern Stroll: a creek along the bottom terrace (the stepping stones cross it), a footbridge
    // over it, and a bicycle leaning on the streetlamp.
    const val CREEK_ROWS = 6
    fun walkCurbY(ch: Float, p: Float) = ch * 0.66f + 30f * p
    fun walkCreekTop(ch: Float, p: Float): Float {
        val curb = walkCurbY(ch, p)
        return curb + (ch - curb) * 0.60f + 3f * p
    }
    fun walkFootbridge(cw: Float, ch: Float, p: Float) = Offset(cw * 0.66f, walkCreekTop(ch, p) + (CREEK_ROWS + 1) * p)
    /** The glowing mushrooms sit in the creek; taps near them stay theirs. */
    fun walkMushrooms(cw: Float) = cw * 0.50f
    fun hitWalkCreek(tap: Offset, cw: Float, ch: Float, p: Float): Boolean {
        val top = walkCreekTop(ch, p)
        return tap.y in (top - 2f * p)..(top + (CREEK_ROWS + 2) * p) && abs(tap.x - walkMushrooms(cw)) > 14f * p
    }
    fun walkBicycle(cw: Float, ch: Float) = Offset(cw * 0.555f, ch * 0.68f)
    fun hitWalkBicycle(tap: Offset, cw: Float, ch: Float, p: Float): Boolean {
        val b = walkBicycle(cw, ch)
        return abs(tap.x - b.x) < 13f * p && tap.y in (b.y - 20f * p)..(b.y + 2f * p)
    }

    // Meadow: a flower fence to the right of the cottage, and a birdhouse far off on the right.
    const val MEADOW_FENCE_WIDTH = 28
    fun meadowFence(cw: Float, ch: Float) = Offset(cw * 0.71f, ch * 0.69f)
    fun hitMeadowFence(tap: Offset, cw: Float, ch: Float, p: Float): Boolean {
        val f = meadowFence(cw, ch)
        return abs(tap.x - f.x) < (MEADOW_FENCE_WIDTH / 2f + 1f) * p && tap.y in (f.y - 17f * p)..(f.y + 2f * p)
    }
    fun meadowFarBirdhouse(cw: Float, ch: Float, p: Float) = Offset(cw * 0.93f, MeadowLayout.groundY(ch) - 2f * p)

    // Sunroom: a hanging basket from the eave between the first two macrame hangers, and a washing
    // line far out in the garden behind the picket fence.
    fun sunroomBasketHook(cw: Float, ch: Float, p: Float) = Offset(cw * 0.27f, ch * 0.20f + 2f * p)
    fun hitSunroomBasket(tap: Offset, cw: Float, ch: Float, p: Float): Boolean {
        val hook = sunroomBasketHook(cw, ch, p)
        return abs(tap.x - hook.x) < 10f * p && tap.y in (hook.y + 3f * p)..(hook.y + 16f * p)
    }
    fun sunroomFarClothesline(cw: Float, ch: Float, p: Float) = Offset(cw * 0.48f, ch * 0.575f - 13f * p)

    // Campfire: a trail signpost far back in the clearing, in front of the pines.
    fun campfireFarSignpost(cw: Float, ch: Float, p: Float) = Offset(cw * 0.79f, ch * 0.50f + 10f * p)

    // Living room: a pet bed and fish toy on the front row, between the knitting corner and the
    // record rack. Mochi's use of them lives with the pet behaviour (FEATURES).
    fun livingRoomFrontRowBottom(ch: Float, p: Float): Float = ch * 0.65f + ch * 0.35f * 0.75f + 13f * p
    fun petBed(cw: Float, ch: Float, p: Float) = Offset(cw * 0.62f, livingRoomFrontRowBottom(ch, p))
    fun fishToy(cw: Float, ch: Float, p: Float) = Offset(cw * 0.495f, livingRoomFrontRowBottom(ch, p))
}

object WeatherLayout {
    /**
     * Where rain puddles form in each outdoor scene (fractions of the scene): on the ground where
     * water would gather, clear of the props. Scenes not listed use the meadow spots.
     */
    fun puddleSpotsFor(scene: SceneType): List<Pair<Float, Float>> = when (scene) {
        // On the cobbled path in front of the stall and along the walk.
        SceneType.MOMO_STALL -> listOf(0.18f to 0.71f, 0.55f to 0.72f, 0.86f to 0.70f)
        SceneType.WALK -> listOf(0.22f to 0.70f, 0.58f to 0.71f, 0.86f to 0.70f)
        // On the road the scooter rides along.
        SceneType.EVENING_RIDE -> listOf(0.20f to 0.86f, 0.62f to 0.90f, 0.88f to 0.84f)
        // On the deck, away from the bench, the cart and Grandpa Bao's crate.
        SceneType.SEASIDE_PIER -> listOf(0.30f to 0.86f, 0.66f to 0.90f, 0.88f to 0.82f)
        // In the grass, clear of the fire ring, the woodpile and the blanket.
        SceneType.CAMPFIRE -> listOf(0.22f to 0.88f, 0.70f to 0.90f, 0.46f to 0.95f)
        else -> com.example.engine.ParticleSystem.PUDDLE_SPOTS
    }

    const val RAINBOW_SECONDS = 40f
    const val SNOWMAN_MAX_STAGE = 4
    /** Snowflakes to catch for each new snowman stage. */
    const val SNOWFLAKES_PER_STAGE = 3

    /** On the horizon, so the arc's feet go down behind the ground, hills and houses. */
    fun rainbowCenter(cw: Float, ch: Float) = Offset(cw * 0.5f, ch * 0.66f)
    fun rainbowOuterRadius(cw: Float) = cw * 0.50f
    fun rainbowBandWidth(cw: Float) = cw * 0.09f

    fun isOnRainbow(tap: Offset, cw: Float, ch: Float): Boolean {
        val c = rainbowCenter(cw, ch)
        if (tap.y > c.y) return false
        val d = hypot(tap.x - c.x, tap.y - c.y)
        val outer = rainbowOuterRadius(cw)
        return d in (outer - rainbowBandWidth(cw))..outer
    }

    /** The snowman stands in the bottom-left foreground, clear of the couple and the scene props. */
    fun snowmanBase(cw: Float, ch: Float) = Offset(cw * 0.10f, ch * 0.90f)

    fun snowmanHeight(p: Float, stage: Int): Float = when (stage) {
        0 -> 0f
        1 -> 14f * p
        2 -> 22f * p
        else -> 29f * p
    }

    fun isOnSnowman(tap: Offset, cw: Float, ch: Float, p: Float, stage: Int): Boolean {
        if (stage <= 0) return false
        val base = snowmanBase(cw, ch)
        return abs(tap.x - base.x) < 12f * p && tap.y in (base.y - snowmanHeight(p, stage) - 4f * p)..(base.y + 2f * p)
    }

    /** Visual unit for weather on the ground and in the air: about 1/150 of the canvas width. */
    fun weatherUnit(cw: Float, p: Float) = maxOf(p, cw / 150f)

    /** How close a tap must land to a falling snowflake, petal, leaf or dandelion puff to catch it. */
    fun catchRadius(cw: Float, p: Float) = maxOf(8f * p, cw * 0.032f)
}
