package com.example

import com.example.engine.MusicBoxSongs
import com.example.engine.SoundCue
import com.example.engine.SoundStep
import com.example.engine.ToneSynth
import com.example.engine.WeatherCarryOver
import com.example.scene.WeatherType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoundAndWeatherTest {
    private val rate = ToneSynth.SAMPLE_RATE

    @Test
    fun everyCueRendersItsStepsBackToBack() {
        for (cue in SoundCue.entries) {
            val expected = cue.steps.sumOf { step ->
                when (step) {
                    is SoundStep.Note -> ToneSynth.samplesFor(step.ms, 64)
                    is SoundStep.Gap -> rate * step.ms / 1000
                    is SoundStep.Noise -> ToneSynth.samplesFor(step.ms, 50)
                }
            }
            val samples = cue.render()
            assertEquals(expected, samples.size, "$cue length")
            assertTrue(samples.any { abs(it.toInt()) > 500 }, "$cue is audible")
        }
    }

    @Test
    fun cuesKeepAndroidsRecipes() {
        assertEquals(
            listOf(1046.50, 1318.51, 1567.98),
            SoundCue.HEART_CHIME.steps.filterIsInstance<SoundStep.Note>().map { it.freq }
        )
        // Four purrs and a little chime at the end.
        assertEquals(9, SoundCue.CAT_PURR.steps.count { it is SoundStep.Note })
        assertEquals(6, SoundCue.REEL_CLICK.steps.count { it is SoundStep.Note })
    }

    @Test
    fun tonesFadeInAndOutWithoutClicks() {
        val tone = ToneSynth.tone(880.0, ToneSynth.samplesFor(120), 0.5f, isMusicBox = true)
        assertTrue(abs(tone.first().toInt()) < 50 && abs(tone.last().toInt()) < 50)
        assertTrue(tone.maxOf { abs(it.toInt()) } in 3000..Short.MAX_VALUE.toInt())
    }

    @Test
    fun songsLoopWithAPauseAtTheEnd() {
        for (song in MusicBoxSongs.playlist) {
            val samples = MusicBoxSongs.render(song)
            val expectedMs = song.notes.sumOf { it.durationMs.toLong() + MusicBoxSongs.gapAfterMs(it) } + MusicBoxSongs.LOOP_GAP_MS
            assertTrue(abs(samples.size - (expectedMs * rate / 1000).toInt()) <= 2 * song.notes.size + 2, song.id)
            val tail = samples.takeLast(rate * MusicBoxSongs.LOOP_GAP_MS.toInt() / 1000)
            assertTrue(tail.all { it.toInt() == 0 }, "${song.id} ends in silence")
        }
    }

    @Test
    fun wavHasAStandardHeader() {
        val wav = ToneSynth.wav(shortArrayOf(1, -1, 300))
        assertEquals("RIFF", wav.copyOfRange(0, 4).decodeToString())
        assertEquals("WAVE", wav.copyOfRange(8, 12).decodeToString())
        assertEquals("data", wav.copyOfRange(36, 40).decodeToString())
        assertEquals(44 + 6, wav.size)
        assertEquals(300, (wav[48].toInt() and 0xFF) or (wav[49].toInt() shl 8))
    }

    @Test
    fun rainAndThunderAreLongEnough() {
        assertEquals(rate * 2, ToneSynth.rainLoop().size)
        assertEquals(rate * 2600 / 1000, ToneSynth.thunder().size)
    }

    @Test
    fun weatherCarriesOverForAWhileThenFollowsTheSeason() {
        val now = 1_000_000_000L
        val recent = WeatherCarryOver.startWeather("RAIN", now - 10 * 60_000L, now, month = 7, region = "")
        assertEquals(WeatherType.RAIN, recent)
        repeat(30) {
            // An hour later in July (northern summer) it never snows.
            val later = WeatherCarryOver.startWeather("SNOW", now - 60 * 60_000L, now, month = 7, region = "")
            assertTrue(later == WeatherType.SUNNY || later == WeatherType.RAIN, "$later in July")
        }
        repeat(30) {
            val fresh = WeatherCarryOver.startWeather(null, 0L, now, month = 1, region = "")
            assertTrue(fresh != WeatherType.AUTUMN && fresh != WeatherType.SAKURA, "$fresh in January")
        }
    }
}
