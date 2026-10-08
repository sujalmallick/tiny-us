package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.PetKind
import com.example.engine.CastLight
import com.example.engine.drawCastShadow
import com.example.engine.drawContactShadow
import com.example.scene.CatState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Plan 10, E: the other pets, from the tinyus.online footer: a puppy, a bunny, a fox cub, a
 * hedgehog, a duck with her ducklings and an owl. Each one is drawn for Mochi's states (sitting,
 * asleep, walking, playing) with its own little habits. Art is facing right, '.' is empty.
 */
object PetSprites {
    private val K = Color(0xFF2B2024)

    private val PUPPY_COLORS = mapOf('T' to Color(0xFFC68B59), 'D' to Color(0xFF7A4A2A), 'W' to Color(0xFFFFF6EC), 'K' to K, 'P' to Color(0xFFFF8FA3), 'w' to Color(0xFFE8D5C0))
    private val PUPPY_WALK_A = listOf("...........TTT..", "W.........TTTTT.", ".T........DTKTTT", ".T.TTTTTTTDTTWWK", "..TTTTDDTTDTWWP.", "..TTTDDDTTTWW...", "..TTTTTTTTTW....", "..T.T....T.T....", "..W.W....W.W....")
    private val PUPPY_WALK_B = listOf("...........TTT..", ".W........TTTTT.", ".T........DTKTTT", "..TTTTTTTTDTTWWK", "..TTTTDDTTDTWWP.", "..TTTDDDTTTWW...", "..TTTTTTTTTW....", "...T.T..T.T.....", "...W.W..W.W.....")
    private val PUPPY_SIT = listOf("...........TTT..", "..........TTTTT.", "W.........DTKTTT", ".T..TTTTTTDTTWWK", ".TTTTTDDTTDTWWP.", "..TTTDDDTTTWW...", "..TTTTTTTTTW....", "...TT...TT......", "...WW...WW......")
    private val PUPPY_SLEEP = listOf("................", "................", "................", "..........TTT...", "W.........TTTTT.", ".T....TTTTDTwTTT", ".TTTTTTDDTDTTWWK", ".TTTTDDDTTTTWW..", "..WW.....WW.....")
    /** On his back, paws up, for a belly rub. */
    private val PUPPY_BELLY = listOf("................", "..W.W....W.W....", "..T.T....T.T....", "..wwwwwwwwwT....", ".TTTTTTTTTDTTT..", "W.TTTTDDTTDTwTT.", "...TTTTTTTTTWWK.", "................", "................")

    private val BUNNY_COLORS = mapOf('W' to Color(0xFFF3EEE8), 'w' to Color(0xFFD9CFC4), 'P' to Color(0xFFFFB5C2), 'K' to K)
    private val BUNNY = listOf(".....W.W.", ".....WPW.", ".....WWW.", "..WWWWWKW", ".WWWWWWWP", "WWWWWWWW.", "..ww..ww.")
    private val BUNNY_HOP = listOf(".....W.W.", ".....WPW.", ".....WWW.", "..WWWWWKW", ".WWWWWWWP", "WWWWWWWW.", ".w.....w.")
    private val BUNNY_SLEEP = listOf(".........", ".........", "....WW...", "..WWWWWWW", ".WWWWWwWP", "WWWWWWWW.", "..ww..ww.")

    private val FOX_COLORS = mapOf('O' to Color(0xFFE8743B), 'd' to Color(0xFF8C3B22), 'W' to Color(0xFFFFF6EC), 'K' to K, 'B' to K)
    private val FOX_A = listOf(".........d.d.", ".........OOO.", ".........OKOO", "WO..OOOOOOWWB", ".OOOOOOOOOW..", "..OOOOOOOOW..", "..d..d..d..d.")
    private val FOX_B = listOf(".........d.d.", ".........OOO.", ".........OKOO", ".WO.OOOOOOWWB", "..OOOOOOOOW..", "..OOOOOOOOW..", "...d.d.d..d..")
    private val FOX_SIT = listOf(".........d.d.", ".........OOO.", ".........OKOO", "W....OOOOOWWB", "WO.OOOOOOOW..", ".OOOOOOOOOW..", "...dd...dd...")
    /** Curled up with its tail over its nose. */
    private val FOX_SLEEP = listOf(".............", ".............", ".............", "....d.d......", "...OOOOOOOO..", "..OdOOOOOOOOW", "..OOOOOOOWWWW")

