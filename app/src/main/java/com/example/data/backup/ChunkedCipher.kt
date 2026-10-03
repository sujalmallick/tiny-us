package com.example.data.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Thrown when a backup can't be opened: wrong password, damaged, or not a Tiny Us backup. */
class BackupCryptoException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Password-based streaming encryption for backup files.
 *
 * Layout: MAGIC | salt(16) | noncePrefix(8) | chunks…  where each chunk is
 * len(4) | AES-256-GCM(ciphertext+tag). Chunk i uses nonce = prefix ‖ i and the associated data
 * marks whether it is the final chunk, so reordering, dropping or truncating chunks is detected.
 * Chunks are small, so even hundreds of MB of photos never need to fit in memory.
 */
object ChunkedCipher {
    private val MAGIC = "TINYUS-BACKUP-1".toByteArray()
    private const val CHUNK_SIZE = 64 * 1024
    private const val ITERATIONS = 120_000
    private const val TAG_BITS = 128
    private val AAD_MORE = byteArrayOf(0)
    private val AAD_LAST = byteArrayOf(1)

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun nonce(prefix: ByteArray, counter: Int): ByteArray =
        ByteBuffer.allocate(12).put(prefix).putInt(counter).array()

    /** Wraps [out]; everything written is encrypted. Closing finishes the file (and closes [out]). */
    fun encryptingStream(out: OutputStream, password: CharArray): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val prefix = ByteArray(8).also(random::nextBytes)
        val key = deriveKey(password, salt)
        val data = DataOutputStream(out)
        data.write(MAGIC)
        data.write(salt)
        data.write(prefix)

        return object : FilterOutputStream(data) {
            private val buffer = ByteArray(CHUNK_SIZE)
            private var filled = 0
            private var counter = 0
            private var closed = false

            private fun flushChunk(last: Boolean) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce(prefix, counter++)))
                cipher.updateAAD(if (last) AAD_LAST else AAD_MORE)
                val sealed = cipher.doFinal(buffer, 0, filled)
                data.writeInt(sealed.size)
                data.write(sealed)
                filled = 0
            }

            override fun write(b: Int) {
                if (filled == CHUNK_SIZE) flushChunk(last = false)
                buffer[filled++] = b.toByte()
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                var offset = off
                var remaining = len
                while (remaining > 0) {
                    if (filled == CHUNK_SIZE) flushChunk(last = false)
                    val n = minOf(remaining, CHUNK_SIZE - filled)
                    System.arraycopy(b, offset, buffer, filled, n)
                    filled += n
                    offset += n
                    remaining -= n
                }
            }

            override fun close() {
                if (closed) return
                closed = true
                flushChunk(last = true) // always emitted, even when empty, to mark the end
                data.flush()
                data.close()
            }
        }
    }

    /** Wraps [input]; reading yields the decrypted bytes or throws [BackupCryptoException]. */
    fun decryptingStream(input: InputStream, password: CharArray): InputStream {
        val data = DataInputStream(input)
        val magic = ByteArray(MAGIC.size)
        try {
            data.readFully(magic)
        } catch (e: EOFException) {
            throw BackupCryptoException("Not a Tiny Us backup", e)
        }
        if (!magic.contentEquals(MAGIC)) throw BackupCryptoException("Not a Tiny Us backup")
        val salt = ByteArray(16).also(data::readFully)
        val prefix = ByteArray(8).also(data::readFully)
        val key = deriveKey(password, salt)

        return object : InputStream() {
            private var chunk = ByteArray(0)
            private var pos = 0
            private var counter = 0
            private var finished = false

            /** Loads the next chunk; returns false at the verified end of the file. */
            private fun nextChunk(): Boolean {
                if (finished) return false
                val len = try {
                    data.readInt()
                } catch (e: EOFException) {
                    throw BackupCryptoException("Backup file is incomplete", e)
                }
                if (len < TAG_BITS / 8 || len > CHUNK_SIZE + TAG_BITS / 8) throw BackupCryptoException("Backup file is damaged")
                val sealed = ByteArray(len)
                try {
                    data.readFully(sealed)
                } catch (e: EOFException) {
                    throw BackupCryptoException("Backup file is incomplete", e)
                }
                val index = counter++
                // A chunk only authenticates under the AAD it was written with, which tells us if it is the last one.
                val plain = tryOpen(sealed, index, AAD_MORE) ?: tryOpen(sealed, index, AAD_LAST)?.also { finished = true }
                    ?: throw BackupCryptoException(if (index == 0) "Wrong password or damaged backup" else "Backup file is damaged")
                if (finished && data.read() != -1) throw BackupCryptoException("Backup file has unexpected extra data")
                chunk = plain
                pos = 0
                return true
            }

            private fun tryOpen(sealed: ByteArray, index: Int, aad: ByteArray): ByteArray? = try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce(prefix, index)))
                cipher.updateAAD(aad)
                cipher.doFinal(sealed)
            } catch (_: BadPaddingException) { // AEADBadTagException is a subclass
                null
            }

            override fun read(): Int {
                while (pos >= chunk.size) if (!nextChunk()) return -1
                return chunk[pos++].toInt() and 0xFF
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (len == 0) return 0
                while (pos >= chunk.size) if (!nextChunk()) return -1
                val n = minOf(len, chunk.size - pos)
                System.arraycopy(chunk, pos, b, off, n)
                pos += n
                return n
            }

            override fun close() = data.close()
        }
    }
}
