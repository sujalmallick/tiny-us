package com.example.games

import kotlin.random.Random

/**
 * The moment the line goes in (plan 09, F1): the season (WINTER, SPRING, SUMMER, AUTUMN), the
 * weather's name (RAIN, SNOW...), and whether it's night or sunset. What's biting depends on it.
 */
data class FishingConditions(
    val season: String = "SUMMER",
    val weather: String = "SUNNY",
    val isNight: Boolean = false,
    val isSunset: Boolean = false
)

/**
 * What can come up on the line. [weight] is how often, against the others; a few only bite at
 * certain moments ([weightIn]). The common catches are there whatever the weather.
 */
enum class FishingCatch(val weight: Int, val isFish: Boolean) {
    MINNOW(34, true),
    CARP(24, true),
    SEASHELL(18, false),
    OLD_BOOT(10, false),
    BOTTLE(9, false),
    GOLDEN_FISH(5, true),
    // Seasonal, by weather and time (plan 09, F1).
    MOON_JELLY(14, false),
    GLOW_SQUID(9, false),
    RAIN_TROUT(16, true),
    ICE_COD(14, true),
    BLOSSOM_KOI(12, true),
    // Little treasures he reels up for her (plan 09, F): given to her on the spot.
    PEARL(4, false),
    HEART_SHELL(5, false),
    SEA_GLASS_HEART(4, false);

    /** How often this bites in [c]: 0 when it doesn't come up at all then. */
    fun weightIn(c: FishingConditions): Int = when (this) {
        MOON_JELLY, GLOW_SQUID -> if (c.isNight) weight else 0
        RAIN_TROUT -> if (c.weather == "RAIN") weight else 0
        ICE_COD -> if (c.season == "WINTER") weight else 0
        BLOSSOM_KOI -> if (c.season == "SPRING") weight else 0
        // Most likely at sunset, when the water turns gold.
        GOLDEN_FISH -> if (c.isSunset) weight * 3 else weight
        else -> weight
    }

    /** Only bites at some moments (night, rain, a season). */
    val isSeasonal: Boolean get() = this in SEASONAL

    /** Something lovely that he gives her as soon as it's landed. */
    val isTreasure: Boolean get() = this == PEARL || this == HEART_SHELL || this == SEA_GLASS_HEART

    private companion object {
        val SEASONAL = setOf(MOON_JELLY, GLOW_SQUID, RAIN_TROUT, ICE_COD, BLOSSOM_KOI)
    }
}

/**
 * Fishing (plan 07, C4): the couple casts a line next to Grandpa Bao. After a quiet wait the bobber
 * dips; a tap while it's down (a forgiving window) reels something in. Tapping early just jiggles
 * the bobber, and a missed bite swims off and comes back: there's no losing, only waiting a little
 * longer. Plain Kotlin: the engine feeds it taps and time, and records the catch [update] returns.
 */
class FishingGame(private val random: Random = Random.Default) {

    enum class Phase { IDLE, WAITING, BITE, REELING, LANDED }

    enum class TapResult {
        /** Tapped during a bite: reeling in. */
        HOOKED,
        /** Tapped before a bite: the bobber jiggles, nothing else happens. */
        TOO_SOON,
        /** Not fishing, or already reeling. */
        IGNORED
    }

    var phase = Phase.IDLE
        private set
    /** Seconds left in this phase (the wait, the bite window, the reel or the show). */
    var timer = 0f
        private set
    /** What was reeled in, from REELING until the next cast. */
    var caught: FishingCatch? = null
        private set
    /** Bites that swam off this cast (each one makes the next wait a little shorter). */
    var missedBites = 0
        private set

    private var guaranteeFish = false
    private var conditions = FishingConditions()
    /** What the next bite brings, whatever the odds (a message in a bottle that's due). */
    private var forced: FishingCatch? = null

    /** Makes the next bite bring [catch] (a waiting message in a bottle), once. */
    fun forceNext(catch: FishingCatch) {
        forced = catch
    }

    /** The practice cast in Bao's lesson (plan 09, F2): a bite comes soon and waits longer. */
    var practice = false
        private set

    /**
     * Casts the line in [conditions]. [firstEver] makes sure the very first catch is a fish (the
     * "first fish" first); [practice] is Bao's lesson: a quick bite with a wider window.
     */
    fun cast(firstEver: Boolean = false, conditions: FishingConditions = FishingConditions(), practice: Boolean = false) {
        guaranteeFish = firstEver
        this.conditions = conditions
        this.practice = practice
        phase = Phase.WAITING
        caught = null
        missedBites = 0
        timer = if (practice) PRACTICE_WAIT_SECONDS else nextWait()
    }

    fun tap(): TapResult = when (phase) {
        Phase.BITE -> {
            phase = Phase.REELING
            timer = REEL_SECONDS
            caught = forced ?: roll()
            forced = null
            TapResult.HOOKED
        }
        Phase.WAITING -> TapResult.TOO_SOON
        else -> TapResult.IGNORED
    }

    /** Moves time on by [dt] seconds. Returns the catch on the frame it lands, otherwise null. */
    fun update(dt: Float): FishingCatch? {
        if (phase == Phase.IDLE) return null
        timer -= dt
        if (timer > 0f) return null
        when (phase) {
            Phase.WAITING -> {
                phase = Phase.BITE
                timer = if (practice) PRACTICE_WINDOW_SECONDS else BITE_WINDOW_SECONDS
            }
            Phase.BITE -> {
                // It got away, but another one comes along, a little sooner each time.
                missedBites++
                phase = Phase.WAITING
                timer = nextWait()
            }
            Phase.REELING -> {
                phase = Phase.LANDED
                timer = LANDED_SECONDS
                return caught
            }
            Phase.LANDED -> stop()
            Phase.IDLE -> Unit
        }
        return null
    }

    /** Reels the line in (also fine mid-wait: nothing is lost). */
    fun stop() {
        phase = Phase.IDLE
        timer = 0f
        caught = null
        missedBites = 0
    }

    /** How far the bobber is pulled under (0..1), for the drawing. */
    val dip: Float
        get() = when (phase) {
            Phase.BITE -> 1f
            Phase.REELING -> (timer / REEL_SECONDS).coerceIn(0f, 1f)
            else -> 0f
        }

    private fun nextWait(): Float {
        // Never shorter than a second, so there's always a moment of quiet.
        val shorter = (missedBites * 0.5f).coerceAtMost(MIN_WAIT_SECONDS - 1f)
        return MIN_WAIT_SECONDS + random.nextFloat() * (MAX_WAIT_SECONDS - MIN_WAIT_SECONDS) - shorter
    }

    private fun roll(): FishingCatch {
        val pool = FishingCatch.entries.filter { (!guaranteeFish || it.isFish) && it.weightIn(conditions) > 0 }
        var pick = random.nextInt(pool.sumOf { it.weightIn(conditions) })
        for (c in pool) {
            val w = c.weightIn(conditions)
            if (pick < w) return c
            pick -= w
        }
        return pool.last()
    }

    companion object {
        const val MIN_WAIT_SECONDS = 2.5f
        const val MAX_WAIT_SECONDS = 7f
        /** How long the bobber stays down: long enough for anyone. */
        const val BITE_WINDOW_SECONDS = 1.4f
        /** Bao's practice cast: a bite after a short wait, and twice as long to answer it. */
        const val PRACTICE_WAIT_SECONDS = 2f
        const val PRACTICE_WINDOW_SECONDS = 2.8f
        const val REEL_SECONDS = 1.2f
        /** How long the catch is shown before the line is put away. */
        const val LANDED_SECONDS = 3f
    }
}
