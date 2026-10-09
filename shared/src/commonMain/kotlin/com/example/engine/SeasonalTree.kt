package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.scene.WeatherType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The tree on the hill (Under the Tree): one tree living through the year. Pink blossom in spring,
 * a full green crown in summer (deeper in the rain), orange and red in autumn with leaves on the
 * grass, bare branches holding the snow in winter. The trunk, roots, branches and swing stay the
 * same in every season.
 *
 * It's built once for each season and light, one image pixel per game pixel, from a few placed
 * shapes: a gently curved trunk with a root flare, five curved branches (the low left one carries
 * the swing), and a score of leafy puffs closed into one canopy, drawn at the characters' pixel size in
 * the game's blocky way: few tones, lit on the upper right, dark underneath. Each frame draws that image in a few bands, the upper
 * ones nudged by the wind.
 */
object SeasonalTree {
    enum class Season {
        SPRING, SUMMER, RAIN, AUTUMN, WINTER;

        companion object {
            fun of(weather: WeatherType): Season = when (weather) {
                WeatherType.SAKURA -> SPRING
                WeatherType.SUNNY -> SUMMER
                WeatherType.RAIN -> RAIN
                WeatherType.AUTUMN -> AUTUMN
                WeatherType.SNOW -> WINTER
            }
        }
    }

    enum class Light { DAY, SUNSET, NIGHT }

    fun lightFor(isNight: Boolean, isSunset: Boolean): Light = when {
        isNight -> Light.NIGHT
        isSunset -> Light.SUNSET
        else -> Light.DAY
    }

    /**
     * Draws [block] in the tree's [light], so things drawn on the bark match it; [bounds] is
     * where it draws (the whole canvas when not given), so only that much is lit again.
     */
    fun inLight(scope: DrawScope, light: Light, bounds: Rect? = null, block: () -> Unit) {
        if (light == Light.DAY) { block(); return }
        val canvas = scope.drawContext.canvas
        canvas.saveLayer(bounds ?: Rect(Offset.Zero, scope.size), lightPaint(light))
        block()
        canvas.restore()
    }

    /** One paint for each light but the day's, made once (drawing is on one thread). */
    private val lightPaints = HashMap<Light, Paint>()

