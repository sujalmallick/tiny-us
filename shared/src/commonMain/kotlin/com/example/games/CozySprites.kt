package com.example.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.GardenGrowth

/**
 * Pixel art for cooking, fishing and garden care (plan 07, C3-C5): the ingredients, the dishes, what
 * comes up on the line, the garden plot as it grows, a bouquet, the bobber and the watering can.
 * Each sprite is rows of palette letters ('.' is see-through), drawn in whole pixels of size p.
 * In flower sprites, P/Q/R are petals and C a centre, coloured per flower when drawn.
 */
object CozySprites {

    class Sprite(val rows: List<String>) {
        val width get() = rows.maxOf { it.length }
        val height get() = rows.size
    }

    val PALETTE: Map<Char, Color> = mapOf(
        'k' to Color(0xFF2B2D42), // eyes, coal
        'e' to Color(0xFFC9B79C), // soft outline for pale things
        'w' to Color(0xFFFDFBF7), // white
        'c' to Color(0xFFF3E3C3), // cream
        'y' to Color(0xFFFFD166), // yellow
        'h' to Color(0xFFE9A23B), // honey gold
        'o' to Color(0xFFF4A261), // orange
        'b' to Color(0xFFA0673A), // brown
        'd' to Color(0xFF6B4226), // dark brown
        'D' to Color(0xFF5C3A21), // chocolate
        'a' to Color(0xFFD9A066), // biscuit, potato
        'A' to Color(0xFFA0763F), // potato eyes
        'r' to Color(0xFFE63946), // red
        'p' to Color(0xFFFFB5C2), // shell pink
        'q' to Color(0xFFE5738F), // shell ridges
        'g' to Color(0xFF6BBF59), // leaf green
        'G' to Color(0xFF3E7D3A), // dark leaf
        'l' to Color(0xFF9AD1F5), // light blue, glass
        'B' to Color(0xFF4A7FC1), // blue
        's' to Color(0xFFB8C2CC), // silver
        'S' to Color(0xFF6C757D), // dark silver
        'u' to Color(0xFFB497E7), // onion purple
        'U' to Color(0xFF8E6BBF), // onion shade
        't' to Color(0xFF9C7A3C), // tea
        'n' to Color(0xFF7A5230), // soil
        'N' to Color(0xFF5A3A20), // dark soil
        'v' to Color(0x66FFFFFF), // steam
        'L' to Color(0xFFA7D86E), // light green: peas, mint, green tea
        'O' to Color(0xFFD9822B)  // deep orange: pumpkin ribs, terracotta
    )

    // --- Ingredients (on the counter and in the fridge) ---

