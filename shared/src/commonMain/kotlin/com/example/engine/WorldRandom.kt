package com.example.engine

import kotlin.random.Random

/**
 * The one source of randomness behind the world's moving parts: birds, weather and ambient
 * particles. It defaults to [Random.Default], so the game plays exactly as before. Previews and
 * tests swap in a seeded [Random] (via [seed]) so the same scene at the same moment renders
 * identically every run, which is what a pixel-by-pixel before/after comparison needs.
 */
object WorldRandom {
    var rng: Random = Random.Default

    fun seed(value: Long) {
        rng = Random(value)
    }

    fun reset() {
        rng = Random.Default
    }
}
