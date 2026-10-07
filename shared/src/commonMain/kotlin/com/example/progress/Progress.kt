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
    /** [finder] is BOY or GIRL when known, for the collection book's "who found it first" (plan 09, H). */
    data class DiscoveryFound(val kind: String, val finder: String? = null) : ProgressEvent()
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
    /** Mochi was petted, fed or played with on [epochDay] (plan 07, D2). */
    data class MochiCare(val points: Int, val epochDay: Long) : ProgressEvent()
    /** A keepsake given from one partner to the other (plan 07, D3); it goes on the home shelf. */
    data class GiftGiven(val item: String, val fromBoy: Boolean) : ProgressEvent()
    /** A dish served from the cooking game (plan 07, C3): it goes in the recipe book and the box. */
    data class DishCooked(val recipe: String) : ProgressEvent()
    /** Something reeled in at the pier (plan 07, C4): a [com.example.games.FishingCatch] name. */
    data class FishCaught(val kind: String) : ProgressEvent()
    /** Garden care (plan 07, C5): a seed planted, a plot watered, rain, a flower picked. */
    data class GardenPlanted(val plot: Int, val flower: String) : ProgressEvent()
    /** [by] is BOY or GIRL when it's said who watered (the shared plot needs both), else null. */
    data class GardenWatered(val plot: Int, val epochDay: Long, val by: String? = null) : ProgressEvent()
    /** A ripe crop or herb harvested into the pantry (plan 09, E3). */
    data class CropHarvested(val plot: Int) : ProgressEvent()
    data class GardenRained(val epochDay: Long) : ProgressEvent()
    data class FlowerPicked(val plot: Int) : ProgressEvent()
    /** One of them asked for something and the player answered (plan 07, D1). */
    data class RequestGranted(val kind: String) : ProgressEvent()
    /** A birthday surprise was held (plan 09, A). */
    data class BirthdayCelebrated(val forBoy: Boolean, val belated: Boolean) : ProgressEvent()
    /** A Thank-You Jar was filled (plan 09, C); it goes in the keepsake box. */
    object ThankYouJarFilled : ProgressEvent()
    /** A Phones Down session ran its full time (plan 09, C). */
    data class PhonesDown(val minutes: Int) : ProgressEvent()
    /** A festival was celebrated (plan 09, D); its keepsake goes in the box. */
    data class FestivalCelebrated(val festival: String) : ProgressEvent()
    /** The Friday fox came to play with Mochi (plan 10, D). */
    object FoxVisited : ProgressEvent()
    /** They found the ball the fox left on a Friday they missed (plan 10, D). */
    object FoxBallFound : ProgressEvent()
}

/**
 * Mochi's fondness (plan 07, D2): it only grows, a little each day (petting, treats, toys), with a
 * gentle daily cap so it's a slow friendship and not a tapping contest.
 */
object MochiFondness {
    /** Fondness for each level: friendly, cuddly, best friend. */
    val LEVELS = listOf(20, 60, 150)
    const val DAILY_CAP = 12

    fun level(fondness: Int): Int = LEVELS.count { fondness >= it }

    /** How far toward the next level (0..1); 1 at the top level. */
    fun towardNext(fondness: Int): Float {
        val l = level(fondness)
        if (l >= LEVELS.size) return 1f
        val from = if (l == 0) 0 else LEVELS[l - 1]
        return (fondness - from).toFloat() / (LEVELS[l] - from)
    }
}

/** Keepsakes that can be given as gifts (Mochi's toy stays Mochi's). */
/** Keepsake-box prefix for the pantry's produce (plan 09, E3); the Keepsakes page doesn't list it. */
const val PANTRY = "pantry:"

object Gifts {
    /** A bouquet from the garden, as a keepsake id. */
    const val BOUQUET = "garden:BOUQUET"
    val GIVEABLE = listOf(
        "discovery:WILDFLOWER", "discovery:RED_LEAF", "discovery:LOVE_NOTE", "discovery:SEASHELL", "discovery:STAR_PEBBLE",
        BOUQUET, "catch:SEASHELL", "catch:BOTTLE", "catch:GOLDEN_FISH",
        "dish:pancakes", "dish:soup", "dish:dumplings", "dish:cookies", "dish:tea",
        "dish:tomato_soup", "dish:strawberry_pancakes", "dish:pumpkin_pie", "dish:herb_tea", "dish:apple_crumble", "dish:pea_soup"
    )
    const val SHELF = "shelf:"
}

/** Mini-game ids. */
object Game {
    const val CATCH = "catch"
    const val COOKING = "cooking"
    const val FISHING = "fishing"
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
    const val MOCHI_FONDNESS = "mochi_fondness"
    const val GIFTS = "gifts"
    const val DISHES = "dishes"
    const val FISH = "fish"
    const val GOLDEN_FISH = "golden_fish"
    const val BLOOMS_PICKED = "blooms_picked"
    const val BOUQUETS = "bouquets"
    const val REQUESTS = "requests"
    const val HARVESTS = "harvests"
    const val GARDEN_DISHES = "garden_dishes"
    const val BIRTHDAYS = "birthdays"
    const val THANK_YOU_JARS = "thank_you_jars"
    const val PHONES_DOWN = "phones_down"
    const val FESTIVALS = "festivals"
}

