package com.example.autonomy

import com.example.data.InMemoryKeyValueStorage
import com.example.data.PetKind
import com.example.data.PetStore
import com.example.engine.AmbientAudio
import com.example.engine.GameText
import com.example.scene.CatState
import com.example.scene.PetMove
import com.example.scene.PetMoves
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Plan 12, A: every pet's own moves, the trick after a few pats, and where moves can't happen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PetMovesEngineTest {

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
        engine.behaviorBrain.random = Random(5)
        engine.autonomyEnabled = false
        store = PetStore(InMemoryKeyValueStorage())
        engine.petStore = store
        PetKind.entries.forEach { store.meet(it) }
    }

    @After
    fun clear() { GameText.petName = "Mochi" }

    private fun run(seconds: Float) {
        var t = 0f
        while (t < seconds) { engine.update(dt, cw, ch); t += dt }
    }

    @Test
    fun `every pet does each of its moves, and each one ends`() {
        for (kind in PetKind.entries) {
            engine.choosePet(kind)
            engine.loadScene(SceneType.FLOWER)
            run(9f)
            for (move in PetMoves.movesFor(kind)) {
                assertTrue("$kind can $move", engine.startPetMove(move, cw, ch))
                if (!move.travels || move == PetMove.ROLL) {
                    if (move != PetMove.ZOOMIES) assertEquals(move, engine.petMove)
                }
                // Over by the end (it may well find something else to do straight after)
                var t = 0f
                var over = false
                while (t < move.seconds + 0.3f) {
                    engine.update(dt, cw, ch)
                    t += dt
                    if (engine.petMove != move) over = true
                }
                assertTrue("$kind's $move is over", over)
            }
        }
    }

    @Test
    fun `no digging indoors, and nothing in the loft`() {
        engine.choosePet(PetKind.PUPPY)
        engine.loadScene(SceneType.COOKING)
        run(9f)
        assertFalse(engine.startPetMove(PetMove.DIG, cw, ch))
        assertTrue(engine.startPetMove(PetMove.SNIFF, cw, ch))
        engine.loadScene(SceneType.COZY_LOFT)
        assertNull("A new scene ends a move", engine.petMove)
        assertFalse(engine.startPetMove(PetMove.SNIFF, cw, ch))
    }

    @Test
    fun `a pet only does its own moves`() {
        engine.choosePet(PetKind.OWL)
        engine.loadScene(SceneType.FLOWER)
        run(9f)
        assertFalse("Owls don't dig", engine.startPetMove(PetMove.DIG, cw, ch))
        assertTrue(engine.startPetMove(PetMove.HEAD_TURN, cw, ch))
    }

    @Test
    fun `a few pats bring a trick, from every pet`() {
        // The hedgehog curls up at each pat, so it's given a moment to uncurl between them
        for (kind in listOf(PetKind.CAT, PetKind.PUPPY, PetKind.BUNNY, PetKind.FOX, PetKind.DUCK, PetKind.HEDGEHOG)) {
            engine.choosePet(kind)
            engine.loadScene(SceneType.FLOWER)
            run(9f)
            val trick = PetMoves.trickFor(kind)
            // (a pat also ends any move of its own it's busy with)
            var pats = 0
            while (engine.petMove != trick && pats < 12) {
                engine.onTouchCat(cw, ch)
                pats++
                if (kind == PetKind.HEDGEHOG && engine.petMove != trick) run(2.6f)
            }
            assertEquals("$kind's trick", trick, engine.petMove)
            assertTrue("$kind: from the fourth pat, the first it's awake for (pat $pats)", pats in 4..8)
            // Another pat while it's at it doesn't cut it short
            engine.onTouchCat(cw, ch)
            assertEquals("$kind's trick plays out", trick, engine.petMove)
            run(trick.seconds + 0.3f)
        }
    }

    @Test
    fun `calling the pet over ends its move`() {
        engine.loadScene(SceneType.FLOWER)
        run(9f)
        assertTrue(engine.startPetMove(PetMove.GROOM, cw, ch))
        engine.commandCatWalkTo(0.2f, 0.7f, cw, ch)
        assertNull(engine.petMove)
    }
}
