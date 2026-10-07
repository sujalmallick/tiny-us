package com.example.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisitorsTest {
    private val day = LocalDate(2026, 10, 7)
    private fun epoch(d: LocalDate) = d.toEpochDays().toLong()

    @Test
    fun atMostOneVisitorADayEachAtItsOwnPace() {
        val store = VisitorStore(InMemoryKeyValueStorage())
        assertTrue(Visitors.mayCome(VisitorKind.PAINTER, store, day))
        store.noteVisit(VisitorKind.PAINTER, epoch(day))
        assertFalse(Visitors.mayCome(VisitorKind.OLD_COUPLE, store, day), "Nobody else the same day")
        val nextDay = LocalDate(2026, 10, 8)
        assertTrue(Visitors.mayCome(VisitorKind.OLD_COUPLE, store, nextDay))
        assertFalse(Visitors.mayCome(VisitorKind.PAINTER, store, nextDay), "The painter comes about once a week")
        assertTrue(Visitors.mayCome(VisitorKind.PAINTER, store, LocalDate(2026, 10, 13)))
        store.noteVisit(VisitorKind.OLD_COUPLE, epoch(nextDay))
        assertFalse(Visitors.mayCome(VisitorKind.OLD_COUPLE, store, LocalDate(2026, 10, 9)))
        assertTrue(Visitors.mayCome(VisitorKind.OLD_COUPLE, store, LocalDate(2026, 10, 10)))
    }

    @Test
    fun theirAnniversary() {
        assertTrue(Visitors.isAnniversary("2024-10-07", day))
        assertFalse(Visitors.isAnniversary("2026-10-07", day), "Not the day they got together")
        assertFalse(Visitors.isAnniversary("2024-10-08", day))
        assertFalse(Visitors.isAnniversary(null, day))
        assertFalse(Visitors.isAnniversary("nonsense", day))
    }

    @Test
    fun paintingsAndNotesAreKept() {
        val storage = InMemoryKeyValueStorage()
        val store = VisitorStore(storage)
        val p = Painting(epoch(day), "FLOWER", "SAKURA", "AFTERNOON", 2, 5, "HOLD_HANDS", true)
        store.addPainting(p)
        store.addPainting(p.copy(scene = "WALK", together = "APART", mochi = false))
        val again = VisitorStore(storage)
        assertEquals(2, again.paintings().size)
        assertEquals(p, again.paintings().first())
        assertEquals("WALK", again.paintings().last().scene)
        store.addNote(2026)
        assertEquals(setOf(2026), again.noteYears())
    }
}
