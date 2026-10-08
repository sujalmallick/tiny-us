package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A small pixel-art image: one character per game pixel, '.' is see-through.
 * Horizontal runs of the same colour are merged once, so drawing a prop costs a handful of rects.
 */
class PixelGrid(val width: Int, val height: Int) {
    private val cells = CharArray(width * height) { '.' }

    operator fun get(x: Int, y: Int): Char =
        if (x in 0 until width && y in 0 until height) cells[y * width + x] else '.'

    fun set(x: Int, y: Int, ch: Char) {
        if (x in 0 until width && y in 0 until height) cells[y * width + x] = ch
    }

    fun rect(x: Int, y: Int, w: Int, h: Int, ch: Char) {
        for (yy in y until y + h) for (xx in x until x + w) set(xx, yy, ch)
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, ch: Char) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val sx = if (x0 < x1) 1 else -1
        val dy = -abs(y1 - y0)
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            set(x, y, ch)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    fun ring(cx: Int, cy: Int, r: Int, ch: Char) {
        var a = 0
        while (a < 360) {
            val t = a * PI / 180.0
            set((cx + r * cos(t)).roundToInt(), (cy + r * sin(t)).roundToInt(), ch)
            a += 3
        }
    }

    fun stamp(rows: List<String>, ox: Int = 0, oy: Int = 0) {
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, ch -> if (ch != '.') set(ox + x, oy + y, ch) } }
    }

    internal class Run(val x: Int, val y: Int, val length: Int, val ch: Char)

    /** Built on first draw, after the grid is finished. */
    internal val runs: List<Run> by lazy {
        val out = ArrayList<Run>()
        for (y in 0 until height) {
            var x = 0
            while (x < width) {
                val ch = cells[y * width + x]
                if (ch == '.') { x++; continue }
                var end = x + 1
                while (end < width && cells[y * width + end] == ch) end++
                out += Run(x, y, end - x, ch)
                x = end
            }
        }
        out
    }

    /** Number of see-through-free pixels, used by tests. */
    fun filledCount(): Int = cells.count { it != '.' }

    companion object {
        fun of(rows: List<String>): PixelGrid {
            val g = PixelGrid(rows.maxOf { it.length }, rows.size)
            g.stamp(rows)
            return g
        }
    }
}

/**
 * Small cozy props drawn from pixel grids (bridge, bicycle, flower fence, hanging basket, pet bed,
 * fish toy) plus tiny far-away versions (birdhouse, clothesline, signpost) that sit in a scene's
 * background, faded toward the distant colour. Every function anchors at bottom-centre like the
 * other world sprites and takes the scene's game pixel [p].
 */
object CozyProps {

    private val PALETTE: Map<Char, Color> = mapOf(
        'k' to Color(0xFF3B2418), 'w' to Color(0xFF6B4226), 'W' to Color(0xFF8F5A32), 'L' to Color(0xFFC28447),
        'g' to Color(0xFF2F6B34), 'G' to Color(0xFF4F9A45), 'h' to Color(0xFF86C95E),
        'p' to Color(0xFFF38FB0), 'P' to Color(0xFFFFD1E0), 'y' to Color(0xFFFFD166), 'v' to Color(0xFFB48EE0),
        'r' to Color(0xFFD9485F), 'R' to Color(0xFF9C2F45),
        'b' to Color(0xFF4F86D1), 'B' to Color(0xFF9CC5FF), 'n' to Color(0xFF2D4F8A),
        's' to Color(0xFF7F7C78), 'S' to Color(0xFFB9B4AB),
        'c' to Color(0xFFF4E6CF), 'C' to Color(0xFFD6BF9C),
        't' to Color(0xFF5FB3A8), 'T' to Color(0xFF3F8A80),
        'm' to Color(0xFF3A3340), 'M' to Color(0xFFA0A6B4), 'x' to Color(0xFFFFFFFF),
        'f' to Color(0xFFE88AA0), 'F' to Color(0xFFF9C4D0)
    )

