package com.example.progress

/*
 * The couple's progress (plan 07, A): what they've done, seen, found and earned. Plain Kotlin, so
 * the rules are easy to test; ProgressStore keeps it in tiny_us_prefs (so backups carry it).
 * Nothing here ever goes down or expires.
 */

/** Something that happened in the world, reported by the engine or the screens. */
sealed class ProgressEvent {
    object RainbowWish : ProgressEvent()
    data class ConstellationFound(val id: String) : ProgressEvent()
    object SnowmanBuilt : ProgressEvent()
    /** A discovery picked up (wildflower, seashell...): it goes into the keepsake box. */
    data class DiscoveryFound(val kind: String) : ProgressEvent()
    /** A falling snowflake, petal, leaf or dandelion puff caught. */
    data class Caught(val kind: String) : ProgressEvent()
    data class SceneVisited(val scene: String) : ProgressEvent()
    data class WeatherSeen(val weather: String, val season: String) : ProgressEvent()
    /** A full moon seen on [epochDay]; counted once a night. */
    data class FullMoonSeen(val epochDay: Long) : ProgressEvent()
    object TinyMoment : ProgressEvent()
    object LoveNote : ProgressEvent()
    object DreamWritten : ProgressEvent()
    data class DaysTogether(val days: Long) : ProgressEvent()
    /** A mini-game round finished with [score] (plan 07, C). */
    data class GamePlayed(val game: String, val score: Int) : ProgressEvent()
}

/** Mini-game ids. */
object Game {
    const val CATCH = "catch"
}

/** Counter keys, so the firsts and the screens agree on names. */
object Counter {
    const val RAINBOW_WISHES = "rainbow_wishes"
    const val SNOWMEN = "snowmen"
    const val FULL_MOONS = "full_moons"
    const val TINY_MOMENTS = "tiny_moments"
    const val LOVE_NOTES = "love_notes"
    const val DREAMS = "dreams"
    const val CATCHES = "catches"
    const val DISCOVERIES = "discoveries"
    const val DAYS_TOGETHER = "days_together"
}

/** Set keys: things seen at least once. */
object Seen {
    const val SCENES = "scenes"
    const val WEATHERS = "weathers"
    const val SEASONS = "seasons"
    const val CONSTELLATIONS = "constellations"
    const val FULL_MOON_NIGHTS = "full_moon_nights"
}

data class ProgressState(
    val counters: Map<String, Int> = emptyMap(),
    val seen: Map<String, Set<String>> = emptyMap(),
    /** Keepsake box: what's been found or made, with how many. */
    val keepsakes: Map<String, Int> = emptyMap(),
    /** Best scores in the mini-games. */
    val best: Map<String, Int> = emptyMap(),
    /** Little firsts earned, with the epoch day they were earned. */
    val firsts: Map<String, Long> = emptyMap(),
    /** Reward items unlocked (never locked again). */
    val unlocked: Set<String> = emptySet()
) {
    fun count(key: String): Int = counters[key] ?: 0
    fun seenSet(key: String): Set<String> = seen[key] ?: emptySet()

    fun plus(key: String, by: Int = 1) = copy(counters = counters + (key to count(key) + by))
    /** Raises a counter to at least [value] (for running totals like days together). */
    fun atLeast(key: String, value: Int) = if (value <= count(key)) this else copy(counters = counters + (key to value))
    fun see(key: String, item: String) = if (item in seenSet(key)) this else copy(seen = seen + (key to seenSet(key) + item))
    fun keep(item: String, by: Int = 1) = copy(keepsakes = keepsakes + (item to (keepsakes[item] ?: 0) + by))
    fun bestScore(game: String, score: Int) = if (score <= (best[game] ?: 0)) this else copy(best = best + (game to score))

    /** The state after [event]. */
    fun record(event: ProgressEvent): ProgressState = when (event) {
        ProgressEvent.RainbowWish -> plus(Counter.RAINBOW_WISHES)
        is ProgressEvent.ConstellationFound -> see(Seen.CONSTELLATIONS, event.id)
        ProgressEvent.SnowmanBuilt -> plus(Counter.SNOWMEN)
        is ProgressEvent.DiscoveryFound -> plus(Counter.DISCOVERIES).keep("discovery:${event.kind}")
        is ProgressEvent.Caught -> plus(Counter.CATCHES).plus("caught:${event.kind}")
        is ProgressEvent.SceneVisited -> see(Seen.SCENES, event.scene)
        is ProgressEvent.WeatherSeen -> see(Seen.WEATHERS, event.weather).see(Seen.SEASONS, event.season)
        is ProgressEvent.FullMoonSeen ->
            if (event.epochDay.toString() in seenSet(Seen.FULL_MOON_NIGHTS)) this
            else see(Seen.FULL_MOON_NIGHTS, event.epochDay.toString()).plus(Counter.FULL_MOONS)
        ProgressEvent.TinyMoment -> plus(Counter.TINY_MOMENTS)
        ProgressEvent.LoveNote -> plus(Counter.LOVE_NOTES)
        ProgressEvent.DreamWritten -> plus(Counter.DREAMS)
        is ProgressEvent.DaysTogether -> atLeast(Counter.DAYS_TOGETHER, event.days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        is ProgressEvent.GamePlayed -> plus("games_${event.game}").bestScore(event.game, event.score)
    }
}