/** Set keys: things seen at least once. */
object Seen {
    const val SCENES = "scenes"
    const val WEATHERS = "weathers"
    const val SEASONS = "seasons"
    const val CONSTELLATIONS = "constellations"
    const val FULL_MOON_NIGHTS = "full_moon_nights"
    /** Recipes cooked at least once: the recipe book. */
    const val RECIPES = "recipes"
    /** Festivals celebrated at least once (plan 09, D). */
    const val FESTIVALS = "festivals"
    /** The kinds of request granted at least once. */
    const val REQUEST_KINDS = "request_kinds"
    /** The crops harvested at least once. */
    const val CROPS = "crops"
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
    val unlocked: Set<String> = emptySet(),
    /** The garden's plots and the flowers held for a bouquet (plan 07, C5). */
    val garden: com.example.games.GardenPlots = com.example.games.GardenPlots()
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
        is ProgressEvent.MochiCare -> {
            // Points earned today are tracked, and reset when the day changes.
            val day = event.epochDay.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            val fresh = if (count("mochi_care_day") == day) this
            else copy(counters = counters + ("mochi_care_day" to day) + ("mochi_care_today" to 0))
            val add = event.points.coerceAtMost((MochiFondness.DAILY_CAP - fresh.count("mochi_care_today")).coerceAtLeast(0))
            if (add <= 0) fresh else fresh.plus("mochi_care_today", add).plus(Counter.MOCHI_FONDNESS, add)
        }
        is ProgressEvent.DishCooked -> {
            // A garden recipe uses up what it took from the pantry (plan 09, E3).
            val used = com.example.games.Recipes.byId(event.recipe)?.pantry.orEmpty()
            var next = plus(Counter.DISHES).see(Seen.RECIPES, event.recipe).keep("dish:${event.recipe}")
            for (i in used) {
                val key = PANTRY + i.name
                next = next.copy(keepsakes = next.keepsakes + (key to ((next.keepsakes[key] ?: 0) - 1).coerceAtLeast(0)))
            }
            if (used.isNotEmpty()) next = next.plus(Counter.GARDEN_DISHES)
            next
        }
        is ProgressEvent.CropHarvested -> {
            val (after, produce) = garden.harvest(event.plot)
            if (produce == null) this
            else copy(garden = after).keep(PANTRY + produce.name).plus(Counter.HARVESTS).see(Seen.CROPS, produce.name)
        }
        is ProgressEvent.FishCaught -> {
            val kind = com.example.games.FishingCatch.entries.firstOrNull { it.name == event.kind }
            var next = keep("catch:${event.kind}").plus("games_${Game.FISHING}")
            if (kind?.isFish == true) next = next.plus(Counter.FISH)
            if (kind == com.example.games.FishingCatch.GOLDEN_FISH) next = next.plus(Counter.GOLDEN_FISH)
            next
        }
        is ProgressEvent.GardenPlanted -> copy(garden = garden.plant(event.plot, event.flower))
        is ProgressEvent.GardenWatered -> copy(
            garden = garden.water(event.plot, event.epochDay, event.by?.let { com.example.games.PlotOwner.valueOf(it) })
        )
        is ProgressEvent.GardenRained -> copy(garden = garden.rain(event.epochDay))
        is ProgressEvent.FlowerPicked -> {
            val (after, bouquet) = garden.pick(event.plot)
            if (after == garden) this
            else {
                val picked = copy(garden = after).plus(Counter.BLOOMS_PICKED)
                if (bouquet == null) picked else picked.keep(Gifts.BOUQUET).plus(Counter.BOUQUETS)
            }
        }
        is ProgressEvent.RequestGranted -> plus(Counter.REQUESTS).see(Seen.REQUEST_KINDS, event.kind)
        is ProgressEvent.BirthdayCelebrated -> plus(Counter.BIRTHDAYS)
        is ProgressEvent.ThankYouJarFilled -> plus(Counter.THANK_YOU_JARS).keep("jar:THANK_YOU")
        is ProgressEvent.PhonesDown -> plus(Counter.PHONES_DOWN)
        is ProgressEvent.FestivalCelebrated -> see(Seen.FESTIVALS, event.festival).plus(Counter.FESTIVALS).keep("festival:${event.festival}")
        ProgressEvent.FoxVisited -> keep("fox:VISIT")
        ProgressEvent.FoxBallFound -> keep("fox:BALL")
        is ProgressEvent.GiftGiven -> {
            val have = keepsakes[event.item] ?: 0
            if (have <= 0) this
            else copy(keepsakes = keepsakes + (event.item to have - 1)).keep(Gifts.SHELF + event.item).plus(Counter.GIFTS)
        }
    }

    /** What's in the pantry (plan 09, E3): garden produce and how many of each. */
    val pantry: Map<com.example.games.Ingredient, Int>
        get() = com.example.games.Ingredient.entries.filter { it.isProduce }
            .associateWith { keepsakes[PANTRY + it.name] ?: 0 }.filterValues { it > 0 }

    /** The gifts on the home shelf, as keepsake ids, each once (newest kinds last). */
    val shelf: List<String>
        get() = keepsakes.filter { it.key.startsWith(Gifts.SHELF) && it.value > 0 }.keys.map { it.removePrefix(Gifts.SHELF) }.sorted()
}