    private val HEDGEHOG_COLORS = mapOf('S' to Color(0xFF6B4A36), 's' to Color(0xFF9C7A5E), 'F' to Color(0xFFE8C9A0), 'K' to K, 'N' to K, 'f' to Color(0xFF4A3226))
    private val HEDGEHOG_A = listOf(".SSSSS...", "SSsSSSSF.", "SSSSsSFKF", "SSSSSSFFN", ".f.f..f..")
    private val HEDGEHOG_B = listOf(".SSSSS...", "SSsSSSSF.", "SSSSsSFKF", "SSSSSSFFN", "..f.f..f.")
    private val HEDGEHOG_BALL = listOf(".........", "..SSsS...", ".SSSSsS..", ".SsSSSS..", "..SSSS...")

    private val DUCK_COLORS = mapOf('W' to Color(0xFFF4F1EA), 'K' to K, 'O' to Color(0xFFFF9F1C), 'o' to Color(0xFFFF9F1C), 'Y' to Color(0xFFFFD166))
    private val DUCK_A = listOf(".....WW.", "....WWKO", "....WWW.", "WWWWWWW.", ".WWWWWW.", "..o..o..")
    private val DUCK_B = listOf(".....WW.", "....WWKO", "....WWW.", "WWWWWWW.", ".WWWWWW.", "...o.o..")
    private val DUCK_SLEEP = listOf("........", "........", ".....WW.", "WWWWWWWO", ".WWWWWW.", "..o..o..")
    private val DUCKLING_A = listOf("..YY.", "..YKO", "YYYY.", ".o.o.")
    private val DUCKLING_B = listOf("..YY.", "..YKO", "YYYY.", "o..o.")
    private val DUCKLING_SLEEP = listOf(".....", "..YY.", "YYYYO", ".o.o.")

    private val OWL_COLORS = mapOf('b' to Color(0xFF6E5240), 'B' to Color(0xFF8C6A50), 'Y' to Color(0xFFFFD166), 'K' to K, 'L' to Color(0xFFD9C2A0), 'o' to Color(0xFFFF9F1C))
    private val OWL = listOf("b....b", "BBBBBB", "BYBBYB", "BKBBKB", "BLLLLB", "BLLLLB", ".BLLB.", "..oo..")
    private val OWL_CLOSED = listOf("b....b", "BBBBBB", "BBBBBB", "BKKBKK", "BLLLLB", "BLLLLB", ".BLLB.", "..oo..")
    private val OWL_FLAP = listOf("b....b", "BBBBBB", "BYBBYB", "BKBBKB", "BLLLLBB", "BBLLLLB", ".BLLB.", "..oo..")

    /** How much bigger than the world's pixels each pet is drawn, so they sit beside Mochi's size. */
    private fun scaleOf(kind: PetKind): Float = when (kind) {
        PetKind.PUPPY -> 1.25f
        PetKind.BUNNY -> 1.6f
        PetKind.FOX -> 1.45f
        PetKind.HEDGEHOG -> 1.6f
        PetKind.DUCK -> 1.6f
        PetKind.OWL -> 1.8f
        PetKind.CAT -> 1f
    }

    private fun draw(scope: DrawScope, rows: List<String>, colors: Map<Char, Color>, cx: Float, groundY: Float, p: Float, flip: Boolean, lift: Float = 0f, replace: Map<Char, Char> = emptyMap()) {
        val w = rows.maxOf { it.length }
        val left = cx - w * p / 2f
        val top = groundY - rows.size * p - lift
        for ((y, row) in rows.withIndex()) {
            for ((x, ch0) in row.withIndex()) {
                val ch = replace[ch0] ?: ch0
                val c = colors[ch] ?: continue
                val col = if (flip) w - 1 - x else x
                scope.drawRect(c, Offset(left + col * p, top + y * p), Size(p, p))
            }
        }
    }

