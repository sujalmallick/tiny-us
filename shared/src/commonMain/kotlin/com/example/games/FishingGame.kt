package com.example.games

import kotlin.random.Random

/** What can come up on the line. [weight] is how often, against the others. */
enum class FishingCatch(val weight: Int, val isFish: Boolean) {
    MINNOW(34, true),
    CARP(24, true),
    SEASHELL(18, false),
    OLD_BOOT(10, false),
    BOTTLE(9, false),
    GOLDEN_FISH(5, true)
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

    /** Casts the line. [firstEver] makes sure the very first catch is a fish (the "first fish" first). */
    fun cast(firstEver: Boolean = false) {
        guaranteeFish = firstEver
        phase = Phase.WAITING
        caught = null
        missedBites = 0
        timer = nextWait()
    }

    fun tap(): TapResult = when (phase) {
        Phase.BITE -> {
            phase = Phase.REELING
            timer = REEL_SECONDS
            caught = roll()
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
                timer = BITE_WINDOW_SECONDS
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
        val pool = FishingCatch.entries.filter { !guaranteeFish || it.isFish }
        var pick = random.nextInt(pool.sumOf { it.weight })
        for (c in pool) {
            if (pick < c.weight) return c
            pick -= c.weight
        }
        return pool.last()
    }

    companion object {
        const val MIN_WAIT_SECONDS = 2.5f
        const val MAX_WAIT_SECONDS = 7f
        /** How long the bobber stays down: long enough for anyone. */
        const val BITE_WINDOW_SECONDS = 1.4f
        const val REEL_SECONDS = 1.2f
        /** How long the catch is shown before the line is put away. */
        const val LANDED_SECONDS = 3f
    }
}
