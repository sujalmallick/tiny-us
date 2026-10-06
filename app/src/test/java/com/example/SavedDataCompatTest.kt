package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PreferencesManager moved to common code with its own JSON (plan 08, S4). Data saved by the old
 * org.json code must still load, and what it writes now must be exactly what org.json wrote.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedDataCompatTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val raw get() = context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)

    @Before
    fun clean() {
        raw.edit().clear().commit()
    }

    @Test
    fun listsSavedByOrgJsonStillLoad() {
        val notes = JSONArray().put(
            JSONObject().put("id", "n-1").put("text", "Back at 7 / promise").put("author", "Bean")
                .put("date", "Oct 6").put("isCustom", true).put("createdAt", 1_700_000_000_000L)
        )
        val signals = JSONArray().put(
            JSONObject().put("id", "s-1").put("sender", "boy").put("type", "hug")
                .put("timestamp", 1_700_000_000_123L).put("isViewed", false)
        )
        val games = JSONArray().put(
            JSONObject().put("id", "g-1").put("questionId", "q-1").put("type", "WOULD_YOU_RATHER")
                .put("prompt", "Tea or coffee?").put("options", JSONArray().put("Tea").put("Coffee"))
                .put("boyChosenIndex", 1).put("isRevealed", false).put("timestamp", 5L)
        )
        raw.edit()
            .putInt("notes_version", 2)
            .putString("notes_json", notes.toString())
            .putString("ld_signals_json", signals.toString())
            .putString("mini_games_json", games.toString())
            .commit()

        val prefs = PreferencesManager(context)
        val note = prefs.getLoveNotes().single()
        assertEquals("Back at 7 / promise", note.text)
        assertEquals(1_700_000_000_000L, note.createdAt)
        val signal = prefs.getLongDistanceSignals().single()
        assertEquals(1_700_000_000_123L, signal.timestamp)
        assertNull(signal.note)
        val round = prefs.getMiniGameRounds().single()
        assertEquals(listOf("Tea", "Coffee"), round.options)
        assertEquals(1, round.boyChosenIndex)
        assertNull(round.girlChosenIndex)
    }

    @Test
    fun writesAreExactlyWhatOrgJsonWrote() {
        val prefs = PreferencesManager(context)
        prefs.addLoveNote("Line one\n\"two\" / three\ttab", "Sprout")
        prefs.addMemory("First picnic", "Under the big tree", "Oct 6, 2026", "heart")

        for (key in listOf("notes_json", "memories_json")) {
            val saved = raw.getString(key, null)!!
            // Re-serializing through Android's org.json gives the very same text.
            assertEquals(key, JSONArray(saved).toString(), saved)
        }
        val note = JSONArray(raw.getString("notes_json", null)).getJSONObject(0)
        assertEquals("Line one\n\"two\" / three\ttab", note.getString("text"))
        assertTrue(note.getBoolean("isCustom"))
    }

    @Test
    fun polaroidListIsWhatOrgJsonWrote() {
        val polaroidPrefs = context.getSharedPreferences("tiny_us_polaroids", Context.MODE_PRIVATE)
        polaroidPrefs.edit().clear().commit()
        val old = JSONArray().put(
            JSONObject().put("id", "p-1").put("title", "Salt and Sunset").put("date", "Oct 6, 2026")
                .put("time", "6:42 PM").put("sceneName", "Seaside Pier").put("sceneEnvKey", "SEASIDE_PIER")
                .put("imagePath", "/data/polaroids/pol_1.png")
        )
        polaroidPrefs.edit().putString("polaroids_json", old.toString()).commit()

        val manager = com.example.data.PolaroidManager(context)
        assertEquals("Salt and Sunset", manager.getPolaroids().single().title)
        manager.savePolaroid(com.example.data.PolaroidMemory("p-2", "Boardwalk Us", "Oct 7, 2026", "7:00 PM", "Seaside Pier", "SEASIDE_PIER", "/data/polaroids/pol_2.png"))
        val saved = polaroidPrefs.getString("polaroids_json", null)!!
        assertEquals(JSONArray(saved).toString(), saved)
        assertEquals(listOf("p-2", "p-1"), manager.getPolaroids().map { it.id })
    }

    @Test
    fun simpleSettingsUseTheSameKeys() {
        val prefs = PreferencesManager(context)
        prefs.boyfriendName = "Bean"
        prefs.buttonGlassIntensity = 0.3f
        prefs.tinyCareCategories = setOf("water", "rest")
        assertEquals("Bean", raw.getString("bf_name", null))
        assertEquals(0.3f, raw.getFloat("button_glass_intensity", 0f), 0.0001f)
        assertEquals(setOf("water", "rest"), raw.getStringSet("tiny_care_categories", null))
    }
}
