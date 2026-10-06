package com.example.progress

import com.example.games.GardenPlots
import com.example.games.PlotStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What cooking, fishing and the garden add to the progress (plan 07, C3-C5). */
class CozyProgressTest {

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
    fun everyRecipeFillsTheBookAndEarnsTheApron() {
        val (one, first) = run(ProgressEvent.DishCooked("tea"), ProgressEvent.DishCooked("tea"))
        assertEquals(listOf("first_dish"), first)
        assertEquals(2, one.keepsakes["dish:tea"])
        assertEquals(setOf("tea"), one.seenSet(Seen.RECIPES))
        val rest = listOf("pancakes", "soup", "dumplings", "cookies").map { ProgressEvent.DishCooked(it) }
        val (all, earned) = run(*rest.toTypedArray(), start = one)
        assertEquals(listOf("all_recipes"), earned)
        assertTrue(Rewards.CHEF_APRON in all.unlocked)
    }

    @Test
    fun aFishEarnsTheHatButABootDoesNot() {
        val (boot, none) = run(ProgressEvent.FishCaught("OLD_BOOT"))
        assertTrue(none.isEmpty())
        assertEquals(1, boot.keepsakes["catch:OLD_BOOT"])
        val (fish, earned) = run(ProgressEvent.FishCaught("CARP"), ProgressEvent.FishCaught("GOLDEN_FISH"), start = boot)
        assertEquals(listOf("first_fish", "golden_fish"), earned)
        assertTrue(Rewards.FISHER_HAT in fish.unlocked)
        assertEquals(2, fish.count(Counter.FISH))
    }

    @Test
    fun theGardenGrowsInTheProgressAndMakesABouquet() {
        val events = mutableListOf<ProgressEvent>()
        for (i in 0 until GardenPlots.PLOTS) {
            events += ProgressEvent.GardenPlanted(i, GardenPlots.SEEDS[i])
            for (d in 1..GardenPlots.WATERINGS_TO_BLOOM) events += ProgressEvent.GardenWatered(i, d.toLong())
        }
        val (grown, _) = run(*events.toTypedArray())
        assertTrue(grown.garden.plots.all { it.stage == PlotStage.BLOOM })
        val (picked, earned) = run(ProgressEvent.FlowerPicked(0), ProgressEvent.FlowerPicked(1), ProgressEvent.FlowerPicked(2), start = grown)
        assertEquals(listOf("first_bloom", "first_bouquet"), earned)
        assertEquals(1, picked.keepsakes[Gifts.BOUQUET])
        assertTrue(Rewards.FLOWER_CROWN in picked.unlocked)
        assertTrue(picked.garden.plots.all { it.stage == PlotStage.EMPTY })
        // Picking an empty plot changes nothing.
        assertEquals(picked, run(ProgressEvent.FlowerPicked(0), start = picked).first)
    }

    @Test
    fun rainWatersThePlantedPlots() {
        val (s, _) = run(ProgressEvent.GardenPlanted(1, "daisy"), ProgressEvent.GardenRained(7))
        assertEquals(PlotStage.SPROUT, s.garden.plots[1].stage)
        assertEquals(s, run(ProgressEvent.GardenRained(7), start = s).first)
    }

    @Test
    fun aDishCanBeGivenAndTheRecipeBookKeepsIt() {
        val (s, _) = run(ProgressEvent.DishCooked("cookies"))
        val (given, _) = run(ProgressEvent.GiftGiven("dish:cookies", fromBoy = false), start = s)
        assertEquals(0, given.keepsakes["dish:cookies"])
        assertEquals(listOf("dish:cookies"), given.shelf)
        assertEquals(setOf("cookies"), given.seenSet(Seen.RECIPES))
        assertTrue(Gifts.BOUQUET in Gifts.GIVEABLE)
    }

    @Test
    fun grantedRequestsEarnTheirFirstsOnce() {
        val (one, first) = run(ProgressEvent.RequestGranted("TEA"), ProgressEvent.RequestGranted("TEA"))
        assertEquals(listOf("first_request"), first)
        assertEquals(2, one.count(Counter.REQUESTS))
        val rest = listOf("WARM", "SONG", "SNACK", "MOCHI").map { ProgressEvent.RequestGranted(it) }
        val (_, earned) = run(*rest.toTypedArray(), start = one)
        assertEquals(listOf("all_requests"), earned)
    }
}
