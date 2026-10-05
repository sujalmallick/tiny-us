package com.example.progress

import com.example.engine.RoomTheme

/**
 * Where each reward lives in the wardrobe and the room picker (plan 07, B4). Items not listed here
 * are free, as they always were.
 */
object RewardItems {
    /** The girl's (dress) outfit and the boy's (trouser) outfit that are star hoodies. */
    const val STAR_HOODIE_DRESS_INDEX = 11
    const val STAR_HOODIE_TROUSER_INDEX = 5
    const val RAINBOW_SCARF_INDEX = 4
    const val SNOWMAN_BEANIE_INDEX = 5
    const val MOCHI_HEADBAND_INDEX = 6

    fun forDressOutfit(index: Int): String? = if (index == STAR_HOODIE_DRESS_INDEX) Rewards.STAR_HOODIE else null
    fun forTrouserOutfit(index: Int): String? = if (index == STAR_HOODIE_TROUSER_INDEX) Rewards.STAR_HOODIE else null
    fun forAccessory(index: Int): String? = when (index) {
        RAINBOW_SCARF_INDEX -> Rewards.RAINBOW_SCARF
        SNOWMAN_BEANIE_INDEX -> Rewards.SNOWMAN_BEANIE
        MOCHI_HEADBAND_INDEX -> Rewards.MOCHI_HEADBAND
        else -> null
    }
    fun forTheme(theme: RoomTheme): String? = if (theme == RoomTheme.STARRY_NIGHT) Rewards.STARRY_ROOM else null

    /** The little first that unlocks [reward]. */
    fun earnedBy(reward: String): LittleFirst? = LittleFirsts.ALL.firstOrNull { it.reward == reward }
}
