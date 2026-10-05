package com.example.games

import com.example.R
import com.example.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * A constellation to connect (plan 07, C2). [stars] are positions as shares of the stage (x across,
 * y down) before the sky's slow turn; [closed] joins the last star back to the first.
 */
class Constellation(
    val id: String,
    val name: StringResource,
    val story: StringResource,
    val stars: List<Pair<Float, Float>>,
    val closed: Boolean
)

object Constellations {
    val ALL = listOf(
        // The three the night sky always had (the bright stars are part of the star field).
        Constellation("constellation_1", Res.string.star_two_hearts, Res.string.star_two_hearts_story,
            listOf(0.14f to 0.11f, 0.17f to 0.08f, 0.20f to 0.16f, 0.23f to 0.22f, 0.11f to 0.18f), closed = true),
        Constellation("constellation_2", Res.string.star_teapot, Res.string.star_teapot_story,
            listOf(0.36f to 0.09f, 0.49f to 0.12f, 0.47f to 0.19f, 0.40f to 0.20f, 0.34f to 0.17f), closed = true),
        Constellation("constellation_3", Res.string.star_trail, Res.string.star_trail_story,
            listOf(0.62f to 0.10f, 0.68f to 0.08f, 0.73f to 0.12f, 0.82f to 0.11f, 0.89f to 0.14f), closed = false),
        // Three more, lower in the sky.
        Constellation("constellation_4", Res.string.star_whiskers, Res.string.star_whiskers_story,
            listOf(0.10f to 0.30f, 0.14f to 0.26f, 0.18f to 0.30f, 0.17f to 0.34f, 0.11f to 0.34f), closed = true),
        Constellation("constellation_5", Res.string.star_scooter, Res.string.star_scooter_story,
            listOf(0.40f to 0.31f, 0.46f to 0.27f, 0.52f to 0.27f, 0.56f to 0.32f, 0.45f to 0.34f), closed = false),
        Constellation("constellation_6", Res.string.star_kite, Res.string.star_kite_story,
            listOf(0.74f to 0.24f, 0.80f to 0.29f, 0.74f to 0.35f, 0.68f to 0.29f), closed = true)
    )

    /** The constellation whose patch of sky holds the point [x], [y] (shares, before the turn). */
    fun at(x: Float, y: Float): Constellation? = ALL.firstOrNull { c ->
        x in (c.stars.minOf { it.first } - PAD)..(c.stars.maxOf { it.first } + PAD) &&
            y in (c.stars.minOf { it.second } - PAD)..(c.stars.maxOf { it.second } + PAD)
    }

    private const val PAD = 0.035f
}

/**
 * Connecting a constellation's stars in order. Plain Kotlin; nothing can be lost: a wrong tap just
 * makes the star wiggle. Positions come in already turned with the sky ([starX] takes the drift).
 */
class StarPuzzle {
    enum class Tap { CONNECTED, WRONG, DONE, MISSED }

    var current: Constellation? = null
        private set
    /** How many stars are connected so far. */
    var connected = 0
        private set
    /** The constellation that just glowed on completion, and for how long more. */
    var glowing: Constellation? = null
        private set
    var glowTimer = 0f
        private set
    /** A gentle wiggle on the star tapped out of order. */
    var wiggleStar = -1
        private set
    var wiggleTimer = 0f
        private set

    fun start(c: Constellation) {
        current = c
        connected = 0
        wiggleStar = -1
    }

    fun stop() {
        current = null
        connected = 0
    }

    /** A constellation already found just glows again. */
    fun glow(c: Constellation) {
        glowing = c
        glowTimer = GLOW_SECONDS
    }

    /** Where star [i] of [c] is across the stage, after the sky has turned by [drift]. */
    fun starX(c: Constellation, i: Int, drift: Float): Float {
        val x = c.stars[i].first - drift
        return if (x < 0f) x + 1f else x
    }

    /**
     * A tap at ([x], [y]) in world units on a [cw] x [ch] stage. Connects the next star when it's
     * close, wiggles another star of the constellation, and finishes after the last one.
     */
    fun tap(x: Float, y: Float, cw: Float, ch: Float, drift: Float): Tap {
        val c = current ?: return Tap.MISSED
        val radius = HIT_RADIUS * cw
        fun dist(i: Int) = kotlin.math.hypot(starX(c, i, drift) * cw - x, c.stars[i].second * ch - y)
        if (dist(connected) <= radius) {
            connected++
            if (connected == c.stars.size) {
                glow(c)
                stop()
                return Tap.DONE
            }
            return Tap.CONNECTED
        }
        val other = c.stars.indices.firstOrNull { it != connected && dist(it) <= radius }
        if (other != null) {
            wiggleStar = other
            wiggleTimer = WIGGLE_SECONDS
            return Tap.WRONG
        }
        return Tap.MISSED
    }

    fun update(dt: Float) {
        if (glowTimer > 0f) glowTimer = (glowTimer - dt).coerceAtLeast(0f)
        if (glowTimer == 0f) glowing = null
        if (wiggleTimer > 0f) wiggleTimer = (wiggleTimer - dt).coerceAtLeast(0f)
    }

    companion object {
        /** How close a tap must be to a star, as a share of the stage width. */
        const val HIT_RADIUS = 0.05f
        const val GLOW_SECONDS = 3f
        const val WIGGLE_SECONDS = 0.5f
    }
}
