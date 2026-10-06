package com.example.progress

import com.example.scene.SceneType
import com.example.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * A gentle achievement (plan 07, B): earned once, kept forever. [hint] is what the page shows
 * before it's earned, so there's something to look for without a checklist feel. [reward] is an
 * item it unlocks, if any (see [Rewards]).
 */
class LittleFirst(
    val id: String,
    val title: StringResource,
    val hint: StringResource,
    val reward: String? = null,
    val earned: (ProgressState) -> Boolean
)

/** Reward item ids. New items only: nothing that's free today is ever locked. */
object Rewards {
    const val RAINBOW_SCARF = "accessory_rainbow_scarf"
    const val SNOWMAN_BEANIE = "accessory_snowman_beanie"
    const val STAR_HOODIE = "outfit_star_hoodie"
    const val STARRY_ROOM = "theme_starry_night"
    const val MOCHI_HEADBAND = "accessory_mochi_headband"
    const val CHEF_APRON = "accessory_chef_apron"
    const val FISHER_HAT = "accessory_fisher_hat"
    const val FLOWER_CROWN = "accessory_flower_crown"
    const val PARTY_HAT = "accessory_party_hat"
}

object LittleFirsts {
    /** The discovery kinds, all of which count toward "a little collector". */
    /** The kinds of request the couple can make (plan 07 D1): warmth, tea, a song, a snack, Mochi. */
    const val REQUEST_KIND_COUNT = 5

    val DISCOVERY_KINDS = listOf("WILDFLOWER", "RED_LEAF", "LOVE_NOTE", "MOCHI_TOY", "SEASHELL", "STAR_PEBBLE")