    val INGREDIENTS: Map<Ingredient, Sprite> = mapOf(
        Ingredient.FLOUR to Sprite(listOf(
            "...dd...",
            "..cccc..",
            ".cccccc.",
            ".ccbbcc.",
            ".cccccc.",
            ".cccccc.",
            "..cccc.."
        )),
        Ingredient.EGG to Sprite(listOf(
            "..ee..",
            ".ewwe.",
            "ewwwwe",
            "ewwwwe",
            "ewwwce",
            ".ecce.",
            "..ee.."
        )),
        Ingredient.MILK to Sprite(listOf(
            "..ll..",
            "..ee..",
            ".ewwe.",
            "ewwwwe",
            "eBBBBe",
            "ewwwwe",
            "ewwwwe",
            ".eeee."
        )),
        Ingredient.BUTTER to Sprite(listOf(
            "..yyyy..",
            ".yyyyyh.",
            ".yyyyyh.",
            "wwwwwwww",
            ".eeeeee."
        )),
        Ingredient.SUGAR to Sprite(listOf(
            ".sssss.",
            ".ewwwe.",
            "ewwwwwe",
            "ewwwwwe",
            "ewwwwwe",
            ".eeeee."
        )),
        Ingredient.WATER to Sprite(listOf(
            "...l...",
            "..lBl..",
            ".lBBBl.",
            "lBBlBBl",
            "lBBBBBl",
            ".lBBBl.",
            "..lll.."
        )),
        Ingredient.CARROT to Sprite(listOf(
            ".....gG",
            "....gg.",
            "...oo..",
            "..ooo..",
            ".ooo...",
            ".oo....",
            "o......"
        )),
        Ingredient.POTATO to Sprite(listOf(
            ".aaaa..",
            "aaAaaaa",
            "aaaaaAa",
            "aAaaaaa",
            ".aaaa.."
        )),
        Ingredient.ONION to Sprite(listOf(
            "...G...",
            "...g...",
            "..uuu..",
            ".uuuuu.",
            "uuUuuuu",
            ".uuuuu.",
            "..uUu.."
        )),
        Ingredient.CABBAGE to Sprite(listOf(
            "..gggg..",
            ".gGggGg.",
            "gggGGggg",
            "gGggggGg",
            "gggGGggg",
            ".gGggGg.",
            "..gggg.."
        )),
        Ingredient.CHOCOLATE to Sprite(listOf(
            "DDDDDDDD",
            "DbDDbDDb",
            "DDDDDDDD",
            "DbDDbDDb",
            "rrrrrrrr",
            "rwrrrrwr"
        )),
        Ingredient.TEA_LEAVES to Sprite(listOf(
            ".sssss.",
            "ggggggg",
            "gGgggGg",
            "ggwwggg",
            "ggwwggg",
            "gGgggGg",
            "ggggggg"
        )),
        Ingredient.HONEY to Sprite(listOf(
            ".rrrrr.",
            "..eee..",
            ".ehhhe.",
            "ehhyhhe",
            "ehhhhhe",
            "ehhhhhe",
            ".eeeee."
        )),
        // From the garden (plan 09, E3).
        Ingredient.STRAWBERRY to Sprite(listOf(
            "..gG..",
            ".gggg.",
            "rrrrrr",
            "rryrrr",
            ".rrrr.",
            ".ryrr.",
            "..rr.."
        )),
        Ingredient.PEAS to Sprite(listOf(
            "......gG",
            ".GGGGGG.",
            "GLgLgLgG",
            ".GGGGGG."
        )),
        Ingredient.TOMATO to Sprite(listOf(
            "..gGg..",
            ".rrgrr.",
            "rrrrrrr",
            "rwrrrrr",
            "rrrrrrr",
            ".rrrrr.",
            "..rrr.."
        )),
        Ingredient.BASIL to Sprite(listOf(
            "...g...",
            "..gGg..",
            ".gGgGg.",
            "gGgGgGg",
            "..gGg..",
            "...G..."
        )),
        Ingredient.PUMPKIN to Sprite(listOf(
            "...dG...",
            ".oooooo.",
            "oOooOooO",
            "oOooOooO",
            "oOooOooO",
            ".oooooo."
        )),
        Ingredient.APPLE to Sprite(listOf(
            "...dg..",
            "..d.gg.",
            ".rrrrr.",
            "rrrrwrr",
            "rrrrrrr",
            "rrrrrrr",
            ".rr.rr."
        )),
        Ingredient.MINT to Sprite(listOf(
            "..L.L..",
            ".LgLgL.",
            "LgLgLgL",
            ".LgLgL.",
            "..LgL..",
            "...g..."
        ))
    )

    // --- Dishes (on the table, and in the recipe book) ---

