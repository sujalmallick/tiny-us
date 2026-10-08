package com.example.engine

/**
 * Continues a scene's background above and below its stage (see [WorldCamera]) on a frame of ARGB
 * pixels, so a tall screen shows more sky and more ground instead of an empty band.
 *
 * It reads a band of rows at each edge of the stage:
 * - when the band's rows repeat exactly (a checkerboard floor, planks with joints), that pattern
 *   is tiled on;
 * - otherwise each row's most common colour is used; rows that repeat (plank seams, brick
 *   courses) keep repeating, and the edge row's colour carries on;
 * - above the stage, columns that are one colour all the way through the band (a wall stripe, a
 *   lamp cord, a window mullion) carry on as columns;
 * - below the stage, the ground's colour carries on with subtle grass ticks in its own shades (no
 *   scene pixels are copied, so props never repeat there), darkening in soft steps toward the edge;
 * - above an outdoor night sky (starry), a few stars.
 */
object StageExtension {
    private const val BAND = 16

    fun fill(px: IntArray, w: Int, h: Int, stageTop: Int, stageBottom: Int, starry: Boolean = false) {
        val stageH = stageBottom - stageTop
        if (stageH <= 0) return
        val band = minOf(BAND, stageH / 4).coerceAtLeast(1)
        if (stageTop > 0 && !tilePattern(px, w, stageTop, band, up = true, from = stageTop - 1, to = 0)) {
            fillAbove(px, w, stageTop, band, starry)
        }
        if (stageBottom < h) {
            if (!tilePattern(px, w, stageBottom - band, band, up = false, from = stageBottom, to = h - 1) &&
                !tileFloor(px, w, stageBottom, minOf(FLOOR_BAND, stageH / 3), h)
            ) {
                fillBelow(px, w, h, stageBottom, band)
            }
            // Whatever filled it (tiled floor, planks or ground), the area below the stage settles
            // into the same soft foreground shade, so every scene's bottom blends the same way.
            shadeBelow(px, w, h, stageBottom)
        }
    }

    /**
     * When the band's rows repeat exactly with some period, copies that pattern row by row into
     * [from]..[to]. Returns false when there is no such pattern.
     */
    private fun tilePattern(px: IntArray, w: Int, first: Int, band: Int, up: Boolean, from: Int, to: Int): Boolean {
        if (band < 4) return false
        var period = 0
        for (p in 1..band / 2) {
            var ok = true
            loop@ for (i in 0 until band - p) {
                val a = (first + i) * w
                val b = (first + i + p) * w
                for (x in 0 until w) if (px[a + x] != px[b + x]) { ok = false; break@loop }
            }
            if (ok) { period = p; break }
        }
        // One repeated row is a flat band, which the row-colour fill handles better.
        if (period < 2) return false
        if (up) {
            for (y in from downTo to) {
                val d = first - y
                val src = first + (period - d % period) % period
                px.copyInto(px, y * w, src * w, src * w + w)
            }
        } else {
            val last = first + band - 1
            for (y in from..to) {
                val d = y - last
                val src = last - period + 1 + (period - 1 + d) % period
                px.copyInto(px, y * w, src * w, src * w + w)
            }
        }
        return true
    }

    /** Rows sampled when looking for a floor pattern that has props standing on it. */
    private const val FLOOR_BAND = 72 // finds repeats up to 36 rows (two 16-pixel tile rows and more)