    private fun shadow(scope: DrawScope, cx: Float, groundY: Float, width: Float, p: Float, castLight: CastLight?) {
        val widthPx = (width / p).roundToInt()
        // In the world it also throws a shadow away from the light, about as long as it is tall
        if (castLight != null) drawCastShadow(scope, cx, groundY, widthPx - 2, p, heightPx = widthPx * 3 / 4, light = castLight)
        drawContactShadow(scope, cx, groundY, widthPx, p)
    }

    /**
     * Draws [kind] (not the cat: she has her own [com.example.engine.WorldSprites.drawCat]) in
     * [state], its feet on [groundY], at the world's pixel size [worldP].
     */
    fun drawPet(
        scope: DrawScope,
        kind: PetKind,
        cx: Float,
        groundY: Float,
        worldP: Float,
        time: Float,
        state: CatState,
        facingLeft: Boolean,
        night: Boolean = false,
        castLight: CastLight? = null
    ) {
        fun shadow(s: DrawScope, scx: Float, sgroundY: Float, swidth: Float, sp: Float) =
            this.shadow(s, scx, sgroundY, swidth, sp, castLight)
        val p = worldP * scaleOf(kind)
        val step = ((time * 5f).toInt() and 1) == 0
        val playing = state == CatState.PLAYFUL_POUNCE || state == CatState.BELLY_ROLL
        val walking = state == CatState.WALK_FOLLOW
        when (kind) {
            PetKind.CAT -> Unit
            PetKind.PUPPY -> {
                shadow(scope, cx, groundY, 13f * p, p)
                when {
                    walking -> draw(scope, if (step) PUPPY_WALK_A else PUPPY_WALK_B, PUPPY_COLORS, cx, groundY, p, facingLeft)
                    state == CatState.SLEEPING -> draw(scope, PUPPY_SLEEP, PUPPY_COLORS, cx, groundY, p, facingLeft)
                    state == CatState.BELLY_ROLL -> {
                        // Wriggling on his back, paws paddling
                        val wiggle = if (((time * 6f).toInt() and 1) == 0) 0f else p * 0.5f
                        draw(scope, PUPPY_BELLY, PUPPY_COLORS, cx + wiggle, groundY, p, facingLeft)
                    }
                    state == CatState.PLAYFUL_POUNCE -> draw(scope, if (step) PUPPY_WALK_A else PUPPY_WALK_B, PUPPY_COLORS, cx, groundY, p, facingLeft, lift = abs(sin(time * 7f)) * 3f * p)
                    else -> {
                        // Sitting, the tail wagging three times a second
                        val wag = ((time * 6f).toInt() and 1) == 0
                        val rows = if (wag) PUPPY_SIT else PUPPY_SIT.toMutableList().also { it[2] = ".W........DTKTTT" }
                        draw(scope, rows, PUPPY_COLORS, cx, groundY, p, facingLeft)
                    }
                }
            }
            PetKind.BUNNY -> {
                shadow(scope, cx, groundY, 8f * p, p)
                when {
                    state == CatState.SLEEPING -> draw(scope, BUNNY_SLEEP, BUNNY_COLORS, cx, groundY, p, facingLeft)
                    walking || playing -> {
                        // Hops in little arcs; a happy one twists up higher (a binky)
                        val arc = abs(sin(time * (if (playing) 4f else 9f)))
                        val lift = arc * (if (playing) 6f else 3f) * p
                        draw(scope, if (lift > p) BUNNY_HOP else BUNNY, BUNNY_COLORS, cx, groundY, p, if (playing && arc > 0.7f) !facingLeft else facingLeft, lift)
                    }
                    else -> {
                        // Sitting, nose twitching
                        val twitch = ((time * 4f).toInt() % 3) == 0
                        draw(scope, BUNNY, BUNNY_COLORS, cx, groundY, p, facingLeft, replace = if (twitch) mapOf('P' to 'W') else emptyMap())
                    }
                }
            }
            PetKind.FOX -> {
                shadow(scope, cx, groundY, 11f * p, p)
                when {
                    state == CatState.SLEEPING -> draw(scope, FOX_SLEEP, FOX_COLORS, cx, groundY, p, facingLeft)
                    walking -> draw(scope, if (((time * 8f).toInt() and 1) == 0) FOX_A else FOX_B, FOX_COLORS, cx, groundY, p, facingLeft)
                    playing -> {
                        // Pouncing about with its ball
                        val lift = abs(sin(time * 5f)) * 4f * p
                        draw(scope, FOX_A, FOX_COLORS, cx, groundY, p, facingLeft, lift)
                        val bx = cx + (if (facingLeft) -9f else 9f) * p
                        scope.drawRect(Color(0xFFE63946), Offset(bx - 1.5f * p, groundY - 3f * p), Size(3f * p, 3f * p))
                        scope.drawRect(Color(0xFFFFD3DC), Offset(bx - 1.5f * p, groundY - 3f * p), Size(p, p))
                    }
                    else -> draw(scope, FOX_SIT, FOX_COLORS, cx, groundY, p, facingLeft)
                }
            }
            PetKind.HEDGEHOG -> {
                shadow(scope, cx, groundY, 8f * p, p)
                when {
                    state == CatState.SLEEPING -> draw(scope, HEDGEHOG_BALL, HEDGEHOG_COLORS, cx, groundY, p, facingLeft)
                    playing -> draw(scope, HEDGEHOG_BALL, HEDGEHOG_COLORS, cx + sin(time * 4f) * 2f * p, groundY, p, ((time * 4f).toInt() and 1) == 0, abs(sin(time * 8f)) * p)
                    walking -> draw(scope, if (((time * 3f).toInt() and 1) == 0) HEDGEHOG_A else HEDGEHOG_B, HEDGEHOG_COLORS, cx, groundY, p, facingLeft)
                    else -> draw(scope, HEDGEHOG_A, HEDGEHOG_COLORS, cx, groundY, p, facingLeft)
                }
            }
            PetKind.DUCK -> {
                val asleep = state == CatState.SLEEPING
                // Her three ducklings, always just behind her
                val behind = if (facingLeft) 1f else -1f
                for (i in 1..3) {
                    val gap = (if (asleep) 5f else 8f) * i * p
                    val dx = cx + behind * gap
                    val rows = when {
                        asleep -> DUCKLING_SLEEP
                        walking -> if ((((time * 4f).toInt() + i) and 1) == 0) DUCKLING_A else DUCKLING_B
                        else -> DUCKLING_A
                    }
                    shadow(scope, dx, groundY, 4f * p, p)
                    draw(scope, rows, DUCK_COLORS, dx, groundY, p, facingLeft)
                }
                shadow(scope, cx, groundY, 7f * p, p)
                when {
                    asleep -> draw(scope, DUCK_SLEEP, DUCK_COLORS, cx, groundY, p, facingLeft)
                    walking -> draw(scope, if (((time * 4f).toInt() and 1) == 0) DUCK_A else DUCK_B, DUCK_COLORS, cx, groundY, p, facingLeft)
                    playing -> draw(scope, DUCK_A, DUCK_COLORS, cx, groundY, p, facingLeft, abs(sin(time * 6f)) * 2f * p)
                    else -> draw(scope, DUCK_A, DUCK_COLORS, cx, groundY, p, facingLeft)
                }
            }
            PetKind.OWL -> {
                shadow(scope, cx, groundY, 6f * p, p)
                val blink = (time % 3.7f) < 0.2f
                when {
                    state == CatState.SLEEPING -> draw(scope, OWL_CLOSED, OWL_COLORS, cx, groundY, p, facingLeft)
                    walking -> draw(scope, OWL, OWL_COLORS, cx, groundY, p, facingLeft, abs(sin(time * 6f)) * 2f * p)
                    playing -> draw(scope, if (step) OWL_FLAP else OWL, OWL_COLORS, cx, groundY, p, facingLeft, abs(sin(time * 4f)) * 3f * p)
                    else -> draw(scope, if (blink) OWL_CLOSED else OWL, OWL_COLORS, cx, groundY, p, facingLeft)
                }
            }
        }
        // A sleeping pet breathes out a little z now and then
        if (state == CatState.SLEEPING && kind != PetKind.CAT && ((time * 0.8f).toInt() % 2 == 0)) {
            val zx = cx + (if (facingLeft) -4f else 4f) * p
            val rise = (time % 1.25f) * 4f * worldP
            scope.drawRect(Color.White.copy(alpha = 0.7f), Offset(zx, groundY - 9f * p - rise), Size(2f * worldP, worldP * 0.6f))
        }
    }

