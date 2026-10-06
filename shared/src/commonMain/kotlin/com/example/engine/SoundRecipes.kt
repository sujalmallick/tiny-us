package com.example.engine

/** One step of a little sound: a tone, a pause, or a soft noise burst. */
sealed interface SoundStep {
    data class Note(val freq: Double, val ms: Int, val volume: Float, val musicBox: Boolean) : SoundStep
    data class Gap(val ms: Int) : SoundStep
    data class Noise(val ms: Int, val volume: Float) : SoundStep
}

private fun box(freq: Double, ms: Int, volume: Float) = SoundStep.Note(freq, ms, volume, musicBox = true)
private fun plain(freq: Double, ms: Int, volume: Float) = SoundStep.Note(freq, ms, volume, musicBox = false)
private fun gap(ms: Int) = SoundStep.Gap(ms)

/** Each note followed by the same pause, as in a quick arpeggio. */
private fun run(freqs: List<Double>, ms: Int, volume: Float, musicBox: Boolean, pause: Int): List<SoundStep> =
    freqs.flatMap { listOf(SoundStep.Note(it, ms, volume, musicBox), gap(pause)) }

private fun repeated(times: Int, steps: List<SoundStep>): List<SoundStep> = List(times) { steps }.flatten()

/**
 * The one-shot sounds that answer taps and moments in the world, as recipes: each note plays to
 * its end, then the next step follows. Android plays the steps one by one; iOS renders the whole
 * recipe into one buffer ([render]); both sound the same.
 */
enum class SoundCue(val steps: List<SoundStep>) {
    /** C6, E6, G6, each ringing a little longer. */
    HEART_CHIME(listOf(box(1046.50, 110, 0.45f), gap(20), box(1318.51, 140, 0.45f), gap(20), box(1567.98, 190, 0.45f), gap(20))),

    /** A mechanical instant-camera click with a warm chime. */
    CAMERA_SHUTTER(listOf(plain(1760.0, 25, 0.45f), gap(35), plain(880.0, 40, 0.40f), gap(55), box(1318.51, 90, 0.35f), gap(25), box(1760.0, 140, 0.40f))),
    BUBBLE_POP(listOf(plain(784.0, 60, 0.38f), gap(15), plain(1174.6, 85, 0.38f))),
    STAR_TWINKLE(listOf(box(1318.5, 75, 0.38f), gap(15), box(1567.98, 105, 0.38f), gap(15), box(2093.0, 140, 0.38f), gap(15))),
    LEAF_RUSTLE(listOf(SoundStep.Noise(140, 0.22f))),
    BIRD_CHIRP(listOf(box(2349.32, 60, 0.35f), gap(15), box(2793.83, 80, 0.35f))),
    COOKING_BUBBLES(listOf(plain(587.33, 40, 0.30f), gap(10), plain(783.99, 45, 0.30f))),
    SCOOTER_HORN(listOf(plain(880.0, 60, 0.40f), gap(20), plain(1174.66, 90, 0.45f))),
    CAT_PURR(repeated(4, listOf(plain(140.0, 55, 0.28f), gap(30), plain(115.0, 75, 0.24f), gap(60))) + box(880.0, 45, 0.22f)),
    WIND_CHIME(listOf(box(1046.50, 80, 0.25f), gap(40), box(1318.51, 90, 0.28f), gap(40), box(1567.98, 120, 0.30f))),
    THINKING_OF_YOU(run(listOf(523.25, 659.25, 783.99, 1046.50), 70, 0.35f, true, 50)),

    /** Kitchen clock: three fading ticks. */
    TICK_TICK(listOf(plain(1200.0, 35, 0.28f), gap(80), plain(1200.0, 35, 0.22f), gap(80), plain(1200.0, 35, 0.16f))),

    /** Seaside pier: Pip's cheeky two-note squawk, twice. */
    SEAGULL_CALL(repeated(2, listOf(plain(1318.51, 70, 0.26f), gap(20), plain(987.77, 110, 0.22f), gap(90)))),