    /**
     * A floor pattern (checkerboard tiles, planks) that repeats with some period even though a
     * few props stand on it: rows match their period-mates for most pixels. The pattern is rebuilt
     * from the band with the floor's own colours winning over the props', then tiled downward.
     */
    private fun tileFloor(px: IntArray, w: Int, bottom: Int, band: Int, h: Int): Boolean {
        if (band < 8) return false
        val first = bottom - band
        // The band's colour counts: the two most common are the floor's own colours.
        val counts = HashMap<Int, Int>()
        for (i in first until bottom) for (x in 0 until w) {
            val c = px[i * w + x]
            counts[c] = (counts[c] ?: 0) + 1
        }
        val floorColours = counts.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()
        var period = 0
        for (p in 2..band / 2) {
            var same = 0
            var compared = 0
            for (i in 0 until band - p) {
                val a = (first + i) * w
                val b = (first + i + p) * w
                for (x in 0 until w) {
                    val ca = px[a + x]
                    val cb = px[b + x]
                    // Props (rugs, crates, plants) don't count either way.
                    if (ca !in floorColours || cb !in floorColours) continue
                    compared++
                    if (ca == cb) same++
                }
            }
            if (compared >= band * w / 3 && same >= compared * 0.97f) { period = p; break }
        }
        if (period == 0) return false
        // Template row k continues the pattern k rows after the band's end.
        val template = IntArray(period * w)
        for (k in 0 until period) {
            for (x in 0 until w) {
                // Prefer a floor colour from any period-mate row; fall back to the most common one.
                var best = 0
                var bestN = -1
                var i = bottom - period + k
                while (i >= first) {
                    val c = px[i * w + x]
                    val n = (counts[c] ?: 0) + (if (c in floorColours) 1_000_000 else 0)
                    if (n > bestN) { bestN = n; best = c }
                    i -= period
                }
                template[k * w + x] = best
            }
        }
        for (y in bottom until h) template.copyInto(px, y * w, ((y - bottom) % period) * w, ((y - bottom) % period) * w + w)
        return true
    }

    private fun fillAbove(px: IntArray, w: Int, top: Int, band: Int, starry: Boolean) {
        val rows = IntArray(band) { dominant(px, w, top + it) }
        val columns = steadyColumns(px, w, top, band, rows[0])
        val period = period(rows)
        val edge = rows[0]
        // Stars as thick as the stage's own (none when clouds or rain hide them).
        val starColour = if (starry) brightest(px, w, top, band, edge) else null
        val starEvery = if (starColour == null) 0 else starSpacing(px, w, top, band, edge)
        for (y in top - 1 downTo 0) {
            val d = top - y
            val base = if (period > 0) rows[(period - d % period) % period] else edge
            val row = y * w
            for (x in 0 until w) {
                var c = columns[x].takeIf { it != NONE } ?: base
                if (starEvery > 0 && c == base && hash(x, y) % starEvery == 0) c = starColour!!
                px[row + x] = c
            }
        }
    }

    /**
     * Continues the scene's ground below the stage in its own colour: each row takes the ground colour
     * (repeating row patterns such as terrace bands or plank seams carry on in step), with short grass
     * ticks a shade lighter or darker drawn in, never pixels copied from the scene, so a creek, a glow
     * or a plant at the stage's edge can't be repeated below it. The fill then darkens in a few dithered
     * steps toward the screen edge, like a foreground in shade.
     */
    private fun fillBelow(px: IntArray, w: Int, h: Int, bottom: Int, band: Int) {
        val first = bottom - band
        val rows = IntArray(band) { dominant(px, w, first + it) }
        val period = period(rows)
        val edge = rows[band - 1]
        for (y in bottom until h) {
            val d = y - bottom
            val base = if (period > 0) rows[band - period + (period + d) % period] else edge
            val row = y * w
            for (x in 0 until w) {
                // Grass ticks: a few columns get a 2-3 pixel mark per 3-row block, darker or lighter.
                val tick = hash(x, (d + (x % 3)) / 3) % 100
                px[row + x] = when {
                    tick < TICK_DARK -> darken(base, 0.14f)
                    tick < TICK_DARK + TICK_LIGHT -> lighten(base, 0.10f)
                    else -> base
                }
            }
        }
    }

    /**
     * Darkens the area below the stage in [SHADE_STEPS] soft steps toward the screen edge, like a
     * foreground in shade. The first step is untouched, so the join with the stage stays seamless,
     * and each later step begins with a checkerboard row or two of the lighter shade.
     */
    private fun shadeBelow(px: IntArray, w: Int, h: Int, bottom: Int) {
        val fillH = (h - bottom).coerceAtLeast(1)
        for (y in bottom until h) {
            val d = y - bottom
            val level = minOf(SHADE_STEPS - 1, d * SHADE_STEPS / fillH)
            if (level == 0) continue
            val levelStart = (level * fillH + SHADE_STEPS - 1) / SHADE_STEPS
            val row = y * w
            for (x in 0 until w) {
                val l = if (d - levelStart < 2 && (x + y) % 2 == 0) level - 1 else level
                if (l > 0) px[row + x] = darken(px[row + x], l * SHADE_PER_STEP)
            }
        }
    }