    // ── Plan 12, A: the pets' moves ───────────────────────────────────────

    /**
     * [rows] drawn like [draw], with the front of the head dipped [headDrop] rows (a sniff, a
     * dig, a stretch) or raised when negative (a yawn), all on whole pixels.
     */
    private fun drawRows(scope: DrawScope, rows: List<String>, colors: Map<Char, Color>, cx: Float, groundY: Float, p: Float, flip: Boolean, lift: Float = 0f, headDrop: Int = 0, replace: Map<Char, Char> = emptyMap()) {
        val w = rows.maxOf { it.length }
        val left = cx - w * p / 2f
        val top = groundY - rows.size * p - lift
        for ((y, row) in rows.withIndex()) {
            for ((x, ch0) in row.withIndex()) {
                val ch = replace[ch0] ?: ch0
                val c = colors[ch] ?: continue
                val col = if (flip) w - 1 - x else x
                val dy = if (headDrop != 0 && x >= w * 0.6f) headDrop else 0
                scope.drawRect(c, Offset(left + col * p, top + (y + dy) * p), Size(p, p))
            }
        }
    }

    /** One pixel of [rows]' grid, for little extras drawn on top (a paw, a hole, a wing). */
    private fun cell(scope: DrawScope, rows: List<String>, cx: Float, groundY: Float, p: Float, flip: Boolean, lift: Float, col: Int, row: Int, c: Color) {
        val w = rows.maxOf { it.length }
        val left = cx - w * p / 2f
        val top = groundY - rows.size * p - lift
        val x = if (flip) w - 1 - col else col
        scope.drawRect(c, Offset(left + x * p, top + row * p), Size(p, p))
    }

