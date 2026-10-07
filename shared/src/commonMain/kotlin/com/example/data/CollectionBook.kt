package com.example.data

import com.example.progress.Gifts
import com.example.progress.ProgressState
import com.example.progress.Seen

/*
 * Plan 09, H: the collection book. A scrapbook of everything the couple has found, caught, cooked,
 * grown and celebrated, each with the little story of the first time: who, where, when. Things not
 * found yet show as a silhouette with a gentle hint; a few rare ones stay a mystery ("???").
 */

enum class CollectionPage { FINDS, SEA, KITCHEN, GARDEN, MOMENTS }

/** When or where a thing turns up, as a hint for the ones not found yet. */
enum class CollectionHint { SPRING, SUMMER, AUTUMN, WINTER, NIGHT, RAIN, FOR_HER, FRIDAY }

/**
 * One thing that can be collected. [key] is its keepsake id (`discovery:SEASHELL`, `catch:PEARL`,
 * `dish:soup`, `crop:TOMATO`, `festival:LANTERN_NIGHT`...). A [hidden] one shows as "???" until found.
 */
data class CollectionEntry(
    val key: String,
    val page: CollectionPage,
    val hint: CollectionHint? = null,
    val hidden: Boolean = false
)

/** The first time something was found. [by] is BOY, GIRL or BOTH; [forPartner] when it was found for the other. */
data class FirstFind(
    val key: String,
    val by: String,
    val forPartner: Boolean,
    val scene: String,
    val weather: String,
    val phase: String,
    val epochDay: Long
)

object CollectionBook {
    val ENTRIES: List<CollectionEntry> = buildList {
        // Little finds, on walks
        add(CollectionEntry("discovery:WILDFLOWER", CollectionPage.FINDS))
        add(CollectionEntry("discovery:RED_LEAF", CollectionPage.FINDS, CollectionHint.AUTUMN))
        add(CollectionEntry("discovery:LOVE_NOTE", CollectionPage.FINDS))
        add(CollectionEntry("discovery:MOCHI_TOY", CollectionPage.FINDS))
        add(CollectionEntry("discovery:SEASHELL", CollectionPage.FINDS))
        add(CollectionEntry("discovery:STAR_PEBBLE", CollectionPage.FINDS, CollectionHint.NIGHT, hidden = true))
        // Sea and shore, from fishing at the pier
        add(CollectionEntry("catch:MINNOW", CollectionPage.SEA))
        add(CollectionEntry("catch:CARP", CollectionPage.SEA))
        add(CollectionEntry("catch:SEASHELL", CollectionPage.SEA))
        add(CollectionEntry("catch:OLD_BOOT", CollectionPage.SEA))
        add(CollectionEntry("catch:BOTTLE", CollectionPage.SEA))
        add(CollectionEntry("catch:BLOSSOM_KOI", CollectionPage.SEA, CollectionHint.SPRING))
        add(CollectionEntry("catch:RAIN_TROUT", CollectionPage.SEA, CollectionHint.RAIN))
        add(CollectionEntry("catch:ICE_COD", CollectionPage.SEA, CollectionHint.WINTER))
        add(CollectionEntry("catch:MOON_JELLY", CollectionPage.SEA, CollectionHint.NIGHT))
        add(CollectionEntry("catch:GLOW_SQUID", CollectionPage.SEA, CollectionHint.NIGHT, hidden = true))
        add(CollectionEntry("catch:GOLDEN_FISH", CollectionPage.SEA, hidden = true))
        add(CollectionEntry("catch:HEART_SHELL", CollectionPage.SEA, CollectionHint.FOR_HER))
        add(CollectionEntry("catch:PEARL", CollectionPage.SEA, CollectionHint.FOR_HER, hidden = true))
        add(CollectionEntry("catch:SEA_GLASS_HEART", CollectionPage.SEA, CollectionHint.FOR_HER, hidden = true))
        // Kitchen: every recipe, the garden ones in their season
        for (r in com.example.games.Recipes.ALL) {
            val crop = r.pantry.firstOrNull()?.let { ing -> com.example.games.Seeds.ALL.firstOrNull { it.produce == ing } }
            add(CollectionEntry("dish:${r.id}", CollectionPage.KITCHEN, crop?.seasons?.singleOrNull()?.let(::seasonHint)))
        }
        // Garden: a bouquet, and each crop, in its season
        add(CollectionEntry(Gifts.BOUQUET, CollectionPage.GARDEN))
        for (s in com.example.games.Seeds.ALL) {
            val produce = s.produce ?: continue
            add(CollectionEntry("crop:${produce.name}", CollectionPage.GARDEN, s.seasons.singleOrNull()?.let(::seasonHint)))
        }
        // Moments: the festivals and a full Thank-You Jar
        add(CollectionEntry("festival:BLOSSOM_PICNIC", CollectionPage.MOMENTS, CollectionHint.SPRING))
        add(CollectionEntry("festival:LANTERN_NIGHT", CollectionPage.MOMENTS, CollectionHint.SUMMER))
        add(CollectionEntry("festival:GIFT_EXCHANGE", CollectionPage.MOMENTS, CollectionHint.WINTER))
        add(CollectionEntry("jar:THANK_YOU", CollectionPage.MOMENTS))
        // The Friday fox (plan 10, D), and the ball it leaves on a Friday they missed
        add(CollectionEntry("fox:VISIT", CollectionPage.MOMENTS, CollectionHint.FRIDAY))
        add(CollectionEntry("fox:BALL", CollectionPage.MOMENTS, CollectionHint.FRIDAY, hidden = true))
    }

