@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.security

import com.example.data.AppLockPolicy
import com.example.data.KeyValueStorage
import kotlin.io.encoding.Base64
import kotlin.time.Clock

/**
 * Persistent privacy-lock settings. The PIN itself is never stored: only a salted
 * PBKDF2-HMAC-SHA256 hash ([PinHasher]), compared in constant time. Common code on
 * [KeyValueStorage]; Android keeps it in "tiny_us_lock" with the keys it always used.
 */
class AppLockStore(private val storage: KeyValueStorage) {

    val isEnabled: Boolean
        get() = storage.getBoolean(KEY_ENABLED, false) && storage.getString(KEY_PIN_HASH, null) != null

    var biometricsEnabled: Boolean
        get() = storage.getBoolean(KEY_BIOMETRICS, false)
        set(value) = storage.putBoolean(KEY_BIOMETRICS, value)

    var graceSeconds: Int
        get() = storage.getInt(KEY_GRACE, 0)
        set(value) = storage.putInt(KEY_GRACE, value.coerceAtLeast(0))

    /** Hide the app preview (Recents, the app switcher) while the lock is on; Android also blocks screenshots. */
    var hidePreview: Boolean
        get() = storage.getBoolean(KEY_HIDE_PREVIEW, true)
        set(value) = storage.putBoolean(KEY_HIDE_PREVIEW, value)

    val failedAttempts: Int get() = storage.getInt(KEY_FAILED, 0)

    /** Epoch millis until which PIN entry is paused after too many wrong guesses. */
    val lockedOutUntil: Long get() = storage.getLong(KEY_LOCKOUT_UNTIL, 0L)

    /** Turns the lock on with [pin]. Returns false if the PIN is not 4–6 digits. */
    fun enable(pin: String): Boolean {
        if (!AppLockPolicy.isValidPin(pin)) return false
        val salt = PinHasher.randomBytes(16)
        storage.putString(KEY_PIN_SALT, encode(salt))
        storage.putString(KEY_PIN_HASH, encode(PinHasher.hash(pin, salt, ITERATIONS)))
        storage.putBoolean(KEY_ENABLED, true)
        storage.putInt(KEY_FAILED, 0)
        storage.putLong(KEY_LOCKOUT_UNTIL, 0L)
        return true
    }

    fun disable() {
        storage.remove(KEY_PIN_SALT)
        storage.remove(KEY_PIN_HASH)
        storage.putBoolean(KEY_ENABLED, false)
        storage.putBoolean(KEY_BIOMETRICS, false)
        storage.putInt(KEY_FAILED, 0)
        storage.putLong(KEY_LOCKOUT_UNTIL, 0L)
    }

    sealed interface PinResult {
        data object Correct : PinResult
        data class Wrong(val cooldownSeconds: Int) : PinResult
        data class CoolingDown(val secondsLeft: Int) : PinResult
    }

    fun checkPin(pin: String, nowMs: Long = Clock.System.now().toEpochMilliseconds()): PinResult {
        val until = lockedOutUntil
        if (until > nowMs) return PinResult.CoolingDown(((until - nowMs + 999) / 1000).toInt())

        val salt = storage.getString(KEY_PIN_SALT, null)?.let(::decode)
        val expected = storage.getString(KEY_PIN_HASH, null)?.let(::decode)
        if (salt != null && expected != null && sameBytes(PinHasher.hash(pin, salt, ITERATIONS), expected)) {
            recordSuccess()
            return PinResult.Correct
        }

        val failed = failedAttempts + 1
        val cooldown = AppLockPolicy.cooldownSecondsAfter(failed)
        storage.putInt(KEY_FAILED, failed)
        storage.putLong(KEY_LOCKOUT_UNTIL, if (cooldown > 0) nowMs + cooldown * 1000L else 0L)
        return PinResult.Wrong(cooldown)
    }

    /** Biometric or device-credential unlock also clears the wrong-guess counter. */
    fun recordSuccess() {
        storage.putInt(KEY_FAILED, 0)
        storage.putLong(KEY_LOCKOUT_UNTIL, 0L)
    }

    /** Compares every byte, so the time taken doesn't hint at how much of a guess was right. */
    private fun sameBytes(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun encode(bytes: ByteArray) = Base64.encode(bytes)
    private fun decode(text: String) = Base64.decode(text)

    companion object {
        const val PREFS_NAME = "tiny_us_lock"
        private const val ITERATIONS = 60_000
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRICS = "biometrics"
        private const val KEY_GRACE = "grace_seconds"
        private const val KEY_HIDE_PREVIEW = "hide_preview"
        private const val KEY_FAILED = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    }
}

/** PBKDF2-HMAC-SHA256 and secure random bytes from the platform's crypto. */
expect object PinHasher {
    fun hash(pin: String, salt: ByteArray, iterations: Int): ByteArray
    fun randomBytes(count: Int): ByteArray
}