    private class Look(val sit: List<String>, val walkA: List<String>, val walkB: List<String>, val rest: List<String>, val colors: Map<Char, Color>, val width: Float)

    private fun lookOf(kind: PetKind): Look? = when (kind) {
        PetKind.PUPPY -> Look(PUPPY_SIT, PUPPY_WALK_A, PUPPY_WALK_B, PUPPY_SLEEP, PUPPY_COLORS, 13f)
        PetKind.BUNNY -> Look(BUNNY, BUNNY_HOP, BUNNY, BUNNY_SLEEP, BUNNY_COLORS, 8f)
        PetKind.FOX -> Look(FOX_SIT, FOX_A, FOX_B, FOX_SLEEP, FOX_COLORS, 11f)
        PetKind.HEDGEHOG -> Look(HEDGEHOG_A, HEDGEHOG_A, HEDGEHOG_B, HEDGEHOG_BALL, HEDGEHOG_COLORS, 8f)
        PetKind.DUCK -> Look(DUCK_A, DUCK_A, DUCK_B, DUCK_SLEEP, DUCK_COLORS, 7f)
        PetKind.OWL -> Look(OWL, OWL, OWL_FLAP, OWL_CLOSED, OWL_COLORS, 6f)
        PetKind.CAT -> null
    }