    private fun lightPaint(light: Light): Paint = lightPaints.getOrPut(light) {
        val m = if (light == Light.NIGHT) {
            floatArrayOf(0.42f, 0f, 0f, 0f, 0x14 * 0.30f, 0f, 0.46f, 0f, 0f, 0x1E * 0.30f, 0f, 0f, 0.55f, 0f, 0x48 * 0.35f, 0f, 0f, 0f, 1f, 0f)
        } else {
            floatArrayOf(0.92f, 0f, 0f, 0f, 0xFF * 0.12f, 0f, 0.84f, 0f, 0f, 0x7A * 0.10f, 0f, 0f, 0.78f, 0f, 0x40 * 0.06f, 0f, 0f, 0f, 1f, 0f)
        }
        Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix(m)) }
    }

    /** Image size in game pixels. */
    const val W = 128
    private const val U_MIN = -9
    private const val U_MAX = 136
    const val H = U_MAX - U_MIN + 1
    /** The trunk's foot, across the image. */
    private const val BX = 64
    private const val TRUNK_TOP = 52
    /** Rows higher than this bend in the wind. */
    private const val SWAY_FROM = 55
    /** The highest row the sway never shifts (draw's bands: under half a pixel at the strongest gust). */
    private const val STILL_TOP = 71

    /** Where the tree stands in its scene, as fractions of the world. */
    const val SCENE_X = 0.5f
    const val SCENE_GROUND = 0.69f

    // ---------------------------------------------------------------- the shape (every season)

    /** Main branches: quadratic curves (start, control, end) as (dx, height), thickness start to end. */
    private val BRANCHES = arrayOf(
        floatArrayOf(-4f, 48f, -20f, 58f, -43f, 61f, 6f, 2.5f), // the swing branch, long and low on the left
        floatArrayOf(4f, 48f, 22f, 58f, 37f, 80f, 6f, 2.5f),
        floatArrayOf(-3f, 50f, -12f, 72f, -18f, 104f, 7f, 2f),
        floatArrayOf(4f, 50f, 14f, 74f, 21f, 107f, 6.5f, 2f),
        floatArrayOf(0f, 52f, -2f, 84f, 2f, 117f, 4.5f, 1.5f)
    )

    /** Smaller branches off the main ones, seen when the leaves thin: (branch, at, end dx, end height, thickness). */
    private val SECONDARY = arrayOf(
        floatArrayOf(0f, 0.55f, -30f, 72f, 2f), floatArrayOf(0f, 0.85f, -48f, 70f, 1.5f),
        floatArrayOf(1f, 0.55f, 30f, 82f, 2f), floatArrayOf(1f, 0.85f, 50f, 80f, 1.5f),
        floatArrayOf(2f, 0.45f, -28f, 86f, 2.5f), floatArrayOf(2f, 0.75f, -32f, 101f, 1.8f), floatArrayOf(2f, 0.95f, -24f, 116f, 1.5f),
        floatArrayOf(3f, 0.45f, 30f, 88f, 2.5f), floatArrayOf(3f, 0.75f, 34f, 103f, 1.8f), floatArrayOf(3f, 0.95f, 28f, 119f, 1.5f),
        floatArrayOf(4f, 0.7f, -8f, 112f, 1.5f), floatArrayOf(4f, 0.8f, 9f, 114f, 1.5f)
    )

    /** Roots: curves (start, control, end) as (dx, height) and their thickness at the trunk. */
    private val ROOTS = arrayOf(
        floatArrayOf(-9f, 5f, -17f, 1f, -27f, -1f, 4f),
        floatArrayOf(9f, 5f, 16f, 1f, 25f, -1f, 4f),
        floatArrayOf(-2f, 3f, 2f, 0f, 6f, -3f, 3f)
    )

    /** Leafy puffs, back to front: (dx, height, radius). */
    private val PUFFS = arrayOf(
        intArrayOf(0, 110, 16), intArrayOf(-21, 103, 15), intArrayOf(22, 104, 15), intArrayOf(-9, 114, 9), intArrayOf(11, 115, 9),
        intArrayOf(-38, 90, 13), intArrayOf(39, 92, 13),
        intArrayOf(-12, 93, 15), intArrayOf(13, 94, 15),
        intArrayOf(-30, 77, 12), intArrayOf(31, 80, 12), intArrayOf(0, 85, 14),
        intArrayOf(-47, 77, 8), intArrayOf(48, 82, 8), intArrayOf(-17, 77, 10), intArrayOf(18, 79, 10),
        // Low over the fork, so no sky shows between the branches where they meet the leaves
        intArrayOf(0, 65, 12), intArrayOf(-13, 67, 10), intArrayOf(14, 68, 10), intArrayOf(26, 71, 9), intArrayOf(-24, 71, 8)
    )


    /** In autumn twigs show through the leaves only up to this height. */
    private const val AUTUMN_REACH = 105

    /** The swing hangs from the left branch, its middle this far left of the trunk; seat this high. */
    private const val SWING_DX = -32
    private const val SEAT_U = 18
    /** The swing only drifts in the breeze: at most this far each way (radians), slowly. */
    private const val SWING_ARC = 0.07f
    private const val SWING_SPEED = 0.9f

    // ---------------------------------------------------------------- palettes

    private val CANOPY = mapOf(
        // shade, leaf, sunlit, highlight: the game's own leaf colours
        Season.SUMMER to intArrayOf(0x2D5A27, 0x40916C, 0x74C69D, 0xA7E0B8),
        Season.RAIN to intArrayOf(0x24492A, 0x356F4F, 0x5A9E7A, 0x86C39E),
        Season.SPRING to intArrayOf(0xC75D7B, 0xF291B0, 0xFFB5C2, 0xFFD5E0),
        Season.AUTUMN to intArrayOf(0x8B1E1E, 0xD46A28, 0xF4B942, 0xFFD982)
    )
    /** Bark: shade, wood, sunlit side, groove. */
    private val WOOD = intArrayOf(0x45250F, 0x6B4226, 0x8E5C36, 0x37190A)

    // ---------------------------------------------------------------- grid helpers

    private fun inGrid(x: Int, u: Int) = x in 0 until W && u in U_MIN..U_MAX
    private fun idx(x: Int, u: Int) = (U_MAX - u) * W + x

    /** A stable pseudo-random number in [0, 1) for a cell: the same tree every time. */
    private fun hash(x: Int, y: Int, s: Int): Float {
        var n = x * 374761393 + y * 668265263 + s * 1442695041
        n = (n xor (n ushr 13)) * 1274126177
        n = n xor (n ushr 16)
        return (n.toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
    }

    private fun bez(a: Float, c: Float, b: Float, t: Float) = (1 - t) * (1 - t) * a + 2 * (1 - t) * t * c + t * t * b

    private fun trunkCenter(u: Int): Float = BX + 2.6f * sin(u / 60f * PI.toFloat() * 1.1f + 0.15f) - 0.6f

    private fun trunkHalf(u: Int): Float = if (u < 8) 11f + (8 - u) * 0.95f else 11f - max(0f, u - 26f) * 0.11f

    /** A tapering round stroke through [xs], [us]: lit along the top, in shade underneath. */
    private fun stroke(wood: IntArray, xs: FloatArray, us: FloatArray, t0: Float, t1: Float) {
        val n = xs.size
        for (k in 0 until n) {
            val f = k / max(1f, (n - 1).toFloat())
            val r = (t0 + (t1 - t0) * f) / 2f
            val x = xs[k]
            val u = us[k]
            for (yy in floor(u - r - 1).toInt()..ceil(u + r + 1).toInt()) {
                for (xx in floor(x - r - 1).toInt()..ceil(x + r + 1).toInt()) {
                    if (!inGrid(xx, yy)) continue
                    val d2 = (xx - x) * (xx - x) + (yy - u) * (yy - u)
                    if (d2 > r * r + 0.35f) continue
                    val rel = (yy - u) / max(r, 0.5f)
                    val s = if (rel > 0.45f) 2 else if (rel < -0.45f) 0 else 1
                    val i = idx(xx, yy)
                    val cur = wood[i]
                    if (cur < 0 || s > cur || cur == 3) wood[i] = s
                }
            }
        }
    }

    private fun curve(ax: Float, au: Float, cx: Float, cu: Float, bx: Float, bu: Float, steps: Int): Pair<FloatArray, FloatArray> {
        val xs = FloatArray(steps + 1) { bez(ax, cx, bx, it / steps.toFloat()) }
        val us = FloatArray(steps + 1) { bez(au, cu, bu, it / steps.toFloat()) }
        return xs to us
    }

    /**
     * The wood: -1 none, 0 shade, 1 wood, 2 sunlit, 3 bark groove. [bare] adds the smaller
     * branches, up to [reach] high (in autumn the highest stay hidden in the leaves).
     */
    private fun buildWood(bare: Boolean, reach: Float = Float.MAX_VALUE): IntArray {
        val wood = IntArray(W * H) { -1 }
        for (u in -1..TRUNK_TOP) {
            val c = trunkCenter(max(u, 0))
            val half = trunkHalf(max(u, 0))
            val left = c - half
            val right = c + half
            for (x in floor(left).toInt() until ceil(right).toInt()) {
                if (!inGrid(x, u)) continue
                val t = (x + 0.5f - left) / (right - left)
                wood[idx(x, u)] = if (t < 0.24f) 0 else if (t > 0.78f) 2 else 1
            }
        }
        // Bark grooves follow the trunk's curve
        val offsets = floatArrayOf(-5.5f, -1.5f, 3f, 6.5f)
        for ((i, off) in offsets.withIndex()) {
            var drift = 0f
            for (u in 3 + i * 2 until TRUNK_TOP - 6 + (i % 2) * 4) {
                if (u % 6 == 0) drift += (hash(i, u, 8) - 0.5f) * 1.6f
                val x = (trunkCenter(u) + off + drift).roundToInt()
                if (inGrid(x, u) && wood[idx(x, u)] == 1 && hash(i, u / 7, 7) > 0.12f) wood[idx(x, u)] = 3
            }
        }
        // A knot
        val kx = trunkCenter(40).roundToInt() + 4
        for ((dx, du) in arrayOf(0 to 0, 1 to 0, 0 to 1)) wood[idx(kx + dx, 40 + du)] = 3
        // Roots, curving out over the grass
        for (r in ROOTS) {
            val (xs, us) = curve(BX + r[0], r[1], BX + r[2], r[3], BX + r[4], r[5], 24)
            stroke(wood, xs, us, r[6], 1.2f)
        }
        for (b in BRANCHES) {
            val (xs, us) = curve(BX + b[0], b[1], BX + b[2], b[3], BX + b[4], b[5], 40)
            stroke(wood, xs, us, b[6], b[7])
        }
        if (bare) {
            for (s in SECONDARY) {
                if (s[3] > reach) continue
                val b = BRANCHES[s[0].toInt()]
                val t = s[1]
                val sx = bez(BX + b[0], BX + b[2], BX + b[4], t)
                val su = bez(b[1], b[3], b[5], t)
                val ex = BX + s[2]
                val (xs, us) = curve(sx, su, (sx + ex) / 2f, (su + s[3]) / 2f + 2f, ex, s[3], 20)
                stroke(wood, xs, us, s[4], 1f)
            }
        }
        return wood
    }

    /** Thin twigs at the tips of the branches, seen in winter and poking through in autumn. */
    private fun buildTwigs(): BooleanArray {
        val twigs = BooleanArray(W * H)
        val tips = SECONDARY.map { it[2] to it[3] } + BRANCHES.map { it[4] to it[5] }
        for ((i, tip) in tips.withIndex()) {
            val (ex, eu) = tip
            for (j in 0 until 3) {
                val ang = PI.toFloat() / 2f - (ex / 60f) * 1.2f + (j - 1) * 0.55f
                val len = 4 + (hash(i, j, 52) * 5).toInt()
                for (k in 1..len) {
                    val x = (BX + ex + cos(ang) * k).roundToInt()
                    val u = (eu + sin(ang) * k).roundToInt()
                    if (inGrid(x, u)) twigs[idx(x, u)] = true
                }
            }
        }
        return twigs
    }

    /** The canopy is drawn at the characters' pixel size: one leaf cell is 2 x 2 image pixels. */
    private const val HW = W / 2
    private const val HV_MIN = -5
    private const val HV_MAX = U_MAX / 2
    private const val HH = HV_MAX - HV_MIN + 1
    private fun inHalf(x: Int, v: Int) = x in 0 until HW && v in HV_MIN..HV_MAX
    private fun hidx(x: Int, v: Int) = (v - HV_MIN) * HW + x

    /**
     * The canopy's tones on the leaf-cell grid (-1 none, 0 shade, 1 leaf, 2 sunlit, 3 highlight),
     * in the game's own blocky way: each puff light on its upper right and dark along its
     * underside, a couple of highlight notches, no texture noise; the crown closed so no sky
     * shows between the puffs, and its very bottom in shade.
     */
    private fun buildLeafCells(): IntArray {
        val cells = IntArray(HW * HH) { -1 }
        for ((i, puff) in PUFFS.withIndex()) {
            val cx = (BX + puff[0]) / 2f
            val cv = puff[1] / 2f
            val r = puff[2] / 2f
            for (v in (cv - r - 2).toInt()..(cv + r + 2).toInt()) for (x in (cx - r - 2).toInt()..(cx + r + 2).toInt()) {
                if (!inHalf(x, v)) continue
                val ddx = x + 0.5f - cx
                val ddv = v + 0.5f - cv
                val d = hypot(ddx, ddv)
                val bucket = ((atan2(ddv, ddx) + PI.toFloat()) / (2f * PI.toFloat()) * 12f).toInt() % 12
                val bump = (hash(bucket, i, 3) - 0.5f) * 1.1f
                if (d > r + bump) continue
                val nx = ddx / r
                val nv = ddv / r
                val lit = nv * 0.8f + nx * 0.45f
                var t = if (lit > 0.25f) 2 else if (lit > -0.45f) 1 else 0
                if (d > r + bump - 1f && nv < 0.1f) t = 0
                cells[hidx(x, v)] = t
            }
        }
        // Close small notches between puffs
        val grown = BooleanArray(HW * HH)
        for (v in HV_MIN..HV_MAX) for (x in 0 until HW) {
            if (cells[hidx(x, v)] < 0) continue
            for (dv in -1..1) for (dx in -1..1) if (inHalf(x + dx, v + dv)) grown[hidx(x + dx, v + dv)] = true
        }
        for (v in HV_MIN..HV_MAX) for (x in 0 until HW) {
            val k = hidx(x, v)
            if (cells[k] >= 0 || !grown[k]) continue
            var closed = true
            for (dv in -1..1) for (dx in -1..1) if (!inHalf(x + dx, v + dv) || !grown[hidx(x + dx, v + dv)]) closed = false
            if (closed) cells[k] = 1
        }
        // A little block of the brightest leaves on each puff's upper right
        for ((i, puff) in PUFFS.withIndex()) {
            val cx = (BX + puff[0]) / 2f
            val cv = puff[1] / 2f
            val r = puff[2] / 2f
            val hx = (cx + r * 0.25f).toInt()
            val hv = (cv + r * 0.45f).toInt()
            val third = if (hash(i, 0, 5) > 0.5f) intArrayOf(0, -1) else intArrayOf(1, 1)
            for (o in arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), third)) {
                if (inHalf(hx + o[0], hv + o[1]) && cells[hidx(hx + o[0], hv + o[1])] == 2) cells[hidx(hx + o[0], hv + o[1])] = 3
            }
        }
        // The crown's very bottom is always in shade
        val bottom = ArrayList<Int>()
        for (v in HV_MIN..HV_MAX) for (x in 0 until HW) {
            if (cells[hidx(x, v)] >= 0 && (!inHalf(x, v - 1) || cells[hidx(x, v - 1)] < 0)) bottom += hidx(x, v)
        }
        for (k in bottom) cells[k] = 0
        return cells
    }

    private val leafCells by lazy { buildLeafCells() }

    /** The canopy at image resolution (-1 none, else 0..3), each leaf cell 2 x 2. */
    private fun buildCanopy(): IntArray {
        val tone = IntArray(W * H) { -1 }
        for (v in HV_MIN..HV_MAX) for (x in 0 until HW) {
            val t = leafCells[hidx(x, v)]
            if (t < 0) continue
            for (oy in 0..1) for (ox in 0..1) if (inGrid(x * 2 + ox, v * 2 + oy)) tone[idx(x * 2 + ox, v * 2 + oy)] = t
        }
        return tone
    }

    private val leafyWood by lazy { buildWood(bare = false) }
    private val bareWood by lazy { buildWood(bare = true) }
    private val autumnWood by lazy { buildWood(bare = true, reach = AUTUMN_REACH.toFloat()) }
    private val twigs by lazy { buildTwigs() }
    private val canopy by lazy { buildCanopy() }

    /** Where each rope is tied: the underside of the swing branch above it. */
    private val ropeTops by lazy {
        IntArray(2) { side ->
            val x = BX + SWING_DX + if (side == 0) -6 else 6
            var u = 60
            while (u > 30 && leafyWood[idx(x, u)] < 0) u--
            u
        }
    }

    /** Cells along the canopy's underside, where falling petals and leaves come from. */
    private val dropCells by lazy {
        val out = ArrayList<Int>()
        val tone = canopy
        for (u in 45..U_MAX) for (x in 0 until W) {
            if (tone[idx(x, u)] >= 0 && (u - 1 < U_MIN || tone[idx(x, u - 1)] < 0)) out += (x - BX) * 1000 + u
        }
        out
    }

    // ---------------------------------------------------------------- one season, one light

    private val cache = LinkedHashMap<Int, PixelSurface>()

    private fun lit(rgb: Int, light: Light): Int {
        var r = (rgb shr 16) and 255
        var g = (rgb shr 8) and 255
        var b = rgb and 255
        when (light) {
            Light.NIGHT -> {
                r = (r * 0.42f + 0x14 * 0.30f).toInt(); g = (g * 0.46f + 0x1E * 0.30f).toInt(); b = (b * 0.55f + 0x48 * 0.35f).toInt()
            }
            Light.SUNSET -> {
                r = min(255, (r * 0.92f + 0xFF * 0.12f).toInt()); g = (g * 0.84f + 0x7A * 0.10f).toInt(); b = (b * 0.78f + 0x40 * 0.06f).toInt()
            }
            Light.DAY -> Unit
        }
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun build(season: Season, light: Light): PixelSurface {
        val px = IntArray(W * H)
        fun put(x: Int, u: Int, rgb: Int) {
            if (inGrid(x, u)) px[idx(x, u)] = lit(rgb, light)
        }
        val bare = season == Season.AUTUMN || season == Season.WINTER
        val wood = when (season) {
            Season.AUTUMN -> autumnWood
            Season.WINTER -> bareWood
            else -> leafyWood
        }
        val twig = twigs
        fun isWood(x: Int, u: Int) = inGrid(x, u) && wood[idx(x, u)] >= 0
        fun isTwig(x: Int, u: Int) = inGrid(x, u) && twig[idx(x, u)]

        val twigTop = if (season == Season.AUTUMN) AUTUMN_REACH else U_MAX
        if (bare) for (u in U_MIN..twigTop) for (x in 0 until W) if (twig[idx(x, u)]) put(x, u, WOOD[0])
        for (u in U_MIN..U_MAX) for (x in 0 until W) {
            val s = wood[idx(x, u)]
            if (s >= 0) put(x, u, WOOD[s])
        }

        if (season != Season.WINTER) {
            val pal = CANOPY.getValue(season)
            val tone = canopy
            for (u in U_MIN..U_MAX) for (x in 0 until W) {
                val t = tone[idx(x, u)]
                if (t >= 0) put(x, u, pal[t])
            }
            fun cell(x: Int, v: Int, rgb: Int) {
                for (oy in 0..1) for (ox in 0..1) put(x * 2 + ox, v * 2 + oy, rgb)
            }
            if (season == Season.SPRING) {
                // Blossom: a few whole cells of white and deep pink among the pink
                for (v in HV_MIN..HV_MAX) for (x in 0 until HW) {
                    val t = leafCells[hidx(x, v)]
                    if (t >= 1 && hash(x, v, 11) > 0.93f) cell(x, v, if (hash(x, v, 12) > 0.4f) 0xFFF0F5 else 0xFF758F)
                }
            }
            if (season == Season.SUMMER) {
                // A few red fruit among the leaves
                for (v in HV_MIN..HV_MAX) for (x in 0 until HW) {
                    if (leafCells[hidx(x, v)] == 1 && hash(x, v, 13) > 0.95f) cell(x, v, 0xD62828)
                }
            }
        } else {
            // Snow lies along the top of every branch, twig and root (not down the trunk's sides)
            for (u in U_MIN until U_MAX - 1) for (x in 0 until W) {
                if (!isWood(x, u) && !isTwig(x, u)) continue
                if (isWood(x, u + 1) || isTwig(x, u + 1)) continue
                if (u <= TRUNK_TOP - 4 && u > 6) continue
                put(x, u + 1, 0xF4F8FC)
                val wide = isWood(x - 1, u + 1) || isWood(x + 1, u + 1) || isTwig(x - 1, u + 1) || isTwig(x + 1, u + 1)
                if ((wide || hash(x, u, 21) > 0.5f) && !isWood(x, u + 2) && !isTwig(x, u + 2)) put(x, u + 2, 0xFFFFFF)
                if (isTwig(x, u)) put(x, u, 0xC9D9E8)
            }
            // A few last leaves hanging on
            for ((dx, u) in arrayOf(-35 to 70, 27 to 92, -14 to 104, 40 to 84, 6 to 118, -26 to 95)) {
                put(BX + dx, u, 0xA05A2A); put(BX + dx, u - 1, 0x7A401C)
            }
        }

        // The ground at its foot
        when (season) {
            Season.SPRING, Season.SUMMER, Season.RAIN -> {
                for (x in BX - 30..BX + 30) {
                    if (isWood(x, 1)) continue
                    val tall = (hash(x, 1, 31) * 3).toInt() + if (kotlin.math.abs(x - BX) < 20) 1 else 0
                    for (k in 0 until tall) put(x, k - 1, if (k == 0 || hash(x, k, 32) > 0.5f) 0x3A7D44 else 0x5FAF4E)
                }
                val flowers = arrayOf(-25 to 0, -18 to -2, -6 to -3, 15 to -2, 22 to 0, 28 to -3)
                for ((i, f) in flowers.withIndex()) {
                    val (dx, u) = f
                    val petal = if (season == Season.SPRING) (if (i % 2 == 1) 0xFFFFFF else 0xFF9EB8) else (if (i != 3) 0xFFFFFF else 0xE63946)
                    val heart = if (season == Season.SPRING) 0xFFC85A else 0xFFD166
                    for ((ax, au) in arrayOf(0 to 1, -1 to 0, 1 to 0, 0 to -1)) put(BX + dx + ax, u + au, petal)
                    put(BX + dx, u, heart)
                }
                if (season == Season.SPRING) {
                    // Fallen petals on the grass
                    for (k in 0 until 14) put(BX - 34 + (hash(k, 1, 33) * 68).toInt(), -(hash(k, 2, 34) * 6).toInt(), 0xFFB5C8)
                }
            }
            Season.AUTUMN -> {
                val leaves = intArrayOf(0xE06C1E, 0xC24118, 0xF2A23A, 0x8C3B16, 0xFFC85A)
                for (u in -5..2) {
                    val dv = (u + 1) / 3.8f
                    val half = 33f * sqrt(max(0f, 1f - dv * dv))
                    for (x in (BX - half).toInt() until (BX + half).toInt()) {
                        if (u >= 0 && isWood(x, u)) continue
                        if (hash(x, u, 41) < 0.95f - kotlin.math.abs(x - BX) / 40f) put(x, u, leaves[(hash(x, u, 42) * leaves.size).toInt()])
                    }
                }
                for (k in 0 until 12) {
                    val x = BX - 46 + (hash(k, 3, 43) * 92).toInt()
                    val u = -(hash(k, 4, 44) * 7).toInt()
                    put(x, u, leaves[k % leaves.size]); put(x + 1, u, leaves[(k + 2) % leaves.size])
                }
            }
            Season.WINTER -> {
                for (u in -5..4) {
                    val dv = (u + 0.5f) / 5f
                    val half = 30f * sqrt(max(0f, 1f - dv * dv))
                    for (x in (BX - half).toInt() until (BX + half).toInt()) {
                        if (u >= 2 && isWood(x, u)) continue
                        put(x, u, if ((x - BX) > -half * 0.2f && u >= 0) 0xFFFFFF else 0xD3E2EF)
                    }
                }
            }
        }

        val surface = PixelSurface(W, H)
        surface.writePixels(px, 0, 0, H)
        surface.commit()
        return surface
    }

    private fun built(season: Season, light: Light): PixelSurface {
        val key = season.ordinal * 3 + light.ordinal
        cache[key]?.let { return it }
        val b = build(season, light)
        cache[key] = b
        if (cache.size > 4) cache.remove(cache.keys.first())
        return b
    }

    // ---------------------------------------------------------------- drawing

    /**
     * Draws the tree with its foot at ([baseX], [groundY]), one image pixel to [p], with its shade
     * on the grass under it. The upper canopy sways with [time].
     */
    fun draw(scope: DrawScope, baseX: Float, groundY: Float, p: Float, time: Float, weather: WeatherType, light: Light) {
        val season = Season.of(weather)
        val b = built(season, light)
        val left = ((baseX / p).roundToInt() - BX) * p
        val ground = (groundY / p).roundToInt() * p
        fun rowY(u: Int) = ground - (u + 1) * p

        // The shade: a soft patch under the canopy (dappled in leafy sunshine), a darker one at the roots
        val shadeColor = if (season == Season.WINTER) Color(0xFF243A60) else Color(0xFF102A14)
        if (light != Light.NIGHT) {
            for (u in -5..2) {
                val dv = (u + 1) / 4.5f
                val half = 44f * sqrt(max(0f, 1f - dv * dv))
                var x = (BX - 6 - half).toInt()
                val end = (BX - 6 + half).toInt()
                while (x < end) {
                    val dapple = (season == Season.SUMMER || season == Season.SPRING) && hash(x.floorDiv(3), u.floorDiv(2), 9) > 0.85f
                    var run = 1
                    while (x + run < end && ((season == Season.SUMMER || season == Season.SPRING) &&
                            hash((x + run).floorDiv(3), u.floorDiv(2), 9) > 0.85f) == dapple) run++
                    if (!dapple) scope.drawRect(shadeColor.copy(alpha = 0.16f), Offset(left + x * p, rowY(u)), Size(run * p, p))
                    x += run
                }
            }
        }
        // (by day it lies over the patch above, so together they're darker at the roots)
        for (u in -3..1) {
            val dv = (u + 1) / 2.2f
            val half = 21f * sqrt(max(0f, 1f - dv * dv))
            val x0 = (BX - half).toInt()
            val x1 = (BX + half).toInt()
            if (x1 > x0) scope.drawRect(shadeColor.copy(alpha = if (light == Light.NIGHT) 0.22f else 0.15f), Offset(left + x0 * p, rowY(u)), Size((x1 - x0) * p, p))
        }

        // The tree, in bands: rows higher up bend a pixel or two with the wind
        val sway = sin(time * 1.5f) * 1.6f + sin(time * 0.37f) * 0.5f
        fun shiftOf(row: Int): Int {
            val u = U_MAX - row
            return if (u > SWAY_FROM) (sway * (u - SWAY_FROM) / 70f).roundToInt() else 0
        }
        val top = rowY(U_MAX)
        var row = 0
        while (row < H) {
            val s = shiftOf(row)
            var end = row + 1
            while (end < H && shiftOf(end) == s) end++
            scope.drawImage(
                image = b.image,
                srcOffset = IntOffset(0, row),
                srcSize = IntSize(W, end - row),
                dstOffset = IntOffset((left + s * p).roundToInt(), (top + row * p).roundToInt()),
                dstSize = IntSize((W * p).roundToInt(), ((end - row) * p).roundToInt()),
                filterQuality = FilterQuality.None
            )
            row = end
        }
    }

    /**
     * The swing, hanging from the left branch, drifting slowly in the breeze. The ropes stay tied
     * where they meet the wood and slant as the seat moves; a cap of snow sits on it in winter.
     */
    fun drawSwing(scope: DrawScope, baseX: Float, groundY: Float, p: Float, time: Float, weather: WeatherType, light: Light) {
        val left = ((baseX / p).roundToInt() - BX) * p
        val ground = (groundY / p).roundToInt() * p
        fun rowY(u: Int) = ground - (u + 1) * p
        fun c(rgb: Int) = Color(lit(rgb, light))
        val pivot = BX + SWING_DX
        // The breeze comes and goes (the same slow gusts that move the canopy); calm, it hardly moves
        val gust = 0.25f + 0.75f * (0.5f + 0.5f * sin(time * 0.37f))
        val angle = SWING_ARC * gust * sin(time * SWING_SPEED)
        val length = (ropeTops[0] + ropeTops[1]) / 2f - SEAT_U
        val swayX = (length * sin(angle)).roundToInt()
        val seatU = SEAT_U + (length * (1f - cos(angle))).roundToInt()
        for ((side, tieX) in intArrayOf(pivot - 6, pivot + 6).withIndex()) {
            val tieU = ropeTops[side]
            for (u in seatU + 1 until tieU) {
                val f = (tieU - u).toFloat() / max(1, tieU - seatU)
                val x = (tieX + swayX * f).roundToInt()
                scope.drawRect(c(if (u % 3 != 0) 0xDDA15E else 0xB07D47), Offset(left + x * p, rowY(u)), Size(p, p))
            }
        }
        val sx = pivot + swayX
        scope.drawRect(c(0x8C5A35), Offset(left + (sx - 8) * p, rowY(seatU)), Size(17 * p, p))
        scope.drawRect(c(0x6B4226), Offset(left + (sx - 8) * p, rowY(seatU - 1)), Size(17 * p, p))
        scope.drawRect(c(0x45250F), Offset(left + (sx - 8) * p, rowY(seatU - 2)), Size(17 * p, p))
        if (Season.of(weather) == Season.WINTER) {
            scope.drawRect(c(0xF4F8FC), Offset(left + (sx - 7) * p, rowY(seatU + 1)), Size(15 * p, p))
            scope.drawRect(c(0xFFFFFF), Offset(left + (sx - 4) * p, rowY(seatU + 2)), Size(8 * p, p))
        }
    }

    /**
     * Where a bird can sit on the tree: the tops of branches with open sky above them, as
     * (dx from the trunk, height of the bird's feet). Under the leaves only the swing branch
     * shows; in winter the bare branches have room all over.
     */
    fun perches(weather: WeatherType): List<IntArray> = perchCache.getOrPut(Season.of(weather)) {
        val season = Season.of(weather)
        val wood = when (season) {
            Season.AUTUMN -> autumnWood
            Season.WINTER -> bareWood
            else -> leafyWood
        }
        val columns = if (season == Season.WINTER) intArrayOf(-44, -33, -22, -15, 14, 22, 31, 41) else intArrayOf(-44, -33, -30)
        val out = ArrayList<IntArray>()
        for (dx in columns) {
            val x = BX + dx
            for (u in 45 until U_MAX - 3) {
                // Only where the wind doesn't bend the tree, or the bird would slide about on it
                if (u > STILL_TOP) break
                if (wood[idx(x, u)] < 0 || wood[idx(x, u + 1)] >= 0) continue
                val clear = season == Season.WINTER || (canopy[idx(x, u + 1)] < 0 && canopy[idx(x, u + 2)] < 0 && canopy[idx(x, u + 3)] < 0)
                if (clear) { out += intArrayOf(dx, u + 1); break }
            }
        }
        out
    }

    private val perchCache = HashMap<Season, List<IntArray>>()

    /** The screen point for a [perch] from [perches], for the tree with its foot at ([baseX], [groundY]). */
    fun perchAt(perch: IntArray, baseX: Float, groundY: Float, p: Float): Pair<Float, Float> {
        val left = ((baseX / p).roundToInt() - BX) * p
        val ground = (groundY / p).roundToInt() * p
        return Pair(left + (BX + perch[0]) * p + p / 2f, ground - perch[1] * p)
    }

    /**
     * A place under the canopy for a petal or a leaf to fall from, as (dx, height) in tree pixels
     * from the foot of the trunk.
     */
    fun dropPoint(random: Random): Pair<Int, Int> {
        val cells = dropCells
        val v = cells[random.nextInt(cells.size)]
        val u = ((v % 1000) + 1000) % 1000
        val dx = (v - u) / 1000
        return dx to u
    }
}
