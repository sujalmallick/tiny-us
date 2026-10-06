package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.data.AppLockPolicy
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Persistent privacy-lock settings. The PIN itself is never stored: only a salted
 * PBKDF2-HMAC-SHA256 hash, compared in constant time.
 */
class AppLockStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false) && prefs.contains(KEY_PIN_HASH)

    var biometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS, value).apply()

    var graceSeconds: Int
        get() = prefs.getInt(KEY_GRACE, 0)
        set(value) = prefs.edit().putInt(KEY_GRACE, value.coerceAtLeast(0)).apply()

    /** Hide the app preview in Recents and block screenshots while the lock is on. */
    var hidePreview: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PREVIEW, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_PREVIEW, value).apply()

    val failedAttempts: Int get() = prefs.getInt(KEY_FAILED, 0)

    /** Epoch millis until which PIN entry is paused after too many wrong guesses. */
    val lockedOutUntil: Long get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)

    /** Turns the lock on with [pin]. Returns false if the PIN is not 4–6 digits. */
    fun enable(pin: String): Boolean {
        if (!AppLockPolicy.isValidPin(pin)) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PIN_SALT, encode(salt))
            .putString(KEY_PIN_HASH, encode(hash(pin, salt)))
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun disable() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_ENABLED, false)
            .putBoolean(KEY_BIOMETRICS, false)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    sealed interface PinResult {
        data object Correct : PinResult
        data class Wrong(val cooldownSeconds: Int) : PinResult
        data class CoolingDown(val secondsLeft: Int) : PinResult
    }

    fun checkPin(pin: String, nowMs: Long = System.currentTimeMillis()): PinResult {
        val until = lockedOutUntil
        if (until > nowMs) return PinResult.CoolingDown(((until - nowMs + 999) / 1000).toInt())

        val salt = prefs.getString(KEY_PIN_SALT, null)?.let(::decode)
        val expected = prefs.getString(KEY_PIN_HASH, null)?.let(::decode)
        if (salt != null && expected != null && MessageDigest.isEqual(hash(pin, salt), expected)) {
            recordSuccess()
            return PinResult.Correct
        }

        val failed = failedAttempts + 1
        val cooldown = AppLockPolicy.cooldownSecondsAfter(failed)
        prefs.edit()
            .putInt(KEY_FAILED, failed)
            .putLong(KEY_LOCKOUT_UNTIL, if (cooldown > 0) nowMs + cooldown * 1000L else 0L)
            .apply()
        return PinResult.Wrong(cooldown)
    }

    /** Biometric or device-credential unlock also clears the wrong-guess counter. */
    fun recordSuccess() {
        prefs.edit().putInt(KEY_FAILED, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String) = Base64.decode(text, Base64.NO_WRAP)

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