    /**
     * Draws [kind] (not the cat: see [drawCatMove]) doing [move], [t] seconds in, its feet on
     * [groundY] at the world's pixel size [worldP].
     */
    fun drawMove(scope: DrawScope, kind: PetKind, move: com.example.scene.PetMove, t: Float, cx: Float, groundY: Float, worldP: Float, facingLeft: Boolean) {
        val look = lookOf(kind) ?: return
        val p = worldP * scaleOf(kind)
        val f = (t / move.seconds).coerceIn(0f, 1f)
        val beat = ((t * 8f).toInt() and 1) == 0
        val ahead = if (facingLeft) -1f else 1f
        drawContactShadow(scope, cx, groundY, (look.width).roundToInt(), p)
        when (move) {
            com.example.scene.PetMove.STRETCH -> {
                // Front down low, a long reach, and up again
                val drop = (sin(PI.toFloat() * f) * 2.4f).roundToInt()
                drawRows(scope, look.walkA, look.colors, cx, groundY, p, facingLeft, headDrop = drop)
            }
            com.example.scene.PetMove.GROOM -> {
                // A preen or a wash: the head dips to the chest, a beat at a time
                val rows = if (kind == PetKind.OWL || kind == PetKind.DUCK) look.sit else look.sit
                drawRows(scope, rows, look.colors, cx, groundY, p, facingLeft, headDrop = if (beat) 1 else 0)
                if (kind == PetKind.BUNNY) {
                    // Paws up to the face
                    cell(scope, rows, cx, groundY, p, facingLeft, 0f, 6, if (beat) 2 else 3, Color(0xFFD9CFC4))
                }
            }
            com.example.scene.PetMove.SCRATCH -> {
                // Sitting, a back foot going at the ear
                drawRows(scope, look.sit, look.colors, cx, groundY, p, facingLeft)
                val w = look.sit.maxOf { it.length }
                val foot = (w * 0.55f).toInt()
                // the paw in a colour that shows against the head
                val fur = when (kind) {
                    PetKind.PUPPY -> look.colors.getValue('W')
                    PetKind.FOX -> look.colors.getValue('d')
                    else -> look.colors.values.first()
                }
                cell(scope, look.sit, cx, groundY, p, facingLeft, 0f, foot, if (beat) 1 else 2, fur)
                cell(scope, look.sit, cx, groundY, p, facingLeft, 0f, foot - 1, if (beat) 2 else 3, fur)
            }
            com.example.scene.PetMove.CHASE_TAIL -> {
                // Round and round: facing one way, then the other
                val turn = ((t / 0.18f).toInt() and 1) == 0
                val dx = sin(t * 11f) * 2f * p
                drawRows(scope, if (beat) look.walkA else look.walkB, look.colors, cx + dx, groundY, p, turn, lift = abs(sin(t * 11f)) * p)
            }
            com.example.scene.PetMove.SNIFF -> {
                // Nose down, sniffing in little dips
                drawRows(scope, look.walkA, look.colors, cx, groundY, p, facingLeft, headDrop = if (((t * 5f).toInt() and 1) == 0) 1 else 2)
            }
            com.example.scene.PetMove.DIG -> {
                // Head down, paws going, a hole getting deeper in front
                val hole = (1 + f * 3f).toInt()
                val w = look.walkA.maxOf { it.length }
                for (i in 0 until hole) {
                    scope.drawRect(Color(0xFF5A3A22), Offset(cx + ahead * (w / 2f + 0.5f) * p - p / 2f + ahead * i * p * 0.5f, groundY - p * 0.5f), Size(p, p * 0.8f))
                }
                drawRows(scope, if (beat) look.walkA else look.walkB, look.colors, cx, groundY, p, facingLeft, headDrop = 2)
            }
            com.example.scene.PetMove.POUNCE -> {
                if (f < 0.35f) {
                    // The crouch, a wiggle
                    val wiggle = if (beat) p else 0f
                    drawRows(scope, look.walkA, look.colors, cx + wiggle * 0.5f, groundY, p, facingLeft, headDrop = 1)
                } else {
                    // The leap, nose first at the end
                    val g = (f - 0.35f) / 0.65f
                    val lift = sin(PI.toFloat() * g) * 8f * p
                    drawRows(scope, look.walkB, look.colors, cx + ahead * g * 8f * worldP, groundY, p, facingLeft, lift = lift, headDrop = if (g > 0.55f) 2 else -1)
                }
            }
            com.example.scene.PetMove.SHAKE -> {
                // A shiver from nose to tail
                val dx = if (((t * 20f).toInt() and 1) == 0) p else -p
                drawRows(scope, look.sit, look.colors, cx + dx, groundY, p, facingLeft)
            }
            com.example.scene.PetMove.YAWN -> {
                // Head up and a big open yawn
                val wide = f in 0.25f..0.75f
                val rows = if (kind == PetKind.OWL) look.rest else look.sit
                drawRows(scope, rows, look.colors, cx, groundY, p, facingLeft, headDrop = if (wide) -1 else 0)
            }
            com.example.scene.PetMove.FLOP -> {
                // Over onto one side with a little bounce, and a contented lie
                val bounce = if (f < 0.12f) sin(PI.toFloat() * f / 0.12f) * 2f * p else 0f
                drawRows(scope, look.rest, look.colors, cx, groundY, p, facingLeft, lift = bounce)
            }
            com.example.scene.PetMove.HOP -> {
                // Two happy hops
                val lift = abs(sin(2f * PI.toFloat() * f)) * 5f * p
                drawRows(scope, if (lift > p) look.walkA else look.sit, look.colors, cx, groundY, p, facingLeft, lift = lift)
            }
            com.example.scene.PetMove.HEAD_TURN -> {
                // The owl's head goes right round: for a moment only the back of it shows
                val back = f in 0.3f..0.7f
                drawRows(scope, look.sit, look.colors, cx, groundY, p, if (f > 0.5f) !facingLeft else facingLeft,
                    replace = if (back) mapOf('Y' to 'B', 'K' to 'B') else emptyMap())
            }
            com.example.scene.PetMove.FLAP -> {
                // Wings out, a few flaps and a lift
                val lift = sin(PI.toFloat() * f) * 4f * p
                if (kind == PetKind.OWL) {
                    drawRows(scope, if (beat) look.walkB else look.sit, look.colors, cx, groundY, p, facingLeft, lift = lift)
                } else {
                    drawRows(scope, look.walkA, look.colors, cx, groundY, p, facingLeft, lift = lift)
                    // a wing raised over the back
                    val wing = Color(0xFFF4F1EA)
                    for (i in 1..3) cell(scope, look.walkA, cx, groundY, p, facingLeft, lift, i, if (beat) 1 else 2, wing)
                }
            }
            com.example.scene.PetMove.PARADE -> {
                // Mother duck waddles to and fro, her three ducklings marching after her
                for (i in 3 downTo 0) {
                    val phase = 2f * PI.toFloat() * f - i * 0.55f
                    val x = cx + sin(phase) * 9f * p
                    val left = cos(phase) < 0f
                    if (i == 0) {
                        drawRows(scope, if (beat) DUCK_A else DUCK_B, DUCK_COLORS, x, groundY, p, left)
                    } else {
                        drawContactShadow(scope, x, groundY, 4, p)
                        drawRows(scope, if ((((t * 6f).toInt() + i) and 1) == 0) DUCKLING_A else DUCKLING_B, DUCK_COLORS, x, groundY, p, left)
                    }
                }
            }
            com.example.scene.PetMove.ROLL -> {
                // Curled into a ball, rolling along (the engine carries it)
                val spin = ((t * 10f).toInt() and 1) == 0
                drawRows(scope, HEDGEHOG_BALL, HEDGEHOG_COLORS, cx, groundY, p, spin, lift = abs(sin(t * 12f)) * p)
            }
            com.example.scene.PetMove.ZOOMIES -> {
                drawRows(scope, if (((t * 16f).toInt() and 1) == 0) look.walkA else look.walkB, look.colors, cx, groundY, p, facingLeft)
            }
        }
    }

