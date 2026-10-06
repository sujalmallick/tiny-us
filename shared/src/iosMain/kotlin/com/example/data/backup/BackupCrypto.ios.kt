@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.example.data.backup

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import platform.zlib.ZLIB_VERSION
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream
import platform.posix.memcpy

/**
 * AES-GCM from CryptoKit, which only Swift can call: the Swift app sets [IosBackupCrypto.aesGcm]
 * at launch (`IosBackupCrypto.shared.aesGcm = AesGcmBridgeImpl()`).
 */
interface AesGcmBridge {
    /** The ciphertext followed by the 16-byte tag, or null on failure. */
    fun seal(key: NSData, nonce: NSData, plain: NSData, aad: NSData): NSData?

    /** The plaintext, or null when the tag doesn't verify. */
    fun open(key: NSData, nonce: NSData, sealed: NSData, aad: NSData): NSData?
}

object IosBackupCrypto {
    var aesGcm: AesGcmBridge? = null
}

internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

internal fun NSData.toByteArray(): ByteArray {
    val n = length.toInt()
    if (n == 0) return ByteArray(0)
    return ByteArray(n).apply { usePinned { memcpy(it.addressOf(0), bytes, length) } }
}

actual object BackupCrypto {
    actual fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        // Java's PBKDF2WithHmacSHA256 reads the password as UTF-8, and so does this.
        val text = password.concatToString()
        val out = ByteArray(32)
        salt.usePinned { s ->
            out.usePinned { o ->
                CCKeyDerivationPBKDF(
                    kCCPBKDF2, text, text.encodeToByteArray().size.convert(),
                    s.addressOf(0).reinterpret(), salt.size.convert(),
                    kCCPRFHmacAlgSHA256, iterations.convert(),
                    o.addressOf(0).reinterpret(), out.size.convert()
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

    private val bridge: AesGcmBridge get() = IosBackupCrypto.aesGcm ?: throw BackupError("Backup isn't available")

    actual fun seal(key: ByteArray, nonce: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray =
        bridge.seal(key.toNSData(), nonce.toNSData(), plain.toNSData(), aad.toNSData())?.toByteArray()
            ?: throw BackupError("Backup couldn't be written")

    actual fun open(key: ByteArray, nonce: ByteArray, sealed: ByteArray, aad: ByteArray): ByteArray? =
        bridge.open(key.toNSData(), nonce.toNSData(), sealed.toNSData(), aad.toNSData())?.toByteArray()
}

actual object RawInflate {
    actual fun inflate(data: ByteArray, size: Int): ByteArray? {
        if (size == 0) return ByteArray(0)
        if (data.isEmpty()) return null
        val out = ByteArray(size)
        val ok = memScoped {
            val stream = alloc<z_stream>()
            data.usePinned { d ->
                out.usePinned { o ->
                    stream.next_in = d.addressOf(0).reinterpret()
                    stream.avail_in = data.size.convert()
                    stream.next_out = o.addressOf(0).reinterpret()
                    stream.avail_out = size.convert()
                    // Window bits -15: raw DEFLATE (RFC 1951), what ZIP entries hold.
                    if (inflateInit2_(stream.ptr, -15, ZLIB_VERSION, sizeOf<z_stream>().convert()) != Z_OK) {
                        false
                    } else {
                        val result = inflate(stream.ptr, Z_FINISH)
                        val written = stream.total_out.toLong()
                        inflateEnd(stream.ptr)
                        result == Z_STREAM_END && written == size.toLong()
                    }
                }
            }
        }
        return if (ok) out else null
    }
}
