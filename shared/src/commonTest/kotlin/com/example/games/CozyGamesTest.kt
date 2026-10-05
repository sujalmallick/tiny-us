package com.example.games

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The rules of cooking, fishing and garden care (plan 07, C3-C5): nothing to lose, only to make. */
class CozyGamesTest {

    // --- Cooking ---

    @Test
    fun everyRecipeIsShortAndNamedOnce() {
        assertEquals(Recipes.ALL.size, Recipes.ALL.map { it.id }.toSet().size)
        for (r in Recipes.ALL) assertTrue(r.ingredients.size in 3..5, r.id)
        assertTrue(Recipes.ALL.size >= CookingGame.GOOD_DISHES)
    }

    @Test
    fun theRightOrderCooksAndServesTheDish() {
        val game = CookingGame()
        val soup = Recipes.byId("soup")!!
        game.start(soup)
        soup.ingredients.dropLast(1).forEach { assertEquals(CookingGame.TapResult.ADDED, game.tap(it)) }
        assertEquals(CookingGame.TapResult.COOKING, game.tap(soup.ingredients.last()))
        assertEquals(CookingGame.Phase.COOKING, game.phase)
        // A tap while it simmers changes nothing.
        assertEquals(CookingGame.TapResult.IGNORED, game.tap(Ingredient.SUGAR))

        var served: Recipe? = null
        repeat(100) { game.update(0.05f)?.let { served = it } }
        assertEquals(soup, served)
        assertEquals(CookingGame.Phase.SERVED, game.phase)
        // The dish shows for a while, then the card goes away.
        repeat(100) { game.update(0.05f) }
        assertEquals(CookingGame.Phase.IDLE, game.phase)
    }

    @Test
    fun aWrongIngredientOnlyWiggles() {
        val game = CookingGame()
        game.start(Recipes.byId("tea")!!)
        assertEquals(CookingGame.TapResult.WRONG, game.tap(Ingredient.HONEY))
        assertEquals(Ingredient.HONEY, game.wiggling)
        assertEquals(0, game.added)
        assertEquals(Ingredient.WATER, game.next)
        repeat(20) { game.update(0.05f) }
        assertNull(game.wiggling)
        // Still on the same step, and the right one carries on.
        assertEquals(CookingGame.TapResult.ADDED, game.tap(Ingredient.WATER))
        assertEquals(Ingredient.TEA_LEAVES, game.next)
    }

    @Test
    fun suggestionsPreferSomethingNew() {
        val allButCookies = Recipes.ALL.map { it.id }.toSet() - "cookies"
        repeat(20) { assertEquals("cookies", Recipes.suggest(allButCookies, Random(it)).id) }
        // Once everything is cooked, anything goes.
        val everything = Recipes.ALL.map { it.id }.toSet()
        assertTrue(Recipes.suggest(everything, Random(1)).id in everything)
    }

    // --- Fishing ---

    private fun FishingGame.runUntil(phase: FishingGame.Phase, limitSeconds: Float = 30f): FishingCatch? {
        var landed: FishingCatch? = null
        var t = 0f
        while (this.phase != phase && t < limitSeconds) {
            update(0.05f)?.let { landed = it }
            t += 0.05f
        }
        assertEquals(phase, this.phase)
        return landed
    }

    @Test
    fun aTapDuringTheBiteReelsSomethingIn() {
        val game = FishingGame(Random(7))
        game.cast()
        assertEquals(FishingGame.TapResult.TOO_SOON, game.tap())
        assertEquals(FishingGame.Phase.WAITING, game.phase)
        game.runUntil(FishingGame.Phase.BITE)
        assertEquals(1f, game.dip)
        assertEquals(FishingGame.TapResult.HOOKED, game.tap())
        val landed = game.runUntil(FishingGame.Phase.LANDED)
        assertNotNull(landed)
        assertEquals(landed, game.caught)
        game.runUntil(FishingGame.Phase.IDLE)
    }

