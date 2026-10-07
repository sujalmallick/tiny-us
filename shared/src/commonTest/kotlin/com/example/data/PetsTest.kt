package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PetsTest {
    @Test
    fun mochiIsTheirsFromTheStartAndTheRestAreMetOnce() {
        val store = PetStore(InMemoryKeyValueStorage())
        assertEquals(setOf(PetKind.CAT), store.met())
        assertEquals(PetKind.CAT, store.chosen)
        assertTrue(store.meet(PetKind.BUNNY))
        assertFalse(store.meet(PetKind.BUNNY))
        assertEquals(setOf(PetKind.CAT, PetKind.BUNNY), store.met())
    }

    @Test
    fun onlyAPetTheyveMetCanLiveWithThem() {
        val store = PetStore(InMemoryKeyValueStorage())
        store.chosen = PetKind.OWL
        assertEquals(PetKind.CAT, store.chosen)
        store.meet(PetKind.OWL)
        store.chosen = PetKind.OWL
        assertEquals(PetKind.OWL, store.chosen)
        store.chosen = PetKind.CAT
        assertEquals(PetKind.CAT, store.chosen)
    }

    @Test
    fun bobasDaysAreCountedOnceEach() {
        val store = PetStore(InMemoryKeyValueStorage())
        store.notePupPetted("2026-10-01")
        store.notePupPetted("2026-10-01")
        store.notePupPetted("2026-10-03")
        assertEquals(2, store.pupDays().size)
    }

    @Test
    fun everyPetHasItsOwnName() {
        assertEquals(PetKind.entries.size, PetKind.entries.map { it.defaultName }.toSet().size)
        assertEquals("Mochi", PetKind.CAT.defaultName)
        assertEquals(PetKind.CAT, PetKind.from("nonsense"))
    }
}
