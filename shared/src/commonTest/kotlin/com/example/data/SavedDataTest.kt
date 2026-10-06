package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedDataTest {
    @Test
    fun jsonWritesLikeOrgJson() {
        val obj = JSONObject()
            .put("id", "a/b")
            .put("text", "Line one\n\"quoted\"\t\\ end")
            .put("count", 3)
            .put("at", 1_700_000_000_000L)
            .put("ok", true)
            .put("tags", JSONArray().put("x").put("y"))
        assertEquals(
            """{"id":"a\/b","text":"Line one\n\"quoted\"\t\\ end","count":3,"at":1700000000000,"ok":true,"tags":["x","y"]}""",
            obj.toString()
        )
    }

    @Test
    fun jsonReadsBackWhatItWrote() {
        val text = "Caf\u00e9, \uD834\uDD1E and \u0001 control"
        val written = JSONArray().put(JSONObject().put("text", text).put("n", 7).put("big", 5_000_000_000L)).toString()
        val obj = JSONArray(written).getJSONObject(0)
        assertEquals(text, obj.getString("text"))
        assertEquals(7, obj.getInt("n"))
        assertEquals(5_000_000_000L, obj.getLong("big"))
    }

    @Test
    fun jsonReadsForgivinglyLikeOrgJson() {
        val obj = JSONObject("""{ "num": 12, "flag": "TRUE", "nothing": null, "s": "42", "list": [ "a" , "b" ] }""")
        assertEquals("12", obj.getString("num"))
        assertTrue(obj.optBoolean("flag", false))
        assertTrue(obj.has("nothing"))
        assertTrue(obj.isNull("nothing"))
        assertTrue(obj.isNull("missing"))
        assertEquals("fallback", obj.optString("missing", "fallback"))
        assertEquals(42L, obj.getLong("s"))
        assertEquals(0L, obj.optLong("missing", 0L))
        assertEquals("b", obj.getJSONArray("list").getString(1))
        assertNull(obj.optJSONArray("num"))
        assertFailsWith<JSONException> { obj.getString("missing") }
        assertFailsWith<JSONException> { JSONArray("""[1, 2""") }
        assertFailsWith<JSONException> { JSONArray("""{"a":1}""") }
    }

    @Test
    fun savedListsRoundTripThroughAnyStorage() {
        val storage = InMemoryKeyValueStorage()
        val prefs = PreferencesManager(storage)
        prefs.addLoveNote("See you soon / tonight", "Bean")
        prefs.addDreamEntry(DreamEntry(id = "d1", text = "Flying over the pier", matchedKeywords = listOf("fly", "sea"), dreamTheme = "SKY", timestamp = 42L))

        val reopened = PreferencesManager(storage)
        val note = reopened.getLoveNotes().first()
        assertEquals("See you soon / tonight", note.text)
        assertEquals("Bean", note.author)
        assertTrue(note.isCustom)
        assertTrue(note.createdAt > 0L)
        val dream = reopened.getDreamEntries().single()
        assertEquals(listOf("fly", "sea"), dream.matchedKeywords)
        assertEquals(42L, dream.timestamp)
        assertTrue(storage.getString("notes_json")!!.contains("tonight"))
    }

    @Test
    fun settingsKeepTheirKeysAndLimits() {
        val storage = InMemoryKeyValueStorage()
        val prefs = PreferencesManager(storage)
        prefs.girlOutfitIndex = 99
        prefs.buttonGlassIntensity = 3f
        prefs.catName = "   "
        assertEquals(20, storage.getInt("girl_outfit_index"))
        assertEquals(1.0f, storage.getFloat("button_glass_intensity"))
        assertEquals("Mochi", storage.getString("cat_name"))
        assertFalse(prefs.isOnboardingCompleted)
    }

    @Test
    fun daysOpenedCountUniqueDaysAndGaps() {
        val storage = InMemoryKeyValueStorage()
        val prefs = PreferencesManager(storage)
        assertTrue(prefs.markAppOpenedToday())
        assertFalse(prefs.markAppOpenedToday())
        assertEquals(1, prefs.uniqueDaysOpened)
        prefs.debugDayOffset = 3
        assertTrue(prefs.markAppOpenedToday())
        assertEquals(2, prefs.uniqueDaysOpened)
        assertEquals(3, prefs.consumeDaysAway())
        assertEquals(0, prefs.consumeDaysAway())
    }
}
