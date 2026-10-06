package com.example.care

import com.example.data.CoupleDates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import com.example.resources.*

/** Which of their dates a morning reminder is for (plan 09, C-6). */
enum class DateMorning { ANNIVERSARY, MONTHIVERSARY }

/**
 * Opt-in morning reminders for the couple's own dates: the anniversary every year, and the
 * "month-iversary" (the same day each month, the last day in shorter months). At 9:00, like the
 * birthday morning. Each platform schedules them at [nextMillis].
 */
object DateMornings {
    const val HOUR = BirthdayMorning.HOUR

    /** What [day] is, given they got together on [start]; null for an ordinary day. */
    fun on(day: LocalDate, start: LocalDate): DateMorning? {
        if (day <= start) return null
        if (sameMonthDay(day, start, day.year)) return DateMorning.ANNIVERSARY
        val target = minOf(start.day, lastDay(day.year, day.month.ordinal + 1))
        return if (day.day == target) DateMorning.MONTHIVERSARY else null
    }

    /** Months together on a month-iversary [day]. */
    fun monthsTogether(day: LocalDate, start: LocalDate): Int =
        (day.year - start.year) * 12 + (day.month.ordinal - start.month.ordinal)

    /**
     * The next reminder after [nowMillis] (today's 9:00 included if it's still ahead), for the
     * kinds switched on, or null.
     */
    fun nextMillis(
        start: LocalDate,
        anniversary: Boolean,
        monthiversary: Boolean,
        nowMillis: Long,
        today: LocalDate = CoupleDates.today(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Long? {
        if (!anniversary && !monthiversary) return null
        var day = today
        repeat(400) {
            val kind = on(day, start)
            val wanted = (kind == DateMorning.ANNIVERSARY && anniversary) || (kind == DateMorning.MONTHIVERSARY && monthiversary)
            if (wanted) {
                val at = LocalDateTime(day.year, day.month, day.day, HOUR, 0).toInstant(zone).toEpochMilliseconds()
                if (at > nowMillis) return at
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return null
    }

    private fun sameMonthDay(day: LocalDate, start: LocalDate, year: Int): Boolean {
        val d = if (start.month.ordinal == 1 && start.day == 29 && !isLeap(year)) 28 else start.day
        return day.month == start.month && day.day == d
    }

    private fun lastDay(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeap(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
}

/**
 * All the opt-in mornings together (plan 09, A and C): birthdays, the anniversary and the
 * month-iversary. One alarm at the soonest; on the day, [messageFor] says what to show (a
 * birthday wins, and never says whose).
 */
object CoupleMornings {
    fun nextMillis(
        birthdays: com.example.data.BirthdayStore,
        life: com.example.data.CoupleLifeStore,
        start: LocalDate?,
        nowMillis: Long,
        today: LocalDate = CoupleDates.today(),
        zone: TimeZone = TimeZone.currentSystemDefault(),
        /** The festivals (plan 09, D): a little note on each one's first morning (always on). */
        festivals: com.example.data.FestivalStore? = null,
        southern: Boolean = false
    ): Long? {
        val birthday = BirthdayMorning.nextMillis(birthdays, nowMillis, today, zone)
        val dates = start?.let { DateMornings.nextMillis(it, life.anniversaryReminder, life.monthiversaryReminder, nowMillis, today, zone) }
        val festival = festivals?.let { nextFestivalMillis(southern, nowMillis, today, zone) }
        return listOfNotNull(birthday, dates, festival).minOrNull()
    }

    /** 9:00 on the next festival's first day (this year's if still ahead, else next year's). */
    fun nextFestivalMillis(southern: Boolean, nowMillis: Long, today: LocalDate, zone: TimeZone): Long? =
        com.example.data.Festival.entries.flatMap { f ->
            listOf(today.year, today.year + 1).map { y ->
                LocalDateTime(y, com.example.data.Festivals.month(f, southern), com.example.data.Festivals.FIRST_DAY, DateMornings.HOUR, 0)
                    .toInstant(zone).toEpochMilliseconds()
            }
        }.filter { it > nowMillis }.minOrNull()

    /** The note for a festival's first morning. */
    fun festivalNote(f: com.example.data.Festival): org.jetbrains.compose.resources.StringResource = when (f) {
        com.example.data.Festival.BLOSSOM_PICNIC -> Res.string.fest_note_blossom_picnic
        com.example.data.Festival.LANTERN_NIGHT -> Res.string.fest_note_lantern_night
        com.example.data.Festival.GIFT_EXCHANGE -> Res.string.fest_note_gift_exchange
    }

    /** The reminder's text for [today], or null when nothing switched on falls today. */
    fun messageFor(
        today: LocalDate,
        birthdays: com.example.data.BirthdayStore,
        life: com.example.data.CoupleLifeStore,
        start: LocalDate?,
        festivals: com.example.data.FestivalStore? = null,
        southern: Boolean = false
    ): org.jetbrains.compose.resources.StringResource? {
        if (birthdays.morningReminderEnabled &&
            birthdays.birthdays().values.filterNotNull().any { com.example.data.Birthdays.isToday(it, today) }
        ) return Res.string.bday_morning_reminder
        // A festival's first day (a birthday still comes first).
        if (festivals != null) {
            com.example.data.Festival.entries.firstOrNull { f ->
                today.month.ordinal + 1 == com.example.data.Festivals.month(f, southern) && today.day == com.example.data.Festivals.FIRST_DAY
            }?.let { return festivalNote(it) }
        }
        return when (start?.let { DateMornings.on(today, it) }) {
            DateMorning.ANNIVERSARY -> if (life.anniversaryReminder) Res.string.reminder_anniversary else null
            DateMorning.MONTHIVERSARY -> if (life.monthiversaryReminder) Res.string.reminder_monthiversary else null
            null -> null
        }
    }
}
