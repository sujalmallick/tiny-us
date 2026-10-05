package com.example.games

import kotlin.random.Random

/**
 * "Catch together" (plan 07, C1): a 30-second round of catching whatever the weather drops in a
 * basket the player slides along the ground. Rare golden stars count three. It can't be lost; it
 * just ends with a score. Positions are shares of the stage (0..1), so the rules don't care about
 * screen size. Plain Kotlin: the engine feeds it time and asks what the basket caught.
 */
class CatchGame(private val random: Random = Random.Default) {

    /** A golden star falling toward the basket. */
    class Golden(var x: Float, var y: Float, val speed: Float)

    var active = false
        private set
    var timeLeft = 0f
        private set
    var score = 0
        private set

    /** The basket's centre across the stage, easing toward [targetX]. */
    var basketX = 0.5f
        private set
    var targetX = 0.5f
        private set

    val goldens = mutableListOf<Golden>()
    private var goldenTimer = 0f

    fun start() {
        active = true
        timeLeft = DURATION_SECONDS
        score = 0
        basketX = 0.5f
        targetX = 0.5f
        goldens.clear()
        goldenTimer = nextGoldenDelay()
    }

    /** Where the player's finger is, across the stage (0..1). */
    fun moveTo(x: Float) {
        targetX = x.coerceIn(HALF_WIDTH, 1f - HALF_WIDTH)
    }

    /** Things caught by the basket this frame (from the falling weather): each counts one. */
    fun addCatches(count: Int) {
        if (active && count > 0) score += count
    }

    /**
     * Moves time on by [dt] seconds. Returns true on the frame the round ends.
     */
    fun update(dt: Float): Boolean {
        if (!active) return false
        // The basket glides after the finger rather than teleporting.
        val step = BASKET_SPEED * dt
        basketX += (targetX - basketX).coerceIn(-step, step)

        goldenTimer -= dt
        if (goldenTimer <= 0f) {
            goldens += Golden(random.nextFloat() * 0.8f + 0.1f, TOP, 0.18f + random.nextFloat() * 0.08f)
            goldenTimer = nextGoldenDelay()
        }
        val it = goldens.iterator()
        while (it.hasNext()) {
            val g = it.next()
            g.y += g.speed * dt
            if (g.y >= RIM_Y && g.y - g.speed * dt < RIM_Y + RIM_DEPTH && inBasket(g.x)) {
                score += GOLDEN_VALUE
                it.remove()
            } else if (g.y > 1f) {
                it.remove()
            }
        }

        timeLeft -= dt
        if (timeLeft <= 0f) {
            timeLeft = 0f
            active = false
            goldens.clear()
            return true
        }
        return false
    }

    /** Whether something at [x] across the stage is over the basket's opening. */
    fun inBasket(x: Float) = kotlin.math.abs(x - basketX) <= HALF_WIDTH

    private fun nextGoldenDelay() = 4f + random.nextFloat() * 4f

    companion object {
        const val DURATION_SECONDS = 30f
        /** Half the basket's width, as a share of the stage. */
        const val HALF_WIDTH = 0.09f
        /** The basket's rim height, as a share of the stage (just above most ground lines). */
        const val RIM_Y = 0.72f
        const val RIM_DEPTH = 0.03f
        const val GOLDEN_VALUE = 3
        /** Stage widths per second. */
        const val BASKET_SPEED = 1.6f
        const val TOP = 0.05f
        /** Scores worth a little first. */
        const val GOOD_SCORE = 30
    }
}
