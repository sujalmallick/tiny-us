package com.example.engine

import com.example.data.CoupleDates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/** Days the world dresses up for (plan 06, G2). */
enum class SpecialDay { ANNIVERSARY, BOY_BIRTHDAY, GIRL_BIRTHDAY, NEW_YEAR, VALENTINES, HOLI, DIWALI, CHRISTMAS }

/**
 * Which special day today is, if any. The couple's own days (from the profile) come before
 * festivals: a birthday on Christmas Day is a birthday.
 */
object SpecialDays {
    /**
     * Diwali (Lakshmi Puja day). The world dresses up on Choti Diwali, the day before, too.
     * Checked against two panchang sources in October 2026; 2031 to 2035 from drikpanchang
     * (October 2026). Add years before 2036.
     */
    private val DIWALI = listOf(
        LocalDate(2026, 11, 8), LocalDate(2027, 10, 29), LocalDate(2028, 10, 17),
        LocalDate(2029, 11, 5), LocalDate(2030, 10, 26),
        LocalDate(2031, 11, 14), LocalDate(2032, 11, 2), LocalDate(2033, 10, 22),
        LocalDate(2034, 11, 10), LocalDate(2035, 10, 30)
    )

    /**
     * Holi: the bonfire evening (Holika Dahan) and the next day's colours. Sources differ by a
     * day on which of the two they list, so each date opens a two-day window that covers both
     * readings. Checked in October 2026; 2031 to 2035 are Holika Dahan from vedpanchang
     * (October 2026). Add years before 2036.
     */
    private val HOLI = listOf(
        LocalDate(2026, 3, 3), LocalDate(2027, 3, 22), LocalDate(2028, 3, 11),
        LocalDate(2029, 2, 28), LocalDate(2030, 3, 20),
        LocalDate(2031, 3, 8), LocalDate(2032, 3, 26), LocalDate(2033, 3, 15),
        LocalDate(2034, 3, 4), LocalDate(2035, 3, 23)
    )

    /** Shows one special day everywhere instead of today's; for previews and tests only. */
    var override: SpecialDay? = null

    /** What [date] is, given the couple's [anniversary] (the day they got together) and birthdays. */
    fun on(date: LocalDate, anniversary: LocalDate?, boyBirthday: LocalDate?, girlBirthday: LocalDate?): SpecialDay? {
        if (anniversary != null && date.year > anniversary.year && sameDay(date, anniversary)) return SpecialDay.ANNIVERSARY
        if (boyBirthday != null && sameDay(date, boyBirthday)) return SpecialDay.BOY_BIRTHDAY
        if (girlBirthday != null && sameDay(date, girlBirthday)) return SpecialDay.GIRL_BIRTHDAY
        val md = date.month.number to date.day
        return when {
            md == 12 to 31 || md == 1 to 1 -> SpecialDay.NEW_YEAR
            md == 2 to 14 -> SpecialDay.VALENTINES
            md == 12 to 24 || md == 12 to 25 -> SpecialDay.CHRISTMAS
            DIWALI.any { date == it || date == it.minus(1, DateTimeUnit.DAY) } -> SpecialDay.DIWALI
            HOLI.any { date == it || date == it.plus(1, DateTimeUnit.DAY) } -> SpecialDay.HOLI
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
        if (event.month.number == 2 && event.day == 29 && !isLeapYear(date.year)) {
            return date.month.number == 2 && date.day == 28
        }
        return date.month.number == event.month.number && date.day == event.day
    }

    private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private var cachedEpochDay = Long.MIN_VALUE
    private var cachedDay: SpecialDay? = null

    /** Today's special day, from the couple's profile; worked out once a day (it's asked every frame). */
    fun today(): SpecialDay? {
        override?.let { return it }
        val now = CoupleDates.today()
        if (now.toEpochDays().toLong() != cachedEpochDay) {
            cachedEpochDay = now.toEpochDays().toLong()
            cachedDay = on(now, CoupleDates.anniversary, CoupleDates.boyBirthday, CoupleDates.girlBirthday)
        }
        return cachedDay
    }

    /** Forget today's answer, e.g. after the couple edits their dates. */
    fun refresh() {
        cachedEpochDay = Long.MIN_VALUE
    }
}
