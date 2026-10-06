package com.example

import com.example.care.TinyCareCategory
import com.example.care.TinyCarePlan
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class TinyCarePlanTest {
    private val zone = TimeZone.UTC
    private fun at(hour: Int, minute: Int = 0) = LocalDateTime(2026, 10, 7, hour, minute).toInstant(zone).toEpochMilliseconds()
    private fun hourOf(ms: Long) = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).hour

    @Test
    fun firstReminderComesSoonThenEveryFewHours() {
        repeat(50) { seed ->
            val random = Random(seed)
            val first = TinyCarePlan.nextTime(at(10), first = true, random = random, zone = zone)
            assertTrue((first - at(10)) / 60_000 in 15..30)
            val next = TinyCarePlan.nextTime(first, random = random, zone = zone)
            assertTrue((next - first) / 60_000 in 90..150)
        }
    }

    @Test
    fun quietHoursMoveToTheMorning() {
        repeat(50) { seed ->
            val time = TinyCarePlan.nextTime(at(22, 30), random = Random(seed), zone = zone)
            val local = Instant.fromEpochMilliseconds(time).toLocalDateTime(zone)
            assertEquals(8, local.date.day, "the next morning")
            assertTrue(local.hour == 7 && local.minute in 15..45)
        }
    }

    @Test
    fun aFewDaysPlannedNeverInQuietHoursAndVaried() {
        val all = TinyCareCategory.allIds()
        val plan = TinyCarePlan.upcoming(at(9), at(9) + 3 * 24 * 3600_000L, all, emptyList(), random = Random(7), zone = zone)
        assertTrue(plan.size in 15..40, "${plan.size} reminders in three days")
        assertTrue(plan.none { TinyCarePlan.isTimeInQuietWindow(hourOf(it.atMillis), 23, 7) })
        assertTrue(plan.zipWithNext().all { (a, b) -> b.atMillis > a.atMillis })
        assertTrue(plan.windowed(10).all { w -> w.map { it.message.id }.toSet().size == 10 }, "no repeats within ten")
    }

    @Test
    fun afterTheFirstDaysOneADayThenNothingPastTheEnd() {
        val all = TinyCareCategory.allIds()
        val plan = TinyCarePlan.upcomingThinning(at(9), denseDays = 3, sparseDays = 14, categoryIds = all, recentIds = emptyList(), max = 58, random = Random(11), zone = zone)
        val denseEnd = at(9) + 3 * 24 * 3600_000L
        val tail = plan.filter { it.atMillis > denseEnd }
        assertEquals(14, tail.size, "one a day for two weeks")
        assertEquals(14, tail.map { Instant.fromEpochMilliseconds(it.atMillis).toLocalDateTime(zone).date }.toSet().size)
        assertTrue(tail.all { hourOf(it.atMillis) in 14..15 }, "around the middle of 7:00 to 23:00")
        assertTrue(plan.size <= 58)
        assertTrue(plan.none { TinyCarePlan.isTimeInQuietWindow(hourOf(it.atMillis), 23, 7) })
        assertTrue(plan.zipWithNext().all { (a, b) -> b.atMillis > a.atMillis })
        assertTrue(plan.windowed(10).all { w -> w.map { it.message.id }.toSet().size == 10 }, "no repeats within ten")
    }

    @Test
    fun theTailNeverPassesTheLimit() {
        val plan = TinyCarePlan.upcomingThinning(at(9), denseDays = 3, sparseDays = 30, categoryIds = TinyCareCategory.allIds(), recentIds = emptyList(), max = 30, random = Random(5), zone = zone)
        assertEquals(30, plan.size)
    }

    @Test
    fun onlyTheChosenCategories() {
        val plan = TinyCarePlan.upcoming(at(9), at(21), setOf("hydration"), emptyList(), random = Random(3), zone = zone)
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.all { it.message.category == TinyCareCategory.HYDRATION })
        assertFalse(TinyCarePlan.upcoming(at(9), at(21), emptySet(), emptyList(), zone = zone).isNotEmpty())
    }
}
