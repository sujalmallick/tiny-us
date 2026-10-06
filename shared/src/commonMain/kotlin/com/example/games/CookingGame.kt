package com.example.games

import kotlin.random.Random

/**
 * Something on the kitchen counter or in the fridge (always there), or from the garden (only what's
 * in the pantry: see [isProduce]).
 */
enum class Ingredient {
    FLOUR, EGG, MILK, BUTTER, SUGAR, WATER, CARROT, POTATO, ONION, CABBAGE, CHOCOLATE, TEA_LEAVES, HONEY,
    // Grown in the garden (plan 09, E3).
    STRAWBERRY, PEAS, TOMATO, BASIL, PUMPKIN, APPLE, MINT;

    /** Grown, not bought: it comes from the pantry and a dish uses it up. */
    val isProduce: Boolean get() = ordinal >= STRAWBERRY.ordinal
}

/** A recipe card: [ingredients] go in this order. [id] names the dish (its text and its art). */
class Recipe(val id: String, val ingredients: List<Ingredient>) {
    /** What it needs from the pantry (nothing, for the five starting recipes). */
    val pantry: List<Ingredient> get() = ingredients.filter { it.isProduce }

    /** Whether [pantry] (ingredient to how many) has what this recipe needs. */
    fun canCook(pantry: Map<Ingredient, Int>): Boolean =
        this.pantry.groupingBy { it }.eachCount().all { (i, n) -> (pantry[i] ?: 0) >= n }
}

object Recipes {
    /** The five recipes that are always open. */
    val STARTERS: List<Recipe> = listOf(
        Recipe("pancakes", listOf(Ingredient.FLOUR, Ingredient.EGG, Ingredient.MILK, Ingredient.BUTTER)),
        Recipe("soup", listOf(Ingredient.WATER, Ingredient.CARROT, Ingredient.POTATO, Ingredient.ONION)),
        Recipe("dumplings", listOf(Ingredient.FLOUR, Ingredient.WATER, Ingredient.CABBAGE)),
        Recipe("cookies", listOf(Ingredient.BUTTER, Ingredient.SUGAR, Ingredient.EGG, Ingredient.FLOUR, Ingredient.CHOCOLATE)),
        Recipe("tea", listOf(Ingredient.WATER, Ingredient.TEA_LEAVES, Ingredient.HONEY))
    )

    /** Recipes that need something from the garden (plan 09, E3). */
    val GARDEN: List<Recipe> = listOf(
        Recipe("tomato_soup", listOf(Ingredient.WATER, Ingredient.TOMATO, Ingredient.BASIL, Ingredient.ONION)),
        Recipe("strawberry_pancakes", listOf(Ingredient.FLOUR, Ingredient.EGG, Ingredient.MILK, Ingredient.STRAWBERRY)),
        Recipe("pumpkin_pie", listOf(Ingredient.FLOUR, Ingredient.BUTTER, Ingredient.PUMPKIN, Ingredient.SUGAR)),
        Recipe("herb_tea", listOf(Ingredient.WATER, Ingredient.MINT, Ingredient.HONEY)),
        Recipe("apple_crumble", listOf(Ingredient.APPLE, Ingredient.FLOUR, Ingredient.BUTTER, Ingredient.SUGAR)),
        Recipe("pea_soup", listOf(Ingredient.WATER, Ingredient.PEAS, Ingredient.ONION))
    )

    val ALL: List<Recipe> = STARTERS + GARDEN

    fun byId(id: String): Recipe? = ALL.firstOrNull { it.id == id }

    /** What can be cooked now: the starters, and the garden recipes the pantry has enough for. */
    fun available(pantry: Map<Ingredient, Int>): List<Recipe> = ALL.filter { it.canCook(pantry) }

    /** A recipe to suggest from what's [available]: one not cooked yet if there is one, otherwise any. */
    fun suggest(cooked: Set<String>, random: Random = Random.Default, pantry: Map<Ingredient, Int> = emptyMap()): Recipe {
        val open = available(pantry)
        val fresh = open.filter { it.id !in cooked }
        return (fresh.ifEmpty { open }).random(random)
    }
}

/**
 * Cooking (plan 07, C3): a recipe card shows its ingredients; tap them in order on the counter and
 * in the fridge. A wrong one just wiggles; nothing is lost and nothing burns. When the last one is
 * in, the pot simmers for a moment and the dish is served. Plain Kotlin: the engine feeds it taps
 * and time, and records the dish when [update] says it's served.
 */
class CookingGame {

    enum class Phase { IDLE, GATHERING, COOKING, SERVED }

    enum class TapResult {
        /** The right ingredient; more to go. */
        ADDED,
        /** The last ingredient: the pot starts to simmer. */
        COOKING,
        /** Not the next one: it wiggles, and the card shows what's next. */
        WRONG,
        /** No recipe on the go (or it's already in the pot). */
        IGNORED
    }

    var phase = Phase.IDLE
        private set
    var recipe: Recipe? = null
        private set
    /** How many of the recipe's ingredients are in. */
    var added = 0
        private set
    /** The ingredient wiggling after a wrong tap, while [wiggleTimer] runs. */
    var wiggling: Ingredient? = null
        private set
    var wiggleTimer = 0f
        private set
    /** Seconds left simmering (in COOKING), or showing the dish (in SERVED). */
    var timer = 0f
        private set

    val next: Ingredient?
        get() = if (phase == Phase.GATHERING) recipe?.ingredients?.getOrNull(added) else null

    fun start(recipe: Recipe) {
        this.recipe = recipe
        phase = Phase.GATHERING
        added = 0
        wiggling = null
        wiggleTimer = 0f
        timer = 0f
    }

    fun tap(ingredient: Ingredient): TapResult {
        val r = recipe
        if (phase != Phase.GATHERING || r == null) return TapResult.IGNORED
        if (ingredient != r.ingredients[added]) {
            wiggling = ingredient
            wiggleTimer = WIGGLE_SECONDS
            return TapResult.WRONG
        }
        added++
        wiggling = null
        wiggleTimer = 0f
        if (added < r.ingredients.size) return TapResult.ADDED
        phase = Phase.COOKING
        timer = SIMMER_SECONDS
        return TapResult.COOKING
    }

    /** Moves time on by [dt] seconds. Returns the dish on the frame it's served, otherwise null. */
    fun update(dt: Float): Recipe? {
        if (wiggleTimer > 0f) {
            wiggleTimer = (wiggleTimer - dt).coerceAtLeast(0f)
            if (wiggleTimer == 0f) wiggling = null
        }
        when (phase) {
            Phase.COOKING -> {
                timer -= dt
                if (timer <= 0f) {
                    phase = Phase.SERVED
                    timer = SERVED_SECONDS
                    return recipe
                }
            }
            Phase.SERVED -> {
                timer -= dt
                if (timer <= 0f) stop()
            }
            else -> Unit
        }
        return null
    }

    /** Puts the card away (also fine halfway: nothing is lost). */
    fun stop() {
        phase = Phase.IDLE
        recipe = null
        added = 0
        wiggling = null
        wiggleTimer = 0f
        timer = 0f
    }

    companion object {
        const val WIGGLE_SECONDS = 0.6f
        const val SIMMER_SECONDS = 2.5f
        /** How long the finished dish shows on the table before the card goes away. */
        const val SERVED_SECONDS = 4f
        /** Different dishes cooked that earn a little first (and the chef's apron): the five starters. */
        const val GOOD_DISHES = 5
    }
}
