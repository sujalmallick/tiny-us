package com.example.data

import com.example.care.CoupleMornings
import com.example.care.DateMorning
import com.example.care.DateMornings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.random.Random
import com.example.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoupleLifeTest {
    private val zone = TimeZone.UTC

    @Test
    fun deciderSpinsOnlyWhatSurvivedBothVetoes() {
        val left = DinnerDecider.survivors(8, setOf(0, 3), setOf(3, 5))
        assertEquals(listOf(1, 2, 4, 6, 7), left)
        repeat(50) { assertTrue(DinnerDecider.spin(left, Random(it)) in left) }
        // Everything crossed out: all of it goes back on the wheel.
        assertEquals((0 until 4).toList(), DinnerDecider.survivors(4, setOf(0, 1), setOf(2, 3)))
    }

    @Test
    fun theWheelPutsOurOwnOptionsOnFirst() {
        val builtIn = (1..16).map { "Option $it" }
        val custom = listOf(DeciderOption("Mom's soup", Partner.GIRL), DeciderOption("option 3", Partner.BOY))
        val wheel = DinnerDecider.wheel(builtIn, custom, Random(1))
        assertEquals(DinnerDecider.WHEEL_SIZE, wheel.size)
        assertTrue(wheel.any { it.text == "Mom's soup" })
        // A custom one that repeats a built-in one isn't doubled.
        assertEquals(1, wheel.count { it.text.equals("option 3", ignoreCase = true) })
    }

    @Test
    fun customOptionsAreKeptPerCategory() {
        val store = CoupleLifeStore(InMemoryKeyValueStorage())
        store.addCustomOption(DeciderCategory.EAT, "  Ramen  ", Partner.BOY)
        store.addCustomOption(DeciderCategory.EAT, "ramen", Partner.GIRL) // same, replaced
        store.addCustomOption(DeciderCategory.WATCH, "Ghibli", Partner.GIRL)
        assertEquals(listOf(DeciderOption("ramen", Partner.GIRL)), store.customOptions(DeciderCategory.EAT))
        assertEquals(1, store.customOptions(DeciderCategory.WATCH).size)
        assertTrue(store.customOptions(DeciderCategory.DO).isEmpty())
    }

    @Test
    fun theJarFillsAtTwentyThenStartsAgain() {
        val storage = InMemoryKeyValueStorage()
        val store = CoupleLifeStore(storage)
        repeat(ThankYouJar.FULL - 1) { assertNull(store.addThankYou(it % 2 == 0, "thanks $it")) }
        assertEquals(19, store.thankYous().size)
        assertNull(store.addThankYou(true, "   ")) // nothing said, nothing added
        val jar = store.addThankYou(false, "for everything")
        assertNotNull(jar)
        assertEquals(20, jar.count)
        assertTrue(store.thankYous().isEmpty())
        assertEquals(1, CoupleLifeStore(storage).filledJars().size)
        assertEquals(20, CoupleLifeStore(storage).allThankYous().size)
    }

    @Test
    fun phonesDownCountsOnlyWhenItRunsItsTime() {
        val store = CoupleLifeStore(InMemoryKeyValueStorage())
        store.startPhonesDown(30, at = 1_000_000L)
        assertEquals(30 * 60L, store.phonesDownSecondsLeft(at = 1_000_000L))
        assertFalse(store.endPhonesDown(at = 1_000_000L + 10 * 60_000L)) // ended early
        assertNull(store.phonesDownSecondsLeft())
        assertTrue(store.phonesDownSessions().isEmpty())

        store.startPhonesDown(15, at = 5_000_000L)
        assertEquals(0L, store.phonesDownSecondsLeft(at = 5_000_000L + 20 * 60_000L))
        assertTrue(store.endPhonesDown(at = 5_000_000L + 20 * 60_000L))
        assertEquals(15, store.phonesDownSessions().single().minutes)
    }

    @Test
    fun keptMakeUpsAreTheOnlyOnesSaved() {
        val store = CoupleLifeStore(InMemoryKeyValueStorage())
        assertTrue(store.keptMakeUps().isEmpty())
        store.keepMakeUp("tired", "a hug", "unheard", "to talk", MakeUpChoice.TEA)
        assertEquals(MakeUpChoice.TEA, store.keptMakeUps().single().choice)
    }

    @Test
    fun anniversaryAndMonthiversaryDays() {
        val start = LocalDate(2024, 1, 31)
        assertNull(DateMornings.on(start, start))
        assertEquals(DateMorning.ANNIVERSARY, DateMornings.on(LocalDate(2026, 1, 31), start))
        assertEquals(DateMorning.MONTHIVERSARY, DateMornings.on(LocalDate(2026, 3, 31), start))
        // Shorter months: the last day.
        assertEquals(DateMorning.MONTHIVERSARY, DateMornings.on(LocalDate(2026, 2, 28), start))
        assertEquals(DateMorning.MONTHIVERSARY, DateMornings.on(LocalDate(2026, 4, 30), start))
        assertNull(DateMornings.on(LocalDate(2026, 4, 29), start))
        assertEquals(26, DateMornings.monthsTogether(LocalDate(2026, 3, 31), start))
    }

    @Test
    fun theNextMorningFollowsWhatIsSwitchedOn() {
        val start = LocalDate(2024, 5, 10)
        val today = LocalDate(2026, 4, 1)
        val now = LocalDateTime(2026, 4, 1, 12, 0).toInstant(zone).toEpochMilliseconds()
        fun at(d: LocalDate) = LocalDateTime(d.year, d.month, d.day, 9, 0).toInstant(zone).toEpochMilliseconds()
        assertNull(DateMornings.nextMillis(start, anniversary = false, monthiversary = false, nowMillis = now, today = today, zone = zone))
        assertEquals(at(LocalDate(2026, 4, 10)), DateMornings.nextMillis(start, false, true, now, today, zone))
        assertEquals(at(LocalDate(2026, 5, 10)), DateMornings.nextMillis(start, true, false, now, today, zone))
    }

    @Test
    fun aBirthdayWinsTheMorningAndNeverSaysWhose() {
        val storage = InMemoryKeyValueStorage()
        val birthdays = BirthdayStore(storage)
        val life = CoupleLifeStore(storage)
        val start = LocalDate(2024, 6, 1)
        val day = LocalDate(2026, 6, 1)
        life.anniversaryReminder = true
        assertEquals(Res.string.reminder_anniversary, CoupleMornings.messageFor(day, birthdays, life, start))
        birthdays.setBirthday(Partner.GIRL, "1998-06-01")
        birthdays.morningReminderEnabled = true
        assertEquals(Res.string.bday_morning_reminder, CoupleMornings.messageFor(day, birthdays, life, start))
        assertNull(CoupleMornings.messageFor(LocalDate(2026, 6, 2), birthdays, life, start))
    }
}