    @Test
    fun aMissedBiteJustComesBack() {
        val game = FishingGame(Random(3))
        game.cast()
        repeat(6) {
            game.runUntil(FishingGame.Phase.BITE)
            // Let it swim off: back to waiting, never less than a second of quiet.
            repeat((FishingGame.BITE_WINDOW_SECONDS / 0.05f).toInt() + 2) { game.update(0.05f) }
            assertEquals(FishingGame.Phase.WAITING, game.phase)
            assertTrue(game.timer >= 0.9f, "wait ${game.timer}")
        }
        assertEquals(6, game.missedBites)
    }

    @Test
    fun theFirstCatchEverIsAFish() {
        repeat(200) { seed ->
            val game = FishingGame(Random(seed))
            game.cast(firstEver = true)
            game.runUntil(FishingGame.Phase.BITE)
            game.tap()
            assertTrue(game.caught!!.isFish, "seed $seed: ${game.caught}")
        }
    }

    @Test
    fun everyKindOfCatchTurnsUp() {
        val seen = mutableSetOf<FishingCatch>()
        repeat(600) { seed ->
            val game = FishingGame(Random(seed))
            game.cast()
            game.runUntil(FishingGame.Phase.BITE)
            game.tap()
            seen += game.caught!!
        }
        assertEquals(FishingCatch.entries.toSet(), seen)
    }

    // --- Garden ---

    @Test
    fun aSeedGrowsOneStageForEachDayItsWatered() {
        var g = GardenPlots().plant(0, "tulip")
        assertEquals(PlotStage.SEED, g.plots[0].stage)
        g = g.water(0, 100)
        assertEquals(PlotStage.SPROUT, g.plots[0].stage)
        // Twice in a day is the same as once.
        assertEquals(g, g.water(0, 100))
        assertFalse(g.needsWater(0, 100))
        // A week away: it just waited.
        g = g.water(0, 107)
        assertEquals(PlotStage.BUD, g.plots[0].stage)
        g = g.water(0, 108)
        assertEquals(PlotStage.BLOOM, g.plots[0].stage)
        // In bloom it needs nothing more.
        assertFalse(g.needsWater(0, 109))
        assertEquals(g, g.water(0, 109))
    }

    @Test
    fun rainWatersEveryGrowingPlot() {
        val g = GardenPlots().plant(0, "daisy").plant(2, "sunflower").water(2, 50).rain(50)
        assertEquals(PlotStage.SPROUT, g.plots[0].stage)
        // Plot 2 already had its water today; the empty one stays empty.
        assertEquals(PlotStage.SPROUT, g.plots[2].stage)
        assertEquals(PlotStage.EMPTY, g.plots[1].stage)
    }

    @Test
    fun plantingOnlyFitsAnEmptyPlotAndARealSeed() {
        val g = GardenPlots().plant(0, "tulip")
        assertEquals(g, g.plant(0, "daisy"))
        assertEquals(g, g.plant(1, "cactus"))
        assertEquals(g, g.plant(9, "daisy"))
    }

    @Test
    fun threePickedFlowersMakeABouquet() {
        fun bloom(g: GardenPlots, i: Int, flower: String) =
            (1..GardenPlots.WATERINGS_TO_BLOOM).fold(g.plant(i, flower)) { acc, d -> acc.water(i, d.toLong()) }

        var g = bloom(bloom(bloom(GardenPlots(), 0, "tulip"), 1, "daisy"), 2, "lavender")
        // A bud can't be picked.
        val budding = GardenPlots().plant(0, "tulip")
        assertNull(budding.pick(0).second)

        val (a, first) = g.pick(0)
        assertNull(first)
        assertEquals(listOf("tulip"), a.stems)
        assertEquals(PlotStage.EMPTY, a.plots[0].stage)
        val (b, _) = a.pick(1)
        val (c, bouquet) = b.pick(2)
        assertEquals(listOf("tulip", "daisy", "lavender"), bouquet)
        assertTrue(c.stems.isEmpty())
        assertTrue(c.plots.all { it.stage == PlotStage.EMPTY })
        g = c
        assertEquals(GardenPlots(), g)
    }
}
