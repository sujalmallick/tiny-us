package com.example.engine

import androidx.compose.ui.graphics.Color

/**
 * The details that make each outfit what its name says (plan 12, B): the cable knit, the varsity
 * patch, the zip, the cargo pockets, the strawberries, the gingham, the plaid, the pleats... drawn
 * over the plain sprite in the outfit's own colours. Coordinates are the standing sprite's 18 x 26
 * grid; [sitting] moves the torso details onto the seated sprite and the skirt or trousers onto
 * the lap. Nothing here is drawn in the snow, when everyone wears their winter coats.
 */
internal object OutfitDetails {
    private val White = Color(0xFFFFFFFF)
    private val Gold = Color(0xFFE9C46A)
    private val Strawberry = Color(0xFFE63946)
    private val Leaf = Color(0xFF52B788)

    private fun tint(c: Color, f: Float) = Color(c.red + (1f - c.red) * f, c.green + (1f - c.green) * f, c.blue + (1f - c.blue) * f, c.alpha)
    private fun shade(c: Color, f: Float) = Color(c.red * (1f - f), c.green * (1f - f), c.blue * (1f - f), c.alpha)

    /** Seated, the torso is shorter: its rows (standing 12..17) fold onto 13..16. */
    private fun torsoRow(r: Int, sitting: Boolean) = if (!sitting) r else when (r) {
        12 -> 13
        13, 14 -> 14
        15 -> 15
        else -> 16
    }

    /** Seated, the skirt or trousers are the lap: standing rows 18..20 show on 17..18. */
    private fun lapRow(r: Int, sitting: Boolean) = if (!sitting) r else if (r <= 18) 17 else 18

    /** The front of the top: knit, patches, zips, prints, collars, belts. */
    fun torso(
        px: (Int, Int, Color) -> Unit,
        isGirl: Boolean,
        index: Int,
        dress: PixelArtRenderer.GirlDressPalette,
        boy: PixelArtRenderer.BoyOutfitPalette,
        sitting: Boolean,
        twinkle: Boolean
    ) {
        fun p(x: Int, r: Int, c: Color) = px(x, torsoRow(r, sitting), c)
        if (!isGirl) {
            when (index) {
                0 -> {
                    // Spruce knit: two cables down the front and a ribbed hem
                    for (r in 13..16) {
                        p(8, r, if (r % 2 == 1) boy.sweaterHighlight else boy.collar)
                        p(10, r, if (r % 2 == 0) boy.sweaterHighlight else boy.collar)
                    }
                    for (x in 6..12) p(x, 17, if (x % 2 == 0) boy.collar else boy.sweater)
                }
                1 -> {
                    // Varsity: an emerald letter patch on the chest
                    p(6, 13, boy.collar); p(7, 13, boy.collar)
                    p(6, 14, boy.collar); p(7, 14, White)
                }
                2 -> {
                    // Zip hoodie: the zip down the middle, its pull at the top
                    for (r in 13..17) p(9, r, Color(0xFFADB5BD))
                    p(9, 13, Color(0xFFE9ECEF))
                }
                3 -> {
                    // Oatmeal hoodie: a little embroidered logo
                    p(11, 13, boy.collar)
                }
                4 -> {
                    // Midnight graphic: a crescent moon and a star
                    p(9, 13, Color(0xFFFFE8A3)); p(9, 14, Color(0xFFFFD166))
                    p(11, 13, if (twinkle) White else Color(0xFF94A3B8))
                }
                5 -> if (twinkle) p(9, 14, Color(0xFFFFD166))
            }
            return
        }
        when (index) {
            0 -> {
                // Strawberry knit: two little strawberries
                p(7, 13, Leaf); p(7, 14, Strawberry)
                p(11, 14, Leaf); p(11, 15, Strawberry)
            }
            1 -> {
                // Wrap dress: the crossover neckline and a tie at the waist
                p(8, 13, dress.trim); p(9, 14, dress.trim); p(10, 15, dress.trim)
                p(11, 16, dress.ribbon); p(11, 17, shade(dress.ribbon, 0.2f))
            }
            2 -> {
                // Velvet gown: a gold belt with its buckle
                for (x in 6..12) p(x, 17, shade(dress.skirt, 0.25f))
                p(9, 17, Gold)
            }
            3 -> {
                // Picnic dress: a bow at the waist
                p(8, 17, dress.ribbon); p(10, 17, dress.ribbon); p(9, 17, shade(dress.ribbon, 0.25f))
            }
            4 -> {
                // Starlight gown: a sash and a sparkle
                for (x in 6..12) p(x, 17, dress.trim)
                p(7, 14, if (twinkle) White else dress.trim)
            }
            5 -> {
                // Tea dress: a round white collar and two little buttons
                p(7, 12, White); p(8, 12, White); p(9, 12, dress.sweater); p(10, 12, White); p(11, 12, White)
                p(9, 14, Color(0xFFFFB5C2)); p(9, 16, Color(0xFFFFB5C2))
            }
            6 -> {
                // Flannel: plaid lines with red where they cross, and buttons
                for (r in 13..17) { p(7, r, dress.trim); p(11, r, dress.trim) }
                for (x in 6..12) { p(x, 14, dress.trim); p(x, 16, dress.trim) }
                p(7, 14, dress.ribbon); p(11, 14, dress.ribbon); p(7, 16, dress.ribbon); p(11, 16, dress.ribbon)
                p(9, 13, White); p(9, 15, White)
            }
            8 -> {
                // Colourblock: a band of cream across the chest (the drawstrings over it)
                for (x in intArrayOf(6, 7, 9, 11, 12)) p(x, 14, dress.trim)
            }
            10 -> {
                // Star shimmer: sparkles that come and go
                p(7, 14, if (twinkle) White else dress.trim)
                p(11, 13, if (twinkle) dress.trim else White)
            }
            11 -> if (twinkle) p(9, 14, Color(0xFFFFD166))
        }
    }

