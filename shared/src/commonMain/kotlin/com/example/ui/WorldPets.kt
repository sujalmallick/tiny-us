package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.PetKind
import com.example.engine.drawCastShadow
import com.example.engine.drawContactShadow
import com.example.scene.CatState
import kotlin.math.abs
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

    private fun shadow(
        scope: DrawScope,
        cx: Float,
        groundY: Float,
        width: Float,
        p: Float,
        sunProgress: Float? = null,
        isOutdoor: Boolean = true,
        isNight: Boolean = false,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
        localLightX: Float? = null,
        minGroundY: Float? = null,
        maxGroundY: Float? = null
    ) {
        val widthPx = (width / p).roundToInt()
        drawCastShadow(
            scope = scope,
            centerX = cx,
            groundY = groundY,
            widthPx = widthPx,
            p = p,
            sunProgress = sunProgress,
            isOutdoor = isOutdoor,
            isNight = isNight,
            weather = weather,
            localLightX = localLightX,
            minGroundY = minGroundY,
            maxGroundY = maxGroundY
        )
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
        sunProgress: Float? = null,
        isOutdoor: Boolean = true,
        weather: com.example.scene.WeatherType = com.example.scene.WeatherType.SUNNY,
        localLightX: Float? = null,
        minGroundY: Float? = null,
        maxGroundY: Float? = null
    ) {
        fun shadow(s: DrawScope, scx: Float, sgroundY: Float, swidth: Float, sp: Float) =
            this.shadow(s, scx, sgroundY, swidth, sp, sunProgress, isOutdoor, night, weather, localLightX, minGroundY, maxGroundY)
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
