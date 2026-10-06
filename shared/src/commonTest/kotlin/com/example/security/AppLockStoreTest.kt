package com.example.security

import com.example.data.InMemoryKeyValueStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The PIN store on each platform's own crypto (javax on Android, CommonCrypto on iOS). */
class AppLockStoreTest {
    @Test
    fun aPinUnlocksAndIsNeverStored() {
        val storage = InMemoryKeyValueStorage()
        val store = AppLockStore(storage)
        assertFalse(store.isEnabled)
        assertTrue(store.enable("482913"))
        assertTrue(store.isEnabled)
        assertEquals(AppLockStore.PinResult.Correct, store.checkPin("482913"))
        assertTrue(store.checkPin("000000") is AppLockStore.PinResult.Wrong)
        val saved = listOf("pin_hash", "pin_salt").map { storage.getString(it).orEmpty() }
        assertTrue(saved.all { it.isNotEmpty() && "482913" !in it })
    }

    @Test
    fun theSameSaltGivesTheSameHash() {
        val salt = ByteArray(16) { it.toByte() }
        val a = PinHasher.hash("1357", salt, 1000)
        assertEquals(32, a.size)
        assertTrue(a.contentEquals(PinHasher.hash("1357", salt, 1000)))
        assertFalse(a.contentEquals(PinHasher.hash("1358", salt, 1000)))
        assertFalse(PinHasher.randomBytes(16).contentEquals(PinHasher.randomBytes(16)))
    }

    @Test
    fun tooManyWrongGuessesPauseEntry() {
        val store = AppLockStore(InMemoryKeyValueStorage())
        store.enable("1357")
        val now = 1_000_000L
        repeat(4) { assertEquals(AppLockStore.PinResult.Wrong(0), store.checkPin("0000", now)) }
        assertEquals(AppLockStore.PinResult.Wrong(30), store.checkPin("0000", now))
        assertTrue(store.checkPin("1357", now + 1_000) is AppLockStore.PinResult.CoolingDown)
        assertEquals(AppLockStore.PinResult.Correct, store.checkPin("1357", now + 31_000))
        store.disable()
        assertFalse(store.isEnabled)
    }

    @Test
    fun theLockFollowsTheGracePeriod() {
        val store = AppLockStore(InMemoryKeyValueStorage())
        store.enable("2468")
        store.graceSeconds = 60
        AppLock.unlock()
        AppLock.onAppBackgrounded()
        AppLock.onAppForegrounded(store)
        assertFalse(AppLock.isLocked, "back within the grace period")
        store.graceSeconds = 0
        AppLock.onAppBackgrounded()
        AppLock.onAppForegrounded(store)
        assertTrue(AppLock.isLocked, "locks right away")
        AppLock.unlock()
    }
}
