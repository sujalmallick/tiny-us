@file:OptIn(ExperimentalForeignApi::class)

package com.example.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

actual object PinHasher {
    actual fun hash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val out = ByteArray(32)
        salt.usePinned { s ->
            out.usePinned { o ->
                // The password goes in as a C string (UTF-8; a PIN is plain digits).
                CCKeyDerivationPBKDF(
                    kCCPBKDF2,
                    pin,
                    pin.encodeToByteArray().size.convert(),
                    s.addressOf(0).reinterpret(),
                    salt.size.convert(),
                    kCCPRFHmacAlgSHA256,
                    iterations.convert(),
                    o.addressOf(0).reinterpret(),
                    out.size.convert()
                )
            }
        }
        return out
    }

    actual fun randomBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, count.convert(), it.addressOf(0)) }
        return bytes
    }
}