    val DISHES: Map<String, Sprite> = mapOf(
        "pancakes" to Sprite(listOf(
            "....yy....",
            "..oooooo..",
            "..bbbbbb..",
            "..oooooo..",
            "..bbbbbb..",
            ".wwwwwwww.",
            "..eeeeee.."
        )),
        "soup" to Sprite(listOf(
            "...v..v...",
            "....v..v..",
            "eooroogooe",
            "ewwwwwwwwe",
            ".ewwwwwwe.",
            "..eeeeee.."
        )),
        "dumplings" to Sprite(listOf(
            "..c.....c..",
            ".ccc...ccc.",
            "ccccc.ccccc",
            "eeeee.eeeee",
            "wwwwwwwwwww",
            ".eeeeeeeee."
        )),
        "cookies" to Sprite(listOf(
            "..aaaaaa..",
            ".aaDaaaaa.",
            ".aaaaaDaa.",
            ".aDaaaaaa.",
            "..aaaaaa..",
            ".wwwwwwww.",
            "..eeeeee.."
        )),
        "tea" to Sprite(listOf(
            "...v.v....",
            "....v.v...",
            ".eeeeeee..",
            ".ettttteee",
            ".ewwwwwe.e",
            "..ewwweee.",
            "eeeeeeeeee"
        )),
        // Garden recipes (plan 09, E3).
        "tomato_soup" to Sprite(listOf(
            "...v..v...",
            "....v..v..",
            "errrrgrrre",
            "ewwwwwwwwe",
            ".ewwwwwwe.",
            "..eeeeee.."
        )),
        "strawberry_pancakes" to Sprite(listOf(
            "...rr.rr..",
            "..oooooo..",
            "..bbbbbb..",
            "..oooooo..",
            "..bbbbbb..",
            ".wwwwwwww.",
            "..eeeeee.."
        )),
        "pumpkin_pie" to Sprite(listOf(
            "...aaaa...",
            ".aaOoOoaa.",
            "aOoooooOoa",
            "aaaaaaaaaa",
            ".wwwwwwww.",
            "..eeeeee.."
        )),
        "herb_tea" to Sprite(listOf(
            "...v.v....",
            "....v.v...",
            ".eeeeeee..",
            ".eLLLLLeee",
            ".ewwwwwe.e",
            "..ewwweee.",
            "eeeeeeeeee"
        )),
        "apple_crumble" to Sprite(listOf(
            "...aAaa...",
            ".aaAaaAaa.",
            "brrbrrbrrb",
            "bbbbbbbbbb",
            ".wwwwwwww.",
            "..eeeeee.."
        )),
        "pea_soup" to Sprite(listOf(
            "...v..v...",
            "....v..v..",
            "eLLgLLgLLe",
            "ewwwwwwwwe",
            ".ewwwwwwe.",
            "..eeeeee.."
        ))
    )

    // --- What comes up on the line ---

    val CATCHES: Map<FishingCatch, Sprite> = mapOf(
        FishingCatch.MINNOW to Sprite(listOf(
            "...ss...",
            "s.sssss.",
            "sssssks.",
            "s.SSSSS."
        )),
        FishingCatch.CARP to Sprite(listOf(
            "....ooo...",
            "o..oooooo.",
            "oooooooko.",
            "o..hhhhhh.",
            "....hh...."
        )),
        FishingCatch.SEASHELL to Sprite(listOf(
            "..ppppp..",
            ".pqpqpqp.",
            "pqpqpqpqp",
            "pqpqpqpqp",
            ".pqpqpqp.",
            "..pqpqp..",
            "..ppppp.."
        )),
        FishingCatch.OLD_BOOT to Sprite(listOf(
            "dddd....",
            "dbbd....",
            "dbbd....",
            "dbbd....",
            "dbbdddd.",
            "dbbbbbbd",
            "dddddddd"
        )),
        FishingCatch.BOTTLE to Sprite(listOf(
            "..dd..",
            "..ll..",
            ".llll.",
            "llccll",
            "lcwwcl",
            "lcwwcl",
            "llccll",
            ".llll."
        )),
        FishingCatch.GOLDEN_FISH to Sprite(listOf(
            "w...yyy...",
            "y..yyyyyy.",
            "yyyyyyyky.",
            "y..hhhhhh.",
            "....hh..w."
        ))
    )

    // --- Garden ---

    private val SOIL = listOf(".nnnnnn.", "nNnnnNnn")

    /** The plot at each stage (the empty one is just the soil). */
    val PLOT: Map<PlotStage, Sprite> = mapOf(
        PlotStage.EMPTY to Sprite(List(6) { "........" } + SOIL),
        PlotStage.SEED to Sprite(List(5) { "........" } + "...d...." + SOIL),
        PlotStage.SPROUT to Sprite(listOf("........", "........", "........", "..g.g...", "...g....", "...g....") + SOIL),
        PlotStage.BUD to Sprite(listOf("........", "...P....", "..gPg...", "...g....", "..gg....", "...g....") + SOIL),
        PlotStage.BLOOM to Sprite(listOf("..PPP...", ".PPCPP..", "..PPP...", "...g.g..", "..gg....", "...gg...") + SOIL)
    )

    /** A crop's bud: all leaves, no petals. */
    private val CROP_BUD = listOf("........", "...g....", "..gGg...", "...g....", "..gg....", "...g....")

