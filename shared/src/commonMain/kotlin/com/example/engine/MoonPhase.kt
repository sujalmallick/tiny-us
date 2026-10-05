package com.example.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.time.Clock

/**
 * Tonight's moon, worked out from the date alone (no internet): the moon goes round its phases
 * every synodic month, counted from a known new moon. Good to within about a day, which is plenty
 * for a pixel moon.
 */
object MoonPhase {
    /** Average length of the lunar phase cycle, in days. */
    const val SYNODIC_DAYS = 29.530588853

    /** A known new moon: 6 January 2000, 18:14 UTC. */
    private const val REFERENCE_NEW_MOON_MS = 947_182_440_000L

    private const val DAY_MS = 86_400_000.0

    /** Pins the phase (0 new, 0.5 full) instead of tonight's; for previews and tests only. */
    var override: Float? = null

    /** Tonight's phase, or [override] when a preview pins it. */
    fun current(): Float = override ?: fraction()

    /**
     * Where in the cycle the moon is at [epochMs]: 0 is new, 0.25 first quarter, 0.5 full,
     * 0.75 last quarter, back to new at 1.
     */
    fun fraction(epochMs: Long = Clock.System.now().toEpochMilliseconds()): Float {
        val days = (epochMs - REFERENCE_NEW_MOON_MS) / DAY_MS
        val f = (days / SYNODIC_DAYS) % 1.0
        return (if (f < 0) f + 1.0 else f).toFloat()
    }

    /** How much of the disc is lit, 0 (new) to 1 (full). */
    fun illumination(fraction: Float): Float = ((1.0 - cos(2.0 * PI * fraction)) / 2.0).toFloat()

    /**
     * Across one row of the disc, with half-width [halfWidth]: the stretch that's in shadow, as
     * offsets from the disc's centre (left, right). The lit side is the right while waxing and the
     * left while waning, as seen from the northern hemisphere. Returns null when the row is fully lit.
     */
    fun shadowSpan(fraction: Float, halfWidth: Float): Pair<Float, Float>? {
        val terminator = halfWidth * cos(2.0 * PI * fraction).toFloat()
        return if (fraction < 0.5f) {
            // Waxing: dark from the left edge to the terminator.
            if (terminator <= -halfWidth) null else -halfWidth to terminator
        } else {
            // Waning: dark from the mirrored terminator to the right edge.
            if (-terminator >= halfWidth) null else -terminator to halfWidth
        }
    }
}
