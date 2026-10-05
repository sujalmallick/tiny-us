package com.example.games

import com.example.engine.AmbientAudio
import com.example.progress.Game
import com.example.progress.ProgressEvent
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Catch together" inside the real engine: the basket catches the snow, and the round is recorded. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatchGameEngineTest {

    private fun engine(weather: WeatherType, scene: SceneType = SceneType.FLOWER) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            loadScene(scene)
            updateAtmosphereMode("DAY")
            weatherDriftEnabled = false
            this.weather = weather
            repeat(120) { update(1f / 60f, 1080f, 2400f) }
        }

    @Test
    fun aRoundInTheSnowCatchesFlakesAndIsRecorded() {
        val e = engine(WeatherType.SNOW)
        val events = mutableListOf<ProgressEvent>()
        var result = -1
        e.onProgress = { events += it }
        e.onCatchGameOver = { result = it }
        e.startCatchGame()
        assertTrue(e.catchActive)
        // Sweep the basket back and forth for the whole round.
        var t = 0f
        while (e.catchActive && t < 40f) {
            e.catchGame.moveTo(0.5f + 0.35f * kotlin.math.sin(t * 1.3f))
            e.update(1f / 60f, 1080f, 2400f)
            t += 1f / 60f
        }
        assertFalse(e.catchActive)
        assertTrue("caught some snow: $result", result > 5)
        assertEquals(result, e.catchScore)
        assertTrue(events.any { it is ProgressEvent.GamePlayed && it.game == Game.CATCH && it.score == result })
    }

    @Test
    fun noRoundWithoutSomethingFalling() {
        val sunny = engine(WeatherType.SUNNY)
        assertFalse(sunny.hasCatchableWeather)
        sunny.startCatchGame()
        assertFalse(sunny.catchActive)
        val indoors = engine(WeatherType.SNOW, SceneType.COZY_LOFT)
        assertFalse(indoors.hasCatchableWeather)
    }
}
