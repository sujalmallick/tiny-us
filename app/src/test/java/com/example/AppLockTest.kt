package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.AppLockStore
import com.example.security.DiscreetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `lock is off by default and rejects invalid PINs`() {
        val store = AppLockStore(context)
        assertFalse(store.isEnabled)
        assertFalse(store.enable("12"))
        assertFalse(store.enable("abcd"))
        assertFalse(store.isEnabled)
    }

    @Test
    fun `correct PIN unlocks and the PIN itself is never stored`() {
        val store = AppLockStore(context)
        assertTrue(store.enable("482913"))
        assertTrue(store.isEnabled)
        assertEquals(AppLockStore.PinResult.Correct, store.checkPin("482913"))

        val raw = context.getSharedPreferences(AppLockStore.PREFS_NAME, Context.MODE_PRIVATE).all.values.joinToString("|")
        assertFalse("PIN must not appear in storage", raw.contains("482913"))
    }

    @Test
    fun `repeated wrong PINs trigger a cooldown that blocks even the right PIN`() {
        val store = AppLockStore(context)
        store.enable("1357")
        val now = 1_000_000L
        repeat(4) { assertEquals(AppLockStore.PinResult.Wrong(0), store.checkPin("0000", now)) }
        assertEquals(AppLockStore.PinResult.Wrong(30), store.checkPin("0000", now))
        assertTrue(store.checkPin("1357", now + 1_000) is AppLockStore.PinResult.CoolingDown)
        // After the cooldown the right PIN works and resets the counter.
        assertEquals(AppLockStore.PinResult.Correct, store.checkPin("1357", now + 31_000))
        assertEquals(0, store.failedAttempts)
    }

    @Test
    fun `disabling clears the PIN and biometrics`() {
        val store = AppLockStore(context)
        store.enable("2468")
        store.biometricsEnabled = true
        store.disable()
        assertFalse(store.isEnabled)
        assertFalse(store.biometricsEnabled)
        assertTrue(store.checkPin("2468") is AppLockStore.PinResult.Wrong)
    }

    @Test
    fun `discreet mode swaps the launcher entry and back`() {
        assertFalse(DiscreetMode.isEnabled(context))
        DiscreetMode.setEnabled(context, true)
        assertTrue(DiscreetMode.isEnabled(context))
        DiscreetMode.setEnabled(context, false)
        assertFalse(DiscreetMode.isEnabled(context))
    }
}
