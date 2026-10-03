package com.example

import com.example.data.AdventureStatus
import com.example.data.DateAdventure
import com.example.data.DreamEntry
import com.example.data.GardenGrowth
import com.example.data.LoveNoteItem
import com.example.data.MemoryItem
import com.example.data.PolaroidMemory
import com.example.data.StoryInput
import com.example.data.StoryKind
import com.example.data.StoryTimeline
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoryAndGardenTest {
    private val utc = TimeZone.UTC
    private val today = LocalDate(2026, 10, 3)

    @Test
    fun parsesEveryDateFormatTheAppHasWritten() {
        assertEquals(LocalDate(2026, 10, 1), StoryTimeline.parseLooseDate("2026-10-01"))
        assertEquals(LocalDate(2026, 9, 28), StoryTimeline.parseLooseDate("Sep 28, 2026"))
        assertEquals(LocalDate(2026, 9, 28), StoryTimeline.parseLooseDate("September 28, 2026"))
        assertEquals(LocalDate(2026, 9, 28), StoryTimeline.parseLooseDate("28 Sep 2026"))
        assertNull(StoryTimeline.parseLooseDate("Quiet Night"))
        assertNull(StoryTimeline.parseLooseDate("Sep 3")) // no year: unknowable
        assertNull(StoryTimeline.parseLooseDate("Feb 30, 2026"))
    }

    @Test
    fun polaroidCaptureTimeComesFromTheFileName() {
        val photo = PolaroidMemory("p", "t", "Sep 28, 2026", "10:49 PM", "Lantern Stroll", "PATH", "/x/polaroids/pol_1790000000000_ab12cd.png")
        assertEquals(1790000000000L, StoryTimeline.captureMillis(photo))
    }

    @Test
    fun storyIsChronologicalAndKeepsUndatedEntries() {
        val input = StoryInput(
            anniversary = LocalDate(2025, 1, 10),
            memories = listOf(
                MemoryItem("m1", "Picnic", "Jun 3, 2025", "Sunny", "flower"),
                MemoryItem("m_sample", "Stargazing", "Quiet Night", "Stars", "stars")
            ),
            letters = listOf(
                LoveNoteItem("n1", "Sample", "From Me", "Today"), // bundled sample: skipped
                LoveNoteItem("c1", "I love you", "From Bean", "Mar 2", isCustom = true, createdAt = 1772409600000L) // 2026-03-02
            ),
            dreams = listOf(DreamEntry("d1", "Flying over Japan", listOf("japan"), "JAPAN", 1767225600000L)), // 2026-01-01
            adventures = listOf(
                DateAdventure("a1", "Bake together", "Cookies", "COZY_HOME", status = AdventureStatus.COMPLETED, completedTimestamp = 1751328000000L), // 2025-07-01
                DateAdventure("a2", "Not done yet", "Later", "COZY_HOME")
            )
        )
        val story = StoryTimeline.build(input, today, utc)

        assertTrue(story.none { it.id == "letter:n1" })
        assertTrue(story.none { it.id == "adventure:a2" })
        assertEquals("milestone:2025-01-10", story.first().id)
        val dated = story.filter { it.date != null && !it.isUpcoming }
        assertEquals(dated.map { it.date }, dated.map { it.date }.sortedBy { it })
        assertEquals("memory:m_sample", story.last().id) // undated goes to the end, not dropped
        assertEquals(LocalDate(2026, 3, 2), story.first { it.id == "letter:c1" }.date)

        val sections = StoryTimeline.groupByMonth(story)
        assertNull(sections.last().year)
        assertEquals(1, sections.last().entries.size)
    }

    @Test
    fun milestonesStopAtTodayPlusOneUpcoming() {
        val m = StoryTimeline.milestones(LocalDate(2025, 1, 10), today)
        val titles = m.map { it.title }
        assertTrue("Day 100 together" in titles)
        assertTrue("Our first anniversary" in titles)
        assertEquals(LocalDate(2025, 4, 19), m.first { it.title == "Day 100 together" }.date) // inclusive counting
        assertEquals(1, m.count { it.isUpcoming })
        assertTrue(m.filter { !it.isUpcoming }.all { it.date!! <= today })
        assertTrue(StoryTimeline.milestones(LocalDate(2030, 1, 1), today).isEmpty())
    }

    @Test
    fun gardenStagesMatchTheOriginalCurve() {
        assertEquals(listOf(0, 0, 1, 2, 2, 3, 3, 4, 4, 4, 5, 5, 5, 5, 6), (0..14).map { GardenGrowth.stageFor(it) })
    }

    @Test
    fun gardenOnlyEverGrows() {
        var previous = 0
        for (day in 0..400) {
            val count = GardenGrowth.bloomsFor(day).size
            assertTrue(count >= previous, "garden shrank on day $day")
            previous = count
        }
        assertEquals(0, GardenGrowth.bloomsFor(14).size)
        assertEquals(1, GardenGrowth.bloomsFor(15).size)
        assertEquals(GardenGrowth.keepsakePlants.size, GardenGrowth.bloomsFor(70).size)
        assertTrue(GardenGrowth.bloomsFor(100).last().isGolden)
    }

    @Test
    fun nextBloomCountdownIsAlwaysPositive() {
        for (day in 0..200) assertTrue(GardenGrowth.daysUntilNextBloom(day) >= 1)
        assertEquals(1, GardenGrowth.daysUntilNextBloom(14))
        assertEquals(5, GardenGrowth.daysUntilNextBloom(15))
    }

    @Test
    fun welcomeBackIsGentleAndOnlyAfterABreak() {
        assertNull(GardenGrowth.welcomeBackMessage(1, "Mochi"))
        assertTrue(GardenGrowth.welcomeBackMessage(4, "Mochi")!!.contains("Mochi"))
        val long = GardenGrowth.welcomeBackMessage(40, "Mochi")!!
        listOf("wilt", "die", "dead", "lost", "streak").forEach { assertTrue(!long.lowercase().contains(it)) }
    }
}