    private const val TICK_DARK = 5   // percent of 3-row blocks per column with a darker grass mark
    private const val TICK_LIGHT = 2  // ... and with a lighter one
    private const val SHADE_STEPS = 4
    private const val SHADE_PER_STEP = 0.08f

    private fun lighten(c: Int, amount: Float): Int {
        fun ch(v: Int) = (v + (255 - v) * amount).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch((c shr 16) and 0xFF) shl 16) or (ch((c shr 8) and 0xFF) shl 8) or ch(c and 0xFF)
    }

    private fun darken(c: Int, amount: Float): Int {
        if (amount <= 0f) return c
        val k = 1f - amount
        val r = (((c shr 16) and 0xFF) * k).toInt()
        val g = (((c shr 8) and 0xFF) * k).toInt()
        val b = ((c and 0xFF) * k).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private const val NONE = 0

    /** Per column: its colour when it is one colour through the whole band (and not the background), else [NONE]. */
    private fun steadyColumns(px: IntArray, w: Int, first: Int, band: Int, background: Int): IntArray {
        val out = IntArray(w)
        if (band < 4) return out
        for (x in 0 until w) {
            val c = px[first * w + x]
            if (c == background || c == NONE) continue
            var steady = true
            for (i in 1 until band) if (px[(first + i) * w + x] != c) { steady = false; break }
            if (steady) out[x] = c
        }
        return out
    }

    /** The shortest repeat in [rows] (at least two rows, and not all one colour), or 0. */
    private fun period(rows: IntArray): Int {
        if (rows.all { it == rows[0] }) return 0
        for (p in 2..rows.size / 2) {
            var ok = true
            for (i in 0 until rows.size - p) if (rows[i] != rows[i + p]) { ok = false; break }
            if (ok) return p
        }
        return 0
    }

    private fun dominant(px: IntArray, w: Int, y: Int): Int {
        val counts = HashMap<Int, Int>()
        var best = px[y * w]
        var bestN = 0
        for (x in 0 until w) {
            val c = px[y * w + x]
            val n = (counts[c] ?: 0) + 1
            counts[c] = n
            if (n > bestN) { bestN = n; best = c }
        }
        return best
    }

    /** One star per this many sky pixels, as in the band; 0 when the band has no stars. */
    private fun starSpacing(px: IntArray, w: Int, first: Int, band: Int, background: Int): Int {
        var bright = 0
        for (i in 0 until band) for (x in 0 until w) {
            val c = px[(first + i) * w + x]
            if (c != background && luma(c) > 150) bright++
        }
        return if (bright == 0) 0 else (band * w / bright).coerceAtLeast(40)
    }

    private fun brightest(px: IntArray, w: Int, first: Int, band: Int, background: Int): Int? {
        var best: Int? = null
        var bestL = 150
        for (i in 0 until band) for (x in 0 until w) {
            val c = px[(first + i) * w + x]
            if (c == background) continue
            val l = luma(c)
            if (l > bestL) { bestL = l; best = c }
        }
        return best
    }

    /** True when two colours differ by at most about a fifth on every channel. */
    private fun close(a: Int, b: Int): Boolean {
        for (shift in intArrayOf(16, 8, 0)) {
            if (kotlin.math.abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF)) > 48) return false
        }
        return true
    }

    private fun luma(c: Int): Int = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000

    /** Deterministic per-pixel noise, so the texture holds still from frame to frame. */
    private fun hash(x: Int, y: Int): Int {
        var v = x * 73856093 xor y * 19349663
        v = v xor (v ushr 13)
        v *= 1274126177
        v = v xor (v ushr 16)
        return v and 0x7FFFFFFF
    }
}
