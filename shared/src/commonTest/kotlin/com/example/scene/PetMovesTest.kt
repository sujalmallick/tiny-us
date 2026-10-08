package com.example.scene

import com.example.data.PetKind
import com.example.scene.autonomy.SpotAction
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PetMovesTest {
    @Test
    fun everyPetHasItsOwnMoves() {
        for (kind in PetKind.entries) {
            assertTrue(PetMoves.movesFor(kind).size >= 4, "$kind has a few moves")
            assertTrue(PetMoves.trickFor(kind) in PetMoves.movesFor(kind), "$kind's trick is one of its own")
        }
    }

    @Test
    fun atAnythingInTheSceneAPetDoesSomethingItCanAndStaysThere() {
        for (kind in PetKind.entries) for (action in SpotAction.entries) {
            val move = PetMoves.atSpot(action, kind)
            assertTrue(PetMoves.canDo(kind, move), "$kind at $action: $move")
            assertFalse(move.travels, "$kind at $action stays put")
        }
    }

    @Test
    fun onlyWetCoatsGetShaken() {
        assertTrue(PetMoves.canDo(PetKind.PUPPY, PetMove.SHAKE))
        assertFalse(PetMoves.canDo(PetKind.OWL, PetMove.SHAKE))
        assertFalse(PetMoves.canDo(PetKind.HEDGEHOG, PetMove.SHAKE))
    }
}
