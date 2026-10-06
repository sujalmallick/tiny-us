package com.example.data

import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** One day on the couple's special calendar (their beginning, birthdays, kisses, meetings). */
data class TinyUsMemory(
    val id: String,
    val date: LocalDate,
    val title: String,
    val description: String,
    val type: SpecialMemoryType,
    val isPrivate: Boolean = false,
    val isFuture: Boolean = false,
    val annualRecurring: Boolean = false
)

/** How long they have been together, down to the second. */
data class RelationshipDuration(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val totalDays: Long
)

/**
 * The couple's calendar for shared code: special days, countdowns and time together, from
 * [CoupleDates]. Android's SpecialCalendarManager and RelationshipTimeManager keep their java.time
 * API on top of this.
 */
object CoupleCalendar {
    /** The time of day they got together (midnight unless set). */
    var startTime: LocalTime = LocalTime(0, 0)

    /** Memories entered by the couple instead of the generated ones, if any. */
    var customMemories: List<TinyUsMemory>? = null

    var boyName: String = PersonalProfile.DEFAULT_NAME_A
    var girlName: String = PersonalProfile.DEFAULT_NAME_B

    fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    /** Their beginning (every year) and both birthdays, unless the couple entered their own list. */
    val fixedMemories: List<TinyUsMemory>
        get() {
            customMemories?.let { return it }
            val list = mutableListOf(
                TinyUsMemory(
                    id = "our_beginning",
                    date = CoupleDates.anniversary,
                    title = "Our Beginning",
                    description = "Where our story began. The first day of forever.",
                    type = SpecialMemoryType.RELATIONSHIP,
                    annualRecurring = true
                )
            )
            CoupleDates.boyBirthday?.let { bDate ->
                list.add(
                    TinyUsMemory(
                        id = "boy_birthday",
                        date = bDate,
                        title = "${boyName}'s Birthday",
                        description = "Celebrating the most wonderful person in the world!",
                        type = SpecialMemoryType.BIRTHDAY,
                        annualRecurring = true
                    )
                )
            }
            CoupleDates.girlBirthday?.let { gDate ->
                list.add(
                    TinyUsMemory(
                        id = "girl_birthday",
                        date = gDate,
                        title = "${girlName}'s Birthday",
                        description = "Celebrating the most special, beautiful soul!",
                        type = SpecialMemoryType.BIRTHDAY,
                        annualRecurring = true
                    )
                )
            }
            return list
        }

    fun getMemoriesForDate(date: LocalDate): List<TinyUsMemory> =
        fixedMemories.filter { mem ->
            if (mem.annualRecurring) {
                mem.date.month == date.month && mem.date.day == date.day
            } else {
                mem.date == date
            }
        }

    fun hasMemoryOnDate(date: LocalDate): Boolean = getMemoriesForDate(date).isNotEmpty()

    /** Time left until [targetDate] begins (wall-clock arithmetic, as java.time's LocalDateTime does). */
    fun calculateCountdown(targetDate: LocalDate, now: LocalDateTime = now()): LiveCountdown {
        val target = targetDate.atStartOfDayIn(TimeZone.UTC)
        val totalSecs = (target - now.toInstant(TimeZone.UTC)).inWholeSeconds
        return RelationshipTimeCalculator.computeCountdown(totalSecs, now.date == targetDate)
    }

    /** Day N together (day 1 is the anniversary). */
    fun tinyUsDay(current: LocalDate = CoupleDates.today()): Long =
        RelationshipTimeCalculator.calculateTinyUsDay(CoupleDates.anniversary, current)

    /** Years, months, days and the clock since they got together. */
    fun exactDuration(now: LocalDateTime = now()): RelationshipDuration {
        val start = CoupleDates.anniversary
        val startDateTime = LocalDateTime(start, startTime)
        if (now < startDateTime) return RelationshipDuration(0, 0, 0, 0, 0, 0, 1)

        val totalDays = tinyUsDay(now.date)

        // Calendar years, months and days; the day only counts once its start time has passed.
        val endDateForPeriod = if (now.time < startTime) now.date.minus(1, DateTimeUnit.DAY) else now.date
        val period = start.periodUntil(endDateForPeriod)

        // Hours, minutes and seconds within the current 24-hour cycle.
        val nowSecondsOfDay = now.time.toSecondOfDay()
        val startSecondsOfDay = startTime.toSecondOfDay()
        val secondsDiff = if (nowSecondsOfDay >= startSecondsOfDay) {
            (nowSecondsOfDay - startSecondsOfDay).toLong()
        } else {
            (86400 - startSecondsOfDay + nowSecondsOfDay).toLong()
        }

        return RelationshipDuration(
            years = period.years,
            months = period.months,
            days = period.days,
            hours = secondsDiff / 3600,
            minutes = (secondsDiff % 3600) / 60,
            seconds = secondsDiff % 60,
            totalDays = totalDays
        )
    }
}
