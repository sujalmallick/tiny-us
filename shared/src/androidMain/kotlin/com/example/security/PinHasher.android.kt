package com.example.security

import android.content.Context
import com.example.data.SharedPreferencesStorage
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

actual object PinHasher {
    actual fun hash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    actual fun randomBytes(count: Int): ByteArray = ByteArray(count).also { SecureRandom().nextBytes(it) }
}

/** The lock settings in "tiny_us_lock", as the app always kept them. */
fun AppLockStore(context: Context): AppLockStore =
    AppLockStore(SharedPreferencesStorage(context.applicationContext.getSharedPreferences(AppLockStore.PREFS_NAME, Context.MODE_PRIVATE)))