    /** Each crop and herb when it's ripe (plan 09, E1), above its soil or pot. */
    private val RIPE: Map<String, List<String>> = mapOf(
        "strawberry" to listOf("........", "........", "..g..g..", ".gGggGg.", "grgGgrg.", ".gr.gr.."),
        "peas" to listOf("...d....", "..gdL...", ".gLdg...", "..gdgL..", ".Lgdg...", "..gdg..."),
        "tomato" to listOf("..d.....", ".gdgr...", "gGdrrg..", ".gdgrr..", "rrdgG...", ".gdg...."),
        "basil" to listOf("........", "...g....", "..gGg...", ".gGgGg..", "gGgGgGg.", "..gGg..."),
        "pumpkin" to listOf("........", "........", ".g.dG...", "goooooo.", "oOooOoo.", ".oooooo."),
        "apple" to listOf("..GgG...", ".GgrgG..", "GgGgGrg.", ".rgGgG..", "...d....", "...d...."),
        "mint" to listOf("........", "...L....", "..LgL...", ".LgLgL..", "LgLgLgL.", "..LgL...")
    )

    /** A sunroom pot, in place of the meadow soil. */
    private val POT = listOf(".OOOOOO.", "..oOoo..", "..OOOO..")

    /**
     * The sprite for a spot at [stage] with [seed] planted (null when empty): flowers use [PLOT]
     * (coloured when drawn), crops their own bud and ripe plant. [pot] puts it in a sunroom pot.
     */
    fun spot(stage: PlotStage, seed: String?, pot: Boolean): Sprite {
        val crop = Seeds.byId(seed)?.let { it.kind != SeedKind.FLOWER } == true
        val top = when {
            crop && stage == PlotStage.BLOOM -> RIPE[seed] ?: CROP_BUD
            crop && stage == PlotStage.BUD -> CROP_BUD
            else -> PLOT.getValue(stage).rows.take(6)
        }
        return Sprite(top + if (pot) POT else SOIL)
    }

    /** The ripe crops, for tests and previews. */
    val ripeCrops: Set<String> get() = RIPE.keys

    /** Three flowers tied with a ribbon: P, Q and R are the three flowers picked. */
    val BOUQUET = Sprite(listOf(
        ".P..Q..R.",
        "PyPQyQRyR",
        ".P.gQg.R.",
        "..g.g.g..",
        "...ggg...",
        "...rrr...",
        "..r.g.r..",
        "....g....",
        "...g.g..."
    ))

    /** A bouquet kept or given doesn't remember its flowers: these three stand for it. */
    val BOUQUET_COLORS = listOf(Color(0xFFFF6F91), Color(0xFFFFFFFF), Color(0xFFB497E7))

    val BOBBER = Sprite(listOf(
        "..r..",
        ".rrr.",
        "rrrrr",
        "wwwww",
        ".www.",
        "..w.."
    ))

    val WATERING_CAN = Sprite(listOf(
        "..BBB....",
        ".B...B...",
        "BBBBBB..l",
        "BBBBBBBl.",
        "BBBBBB...",
        ".BBBB...."
    ))

    /** A seed's petal and centre colours, shared with the meadow's keepsake plants. */
    fun flowerColors(id: String): Pair<Color, Color> {
        val plant = GardenGrowth.keepsakePlants.firstOrNull { it.id == id }
            ?: return Color(0xFFFF8FAB) to Color(0xFFFFD166)
        return Color(plant.petal) to Color(plant.center)
    }

    /**
     * Draws [sprite] with its top-left at ([left], [top]) in pixels of size [p]. [petals] colour
     * P, Q and R in turn (one is enough for a single flower); [center] colours C.
     */
    fun draw(
        scope: DrawScope,
        sprite: Sprite,
        left: Float,
        top: Float,
        p: Float,
        petals: List<Color> = listOf(Color(0xFFFF8FAB)),
        center: Color = Color(0xFFFFD166)
    ) {
        for ((y, row) in sprite.rows.withIndex()) {
            for ((x, ch) in row.withIndex()) {
                val color = when (ch) {
                    '.' -> null
                    'P' -> petals[0]
                    'Q' -> petals.getOrElse(1) { petals[0] }
                    'R' -> petals.getOrElse(2) { petals[0] }
                    'C' -> center
                    else -> PALETTE[ch]
                } ?: continue
                scope.drawRect(color, Offset(left + x * p, top + y * p), Size(p, p))
            }
        }
    }
}
