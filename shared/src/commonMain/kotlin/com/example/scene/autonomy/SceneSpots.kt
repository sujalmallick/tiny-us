package com.example.scene.autonomy

import com.example.engine.CharacterPose
import com.example.scene.SceneType

/** What a character does once they reach an interaction spot. The engine plays the effect. */
enum class SpotAction {
    SMELL_FLOWERS, PICK_FLOWER, LISTEN_CHIMES, LOOK_UP_TREE, SIT_GRASS,
    STIR_POT, RINSE_DISHES, PEEK_OVEN, SIT_TABLE,
    WATER_PLANT, PEEK_BOX, SIT_POUF, LIGHT_CANDLE,
    USE_TELESCOPE, ADMIRE_LANTERN, SMELL_LAVENDER, POKE_MUSHROOMS,
    SNIFF_STEAMER, READ_CHALKBOARD, CHECK_CRATE, FILL_SAUCER,
    LOOK_WINDOW, BROWSE_BOOKS, ADMIRE_FAIRY_LIGHTS,
    FOG_WINDOW, PET_PUP, ORDER_COFFEE,
    MIST_PLANTS, LOOK_SKYLIGHT,
    WARM_HANDS, STRUM_GUITAR, TEND_LANTERN,
    SPOT_DOLPHINS, LOOK_SEA, PEEK_BUCKET
}

/**
 * A place a character can walk to and do something. Positions are normalized world
 * coordinates of the character's feet; [faceLeft] is which way they look while there.
 * Only props that are safe for a character to use on their own are listed: nothing that
 * opens a dialog, changes the scene, or starts real music.
 */
class SceneSpot(
    val id: Int,
    val x: Float,
    val y: Float,
    val faceLeft: Boolean,
    val action: SpotAction,
    val pose: CharacterPose = CharacterPose.IDLE,
    val dwellSeconds: Float = 3.2f,
    /** Only indoors on a rainy day, or only outdoors, etc. is decided by the brain; this marks windows. */
    val isWindow: Boolean = false
)

object SceneSpots {
    private val NONE = emptyList<SceneSpot>()

