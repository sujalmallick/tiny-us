package com.example.scene

/**
 * The street painter's visit (plan 11): walks in, sets up an easel at the edge of the scene, paints
 * the two of them for a while, turns the canvas round, and wanders off. World fractions.
 */
class PainterVisit {
    enum class Phase { NONE, ARRIVING, PAINTING, REVEAL, LEAVING }
    enum class Event { ARRIVED, FINISHED, GONE }

    var phase = Phase.NONE
        private set
    var x = 0f
        private set
    var y = 0f
        private set
    /** Where the easel stands. */
    var easelX = 0f
        private set
    var age = 0f
        private set
    private var phaseTime = 0f

    val active: Boolean get() = phase != Phase.NONE
    val easelUp: Boolean get() = phase == Phase.PAINTING || phase == Phase.REVEAL

    /** How far along the painting is, 0..1. */
    val progress: Float
        get() = when (phase) {
            Phase.PAINTING -> (phaseTime / PAINT_SECONDS).coerceIn(0f, 1f)
            Phase.REVEAL, Phase.LEAVING -> 1f
            else -> 0f
        }

    /** True while the canvas is turned round to show them. */
    val showing: Boolean get() = phase == Phase.REVEAL

    fun start(easelX: Float, groundY: Float) {
        this.easelX = easelX
        x = 1.08f
        y = groundY
        phase = Phase.ARRIVING
        age = 0f
        phaseTime = 0f
    }

    fun stop() {
        phase = Phase.NONE
    }

    fun update(dt: Float): Event? {
        if (phase == Phase.NONE) return null
        age += dt
        phaseTime += dt
        return when (phase) {
            Phase.ARRIVING -> {
                val standX = easelX + STAND_GAP
                x = (x - WALK_SPEED * dt).coerceAtLeast(standX)
                if (x <= standX) { phase = Phase.PAINTING; phaseTime = 0f; Event.ARRIVED } else null
            }
            Phase.PAINTING -> if (phaseTime >= PAINT_SECONDS) { phase = Phase.REVEAL; phaseTime = 0f; Event.FINISHED } else null
            Phase.REVEAL -> { if (phaseTime >= REVEAL_SECONDS) { phase = Phase.LEAVING; phaseTime = 0f }; null }
            Phase.LEAVING -> {
                x += WALK_SPEED * dt
                if (x > 1.1f) { phase = Phase.NONE; Event.GONE } else null
            }
            Phase.NONE -> null
        }
    }

    /** Facing the couple (left) while painting; the way they're walking otherwise. */
    val facingLeft: Boolean get() = phase != Phase.LEAVING

    companion object {
        const val WALK_SPEED = 0.08f
        const val STAND_GAP = 0.07f
        const val PAINT_SECONDS = 45f
        const val REVEAL_SECONDS = 8f
    }
}

/**
 * Billionaire and The Great on the pier (plan 11): they stroll in, sit on their bench, and do what
 * the two of them do a moment later. After a while they stroll off; on the anniversary they leave
 * a note under a pebble on the bench.
 */
class OldCoupleVisit {
    enum class Phase { NONE, ARRIVING, SITTING, LEAVING }
    enum class Mood { APART, HOLD_HANDS, SNUGGLE, HUG, SNACK }

    var phase = Phase.NONE
        private set
    /** How far along the deck they are (world x of the middle of the pair). */
    var x = 0f
        private set
    var age = 0f
        private set
    var anniversary = false
        private set
    /** The note is lying on the bench, waiting to be picked up. */
    var noteWaiting = false
    /** What they're doing: the couple's mood from [MIRROR_DELAY] seconds ago. */
    var mood = Mood.APART
        private set
    /** Which of his lines, and of hers, comes next when one of them is tapped. */
    var hisNext = 0
        private set
    var herNext = 0
        private set

    private var phaseTime = 0f
    private val history = ArrayDeque<Pair<Float, Mood>>()

    val active: Boolean get() = phase != Phase.NONE

    fun start(anniversary: Boolean) {
        this.anniversary = anniversary
        phase = Phase.ARRIVING
        x = -0.1f
        age = 0f
        phaseTime = 0f
        mood = Mood.APART
        history.clear()
    }

    fun stop() {
        phase = Phase.NONE
        history.clear()
    }

    /** He said a line; next time, his next one. */
    fun saidHis() {
        hisNext = (hisNext + 1) % LINES_EACH
    }

    /** She said a line; next time, her next one. */
    fun saidHers() {
        herNext = (herNext + 1) % LINES_EACH
    }

    /** Advances the visit, watching the couple's [coupleMood]; returns true the moment they leave the note. */
    fun update(dt: Float, coupleMood: Mood): Boolean {
        if (phase == Phase.NONE) return false
        age += dt
        phaseTime += dt
        history.addLast(age to coupleMood)
        while (history.size > 1 && history[1].first <= age - MIRROR_DELAY) history.removeFirst()
        when (phase) {
            Phase.ARRIVING -> {
                x = (x + STROLL_SPEED * dt).coerceAtMost(BENCH_X)
                if (x >= BENCH_X) { phase = Phase.SITTING; phaseTime = 0f }
            }
            Phase.SITTING -> {
                // Mirroring, a beat behind
                if (history.first().first <= age - MIRROR_DELAY) mood = history.first().second
                if (phaseTime >= if (anniversary) ANNIVERSARY_STAY else STAY_SECONDS) {
                    phase = Phase.LEAVING
                    phaseTime = 0f
                    mood = Mood.APART
                    if (anniversary) {
                        noteWaiting = true
                        return true
                    }
                }
            }
            Phase.LEAVING -> {
                x -= STROLL_SPEED * dt
                if (x < -0.12f) phase = Phase.NONE
            }
            Phase.NONE -> Unit
        }
        return false
    }

    companion object {
        const val BENCH_X = 0.2f
        const val BENCH_Y = 0.86f
        const val STROLL_SPEED = 0.05f
        const val MIRROR_DELAY = 1.5f
        const val STAY_SECONDS = 100f
        const val ANNIVERSARY_STAY = 45f
        const val LINES_EACH = 3
    }
}
