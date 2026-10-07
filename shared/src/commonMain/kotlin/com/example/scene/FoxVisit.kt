package com.example.scene

/**
 * The Friday fox's visit (plan 10, D), in world fractions: it trots in from one side with a ball
 * in its mouth, stops near Mochi, and the ball goes back and forth between them in little arcs.
 * Then it catches the ball one last time and trots off the way it came. The engine drives it and
 * reacts to its [Event]s; the renderer reads it.
 */
class FoxVisit {
    enum class Phase { NONE, ARRIVING, PLAYING, LEAVING }
    enum class Event { ARRIVED, BALL_TO_MOCHI, BALL_TO_FOX, LEAVING, GONE }

    var phase = Phase.NONE
        private set
    var x = 0f
        private set
    var y = 0f
        private set
    var facingLeft = false
        private set
    /** Seconds since the visit began, for the trot and the tail wag. */
    var age = 0f
        private set

    /** Where the ball flies from and to (world x), and how far along it is (0..1) while in flight. */
    var ballFrom = 0f
        private set
    var ballTo = 0f
        private set
    var ballT = 0f
        private set
    /** True while the ball is in the air; otherwise it's in the fox's mouth or at Mochi's paws. */
    var ballFlying = false
        private set
    /** True while Mochi has it, between her catch and her bat back. */
    var ballWithMochi = false
        private set

    /** A little hop when the fox is tapped. */
    var hop = 0f

    private var fromRight = true
    private var phaseTime = 0f
    private var ballToMochi = false

    /** Where Mochi is (world x); the engine keeps it current, and the fox plays wherever she is. */
    var mochiX = 0f

    private val playX: Float get() = (if (fromRight) mochiX + PLAY_GAP else mochiX - PLAY_GAP).coerceIn(0.06f, 0.94f)

    val active: Boolean get() = phase != Phase.NONE

    /** Comes in from the side with more room beside Mochi, on her ground line. */
    fun start(groundY: Float, mochiX: Float) {
        this.mochiX = mochiX
        fromRight = mochiX < 0.5f
        phase = Phase.ARRIVING
        x = if (fromRight) 1.08f else -0.08f
        y = groundY
        facingLeft = fromRight
        age = 0f
        phaseTime = 0f
        ballFlying = false
        ballWithMochi = false
    }

    fun stop() {
        phase = Phase.NONE
        ballFlying = false
        ballWithMochi = false
    }

    /** Advances the visit; returns what just happened, if anything. */
    fun update(dt: Float): Event? {
        if (phase == Phase.NONE) return null
        age += dt
        phaseTime += dt
        if (hop > 0f) hop = (hop - dt).coerceAtLeast(0f)
        return when (phase) {
            Phase.ARRIVING -> {
                if (trotTowards(playX, dt)) {
                    phase = Phase.PLAYING
                    phaseTime = 0f
                    facingLeft = mochiX < x
                    throwBall(toMochi = true)
                    Event.ARRIVED
                } else null
            }
            Phase.PLAYING -> {
                if (ballWithMochi) {
                    // She holds it a moment, then bats it back
                    if (phaseTime >= HOLD_SECONDS) {
                        ballWithMochi = false
                        throwBall(toMochi = false)
                    }
                    return null
                }
                if (!ballFlying) return null
                ballT += dt / THROW_SECONDS
                if (ballT < 1f) return null
                ballFlying = false
                phaseTime = 0f
                if (ballToMochi) {
                    ballWithMochi = true
                    Event.BALL_TO_MOCHI
                } else if (age >= PLAY_SECONDS) {
                    // Back in its mouth for the last time: off it goes
                    phase = Phase.LEAVING
                    facingLeft = !fromRight
                    Event.LEAVING
                } else {
                    throwBall(toMochi = true)
                    Event.BALL_TO_FOX
                }
            }
            Phase.LEAVING -> {
                val exit = if (fromRight) 1.1f else -0.1f
                if (trotTowards(exit, dt)) {
                    phase = Phase.NONE
                    Event.GONE
                } else null
            }
            Phase.NONE -> null
        }
    }

    private fun throwBall(toMochi: Boolean) {
        ballToMochi = toMochi
        ballFrom = if (toMochi) x else mochiX
        ballTo = if (toMochi) mochiX else x
        ballT = 0f
        ballFlying = true
        phaseTime = 0f
    }

    /** Moves towards [target]; true on arrival. */
    private fun trotTowards(target: Float, dt: Float): Boolean {
        val step = TROT_SPEED * dt
        if (kotlin.math.abs(target - x) <= step) {
            x = target
            return true
        }
        x += if (target > x) step else -step
        facingLeft = target < x
        return false
    }

    companion object {
        const val TROT_SPEED = 0.16f
        const val PLAY_GAP = 0.2f
        const val THROW_SECONDS = 0.9f
        const val HOLD_SECONDS = 0.6f
        /** How long they play before the fox heads home. */
        const val PLAY_SECONDS = 22f
    }
}