    private val byScene: Map<SceneType, List<SceneSpot>> = mapOf(
        SceneType.FLOWER to listOf(
            SceneSpot(1, 0.30f, 0.73f, true, SpotAction.SMELL_FLOWERS),
            SceneSpot(2, 0.70f, 0.74f, false, SpotAction.PICK_FLOWER, CharacterPose.GIVE_FLOWER),
            SceneSpot(3, 0.26f, 0.70f, true, SpotAction.LISTEN_CHIMES)
        ),
        SceneType.UNDER_TREE to listOf(
            SceneSpot(1, 0.50f, 0.71f, false, SpotAction.LOOK_UP_TREE),
            SceneSpot(2, 0.28f, 0.75f, false, SpotAction.SIT_GRASS, CharacterPose.SIT, 5.5f),
            SceneSpot(3, 0.72f, 0.75f, true, SpotAction.SIT_GRASS, CharacterPose.SIT, 5.5f)
        ),
        SceneType.COOKING to listOf(
            SceneSpot(1, 0.58f, 0.70f, false, SpotAction.STIR_POT, CharacterPose.COOK, 4f),
            SceneSpot(2, 0.32f, 0.70f, true, SpotAction.RINSE_DISHES, CharacterPose.COOK),
            SceneSpot(3, 0.66f, 0.71f, false, SpotAction.PEEK_OVEN),
            SceneSpot(4, com.example.scene.CozyGames.TABLE_BOY_X, com.example.scene.CozyGames.TABLE_SEAT_Y, false, SpotAction.SIT_TABLE, CharacterPose.SIT, 5f)
        ),
        SceneType.SLEEP to listOf(
            SceneSpot(1, 0.20f, 0.70f, true, SpotAction.WATER_PLANT),
            SceneSpot(2, 0.27f, 0.90f, true, SpotAction.PEEK_BOX),
            SceneSpot(3, 0.70f, 0.778f, false, SpotAction.SIT_POUF, CharacterPose.SIT, 5f),
            SceneSpot(4, 0.54f, 0.72f, false, SpotAction.LIGHT_CANDLE)
        ),
        SceneType.WALK to listOf(
            SceneSpot(1, 0.36f, 0.70f, false, SpotAction.USE_TELESCOPE, dwellSeconds = 3.8f),
            SceneSpot(2, 0.22f, 0.74f, true, SpotAction.ADMIRE_LANTERN),
            SceneSpot(3, 0.78f, 0.75f, false, SpotAction.SMELL_LAVENDER),
            SceneSpot(4, 0.60f, 0.78f, false, SpotAction.POKE_MUSHROOMS)
        ),
        SceneType.LOOKING to listOf(
            SceneSpot(1, 0.28f, 0.70f, true, SpotAction.USE_TELESCOPE, dwellSeconds = 3.8f),
            SceneSpot(2, 0.72f, 0.75f, false, SpotAction.SIT_GRASS, CharacterPose.SIT, 5.5f)
        ),
        SceneType.MOMO_STALL to listOf(
            SceneSpot(1, 0.40f, 0.71f, false, SpotAction.SNIFF_STEAMER),
            SceneSpot(2, 0.20f, 0.73f, false, SpotAction.READ_CHALKBOARD),
            SceneSpot(3, 0.63f, 0.73f, false, SpotAction.CHECK_CRATE),
            SceneSpot(4, 0.47f, 0.77f, true, SpotAction.FILL_SAUCER)
        ),
        SceneType.COZY_LOFT to listOf(
            SceneSpot(1, 0.74f, 0.58f, false, SpotAction.LOOK_WINDOW, isWindow = true, dwellSeconds = 4f),
            SceneSpot(2, 0.42f, 0.58f, true, SpotAction.BROWSE_BOOKS, dwellSeconds = 4f),
            SceneSpot(3, 0.62f, 0.60f, false, SpotAction.ADMIRE_FAIRY_LIGHTS)
        ),
        SceneType.RAINY_CAFE to listOf(
            SceneSpot(1, 0.62f, 0.71f, false, SpotAction.FOG_WINDOW, isWindow = true),
            SceneSpot(2, 0.76f, 0.74f, false, SpotAction.PET_PUP, CharacterPose.HEAD_PAT),
            SceneSpot(3, 0.26f, 0.71f, true, SpotAction.ORDER_COFFEE)
        ),
        SceneType.SUNROOM to listOf(
            SceneSpot(1, 0.72f, 0.70f, false, SpotAction.MIST_PLANTS),
            SceneSpot(2, 0.26f, 0.72f, true, SpotAction.WATER_PLANT),
            SceneSpot(3, 0.50f, 0.70f, false, SpotAction.LOOK_SKYLIGHT, isWindow = true)
        ),
        SceneType.CAMPFIRE to listOf(
            SceneSpot(1, 0.39f, 0.70f, false, SpotAction.WARM_HANDS, CharacterPose.SIT, 4.5f),
            SceneSpot(2, 0.57f, 0.70f, true, SpotAction.WARM_HANDS, CharacterPose.SIT, 4.5f),
            SceneSpot(3, 0.64f, 0.70f, false, SpotAction.STRUM_GUITAR, CharacterPose.SIT, 4.5f),
            SceneSpot(4, 0.24f, 0.70f, true, SpotAction.TEND_LANTERN)
        ),
        SceneType.SEASIDE_PIER to listOf(
            SceneSpot(1, 0.31f, 0.70f, true, SpotAction.SPOT_DOLPHINS, dwellSeconds = 3.8f),
            SceneSpot(2, 0.50f, 0.69f, false, SpotAction.LOOK_SEA, isWindow = true),
            SceneSpot(3, 0.67f, 0.78f, false, SpotAction.PEEK_BUCKET)
        )
    )

    fun forScene(scene: SceneType): List<SceneSpot> = byScene[scene] ?: NONE
}
