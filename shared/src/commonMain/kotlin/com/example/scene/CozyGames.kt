package com.example.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.CoupleDates
import com.example.engine.CharacterEmotion
import com.example.engine.EmoteType
import com.example.engine.GameText
import com.example.games.CookingGame
import com.example.games.FishingCatch
import com.example.games.FishingGame
import com.example.games.GardenPlots
import com.example.games.Ingredient
import com.example.games.PlotStage
import com.example.games.Recipe
import com.example.games.Recipes
import com.example.progress.ProgressEvent
import com.example.resources.*
import org.jetbrains.compose.resources.StringResource
import kotlin.random.Random

/**
 * Cooking, fishing and garden care inside the world (plan 07, C3-C5). The rules live in
 * [com.example.games]; this runs them in their scenes, reacts with the couple, sounds and sparkles,
 * and reports what happened through the engine's onProgress. Games only start when the player
 * starts them (the garden is always there, but only grows when it's looked after).
 */
class CozyGames(private val engine: SceneEngine) {

    val cooking = CookingGame()
    val fishing = FishingGame()

    // Compose state for the screen's recipe card and fishing counter.
    var cookingActive by mutableStateOf(false)
        private set
    /** The ingredients on the card: the recipe's, plus a couple of others, shuffled. */
    var cookingChoices by mutableStateOf<List<Ingredient>>(emptyList())
        private set
    /** Bumped on every change to the cooking game, so the card redraws. */
    var cookingTick by mutableIntStateOf(0)
        private set
    var fishingActive by mutableStateOf(false)
        private set
    var fishingPhase by mutableStateOf(FishingGame.Phase.IDLE)
        private set

    /** What the progress knows, kept up to date by the screen. */
    var garden: GardenPlots = GardenPlots()
    var cookedRecipes: Set<String> = emptySet()
    var hasCaughtFish = false

    private var steamTimer = 0f
    private var lastRainDay = -1L
    private var tooSoonHold = 0f

    private fun today() = CoupleDates.today().toEpochDays().toLong()
    private fun report(event: ProgressEvent) = engine.onProgress?.invoke(event)

    val canCook: Boolean
        get() = engine.currentScene == SceneType.COOKING && !engine.isDreamMode

    val canFish: Boolean
        get() = engine.currentScene == SceneType.SEASIDE_PIER && !engine.isDreamMode

    // ── Cooking ──

    fun startCooking(random: Random = Random.Default) {
        if (!canCook || cookingActive) return
        val recipe = Recipes.suggest(cookedRecipes, random)
        val others = Ingredient.entries.filter { it !in recipe.ingredients }.shuffled(random)
        cookingChoices = (recipe.ingredients.distinct() + others.take(6 - recipe.ingredients.distinct().size)).shuffled(random)
        cooking.start(recipe)
        cookingActive = true
        cookingTick++
        engine.girl.emotion = CharacterEmotion.HAPPY
        engine.boy.emotion = CharacterEmotion.HAPPY
        engine.audio.playPaperFlip()
        engine.showMessage(GameText.get(Res.string.cooking_hint), duration = 3.5f)
    }

    fun tapIngredient(ingredient: Ingredient) {
        val w = engine.lastWorldW
        val h = engine.lastWorldH
        when (cooking.tap(ingredient)) {
            CookingGame.TapResult.ADDED -> {
                engine.audio.playBubblePop()
                engine.particles.spawnSparkles(w * POT_X, h * POT_Y, 5)
            }
            CookingGame.TapResult.COOKING -> {
                engine.audio.playCookingBubbles()
                repeat(6) { engine.particles.spawnSteam(w * POT_X, h * POT_Y) }
            }
            CookingGame.TapResult.WRONG -> {
                engine.audio.playSoftThud()
                cooking.next?.let { engine.showMessage(GameText.get(Res.string.cooking_wrong, GameText.get(ingredientName(it))), duration = 2.5f) }
            }
            CookingGame.TapResult.IGNORED -> Unit
        }
        cookingTick++
    }

    fun stopCooking() {
        cooking.stop()
        cookingActive = false
        cookingTick++
    }

    /** The dish on the kitchen table while it's being shown. */
    val servedDish: Recipe?
        get() = if (cooking.phase == CookingGame.Phase.SERVED) cooking.recipe else null

    private fun updateCooking(dt: Float, cw: Float, ch: Float) {
        if (!cookingActive) return
        if (engine.currentScene != SceneType.COOKING) {
            stopCooking()
            return
        }
        val wiggling = cooking.wiggling
        val served = cooking.update(dt)
        if (cooking.phase == CookingGame.Phase.COOKING) {
            steamTimer -= dt
            if (steamTimer <= 0f) {
                engine.particles.spawnSteam(cw * POT_X, ch * POT_Y)
                steamTimer = 0.25f
            }
        }
        if (served != null) {
            engine.audio.playHeartChime()
            engine.particles.spawnHeart(cw * 0.5f, ch * 0.78f)
            engine.particles.spawnSparkles(cw * 0.5f, ch * 0.80f, 10, Color(0xFFFFD166))
            cheer()
            engine.showMessage(GameText.get(Res.string.cooking_served, GameText.get(recipeName(served.id))), duration = 4f)
            cookedRecipes = cookedRecipes + served.id
            report(ProgressEvent.DishCooked(served.id))
            cookingTick++
        }
        if (cooking.phase == CookingGame.Phase.IDLE) {
            cookingActive = false
            cookingTick++
        } else if (wiggling != cooking.wiggling) {
            cookingTick++
        }
    }