    /** Draws [grid] with its top-left at ([left], [top]); [haze] fades every colour toward a distant tint. */
    fun drawGrid(
        scope: DrawScope,
        grid: PixelGrid,
        left: Float,
        top: Float,
        cell: Float,
        haze: Color? = null,
        hazeAmount: Float = 0f
    ) {
        for (run in grid.runs) {
            val base = PALETTE[run.ch] ?: continue
            val c = if (haze != null && hazeAmount > 0f) lerp(base, haze, hazeAmount) else base
            scope.drawRect(c, Offset(left + run.x * cell, top + run.y * cell), Size(run.length * cell, cell))
        }
    }

    private fun drawAnchored(scope: DrawScope, grid: PixelGrid, cx: Float, bottomY: Float, p: Float, haze: Color? = null, hazeAmount: Float = 0f) =
        drawGrid(scope, grid, cx - grid.width * p / 2f, bottomY - grid.height * p, p, haze, hazeAmount)

    // ---------------------------------------------------------------- footbridge and creek

    /** A small arched footbridge; its ends rest on stones. 28 x 9 game pixels. */
    val footbridge: PixelGrid by lazy {
        val g = PixelGrid(28, 9)
        val tops = IntArray(28)
        for (x in 1..26) {
            val t = (x - 1) / 25.0
            val top = 6 - (2 * sin(PI * t)).roundToInt()
            tops[x] = top
            g.set(x, top - 3, 'L')
            g.set(x, top - 2, 'w')
            g.set(x, top, 'L')
            g.set(x, top + 1, if ((x - 1) % 4 == 0) 'k' else 'W')
            g.set(x, top + 2, 'k')
        }
        var px = 2
        while (px <= 25) {
            g.rect(px, tops[px] - 4, 2, 4, 'w')
            g.set(px, tops[px] - 4, 'k'); g.set(px + 1, tops[px] - 4, 'k')
            g.set(px + 1, tops[px] - 3, 'W')
            px += 7
        }
        g.rect(0, 7, 3, 2, 'S'); g.rect(0, 8, 3, 1, 's')
        g.rect(25, 7, 3, 2, 'S'); g.rect(25, 8, 3, 1, 's')
        g
    }

    fun drawFootbridge(scope: DrawScope, cx: Float, bottomY: Float, p: Float) =
        drawAnchored(scope, footbridge, cx, bottomY, p)

    /**
     * A shallow creek across [left]..[left]+[width], [rows] game pixels tall, with drifting glints.
     * [rippleX] / [rippleProgress] (0..1, or < 0 for none) draw an expanding ring where it was tapped.
     */
    fun drawCreek(
        scope: DrawScope,
        left: Float,
        top: Float,
        width: Float,
        rows: Int,
        p: Float,
        time: Float,
        rippleX: Float,
        rippleProgress: Float
    ) {
        val water = Color(0xFF3F74BD)
        val edge = Color(0xFF9CC5FF)
        val deep = Color(0xFF2D4F8A)
        scope.drawRect(water, Offset(left, top), Size(width, rows * p))
        scope.drawRect(edge, Offset(left, top), Size(width, p))
        scope.drawRect(deep, Offset(left, top + (rows - 1) * p), Size(width, p))
        // Glints drift slowly downstream (left to right).
        val cols = (width / p).toInt().coerceAtLeast(1)
        val step = (time * 4f).toInt()
        for (i in 0 until 9) {
            val gx = ((i * 41 + step * (1 + i % 2)) % cols)
            val gy = 1 + (i % (rows - 2).coerceAtLeast(1))
            scope.drawRect(edge, Offset(left + gx * p, top + gy * p), Size(2f * p, p))
        }
        if (rippleProgress in 0f..1f) {
            val r = (1f + rippleProgress * 7f) * p
            val midY = top + (rows / 2) * p
            val ring = lerp(Color.White, water, rippleProgress)
            for (side in listOf(-1f, 1f)) {
                scope.drawRect(ring, Offset(rippleX + side * r - p, midY), Size(2f * p, p))
                scope.drawRect(ring, Offset(rippleX + side * r * 0.55f - p, midY - p), Size(2f * p, p))
                scope.drawRect(ring, Offset(rippleX + side * r * 0.55f - p, midY + p), Size(2f * p, p))
            }
        }
    }

