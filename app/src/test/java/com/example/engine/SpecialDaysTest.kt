package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.datetime.LocalDate

class SpecialDaysTest {
    private val anniversary = LocalDate(2021, 6, 12)
    private val boy = LocalDate(1999, 12, 25)
    private val girl = LocalDate(2000, 2, 29)

    private fun on(y: Int, m: Int, d: Int) = SpecialDays.on(LocalDate(y, m, d), anniversary, boy, girl)

    @Test
    fun fixedDateFestivals() {
        assertEquals(SpecialDay.NEW_YEAR, on(2026, 12, 31))
        assertEquals(SpecialDay.NEW_YEAR, on(2027, 1, 1))
        assertEquals(SpecialDay.VALENTINES, on(2027, 2, 14))
        assertEquals(SpecialDay.CHRISTMAS, on(2026, 12, 24))
        assertNull(on(2026, 10, 5))
    }

    @Test
    fun diwaliIsTheDayAndItsEve() {
        assertEquals(SpecialDay.DIWALI, on(2026, 11, 7))
        assertEquals(SpecialDay.DIWALI, on(2026, 11, 8))
        assertNull(on(2026, 11, 9))
        assertEquals(SpecialDay.DIWALI, on(2027, 10, 29))
    }

    @Test
    fun holiCoversTheBonfireAndTheColours() {
        assertEquals(SpecialDay.HOLI, on(2026, 3, 3))
        assertEquals(SpecialDay.HOLI, on(2026, 3, 4))
        assertNull(on(2026, 3, 5))
        assertEquals(SpecialDay.HOLI, on(2029, 3, 1))
    }

    @Test
    fun theCouplesOwnDaysComeFirst() {
        // The boy's birthday is on Christmas Day.
        assertEquals(SpecialDay.BOY_BIRTHDAY, on(2026, 12, 25))
        assertEquals(SpecialDay.ANNIVERSARY, on(2026, 6, 12))
        assertEquals(5, SpecialDays.yearsTogether(LocalDate(2026, 6, 12), anniversary))
    }

    @Test
    fun noAnniversaryOnTheDayTheyMet() {
        assertNull(SpecialDays.on(LocalDate(2021, 6, 12), anniversary, null, null))
    }

    @Test
    fun leapDayBirthdaysFallOnTheTwentyEighth() {
        assertEquals(SpecialDay.GIRL_BIRTHDAY, on(2027, 2, 28))
        assertEquals(SpecialDay.GIRL_BIRTHDAY, on(2028, 2, 29))
        assertNull(on(2028, 2, 28))
    }
}