    // ── Fishing ──

    fun startFishing() {
        if (!canFish || fishingActive) return
        fishing.cast(firstEver = !hasCaughtFish)
        fishingActive = true
        fishingPhase = fishing.phase
        engine.audio.playReelClick()
        engine.showMessage(GameText.get(Res.string.fishing_hint), duration = 3.5f)
    }

    fun tapFishing() {
        when (fishing.tap()) {
            FishingGame.TapResult.HOOKED -> {
                engine.audio.playReelClick()
                engine.particles.spawnSparkles(engine.lastWorldW * BOBBER_X, engine.lastWorldH * BOBBER_Y, 8, Color(0xFFBDE0FE))
            }
            FishingGame.TapResult.TOO_SOON -> if (tooSoonHold <= 0f) {
                engine.showMessage(GameText.get(Res.string.fishing_too_soon), duration = 1.6f)
                tooSoonHold = 2f
            }
            FishingGame.TapResult.IGNORED -> Unit
        }
        fishingPhase = fishing.phase
    }

    fun stopFishing() {
        fishing.stop()
        fishingActive = false
        fishingPhase = fishing.phase
    }

    private fun updateFishing(dt: Float, cw: Float, ch: Float) {
        tooSoonHold = (tooSoonHold - dt).coerceAtLeast(0f)
        if (!fishingActive) return
        if (engine.currentScene != SceneType.SEASIDE_PIER) {
            stopFishing()
            return
        }
        val before = fishing.phase
        val landed = fishing.update(dt)
        if (before == FishingGame.Phase.WAITING && fishing.phase == FishingGame.Phase.BITE) {
            engine.audio.playWaterDrip()
            engine.particles.spawnSparkles(cw * BOBBER_X, ch * BOBBER_Y, 4, Color(0xFFBDE0FE))
        }
        if (before == FishingGame.Phase.BITE && fishing.phase == FishingGame.Phase.WAITING) {
            engine.showMessage(GameText.get(Res.string.fishing_got_away), duration = 2.2f)
        }
        if (landed != null) {
            engine.audio.playHeartChime()
            if (landed == FishingCatch.GOLDEN_FISH) engine.audio.playStarArpeggio()
            engine.particles.spawnSparkles(cw * BOBBER_X, ch * BOBBER_Y, 10, if (landed == FishingCatch.GOLDEN_FISH) Color(0xFFFFD166) else null)
            cheer()
            engine.showMessage(GameText.get(Res.string.fishing_caught, GameText.get(catchName(landed))), duration = 3.5f)
            if (landed.isFish) hasCaughtFish = true
            report(ProgressEvent.FishCaught(landed.name))
        }
        // After a catch is shown, the line goes straight back in until they stop.
        if (fishing.phase == FishingGame.Phase.IDLE) fishing.cast(firstEver = !hasCaughtFish)
        fishingPhase = fishing.phase
    }

    // ── Garden ──

    /** The centre of the base of plot [index] in the meadow. */
    fun plotBase(index: Int, cw: Float, ch: Float) = Offset(cw * PLOT_XS[index], ch * PLOT_Y)

    /** Handles a tap in the meadow; true if it was on a plot. */
    fun onGardenTap(x: Float, y: Float, cw: Float, ch: Float, p: Float, random: Random = Random.Default): Boolean {
        if (engine.currentScene != SceneType.FLOWER || engine.isDreamMode) return false
        val q = p * PLOT_SCALE
        val index = PLOT_XS.indices.firstOrNull { i ->
            val base = plotBase(i, cw, ch)
            kotlin.math.abs(x - base.x) < 6f * q && y > base.y - 11f * q && y < base.y + 3f * q
        } ?: return false
        val plot = garden.plots[index]
        val day = today()
        val base = plotBase(index, cw, ch)
        when {
            plot.stage == PlotStage.EMPTY -> {
                val seed = GardenPlots.SEEDS.random(random)
                garden = garden.plant(index, seed)
                report(ProgressEvent.GardenPlanted(index, seed))
                engine.audio.playLeafRustle()
                engine.particles.spawnGrassPuff(base.x, base.y, 4)
                val gardener = if (random.nextBoolean()) engine.boy.name else engine.girl.name
                engine.showMessage(GameText.get(Res.string.garden_planted, gardener, GameText.get(flowerName(seed))), duration = 3f)
            }
            plot.stage == PlotStage.BLOOM -> {
                val held = garden.stems.size + 1
                garden = garden.pick(index).first
                report(ProgressEvent.FlowerPicked(index))
                engine.audio.playHeartChime()
                engine.particles.spawnPetals(base.x, base.y - 6f * p, 5)
                if (held >= GardenPlots.BOUQUET_SIZE) {
                    cheer()
                    engine.showMessage(GameText.get(Res.string.garden_bouquet), duration = 3.5f)
                } else {
                    engine.showMessage(GameText.get(Res.string.garden_picked, GardenPlots.BOUQUET_SIZE - held), duration = 2.5f)
                }
            }
            garden.needsWater(index, day) -> {
                garden = garden.water(index, day)
                report(ProgressEvent.GardenWatered(index, day))
                engine.audio.playWaterDrip()
                engine.particles.spawnSparkles(base.x, base.y - 4f * p, 6, Color(0xFF9AD1F5))
                engine.showMessage(GameText.get(Res.string.garden_watered), duration = 2.5f)
            }
            else -> engine.showMessage(GameText.get(Res.string.garden_already_watered), duration = 2f)
        }
        return true
    }

