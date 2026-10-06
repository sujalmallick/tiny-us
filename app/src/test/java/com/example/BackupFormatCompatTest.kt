package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupCipher
import com.example.data.backup.BackupContent
import com.example.data.backup.BackupError
import com.example.data.backup.TinyBackup
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * iOS writes and reads backups with the shared format (BackupCipher, BackupContent, TinyZip);
 * Android with its streaming TinyBackup. The same file must work in both directions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupFormatCompatTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val password = "little world".toCharArray()

    @Test
    fun anAndroidBackupOpensWithTheSharedFormat() {
        context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE).edit().clear()
            .putString("bf_name", "Bean").putInt("garden_stage", 3).putLong("mood_last_updated", 5_000_000_000L)
            .putFloat("button_glass_intensity", 0.4f).putBoolean("sound_enabled", false)
            .putStringSet("tiny_care_categories", setOf("hydration", "sleep")).commit()
        val photos = File(context.filesDir, "polaroids").apply { mkdirs() }
        File(photos, "pol_1.png").writeBytes(ByteArray(100_000) { (it % 251).toByte() })

        val file = ByteArrayOutputStream().also { TinyBackup.export(context, password.copyOf(), it) }.toByteArray()
        val contents = BackupContent.parse(BackupCipher.open(file, password))

        val prefs = contents.prefs.getValue("tiny_us_prefs")
        assertEquals("Bean", prefs["bf_name"])
        assertEquals(3, prefs["garden_stage"])
        assertEquals(5_000_000_000L, prefs["mood_last_updated"])
        assertEquals(0.4f, prefs["button_glass_intensity"])
        assertEquals(false, prefs["sound_enabled"])
        assertEquals(setOf("hydration", "sleep"), prefs["tiny_care_categories"])
        assertEquals(100_000, contents.photos.getValue("pol_1.png").size)

        try {
            BackupCipher.open(file, "wrong password".toCharArray())
            fail("a wrong password must not open it")
        } catch (e: BackupError) {
            assertTrue(e.message!!.contains("Wrong password"))
        }
    }

    @Test
    fun aSharedFormatBackupRestoresOnAndroid() {
        val bigPhoto = ByteArray(200_000) { (it % 199).toByte() } // more than one 64 KB chunk
        val polaroids = """[{"id":"p1","title":"Golden hour","date":"Oct 7, 2026","time":"6:00 PM","sceneName":"Meadow","sceneEnvKey":"MEADOW","imagePath":"/var/mobile/Documents/polaroids/pol_9.png"}]"""
        val contents = BackupContent.Contents(
            prefs = mapOf(
                "tiny_us_prefs" to mapOf("gf_name" to "Sprout", "unique_days_opened" to 12, "tiny_care_next_trigger" to 0L, "onboarding_completed" to true),
                "tiny_us_polaroids" to mapOf("polaroids_json" to polaroids)
            ),
            photos = mapOf("pol_9.png" to bigPhoto)
        )
        val file = BackupCipher.seal(BackupContent.build(contents, 1L), password)

        TinyBackup.restore(context, password.copyOf(), ByteArrayInputStream(file))

        val prefs = context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)
        assertEquals("Sprout", prefs.getString("gf_name", null))
        assertEquals(12, prefs.getInt("unique_days_opened", 0))
        assertEquals(0L, prefs.getLong("tiny_care_next_trigger", -1L))
        assertTrue(prefs.getBoolean("onboarding_completed", false))
        val restoredPhoto = File(File(context.filesDir, "polaroids"), "pol_9.png")
        assertEquals(bigPhoto.size.toLong(), restoredPhoto.length())
        val list = context.getSharedPreferences("tiny_us_polaroids", Context.MODE_PRIVATE).getString("polaroids_json", null)!!
        val imagePath = org.json.JSONArray(list).getJSONObject(0).getString("imagePath")
        assertEquals("photo paths point at this phone", restoredPhoto.absolutePath, imagePath)
    }
}
