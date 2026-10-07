package com.example.autonomy

import com.example.data.FoxStore
import com.example.data.FridayFox
import com.example.data.InMemoryKeyValueStorage
import com.example.data.PetKind
import com.example.data.PetStore
import com.example.engine.AmbientAudio
import com.example.engine.GameText
import com.example.engine.WorldViewport
import com.example.resources.Res
import com.example.resources.scene_mochi_is_purring_happily
import com.example.scene.CatState
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Plan 10, E: meeting the other pets, and having one live with them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PetsEngineTest {

    private lateinit var engine: SceneEngine
    private lateinit var store: PetStore
    private val cw = 1080f
    private val ch = 2400f
    private val dt = 0.05f

    @Before
    fun setup() {
        runBlocking { GameText.load() }
        engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {})
        engine.weatherDriftEnabled = false
        engine.updateAtmosphereMode("DAY")
        engine.behaviorBrain.random = Random(3)
        engine.autonomyEnabled = false
        store = PetStore(InMemoryKeyValueStorage())
        engine.petStore = store
    }

    @After
    fun clear() {
        FridayFox.todayOverride = null
        GameText.petName = "Mochi"
    }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) { engine.update(dt, cw, ch); t += dt }
    }

    private fun tapVisitor(): Boolean {
        val v = engine.petVisitor ?: return false
        val p = WorldViewport.pixelScale(cw)
        return engine.onPetVisitorTap(cw * v.x, ch * v.y - 7f * p, cw, ch, p)
    }

    @Test
    fun `a bunny turns up in the meadow, and a tap meets her`() {
        engine.loadScene(SceneType.FLOWER)
        engine.weather = WeatherType.SUNNY
        run(SceneEngine.PET_VISITOR_AFTER + 1f)
        val v = engine.petVisitor
        assertNotNull(v)
        assertEquals(PetKind.BUNNY, v!!.kind)
        assertTrue(tapVisitor())
        assertTrue(store.hasMet(PetKind.BUNNY))
        // She hops off happily, and once met she doesn't come back as a visitor
        run(10f)
        assertNull(engine.petVisitor)
        engine.loadScene(SceneType.WALK)
        engine.loadScene(SceneType.FLOWER)
        run(SceneEngine.PET_VISITOR_AFTER + 2f)
        assertNull(engine.petVisitor)
    }

    @Test
    fun `each visitor has its own place and time`() {
        engine.loadScene(SceneType.WALK)
        engine.weather = WeatherType.SUNNY
        run(SceneEngine.PET_VISITOR_AFTER + 1f)
        assertNull("No duck on a dry day", engine.petVisitor)
        engine.loadScene(SceneType.WALK)
        engine.weather = WeatherType.RAIN
        run(SceneEngine.PET_VISITOR_AFTER + 1f)
        assertEquals(PetKind.DUCK, engine.petVisitor?.kind)

        engine.loadScene(SceneType.LOOKING)
        run(SceneEngine.PET_VISITOR_AFTER + 1f)
        assertNull("The owl is only out at night", engine.petVisitor)
        engine.updateAtmosphereMode("NIGHT")
        engine.loadScene(SceneType.LOOKING)
        run(SceneEngine.PET_VISITOR_AFTER + 1f)
        assertEquals(PetKind.OWL, engine.petVisitor?.kind)
    }

    @Test
    fun `a pet they've met can live with them, and the scene says its name`() {
        assertFalse("Not met yet", engine.choosePet(PetKind.OWL))
        store.meet(PetKind.PUPPY)
        assertTrue(engine.choosePet(PetKind.PUPPY))
        assertEquals(PetKind.PUPPY, engine.petKind)
        assertEquals(PetKind.PUPPY, store.chosen)
        assertEquals("Boba", engine.petName)
        assertTrue(GameText.get(Res.string.scene_mochi_is_purring_happily).startsWith("Boba"))
        // And Mochi comes home again
        assertTrue(engine.choosePet(PetKind.CAT))
        assertEquals("Mochi", GameText.petName)
    }

    @Test
    fun `the owl naps by day, and the hedgehog curls up when tapped`() {
        store.meet(PetKind.OWL)
        engine.choosePet(PetKind.OWL)
        engine.loadScene(SceneType.FLOWER)
        run(2f)
        assertEquals(CatState.SLEEPING, engine.catState)

        store.meet(PetKind.HEDGEHOG)
        engine.choosePet(PetKind.HEDGEHOG)
        run(1f)
        engine.onTouchCat(cw, ch)
        assertEquals(CatState.SLEEPING, engine.catState)
        run(2.4f)
        assertTrue("Uncurled again", engine.catState != CatState.SLEEPING)
    }

    @Test
    fun `Boba comes home after three days of pats, and the fox stays after three Fridays`() {
        engine.loadScene(SceneType.RAINY_CAFE)
        store.notePupPetted("2026-01-01")
        store.notePupPetted("2026-01-02")
        engine.onTouchCafePup(cw, ch)
        assertTrue(store.hasMet(PetKind.PUPPY))

        val friday = LocalDate(2026, 10, 9)
        FridayFox.todayOverride = friday
        val foxes = FoxStore(InMemoryKeyValueStorage()).apply {
            noteSeen(LocalDate(2026, 9, 1))
            markVisited(LocalDate(2026, 9, 25))
            markVisited(LocalDate(2026, 10, 2))
        }
        engine.foxStore = foxes
        engine.loadScene(SceneType.FLOWER)
        var t = 0f
        while (!store.hasMet(PetKind.FOX) && t < 90f) { engine.update(dt, cw, ch); t += dt }
        assertTrue("Ember stays after the third Friday", store.hasMet(PetKind.FOX))
    }
}
