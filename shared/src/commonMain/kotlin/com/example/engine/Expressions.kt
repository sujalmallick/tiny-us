package com.example.engine

import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/**
 * A passing look on a character's face (plan 12, B). It shows for a few seconds over their
 * [CharacterEmotion] and then fades back to their usual face; see [PixelCharacter.express].
 */
enum class Expression {
    NONE,
    /** A big open smile, eyes bright. */
    GRIN,
    /** Eyes down, a wide blush, a tiny mouth. */
    SHY,
    /** Eyes to the side, one brow up, a small "o". */
    CURIOUS,
    /** One eye shut, tongue out. */
    WINK,
    /** Eyes squeezed shut, mouth wide open, a little giggle bob. */
    LAUGH,
    /** Twinkling star eyes. */
    STARRY,
    /** Shining eyes and a happy tear. */
    TEARY,
    /** Puffed cheeks, a frown, a sideways glare. */
    POUT,
    /** Half-lidded eyes and a sideways smirk. */
    SMUG,
    /** Brows up in the middle, a wobbly mouth, a drop of sweat. */
    WORRIED,
    /** Little pink hearts for eyes. */
    HEART_EYES,
    /** Eyes narrowed, the tip of the tongue out: concentrating. */
    FOCUSED,
    /** Eyes softly closed, a content smile. */
    BLISS
}

/** How long an expression shows when nothing says otherwise. */
const val EXPRESSION_SECONDS = 2.6f

/** The look that goes with each feeling, shown briefly whenever a character's emotion changes. */
fun expressionFor(emotion: CharacterEmotion): Expression = when (emotion) {
    CharacterEmotion.HAPPY -> Expression.GRIN
    CharacterEmotion.SHY -> Expression.SHY
    CharacterEmotion.CURIOUS -> Expression.CURIOUS
    CharacterEmotion.PLAYFUL -> Expression.WINK
    else -> Expression.NONE
}

/**
 * Paints [e] on the face of the 18 x 26 character sprite, replacing the usual eyes, cheeks and
 * mouth. [px] puts one sprite pixel at grid (x, y) and already handles facing and breathing, so
 * "forward" is always +x. [dy] is 1 for the seated sprite, whose face sits a row lower and has no
 * jaw row. [t] runs on for the little animations (twinkles, the tear, the sweat drop).
 */
