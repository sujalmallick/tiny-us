package com.example

import com.example.data.AppLockPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLockPolicyTest {
    @Test
    fun pinMustBeFourToSixDigits() {
        assertTrue(AppLockPolicy.isValidPin("1234"))
        assertTrue(AppLockPolicy.isValidPin("123456"))
        assertFalse(AppLockPolicy.isValidPin("123"))
        assertFalse(AppLockPolicy.isValidPin("1234567"))
        assertFalse(AppLockPolicy.isValidPin("12a4"))
        assertFalse(AppLockPolicy.isValidPin("１２３４")) // full-width digits are not ASCII digits
    }

    @Test
    fun coldStartAlwaysLocksWhenEnabled() {
        assertTrue(AppLockPolicy.shouldLockOnReturn(enabled = true, backgroundedAtMs = null, nowMs = 0, graceSeconds = 300))
        assertFalse(AppLockPolicy.shouldLockOnReturn(enabled = false, backgroundedAtMs = null, nowMs = 0, graceSeconds = 0))
    }

    @Test
    fun gracePeriodIsRespected() {
        val left = 1_000_000L
        assertFalse(AppLockPolicy.shouldLockOnReturn(true, left, left + 59_000, graceSeconds = 60))
        assertTrue(AppLockPolicy.shouldLockOnReturn(true, left, left + 60_000, graceSeconds = 60))
        assertTrue(AppLockPolicy.shouldLockOnReturn(true, left, left + 1, graceSeconds = 0))
    }

    @Test
    fun clockGoingBackwardsLocks() {
        assertTrue(AppLockPolicy.shouldLockOnReturn(true, backgroundedAtMs = 5_000, nowMs = 1_000, graceSeconds = 300))
    }

    @Test
    fun cooldownGrowsButIsCapped() {
        assertEquals(0, AppLockPolicy.cooldownSecondsAfter(4))
        assertEquals(30, AppLockPolicy.cooldownSecondsAfter(5))
        assertEquals(60, AppLockPolicy.cooldownSecondsAfter(6))
        assertEquals(120, AppLockPolicy.cooldownSecondsAfter(7))
        assertEquals(15 * 60, AppLockPolicy.cooldownSecondsAfter(50))
    }
}
