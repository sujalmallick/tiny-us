package com.example.engine

import java.time.LocalDate
import java.time.MonthDay

/** Days the world dresses up for (plan 06, G2). */
enum class SpecialDay { ANNIVERSARY, BOY_BIRTHDAY, GIRL_BIRTHDAY, NEW_YEAR, VALENTINES, HOLI, DIWALI, CHRISTMAS }

/**
 * Which special day today is, if any. The couple's own days (from the profile) come before
 * festivals: a birthday on Christmas Day is a birthday.
 */
object SpecialDays {
    /**
     * Diwali (Lakshmi Puja day). The world dresses up on Choti Diwali, the day before, too.
     * Checked against two panchang sources in October 2026; add years before 2031.
     */
    private val DIWALI = listOf(
        LocalDate.of(2026, 11, 8), LocalDate.of(2027, 10, 29), LocalDate.of(2028, 10, 17),
        LocalDate.of(2029, 11, 5), LocalDate.of(2030, 10, 26)
    )

    /**
     * Holi: the bonfire evening (Holika Dahan) and the next day's colours. Sources differ by a
     * day on which of the two they list, so each date opens a two-day window that covers both
     * readings. Checked in October 2026; add years before 2031.
     */
    private val HOLI = listOf(
        LocalDate.of(2026, 3, 3), LocalDate.of(2027, 3, 22), LocalDate.of(2028, 3, 11),
        LocalDate.of(2029, 2, 28), LocalDate.of(2030, 3, 20)
    )

    /** Shows one special day everywhere instead of today's; for previews and tests only. */
    @androidx.annotation.VisibleForTesting
    var override: SpecialDay? = null

    /** What [date] is, given the couple's [anniversary] (the day they got together) and birthdays. */
    fun on(date: LocalDate, anniversary: LocalDate?, boyBirthday: LocalDate?, girlBirthday: LocalDate?): SpecialDay? {
        if (anniversary != null && date.year > anniversary.year && sameDay(date, anniversary)) return SpecialDay.ANNIVERSARY
        if (boyBirthday != null && sameDay(date, boyBirthday)) return SpecialDay.BOY_BIRTHDAY
        if (girlBirthday != null && sameDay(date, girlBirthday)) return SpecialDay.GIRL_BIRTHDAY
        val md = MonthDay.from(date)
        return when {
            md == MonthDay.of(12, 31) || md == MonthDay.of(1, 1) -> SpecialDay.NEW_YEAR
            md == MonthDay.of(2, 14) -> SpecialDay.VALENTINES
            md == MonthDay.of(12, 24) || md == MonthDay.of(12, 25) -> SpecialDay.CHRISTMAS
            DIWALI.any { date == it || date == it.minusDays(1) } -> SpecialDay.DIWALI
            HOLI.any { date == it || date == it.plusDays(1) } -> SpecialDay.HOLI
            else -> null
        }
    }

    /** Years together on an anniversary [date]. */
    fun yearsTogether(date: LocalDate, anniversary: LocalDate): Int = date.year - anniversary.year

    /**
     * Same month and day, every year. A 29 February date is kept on 28 February in other years,
     * so it never goes missing.
     */
    private fun sameDay(date: LocalDate, event: LocalDate): Boolean {
        if (event.monthValue == 2 && event.dayOfMonth == 29 && !date.isLeapYear) {
            return date.monthValue == 2 && date.dayOfMonth == 28
        }
        return date.monthValue == event.monthValue && date.dayOfMonth == event.dayOfMonth
    }

    private var cachedEpochDay = Long.MIN_VALUE
    private var cachedDay: SpecialDay? = null

    /** Today's special day, from the couple's profile; worked out once a day (it's asked every frame). */
    fun today(): SpecialDay? {
        override?.let { return it }
        val now = LocalDate.now()
        if (now.toEpochDay() != cachedEpochDay) {
            cachedEpochDay = now.toEpochDay()
            cachedDay = on(
                now,
                com.example.data.RelationshipTimeManager.relationshipStartDate,
                com.example.data.SpecialCalendarManager.boyBirthday,
                com.example.data.SpecialCalendarManager.girlBirthday
            )
        }
        return cachedDay
    }

    /** Forget today's answer, e.g. after the couple edits their dates. */
    fun refresh() {
        cachedEpochDay = Long.MIN_VALUE
    }
}
