package com.example.data.backup

/** Raw DEFLATE (RFC 1951) inflation from the platform, for ZIP entries Android compressed. */
expect object RawInflate {
    /** [data] inflated to exactly [size] bytes, or null when it's damaged. */
    fun inflate(data: ByteArray, size: Int): ByteArray?
}

/**
 * Just enough ZIP for backups. Writing stores entries uncompressed (they are JSON and PNGs, which
 * don't shrink much); reading takes stored or DEFLATE entries through the central directory, so
 * files written by Android's ZipOutputStream (with data descriptors) read too.
 */
object TinyZip {
    private const val LOCAL = 0x04034b50
    private const val CENTRAL = 0x02014b50
    private const val END = 0x06054b50
    private const val MAX_ENTRY_BYTES = 64 * 1024 * 1024

    fun write(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteBuilder()
        val central = ByteBuilder()
        entries.forEach { (name, data) ->
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)
            val offset = out.size
            out.addIntLE(LOCAL); out.addShortLE(20); out.addShortLE(0x0800); out.addShortLE(0) // version, UTF-8 names, stored
            out.addShortLE(0); out.addShortLE(0x21) // time, date (1980-01-01)
            out.addIntLE(crc); out.addIntLE(data.size); out.addIntLE(data.size)
            out.addShortLE(nameBytes.size); out.addShortLE(0)
            out.add(nameBytes); out.add(data)

            central.addIntLE(CENTRAL); central.addShortLE(20); central.addShortLE(20); central.addShortLE(0x0800); central.addShortLE(0)
            central.addShortLE(0); central.addShortLE(0x21)
            central.addIntLE(crc); central.addIntLE(data.size); central.addIntLE(data.size)
            central.addShortLE(nameBytes.size); central.addShortLE(0); central.addShortLE(0) // extra, comment
            central.addShortLE(0); central.addShortLE(0); central.addIntLE(0) // disk, internal, external attributes
            central.addIntLE(offset)
            central.add(nameBytes)
        }
        val centralStart = out.size
        val centralBytes = central.toByteArray()
        out.add(centralBytes)
        out.addIntLE(END); out.addShortLE(0); out.addShortLE(0)
        out.addShortLE(entries.size); out.addShortLE(entries.size)
        out.addIntLE(centralBytes.size); out.addIntLE(centralStart); out.addShortLE(0)
        return out.toByteArray()
    }

    fun read(zip: ByteArray): List<Pair<String, ByteArray>> {
        fun u16(at: Int) = (zip[at].toInt() and 0xFF) or ((zip[at + 1].toInt() and 0xFF) shl 8)
        fun u32(at: Int) = u16(at) or (u16(at + 2) shl 16)
        // The end record is in the last 64 KB (it may be followed by a comment).
        var end = zip.size - 22
        val stop = maxOf(0, zip.size - 22 - 65_535)
        while (end >= stop && u32(end) != END) end--
        if (end < stop) throw BackupError("Backup file is damaged")
        val count = u16(end + 10)
        var at = u32(end + 16)
        val entries = ArrayList<Pair<String, ByteArray>>(count)
        repeat(count) {
            if (at + 46 > zip.size || u32(at) != CENTRAL) throw BackupError("Backup file is damaged")
            val method = u16(at + 10)
            val crc = u32(at + 16)
            val packed = u32(at + 20)
            val size = u32(at + 24)
            val nameLen = u16(at + 28)
            val extraLen = u16(at + 30)
            val commentLen = u16(at + 32)
            val local = u32(at + 42)
            val name = zip.decodeToString(at + 46, at + 46 + nameLen)
            at += 46 + nameLen + extraLen + commentLen

            if (packed < 0 || size < 0 || size > MAX_ENTRY_BYTES || local + 30 > zip.size || u32(local) != LOCAL) throw BackupError("Backup file is damaged")
            val dataStart = local + 30 + u16(local + 26) + u16(local + 28)
            if (dataStart + packed > zip.size) throw BackupError("Backup file is damaged")
            val raw = zip.copyOfRange(dataStart, dataStart + packed)
            val data = when (method) {
                0 -> raw
                8 -> RawInflate.inflate(raw, size) ?: throw BackupError("Backup file is damaged")
                else -> throw BackupError("Backup file is damaged")
            }
            if (crc32(data) != crc) throw BackupError("Backup file is damaged")
            if (!name.endsWith("/")) entries += name to data
        }
        return entries
    }

    private val CRC_TABLE = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
        c
    }

    fun crc32(data: ByteArray): Int {
        var c = -1
        for (b in data) c = CRC_TABLE[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }
}