    private fun updateGarden() {
        // Rain waters the garden, once a day, wherever the two of them are.
        if (engine.weather != WeatherType.RAIN) return
        val day = today()
        if (day == lastRainDay) return
        lastRainDay = day
        if (garden.plots.indices.none { garden.needsWater(it, day) }) return
        garden = garden.rain(day)
        report(ProgressEvent.GardenRained(day))
        if (engine.currentScene == SceneType.FLOWER) engine.showMessage(GameText.get(Res.string.garden_rained), duration = 3f)
    }

    // ── Shared ──

    fun update(dt: Float, cw: Float, ch: Float) {
        updateCooking(dt, cw, ch)
        updateFishing(dt, cw, ch)
        updateGarden()
    }

    /** Puts everything away (a new scene, a dream). */
    fun stopAll() {
        if (cookingActive) stopCooking()
        if (fishingActive) stopFishing()
    }

    private fun cheer() {
        engine.boy.emote = EmoteType.HEART
        engine.girl.emote = EmoteType.HEART
        engine.boy.emoteTimer = 2.4f
        engine.girl.emoteTimer = 2.4f
    }

    companion object {
        /** The stove pot in the kitchen, as shares of the stage. */
        const val POT_X = 0.58f
        const val POT_Y = 0.59f
        /** Where the couple's bobber floats at the pier (Grandpa Bao fishes further right). */
        const val BOBBER_X = 0.23f
        const val BOBBER_Y = 0.55f
        /** The three garden plots along the front of the meadow. */
        val PLOT_XS = listOf(0.22f, 0.40f, 0.58f)
        const val PLOT_Y = 0.80f
        /** Plots are drawn at this many game pixels per sprite pixel. */
        const val PLOT_SCALE = 2f

        fun recipeName(id: String): StringResource = when (id) {
            "pancakes" -> Res.string.recipe_pancakes
            "soup" -> Res.string.recipe_soup
            "dumplings" -> Res.string.recipe_dumplings
            "cookies" -> Res.string.recipe_cookies
            else -> Res.string.recipe_tea
        }

        fun ingredientName(i: Ingredient): StringResource = when (i) {
            Ingredient.FLOUR -> Res.string.ingredient_flour
            Ingredient.EGG -> Res.string.ingredient_egg
            Ingredient.MILK -> Res.string.ingredient_milk
            Ingredient.BUTTER -> Res.string.ingredient_butter
            Ingredient.SUGAR -> Res.string.ingredient_sugar
            Ingredient.WATER -> Res.string.ingredient_water
            Ingredient.CARROT -> Res.string.ingredient_carrot
            Ingredient.POTATO -> Res.string.ingredient_potato
            Ingredient.ONION -> Res.string.ingredient_onion
            Ingredient.CABBAGE -> Res.string.ingredient_cabbage
            Ingredient.CHOCOLATE -> Res.string.ingredient_chocolate
            Ingredient.TEA_LEAVES -> Res.string.ingredient_tea_leaves
            Ingredient.HONEY -> Res.string.ingredient_honey
        }

        fun catchName(c: FishingCatch): StringResource = when (c) {
            FishingCatch.MINNOW -> Res.string.catch_minnow
            FishingCatch.CARP -> Res.string.catch_carp
            FishingCatch.SEASHELL -> Res.string.catch_seashell
            FishingCatch.OLD_BOOT -> Res.string.catch_old_boot
            FishingCatch.BOTTLE -> Res.string.catch_bottle
            FishingCatch.GOLDEN_FISH -> Res.string.catch_golden_fish
        }

        fun flowerName(id: String): StringResource = when (id) {
            "sunflower" -> Res.string.flower_sunflower
            "tulip" -> Res.string.flower_tulip
            "daisy" -> Res.string.flower_daisy
            "lavender" -> Res.string.flower_lavender
            else -> Res.string.flower_forget_me_not
        }
    }
}