internal fun drawExpressionFace(
    px: (Int, Int, Color) -> Unit,
    e: Expression,
    dy: Int,
    skin: Color,
    skinShadow: Color,
    hair: Color,
    blinking: Boolean,
    t: Float
) {
    val ink = PixelArtRenderer.EyeDark
    val shine = PixelArtRenderer.EyeSparkle
    val rose = PixelArtRenderer.BlushRose
    val coral = PixelArtRenderer.BlushCoral
    val mouthRed = Color(0xFFC9184A)
    val tongue = Color(0xFFFF8FA3)
    val tearBlue = Color(0xFF9BD7FF)
    val hasJaw = dy == 0
    fun p(x: Int, y: Int, c: Color) {
        if (!hasJaw && y >= 11) return
        px(x, y + dy, c)
    }
    fun rect(x: Int, y: Int, w: Int, h: Int, c: Color) {
        for (i in 0 until w) for (j in 0 until h) p(x + i, y + j, c)
    }
    fun cheeks(c: Color, wide: Boolean) {
        rect(6, 9, if (wide) 2 else 1, 1, c)
        rect(11, 9, if (wide) 2 else 1, 1, c)
    }
    fun blinkEyes() {
        rect(7, 9, 2, 1, ink)
        rect(10, 9, 2, 1, ink)
    }
    /** Both eyes open and bright. */
    fun openEyes() {
        if (blinking) { blinkEyes(); return }
        rect(7, 8, 2, 2, ink); rect(10, 8, 2, 2, ink)
        p(7, 8, shine); p(10, 8, shine)
    }
    /** A wide open smile: the corners turned up, red inside. */
    fun bigSmile() {
        p(7, 10, ink); p(11, 10, ink)
        rect(8, 10, 3, 1, mouthRed)
        rect(8, 11, 3, 1, ink)
    }
    /** A small closed smile. */
    fun smallSmile() {
        p(8, 10, ink); p(10, 10, ink)
        if (hasJaw) p(9, 11, ink) else p(9, 10, ink)
    }
    /** Eyes shut in two happy upward arcs. */
    fun arcEyes() {
        p(7, 9, ink); p(8, 8, ink); p(9, 9, ink)
        p(10, 9, ink); p(11, 8, ink); p(12, 9, ink)
    }
    // A clean face to paint on: the usual eyes, cheeks and mouth would show through otherwise.
    rect(6, 7, 7, 4, skin)
    if (hasJaw) rect(7, 11, 5, 1, skin)
    p(6, 10, skinShadow); p(12, 10, skinShadow)

    when (e) {
        Expression.NONE -> Unit
        Expression.GRIN -> {
            openEyes()
            cheeks(coral, wide = true)
            bigSmile()
        }
        Expression.LAUGH -> {
            arcEyes()
            cheeks(rose, wide = true)
            bigSmile()
        }
        Expression.SHY -> {
            // Eyes lowered, a deep blush, a tiny mouth
            rect(7, 9, 2, 1, ink); rect(10, 9, 2, 1, ink)
            cheeks(rose, wide = true)
            p(6, 10, rose); p(12, 10, rose)
            p(9, 10, ink)
        }
        Expression.CURIOUS -> {
            // Wide eyes glancing ahead, a small "o"
            if (blinking) blinkEyes() else {
                p(7, 8, Color.White); p(7, 9, Color.White); rect(8, 8, 1, 2, ink)
                p(10, 8, Color.White); p(10, 9, Color.White); rect(11, 8, 1, 2, ink)
            }
            cheeks(coral, wide = false)
            p(10, 10, ink)
        }
        Expression.WINK -> {
            // The back eye shut, the front one bright, the tip of the tongue out
            rect(7, 9, 2, 1, ink)
            rect(10, 8, 2, 2, ink); p(10, 8, shine)
            cheeks(coral, wide = true)
            smallSmile()
            p(9, 10, tongue)
        }
        Expression.STARRY -> {
            // Two little stars that twinkle between gold and white
            val twinkle = sin(t * 9f) > 0f
            val star = if (twinkle) Color(0xFFFFD166) else Color(0xFFFFE9A8)
            val core = if (twinkle) Color.White else Color(0xFFFFD166)
            for ((cx, cy) in listOf(8 to 8, 11 to 8)) {
                p(cx, cy - 1, star); p(cx - 1, cy, star); p(cx + 1, cy, star); p(cx, cy + 1, star)
                p(cx, cy, core)
            }
            cheeks(coral, wide = false)
            bigSmile()
        }
        Expression.TEARY -> {
            // Shining eyes, happy tears running down, a smile
            rect(7, 8, 2, 2, ink); rect(10, 8, 2, 2, ink)
            p(7, 8, shine); p(10, 8, shine)
            p(8, 9, tearBlue); p(11, 9, tearBlue)
            val run = ((t * 2.4f) % 2f).toInt()
            p(7, 10, tearBlue); p(7, 10 + run, tearBlue)
            cheeks(rose, wide = false)
            smallSmile()
        }
        Expression.POUT -> {
            // Sulking: lids lowered, cheeks puffed out, lips pursed
            rect(7, 8, 2, 1, skinShadow); rect(10, 8, 2, 1, skinShadow)
            rect(7, 9, 2, 1, ink); rect(10, 9, 2, 1, ink)
            cheeks(rose, wide = true)
            p(5, 9, skin); p(5, 10, skin); p(13, 9, skin); p(13, 10, skin)
            p(9, 10, mouthRed)
        }
        Expression.SMUG -> {
            // One brow up, lids down, a lopsided smirk
            p(10, 7, ink); p(11, 7, ink)
            rect(7, 9, 2, 1, ink); rect(10, 9, 2, 1, ink)
            p(7, 8, skinShadow); p(8, 8, skinShadow)
            cheeks(coral, wide = false)
            if (hasJaw) { p(9, 11, ink); p(10, 11, ink); p(11, 10, ink) } else { p(9, 10, ink); p(10, 10, ink); p(11, 9, ink) }
        }
        Expression.WORRIED -> {
            // Wide eyes glancing at each other, a little frown, a bead of sweat
            if (blinking) blinkEyes() else {
                p(7, 8, Color.White); rect(8, 8, 1, 2, ink); p(7, 9, Color.White)
                p(11, 8, Color.White); rect(10, 8, 1, 2, ink); p(11, 9, Color.White)
            }
            cheeks(coral, wide = false)
            if (hasJaw) { p(8, 11, ink); p(9, 10, ink); p(10, 11, ink) } else p(9, 10, ink)
            val drop = if (sin(t * 3f) > -0.3f) 1 else 0
            p(13, 7 + drop, tearBlue); p(13, 8 + drop, tearBlue)
        }
        Expression.HEART_EYES -> {
            // Pink hearts that beat
            val heart = if (sin(t * 8f) > 0f) Color(0xFFFF4D6D) else Color(0xFFFF758F)
            for (left in listOf(6, 10)) {
                p(left, 7, heart); p(left + 2, 7, heart)
                rect(left, 8, 3, 1, heart)
                p(left + 1, 9, heart)
            }
            cheeks(rose, wide = false)
            bigSmile()
        }
        Expression.FOCUSED -> {
            // Eyes on the task, brows knitted, the tip of the tongue poking out
            rect(7, 8, 2, 1, skinShadow); rect(10, 8, 2, 1, skinShadow)
            rect(7, 9, 2, 1, ink); rect(10, 9, 2, 1, ink)
            p(8, 7, ink); p(10, 7, ink)
            p(8, 10, ink); p(9, 10, ink); p(10, 10, tongue)
        }
        Expression.BLISS -> {
            // Eyes softly shut in happy arcs, rosy cheeks, a small smile
            arcEyes()
            cheeks(rose, wide = true)
            smallSmile()
        }
    }
}