    /** True for the pets that are drawn by [drawPet] (everyone but Mochi). */
    fun drawsKind(kind: PetKind) = kind != PetKind.CAT
}

/** A pet they haven't met yet, out in the world (plan 10, E); the owl on a little post. */
fun drawPetVisitor(scope: DrawScope, engine: com.example.scene.SceneEngine, cw: Float, ch: Float, p: Float) {
    val v = engine.petVisitor ?: return
    val x = cw * v.x
    val ground = ch * v.y
    var feet = ground
    if (v.kind == PetKind.OWL && !v.met) {
        // A fence post to sit on, as on the website
        scope.drawRect(Color(0xFF5C3A24), Offset(x - 2f * p, ground - 12f * p), Size(4f * p, 12f * p))
        scope.drawRect(Color(0xFF7A5236), Offset(x - 3f * p, ground - 13f * p), Size(6f * p, 2f * p))
        feet = ground - 13f * p
    }
    PetSprites.drawPet(scope, v.kind, x, feet, p, v.age, v.state, v.facingLeft, night = engine.timeOfDayPhase.isNight)
}

/**
 * Mochi doing [move] (plan 12, A), drawn from her own poses: the crouch held still for a
 * stretch, a paw to her face for a wash, a turn after her tail, a leap, a yawn, a flop onto her
 * back, a hop. [t] is how far into the move she is; [time] the scene's clock.
 */
