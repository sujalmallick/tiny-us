package com.example.data.backup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The backup's ZIP on every platform (the JVM and the iOS simulator). */
class TinyZipTest {
    @Test
    fun entriesComeBackAsWritten() {
        val photo = ByteArray(70_000) { (it * 31).toByte() }
        val zip = TinyZip.write(listOf("manifest.json" to "{}".encodeToByteArray(), "files/polaroids/pol_1.png" to photo, "empty" to ByteArray(0)))
        val read = TinyZip.read(zip)
        assertEquals(listOf("manifest.json", "files/polaroids/pol_1.png", "empty"), read.map { it.first })
        assertContentEquals(photo, read[1].second)
        assertEquals(0, read[2].second.size)
    }

    @Test
    fun crcMatchesTheStandard() {
        assertEquals(0xCBF43926.toInt(), TinyZip.crc32("123456789".encodeToByteArray()))
    }

    @Test
    fun deflatedEntriesInflate() {
        // Raw DEFLATE, as Android's ZipOutputStream writes entries.
        val text = "Tiny Us backup: hello hello hello hello, a little world for two."
        val raw = byteArrayOf(11, -55, -52, -85, 84, 8, 45, 86, 72, 74, 76, -50, 46, 45, -80, 82, -56, 72, -51, -55, -55, -57, 36, 117, 20, 18, 21, 114, 50, 75, 74, 114, 82, 21, -54, -13, -117, 114, 82, 20, -46, -14, -117, 20, 74, -54, -13, -11, 0)
        assertEquals(text, RawInflate.inflate(raw, text.length)?.decodeToString())
        assertEquals(null, RawInflate.inflate(raw.copyOf(10), text.length))
    }

    @Test
    fun damagedFilesAreRejected() {
        assertFailsWith<BackupError> { TinyZip.read(ByteArray(100)) }
        val zip = TinyZip.write(listOf("a" to "hello".encodeToByteArray()))
        zip[32] = (zip[32] + 1).toByte() // a byte of the entry's data
        assertFailsWith<BackupError> { TinyZip.read(zip) }
    }

    @Test
    fun contentsKeepTheirTypes() {
        val prefs = mapOf<String, Any>("name" to "Bean", "days" to 3, "at" to 5_000_000_000L, "glass" to 0.5f, "on" to true, "cats" to setOf("water", "rest"))
        val zip = BackupContent.build(BackupContent.Contents(mapOf("tiny_us_prefs" to prefs), mapOf("pol_1.png" to byteArrayOf(1, 2))), 0L)
        val back = BackupContent.parse(zip)
        assertEquals(prefs, back.prefs["tiny_us_prefs"])
        assertEquals(emptyMap(), back.prefs["tiny_us_polaroids"])
        assertContentEquals(byteArrayOf(1, 2), back.photos["pol_1.png"])
    }
}
