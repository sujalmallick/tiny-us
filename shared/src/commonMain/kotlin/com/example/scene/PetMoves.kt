package com.example.scene

import com.example.data.PetKind
import com.example.scene.autonomy.SpotAction

/**
 * Something a pet does for a moment (plan 12, A): a stretch, a wash, a chase of its own tail, a
 * sniff, a dig, a pounce... Each pet has its own few, in its own way; they do them now and then
 * on their own, at the things they wander over to, and as a trick when they've had a few pats.
 */
enum class PetMove(val seconds: Float) {
    /** Front low, back up, a long stretch. */
    STRETCH(2.4f),
    /** A wash: a paw to the face, or a preen. */
    GROOM(3.0f),
    /** A back foot scratching behind an ear. */
    SCRATCH(2.2f),
    /** Round and round after its own tail. */
    CHASE_TAIL(2.6f),
    /** Nose down, sniffing about. */
    SNIFF(2.6f),
    /** Digging a little hole, earth flying. */
    DIG(3.0f),
    /** A crouch, a wiggle and a leap, nose first. */
    POUNCE(1.6f),
    /** A shake from nose to tail, drops flying. */
    SHAKE(1.4f),
    /** A big yawn. */
    YAWN(2.0f),
    /** Flopped over on its side, perfectly content. */
    FLOP(3.6f),
    /** A happy hop or two. */
    HOP(1.8f),
    /** The owl's head turning right round. */
    HEAD_TURN(2.8f),
    /** Wings out, a flap and a lift. */
    FLAP(1.8f),
    /** The ducklings marching round their mother. */
    PARADE(4.0f),
    /** The hedgehog rolling along in a ball. */
    ROLL(2.4f),
    /** A dash across the scene and back (drawn as a run; the engine moves the pet). */
    ZOOMIES(3.0f);

    /** Moves the engine carries the pet along for (the rest stay where they are). */
    val travels: Boolean get() = this == ZOOMIES || this == ROLL
}

object PetMoves {
    /** Each pet's own moves. */
    fun movesFor(kind: PetKind): List<PetMove> = when (kind) {
        PetKind.CAT -> listOf(PetMove.STRETCH, PetMove.GROOM, PetMove.CHASE_TAIL, PetMove.ZOOMIES, PetMove.POUNCE, PetMove.YAWN, PetMove.SNIFF)
        PetKind.PUPPY -> listOf(PetMove.CHASE_TAIL, PetMove.ZOOMIES, PetMove.DIG, PetMove.SCRATCH, PetMove.SNIFF, PetMove.STRETCH, PetMove.HOP)
        PetKind.BUNNY -> listOf(PetMove.HOP, PetMove.FLOP, PetMove.GROOM, PetMove.SNIFF, PetMove.ZOOMIES, PetMove.DIG)
        PetKind.FOX -> listOf(PetMove.POUNCE, PetMove.CHASE_TAIL, PetMove.SCRATCH, PetMove.DIG, PetMove.STRETCH, PetMove.YAWN)
        PetKind.HEDGEHOG -> listOf(PetMove.SNIFF, PetMove.ROLL, PetMove.YAWN, PetMove.STRETCH, PetMove.DIG)
        PetKind.DUCK -> listOf(PetMove.GROOM, PetMove.FLAP, PetMove.PARADE, PetMove.SNIFF)
        PetKind.OWL -> listOf(PetMove.HEAD_TURN, PetMove.FLAP, PetMove.GROOM, PetMove.YAWN, PetMove.HOP)
    }

    /** A wet coat gets shaken off by everyone but the owl and the hedgehog. */
    fun canDo(kind: PetKind, move: PetMove): Boolean =
        move in movesFor(kind) || (move == PetMove.SHAKE && kind != PetKind.OWL && kind != PetKind.HEDGEHOG)

    /** The trick a pet shows off after a few pats. */
    fun trickFor(kind: PetKind): PetMove = when (kind) {
        PetKind.CAT, PetKind.PUPPY -> PetMove.CHASE_TAIL
        PetKind.BUNNY -> PetMove.HOP
        PetKind.FOX -> PetMove.POUNCE
        PetKind.HEDGEHOG -> PetMove.ROLL
        PetKind.DUCK -> PetMove.PARADE
        PetKind.OWL -> PetMove.HEAD_TURN
    }

    /** What a pet does at something it has wandered over to; its own way if it can't do that. */
    fun atSpot(action: SpotAction, kind: PetKind): PetMove {
        val want = when (action) {
            SpotAction.SMELL_FLOWERS, SpotAction.SMELL_LAVENDER, SpotAction.PICK_FLOWER, SpotAction.WATER_PLANT,
            SpotAction.MIST_PLANTS, SpotAction.ADMIRE_LANTERN, SpotAction.CHECK_CRATE, SpotAction.ORDER_COFFEE,
            SpotAction.FOG_WINDOW, SpotAction.TEND_LANTERN, SpotAction.LOOK_SEA, SpotAction.SNIFF_STEAMER,
            SpotAction.STIR_POT, SpotAction.PEEK_OVEN, SpotAction.BROWSE_BOOKS -> PetMove.SNIFF
            SpotAction.LISTEN_CHIMES, SpotAction.POKE_MUSHROOMS, SpotAction.PEEK_BUCKET -> PetMove.POUNCE
            SpotAction.LOOK_UP_TREE -> PetMove.STRETCH
            SpotAction.SIT_GRASS, SpotAction.SIT_POUF, SpotAction.WARM_HANDS -> PetMove.FLOP
            SpotAction.SIT_TABLE, SpotAction.PET_PUP, SpotAction.STRUM_GUITAR, SpotAction.SPOT_DOLPHINS -> PetMove.HOP
            SpotAction.LIGHT_CANDLE, SpotAction.LOOK_SKYLIGHT, SpotAction.LOOK_WINDOW,
            SpotAction.ADMIRE_FAIRY_LIGHTS, SpotAction.USE_TELESCOPE -> PetMove.YAWN
            SpotAction.READ_CHALKBOARD, SpotAction.RINSE_DISHES -> PetMove.SCRATCH
            SpotAction.PEEK_BOX, SpotAction.FILL_SAUCER -> PetMove.SNIFF
        }
        if (canDo(kind, want) && !want.travels) return want
        // Its own nearest thing
        val own = movesFor(kind).filter { !it.travels }
        return when (want) {
            PetMove.POUNCE, PetMove.HOP -> own.firstOrNull { it == PetMove.HOP || it == PetMove.POUNCE || it == PetMove.FLAP }
            PetMove.FLOP -> own.firstOrNull { it == PetMove.FLOP || it == PetMove.YAWN || it == PetMove.STRETCH }
            else -> null
        } ?: own.first()
    }
}