fun drawCatMove(
    scope: DrawScope,
    move: com.example.scene.PetMove,
    t: Float,
    cx: Float,
    groundY: Float,
    p: Float,
    time: Float,
    facingLeft: Boolean,
    isSnow: Boolean,
    collarStyle: Int
) {
    val f = (t / move.seconds).coerceIn(0f, 1f)
    val ahead = if (facingLeft) -1f else 1f
    fun cat(state: CatState, x: Float = cx, lift: Float = 0f, face: Boolean = facingLeft, clock: Float = time) =
        com.example.engine.WorldSprites.drawCat(
            scope = scope, cx = x, groundY = groundY - lift, p = p, timeSeconds = clock, catState = state,
            isSleeping = false, isSnow = isSnow, facingLeft = face, collarStyle = collarStyle
        )
    // The sitting pose's face, for a paw or an open mouth over it
    val headTop = groundY - 13f * p
    when (move) {
        com.example.scene.PetMove.STRETCH -> cat(CatState.PLAYFUL_POUNCE, clock = 0f)
        com.example.scene.PetMove.GROOM -> {
            cat(CatState.SITTING_PURR)
            val up = sin(t * 10f) > 0f
            val pawTop = headTop + (if (up) 2f else 3f) * p
            scope.drawRect(Color(0xFFCED4DA), Offset(cx - 1.6f * p, pawTop), Size(3.2f * p, 3.4f * p))
            scope.drawRect(Color(0xFFF8F9FA), Offset(cx - 1.2f * p, pawTop), Size(2.4f * p, 3f * p))
            scope.drawRect(Color(0xFFFFCAD4), Offset(cx - 0.5f * p, pawTop + 0.4f * p), Size(p, 0.8f * p))
            if (up) scope.drawRect(Color(0xFFFF8FA3), Offset(cx - 0.5f * p, headTop + 1.6f * p), Size(p, 0.8f * p))
        }
        com.example.scene.PetMove.CHASE_TAIL -> {
            val turn = ((t / 0.18f).toInt() and 1) == 0
            cat(CatState.WALK_FOLLOW, x = cx + sin(t * 11f) * 2f * p, face = turn, clock = time * 2f)
        }
        com.example.scene.PetMove.POUNCE -> if (f < 0.35f) {
            cat(CatState.PLAYFUL_POUNCE)
        } else {
            val g = (f - 0.35f) / 0.65f
            cat(CatState.WALK_FOLLOW, x = cx + ahead * g * 8f * p, lift = sin(PI.toFloat() * g) * 9f * p, clock = 0.2f)
        }
        com.example.scene.PetMove.YAWN -> {
            cat(CatState.SITTING_PURR)
            if (f in 0.2f..0.8f) {
                scope.drawRect(Color(0xFF6B2737), Offset(cx - 1.1f * p, headTop + 2.6f * p), Size(2.2f * p, 1.8f * p))
                scope.drawRect(Color(0xFFFF8FA3), Offset(cx - 0.6f * p, headTop + 3.4f * p), Size(1.2f * p, 0.9f * p))
            }
        }
        com.example.scene.PetMove.SNIFF -> cat(CatState.PLAYFUL_POUNCE, clock = t * 0.25f)
        com.example.scene.PetMove.SHAKE -> cat(CatState.SITTING_PURR, x = cx + if (((t * 20f).toInt() and 1) == 0) p else -p)
        com.example.scene.PetMove.FLOP -> cat(CatState.BELLY_ROLL)
        com.example.scene.PetMove.HOP -> cat(CatState.SITTING_PURR, lift = abs(sin(2f * PI.toFloat() * f)) * 5f * p)
        com.example.scene.PetMove.ZOOMIES -> cat(CatState.WALK_FOLLOW, clock = time * 2f)
        else -> cat(CatState.SITTING_PURR)
    }
}
