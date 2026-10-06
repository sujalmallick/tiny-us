package com.example.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BirthdaysTest {
    private val boy = LocalDate(1998, 3, 14)
    private val girl = LocalDate(Birthdays.NO_YEAR, 11, 2)

    @Test
    fun nextAndDaysUntil() {
        val today = LocalDate(2026, 10, 6)
        assertEquals(LocalDate(2027, 3, 14), Birthdays.next(boy, today))
        assertEquals(LocalDate(2026, 11, 2), Birthdays.next(girl, today))
        assertEquals(27, Birthdays.daysUntil(girl, today))
        assertTrue(Birthdays.isToday(boy, LocalDate(2027, 3, 14)))
    }

    @Test
    fun leapDayFallsOnTheTwentyEighth() {
        val leap = LocalDate(2000, 2, 29)
        assertEquals(LocalDate(2027, 2, 28), Birthdays.next(leap, LocalDate(2027, 1, 1)))
        assertEquals(LocalDate(2028, 2, 29), Birthdays.next(leap, LocalDate(2028, 1, 1)))
        assertTrue(Birthdays.isToday(leap, LocalDate(2027, 2, 28)))
        // A 29 February birthday without a year still fits (NO_YEAR is a leap year).
        assertEquals("1904-02-29", Birthdays.format(2, 29, null))
    }

    @Test
    fun ageOnlyWithAYear() {
        assertEquals(28, Birthdays.ageOn(boy, LocalDate(2026, 3, 14)))
        assertNull(Birthdays.ageOn(girl, LocalDate(2026, 11, 2)))
        assertFalse(Birthdays.hasYear(girl))
    }

    @Test
    fun partyDueOnTheDayOrUpToThreeDaysLateAndOnlyOnce() {
        val birthdays = mapOf(Partner.BOY to boy, Partner.GIRL to girl)
        val day = LocalDate(2027, 3, 14)
        assertEquals(listOf(Partner.BOY), Birthdays.partyDue(day, birthdays) { null })
        assertEquals(listOf(Partner.BOY), Birthdays.partyDue(LocalDate(2027, 3, 17), birthdays) { null })
        assertTrue(Birthdays.partyDue(LocalDate(2027, 3, 18), birthdays) { null }.isEmpty())
        assertTrue(Birthdays.partyDue(LocalDate(2027, 3, 13), birthdays) { null }.isEmpty())
        // Held this year: not again. Held last year: due again.
        assertTrue(Birthdays.partyDue(day, birthdays) { LocalDate(2027, 3, 14) }.isEmpty())
        assertEquals(listOf(Partner.BOY), Birthdays.partyDue(day, birthdays) { LocalDate(2026, 3, 15) })
        assertTrue(Birthdays.isBelated(boy, LocalDate(2027, 3, 16)))
        assertFalse(Birthdays.isBelated(boy, day))
    }

    @Test
    fun bothOnTheSameDay() {
        val same = mapOf(Partner.BOY to LocalDate(1999, 6, 1), Partner.GIRL to LocalDate(2000, 6, 1))
        assertEquals(listOf(Partner.BOY, Partner.GIRL), Birthdays.partyDue(LocalDate(2026, 6, 1), same) { null })
    }

    @Test
    fun letterHintTwoWeeksBefore() {
        val birthdays = mapOf(Partner.BOY to boy, Partner.GIRL to girl)
        assertEquals(Partner.GIRL, Birthdays.upcoming(LocalDate(2026, 10, 20), birthdays))
        assertNull(Birthdays.upcoming(LocalDate(2026, 10, 18), birthdays)) // 15 days out
        assertNull(Birthdays.upcoming(LocalDate(2026, 11, 2), birthdays)) // the day itself
    }

    @Test
    fun sealedLettersStayHiddenUntilTheirDay() {
        val store = BirthdayStore(InMemoryKeyValueStorage())
        val day = LocalDate(2026, 11, 2)
        val letter = store.seal(Partner.GIRL, LetterKind.BIRTHDAY, "Happy birthday, love", "Bean", opensOn = day)
        assertFalse(letter.canOpen(LocalDate(2026, 11, 1)))
        assertTrue(letter.canOpen(day))
        assertNotNull(store.birthdayLetter(Partner.GIRL, day))
        assertNull(store.birthdayLetter(Partner.BOY, day))

        store.markOpened(letter.id)
        assertNull(store.birthdayLetter(Partner.GIRL, day))
        assertTrue(store.letters().single().isOpened)
    }

    @Test
    fun storeSurvivesAReload() {
        val storage = InMemoryKeyValueStorage()
        val store = BirthdayStore(storage)
        store.setBirthday(Partner.GIRL, "1904-11-02")
        store.seal(Partner.BOY, LetterKind.OPEN_WHEN, "Breathe. I'm here.", "Sprout", occasion = "when you can't sleep")
        store.recordParty(BirthdayRecord(Partner.GIRL, "2026-11-02", wish = "a little garden"))

        val again = BirthdayStore(storage)
        assertEquals(LocalDate(1904, 11, 2), again.birthdays()[Partner.GIRL])
        assertEquals("when you can't sleep", again.letters().single().occasion)
        assertEquals("a little garden", again.records().single().wish)
        assertEquals(LocalDate(2026, 11, 2), again.partyHeldOn(Partner.GIRL))
        assertTrue(again.partyDue(LocalDate(2026, 11, 2)).isEmpty())
    }
}
