package com.example.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number

/**
 * The couple's own seasonal festivals (plan 09, D). Each is something the two of them do
 * together, with a secret choice revealed at the end. (Autumn has none: the user dropped the
 * Harvest Fair.)
 */
enum class Festival(val season: String, val scene: com.example.scene.SceneType) {
    /** Spring, in the meadow: each secretly picks the flowers for the other's crown. */
    BLOSSOM_PICNIC("SPRING", com.example.scene.SceneType.FLOWER),
    /** Summer, at the pier at dusk: private wishes on lanterns, sealed until next summer. */
    LANTERN_NIGHT("SUMMER", com.example.scene.SceneType.SEASIDE_PIER),
    /** Winter, in the living room: a secret gift each from the keepsakes, opened together. */
    GIFT_EXCHANGE("WINTER", com.example.scene.SceneType.SLEEP)
}

/** Where the calendar is: the poster's up, or it's the day. */
enum class FestivalPhase { POSTER, ON }

data class FestivalDay(val festival: Festival, val phase: FestivalPhase, val year: Int)

/**
 * When each festival is: the 14th to the 16th of the middle month of its season, which is six
 * months later in the southern hemisphere. A poster goes up on the two days before.
 */
object Festivals {
    const val FIRST_DAY = 14
    const val DAYS = 3
    const val POSTER_DAYS = 2

    /** Shows one festival (on its day) everywhere instead of today's; for previews and tests. */
    var override: Festival? = null

    /** The middle month of [festival]'s season (1..12). */
    fun month(festival: Festival, southern: Boolean): Int {
        val north = when (festival.season) {
            "SPRING" -> 4
            "SUMMER" -> 7
            "AUTUMN" -> 10
            else -> 1
        }
        return if (southern) (north + 5) % 12 + 1 else north
    }

    /** Which festival [date] is in, if any, and whether it's the poster or the day. */
    fun on(date: LocalDate, southern: Boolean): FestivalDay? {
        override?.let { return FestivalDay(it, FestivalPhase.ON, date.year) }
        for (f in Festival.entries) {
            val start = LocalDate(date.year, month(f, southern), FIRST_DAY)
            val end = LocalDate(date.year, month(f, southern), FIRST_DAY + DAYS - 1)
            if (date in start..end) return FestivalDay(f, FestivalPhase.ON, date.year)
            if (date >= start.minus(POSTER_DAYS, DateTimeUnit.DAY) && date < start) return FestivalDay(f, FestivalPhase.POSTER, date.year)
        }
        return null
    }

    /** True for the countries the weather already treats as southern (SeasonalWeather). */
    fun isSouthern(region: String?): Boolean =
        com.example.engine.SeasonalWeather.seasonOf(1, region.orEmpty()) == "SUMMER"

    /** Today's festival for this phone's region. */
    fun today(): FestivalDay? = on(CoupleDates.today(), isSouthern(androidx.compose.ui.text.intl.Locale.current.region))
}

/** The two secret picks for one festival, as indexes or keepsake ids, and the notes. */
data class FestivalPicks(
    val boy: String = "",
    val girl: String = "",
    val boyNote: String = "",
    val girlNote: String = "",
    val revealed: Boolean = false
)

/**
 * What the festivals keep, in `tiny_us_prefs` (so backups carry it). Lantern wishes are sealed
 * letters (`LetterKind.LANTERN`) that open at next summer's Lantern Night.
 */
class FestivalStore(private val storage: KeyValueStorage) {
    /** The years [festival] was celebrated. */
    fun years(festival: Festival): Set<Int> =
        storage.getStringSet(KEY_YEARS + festival.name).mapNotNull { it.toIntOrNull() }.toSet()

    fun celebrated(festival: Festival, year: Int): Boolean = year in years(festival)

    fun markCelebrated(festival: Festival, year: Int) =
        storage.putStringSet(KEY_YEARS + festival.name, years(festival).map { it.toString() }.toSet() + year.toString())

    fun picks(festival: Festival, year: Int): FestivalPicks {
        val raw = storage.getString("$KEY_PICKS${festival.name}_$year", null) ?: return FestivalPicks()
        return try {
            val o = JSONObject(raw)
            FestivalPicks(o.optString("boy"), o.optString("girl"), o.optString("boyNote"), o.optString("girlNote"), o.optBoolean("revealed"))
        } catch (_: Exception) {
            FestivalPicks()
        }
    }

    fun savePicks(festival: Festival, year: Int, picks: FestivalPicks) {
        val o = JSONObject().apply {
            put("boy", picks.boy); put("girl", picks.girl)
            put("boyNote", picks.boyNote); put("girlNote", picks.girlNote)
            put("revealed", picks.revealed)
        }
        storage.putString("$KEY_PICKS${festival.name}_$year", o.toString())
    }

    companion object {
        private const val KEY_YEARS = "festival_years_"
        private const val KEY_PICKS = "festival_picks_"
    }
}
