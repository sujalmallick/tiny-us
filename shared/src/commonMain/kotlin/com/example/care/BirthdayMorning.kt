package com.example.care

import com.example.data.BirthdayStore
import com.example.data.Birthdays
import com.example.data.CoupleDates
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * The opt-in morning reminder on a birthday (plan 09, A): at 9:00 on the next birthday, saying only
 * that something is waiting, so the surprise holds. Each platform schedules it at [nextMillis].
 */
object BirthdayMorning {
    const val HOUR = 9

    /** When the next reminder should go off, or null when it's off or no birthday is set. */
    fun nextMillis(
        store: BirthdayStore,
        nowMillis: Long,
        today: LocalDate = CoupleDates.today(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): Long? {
        if (!store.morningReminderEnabled) return null
        return store.birthdays().values.filterNotNull()
            .flatMap { b -> listOf(Birthdays.next(b, today), Birthdays.next(b, today.plusYear())) }
            .map { day -> LocalDateTime(day.year, day.month, day.day, HOUR, 0).toInstant(zone).toEpochMilliseconds() }
            .filter { it > nowMillis }
            .minOrNull()
    }

    private fun LocalDate.plusYear() = LocalDate(year + 1, month, if (month.ordinal == 1 && day == 29) 28 else day)
}
