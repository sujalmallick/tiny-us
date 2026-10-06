package com.example.games

import com.example.engine.AmbientAudio
import com.example.progress.ProgressEvent
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Cooking, fishing and garden care running inside the real engine (plan 07, C3-C5). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CozyGamesEngineTest {

    private val w = 1080f
    private val h = 2400f

    private fun engine(scene: SceneType) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            loadScene(scene)
            updateAtmosphereMode("DAY")
            weatherDriftEnabled = false
            repeat(30) { update(1f / 60f, w, h) }
        }

    private fun SceneEngine.run(seconds: Float) = repeat((seconds * 60).toInt()) { update(1f / 60f, w, h) }

    @Test
    fun cookingARecipeServesTheDishAndRecordsIt() {
        val e = engine(SceneType.COOKING)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        assertTrue(e.cozy.canCook)
        e.cozy.startCooking(Random(1))
        val recipe = e.cozy.cooking.recipe!!
        assertTrue(e.cozy.cookingChoices.containsAll(recipe.ingredients))
        // A wrong one first, then the right order.
        val wrong = e.cozy.cookingChoices.first { it != recipe.ingredients.first() }
        e.cozy.tapIngredient(wrong)
        assertEquals(0, e.cozy.cooking.added)
        recipe.ingredients.forEach { e.cozy.tapIngredient(it) }
        e.run(CookingGame.SIMMER_SECONDS + 0.5f)
        assertEquals(recipe, e.cozy.servedDish)
        assertTrue(events.contains(ProgressEvent.DishCooked(recipe.id)))
        e.run(CookingGame.SERVED_SECONDS + 0.5f)
        assertFalse(e.cozy.cookingActive)
    }

    @Test
    fun leavingTheKitchenPutsTheCardAway() {
        val e = engine(SceneType.COOKING)
        e.cozy.startCooking()
        e.loadScene(SceneType.FLOWER)
        assertFalse(e.cozy.cookingActive)
        assertFalse(e.cozy.canCook)
    }

    @Test
    fun fishingReelsInAFirstFishAndCastsAgain() {
        val e = engine(SceneType.SEASIDE_PIER)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.cozy.startFishing()
        var t = 0f
        while (e.cozy.fishing.phase != FishingGame.Phase.BITE && t < 20f) {
            e.run(0.1f)
            t += 0.1f
        }
        e.cozy.tapFishing()
        e.run(FishingGame.REEL_SECONDS + 0.2f)
        val caught = events.filterIsInstance<ProgressEvent.FishCaught>().single()
        assertTrue(FishingCatch.valueOf(caught.kind).isFish)
        e.run(FishingGame.LANDED_SECONDS + 0.2f)
        // Still fishing: the line went straight back in.
        assertTrue(e.cozy.fishingActive)
        assertEquals(FishingGame.Phase.WAITING, e.cozy.fishing.phase)
        e.cozy.stopFishing()
        assertFalse(e.cozy.fishingActive)
    }

    @Test
    fun tappingAPlotPlantsWatersAndPicks() {
        val e = engine(SceneType.FLOWER)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        val p = com.example.engine.WorldViewport.pixelScale(w)
        val plot = e.cozy.plotBase(0, w, h)
        fun tap() = e.cozy.onGardenTap(plot.x, plot.y - 3f * p, w, h, p)
        assertTrue(tap())
        assertNotNull(events.filterIsInstance<ProgressEvent.GardenPlanted>().singleOrNull())
        assertTrue(tap())
        assertEquals(1, events.filterIsInstance<ProgressEvent.GardenWatered>().size)
        // Once a day: the second watering is only a gentle note.
        assertTrue(tap())
        assertEquals(1, events.filterIsInstance<ProgressEvent.GardenWatered>().size)
        // A tap well away from the plots is not for the garden.
        assertFalse(e.cozy.onGardenTap(w * 0.9f, h * 0.3f, w, h, p))
        // In bloom, a tap picks it.
        e.cozy.garden = GardenPlots().plant(0, "tulip").water(0, 1).water(0, 2).water(0, 3)
        assertTrue(tap())
        assertTrue(events.contains(ProgressEvent.FlowerPicked(0)))
        assertEquals(listOf("tulip"), e.cozy.garden.stems)
    }
}