    val ALL: List<LittleFirst> = listOf(
        LittleFirst("first_rainbow", Res.string.first_rainbow, Res.string.first_rainbow_hint, Rewards.RAINBOW_SCARF) { it.count(Counter.RAINBOW_WISHES) >= 1 },
        LittleFirst("first_full_moon", Res.string.first_full_moon, Res.string.first_full_moon_hint) { it.count(Counter.FULL_MOONS) >= 1 },
        LittleFirst("first_snowman", Res.string.first_snowman, Res.string.first_snowman_hint, Rewards.SNOWMAN_BEANIE) { it.count(Counter.SNOWMEN) >= 1 },
        LittleFirst("first_star", Res.string.first_star, Res.string.first_star_hint) { it.seenSet(Seen.CONSTELLATIONS).isNotEmpty() },
        LittleFirst("all_stars", Res.string.all_stars, Res.string.all_stars_hint, Rewards.STAR_HOODIE) { it.seenSet(Seen.CONSTELLATIONS).size >= com.example.games.Constellations.ALL.size },
        LittleFirst("every_scene", Res.string.every_scene, Res.string.every_scene_hint) { it.seenSet(Seen.SCENES).size >= SceneType.values().size },
        LittleFirst("four_seasons", Res.string.four_seasons, Res.string.four_seasons_hint) { it.seenSet(Seen.SEASONS).size >= 4 },
        LittleFirst("every_weather", Res.string.every_weather, Res.string.every_weather_hint) { it.seenSet(Seen.WEATHERS).size >= 5 },
        LittleFirst("moments_10", Res.string.moments_10, Res.string.moments_10_hint) { it.count(Counter.TINY_MOMENTS) >= 10 },
        LittleFirst("moments_50", Res.string.moments_50, Res.string.moments_50_hint) { it.count(Counter.TINY_MOMENTS) >= 50 },
        LittleFirst("first_love_note", Res.string.first_love_note, Res.string.first_love_note_hint) { it.count(Counter.LOVE_NOTES) >= 1 },
        LittleFirst("first_dream", Res.string.first_dream, Res.string.first_dream_hint) { it.count(Counter.DREAMS) >= 1 },
        LittleFirst("first_discovery", Res.string.first_discovery, Res.string.first_discovery_hint) { it.count(Counter.DISCOVERIES) >= 1 },
        LittleFirst("all_discoveries", Res.string.all_discoveries, Res.string.all_discoveries_hint) { s -> DISCOVERY_KINDS.all { (s.keepsakes["discovery:$it"] ?: 0) > 0 } },
        LittleFirst("catches_100", Res.string.catches_100, Res.string.catches_100_hint) { it.count(Counter.CATCHES) >= 100 },
        LittleFirst("days_7", Res.string.days_7, Res.string.days_7_hint) { it.count(Counter.DAYS_TOGETHER) >= 7 },
        LittleFirst("days_30", Res.string.days_30, Res.string.days_30_hint) { it.count(Counter.DAYS_TOGETHER) >= 30 },
        LittleFirst("days_100", Res.string.days_100, Res.string.days_100_hint, Rewards.STARRY_ROOM) { it.count(Counter.DAYS_TOGETHER) >= 100 },
        LittleFirst("days_365", Res.string.days_365, Res.string.days_365_hint) { it.count(Counter.DAYS_TOGETHER) >= 365 },
        LittleFirst("first_catch_game", Res.string.first_catch_game, Res.string.first_catch_game_hint) { it.count("games_${Game.CATCH}") >= 1 },
        LittleFirst("catch_30", Res.string.catch_30, Res.string.catch_30_hint) { (it.best[Game.CATCH] ?: 0) >= com.example.games.CatchGame.GOOD_SCORE },
        LittleFirst("mochi_friendly", Res.string.mochi_friendly, Res.string.mochi_friendly_hint) { MochiFondness.level(it.count(Counter.MOCHI_FONDNESS)) >= 1 },
        LittleFirst("mochi_cuddly", Res.string.mochi_cuddly, Res.string.mochi_cuddly_hint) { MochiFondness.level(it.count(Counter.MOCHI_FONDNESS)) >= 2 },
        LittleFirst("mochi_best_friend", Res.string.mochi_best_friend, Res.string.mochi_best_friend_hint, Rewards.MOCHI_HEADBAND) { MochiFondness.level(it.count(Counter.MOCHI_FONDNESS)) >= 3 },
        LittleFirst("first_gift", Res.string.first_gift, Res.string.first_gift_hint) { it.count(Counter.GIFTS) >= 1 },
        LittleFirst("first_dish", Res.string.first_dish, Res.string.first_dish_hint) { it.count(Counter.DISHES) >= 1 },
        LittleFirst("all_recipes", Res.string.all_recipes, Res.string.all_recipes_hint, Rewards.CHEF_APRON) { s -> com.example.games.Recipes.STARTERS.all { it.id in s.seenSet(Seen.RECIPES) } },
        LittleFirst("first_fish", Res.string.first_fish, Res.string.first_fish_hint, Rewards.FISHER_HAT) { it.count(Counter.FISH) >= 1 },
        LittleFirst("golden_fish", Res.string.golden_fish, Res.string.golden_fish_hint) { it.count(Counter.GOLDEN_FISH) >= 1 },
        LittleFirst("first_bloom", Res.string.first_bloom, Res.string.first_bloom_hint) { it.count(Counter.BLOOMS_PICKED) >= 1 },
        LittleFirst("first_bouquet", Res.string.first_bouquet, Res.string.first_bouquet_hint, Rewards.FLOWER_CROWN) { it.count(Counter.BOUQUETS) >= 1 },
        LittleFirst("first_request", Res.string.first_request, Res.string.first_request_hint) { it.count(Counter.REQUESTS) >= 1 },
        LittleFirst("all_requests", Res.string.all_requests, Res.string.all_requests_hint) { it.seenSet(Seen.REQUEST_KINDS).size >= REQUEST_KIND_COUNT },
        LittleFirst("birthday_surprise", Res.string.first_birthday_surprise, Res.string.first_birthday_surprise_hint, Rewards.PARTY_HAT) { it.count(Counter.BIRTHDAYS) >= 1 },
        LittleFirst("first_harvest", Res.string.first_harvest, Res.string.first_harvest_hint) { it.count(Counter.HARVESTS) >= 1 },
        LittleFirst("first_garden_dish", Res.string.first_garden_dish, Res.string.first_garden_dish_hint) { it.count(Counter.GARDEN_DISHES) >= 1 }
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
