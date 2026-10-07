package com.example.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FridayFoxTest {
    // 2026-10-09 is a Friday.
    private val friday = LocalDate(2026, 10, 9)
    private val saturday = LocalDate(2026, 10, 10)
    private val thursday = LocalDate(2026, 10, 15)

    @Test
    fun fridaysAndTheLastOne() {
        assertTrue(FridayFox.isFriday(friday))
        assertFalse(FridayFox.isFriday(saturday))
        assertEquals(friday, FridayFox.lastFriday(saturday))
        assertEquals(friday, FridayFox.lastFriday(thursday))
        // Strictly before: on a Friday it's the week before.
        assertEquals(LocalDate(2026, 10, 2), FridayFox.lastFriday(friday))
    }

    @Test
    fun aMissedFridayLeavesTheBallUntilItsPickedUp() {
        val store = FoxStore(InMemoryKeyValueStorage())
        store.noteSeen(LocalDate(2026, 10, 1))
        assertEquals(friday, FridayFox.ballWaiting(store, saturday))
        assertEquals(friday, FridayFox.ballWaiting(store, thursday))
        store.markBallCollected(friday)
        assertNull(FridayFox.ballWaiting(store, thursday))
    }

    @Test
    fun noBallWhenTheFoxCameOrTheyHadNoAppYetOrItsFridayAgain() {
        val visitedStore = FoxStore(InMemoryKeyValueStorage()).apply { noteSeen(LocalDate(2026, 10, 1)); markVisited(friday) }
        assertNull(FridayFox.ballWaiting(visitedStore, saturday))

        val newInstall = FoxStore(InMemoryKeyValueStorage()).apply { noteSeen(saturday) }
        assertNull(FridayFox.ballWaiting(newInstall, saturday))

        val missed = FoxStore(InMemoryKeyValueStorage()).apply { noteSeen(LocalDate(2026, 10, 1)) }
        assertNull(FridayFox.ballWaiting(missed, LocalDate(2026, 10, 16)), "On a Friday the fox itself is coming")
    }

    @Test
    fun firstSeenOnlyMovesEarlier() {
        val store = FoxStore(InMemoryKeyValueStorage())
        store.noteSeen(saturday)
        store.noteSeen(thursday)
        assertEquals(saturday, store.firstSeen())
        store.markVisited(friday)
        assertEquals(1, store.visits())
    }
}
