package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * The world's synthesized sounds as 16-bit mono PCM: music-box and plain tones, soft noise, thunder
 * and the rain loop. Android plays the buffers through AudioTrack, iOS through AVAudioPlayer, so
 * both platforms hear the same sounds.
 */
object ToneSynth {
    const val SAMPLE_RATE = 22050

    /** Samples in [durationMs] of sound; never shorter than [minSamples]. */
    fun samplesFor(durationMs: Int, minSamples: Int = 64, sampleRate: Int = SAMPLE_RATE): Int =
        (sampleRate * durationMs / 1000).coerceAtLeast(minSamples)

    /**
     * A tone with a click-free envelope (cosine attack, exponential ring, cosine release). The music
     * box adds celesta-like harmonics; plain tones are a pure sine.
     */
    fun tone(
        frequency: Double,
        numSamples: Int,
        volume: Float = 0.55f,
        isMusicBox: Boolean = true,
        sampleRate: Int = SAMPLE_RATE
    ): ShortArray {
        val buffer = ShortArray(numSamples)
        val decaySamples = (numSamples * 0.85).toInt().coerceAtLeast(1)
        val attackSamples = (numSamples * 0.05).toInt().coerceIn(120, 360)
        val releaseSamples = 160.coerceAtMost(numSamples / 4)

        for (i in 0 until numSamples) {
            val t = 2.0 * PI * i / (sampleRate / frequency)
            var sample = sin(t)

            if (isMusicBox) {
                // Rich harmonic celesta/music box timbre (fundamental + 2nd + 3rd + detuned chime)
                sample += 0.35 * sin(2.0 * t)
                sample += 0.18 * sin(3.0 * t)
                sample += 0.08 * sin(4.004 * t)
                sample /= 1.61
            }

            // Smooth cosine attack ramp to eliminate starting clicks
            val attackGain = if (i < attackSamples) {
                0.5 * (1.0 - cos(PI * i / attackSamples))
            } else {
                1.0
            }

            // Exponential musical ring decay
            val decayGain = if (i >= attackSamples) {
                val progress = ((i - attackSamples).toDouble() / decaySamples).coerceIn(0.0, 1.0)
                exp(-2.6 * progress)
            } else {
                1.0
            }

            // Smooth cosine release fade to zero on buffer tail to eliminate ending pops
            val releaseGain = if (i >= numSamples - releaseSamples) {
                val relIdx = i - (numSamples - releaseSamples)
                0.5 * (1.0 + cos(PI * relIdx / releaseSamples))
            } else {
                1.0
            }

            val envelope = attackGain * decayGain * releaseGain
            val finalSample = (sample * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = finalSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /** A soft brown-ish noise burst that fades out (leaves rustling). */
    fun noise(numSamples: Int, volume: Float): ShortArray {
        val buffer = ShortArray(numSamples)
        var last = 0f
        for (i in 0 until numSamples) {
            val white = (Random.nextDouble() * 2.0 - 1.0).toFloat()
            last = (last + (0.05f * white)) / 1.05f
            val env = 1.0 - (i.toDouble() / numSamples)
            buffer[i] = (last * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    /** 2.6 seconds of thunder: a crack, then rolling echoes over a falling bass. */
    fun thunder(sampleRate: Int = SAMPLE_RATE): ShortArray {
        val durationMs = 2600
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        // Multi-stage echo reflections: strike clap followed by cloud reverberations
        val echoes = listOf(
            Triple(0.10, 1.00, 0.15),  // Concussive strike clap
            Triple(0.45, 0.85, 0.25),  // First cloud echo wave
            Triple(1.00, 0.70, 0.35),  // Second rolling boom
            Triple(1.80, 0.50, 0.40)   // Lingering rolling echo
        )

        var lp1 = 0.0
        var lp2 = 0.0
        var bassPhase = 0.0

        for (i in 0 until numSamples) {
            val tSec = i.toDouble() / sampleRate
            val progress = tSec / 2.6

            // 1. Initial lightning strike crack (crisp electrical snap in first 80ms)
            val snapEnv = if (tSec < 0.015) tSec / 0.015 else exp(-(tSec - 0.015) * 28.0)
            val snapWhite = (Random.nextDouble() * 2.0 - 1.0)
            val snapNoise = snapWhite * snapEnv * 0.55

            // 2. Multi-stage rolling thunder envelope
            var rumbleGain = 0.0
            for ((center, gain, width) in echoes) {
                val dt = tSec - center
                val w = if (dt < 0) width * 0.4 else width
                val bell = exp(-(dt * dt) / (2.0 * w * w))
                rumbleGain += bell * gain
            }
            val masterEnv = exp(-progress * 1.8) * (1.0 - exp(-tSec * 40.0))
            val totalRumble = (rumbleGain * masterEnv).coerceAtMost(1.2)

            // 3. Resonant bass sweep from 130 Hz down to 68 Hz with rich harmonics
            val bassFreq = 130.0 - progress * 62.0
            bassPhase += 2.0 * PI * bassFreq / sampleRate
            val bassWave = sin(bassPhase) * 0.45 + sin(bassPhase * 1.5) * 0.30 + sin(bassPhase * 2.0) * 0.15

            // 4. Low-pass roar noise (thunder rumbling through clouds: cutoff ~350Hz)
            val white = (Random.nextDouble() * 2.0 - 1.0)
            lp1 += 0.09 * (white - lp1)
            lp2 += 0.09 * (lp1 - lp2)

            // 5. Combine strike snap + deep resonant bass + rolling roar
            val combined = (bassWave * 0.50 + lp2 * 1.8) * totalRumble + snapNoise

            // Soft saturation for explosive acoustic body
            val saturated = tanh(combined * 1.5) * 0.92

            buffer[i] = (saturated * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /** Two seconds of soft rain that loops seamlessly: a warm bed with gentle drops. */
    fun rainLoop(sampleRate: Int = SAMPLE_RATE): ShortArray {
        val durationSec = 2
        val numSamples = sampleRate * durationSec
        val rawSamples = DoubleArray(numSamples)

        // 1. Layer 1: Warm low-pass filtered bed (rain on grass, leaves, and cottage roof)
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var brown = 0.0
        var lp1 = 0.0
        var lp2 = 0.0

        for (i in 0 until numSamples) {
            val white = Random.nextDouble() * 2.0 - 1.0
            // Pink noise generation
            b0 = 0.99765 * b0 + white * 0.055
            b1 = 0.96300 * b1 + white * 0.115
            b2 = 0.57000 * b2 + white * 0.220
            val pink = (b0 + b1 + b2) * 0.35
            brown = (brown + 0.06 * white) / 1.06

            // Warm blend
            val rainBed = pink * 0.45 + brown * 0.55

            // Dual-pole low pass filter (cutoff ~700 Hz: removes all harsh rushing flood hiss)
            lp1 += 0.18 * (rainBed - lp1)
            lp2 += 0.18 * (lp1 - lp2)

            // Gentle natural swell (soft breeze undulating rain against window)
            val tSec = i.toDouble() / sampleRate
            val breeze = 0.88 + 0.12 * sin(2.0 * PI * tSec / 4.2)
            rawSamples[i] = lp2 * breeze
        }

        // 2. Layer 2: Soft, gentle individual raindrop patter (cozy muffled plinks)
        val numDrops = 35
        for (d in 0 until numDrops) {
            val dropStart = Random.nextInt(numSamples - 1000)
            val dropDur = Random.nextInt(250, 550) // ~11ms - 25ms
            val dropFreq = Random.nextDouble(420.0, 880.0) // warm soothing frequencies
            val dropVol = Random.nextDouble(0.04, 0.09) // soft gentle patter
            for (j in 0 until dropDur) {
                val progress = j.toDouble() / dropDur
                val decay = exp(-progress * 8.0)
                val dropTone = sin(2.0 * PI * j * dropFreq / sampleRate)
                rawSamples[dropStart + j] += dropTone * decay * dropVol
            }
        }

        // 3. Seamless crossfade at buffer boundaries (0.3s)
        val fadeLen = (sampleRate * 0.30).toInt()
        for (i in 0 until fadeLen) {
            val t = i.toDouble() / fadeLen
            val wIn = sin(t * PI * 0.5)
            val wOut = cos(t * PI * 0.5)
            val blended = rawSamples[i] * wIn + rawSamples[numSamples - fadeLen + i] * wOut
            rawSamples[i] = blended
            rawSamples[numSamples - fadeLen + i] = blended
        }

        // 4. Normalized master gain (gentle, clearly audible volume)
        var maxAmp = 0.0
        for (i in 0 until numSamples) {
            val a = abs(rawSamples[i])
            if (a > maxAmp) maxAmp = a
        }
        val scale = if (maxAmp > 0.001) (0.60 / maxAmp) else 1.0
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            buffer[i] = (rawSamples[i] * scale * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /** [samples] as a mono 16-bit WAV file (what AVAudioPlayer and other players can open). */
    fun wav(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val dataBytes = samples.size * 2
        val out = ByteArray(44 + dataBytes)
        fun text(at: Int, s: String) = s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
        fun int32(at: Int, v: Int) { for (b in 0 until 4) out[at + b] = (v shr (8 * b)).toByte() }
        fun int16(at: Int, v: Int) { out[at] = v.toByte(); out[at + 1] = (v shr 8).toByte() }
        text(0, "RIFF"); int32(4, 36 + dataBytes); text(8, "WAVE")
        text(12, "fmt "); int32(16, 16); int16(20, 1); int16(22, 1)
        int32(24, sampleRate); int32(28, sampleRate * 2); int16(32, 2); int16(34, 16)
        text(36, "data"); int32(40, dataBytes)
        for (i in samples.indices) int16(44 + i * 2, samples[i].toInt())
        return out
    }
}