    // ---------------------------------------------------------------- bicycle

    /** A red bicycle with a front basket. 29 x 19 game pixels; flowers in the basket are drawn separately so they can bob. */
    val bicycle: PixelGrid by lazy {
        val g = PixelGrid(29, 19)
        for (cx in listOf(7, 22)) {
            g.ring(cx, 13, 5, 'm'); g.ring(cx, 13, 4, 'M')
            g.line(cx - 3, 13, cx + 3, 13, 'S'); g.line(cx, 10, cx, 16, 'S'); g.set(cx, 13, 'k')
        }
        g.line(7, 13, 14, 14, 'R'); g.line(14, 14, 12, 7, 'r'); g.line(7, 13, 12, 7, 'r')
        g.line(12, 7, 19, 6, 'r'); g.line(14, 14, 19, 7, 'r'); g.line(19, 6, 22, 13, 'M')
        g.rect(10, 5, 5, 1, 'C'); g.set(12, 6, 'k')
        g.line(18, 4, 21, 4, 'm'); g.set(19, 5, 'm'); g.set(17, 4, 'M')
        g.set(14, 14, 'S'); g.set(13, 15, 'k'); g.set(15, 13, 'k')
        g.rect(21, 4, 7, 5, 'W')
        for (x in 21 until 28 step 2) { g.set(x, 5, 'L'); g.set(x + 1, 7, 'L') }
        g.rect(21, 8, 7, 1, 'k')
        g
    }

    private val basketFlowers = listOf(Triple(21, 3, 'G'), Triple(22, 3, 'p'), Triple(23, 2, 'P'), Triple(24, 3, 'y'), Triple(25, 3, 'G'), Triple(26, 2, 'p'), Triple(27, 3, 'G'))

    /** [bellProgress] 0..1 while the bell rings (< 0 when idle): the basket flowers bob and the bell glints. */
    fun drawBicycle(scope: DrawScope, cx: Float, groundY: Float, p: Float, bellProgress: Float) {
        val left = cx - bicycle.width * p / 2f
        val top = groundY - bicycle.height * p
        drawGrid(scope, bicycle, left, top, p)
        val ringing = bellProgress in 0f..1f
        val bob = if (ringing && sin(bellProgress * PI.toFloat() * 6f) > 0f) -p else 0f
        for ((x, y, ch) in basketFlowers) {
            scope.drawRect(PALETTE.getValue(ch), Offset(left + x * p, top + y * p + bob), Size(p, p))
        }
        if (ringing && bellProgress < 0.7f) {
            val glint = PALETTE.getValue('y')
            scope.drawRect(glint, Offset(left + 15f * p, top + 2f * p), Size(p, p))
            scope.drawRect(glint, Offset(left + 16f * p, top + 1f * p), Size(p, p))
            scope.drawRect(glint, Offset(left + 15f * p, top + 5f * p), Size(p, p))
        }
    }

    // ---------------------------------------------------------------- flower fence

    private val BUSH = listOf("...p.y...", ".pGPGpGy.", "GhGpGGhPG", "gGhGGyGhg", "gGGGhGGGg", ".ggggggg.")

