package com.example.data

import com.example.engine.SpecialDay
import com.example.engine.SpecialDays
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlinx.datetime.toInstant
import com.example.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FestivalsTest {
    @AfterTest
    fun clear() { Festivals.override = null }

    @Test
    fun threeFestivalsMidSeasonWithAPosterBefore() {
        assertEquals(3, Festival.entries.size) // no Harvest Fair
        assertEquals(FestivalDay(Festival.BLOSSOM_PICNIC, FestivalPhase.ON, 2027), Festivals.on(LocalDate(2027, 4, 14), southern = false))
        assertEquals(FestivalPhase.ON, Festivals.on(LocalDate(2027, 4, 16), false)?.phase)
        assertNull(Festivals.on(LocalDate(2027, 4, 17), false))
        assertEquals(FestivalDay(Festival.BLOSSOM_PICNIC, FestivalPhase.POSTER, 2027), Festivals.on(LocalDate(2027, 4, 12), false))
        assertNull(Festivals.on(LocalDate(2027, 4, 11), false))
        assertEquals(Festival.LANTERN_NIGHT, Festivals.on(LocalDate(2027, 7, 15), false)?.festival)
        assertEquals(Festival.GIFT_EXCHANGE, Festivals.on(LocalDate(2027, 1, 15), false)?.festival)
        assertNull(Festivals.on(LocalDate(2027, 10, 15), false)) // autumn: none
    }

    @Test
    fun theSouthernHemisphereIsSixMonthsAhead() {
        assertEquals(Festival.LANTERN_NIGHT, Festivals.on(LocalDate(2027, 1, 15), southern = true)?.festival)
        assertEquals(Festival.GIFT_EXCHANGE, Festivals.on(LocalDate(2027, 7, 15), southern = true)?.festival)
        assertEquals(Festival.BLOSSOM_PICNIC, Festivals.on(LocalDate(2027, 10, 15), southern = true)?.festival)
        assertTrue(Festivals.isSouthern("AU"))
        assertFalse(Festivals.isSouthern("IN"))
    }

    @Test
    fun theOverrideShowsAFestivalOnAnyDay() {
        Festivals.override = Festival.GIFT_EXCHANGE
        assertEquals(FestivalPhase.ON, Festivals.on(LocalDate(2027, 5, 3), false)?.phase)
    }

    @Test
    fun picksAndYearsAreKept() {
        val storage = InMemoryKeyValueStorage()
        val store = FestivalStore(storage)
        assertFalse(store.celebrated(Festival.LANTERN_NIGHT, 2027))
        store.markCelebrated(Festival.LANTERN_NIGHT, 2027)
        store.markCelebrated(Festival.LANTERN_NIGHT, 2028)
        store.savePicks(Festival.GIFT_EXCHANGE, 2028, FestivalPicks(boy = "discovery:SEASHELL", girl = "card", girlNote = "for you"))
        val again = FestivalStore(storage)
        assertEquals(setOf(2027, 2028), again.years(Festival.LANTERN_NIGHT))
        assertEquals("for you", again.picks(Festival.GIFT_EXCHANGE, 2028).girlNote)
        assertFalse(again.picks(Festival.GIFT_EXCHANGE, 2028).revealed)
        assertEquals(FestivalPicks(), again.picks(Festival.BLOSSOM_PICNIC, 2028))
    }

    @Test
    fun lanternWishesWaitAYear() {
        val letters = BirthdayStore(InMemoryKeyValueStorage())
        val wish = letters.seal(Partner.GIRL, LetterKind.LANTERN, "a little garden", "Sprout", opensOn = LocalDate(2028, 7, 14))
        assertFalse(wish.canOpen(LocalDate(2027, 7, 15)))
        assertTrue(wish.canOpen(LocalDate(2028, 7, 14)))
    }

    @Test
    fun diwaliAndHoliGoOnPast2030() {
        assertEquals(SpecialDay.DIWALI, SpecialDays.on(LocalDate(2031, 11, 14), null, null, null))
        assertEquals(SpecialDay.DIWALI, SpecialDays.on(LocalDate(2034, 11, 9), null, null, null)) // Choti Diwali
        assertEquals(SpecialDay.HOLI, SpecialDays.on(LocalDate(2032, 3, 27), null, null, null))
        assertEquals(SpecialDay.HOLI, SpecialDays.on(LocalDate(2035, 3, 23), null, null, null))
    }
}

class FestivalNotesTest {
    private val zone = kotlinx.datetime.TimeZone.UTC

    @Test
    fun aLittleNoteOnEachFestivalsFirstMorning() {
        val storage = InMemoryKeyValueStorage()
        val birthdays = BirthdayStore(storage)
        val life = CoupleLifeStore(storage)
        val festivals = FestivalStore(storage)
        val note = com.example.care.CoupleMornings.messageFor(LocalDate(2027, 7, 14), birthdays, life, null, festivals)
        assertEquals(Res.string.fest_note_lantern_night, note)
        assertNull(com.example.care.CoupleMornings.messageFor(LocalDate(2027, 7, 15), birthdays, life, null, festivals)) // only the first day
        // Without the festivals (an older caller), no note.
        assertNull(com.example.care.CoupleMornings.messageFor(LocalDate(2027, 7, 14), birthdays, life, null))
    }

    @Test
    fun theNextNoteIsTheNextFestivalsFirstMorning() {
        val today = LocalDate(2027, 5, 1)
        val now = kotlinx.datetime.LocalDateTime(2027, 5, 1, 12, 0).toInstant(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
        val next = com.example.care.CoupleMornings.nextFestivalMillis(southern = false, nowMillis = now, today = today, zone = zone)
        val expected = kotlinx.datetime.LocalDateTime(2027, 7, 14, 9, 0).toInstant(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
        assertEquals(expected, next)
    }
}
