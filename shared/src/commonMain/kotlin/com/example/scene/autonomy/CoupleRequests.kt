package com.example.scene.autonomy

import kotlin.random.Random

/** Small things one of them asks for now and then (plan 07 D1). Each is granted by tapping a prop. */
enum class RequestKind { WARM, TEA, SONG, SNACK, MOCHI }

/**
 * The couple's requests: at most one at a time, rare, and never pressing. A request lasts
 * [WINDOW] seconds, then fades without a word if nobody grants it. The engine decides which
 * kinds fit the moment and plays the reactions; this class only keeps the timing.
 */
class CoupleRequests {
    var kind: RequestKind = RequestKind.WARM
        private set
    /** True when the girl is asking, false for the boy. */
    var askerIsGirl: Boolean = false
        private set
    /** Seconds before the request fades; 0 when there is none. */
    var secondsLeft: Float = 0f
        private set
    /** Seconds before another request may start (shared by the couple). */
    var cooldown: Float = FIRST_DELAY
        private set
    /** Seconds of quiet after a cinematic, before a request may start. */
    var quiet: Float = 0f
        private set
    private var lastKind: RequestKind? = null

    val active: Boolean get() = secondsLeft > 0f

    /** How visible the bubble is: 1 until the last [FADE_SECONDS], then down to 0. */
    val fade: Float get() = if (!active) 0f else (secondsLeft / FADE_SECONDS).coerceIn(0f, 1f)

    /** A new scene: nothing pending, and none in its first [FIRST_DELAY] seconds. */
    fun reset() {
        secondsLeft = 0f
        cooldown = FIRST_DELAY
        quiet = 0f
    }

    /** Advances the clocks; returns true when an unanswered request has just faded. */
    fun tick(dt: Float): Boolean {
        if (quiet > 0f) quiet = (quiet - dt).coerceAtLeast(0f)
        if (active) {
            secondsLeft -= dt
            if (secondsLeft <= 0f) {
                secondsLeft = 0f
                cooldown = IGNORED_GAP
                return true
            }
            return false
        }
        if (cooldown > 0f) cooldown = (cooldown - dt).coerceAtLeast(0f)
        return false
    }

    /** True when a new request may start now (some kind still has to fit). */
    val canStart: Boolean get() = !active && cooldown <= 0f && quiet <= 0f

    /**
     * Picks a kind that [fits], leaning toward each one's usual asker (the girl for a song,
     * the boy for a snack) and away from the last kind asked. Null if nothing fits.
     */
    fun pickKind(askerIsGirl: Boolean, random: Random, fits: (RequestKind) -> Boolean): RequestKind? {
        var total = 0f
        for (k in RequestKind.entries) if (fits(k)) total += weight(k, askerIsGirl)
        if (total <= 0f) return null
        var roll = random.nextFloat() * total
        var chosen: RequestKind? = null
        for (k in RequestKind.entries) {
            if (!fits(k)) continue
            val w = weight(k, askerIsGirl)
            chosen = k
            if (roll < w) break
            roll -= w
        }
        return chosen
    }

    private fun weight(k: RequestKind, askerIsGirl: Boolean): Float {
        var w = 1f
        if (k == RequestKind.SONG && askerIsGirl) w *= 2f
        if (k == RequestKind.SNACK && !askerIsGirl) w *= 2f
        if (k == lastKind) w *= 0.25f
        return w
    }

    fun start(kind: RequestKind, askerIsGirl: Boolean) {
        this.kind = kind
        this.askerIsGirl = askerIsGirl
        lastKind = kind
        secondsLeft = WINDOW
    }

    /** The player tapped the prop for [kind]: true if that answers the request. */
    fun grant(kind: RequestKind, random: Random): Boolean {
        if (!active || this.kind != kind) return false
        secondsLeft = 0f
        cooldown = GAP_MIN + random.nextFloat() * GAP_RANGE
        return true
    }

    /** A cinematic or a mini-game took over: drop the request quietly and keep a quiet spell. */
    fun interrupt(random: Random) {
        if (active) {
            secondsLeft = 0f
            cooldown = maxOf(cooldown, GAP_MIN + random.nextFloat() * GAP_RANGE)
        }
        quiet = QUIET_AFTER_CINEMATIC
    }

    /** For tests and previews: start [kind] at once, ignoring the timing. */
    fun forceStart(kind: RequestKind, askerIsGirl: Boolean) {
        cooldown = 0f
        quiet = 0f
        start(kind, askerIsGirl)
    }

    companion object {
        /** How long a request waits to be granted. */
        const val WINDOW = 25f
        /** The bubble fades over this many final seconds. */
        const val FADE_SECONDS = 5f
        /** None in a scene's first minute and a half. */
        const val FIRST_DELAY = 90f
        const val GAP_MIN = 240f
        const val GAP_RANGE = 120f
        /** A longer wait after a request nobody answered. */
        const val IGNORED_GAP = 480f
        const val QUIET_AFTER_CINEMATIC = 20f
    }
}