    /** A short wooden fence with flowering bushes at its foot. [width] game pixels wide, 16 tall. */
    fun flowerFenceGrid(width: Int): PixelGrid {
        val g = PixelGrid(width, 16)
        g.rect(0, 5, width, 2, 'L'); g.rect(0, 7, width, 1, 'w')
        g.rect(0, 10, width, 2, 'L'); g.rect(0, 12, width, 1, 'w')
        var x = 1
        while (x < width - 2) {
            g.rect(x, 2, 3, 13, 'W'); g.rect(x, 2, 1, 13, 'w'); g.rect(x, 1, 3, 1, 'k')
            x += 13
        }
        val vines = listOf(Triple(3, 4, 'G'), Triple(4, 4, 'p'), Triple(6, 4, 'G'), Triple(9, 9, 'G'), Triple(10, 9, 'y'), Triple(11, 9, 'G'),
            Triple(18, 4, 'G'), Triple(19, 4, 'v'), Triple(20, 4, 'G'), Triple(23, 9, 'G'), Triple(24, 9, 'p'))
        for ((vx, vy, ch) in vines) g.set(vx, vy, ch)
        var bx = 3
        while (bx + 9 <= width) { g.stamp(BUSH, bx, 10); bx += 13 }
        g.rect(0, 15, width, 1, 'g')
        return g
    }

    private val fenceCache = HashMap<Int, PixelGrid>()
    fun flowerFence(width: Int): PixelGrid = fenceCache.getOrPut(width) { flowerFenceGrid(width) }

    /** [rustleProgress] 0..1 while petals are shaken loose (< 0 when idle): the blooms bob. */
    fun drawFlowerFence(scope: DrawScope, cx: Float, groundY: Float, p: Float, widthPx: Int, rustleProgress: Float) {
        val grid = flowerFence(widthPx)
        val left = cx - grid.width * p / 2f
        val top = groundY - grid.height * p
        drawGrid(scope, grid, left, top, p)
        if (rustleProgress in 0f..1f) {
            val lift = if (sin(rustleProgress * PI.toFloat() * 5f) > 0f) p else 0f
            if (lift > 0f) {
                var bx = 3
                while (bx + 9 <= grid.width) {
                    scope.drawRect(PALETTE.getValue('p'), Offset(left + (bx + 3) * p, top + 9f * p), Size(p, p))
                    scope.drawRect(PALETTE.getValue('y'), Offset(left + (bx + 5) * p, top + 9f * p), Size(p, p))
                    bx += 13
                }
            }
        }
    }

    // ---------------------------------------------------------------- hanging basket

    /** A hanging flower basket on three cords, hook at the top centre. 14 x 14 game pixels. */
    val hangingBasket: PixelGrid by lazy {
        PixelGrid.of(listOf(
            "......kk......",
            ".....k..k.....",
            "....k....k....",
            "...k......k...",
            "..k........k..",
            ".kpPpGpyPpGpk.",
            "kGpGhpGpPGhpGk",
            "kwWLWLWLWLWLwk",
            ".kwWLWLWLWLwk.",
            "..kwWWWWWWwk..",
            "...kkkkkkkk...",
            "...G..G..h....",
            "...p..G..G....",
            "......p......."
        ))
    }

    /** Hangs from ([cx], [hookY]). [swayProgress] 0..1 swings it after a tap and settles (< 0 when idle). */
    fun drawHangingBasket(scope: DrawScope, cx: Float, hookY: Float, p: Float, swayProgress: Float, time: Float) {
        val idle = sin(time * 0.9f) * 0.4f
        val tap = if (swayProgress in 0f..1f) sin(swayProgress * PI.toFloat() * 4f) * (1f - swayProgress) * 2.5f else 0f
        val shift = (idle + tap).roundToInt() * p
        drawGrid(scope, hangingBasket, cx - hangingBasket.width * p / 2f + shift, hookY, p)
    }

    // ---------------------------------------------------------------- pet bed and fish toy

