package com.example.data.backup

/** A backup can't be opened: wrong password, damaged, or not a Tiny Us backup. */
class BackupError(message: String) : Exception(message)

/** The platform's crypto for backups: PBKDF2-HMAC-SHA256, secure random bytes and AES-256-GCM. */
expect object BackupCrypto {
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray
    fun randomBytes(count: Int): ByteArray

    /** AES-256-GCM with a 128-bit tag: the ciphertext followed by the tag. */
    fun seal(key: ByteArray, nonce: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray

    /** The plaintext, or null when the tag doesn't verify. */
    fun open(key: ByteArray, nonce: ByteArray, sealed: ByteArray, aad: ByteArray): ByteArray?
}

/**
 * The backup file's encryption, the same layout as Android's ChunkedCipher so a backup moves
 * between phones: MAGIC | salt(16) | noncePrefix(8) | chunks, each len(4, big-endian) |
 * AES-256-GCM(chunk + tag). Chunk i uses nonce prefix ‖ i; the associated data marks the last
 * chunk (always written, even empty), so reordered, dropped or cut chunks are caught.
 */
object BackupCipher {
    private val MAGIC = "TINYUS-BACKUP-1".encodeToByteArray()
    private const val CHUNK_SIZE = 64 * 1024
    private const val ITERATIONS = 120_000
    private const val TAG_BYTES = 16
    private val AAD_MORE = byteArrayOf(0)
    private val AAD_LAST = byteArrayOf(1)

    private fun nonce(prefix: ByteArray, counter: Int): ByteArray =
        prefix + byteArrayOf((counter ushr 24).toByte(), (counter ushr 16).toByte(), (counter ushr 8).toByte(), counter.toByte())

    fun seal(plain: ByteArray, password: CharArray): ByteArray {
        val salt = BackupCrypto.randomBytes(16)
        val prefix = BackupCrypto.randomBytes(8)
        val key = BackupCrypto.deriveKey(password, salt, ITERATIONS)
        val out = ByteBuilder()
        out.add(MAGIC)
        out.add(salt)
        out.add(prefix)
        var counter = 0
        var offset = 0
        // Full chunks while more follows; the remainder (maybe empty) is always the sealed last chunk.
        while (plain.size - offset > CHUNK_SIZE) {
            out.addChunk(BackupCrypto.seal(key, nonce(prefix, counter++), plain.copyOfRange(offset, offset + CHUNK_SIZE), AAD_MORE))
            offset += CHUNK_SIZE
        }
        out.addChunk(BackupCrypto.seal(key, nonce(prefix, counter), plain.copyOfRange(offset, plain.size), AAD_LAST))
        return out.toByteArray()
    }

    fun open(file: ByteArray, password: CharArray): ByteArray {
        if (file.size < MAGIC.size + 24 || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw BackupError("Not a Tiny Us backup")
        var pos = MAGIC.size
        val salt = file.copyOfRange(pos, pos + 16).also { pos += 16 }
        val prefix = file.copyOfRange(pos, pos + 8).also { pos += 8 }
        val key = BackupCrypto.deriveKey(password, salt, ITERATIONS)
        val out = ByteBuilder()
        var index = 0
        while (true) {
            if (pos + 4 > file.size) throw BackupError("Backup file is incomplete")
            val len = ((file[pos].toInt() and 0xFF) shl 24) or ((file[pos + 1].toInt() and 0xFF) shl 16) or
                ((file[pos + 2].toInt() and 0xFF) shl 8) or (file[pos + 3].toInt() and 0xFF)
            pos += 4
            if (len < TAG_BYTES || len > CHUNK_SIZE + TAG_BYTES) throw BackupError("Backup file is damaged")
            if (pos + len > file.size) throw BackupError("Backup file is incomplete")
            val sealed = file.copyOfRange(pos, pos + len)
            pos += len
            val n = nonce(prefix, index)
            // A chunk only authenticates under the AAD it was written with, which tells if it is the last.
            val more = BackupCrypto.open(key, n, sealed, AAD_MORE)
            if (more != null) {
                out.add(more)
                index++
                continue
            }
            val last = BackupCrypto.open(key, n, sealed, AAD_LAST)
                ?: throw BackupError(if (index == 0) "Wrong password or damaged backup" else "Backup file is damaged")
            out.add(last)
            if (pos != file.size) throw BackupError("Backup file has unexpected extra data")
            return out.toByteArray()
        }
    }
}

/** A growing byte buffer (common code has no ByteArrayOutputStream). */
internal class ByteBuilder {
    private var bytes = ByteArray(64 * 1024)
    var size = 0
        private set

    fun add(more: ByteArray) {
        ensure(more.size)
        more.copyInto(bytes, size)
        size += more.size
    }

    fun addByte(b: Int) {
        ensure(1)
        bytes[size++] = b.toByte()
    }

    fun addShortLE(v: Int) { addByte(v); addByte(v ushr 8) }

    fun addIntLE(v: Int) { addByte(v); addByte(v ushr 8); addByte(v ushr 16); addByte(v ushr 24) }

    /** A length-prefixed (big-endian) chunk. */
    fun addChunk(chunk: ByteArray) {
        addByte(chunk.size ushr 24); addByte(chunk.size ushr 16); addByte(chunk.size ushr 8); addByte(chunk.size)
        add(chunk)
    }

    private fun ensure(extra: Int) {
        if (size + extra <= bytes.size) return
        var cap = bytes.size * 2
        while (cap < size + extra) cap *= 2
        bytes = bytes.copyOf(cap)
    }

    fun toByteArray(): ByteArray = bytes.copyOf(size)
}
