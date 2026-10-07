package com.example.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/*
 * Plan 10, D: the Friday fox. Every Friday a little fox cub trots into the meadow with a ball and
 * plays catch with Mochi. The twist: if they miss a Friday, the fox leaves its ball in the grass
 * for them, as if it waited all day.
 */
object FridayFox {
    /** For tests and previews: the day to treat as today. */
    var todayOverride: LocalDate? = null

    fun today(): LocalDate = todayOverride ?: CoupleDates.today()

    fun isFriday(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.FRIDAY

    /** The most recent Friday strictly before [date]. */
    fun lastFriday(date: LocalDate): LocalDate {
        var d = date.minus(1, DateTimeUnit.DAY)
        while (!isFriday(d)) d = d.minus(1, DateTimeUnit.DAY)
        return d
    }

    /**
     * The Friday whose ball is lying in the grass on [today], or null. It's there when they had the
     * app before last Friday, the fox didn't find them that day, and they haven't picked it up.
     * On a Friday the fox itself is coming, so there's no ball waiting.
     */
    fun ballWaiting(store: FoxStore, today: LocalDate): LocalDate? {
        if (isFriday(today)) return null
        val friday = lastFriday(today)
        val since = store.firstSeen() ?: return null
        if (since > friday) return null
        if (store.visited(friday) || store.ballCollected(friday)) return null
        return friday
    }
}

/** What the fox has done, in `tiny_us_prefs` (so backups carry it). Dates are yyyy-MM-dd. */
class FoxStore(private val storage: KeyValueStorage) {
    /** The first day the app looked for the fox; a new install never finds a ball on day one. */
    fun firstSeen(): LocalDate? = storage.getString(KEY_FIRST_SEEN, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun noteSeen(today: LocalDate) {
        val first = firstSeen()
        if (first == null || today < first) storage.putString(KEY_FIRST_SEEN, today.toString())
    }

    fun visited(friday: LocalDate): Boolean = friday.toString() in storage.getStringSet(KEY_VISITS)

    fun markVisited(friday: LocalDate) = storage.putStringSet(KEY_VISITS, storage.getStringSet(KEY_VISITS) + friday.toString())

    fun ballCollected(friday: LocalDate): Boolean = friday.toString() in storage.getStringSet(KEY_BALLS)

    fun markBallCollected(friday: LocalDate) = storage.putStringSet(KEY_BALLS, storage.getStringSet(KEY_BALLS) + friday.toString())

    /** How many Fridays the fox has visited. */
    fun visits(): Int = storage.getStringSet(KEY_VISITS).size

    private companion object {
        const val KEY_FIRST_SEEN = "fox_first_seen"
        const val KEY_VISITS = "fox_visits"
        const val KEY_BALLS = "fox_balls_collected"
    }
}