    /** Below the waist: belts, pockets, prints, pleats, stitching. [stillLegs] when standing still. */
    fun lower(
        px: (Int, Int, Color) -> Unit,
        isGirl: Boolean,
        index: Int,
        dress: PixelArtRenderer.GirlDressPalette,
        boy: PixelArtRenderer.BoyOutfitPalette,
        sitting: Boolean,
        stillLegs: Boolean,
        twinkle: Boolean
    ) {
        fun p(x: Int, r: Int, c: Color) = px(x, lapRow(r, sitting), c)
        // The thighs (standing still only; walking moves the legs about)
        fun thigh(x: Int, r: Int, c: Color) { if (!sitting && stillLegs) px(x, r, c) }
        if (!isGirl) {
            when (index) {
                0 -> { p(7, 18, boy.pantsFold); p(11, 18, boy.pantsFold) }
                1 -> {
                    // Chinos with a belt and a brass buckle
                    for (x in 6..12) p(x, 18, Color(0xFF7F5539))
                    p(9, 18, Gold)
                }
                2 -> {
                    // Dark denim: gold stitching at the pockets
                    p(7, 19, Color(0xFFD4A373)); p(11, 19, Color(0xFFD4A373))
                }
                3 -> {
                    // Cargo trousers: pockets on the thighs, flaps over them
                    thigh(6, 20, tint(boy.pants, 0.2f)); thigh(6, 21, boy.pantsFold); thigh(6, 22, boy.pantsFold)
                    thigh(11, 20, tint(boy.pants, 0.2f)); thigh(11, 21, boy.pantsFold); thigh(11, 22, boy.pantsFold)
                    if (sitting) { p(6, 18, boy.pantsFold); p(11, 18, boy.pantsFold) }
                }
                4 -> {
                    // Black denim, faded at the knees
                    thigh(6, 21, tint(boy.pants, 0.22f)); thigh(11, 21, tint(boy.pants, 0.22f))
                }
            }
            return
        }
        when (index) {
            0 -> for (x in 5..12 step 2) p(x, 20, dress.trim)                 // a lace hem
            1 -> { p(6, 19, tint(dress.trim, 0.6f)); p(9, 18, tint(dress.trim, 0.6f)); p(11, 20, tint(dress.trim, 0.6f)) }
            2 -> { p(6, 18, dress.trim); p(7, 19, dress.trim); p(11, 18, tint(dress.skirt, 0.25f)) }   // velvet sheen
            3 -> {
                // Gingham
                for (r in 18..20) for (x in 5..12) if ((x + r) % 2 == 0) p(x, r, tint(dress.skirt, 0.55f))
            }
            4 -> {
                p(6, 19, if (twinkle) White else Color(0xFF98C1D9)); p(9, 18, Color(0xFFE0FBFC))
                p(11, 20, if (twinkle) Color(0xFF98C1D9) else White)
            }
            5 -> for (x in 5..12) p(x, 19, dress.trim)                        // a second tier
            7 -> for (r in 18..20) for (x in intArrayOf(7, 9, 11)) p(x, r, dress.skirtShadow)  // pleats
            8 -> { p(6, 19, tint(dress.skirt, 0.25f)); p(10, 18, tint(dress.skirt, 0.25f)); p(8, 20, tint(dress.skirt, 0.25f)) }
            9 -> {
                // Denim skirt: a seam, a button and gold stitching
                for (r in 18..20) p(9, r, dress.skirtShadow)
                p(9, 18, Gold)
                for (x in 5..12 step 2) p(x, 20, Color(0xFFC9A86A))
            }
            10 -> for (x in 5..12) p(x, 20, dress.skirtShadow)
        }
    }

    /** Cuffs at the ends of relaxed sleeves, standing ([sitting] false) or seated. */
    fun cuffs(px: (Int, Int, Color) -> Unit, isGirl: Boolean, dress: PixelArtRenderer.GirlDressPalette, boy: PixelArtRenderer.BoyOutfitPalette, sitting: Boolean) {
        val c = if (isGirl) dress.trim else boy.collar
        if (sitting) {
            px(5, 16, c); px(6, 16, c); px(11, 16, c); px(12, 16, c)
        } else {
            px(4, 16, c); px(5, 16, c); px(12, 16, c); px(13, 16, c)
        }
    }

    /** A highlight on the shoes when standing still: laces for him, a strap for her. */
    fun shoes(px: (Int, Int, Color) -> Unit, isGirl: Boolean, shoe: Color) {
        val hl = tint(shoe, 0.35f)
        if (isGirl) { px(6, 24, hl); px(11, 24, hl) } else { px(6, 24, hl); px(11, 24, hl) }
    }
}
