package com.example.games

import com.example.progress.Counter
import com.example.progress.LittleFirsts
import com.example.progress.PANTRY
import com.example.progress.ProgressEvent
import com.example.progress.ProgressState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Garden to kitchen (plan 09, E): seeds by season, the shared plot, the pots, the pantry and its recipes. */
class GardenKitchenTest {

    @Test
    fun theMeadowTakesSeasonalSeedsAndRestsInWinter() {
        val spring = Seeds.forMeadow("SPRING").map { it.id }
        assertTrue("strawberry" in spring && "peas" in spring && "tulip" in spring)
        assertFalse("pumpkin" in spring)
        assertTrue("pumpkin" in Seeds.forMeadow("AUTUMN").map { it.id })
        assertTrue(Seeds.forMeadow("WINTER").isEmpty())
        // The sunroom pots grow herbs all year.
        assertEquals(setOf("basil", "mint"), Seeds.forPots().map { it.id }.toSet())
    }

    @Test
    fun potsOnlyTakeHerbsAndMintOnlyGrowsInPots() {
        val pot = GardenPlots.PLOTS
        val g = GardenPlots()
        assertEquals(g, g.plant(pot, "tulip"))
        assertEquals(g, g.plant(0, "mint"))
        assertEquals("mint", g.plant(pot, "mint").plots[pot].flower)
        // Rain doesn't reach indoors.
        val planted = g.plant(pot, "mint").plant(0, "tomato")
        val rained = planted.rain(5)
        assertEquals(PlotStage.SEED, rained.plots[pot].stage)
        assertEquals(PlotStage.SPROUT, rained.plots[0].stage)
    }

    @Test
    fun theSharedPlotGrowsOnlyWhenBothHaveWateredThatDay() {
        val shared = 1
        assertEquals(PlotOwner.SHARED, GardenPlots.ownerOf(shared))
        var g = GardenPlots().plant(shared, "tomato")
        g = g.water(shared, 10, PlotOwner.BOY)
        assertEquals(PlotStage.SEED, g.plots[shared].stage)
        assertEquals(PlotOwner.BOY, g.waitingOnOther(shared, 10))
        assertFalse(g.needsWater(shared, 10, PlotOwner.BOY))
        assertTrue(g.needsWater(shared, 10, PlotOwner.GIRL))
        // The same partner again changes nothing.
        assertEquals(g, g.water(shared, 10, PlotOwner.BOY))
        g = g.water(shared, 10, PlotOwner.GIRL)
        assertEquals(PlotStage.SPROUT, g.plots[shared].stage)
        assertNull(g.waitingOnOther(shared, 10))
        // A new day starts afresh; yesterday's half doesn't count.
        g = g.water(shared, 11, PlotOwner.GIRL)
        assertEquals(PlotStage.SPROUT, g.plots[shared].stage)
        assertEquals(PlotOwner.GIRL, g.waitingOnOther(shared, 11))
    }

    @Test
    fun aRipeCropIsHarvestedNotPicked() {
        val g = (1..GardenPlots.WATERINGS_TO_BLOOM).fold(GardenPlots().plant(0, "tomato")) { acc, d -> acc.water(0, d.toLong()) }
        assertEquals(PlotStage.BLOOM, g.plots[0].stage)
        assertNull(g.pick(0).second)
        assertEquals(g, g.pick(0).first)
        val (after, produce) = g.harvest(0)
        assertEquals(Ingredient.TOMATO, produce)
        assertEquals(PlotStage.EMPTY, after.plots[0].stage)
    }

    @Test
    fun gardenRecipesWaitForThePantry() {
        val starters = Recipes.available(emptyMap()).map { it.id }.toSet()
        assertEquals(Recipes.STARTERS.map { it.id }.toSet(), starters)
        val soup = Recipes.byId("tomato_soup")!!
        assertFalse(soup.canCook(mapOf(Ingredient.TOMATO to 1)))
        assertTrue(soup.canCook(mapOf(Ingredient.TOMATO to 1, Ingredient.BASIL to 1)))
        // Every garden recipe uses something grown, and every crop has a recipe.
        assertTrue(Recipes.GARDEN.all { it.pantry.isNotEmpty() })
        val grown = Seeds.ALL.mapNotNull { it.produce }.toSet()
        assertEquals(grown, Recipes.GARDEN.flatMap { it.pantry }.toSet())
    }

    private fun run(vararg events: ProgressEvent, start: ProgressState = ProgressState()): Pair<ProgressState, List<String>> {
        var s = start
        val earned = mutableListOf<String>()
        for (e in events) {
            val (next, firsts) = LittleFirsts.apply(s, e, 20000)
            s = next
            earned += firsts.map { it.id }
        }
        return s to earned
    }

    @Test
    fun harvestsFillThePantryAndAGardenDishUsesThem() {
        val grow = listOf(
            ProgressEvent.GardenPlanted(0, "tomato"), ProgressEvent.GardenPlanted(2, "basil")
        ) + (1..GardenPlots.WATERINGS_TO_BLOOM).flatMap { d -> listOf(ProgressEvent.GardenWatered(0, d.toLong()), ProgressEvent.GardenWatered(2, d.toLong())) }
        val (grown, _) = run(*grow.toTypedArray())
        val (picked, firsts) = run(ProgressEvent.CropHarvested(0), ProgressEvent.CropHarvested(2), start = grown)
        assertEquals(listOf("first_harvest"), firsts)
        assertEquals(mapOf(Ingredient.TOMATO to 1, Ingredient.BASIL to 1), picked.pantry)
        val (cooked, dishFirsts) = run(ProgressEvent.DishCooked("tomato_soup"), start = picked)
        assertTrue("first_garden_dish" in dishFirsts)
        assertTrue(cooked.pantry.isEmpty())
        assertEquals(0, cooked.keepsakes[PANTRY + "TOMATO"])
        assertEquals(1, cooked.count(Counter.GARDEN_DISHES))
        // A starter doesn't touch the pantry.
        val (tea, _) = run(ProgressEvent.DishCooked("tea"), start = picked)
        assertEquals(picked.pantry, tea.pantry)
    }

    @Test
    fun theSharedPlotWatersThroughTheProgressToo() {
        val (s, _) = run(
            ProgressEvent.GardenPlanted(1, "peas"),
            ProgressEvent.GardenWatered(1, 3, "GIRL"),
            ProgressEvent.GardenWatered(1, 3, "BOY")
        )
        assertEquals(PlotStage.SPROUT, s.garden.plots[1].stage)
    }

    @Test
    fun theChefsApronStillMeansTheFiveStarters() {
        val gardenOnes = Recipes.GARDEN.map { ProgressEvent.DishCooked(it.id) }
        val (s, earned) = run(*gardenOnes.toTypedArray())
        assertFalse("all_recipes" in earned)
        val (_, more) = run(*Recipes.STARTERS.map { ProgressEvent.DishCooked(it.id) }.toTypedArray(), start = s)
        assertTrue("all_recipes" in more)
    }
}
