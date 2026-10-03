package com.example.data

/**
 * Platform-independent rules for the optional privacy lock. Hashing and biometrics live in the
 * platform layer; the decisions about *when* to lock and how to throttle guesses live here so
 * Android and iOS behave the same.
 */
object AppLockPolicy {
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6

    /** Grace periods offered in settings, in seconds. 0 = lock as soon as the app is left. */
    val GRACE_PERIOD_OPTIONS_SECONDS = listOf(0, 60, 300)

    /** Wrong guesses allowed before the first cooldown. */
    const val FREE_ATTEMPTS = 5

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it in '0'..'9' }

    /**
     * Whether returning to the app should show the lock screen.
     * [backgroundedAtMs] is null when the app was never left during this process (cold start).
     */
    fun shouldLockOnReturn(enabled: Boolean, backgroundedAtMs: Long?, nowMs: Long, graceSeconds: Int): Boolean {
        if (!enabled) return false
        if (backgroundedAtMs == null) return true
        val elapsed = nowMs - backgroundedAtMs
        // A clock that jumped backwards is treated as "long ago" rather than trusted.
        return elapsed < 0 || elapsed >= graceSeconds * 1000L
    }

    /**
     * Cooldown after [failedAttempts] consecutive wrong PINs: none for the first few, then
     * 30s, 60s, 120s… capped at 15 minutes. There is never a permanent lockout or data wipe.
     */
    fun cooldownSecondsAfter(failedAttempts: Int): Int {
        if (failedAttempts < FREE_ATTEMPTS) return 0
        val step = failedAttempts - FREE_ATTEMPTS
        return (30L shl step.coerceAtMost(5)).coerceAtMost(15 * 60L).toInt()
    }
}