    /** Seaside pier: a soft, low lighthouse foghorn (root and fifth). */
    FOGHORN(listOf(plain(110.0, 520, 0.30f), gap(60), plain(164.81, 420, 0.20f))),

    /** Seaside pier: the little sailboat's "toot toot". */
    BOAT_HORN(repeated(2, listOf(plain(261.63, 160, 0.24f), gap(110)))),

    /** Seaside pier: the fishing reel ticking. */
    REEL_CLICK(repeated(6, listOf(plain(1760.0, 18, 0.16f), gap(38)))),
    WOOD_KNOCK(listOf(plain(220.0, 55, 0.32f), gap(40), plain(196.0, 50, 0.26f))),

    /** Kitchen: four descending soft pings for watering the planter. */
    WATER_DRIP(run(listOf(1200.0, 1000.0, 880.0, 740.0), 70, 0.20f, true, 55)),
    WOOD_CREAK(listOf(plain(320.0, 110, 0.28f), gap(40), plain(280.0, 80, 0.20f))),
    CANDLE_FLICKER(listOf(box(659.25, 60, 0.22f), gap(50), box(783.99, 70, 0.20f))),
    SOFT_THUD(listOf(plain(110.0, 70, 0.32f), gap(30), plain(95.0, 50, 0.22f))),
    CAT_CHIRP(listOf(box(1760.0, 45, 0.30f), gap(65), box(2093.0, 55, 0.28f))),
    SOFT_ROLL(run(listOf(261.63, 246.94, 220.00), 40, 0.24f, false, 45)),
    PAPER_FLIP(listOf(plain(1567.98, 30, 0.24f), gap(35), plain(1760.0, 40, 0.22f))),
    PAGODA_CHIME(run(listOf(1318.51, 1567.98, 1760.00), 65, 0.24f, true, 55)),
    LAVENDER_RUSTLE(run(listOf(440.00, 392.00, 349.23), 80, 0.18f, false, 60)),
    MUSHROOM_CHIME(run(listOf(523.25, 659.25, 783.99, 1046.50), 50, 0.22f, true, 45)),
    STAR_ARPEGGIO(run(listOf(880.00, 1108.73, 1318.51, 1760.00), 70, 0.26f, true, 60)),
    NEON_BUZZ(run(listOf(220.0, 246.94, 220.0), 50, 0.24f, false, 45)),
    STEAM_HISS(run(listOf(2800.0, 2400.0, 2000.0), 45, 0.20f, false, 40)),
    SPICE_ZING(listOf(box(2093.0, 50, 0.25f), gap(40), box(2637.0, 60, 0.22f))),
    CHALK_SQUEAK(run(listOf(1975.5, 2093.0, 2217.4), 35, 0.20f, false, 35)),
    BAMBOO_KNOCK(listOf(plain(392.0, 50, 0.26f), gap(45), plain(329.63, 45, 0.22f))),
    SAUCER_SIP(listOf(plain(880.0, 30, 0.22f), gap(35), plain(987.77, 35, 0.20f)));

    /** The whole recipe as one buffer, with each step where it would start when played live. */
    fun render(sampleRate: Int = ToneSynth.SAMPLE_RATE): ShortArray {
        val parts = steps.map { step ->
            when (step) {
                is SoundStep.Note -> ToneSynth.tone(step.freq, ToneSynth.samplesFor(step.ms, 64, sampleRate), step.volume, step.musicBox, sampleRate)
                is SoundStep.Gap -> ShortArray(sampleRate * step.ms / 1000)
                is SoundStep.Noise -> ToneSynth.noise(ToneSynth.samplesFor(step.ms, 50, sampleRate), step.volume)
            }
        }
        val out = ShortArray(parts.sumOf { it.size })
        var at = 0
        for (part in parts) {
            part.copyInto(out, at)
            at += part.size
        }
        return out
    }
}