    /** A round pet bed with a paw-print cushion. 22 x 10 game pixels. */
    val petBed: PixelGrid by lazy {
        val k14 = "kkkkkkkkkkkkkk"
        PixelGrid.of(listOf(
            "....kkkkkkkkkkkkkk....",
            "..kk" + "cCcCcCcCcCcCcC" + "kk..",
            ".kcC" + k14 + "Cck.",
            ".kCk" + "FFfFFFFFFFfFFF" + "kCk.",
            "kcCk" + "FFFFFRFRFRFFFF" + "kCck",
            "kcCk" + "FFFFFFRRRFFFFF" + "kCck",
            "kcCk" + "FFFFFRRRRRFFFF" + "kCck",
            ".kcC" + k14 + "Cck.",
            ".k" + "cCcCcCcCcCcCcCcCcC" + "k.",
            "..kkkkkkkkkkkkkkkkkk.."
        ))
    }

    /** A little blue fish toy. 14 x 7 game pixels. */
    val fishToy: PixelGrid by lazy {
        PixelGrid.of(listOf(
            "....kkkkk.....",
            "..kkBBBBBkk.kk",
            ".kBkBBbBBBBkBk",
            "kBBBBbBbBBBBBk",
            ".kbbbbbbbbbkBk",
            "..kkbbbbbkk.kk",
            "....kkkkk....."
        ))
    }

    fun drawPetBed(scope: DrawScope, cx: Float, floorY: Float, p: Float) {
        drawContactShadow(scope, cx, floorY, petBed.width, p)
        drawAnchored(scope, petBed, cx, floorY, p)
    }

    fun drawFishToy(scope: DrawScope, cx: Float, floorY: Float, p: Float) {
        drawContactShadow(scope, cx, floorY, fishToy.width - 2, p)
        drawAnchored(scope, fishToy, cx, floorY, p)
    }

    // ---------------------------------------------------------------- far-away props

    /** A birdhouse on a post, seen from far away. 7 x 12 game pixels. */
    val farBirdhouse: PixelGrid by lazy {
        PixelGrid.of(listOf(
            "...k...",
            "..kRk..",
            ".kRrRk.",
            "kkkkkkk",
            ".kLmLk.",
            ".kLLLk.",
            ".kkkkk.",
            "...w...",
            "...w...",
            "...w...",
            "..GwG..",
            "gGGGGGg"
        ))
    }

    /** A trail signpost seen from far away. 9 x 12 game pixels. */
    val farSignpost: PixelGrid by lazy {
        PixelGrid.of(listOf(
            "....kk...",
            ".kkkkkkk.",
            ".kLLLLLLk",
            ".kkkkkkk.",
            "....wW...",
            "kkkkkkk..",
            "kLLLLLLk.",
            ".kkkkkkk.",
            "....wW...",
            "....wW...",
            "...GwWG..",
            "..gGGGGg."
        ))
    }

    /** A washing line with three towels, seen from far away. 24 x 10 game pixels. */
    val farClothesline: PixelGrid by lazy {
        val g = PixelGrid(24, 10)
        g.rect(0, 1, 1, 9, 'W'); g.rect(23, 1, 1, 9, 'W')
        for (x in 1..22) {
            val sag = (1.5 * sin(PI * (x - 1) / 21.0)).roundToInt()
            g.set(x, 2 + sag, 'C')
        }
        val towels = listOf(Triple(4, 'c', 'C'), Triple(10, 'F', 'f'), Triple(16, 't', 'T'))
        for ((tx, a, b) in towels) {
            val top = 3 + (1.5 * sin(PI * (tx) / 21.0)).roundToInt()
            g.rect(tx, top, 3, 4, a)
            g.rect(tx, top + 3, 3, 1, b)
        }
        g.rect(0, 9, 24, 1, 'g')
        g
    }

    /** Draws a far-away prop bottom-centred at ([cx], [groundY]), faded [hazeAmount] of the way to [haze]. */
    fun drawFar(scope: DrawScope, grid: PixelGrid, cx: Float, groundY: Float, p: Float, haze: Color, hazeAmount: Float) =
        drawAnchored(scope, grid, cx, groundY, p, haze, hazeAmount)
}
