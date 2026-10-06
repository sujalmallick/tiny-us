package com.example.games

import com.example.engine.AmbientAudio
import com.example.engine.CharacterPose
import com.example.engine.WorldViewport
import com.example.progress.ProgressEvent
import com.example.scene.CozyGames
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Garden to kitchen in the real engine (plan 09, E). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GardenKitchenEngineTest {

    private val w = 1080f
    private val h = 2400f

    private fun engine(scene: SceneType) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateNames("Leo", "Mia")
            loadScene(scene)
            updateAtmosphereMode("DAY")
            weatherDriftEnabled = false
            repeat(30) { update(1f / 60f, w, h) }
        }

    private fun SceneEngine.run(seconds: Float) = repeat((seconds * 60).toInt()) { update(1f / 60f, w, h) }

    @Test
    fun theSharedPlotAsksWhoIsWateringAndWaitsForTheOther() {
        val e = engine(SceneType.FLOWER)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.cozy.garden = GardenPlots().plant(1, "peas")
        val p = WorldViewport.pixelScale(w)
        val base = e.cozy.plotBase(1, w, h)
        assertTrue(e.cozy.onGardenTap(base.x, base.y - 3f * p, w, h, p))
        assertEquals(1, e.cozy.whoWatersSpot)
        e.cozy.waterAs(PlotOwner.GIRL)
        assertNull(e.cozy.whoWatersSpot)
        assertTrue(events.any { it is ProgressEvent.GardenWatered && it.by == "GIRL" })
        assertTrue(e.sceneMessage.orEmpty(), e.sceneMessage?.contains("Mia watered. Waiting for Leo") == true)
        // His turn: now it grows.
        assertTrue(e.cozy.onGardenTap(base.x, base.y - 3f * p, w, h, p))
        e.cozy.waterAs(PlotOwner.BOY)
        assertEquals(PlotStage.SPROUT, e.cozy.garden.plots[1].stage)
    }

    @Test
    fun winterSendsSeedsToTheSunroomPots() {
        val e = engine(SceneType.FLOWER)
        e.cozy.season = "WINTER"
        val p = WorldViewport.pixelScale(w)
        val base = e.cozy.plotBase(0, w, h)
        assertTrue(e.cozy.onGardenTap(base.x, base.y - 3f * p, w, h, p))
        assertNull(e.cozy.seedPickerSpot)
        assertTrue(e.sceneMessage.orEmpty(), e.sceneMessage?.contains("sunroom") == true)

        val sun = engine(SceneType.SUNROOM)
        val pot = sun.cozy.plotBase(GardenPlots.PLOTS, w, h)
        assertTrue(sun.cozy.onPotTap(pot.x, pot.y - 3f * p, w, h, p))
        assertEquals(GardenPlots.PLOTS, sun.cozy.seedPickerSpot)
        sun.cozy.chooseSeed(Seeds.byId("mint")!!)
        assertEquals("mint", sun.cozy.garden.plots[GardenPlots.PLOTS].flower)
    }

    @Test
    fun aGardenRecipeNeedsThePantryAndThenTheyEatTogether() {
        val e = engine(SceneType.COOKING)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.cozy.openRecipePicker()
        assertTrue(e.cozy.cookingPicking)
        val soup = Recipes.byId("tomato_soup")!!
        e.cozy.chooseRecipe(soup)
        assertFalse("locked without tomatoes and basil", e.cozy.cookingActive)
        e.cozy.pantry = mapOf(Ingredient.TOMATO to 1, Ingredient.BASIL to 1)
        e.cozy.chooseRecipe(soup, Random(4))
        assertTrue(e.cozy.cookingActive)
        assertFalse(e.cozy.cookingPicking)
        soup.ingredients.forEach { e.cozy.tapIngredient(it) }
        e.run(CookingGame.SIMMER_SECONDS + 0.3f)
        assertTrue(events.contains(ProgressEvent.DishCooked("tomato_soup")))
        assertTrue(e.cozy.pantry.values.all { it == 0 })
        // They sit down at the table, and talk over dinner.
        assertEquals(CozyGames.TABLE_BOY_X, e.boy.targetWorldX, 0.01f)
        assertEquals(CozyGames.TABLE_GIRL_X, e.girl.targetWorldX, 0.01f)
        e.run(CozyGames.DINNER_TALK_AFTER + 0.2f)
        assertTrue(e.sceneMessage.orEmpty(), e.sceneMessage?.startsWith("Over dinner") == true)
        e.run(3f)
        assertEquals(CharacterPose.SIT, e.boy.pose)
        assertEquals(CharacterPose.SIT, e.girl.pose)
    }

    @Test
    fun aRipeCropGoesToThePantry() {
        val e = engine(SceneType.FLOWER)
        val events = mutableListOf<ProgressEvent>()
        e.onProgress = { events += it }
        e.cozy.garden = GardenPlots().plant(2, "strawberry").water(2, 1).water(2, 2).water(2, 3)
        val p = WorldViewport.pixelScale(w)
        val base = e.cozy.plotBase(2, w, h)
        assertTrue(e.cozy.onGardenTap(base.x, base.y - 3f * p, w, h, p))
        assertTrue(events.contains(ProgressEvent.CropHarvested(2)))
        assertEquals(1, e.cozy.pantry[Ingredient.STRAWBERRY])
        assertEquals(PlotStage.EMPTY, e.cozy.garden.plots[2].stage)
    }
}
