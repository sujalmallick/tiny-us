package com.example.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number

/** One of the two of them: the boy's or the girl's slot (whatever names they chose). */
enum class Partner { BOY, GIRL;

    val other: Partner get() = if (this == BOY) GIRL else BOY
}

/**
 * Birthday dates (plan 09, A). Birthdays are saved as `yyyy-mm-dd`, as always; a birthday whose
 * year they'd rather not say is saved with [NO_YEAR], so no age is ever shown for it.
 */
object Birthdays {
    /** The year saved when none is given: a leap year, so 29 February still fits. */
    const val NO_YEAR = 1904
    /** The partner is offered a sealed letter from this many days before a birthday. */
    const val LETTER_HINT_DAYS = 14
    /** A party missed on the day (the app wasn't opened) is still held up to this many days late. */
    const val BELATED_DAYS = 3

    fun parse(text: String?): LocalDate? =
        text?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun format(month: Int, day: Int, year: Int?): String =
        LocalDate(year ?: NO_YEAR, month, day).toString()

    fun hasYear(birthday: LocalDate): Boolean = birthday.year != NO_YEAR

    /** The birthday's date in [year]; 29 February falls on 28 February in other years. */
    fun inYear(birthday: LocalDate, year: Int): LocalDate {
        val month = birthday.month.number
        val day = if (month == 2 && birthday.day == 29 && !isLeapYear(year)) 28 else birthday.day
        return LocalDate(year, month, day)
    }

    /** The next time the birthday comes round, today included. */
    fun next(birthday: LocalDate, today: LocalDate): LocalDate {
        val thisYear = inYear(birthday, today.year)
        return if (thisYear >= today) thisYear else inYear(birthday, today.year + 1)
    }

    /** The most recent birthday, today included. */
    fun last(birthday: LocalDate, today: LocalDate): LocalDate {
        val thisYear = inYear(birthday, today.year)
        return if (thisYear <= today) thisYear else inYear(birthday, today.year - 1)
    }

    fun daysUntil(birthday: LocalDate, today: LocalDate): Int = today.daysUntil(next(birthday, today))

    fun isToday(birthday: LocalDate, today: LocalDate): Boolean = daysUntil(birthday, today) == 0

    /** The age turned on [birthdayThisYear], or null when the year is unknown or doesn't make sense. */
    fun ageOn(birthday: LocalDate, birthdayThisYear: LocalDate): Int? {
        if (!hasYear(birthday)) return null
        val age = birthdayThisYear.year - birthday.year
        return age.takeIf { it in 1..120 }
    }

    /**
     * Whose party is due on [today]: a birthday today, or one missed in the last [BELATED_DAYS]
     * days, that hasn't had its party this year. [heldOn] gives the date of each partner's last party.
     */
    fun partyDue(today: LocalDate, birthdays: Map<Partner, LocalDate?>, heldOn: (Partner) -> LocalDate?): List<Partner> =
        Partner.entries.filter { p ->
            val birthday = birthdays[p] ?: return@filter false
            val last = last(birthday, today)
            val late = last.daysUntil(today)
            val held = heldOn(p)
            late in 0..BELATED_DAYS && (held == null || held < last)
        }

    /** True when the party for [partner] is being held after the day itself. */
    fun isBelated(birthday: LocalDate, today: LocalDate): Boolean = last(birthday, today) != today

    /** Whose birthday is coming within [LETTER_HINT_DAYS] (not today), the soonest first. */
    fun upcoming(today: LocalDate, birthdays: Map<Partner, LocalDate?>): Partner? =
        Partner.entries
            .mapNotNull { p -> birthdays[p]?.let { p to daysUntil(it, today) } }
            .filter { it.second in 1..LETTER_HINT_DAYS }
            .minByOrNull { it.second }
            ?.first

    private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
