package com.example

import com.example.engine.AmbientAudio
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import com.example.ui.parseDreamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DreamModeArchitectureTest {

    @Test
    fun testDreamModeActivationAndIsolation() {
        val audio = AmbientAudio(context = null)
        val engine = SceneEngine(
            audio = audio,
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )

        // Setup an initial scene and weather
        engine.loadScene(SceneType.FLOWER)
        engine.weather = WeatherType.SUNNY

        val initialScene = engine.currentScene
        val initialWeather = engine.weather
        val initialBoyX = engine.boy.worldX
        val initialBoyY = engine.boy.worldY
        val initialGirlX = engine.girl.worldX
        val initialGirlY = engine.girl.worldY

        // Initially not in dream mode
        assertFalse("Initially should not be in dream mode", engine.isDreamMode)
        assertNull("Dream state should initially be null", engine.dreamState)
        assertNull("activeDreamTheme should be null", engine.activeDreamTheme)
        assertNull("activeDreamText should be null", engine.activeDreamText)
        assertEquals(0f, engine.dreamOverlayAlpha, 0.001f)

        // Activate dream mode
        engine.activateDream(theme = "JAPAN", text = "Walking together under cherry blossoms")

        // Assert dream state is active
        assertTrue("isDreamMode must be true when dream is activated", engine.isDreamMode)
        assertNotNull("dreamState must be non-null", engine.dreamState)
        assertEquals("JAPAN", engine.dreamState?.theme)
        assertEquals("Walking together under cherry blossoms", engine.dreamState?.text)
        assertEquals(1.0f, engine.dreamState?.alpha ?: 0f, 0.001f)

        // Assert backward-compatible getters
        assertEquals("JAPAN", engine.activeDreamTheme)
        assertEquals("Walking together under cherry blossoms", engine.activeDreamText)
        assertEquals(1.0f, engine.dreamOverlayAlpha, 0.001f)

        // Assert scene state, weather, and character positions are strictly preserved
        assertEquals("currentScene must remain unchanged when entering dream mode", initialScene, engine.currentScene)
        assertEquals("weather must remain unchanged when entering dream mode", initialWeather, engine.weather)
        assertEquals("boy.worldX must remain unchanged", initialBoyX, engine.boy.worldX, 0.001f)
        assertEquals("boy.worldY must remain unchanged", initialBoyY, engine.boy.worldY, 0.001f)
        assertEquals("girl.worldX must remain unchanged", initialGirlX, engine.girl.worldX, 0.001f)
        assertEquals("girl.worldY must remain unchanged", initialGirlY, engine.girl.worldY, 0.001f)

        // Simulate frame updates while in dream mode
        engine.update(0.016f, 1080f, 2400f)
        engine.update(0.016f, 1080f, 2400f)

        // Scene and weather should not mutate
        assertEquals("currentScene must remain unchanged after updates in dream mode", initialScene, engine.currentScene)
        assertEquals("weather must remain unchanged after updates in dream mode", initialWeather, engine.weather)

        // Wake up / clear dream mode
        engine.clearDream()

        // Assert dream mode is deactivated
        assertFalse("isDreamMode must be false after clearDream", engine.isDreamMode)
        assertNull("dreamState must be null after clearDream", engine.dreamState)
        assertNull("activeDreamTheme must be null after clearDream", engine.activeDreamTheme)
        assertNull("activeDreamText must be null after clearDream", engine.activeDreamText)
        assertEquals(0f, engine.dreamOverlayAlpha, 0.001f)

        // Verify scene and weather are still exactly intact
        assertEquals("currentScene must still be initial scene after wake up", initialScene, engine.currentScene)
        assertEquals("weather must still be initial weather after wake up", initialWeather, engine.weather)
    }

    @Test
    fun testDreamThemeParsingAndFallback() {
        // Unrecognized dream text must map to FALLBACK theme
        val (fallbackTheme, fallbackKeywords) = parseDreamTheme("Random unrecognized fantasy journey 12345")
        assertEquals("FALLBACK", fallbackTheme)
        assertTrue(fallbackKeywords.isEmpty())

        // Empty string must map to FALLBACK
        val (emptyTheme, emptyKeywords) = parseDreamTheme("")
        assertEquals("FALLBACK", emptyTheme)
        assertTrue(emptyKeywords.isEmpty())

        // Keyword matching tests for iconic themes
        val (japanTheme, japanKeywords) = parseDreamTheme("We saw beautiful sakura trees in Tokyo")
        assertEquals("JAPAN", japanTheme)
        assertTrue(japanKeywords.contains("sakura"))
        assertTrue(japanKeywords.contains("tokyo"))

        val (norwayTheme, norwayKeywords) = parseDreamTheme("Watching the green aurora dancing in Norway")
        assertEquals("NORWAY", norwayTheme)
        assertTrue(norwayKeywords.contains("aurora"))
        assertTrue(norwayKeywords.contains("norway"))

        val (oceanTheme, oceanKeywords) = parseDreamTheme("Walking on the sunny beach near ocean waves")
        assertEquals("OCEAN", oceanTheme)
        assertTrue(oceanKeywords.contains("ocean"))
        assertTrue(oceanKeywords.contains("beach"))

        val (skyTheme, _) = parseDreamTheme("Floating in the sky above soft white clouds")
        assertEquals("FLYING", skyTheme)

        val (starsTheme, _) = parseDreamTheme("Stargazing at the cosmos in deep space")
        assertEquals("STARS", starsTheme)

        val (forestTheme, _) = parseDreamTheme("Hiking through an emerald green forest")
        assertEquals("FOREST", forestTheme)

        val (homeTheme, _) = parseDreamTheme("Cozy warm tea by the fireplace in our cottage")
        assertEquals("HOME", homeTheme)

        val (rainTheme, _) = parseDreamTheme("Listening to the rainy drizzle and thunder")
        assertEquals("RAIN", rainTheme)

        val (cityTheme, _) = parseDreamTheme("Neon lights in the urban skyline night out")
        assertEquals("CITY", cityTheme)

        val (sweetTheme, _) = parseDreamTheme("Eating sweet cake and delicious momo at a picnic")
        assertEquals("SWEET", sweetTheme)
    }
}
