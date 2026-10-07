package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FriendsTest {
    @Test
    fun theirHours() {
        assertFalse(Friend.cafeOpen(6))
        assertTrue(Friend.cafeOpen(7))
        assertTrue(Friend.cafeOpen(20))
        assertFalse(Friend.cafeOpen(21))
        assertTrue(Friend.baoTeaTime(16))
        assertFalse(Friend.baoTeaTime(17))
        assertTrue(Friend.pipDaring(12))
        assertFalse(Friend.pipDaring(9))
    }

    @Test
    fun theirFavourites() {
        assertEquals("dish:tea", Friend.BAO.favouriteIn(mapOf("dish:tea" to 1, "catch:OLD_BOOT" to 1)))
        assertEquals("catch:OLD_BOOT", Friend.BAO.favouriteIn(mapOf("dish:tea" to 0, "catch:OLD_BOOT" to 1)))
        assertNull(Friend.LEO.favouriteIn(mapOf("dish:tea" to 4)))
        assertEquals("catch:CARP", Friend.PIP.favouriteIn(mapOf("catch:CARP" to 1)))
    }

    @Test
    fun aSecretOpensOnceAndAGiftADay() {
        val store = FriendsStore(InMemoryKeyValueStorage())
        assertTrue(store.openSecret(Friend.LEO))
        assertFalse(store.openSecret(Friend.LEO))
        assertTrue(store.secretOpen(Friend.LEO))
        assertFalse(store.secretOpen(Friend.PIP))
        store.noteGift(Friend.PIP, "2026-10-07")
        assertTrue(store.giftedOn(Friend.PIP, "2026-10-07"))
        assertFalse(store.giftedOn(Friend.PIP, "2026-10-08"))
    }

    @Test
    fun theirSecretsAreInTheBook() {
        for (f in Friend.entries) assertTrue(CollectionBook.ENTRIES.any { it.key == f.secretKey && it.hidden })
    }
}
