package com.example.progress

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps [ProgressState] as one JSON entry in tiny_us_prefs, the file Android backup and the
 * in-app Backup & Restore already carry.
 */
class ProgressStore(private val prefs: SharedPreferences) {

    fun load(): ProgressState = runCatching { decode(prefs.getString(KEY, null)) }.getOrNull() ?: ProgressState()

    fun save(state: ProgressState) {
        prefs.edit().putString(KEY, encode(state)).apply()
    }

    companion object {
        const val KEY = "progress_v1"
        /** The preferences file (PreferencesManager's), so backups include progress. */
        const val PREFS_FILE = "tiny_us_prefs"

        fun encode(s: ProgressState): String = JSONObject().apply {
            put("counters", JSONObject(s.counters))
            put("seen", JSONObject().apply { s.seen.forEach { (k, v) -> put(k, JSONArray(v.sorted())) } })
            put("keepsakes", JSONObject(s.keepsakes))
            put("best", JSONObject(s.best))
            put("firsts", JSONObject().apply { s.firsts.forEach { (k, v) -> put(k, v) } })
            put("unlocked", JSONArray(s.unlocked.sorted()))
        }.toString()

        fun decode(raw: String?): ProgressState? {
            if (raw.isNullOrBlank()) return null
            val o = JSONObject(raw)
            fun ints(name: String): Map<String, Int> = o.optJSONObject(name)?.let { j -> j.keys().asSequence().associateWith { j.optInt(it) } } ?: emptyMap()
            fun strings(a: JSONArray?): Set<String> = a?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.toSet() } ?: emptySet()
            return ProgressState(
                counters = ints("counters"),
                seen = o.optJSONObject("seen")?.let { j -> j.keys().asSequence().associateWith { strings(j.optJSONArray(it)) } } ?: emptyMap(),
                keepsakes = ints("keepsakes"),
                best = ints("best"),
                firsts = o.optJSONObject("firsts")?.let { j -> j.keys().asSequence().associateWith { j.optLong(it) } } ?: emptyMap(),
                unlocked = strings(o.optJSONArray("unlocked"))
            )
        }
    }
}
