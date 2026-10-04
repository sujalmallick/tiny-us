package com.example

import com.example.data.DateAdventureCatalog
import com.example.data.DailyPromptCatalog
import com.example.data.InMemoryKeyValueStorage
import com.example.data.RelationshipTimeCalculator
import com.example.engine.AntiRepeatRandomPicker
import com.example.engine.TimeOfDayPhase
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SharedCoreTest {
    @Test
    fun relationshipDayIsInclusiveAcrossLeapDay() {
        assertEquals(1L, RelationshipTimeCalculator.calculateTinyUsDay(LocalDate(2024, 2, 28), LocalDate(2024, 2, 28)))
        assertEquals(3L, RelationshipTimeCalculator.calculateTinyUsDay(LocalDate(2024, 2, 28), LocalDate(2024, 3, 1)))
        assertEquals(1L, RelationshipTimeCalculator.calculateTinyUsDay(LocalDate(2025, 5, 2), LocalDate(2025, 4, 30)))
        assertEquals(1L, RelationshipTimeCalculator.calculateDaysFromIso("not-a-date", "2025-01-01"))
    }

    @Test
    fun countdownSplitsTimeAndHandlesPassedDates() {
        val countdown = RelationshipTimeCalculator.computeCountdown(172_865L, isSameDay = false)
        assertEquals(2L, countdown.days)
        assertEquals(0L, countdown.hours)
        assertEquals(1L, countdown.minutes)
        assertEquals(5L, countdown.seconds)
        assertTrue(RelationshipTimeCalculator.computeCountdown(0L, isSameDay = true).isToday)
        assertTrue(RelationshipTimeCalculator.computeCountdown(-1L, isSameDay = false).isPassed)
    }

    @Test
    fun dailyPromptCatalogWrapsNegativeAndLargeDayIndexes() {
        val prompts = DailyPromptCatalog.defaultPrompts
        assertTrue(prompts.isNotEmpty())
        assertEquals(prompts.last(), DailyPromptCatalog.getPromptForDay(-1))
        assertEquals(prompts[0], DailyPromptCatalog.getPromptForDay(prompts.size))
        assertNotEquals(prompts[0].id, prompts[1].id)
    }

    @Test
    fun sharedCatalogsAndSceneWeatherDefinitionsAreUsable() {
        assertTrue(DateAdventureCatalog.defaultAdventures.isNotEmpty())
        assertEquals(DateAdventureCatalog.defaultAdventures.size, DateAdventureCatalog.defaultAdventures.map { it.id }.toSet().size)
        // Scene names are persisted (last scene, recent scenes), so renames must be deliberate.
        assertEquals(
            listOf(
                "FLOWER", "UNDER_TREE", "COOKING", "SLEEP", "WALK", "LOOKING", "MOMO_STALL",
                "EVENING_RIDE", "COZY_LOFT", "RAINY_CAFE", "SUNROOM", "CAMPFIRE", "SEASIDE_PIER"
            ),
            SceneType.values().map { it.name }
        )
        assertTrue(SceneType.values().all { it.title.isNotBlank() && it.subtitle.isNotBlank() })
        assertEquals(5, WeatherType.values().size)
        assertTrue(WeatherType.values().all { it.displayName.isNotBlank() })
    }

    @Test
    fun timeOfDayBoundariesAreStable() {
        assertEquals(TimeOfDayPhase.NIGHT, TimeOfDayPhase.fromHour(4))
        assertEquals(TimeOfDayPhase.MORNING, TimeOfDayPhase.fromHour(5))
        assertEquals(TimeOfDayPhase.AFTERNOON, TimeOfDayPhase.fromHour(8))
        assertEquals(TimeOfDayPhase.SUNSET, TimeOfDayPhase.fromHour(17))
        assertEquals(TimeOfDayPhase.NIGHT, TimeOfDayPhase.fromHour(20))
        assertEquals(TimeOfDayPhase.NIGHT, TimeOfDayPhase.fromHour(23))
        assertEquals(TimeOfDayPhase.NIGHT, TimeOfDayPhase.fromHour(0))
    }

    @Test
    fun antiRepeatPickerAvoidsAdjacentRepeatsAndRejectsEmptyInputs() {
        val picker = AntiRepeatRandomPicker(listOf("a", "b"))
        val first = picker.pick()
        val second = picker.pick()
        assertNotEquals(first, second)
        assertEquals(first, picker.pick())
        picker.reset()
        assertEquals("a", picker.pick())
        assertFailsWith<NoSuchElementException> { AntiRepeatRandomPicker(emptyList<String>()).pick() }
        assertFailsWith<NoSuchElementException> { AntiRepeatRandomPicker(listOf("a")).pickFrom(emptyList()) }
    }

    @Test
    fun inMemoryStoragePreservesTypesDefaultsAndRemoval() {
        val storage = InMemoryKeyValueStorage()
        assertEquals("fallback", storage.getString("missing", "fallback"))
        assertEquals(true, storage.getBoolean("missing", true))
        assertEquals(4, storage.getInt("missing", 4))
        assertEquals(5L, storage.getLong("missing", 5L))
        assertEquals(setOf("fallback"), storage.getStringSet("missing", setOf("fallback")))
        storage.putString("text", "hello")
        storage.putBoolean("enabled", true)
        storage.putInt("count", 9)
        storage.putLong("timestamp", 12L)
        storage.putStringSet("tags", setOf("one", "two"))
        assertEquals("hello", storage.getString("text"))
        assertEquals(true, storage.getBoolean("enabled"))
        assertEquals(9, storage.getInt("count"))
        assertEquals(12L, storage.getLong("timestamp"))
        assertEquals(setOf("one", "two"), storage.getStringSet("tags"))
        storage.remove("text")
        assertEquals(null, storage.getString("text"))
        storage.clear()
        assertEquals(false, storage.getBoolean("enabled"))
        assertEquals(0, storage.getInt("count"))
    }
}
