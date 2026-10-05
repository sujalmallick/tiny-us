package com.example.progress

import androidx.annotation.StringRes
import com.example.R
import com.example.scene.SceneType

/**
 * A gentle achievement (plan 07, B): earned once, kept forever. [hint] is what the page shows
 * before it's earned, so there's something to look for without a checklist feel. [reward] is an
 * item it unlocks, if any (see [Rewards]).
 */
class LittleFirst(
    val id: String,
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val reward: String? = null,
    val earned: (ProgressState) -> Boolean
)

/** Reward item ids. New items only: nothing that's free today is ever locked. */
object Rewards {
    const val RAINBOW_SCARF = "accessory_rainbow_scarf"
    const val SNOWMAN_BEANIE = "accessory_snowman_beanie"
    const val STAR_HOODIE = "outfit_star_hoodie"
    const val STARRY_ROOM = "theme_starry_night"
}

object LittleFirsts {
    /** The discovery kinds, all of which count toward "a little collector". */
    val DISCOVERY_KINDS = listOf("WILDFLOWER", "RED_LEAF", "LOVE_NOTE", "MOCHI_TOY", "SEASHELL", "STAR_PEBBLE")

    val ALL: List<LittleFirst> = listOf(
        LittleFirst("first_rainbow", R.string.first_rainbow, R.string.first_rainbow_hint, Rewards.RAINBOW_SCARF) { it.count(Counter.RAINBOW_WISHES) >= 1 },
        LittleFirst("first_full_moon", R.string.first_full_moon, R.string.first_full_moon_hint) { it.count(Counter.FULL_MOONS) >= 1 },
        LittleFirst("first_snowman", R.string.first_snowman, R.string.first_snowman_hint, Rewards.SNOWMAN_BEANIE) { it.count(Counter.SNOWMEN) >= 1 },
        LittleFirst("first_star", R.string.first_star, R.string.first_star_hint) { it.seenSet(Seen.CONSTELLATIONS).isNotEmpty() },
        LittleFirst("all_stars", R.string.all_stars, R.string.all_stars_hint, Rewards.STAR_HOODIE) { it.seenSet(Seen.CONSTELLATIONS).size >= 3 },
        LittleFirst("every_scene", R.string.every_scene, R.string.every_scene_hint) { it.seenSet(Seen.SCENES).size >= SceneType.values().size },
        LittleFirst("four_seasons", R.string.four_seasons, R.string.four_seasons_hint) { it.seenSet(Seen.SEASONS).size >= 4 },
        LittleFirst("every_weather", R.string.every_weather, R.string.every_weather_hint) { it.seenSet(Seen.WEATHERS).size >= 5 },
        LittleFirst("moments_10", R.string.moments_10, R.string.moments_10_hint) { it.count(Counter.TINY_MOMENTS) >= 10 },
        LittleFirst("moments_50", R.string.moments_50, R.string.moments_50_hint) { it.count(Counter.TINY_MOMENTS) >= 50 },
        LittleFirst("first_love_note", R.string.first_love_note, R.string.first_love_note_hint) { it.count(Counter.LOVE_NOTES) >= 1 },
        LittleFirst("first_dream", R.string.first_dream, R.string.first_dream_hint) { it.count(Counter.DREAMS) >= 1 },
        LittleFirst("first_discovery", R.string.first_discovery, R.string.first_discovery_hint) { it.count(Counter.DISCOVERIES) >= 1 },
        LittleFirst("all_discoveries", R.string.all_discoveries, R.string.all_discoveries_hint) { s -> DISCOVERY_KINDS.all { (s.keepsakes["discovery:$it"] ?: 0) > 0 } },
        LittleFirst("catches_100", R.string.catches_100, R.string.catches_100_hint) { it.count(Counter.CATCHES) >= 100 },
        LittleFirst("days_7", R.string.days_7, R.string.days_7_hint) { it.count(Counter.DAYS_TOGETHER) >= 7 },
        LittleFirst("days_30", R.string.days_30, R.string.days_30_hint) { it.count(Counter.DAYS_TOGETHER) >= 30 },
        LittleFirst("days_100", R.string.days_100, R.string.days_100_hint, Rewards.STARRY_ROOM) { it.count(Counter.DAYS_TOGETHER) >= 100 },
        LittleFirst("days_365", R.string.days_365, R.string.days_365_hint) { it.count(Counter.DAYS_TOGETHER) >= 365 },
        LittleFirst("first_catch_game", R.string.first_catch_game, R.string.first_catch_game_hint) { it.count("games_${Game.CATCH}") >= 1 },
        LittleFirst("catch_30", R.string.catch_30, R.string.catch_30_hint) { (it.best[Game.CATCH] ?: 0) >= com.example.games.CatchGame.GOOD_SCORE }
    )

    fun byId(id: String): LittleFirst? = ALL.firstOrNull { it.id == id }

    /**
     * Records [event] and returns the new state together with the firsts it just earned. Earned
     * firsts are dated [today] (epoch day) and unlock their rewards; nothing is ever taken back.
     */
    fun apply(state: ProgressState, event: ProgressEvent, today: Long): Pair<ProgressState, List<LittleFirst>> {
        var next = state.record(event)
        val earned = ALL.filter { it.id !in next.firsts && it.earned(next) }
        if (earned.isNotEmpty()) {
            next = next.copy(
                firsts = next.firsts + earned.associate { it.id to today },
                unlocked = next.unlocked + earned.mapNotNull { it.reward }
            )
        }
        return next to earned
    }
}
