package com.example.engine

import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/**
 * A passing look on a character's face (plan 12, B). It shows for a few seconds over their
 * [CharacterEmotion] and then fades back to their usual face; see [PixelCharacter.express].
 */
enum class Expression {
    NONE,
    /** Bright eyes and an open smile. */
    GRIN,
    /** Eyes lowered, a deep blush, a tiny mouth. */
    SHY,
    /** Eyes glancing ahead, one brow up. */
    CURIOUS,
    /** One eye shut, the tip of the tongue out. */
    WINK,
    /** Eyes shut in happy arcs, mouth wide open, a little giggle bob. */
    LAUGH,
    /** Twinkling star eyes. */
    STARRY,
    /** Shining eyes and tears running down. */
    TEARY,
    /** Lids lowered, cheeks puffed out, lips pursed. */
    POUT,
    /** One brow up, a half-shut eye, a smirk. */
    SMUG,
    /** Wide eyes, brows up in the middle, a little frown, a drop of sweat. */
    WORRIED,
    /** Little pink hearts for eyes. */
    HEART_EYES,
    /** Brows knitted, the tip of the tongue out: concentrating. */
    FOCUSED,
    /** Eyes softly shut, a content smile. */
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
 * jaw row. [t] runs on for the little animations (twinkles, the tears, the sweat drop).
 *
 * Every face keeps to the same rules, so each one reads at the size it's really shown:
 * - the face is symmetric about x = 9 (skin x 6..12, rows 7..10, the jaw row 11 at x 7..11);
 * - open eyes are 2 x 2 at x 7-8 and 10-11, rows 8-9, with a catchlight top-left;
 * - shut eyes are the game's own arcs, raised to rows 7-8 so a clear row sits above the mouth;
 * - blush goes at x 6 and 12, never inside an eye;
 * - under open eyes a dark mouth keeps to the jaw row, and only x 9 or soft colours touch row 10,
 *   so the mouth never runs into the eyes.
 */
internal fun drawExpressionFace(
    px: (Int, Int, Color) -> Unit,
    e: Expression,
    dy: Int,
    skin: Color,
    skinShadow: Color,
    blinking: Boolean,
    t: Float
) {
    val ink = PixelArtRenderer.EyeDark
    val shine = PixelArtRenderer.EyeSparkle
    val rose = PixelArtRenderer.BlushRose
    val coral = PixelArtRenderer.BlushCoral
    val mouthRed = Color(0xFFC9184A)
    val tongue = Color(0xFFFF8FA3)
    val tearBlue = Color(0xFF7CC8FF)
    val white = Color.White
    val hasJaw = dy == 0
    fun p(x: Int, y: Int, c: Color) {
        if (!hasJaw && y >= 11) return
        px(x, y + dy, c)
    }
    fun rect(x: Int, y: Int, w: Int, h: Int, c: Color) {
        for (i in 0 until w) for (j in 0 until h) p(x + i, y + j, c)
    }

    /** Both eyes shut, relaxed: a blink. */
    fun lids() { rect(7, 9, 2, 1, ink); rect(10, 9, 2, 1, ink) }
    /** Both eyes open and bright. */
    fun eyes() {
        if (blinking) { lids(); return }
        rect(7, 8, 2, 2, ink); rect(10, 8, 2, 2, ink)
        p(7, 8, shine); p(10, 8, shine)
    }
    /** Eyes shut in happy arcs, raised clear of the mouth. */
    fun arcs() {
        p(7, 8, ink); p(8, 7, ink); p(9, 8, ink)
        p(10, 8, ink); p(11, 7, ink); p(12, 8, ink)
    }
    /** Heavy lids: the top of each eye in shadow, the eye a line below it. */
    fun lowLids() { rect(7, 8, 2, 1, skinShadow); rect(10, 8, 2, 1, skinShadow); lids() }
    /** Cheeks at the edges of the face; [strong] spreads them a row lower. */
    fun blush(c: Color, strong: Boolean = false) {
        p(6, 9, c); p(12, 9, c)
        if (strong) { p(6, 10, c); p(12, 10, c) }
    }
    /** With the eyes shut and raised, the blush can spread under them. */
    fun blushUnderArcs(c: Color) { rect(6, 9, 2, 1, c); rect(11, 9, 2, 1, c) }
    /** An open smile, red: a point on row 10, wide on the jaw. */
    fun openSmile() {
        if (hasJaw) { p(9, 10, mouthRed); rect(8, 11, 3, 1, mouthRed) } else rect(8, 10, 3, 1, mouthRed)
    }
    /** A soft closed smile in rose. */
    fun softSmile() {
        if (hasJaw) { p(8, 10, rose); p(9, 11, rose); p(10, 10, rose) } else p(9, 10, rose)
    }

    // A clean face to paint on: the usual eyes, cheeks and mouth would show through otherwise.
    rect(6, 7, 7, 4, skin)
    if (hasJaw) rect(7, 11, 5, 1, skin)
    p(6, 10, skinShadow); p(12, 10, skinShadow)

    when (e) {
        Expression.NONE -> Unit
        Expression.GRIN -> {
            eyes()
            blush(coral, strong = true)
            openSmile()
        }
        Expression.LAUGH -> {
            // ^ ^ and a wide-open laugh
            arcs()
            blushUnderArcs(rose)
            rect(8, 10, 3, 1, mouthRed)
            if (hasJaw) rect(8, 11, 3, 1, ink)
        }
        Expression.SHY -> {
            // Eyes lowered, a deep blush, a tiny mouth
            lowLids()
            blush(rose, strong = true)
            p(9, 10, ink)
        }
        Expression.CURIOUS -> {
            // Eyes glancing ahead, the front brow up: "hm?"
            if (blinking) lids() else {
                p(7, 8, white); p(7, 9, white); rect(8, 8, 1, 2, ink)
                p(10, 8, white); p(10, 9, white); rect(11, 8, 1, 2, ink)
            }
            p(11, 7, ink); p(12, 7, ink)
            blush(coral)
            p(9, 10, ink)
        }
        Expression.WINK -> {
            // The back eye shut, the front one bright, a smile with the tip of the tongue
            rect(7, 9, 2, 1, ink)
            rect(10, 8, 2, 2, ink); p(10, 8, shine)
            blush(coral, strong = true)
            softSmile()
            if (hasJaw) p(9, 11, tongue)
        }
        Expression.STARRY -> {
            // Two little stars that twinkle between gold and white
            val twinkle = sin(t * 9f) > 0f
            val star = if (twinkle) Color(0xFFFFD166) else Color(0xFFFFE9A8)
            val core = if (twinkle) white else Color(0xFFFFD166)
            for (cx in intArrayOf(8, 11)) {
                p(cx, 7, star); p(cx - 1, 8, star); p(cx + 1, 8, star); p(cx, 9, star)
                p(cx, 8, core)
            }
            p(9, 8, skin)
            blush(coral)
            openSmile()
        }
        Expression.TEARY -> {
            // Shining eyes and tears running down both cheeks
            eyes()
            p(8, 9, tearBlue); p(11, 9, tearBlue)
            val run = ((t * 2.4f) % 2f).toInt()
            p(7, 10, tearBlue); p(11, 10, tearBlue)
            p(7, 10 + run, tearBlue); p(11, 10 + run, tearBlue)
            blush(rose)
            p(9, 10, ink)
        }
        Expression.POUT -> {
            // Sulking: lids lowered, cheeks puffed out, lips pursed
            lowLids()
            blush(rose, strong = true)
            p(5, 9, skin); p(5, 10, skin); p(13, 9, skin); p(13, 10, skin)
            p(9, 10, mouthRed)
        }
        Expression.SMUG -> {
            // One brow up, the back eye half shut, a smirk up one side
            p(7, 8, skinShadow); p(8, 8, skinShadow); lids()
            p(10, 7, ink); p(11, 7, ink)
            blush(coral)
            if (hasJaw) { p(9, 11, ink); p(10, 11, ink); p(11, 10, ink) } else { p(9, 10, ink); p(10, 10, ink) }
        }
        Expression.WORRIED -> {
            // Wide eyes with small pupils, brows up in the middle, a little frown, a bead of sweat
            if (blinking) lids() else {
                rect(7, 8, 2, 2, white); rect(10, 8, 2, 2, white)
                p(8, 9, ink); p(10, 9, ink)
            }
            p(8, 7, ink); p(10, 7, ink)
            blush(coral)
            if (hasJaw) { p(8, 11, ink); p(9, 10, ink); p(10, 11, ink) } else p(9, 10, ink)
            val drop = if (sin(t * 3f) > -0.3f) 1 else 0
            p(13, 7 + drop, tearBlue); p(13, 8 + drop, tearBlue)
        }
        Expression.HEART_EYES -> {
            // Pink hearts that beat
            val heart = if (sin(t * 8f) > 0f) Color(0xFFFF4D6D) else Color(0xFFFF758F)
            for (left in intArrayOf(6, 10)) {
                p(left, 7, heart); p(left + 2, 7, heart)
                rect(left, 8, 3, 1, heart)
                p(left + 1, 9, heart)
            }
            openSmile()
        }
        Expression.FOCUSED -> {
            // Brows knitted over eyes on the task, mouth set, the tip of the tongue out
            rect(7, 7, 2, 1, ink); rect(10, 7, 2, 1, ink)
            lowLids()
            if (hasJaw) { rect(8, 11, 3, 1, ink); p(10, 10, tongue) } else p(9, 10, ink)
        }
        Expression.BLISS -> {
            // Eyes softly shut in happy arcs, rosy cheeks, a small smile
            arcs()
            blushUnderArcs(rose)
            softSmile()
        }
    }
}
