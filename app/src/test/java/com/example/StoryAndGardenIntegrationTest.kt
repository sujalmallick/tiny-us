package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GardenGrowth
import com.example.data.PolaroidManager
import com.example.data.PreferencesManager
import com.example.data.StoryKind
import com.example.ui.loadStory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoryAndGardenIntegrationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `new keepsakes and letters are timestamped and appear in Our Story`() {
        val prefs = PreferencesManager(context)
        prefs.anniversaryDate = "2025-02-14"
        prefs.addMemory("First picnic", "Strawberries everywhere", "", "flower")
        prefs.addLoveNote("Thank you for today", "From Bean")

        val reloaded = PreferencesManager(context)
        assertTrue(reloaded.getMemories().first().createdAt > 0L)
        assertTrue(reloaded.getLoveNotes().first().createdAt > 0L)

        val story = loadStory(reloaded, PolaroidManager(context))
        val memory = story.first { it.title == "First picnic" }
        val letter = story.first { it.kind == StoryKind.LETTER && it.body == "Thank you for today" }
        assertNotNull(memory.date)
        assertNotNull(letter.date)
        assertEquals("milestone:2025-02-14", story.first().id)
    }

    @Test
    fun `garden blooms are recorded once and never removed`() {
        val prefs = PreferencesManager(context)
        prefs.markAppOpenedToday()
        repeat(GardenGrowth.FIRST_BLOOM_DAY - 1) { prefs.fastForwardDayForDebug() }
        assertEquals(GardenGrowth.FIRST_BLOOM_DAY, prefs.uniqueDaysOpened)

        val first = prefs.getGardenBloomDates()
        assertEquals(setOf(0), first.keys)

        repeat(GardenGrowth.DAYS_BETWEEN_BLOOMS) { prefs.fastForwardDayForDebug() }
        val second = prefs.getGardenBloomDates()
        assertEquals(setOf(0, 1), second.keys)
        assertEquals(first[0], second[0]) // the first bloom keeps its original date
        assertEquals(6, prefs.gardenStage)
    }
}
