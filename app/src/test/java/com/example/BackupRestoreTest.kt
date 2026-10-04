package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PolaroidManager
import com.example.data.PreferencesManager
import com.example.data.backup.BackupCryptoException
import com.example.data.backup.ChunkedCipher
import com.example.data.backup.TinyBackup
import com.example.data.PolaroidMemory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val password = "cozy-garden".toCharArray()

    private fun encrypt(plain: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ChunkedCipher.encryptingStream(out, password).use { it.write(plain) }
        return out.toByteArray()
    }

    private fun decrypt(sealed: ByteArray, pw: CharArray = password): ByteArray =
        ChunkedCipher.decryptingStream(ByteArrayInputStream(sealed), pw).use { it.readBytes() }

    @Test
    fun `cipher round trips data spanning many chunks, and empty data`() {
        val big = Random(7).nextBytes(300_000)
        assertArrayEquals(big, decrypt(encrypt(big)))
        assertArrayEquals(ByteArray(0), decrypt(encrypt(ByteArray(0))))
    }

    @Test
    fun `wrong password, tampering and truncation are all rejected`() {
        val sealed = encrypt(Random(1).nextBytes(200_000))
        assertRejected { decrypt(sealed, "not-it".toCharArray()) }
        assertRejected { decrypt(sealed.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }) }
        assertRejected { decrypt(sealed.copyOf(sealed.size - 70_000)) } // whole last chunk missing
        assertRejected { decrypt("hello".toByteArray()) }
    }

    private fun assertRejected(block: () -> Unit) {
        try {
            block(); fail("expected BackupCryptoException")
        } catch (_: BackupCryptoException) {
        }
    }

    @Test
    fun `full backup restores data and photos and replaces what was there`() {
        val prefs = PreferencesManager(context)
        prefs.boyfriendName = "Robin"
        prefs.girlfriendName = "Kai"
        prefs.addMemory("Lake day", "Paddle boats", "Jul 4, 2025", "flower")
        prefs.tinyCareCategories = setOf("hydration", "rest")
        val photoDir = File(context.filesDir, "polaroids").apply { mkdirs() }
        val photo = File(photoDir, "pol_1790000000000_abc123.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        PolaroidManager(context).savePolaroid(PolaroidMemory("p1", "Golden hour", "Sep 28, 2026", "6:00 PM", "Meadow", "MEADOW", photo.absolutePath))

        val file = ByteArrayOutputStream()
        val exported = TinyBackup.export(context, password, file)
        assertEquals(1, exported.photos)

        // Change everything, then restore.
        prefs.boyfriendName = "Someone else"
        prefs.addMemory("Should disappear", "", "", "heart")
        photo.delete()

        val restored = TinyBackup.restore(context, password, ByteArrayInputStream(file.toByteArray()))
        assertEquals(1, restored.photos)

        val fresh = PreferencesManager(context)
        assertEquals("Robin", fresh.boyfriendName)
        assertEquals("Kai", fresh.girlfriendName)
        assertTrue(fresh.getMemories().any { it.title == "Lake day" })
        assertFalse(fresh.getMemories().any { it.title == "Should disappear" })
        assertEquals(setOf("hydration", "rest"), fresh.tinyCareCategories)
        val polaroid = PolaroidManager(context).getPolaroids().single()
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), File(polaroid.imagePath).readBytes())
    }

    @Test
    fun `a failed restore leaves existing data untouched`() {
        val prefs = PreferencesManager(context)
        prefs.boyfriendName = "Robin"
        val file = ByteArrayOutputStream().also { TinyBackup.export(context, password, it) }.toByteArray()
        prefs.boyfriendName = "Current"

        assertRejected { TinyBackup.restore(context, "wrong-password".toCharArray(), ByteArrayInputStream(file)) }
        assertEquals("Current", PreferencesManager(context).boyfriendName)
    }
}