    private fun seasonHint(season: String): CollectionHint? = when (season) {
        "SPRING" -> CollectionHint.SPRING
        "SUMMER" -> CollectionHint.SUMMER
        "AUTUMN" -> CollectionHint.AUTUMN
        "WINTER" -> CollectionHint.WINTER
        else -> null
    }

    fun page(page: CollectionPage): List<CollectionEntry> = ENTRIES.filter { it.page == page }

    /**
     * Whether [key] has ever been had, from the progress alone: still in the box, given to the
     * shelf, cooked, harvested or celebrated. (Finds from before the book count too.)
     */
    fun isFound(key: String, progress: ProgressState): Boolean {
        val kind = key.substringAfter(":")
        return when {
            key.startsWith("crop:") -> kind in progress.seenSet(Seen.CROPS)
            key.startsWith("dish:") -> kind in progress.seenSet(Seen.RECIPES) || (progress.keepsakes[key] ?: 0) > 0
            key.startsWith("festival:") -> kind in progress.seenSet(Seen.FESTIVALS)
            else -> (progress.keepsakes[key] ?: 0) > 0 || (progress.keepsakes[Gifts.SHELF + key] ?: 0) > 0
        }
    }

    /** How many things in the book have been found. */
    fun foundCount(progress: ProgressState): Int = ENTRIES.count { isFound(it.key, progress) }

    /**
     * The "Found you" page: the first thing either of them ever found was the other. Its love lines
     * open one by one as the book fills, the first from the start and the last when every page is
     * full; these are how many things it takes for each.
     */
    val FOUND_YOU_STEPS: List<Int> get() = listOf(0, 3, 8, 15, 22, 30, ENTRIES.size)

    /** How many of the "Found you" lines are open with [found] things found. */
    fun foundYouLinesOpen(found: Int): Int = FOUND_YOU_STEPS.count { found >= it }

    /** How many more things open the next line, or null when they are all open. */
    fun foundYouNextIn(found: Int): Int? = FOUND_YOU_STEPS.firstOrNull { found < it }?.let { it - found }

    /** The entries found in [after] but not in [before]: what the latest event brought. */
    fun newlyFound(before: ProgressState, after: ProgressState): List<String> =
        ENTRIES.filter { !isFound(it.key, before) && isFound(it.key, after) }.map { it.key }
}

/** The first-find stories, in `tiny_us_prefs` (so backups carry them). */
class CollectionStore(private val storage: KeyValueStorage) {
    private var cached: Map<String, FirstFind>? = null

    fun all(): Map<String, FirstFind> {
        cached?.let { return it }
        val raw = storage.getString(KEY, null)
        val map = if (raw.isNullOrEmpty()) emptyMap() else try {
            val arr = JSONArray(raw)
            (0 until arr.length()).associate { i ->
                val o = arr.getJSONObject(i)
                val f = FirstFind(
                    o.getString("key"), o.optString("by", "BOTH"), o.optBoolean("forPartner"),
                    o.optString("scene"), o.optString("weather"), o.optString("phase"), o.optLong("epochDay")
                )
                f.key to f
            }
        } catch (_: Exception) {
            emptyMap()
        }
        cached = map
        return map
    }

    fun first(key: String): FirstFind? = all()[key]

    /** Keeps the story of the first find of [find]'s key; later finds don't change it. */
    fun record(find: FirstFind) {
        val now = all()
        if (find.key in now) return
        val next = now + (find.key to find)
        cached = next
        val arr = JSONArray()
        next.values.forEach { f ->
            arr.put(JSONObject().apply {
                put("key", f.key); put("by", f.by); put("forPartner", f.forPartner)
                put("scene", f.scene); put("weather", f.weather); put("phase", f.phase); put("epochDay", f.epochDay)
            })
        }
        storage.putString(KEY, arr.toString())
    }

    companion object {
        private const val KEY = "collection_first_finds_json"
    }
}
