package com.example.progress

import com.example.data.JSONArray
import com.example.data.JSONObject
import com.example.data.KeyValueStorage

/**
 * Keeps [ProgressState] as one JSON entry in tiny_us_prefs, the file Android backup and the
 * in-app Backup & Restore already carry. Common code on [KeyValueStorage]; the JSON is the same
 * as org.json wrote it.
 */
class ProgressStore(private val storage: KeyValueStorage) {

    fun load(): ProgressState = runCatching { decode(storage.getString(KEY, null)) }.getOrNull() ?: ProgressState()

    fun save(state: ProgressState) {
        storage.putString(KEY, encode(state))
    }

    companion object {
        const val KEY = "progress_v1"
        /** The preferences file (PreferencesManager's), so backups include progress. */
        const val PREFS_FILE = "tiny_us_prefs"

        private fun ints(map: Map<String, Int>) = JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }
        private fun strings(list: List<String>) = JSONArray().apply { list.forEach { put(it) } }

        fun encode(s: ProgressState): String = JSONObject().apply {
            put("counters", ints(s.counters))
            put("seen", JSONObject().apply { s.seen.forEach { (k, v) -> put(k, strings(v.sorted())) } })
            put("keepsakes", ints(s.keepsakes))
            put("best", ints(s.best))
            put("firsts", JSONObject().apply { s.firsts.forEach { (k, v) -> put(k, v) } })
            put("unlocked", strings(s.unlocked.sorted()))
            put("garden", JSONObject().apply {
                put("plots", JSONArray().apply {
                    s.garden.plots.forEach { plot ->
                        put(JSONObject().apply {
                            plot.flower?.let { put("flower", it) }
                            put("waterings", plot.waterings)
                            put("lastWatered", plot.lastWatered)
                        })
                    }
                })
                put("stems", strings(s.garden.stems))
            })
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
                unlocked = strings(o.optJSONArray("unlocked")),
                garden = o.optJSONObject("garden")?.let { garden(it) } ?: com.example.games.GardenPlots()
            )
        }

        private fun garden(o: JSONObject): com.example.games.GardenPlots {
            val saved = o.optJSONArray("plots")
            val plots = List(com.example.games.GardenPlots.PLOTS) { i ->
                val p = saved?.optJSONObject(i) ?: return@List com.example.games.Plot()
                com.example.games.Plot(
                    flower = p.optString("flower").takeIf { it.isNotEmpty() },
                    waterings = p.optInt("waterings"),
                    lastWatered = p.optLong("lastWatered", -1L)
                )
            }
            val stems = o.optJSONArray("stems")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
            return com.example.games.GardenPlots(plots, stems)
        }
    }
}
