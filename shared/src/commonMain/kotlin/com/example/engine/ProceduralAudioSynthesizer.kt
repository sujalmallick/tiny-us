package com.example.engine

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural waveform synthesis engine.
 * Generates raw PCM audio samples purely through mathematical formulas.
 */
object ProceduralAudioSynthesizer {

    /**
     * Synthesizes a warm music-box chime sample buffer using fundamental frequency, harmonics, and exponential decay.
     */
    fun synthesizeChime(
        frequency: Double,
        durationMs: Int,
        sampleRate: Int = 22050,
        volume: Float = 0.8f
    ): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val decayRate = 3.5 / numSamples

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val fundamental = sin(2.0 * PI * frequency * t)
            val harmonic2 = 0.25 * sin(4.0 * PI * frequency * t)
            val harmonic3 = 0.10 * sin(6.0 * PI * frequency * t)
            val envelope = exp(-i * decayRate)
            val sample = (fundamental + harmonic2 + harmonic3) * envelope * volume * Short.MAX_VALUE
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes a soft retro footstep sound effect.
     */
    fun synthesizeFootstep(sampleRate: Int = 22050): ShortArray {
        val numSamples = (sampleRate * 35 / 1000).coerceAtLeast(50)
        val buffer = ShortArray(numSamples)
        var last = 0f
        for (i in 0 until numSamples) {
            val white = (Random.nextDouble() * 2.0 - 1.0).toFloat()
            last = (last + (0.05f * white)) / 1.05f
            val env = 1.0 - (i.toDouble() / numSamples)
            buffer[i] = (last * env * 0.16f * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
}
