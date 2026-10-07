package com.example.data

import kotlinx.datetime.LocalDate

/*
 * Plan 11: visitors. Rare and short: at most one a day, in a scene that fits, for a minute or two.
 * The street painter paints the two of them as they are right then; Billionaire and The Great, an
 * old couple on the pier, quietly do whatever the two of them do a moment later.
 */
enum class VisitorKind { PAINTER, OLD_COUPLE }

/** A painting of them, as they were when it was finished: the scene, the light, what they wore. */
data class Painting(
    val epochDay: Long,
    val scene: String,
    val weather: String,
    val phase: String,
    val boyOutfit: Int,
    val girlOutfit: Int,
    /** HOLD_HANDS, HUG, SNUGGLE or APART: how they were with each other. */
    val together: String,
    val mochi: Boolean
)

object Visitors {
    /** For tests and previews: the day to treat as today. */
    var todayOverride: LocalDate? = null

    fun today(): LocalDate = todayOverride ?: CoupleDates.today()

    /** The painter comes about once a week. */
    const val PAINTER_EVERY_DAYS = 6

    /** Billionaire and The Great come a few times a week. */
    const val OLD_COUPLE_EVERY_DAYS = 2

    /** Whether [kind] may come on [today]: once a day for anyone, and each at its own pace. */
    fun mayCome(kind: VisitorKind, store: VisitorStore, today: LocalDate): Boolean {
        val day = today.toEpochDays().toLong()
        if (store.lastAnyVisit() == day) return false
        val last = store.lastVisit(kind) ?: return true
        val every = if (kind == VisitorKind.PAINTER) PAINTER_EVERY_DAYS else OLD_COUPLE_EVERY_DAYS
        return day - last >= every
    }

    /** Their anniversary, from the date they got together (yyyy-MM-dd): the old couple is always there. */
    fun isAnniversary(together: String?, today: LocalDate): Boolean {
        val d = together?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        return d.month == today.month && d.day == today.day && today.year > d.year
    }
}

/** When each visitor last came, the paintings, and the anniversary notes, in `tiny_us_prefs`. */
class VisitorStore(private val storage: KeyValueStorage) {
    fun lastVisit(kind: VisitorKind): Long? = storage.getString(KEY_LAST + kind.name, null)?.toLongOrNull()

    fun lastAnyVisit(): Long? = storage.getString(KEY_LAST_ANY, null)?.toLongOrNull()

    fun noteVisit(kind: VisitorKind, epochDay: Long) {
        storage.putString(KEY_LAST + kind.name, epochDay.toString())
        storage.putString(KEY_LAST_ANY, epochDay.toString())
    }

    private var cached: List<Painting>? = null

    /** Every painting, oldest first; the newest hangs in the loft. Read once, then kept. */
    fun paintings(): List<Painting> {
        cached?.let { return it }
        val raw = storage.getString(KEY_PAINTINGS, null) ?: return emptyList<Painting>().also { cached = it }
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Painting(
                    o.optLong("day"), o.optString("scene"), o.optString("weather"), o.optString("phase"),
                    o.optInt("boyOutfit"), o.optInt("girlOutfit"), o.optString("together", "APART"), o.optBoolean("mochi")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }.also { cached = it }
    }

    fun addPainting(p: Painting) {
        val arr = JSONArray()
        (paintings() + p).forEach { q ->
            arr.put(JSONObject().apply {
                put("day", q.epochDay); put("scene", q.scene); put("weather", q.weather); put("phase", q.phase)
                put("boyOutfit", q.boyOutfit); put("girlOutfit", q.girlOutfit); put("together", q.together); put("mochi", q.mochi)
            })
        }
        storage.putString(KEY_PAINTINGS, arr.toString())
        cached = null
    }

    /** The years the old couple left them an anniversary note. */
    fun noteYears(): Set<Int> = storage.getStringSet(KEY_NOTES).mapNotNull { it.toIntOrNull() }.toSet()

    fun addNote(year: Int) = storage.putStringSet(KEY_NOTES, storage.getStringSet(KEY_NOTES) + year.toString())

    private companion object {
        const val KEY_LAST = "visitor_last_"
        const val KEY_LAST_ANY = "visitor_last_any"
        const val KEY_PAINTINGS = "visitor_paintings_json"
        const val KEY_NOTES = "visitor_notes"
    }
}
