package com.example.scene

import com.example.data.PetKind

/**
 * A pet they haven't met yet, out in the world for a moment (plan 10, E): a bunny in the meadow,
 * a hedgehog under the tree, a duck and her ducklings on the walk in the rain, an owl on the hill
 * at night. It sits about (the duck waddles to and fro) until it's tapped; then it's met, gives a
 * happy hop, and wanders off.
 */
class PetVisitor(val kind: PetKind, startX: Float, val y: Float) {
    var x = startX
        private set
    var facingLeft = startX > 0.5f
        private set
    var age = 0f
        private set
    /** Seconds since it was met, or -1 while it's still waiting. */
    var metFor = -1f
        private set
    val met: Boolean get() = metFor >= 0f
    val gone: Boolean get() = met && (x < -0.1f || x > 1.1f)

    /** How it's drawn right now. */
    val state: CatState
        get() = when {
            met && metFor < HAPPY_SECONDS -> CatState.PLAYFUL_POUNCE
            met || kind == PetKind.DUCK -> CatState.WALK_FOLLOW
            else -> CatState.SITTING_PURR
        }

    private val homeX = startX

    fun meet() {
        if (!met) metFor = 0f
    }

    fun update(dt: Float) {
        age += dt
        if (met) {
            metFor += dt
            if (metFor < HAPPY_SECONDS) return
            // Off it goes, the nearer way out
            facingLeft = homeX < 0.5f
            x += (if (facingLeft) -1f else 1f) * LEAVE_SPEED * dt
            return
        }
        if (kind == PetKind.DUCK) {
            // To and fro along the path with her ducklings
            x += (if (facingLeft) -1f else 1f) * WADDLE_SPEED * dt
            if (x < homeX - WADDLE_RANGE) facingLeft = false
            if (x > homeX + WADDLE_RANGE) facingLeft = true
        }
    }

    companion object {
        const val HAPPY_SECONDS = 2.2f
        const val LEAVE_SPEED = 0.18f
        const val WADDLE_SPEED = 0.03f
        const val WADDLE_RANGE = 0.12f
    }
}
