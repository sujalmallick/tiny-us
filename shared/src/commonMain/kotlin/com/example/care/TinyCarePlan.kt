package com.example.care

import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** A Tiny Care reminder planned for a time. */
data class PlannedReminder(val atMillis: Long, val message: TinyCareMessage)

/**
 * When Tiny Care reminders go off, the same rules on every platform: the first one 15 to 30
 * minutes after turning them on, then every 90 to 150 minutes, and never in the quiet hours (a
 * reminder that would land there moves to 15 to 45 minutes after they end). Android rolls one
 * reminder at a time; iOS cannot run code in the background, so it plans a few days ahead with
 * [upcoming] and plans again whenever the app opens.
 */
object TinyCarePlan {
    fun isTimeInQuietWindow(hour: Int, startHour: Int, endHour: Int): Boolean =
        if (startHour > endHour) {
            // e.g. 23 (11 PM) to 7 (7 AM)
            hour >= startHour || hour < endHour
        } else {
            // e.g. 1 AM to 8 AM
            hour in startHour until endHour
        }

    /** The next reminder after [fromMillis]; [first] is the quick first check-in. */
    fun nextTime(
        fromMillis: Long,
        quietStartHour: Int = 23,
        quietEndHour: Int = 7,
        first: Boolean = false,
        random: Random = Random.Default,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Long {
        val intervalMinutes = if (first) random.nextInt(15, 31) else random.nextInt(90, 151)
        val target = fromMillis + intervalMinutes * 60_000L
        val targetHour = Instant.fromEpochMilliseconds(target).toLocalDateTime(zone).hour
        if (!isTimeInQuietWindow(targetHour, quietStartHour, quietEndHour)) return target

        // To the morning: the quiet hours' end, plus 15 to 45 minutes.
        val now = Instant.fromEpochMilliseconds(fromMillis).toLocalDateTime(zone)
        val day = if (now.hour >= quietEndHour) now.date.plus(1, DateTimeUnit.DAY) else now.date
        val morning = LocalDateTime(day, LocalTime(quietEndHour, 0)).toInstant(zone).toEpochMilliseconds()
        return morning + random.nextInt(15, 46) * 60_000L
    }

    /**
     * Reminders from [fromMillis] until [untilMillis] (at most [max]), each with a message from the
     * chosen categories that wasn't shown recently.
     */
    fun upcoming(
        fromMillis: Long,
        untilMillis: Long,
        categoryIds: Set<String>,
        recentIds: List<String>,
        quietStartHour: Int = 23,
        quietEndHour: Int = 7,
        firstSoon: Boolean = false,
        max: Int = 40,
        random: Random = Random.Default,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): List<PlannedReminder> {
        val plan = mutableListOf<PlannedReminder>()
        val recent = recentIds.toMutableList()
        var at = nextTime(fromMillis, quietStartHour, quietEndHour, first = firstSoon, random = random, zone = zone)
        while (at <= untilMillis && plan.size < max) {
            val message = TinyCareMessagePool.pickMessage(categoryIds, recent) ?: break
            plan += PlannedReminder(at, message)
            recent.remove(message.id)
            recent.add(0, message.id)
            if (recent.size > 10) recent.removeAt(recent.lastIndex)
            at = nextTime(at, quietStartHour, quietEndHour, random = random, zone = zone)
        }
        return plan
    }
}
