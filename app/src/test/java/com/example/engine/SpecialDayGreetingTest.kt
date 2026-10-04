package com.example.engine

import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The couple's special-day greeting stays up long enough to read, whatever else is going on. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpecialDayGreetingTest {

    @Test
    fun greetingIsNotTalkedOver() {
        val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.loadScene(SceneType.FLOWER)
        // Let the scene settle so the couple's own routine (and its chatter) is running.
        repeat(400) { engine.update(0.05f, 1080f, 2400f) }

        engine.greetSpecialDay("Happy Diwali!", "Every lamp is lit.")
        // Most of the greeting's time: autonomy and scene moments keep running meanwhile.
        repeat(((SceneEngine.SPECIAL_DAY_LINE_SECONDS - 0.5f) / 0.05f).toInt()) {
            engine.update(0.05f, 1080f, 2400f)
            assertEquals("Happy Diwali!", engine.boySpeechText)
            assertEquals("Every lamp is lit.", engine.girlSpeechText)
        }
    }
}
